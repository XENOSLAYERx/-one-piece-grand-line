package com.nerodev.grandline.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Implements {@link DeathMode#CREATOR}: the player keeps their inventory and XP on death.
 * <p>
 * On death the inventory is stashed and cleared (so nothing drops), then restored onto the respawned player.
 * If the vanilla keepInventory gamerule is on, vanilla already does this and we stay out of the way.
 */
public final class DeathModeHandler {
    private record Stash(ListTag inventory, int level, float progress, int total) {}

    private static final Map<UUID, Stash> STASHES = new ConcurrentHashMap<>();

    private DeathModeHandler() {}

    public static void register() {
        // LOWEST + skip cancelled: only act once the death is final (totems and other mods have had their say).
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, DeathModeHandler::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingExperienceDropEvent.class, DeathModeHandler::onXpDrop);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.Clone.class, DeathModeHandler::onClone);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> STASHES.remove(e.getEntity().getUUID()));
    }

    private static boolean keepsEverything(ServerPlayer player) {
        return GLPlayer.data(player).deathMode() == DeathMode.CREATOR
                && !player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && keepsEverything(player)) {
            stash(player);
        }
    }

    private static void onXpDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && STASHES.containsKey(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()
                && event.getEntity() instanceof ServerPlayer respawned
                && event.getOriginal() instanceof ServerPlayer original) {
            restore(original, respawned);
        }
    }

    /** Saves inventory + XP and empties the inventory so vanilla has nothing to drop. Public for GameTests. */
    public static void stash(ServerPlayer player) {
        ListTag inventory = player.getInventory().save(new ListTag());
        STASHES.put(player.getUUID(), new Stash(inventory, player.experienceLevel, player.experienceProgress,
                player.totalExperience));
        player.getInventory().clearContent();
    }

    /** Restores a stash made by {@link #stash} onto {@code to}. Public for GameTests. */
    public static boolean restore(ServerPlayer from, ServerPlayer to) {
        Stash stash = STASHES.remove(from.getUUID());
        if (stash == null) {
            return false;
        }
        to.getInventory().load(stash.inventory());
        to.experienceLevel = stash.level();
        to.experienceProgress = stash.progress();
        to.totalExperience = stash.total();
        return true;
    }
}
