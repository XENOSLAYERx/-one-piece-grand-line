package com.nerodev.grandline.ability;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Creator testing toggles, set with {@code /onepiece <flag> [on|off]}. */
public enum TestFlag implements StringRepresentable {
    INFINITE_STAMINA("stamina infinite"),
    NO_COOLDOWNS("cooldown off"),
    INFINITE_HEALTH("health infinite"),
    INVINCIBLE("invincible"),
    /** Haki abilities cost no stamina and have no cooldown. */
    INFINITE_HAKI("haki infinite");

    public static final Codec<TestFlag> CODEC = StringRepresentable.fromEnum(TestFlag::values);

    private final String label;

    TestFlag(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
