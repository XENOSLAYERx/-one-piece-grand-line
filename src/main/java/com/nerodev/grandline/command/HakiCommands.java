package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.haki.HakiAbilities;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** {@code /onepiece haki ...}, plus the Haki parts of {@code unlock} and {@code mastery}. */
final class HakiCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece haki info [player]", "Your Haki, levels and training progress", PermissionTier.PLAYER),
            new HelpEntry("/onepiece haki unlock <all|type> [players]", "Awaken Haki instantly", PermissionTier.CREATOR),
            new HelpEntry("/onepiece haki set <type> <0-100> [players]", "Awaken a Haki at a level", PermissionTier.CREATOR),
            new HelpEntry("/onepiece haki remove <all|type> [players]", "Take Haki away (levels are kept)", PermissionTier.CREATOR),
            new HelpEntry("/onepiece haki infinite [on|off] [players]", "Haki costs nothing and has no cooldown", PermissionTier.CREATOR),
            new HelpEntry("/onepiece unlock haki [players]", "Awaken all three Haki", PermissionTier.CREATOR),
            new HelpEntry("/onepiece mastery set <haki type> <0-100> [players]", "Set a Haki level", PermissionTier.CREATOR));

    @FunctionalInterface
    private interface TargetedCommand {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private HakiCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> haki = Commands.literal("haki")
                .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()));
        haki.then(Commands.literal("info")
                .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> Permissions.has(source, PermissionTier.MODERATOR))
                        .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

        LiteralArgumentBuilder<CommandSourceStack> unlock = creator("unlock")
                .then(withTargets(Commands.literal("all"), (ctx, targets) -> unlock(ctx, targets, HakiType.values())));
        LiteralArgumentBuilder<CommandSourceStack> set = creator("set");
        LiteralArgumentBuilder<CommandSourceStack> remove = creator("remove")
                .then(withTargets(Commands.literal("all"), (ctx, targets) -> remove(ctx, targets, HakiType.values())));
        for (HakiType type : HakiType.values()) {
            String name = type.getSerializedName();
            unlock.then(withTargets(Commands.literal(name), (ctx, targets) -> unlock(ctx, targets, type)));
            set.then(Commands.literal(name).then(withTargets(levelArgument(),
                    (ctx, targets) -> setLevel(ctx, targets, type, DoubleArgumentType.getDouble(ctx, "level")))));
            remove.then(withTargets(Commands.literal(name), (ctx, targets) -> remove(ctx, targets, type)));
        }
        haki.then(unlock).then(set).then(remove);
        haki.then(withTargets(creator("infinite"), (ctx, targets) -> infinite(ctx, targets, true))
                .then(withTargets(Commands.literal("on"), (ctx, targets) -> infinite(ctx, targets, true)))
                .then(withTargets(Commands.literal("off"), (ctx, targets) -> infinite(ctx, targets, false))));
        root.then(haki);

        // /onepiece unlock haki, /onepiece mastery set <type> <level>: merged into the nodes other classes build.
        root.then(creator("unlock").then(withTargets(Commands.literal("haki"),
                (ctx, targets) -> unlock(ctx, targets, HakiType.values()))));
        LiteralArgumentBuilder<CommandSourceStack> masterySet = Commands.literal("set");
        for (HakiType type : HakiType.values()) {
            masterySet.then(Commands.literal(type.getSerializedName()).then(withTargets(levelArgument(),
                    (ctx, targets) -> setLevel(ctx, targets, type, DoubleArgumentType.getDouble(ctx, "level")))));
        }
        root.then(creator("mastery").then(masterySet));
    }

    // ---------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Double> levelArgument() {
        return Commands.argument("level", DoubleArgumentType.doubleArg(0, PlayerData.MAX_MASTERY));
    }

    private static <B extends ArgumentBuilder<CommandSourceStack, B>> B withTargets(B node, TargetedCommand command) {
        return node
                .executes(ctx -> command.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> command.run(ctx, EntityArgument.getPlayers(ctx, "players"))));
    }

    private static void feedback(CommandSourceStack source, Collection<ServerPlayer> targets, Function<String, Component> message) {
        String name = targets.size() == 1 ? targets.iterator().next().getGameProfile().getName() : targets.size() + " players";
        Component line = message.apply(name);
        source.sendSuccess(() -> line, true);
    }

    private static String names(HakiType... types) {
        if (types.length == HakiType.values().length) {
            return "all Haki";
        }
        StringBuilder out = new StringBuilder();
        for (HakiType type : types) {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append(type.displayName().getString());
        }
        return out.toString();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static int info(CommandSourceStack source, ServerPlayer player) {
        PlayerData data = GLPlayer.data(player);
        Text.send(source, Text.header(Component.literal("Haki of " + player.getGameProfile().getName())));
        for (HakiType type : HakiType.values()) {
            MutableComponent line = Component.literal("  ").append(type.displayName().copy()
                    .withStyle(style -> style.withColor(type.color()))).append(": ");
            if (HakiManager.has(player, type)) {
                line.append(Component.literal(String.format(Locale.ROOT, "level %.0f", HakiManager.level(player, type)))
                        .withStyle(ChatFormatting.WHITE));
                Text.send(source, line);
                for (Ability ability : HakiAbilities.of(type)) {
                    boolean usable = AbilityAccess.canUse(player, ability);
                    double required = ability.properties().requiredMastery();
                    Text.send(source, Component.literal("    - ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(ability.displayName().copy().withStyle(usable ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY))
                            .append(Component.literal(usable || required <= 0 ? ""
                                    : String.format(Locale.ROOT, " (level %.0f)", required)).withStyle(ChatFormatting.GRAY)));
                }
                continue;
            }
            String progress = switch (type) {
                case ARMAMENT -> GLServerConfig.hakiTraining()
                        ? "not awakened - " + data.counter(HakiManager.ARMAMENT_HITS) + "/" + GLServerConfig.armamentTrainingHits()
                                + " blows landed in training"
                        : "not awakened";
                case OBSERVATION -> GLServerConfig.hakiTraining()
                        ? "not awakened - " + data.counter(HakiManager.OBSERVATION_HITS) + "/" + GLServerConfig.observationTrainingHits()
                                + " blows taken in training"
                        : "not awakened";
                case CONQUERORS -> "not born with it";
            };
            Text.send(source, line.append(Component.literal(progress).withStyle(ChatFormatting.DARK_GRAY)));
        }
        if (AbilityState.of(player).hasFlag(TestFlag.INFINITE_HAKI)) {
            Text.send(source, Component.literal("  Infinite Haki is on (test flag)").withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    private static int unlock(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, HakiType... types) {
        for (ServerPlayer player : targets) {
            for (HakiType type : types) {
                HakiManager.unlock(player, type, false);
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Awakened " + names(types) + " for " + name));
        return targets.size();
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, HakiType type, double level) {
        for (ServerPlayer player : targets) {
            HakiManager.setLevel(player, type, level);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal(String.format(Locale.ROOT, "%s of %s set to level %.0f",
                type.displayName().getString(), name, level)));
        return targets.size();
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, HakiType... types) {
        int removed = 0;
        for (ServerPlayer player : targets) {
            for (HakiType type : types) {
                if (HakiManager.remove(player, type)) {
                    removed++;
                }
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Removed " + names(types) + " from " + name));
        return removed;
    }

    private static int infinite(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean on) {
        for (ServerPlayer player : targets) {
            AbilityState.of(player).setFlag(TestFlag.INFINITE_HAKI, on);
            AbilityManager.sync(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Infinite Haki " + (on ? "enabled" : "disabled") + " for " + name));
        return targets.size();
    }

    /** Every Haki at full level (used by {@code /onepiece unlock all}). */
    static void unlockAllHaki(ServerPlayer player) {
        for (HakiType type : HakiType.values()) {
            HakiManager.setLevel(player, type, PlayerData.MAX_MASTERY);
        }
    }

    /** Maxes the level of every Haki the player has (used by {@code /onepiece mastery max}). @return how many */
    static int maxUnlockedHaki(ServerPlayer player) {
        int count = 0;
        for (HakiType type : HakiType.values()) {
            if (HakiManager.has(player, type)) {
                HakiManager.setLevel(player, type, PlayerData.MAX_MASTERY);
                count++;
            }
        }
        return count;
    }
}
