package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Woshu Woshu no Mi. Wash people like laundry: scrub the fight out of them, wring them out flat, hang them up to dry,
 * and wash the very evil out of a monster's heart.
 */
public class WashFruit extends DevilFruit {
    public static final int COLOR = 0x8AC8E8;
    public static final int FOAM = 0xF4FAFF;
    static final String M = "fruit/grandline:wash";

    public static final DeferredHolder<Ability, Scrub> SCRUB = ability("wash_scrub", Scrub::new);
    public static final DeferredHolder<Ability, Wring> WRING = ability("wash_wring", Wring::new);
    public static final DeferredHolder<Ability, HangToDry> HANG_TO_DRY = ability("wash_hang_to_dry", HangToDry::new);
    public static final DeferredHolder<Ability, WashAwayEvil> WASH_AWAY_EVIL = ability("wash_away_evil", WashAwayEvil::new);
    public static final DeferredHolder<Ability, Suds> SUDS = ability("wash_suds", Suds::new);
    public static final DeferredHolder<Ability, GreatLaundry> GREAT_LAUNDRY = ability("wash_great_laundry", GreatLaundry::new);
    public static final DeferredHolder<Ability, SpinCycle> SPIN_CYCLE = ability("wash_spin_cycle", SpinCycle::new);

    public WashFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Woshu Woshu no Mi", COLOR)
                .abilities(SCRUB, WRING, HANG_TO_DRY, WASH_AWAY_EVIL, SUDS)
                .awakened(GREAT_LAUNDRY, SPIN_CYCLE));
    }

    static void init() {}

    static void behaviors() {}

    /** Washed out: weak and sluggish, the fight scrubbed right out of it. */
    static void washed(LivingEntity target, int ticks, int level) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, level));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, level));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Grab a foe and give it a good scrub: it comes out weak and sluggish. */
    public static final class Scrub extends Ability {
        Scrub() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 5f * ctx.power());
                washed(target, 140, 2);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), FOAM, 1.4f, Kind.BUBBLE, 0);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.2f, 1f);
        }
    }

    /** Wring a foe out like a wet towel: a crushing twist, and it is left limp. */
    public static final class Wring extends Ability {
        Wring() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(120).stamina(14).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 12f * ctx.power());
                washed(target, 100, 1);
                Vfx.spiral(ctx.level(), target, target.getBoundingBox().getCenter(), FOAM, 1f, Kind.DROP);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1.2f, 0.6f);
        }
    }

    /** Hang everyone around the spot you point at out to dry: they dangle helplessly in the air. */
    public static final class HangToDry extends Ability {
        HangToDry() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, FOAM).cooldown(200).stamina(16).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.launch(target, new Vec3(0, 0.7, 0));
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 0));
                Kit.encase(target, Vfx.StatusLook.STRINGS, 50);
            }
            Vfx.beam(ctx.level(), player, at.add(-4, 3, 0), at.add(4, 3, 0), FOAM, 0.05f, Kind.SPARK, Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.WOOL_PLACE, 1.4f, 0.8f);
        }
    }

    /** Wash the malice out of a monster: it forgets its anger, loses every blessing and takes a cleansing blow. */
    public static final class WashAwayEvil extends Ability {
        WashAwayEvil() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, FOAM).cooldown(240).stamina(18).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                for (Holder<MobEffect> effect : new ArrayList<>(target.getActiveEffectsMap().keySet())) {
                    if (effect.value().isBeneficial()) {
                        target.removeEffect(effect);
                    }
                }
                float damage = (target instanceof Enemy ? 12f : 5f) * ctx.power();
                Kit.hit(player, target, AbilityDamage.LIGHT, damage);
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                }
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), FOAM, 1.6f, Kind.BUBBLE, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.4f, 1.4f);
        }
    }

    /** Throw a wave of suds over everyone in front of you: they slip and lose their strength. */
    public static final class Suds extends Ability {
        Suds() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(100).stamina(12).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 7 * ctx.power(), 45)) {
                SlipFruit.slide(target, 0.5);
                washed(target, 100, 1);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(7)), FOAM, 1.6f,
                    Kind.BUBBLE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.2f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Do the whole battlefield's laundry at once: everyone near you is washed, wrung and left too limp to fight. */
    public static final class GreatLaundry extends Ability {
        GreatLaundry() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, FOAM).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                washed(target, 200, 2);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), FOAM, (float) radius, Kind.BUBBLE, Vfx.RING | Vfx.HEAVY);
            Vfx.rain(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.DROP);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 2f, 0.8f);
        }
    }

    /** Hold to whip up a washing-machine whirl that drags foes in and tumbles them round and round. */
    public static final class SpinCycle extends Ability {
        SpinCycle() {
            super(props(ActivationType.HOLD, AbilityIcon.TORNADO, COLOR).cooldown(500).stamina(24).staminaPerTick(0.4f)
                    .maxActive(80).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 5 == 0) {
                ServerPlayer player = ctx.player();
                Vec3 center = player.position().add(0, 1, 0);
                for (LivingEntity target : AbilityDamage.around(player, center, 7 * ctx.power())) {
                    Vec3 to = target.position().subtract(center);
                    Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.5);
                    Kit.launch(target, swirl.add(to.normalize().scale(-0.25)).add(0, 0.1, 0));
                    Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                }
                Vfx.spiral(ctx.level(), player, center, FOAM, 5f, Kind.BUBBLE);
                ctx.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.8f, 1f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }
}
