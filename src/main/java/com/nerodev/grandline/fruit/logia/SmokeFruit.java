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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Moku Moku no Mi. A body of smoke: it cannot be crushed or hurt by falling, it binds anything it wraps around,
 * and it can carry its user through the air. Awakened smoke seals whole areas.
 */
public class SmokeFruit extends LogiaFruit {
    public static final int COLOR = 0xD8DCE0;
    static final String M = "fruit/grandline:smoke";

    public static final DeferredHolder<Ability, WhiteBlow> WHITE_BLOW = ability("white_blow", WhiteBlow::new);
    public static final DeferredHolder<Ability, WhiteSnake> WHITE_SNAKE = ability("white_snake", WhiteSnake::new);
    public static final DeferredHolder<Ability, WhiteLauncher> WHITE_LAUNCHER = ability("white_launcher", WhiteLauncher::new);
    public static final DeferredHolder<Ability, WhiteOut> WHITE_OUT = ability("white_out", WhiteOut::new);
    public static final DeferredHolder<Ability, SmokeRide> SMOKE_RIDE = ability("smoke_ride", SmokeRide::new);
    public static final DeferredHolder<Ability, WhitePrison> WHITE_PRISON = ability("white_prison", WhitePrison::new);
    public static final DeferredHolder<Ability, WhiteVortex> WHITE_VORTEX = ability("white_vortex", WhiteVortex::new);

    public static final ResourceLocation BLOW = GrandLine.id("white_blow");
    public static final ResourceLocation SNAKE = GrandLine.id("white_snake");
    public static final ResourceLocation SCREEN = GrandLine.id("white_out");

    public SmokeFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.EPIC, "Moku Moku no Mi", COLOR)
                .abilities(WHITE_BLOW, WHITE_SNAKE, WHITE_LAUNCHER, WHITE_OUT, SMOKE_RIDE)
                .awakened(WHITE_PRISON, WHITE_VORTEX));
    }

    static void init() {}

    static void behaviors() {
        // The fist of smoke grabs what it hits and drags it back.
        ProjectileBehavior.Registry.register(BLOW, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.STRINGS, 50);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.pull(target, owner.position(), 0.9);
                Vfx.burst(owner.serverLevel(), target, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.SMOKE, 0);
            }
        }));
        ProjectileBehavior.Registry.register(SNAKE, Projectiles.homing(14, 0.25, Projectiles.bolt(AbilityDamage.STRIKE,
                (p, target) -> Kit.root(target, Vfx.StatusLook.STRINGS, 70))));
        ProjectileBehavior.Registry.register(SCREEN, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 10 == 0 && owner.position().distanceTo(p.position()) < p.size() / 2) {
                owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, true));
            }
        }));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A fist of smoke that seizes whoever it hits and drags them back to you. */
    public static final class WhiteBlow extends Ability {
        WhiteBlow() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(30).stamina(10).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BLOW, 1.4, 0.9f, COLOR, 6f, 22);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1f, 1.1f);
        }
    }

    /** Three serpents of smoke hunt the nearest enemies and coil around their legs. */
    public static final class WhiteSnake extends Ability {
        WhiteSnake() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(80).stamina(16).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            Vec3 side = ctx.right();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = look.add(side.scale(i * 0.35)).normalize();
                AbilityProjectile.spawn(player, SNAKE, ctx.handPos(), dir.scale(0.8), 0.6f, COLOR, 4f * ctx.power(), 50);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SMOKE_BIND.get(), 1f, 1f);
        }
    }

    /** Turn into a jet of smoke and blast forward, bowling over anything in the way. */
    public static final class WhiteLauncher extends Ability {
        WhiteLauncher() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH, COLOR).cooldown(60).stamina(12).maxActive(10)
                    .mastery(M, 20).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.dash(player, 2.0, 0.25);
            ctx.state().evadeFor(ctx.level().getGameTime(), 10);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1.2f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = player.position().add(0, 1, 0);
            for (LivingEntity target : AbilityDamage.around(player, at, 1.8)) {
                if (AbilityDamage.hurt(player, target, AbilityDamage.STRIKE, 7f * ctx.power(), player)) {
                    AbilityDamage.knockback(target, player.position(), 1.2, 0.35);
                }
            }
            Vfx.trail(ctx.level(), player, at.subtract(player.getDeltaMovement()), at, COLOR, 1.4f, Kind.SMOKE);
            return true;
        }
    }

    /** Flood the area with smoke: enemies inside are blinded and slowed, and you vanish within it. */
    public static final class WhiteOut extends Ability {
        WhiteOut() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(300).stamina(25).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, SCREEN, player.position(), 6 * ctx.power(), COLOR, 0, 160);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 5f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1.4f, 0.6f);
        }
    }

    /** Become a drifting cloud and fly. Drains stamina while airborne. */
    public static final class SmokeRide extends Ability {
        SmokeRide() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TORNADO, COLOR).cooldown(100).stamina(10)
                    .staminaPerTick(0.12f).maxActive(20 * 60).mastery(M, 50).masteryGain(0.4));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            ctx.player().setDeltaMovement(ctx.player().getDeltaMovement().add(0, 0.6, 0));
            ctx.player().hurtMarked = true;
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1f, 1.3f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 3 == 0) {
                Vec3 feet = player.position();
                Vfx.trail(ctx.level(), player, feet.add(0, 0.3, 0), feet.subtract(player.getDeltaMovement().scale(3)), COLOR,
                        1.6f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
            ctx.player().addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, true));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Smoke engulfs a whole area and hardens around everyone in it. */
    public static final class WhitePrison extends Ability {
        WhitePrison() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(400).stamina(35).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.aimPoint(player, 24);
            for (LivingEntity target : AbilityDamage.around(player, at, 5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.SMOKE, 80);
            }
            Vfx.burst(ctx.level(), player, at, COLOR, 6f, Kind.SMOKE, Vfx.RING | Vfx.SHOCKWAVE);
            Vfx.beam(ctx.level(), player, ctx.handPos(), at, COLOR, 1.2f, Kind.SMOKE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SMOKE_BIND.get(), 1.5f, 0.6f);
        }
    }

    /** Hold to become a cyclone of smoke that drags everything nearby into you. */
    public static final class WhiteVortex extends Ability {
        WhiteVortex() {
            super(props(ActivationType.HOLD, AbilityIcon.TORNADO, COLOR).cooldown(300).stamina(20).staminaPerTick(0.5f)
                    .maxActive(80).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SMOKE_WHOOSH.get(), 1.5f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.pull(target, player.position(), 0.45);
                if (ctx.activeTicks() % 5 == 0) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 2f * ctx.power());
                }
            }
            if (ctx.activeTicks() % 4 == 0) {
                Vfx.spiral(ctx.level(), player, player.position(), COLOR, (float) radius * 0.5f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }
}
