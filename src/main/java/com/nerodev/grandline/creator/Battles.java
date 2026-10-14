package com.nerodev.grandline.creator;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.boss.BossDefinition;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Battle tests: waves of enemies spawned in a ring around a creator, each tagged so they can be cleared again.
 * Random waves are drawn from the islanders who fight (pirates, gunners, Fish-Men, bounty hunters); a legendary wave
 * brings a boss. Any creature can also be spawned by id.
 */
public final class Battles {
    /** Entity tag every battle-test enemy carries. */
    public static final String TAG = "grandline_battle";
    public static final int MAX_SPAWN = 20;

    /** How hard a random wave is. */
    public enum Difficulty implements StringRepresentable {
        EASY(false, "pirate_grunt", "pirate_grunt", "pirate_grunt"),
        NORMAL(false, "pirate_grunt", "pirate_grunt", "pirate_gunner", "pirate_gunner"),
        HARD(false, "pirate_grunt", "pirate_grunt", "pirate_gunner", "pirate_gunner", "fishman_pirate", "pirate_captain"),
        EXTREME(false, "fishman_pirate", "fishman_pirate", "bounty_hunter_veteran", "bounty_hunter_veteran", "pirate_gunner",
                "pirate_gunner", "pirate_captain"),
        LEGENDARY(true, "fishman_pirate", "fishman_pirate", "bounty_hunter_veteran", "bounty_hunter_veteran");

        private final boolean boss;
        private final List<ResourceLocation> wave;

        Difficulty(boolean boss, String... npcs) {
            this.boss = boss;
            this.wave = Arrays.stream(npcs).map(GrandLine::id).toList();
        }

        /** The islanders of the wave. */
        public List<ResourceLocation> wave() {
            return wave;
        }

        /** Whether a random boss leads the wave. */
        public boolean boss() {
            return boss;
        }

        /** How many enemies the wave brings. */
        public int size() {
            return wave.size() + (boss ? 1 : 0);
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

    /** Spawns a wave of {@code difficulty} around the player, every one of them already after the player. @return how many appeared */
    public static int spawnWave(ServerPlayer player, Difficulty difficulty) {
        List<ResourceLocation> wave = difficulty.wave();
        int total = difficulty.size();
        int spawned = 0;
        for (int i = 0; i < wave.size(); i++) {
            BlockPos at = ring(player, i, total, 6);
            NpcEntity npc = NpcEntity.spawn(player.serverLevel(), wave.get(i), at, player.getRandom().nextFloat() * 360f, MobSpawnType.COMMAND);
            if (npc != null) {
                npc.addTag(TAG);
                npc.provoke(player);
                spawned++;
            }
        }
        if (difficulty.boss() && spawnBoss(player, total - 1, total) != null) {
            spawned++;
        }
        return spawned;
    }

    /**
     * Spawns a wave of {@code difficulty} on a ring {@code radius} out from {@code centre} (an arena), each set on one of
     * {@code targets}. @return those who appeared
     */
    public static List<LivingEntity> spawnWaveAt(ServerLevel level, BlockPos centre, double radius, Difficulty difficulty,
            List<ServerPlayer> targets) {
        List<LivingEntity> out = new ArrayList<>();
        if (targets.isEmpty()) {
            return out;
        }
        RandomSource random = level.getRandom();
        int total = difficulty.size();
        for (int i = 0; i < difficulty.wave().size(); i++) {
            BlockPos at = ring(level, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, i, total, radius, random);
            NpcEntity npc = NpcEntity.spawn(level, difficulty.wave().get(i), at, random.nextFloat() * 360f, MobSpawnType.COMMAND);
            if (npc != null) {
                npc.addTag(TAG);
                npc.clearRestriction();
                npc.provoke(targets.get(i % targets.size()));
                out.add(npc);
            }
        }
        if (difficulty.boss()) {
            List<ResourceLocation> bosses = landBosses(level);
            if (!bosses.isEmpty()) {
                BlockPos at = ring(level, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, total - 1, total, radius, random);
                BossEntity boss = BossEntity.spawn(level, bosses.get(random.nextInt(bosses.size())), at, random.nextFloat() * 360f,
                        MobSpawnType.COMMAND);
                if (boss != null) {
                    boss.addTag(TAG);
                    boss.setTarget(targets.get(random.nextInt(targets.size())));
                    out.add(boss);
                }
            }
        }
        return out;
    }

    /** Bosses who fight on land (Sea Kings stay in the sea). */
    public static List<ResourceLocation> landBosses(ServerLevel level) {
        List<ResourceLocation> out = new ArrayList<>();
        level.registryAccess().registryOrThrow(GLRegistries.BOSS).entrySet().stream()
                .filter(e -> e.getValue().body() == BossDefinition.Body.HUMANOID)
                .forEach(e -> out.add(e.getKey().location()));
        out.sort(Comparator.comparing(ResourceLocation::toString));
        return out;
    }

    /** Spawns a random boss near the player, already fighting them. @return the boss, or null if there is none */
    @Nullable
    public static BossEntity spawnBoss(ServerPlayer player, int index, int total) {
        List<ResourceLocation> bosses = landBosses(player.serverLevel());
        if (bosses.isEmpty()) {
            return null;
        }
        ResourceLocation id = bosses.get(player.getRandom().nextInt(bosses.size()));
        BossEntity boss = BossEntity.spawn(player.serverLevel(), id, ring(player, index, total, 9), player.getYRot() + 180f, MobSpawnType.COMMAND);
        if (boss != null) {
            boss.addTag(TAG);
            boss.setTarget(player);
        }
        return boss;
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
        BlockPos ground = ring(player, index, total, 6);
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

    /** Spot {@code index} of {@code total} on a ring {@code radius}-{@code radius + 2} blocks around the player, on the ground. */
    private static BlockPos ring(ServerPlayer player, int index, int total, double radius) {
        return ring(player.serverLevel(), player.getX(), player.getY(), player.getZ(), index, total, radius, player.getRandom());
    }

    private static BlockPos ring(ServerLevel level, double cx, double cy, double cz, int index, int total, double radius, RandomSource random) {
        double angle = Mth.TWO_PI * index / Math.max(1, total) + random.nextDouble() * 0.4;
        double r = radius + random.nextDouble() * 2;
        int x = Mth.floor(cx + Math.cos(angle) * r);
        int z = Mth.floor(cz + Math.sin(angle) * r);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (Math.abs(ground.getY() - cy) > 12) {
            ground = new BlockPos(x, Mth.floor(cy), z);
        }
        return ground;
    }
}
