package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Horo Horo no Mi. Ghosts that pass through bodies: negative ones drain every will to fight, exploding ones burst on
 * contact, and the user's own spirit can leave the body to roam.
 */
public class HollowFruit extends DevilFruit {
    public static final int COLOR = 0xF0B8D8;
    public static final int DARK = 0x6A3A6A;
    static final String M = "fruit/grandline:hollow";

    public static final DeferredHolder<Ability, NegativeHollow> NEGATIVE_HOLLOW = ability("hollow_negative", NegativeHollow::new);
    public static final DeferredHolder<Ability, GhostRap> GHOST_RAP = ability("hollow_ghost_rap", GhostRap::new);
    public static final DeferredHolder<Ability, MiniHollows> MINI_HOLLOWS = ability("hollow_mini_hollows", MiniHollows::new);
    public static final DeferredHolder<Ability, Astral> ASTRAL = ability("hollow_astral", Astral::new);
    public static final DeferredHolder<Ability, NegativeWave> NEGATIVE_WAVE = ability("hollow_negative_wave", NegativeWave::new);
    public static final DeferredHolder<Ability, GhostParade> GHOST_PARADE = ability("hollow_ghost_parade", GhostParade::new);
    public static final DeferredHolder<Ability, TokuHollow> TOKU_HOLLOW = ability("hollow_toku_hollow", TokuHollow::new);

    public static final ResourceLocation NEGATIVE = GrandLine.id("hollow_negative");
    public static final ResourceLocation RAP = GrandLine.id("hollow_rap");
    public static final ResourceLocation MINI = GrandLine.id("hollow_mini");
    public static final ResourceLocation TOKU = GrandLine.id("hollow_toku");

    public HollowFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Horo Horo no Mi", COLOR)
                .abilities(NEGATIVE_HOLLOW, GHOST_RAP, MINI_HOLLOWS, ASTRAL, NEGATIVE_WAVE)
                .awakened(GHOST_PARADE, TOKU_HOLLOW));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(NEGATIVE, Projectiles.homing(18, 0.2, Projectiles.bolt(AbilityDamage.DARK,
                (p, target) -> despair(target, 100))));
        ProjectileBehavior.Registry.register(RAP, Projectiles.homing(18, 0.25, Projectiles.blast(2.5, AbilityDamage.BLAST, null, p -> {
            if (p.level() instanceof ServerLevel level) {
                Vfx.burst(level, p, p.position(), COLOR, 2.5f, Kind.SMOKE, Vfx.SHOCKWAVE | Vfx.FLASH);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        })));
        ProjectileBehavior.Registry.register(MINI, Projectiles.homing(14, 0.3, Projectiles.bolt(AbilityDamage.DARK,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)))));
        ProjectileBehavior.Registry.register(TOKU, Projectiles.homing(24, 0.12, Projectiles.blast(5, AbilityDamage.BLAST,
                (p, target) -> despair(target, 80), p -> {
                    if (p.level() instanceof ServerLevel level) {
                        Vfx.burst(level, p, p.position(), COLOR, 5f, Kind.SMOKE, Vfx.SHOCKWAVE | Vfx.FLASH | Vfx.HEAVY);
                        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                                net.minecraft.sounds.SoundSource.PLAYERS, 2f, 0.8f);
                    }
                })));
    }

    /**
     * A negative ghost passes through: a beast slumps to the ground in despair; a player loses heart, half-blind,
     * slow and too weak to fight.
     */
    static void despair(LivingEntity target, int ticks) {
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 3));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0));
        } else if (!target.getType().is(Tags.EntityTypes.BOSSES)) {
            target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, ticks / 2, 0, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2));
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        }
        if (target.level() instanceof ServerLevel level) {
            Vfx.impact(level, target, target.getEyePosition(), DARK, 0.8f, Kind.SMOKE);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Send a ghost through a foe: it collapses in despair, too miserable to fight.
     *
     * @name Negative Hollow
     */
    public static final class NegativeHollow extends Ability {
        NegativeHollow() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, COLOR).cooldown(120).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, NEGATIVE, 0.7, 0.7f, COLOR, 2f, 60);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.VEX_AMBIENT, 1f, 1.2f);
        }
    }

    /**
     * Three grinning ghosts float after your foes and explode when they touch them.
     *
     * @name Ghost Rap
     */
    public static final class GhostRap extends Ability {
        GhostRap() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(90).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.3)).add(0, 0.15, 0).normalize();
                AbilityProjectile.spawn(player, RAP, ctx.handPos(), dir.scale(0.6), 0.6f, COLOR, 6f * ctx.power(), 60)
                        .withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.VEX_CHARGE, 1f, 1.2f);
        }
    }

    /**
     * A swarm of tiny ghosts nibbles at nearby foes, sapping their strength.
     *
     * @name Mini Hollows
     */
    public static final class MiniHollows extends Ability {
        MiniHollows() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(60).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Vec3 dir = new Vec3(Math.cos(a), 0.3, Math.sin(a)).normalize();
                AbilityProjectile.spawn(player, MINI, player.position().add(0, 1.5, 0), dir.scale(0.5), 0.3f, COLOR,
                        2.5f * ctx.power(), 50);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.VEX_AMBIENT, 1.2f, 1.8f);
        }
    }

    /** Leave your body behind as a ghost: invisible, floating, and free to roam for a while. */
    public static final class Astral extends Ability {
        Astral() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, COLOR).cooldown(300).stamina(16).staminaPerTick(0.15f)
                    .maxActive(400).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            ctx.sound(SoundEvents.VEX_AMBIENT, 1f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            player.resetFallDistance();
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 0.6f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
            ctx.player().removeEffect(MobEffects.INVISIBILITY);
        }
    }

    /** A wave of negative ghosts washes over everyone around you, crushing their will to fight. */
    public static final class NegativeWave extends Ability {
        NegativeWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, DARK).cooldown(260).stamina(22).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                despair(target, 120);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 6f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.VEX_DEATH, 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Hold to release a parade of ghosts that stream out and chase down everyone in sight. */
    public static final class GhostParade extends Ability {
        GhostParade() {
            super(props(ActivationType.HOLD, AbilityIcon.SKULL, COLOR).cooldown(500).stamina(24).staminaPerTick(0.4f)
                    .maxActive(60).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                double a = player.getRandom().nextDouble() * Math.PI * 2;
                Vec3 dir = player.getLookAngle().add(Math.cos(a) * 0.4, 0.3, Math.sin(a) * 0.4).normalize();
                ResourceLocation kind = ctx.activeTicks() % 12 == 0 ? NEGATIVE : RAP;
                AbilityProjectile.spawn(player, kind, ctx.handPos(), dir.scale(0.7), 0.5f, COLOR, 5f * ctx.power(), 60)
                        .withPower(ctx.power());
                ctx.sound(SoundEvents.VEX_CHARGE, 0.5f, 1.6f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /**
     * Send one huge ghost after your foes; it explodes in a great blast of despair.
     *
     * @name Toku Hollow
     */
    public static final class TokuHollow extends Ability {
        TokuHollow() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, DARK).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, TOKU, 0.5, 2.2f, COLOR, 16f, 120);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.VEX_CHARGE, 1.5f, 0.6f);
        }
    }
}
