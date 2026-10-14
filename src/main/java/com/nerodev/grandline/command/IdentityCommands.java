package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.data.DeathMode;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.definition.RaceDefinition;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Creator commands that edit a player's identity: race, faction, bounty, death mode, resets.
 * Every command defaults to the caller and optionally takes target players.
 */
final class IdentityCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece race set <race> [players]", "Change race instantly", PermissionTier.CREATOR),
            new HelpEntry("/onepiece faction set <faction> [players]", "Join a faction", PermissionTier.CREATOR),
            new HelpEntry("/onepiece faction clear [players]", "Leave your faction", PermissionTier.CREATOR),
            new HelpEntry("/onepiece bounty set <amount> [players]", "Set bounty", PermissionTier.CREATOR),
            new HelpEntry("/onepiece bounty add <amount> [players]", "Add to (or subtract from) bounty", PermissionTier.CREATOR),
            new HelpEntry("/onepiece bounty reset [players]", "Clear bounty", PermissionTier.CREATOR),
            new HelpEntry("/onepiece deathmode <normal|creator> [players]", "Creator mode keeps items and XP on death", PermissionTier.CREATOR),
            new HelpEntry("/onepiece reset <part> [confirm] [players]", "Reset race, fruit, haki, faction, bounty, mastery or all", PermissionTier.CREATOR));

    /** What {@code /onepiece reset} can reset. */
    private enum ResetPart {
        RACE("race", data -> data.setRace(PlayerData.DEFAULT_RACE)),
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
        root.then(creator("race")
                .then(Commands.literal("set")
                        .then(withTargets(Commands.argument("race", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.RACE)), IdentityCommands::setRace))));

        root.then(creator("faction")
                .then(Commands.literal("set")
                        .then(withTargets(Commands.argument("faction", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.FACTION)), IdentityCommands::setFaction)))
                .then(withTargets(Commands.literal("clear"), IdentityCommands::clearFaction)));

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
