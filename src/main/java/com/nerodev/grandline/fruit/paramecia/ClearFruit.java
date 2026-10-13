package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Suke Suke no Mi. Become invisible, along with whatever you touch: vanish, strike from nowhere, and leave foes
 * swinging at empty air.
 */
public class ClearFruit extends DevilFruit {
    public static final int COLOR = 0xE8F4F8;
    public static final int SHIMMER = 0xB8D0D8;
    static final String M = "fruit/grandline:clear";

    public static final DeferredHolder<Ability, Vanish> VANISH = ability("clear_vanish", Vanish::new);
    public static final DeferredHolder<Ability, UnseenStrike> UNSEEN_STRIKE = ability("clear_unseen_strike", UnseenStrike::new);
    public static final DeferredHolder<Ability, Veil> VEIL = ability("clear_veil", Veil::new);
    public static final DeferredHolder<Ability, PhaseStep> PHASE_STEP = ability("clear_phase_step", PhaseStep::new);
    public static final DeferredHolder<Ability, BlindSpot> BLIND_SPOT = ability("clear_blind_spot", BlindSpot::new);
    public static final DeferredHolder<Ability, ClearWorld> CLEAR_WORLD = ability("clear_world", ClearWorld::new);
    public static final DeferredHolder<Ability, InvisibleBlade> INVISIBLE_BLADE = ability("clear_invisible_blade", InvisibleBlade::new);

    public ClearFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Suke Suke no Mi", COLOR)
                .abilities(VANISH, UNSEEN_STRIKE, VEIL, PHASE_STEP, BLIND_SPOT)
                .awakened(CLEAR_WORLD, INVISIBLE_BLADE));
    }

    static void init() {}

    static void behaviors() {}

    static boolean hidden(ServerPlayer player) {
        return AbilityState.of(player).isActive(VANISH.getId()) || player.hasEffect(MobEffects.INVISIBILITY);
    }

    /** Every creature hunting {@code player} within {@code range} loses track of it. */
    static void loseTrack(ServerPlayer player, double range) {
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range),
                m -> m.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Turn completely invisible. Monsters more than two blocks away lose track of you while it lasts. */
    public static final class Vanish extends Ability {
        Vanish() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, COLOR).cooldown(100).stamina(10).staminaPerTick(0.12f)
                    .maxActive(1200).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHIMMER, 1.2f, Kind.STAR, 0);
            ctx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1.4f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
            if (ctx.activeTicks() % 10 == 0) {
                for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24),
                        m -> m.getTarget() == player && m.distanceToSqr(player) > 4)) {
                    mob.setTarget(null);
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ctx.player().removeEffect(MobEffects.INVISIBILITY);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SHIMMER, 1.2f, Kind.STAR, 0);
        }
    }

    /** Strike the foe in front of you. From invisibility the blow lands where nobody saw it coming, for double damage. */
    public static final class UnseenStrike extends Ability {
        UnseenStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, SHIMMER).cooldown(40).stamina(8).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float damage = (hidden(player) ? 14f : 7f) * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.SLASH, damage);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), player.getLookAngle(), SHIMMER, 1.4f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.6f);
        }
    }

    /** Lay a hand on everyone beside you and share your power: you and nearby players turn invisible for fifteen seconds. */
    public static final class Veil extends Ability {
        Veil() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(400).stamina(16).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Player other : ctx.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(6))) {
                other.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0));
                Vfx.impact(ctx.level(), other, other.getBoundingBox().getCenter(), SHIMMER, 1f, Kind.STAR);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 1f, 1.2f);
        }
    }

    /** Vanish and reappear right behind the foe you point at. */
    public static final class PhaseStep extends Ability {
        PhaseStep() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, SHIMMER).cooldown(80).stamina(12).mastery(M, 20)
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
                Vec3 from = player.position();
                Vec3 facing = target.getLookAngle().multiply(1, 0, 1);
                Vec3 behind = target.position().subtract(facing.lengthSqr() < 1.0E-4 ? Vec3.ZERO : facing.normalize().scale(1.5));
                player.teleportTo(behind.x, target.getY(), behind.z);
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                Vfx.blink(ctx.level(), player, from, player.position(), SHIMMER, Kind.STAR);
            });
            ctx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1.6f);
        }
    }

    /** Fade from every eye around you: monsters lose your trail and are left half-blind. */
    public static final class BlindSpot extends Ability {
        BlindSpot() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, SHIMMER).cooldown(240).stamina(16).mastery(M, 35)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            loseTrack(player, 16);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            }
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), SHIMMER, 6f, Kind.STAR, Vfx.RING);
            ctx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Everything near you goes clear: allies vanish, foes lose sight of the world around them. */
    public static final class ClearWorld extends Ability {
        ClearWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, SHIMMER).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            for (Player other : ctx.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius))) {
                if (!AbilityDamage.canHarm(player, other)) {
                    other.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0));
                }
            }
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), SHIMMER, (float) radius, Kind.STAR, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 2f, 0.6f);
        }
    }

    /** An invisible sword cut that runs a long way ahead, hitting everything in its line before anyone sees it. */
    public static final class InvisibleBlade extends Ability {
        InvisibleBlade() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 16 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.2)) {
                Kit.hit(player, target, AbilityDamage.SLASH, (hidden(player) ? 22f : 18f) * ctx.power());
                Vfx.impact(ctx.level(), target, target.getBoundingBox().getCenter(), SHIMMER, 1.2f, Kind.STAR);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.SAND_SLASH.get(), 1.2f, 1.6f);
        }
    }
}
