package com.nerodev.grandline.ability;

import java.util.Locale;

/**
 * Glyph drawn in the HUD slot. Each glyph is a white 16x16 sprite at
 * {@code textures/gui/ability/<name>.png}, tinted with the ability colour.
 */
public enum AbilityIcon {
    FIST, KICK, DASH, JUMP, SHIELD, BEAM, BURST, WAVE, CLAW, FLAME, SWIRL, BLADE, EYE, CROWN, GRAB, ORB, TRANSFORM,
    BOLT, DROP, CLOUD, STAR, SNOWFLAKE, LEAF, HAND, WALL, SPEAR, VORTEX, SUN, SKULL, HEART, CAGE, TORNADO, MOON, METEOR,
    SWORD, GUN, SWORDS;

    public String texturePath() {
        return "textures/gui/ability/" + name().toLowerCase(Locale.ROOT) + ".png";
    }
}
