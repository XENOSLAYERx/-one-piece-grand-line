package com.nerodev.grandline.race;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The abilities each race is born with (granted by the race's JSON "abilities" list) and the Mink's Sulong form.
 * Race abilities train the race's own mastery ({@code race/<race id>}).
 */
public final class RaceAbilities {
    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);
    private static final DeferredRegister<Transformation> FORMS = DeferredRegister.create(GLTransformations.KEY, GrandLine.MODID);

    public static final int ELECTRO = 0xA8E0FF;
    public static final int SULONG = 0xF4F8FF;
    static final String MINK = "race/grandline:mink";

    public static final DeferredHolder<Ability, SecondWind> SECOND_WIND = ability("human_second_wind", SecondWind::new);
    public static final DeferredHolder<Ability, SharkDash> SHARK_DASH = ability("fishman_shark_dash", SharkDash::new);
    public static final DeferredHolder<Ability, Electro> ELECTRO_STRIKE = ability("mink_electro", Electro::new);
    public static final DeferredHolder<Ability, ElectroBurst> ELECTRO_BURST = ability("mink_electro_burst", ElectroBurst::new);
    public static final DeferredHolder<Ability, Sulong> SULONG_FORM = ability("mink_sulong", Sulong::new);
    public static final DeferredHolder<Ability, Stomp> GIANT_STOMP = ability("giant_stomp", Stomp::new);
    public static final DeferredHolder<Ability, LongGrab> LONG_GRAB = ability("longarm_long_grab", LongGrab::new);
    public static final DeferredHolder<Ability, SkyStride> SKY_STRIDE = ability("longleg_sky_stride", SkyStride::new);
    public static final DeferredHolder<Ability, Glide> GLIDE = ability("skypiean_glide", Glide::new);

    /** Sulong: under the full moon a Mink's fur turns white and its strength and speed soar. */
    public static final DeferredHolder<Transformation, Transformation> SULONG_BODY = FORMS.register("sulong", () ->
            Transformation.builder(Transformation.Group.BODY)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.4, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6.0, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4.0, Operation.ADD_VALUE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.25, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 8.0, Operation.ADD_VALUE)
                    .modifier(Attributes.SCALE, 0.15, Operation.ADD_VALUE)
                    .source(RaceAbilities.SULONG_FORM)
                    .build());

    private RaceAbilities() {}

    private static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.RACE).icon(icon, color);
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        FORMS.register(modBus);
    }

    /** True while a Mink is in Sulong form: Electro grows half again as strong. */
    public static boolean inSulong(ServerPlayer player) {
        return TransformationManager.isActive(player, SULONG_BODY.get());
    }

    /** Night by the clock (13000 to 23000 ticks into the day), in a world with a sky. */
    public static boolean night(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime() && time >= 13000 && time < 23000;
    }

    /** Sulong needs the full moon in the sky above (night, moon phase 0, open sky). */
    public static boolean fullMoonVisible(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return night(level) && level.getMoonPhase() == 0 && level.canSeeSky(player.blockPosition().above());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Human
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Grit your teeth and find the strength to go on: half your stamina comes back and your wounds close faster.
     *
     * @name Second Wind
     */
    public static final class SecondWind extends Ability {
        SecondWind() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, 0xE8C878).cooldown(1200).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            long now = AbilityManager.now(player);
            float restore = ctx.state().maxStamina() * 0.5f;
            ctx.state().restoreStamina(restore, now);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), 0xF0D890, 1.4f, Kind.GLOW, Vfx.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.PLAYER_LEVELUP, 0.6f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fish-Man
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Burst forward like a shark, ramming everything in your way. In the water you go three times as far.
     *
     * @name Shark Dash
     */
    public static final class SharkDash extends Ability {
        SharkDash() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, 0x3A8AC8).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            boolean wet = player.isInWater();
            double reach = (wet ? 12 : 4) * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, reach, 1.2)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, (wet ? 12f : 8f) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.3);
            }
            Vec3 from = player.position();
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.scale(wet ? 2.4 : 1.2).add(0, wet ? 0 : 0.15, 0));
            player.hurtMarked = true;
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(look.scale(reach)).add(0, 1, 0), 0x6AC8F0, 0.8f,
                    wet ? Kind.BUBBLE : Kind.STREAK);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mink
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Strike the foe in front of you with a fist full of Electro: it is shocked and stunned for a moment.
     *
     * @name Electro
     */
    public static final class Electro extends Ability {
        Electro() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, ELECTRO).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float boost = inSulong(player) ? 1.5f : 1f;
            Kit.target(player, 4).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.SHOCK, 10f * boost * ctx.power());
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 8, 0, false, false, true));
                AbilityDamage.knockback(target, player.position(), 0.8, 0.2);
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), ELECTRO, 0.4f, Kind.SPARK, Vfx.BEAM_BOLT);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), ELECTRO, 1.4f, Kind.SPARK);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 0.9f, 1.3f);
        }
    }

    /**
     * Let the Electro in your body burst out: everything around you is shocked and thrown back.
     *
     * @name Electro Burst
     */
    public static final class ElectroBurst extends Ability {
        ElectroBurst() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, ELECTRO).cooldown(140).stamina(14).mastery(MINK, 15).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float boost = inSulong(player) ? 1.5f : 1f;
            double radius = 4.5 * ctx.power() * (inSulong(player) ? 1.3 : 1);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.SHOCK, 9f * boost * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.35);
                Vfx.beam(ctx.level(), player, player.getBoundingBox().getCenter(), target.getBoundingBox().getCenter(), ELECTRO, 0.3f,
                        Kind.SPARK, Vfx.BEAM_BOLT);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), ELECTRO, (float) radius, Kind.SPARK, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.RUMBLE_THUNDER.get(), 0.8f, 1.5f);
        }
    }

    /**
     * Under the full moon, look up and become Sulong: you glow white in the moonlight, and your speed, strength and
     * Electro soar. It ends at dawn.
     *
     * @name Sulong
     */
    public static final class Sulong extends Ability {
        Sulong() {
            super(props(ActivationType.TOGGLE, AbilityIcon.MOON, SULONG).cooldown(600).stamina(20).staminaPerTick(0.12f).maxActive(20 * 90)
                    .mastery(MINK, 30).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return fullMoonVisible(player) ? Optional.empty() : Optional.of(Component.literal("Sulong needs the full moon in the sky"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            TransformationManager.start(player, SULONG_BODY.get());
            ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(SULONG).scale(1.6f));
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), ELECTRO, 3f, Kind.SPARK, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1.1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!TransformationManager.isActive(player, SULONG_BODY.get()) || !night(player.serverLevel())) {
                return false;
            }
            if (ctx.activeTicks() % 4 == 0) {
                Vec3 at = player.position().add(0, 0.4 + player.getRandom().nextDouble() * 1.6, 0);
                Vfx.impact(ctx.level(), player, at, SULONG, 0.8f, Kind.GLOW);
            }
            if (ctx.activeTicks() % 9 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), ELECTRO, 0.9f, Kind.SPARK);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            if (TransformationManager.isActive(player, SULONG_BODY.get())) {
                TransformationManager.end(player);
                ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(0xD8D0C0).scale(0.8f));
            }
            Fx.stopAnimation(player);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Giant
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Stamp the ground with a giant's foot: everything around you is crushed and thrown into the air.
     *
     * @name Giant Stomp
     */
    public static final class Stomp extends Ability {
        Stomp() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, 0xB08A5A).cooldown(100).stamina(14).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 5.5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 12f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.7, 0));
            }
            Vfx.burst(ctx.level(), player, player.position(), 0x8A7A5A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.3f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Longarm and Longleg
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Reach out with your long arms, seize a foe up to ten blocks away and haul it to you.
     *
     * @name Long Grab
     */
    public static final class LongGrab extends Ability {
        LongGrab() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, 0xD8A878).cooldown(80).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                Kit.pull(target, player.position().add(ctx.horizontalLook().scale(1.5)), 1.4);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), 0xE8C8A0, 0.5f, Kind.STREAK);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
        }
    }

    /**
     * Push off with your long legs and leap high into the air; you land as lightly as a cat.
     *
     * @name Sky Stride
     */
    public static final class SkyStride extends Ability {
        SkyStride() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, 0xC8B8E8).cooldown(60).stamina(8).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 forward = ctx.horizontalLook().scale(0.9);
            player.setDeltaMovement(forward.x, 1.35 * Math.min(1.5, ctx.power()), forward.z);
            player.hurtMarked = true;
            player.resetFallDistance();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, false, false));
            Vfx.burst(ctx.level(), player, player.position(), 0xE8E8F0, 1.4f, Kind.SHOCKWAVE, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(GLSounds.GEPPO.get(), 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Skypiean
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Spread your little wings and glide: you sink slowly and sail forward as long as you hold it.
     *
     * @name Glide
     */
    public static final class Glide extends Ability {
        Glide() {
            super(props(ActivationType.HOLD, AbilityIcon.CLOUD, 0xF0F4FF).cooldown(20).stamina(4).staminaPerTick(0.15f).maxActive(300)
                    .masteryGain(0.3));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return player.onGround() ? Optional.of(Component.literal("You need to be in the air to glide")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.sound(SoundEvents.ELYTRA_FLYING, 0.4f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (player.onGround() || player.isInWater()) {
                return false;
            }
            Vec3 look = ctx.horizontalLook();
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.6 + look.x * 0.22, Math.max(v.y, -0.12), v.z * 0.6 + look.z * 0.22);
            player.hurtMarked = true;
            player.resetFallDistance();
            if (ctx.activeTicks() % 3 == 0) {
                Vfx.impact(ctx.level(), player, player.position().add(0, 1.2, 0).subtract(look.scale(0.4)), 0xFFFFFF, 0.5f, Kind.FEATHER);
            }
            return true;
        }
    }
}
