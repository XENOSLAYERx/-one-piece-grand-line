package com.nerodev.grandline.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * {@code /onepiece debug ...}: inspection tools for testers and server staff.
 * <p>
 * Later milestones extend the overview with player state (fruit, mastery, Haki, cooldowns, resources...).
 */
final class DebugCommand {
    static final List<HelpEntry> HELP;

    static {
        List<HelpEntry> help = new ArrayList<>();
        help.add(new HelpEntry("/onepiece debug", "Mod, registry and permission overview", PermissionTier.MODERATOR));
        help.add(new HelpEntry("/onepiece debug permissions [players]", "Show permission tiers", PermissionTier.MODERATOR));
        help.add(new HelpEntry("/onepiece debug config", "Show the active server config", PermissionTier.ADMIN));
        for (DefinitionType<?> type : DefinitionType.ALL) {
            help.add(new HelpEntry("/onepiece debug " + type.commandName() + " [id]",
                    "List or inspect " + type.commandName() + " definitions", PermissionTier.MODERATOR));
        }
        HELP = List.copyOf(help);
    }

    private static final Dynamic2CommandExceptionType UNKNOWN_DEFINITION = new Dynamic2CommandExceptionType(
            (type, id) -> Component.literal("Unknown " + type + ": " + id));

    private DebugCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> debug = Commands.literal("debug")
                .requires(source -> Permissions.has(source, PermissionTier.MODERATOR))
                .executes(ctx -> overview(ctx.getSource()));

        debug.then(Commands.literal("config")
                .requires(source -> Permissions.has(source, PermissionTier.ADMIN))
                .executes(ctx -> config(ctx.getSource())));

