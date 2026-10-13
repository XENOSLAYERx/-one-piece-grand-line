package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: request to put an ability in a slot (or clear it). The server checks the player may use it. */
public record SetSlotPayload(int slot, Optional<ResourceLocation> ability) implements CustomPacketPayload {
    public static final Type<SetSlotPayload> TYPE = new Type<>(GrandLine.id("set_slot"));

    public static final StreamCodec<ByteBuf, SetSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetSlotPayload::slot,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SetSlotPayload::ability,
            SetSlotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
