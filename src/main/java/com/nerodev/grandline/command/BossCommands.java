package com.nerodev.grandline.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.boss.BossDefinition;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.boss.BossMove;
import com.nerodev.grandline.boss.BossMoves;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.DefinitionType;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** {@code /onepiece boss}: the bosses, and tools to stage and inspect their fights. */
final class BossCommands {
    static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/onepiece boss list", "Every boss, their title and their phases", PermissionTier.PLAYER),
            new HelpEntry("/onepiece boss info", "The nearest boss: health, phase and the moves it has made", PermissionTier.PLAYER),
            new HelpEntry("/onepiece boss spawn <boss|random>", "Spawn a boss where you look; it guards that spot", PermissionTier.CREATOR),
            new HelpEntry("/onepiece boss move <move>", "Make the nearest boss use a move now", PermissionTier.CREATOR),
            new HelpEntry("/onepiece boss phase <phase>", "Drop the nearest boss into a later phase of its fight", PermissionTier.CREATOR),
            new HelpEntry("/onepiece boss remove [radius]", "Remove the bosses around you", PermissionTier.CREATOR));

    private static final double NEAR = 48;

    private BossCommands() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("boss")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(creator("spawn").then(Commands.literal("random").executes(ctx -> spawnRandom(ctx.getSource())))
                        .then(Commands.argument("boss", ResourceLocationArgument.id())
                        .suggests(RegistryArguments.suggest(DefinitionType.BOSS))
                        .executes(BossCommands::spawn)))
                .then(creator("move").then(Commands.argument("move", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BossMoves.all().stream().map(BossMove::id), builder))
                        .executes(ctx -> move(ctx.getSource(), StringArgumentType.getString(ctx, "move")))))
                .then(creator("phase").then(Commands.argument("phase", IntegerArgumentType.integer(1, 9))
                        .executes(ctx -> phase(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "phase")))))
                .then(creator("remove")
                        .executes(ctx -> remove(ctx.getSource(), 32))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> creator(String name) {
        return Commands.literal(name).requires(source -> Permissions.has(source, PermissionTier.CREATOR));
    }

    private static int list(CommandSourceStack source) {
        Text.send(source, Text.header("Bosses"));
        List<ResourceLocation> ids = DefinitionType.BOSS.sortedIds(source.registryAccess());
        for (ResourceLocation id : ids) {
            BossDefinition boss = DefinitionType.BOSS.registry(source.registryAccess()).get(id);
            Text.send(source, Component.literal("  ").append(boss.displayName().copy().withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(", ").withStyle(ChatFormatting.GRAY)).append(boss.title().copy().withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" (" + id + ")").withStyle(ChatFormatting.DARK_GRAY)));
            MutableComponent phases = Component.empty();
            for (int i = 0; i < boss.phases().size(); i++) {
                BossDefinition.Phase phase = boss.phases().get(i);
                if (i > 0) {
                    phases.append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY));
                }
                phases.append(phase.name().copy().withStyle(ChatFormatting.YELLOW));
            }
            Text.send(source, Component.literal("    ").append(phases).append(Component.literal("  " + (int) boss.stats().health() + " health, "
                    + Bounties.format(boss.rewards().bounty()) + " bounty").withStyle(ChatFormatting.GRAY)));
        }
        return ids.size();
    }

    @Nullable
    private static BossEntity nearest(CommandSourceStack source) {
        Vec3 at = source.getPosition();
        return source.getLevel().getEntitiesOfClass(BossEntity.class, new AABB(at, at).inflate(NEAR), BossEntity::isAlive).stream()
                .min(Comparator.comparingDouble(boss -> boss.distanceToSqr(at))).orElse(null);
    }

    private static int info(CommandSourceStack source) {
        BossEntity boss = nearest(source);
        BossDefinition def = boss == null ? null : boss.definition();
        if (boss == null || def == null) {
            source.sendFailure(Component.literal("No boss within " + (int) NEAR + " blocks"));
            return 0;
        }
        Text.send(source, Text.header(boss.bossName()));
        Text.send(source, Text.line("Health", String.format(Locale.ROOT, "%.0f / %.0f", boss.getHealth(), boss.getMaxHealth())));
        Text.send(source, Text.line("Phase", def.phases().get(Math.min(boss.phase(), def.phases().size() - 1)).name().copy()
                .append(Component.literal(" (" + (boss.phase() + 1) + " of " + def.phases().size() + ")"))));
        BossMove move = boss.currentMove();
        Text.send(source, Text.line("Doing", move == null ? "-" : move.id()));
        Text.send(source, Text.line("Fighting", boss.getTarget() == null ? Component.literal("no one") : boss.getTarget().getDisplayName()));
        Text.send(source, Text.line("Lately", boss.history().isEmpty() ? "-" : String.join(", ", boss.history())));
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Map.Entry<ResourceLocation, BossDefinition> boss = RegistryArguments.get(ctx, "boss", DefinitionType.BOSS);
        Vec3 look = Vec3.directionFromRotation(0, source.getRotation().y);
        BlockPos pos = BlockPos.containing(source.getPosition().add(look.scale(5)));
        BossEntity spawned = BossEntity.spawn(source.getLevel(), boss.getKey(), pos, source.getRotation().y + 180, MobSpawnType.COMMAND);
        if (spawned == null) {
            source.sendFailure(Component.literal("Could not spawn " + boss.getKey()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned ").append(boss.getValue().displayName()), true);
        return 1;
    }

    private static int spawnRandom(CommandSourceStack source) {
        List<ResourceLocation> ids = DefinitionType.BOSS.sortedIds(source.registryAccess());
        if (ids.isEmpty()) {
            source.sendFailure(Component.literal("There are no bosses"));
            return 0;
        }
        ResourceLocation id = ids.get(source.getLevel().getRandom().nextInt(ids.size()));
        Vec3 look = Vec3.directionFromRotation(0, source.getRotation().y);
        BossEntity spawned = BossEntity.spawn(source.getLevel(), id, BlockPos.containing(source.getPosition().add(look.scale(5))),
                source.getRotation().y + 180, MobSpawnType.COMMAND);
        if (spawned == null) {
            source.sendFailure(Component.literal("Could not spawn " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned ").append(spawned.bossName()), true);
        return 1;
    }

    private static int move(CommandSourceStack source, String id) {
        BossMove move = BossMoves.get(id.toLowerCase(Locale.ROOT));
        if (move == null) {
            source.sendFailure(Component.literal("Unknown move: " + id));
            return 0;
        }
        BossEntity boss = nearest(source);
        if (boss == null) {
            source.sendFailure(Component.literal("No boss within " + (int) NEAR + " blocks"));
            return 0;
        }
        boss.perform(move);
        source.sendSuccess(() -> boss.bossName().copy().append(Component.literal(" uses " + move.id())), false);
        return 1;
    }

    private static int phase(CommandSourceStack source, int phase) {
        BossEntity boss = nearest(source);
        BossDefinition def = boss == null ? null : boss.definition();
        if (boss == null || def == null) {
            source.sendFailure(Component.literal("No boss within " + (int) NEAR + " blocks"));
            return 0;
        }
        if (phase < 2 || phase > def.phases().size()) {
            source.sendFailure(Component.literal(def.displayName().getString() + "'s later phases are 2 to " + def.phases().size()));
            return 0;
        }
        boss.forcePhase(phase - 1);
        source.sendSuccess(() -> boss.bossName().copy().append(Component.literal(" enters ")).append(def.phases().get(phase - 1).name()), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, int radius) {
        Vec3 at = source.getPosition();
        List<BossEntity> bosses = source.getLevel().getEntitiesOfClass(BossEntity.class, new AABB(at, at).inflate(radius));
        bosses.forEach(BossEntity::discard);
        source.sendSuccess(() -> Component.literal("Removed " + bosses.size() + " boss(es)"), true);
        return bosses.size();
    }
}
