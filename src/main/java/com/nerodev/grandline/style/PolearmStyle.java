package com.nerodev.grandline.style;

import static com.nerodev.grandline.style.GLStyles.ability;
import static com.nerodev.grandline.style.GLStyles.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.weapon.WeaponItem;
import com.nerodev.grandline.weapon.WeaponKind;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The art of the long weapon: naginata, spear and bisento. Reach and sweeping power keep foes at bay; a polearm blow
 * hits harder and pushes the foe back, and combos end by hooking its legs out from under it.
 */
public class PolearmStyle extends FightingStyle {
    public static final int COLOR = 0xC89A4A;
    public static final int EDGE = 0xF0E8D8;
    static final String M = "style/grandline:polearm";

    public static final DeferredHolder<Ability, Thrust> THRUST = ability("polearm_thrust", Thrust::new);
    public static final DeferredHolder<Ability, CrescentSweep> CRESCENT_SWEEP = ability("polearm_crescent_sweep", CrescentSweep::new);
    public static final DeferredHolder<Ability, VaultingStrike> VAULTING_STRIKE = ability("polearm_vaulting_strike", VaultingStrike::new);
    public static final DeferredHolder<Ability, Whirlwind> WHIRLWIND = ability("polearm_whirlwind", Whirlwind::new);
    public static final DeferredHolder<Ability, Skysplitter> SKYSPLITTER = ability("polearm_skysplitter", Skysplitter::new);

    public PolearmStyle() {
        super(COLOR, PolearmStyle::legSweep, THRUST, CRESCENT_SWEEP, VAULTING_STRIKE, WHIRLWIND, SKYSPLITTER);
    }

    static void init() {}

    static boolean isPolearm(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem weapon && weapon.kind() == WeaponKind.POLEARM;
    }

    static Optional<Component> needPolearm(ServerPlayer player) {
        return isPolearm(player.getMainHandItem()) ? Optional.empty() : Optional.of(Component.literal("You need a polearm in hand"));
    }

    /** Combo finisher: the haft hooks the foe's legs and it falls, slowed and thrown back. */
    static void legSweep(ServerPlayer attacker, LivingEntity target, int combo) {
        if (!isPolearm(attacker.getMainHandItem())) {
            CombatManager.UPPERCUT.finish(attacker, target, combo);
            return;
        }
        AbilityDamage.knockback(target, attacker.position(), 1.6, 0.2);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        Vfx.slash(attacker.serverLevel(), attacker, target.position().add(0, 0.3, 0), SwordsmanStyle.right(attacker.getLookAngle()), EDGE,
                2.4f, 1);
        Fx.animate(attacker, GLEffects.ANIM_SWEEP);
    }

    /** A polearm blow hits harder and drives the foe back. */
    @Override
    public void onArmedHit(ServerPlayer user, LivingEntity target, ItemStack weapon, LivingIncomingDamageEvent event) {
        if (isPolearm(weapon)) {
            event.setAmount(event.getAmount() + 2f);
            AbilityDamage.knockback(target, user.position(), 0.4, 0.05);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Lunge and drive the point through everything in a long line ahead. */
    public static final class Thrust extends Ability {
        Thrust() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needPolearm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = 7 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, reach, 1.0)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 12f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.0, 0.15);
            }
            Kit.dash(player, 0.6, 0);
            Vec3 eye = player.getEyePosition();
            Vfx.trail(ctx.level(), player, eye.add(ctx.horizontalLook()), eye.add(player.getLookAngle().scale(reach)), EDGE, 0.6f,
                    Kind.STREAK);
            ctx.animate(GLEffects.ANIM_POLEARM_THRUST);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.3f);
        }
    }

    /** Swing the blade in a wide crescent: everything in front of you is cut and knocked aside. */
    public static final class CrescentSweep extends Ability {
        CrescentSweep() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needPolearm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 100)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.3, 0.3);
            }
            Vfx.slash(ctx.level(), player, player.position().add(0, 1, 0).add(ctx.horizontalLook().scale(2)), ctx.right(), EDGE, 4f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        }
    }

    /** Plant the haft and vault at the foe you look at, coming down blade-first on everything around it. */
    public static final class VaultingStrike extends Ability {
        VaultingStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(140).stamina(14).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            Optional<Component> polearm = needPolearm(player);
            if (polearm.isPresent()) {
                return polearm;
            }
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one in reach of a vault"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().subtract(ctx.horizontalLook().scale(1.4));
                player.teleportTo(to.x, target.getY(), to.z);
                player.resetFallDistance();
                for (LivingEntity hit : AbilityDamage.around(player, target.position(), 3 * ctx.power())) {
                    Kit.hit(player, hit, AbilityDamage.CRUSH, 12f * ctx.power());
                    Kit.launch(hit, new Vec3(0, 0.5, 0));
                }
                Vfx.trail(ctx.level(), player, from.add(0, 3, 0), to.add(0, 1, 0), EDGE, 0.8f, Kind.STREAK);
                Vfx.burst(ctx.level(), player, target.position(), 0x8A7A5A, 3f, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_SWORD_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 0.9f, 1.3f);
        }
    }

    /** Spin the polearm around you: everything within its reach is cut three times and thrown back. */
    public static final class Whirlwind extends Ability {
        Whirlwind() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, COLOR).cooldown(160).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needPolearm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 4.5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                for (int i = 0; i < 3; i++) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 4.5f * ctx.power());
                }
                AbilityDamage.knockback(target, player.position(), 1.5, 0.35);
            }
            Vfx.spiral(ctx.level(), player, player.position().add(0, 0.8, 0), EDGE, (float) radius, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_SWORD_SPIN);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.6f);
        }
    }

    /**
     * Raise the polearm high and bring it down so hard the shock splits the ground in a long line ahead.
     *
     * @name Skysplitter
     */
    public static final class Skysplitter extends Ability {
        Skysplitter() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, EDGE).cooldown(260).stamina(20).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needPolearm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 14 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 2.0)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 18f * ctx.power());
                Kit.launch(target, new Vec3(0, 0.7, 0));
            }
            Vec3 ahead = ctx.horizontalLook();
            for (int i = 1; i <= 5; i++) {
                Vfx.burst(ctx.level(), player, player.position().add(ahead.scale(i * length / 5)), 0x8A7A5A, 1.4f + i * 0.2f, Kind.SHARD,
                        Vfx.GROUND);
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ahead.scale(2.5)), new Vec3(0, 1, 0), EDGE, 4.5f, 1);
            ctx.animate(GLEffects.ANIM_SWORD_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 0.8f);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.3f);
        }
    }
}
