package com.nerodev.grandline.npc;

import net.minecraft.ChatFormatting;

/** How an islander feels about someone: it decides whether they talk, trade, run or fight. */
public enum Reaction {
    FRIENDLY(ChatFormatting.GREEN),
    NEUTRAL(ChatFormatting.GRAY),
    HOSTILE(ChatFormatting.RED);

    private final ChatFormatting color;

    Reaction(ChatFormatting color) {
        this.color = color;
    }

    public ChatFormatting color() {
        return color;
    }
}
