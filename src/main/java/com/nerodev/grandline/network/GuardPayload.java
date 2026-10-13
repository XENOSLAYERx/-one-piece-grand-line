package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the guard key went down or up. */
public record GuardPayload(boolean down) implements CustomPacketPayload {
    public static final Type<GuardPayload> TYPE = new Type<>(GrandLine.id("guard"));

    public static final StreamCodec<ByteBuf, GuardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GuardPayload::down,
            GuardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
