package com.nerodev.grandline.ship;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** What a ship flies: the emblem on its main sail and flag. */
public enum SailStyle implements StringRepresentable {
    /** Plain canvas, no colours. */
    PLAIN,
    /** Black sails with a skull and crossbones. */
    PIRATE,
    /** White sails with the Marine seagull and a blue stripe. */
    MARINE,
    /** Dark red sails with the Revolutionary Army's emblem. */
    REVOLUTIONARY,
    /** White sails with the Straw Hat Jolly Roger. */
    STRAW_HAT;

    public static final Codec<SailStyle> CODEC = StringRepresentable.fromEnum(SailStyle::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
