package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Gura Gura no Mi. The power to shake the world: fists that crack the air itself like glass, shockwaves that
 * travel through anything, and quakes that throw everything around off its feet.
 */
public class TremorFruit extends DevilFruit {
    public static final int COLOR = 0xF2F0E0;
    public static final int CRACK = 0xBFE0FF;
    static final String M = "fruit/grandline:tremor";

    public static final DeferredHolder<Ability, QuakePunch> QUAKE_PUNCH = ability("tremor_quake_punch", QuakePunch::new);
    public static final DeferredHolder<Ability, QuakeBubble> QUAKE_BUBBLE = ability("tremor_quake_bubble", QuakeBubble::new);
    public static final DeferredHolder<Ability, GroundQuake> GROUND_QUAKE = ability("tremor_ground_quake", GroundQuake::new);
    public static final DeferredHolder<Ability, AirStep> AIR_STEP = ability("tremor_air_step", AirStep::new);
    public static final DeferredHolder<Ability, QuakeFists> QUAKE_FISTS = ability("tremor_quake_fists", QuakeFists::new);
    public static final DeferredHolder<Ability, SeaQuake> SEA_QUAKE = ability("tremor_sea_quake", SeaQuake::new);
    public static final DeferredHolder<Ability, SkyCrack> SKY_CRACK = ability("tremor_sky_crack", SkyCrack::new);

    public static final ResourceLocation BUBBLE = GrandLine.id("tremor_bubble");

