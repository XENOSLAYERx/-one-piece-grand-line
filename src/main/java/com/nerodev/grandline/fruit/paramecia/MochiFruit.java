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
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mochi Mochi no Mi, a special Paramecia: the user creates, controls and becomes mochi. Blows sink into the body
 * harmlessly unless they carry Haki, and anything mochi touches sticks fast.
 */
public class MochiFruit extends DevilFruit {
    public static final int COLOR = 0xF4ECE0;
    public static final int SHADE = 0xD8CCE8;
    static final String M = "fruit/grandline:mochi";

    public static final DeferredHolder<Ability, ZanGiri> ZAN_GIRI = ability("mochi_zan_giri", ZanGiri::new);
    public static final DeferredHolder<Ability, Mogura> MOGURA = ability("mochi_mogura", Mogura::new);
    public static final DeferredHolder<Ability, KakuMochi> KAKU_MOCHI = ability("mochi_kaku_mochi", KakuMochi::new);
    public static final DeferredHolder<Ability, StickyFloor> STICKY_FLOOR = ability("mochi_sticky_floor", StickyFloor::new);
    public static final DeferredHolder<Ability, RyuseiKazan> RYUSEI_KAZAN = ability("mochi_ryusei_kazan", RyuseiKazan::new);
    public static final DeferredHolder<Ability, ButoMochi> BUTO_MOCHI = ability("mochi_buto_mochi", ButoMochi::new);
    public static final DeferredHolder<Ability, MochiWorld> MOCHI_WORLD = ability("mochi_mochi_world", MochiWorld::new);

    public static final ResourceLocation SPEAR = GrandLine.id("mochi_mogura");
    public static final ResourceLocation FLOOR = GrandLine.id("mochi_floor");
    public static final ResourceLocation WORLD = GrandLine.id("mochi_world");

    public MochiFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Mochi Mochi no Mi", COLOR)
                .abilities(ZAN_GIRI, MOGURA, KAKU_MOCHI, STICKY_FLOOR, RYUSEI_KAZAN)
                .awakened(BUTO_MOCHI, MOCHI_WORLD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.SLASH,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1))));
        ProjectileBehavior.Registry.register(FLOOR, Projectiles.zone(10, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.MOCHI, 14);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }, null));
        ProjectileBehavior.Registry.register(WORLD, Projectiles.zone(10, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 40 == 0) {
                Kit.root(target, Vfx.StatusLook.MOCHI, 25);
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
    }

    /** Mochi body: physical blows without Haki sink in and lose half their force. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(AbilityDamage.PHYSICAL) && !LogiaFruit.canTouch(event.getSource())) {
            event.setAmount(event.getAmount() * 0.5f);
            Vfx.burst(user.serverLevel(), user, user.getBoundingBox().getCenter(), COLOR, 0.8f, Kind.SMOKE, 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Stretch your arm into a thick rope of mochi and slam it through everything in front of you. */
    public static final class ZanGiri extends Ability {
        ZanGiri() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(50).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 11 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.0)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.0, 0.3);
            }
            Vec3 end = AbilityDamage.lookTarget(player, range);
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, COLOR, 0.9f, Kind.SMOKE, Vfx.BEAM_PARTICLES);
            Vfx.impact(ctx.level(), player, end, COLOR, 1.4f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.RUBBER_SNAP.get(), 1f, 0.7f);
        }
    }

    /** Throw Mogura, your trident of mochi, straight through a line of enemies. */
    public static final class Mogura extends Ability {
        Mogura() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, SHADE).cooldown(60).stamina(12).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, SPEAR, 1.9, 0.7f, COLOR, 9f, 30);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.8f);
        }
    }

    /**
     * Wrap a foe in a ball of mochi that sets hard: it can neither move nor fight until it breaks free.
     *
     * @name Kaku Mochi
     */
    public static final class KakuMochi extends Ability {
        KakuMochi() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(180).stamina(16).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.MOCHI, 70);
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), COLOR, 0.9f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 1.2f, 0.7f);
        }
    }

    /** Spread sticky mochi over the ground: anyone who steps in gets stuck. */
    public static final class StickyFloor extends Ability {
        StickyFloor() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(220).stamina(18).mastery(M, 20)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, FLOOR, at, 5 * ctx.power(), COLOR, 0f, 180);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 1.4f, 0.5f);
        }
    }

    /**
     * Hold to unleash a storm of mochi fists, each one bulging from the last.
     *
     * @name Ryusei Kazan
     */
    public static final class RyuseiKazan extends Ability {
        RyuseiKazan() {
            super(props(ActivationType.HOLD, AbilityIcon.FIST, SHADE).cooldown(160).stamina(14).staminaPerTick(0.4f)
                    .maxActive(50).mastery(M, 35).masteryGain(1.1));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_GATLING);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 7 * ctx.power(), 30)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 2.5f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 0.15, 0.02);
            }
            Vec3 side = ctx.right().scale((player.getRandom().nextDouble() - 0.5) * 1.6);
            Vec3 from = ctx.handPos().add(side).add(0, (player.getRandom().nextDouble() - 0.5) * 0.8, 0);
            Vfx.beam(ctx.level(), player, from, from.add(player.getLookAngle().scale(6)), COLOR, 0.6f, Kind.SMOKE,
                    Vfx.BEAM_PARTICLES);
            ctx.sound(GLSounds.GATLING.get(), 0.6f, 0.8f);
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * A colossal fist of mochi bursts up out of the ground where you point.
     *
     * @name Buto Mochi
     */
    public static final class ButoMochi extends Ability {
        ButoMochi() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, SHADE).cooldown(500).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 26), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 3.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 18f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.5, 0));
            }
            Vfx.eruption(ctx.level(), player, at, COLOR, 8f, Kind.SMOKE);
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.SMOKE, Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.8f);
        }
    }

    /** Turn the land around you into mochi: everyone in it is slowed, weakened, and periodically swallowed. */
    public static final class MochiWorld extends Ability {
        MochiWorld() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(800).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 16 * ctx.power();
            AbilityProjectile.zone(player, WORLD, player.position(), radius, COLOR, 4f * ctx.power(), 240);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), COLOR, (float) radius, Kind.SMOKE,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 2f, 0.4f);
        }
    }
}
