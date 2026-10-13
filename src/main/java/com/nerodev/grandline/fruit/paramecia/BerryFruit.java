package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Beri Beri no Mi. The body splits into a cluster of berry-like balls: a blow finds nothing to hit as the balls
 * scatter, and they can roll, fly and smother on their own.
 */
public class BerryFruit extends DevilFruit {
    public static final int COLOR = 0x8A3A8A;
    public static final int LIGHT = 0xE070C0;
    static final String M = "fruit/grandline:berry";

    public static final DeferredHolder<Ability, Scatter> SCATTER = ability("berry_scatter", Scatter::new);
    public static final DeferredHolder<Ability, BallBarrage> BALL_BARRAGE = ability("berry_ball_barrage", BallBarrage::new);
    public static final DeferredHolder<Ability, BerryRoll> BERRY_ROLL = ability("berry_roll", BerryRoll::new);
    public static final DeferredHolder<Ability, BerryNet> BERRY_NET = ability("berry_net", BerryNet::new);
    public static final DeferredHolder<Ability, LooseBody> LOOSE_BODY = ability("berry_loose_body", LooseBody::new);
    public static final DeferredHolder<Ability, Swarm> SWARM = ability("berry_swarm", Swarm::new);
    public static final DeferredHolder<Ability, GiantBerry> GIANT_BERRY = ability("berry_giant_berry", GiantBerry::new);

    public static final ResourceLocation BALL = GrandLine.id("berry_ball");

    public BerryFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Beri Beri no Mi", COLOR)
                .abilities(SCATTER, BALL_BARRAGE, BERRY_ROLL, BERRY_NET, LOOSE_BODY)
                .awakened(SWARM, GIANT_BERRY));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BALL, Projectiles.bolt(AbilityDamage.STRIKE, null));
    }

    /** The body scatters around a blow: a quarter of physical hits miss entirely (most of them in Loose Body). */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (!event.getSource().is(AbilityDamage.PHYSICAL) || LogiaFruit.canTouch(event.getSource())) {
            return;
        }
        float chance = AbilityState.of(user).isActive(LOOSE_BODY.getId()) ? 0.6f : 0.25f;
        if (user.getRandom().nextFloat() < chance) {
            event.setCanceled(true);
            Vfx.burst(user.serverLevel(), user, user.getBoundingBox().getCenter(), LIGHT, 1.2f, Kind.BUBBLE, 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Scatter into balls and reassemble right behind the nearest foe, striking as you form. */
    public static final class Scatter extends Ability {
        Scatter() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, LIGHT).cooldown(80).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return AbilityDamage.around(player, player.position(), 12).isEmpty() ? Optional.of(Component.literal("No foe nearby"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            LivingEntity target = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : AbilityDamage.around(player, player.position(), 12)) {
                double d = e.distanceToSqr(player);
                if (d < best) {
                    best = d;
                    target = e;
                }
            }
            if (target == null) {
                return;
            }
            Vec3 from = player.position();
            Vec3 behind = target.position().add(target.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.5));
            player.teleportTo(behind.x, target.getY(), behind.z);
            Kit.hit(player, target, AbilityDamage.STRIKE, 5f * ctx.power());
            Vfx.blink(ctx.level(), player, from, player.position(), LIGHT, Kind.BUBBLE);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.SLIME_JUMP_SMALL, 1.2f, 1.2f);
        }
    }

    /** Fling a handful of your own body's balls at your foes. */
    public static final class BallBarrage extends Ability {
        BallBarrage() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < 6; i++) {
                Vec3 spread = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian() * 0.5,
                        player.getRandom().nextGaussian()).scale(0.07);
                AbilityProjectile.spawn(player, BALL, ctx.handPos(), player.getLookAngle().add(spread).normalize().scale(1.5),
                        0.4f, i % 2 == 0 ? COLOR : LIGHT, 3f * ctx.power(), 25).withGravity(0.03);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_SQUISH_SMALL, 1.2f, 1f);
        }
    }

    /** Your balls roll out in a line, bowling over and tripping everyone in the way. */
    public static final class BerryRoll extends Ability {
        BerryRoll() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, LIGHT).cooldown(80).stamina(12).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.2)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 7f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            Vec3 start = player.position().add(0, 0.3, 0);
            Vfx.trail(ctx.level(), player, start, start.add(ctx.horizontalLook().scale(range)), LIGHT, 0.8f, Kind.BUBBLE);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1f, 1.2f);
        }
    }

    /** Balls cluster around a foe like a sticky net and pin it in place. */
    public static final class BerryNet extends Ability {
        BerryNet() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(160).stamina(14).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.CANDY, 50);
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), LIGHT, 0.7f, Kind.BUBBLE);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 1f, 1.2f);
        }
    }

    /** Hold your body loosely together: most blows pass between the balls, and you move quicker. */
    public static final class LooseBody extends Buffs.Toggle {
        LooseBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, LIGHT).cooldown(240).stamina(12).staminaPerTick(0.25f)
                            .maxActive(300).mastery(M, 35).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("berry_loose_speed"), 0.15,
                            Operation.ADD_MULTIPLIED_BASE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), LIGHT, 1.4f, Kind.BUBBLE, 0);
            ctx.sound(SoundEvents.SLIME_SQUISH_SMALL, 1f, 0.8f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.BUBBLE);
            }
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Hold to dissolve into a swarm of hundreds of balls that whirl around you and pound everything nearby. */
    public static final class Swarm extends Ability {
        Swarm() {
            super(props(ActivationType.HOLD, AbilityIcon.TORNADO, COLOR).cooldown(500).stamina(26).staminaPerTick(0.4f)
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
                for (LivingEntity target : AbilityDamage.around(player, center, 6 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                }
                Vfx.spiral(ctx.level(), player, center, LIGHT, 5f, Kind.BUBBLE);
                ctx.sound(SoundEvents.SLIME_SQUISH_SMALL, 0.6f, 1.4f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Pack every ball together into one giant berry and drop it on the spot you point at. */
    public static final class GiantBerry extends Ability {
        GiantBerry() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, LIGHT).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 3.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 16f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            Vfx.trail(ctx.level(), player, at.add(0, 12, 0), at, LIGHT, 2.5f, Kind.BUBBLE);
            Vfx.burst(ctx.level(), player, at, COLOR, 3.5f, Kind.BUBBLE, Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_BLOCK_FALL, 2f, 0.5f);
        }
    }
}
