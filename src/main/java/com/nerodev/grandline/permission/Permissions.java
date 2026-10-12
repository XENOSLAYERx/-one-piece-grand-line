package com.nerodev.grandline.permission;

import com.nerodev.grandline.config.GLServerConfig;
import java.util.function.IntPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves {@link PermissionTier}s. Server-side only: every check runs against server state, never client input.
 */
public final class Permissions {
    private Permissions() {}

    /**
     * Tier of a command source. Uses the source's own op level (so {@code /execute as} from the console keeps the
     * console's rights), raised to CREATOR if the source entity is an explicitly granted creator.
     */
    public static PermissionTier tierOf(CommandSourceStack source) {
        PermissionTier byLevel = fromOpLevel(source::hasPermission);
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionTier.max(byLevel, grantedTier(player));
        }
        return byLevel;
    }

    /** Tier of a player regardless of how a command was invoked. */
    public static PermissionTier tierOf(ServerPlayer player) {
        int level = opLevel(player);
        return PermissionTier.max(fromOpLevel(required -> level >= required), grantedTier(player));
    }

    public static boolean has(CommandSourceStack source, PermissionTier tier) {
        return tierOf(source).atLeast(tier);
    }

    public static boolean has(ServerPlayer player, PermissionTier tier) {
        return tierOf(player).atLeast(tier);
    }

    public static int opLevel(ServerPlayer player) {
        return player.server.getProfilePermissions(player.getGameProfile());
    }

    public static boolean isListedCreator(ServerPlayer player) {
        return GLServerConfig.creatorUuids().contains(player.getUUID());
    }

    public static boolean isSingleplayerOwner(ServerPlayer player) {
        MinecraftServer server = player.server;
        return !server.isDedicatedServer() && server.isSingleplayerOwner(player.getGameProfile());
    }

    /** Tier granted by config, independent of op level. */
    private static PermissionTier grantedTier(ServerPlayer player) {
        if (isListedCreator(player)) {
            return PermissionTier.CREATOR;
        }
        if (GLServerConfig.singleplayerOwnerIsCreator() && isSingleplayerOwner(player)) {
            return PermissionTier.CREATOR;
        }
        return PermissionTier.PLAYER;
    }

    private static PermissionTier fromOpLevel(IntPredicate hasLevel) {
        if (hasLevel.test(GLServerConfig.creatorOpLevel())) {
            return PermissionTier.CREATOR;
        }
        if (hasLevel.test(GLServerConfig.adminOpLevel())) {
            return PermissionTier.ADMIN;
        }
        if (hasLevel.test(GLServerConfig.moderatorOpLevel())) {
            return PermissionTier.MODERATOR;
        }
        return PermissionTier.PLAYER;
    }
}
