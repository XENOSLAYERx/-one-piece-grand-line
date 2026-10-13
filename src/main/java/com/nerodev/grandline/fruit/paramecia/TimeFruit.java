package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Toki Toki no Mi. Travel forward through time, and send others with you: step out of the present, skip ahead of a
 * blow, or send a foe seconds into the future where it can't touch anyone.
 */
public class TimeFruit extends DevilFruit {
    public static final int COLOR = 0xA8A0E8;
    public static final int GOLD = 0xF0E0A0;
    static final String M = "fruit/grandline:time";

    public static final DeferredHolder<Ability, TimeLeap> TIME_LEAP = ability("time_leap", TimeLeap::new);
    public static final DeferredHolder<Ability, SendAhead> SEND_AHEAD = ability("time_send_ahead", SendAhead::new);
    public static final DeferredHolder<Ability, Accelerate> ACCELERATE = ability("time_accelerate", Accelerate::new);
    public static final DeferredHolder<Ability, SlowMoment> SLOW_MOMENT = ability("time_slow_moment", SlowMoment::new);
    public static final DeferredHolder<Ability, FastForward> FAST_FORWARD = ability("time_fast_forward", FastForward::new);
    public static final DeferredHolder<Ability, YearsLater> YEARS_LATER = ability("time_years_later", YearsLater::new);
    public static final DeferredHolder<Ability, AheadOfTheBlow> AHEAD_OF_THE_BLOW = ability("time_ahead_of_the_blow",
            AheadOfTheBlow::new);

    public static final ResourceLocation STILL = GrandLine.id("time_still");

    public TimeFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Toki Toki no Mi", COLOR)
                .abilities(TIME_LEAP, SEND_AHEAD, ACCELERATE, SLOW_MOMENT, FAST_FORWARD)
                .awakened(YEARS_LATER, AHEAD_OF_THE_BLOW));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(STILL, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 2));
            target.setDeltaMovement(target.getDeltaMovement().scale(0.3));
        }, null));
    }

    /**
     * Sends {@code target} {@code ticks} into the future: it vanishes from the fight, can neither move nor act, and
     * nothing nearby keeps chasing it.
     */
    static void sendAhead(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.ENCASED, ticks, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob mob) {
            mob.setTarget(null);
        }
        for (Mob hunter : target.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(16),
                m -> m.getTarget() == target)) {
            hunter.setTarget(null);
        }
        if (target.level() instanceof ServerLevel level) {
            Vfx.burst(level, target, target.getBoundingBox().getCenter(), COLOR, 1.6f, Kind.STAR, Vfx.FLASH | Vfx.RING);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Hold to step out of the present: unseen, untouchable and unmoving, for up to three seconds.
     *
     * @name Time Leap
     */
    public static final class TimeLeap extends Ability {
        TimeLeap() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, COLOR).cooldown(200).stamina(14).staminaPerTick(0.4f).maxActive(60)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.6f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.hurtMarked = true;
            if (ctx.activeTicks() % 5 == 0) {
                for (Mob hunter : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
                        m -> m.getTarget() == player)) {
                    hunter.setTarget(null);
                }
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setCanceled(true);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ctx.player().removeEffect(MobEffects.INVISIBILITY);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), GOLD, 1.6f, Kind.STAR, Vfx.FLASH);
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Send a foe three seconds into the future: it vanishes from the fight, and nothing can reach it or it them. */
    public static final class SendAhead extends Ability {
        SendAhead() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, GOLD).cooldown(220).stamina(16).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 12).ifPresent(target -> sendAhead(target, 60));
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BELL_RESONATE, 1.2f, 1.4f);
        }
    }

    /** Hurry your own time along: you move and strike much faster. */
    public static final class Accelerate extends Buffs.Toggle {
        Accelerate() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH, COLOR).cooldown(240).stamina(12).staminaPerTick(0.25f)
                            .maxActive(400).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("time_accelerate_speed"), 0.4,
                                    Operation.ADD_MULTIPLIED_TOTAL),
                            Buffs.mod(Attributes.ATTACK_SPEED, GrandLine.id("time_accelerate_attack"), 2, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1f, 1.8f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.7f, Kind.STREAK);
            }
            return true;
        }
    }

    /** Make time crawl where you point: for eight seconds, everything inside moves at a snail's pace. */
    public static final class SlowMoment extends Ability {
        SlowMoment() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(300).stamina(18).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, STILL, at, 6 * ctx.power(), COLOR, 0f, 160);
            Vfx.burst(ctx.level(), player, at, COLOR, 6f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BELL_RESONATE, 1.4f, 0.6f);
        }
    }

    /**
     * Fast-forward a foe through time: every effect on it runs out at once, the young grow up, and the years hurt.
     *
     * @name Fast-Forward
     */
    public static final class FastForward extends Ability {
        FastForward() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, GOLD).cooldown(260).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                target.removeAllEffects();
                target.clearFire();
                if (target instanceof AgeableMob young && young.isBaby()) {
                    young.setAge(0);
                }
                Kit.hit(player, target, AbilityDamage.DRAIN, 10f * ctx.power());
                Vfx.spiral(ctx.level(), player, target.getBoundingBox().getCenter(), GOLD, 1.2f, Kind.STAR);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.BELL_BLOCK, 1.2f, 1.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Send everyone around you years ahead: every foe nearby vanishes from the fight for four seconds. */
    public static final class YearsLater extends Ability {
        YearsLater() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, COLOR).cooldown(900).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                sendAhead(target, 80);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 10f, Kind.STAR, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BELL_RESONATE, 2f, 0.8f);
        }
    }

    /** For ten seconds, leap a moment ahead of every blow: attacks that would hit you land on nothing. */
    public static final class AheadOfTheBlow extends Ability {
        AheadOfTheBlow() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, GOLD).cooldown(900).stamina(24).staminaPerTick(0.4f)
                    .maxActive(200).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), GOLD, 0.8f, Kind.STAR);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().getEntity() == null) {
                return;
            }
            event.setCanceled(true);
            ServerPlayer player = ctx.player();
            Vfx.blink(ctx.level(), player, player.position(), player.position(), GOLD, Kind.STAR);
            ctx.sound(GLSounds.EVADE.get(), 1f, 1.2f);
        }
    }
}
