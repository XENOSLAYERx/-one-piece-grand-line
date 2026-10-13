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
import com.nerodev.grandline.effect.GLMobEffects;
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
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Sube Sube no Mi. Perfectly smooth skin: blows and bullets slide off, nothing can get a grip on you, and you glide
 * over the ground like oil on glass.
 */
public class SlipFruit extends DevilFruit {
    public static final int COLOR = 0xFFC8E8;
    public static final int SHEEN = 0xFFF0FA;
    static final String M = "fruit/grandline:slip";
    static final String COUNTER = "grandline_slip_counter";

    public static final DeferredHolder<Ability, Spur> SPUR = ability("slip_spur", Spur::new);
    public static final DeferredHolder<Ability, SlickSkin> SLICK_SKIN = ability("slip_slick_skin", SlickSkin::new);
    public static final DeferredHolder<Ability, OilSlick> OIL_SLICK = ability("slip_oil_slick", OilSlick::new);
    public static final DeferredHolder<Ability, Wriggle> WRIGGLE = ability("slip_wriggle", Wriggle::new);
    public static final DeferredHolder<Ability, Counter> SLIP_COUNTER = ability("slip_counter", Counter::new);
    public static final DeferredHolder<Ability, Frictionless> FRICTIONLESS = ability("slip_frictionless", Frictionless::new);
    public static final DeferredHolder<Ability, Spiral> SPIRAL = ability("slip_spiral", Spiral::new);

    public static final ResourceLocation SLICK = GrandLine.id("slip_slick");
    public static final ResourceLocation WORLD = GrandLine.id("slip_frictionless");

    public SlipFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Sube Sube no Mi", COLOR)
                .abilities(SPUR, SLICK_SKIN, OIL_SLICK, WRIGGLE, SLIP_COUNTER)
                .awakened(FRICTIONLESS, SPIRAL));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SLICK, Projectiles.zone(10, (p, target) -> slide(target, 0.45), null));
        ProjectileBehavior.Registry.register(WORLD, Projectiles.zone(10, (p, target) -> {
            slide(target, 0.6);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 2));
        }, null));
    }

    /** Knocks a creature's feet out from under it: it skids in a random direction and can barely fight. */
    static void slide(LivingEntity target, double strength) {
        double a = target.getRandom().nextDouble() * Math.PI * 2;
        Vec3 skid = new Vec3(Math.cos(a) * strength, 0.05, Math.sin(a) * strength);
        Kit.launch(target, target.getDeltaMovement().scale(0.5).add(skid));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
    }

    /** Arrows and bullets glance off; some blows slide away entirely (always while Slick Skin or a counter is up). */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (LogiaFruit.canTouch(event.getSource())) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount(event.getAmount() * 0.4f);
            return;
        }
        if (!event.getSource().is(AbilityDamage.PHYSICAL)) {
            return;
        }
        long now = user.level().getGameTime();
        boolean countering = user.getPersistentData().getLong(COUNTER) >= now;
        boolean slick = AbilityState.of(user).isActive(SLICK_SKIN.getId());
        if (countering || slick || user.getRandom().nextFloat() < 0.2f) {
            event.setCanceled(true);
            Vfx.burst(user.serverLevel(), user, user.getBoundingBox().getCenter(), SHEEN, 1f, Kind.STAR, 0);
            if (countering && event.getSource().getEntity() instanceof LivingEntity attacker) {
                user.getPersistentData().remove(COUNTER);
                AbilityDamage.knockback(attacker, user.position(), 1.2, 0.3);
                attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                Kit.hit(user, attacker, AbilityDamage.STRIKE, 6f);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Skate forward on frictionless skin, bowling over everyone in your way.
     *
     * @name Sube Sube Spur
     */
    public static final class Spur extends Ability {
        Spur() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 9 * ctx.power(), 1.0)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.0, 0.4);
            }
            Vec3 dir = ctx.horizontalLook();
            Kit.launch(player, dir.scale(1.6).add(0, 0.08, 0));
            player.resetFallDistance();
            Vfx.trail(ctx.level(), player, player.position(), player.position().add(dir.scale(9)), SHEEN, 0.6f, Kind.STAR);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(SoundEvents.ICE_BREAK, 0.6f, 1.8f);
        }
    }

    /** Turn your skin truly frictionless: you move faster and every blow slides off you. */
    public static final class SlickSkin extends Buffs.Toggle {
        SlickSkin() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, SHEEN).cooldown(240).stamina(12).staminaPerTick(0.3f)
                            .maxActive(300).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("slip_slick_speed"), 0.25, Operation.ADD_MULTIPLIED_BASE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHEEN, 1.5f, Kind.STAR, Vfx.FLASH);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHEEN, 0.6f, Kind.STAR);
            }
            return true;
        }
    }

    /** Smear the ground ahead with slick oil from your skin: anyone on it skids helplessly around. */
    public static final class OilSlick extends Ability {
        OilSlick() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(200).stamina(16).mastery(M, 15)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, SLICK, at, 5 * ctx.power(), SHEEN, 0f, 160);
            Vfx.burst(ctx.level(), player, at, SHEEN, 5f, Kind.STAR, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.HONEY_BLOCK_SLIDE, 1.2f, 1.4f);
        }
    }

    /** Slip out of anything that holds you: bindings, casings and slowness all slide off, and you dart away. */
    public static final class Wriggle extends Ability {
        Wriggle() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, SHEEN).cooldown(300).stamina(16).mastery(M, 25)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.removeEffect(GLMobEffects.ROOTED);
            player.removeEffect(GLMobEffects.ENCASED);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.LEVITATION);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), SHEEN, 1.4f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(GLSounds.EVADE.get(), 1f, 1.3f);
        }
    }

    /** For three seconds, the next blow that reaches you slides off and leaves its owner stumbling. */
    public static final class Counter extends Ability {
        Counter() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(14).mastery(M, 35)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.player().getPersistentData().putLong(COUNTER, ctx.level().getGameTime() + 60);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHEEN, 1.2f, Kind.RING, 0);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Take the friction out of everything around you: foes skid, stumble and cannot land a solid blow. */
    public static final class Frictionless extends Ability {
        Frictionless() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, SHEEN).cooldown(700).stamina(36).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            AbilityProjectile.zone(player, WORLD, player.position(), radius, SHEEN, 0f, 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), SHEEN, (float) radius, Kind.STAR,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.2f);
        }
    }

    /**
     * Hold to spin across the ground in a frictionless whirl that batters everything around.
     *
     * @name Sube Sube Spiral
     */
    public static final class Spiral extends Ability {
        Spiral() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, COLOR).cooldown(400).stamina(24).staminaPerTick(0.35f)
                    .maxActive(60).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_SWEEP);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Kit.launch(player, new Vec3(dir.x * 0.5, player.getDeltaMovement().y, dir.z * 0.5));
            if (ctx.activeTicks() % 5 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position().add(0, 1, 0), 4 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                    AbilityDamage.knockback(target, player.position(), 0.8, 0.3);
                }
                Vfx.spiral(ctx.level(), player, player.position().add(0, 0.6, 0), SHEEN, 2.5f, Kind.STAR);
                ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.6f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }
}
