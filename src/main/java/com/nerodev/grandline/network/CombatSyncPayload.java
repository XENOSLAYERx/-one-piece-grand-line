package com.nerodev.grandline.network;

import com.nerodev.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to the owning client: what the combat HUD shows. Times are game ticks, so the client can count down on its
 * own and the server only sends this when something changes.
 *
 * @param combo        blows in the current combo
 * @param comboUntil   tick the combo drops if no blow lands
 * @param guarding     whether the guard is up
 * @param counterUntil tick the counter window (after a parry) closes
 * @param dodgeReady   tick the next dodge is allowed
 */
public record CombatSyncPayload(int combo, long comboUntil, boolean guarding, long counterUntil, long dodgeReady)
        implements CustomPacketPayload {
    public static final Type<CombatSyncPayload> TYPE = new Type<>(GrandLine.id("combat_sync"));

    public static final StreamCodec<ByteBuf, CombatSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CombatSyncPayload::combo,
            ByteBufCodecs.VAR_LONG, CombatSyncPayload::comboUntil,
            ByteBufCodecs.BOOL, CombatSyncPayload::guarding,
            ByteBufCodecs.VAR_LONG, CombatSyncPayload::counterUntil,
            ByteBufCodecs.VAR_LONG, CombatSyncPayload::dodgeReady,
            CombatSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
