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
import com.nerodev.grandline.effect.GLMobEffects;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Nagi Nagi no Mi. Silence: the user (or anything they touch) makes no sound at all. Monsters can't hear them coming;
 * a silenced foe can't shout the words its powers need.
 */
public class CalmFruit extends DevilFruit {
    public static final int COLOR = 0xD0D8F0;
    public static final int DEEP = 0x7A8AB8;
    static final String M = "fruit/grandline:calm";

    public static final DeferredHolder<Ability, Silent> SILENT = ability("calm_silent", Silent::new);
    public static final DeferredHolder<Ability, SoundlessStrike> SOUNDLESS_STRIKE = ability("calm_soundless_strike", SoundlessStrike::new);
    public static final DeferredHolder<Ability, SilenceTouch> SILENCE_TOUCH = ability("calm_silence_touch", SilenceTouch::new);
    public static final DeferredHolder<Ability, SoundproofWall> SOUNDPROOF_WALL = ability("calm_soundproof_wall", SoundproofWall::new);
    public static final DeferredHolder<Ability, Ambush> AMBUSH = ability("calm_ambush", Ambush::new);
    public static final DeferredHolder<Ability, GreatSilence> GREAT_SILENCE = ability("calm_great_silence", GreatSilence::new);
    public static final DeferredHolder<Ability, Shroud> SHROUD = ability("calm_shroud", Shroud::new);

    public static final ResourceLocation QUIET = GrandLine.id("calm_quiet");

    public CalmFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Nagi Nagi no Mi", COLOR)
                .abilities(SILENT, SOUNDLESS_STRIKE, SILENCE_TOUCH, SOUNDPROOF_WALL, AMBUSH)
                .awakened(GREAT_SILENCE, SHROUD));
    }

    static void init() {}

    static void behaviors() {
        // Inside the soundproof bubble, monsters lose track of what they were hunting and stumble about.
        ProjectileBehavior.Registry.register(QUIET, Projectiles.zone(20, (p, target) -> {
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
        }, null));
    }

    /** Silences a creature: monsters lose their quarry; a player cannot call on a Devil Fruit for a few seconds. */
    static void silence(LivingEntity target, int ticks) {
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(GLMobEffects.FRUIT_SEALED, ticks, 0));
        } else if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        }
        if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
            Vfx.impact(level, target, target.getEyePosition(), COLOR, 0.8f, Kind.RING);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Make no sound at all: monsters more than four blocks away cannot hear you and lose your trail. */
    public static final class Silent extends Ability {
        Silent() {
            super(props(ActivationType.TOGGLE, AbilityIcon.MOON, COLOR).cooldown(100).stamina(8).staminaPerTick(0.08f)
                    .maxActive(2400).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getEyePosition(), COLOR, 1.2f, Kind.RING, 0);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                ServerPlayer player = ctx.player();
                for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32),
                        m -> m.getTarget() == player && m.distanceToSqr(player) > 16 && m.getLastHurtByMob() != player)) {
                    mob.setTarget(null);
                }
            }
            return true;
        }
    }

    /** A blow nobody hears coming: it hits twice as hard on a foe that isn't watching you. */
    public static final class SoundlessStrike extends Ability {
        SoundlessStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, DEEP).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * ctx.power(), 40)) {
                boolean unaware = !(target instanceof Mob mob) || mob.getTarget() != player;
                Kit.hit(player, target, AbilityDamage.STRIKE, (unaware ? 16f : 8f) * ctx.power());
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), COLOR, 1f, Kind.RING);
            ctx.animate(GLEffects.ANIM_PUNCH);
        }
    }

    /** Touch a foe and steal its voice: a monster forgets its prey; a player can't use a Devil Fruit for four seconds. */
    public static final class SilenceTouch extends Ability {
        SilenceTouch() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(200).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.target(ctx.player(), 4.5).ifPresent(target -> silence(target, 80));
            ctx.animate(GLEffects.ANIM_PALMS);
        }
    }

    /** Throw a soundproof bubble over the spot you point at: monsters inside lose their quarry and stumble in the dark. */
    public static final class SoundproofWall extends Ability {
        SoundproofWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(240).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, QUIET, at, 6 * ctx.power(), COLOR, 0f, 200);
            Vfx.burst(ctx.level(), player, at.add(0, 1, 0), COLOR, 6f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
        }
    }

    /** Slip silently behind the foe you point at and strike before it hears a thing. */
    public static final class Ambush extends Ability {
        Ambush() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, DEEP).cooldown(100).stamina(12).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Vec3 facing = target.getLookAngle().multiply(1, 0, 1);
                Vec3 behind = target.position().subtract(facing.lengthSqr() < 1.0E-4 ? Vec3.ZERO : facing.normalize().scale(1.4));
                Vec3 from = player.position();
                player.teleportTo(behind.x, target.getY(), behind.z);
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * ctx.power());
                Vfx.blink(ctx.level(), player, from, player.position(), COLOR, Kind.RING);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Silence everything around you: monsters forget what they were doing, players lose their fruit powers briefly. */
    public static final class GreatSilence extends Ability {
        GreatSilence() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, DEEP).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 14 * ctx.power())) {
                silence(target, 60);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 14f, Kind.RING, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.3f, 0.5f);
        }
    }

    /** Wrap yourself and everyone beside you in silence: monsters can't track any of you for fifteen seconds. */
    public static final class Shroud extends Ability {
        Shroud() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(600).stamina(24).staminaPerTick(0.15f)
                    .maxActive(300).masteryGain(1.5));
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                ServerPlayer player = ctx.player();
                for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32),
                        m -> m.getTarget() instanceof Player p && p.distanceToSqr(player) < 36 && m.getLastHurtByMob() != p)) {
                    mob.setTarget(null);
                }
                Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 6f, Kind.RING, Vfx.RING);
            }
            return true;
        }
    }
}
