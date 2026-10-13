package com.nerodev.grandline.fruit.kit;

import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Creatures a power brings into being for a while: biscuit soldiers, shadows, ghosts, drawings, toys. They fight for
 * their owner (whatever the owner attacks or is attacked by, else the nearest monster), never turn on the owner or
 * the owner's other minions, drop nothing, follow the owner around, and vanish when their time is up, when the
 * owner leaves, or when the server stops. A minion found in a saved chunk is removed on load.
 */
public final class Minions {
    /** Persistent-data flag every minion carries so stray ones can be recognised after a reload. */
    public static final String TAG = "grandline_minion";
    /** Flag on an existing creature temporarily bound to someone's will (released, not removed, afterwards). */
    public static final String BORROWED = "grandline_borrowed";

    private record Minion(Mob mob, UUID owner, ResourceLocation kind, long until, boolean borrowed) {}

    private static final List<Minion> ACTIVE = new ArrayList<>();

    private Minions() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> dismissAll());
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, Minions::onJoin);
        NeoForge.EVENT_BUS.addListener(LivingChangeTargetEvent.class, Minions::onTarget);
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, Minions::onDamage);
        NeoForge.EVENT_BUS.addListener(LivingDropsEvent.class, e -> {
            if (isMinion(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingExperienceDropEvent.class, e -> {
            if (isMinion(e.getEntity())) {
                e.setCanceled(true);
            }
        });
    }

    /**
     * Summons a minion of {@code type} at {@code pos} for {@code lifetime} ticks.
     *
     * @param kind  which power made it (so that power can dismiss just its own)
     * @param setup optional extra setup (equipment, attributes, effects) applied before it joins the world
     * @return the minion, or null if the type could not be created
     */
    @Nullable
    public static <T extends Mob> T spawn(ServerPlayer owner, ResourceLocation kind, EntityType<T> type, Vec3 pos, int lifetime,
            @Nullable Consumer<T> setup) {
        ServerLevel level = owner.serverLevel();
        T mob = type.create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(BlockPos.containing(pos)),
                MobSpawnType.MOB_SUMMONED, null);
        mob.getPersistentData().putBoolean(TAG, true);
        mob.setPersistenceRequired();
        if (setup != null) {
            setup.accept(mob);
        }
        ACTIVE.add(new Minion(mob, owner.getUUID(), kind, level.getGameTime() + lifetime, false));
        level.addFreshEntity(mob);
        Vfx.burst(level, mob, mob.position().add(0, mob.getBbHeight() / 2, 0), 0xFFFFFF, mob.getBbWidth() + 0.5f, Kind.SMOKE, 0);
        return mob;
    }

    /**
     * Like {@link #spawn} for a type only known at runtime (e.g. a creature the user copied). Returns null if the type
     * is not a mob or is a boss.
     */
    @Nullable
    public static Mob spawnAny(ServerPlayer owner, ResourceLocation kind, EntityType<?> type, Vec3 pos, int lifetime,
            @Nullable Consumer<Mob> setup) {
        if (type.is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) {
            return null;
        }
        ServerLevel level = owner.serverLevel();
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        mob.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(BlockPos.containing(pos)),
                MobSpawnType.MOB_SUMMONED, null);
        mob.getPersistentData().putBoolean(TAG, true);
        mob.setPersistenceRequired();
        if (setup != null) {
            setup.accept(mob);
        }
        ACTIVE.add(new Minion(mob, owner.getUUID(), kind, level.getGameTime() + lifetime, false));
        level.addFreshEntity(mob);
        Vfx.burst(level, mob, mob.position().add(0, mob.getBbHeight() / 2, 0), 0xFFFFFF, mob.getBbWidth() + 0.5f, Kind.SMOKE, 0);
        return mob;
    }

    /**
     * Bends an existing creature to {@code owner}'s will for {@code ticks} (a puppet, a toy, a rewritten memory): it
     * fights for the owner like a minion, then is simply released. Bosses refuse. @return true if bound
     */
    public static boolean bind(ServerPlayer owner, ResourceLocation kind, Mob mob, int ticks) {
        if (mob.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES) || isMinion(mob)) {
            return false;
        }
        ACTIVE.removeIf(m -> m.mob() == mob);
        mob.getPersistentData().putBoolean(BORROWED, true);
        mob.setTarget(null);
        ACTIVE.add(new Minion(mob, owner.getUUID(), kind, mob.level().getGameTime() + ticks, true));
        return true;
    }

    public static boolean isMinion(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG);
    }

    /** True if {@code entity} is one of {@code owner}'s minions. */
    public static boolean isAlly(Entity owner, Entity entity) {
        for (Minion m : ACTIVE) {
            if (m.mob() == entity) {
                return m.owner().equals(owner.getUUID());
            }
        }
        return false;
    }

    /** The owner of a minion, if online. */
    @Nullable
    public static ServerPlayer ownerOf(Entity entity) {
        for (Minion m : ACTIVE) {
            if (m.mob() == entity && entity.getServer() != null) {
                return entity.getServer().getPlayerList().getPlayer(m.owner());
            }
        }
        return null;
    }

    /** {@code owner}'s living minions of {@code kind} (any kind if null). */
    public static List<Mob> of(ServerPlayer owner, @Nullable ResourceLocation kind) {
        List<Mob> out = new ArrayList<>();
        for (Minion m : ACTIVE) {
            if (m.owner().equals(owner.getUUID()) && (kind == null || kind.equals(m.kind())) && m.mob().isAlive()) {
                out.add(m.mob());
            }
        }
        return out;
    }

    /** Sends away {@code owner}'s minions of {@code kind} (all of them if null). */
    public static void dismiss(ServerPlayer owner, @Nullable ResourceLocation kind) {
        Iterator<Minion> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Minion m = it.next();
            if (m.owner().equals(owner.getUUID()) && (kind == null || kind.equals(m.kind()))) {
                end(m);
                it.remove();
            }
        }
    }

    private static void dismissAll() {
        for (Minion m : ACTIVE) {
            if (m.borrowed()) {
                release(m.mob());
            } else {
                m.mob().discard();
            }
        }
        ACTIVE.clear();
    }

    private static void end(Minion m) {
        if (m.borrowed()) {
            release(m.mob());
        } else {
            vanish(m.mob());
        }
    }

    /** A borrowed creature comes back to its senses. */
    private static void release(Mob mob) {
        mob.getPersistentData().remove(BORROWED);
        mob.setTarget(null);
    }

    private static void vanish(Mob mob) {
        if (mob.isAlive() && mob.level() instanceof ServerLevel level) {
            Vfx.burst(level, mob, mob.position().add(0, mob.getBbHeight() / 2, 0), 0xFFFFFF, mob.getBbWidth() + 0.5f, Kind.SMOKE, 0);
        }
        mob.discard();
    }

    private static void tick(MinecraftServer server) {
        Iterator<Minion> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Minion m = it.next();
            Mob mob = m.mob();
            ServerPlayer owner = server.getPlayerList().getPlayer(m.owner());
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                continue;
            }
            if (owner == null || owner.level() != mob.level() || mob.level().getGameTime() >= m.until()) {
                end(m);
                it.remove();
                continue;
            }
            if (mob.distanceToSqr(owner) > 32 * 32) {
                Vec3 near = owner.position().add(owner.getRandom().nextDouble() * 4 - 2, 0, owner.getRandom().nextDouble() * 4 - 2);
                mob.teleportTo(near.x, near.y, near.z);
            }
            if (mob.tickCount % 10 == 0) {
                retarget(mob, owner);
            }
        }
    }

    private static void retarget(Mob mob, ServerPlayer owner) {
        LivingEntity current = mob.getTarget();
        if (current != null && (!current.isAlive() || friendly(owner, current))) {
            mob.setTarget(null);
            current = null;
        }
        LivingEntity wanted = null;
        LivingEntity hit = owner.getLastHurtMob();
        if (hit != null && hit.isAlive() && owner.tickCount - owner.getLastHurtMobTimestamp() < 200 && !friendly(owner, hit)) {
            wanted = hit;
        }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (wanted == null && attacker != null && attacker.isAlive() && !friendly(owner, attacker)) {
            wanted = attacker;
        }
        if (wanted == null && current == null) {
            double best = 16 * 16;
            for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16),
                    e -> e instanceof Enemy && e.isAlive() && !friendly(owner, e))) {
                double d = candidate.distanceToSqr(mob);
                if (d < best) {
                    best = d;
                    wanted = candidate;
                }
            }
        }
        if (wanted != null && wanted != current && wanted.distanceToSqr(mob) < 32 * 32) {
            mob.setTarget(wanted);
        }
    }

    /** The owner, their minions, and anything else the owner's abilities may not harm. */
    private static boolean friendly(ServerPlayer owner, LivingEntity entity) {
        return entity == owner || isAlly(owner, entity) || !com.nerodev.grandline.combat.AbilityDamage.canHarm(owner, entity);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk()) {
            return;
        }
        if (isMinion(event.getEntity())) {
            event.setCanceled(true);
        } else if (event.getEntity().getPersistentData().getBoolean(BORROWED)) {
            event.getEntity().getPersistentData().remove(BORROWED);
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        ServerPlayer owner = ownerOf(event.getEntity());
        if (owner != null && friendly(owner, target)) {
            event.setCanceled(true);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (source == null || !isMinion(source)) {
            return;
        }
        ServerPlayer owner = ownerOf(source);
        if (owner != null && friendly(owner, event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
