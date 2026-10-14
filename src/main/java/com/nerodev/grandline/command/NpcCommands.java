package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcDefinition;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.Reaction;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** {@code /onepiece npc} and {@code /onepiece reputation}: islanders and a player's standing with the factions. */
final class NpcCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece npc list", "Every kind of islander", PermissionTier.PLAYER),
            new HelpEntry("/onepiece npc spawn <npc> [count]", "Spawn islanders where you look; they live there", PermissionTier.CREATOR),
            new HelpEntry("/onepiece npc remove [radius]", "Remove the islanders around you", PermissionTier.CREATOR),
            new HelpEntry("/onepiece reputation [players]", "Show your standing with every faction", PermissionTier.PLAYER),
            new HelpEntry("/onepiece reputation set|add <faction> <amount> [players]", "Change standing with a faction or the townsfolk",
                    PermissionTier.CREATOR));

    /** Factions plus the townsfolk, for reputation commands. */
    private static final SuggestionProvider<CommandSourceStack> STANDINGS = (ctx, builder) -> {
        List<String> ids = new ArrayList<>();
        ctx.getSource().registryAccess().registryOrThrow(GLRegistries.FACTION).keySet().forEach(id -> ids.add(id.toString()));
        ids.add(Factions.CIVILIANS.toString());
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private NpcCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("npc")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(creator("spawn").then(Commands.argument("npc", ResourceLocationArgument.id())
                        .suggests(RegistryArguments.suggest(DefinitionType.NPC))
                        .executes(ctx -> spawn(ctx, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 16))
                                .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(creator("remove")
                        .executes(ctx -> remove(ctx.getSource(), 16))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));

        root.then(Commands.literal("reputation")
                .executes(ctx -> show(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .requires(source -> Permissions.has(source, PermissionTier.CREATOR))
                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))
                .then(creator("set").then(change(false)))
                .then(creator("add").then(change(true))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> change(boolean add) {
        return Commands.argument("faction", ResourceLocationArgument.id()).suggests(STANDINGS)
                .then(Commands.argument("amount", IntegerArgumentType.integer(-PlayerData.MAX_REPUTATION * 2, PlayerData.MAX_REPUTATION * 2))
                        .executes(ctx -> change(ctx, List.of(ctx.getSource().getPlayerOrException()), add))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> change(ctx, EntityArgument.getPlayers(ctx, "players"), add))));
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Islanders"));
        var registry = source.registryAccess().registryOrThrow(GLRegistries.NPC);
        List<Map.Entry<ResourceLocation, NpcDefinition>> npcs = registry.entrySet().stream()
                .map(e -> Map.entry(e.getKey().location(), e.getValue()))
                .sorted(Comparator.comparing((Map.Entry<ResourceLocation, NpcDefinition> e) -> e.getValue().faction()
                        .map(ResourceLocation::toString).orElse("~")).thenComparing(e -> e.getKey().toString()))
                .toList();
        for (Map.Entry<ResourceLocation, NpcDefinition> e : npcs) {
            NpcDefinition npc = e.getValue();
            Text.send(source, Component.literal("  ").append(npc.displayName().copy()
                            .withStyle(s -> s.withColor(Factions.color(source.registryAccess(), npc.faction()))))
                    .append(Component.literal(" " + e.getKey() + " - " + npc.role().getSerializedName()
                            + npc.faction().map(f -> ", " + f.getPath()).orElse("") + (npc.trades().isEmpty() ? "" : ", trades"))
                            .withStyle(ChatFormatting.GRAY)));
        }
        return npcs.size();
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Map.Entry<ResourceLocation, NpcDefinition> npc = RegistryArguments.get(ctx, "npc", DefinitionType.NPC);
        ServerLevel level = source.getLevel();
        Vec3 look = Vec3.directionFromRotation(0, source.getRotation().y);
        Vec3 centre = source.getPosition().add(look.scale(3));
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double angle = i * Mth.TWO_PI / count;
            double spread = count == 1 ? 0 : 1.5;
            BlockPos pos = BlockPos.containing(centre.x + Math.cos(angle) * spread, centre.y, centre.z + Math.sin(angle) * spread);
            if (NpcEntity.spawn(level, npc.getKey(), pos, source.getRotation().y + 180, MobSpawnType.COMMAND) != null) {
                spawned++;
            }
        }
        int total = spawned;
        source.sendSuccess(() -> Component.literal("Spawned " + total + " ").append(npc.getValue().displayName()), true);
        return spawned;
    }

    private static int remove(CommandSourceStack source, int radius) {
        List<NpcEntity> npcs = source.getLevel().getEntitiesOfClass(NpcEntity.class, new AABB(BlockPos.containing(source.getPosition()))
                .inflate(radius));
        npcs.forEach(NpcEntity::discard);
        source.sendSuccess(() -> Component.literal("Removed " + npcs.size() + " islanders"), true);
        return npcs.size();
    }

    private static int show(CommandSourceStack source, Collection<ServerPlayer> players) {
        var access = source.registryAccess();
        for (ServerPlayer player : players) {
            PlayerData data = GLPlayer.data(player);
            Text.send(source, Text.header("Standing of " + player.getGameProfile().getName()));
            List<ResourceLocation> ids = new ArrayList<>(access.registryOrThrow(GLRegistries.FACTION).keySet());
            ids.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation id : ids) {
                Reaction reaction = Factions.reaction(player, Optional.of(id));
                Text.send(source, Text.line(ProfileCommand.factionName(access, id).getString(), standing(data.reputation(id), reaction)));
            }
            Text.send(source, Text.line("Townsfolk", standing(data.reputation(Factions.CIVILIANS), Factions.reaction(player, Optional.empty()))));
        }
        return players.size();
    }

    private static MutableComponent standing(int value, Reaction reaction) {
        String word = switch (reaction) {
            case FRIENDLY -> "friendly";
            case NEUTRAL -> "neutral";
            case HOSTILE -> "hostile";
        };
        return Component.literal((value > 0 ? "+" : "") + value + " (" + word + ")").withStyle(reaction.color());
    }

    private static int change(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean add)
            throws CommandSyntaxException {
        ResourceLocation faction = ResourceLocationArgument.getId(ctx, "faction");
        if (!faction.equals(Factions.CIVILIANS) && !ctx.getSource().registryAccess().registryOrThrow(GLRegistries.FACTION).containsKey(faction)) {
            ctx.getSource().sendFailure(Component.literal("Unknown faction: " + faction));
            return 0;
        }
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        for (ServerPlayer player : players) {
            GLPlayer.edit(player, data -> {
                if (add) {
                    data.addReputation(faction, amount);
                } else {
                    data.setReputation(faction, amount);
                }
            });
        }
        ctx.getSource().sendSuccess(() -> Component.literal((add ? "Changed" : "Set") + " standing with " + faction + " for "
                + players.size() + " player(s)"), true);
        return players.size();
    }
}
