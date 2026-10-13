package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.props;

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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Moa Moa no Mi. Multiply the speed and force of anything by ten, a hundred, a thousand, as the Galaxy Pirate Byrnndi
 * World does: a flicked pebble flies like a bullet, a swing of the arm becomes a gale, and a thrown rock lands like a
 * meteor.
 */
public class MoreFruit extends DevilFruit {
    public static final int COLOR = 0xA83AE8;
    public static final int GLOW = 0xE8C8FF;
    public static final int STONE = 0x8A8A82;
    static final String M = "fruit/grandline:more";

    public static final DeferredHolder<Ability, TenfoldPebble> TENFOLD_PEBBLE = ability("more_tenfold_pebble", TenfoldPebble::new);
    public static final DeferredHolder<Ability, HundredfoldGust> HUNDREDFOLD_GUST = ability("more_hundredfold_gust",
            HundredfoldGust::new);
    public static final DeferredHolder<Ability, Accelerate> ACCELERATE = ability("more_accelerate", Accelerate::new);
    public static final DeferredHolder<Ability, AmplifiedStrike> AMPLIFIED_STRIKE = ability("more_amplified_strike",
            AmplifiedStrike::new);
    public static final DeferredHolder<Ability, HundredfoldBoulder> BOULDER = ability("more_hundredfold_boulder", HundredfoldBoulder::new);
    public static final DeferredHolder<Ability, ThousandfoldBarrage> BARRAGE = ability("more_thousandfold_barrage",
            ThousandfoldBarrage::new);
    public static final DeferredHolder<Ability, WorldBreaker> WORLD_BREAKER = ability("more_world_breaker", WorldBreaker::new);

    public static final ResourceLocation PEBBLE = GrandLine.id("more_pebble");
    public static final ResourceLocation ROCK = GrandLine.id("more_rock");

    public MoreFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Moa Moa no Mi", COLOR)
                .abilities(TENFOLD_PEBBLE, HUNDREDFOLD_GUST, ACCELERATE, AMPLIFIED_STRIKE, BOULDER)
                .awakened(BARRAGE, WORLD_BREAKER));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(PEBBLE, Projectiles.piercing(AbilityDamage.STRIKE, (p, target) ->
                Kit.launch(target, p.getDeltaMovement().normalize().scale(0.8).add(0, 0.2, 0))));
        ProjectileBehavior.Registry.register(ROCK, Projectiles.blast(4, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), STONE, 4f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY)));
    }

    /** The purple flash of the power taking hold of something. */
    static void amplify(AbilityContext ctx, Vec3 at, float size) {
        Vfx.burst(ctx.level(), ctx.player(), at, COLOR, size, Kind.SHOCKWAVE, Vfx.FLASH);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Flick a pebble at ten times its speed: it punches straight through everything in its path. */
    public static final class TenfoldPebble extends Ability {
        TenfoldPebble() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, STONE).cooldown(25).stamina(6).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, PEBBLE, 3.4, 0.25f, STONE, 9f, 20);
            amplify(ctx, ctx.handPos(), 0.8f);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1f, 1.6f);
        }
    }

    /** Swing your arm and multiply the air it moves a hundredfold: a gale that hurls everything in front of you away. */
    public static final class HundredfoldGust extends Ability {
        HundredfoldGust() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, GLOW).cooldown(80).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 50)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                Kit.launch(target, look.scale(3.0).add(0, 0.6, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(3)), GLOW, 4f, Kind.SHOCKWAVE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.BREEZE_WHIRL, 1.4f, 0.8f);
        }
    }

    /** Multiply your own speed tenfold: you flash forward to where you look and stay blindingly fast for a few seconds. */
    public static final class Accelerate extends Ability {
        Accelerate() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(120).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.blink(player, 16 * ctx.power(), COLOR, Kind.STREAK);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 3));
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(GLSounds.SORU.get(), 1f, 1.3f);
        }
    }

    /** Amplify a single blow a hundredfold: the foe in front of you is struck and blasted far away. */
    public static final class AmplifiedStrike extends Ability {
        AmplifiedStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 18f * ctx.power());
                Kit.launch(target, player.getLookAngle().scale(3.5).add(0, 0.8, 0));
                amplify(ctx, target.getBoundingBox().getCenter(), 2.4f);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.4f, 1.2f);
        }
    }

    /** Hurl a rock and multiply it a hundredfold in flight: it lands like a meteor wherever it strikes. */
    public static final class HundredfoldBoulder extends Ability {
        HundredfoldBoulder() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, STONE).cooldown(240).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, ROCK, 1.8, 2.2f, STONE, 20f, 60);
            amplify(ctx, ctx.handPos(), 2f);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A handful of gravel, each stone multiplied a thousandfold: a storm of bullets in a wide fan. */
    public static final class ThousandfoldBarrage extends Ability {
        ThousandfoldBarrage() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, GLOW).cooldown(500).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < 12; i++) {
                Vec3 dir = look.yRot((float) ((i - 5.5) * 0.06)).add(0, (player.getRandom().nextFloat() - 0.5f) * 0.12, 0).normalize();
                AbilityProjectile.spawn(player, PEBBLE, ctx.handPos(), dir.scale(3.4), 0.25f, STONE, 8f * ctx.power(), 20)
                        .withPower(ctx.power());
            }
            amplify(ctx, ctx.handPos(), 2.4f);
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1.6f, 1.2f);
        }
    }

    /** Multiply a single blow against the ground where you point until the world itself cracks: a vast crater. */
    public static final class WorldBreaker extends Ability {
        WorldBreaker() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(900).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 32), 10);
            double radius = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 26f * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(1.6).add(0, 1.0, 0));
            }
            Vfx.eruption(ctx.level(), player, at, STONE, 6f, Kind.SHARD);
            Vfx.burst(ctx.level(), player, at, COLOR, (float) radius, Kind.SHOCKWAVE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }
}
