package com.nerodev.grandline.fruit.kit;

import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Taking another shape (Clone, Tanuki, Nine-Tailed Fox): everyone sees the player as another creature, or with
 * another player's face. Creatures of the same kind are fooled and leave the player alone until the player hurts
 * them. The look is synced through {@link PublicState} and drawn by the client's disguise renderer.
 */
public final class Disguise {
    private static final String PLAYER_PREFIX = "player:";

    private Disguise() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LivingChangeTargetEvent.class, Disguise::onTarget);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                clear(player);
            }
        });
    }

    /** Look like a creature of {@code type}. */
    public static void as(ServerPlayer player, EntityType<?> type) {
        set(player, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    /** Look like the player with id {@code other}. */
    public static void asPlayer(ServerPlayer player, UUID other) {
        set(player, PLAYER_PREFIX + other);
    }

    public static void clear(ServerPlayer player) {
        if (!current(player).isEmpty()) {
            set(player, "");
        }
    }

    /** The raw disguise string ("" when undisguised). */
    public static String current(Player player) {
        return player.hasData(GLAttachments.PUBLIC_STATE) ? PublicState.of(player).disguise() : "";
    }

    /** The creature type a player is disguised as, or null (not disguised, or disguised as a player). */
    @Nullable
    public static EntityType<?> type(Player player) {
        String disguise = current(player);
        if (disguise.isEmpty() || disguise.startsWith(PLAYER_PREFIX)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(disguise);
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    /** The player whose face this player wears, or null. */
    @Nullable
    public static UUID playerFace(Player player) {
        String disguise = current(player);
        if (!disguise.startsWith(PLAYER_PREFIX)) {
            return null;
        }
        try {
            return UUID.fromString(disguise.substring(PLAYER_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void set(ServerPlayer player, String disguise) {
        PublicState.of(player).setDisguise(disguise);
        player.refreshDimensions();
        TransformationManager.sync(player);
    }

    /** Creatures don't attack someone who looks like one of their own, unless that someone hurt them. */
    private static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player target) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        EntityType<?> look = type(target);
        LivingEntity grudge = mob.getLastHurtByMob();
        if (look != null && look == mob.getType() && grudge != target) {
            event.setCanceled(true);
        }
    }
}
