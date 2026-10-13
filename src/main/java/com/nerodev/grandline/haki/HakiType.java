package com.nerodev.grandline.haki;

import com.nerodev.grandline.GrandLine;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * The three kinds of Haki. Each is unlocked separately ({@code UnlockCategory.HAKI}) and has its own level
 * (mastery 0-100 under {@link #masteryKey()}), which scales its abilities and unlocks advanced techniques.
 */
public enum HakiType implements StringRepresentable {
    OBSERVATION(0x7FD8FF),
    ARMAMENT(0x8A63D2),
    CONQUERORS(0xE0182C);

    private final int color;
    private final ResourceLocation id;

    HakiType(int color) {
        this.color = color;
        this.id = GrandLine.id(getSerializedName());
    }

    /** Id stored in the player's HAKI unlocks. */
    public ResourceLocation id() {
        return id;
    }

    /** Mastery key holding this Haki's level. */
    public String masteryKey() {
        return "haki/" + getSerializedName();
    }

    public int color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("haki.grandline." + getSerializedName());
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<HakiType> byName(String name) {
        for (HakiType type : values()) {
            if (type.getSerializedName().equals(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
