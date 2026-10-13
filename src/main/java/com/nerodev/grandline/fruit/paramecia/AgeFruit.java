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
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Toshi Toshi no Mi. Make anyone older or younger, yourself included: age foes into frailty, shrink them into
 * children, or reach into a possible future and become it.
 */
public class AgeFruit extends DevilFruit {
    public static final int COLOR = 0xF0A0D0;
    public static final int TEAL = 0x8AE0E0;
    static final String M = "fruit/grandline:age";

    public static final DeferredHolder<Ability, AgeUp> AGE_UP = ability("age_up", AgeUp::new);
    public static final DeferredHolder<Ability, AgeDown> AGE_DOWN = ability("age_down", AgeDown::new);
    public static final DeferredHolder<Ability, DistortedFuture> DISTORTED_FUTURE = ability("age_distorted_future",
            DistortedFuture::new);
    public static final DeferredHolder<Ability, SecondChildhood> SECOND_CHILDHOOD = ability("age_second_childhood",
            SecondChildhood::new);
    public static final DeferredHolder<Ability, WitherTouch> WITHER_TOUCH = ability("age_wither_touch", WitherTouch::new);
    public static final DeferredHolder<Ability, AgingWave> AGING_WAVE = ability("age_aging_wave", AgingWave::new);
    public static final DeferredHolder<Ability, AgeReversal> AGE_REVERSAL = ability("age_reversal", AgeReversal::new);

    public AgeFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Toshi Toshi no Mi", COLOR)
                .abilities(AGE_UP, AGE_DOWN, DISTORTED_FUTURE, SECOND_CHILDHOOD, WITHER_TOUCH)
                .awakened(AGING_WAVE, AGE_REVERSAL));
    }

    static void init() {}

    static void behaviors() {}

    /** Decades pass in a moment: the creature is slow, frail and tired. */
    static void age(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1));
    }

    /** Clears every harmful or neutral effect (the body's clock is wound back past them). */
    static void unwind(LivingEntity target) {
        for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial()) {
                target.removeEffect(effect.getEffect());
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Age a foe decades in a moment: it is hurt, and slow, weak and frail for eight seconds.
     *
     * @name Age Up
     */
    public static final class AgeUp extends Ability {
        AgeUp() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, COLOR).cooldown(80).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                age(target, 160);
                Kit.hit(player, target, AbilityDamage.DRAIN, 6f * ctx.power());
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), COLOR, 0.25f, Kind.SPARK,
                        Vfx.BEAM_PARTICLES);
                Vfx.impact(ctx.level(), player, target.getEyePosition(), 0xD0D0D0, 1f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 1.4f);
        }
    }

    /**
     * Turn a foe back into a child for ten seconds: half its size and half its strength.
     *
     * @name Age Down
     */
    public static final class AgeDown extends Ability {
        AgeDown() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, TEAL).cooldown(160).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Buffs.timed(target, Buffs.mod(Attributes.SCALE, GrandLine.id("age_down_scale"), -0.5, Operation.ADD_MULTIPLIED_TOTAL),
                        200);
                Buffs.timed(target, Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("age_down_damage"), -0.5,
                        Operation.ADD_MULTIPLIED_TOTAL), 200);
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), TEAL, 1.4f, Kind.STAR, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.8f);
        }
    }

    /** Reach into a possible future and become it: bigger, stronger, tougher and faster. */
    public static final class DistortedFuture extends Buffs.Toggle {
        DistortedFuture() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(400).stamina(16).staminaPerTick(0.25f)
                            .maxActive(500).mastery(M, 10).masteryGain(0.9),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("age_future_scale"), 0.3, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("age_future_damage"), 5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("age_future_health"), 8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("age_future_speed"), 0.15,
                                    Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2.4f, Kind.STAR,
                    Vfx.FLASH | Vfx.RING);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 1.2f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.STAR);
            }
            return true;
        }
    }

    /** Wind your own age back: you are small, quick and fresh for fifteen seconds, and the years' aches are gone. */
    public static final class SecondChildhood extends Ability {
        SecondChildhood() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, TEAL).cooldown(400).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Buffs.timed(player, Buffs.mod(Attributes.SCALE, GrandLine.id("age_child_scale"), -0.4, Operation.ADD_MULTIPLIED_TOTAL), 300);
            Buffs.timed(player, Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("age_child_speed"), 0.3,
                    Operation.ADD_MULTIPLIED_TOTAL), 300);
            player.heal(6f);
            unwind(player);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), TEAL, 1.6f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 2f);
        }
    }

    /** Age whatever you touch to dust: a terrible wound that keeps withering. */
    public static final class WitherTouch extends Ability {
        WitherTouch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(200).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4, 45)) {
                Kit.hit(player, target, AbilityDamage.DRAIN, 14f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), 0x3A3A3A, 1.2f, Kind.SMOKE);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.WITHER_SHOOT, 0.8f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Sweep a wave of years over everything in front of you: they age, wither and slow to a crawl. */
    public static final class AgingWave extends Ability {
        AgingWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.DRAIN, 10f * ctx.power());
                age(target, 200);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
            }
            for (int i = 1; i <= 3; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(i * 3)), COLOR, 1.4f + i,
                        Kind.STAR, 0);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.2f, 0.8f);
        }
    }

    /**
     * Wind back the clocks of you and every friend nearby: wounds close and every ailment is undone.
     *
     * @name Age Reversal
     */
    public static final class AgeReversal extends Ability {
        AgeReversal() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, TEAL).cooldown(1200).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ClothesFruit.friends(ctx.player(), 10)) {
                friend.heal(20f);
                unwind(friend);
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                Vfx.impact(ctx.level(), friend, friend.getBoundingBox().getCenter(), TEAL, 1.2f, Kind.HEART);
            }
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), TEAL, 10f, Kind.STAR, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1.4f, 1.4f);
        }
    }
}
