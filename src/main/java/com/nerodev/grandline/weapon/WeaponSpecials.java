package com.nerodev.grandline.weapon;

import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.weapon.WeaponItem.Special;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** The special properties of the named blades and the stranger ordinary weapons. */
final class WeaponSpecials {
    private WeaponSpecials() {}

    /** Kanabo: a swung blow staggers the foe, slowing it and knocking it back. */
    static final Special STAGGER = new Special() {
        @Override
        public void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {
            if (!swing) {
                return;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            AbilityDamage.knockback(target, attacker.position(), 0.7, 0.2);
            impact(target, 0x8A6A4A, 1.2f, Kind.SHOCKWAVE);
        }
    };

    /** Seastone jitte: every blow seals the target's Devil Fruit for three seconds. */
    static final Special SEAL = new Special() {
        @Override
        public void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {
            target.addEffect(new MobEffectInstance(GLMobEffects.FRUIT_SEALED, 60, 0, false, true, true));
            if (target.level() instanceof ServerLevel level) {
                Vfx.status(level, target, Vfx.StatusLook.SEASTONE, 60);
            }
            impact(target, GLWeapons.SEASTONE_COLOR, 0.8f, Kind.SPARK);
        }
    };

    /** Sandai Kitetsu: its cuts bleed, but the cursed blade now and then cuts its own wielder. */
    static final Special CURSED = new Special() {
        @Override
        public void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {
            target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 80, 0));
            if (swing && attacker.getRandom().nextFloat() < 0.08f) {
                attacker.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 40, 0));
                if (attacker instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.literal("The cursed blade bites its wielder").withStyle(ChatFormatting.DARK_RED),
                            true);
                }
            }
        }
    };

    /** Wado Ichimonji: a straight, true edge that cuts through armour. */
    static final Special TRUE_EDGE = new Special() {
        @Override
        public void onHit(LivingEntity attacker, LivingEntity target, LivingIncomingDamageEvent event, boolean swing) {
            event.setAmount(event.getAmount() + target.getArmorValue() * 0.25f);
        }
    };

    /** Enma: draws out its wielder's Haki. Armament-coated blows hit a third harder, at a cost in stamina. */
    static final Special ENMA = new Special() {
        @Override
        public void onHit(LivingEntity attacker, LivingEntity target, LivingIncomingDamageEvent event, boolean swing) {
            if (attacker instanceof ServerPlayer player && HakiManager.armamentActive(player)
                    && AbilityState.of(player).spendStamina(3f, AbilityManager.now(player))) {
                event.setAmount(event.getAmount() * 1.33f);
                impact(target, 0x7A2A6A, 1.1f, Kind.SPARK);
                AbilityManager.sync(player);
            }
        }
    };

    /** Kikoku: the long nodachi's full swings cut deep wounds that bleed. */
    static final Special KIKOKU = new Special() {
        @Override
        public void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {
            if (swing && dealt >= 8f) {
                target.addEffect(new MobEffectInstance(GLMobEffects.BLEEDING, 60, 0));
            }
        }
    };

    /** Gryphon: Shanks' saber carries Conqueror's Haki. Infused blows hit a quarter harder. */
    static final Special GRYPHON = new Special() {
        @Override
        public void onHit(LivingEntity attacker, LivingEntity target, LivingIncomingDamageEvent event, boolean swing) {
            if (attacker instanceof ServerPlayer player && HakiManager.infusionActive(player)) {
                event.setAmount(event.getAmount() * 1.25f);
                impact(target, 0xB01A1A, 1.2f, Kind.SPARK);
            }
        }
    };

    /** Yoru: the world's strongest black blade. A swung blow also cuts everything in a narrow line ahead. */
    static final Special YORU = new Special() {
        @Override
        public void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {
            if (swing && attacker instanceof ServerPlayer player) {
                cleave(player, target, dealt * 0.4f, 6, 25, 0.3);
                Vfx.slash(player.serverLevel(), player, player.getEyePosition().add(player.getLookAngle().scale(2.5)), right(player),
                        0x2A5A3A, 3.4f, 1);
            }
        }
    };

    /** Murakumogiri: Whitebeard's bisento sweeps a wide arc, cutting and throwing back everything in it. */
    static final Special MURAKUMOGIRI = new Special() {
        @Override
        public void afterHit(LivingEntity attacker, LivingEntity target, float dealt, boolean swing) {
            if (swing && attacker instanceof ServerPlayer player) {
                cleave(player, target, dealt * 0.5f, 5.5, 70, 1.0);
                Vfx.slash(player.serverLevel(), player, player.position().add(0, 1, 0).add(player.getLookAngle().scale(2)), right(player),
                        0xE8E8F0, 4.2f, 1);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 1.2f, 0.6f);
            }
        }
    };

    // ---------------------------------------------------------------------------------------------------------

    /** Cuts every other creature within {@code range} and {@code halfAngle} degrees of the wielder's look. */
    private static void cleave(ServerPlayer player, LivingEntity struck, float damage, double range, double halfAngle, double knockback) {
        for (LivingEntity other : AbilityDamage.cone(player, range, halfAngle)) {
            if (other != struck) {
                Kit.hit(player, other, AbilityDamage.SLASH, damage);
                if (knockback > 0) {
                    AbilityDamage.knockback(other, player.position(), knockback, 0.25);
                }
            }
        }
    }

    private static Vec3 right(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        return new Vec3(-look.z, 0, look.x).normalize();
    }

    private static void impact(LivingEntity target, int color, float size, Kind kind) {
        if (target.level() instanceof ServerLevel level) {
            Vfx.impact(level, null, target.getBoundingBox().getCenter(), color, size, kind);
        }
    }
}
