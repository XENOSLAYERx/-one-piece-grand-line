package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.StyleManager;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.weapon.GLWeapons;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ability management and creator testing commands:
 * {@code ability ...}, {@code unlock ...}, {@code cooldown reset}, {@code stamina infinite}, {@code health infinite},
 * {@code invincible}, {@code test}.
 */
final class AbilityCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece ability list", "Your abilities and slots", PermissionTier.PLAYER),
            new HelpEntry("/onepiece ability equip <slot> <ability> [players]", "Put an ability in slot 1-5", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ability unlock <ability> [players]", "Unlock one ability", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ability lock <ability> [players]", "Remove an unlocked ability", PermissionTier.CREATOR),
            new HelpEntry("/onepiece ability use <ability> [players]", "Trigger an ability (testing)", PermissionTier.CREATOR),
            new HelpEntry("/onepiece unlock all [players]", "Unlock everything that exists", PermissionTier.CREATOR),
            new HelpEntry("/onepiece unlock abilities [players]", "Unlock every ability", PermissionTier.CREATOR),
            new HelpEntry("/onepiece cooldown reset [players]", "Clear all cooldowns", PermissionTier.CREATOR),
            new HelpEntry("/onepiece cooldown off|on [players]", "Disable or restore cooldowns", PermissionTier.CREATOR),
            new HelpEntry("/onepiece stamina infinite|normal [players]", "Toggle infinite stamina", PermissionTier.CREATOR),
            new HelpEntry("/onepiece health infinite|normal [players]", "Never die (damage still lands)", PermissionTier.CREATOR),
            new HelpEntry("/onepiece invincible on|off [players]", "Ignore all damage", PermissionTier.CREATOR),
            new HelpEntry("/onepiece test", "Show test toggles", PermissionTier.CREATOR));

    private static final DynamicCommandExceptionType NOT_USABLE = new DynamicCommandExceptionType(
            name -> Component.literal("That player can't use " + name + " (unlock it first)"));

    @FunctionalInterface
    private interface TargetedCommand {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private AbilityCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("ability")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(creator("equip").then(Commands.argument("slot", IntegerArgumentType.integer(1, AbilityState.SLOT_COUNT))
                        .then(withTargets(abilityArgument(), AbilityCommands::equip))))
                .then(creator("unlock").then(withTargets(abilityArgument(), (ctx, targets) -> setUnlocked(ctx, targets, true))))
                .then(creator("lock").then(withTargets(abilityArgument(), (ctx, targets) -> setUnlocked(ctx, targets, false))))
                .then(creator("use").then(withTargets(abilityArgument(), AbilityCommands::use))));

        root.then(creator("unlock")
                .then(withTargets(Commands.literal("all"), AbilityCommands::unlockAll))
                .then(withTargets(Commands.literal("abilities"), AbilityCommands::unlockAll)));

        root.then(creator("cooldown")
                .then(withTargets(Commands.literal("reset"), AbilityCommands::resetCooldowns))
                .then(withTargets(Commands.literal("off"), (ctx, t) -> flag(ctx, t, TestFlag.NO_COOLDOWNS, true)))
                .then(withTargets(Commands.literal("on"), (ctx, t) -> flag(ctx, t, TestFlag.NO_COOLDOWNS, false))));
        root.then(creator("stamina")
                .then(withTargets(Commands.literal("infinite"), (ctx, t) -> flag(ctx, t, TestFlag.INFINITE_STAMINA, true)))
                .then(withTargets(Commands.literal("normal"), (ctx, t) -> flag(ctx, t, TestFlag.INFINITE_STAMINA, false))));
        root.then(creator("health")
                .then(withTargets(Commands.literal("infinite"), (ctx, t) -> flag(ctx, t, TestFlag.INFINITE_HEALTH, true)))
                .then(withTargets(Commands.literal("normal"), (ctx, t) -> flag(ctx, t, TestFlag.INFINITE_HEALTH, false))));
        root.then(creator("invincible")
                .then(withTargets(Commands.literal("on"), (ctx, t) -> flag(ctx, t, TestFlag.INVINCIBLE, true)))
                .then(withTargets(Commands.literal("off"), (ctx, t) -> flag(ctx, t, TestFlag.INVINCIBLE, false))));
        root.then(creator("test").executes(ctx -> testStatus(ctx.getSource(), ctx.getSource().getPlayerOrException())));
    }

    // ---------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> abilityArgument() {
        return Commands.argument("ability", ResourceLocationArgument.id())
                .suggests(RegistryArguments.suggest(DefinitionType.ABILITY));
    }

    private static <B extends ArgumentBuilder<CommandSourceStack, B>> B withTargets(B node, TargetedCommand command) {
        return node
                .executes(ctx -> command.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> command.run(ctx, EntityArgument.getPlayers(ctx, "players"))));
    }

    private static Ability ability(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return RegistryArguments.get(ctx, "ability", DefinitionType.ABILITY).getValue();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static int list(CommandSourceStack source, ServerPlayer player) {
        AbilityState state = AbilityState.of(player);
        Text.send(source, Text.header("Abilities"));
        for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
            ResourceLocation id = state.slot(i);
            Ability ability = id == null ? null : GLAbilities.REGISTRY.get(id);
            Text.send(source, Text.line("Slot " + (i + 1), ability == null
                    ? Component.literal("empty").withStyle(ChatFormatting.DARK_GRAY)
                    : ability.displayName().copy().withStyle(ChatFormatting.WHITE)));
        }
        List<Ability> usable = AbilityAccess.usable(player);
        MutableComponent names = Component.empty();
        for (int i = 0; i < usable.size(); i++) {
            if (i > 0) {
                names.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            names.append(usable.get(i).displayName().copy().withStyle(ChatFormatting.AQUA));
        }
        Text.send(source, Text.line("Usable (" + usable.size() + ")", usable.isEmpty()
                ? Component.literal("none yet").withStyle(ChatFormatting.DARK_GRAY) : names));
        Text.send(source, Text.line("Stamina", String.format(Locale.ROOT, "%.0f / %.0f",
                state.stamina(AbilityManager.now(player)), state.maxStamina())));
        return usable.size();
    }

    private static int equip(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        Ability ability = ability(ctx);
        int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
        for (ServerPlayer player : targets) {
            if (!AbilityAccess.canUse(player, ability)) {
                throw NOT_USABLE.create(ability.displayName().getString());
            }
        }
        for (ServerPlayer player : targets) {
            AbilityManager.setSlot(player, slot, ability.id());
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Equipped ").append(ability.displayName())
                .append(" in slot " + (slot + 1) + " for " + name));
        return targets.size();
    }

    private static int setUnlocked(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean unlock)
            throws CommandSyntaxException {
        Ability ability = ability(ctx);
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> {
                if (unlock) {
                    data.unlock(UnlockCategory.ABILITY, ability.id());
                } else {
                    data.lock(UnlockCategory.ABILITY, ability.id());
                }
            });
            if (unlock) {
                AbilityState.of(player).equipInFreeSlot(ability.id());
            } else if (!AbilityAccess.canUse(player, ability)) {
                AbilityManager.endAll(player, Ability.EndReason.INTERRUPTED, false);
                AbilityState state = AbilityState.of(player);
                for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
                    if (ability.id().equals(state.slot(i))) {
                        state.setSlot(i, null);
                    }
                }
            }
            AbilityManager.sync(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal(unlock ? "Unlocked " : "Locked ")
                .append(ability.displayName()).append(" for " + name));
        return targets.size();
    }

    private static int use(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        Ability ability = ability(ctx);
        int started = 0;
        for (ServerPlayer player : targets) {
            AbilityManager.Result result = AbilityManager.press(player, ability);
            if (result == AbilityManager.Result.STARTED || result == AbilityManager.Result.STOPPED) {
                started++;
            } else {
                Component line = Component.literal(player.getGameProfile().getName() + ": " + result.name().toLowerCase(Locale.ROOT))
                        .withStyle(ChatFormatting.RED);
                ctx.getSource().sendFailure(line);
            }
        }
        return started;
    }

    /** Unlocks every registered ability, fruit, fighting style and Haki. Weapons join this when their system arrives. */
    private static int unlockAll(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        // Registry iteration follows registration order, so slots fill predictably.
        Map<ResourceLocation, Ability> all = new java.util.LinkedHashMap<>();
        for (Ability ability : GLAbilities.REGISTRY) {
            all.put(ability.id(), ability);
        }
        for (ServerPlayer player : targets) {
            FruitCommands.unlockAllFruits(player);
            GLStyles.REGISTRY.forEach(style -> StyleManager.learn(player, style));
            GLPlayer.edit(player, data -> {
                all.keySet().forEach(id -> data.unlock(UnlockCategory.ABILITY, id));
                GLWeapons.weapons().values().forEach(item -> data.unlock(UnlockCategory.WEAPON, item.getId()));
                GLTransformations.REGISTRY.keySet().forEach(id -> data.unlock(UnlockCategory.TRANSFORMATION, id));
                GLFruits.REGISTRY.keySet().forEach(id -> data.unlock(UnlockCategory.AWAKENING, id));
            });
            if (FruitManager.hasFruit(player)) {
                FruitManager.setMastery(player, com.nerodev.grandline.data.PlayerData.MAX_MASTERY);
            }
            AbilityState state = AbilityState.of(player);
            all.values().stream().filter(ability -> AbilityAccess.canUse(player, ability))
                    .forEach(ability -> state.equipInFreeSlot(ability.id()));
            // After slots are filled in registry order, so Haki does not jump the queue.
            HakiCommands.unlockAllHaki(player);
            AbilityManager.sync(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Unlocked " + all.size() + " abilities for " + name));
        return targets.size();
    }

    private static int resetCooldowns(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AbilityState state = AbilityState.of(player);
            state.clearCooldowns();
            state.refillStamina(AbilityManager.now(player));
            AbilityManager.sync(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Reset cooldowns and stamina of " + name));
        return targets.size();
    }

    private static int flag(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, TestFlag flag, boolean on) {
        for (ServerPlayer player : targets) {
            AbilityState.of(player).setFlag(flag, on);
            AbilityManager.sync(player);
        }
        String label = flag.getSerializedName().replace('_', ' ');
        feedback(ctx.getSource(), targets, name -> Component.literal(label + " " + (on ? "enabled" : "disabled") + " for " + name));
        return targets.size();
    }

    private static int testStatus(CommandSourceStack source, ServerPlayer player) {
        AbilityState state = AbilityState.of(player);
        Text.send(source, Text.header("Test toggles"));
        toggleLine(source, "Infinite stamina", state.hasFlag(TestFlag.INFINITE_STAMINA), "stamina infinite", "stamina normal");
        toggleLine(source, "No cooldowns", state.hasFlag(TestFlag.NO_COOLDOWNS), "cooldown off", "cooldown on");
        toggleLine(source, "Infinite health", state.hasFlag(TestFlag.INFINITE_HEALTH), "health infinite", "health normal");
        toggleLine(source, "Invincible", state.hasFlag(TestFlag.INVINCIBLE), "invincible on", "invincible off");
        toggleLine(source, "Infinite Haki", state.hasFlag(TestFlag.INFINITE_HAKI), "haki infinite on", "haki infinite off");
        Text.send(source, Component.literal("  ")
                .append(Text.run("[Reset cooldowns]", "/onepiece cooldown reset", ChatFormatting.YELLOW))
                .append(" ")
                .append(Text.run("[Unlock all]", "/onepiece unlock all", ChatFormatting.LIGHT_PURPLE)));
        return 1;
    }

    private static void toggleLine(CommandSourceStack source, String label, boolean on, String enable, String disable) {
        Text.send(source, Component.literal("  " + label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Text.yesNo(on))
                .append(" ")
                .append(Text.run(on ? "[turn off]" : "[turn on]", "/onepiece " + (on ? disable : enable), ChatFormatting.AQUA)));
    }

    private static void feedback(CommandSourceStack source, Collection<ServerPlayer> targets, Function<String, Component> message) {
        String name = targets.size() == 1 ? targets.iterator().next().getGameProfile().getName() : targets.size() + " players";
        Component line = message.apply(name);
        source.sendSuccess(() -> line, true);
    }
}
