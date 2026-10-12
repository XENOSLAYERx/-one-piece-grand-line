package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Devil Fruit classes. Zoan sub-classes are separate constants because they play differently. */
public enum FruitType implements StringRepresentable {
    PARAMECIA("Paramecia", ChatFormatting.LIGHT_PURPLE),
    ZOAN("Zoan", ChatFormatting.GOLD),
    ANCIENT_ZOAN("Ancient Zoan", ChatFormatting.DARK_RED),
    MYTHICAL_ZOAN("Mythical Zoan", ChatFormatting.AQUA),
    LOGIA("Logia", ChatFormatting.RED);

    public static final Codec<FruitType> CODEC = StringRepresentable.fromEnum(FruitType::values);

    private final String displayName;
    private final ChatFormatting color;

    FruitType(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public boolean isZoan() {
        return this == ZOAN || this == ANCIENT_ZOAN || this == MYTHICAL_ZOAN;
    }

    public Component display() {
        return Component.literal(displayName).withStyle(color);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
