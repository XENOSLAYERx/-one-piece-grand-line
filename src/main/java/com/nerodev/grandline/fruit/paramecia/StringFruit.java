package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

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
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Ito Ito no Mi. Strings fine enough to be invisible and strong enough to cut steel: whips, bullets, puppet strings
 * that take over a body, clouds to walk on, and a cage of strings that slowly closes over everything.
 */
public class StringFruit extends DevilFruit {
    public static final int COLOR = 0xF0A0C8;
    public static final int THREAD = 0xFFF4FA;
    public static final int HEAT = 0xFF6A8A;
    static final String M = "fruit/grandline:string";

    public static final DeferredHolder<Ability, Overheat> OVERHEAT = ability("string_overheat", Overheat::new);
    public static final DeferredHolder<Ability, BulletString> BULLET_STRING = ability("string_bullet_string", BulletString::new);
    public static final DeferredHolder<Ability, Parasite> PARASITE = ability("string_parasite", Parasite::new);
    public static final DeferredHolder<Ability, SkyWalk> SKY_WALK = ability("string_sky_walk", SkyWalk::new);
    public static final DeferredHolder<Ability, FiveColorStrings> FIVE_COLOR = ability("string_five_color", FiveColorStrings::new);
    public static final DeferredHolder<Ability, Birdcage> BIRDCAGE = ability("string_birdcage", Birdcage::new);
    public static final DeferredHolder<Ability, GodThread> GOD_THREAD = ability("string_god_thread", GodThread::new);

    public static final ResourceLocation SHOT = GrandLine.id("string_shot");
    public static final ResourceLocation CAGE = GrandLine.id("string_birdcage");

