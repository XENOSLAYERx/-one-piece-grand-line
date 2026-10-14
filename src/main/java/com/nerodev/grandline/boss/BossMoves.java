package com.nerodev.grandline.boss;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.NpcShot;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.weapon.GLWeapons;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Every boss move, by id. Each is telegraphed during its windup (a pose, sounds and warning particles in the shape of
 * what is coming) so a sharp player can dodge, guard or get clear, and each leaves the boss open while it recovers.
 */
public final class BossMoves {
    private static final Map<String, BossMove> MOVES = new LinkedHashMap<>();

    // ---------------------------------------------------------------------------------------------------------
    // Shared
    // ---------------------------------------------------------------------------------------------------------

    /** Between phases: an untouchable roar that throws everyone back. */
    public static final BossMove ROAR = register(new BossMove("roar", 0, 34, 0, 0, 0, 64, true, (boss, target, t) -> {
        if (t == 0) {
            sound(boss, SoundEvents.RAVAGER_ROAR, 2f, 0.8f);
            boss.getNavigation().stop();
        }
        if (t % 6 == 0) {
            burst(boss, boss.position().add(0, 0.2, 0), 0xE8E0D0, 3f + t * 0.15f, Kind.RING, Vfx.RING);
        }
        if (t == 10) {
            for (LivingEntity victim : boss.targetsAround(boss.position(), 7)) {
                AbilityDamage.knockback(victim, boss.position(), 1.4, 0.5);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            burst(boss, boss.position().add(0, 1, 0), 0xFFFFFF, 6f, Kind.SHOCKWAVE, Vfx.SHOCKWAVE | Vfx.HEAVY);
        }
    }));

    /** A raised guard: blows from the front glance off and are answered with a counter. */
    public static final BossMove GUARD = register(new BossMove("guard", 0, 36, 4, 120, 0, 6, false, (boss, target, t) -> {
        if (t == 0) {
            sound(boss, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2f, 0.7f);
        }
        boss.getNavigation().stop();
        if (target != null) {
            boss.getLookControl().setLookAt(target, 30f, 30f);
        }
    }));

    // ---------------------------------------------------------------------------------------------------------
    // Captain "Axe-Hand" Morgan
    // ---------------------------------------------------------------------------------------------------------

    /** A wide axe swing in front of him. */
    public static final BossMove CLEAVE = register(new BossMove("cleave", 14, 3, 12, 40, 0, 4.5, false, (boss, target, t) -> {
        face(boss, target, t < 10);
        if (t < 14 && t % 3 == 0) {
            telegraphArc(boss, 4.5, 140, 0xE84A3A);
        }
        if (t == 14) {
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
            sound(boss, SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.8f);
            slash(boss, 4.5f, 0xD8DCE6);
            for (LivingEntity victim : boss.targetsInArc(4.5, 140)) {
                boss.strike(victim, 1.4f, AbilityDamage.SLASH);
                AbilityDamage.knockback(victim, boss.position(), 0.9, 0.25);
            }
        }
    }));

    /** The axe brought down overhead: a shockwave rings out across the ground. */
    public static final BossMove SLAM = register(new BossMove("slam", 18, 8, 18, 90, 0, 6, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        face(boss, target, t < 12);
        if (t < 18 && t % 4 == 0) {
            burst(boss, boss.position().add(0, 0.1, 0), 0xE84A3A, 6f, Kind.RING, Vfx.RING);
        }
        if (t == 18) {
            sound(boss, SoundEvents.ANVIL_LAND, 1.6f, 0.5f);
            sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 1f, 0.8f);
            Vec3 centre = boss.position().add(boss.forward().scale(1.5));
            burst(boss, centre.add(0, 0.2, 0), 0x8A7A66, 6f, Kind.SHARD, Vfx.GROUND | Vfx.HEAVY);
            for (LivingEntity victim : boss.targetsAround(centre, 6)) {
                float falloff = (float) Math.max(0.35, 1 - victim.distanceTo(boss) / 7);
                boss.strike(victim, 1.8f * falloff, AbilityDamage.CRUSH);
                AbilityDamage.knockback(victim, centre, 0.6, 0.7);
            }
        }
        if (t > 18 && t < 26) {
            burst(boss, boss.position().add(0, 0.1, 0), 0xC8B89A, 2f + (t - 18) * 0.6f, Kind.RING, Vfx.RING);
        }
    }));

    /** A head-down charge in a straight line, bowling over anyone in the way. */
    public static final BossMove CHARGE = register(new BossMove("charge", 14, 16, 22, 140, 5, 16, false, (boss, target, t) -> {
        if (t < 14) {
            boss.getNavigation().stop();
            if (target != null) {
                boss.lockDirection(target.position().subtract(boss.position()).multiply(1, 0, 1));
                boss.getLookControl().setLookAt(target, 30f, 30f);
            }
            if (t % 3 == 0) {
                trail(boss, boss.position().add(0, 0.1, 0), boss.position().add(boss.lockedDirection().scale(10)).add(0, 0.1, 0), 0xE84A3A);
            }
            if (t == 0) {
                sound(boss, SoundEvents.RAVAGER_STEP, 1.5f, 0.6f);
            }
            return;
        }
        if (t < 30) {
            if (boss.horizontalCollision && t > 15) {
                sound(boss, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.6f);
                burst(boss, boss.position().add(0, 1, 0), 0x8A7A66, 1.5f, Kind.SHARD, Vfx.HEAVY);
                boss.skipTo(30);
                return;
            }
            Vec3 dir = boss.lockedDirection();
            boss.setDeltaMovement(dir.x * 0.85, boss.getDeltaMovement().y, dir.z * 0.85);
            if (t % 2 == 0) {
                burst(boss, boss.position().add(0, 0.3, 0), 0xB8A890, 0.8f, Kind.SMOKE, 0);
            }
            for (LivingEntity victim : boss.targetsAround(boss.position().add(dir.scale(1.2)), 1.8)) {
                if (boss.firstHit(victim)) {
                    boss.strike(victim, 1.5f, AbilityDamage.STRIKE);
                    AbilityDamage.knockback(victim, boss.position(), 1.6, 0.45);
                    sound(boss, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4f, 0.7f);
                }
            }
        }
    }));

    /** A crack of the iron jaw into whoever is closest. */
    public static final BossMove JAW_BASH = register(new BossMove("jaw_bash", 8, 3, 10, 50, 0, 3, false, (boss, target, t) -> {
        face(boss, target, true);
        if (t == 8) {
            sound(boss, SoundEvents.ANVIL_HIT, 1.4f, 0.6f);
            for (LivingEntity victim : boss.targetsInArc(3.2, 70)) {
                boss.strike(victim, 1.2f, AbilityDamage.STRIKE);
                AbilityDamage.knockback(victim, boss.position(), 1.8, 0.5);
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                burst(boss, victim.getBoundingBox().getCenter(), 0xC0C4CC, 1.2f, Kind.STAR, Vfx.FLASH);
            }
        }
    }));

    /** Morgan calls his Marines to arms. */
    public static final BossMove REINFORCEMENTS = register(new BossMove("reinforcements", 20, 1, 10, 600, 0, 40, false,
            (boss, target, t) -> {
                boss.getNavigation().stop();
                if (t == 0) {
                    sound(boss, SoundEvents.RAID_HORN.value(), 1.5f, 1.2f);
                    boss.shout("Marines! Seize them!");
                }
                if (t == 20 && boss.level() instanceof ServerLevel level) {
                    for (int i = 0; i < 2; i++) {
                        double angle = boss.getRandom().nextDouble() * Mth.TWO_PI;
                        BlockPos at = BlockPos.containing(boss.getX() + Math.cos(angle) * 3, boss.getY(), boss.getZ() + Math.sin(angle) * 3);
                        NpcEntity marine = NpcEntity.spawn(level, GrandLine.id("marine_recruit"), at, boss.getYRot(), MobSpawnType.MOB_SUMMONED);
                        if (marine != null) {
                            boss.summoned(marine);
                            if (target != null) {
                                marine.provoke(target);
                            }
                            burst(boss, marine.position().add(0, 1, 0), 0x2F63B8, 1f, Kind.SMOKE, 0);
                        }
                    }
                }
            }));

    /** Spinning with the axe held out: a whirl that follows its prey. */
    public static final BossMove WHIRLWIND = register(new BossMove("whirlwind", 10, 44, 16, 160, 0, 9, false, (boss, target, t) -> {
        if (t < 10) {
            face(boss, target, true);
            boss.getNavigation().stop();
            return;
        }
        if (t < 54) {
            if (target != null) {
                boss.getNavigation().moveTo(target, 0.75);
            }
            if (t % 6 == 0) {
                sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.9f + boss.getRandom().nextFloat() * 0.2f);
                spiral(boss, 3.2f, 0xD8DCE6);
                for (LivingEntity victim : boss.targetsAround(boss.position(), 3.2)) {
                    boss.strike(victim, 0.75f, AbilityDamage.SLASH);
                    AbilityDamage.knockback(victim, boss.position(), 0.5, 0.15);
                }
            }
        } else {
            boss.getNavigation().stop();
        }
    }));

    // ---------------------------------------------------------------------------------------------------------
    // Arlong the Saw
    // ---------------------------------------------------------------------------------------------------------

    /** A lunging shark bite that leaves its prey bleeding. */
    public static final BossMove BITE = register(new BossMove("bite", 10, 6, 10, 40, 0, 6, false, (boss, target, t) -> {
        face(boss, target, t < 10);
        if (t == 0) {
            sound(boss, SoundEvents.EVOKER_FANGS_ATTACK, 1.2f, 0.6f);
        }
        if (t == 10 && target != null) {
            Vec3 lunge = target.position().subtract(boss.position()).normalize();
            boss.setDeltaMovement(lunge.x * 1.1, 0.25, lunge.z * 1.1);
        }
        if (boss.moveIsActive(t) && target != null && boss.distanceTo(target) < 2.4 && boss.firstHit(target)) {
            boss.strike(target, 1.6f, AbilityDamage.SLASH);
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 80, 0));
            sound(boss, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.6f);
            burst(boss, target.getBoundingBox().getCenter(), 0xB02828, 1f, Kind.SPARK, Vfx.FLASH);
        }
    }));

    /** Kiribachi swept through a wide half circle. */
    public static final BossMove SAW_SWEEP = register(new BossMove("saw_sweep", 16, 3, 14, 60, 0, 5.5, false, (boss, target, t) -> {
        face(boss, target, t < 12);
        if (t < 16 && t % 3 == 0) {
            telegraphArc(boss, 5.5, 200, 0xE84A3A);
        }
        if (t == 16) {
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
            slash(boss, 5.5f, 0xC8CCD6);
            for (LivingEntity victim : boss.targetsInArc(5.5, 200)) {
                boss.strike(victim, 1.5f, AbilityDamage.SLASH);
                victim.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 100, 0));
                AbilityDamage.knockback(victim, boss.position(), 0.8, 0.2);
            }
        }
    }));

    /** Shark on Darts: Arlong launches himself nose-first like a torpedo, straight through anything in his path. */
    public static final BossMove SHARK_ON_DARTS = register(new BossMove("shark_on_darts", 22, 18, 26, 200, 6, 24, false,
            (boss, target, t) -> {
                if (t < 22) {
                    boss.getNavigation().stop();
                    if (target != null) {
                        boss.lockDirection(target.getEyePosition().subtract(boss.getEyePosition()));
                        boss.getLookControl().setLookAt(target, 30f, 30f);
                    }
                    if (t % 3 == 0) {
                        trail(boss, boss.getEyePosition(), boss.getEyePosition().add(boss.lockedDirection().scale(12)), 0x5AA6C8);
                    }
                    if (t == 0) {
                        sound(boss, SoundEvents.ELDER_GUARDIAN_CURSE, 1f, 1.6f);
                    }
                    return;
                }
                if (t == 22) {
                    boss.setNoGravity(true);
                    sound(boss, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.6f, 0.8f);
                }
                if (t < 40) {
                    if (boss.horizontalCollision || boss.verticalCollision && t > 24) {
                        boss.setNoGravity(false);
                        burst(boss, boss.position().add(0, 1, 0), 0x5AA6C8, 2f, Kind.DROP, Vfx.HEAVY);
                        boss.skipTo(40);
                        return;
                    }
                    double speed = boss.isInWater() ? 2.1 : 1.6;
                    boss.setDeltaMovement(boss.lockedDirection().scale(speed));
                    burst(boss, boss.position().add(0, 1, 0), 0x8AC8E8, 0.6f, Kind.DROP, 0);
                    for (LivingEntity victim : boss.targetsAround(boss.position().add(0, 1, 0), 2.0)) {
                        if (boss.firstHit(victim)) {
                            boss.strike(victim, 2.4f, AbilityDamage.STRIKE);
                            AbilityDamage.knockback(victim, boss.position(), 2.0, 0.6);
                            sound(boss, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.6f, 0.5f);
                        }
                    }
                }
                if (t == 40) {
                    boss.setNoGravity(false);
                    boss.setDeltaMovement(boss.getDeltaMovement().scale(0.2));
                }
            }));

    /** Tooth Gun: Arlong flicks teeth from his jaw like bullets. */
    public static final BossMove TOOTH_GUN = register(new BossMove("tooth_gun", 12, 9, 10, 90, 4, 20, false, (boss, target, t) -> {
        face(boss, target, true);
        boss.getNavigation().stop();
        if (target != null && t >= 12 && t < 21 && (t - 12) % 3 == 0 && boss.level() instanceof ServerLevel level) {
            Vec3 from = boss.getEyePosition().add(boss.forward().scale(0.8));
            Vec3 aim = target.getEyePosition().subtract(0, 0.4, 0).subtract(from).normalize();
            NpcShot tooth = new NpcShot(level, boss, boss.attackDamage() * 0.6f);
            tooth.setItem(new ItemStack(Items.QUARTZ));
            tooth.setPos(from.x, from.y, from.z);
            tooth.setDeltaMovement(aim.add(boss.getRandom().nextGaussian() * 0.04, boss.getRandom().nextGaussian() * 0.03,
                    boss.getRandom().nextGaussian() * 0.04).scale(1.8));
            level.addFreshEntity(tooth);
            sound(boss, SoundEvents.ARROW_SHOOT, 1.2f, 1.4f);
        }
    }));

    /** In his fury Arlong spins Kiribachi around him, tearing at everything near. */
    public static final BossMove KIRIBACHI_SPIN = register(new BossMove("kiribachi_spin", 12, 50, 18, 150, 0, 9, false,
            (boss, target, t) -> {
                if (t < 12) {
                    face(boss, target, true);
                    boss.getNavigation().stop();
                    return;
                }
                if (t < 62) {
                    if (target != null) {
                        boss.getNavigation().moveTo(target, 0.85);
                    }
                    if (t % 5 == 0) {
                        sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.6f + boss.getRandom().nextFloat() * 0.2f);
                        spiral(boss, 3.6f, 0xB02828);
                        for (LivingEntity victim : boss.targetsAround(boss.position(), 3.6)) {
                            boss.strike(victim, 0.7f, AbilityDamage.SLASH);
                            victim.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 60, 0));
                        }
                    }
                } else {
                    boss.getNavigation().stop();
                }
            }));

    // ---------------------------------------------------------------------------------------------------------
    // Sea Kings
    // ---------------------------------------------------------------------------------------------------------

    /** Jaws wide, the Sea King lunges out of the water at its prey. */
    public static final BossMove SEA_BITE = register(new BossMove("sea_bite", 12, 6, 14, 40, 0, 9, false, (boss, target, t) -> {
        face(boss, target, t < 12);
        if (t == 0) {
            sound(boss, SoundEvents.RAVAGER_ROAR, 1.4f, 0.5f);
        }
        if (t == 12 && target != null) {
            Vec3 lunge = target.position().subtract(boss.position()).normalize();
            boss.setDeltaMovement(lunge.x * 1.4, Math.max(0.3, lunge.y * 1.2 + 0.3), lunge.z * 1.4);
            burst(boss, boss.position().add(0, 1, 0), 0x8AC8E8, 2.5f, Kind.DROP, Vfx.HEAVY);
        }
        if (boss.moveIsActive(t)) {
            Vec3 jaws = boss.position().add(boss.forward().scale(boss.getBbWidth() * 0.8)).add(0, boss.getBbHeight() * 0.5, 0);
            for (LivingEntity victim : boss.targetsAround(jaws, 2.5)) {
                if (boss.firstHit(victim)) {
                    boss.strike(victim, 2.0f, AbilityDamage.SLASH);
                    victim.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 100, 1));
                    sound(boss, SoundEvents.EVOKER_FANGS_ATTACK, 1.6f, 0.4f);
                    burst(boss, victim.getBoundingBox().getCenter(), 0xB02828, 1.4f, Kind.SPARK, Vfx.FLASH);
                }
            }
        }
    }));

    /** The great tail lashed round in a circle, throwing everything near into the sea. */
    public static final BossMove TAIL_SWEEP = register(new BossMove("tail_sweep", 14, 3, 16, 80, 0, 8, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        if (t < 14 && t % 4 == 0) {
            burst(boss, boss.position().add(0, 0.3, 0), 0x5AA6C8, 8f, Kind.RING, Vfx.RING);
        }
        if (t == 14) {
            sound(boss, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2f, 0.5f);
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.4f);
            burst(boss, boss.position().add(0, 0.5, 0), 0x8AC8E8, 8f, Kind.DROP, Vfx.RING | Vfx.SHOCKWAVE | Vfx.HEAVY);
            for (LivingEntity victim : boss.targetsAround(boss.position(), 8)) {
                boss.strike(victim, 1.3f, AbilityDamage.CRUSH);
                AbilityDamage.knockback(victim, boss.position(), 1.6, 0.6);
            }
        }
    }));

    /** A jet of sea water spat with the force of a cannon, along a line the Sea King marks first. */
    public static final BossMove WATER_JET = register(new BossMove("water_jet", 18, 12, 16, 120, 4, 22, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        Vec3 mouth = boss.position().add(boss.forward().scale(boss.getBbWidth() * 0.6)).add(0, boss.getBbHeight() * 0.7, 0);
        if (t < 18) {
            if (target != null) {
                boss.lockDirection(target.getBoundingBox().getCenter().subtract(mouth));
                boss.getLookControl().setLookAt(target, 30f, 30f);
            }
            if (t % 3 == 0) {
                trail(boss, mouth, mouth.add(boss.lockedDirection().scale(20)), 0x5AA6C8);
            }
            if (t == 0) {
                sound(boss, SoundEvents.ELDER_GUARDIAN_AMBIENT, 1.4f, 0.6f);
            }
            return;
        }
        if (t < 30 && t % 2 == 0 && boss.level() instanceof ServerLevel level) {
            Vec3 end = mouth.add(boss.lockedDirection().scale(20));
            Vfx.beam(level, boss, mouth, end, 0x8AD0F0, 1.2f, Kind.DROP, Vfx.BEAM_PARTICLES);
            sound(boss, SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.2f, 1.4f);
            for (LivingEntity victim : boss.targetsAround(mouth.add(boss.lockedDirection().scale(10)), 11)) {
                Vec3 to = victim.getBoundingBox().getCenter().subtract(mouth);
                double along = to.dot(boss.lockedDirection());
                double off = to.subtract(boss.lockedDirection().scale(along)).length();
                if (along > 0 && along < 20 && off < 1.6 && boss.firstHit(victim)) {
                    boss.strike(victim, 1.6f, AbilityDamage.CRUSH);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(boss.lockedDirection().scale(1.4)));
                    victim.hurtMarked = true;
                }
            }
        }
    }));

    /** The Sea King rams the nearest ship, staving in its hull and throwing its crew about. */
    public static final BossMove CAPSIZE = register(new BossMove("capsize", 16, 12, 20, 160, 0, 22, false, (boss, target, t) -> {
        ShipEntity ship = nearestShip(boss, target);
        if (t < 16) {
            boss.getNavigation().stop();
            Vec3 aim = ship != null ? ship.position() : target != null ? target.position() : boss.position().add(boss.forward());
            boss.lockDirection(aim.subtract(boss.position()).multiply(1, 0, 1));
            if (t % 3 == 0) {
                trail(boss, boss.position().add(0, 0.5, 0), boss.position().add(boss.lockedDirection().scale(12)).add(0, 0.5, 0), 0xE84A3A);
            }
            if (t == 0) {
                sound(boss, SoundEvents.RAVAGER_ROAR, 1.6f, 0.4f);
            }
            return;
        }
        if (t < 28) {
            Vec3 dir = boss.lockedDirection();
            boss.setDeltaMovement(dir.x * 1.2, boss.getDeltaMovement().y, dir.z * 1.2);
            if (t % 2 == 0) {
                burst(boss, boss.position().add(0, 0.6, 0), 0xD8F0FF, 1.5f, Kind.DROP, 0);
            }
            // The boss's own mark in the move's hit list stands for the ship, so it is rammed once.
            if (ship != null && boss.distanceTo(ship) < boss.getBbWidth() + 3 && boss.firstHit(boss)) {
                ship.hurt(boss.damageSources().mobAttack(boss), boss.attackDamage() * 6);
                for (var passenger : ship.getPassengers()) {
                    passenger.setDeltaMovement(passenger.getDeltaMovement().add(0, 0.9, 0));
                    passenger.hurtMarked = true;
                }
                sound(boss, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 2f, 0.5f);
                burst(boss, ship.position().add(0, 1, 0), 0x8A6A44, 4f, Kind.SHARD, Vfx.HEAVY | Vfx.SHOCKWAVE);
                boss.skipTo(28);
            }
            for (LivingEntity victim : boss.targetsAround(boss.position().add(boss.lockedDirection().scale(2)), 3)) {
                if (boss.firstHit(victim)) {
                    boss.strike(victim, 1.5f, AbilityDamage.STRIKE);
                    AbilityDamage.knockback(victim, boss.position(), 2.0, 0.8);
                }
            }
        }
    }));

    /** The Sea King sinks out of reach, then bursts up from the deep beneath its prey. */
    public static final BossMove DIVE = register(new BossMove("dive", 10, 30, 18, 200, 4, 24, true, (boss, target, t) -> {
        boss.getNavigation().stop();
        if (t == 0) {
            sound(boss, SoundEvents.PLAYER_SPLASH, 2f, 0.4f);
            burst(boss, boss.position().add(0, 1, 0), 0x8AC8E8, 3f, Kind.DROP, Vfx.RING);
        }
        if (t < 10) {
            boss.setDeltaMovement(0, -0.4, 0);
            return;
        }
        if (t == 30 && target != null) {
            // Slip beneath the prey, as deep as the water allows.
            BlockPos under = target.blockPosition();
            int depth = 0;
            while (depth < 8 && !boss.level().getFluidState(under.below(depth + 1)).isEmpty()) {
                depth++;
            }
            boss.moveTo(target.getX(), target.getY() - Math.max(2, depth), target.getZ(), boss.getYRot(), 0);
            boss.setDeltaMovement(Vec3.ZERO);
        }
        if (t > 30 && t < 38 && target != null && t % 2 == 0) {
            burst(boss, target.position().add(0, 0.2, 0), 0xE84A3A, 3f, Kind.RING, Vfx.RING);
        }
        if (t == 38) {
            boss.setDeltaMovement(0, 1.3, 0);
            sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.6f);
            burst(boss, boss.position().add(0, 2, 0), 0xD8F0FF, 4f, Kind.DROP, Vfx.HEAVY | Vfx.SHOCKWAVE);
            for (LivingEntity victim : boss.targetsAround(boss.position().add(0, 2, 0), 4)) {
                boss.strike(victim, 1.9f, AbilityDamage.CRUSH);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 1.1, 0));
                victim.hurtMarked = true;
            }
        }
    }));

    // ---------------------------------------------------------------------------------------------------------
    // Buggy the Clown
    // ---------------------------------------------------------------------------------------------------------

    /** Three knives thrown in a fan. */
    public static final BossMove KNIFE_THROW = register(new BossMove("knife_throw", 10, 6, 10, 50, 3, 16, false, (boss, target, t) -> {
        face(boss, target, true);
        boss.getNavigation().stop();
        if (target != null && t >= 10 && t < 16 && (t - 10) % 2 == 0 && boss.level() instanceof ServerLevel level) {
            Vec3 from = boss.getEyePosition().add(boss.forward().scale(0.6));
            Vec3 aim = target.getEyePosition().subtract(0, 0.3, 0).subtract(from).normalize();
            double spread = ((t - 10) / 2 - 1) * 0.12;
            Vec3 dir = new Vec3(aim.x * Math.cos(spread) - aim.z * Math.sin(spread), aim.y, aim.x * Math.sin(spread) + aim.z * Math.cos(spread));
            NpcShot knife = new NpcShot(level, boss, boss.attackDamage() * 0.7f);
            knife.setItem(new ItemStack(GLWeapons.BUGGYS_KNIFE.get()));
            knife.setPos(from.x, from.y, from.z);
            knife.setDeltaMovement(dir.scale(1.6));
            level.addFreshEntity(knife);
            sound(boss, SoundEvents.WITCH_THROW, 1.1f, 1.4f);
        }
    }));

    /** Chop-Chop Cannon: Buggy's fist flies off his arm at his prey. */
    public static final BossMove BARA_BARA_CANNON = register(new BossMove("bara_bara_cannon", 14, 4, 12, 70, 3, 18, false,
            (boss, target, t) -> {
                face(boss, target, true);
                boss.getNavigation().stop();
                if (t < 14 && t % 4 == 0 && target != null) {
                    trail(boss, boss.getEyePosition(), target.getEyePosition(), 0xE84A3A);
                }
                if (t == 14 && target != null && boss.level() instanceof ServerLevel level) {
                    Vec3 from = boss.getEyePosition().add(boss.forward().scale(0.8)).subtract(0, 0.3, 0);
                    Vec3 aim = target.getBoundingBox().getCenter().subtract(from).normalize();
                    NpcShot fist = new NpcShot(level, boss, boss.attackDamage() * 1.6f);
                    fist.setItem(new ItemStack(GLBosses.CHOP_FIST.get()));
                    fist.setPos(from.x, from.y, from.z);
                    fist.setDeltaMovement(aim.scale(1.5));
                    level.addFreshEntity(fist);
                    sound(boss, SoundEvents.PLAYER_ATTACK_STRONG, 1.4f, 0.8f);
                    burst(boss, from, 0xF4ECE4, 0.8f, Kind.SHARD, 0);
                }
            }));

    /** Chop-Chop Festival: Buggy flies apart, and his pieces strike from every side while there is nothing to hit. */
    public static final BossMove BARA_BARA_FESTIVAL = register(new BossMove("bara_bara_festival", 16, 40, 14, 260, 0, 14, true,
            (boss, target, t) -> {
                boss.getNavigation().stop();
                if (t == 0) {
                    sound(boss, SoundEvents.ARMOR_STAND_BREAK, 1.6f, 0.7f);
                    boss.shout("Chop-Chop Festival!");
                }
                if (t == 16) {
                    boss.setInvisible(true);
                    burst(boss, boss.position().add(0, 1, 0), 0xF4ECE4, 2.5f, Kind.SHARD, Vfx.HEAVY);
                }
                if (t > 16 && t < 56 && t % 5 == 0 && target != null) {
                    // A piece of Buggy flies in from a random side.
                    double angle = boss.getRandom().nextDouble() * Mth.TWO_PI;
                    Vec3 hit = target.getBoundingBox().getCenter();
                    Vec3 from = hit.add(Math.cos(angle) * 4, 1 + boss.getRandom().nextDouble(), Math.sin(angle) * 4);
                    trail(boss, from, hit, 0xF4ECE4);
                    for (LivingEntity victim : boss.targetsAround(hit, 1.5)) {
                        boss.strike(victim, 0.6f, AbilityDamage.STRIKE);
                        AbilityDamage.knockback(victim, from, 0.4, 0.15);
                    }
                    sound(boss, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 1.3f);
                }
                if (t == 56) {
                    boss.setInvisible(false);
                    burst(boss, boss.position().add(0, 1, 0), 0xF4ECE4, 2f, Kind.SHARD, 0);
                }
            }));

    /** Buggy Ball: a cannonball the size of a man, fired at the spot where his prey stood. */
    public static final BossMove BUGGY_BALL = register(new BossMove("buggy_ball", 26, 2, 18, 220, 6, 24, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        face(boss, target, t < 20);
        if (t == 0) {
            sound(boss, SoundEvents.ANVIL_PLACE, 1.2f, 0.6f);
            boss.shout("Buggy Ball! Fire!");
        }
        if (t < 26) {
            if (target != null && t < 20) {
                boss.lockSpot(target.position());
            }
            if (t % 4 == 0) {
                burst(boss, boss.lockedSpot().add(0, 0.1, 0), 0xE84A3A, 4.5f, Kind.RING, Vfx.RING);
            }
            return;
        }
        if (t == 26 && boss.level() instanceof ServerLevel level) {
            Vec3 spot = boss.lockedSpot();
            Vfx.trail(level, boss, boss.getEyePosition(), spot.add(0, 0.5, 0), 0x2A2A2A, 1f, Kind.SMOKE);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.7f);
            sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.2f);
            burst(boss, spot.add(0, 0.6, 0), 0xF0A040, 4.5f, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.HEAVY | Vfx.GROUND);
            burst(boss, spot.add(0, 1, 0), 0x404040, 3f, Kind.SMOKE, 0);
            for (LivingEntity victim : boss.targetsAround(spot, 4.5)) {
                float falloff = (float) Math.max(0.35, 1 - victim.position().distanceTo(spot) / 5);
                boss.strike(victim, 2.2f * falloff, AbilityDamage.BLAST);
                AbilityDamage.knockback(victim, spot, 1.2, 0.7);
            }
        }
    }));

    // ---------------------------------------------------------------------------------------------------------
    // Captain Kuro
    // ---------------------------------------------------------------------------------------------------------

    /** A quick rake of the cat claws. */
    public static final BossMove CLAW_RAKE = register(new BossMove("claw_rake", 8, 3, 8, 30, 0, 4, false, (boss, target, t) -> {
        face(boss, target, true);
        if (t == 8) {
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.5f);
            slash(boss, 3.5f, 0xE8ECF4);
            for (LivingEntity victim : boss.targetsInArc(3.6, 110)) {
                boss.strike(victim, 1.2f, AbilityDamage.SLASH);
                victim.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 60, 0));
            }
        }
    }));

    /** Silent Step: Kuro vanishes and is behind his prey before they hear him move, claws already falling. */
    public static final BossMove SILENT_STEP = register(new BossMove("silent_step", 6, 4, 10, 80, 3, 14, false, (boss, target, t) -> {
        if (t == 6 && target != null) {
            Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(1.8));
            burst(boss, boss.position().add(0, 1, 0), 0x202028, 1f, Kind.SMOKE, 0);
            boss.teleportTo(behind.x, target.getY(), behind.z);
            boss.getLookControl().setLookAt(target, 180f, 180f);
            boss.setYRot((float) (Mth.atan2(target.getZ() - behind.z, target.getX() - behind.x) * Mth.RAD_TO_DEG) - 90f);
            boss.yBodyRot = boss.getYRot();
            boss.yHeadRot = boss.getYRot();
            burst(boss, boss.position().add(0, 1, 0), 0x202028, 1f, Kind.SMOKE, 0);
            sound(boss, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
        }
        if (t == 7 && target != null && boss.distanceTo(target) < 3.5) {
            boss.strike(target, 1.5f, AbilityDamage.SLASH);
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 80, 0));
            slash(boss, 3f, 0xE8ECF4);
            sound(boss, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.4f);
        }
    }));

    /** Out of Bounds (Shakushi): Kuro moves too fast to see and cuts everything around him, his own crew included. */
    public static final BossMove SHAKUSHI = register(new BossMove("shakushi", 20, 50, 24, 260, 0, 12, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        if (t == 0) {
            boss.lockSpot(boss.position());
            boss.shout("Out of Bounds...");
            sound(boss, SoundEvents.ELDER_GUARDIAN_CURSE, 0.8f, 1.6f);
        }
        if (t < 20) {
            if (t % 4 == 0) {
                burst(boss, boss.lockedSpot().add(0, 0.1, 0), 0xE84A3A, 7f, Kind.RING, Vfx.RING);
            }
            return;
        }
        if (t < 70 && t % 3 == 0) {
            Vec3 centre = boss.lockedSpot();
            double angle = boss.getRandom().nextDouble() * Mth.TWO_PI;
            double r = boss.getRandom().nextDouble() * 6;
            Vec3 to = new Vec3(centre.x + Math.cos(angle) * r, centre.y, centre.z + Math.sin(angle) * r);
            trail(boss, boss.position().add(0, 1, 0), to.add(0, 1, 0), 0x303038);
            boss.teleportTo(to.x, centre.y, to.z);
            boss.setYRot((float) (angle * Mth.RAD_TO_DEG));
            for (LivingEntity victim : boss.everyoneAround(to, 2.5)) {
                boss.strike(victim, 0.45f, AbilityDamage.SLASH);
            }
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.6f + boss.getRandom().nextFloat() * 0.3f);
        }
    }));

    // ---------------------------------------------------------------------------------------------------------
    // Don Krieg
    // ---------------------------------------------------------------------------------------------------------

    /** Damashi Uchi: Krieg throws open his coat and the guns hidden in his armour fire a volley. */
    public static final BossMove DAMASHI_UCHI = register(new BossMove("damashi_uchi", 12, 10, 10, 60, 0, 20, false,
            (boss, target, t) -> {
                face(boss, target, true);
                boss.getNavigation().stop();
                if (t == 0) {
                    sound(boss, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2f, 0.6f);
                }
                if (target != null && t >= 12 && t < 22 && (t - 12) % 2 == 0 && boss.level() instanceof ServerLevel level) {
                    Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.6, 0).add(boss.forward().scale(0.7));
                    Vec3 aim = target.getBoundingBox().getCenter().subtract(from).normalize();
                    NpcShot shot = new NpcShot(level, boss, boss.attackDamage() * 0.45f);
                    shot.setPos(from.x, from.y, from.z);
                    shot.setDeltaMovement(aim.add(boss.getRandom().nextGaussian() * 0.05, boss.getRandom().nextGaussian() * 0.04,
                            boss.getRandom().nextGaussian() * 0.05).scale(2.2));
                    level.addFreshEntity(shot);
                    sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 0.4f, 1.8f);
                    burst(boss, from, 0xF0C060, 0.5f, Kind.SPARK, 0);
                }
            }));

    /** Daisensou: a charging thrust of the great spear, whose tip blows up in whatever it meets. */
    public static final BossMove DAISENSOU = register(new BossMove("daisensou", 16, 8, 14, 100, 3, 11, false, (boss, target, t) -> {
        if (t < 16) {
            boss.getNavigation().stop();
            if (target != null) {
                boss.lockDirection(target.position().subtract(boss.position()).multiply(1, 0, 1));
                boss.getLookControl().setLookAt(target, 30f, 30f);
            }
            if (t == 0) {
                sound(boss, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.4f, 0.5f);
            }
            if (t % 3 == 0) {
                trail(boss, boss.position().add(0, 0.1, 0), boss.position().add(boss.lockedDirection().scale(8)).add(0, 0.1, 0), 0xF0A040);
            }
            return;
        }
        if (t < 24) {
            Vec3 dir = boss.lockedDirection();
            boss.setDeltaMovement(dir.x * 0.9, boss.getDeltaMovement().y, dir.z * 0.9);
            Vec3 tip = boss.position().add(dir.scale(2.2)).add(0, 1.1, 0);
            if (t == 23 || boss.horizontalCollision || !boss.targetsAround(tip, 1.4).isEmpty()) {
                sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.9f);
                burst(boss, tip, 0xF0A040, 3f, Kind.FLAME, Vfx.FLASH | Vfx.SHOCKWAVE | Vfx.HEAVY);
                burst(boss, tip, 0x505050, 2f, Kind.SMOKE, 0);
                for (LivingEntity victim : boss.targetsAround(tip, 3)) {
                    boss.strike(victim, 1.8f, AbilityDamage.BLAST);
                    AbilityDamage.knockback(victim, tip, 1.3, 0.5);
                }
                boss.setDeltaMovement(Vec3.ZERO);
                boss.skipTo(24);
            }
        }
    }));

    /** MH5: Krieg puts on his gas mask and hurls a poison-gas shell; the cloud lingers where it bursts. */
    public static final BossMove MH5 = register(new BossMove("mh5", 24, 2, 16, 400, 4, 20, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        face(boss, target, t < 18);
        if (t == 0) {
            boss.addTag(BossEntity.GAS_MASK);
            boss.shout("MH5! Breathe it in!");
            sound(boss, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.2f, 0.8f);
        }
        if (t < 24) {
            if (target != null && t < 18) {
                boss.lockSpot(target.position());
            }
            if (t % 4 == 0) {
                burst(boss, boss.lockedSpot().add(0, 0.1, 0), 0x7AC83A, 5f, Kind.RING, Vfx.RING);
            }
            return;
        }
        if (t == 24 && boss.level() instanceof ServerLevel level) {
            Vec3 spot = boss.lockedSpot();
            Vfx.trail(level, boss, boss.getEyePosition(), spot.add(0, 0.5, 0), 0x6AA83A, 1f, Kind.SMOKE);
            AreaEffectCloud cloud = new AreaEffectCloud(level, spot.x, spot.y, spot.z);
            cloud.setOwner(boss);
            cloud.setRadius(5f);
            cloud.setRadiusPerTick(0f);
            cloud.setWaitTime(0);
            cloud.setDuration(160);
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            level.addFreshEntity(cloud);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
            burst(boss, spot.add(0, 1, 0), 0x7AC83A, 4f, Kind.SMOKE, Vfx.HEAVY);
        }
    }));

    /** The spiked cape: Krieg wraps himself in it; blows barely reach him and the spikes bite whoever strikes. */
    public static final BossMove PORCUPINE_CAPE = register(new BossMove("porcupine_cape", 6, 40, 10, 220, 0, 6, false,
            (boss, target, t) -> {
                boss.getNavigation().stop();
                face(boss, target, true);
                if (t == 0) {
                    sound(boss, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.4f, 0.5f);
                }
                if (t >= 6 && t < 46 && t % 8 == 0) {
                    burst(boss, boss.position().add(0, 1.2, 0), 0x8A8C94, 1.6f, Kind.SHARD, 0);
                }
            }));

    // ---------------------------------------------------------------------------------------------------------
    // Captain Smoker
    // ---------------------------------------------------------------------------------------------------------

    /** White Blow: Smoker's fist turns to smoke and shoots out to seize his prey and drag them to him. */
    public static final BossMove WHITE_BLOW = register(new BossMove("white_blow", 12, 6, 12, 80, 4, 16, false, (boss, target, t) -> {
        face(boss, target, true);
        boss.getNavigation().stop();
        if (t < 12) {
            if (t % 3 == 0 && target != null) {
                smoke(boss, boss.getEyePosition(), target.getBoundingBox().getCenter());
            }
            return;
        }
        if (t == 12 && target != null && boss.distanceTo(target) <= 17 && boss.hasLineOfSight(target)) {
            smoke(boss, boss.getEyePosition(), target.getBoundingBox().getCenter());
            boss.strike(target, 0.9f, AbilityDamage.STRIKE);
            Vec3 pull = boss.position().subtract(target.position()).multiply(1, 0, 1);
            if (pull.lengthSqr() > 1.0E-4) {
                pull = pull.normalize().scale(Math.min(1.6, pull.length() * 0.2));
                target.setDeltaMovement(pull.x, 0.35, pull.z);
                target.hurtMarked = true;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            sound(boss, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f);
            burst(boss, target.getBoundingBox().getCenter(), 0xE8E8E8, 1.4f, Kind.SMOKE, 0);
        }
    }));

    /** White Launcher: Smoker becomes a jet of smoke and blasts through everything in a line. */
    public static final BossMove WHITE_LAUNCHER = register(new BossMove("white_launcher", 10, 8, 12, 90, 3, 12, false,
            (boss, target, t) -> {
                if (t < 10) {
                    boss.getNavigation().stop();
                    if (target != null) {
                        boss.lockDirection(target.position().subtract(boss.position()).multiply(1, 0, 1));
                        boss.getLookControl().setLookAt(target, 30f, 30f);
                    }
                    if (t % 3 == 0) {
                        trail(boss, boss.position().add(0, 0.1, 0), boss.position().add(boss.lockedDirection().scale(12)).add(0, 0.1, 0),
                                0xE8E8E8);
                    }
                    return;
                }
                if (t < 18) {
                    Vec3 dir = boss.lockedDirection();
                    boss.setDeltaMovement(dir.x * 1.3, boss.getDeltaMovement().y, dir.z * 1.3);
                    burst(boss, boss.position().add(0, 1, 0), 0xE8E8E8, 1f, Kind.SMOKE, 0);
                    for (LivingEntity victim : boss.targetsAround(boss.position().add(dir.scale(1.2)), 1.8)) {
                        if (boss.firstHit(victim)) {
                            boss.strike(victim, 1.3f, AbilityDamage.STRIKE);
                            AbilityDamage.knockback(victim, boss.position(), 1.4, 0.4);
                            sound(boss, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f);
                        }
                    }
                }
            }));

    /** White Out: smoke pours from Smoker and binds everyone around him, choking and blinding them. */
    public static final BossMove WHITE_OUT = register(new BossMove("white_out", 16, 40, 14, 240, 0, 8, false, (boss, target, t) -> {
        boss.getNavigation().stop();
        if (t == 0) {
            boss.shout("White Out!");
            sound(boss, SoundEvents.FIRE_EXTINGUISH, 1.6f, 0.5f);
        }
        if (t < 16) {
            if (t % 4 == 0) {
                burst(boss, boss.position().add(0, 0.1, 0), 0xE8E8E8, 6f, Kind.RING, Vfx.RING);
            }
            return;
        }
        if (t < 56 && t % 10 == 6) {
            burst(boss, boss.position().add(0, 1, 0), 0xE0E0E0, 6f, Kind.SMOKE, Vfx.HEAVY);
            for (LivingEntity victim : boss.targetsAround(boss.position(), 6)) {
                boss.strike(victim, 0.3f, AbilityDamage.STRIKE);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
            sound(boss, SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.6f);
        }
    }));

    /** A blow of the Seastone jitte: it seals a Devil Fruit user's power for a while. */
    public static final BossMove JITTE_STRIKE = register(new BossMove("jitte_strike", 8, 3, 10, 50, 0, 4, false, (boss, target, t) -> {
        face(boss, target, true);
        if (t == 8) {
            sound(boss, SoundEvents.PLAYER_ATTACK_STRONG, 1.3f, 0.7f);
            slash(boss, 3.2f, 0x6AB8B0);
            for (LivingEntity victim : boss.targetsInArc(3.6, 100)) {
                boss.strike(victim, 1.3f, AbilityDamage.STRIKE);
                victim.addEffect(new MobEffectInstance(GLMobEffects.FRUIT_SEALED, 100, 0, false, true, true));
                if (boss.level() instanceof ServerLevel level) {
                    Vfx.status(level, victim, Vfx.StatusLook.SEASTONE, 100);
                }
            }
        }
    }));

    private BossMoves() {}

    /** The ship to ram: the one the prey is aboard, else the nearest within reach. */
    @Nullable
    private static ShipEntity nearestShip(BossEntity boss, @Nullable LivingEntity target) {
        if (target != null && target.getVehicle() instanceof ShipEntity ship) {
            return ship;
        }
        return boss.level().getEntitiesOfClass(ShipEntity.class, boss.getBoundingBox().inflate(22)).stream()
                .min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
    }

    private static BossMove register(BossMove move) {
        MOVES.put(move.id(), move);
        return move;
    }

    @Nullable
    public static BossMove get(String id) {
        return MOVES.get(id);
    }

    public static Collection<BossMove> all() {
        return Collections.unmodifiableCollection(MOVES.values());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------

    private static void face(BossEntity boss, @Nullable LivingEntity target, boolean track) {
        if (target != null && track) {
            boss.getLookControl().setLookAt(target, 30f, 30f);
            boss.setYRot(boss.yHeadRot);
            boss.yBodyRot = boss.yHeadRot;
        }
    }

    private static void sound(BossEntity boss, SoundEvent sound, float volume, float pitch) {
        boss.level().playSound(null, boss.getX(), boss.getY(), boss.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void burst(BossEntity boss, Vec3 at, int color, float radius, Kind kind, int flags) {
        if (boss.level() instanceof ServerLevel level) {
            Vfx.burst(level, boss, at, color, radius, kind, flags);
        }
    }

    private static void trail(BossEntity boss, Vec3 from, Vec3 to, int color) {
        if (boss.level() instanceof ServerLevel level) {
            Vfx.trail(level, boss, from, to, color, 0.5f, Kind.SPARK);
        }
    }

    /** A drifting stream of smoke from {@code from} to {@code to}. */
    private static void smoke(BossEntity boss, Vec3 from, Vec3 to) {
        if (boss.level() instanceof ServerLevel level) {
            Vfx.trail(level, boss, from, to, 0xE8E8E8, 0.9f, Kind.SMOKE);
        }
    }

    private static void slash(BossEntity boss, float size, int color) {
        if (boss.level() instanceof ServerLevel level) {
            Vfx.slash(level, boss, boss.position().add(0, boss.getBbHeight() * 0.55, 0).add(boss.forward().scale(size * 0.4)), boss.forward(),
                    color, size, 3);
        }
    }

    private static void spiral(BossEntity boss, float radius, int color) {
        if (boss.level() instanceof ServerLevel level) {
            Vfx.spiral(level, boss, boss.position().add(0, 1, 0), color, radius, Kind.STREAK);
        }
    }

    /** Warning sparks along the edge of the arc a swing will cover. */
    private static void telegraphArc(BossEntity boss, double radius, double degrees, int color) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        double facing = Math.atan2(boss.forward().z, boss.forward().x);
        double half = Math.toRadians(degrees / 2);
        for (int i = 0; i <= 8; i++) {
            double a = facing - half + (2 * half) * i / 8;
            Vec3 at = boss.position().add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius);
            Vfx.burst(level, boss, at, color, 0.25f, Kind.SPARK, 0);
        }
    }
}
