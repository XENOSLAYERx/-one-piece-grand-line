package com.nerodev.grandline.style;

import static com.nerodev.grandline.style.GLStyles.ability;
import static com.nerodev.grandline.style.GLStyles.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.effect.GLMobEffects;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The street fighting everyone knows. Heavy fists, shoulders and foreheads, and combos that end in an
 * uppercut. Bare-handed blows hit a little harder.
 */
public class BrawlerStyle extends FightingStyle {
    public static final int COLOR = 0xC87A4A;
    static final String M = "style/grandline:brawler";

    public static final DeferredHolder<Ability, Haymaker> HAYMAKER = ability("brawler_haymaker", Haymaker::new);
    public static final DeferredHolder<Ability, ShoulderCharge> SHOULDER_CHARGE = ability("brawler_shoulder_charge", ShoulderCharge::new);
    public static final DeferredHolder<Ability, Headbutt> HEADBUTT = ability("brawler_headbutt", Headbutt::new);
    public static final DeferredHolder<Ability, GroundPound> GROUND_POUND = ability("brawler_ground_pound", GroundPound::new);
    public static final DeferredHolder<Ability, BrawlRush> BRAWL_RUSH = ability("brawler_brawl_rush", BrawlRush::new);

    public BrawlerStyle() {
        super(COLOR, CombatManager.UPPERCUT, HAYMAKER, SHOULDER_CHARGE, HEADBUTT, GROUND_POUND, BRAWL_RUSH);
    }

    static void init() {}

    @Override
    public void onUnarmedHit(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        event.setAmount(event.getAmount() + 1f);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Wind up and throw everything into one punch: the foe in front of you is knocked flying. */
    public static final class Haymaker extends Ability {
        Haymaker() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 11f * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.6).add(0, 0.45, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), 0xFFE0C0, 1.4f, Kind.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.1f, 0.9f);
        }
    }

    /** Lower your shoulder and barge forward: everything in your way is knocked aside. */
    public static final class ShoulderCharge extends Ability {
        ShoulderCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(80).stamina(12).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 6, 1.2)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.3, 0.35);
            }
            Kit.dash(player, 1.6, 0.1);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(ctx.horizontalLook().scale(6)).add(0, 1, 0), 0xE8E0D0, 0.8f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
        }
    }

    /** Slam your forehead into the foe in front of you: it reels, dazed and seeing stars. */
    public static final class Headbutt extends Ability {
        Headbutt() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, COLOR).cooldown(120).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 3.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 3.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 10, 0, false, false, true));
                Vfx.impact(ctx.level(), player, target.getEyePosition(), 0xFFF0A0, 1f, Kind.STAR);
            });
            Kit.dash(player, 0.5, 0);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 1.2f);
        }
    }

    /** Bring both fists down on the ground: everything around you is shaken off its feet. */
    public static final class GroundPound extends Ability {
        GroundPound() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 3.5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 9f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.55, 0));
            }
            Vfx.burst(ctx.level(), player, player.position(), 0x8A7A5A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 0.9f, 1.2f);
        }
    }

    /** A flurry of punches into everything in front of you: six blows in a second. */
    public static final class BrawlRush extends Ability {
        BrawlRush() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, 0xE8A060).cooldown(160).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.5, 50)) {
                for (int i = 0; i < 6; i++) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 2.5f * ctx.power());
                }
                AbilityDamage.knockback(target, player.position(), 0.9, 0.2);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), 0xFFE0C0, 1.2f, Kind.SHOCKWAVE);
            }
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(GLSounds.GATLING.get(), 1f, 1.1f);
        }
    }
}
