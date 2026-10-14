package com.nerodev.grandline.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** How a blade is ranked among the world's swords, from ordinary steel to the twelve Supreme Grade blades. */
public enum WeaponGrade {
    ORDINARY("Ordinary", ChatFormatting.GRAY),
    SKILLED("Skilled Grade (Wazamono)", ChatFormatting.GREEN),
    GREAT("Great Grade (O Wazamono)", ChatFormatting.AQUA),
    SUPREME("Supreme Grade (Saijo O Wazamono)", ChatFormatting.LIGHT_PURPLE);

    private final String label;
    private final ChatFormatting color;

    WeaponGrade(String label, ChatFormatting color) {
        this.label = label;
        this.color = color;
    }

    public Component display() {
        return Component.literal(label).withStyle(color);
    }
}
