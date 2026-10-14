package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a crew member fired the cannons or the helmsman called a Coup de Burst. */
public record ShipActionPayload(byte action) implements CustomPacketPayload {
    public static final byte FIRE = 0;
    public static final byte BURST = 1;

    public static final Type<ShipActionPayload> TYPE = new Type<>(GrandLine.id("ship_action"));
    public static final StreamCodec<ByteBuf, ShipActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, ShipActionPayload::action,
            ShipActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
