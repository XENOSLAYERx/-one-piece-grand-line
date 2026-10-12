package com.nerodev.grandline.data;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** What happens when a player dies. */
public enum DeathMode implements StringRepresentable {
    /** Normal RPG rules: vanilla item drops (bounty penalties arrive with the bounty system). */
    NORMAL(ChatFormatting.WHITE),
    /** Creator rules: keep inventory and XP, no penalties. */
    CREATOR(ChatFormatting.LIGHT_PURPLE);

    public static final Codec<DeathMode> CODEC = StringRepresentable.fromEnum(DeathMode::values);

    private final ChatFormatting color;

    DeathMode(ChatFormatting color) {
        this.color = color;
    }

    public Component display() {
        return Component.literal(getSerializedName()).withStyle(color);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
