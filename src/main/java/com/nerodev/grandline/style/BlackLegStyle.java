package com.nerodev.grandline.style;

import static com.nerodev.grandline.style.GLStyles.ability;
import static com.nerodev.grandline.style.GLStyles.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
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
 * Black Leg: Sanji's kicking style. A cook's hands are for cooking, so every bare-handed blow is a kick, heavier and
 * further-reaching than a punch. Combos end in a Collier kick that launches the foe, and Diable Jambe sets the leg
 * ablaze.
 */
public class BlackLegStyle extends FightingStyle {
    public static final int COLOR = 0x2A2A3A;
    public static final int FLAME = 0xF08A2A;
    static final String M = "style/grandline:black_leg";

    public static final DeferredHolder<Ability, CollierShoot> COLLIER_SHOOT = ability("black_leg_collier_shoot", CollierShoot::new);
    public static final DeferredHolder<Ability, MoutonShot> MOUTON_SHOT = ability("black_leg_mouton_shot", MoutonShot::new);
    public static final DeferredHolder<Ability, Concasse> CONCASSE = ability("black_leg_concasse", Concasse::new);
    public static final DeferredHolder<Ability, PartyTable> PARTY_TABLE = ability("black_leg_party_table", PartyTable::new);
    public static final DeferredHolder<Ability, DiableJambe> DIABLE_JAMBE = ability("black_leg_diable_jambe", DiableJambe::new);

    public BlackLegStyle() {
        super(COLOR, BlackLegStyle::collier, COLLIER_SHOOT, MOUTON_SHOT, CONCASSE, PARTY_TABLE, DIABLE_JAMBE);
    }

    static void init() {}

    static boolean blazing(ServerPlayer user) {
        return AbilityState.of(user).isActive(DIABLE_JAMBE.getId());
    }

    /** A kick's extra: with Diable Jambe lit it burns. */
    static void burn(ServerPlayer user, LivingEntity target, float bonus) {
        if (blazing(user)) {
            Kit.hit(user, target, AbilityDamage.FIRE, bonus);
            target.igniteForSeconds(4);
            Vfx.impact(user.serverLevel(), user, target.getBoundingBox().getCenter(), FLAME, 1.2f, Kind.FLAME);
        }
    }

    /** Combo finisher: a Collier kick under the chin that launches the foe up and away. */
    static void collier(ServerPlayer attacker, LivingEntity target, int combo) {
        Vec3 away = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        Vec3 push = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.8);
        target.setDeltaMovement(push.x, 1.0, push.z);
        target.hurtMarked = true;
        burn(attacker, target, 4f);
        Vfx.burst(attacker.serverLevel(), attacker, target.getBoundingBox().getCenter(), blazing(attacker) ? FLAME : 0xE8E8F0, 1.6f,
                Kind.SHOCKWAVE, Vfx.FLASH);
        Fx.animate(attacker, GLEffects.ANIM_KICK_HIGH);
    }

    /** Every bare-handed blow is a kick: harder, and it shoves the foe back further. */
    @Override
    public void onUnarmedHit(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        event.setAmount(event.getAmount() + 3f + (blazing(user) ? 2f : 0f));
        AbilityDamage.knockback(target, user.position(), 0.5, 0.1);
        if (blazing(user)) {
            target.igniteForSeconds(3);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A kick to the neck of the foe in front of you that sends it flying upwards. */
    public static final class CollierShoot extends Ability {
        CollierShoot() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, COLOR).cooldown(50).stamina(9).masteryGain(0.6));
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
                Kit.launch(target, ctx.horizontalLook().scale(0.6).add(0, 1.0, 0));
                burn(player, target, 4f * ctx.power());
                Vfx.impact(ctx.level(), player, target.getEyePosition(), 0xE8E8F0, 1.2f, Kind.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_KICK_HIGH);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.9f);
        }
    }

    /** A spinning kick through everything in front of you that knocks it back hard. */
    public static final class MoutonShot extends Ability {
        MoutonShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(70).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5, 70)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.6, 0.4);
                burn(player, target, 3f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, player.position().add(0, 0.8, 0).add(ctx.horizontalLook().scale(1.5)), ctx.right(),
                    blazing(player) ? FLAME : 0xE8E8F0, 2.6f, 1);
            ctx.animate(GLEffects.ANIM_SPIN_KICK);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        }
    }

    /**
     * Leap at the foe you look at and bring your heel down on its head like an axe.
     *
     * @name Concassé
     */
    public static final class Concasse extends Ability {
        Concasse() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(140).stamina(14).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one in reach of a leap"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().subtract(ctx.horizontalLook().scale(1.2));
                player.teleportTo(to.x, target.getY(), to.z);
                player.resetFallDistance();
                Kit.hit(player, target, AbilityDamage.CRUSH, 14f * ctx.power());
                target.setDeltaMovement(0, -1.0, 0);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                burn(player, target, 5f * ctx.power());
                Vfx.trail(ctx.level(), player, from.add(0, 2, 0), to.add(0, 1, 0), 0xE8E8F0, 0.8f, Kind.STREAK);
                Vfx.burst(ctx.level(), player, target.position(), 0x8A7A5A, 2f, Kind.SHARD, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_AXE_KICK);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.7f);
        }
    }

    /**
     * Drop into a handstand and spin, kicking everything around you again and again.
     *
     * @name Party Table Kick Course
     */
    public static final class PartyTable extends Ability {
        PartyTable() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, 0x6A6A8A).cooldown(160).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 4 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                for (int i = 0; i < 3; i++) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                }
                AbilityDamage.knockback(target, player.position(), 1.4, 0.35);
                burn(player, target, 3f * ctx.power());
            }
            Vfx.spiral(ctx.level(), player, player.position(), blazing(player) ? FLAME : 0xE8E8F0, (float) radius, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_SPIN_KICK);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.1f);
        }
    }

    /**
     * Spin until your leg glows red-hot. For half a minute you move faster and every kick burns, and
     * your techniques set foes alight.
     */
    public static final class DiableJambe extends Ability {
        DiableJambe() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FLAME, FLAME).cooldown(400).stamina(14).staminaPerTick(0.1f).maxActive(600)
                    .mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vfx.spiral(ctx.level(), player, player.position(), FLAME, 1.4f, Kind.FLAME);
            ctx.animate(GLEffects.ANIM_SPIN_KICK);
            ctx.sound(GLSounds.FLAME_CHARGE.get(), 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 10 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 0, false, false));
            }
            if (ctx.activeTicks() % 3 == 0) {
                Vfx.impact(ctx.level(), player, player.position().add(0, 0.4, 0), FLAME, 0.7f, Kind.FLAME);
            }
            if (player.isInWater()) {
                return false;
            }
            return true;
        }
    }
}
