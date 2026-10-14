package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: open the Guide. Carries what the client cannot know on its own: whether the player may use the
 * creator tools (the Guide then offers its Unlock buttons) and their crew, which only the server keeps.
 *
 * @param creator     whether the player has the CREATOR permission
 * @param crewName    the crew's name, empty for no crew
 * @param crewCaptain the captain's name
 * @param crewSize    how many players sail with it
 * @param crewBounty  the crew's total bounty
 */
public record OpenGuidePayload(boolean creator, String crewName, String crewCaptain, int crewSize, long crewBounty)
        implements CustomPacketPayload {
    public static final Type<OpenGuidePayload> TYPE = new Type<>(GrandLine.id("open_guide"));

    public static final StreamCodec<ByteBuf, OpenGuidePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenGuidePayload::creator,
            ByteBufCodecs.stringUtf8(256), OpenGuidePayload::crewName,
            ByteBufCodecs.stringUtf8(256), OpenGuidePayload::crewCaptain,
            ByteBufCodecs.VAR_INT, OpenGuidePayload::crewSize,
            ByteBufCodecs.VAR_LONG, OpenGuidePayload::crewBounty,
            OpenGuidePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
