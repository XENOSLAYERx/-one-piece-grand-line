package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.arena.Arena;
import com.nerodev.grandline.arena.ArenaData;
import com.nerodev.grandline.arena.Arenas;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /onepiece arena}: marking out arenas and fighting round after round in them. */
final class ArenaCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece arena list", "Every arena and the fights under way", PermissionTier.PLAYER),
            new HelpEntry("/onepiece arena join <arena>|leave", "Step into an arena's fight, or out of it", PermissionTier.PLAYER),
            new HelpEntry("/onepiece arena create <name> [radius]", "Mark out an arena around you", PermissionTier.CREATOR),
            new HelpEntry("/onepiece arena reset|delete <arena>", "End an arena's fight, or remove the arena", PermissionTier.CREATOR),
            new HelpEntry("/onepiece arena next", "Send in the next round of your arena now", PermissionTier.CREATOR));

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_\\-]{2,24}");
    private static final SuggestionProvider<CommandSourceStack> ARENAS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            ArenaData.get(ctx.getSource().getServer()).all().stream().map(Arena::name), builder);

    private ArenaCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("arena")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("join").then(arena().executes(ctx -> join(ctx.getSource(), StringArgumentType.getString(ctx, "arena")))))
                .then(Commands.literal("leave").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (Arenas.of(player) == null) {
                        ctx.getSource().sendFailure(Component.literal("You aren't in an arena"));
                        return 0;
                    }
                    Arenas.leave(player);
                    return 1;
                }))
                .then(creator("create").then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"), 16))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(6, 64))
                                .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                                        IntegerArgumentType.getInteger(ctx, "radius"))))))
                .then(creator("reset").then(arena().executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "arena");
                    boolean ended = Arenas.reset(name);
                    ctx.getSource().sendSuccess(() -> Component.literal(ended ? "Ended the fight in " + name : "No fight under way in " + name), true);
                    return ended ? 1 : 0;
                })))
                .then(creator("delete").then(arena().executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "arena");
                    if (!Arenas.delete(ctx.getSource().getServer(), name)) {
                        ctx.getSource().sendFailure(Component.literal("No arena called " + name));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Removed the arena " + name), true);
                    return 1;
                })))
                .then(creator("next").executes(ctx -> {
                    Arenas.Session session = Arenas.of(ctx.getSource().getPlayerOrException());
                    if (session == null) {
                        ctx.getSource().sendFailure(Component.literal("You aren't in an arena"));
                        return 0;
                    }
                    Arenas.startRound(ctx.getSource().getServer(), session);
                    return 1;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> arena() {
        return Commands.argument("arena", StringArgumentType.word()).suggests(ARENAS);
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Arenas"));
        var arenas = ArenaData.get(source.getServer()).all();
        if (arenas.isEmpty()) {
            Text.send(source, Component.literal("  None yet. Creators mark them out with /onepiece arena create <name>.").withStyle(ChatFormatting.GRAY));
        }
        for (Arena arena : arenas) {
            Arenas.Session session = Arenas.session(arena.name());
            BlockPos at = arena.centre();
            String fight = session == null ? "" : " - round " + session.round() + ", " + session.players().size() + " fighting";
            Text.send(source, Component.literal("  " + arena.name()).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" at " + at.getX() + ", " + at.getY() + ", " + at.getZ() + ", radius " + arena.radius() + fight)
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("  ")).append(Text.run("[join]", "/onepiece arena join " + arena.name(), ChatFormatting.GREEN)));
        }
        return arenas.size();
    }

    private static int join(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Arena> arena = ArenaData.get(source.getServer()).byName(name);
        if (arena.isEmpty()) {
            source.sendFailure(Component.literal("No arena called " + name));
            return 0;
        }
        String refusal = Arenas.join(player, arena.get());
        if (refusal != null) {
            source.sendFailure(Component.literal(refusal));
            return 0;
        }
        return 1;
    }

    private static int create(CommandSourceStack source, String name, int radius) {
        if (!NAME.matcher(name).matches()) {
            source.sendFailure(Component.literal("Arena names are 2-24 letters, digits, - or _"));
            return 0;
        }
        if (!Arenas.create(source.getServer(), name, source.getLevel(), BlockPos.containing(source.getPosition()), radius)) {
            source.sendFailure(Component.literal("There is already an arena called " + name));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Marked out the arena " + name + ", " + radius + " blocks around you"), true);
        return 1;
    }
}