    public TremorFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.MYTHICAL, "Gura Gura no Mi", COLOR)
                .abilities(QUAKE_PUNCH, QUAKE_BUBBLE, GROUND_QUAKE, AIR_STEP, QUAKE_FISTS)
                .awakened(SEA_QUAKE, SKY_CRACK));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BUBBLE, Projectiles.blast(3.5, AbilityDamage.BLAST, (p, target) ->
                AbilityDamage.knockback(target, p.position(), 1.4, 0.4), p -> {
                    if (p.level() instanceof ServerLevel level) {
                        crack(level, p, p.position(), 3f);
                    }
                }));
    }

    /** The look of the air shattering at {@code at}: a white flash, a ring of cracks and a shockwave. */
    static void crack(ServerLevel level, net.minecraft.world.entity.Entity source, Vec3 at, float size) {
        Vfx.burst(level, source, at, CRACK, size, Kind.SHOCKWAVE, Vfx.SHOCKWAVE | Vfx.FLASH);
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + level.random.nextDouble() * 0.6;
            Vec3 end = at.add(Math.cos(a) * size, (level.random.nextDouble() - 0.5) * size, Math.sin(a) * size);
            Vfx.beam(level, source, at, end, COLOR, 0.12f, Kind.SPARK, Vfx.BEAM_BOLT);
        }
    }

    /** Throws {@code target} away from {@code from} the way a quake does: hard and upwards. */
    static void quake(LivingEntity target, Vec3 from, double strength) {
        AbilityDamage.knockback(target, from, strength, 0.35 + strength * 0.15);
    }

    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getDirectEntity() == user
                && AbilityState.of(user).isActive(QUAKE_FISTS.getId())) {
            event.setAmount(event.getAmount() + 5f);
            quake(target, user.position(), 1.6);
            crack(user.serverLevel(), user, target.getBoundingBox().getCenter(), 1.4f);
            user.level().playSound(null, target.getX(), target.getY(), target.getZ(), GLSounds.HAKI_IMPACT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Charge a punch and crack the air in front of you: a shockwave blasts everyone in front of you away. */
    public static final class QuakePunch extends Ability {
        QuakePunch() {
            super(props(ActivationType.CHARGE, AbilityIcon.FIST, CRACK).cooldown(100).stamina(18).minCharge(8).maxActive(40)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.CONQUEROR_CHARGE.get(), 0.8f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 5 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos(), CRACK, 0.6f, Kind.SPARK);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float charge = Mth.clamp(ctx.activeTicks() / 40f, 0.3f, 1f);
            double range = (7 + 7 * charge) * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, range, 35)) {
                Kit.hit(player, target, AbilityDamage.BLAST, (6f + 10f * charge) * ctx.power());
                quake(target, player.position(), 1.2 + charge * 1.6);
            }
            Vec3 fist = ctx.handPos().add(player.getLookAngle().scale(1.2));
            crack(ctx.level(), player, fist, 1.5f + charge * 2);
            Vfx.beam(ctx.level(), player, fist, player.getEyePosition().add(player.getLookAngle().scale(range)), CRACK,
                    1.2f + charge, Kind.SHOCKWAVE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f + charge, 0.6f);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.4f, 0.5f);
        }
    }

    /** Hurl a bubble of trembling air that bursts into a quake wherever it lands. */
    public static final class QuakeBubble extends Ability {
        QuakeBubble() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, CRACK).cooldown(60).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BUBBLE, 1.3, 1.0f, CRACK, 8f, 30);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.HAKI_EMISSION.get(), 1f, 0.7f);
        }
    }

    /** Punch the ground: the quake throws everyone around you into the air and leaves them reeling. */
    public static final class GroundQuake extends Ability {
        GroundQuake() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(160).stamina(20).mastery(M, 10)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(0.4).add(0, 0.75, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.1, 0), COLOR, (float) radius, Kind.SHARD,
                    Vfx.RING | Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
            crack(ctx.level(), player, player.position().add(0, 0.3, 0), 2.5f);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.5f);
        }
    }

    /** Crack the air behind you and ride the shockwave forward; whoever stood next to you is blown away. */
    public static final class AirStep extends Ability {
        AirStep() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, CRACK).cooldown(70).stamina(12).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 behind = player.position().add(player.getLookAngle().scale(-1)).add(0, 1, 0);
            for (LivingEntity target : AbilityDamage.around(player, behind, 3.5)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 5f * ctx.power());
                quake(target, behind, 1.4);
            }
            crack(ctx.level(), player, behind, 1.8f);
            Kit.dash(player, 1.9, 0.35);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.2f, 0.8f);
        }
    }

    /** Wrap your fists in quake bubbles: every punch shatters the air and hurls its target away. */
    public static final class QuakeFists extends Ability {
        QuakeFists() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FIST, COLOR).cooldown(200).stamina(14).staminaPerTick(0.2f)
                    .maxActive(600).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.handPos(), CRACK, 1.2f, Kind.SHOCKWAVE, Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(GLSounds.HAKI_HARDEN.get(), 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos(), CRACK, 0.5f, Kind.SPARK);
            }
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Grip the air and tilt the world: a vast quake flings everything for twenty blocks around you skyward. */
    public static final class SeaQuake extends Ability {
        SeaQuake() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, CRACK).cooldown(700).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 18 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                double falloff = 1 - Math.min(0.6, target.distanceTo(player) / radius * 0.6);
                Kit.hit(player, target, AbilityDamage.CRUSH, (float) (16 * falloff) * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(0.8).add(0, 1.2, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            }
            for (Vec3 at : Kit.circle(player.position(), radius * 0.5, 6, 0)) {
                crack(ctx.level(), player, at.add(0, 1.5, 0), 2.5f);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), CRACK, (float) radius, Kind.SHOCKWAVE,
                    Vfx.RING | Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.4f);
            ctx.sound(GLSounds.CONQUEROR_WAVE.get(), 2f, 0.6f);
        }
    }

    /** Charge both fists and crack the sky itself: a colossal shockwave tears through everything in a straight line. */
    public static final class SkyCrack extends Ability {
        SkyCrack() {
            super(props(ActivationType.CHARGE, AbilityIcon.BEAM, CRACK).cooldown(600).stamina(36).minCharge(15).maxActive(50)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(GLSounds.CONQUEROR_CHARGE.get(), 1.4f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                crack(ctx.level(), ctx.player(), ctx.handPos().add(ctx.player().getLookAngle()), 0.8f);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float charge = Mth.clamp(ctx.activeTicks() / 50f, 0.4f, 1f);
            double range = 30 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 2.2)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 20f * charge * ctx.power());
                quake(target, player.position(), 2.4);
            }
            Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(range));
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, CRACK, 3f * charge, Kind.SHOCKWAVE, Vfx.BEAM_GLOW);
            for (int i = 1; i <= 4; i++) {
                crack(ctx.level(), player, ctx.handPos().add(player.getLookAngle().scale(range * i / 5)), 2.5f);
            }
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.5f);
        }
    }
}
