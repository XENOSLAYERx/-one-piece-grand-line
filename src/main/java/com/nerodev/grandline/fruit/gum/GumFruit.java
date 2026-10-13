package com.nerodev.grandline.fruit.gum;

import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Gomu Gomu no Mi. A rubber body: blunt blows are halved, lightning does nothing, arrows bounce back at the
 * shooter, and falls turn into bounces. Blades and Haki still hurt.
 */
public class GumFruit extends DevilFruit {
    public static final float BLUNT_TAKEN = 0.5f;

    public GumFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Gomu Gomu no Mi", GumAbilities.COLOR)
                .abilities(GumAbilities.PISTOL, GumAbilities.ROCKET, GumAbilities.GATLING, GumAbilities.BAZOOKA,
                        GumAbilities.GEAR_SECOND, GumAbilities.GIANT_PISTOL)
                .awakened(GumAbilities.GEAR_FIFTH, GumAbilities.DAWN_FIST));
    }

    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            event.setCanceled(true);
            return;
        }
        if (LogiaFruit.canTouch(source)) {
            return; // Haki-imbued attacks ignore rubber
        }
        if (source.getDirectEntity() instanceof AbstractArrow arrow && !source.is(AbilityDamage.SHARP)) {
            event.setCanceled(true);
            reflect(user, arrow);
            return;
        }
        if (source.is(AbilityDamage.PHYSICAL) && !isSharp(source)) {
            event.setAmount(event.getAmount() * BLUNT_TAKEN);
        }
    }

    private static boolean isSharp(DamageSource source) {
        if (source.is(AbilityDamage.SHARP)) {
            return true;
        }
        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            return weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES);
        }
        return false;
    }

    private static void reflect(ServerPlayer user, AbstractArrow arrow) {
        Vec3 back = arrow.getDeltaMovement().scale(-0.6);
        arrow.setDeltaMovement(back);
        arrow.setOwner(user);
        arrow.hurtMarked = true;
        Fx.play(user.serverLevel(), GLEffects.RUBBER_BOUNCE, user, FxParams.at(arrow.position()).scale(0.5f));
        user.serverLevel().playSound(null, user.blockPosition(), GLSounds.BOUNCE.get(), SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    @Override
    public void onFall(ServerPlayer user, LivingFallEvent event) {
        float distance = event.getDistance();
        if (distance <= 2.5f) {
            return;
        }
        event.setDamageMultiplier(0);
        if (!user.isShiftKeyDown() && distance > 4f) {
            double up = Math.min(1.4, 0.25 + Math.sqrt(distance) * 0.22);
            Vec3 motion = user.getDeltaMovement();
            user.setDeltaMovement(motion.x * 1.2, up, motion.z * 1.2);
            user.hurtMarked = true;
            Fx.play(user.serverLevel(), GLEffects.RUBBER_BOUNCE, user, FxParams.at(user.position()).scale((float) Math.min(2.5, distance / 6)));
            user.serverLevel().playSound(null, user.blockPosition(), GLSounds.BOUNCE.get(), SoundSource.PLAYERS, 0.9f,
                    1.2f - (float) Math.min(0.5, distance / 60));
        }
    }
}
