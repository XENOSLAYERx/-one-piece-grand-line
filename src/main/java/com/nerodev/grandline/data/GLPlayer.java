package com.nerodev.grandline.data;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Entry point for reading and changing a player's Grand Line data. */
public final class GLPlayer {
    private GLPlayer() {}

    /** Read access. On the client this is the last state the server synced. */
    public static PlayerData data(Player player) {
        return player.getData(GLAttachments.PLAYER_DATA);
    }

    /** Server-side change: applies {@code change}, then syncs the profile to the owning client. */
    public static void edit(ServerPlayer player, Consumer<PlayerData> change) {
        change.accept(data(player));
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        if (GLAttachments.canSync(player)) {
            player.syncData(GLAttachments.PLAYER_DATA);
        }
    }
}
