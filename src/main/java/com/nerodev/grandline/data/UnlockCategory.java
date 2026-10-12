package com.nerodev.grandline.data;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Things a player can unlock. Each category stores a set of registry ids in {@link PlayerData}. */
public enum UnlockCategory implements StringRepresentable {
    FRUIT,
    ABILITY,
    STYLE,
    WEAPON,
    TRANSFORMATION,
    AWAKENING,
    TITLE;

    public static final Codec<UnlockCategory> CODEC = StringRepresentable.fromEnum(UnlockCategory::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
