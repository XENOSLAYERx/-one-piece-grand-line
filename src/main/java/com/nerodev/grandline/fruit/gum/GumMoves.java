package com.nerodev.grandline.fruit.gum;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The Gum-Gum Fruit's moves. Everything is stretch-and-snap: long-reach strikes, slingshot movement, a body that
 * can be pumped up (Gear Second) or inflated (Gear Third). Awakening (Gear Fifth) turns the ground itself to rubber.
 * <p>
 * Stretch visuals: {@link GLEffects#STRETCH} with pos = fist end point, dir = second end point (both-arm moves),
 * scale = fist size, variant = limb (0 right, 1 left, 2 both, 3 giant right), color = duration in ticks.
 */
public final class GumMoves {
    private static final String M = GumAbilities.MASTERY;
    private static final int C = GumAbilities.COLOR;

    static final int RIGHT = 0;
    static final int LEFT = 1;
    static final int BOTH = 2;
    static final int GIANT = 3;

    private GumMoves() {}

    private static Ability.Properties props(ActivationType type, AbilityIcon icon) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, C);
    }

    static boolean gearSecond(ServerPlayer player) {
        return TransformationManager.isActive(player, GLTransformations.GEAR_SECOND.get());
    }

    static boolean gearFifth(ServerPlayer player) {
        return TransformationManager.isActive(player, GLTransformations.GEAR_FIFTH.get());
    }

    /** Fist scale: Gear Fifth fists are cartoonishly big. */
    static float fist(ServerPlayer player) {
        return gearFifth(player) ? 1.8f : 1f;
    }

    static void stretch(AbilityContext ctx, Vec3 end, Vec3 second, float fistScale, int limb, int duration) {
        ctx.fx(GLEffects.STRETCH, new FxParams(end, second, fistScale, duration, limb));
    }

    static void punchHit(ServerPlayer player, LivingEntity target, float damage, double knockback, double lift) {
        target.invulnerableTime = 0;
        if (AbilityDamage.hurt(player, target, AbilityDamage.STRIKE, damage, player)) {
            AbilityDamage.knockback(target, player.position(), knockback, lift);
        }
    }

    static void impact(ServerLevel level, Entity source, Vec3 at, float scale) {
        Fx.play(level, GLEffects.PUNCH_IMPACT, source, FxParams.at(at).color(0xFFFFFF).scale(scale));
        level.playSound(null, at.x, at.y, at.z, GLSounds.PUNCH_HEAVY.get(), SoundSource.PLAYERS, 0.6f + scale * 0.3f,
                1.2f - Math.min(0.6f, scale * 0.12f));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Gum-Gum Pistol: wind up and fire a stretching punch up to 14 blocks. In Gear Second it becomes Jet Pistol. */
    public static final class Pistol extends Ability {
        static final double RANGE = 14;

        Pistol() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST).cooldown(22).stamina(9).masteryGain(0.6));
        }

        @Override
        public int cooldownFor(ServerPlayer player) {
            return gearSecond(player) ? 10 : super.cooldownFor(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            boolean jet = gearSecond(player);
            Optional<LivingEntity> hit = AbilityDamage.raycast(player, RANGE, 0.5);
            Vec3 end = hit.map(e -> e.getBoundingBox().getCenter()).orElseGet(() -> AbilityDamage.lookTarget(player, RANGE));
            float damage = 7f * ctx.power() * (jet ? 1.5f : 1f) * (gearFifth(player) ? 1.4f : 1f);
            hit.ifPresent(target -> {
                punchHit(player, target, damage, jet ? 1.7 : 1.1, 0.3);
                impact(player.serverLevel(), player, end, jet ? 1.3f : 1f);
            });
            stretch(ctx, end, Vec3.ZERO, fist(player), RIGHT, jet ? 6 : 9);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.RUBBER_STRETCH.get(), 0.9f, jet ? 1.5f : 1.0f);
        }
    }

    /** Gum-Gum Rocket: grab a far block with both arms and slingshot yourself to it. */
    public static final class Rocket extends Ability {
        static final double RANGE = 26;

        Rocket() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH).cooldown(50).stamina(14).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            Vec3 target = AbilityDamage.lookTarget(player, RANGE);
            return target.distanceTo(player.getEyePosition()) >= RANGE - 0.1
                    ? Optional.of(Component.literal("Nothing in reach to grab"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 anchor = AbilityDamage.lookTarget(player, RANGE);
            Vec3 delta = anchor.subtract(player.getEyePosition());
            double distance = delta.length();
            Vec3 velocity = delta.normalize().scale(Math.min(2.6, 1.0 + distance * 0.07)).add(0, 0.35, 0);
            player.setDeltaMovement(velocity);
            player.resetFallDistance();
            player.hurtMarked = true;
            Vec3 side = ctx.right().scale(0.4);
            stretch(ctx, anchor.add(side), anchor.subtract(side), 1f, BOTH, 10);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.RUBBER_SNAP.get(), 1.0f, 0.9f);
        }
    }

    /** Gum-Gum Gatling: a storm of stretching punches in front of you while held. */
    public static final class Gatling extends Ability {
        Gatling() {
            super(props(ActivationType.HOLD, AbilityIcon.BURST).cooldown(70).stamina(8).staminaPerTick(0.5f)
                    .maxActive(40).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_GATLING);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 2 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            float damage = 2.2f * ctx.power() * (gearSecond(player) ? 1.4f : 1f) * (gearFifth(player) ? 1.4f : 1f);
            for (LivingEntity target : AbilityDamage.cone(player, 7, 32)) {
                punchHit(player, target, damage, 0.15, 0.05);
            }
            Vec3 eye = player.getEyePosition();
            for (int i = 0; i < 2; i++) {
                float yaw = player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * 40f;
                float pitch = player.getXRot() + (player.getRandom().nextFloat() - 0.5f) * 30f;
                Vec3 end = eye.add(Vec3.directionFromRotation(pitch, yaw).scale(4.5 + player.getRandom().nextDouble() * 2));
                stretch(ctx, end, Vec3.ZERO, fist(player), (ctx.activeTicks() / 2 + i) % 2 == 0 ? RIGHT : LEFT, 4);
            }
            if (ctx.activeTicks() % 4 == 0) {
                ctx.sound(GLSounds.GATLING.get(), 0.7f, 0.9f + player.getRandom().nextFloat() * 0.3f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Gum-Gum Bazooka: pull both arms far back, then slam both palms forward for huge knockback. */
    public static final class Bazooka extends Ability {
        static final int FULL_CHARGE = 20;

        Bazooka() {
            super(props(ActivationType.CHARGE, AbilityIcon.WAVE).cooldown(110).stamina(22).maxActive(30).minCharge(8)
                    .mastery(M, 20).masteryGain(1.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(GLSounds.RUBBER_STRETCH.get(), 0.8f, 0.6f);
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float charge = Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE);
            float damage = 9f * ctx.power() * (0.6f + 0.4f * charge);
            List<LivingEntity> targets = AbilityDamage.cone(player, 6, 30);
            for (LivingEntity target : targets) {
                punchHit(player, target, damage, 2.4 * (0.5 + 0.5 * charge), 0.6);
            }
            Vec3 ahead = player.getEyePosition().add(player.getLookAngle().scale(4));
            Vec3 side = ctx.right().scale(0.35);
            stretch(ctx, ahead.add(side), ahead.subtract(side), 1.3f * fist(player), BOTH, 8);
            impact(player.serverLevel(), player, ahead, 1.5f + charge);
            ctx.animate(GLEffects.ANIM_PALMS);
        }
    }

    /** Gear Second: pump blood through rubber veins. Faster, harder hits; Pistol becomes Jet Pistol. */
    public static final class GearSecond extends Ability {
        GearSecond() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM).cooldown(200).stamina(18).staminaPerTick(0.2f)
                    .maxActive(20 * 90).mastery(M, 30).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            TransformationManager.start(ctx.player(), GLTransformations.GEAR_SECOND.get());
            ctx.fx(GLEffects.GEAR_STEAM, FxParams.at(ctx.player().position()).color(0xFF9EA8).variant(0));
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(GLSounds.STEAM.get(), 1.0f, 1.0f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return gearSecond(ctx.player());
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            if (gearSecond(ctx.player())) {
                TransformationManager.end(ctx.player());
            }
        }
    }

    /** Gear Third: Giant Pistol. Inflate your arm to giant size, then punch through everything in a line. */
    public static final class GiantPistol extends Ability {
        static final int FULL_CHARGE = 30;
        static final double RANGE = 20;

        GiantPistol() {
            super(props(ActivationType.CHARGE, AbilityIcon.FIST).cooldown(280).stamina(35).maxActive(40).minCharge(14)
                    .mastery(M, 50).masteryGain(2.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.INFLATE.get(), 1.0f, 0.9f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 2 == 0) {
                float charge = Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE);
                ctx.fx(GLEffects.INFLATE, FxParams.at(ctx.handPos()).scale(1f + 4f * charge));
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float charge = Math.min(1f, ctx.activeTicks() / (float) FULL_CHARGE);
            float damage = 20f * ctx.power() * (0.6f + 0.4f * charge);
            float fist = 3f + 3f * charge;
            List<LivingEntity> targets = AbilityDamage.line(player, RANGE, 0.6 + fist * 0.25);
            for (LivingEntity target : targets) {
                punchHit(player, target, damage, 3.0, 0.8);
            }
            Vec3 end = targets.isEmpty() ? AbilityDamage.lookTarget(player, RANGE) : targets.getLast().getBoundingBox().getCenter();
            stretch(ctx, end, Vec3.ZERO, fist, GIANT, 16);
            impact(player.serverLevel(), player, end, 2.5f + 2f * charge);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.5f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Gear Fifth: the awakened rubber body. Everything nearby bounces: the ground turns to rubber around you. */
    public static final class GearFifth extends Ability {
        static final double RADIUS = 7;

        GearFifth() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD).cooldown(600).stamina(25).staminaPerTick(0.25f)
                    .maxActive(20 * 90).masteryGain(2.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            TransformationManager.start(ctx.player(), GLTransformations.GEAR_FIFTH.get());
            ctx.fx(GLEffects.GEAR_STEAM, FxParams.at(ctx.player().position()).color(0xFFFFFF).variant(2));
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(GLSounds.BOUNCE.get(), 1.4f, 0.7f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!gearFifth(player)) {
                return false;
            }
            if (ctx.activeTicks() % 10 == 0) {
                boolean any = false;
                for (LivingEntity target : AbilityDamage.around(player, player.position(), RADIUS)) {
                    if (target.onGround()) {
                        target.invulnerableTime = 0;
                        AbilityDamage.hurt(player, target, AbilityDamage.STRIKE, 1.5f * ctx.power(), player);
                        target.setDeltaMovement(target.getDeltaMovement().add(0, 0.9, 0));
                        target.hurtMarked = true;
                        any = true;
                    }
                }
                if (any || ctx.activeTicks() % 40 == 0) {
                    ctx.fx(GLEffects.RUBBER_WORLD, FxParams.at(player.position()).scale((float) RADIUS));
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            if (gearFifth(ctx.player())) {
                TransformationManager.end(ctx.player());
            }
        }
    }

    /** Dawn Fist: in Gear Fifth, a fist the size of a house drops from the sky onto the target. */
    public static final class DawnFist extends Ability {
        static final double RANGE = 32;
        static final int FALL_TICKS = 12;

        DawnFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST).cooldown(360).stamina(40).masteryGain(3.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return gearFifth(player) ? Optional.empty() : Optional.of(Component.literal("Requires Gear Fifth"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 target = groundBelow(player.serverLevel(), AbilityDamage.lookTarget(player, RANGE));
            AbilityProjectile.spawn(player, GumAbilities.DAWN_FIST_IMPACT, target, Vec3.ZERO, 6f, 0xFFFFFF,
                    24f * ctx.power(), FALL_TICKS + 2);
            ctx.fx(GLEffects.DAWN_FIST, FxParams.at(target).scale(12f).color(FALL_TICKS));
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.INFLATE.get(), 1.4f, 0.6f);
        }

        static Vec3 groundBelow(ServerLevel level, Vec3 from) {
            BlockPos pos = BlockPos.containing(from);
            for (int i = 0; i < 12; i++) {
                if (!level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                    return new Vec3(from.x, pos.getY(), from.z);
                }
                pos = pos.below();
            }
            return from;
        }
    }

    public static final class DawnFistImpact implements ProjectileBehavior {
        @Override
        public void tick(AbilityProjectile p) {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount != DawnFist.FALL_TICKS) {
                return;
            }
            Vec3 at = p.position();
            for (LivingEntity target : AbilityDamage.around(owner, at, p.size())) {
                target.invulnerableTime = 0;
                if (AbilityDamage.hurt(owner, target, AbilityDamage.STRIKE, p.damage(), p)) {
                    AbilityDamage.knockback(target, at, 2.0, 0.9);
                }
            }
            impact((ServerLevel) p.level(), p, at.add(0, 0.5, 0), 5f);
            Fx.play((ServerLevel) p.level(), GLEffects.RUBBER_WORLD, p, FxParams.at(at).scale(p.size() + 3));
        }

        @Override
        public boolean onHitEntity(AbilityProjectile p, Entity target) {
            return true;
        }

        @Override
        public boolean hitsBlocks() {
            return false;
        }
    }
}
