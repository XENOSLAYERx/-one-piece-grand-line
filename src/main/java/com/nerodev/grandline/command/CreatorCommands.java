package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nerodev.grandline.creator.Battles;
import com.nerodev.grandline.creator.Builds;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.Loadout;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.network.OpenCreatorMenuPayload;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.style.FightingStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.StyleManager;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import com.nerodev.grandline.weapon.GLWeapons;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Creator workflow commands: the Creator Menu, loadouts and random builds, battle tests, forcing a transformation,
 * and unlocking whole categories at once. All need the CREATOR tier.
 */
final class CreatorCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece menu", "Open the Creator Menu", PermissionTier.CREATOR),
            new HelpEntry("/onepiece loadout list", "Your saved builds", PermissionTier.CREATOR),
            new HelpEntry("/onepiece loadout save|load|delete <name>", "Save, restore or forget a build", PermissionTier.CREATOR),
            new HelpEntry("/onepiece random build [players]", "Roll a random race, fruit, style, Haki and weapon", PermissionTier.CREATOR),
            new HelpEntry("/onepiece battle random <difficulty>", "Spawn a wave of enemies around you", PermissionTier.CREATOR),
            new HelpEntry("/onepiece battle spawn <entity> [count]", "Spawn enemies of one kind around you", PermissionTier.CREATOR),
            new HelpEntry("/onepiece battle clear", "Remove every battle-test enemy nearby", PermissionTier.CREATOR),
            new HelpEntry("/onepiece transform <form>|off [players]", "Force a body transformation on or off", PermissionTier.CREATOR),
            new HelpEntry("/onepiece unlock <fruits|styles|weapons|transformations|awakenings> [players]",
                    "Unlock one whole category", PermissionTier.CREATOR),
            new HelpEntry("/onepiece mastery set style <style> <0-100> [players]", "Set a fighting style's mastery",
                    PermissionTier.CREATOR),
            new HelpEntry("/onepiece mastery set race <0-100> [players]", "Set your race's mastery", PermissionTier.CREATOR));

    private static final DynamicCommandExceptionType NO_LOADOUT = new DynamicCommandExceptionType(
            name -> Component.literal("No loadout named " + name));
    private static final DynamicCommandExceptionType BAD_NAME = new DynamicCommandExceptionType(
            name -> Component.literal("Loadout names use a-z, 0-9, _ and - (up to 24): " + name));
    private static final DynamicCommandExceptionType UNKNOWN_FORM = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown transformation: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_STYLE = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown fighting style: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_ENTITY = new DynamicCommandExceptionType(
            id -> Component.literal("Not a creature that can fight: " + id));

    private static final SuggestionProvider<CommandSourceStack> LOADOUTS = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        return SharedSuggestionProvider.suggest(player == null ? List.of() : GLPlayer.data(player).loadouts().keySet(), builder);
    };
    private static final SuggestionProvider<CommandSourceStack> FORMS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(GLTransformations.REGISTRY.keySet(), builder);

    @FunctionalInterface
    private interface TargetedCommand {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private CreatorCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(creator("menu").executes(ctx -> openMenu(ctx.getSource().getPlayerOrException())));

        root.then(creator("loadout")
                .then(Commands.literal("list").executes(ctx -> listLoadouts(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("save").then(Commands.argument("name", StringArgumentType.word()).suggests(LOADOUTS)
                        .executes(CreatorCommands::saveLoadout)))
                .then(Commands.literal("load").then(Commands.argument("name", StringArgumentType.word()).suggests(LOADOUTS)
                        .executes(CreatorCommands::loadLoadout)))
                .then(Commands.literal("delete").then(Commands.argument("name", StringArgumentType.word()).suggests(LOADOUTS)
                        .executes(CreatorCommands::deleteLoadout))));

        root.then(creator("random").then(withTargets(Commands.literal("build"), CreatorCommands::randomBuild)));

        LiteralArgumentBuilder<CommandSourceStack> battle = creator("battle");
        LiteralArgumentBuilder<CommandSourceStack> random = Commands.literal("random");
        for (Battles.Difficulty difficulty : Battles.Difficulty.values()) {
            random.then(Commands.literal(difficulty.getSerializedName())
                    .executes(ctx -> battleRandom(ctx.getSource(), ctx.getSource().getPlayerOrException(), difficulty)));
        }
        battle.then(random);
        battle.then(Commands.literal("spawn").then(Commands.argument("entity", ResourceLocationArgument.id())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(Battles.fighters(), builder))
                .executes(ctx -> battleSpawn(ctx, 1))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, Battles.MAX_SPAWN))
                        .executes(ctx -> battleSpawn(ctx, IntegerArgumentType.getInteger(ctx, "count"))))));
        battle.then(Commands.literal("clear").executes(ctx -> battleClear(ctx.getSource(), ctx.getSource().getPlayerOrException())));
        root.then(battle);

        root.then(creator("transform")
                .then(withTargets(Commands.literal("off"), CreatorCommands::transformOff))
                .then(withTargets(Commands.argument("form", ResourceLocationArgument.id()).suggests(FORMS), CreatorCommands::transform)));

        root.then(creator("unlock")
                .then(withTargets(Commands.literal("fruits"), CreatorCommands::unlockFruits))
                .then(withTargets(Commands.literal("styles"), CreatorCommands::unlockStyles))
                .then(withTargets(Commands.literal("weapons"), CreatorCommands::unlockWeapons))
                .then(withTargets(Commands.literal("transformations"), CreatorCommands::unlockTransformations))
                .then(withTargets(Commands.literal("awakenings"), CreatorCommands::unlockAwakenings)));

        registerMastery(root);
    }

    private static void registerMastery(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(creator("mastery").then(Commands.literal("set")
                .then(Commands.literal("style").then(Commands.argument("style", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(GLStyles.REGISTRY.keySet(), builder))
                        .then(withTargets(Commands.argument("level", DoubleArgumentType.doubleArg(0, PlayerData.MAX_MASTERY)),
                                CreatorCommands::setStyleMastery))))
                .then(Commands.literal("race").then(withTargets(Commands.argument("level",
                        DoubleArgumentType.doubleArg(0, PlayerData.MAX_MASTERY)), CreatorCommands::setRaceMastery)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
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

    // ---------------------------------------------------------------------------------------------------------
    // Menu
    // ---------------------------------------------------------------------------------------------------------

    private static int openMenu(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, OpenCreatorMenuPayload.INSTANCE);
        return 1;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Loadouts and random builds
    // ---------------------------------------------------------------------------------------------------------

    private static int listLoadouts(CommandSourceStack source, ServerPlayer player) {
        Map<String, Loadout> loadouts = GLPlayer.data(player).loadouts();
        Text.send(source, Text.header("Loadouts"));
        if (loadouts.isEmpty()) {
            Text.send(source, Component.literal("  None yet: /onepiece loadout save <name>").withStyle(ChatFormatting.GRAY));
        }
        for (Map.Entry<String, Loadout> entry : loadouts.entrySet()) {
            Loadout loadout = entry.getValue();
            String fruit = loadout.fruit().map(ResourceLocation::getPath).orElse("no fruit");
            String style = loadout.style().map(ResourceLocation::getPath).orElse("brawler");
            Text.send(source, Text.run("  " + entry.getKey(), "/onepiece loadout load " + entry.getKey(), ChatFormatting.YELLOW)
                    .append(Component.literal(" - " + loadout.race().getPath() + ", " + fruit + ", " + style)
                            .withStyle(ChatFormatting.GRAY)));
        }
        return loadouts.size();
    }

    private static String loadoutName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        if (!Loadout.validName(name)) {
            throw BAD_NAME.create(name);
        }
        return name;
    }

    private static int saveLoadout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = loadoutName(ctx);
        Loadout loadout = Builds.capture(player);
        GLPlayer.edit(player, data -> data.saveLoadout(name, loadout));
        ctx.getSource().sendSuccess(() -> Component.literal("Saved loadout " + name), false);
        return 1;
    }

    private static int loadLoadout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = loadoutName(ctx);
        Loadout loadout = GLPlayer.data(player).loadouts().get(name);
        if (loadout == null) {
            throw NO_LOADOUT.create(name);
        }
        Builds.apply(player, loadout);
        ctx.getSource().sendSuccess(() -> Component.literal("Loaded loadout " + name), false);
        return 1;
    }

    private static int deleteLoadout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = loadoutName(ctx);
        boolean[] removed = {false};
        GLPlayer.edit(player, data -> removed[0] = data.deleteLoadout(name));
        if (!removed[0]) {
            throw NO_LOADOUT.create(name);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Deleted loadout " + name), false);
        return 1;
    }

    private static int randomBuild(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            Builds.apply(player, Builds.random(player));
            Builds.fillSlots(player);
            Text.send(ctx.getSource(), Component.literal("Random build for " + player.getGameProfile().getName() + ": ")
                    .append(describe(Builds.capture(player))));
        }
        return targets.size();
    }

    private static Component describe(Loadout loadout) {
        String fruit = loadout.fruit().map(ResourceLocation::getPath).orElse("no fruit") + (loadout.awakened() ? " (awakened)" : "");
        String style = loadout.style().map(ResourceLocation::getPath).orElse("brawler");
        String haki = loadout.haki().isEmpty() ? "no Haki" : String.join("/", loadout.haki().keySet().stream().sorted().toList());
        String weapon = loadout.weapon().map(ResourceLocation::getPath).orElse("bare hands");
        return Component.literal(loadout.race().getPath() + ", " + fruit + ", " + style + ", " + haki + ", " + weapon)
                .withStyle(ChatFormatting.YELLOW);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Battle tests
    // ---------------------------------------------------------------------------------------------------------

    private static int battleRandom(CommandSourceStack source, ServerPlayer player, Battles.Difficulty difficulty) {
        int count = Battles.spawnWave(player, difficulty);
        source.sendSuccess(() -> Component.literal("A " + difficulty.getSerializedName() + " wave of " + count + " enemies appears!")
                .withStyle(ChatFormatting.RED), false);
        return count;
    }

    private static int battleSpawn(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "entity");
        if (!Battles.fighters().contains(id)) {
            throw UNKNOWN_ENTITY.create(id);
        }
        int spawned = Battles.spawn(player, id, count);
        ctx.getSource().sendSuccess(() -> Component.literal("Spawned " + spawned + " " + id.getPath()), false);
        return spawned;
    }

    private static int battleClear(CommandSourceStack source, ServerPlayer player) {
        int removed = Battles.clear(player);
        source.sendSuccess(() -> Component.literal("Removed " + removed + " battle-test enemies"), false);
        return removed;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Transformations
    // ---------------------------------------------------------------------------------------------------------

    private static int transform(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "form");
        Transformation form = GLTransformations.REGISTRY.get(id);
        if (form == null) {
            throw UNKNOWN_FORM.create(id);
        }
        for (ServerPlayer player : targets) {
            TransformationManager.start(player, form);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Transformed " + name + " into " + id));
        return targets.size();
    }

    private static int transformOff(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            TransformationManager.end(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Ended the transformation of " + name));
        return targets.size();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Style and race mastery
    // ---------------------------------------------------------------------------------------------------------

    private static int setStyleMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "style");
        FightingStyle style = GLStyles.REGISTRY.get(id);
        if (style == null) {
            throw UNKNOWN_STYLE.create(id);
        }
        double level = DoubleArgumentType.getDouble(ctx, "level");
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> data.setMastery(style.masteryKey(), level));
            if (StyleManager.style(player) == style) {
                StyleManager.equip(player, style);
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Set " + id.getPath() + " mastery of " + name + " to " + level));
        return targets.size();
    }

    private static int setRaceMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        double level = DoubleArgumentType.getDouble(ctx, "level");
        for (ServerPlayer player : targets) {
            ResourceLocation race = GLPlayer.data(player).race();
            GLPlayer.edit(player, data -> data.setMastery(RaceManager.masteryKey(race), level));
            RaceManager.equipUsable(player);
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Set race mastery of " + name + " to " + level));
        return targets.size();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Unlocking whole categories
    // ---------------------------------------------------------------------------------------------------------

    private static int unlockFruits(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        targets.forEach(FruitCommands::unlockAllFruits);
        feedback(ctx.getSource(), targets, name -> Component.literal("Unlocked every Devil Fruit for " + name));
        return targets.size();
    }

    private static int unlockStyles(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GLStyles.REGISTRY.forEach(style -> StyleManager.learn(player, style));
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Taught every fighting style to " + name));
        return targets.size();
    }

    private static int unlockWeapons(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> GLWeapons.weapons().values().forEach(item -> data.unlock(UnlockCategory.WEAPON, item.getId())));
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Unlocked every weapon for " + name));
        return targets.size();
    }

    private static int unlockTransformations(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> GLTransformations.REGISTRY.keySet()
                    .forEach(id -> data.unlock(UnlockCategory.TRANSFORMATION, id)));
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Unlocked every transformation for " + name));
        return targets.size();
    }

    private static int unlockAwakenings(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GLPlayer.edit(player, data -> GLFruits.REGISTRY.keySet().forEach(id -> data.unlock(UnlockCategory.AWAKENING, id)));
            if (FruitManager.hasFruit(player)) {
                FruitManager.awaken(player);
            }
        }
        feedback(ctx.getSource(), targets, name -> Component.literal("Unlocked every awakening for " + name));
        return targets.size();
    }
}
