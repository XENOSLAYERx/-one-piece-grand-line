package com.nerodev.grandline.fruit.logia;

import static com.nerodev.grandline.fruit.logia.LogiaBatch.ability;
import static com.nerodev.grandline.fruit.logia.LogiaBatch.props;

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
import com.nerodev.grandline.fruit.LogiaFruit;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Pika Pika no Mi. A body of light: kicks at light speed, a rain of light bullets, lasers that pierce everything,
 * instant travel. Light itself cannot hurt it.
 */
public class GlintFruit extends LogiaFruit {
    public static final int COLOR = 0xFFE77A;
    public static final int WHITE = 0xFFFBE0;
    static final String M = "fruit/grandline:glint";

    public static final DeferredHolder<Ability, Yasakani> YASAKANI = ability("yasakani_no_magatama", Yasakani::new);
    public static final DeferredHolder<Ability, LightSword> AMA_NO_MURAKUMO = ability("ama_no_murakumo", LightSword::new);
    public static final DeferredHolder<Ability, LightKick> LIGHT_KICK = ability("light_speed_kick", LightKick::new);
    public static final DeferredHolder<Ability, Laser> LASER = ability("light_laser", Laser::new);
    public static final DeferredHolder<Ability, LightTravel> LIGHT_TRAVEL = ability("light_travel", LightTravel::new);
    public static final DeferredHolder<Ability, Amaterasu> AMATERASU = ability("amaterasu", Amaterasu::new);
    public static final DeferredHolder<Ability, LightRain> LIGHT_RAIN = ability("light_rain", LightRain::new);

    public static final ResourceLocation BULLET = GrandLine.id("magatama");
    public static final ResourceLocation RAIN_FIELD = GrandLine.id("light_rain");

