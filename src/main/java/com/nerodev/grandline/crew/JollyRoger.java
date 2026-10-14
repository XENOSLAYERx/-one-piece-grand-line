package com.nerodev.grandline.crew;

import com.mojang.serialization.Codec;
import com.nerodev.grandline.GrandLine;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/** The emblems a crew can fly on its flag and on its ship's sails. */
public enum JollyRoger implements StringRepresentable {
    CLASSIC("Skull and crossbones"),
    STRAW_HAT("Straw hat"),
    CROSSED_SWORDS("Crossed swords"),
    BEARD("Crescent moustache"),
    SHARK("Shark teeth"),
    FLAME("Burning skull"),
    HEART("Smiling heart");

    public static final Codec<JollyRoger> CODEC = StringRepresentable.fromEnum(JollyRoger::values);

    private final String label;

    JollyRoger(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ResourceLocation flagTexture() {
        return GrandLine.id("textures/entity/ship/flag_crew_" + getSerializedName() + ".png");
    }

    public ResourceLocation sailTexture() {
        return GrandLine.id("textures/entity/ship/sail_crew_" + getSerializedName() + ".png");
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static JollyRoger byName(String name) {
        for (JollyRoger roger : values()) {
            if (roger.getSerializedName().equals(name)) {
                return roger;
            }
        }
        return null;
    }
}
