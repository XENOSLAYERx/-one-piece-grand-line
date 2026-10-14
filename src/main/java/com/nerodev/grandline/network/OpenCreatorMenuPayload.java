package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: open the Creator Menu (sent only after the server has checked the CREATOR permission). */
public record OpenCreatorMenuPayload() implements CustomPacketPayload {
    public static final OpenCreatorMenuPayload INSTANCE = new OpenCreatorMenuPayload();
    public static final Type<OpenCreatorMenuPayload> TYPE = new Type<>(GrandLine.id("open_creator_menu"));
    public static final StreamCodec<ByteBuf, OpenCreatorMenuPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
