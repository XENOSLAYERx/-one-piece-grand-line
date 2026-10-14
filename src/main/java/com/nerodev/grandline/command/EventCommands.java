package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.worldevent.WorldEventType;
import com.nerodev.grandline.worldevent.WorldEvents;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /onepiece event}: world events under way, and starting or stopping them. */
final class EventCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece event list", "Every kind of world event, and those under way", PermissionTier.PLAYER),
            new HelpEntry("/onepiece event start <event> [players]", "Set a raid on players", PermissionTier.CREATOR),
            new HelpEntry("/onepiece event stop", "End every event; the raiders leave", PermissionTier.CREATOR));

    private EventCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("event")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(creator("start").then(Commands.argument("event", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(WorldEventType.values())
                                .map(WorldEventType::getSerializedName), builder))
                        .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "event"),
                                List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "event"),
                                        EntityArgument.getPlayers(ctx, "players"))))))
                .then(creator("stop").executes(ctx -> {
                    int stopped = WorldEvents.stopAll();
                    ctx.getSource().sendSuccess(() -> Component.literal("Stopped " + stopped + " event(s)"), true);
                    return stopped;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("World events"));
        for (WorldEventType type : WorldEventType.values()) {
            Text.send(source, Component.literal("  ").append(Component.literal(type.title()).withStyle(type.color()))
                    .append(Component.literal(" (" + type.getSerializedName() + "): " + (type.boss() != null ? "a Sea King, at sea"
                            : type.band().size() + " raiders") + ", " + type.emeralds()
                            + " emeralds each for beating them").withStyle(ChatFormatting.GRAY)));
        }
        List<WorldEvents.Active> active = WorldEvents.active();
        Text.send(source, Component.literal("  Under way: " + (active.isEmpty() ? "none" : "")).withStyle(ChatFormatting.YELLOW));
        for (WorldEvents.Active event : active) {
            Text.send(source, Component.literal("    " + event.type().title() + ", " + event.raiders().size() + " of " + event.total()
                    + " raiders left").withStyle(ChatFormatting.WHITE));
        }
        return active.size();
    }

    private static int start(CommandSourceStack source, String name, Collection<ServerPlayer> players) {
        WorldEventType type = WorldEventType.byName(name);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown event: " + name));
            return 0;
        }
        int started = 0;
        for (ServerPlayer player : players) {
            if (WorldEvents.start(player, type) != null) {
                started++;
            }
        }
        if (started == 0) {
            source.sendFailure(Component.literal("The raiders found no ground to land on (they need dry land 20-30 blocks away)"));
            return 0;
        }
        int total = started;
        source.sendSuccess(() -> Component.literal("Started " + type.title() + " on " + total + " player(s)"), true);
        return started;
    }
}
