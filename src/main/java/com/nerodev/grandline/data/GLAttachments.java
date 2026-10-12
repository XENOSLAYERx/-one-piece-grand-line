package com.nerodev.grandline.data;

import com.nerodev.grandline.GrandLine;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** All Grand Line data attachments. */
public final class GLAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GrandLine.MODID);

    /** Persistent profile. Saved, kept through death, synced to the owner only. */
    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register("player_data",
            () -> AttachmentType.builder(() -> new PlayerData())
                    .serialize(PlayerData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerData.STREAM_CODEC)
                    .build());

    private GLAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    /** True if the player has a real network connection (not a test or fake player without one). */
    static boolean canSync(ServerPlayer player) {
        return player.connection != null;
    }
}
