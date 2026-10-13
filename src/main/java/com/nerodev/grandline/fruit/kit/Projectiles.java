package com.nerodev.grandline.fruit.kit;

import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ready-made projectile behaviours. Each takes the damage type and what else happens on a hit, so a fruit only writes
 * what is special about its projectile (freeze, bind, burn, pull...).
 */
public final class Projectiles {
    private Projectiles() {}

    /** Hits one creature for its damage and disappears. */
    public static ProjectileBehavior bolt(ResourceKey<DamageType> type, @Nullable BiConsumer<AbilityProjectile, LivingEntity> onHit) {
        return new ProjectileBehavior() {
            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner != null && target instanceof LivingEntity living && AbilityDamage.canHarm(owner, living)) {
                    AbilityDamage.hurt(owner, living, type, p.damage(), p);
                    if (onHit != null) {
                        onHit.accept(p, living);
                    }
                    return false;
                }
                return true;
            }
        };
    }

    /** Passes through every creature in its path, hitting each once. */
    public static ProjectileBehavior piercing(ResourceKey<DamageType> type, @Nullable BiConsumer<AbilityProjectile, LivingEntity> onHit) {
        return new ProjectileBehavior() {
            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner != null && target instanceof LivingEntity living && AbilityDamage.canHarm(owner, living)) {
                    AbilityDamage.hurt(owner, living, type, p.damage(), p);
                    if (onHit != null) {
                        onHit.accept(p, living);
                    }
                }
                return true;
            }
        };
    }

    /**
     * Bursts on the first creature, block or when it expires, damaging everything within {@code radius}.
     *
     * @param onHit   extra effect on each creature caught (may be null)
     * @param onBurst effect at the burst point, e.g. visuals or temporary blocks (may be null)
     */
    public static ProjectileBehavior blast(double radius, ResourceKey<DamageType> type,
            @Nullable BiConsumer<AbilityProjectile, LivingEntity> onHit, @Nullable Consumer<AbilityProjectile> onBurst) {
        return new ProjectileBehavior() {
            private void burst(AbilityProjectile p) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner == null) {
                    return;
                }
                for (LivingEntity target : AbilityDamage.around(owner, p.position(), radius * Math.max(0.5, p.power()))) {
                    target.invulnerableTime = 0;
                    AbilityDamage.hurt(owner, target, type, p.damage(), p);
                    if (onHit != null) {
                        onHit.accept(p, target);
                    }
                }
                if (onBurst != null) {
                    onBurst.accept(p);
                }
            }

            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner != null && !AbilityDamage.canHarm(owner, target)) {
                    return true;
                }
                burst(p);
                return false;
            }

            @Override
            public void onHitBlock(AbilityProjectile p, BlockHitResult hit) {
                p.setPos(hit.getLocation());
                burst(p);
            }

            @Override
            public void onExpire(AbilityProjectile p) {
                burst(p);
            }
        };
    }

    /** Wraps a behaviour so the projectile steers toward the nearest enemy within {@code range}. */
    public static ProjectileBehavior homing(double range, double turn, ProjectileBehavior inner) {
        return new ProjectileBehavior() {
            @Override
            public void tick(AbilityProjectile p) {
                ServerPlayer owner = p.ownerPlayer();
                if (owner != null) {
                    LivingEntity target = nearest(owner, p.position(), range);
                    if (target != null) {
                        Vec3 velocity = p.getDeltaMovement();
                        double speed = Math.max(0.2, velocity.length());
                        Vec3 to = target.getBoundingBox().getCenter().subtract(p.position()).normalize();
                        Vec3 dir = velocity.normalize().lerp(to, turn).normalize();
                        p.setDeltaMovement(dir.scale(speed));
                    }
                }
                inner.tick(p);
            }

            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                return inner.onHitEntity(p, target);
            }

            @Override
            public void onHitBlock(AbilityProjectile p, BlockHitResult hit) {
                inner.onHitBlock(p, hit);
            }

            @Override
            public void onExpire(AbilityProjectile p) {
                inner.onExpire(p);
            }

            @Override
            public boolean hitsBlocks() {
                return inner.hitsBlocks();
            }
        };
    }

    /**
     * A stationary (or following) area: every {@code interval} ticks, {@code each} runs for every harmable creature
     * within its radius (size / 2). {@code tick} (may be null) runs every tick for anything else.
     */
    public static ProjectileBehavior zone(int interval, BiConsumer<AbilityProjectile, LivingEntity> each,
            @Nullable Consumer<AbilityProjectile> tick) {
        return new ProjectileBehavior() {
            @Override
            public void tick(AbilityProjectile p) {
                if (tick != null) {
                    tick.accept(p);
                }
                ServerPlayer owner = p.ownerPlayer();
                if (owner == null || p.tickCount % Math.max(1, interval) != 0) {
                    return;
                }
                for (LivingEntity target : inside(owner, p)) {
                    each.accept(p, target);
                }
            }

            @Override
            public boolean onHitEntity(AbilityProjectile p, Entity target) {
                return true;
            }

            @Override
            public boolean hitsBlocks() {
                return false;
            }
        };
    }

    /** Harmable creatures inside a zone projectile's radius. */
    public static List<LivingEntity> inside(ServerPlayer owner, AbilityProjectile zone) {
        return AbilityDamage.around(owner, zone.position(), zone.size() / 2.0);
    }

    @Nullable
    public static LivingEntity nearest(ServerPlayer owner, Vec3 from, double range) {
        LivingEntity best = null;
        double bestDist = range * range;
        for (LivingEntity e : owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range),
                e -> AbilityDamage.canHarm(owner, e))) {
            double d = e.distanceToSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }
}
