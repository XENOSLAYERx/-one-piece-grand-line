package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.data.DeathMode;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.Titles;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.definition.RaceDefinition;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands that edit a player's identity: race, faction, bounty, death mode, resets. The creator commands default to
 * the caller and optionally take target players; players themselves may choose their race once and join or leave a
 * faction that trusts them.
 */
final class IdentityCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece race set <race> [players]", "Change race instantly", PermissionTier.CREATOR),
            new HelpEntry("/onepiece race choose <race>", "Choose your race (once)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece faction join <faction>", "Join a faction that trusts you (Friendly standing)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece faction leave", "Leave your faction (it will think less of you)", PermissionTier.PLAYER),
            new HelpEntry("/onepiece title", "List the titles you have earned", PermissionTier.PLAYER),
            new HelpEntry("/onepiece title wear <title> | clear", "Wear an earned title before your name, or none", PermissionTier.PLAYER),
            new HelpEntry("/onepiece faction set <faction> [players]", "Join a faction", PermissionTier.CREATOR),
            new HelpEntry("/onepiece faction clear [players]", "Leave your faction", PermissionTier.CREATOR),
            new HelpEntry("/onepiece bounty set <amount> [players]", "Set bounty", PermissionTier.CREATOR),
            new HelpEntry("/onepiece bounty add <amount> [players]", "Add to (or subtract from) bounty", PermissionTier.CREATOR),
            new HelpEntry("/onepiece bounty reset [players]", "Clear bounty", PermissionTier.CREATOR),
            new HelpEntry("/onepiece deathmode <normal|creator> [players]", "Creator mode keeps items and XP on death", PermissionTier.CREATOR),
            new HelpEntry("/onepiece reset <part> [confirm] [players]", "Reset race, fruit, haki, faction, bounty, mastery or all", PermissionTier.CREATOR));

    /** What {@code /onepiece reset} can reset. */
    private enum ResetPart {
        RACE("race", data -> {
            data.setRace(PlayerData.DEFAULT_RACE);
            data.setCounter(RaceManager.CHOSEN, 0);
        }),
        FRUIT("fruit", data -> { }),
        HAKI("haki", data -> { }),
        FACTION("faction", data -> {
            data.setFaction(Optional.empty());
            data.clearReputation();
        }),
        BOUNTY("bounty", data -> data.setBounty(0)),
        MASTERY("mastery", PlayerData::clearMastery),
        ALL("all", data -> data.copyFrom(new PlayerData()));

        final String name;
        final Consumer<PlayerData> reset;

        ResetPart(String name, Consumer<PlayerData> reset) {
            this.name = name;
            this.reset = reset;
        }
    }

    private IdentityCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        // "race" and "faction" are open to everyone: their creator subcommands carry the CREATOR requirement.
        root.then(Commands.literal("race")
                .then(creator("set")
                        .then(withTargets(Commands.argument("race", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.RACE)), IdentityCommands::setRace)))
                .then(Commands.literal("choose")
                        .then(Commands.argument("race", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.RACE))
                                .executes(IdentityCommands::chooseRace))));

        root.then(Commands.literal("title")
                .executes(ctx -> listTitles(ctx.getSource()))
                .then(Commands.literal("wear")
                        .then(Commands.argument("title", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(earnedTitles(ctx.getSource()), builder))
                                .executes(ctx -> wearTitle(ctx.getSource(), Optional.of(ResourceLocationArgument.getId(ctx, "title"))))))
                .then(Commands.literal("clear").executes(ctx -> wearTitle(ctx.getSource(), Optional.empty()))));

        root.then(Commands.literal("faction")
                .then(creator("set")
                        .then(withTargets(Commands.argument("faction", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.FACTION)), IdentityCommands::setFaction)))
                .then(withTargets(creator("clear"), IdentityCommands::clearFaction))
                .then(Commands.literal("join")
                        .then(Commands.argument("faction", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.FACTION))
                                .executes(IdentityCommands::joinFaction)))
                .then(Commands.literal("leave").executes(ctx -> leaveFaction(ctx.getSource()))));

        root.then(creator("bounty")
                .then(Commands.literal("set")
                        .then(withTargets(Commands.argument("amount", LongArgumentType.longArg(0, PlayerData.MAX_BOUNTY)),
                                (ctx, targets) -> changeBounty(ctx, targets, false))))
                .then(Commands.literal("add")
                        .then(withTargets(Commands.argument("amount", LongArgumentType.longArg(-PlayerData.MAX_BOUNTY, PlayerData.MAX_BOUNTY)),
                                (ctx, targets) -> changeBounty(ctx, targets, true))))
                .then(withTargets(Commands.literal("reset"), IdentityCommands::resetBounty)));

        LiteralArgumentBuilder<CommandSourceStack> deathMode = creator("deathmode");
        for (DeathMode mode : DeathMode.values()) {
            deathMode.then(withTargets(Commands.literal(mode.getSerializedName()),
                    (ctx, targets) -> setDeathMode(ctx, targets, mode)));
        }
        root.then(deathMode);

        LiteralArgumentBuilder<CommandSourceStack> reset = creator("reset");
        for (ResetPart part : ResetPart.values()) {
            reset.then(Commands.literal(part.name)
                    .executes(ctx -> warnReset(ctx.getSource(), part))
                    .then(withTargets(Commands.literal("confirm"), (ctx, targets) -> reset(ctx, targets, part))));
        }
        root.then(reset);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Builders
    // ---------------------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface TargetedCommand {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    /** Runs {@code command} on the caller, or on the players given as an optional trailing argument. */
    private static <B extends ArgumentBuilder<CommandSourceStack, B>> B withTargets(B node, TargetedCommand command) {
        return node
                .executes(ctx -> command.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> command.run(ctx, EntityArgument.getPlayers(ctx, "players"))));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------------------------------------------

    private static int setRace(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        Map.Entry<ResourceLocation, RaceDefinition> race = RegistryArguments.get(ctx, "race", DefinitionType.RACE);
        for (ServerPlayer player : targets) {
            RaceManager.setRace(player, race.getKey());
        }
        Component name = race.getValue().displayName();
        feedback(ctx.getSource(), targets, p -> Component.literal("Set race of " + p + " to ").append(name));
        return targets.size();
    }

    private static int setFaction(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        Map.Entry<ResourceLocation, FactionDefinition> faction = RegistryArguments.get(ctx, "faction", DefinitionType.FACTION);
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> data.setFaction(Optional.of(faction.getKey())));
        }
        Component name = ProfileCommand.factionName(ctx.getSource().registryAccess(), faction.getKey());
        feedback(ctx.getSource(), targets, p -> Component.literal(p + " joined ").append(name));
        return targets.size();
    }

    private static int chooseRace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Map.Entry<ResourceLocation, RaceDefinition> race = RegistryArguments.get(ctx, "race", DefinitionType.RACE);
        Optional<Component> refusal = RaceManager.choose(player, race.getKey());
        if (refusal.isPresent()) {
            ctx.getSource().sendFailure(refusal.get());
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("You are ").append(race.getValue().displayName())
                .append(Component.literal(" now, for good.")).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int joinFaction(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Map.Entry<ResourceLocation, FactionDefinition> faction = RegistryArguments.get(ctx, "faction", DefinitionType.FACTION);
        Optional<Component> refusal = Factions.join(player, faction.getKey());
        if (refusal.isPresent()) {
            ctx.getSource().sendFailure(refusal.get());
            return 0;
        }
        Component name = ProfileCommand.factionName(ctx.getSource().registryAccess(), faction.getKey());
        ctx.getSource().sendSuccess(() -> Component.literal("You joined the ").append(name).append("."), false);
        return 1;
    }

    private static int leaveFaction(CommandSourceStack source) throws CommandSyntaxException {
        Optional<ResourceLocation> left = Factions.leave(source.getPlayerOrException());
        if (left.isEmpty()) {
            source.sendFailure(Component.literal("You belong to no faction."));
            return 0;
        }
        Component name = ProfileCommand.factionName(source.registryAccess(), left.get());
        source.sendSuccess(() -> Component.literal("You left the ").append(name).append(". They will not forget it."), false);
        return 1;
    }

    private static Set<ResourceLocation> earnedTitles(CommandSourceStack source) {
        return source.getPlayer() == null ? Set.of() : GLPlayer.data(source.getPlayer()).unlocked(UnlockCategory.TITLE);
    }

    private static int listTitles(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerData data = GLPlayer.data(player);
        Set<ResourceLocation> earned = data.unlocked(UnlockCategory.TITLE);
        Text.send(source, Text.header("Titles"));
        if (earned.isEmpty()) {
            Text.send(source, Component.literal("  None yet: the story's chapters grant them.").withStyle(ChatFormatting.GRAY));
        }
        for (ResourceLocation title : earned) {
            boolean worn = data.title().map(title::equals).orElse(false);
            Text.send(source, Component.literal(worn ? "  * " : "  - ").append(Titles.name(title).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" (" + title + ")" + (worn ? ", worn" : "")).withStyle(ChatFormatting.GRAY)));
        }
        return earned.size();
    }

    private static int wearTitle(CommandSourceStack source, Optional<ResourceLocation> title) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Titles.wear(player, title)) {
            source.sendFailure(Component.literal("You have not earned that title."));
            return 0;
        }
        source.sendSuccess(() -> title.map(t -> Component.literal("You now go by ").append(Titles.name(t)).append("."))
                .orElse(Component.literal("You wear no title.")), false);
        return 1;
    }

    private static int clearFaction(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> data.setFaction(Optional.empty()));
        }
        feedback(ctx.getSource(), targets, p -> Component.literal(p + " left their faction"));
        return targets.size();
    }

    private static int changeBounty(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean add) {
        long amount = LongArgumentType.getLong(ctx, "amount");
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> {
                if (add) {
                    data.addBounty(amount);
                } else {
                    data.setBounty(amount);
                }
            });
        }
        Component formatted = ProfileCommand.formatBounty(amount);
        feedback(ctx.getSource(), targets, p -> add
                ? Component.literal("Added ").append(formatted).append(" to the bounty of " + p)
                : Component.literal("Set bounty of " + p + " to ").append(formatted));
        return targets.size();
    }

    private static int resetBounty(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> data.setBounty(0));
        }
        feedback(ctx.getSource(), targets, p -> Component.literal("Cleared the bounty of " + p));
        return targets.size();
    }

    private static int setDeathMode(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, DeathMode mode) {
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> data.setDeathMode(mode));
        }
        feedback(ctx.getSource(), targets, p -> Component.literal("Death mode of " + p + " is now ").append(mode.display()));
        return targets.size();
    }

    private static int warnReset(CommandSourceStack source, ResetPart part) {
        String confirm = "/" + OnePieceCommand.ROOT + " reset " + part.name + " confirm";
        Text.send(source, Component.literal("This resets your " + part.name + " data and cannot be undone. ")
                .withStyle(ChatFormatting.RED)
                .append(Text.run("[Confirm]", confirm, ChatFormatting.YELLOW)));
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, ResetPart part) {
        for (ServerPlayer player : targets) {
            if (part == ResetPart.FRUIT || part == ResetPart.ALL) {
                FruitManager.removeFruit(player);
            }
            if (part == ResetPart.HAKI || part == ResetPart.ALL) {
                HakiManager.reset(player);
            }
            GLPlayer.edit(player, part.reset);
            if (part == ResetPart.RACE || part == ResetPart.ALL) {
                RaceManager.refresh(player);
            }
        }
        feedback(ctx.getSource(), targets, p -> Component.literal("Reset " + part.name + " data of " + p));
        return targets.size();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** One line per target for small groups, a summary for large ones. Broadcast to ops like vanilla. */
    private static void feedback(CommandSourceStack source, Collection<ServerPlayer> targets,
            Function<String, Component> message) {
        if (targets.size() <= 3) {
            for (ServerPlayer player : targets) {
                Component line = message.apply(player.getGameProfile().getName());
                source.sendSuccess(() -> line, true);
            }
        } else {
            Component line = message.apply(targets.size() + " players");
            source.sendSuccess(() -> line, true);
        }
    }
}
