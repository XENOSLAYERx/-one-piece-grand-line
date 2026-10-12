package com.nerodev.grandline.permission;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Ordered permission tiers. Later constants outrank earlier ones. */
public enum PermissionTier {
    PLAYER(ChatFormatting.GRAY),
    MODERATOR(ChatFormatting.GREEN),
    ADMIN(ChatFormatting.AQUA),
    CREATOR(ChatFormatting.LIGHT_PURPLE);

    private final ChatFormatting color;

    PermissionTier(ChatFormatting color) {
        this.color = color;
    }

    public boolean atLeast(PermissionTier other) {
        return ordinal() >= other.ordinal();
    }

    public static PermissionTier max(PermissionTier a, PermissionTier b) {
        return a.atLeast(b) ? a : b;
    }

    public Component display() {
        return Component.literal(name().toLowerCase(Locale.ROOT)).withStyle(color);
    }
}
