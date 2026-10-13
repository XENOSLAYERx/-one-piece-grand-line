package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: ability slot key went down or up. This is all the client ever says about abilities. */
public record AbilityKeyPayload(int slot, boolean down) implements CustomPacketPayload {
    public static final Type<AbilityKeyPayload> TYPE = new Type<>(GrandLine.id("ability_key"));

    public static final StreamCodec<ByteBuf, AbilityKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityKeyPayload::slot,
            ByteBufCodecs.BOOL, AbilityKeyPayload::down,
            AbilityKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
