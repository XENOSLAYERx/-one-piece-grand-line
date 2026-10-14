package com.nerodev.grandline.data;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Titles: names earned on the story's way (a quest's reward may name one) and worn before the player's own, in chat
 * and in the player list. Earned titles are {@link UnlockCategory#TITLE} unlocks; the one worn is
 * {@link PlayerData#title()}. A title's name is the {@code title.<namespace>.<path>} translation.
 */
public final class Titles {
    private Titles() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.NameFormat.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                GLPlayer.data(player).title().ifPresent(title -> event.setDisplayname(badge(title).append(event.getDisplayname())));
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.TabListNameFormat.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                GLPlayer.data(player).title().ifPresent(title -> event.setDisplayName(badge(title)
                        .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))));
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                refresh(player);
            }
        });
    }

    public static MutableComponent name(ResourceLocation title) {
        return Component.translatable("title." + title.getNamespace() + "." + title.getPath());
    }

    /** "[Clown Breaker] " in gold. */
    private static MutableComponent badge(ResourceLocation title) {
        return Component.literal("[").append(name(title)).append("] ").withStyle(ChatFormatting.GOLD);
    }

    /** {@code player} earns {@code title}; it is worn at once if they wear none yet. */
    public static void grant(ServerPlayer player, ResourceLocation title) {
        PlayerData data = GLPlayer.data(player);
        if (data.isUnlocked(UnlockCategory.TITLE, title)) {
            return;
        }
        boolean wear = data.title().isEmpty();
        GLPlayer.edit(player, d -> {
            d.unlock(UnlockCategory.TITLE, title);
            if (wear) {
                d.setTitle(Optional.of(title));
            }
        });
        player.sendSystemMessage(Component.literal("Title earned: ").withStyle(ChatFormatting.GOLD)
                .append(name(title).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(wear ? " (worn before your name)" : " (wear it with /onepiece title wear)")
                        .withStyle(ChatFormatting.GRAY)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 0.6f, 1.2f);
        if (wear) {
            refresh(player);
        }
    }

    /**
     * Wears an earned title, or none.
     *
     * @return false if {@code title} has not been earned
     */
    public static boolean wear(ServerPlayer player, Optional<ResourceLocation> title) {
        if (title.isPresent() && !GLPlayer.data(player).isUnlocked(UnlockCategory.TITLE, title.get())) {
            return false;
        }
        GLPlayer.edit(player, data -> data.setTitle(title));
        refresh(player);
        return true;
    }

    /** Rebuilds the name shown in chat and the player list after the worn title changed. */
    public static void refresh(ServerPlayer player) {
        player.refreshDisplayName();
        player.refreshTabListName();
    }
}
