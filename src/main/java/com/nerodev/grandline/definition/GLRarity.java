package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Rarity shared by fruits, weapons and other definitions. Drives loot weighting in later milestones. */
public enum GLRarity implements StringRepresentable {
    COMMON(ChatFormatting.WHITE),
    UNCOMMON(ChatFormatting.GREEN),
    RARE(ChatFormatting.BLUE),
    EPIC(ChatFormatting.DARK_PURPLE),
    LEGENDARY(ChatFormatting.GOLD),
    MYTHICAL(ChatFormatting.RED);

    public static final Codec<GLRarity> CODEC = StringRepresentable.fromEnum(GLRarity::values);

    private final ChatFormatting color;

    GLRarity(ChatFormatting color) {
        this.color = color;
    }

    public Component display() {
        String name = name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
        return Component.literal(name).withStyle(color);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
