package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.fx.FxParams;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: play a registered visual effect. {@code entityId} is -1 when there is no source entity. */
public record FxPayload(ResourceLocation effect, int entityId, FxParams params) implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(GrandLine.id("fx"));

    public static final StreamCodec<ByteBuf, FxPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, FxPayload::effect,
            ByteBufCodecs.VAR_INT, FxPayload::entityId,
            FxParams.STREAM_CODEC, FxPayload::params,
            FxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