    public GlintFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.LEGENDARY, "Pika Pika no Mi", COLOR)
                .abilities(YASAKANI, AMA_NO_MURAKUMO, LIGHT_KICK, LASER, LIGHT_TRAVEL)
                .awakened(AMATERASU, LIGHT_RAIN));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BULLET, Projectiles.blast(1.2, AbilityDamage.LIGHT, null, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && owner.getRandom().nextInt(3) == 0) {
                Vfx.impact(owner.serverLevel(), p, p.position(), COLOR, 0.8f, Kind.STAR);
            }
        }));
        // Lasers fall from above on random points of the field.
        ProjectileBehavior.Registry.register(RAIN_FIELD, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount % 3 != 0) {
                return;
            }
            double radius = p.size() / 2;
            double a = owner.getRandom().nextDouble() * Mth.TWO_PI;
            double d = Math.sqrt(owner.getRandom().nextDouble()) * radius;
            Vec3 ground = Kit.ground(owner.serverLevel(), p.position().add(Math.cos(a) * d, 4, Math.sin(a) * d), 12);
            Vfx.beam(owner.serverLevel(), p, ground.add(0, 14, 0), ground, COLOR, 0.6f, Kind.STAR, Vfx.BEAM_GLOW);
            for (LivingEntity target : AbilityDamage.around(owner, ground, 2.0)) {
                Kit.hit(owner, target, AbilityDamage.LIGHT, p.damage());
            }
        }));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(AbilityDamage.LIGHT);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hold to fire a stream of light bullets that burst where they land. */
    public static final class Yasakani extends Ability {
        Yasakani() {
            super(props(ActivationType.HOLD, AbilityIcon.STAR, COLOR).cooldown(160).stamina(14).staminaPerTick(0.5f)
                    .maxActive(50).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 2 == 0) {
                ServerPlayer player = ctx.player();
                Vec3 look = player.getLookAngle();
                Vec3 spread = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian(),
                        player.getRandom().nextGaussian()).scale(0.06);
                AbilityProjectile.spawn(player, BULLET, ctx.handPos(), look.add(spread).normalize().scale(2.4), 0.35f, COLOR,
                        3f * ctx.power(), 20);
                if (ctx.activeTicks() % 6 == 0) {
                    ctx.sound(GLSounds.GLINT_LASER.get(), 0.6f, 1.6f + player.getRandom().nextFloat() * 0.3f);
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Form a sword of light and cut in a wide arc. */
    public static final class LightSword extends Ability {
        LightSword() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, WHITE).cooldown(30).stamina(8).mastery(M, 10)
                    .masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5, 55)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 10f * ctx.power());
            }
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(2.2)).add(0, -0.3, 0);
            Vfx.slash(ctx.level(), player, front, player.getLookAngle(), COLOR, 2.4f, 2);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.GLINT_LASER.get(), 0.8f, 0.9f);
        }
    }

    /** Arrive at your target at the speed of light with a kick that sends it flying. */
    public static final class LightKick extends Ability {
        LightKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, COLOR).cooldown(80).stamina(14).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 26).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 26).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 away = player.position().subtract(target.position());
                Vec3 flat = new Vec3(away.x, 0, away.z);
                Vec3 dest = target.position().add(flat.lengthSqr() > 1.0E-4 ? flat.normalize().scale(1.4) : Vec3.ZERO);
                player.teleportTo(dest.x, dest.y, dest.z);
                Vfx.blink(ctx.level(), player, from, dest, COLOR, Kind.STAR);
                Kit.hit(player, target, AbilityDamage.LIGHT, 14f * ctx.power());
                AbilityDamage.knockback(target, dest, 2.4, 0.5);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), WHITE, 2f, Kind.STAR, Vfx.SHOCKWAVE | Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(GLSounds.GLINT_FLASH.get(), 1.2f, 1.2f);
        }
    }

    /** A laser from your fingertip that pierces everything in a long line. */
    public static final class Laser extends Ability {
        Laser() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, COLOR).cooldown(70).stamina(14).mastery(M, 35)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 32 * ctx.power();
            List<LivingEntity> line = AbilityDamage.line(player, range, 0.7);
            for (LivingEntity target : line) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 12f * ctx.power());
            }
            Vec3 end = AbilityDamage.lookTarget(player, range);
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, COLOR, 0.8f, Kind.STAR, Vfx.BEAM_GLOW);
            Vfx.burst(ctx.level(), player, end, COLOR, 2.5f, Kind.STAR, Vfx.FLASH | Vfx.SHOCKWAVE);
            for (LivingEntity target : AbilityDamage.around(player, end, 2.5)) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 6f * ctx.power());
            }
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(GLSounds.GLINT_LASER.get(), 1.4f, 1f);
        }
    }

    /** Become light and appear far ahead in an instant. */
    public static final class LightTravel extends Ability {
        LightTravel() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, WHITE).cooldown(50).stamina(10).mastery(M, 50)
                    .masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.blink(ctx.player(), 32 * ctx.power(), COLOR, Kind.STAR);
            ctx.sound(GLSounds.GLINT_FLASH.get(), 0.8f, 1.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A flash as bright as the sun: everyone around is blinded, burned and left glowing. */
    public static final class Amaterasu extends Ability {
        Amaterasu() {
            super(props(ActivationType.INSTANT, AbilityIcon.SUN, COLOR).cooldown(600).stamina(35).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.LIGHT, 7f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1.5, 0), WHITE, 16f, Kind.STAR,
                    Vfx.FLASH | Vfx.SHOCKWAVE | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GLINT_FLASH.get(), 2.5f, 0.7f);
        }
    }

    /** Lasers rain from the sky over an area for several seconds. */
    public static final class LightRain extends Ability {
        LightRain() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(700).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 40), 10);
            AbilityProjectile.zone(player, RAIN_FIELD, at, 10 * ctx.power(), COLOR, 6f * ctx.power(), 100);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GLINT_LASER.get(), 2f, 0.6f);
        }
    }
}
