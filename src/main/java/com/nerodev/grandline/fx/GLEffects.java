package com.nerodev.grandline.fx;

import com.nerodev.grandline.GrandLine;
import net.minecraft.resources.ResourceLocation;

/**
 * Ids of client effects and body animations. Effects are implemented client-side in
 * {@code com.nerodev.grandline.client.fx.ClientEffects}; animations are JSON files in
 * {@code assets/grandline/grandline_animations/}.
 */
public final class GLEffects {
    // Effects
    public static final ResourceLocation EVADE = GrandLine.id("evade");
    public static final ResourceLocation SORU_DASH = GrandLine.id("soru_dash");
    public static final ResourceLocation GEPPO_STEP = GrandLine.id("geppo_step");
    public static final ResourceLocation TEKKAI_GUARD = GrandLine.id("tekkai_guard");
    public static final ResourceLocation TEKKAI_BLOCK = GrandLine.id("tekkai_block");

    // Animations
    public static final ResourceLocation ANIM_SORU = GrandLine.id("soru");
    public static final ResourceLocation ANIM_GEPPO = GrandLine.id("geppo");
    public static final ResourceLocation ANIM_TEKKAI = GrandLine.id("tekkai");
    public static final ResourceLocation ANIM_STOP = GrandLine.id("stop");

    private GLEffects() {}
}
