package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.weapon.WeaponItem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/** {@code /onepiece weapon}: list the weapons and hand them out. */
final class WeaponCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece weapon list", "Every weapon, gun and Seastone tool", PermissionTier.PLAYER),
            new HelpEntry("/onepiece weapon give <weapon> [players]", "Give a weapon (named blades included)", PermissionTier.CREATOR));

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.literal("Unknown weapon: " + name));

    private WeaponCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("weapon")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("give").requires(source -> Permissions.has(source, PermissionTier.CREATOR))
                        .then(Commands.argument("weapon", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(GLWeapons.weapons().keySet(), builder))
                                .executes(ctx -> give(ctx, List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> give(ctx, EntityArgument.getPlayers(ctx, "players")))))));
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Component.literal("Weapons:").withStyle(ChatFormatting.GOLD));
        for (Map.Entry<String, DeferredItem<? extends Item>> entry : GLWeapons.weapons().entrySet()) {
            Item item = entry.getValue().get();
            Component line = Component.literal("  " + entry.getKey() + " ").withStyle(ChatFormatting.YELLOW)
                    .append(item.getDescription().copy().withStyle(ChatFormatting.WHITE));
            if (item instanceof WeaponItem weapon) {
                line = line.copy().append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)).append(weapon.grade().display());
            }
            Text.send(source, line);
        }
        return GLWeapons.weapons().size();
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "weapon");
        DeferredItem<? extends Item> weapon = GLWeapons.weapons().get(name);
        if (weapon == null) {
            throw UNKNOWN.create(name);
        }
        for (ServerPlayer player : targets) {
            ItemStack stack = new ItemStack(weapon.get());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        Component what = weapon.get().getDescription();
        ctx.getSource().sendSuccess(() -> Component.literal("Gave ").append(what).append(" to " + targets.size() + " player(s)"), true);
        return targets.size();
    }
}
