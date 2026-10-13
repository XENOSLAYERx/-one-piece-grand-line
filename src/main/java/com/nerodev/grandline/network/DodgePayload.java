package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the dodge key was pressed while moving this way (-1, 0 or +1 forward and to the left). */
public record DodgePayload(byte forward, byte left) implements CustomPacketPayload {
    public static final Type<DodgePayload> TYPE = new Type<>(GrandLine.id("dodge"));

    public static final StreamCodec<ByteBuf, DodgePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, DodgePayload::forward,
            ByteBufCodecs.BYTE, DodgePayload::left,
            DodgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
