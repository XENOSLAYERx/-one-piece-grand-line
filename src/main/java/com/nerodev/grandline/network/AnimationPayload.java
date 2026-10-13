package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: play a body animation on an entity. */
public record AnimationPayload(int entityId, ResourceLocation animation) implements CustomPacketPayload {
    public static final Type<AnimationPayload> TYPE = new Type<>(GrandLine.id("animation"));

    public static final StreamCodec<ByteBuf, AnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimationPayload::entityId,
            ResourceLocation.STREAM_CODEC, AnimationPayload::animation,
            AnimationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
