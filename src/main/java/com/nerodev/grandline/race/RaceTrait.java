package com.nerodev.grandline.race;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Race passives that need code rather than attribute numbers. Listed in a race's JSON under "traits". */
public enum RaceTrait implements StringRepresentable {
    /** Fish-Man: breathes water, sees and swims well underwater, and fights harder in it. */
    AQUATIC,
    /** Mink: bare-handed blows carry Electro, and lightning hurts less. */
    ELECTRO;

    public static final Codec<RaceTrait> CODEC = StringRepresentable.fromEnum(RaceTrait::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