        debug.then(Commands.literal("permissions")
                .executes(ctx -> permissionsOfSource(ctx.getSource()))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> permissionsOfPlayers(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))));

        for (DefinitionType<?> type : DefinitionType.ALL) {
            debug.then(definitionNode(type));
        }

        root.then(debug);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Overview
    // ---------------------------------------------------------------------------------------------------------

    private static int overview(CommandSourceStack source) {
        RegistryAccess access = source.registryAccess();
        String neoVersion = ModList.get().getModContainerById("neoforge")
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");

        Text.send(source, Text.header("Grand Line debug"));
        Text.send(source, Text.line("Version", GrandLine.version() + " (NeoForge " + neoVersion
                + ", Minecraft " + SharedConstants.getCurrentVersion().getName() + ")"));
        Text.send(source, Text.line("Server", source.getServer().isDedicatedServer() ? "dedicated" : "integrated"));
        Text.send(source, Text.line("Your tier", Permissions.tierOf(source).display()));
        Text.send(source, Text.line("Server config loaded", Text.yesNo(GLServerConfig.isLoaded())));

        MutableComponent registries = Component.empty();
        boolean first = true;
        for (DefinitionType<?> type : DefinitionType.ALL) {
            if (!first) {
                registries.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;
            int size = type.registry(access).size();
            registries.append(Text.run(type.pluralName() + " " + size,
                    "/" + OnePieceCommand.ROOT + " debug " + type.commandName(), ChatFormatting.WHITE));
        }
        Text.send(source, Text.line("Registries", registries));
        return 1;
    }

    private static int config(CommandSourceStack source) {
        Text.send(source, Text.header("Grand Line server config"));
        Text.send(source, Text.line("Loaded", Text.yesNo(GLServerConfig.isLoaded())));
        Text.send(source, Text.line("permissions.moderatorOpLevel", GLServerConfig.moderatorOpLevel()));
        Text.send(source, Text.line("permissions.adminOpLevel", GLServerConfig.adminOpLevel()));
        Text.send(source, Text.line("permissions.creatorOpLevel", GLServerConfig.creatorOpLevel()));
        Text.send(source, Text.line("permissions.singleplayerOwnerIsCreator",
                Text.yesNo(GLServerConfig.singleplayerOwnerIsCreator())));
        String uuids = GLServerConfig.creatorUuids().isEmpty() ? "(none)"
                : GLServerConfig.creatorUuids().stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
        Text.send(source, Text.line("permissions.creatorUuids", uuids));
        return 1;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Permissions
    // ---------------------------------------------------------------------------------------------------------

    private static int permissionsOfSource(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return permissionsOfPlayers(source, List.of(player));
        }
        Text.send(source, Text.header("Permissions"));
        Text.send(source, Component.literal("  " + source.getTextName() + ": ").withStyle(ChatFormatting.WHITE)
                .append(Permissions.tierOf(source).display())
                .append(Component.literal(" (op level " + opLevelOf(source) + ")").withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int permissionsOfPlayers(CommandSourceStack source, Collection<ServerPlayer> players) {
        Text.send(source, Text.header("Permissions"));
        for (ServerPlayer player : players) {
            Text.send(source, Component.literal("  " + player.getGameProfile().getName() + ": ").withStyle(ChatFormatting.WHITE)
                    .append(Permissions.tierOf(player).display())
                    .append(Component.literal(" (op level " + Permissions.opLevel(player)
                            + ", listed creator: ").withStyle(ChatFormatting.GRAY))
                    .append(Text.yesNo(Permissions.isListedCreator(player)))
                    .append(Component.literal(", singleplayer owner: ").withStyle(ChatFormatting.GRAY))
                    .append(Text.yesNo(Permissions.isSingleplayerOwner(player)))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
        }
        return players.size();
    }

    private static int opLevelOf(CommandSourceStack source) {
        for (int level = 4; level > 0; level--) {
            if (source.hasPermission(level)) {
                return level;
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Definition registries
    // ---------------------------------------------------------------------------------------------------------

    private static <T> LiteralArgumentBuilder<CommandSourceStack> definitionNode(DefinitionType<T> type) {
        return Commands.literal(type.commandName())
                .executes(ctx -> listDefinitions(ctx.getSource(), type))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(definitionIds(type))
                        .executes(ctx -> inspectDefinition(ctx.getSource(), type, ResourceLocationArgument.getId(ctx, "id"))));
    }

    /**
     * Suggests registry ids. Matches the typed text against the full id or just the path, so typing {@code gum}
     * suggests {@code grandline:gum_gum}. Runs on the server, so it always reflects the server's datapacks.
     */
    static <T> SuggestionProvider<CommandSourceStack> definitionIds(DefinitionType<T> type) {
        return (ctx, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (ResourceLocation id : type.sortedIds(ctx.getSource().registryAccess())) {
                if (id.toString().startsWith(typed) || id.getPath().startsWith(typed)) {
                    builder.suggest(id.toString());
                }
            }
            return builder.buildFuture();
        };
    }

    private static <T> int listDefinitions(CommandSourceStack source, DefinitionType<T> type) {
        RegistryAccess access = source.registryAccess();
        List<ResourceLocation> ids = type.sortedIds(access);
        Text.send(source, Text.header(capitalize(type.pluralName()) + " (" + ids.size() + ")"));
        for (ResourceLocation id : ids) {
            T definition = type.registry(access).get(id);
            Text.send(source, Component.literal("  ")
                    .append(Text.run(id.toString(), "/" + OnePieceCommand.ROOT + " debug " + type.commandName() + " " + id,
                            ChatFormatting.AQUA))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(type.name().apply(definition).copy().withStyle(ChatFormatting.WHITE)));
        }
        return ids.size();
    }

    private static <T> int inspectDefinition(CommandSourceStack source, DefinitionType<T> type, ResourceLocation id)
            throws CommandSyntaxException {
        Map.Entry<ResourceLocation, T> entry = type.find(source.registryAccess(), id)
                .orElseThrow(() -> UNKNOWN_DEFINITION.create(type.commandName(), id));
        T definition = entry.getValue();

        Text.send(source, Text.header(type.name().apply(definition)));
        Text.send(source, Text.line("Id", entry.getKey().toString()));
        for (Component line : type.details().apply(definition)) {
            Text.send(source, line);
        }
        return 1;
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
