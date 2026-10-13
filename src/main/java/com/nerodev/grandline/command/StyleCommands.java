package com.nerodev.grandline.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.style.FightingStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.StyleManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** {@code /onepiece style}: see, equip, learn and forget fighting styles. */
final class StyleCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece style", "Your fighting style and the styles you know", PermissionTier.PLAYER),
            new HelpEntry("/onepiece style equip <style>", "Switch to a style you have learned", PermissionTier.PLAYER),
            new HelpEntry("/onepiece style give <style> [players]", "Teach and equip a style", PermissionTier.CREATOR),
            new HelpEntry("/onepiece style remove <style> [players]", "Forget a style", PermissionTier.CREATOR));

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown fighting style: " + id));
    private static final SuggestionProvider<CommandSourceStack> STYLES = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(GLStyles.REGISTRY.keySet(), builder);

    @FunctionalInterface
    private interface TargetedCommand {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private StyleCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("style")
                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("equip")
                        .then(Commands.argument("style", ResourceLocationArgument.id()).suggests(STYLES)
                                .executes(StyleCommands::equip)))
                .then(Commands.literal("give").requires(source -> Permissions.has(source, PermissionTier.CREATOR))
                        .then(withTargets(Commands.argument("style", ResourceLocationArgument.id()).suggests(STYLES), StyleCommands::give)))
                .then(Commands.literal("remove").requires(source -> Permissions.has(source, PermissionTier.CREATOR))
                        .then(withTargets(Commands.argument("style", ResourceLocationArgument.id()).suggests(STYLES), StyleCommands::remove))));
    }

    private static <B extends ArgumentBuilder<CommandSourceStack, B>> B withTargets(B node, TargetedCommand command) {
        return node
                .executes(ctx -> command.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> command.run(ctx, EntityArgument.getPlayers(ctx, "players"))));
    }

    private static FightingStyle style(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "style");
        FightingStyle style = GLStyles.REGISTRY.get(id);
        if (style == null) {
            throw UNKNOWN.create(id);
        }
        return style;
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        FightingStyle current = StyleManager.style(player);
        Text.send(source, Component.literal("Fighting style: ").withStyle(ChatFormatting.GOLD).append(current.displayName()));
        for (FightingStyle style : GLStyles.REGISTRY) {
            boolean known = StyleManager.knows(player, style);
            double mastery = GLPlayer.data(player).mastery(style.masteryKey());
            Component line = Component.literal(style == current ? "> " : "  ")
                    .append(style.displayName().copy().withStyle(known ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY))
                    .append(Component.literal(known ? String.format("  mastery %.0f", mastery) : "  not learned")
                            .withStyle(ChatFormatting.GRAY));
            Text.send(source, line);
            if (style == current) {
                for (Ability move : style.moves()) {
                    boolean usable = AbilityAccess.canUse(player, move);
                    String need = move.properties().masteryKey().isPresent() && !usable
                            ? String.format(" (mastery %.0f)", move.properties().requiredMastery()) : "";
                    Text.send(source, Component.literal("     ").append(move.displayName().copy()
                            .withStyle(usable ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)).append(need));
                }
            }
        }
        return 1;
    }

    private static int equip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FightingStyle style = style(ctx);
        Optional<Component> refused = StyleManager.equip(player, style);
        if (refused.isPresent()) {
            ctx.getSource().sendFailure(refused.get());
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Now fighting in ").append(style.displayName()).append(" style"), false);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        FightingStyle style = style(ctx);
        for (ServerPlayer player : targets) {
            StyleManager.learn(player, style);
            StyleManager.equip(player, style);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Taught ").append(style.displayName())
                .append(" to " + (targets.size() == 1 ? targets.iterator().next().getGameProfile().getName() : targets.size() + " players")), true);
        return targets.size();
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        FightingStyle style = style(ctx);
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> {
                data.lock(UnlockCategory.STYLE, style.id());
                if (data.style().filter(style.id()::equals).isPresent()) {
                    data.setStyle(Optional.empty());
                }
            });
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Removed ").append(style.displayName())
                .append(" from " + (targets.size() == 1 ? targets.iterator().next().getGameProfile().getName() : targets.size() + " players")), true);
        return targets.size();
    }
}