    public StringFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Ito Ito no Mi", COLOR)
                .abilities(OVERHEAT, BULLET_STRING, PARASITE, SKY_WALK, FIVE_COLOR)
                .awakened(BIRDCAGE, GOD_THREAD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SHOT, Projectiles.piercing(AbilityDamage.SLASH, null));
        // The Birdcage: a cage of strings that shrinks to 40% of its size; touching its edge cuts and throws you back in.
        ProjectileBehavior.Registry.register(CAGE, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, p -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner == null || p.tickCount % 5 != 0) {
                return;
            }
            double start = p.extra().getDouble("radius");
            if (start <= 0) {
                start = p.size() / 2.0;
                p.extra().putDouble("radius", start);
            }
            double radius = start * (1 - 0.6 * Math.min(1, p.tickCount / (double) Math.max(1, p.lifetime())));
            p.setSize((float) (radius * 2));
            for (LivingEntity target : AbilityDamage.around(owner, p.position(), start * 1.2)) {
                Vec3 flat = new Vec3(target.getX() - p.getX(), 0, target.getZ() - p.getZ());
                if (flat.length() > radius - 1) {
                    Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
                    Vec3 in = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(-0.9);
                    Kit.launch(target, in.add(0, 0.25, 0));
                }
            }
        }));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Lash out with a red-hot rope of string that cuts and burns everything along its length. */
    public static final class Overheat extends Ability {
        Overheat() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, HEAT).cooldown(70).stamina(12).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 18 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.7)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), HEAT, 0.3f, Kind.FLAME,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.FIRECHARGE_USE, 1f, 1.4f);
        }
    }

    /** Hold to flick hardened string bullets from your fingertips; each one pierces straight through. */
    public static final class BulletString extends Ability {
        BulletString() {
            super(props(ActivationType.HOLD, AbilityIcon.BEAM, THREAD).cooldown(90).stamina(8).staminaPerTick(0.3f).maxActive(40)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 == 0) {
                ServerPlayer player = ctx.player();
                Vec3 spread = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian(),
                        player.getRandom().nextGaussian()).scale(0.03);
                AbilityProjectile.spawn(player, SHOT, ctx.handPos(), player.getLookAngle().add(spread).normalize().scale(2.6),
                        0.25f, THREAD, 3f * ctx.power(), 16);
                ctx.sound(SoundEvents.ARROW_SHOOT, 0.5f, 1.8f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Sink invisible strings into a foe: it is held in place, and a beast turns on the nearest creature beside it. */
    public static final class Parasite extends Ability {
        Parasite() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, THREAD).cooldown(200).stamina(18).mastery(M, 15)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 22).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 22).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.STRINGS, 90);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                if (target instanceof Mob mob) {
                    LivingEntity victim = null;
                    double best = 12 * 12;
                    for (LivingEntity other : ctx.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(12),
                            e -> e != mob && e != player && e.isAlive() && !e.isSpectator())) {
                        double d = other.distanceToSqr(mob);
                        if (d < best) {
                            best = d;
                            victim = other;
                        }
                    }
                    mob.setTarget(victim);
                }
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), THREAD, 0.08f, Kind.SPARK,
                        Vfx.BEAM_GLOW);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.CROSSBOW_LOADING_END.value(), 1f, 1.6f);
        }
    }

    /** Hook your strings onto the clouds and walk on the air. */
    public static final class SkyWalk extends Ability {
        SkyWalk() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, THREAD).cooldown(120).stamina(10).staminaPerTick(0.18f)
                    .maxActive(900).mastery(M, 25).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            ctx.sound(SoundEvents.CROSSBOW_LOADING_START.value(), 1f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.resetFallDistance();
            if (ctx.activeTicks() % 10 == 0 && !player.onGround()) {
                Vec3 top = player.position().add(0, 1.8, 0);
                Vfx.beam(ctx.level(), player, top, top.add(0, 10, 0), THREAD, 0.05f, Kind.SPARK, Vfx.BEAM_GLOW);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /**
     * Five strings slash down from above where you point, cutting through everyone beneath.
     *
     * @name Five-Color Strings
     */
    public static final class FiveColorStrings extends Ability {
        FiveColorStrings() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(140).stamina(20).mastery(M, 35)
                    .masteryGain(1.1));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 3.2 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.SLASH, 11f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            int[] colors = {0xFF5A7A, 0xFFB04A, 0xFFF06A, 0x6AE08A, 0x6AA8FF};
            for (int i = 0; i < 5; i++) {
                Vec3 ground = at.add((i - 2) * 0.8, 0, (i % 2 == 0 ? 0.6 : -0.6));
                Vfx.beam(ctx.level(), player, ground.add(0, 12, 0), ground, colors[i], 0.12f, Kind.SPARK, Vfx.BEAM_GLOW);
            }
            Vfx.burst(ctx.level(), player, at, COLOR, 3f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Drop a cage of strings over the whole area. It closes slowly, cutting anyone who touches its walls. */
    public static final class Birdcage extends Ability {
        Birdcage() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, THREAD).cooldown(900).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, CAGE);
            double radius = 22 * ctx.power();
            AbilityProjectile.zone(player, CAGE, player.position(), radius, THREAD, 4f * ctx.power(), 400);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), THREAD, (float) radius, Kind.SPARK, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.CROSSBOW_LOADING_END.value(), 2f, 0.5f);
        }
    }

    /** Twist a thousand strings into one spear and drive it through everything in a line. */
    public static final class GodThread extends Ability {
        GodThread() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, THREAD).cooldown(500).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 26 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.6)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 20f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.2, 0.2);
            }
            Vec3 end = AbilityDamage.lookTarget(player, range);
            ServerLevel level = ctx.level();
            Vfx.beam(level, player, ctx.handPos(), end, THREAD, 1.2f, Kind.SPARK, Vfx.BEAM_GLOW);
            Vfx.beam(level, player, ctx.handPos(), end, COLOR, 0.5f, Kind.SPARK, Vfx.BEAM_PARTICLES);
            Vfx.impact(level, player, end, THREAD, 2f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1.6f, 0.6f);
        }
    }
}
