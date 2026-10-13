package com.nerodev.grandline.fx;

import com.nerodev.grandline.network.AnimationPayload;
import com.nerodev.grandline.network.FxPayload;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side entry point for visuals. Sends one small packet; the clients render particles, rings, trails,
 * camera shake and body animations locally, so the server never streams hundreds of particle packets.
 */
public final class Fx {
    /** Players further away than this do not receive effects. */
    public static final double RANGE = 96;

    private Fx() {}

    public static void play(ServerLevel level, ResourceLocation effect, @Nullable Entity source, FxParams params) {
        FxPayload payload = new FxPayload(effect, source == null ? -1 : source.getId(), params);
        PacketDistributor.sendToPlayersNear(level, null, params.pos().x, params.pos().y, params.pos().z, RANGE, payload);
    }

    /** Plays a body animation on {@code entity} for everyone tracking it, including itself if it is a player. */
    public static void animate(Entity entity, ResourceLocation animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new AnimationPayload(entity.getId(), animation));
    }

    /** Stops whatever animation {@code entity} is playing. */
    public static void stopAnimation(Entity entity) {
        animate(entity, GLEffects.ANIM_STOP);
    }
}
