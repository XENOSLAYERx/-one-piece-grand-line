package com.nerodev.grandline.fruit.leopard;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The Leopard's moves. Forms change the body (hybrid: bigger, armoured, clawed; beast: low, very fast, fits through
 * one-block gaps). Most attacks need a form. Awakening unlocks a berserk state and an intimidating roar.
 */
public final class LeopardMoves {
    private static final String M = LeopardAbilities.MASTERY;
    private static final int C = LeopardAbilities.COLOR;

    private LeopardMoves() {}

    private static Ability.Properties props(ActivationType type, AbilityIcon icon) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, C);
    }

    static boolean hybrid(ServerPlayer player) {
        return TransformationManager.isActive(player, GLTransformations.LEOPARD_HYBRID.get());
    }

    static boolean beast(ServerPlayer player) {
        return TransformationManager.isActive(player, GLTransformations.LEOPARD_BEAST.get());
    }

    static Optional<Component> needsForm(ServerPlayer player) {
        return hybrid(player) || beast(player) ? Optional.empty() : Optional.of(Component.literal("Transform first"));
    }

    static void maul(ServerPlayer player, LivingEntity target, float damage, int bleedSeconds, int bleedLevel) {
        target.invulnerableTime = 0;
        if (AbilityDamage.hurt(player, target, AbilityDamage.SLASH, damage, player)) {
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, bleedSeconds * 20, bleedLevel));
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hybrid (human-leopard) or full leopard form. Toggling one while in the other switches directly. */
    public static final class Form extends Ability {
        private final boolean hybridForm;

        Form(boolean hybridForm) {
            super(props(ActivationType.TOGGLE, hybridForm ? AbilityIcon.CLAW : AbilityIcon.TRANSFORM)
                    .cooldown(30).stamina(10).staminaPerTick(0.04f).maxActive(20 * 60 * 30).masteryGain(0.4));
            this.hybridForm = hybridForm;
        }

        private Transformation form() {
            return hybridForm ? GLTransformations.LEOPARD_HYBRID.get() : GLTransformations.LEOPARD_BEAST.get();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            TransformationManager.start(player, form());
            ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(C).scale(hybridForm ? 1.2f : 1f));
            ctx.sound(GLSounds.LEOPARD_GROWL.get(), 1.0f, hybridForm ? 0.9f : 1.1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return TransformationManager.isActive(ctx.player(), form());
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            if (TransformationManager.isActive(player, form())) {
                TransformationManager.end(player);
                ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(0xD8D0C0).scale(0.8f));
            }
        }
    }

    /** Leap at your prey; the first enemy you collide with is mauled and slowed. */
    public static final class Pounce extends Ability {
        Pounce() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH).cooldown(45).stamina(16).maxActive(14).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            double speed = beast(player) ? 1.7 : 1.4;
            player.setDeltaMovement(look.x * speed, Math.max(0.42, look.y * speed + 0.3), look.z * speed);
            player.hurtMarked = true;
            ctx.fx(GLEffects.POUNCE, FxParams.of(player.position(), look).color(C));
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(GLSounds.LEOPARD_GROWL.get(), 0.8f, 1.3f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.getBoundingBox().getCenter(), 1.3)) {
                float damage = 8f * ctx.power() * (beast(player) ? 1.25f : 1f);
                maul(player, target, damage, 3, 0);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                AbilityDamage.knockback(target, player.position(), 0.6, 0.2);
                ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(target.getBoundingBox().getCenter(), player.getLookAngle()).color(0xFFE6B0));
                ctx.sound(GLSounds.CLAW.get(), 1.0f, 0.9f);
                return false;
            }
            return !(ctx.activeTicks() > 3 && player.onGround());
        }
    }

    /** Three raking slashes in front of you. Leaves deep wounds that keep bleeding. */
    public static final class Claws extends Ability {
        Claws() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW).cooldown(18).stamina(8).mastery(M, 10).masteryGain(0.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8, 55)) {
                maul(player, target, 6f * ctx.power(), 4, 0);
            }
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.6)).add(0, -0.3, 0);
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(front, player.getLookAngle()).color(0xFFE6B0).variant(0));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.CLAW.get(), 1.0f, 1.0f + player.getRandom().nextFloat() * 0.2f);
        }
    }

    /** Hold to become a whirlwind of claws, cutting everything around you. Hybrid form only. */
    public static final class Frenzy extends Ability {
        Frenzy() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL).cooldown(160).stamina(15).staminaPerTick(0.6f)
                    .maxActive(50).mastery(M, 40).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return hybrid(player) ? Optional.empty() : Optional.of(Component.literal("Requires hybrid form"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FRENZY);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 3.2)) {
                maul(player, target, 3f * ctx.power(), 3, 1);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.at(player.position().add(0, 1, 0)).color(0xFFE6B0).variant(1)
                    .scale(3.2f));
            if (ctx.activeTicks() % 6 == 0) {
                ctx.sound(GLSounds.CLAW.get(), 0.8f, 1.2f);
            }
            return hybrid(player);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Awakened Zoan berserk: rapid regeneration and toughness while in a form. Costs stamina over time. */
    public static final class PrimalFury extends Ability {
        PrimalFury() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CROWN).cooldown(400).stamina(20).staminaPerTick(0.15f)
                    .maxActive(20 * 60).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.fx(GLEffects.ROAR, FxParams.at(ctx.player().position()).color(0xFF4A2A).scale(0.6f));
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1.0f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!hybrid(player) && !beast(player)) {
                return false;
            }
            if (ctx.activeTicks() % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false, true));
                ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(0xFF4A2A).scale(0.5f).variant(1));
            }
            return true;
        }
    }

    /** A roar that shakes the ground: knocks back and weakens everyone nearby. */
    public static final class Roar extends Ability {
        static final double RADIUS = 8;

        Roar() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE).cooldown(240).stamina(25).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), RADIUS)) {
                target.invulnerableTime = 0;
                AbilityDamage.hurt(player, target, AbilityDamage.STRIKE, 2f * ctx.power(), player);
                AbilityDamage.knockback(target, player.position(), 1.6, 0.4);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            }
            ctx.fx(GLEffects.ROAR, FxParams.of(player.getEyePosition(), player.getLookAngle()).color(C).scale((float) RADIUS));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1.6f, 0.9f);
        }
    }
}
