package com.nerodev.grandline.combat;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.config.GLServerConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Damage and targeting helpers for abilities. All ability damage goes through {@link #hurt} so PvP settings,
 * team rules and the balance multiplier are respected in one place.
 */
public final class AbilityDamage {
    public static final ResourceKey<DamageType> STRIKE = key("strike");
    public static final ResourceKey<DamageType> SLASH = key("slash");
    public static final ResourceKey<DamageType> FIRE = key("fire");
    public static final ResourceKey<DamageType> BLAST = key("blast");

    /** Body blows and blades: what Logia intangibility lets pass through. */
    public static final TagKey<DamageType> PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, GrandLine.id("physical"));
    /** Cutting damage: rubber does not soften it. */
    public static final TagKey<DamageType> SHARP = TagKey.create(Registries.DAMAGE_TYPE, GrandLine.id("sharp"));

    private AbilityDamage() {}

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, GrandLine.id(name));
    }

    public static DamageSource source(ServerPlayer attacker, ResourceKey<DamageType> type, @Nullable Entity direct) {
        return attacker.damageSources().source(type, direct == null ? attacker : direct, attacker);
    }

    /** True if {@code attacker}'s abilities may harm {@code target} (not self, PvP rules, teams). */
    public static boolean canHarm(ServerPlayer attacker, Entity target) {
        if (target == attacker || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof Player other) {
            if (!GLServerConfig.abilityPvp() || !attacker.server.isPvpAllowed() || !attacker.canHarmPlayer(other)) {
                return false;
            }
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity pet && attacker.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        return !(target instanceof Player) && target instanceof LivingEntity living ? !living.isAlliedTo(attacker) : true;
    }

    /** Deals ability damage scaled by the server multiplier. @return true if damage was applied */
    public static boolean hurt(ServerPlayer attacker, Entity target, ResourceKey<DamageType> type, float amount,
            @Nullable Entity direct) {
        if (!canHarm(attacker, target)) {
            return false;
        }
        float scaled = (float) (amount * GLServerConfig.abilityDamageMultiplier());
        return target.hurt(source(attacker, type, direct), scaled);
    }

    /** Pushes {@code target} away from {@code from} horizontally, with some lift. */
    public static void knockback(Entity target, Vec3 from, double strength, double lift) {
        Vec3 away = target.position().subtract(from);
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(1, 0, 0);
        }
        double resist = target instanceof LivingEntity living
                ? 1.0 - living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
                : 1.0;
        if (resist <= 0) {
            return;
        }
        Vec3 push = flat.normalize().scale(strength * resist).add(0, lift * resist, 0);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.5).add(push));
        target.hurtMarked = true;
    }

    /** Living entities within {@code radius} of {@code center}, nearest first, that {@code attacker} may harm. */
    public static List<LivingEntity> around(ServerPlayer attacker, Vec3 center, double radius) {
        ServerLevel level = attacker.serverLevel();
        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius),
                e -> canHarm(attacker, e) && e.position().distanceToSqr(center) <= radius * radius + e.getBbWidth()));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return found;
    }

    /** Living entities inside a cone in front of the attacker. */
    public static List<LivingEntity> cone(ServerPlayer attacker, double range, double halfAngleDegrees) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngleDegrees));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : around(attacker, eye, range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist < 1.0E-3 || to.scale(1 / dist).dot(look) >= cos || dist < e.getBbWidth()) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * First harmable entity along the look ray within {@code range}, stopped by blocks.
     *
     * @param thickness how far off-centre the ray may pass and still hit (punch width)
     */
    public static Optional<LivingEntity> raycast(ServerPlayer attacker, double range, double thickness) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 end = eye.add(attacker.getLookAngle().scale(range));
        var blockHit = attacker.level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, attacker));
        Vec3 stop = blockHit.getLocation();
        AABB sweep = new AABB(eye, stop).inflate(thickness + 1);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : attacker.level().getEntitiesOfClass(LivingEntity.class, sweep, e -> canHarm(attacker, e))) {
            AABB box = e.getBoundingBox().inflate(thickness);
            Optional<Vec3> hit = box.clip(eye, stop);
            if (hit.isPresent() || box.contains(eye)) {
                double d = hit.map(eye::distanceToSqr).orElse(0.0);
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Every harmable entity along the look ray (piercing), nearest first. */
    public static List<LivingEntity> line(ServerPlayer attacker, double range, double thickness) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 stop = lookTarget(attacker, range);
        AABB sweep = new AABB(eye, stop).inflate(thickness + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : attacker.level().getEntitiesOfClass(LivingEntity.class, sweep, e -> canHarm(attacker, e))) {
            AABB box = e.getBoundingBox().inflate(thickness);
            if (box.contains(eye) || box.clip(eye, stop).isPresent()) {
                out.add(e);
            }
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        return out;
    }

    /** End point of the look ray, stopped by the first block. */
    public static Vec3 lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        return player.level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getLocation();
    }
}
