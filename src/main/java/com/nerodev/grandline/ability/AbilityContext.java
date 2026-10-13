package com.nerodev.grandline.ability;

import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Everything an ability hook needs: who is using it, for how long, and helpers for effects.
 *
 * @param player      the user
 * @param ability     the ability being used
 * @param activeTicks ticks since activation (0 on activation)
 * @param power       damage/size multiplier from mastery, 1.0 = base
 */
public record AbilityContext(ServerPlayer player, Ability ability, int activeTicks, float power) {
    public ServerLevel level() {
        return player.serverLevel();
    }

    public AbilityState state() {
        return AbilityState.of(player);
    }

    /** Horizontal look direction, normalised. Falls back to facing if looking straight up/down. */
    public Vec3 horizontalLook() {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            float yaw = player.getYRot() * ((float) Math.PI / 180F);
            flat = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        return flat.normalize();
    }

    /** Plays a registered client effect for everyone nearby (including the user). */
    public void fx(ResourceLocation effect, FxParams params) {
        Fx.play(level(), effect, player, params);
    }

    /** Plays a body animation on the user for everyone nearby. */
    public void animate(ResourceLocation animation) {
        Fx.animate(player, animation);
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
