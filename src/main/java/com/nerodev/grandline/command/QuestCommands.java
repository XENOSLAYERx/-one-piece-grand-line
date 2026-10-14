package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.quest.Journey;
import com.nerodev.grandline.quest.LogPose;
import com.nerodev.grandline.quest.QuestDefinition;
import com.nerodev.grandline.quest.Quests;
import com.nerodev.grandline.registry.DefinitionType;
import com.nerodev.grandline.world.island.IslandType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** {@code /onepiece quest} and {@code /onepiece logpose}: the quest journal, taking quests, and the Log Pose. */
final class QuestCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece quest", "Your quest journal: what you are doing and how far along you are", PermissionTier.PLAYER),
            new HelpEntry("/onepiece quest list", "Every quest and where you stand with it", PermissionTier.PLAYER),
            new HelpEntry("/onepiece quest accept <quest>", "Take a quest an islander near you offers", PermissionTier.PLAYER),
            new HelpEntry("/onepiece quest track|abandon <quest>", "Show a quest on screen, or give it up", PermissionTier.PLAYER),
            new HelpEntry("/onepiece quest complete <quest> [players]", "Finish a quest at once, with its rewards", PermissionTier.CREATOR),
            new HelpEntry("/onepiece quest reset [quest] [players]", "Forget one quest or every quest", PermissionTier.CREATOR),
            new HelpEntry("/onepiece logpose", "The islands you have found and where your Log Pose points", PermissionTier.PLAYER),
            new HelpEntry("/onepiece logpose next|set <island>", "Set the Log Pose on an unknown island or a found one", PermissionTier.PLAYER),
            new HelpEntry("/onepiece logpose record <island>|forget", "Record the island here as found, or forget them all",
                    PermissionTier.CREATOR));

    /** Islanders who can offer a quest are this close when it is accepted. */
    private static final double GIVER_RANGE = 12;

    private static final SuggestionProvider<CommandSourceStack> ISLANDS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            Arrays.stream(IslandType.values()).map(IslandType::getSerializedName), builder);

    private QuestCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("quest")
                .executes(ctx -> journal(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("accept").then(quest().executes(QuestCommands::accept)))
                .then(Commands.literal("track").then(quest().executes(ctx -> track(ctx, false))))
                .then(Commands.literal("abandon").then(quest().executes(ctx -> track(ctx, true))))
                .then(creator("complete").then(quest()
                        .executes(ctx -> complete(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> complete(ctx, EntityArgument.getPlayers(ctx, "players"))))))
                .then(creator("reset")
                        .executes(ctx -> reset(ctx.getSource(), null, List.of(ctx.getSource().getPlayerOrException())))
                        .then(quest()
                                .executes(ctx -> reset(ctx.getSource(), ResourceLocationArgument.getId(ctx, "quest"),
                                        List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> reset(ctx.getSource(), ResourceLocationArgument.getId(ctx, "quest"),
                                                EntityArgument.getPlayers(ctx, "players")))))));

        root.then(Commands.literal("logpose")
                .executes(ctx -> logPose(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("next").executes(ctx -> next(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("set").then(Commands.argument("island", StringArgumentType.word()).suggests(ISLANDS)
                        .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "island")))))
                .then(creator("record").then(Commands.argument("island", StringArgumentType.word()).suggests(ISLANDS)
                        .executes(ctx -> record(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "island")))))
                .then(creator("forget").executes(ctx -> forget(ctx.getSource(), ctx.getSource().getPlayerOrException()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> quest() {
        return Commands.argument("quest", ResourceLocationArgument.id()).suggests(RegistryArguments.suggest(DefinitionType.QUEST));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Quests
    // ---------------------------------------------------------------------------------------------------------

    private static int journal(CommandSourceStack source, ServerPlayer player) {
        RegistryAccess access = source.registryAccess();
        Journey journey = GLPlayer.data(player).journey();
        Text.send(source, Text.header("Quest journal"));
        if (journey.activeQuests().isEmpty()) {
            Text.send(source, Component.literal("  No quests under way. Speak to islanders; some will have work for you.")
                    .withStyle(ChatFormatting.GRAY));
        }
        for (ResourceLocation id : journey.activeQuests()) {
            QuestDefinition quest = Quests.get(access, id);
            if (quest == null) {
                continue;
            }
            boolean tracked = journey.tracked().map(id::equals).orElse(false);
            Text.send(source, Component.literal(tracked ? "> " : "  ").withStyle(ChatFormatting.GOLD).append(quest.displayName().copy()
                    .withStyle(quest.category() == QuestDefinition.Category.MAIN ? ChatFormatting.GOLD : ChatFormatting.YELLOW))
                    .append(Component.literal(quest.category() == QuestDefinition.Category.MAIN ? "  (story)" : "").withStyle(ChatFormatting.DARK_GRAY)));
            for (int i = 0; i < quest.objectives().size(); i++) {
                Text.send(source, Component.literal("  ").append(Quests.objectiveLine(access, quest.objectives().get(i), journey.progress(id, i))));
            }
        }
        Text.send(source, Component.literal("  " + journey.completedQuests().size() + " quests done").withStyle(ChatFormatting.DARK_GRAY));
        return journey.activeQuests().size();
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        RegistryAccess access = source.registryAccess();
        Journey journey = GLPlayer.data(player).journey();
        Text.send(source, Text.header("Quests"));
        List<Map.Entry<ResourceLocation, QuestDefinition>> quests = Quests.all(access);
        for (Map.Entry<ResourceLocation, QuestDefinition> e : quests) {
            ResourceLocation id = e.getKey();
            QuestDefinition quest = e.getValue();
            String blocker = Quests.blocker(player, id, quest);
            MutableComponent state = journey.isCompleted(id) ? Component.literal("done").withStyle(ChatFormatting.GREEN)
                    : journey.isActive(id) ? Component.literal("under way").withStyle(ChatFormatting.YELLOW)
                    : blocker == null ? Component.literal(quest.giver().isPresent() ? "offered by " + quest.giver().get().getPath()
                            : "ready").withStyle(ChatFormatting.AQUA)
                    : Component.literal(blocker).withStyle(ChatFormatting.DARK_GRAY);
            Text.send(source, Component.literal(quest.category() == QuestDefinition.Category.MAIN ? "  * " : "  - ")
                    .withStyle(ChatFormatting.GOLD).append(quest.displayName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + id + "): ").withStyle(ChatFormatting.DARK_GRAY)).append(state));
        }
        return quests.size();
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Map.Entry<ResourceLocation, QuestDefinition> quest = RegistryArguments.get(ctx, "quest", DefinitionType.QUEST);
        String blocker = Quests.blocker(player, quest.getKey(), quest.getValue());
        if (blocker != null) {
            source.sendFailure(Component.literal("You can't take that quest: " + blocker));
            return 0;
        }
        if (quest.getValue().giver().isPresent() && !Permissions.has(source, PermissionTier.CREATOR)) {
            ResourceLocation giver = quest.getValue().giver().get();
            boolean near = !player.serverLevel().getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(GIVER_RANGE),
                    npc -> giver.equals(npc.npcId()) && npc.isAlive()).isEmpty();
            if (!near) {
                source.sendFailure(Component.literal("Only an islander who offers that quest can give it to you"));
                return 0;
            }
        }
        Quests.start(player, quest.getKey(), quest.getValue());
        return 1;
    }

    private static int track(CommandContext<CommandSourceStack> ctx, boolean abandon) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        if (!GLPlayer.data(player).journey().isActive(id)) {
            source.sendFailure(Component.literal("That quest isn't under way"));
            return 0;
        }
        if (abandon) {
            QuestDefinition quest = Quests.get(source.registryAccess(), id);
            if (quest != null && quest.category() == QuestDefinition.Category.MAIN) {
                source.sendFailure(Component.literal("The story can't be abandoned, only put off"));
                return 0;
            }
            Quests.reset(player, id);
            source.sendSuccess(() -> Component.literal("Quest abandoned"), false);
        } else {
            GLPlayer.edit(player, data -> data.journey().track(id));
            source.sendSuccess(() -> Component.literal("Tracking quest"), false);
        }
        return 1;
    }

    private static int complete(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) throws CommandSyntaxException {
        Map.Entry<ResourceLocation, QuestDefinition> quest = RegistryArguments.get(ctx, "quest", DefinitionType.QUEST);
        for (ServerPlayer player : players) {
            if (GLPlayer.data(player).journey().isCompleted(quest.getKey())) {
                continue;
            }
            if (!GLPlayer.data(player).journey().isActive(quest.getKey())) {
                GLPlayer.edit(player, data -> data.journey().start(quest.getKey(), quest.getValue().objectives().size()));
            }
            Quests.complete(player, quest.getKey(), quest.getValue(), true);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Completed ").append(quest.getValue().displayName())
                .append(" for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int reset(CommandSourceStack source, ResourceLocation id, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            Quests.reset(player, id);
            if (id == null) {
                Quests.startDue(player);
            }
        }
        source.sendSuccess(() -> Component.literal((id == null ? "Reset every quest" : "Reset " + id) + " for " + players.size()
                + " player(s)"), true);
        return players.size();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The Log Pose
    // ---------------------------------------------------------------------------------------------------------

    private static int logPose(CommandSourceStack source, ServerPlayer player) {
        Journey journey = GLPlayer.data(player).journey();
        Text.send(source, Text.header("Log Pose"));
        Text.send(source, Component.literal("  ").append(LogPose.reading(player)));
        if (journey.islands().isEmpty()) {
            Text.send(source, Component.literal("  No islands recorded yet").withStyle(ChatFormatting.GRAY));
        }
        for (Journey.Island island : journey.islands()) {
            IslandType type = IslandType.byName(island.type());
            BlockPos at = island.centre();
            Text.send(source, Component.literal("  ").append(type == null ? Component.literal(island.type()) : type.displayName().copy()
                    .withStyle(ChatFormatting.GOLD)).append(Component.literal(" at " + at.getX() + ", " + at.getZ()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("  ")).append(Text.run("[set]", "/onepiece logpose set " + island.type(), ChatFormatting.AQUA)));
        }
        return journey.islands().size();
    }

    private static int next(CommandSourceStack source, ServerPlayer player) {
        BlockPos target = LogPose.aimAtUnknown(player);
        if (target == null) {
            source.sendFailure(Component.literal("The needle drifts: no island pulls at it within reach"));
            return 0;
        }
        source.sendSuccess(() -> LogPose.reading(player), false);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer player, String name) {
        IslandType island = IslandType.byName(name);
        if (island == null || !LogPose.aimAt(player, island)) {
            source.sendFailure(Component.literal("Your Log Pose hasn't recorded " + name));
            return 0;
        }
        source.sendSuccess(() -> LogPose.reading(player), false);
        return 1;
    }

    private static int record(CommandSourceStack source, ServerPlayer player, String name) {
        IslandType island = IslandType.byName(name);
        if (island == null) {
            source.sendFailure(Component.literal("Unknown island: " + name));
            return 0;
        }
        BlockPos centre = BlockPos.containing(source.getPosition());
        if (!LogPose.discover(player, island, centre)) {
            source.sendFailure(Component.literal("That island is already recorded"));
            return 0;
        }
        return 1;
    }

    private static int forget(CommandSourceStack source, ServerPlayer player) {
        GLPlayer.edit(player, data -> data.journey().clearIslands());
        source.sendSuccess(() -> Component.literal("Forgot every recorded island"), true);
        return 1;
    }
}
