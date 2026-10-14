package com.nerodev.grandline.creator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Battle tests: waves of enemies spawned in a ring around a creator, each tagged so they can be cleared again.
 * Waves are drawn from hostile vanilla creatures for now; Grand Line NPCs and bosses join the pools as they arrive.
 */
public final class Battles {
    /** Entity tag every battle-test enemy carries. */
    public static final String TAG = "grandline_battle";
    public static final int MAX_SPAWN = 20;

    /** How hard a random wave is. */
    public enum Difficulty implements StringRepresentable {
        EASY(List.of(EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE)),
        NORMAL(List.of(EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SKELETON)),
        HARD(List.of(EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.SKELETON, EntityType.SKELETON, EntityType.WITCH,
                EntityType.HUSK)),
        EXTREME(List.of(EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.PILLAGER,
                EntityType.PILLAGER, EntityType.RAVAGER)),
        LEGENDARY(List.of(EntityType.RAVAGER, EntityType.EVOKER, EntityType.EVOKER, EntityType.VINDICATOR, EntityType.VINDICATOR,
                EntityType.VINDICATOR, EntityType.WITHER_SKELETON, EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.BLAZE));

        private final List<EntityType<?>> wave;

        Difficulty(List<EntityType<?>> wave) {
            this.wave = wave;
        }

        public List<EntityType<?>> wave() {
            return wave;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private Battles() {}

    /** Every creature type that can be spawned to fight: anything hostile or able to fight back. */
    public static Set<ResourceLocation> fighters() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type.getCategory() == MobCategory.MONSTER || type == EntityType.IRON_GOLEM
                        || type == EntityType.PIGLIN_BRUTE || type == EntityType.POLAR_BEAR || type == EntityType.WOLF)
                .filter(EntityType::canSummon)
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Spawns a random wave of {@code difficulty} around the player. @return how many appeared */
    public static int spawnWave(ServerPlayer player, Difficulty difficulty) {
        List<EntityType<?>> wave = difficulty.wave();
        int spawned = 0;
        for (int i = 0; i < wave.size(); i++) {
            if (spawnOne(player, wave.get(i), i, wave.size()) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    /** Spawns {@code count} creatures of type {@code id} around the player. @return how many appeared */
    public static int spawn(ServerPlayer player, ResourceLocation id, int count) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnOne(player, type, i, count) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    /** Removes every battle-test creature within 128 blocks. @return how many */
    public static int clear(ServerPlayer player) {
        AABB area = new AABB(player.blockPosition()).inflate(128);
        List<Entity> enemies = new ArrayList<>(player.serverLevel().getEntitiesOfClass(Entity.class, area,
                entity -> entity.getTags().contains(TAG)));
        enemies.forEach(Entity::discard);
        return enemies.size();
    }

    /** One creature on a ring 6-8 blocks around the player, standing on the ground and already hunting them. */
    private static Entity spawnOne(ServerPlayer player, EntityType<?> type, int index, int total) {
        ServerLevel level = player.serverLevel();
        Entity entity = type.create(level);
        if (entity == null) {
            return null;
        }
        double angle = Mth.TWO_PI * index / Math.max(1, total) + player.getRandom().nextDouble() * 0.4;
        double radius = 6 + player.getRandom().nextDouble() * 2;
        int x = Mth.floor(player.getX() + Math.cos(angle) * radius);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * radius);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (Math.abs(ground.getY() - player.getY()) > 12) {
            ground = player.blockPosition().offset(x - player.getBlockX(), 0, z - player.getBlockZ());
        }
        entity.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, player.getRandom().nextFloat() * 360f, 0f);
        entity.addTag(TAG);
        if (entity instanceof Mob mob) {
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(ground), MobSpawnType.COMMAND, null);
            mob.setPersistenceRequired();
            mob.setTarget(player);
        }
        level.addFreshEntityWithPassengers(entity);
        return entity;
    }
}
