package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Fuwa Fuwa no Mi. Anything the user has touched (except other living bodies) floats at their command, themselves
 * included: they sail through the sky and hurl boulders, earth lions and whole islands down on their enemies.
 */
public class FloatFruit extends DevilFruit {
    public static final int COLOR = 0xE8E8C8;
    public static final int EARTH = 0x8A7A5A;
    static final String M = "fruit/grandline:float";

    public static final DeferredHolder<Ability, Levitate> LEVITATE = ability("float_levitate", Levitate::new);
    public static final DeferredHolder<Ability, Boulder> BOULDER = ability("float_boulder", Boulder::new);
    public static final DeferredHolder<Ability, GroundLift> GROUND_LIFT = ability("float_ground_lift", GroundLift::new);
    public static final DeferredHolder<Ability, EarthLion> EARTH_LION = ability("float_earth_lion", EarthLion::new);
    public static final DeferredHolder<Ability, SwordLegs> SWORD_LEGS = ability("float_sword_legs", SwordLegs::new);
    public static final DeferredHolder<Ability, IslandDrop> ISLAND_DROP = ability("float_island_drop", IslandDrop::new);
    public static final DeferredHolder<Ability, LionPride> LION_PRIDE = ability("float_lion_pride", LionPride::new);

    public static final ResourceLocation ROCK = GrandLine.id("float_rock");
    public static final ResourceLocation LION = GrandLine.id("float_lion");

    public FloatFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Fuwa Fuwa no Mi", COLOR)
                .abilities(LEVITATE, BOULDER, GROUND_LIFT, EARTH_LION, SWORD_LEGS)
                .awakened(ISLAND_DROP, LION_PRIDE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(ROCK, Projectiles.blast(2.5, AbilityDamage.CRUSH, null, p -> rubble(p.level(), p)));
        ProjectileBehavior.Registry.register(LION, Projectiles.homing(24, 0.15, Projectiles.blast(4, AbilityDamage.CRUSH,
                (p, target) -> Kit.launch(target, new Vec3(0, 0.8, 0)), p -> rubble(p.level(), p))));
    }

    static void rubble(net.minecraft.world.level.Level level, AbilityProjectile p) {
        if (level instanceof ServerLevel server) {
            Vfx.burst(server, p, p.position(), EARTH, p.size() + 1.5f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.GROUND);
            server.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.PLAYERS,
                    1.5f, 0.6f);
        }
    }

    /** The user floats down gently from any height. */
    @Override
    public void onFall(ServerPlayer user, LivingFallEvent event) {
        event.setDamageMultiplier(0f);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Make yourself float and sail through the sky. */
    public static final class Levitate extends Ability {
        Levitate() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(60).stamina(8).staminaPerTick(0.1f)
                    .maxActive(2400).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 1.4f, Kind.FEATHER, Vfx.RING);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 0.6f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ctx.player().resetFallDistance();
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /** Touch the ground, float a boulder out of it and hurl it. */
    public static final class Boulder extends Ability {
        Boulder() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, EARTH).cooldown(50).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, ROCK, 1.2, 1.2f, EARTH, 9f, 40).withGravity(0.02);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.STONE_PLACE, 1.2f, 0.6f);
        }
    }

    /** Float the ground out from under everyone at the spot you point at; they hang in the air, then fall. */
    public static final class GroundLift extends Ability {
        GroundLift() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(160).stamina(16).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 22), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.launch(target, new Vec3(0, 0.9, 0));
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
            }
            Vfx.eruption(ctx.level(), player, at, EARTH, 2f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.STONE_BREAK, 1.4f, 0.8f);
        }
    }

    /**
     * Shape a lion out of the earth, float it, and send it hunting your foes.
     *
     * @name Shishi Odoshi
     */
    public static final class EarthLion extends Ability {
        EarthLion() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, EARTH).cooldown(200).stamina(20).mastery(M, 20).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, LION, 0.8, 2.2f, EARTH, 14f, 80);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1.2f, 0.6f);
        }
    }

    /**
     * Slash with the swords that are your legs, cutting everything around you.
     *
     * @name Ouka Gojuu
     */
    public static final class SwordLegs extends Ability {
        SwordLegs() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(80).stamina(12).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 60)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 11f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)).add(0, -0.6, 0), player.getLookAngle(),
                    COLOR, 2.4f, 2);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Float a chunk of island high overhead and drop it on the spot you point at. */
    public static final class IslandDrop extends Ability {
        IslandDrop() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, EARTH).cooldown(700).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 30), 10);
            AbilityProjectile rock = AbilityProjectile.spawn(player, ROCK, at.add(0, 16, 0), new Vec3(0, -1.0, 0), 5f, EARTH,
                    20f * ctx.power(), 60);
            rock.withPower(ctx.power() * 2);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.STONE_BREAK, 2f, 0.4f);
        }
    }

    /** Raise four lions of earth at once; they hunt down your foes and burst over them. */
    public static final class LionPride extends Ability {
        LionPride() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, EARTH).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Vec3 at : Kit.circle(Vec3.ZERO, 1, 4, player.getYRot() * Math.PI / 180)) {
                AbilityProjectile.spawn(player, LION, player.position().add(at.scale(2)).add(0, 2, 0), at.add(0, 0.3, 0).scale(0.6),
                        2f, EARTH, 12f * ctx.power(), 100).withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 2f, 0.5f);
        }
    }
}
