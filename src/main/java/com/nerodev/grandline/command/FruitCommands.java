package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Devil Fruit and mastery commands. */
final class FruitCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece fruit list", "Every Devil Fruit in the game", PermissionTier.PLAYER),
            new HelpEntry("/onepiece fruit info [fruit]", "Abilities, type and lore of a fruit", PermissionTier.PLAYER),
            new HelpEntry("/onepiece fruit give <fruit> [players]", "Give the fruit item to eat", PermissionTier.CREATOR),
            new HelpEntry("/onepiece fruit set <fruit> [players]", "Grant a fruit's power instantly", PermissionTier.CREATOR),
            new HelpEntry("/onepiece fruit remove [players]", "Remove a fruit's power", PermissionTier.CREATOR),
            new HelpEntry("/onepiece fruit awaken [players]", "Awaken the current fruit", PermissionTier.CREATOR),
            new HelpEntry("/onepiece mastery set fruit <0-100> [players]", "Set current fruit mastery", PermissionTier.CREATOR),
            new HelpEntry("/onepiece mastery max [players]", "Max fruit and Haki mastery (awakens the fruit)", PermissionTier.CREATOR));

    private static final SimpleCommandExceptionType NO_FRUIT = new SimpleCommandExceptionType(
            Component.literal("That player has no Devil Fruit"));
    private static final SimpleCommandExceptionType NOTHING_TO_MAX = new SimpleCommandExceptionType(
            Component.literal("That player has no Devil Fruit or Haki to master"));

    @FunctionalInterface
    private interface TargetedCommand {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private FruitCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("fruit")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), currentFruit(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("fruit", ResourceLocationArgument.id())
                                .suggests(RegistryArguments.suggest(DefinitionType.FRUIT))
                                .executes(ctx -> info(ctx.getSource(), fruit(ctx)))))
                .then(creator("give").then(withTargets(fruitArgument(), FruitCommands::give)))
                .then(creator("set").then(withTargets(fruitArgument(), FruitCommands::set)))
                .then(withTargets(creator("remove"), FruitCommands::remove))
                .then(withTargets(creator("awaken"), FruitCommands::awaken)));

        root.then(creator("mastery")
                .then(Commands.literal("set").then(Commands.literal("fruit")
                        .then(withTargets(Commands.argument("level", DoubleArgumentType.doubleArg(0, PlayerData.MAX_MASTERY)),
                                (ctx, targets) -> setMastery(ctx, targets, DoubleArgumentType.getDouble(ctx, "level"))))))
                .then(withTargets(Commands.literal("max"), FruitCommands::maxMastery)));
    }

    // ---------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, net.minecraft.resources.ResourceLocation> fruitArgument() {
        return Commands.argument("fruit", ResourceLocationArgument.id()).suggests(RegistryArguments.suggest(DefinitionType.FRUIT));
    }

    private static <B extends ArgumentBuilder<CommandSourceStack, B>> B withTargets(B node, TargetedCommand command) {
        return node
                .executes(ctx -> command.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> command.run(ctx, EntityArgument.getPlayers(ctx, "players"))));
    }

    private static DevilFruit fruit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return RegistryArguments.get(ctx, "fruit", DefinitionType.FRUIT).getValue();
    }

    private static DevilFruit currentFruit(ServerPlayer player) throws CommandSyntaxException {
        DevilFruit fruit = FruitManager.fruitOf(player);
        if (fruit == null) {
            throw NO_FRUIT.create();
        }
        return fruit;
    }

    private static void feedback(CommandSourceStack source, Collection<ServerPlayer> targets, Function<String, Component> message) {
        String name = targets.size() == 1 ? targets.iterator().next().getGameProfile().getName() : targets.size() + " players";
        Component line = message.apply(name);
        source.sendSuccess(() -> line, true);
    }

    // ---------------------------------------------------------------------------------------------------------

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Devil Fruits (" + GLFruits.REGISTRY.size() + ")"));
        for (DevilFruit fruit : GLFruits.REGISTRY) {
            Text.send(source, Component.literal("  ")
                    .append(Text.run(fruit.displayName().getString(), "/onepiece fruit info " + fruit.id(), ChatFormatting.AQUA))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(fruit.type().display())
                    .append(Component.literal(" ").append(fruit.rarity().display())));
        }
        return GLFruits.REGISTRY.size();
    }

    private static int info(CommandSourceStack source, DevilFruit fruit) {
        Text.send(source, Text.header(fruit.displayName()));
        Text.send(source, Text.line("Name", Component.literal(fruit.originalName()).withStyle(ChatFormatting.ITALIC)));
        Text.send(source, Text.line("Type", fruit.type().display()));
        Text.send(source, Text.line("Rarity", fruit.rarity().display()));
        Text.send(source, Text.line("About", fruit.description()));
        for (Ability ability : fruit.abilities()) {
            Ability.Properties props = ability.properties();
            String requirement = props.requiredMastery() > 0
                    ? String.format(Locale.ROOT, " (mastery %.0f)", props.requiredMastery()) : "";
            Text.send(source, Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(ability.displayName().copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(requirement).withStyle(ChatFormatting.GRAY)));
        }
        for (Ability ability : fruit.awakenedAbilities()) {
            Text.send(source, Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(ability.displayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal(" (awakened)").withStyle(ChatFormatting.GRAY)));
        }
        Text.send(source, Component.literal("  Weakness: sea water and Seastone drain all Devil Fruit power.")
                .withStyle(ChatFormatting.DARK_RED));
        if (Permissions.has(source, PermissionTier.CREATOR)) {
            Text.send(source, Component.literal("  ")
                    .append(Text.run("[Give fruit]", "/onepiece fruit give " + fruit.id(), ChatFormatting.YELLOW))
                    .append(" ")
                    .append(Text.run("[Gain power now]", "/onepiece fruit set " + fruit.id(), ChatFormatting.LIGHT_PURPLE)));
        }
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        DevilFruit fruit = fruit(ctx);
        Item item = GLItems.itemFor(fruit.id());
        if (item == null) {
            return 0;
        }
        for (ServerPlayer player : targets) {
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Gave ").append(fruit.displayName()).append(" to " + name));
        return targets.size();
    }

    private static int set(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        DevilFruit fruit = fruit(ctx);
        for (ServerPlayer player : targets) {
            FruitManager.setFruit(player, fruit);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal(name + " now has the power of the ").append(fruit.displayName()));
        return targets.size();
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        int removed = 0;
        for (ServerPlayer player : targets) {
            if (FruitManager.removeFruit(player)) {
                removed++;
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Removed the Devil Fruit power of " + name));
        return removed;
    }

    private static int awaken(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        for (ServerPlayer player : targets) {
            currentFruit(player);
        }
        for (ServerPlayer player : targets) {
            FruitManager.setMastery(player, PlayerData.MAX_MASTERY);
            FruitManager.awaken(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Awakened the Devil Fruit of " + name));
        return targets.size();
    }

    private static int setMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, double level)
            throws CommandSyntaxException {
        for (ServerPlayer player : targets) {
            currentFruit(player);
        }
        for (ServerPlayer player : targets) {
            FruitManager.setMastery(player, level);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal(String.format(Locale.ROOT, "Fruit mastery of %s set to %.0f", name, level)));
        return targets.size();
    }

    /** Maxes the fruit (awakening it) and every Haki the player has. */
    private static int maxMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        int maxed = 0;
        for (ServerPlayer player : targets) {
            int before = maxed;
            if (FruitManager.hasFruit(player)) {
                FruitManager.setMastery(player, PlayerData.MAX_MASTERY);
                maxed++;
            }
            maxed += HakiCommands.maxUnlockedHaki(player);
            if (maxed == before && targets.size() == 1) {
                throw NOTHING_TO_MAX.create();
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Maxed fruit and Haki mastery of " + name));
        return maxed;
    }

    /** Unlocks every fruit for the player (used by /onepiece unlock all). */
    static void unlockAllFruits(ServerPlayer player) {
        GLPlayer.edit(player, data -> {
            for (DevilFruit fruit : GLFruits.REGISTRY) {
                data.unlock(com.nerodev.grandline.data.UnlockCategory.FRUIT, fruit.id());
                data.unlock(com.nerodev.grandline.data.UnlockCategory.AWAKENING, fruit.id());
            }
        });
    }
}
