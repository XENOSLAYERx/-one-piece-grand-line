package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Sui Sui no Mi. Swim through the ground and walls as if they were water: slip beneath the surface, surge up under a
 * foe, drag it down by the ankles.
 */
public class SwimFruit extends DevilFruit {
    public static final int COLOR = 0x4AA8E8;
    public static final int SAND = 0xF0E8A0;
    static final String M = "fruit/grandline:swim";

    public static final DeferredHolder<Ability, GroundSwim> GROUND_SWIM = ability("swim_ground_swim", GroundSwim::new);
    public static final DeferredHolder<Ability, Surface> SURFACE = ability("swim_surface", Surface::new);
    public static final DeferredHolder<Ability, DiveBomb> DIVE_BOMB = ability("swim_dive_bomb", DiveBomb::new);
    public static final DeferredHolder<Ability, WallSwim> WALL_SWIM = ability("swim_wall_swim", WallSwim::new);
    public static final DeferredHolder<Ability, DragUnder> DRAG_UNDER = ability("swim_drag_under", DragUnder::new);
    public static final DeferredHolder<Ability, LiquidGround> LIQUID_GROUND = ability("swim_liquid_ground", LiquidGround::new);
    public static final DeferredHolder<Ability, Breach> BREACH = ability("swim_breach", Breach::new);

    public static final ResourceLocation POOL = GrandLine.id("swim_pool");

    public SwimFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Sui Sui no Mi", COLOR)
                .abilities(GROUND_SWIM, SURFACE, DIVE_BOMB, WALL_SWIM, DRAG_UNDER)
                .awakened(LIQUID_GROUND, BREACH));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(POOL, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
            if (p.tickCount % 40 == 0) {
                Kit.root(target, Vfx.StatusLook.MUD, 25);
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Slip beneath the ground and swim: hidden, fast and hard to hurt, though you can't strike from down there. */
    public static final class GroundSwim extends Ability {
        GroundSwim() {
            super(props(ActivationType.TOGGLE, AbilityIcon.WAVE, COLOR).cooldown(100).stamina(10).staminaPerTick(0.15f)
                    .maxActive(600).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24), m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
            Vfx.burst(ctx.level(), player, player.position(), SAND, 1.6f, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.sound(SoundEvents.PLAYER_SPLASH, 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 4, false, false));
            if (ctx.activeTicks() % 3 == 0 && player.onGround()) {
                Vfx.impact(ctx.level(), player, player.position().add(0, 0.05, 0), SAND, 0.8f, Kind.DROP);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            player.removeEffect(MobEffects.INVISIBILITY);
            player.removeEffect(MobEffects.WEAKNESS);
            Vfx.burst(ctx.level(), player, player.position(), SAND, 1.6f, Kind.DROP, Vfx.GROUND);
        }
    }

    /** Surge up out of the ground right under the foe you point at, uppercutting it into the air. */
    public static final class Surface extends Ability {
        Surface() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, SAND).cooldown(90).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 at = target.position().add(target.getLookAngle().multiply(1, 0, 1).scale(0.8));
                player.teleportTo(at.x, target.getY(), at.z);
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.2, 0));
                Vfx.trail(ctx.level(), player, from, player.position(), SAND, 0.6f, Kind.DROP);
                Vfx.burst(ctx.level(), player, player.position(), SAND, 1.8f, Kind.DROP, Vfx.GROUND | Vfx.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1f, 1f);
        }
    }

    /** Leap from the ground and dive headfirst back into it at the spot you point at, like a diver into a pool. */
    public static final class DiveBomb extends Ability {
        DiveBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(120).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 16), 8);
            Vec3 from = player.position();
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 3 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 10f * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(0.5).add(0, 0.6, 0));
            }
            Vfx.trail(ctx.level(), player, from.add(0, 4, 0), at, COLOR, 0.8f, Kind.DROP);
            Vfx.burst(ctx.level(), player, at, SAND, 3f, Kind.DROP, Vfx.SHOCKWAVE | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.4f, 0.8f);
        }
    }

    /** Swim straight through the wall in front of you and come out the other side. */
    public static final class WallSwim extends Ability {
        WallSwim() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(60).stamina(10).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 to = DoorFruit.farthestFree(player, player.position(), dir, 8);
            if (to == null) {
                return;
            }
            Vec3 from = player.position();
            player.teleportTo(to.x, to.y, to.z);
            Vfx.blink(ctx.level(), player, from, to, SAND, Kind.DROP);
            ctx.sound(SoundEvents.PLAYER_SPLASH, 1f, 1.2f);
        }
    }

    /** Grab a foe's ankles from below and drag it half into the ground, where it sticks. */
    public static final class DragUnder extends Ability {
        DragUnder() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, SAND).cooldown(160).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.MUD, 70);
                Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                Vfx.burst(ctx.level(), target, target.position(), SAND, 1.4f, Kind.DROP, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.PLAYER_SPLASH, 1.2f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground around you liquid: foes wade and sink, getting stuck again and again. */
    public static final class LiquidGround extends Ability {
        LiquidGround() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, SAND).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, POOL, player.position(), radius, COLOR, 0f, 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.1, 0), COLOR, (float) radius, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PLAYER_SPLASH, 2f, 0.5f);
        }
    }

    /** Burst out of the ground in a long leaping arc, crashing through every foe along the way. */
    public static final class Breach extends Ability {
        Breach() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 14 * ctx.power(), 1.6)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.0, 0));
            }
            Kit.launch(player, ctx.horizontalLook().scale(1.6).add(0, 0.9, 0));
            player.resetFallDistance();
            Vfx.burst(ctx.level(), player, player.position(), SAND, 2.5f, Kind.DROP, Vfx.SHOCKWAVE | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.6f, 0.7f);
        }
    }
}
