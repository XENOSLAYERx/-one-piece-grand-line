package com.nerodev.grandline.fx;

import com.nerodev.grandline.GrandLine;
import net.minecraft.resources.ResourceLocation;

/**
 * Ids of client effects and body animations. Effects are implemented client-side in
 * {@code com.nerodev.grandline.client.fx.ClientEffects}; animations are JSON files in
 * {@code assets/grandline/grandline_animations/}.
 */
public final class GLEffects {
    // General
    public static final ResourceLocation EVADE = GrandLine.id("evade");
    public static final ResourceLocation FRUIT_EATEN = GrandLine.id("fruit_eaten");
    public static final ResourceLocation AWAKENING = GrandLine.id("awakening");
    public static final ResourceLocation LOGIA_DODGE = GrandLine.id("logia_dodge");
    public static final ResourceLocation TRANSFORM_PUFF = GrandLine.id("transform_puff");

    // Rokushiki
    public static final ResourceLocation SORU_DASH = GrandLine.id("soru_dash");
    public static final ResourceLocation GEPPO_STEP = GrandLine.id("geppo_step");
    public static final ResourceLocation TEKKAI_GUARD = GrandLine.id("tekkai_guard");
    public static final ResourceLocation TEKKAI_BLOCK = GrandLine.id("tekkai_block");

    // Flame-Flame Fruit
    public static final ResourceLocation FLAME_CHARGE = GrandLine.id("flame_charge");
    public static final ResourceLocation FLAME_BURST = GrandLine.id("flame_burst");
    public static final ResourceLocation FIRE_EXPLOSION = GrandLine.id("fire_explosion");
    public static final ResourceLocation FIRE_PILLAR = GrandLine.id("fire_pillar");
    public static final ResourceLocation ENTEI_CHARGE = GrandLine.id("entei_charge");
    public static final ResourceLocation FLAME_FIELD = GrandLine.id("flame_field");

    // Gum-Gum Fruit
    public static final ResourceLocation STRETCH = GrandLine.id("stretch");
    public static final ResourceLocation PUNCH_IMPACT = GrandLine.id("punch_impact");
    public static final ResourceLocation RUBBER_BOUNCE = GrandLine.id("rubber_bounce");
    public static final ResourceLocation GEAR_STEAM = GrandLine.id("gear_steam");
    public static final ResourceLocation INFLATE = GrandLine.id("inflate");
    public static final ResourceLocation RUBBER_WORLD = GrandLine.id("rubber_world");
    public static final ResourceLocation DAWN_FIST = GrandLine.id("dawn_fist");

    // Cat-Cat Fruit, Model: Leopard
    public static final ResourceLocation CLAW_SLASH = GrandLine.id("claw_slash");
    public static final ResourceLocation POUNCE = GrandLine.id("pounce");
    public static final ResourceLocation ROAR = GrandLine.id("roar");

    // Animations
    public static final ResourceLocation ANIM_SORU = GrandLine.id("soru");
    public static final ResourceLocation ANIM_GEPPO = GrandLine.id("geppo");
    public static final ResourceLocation ANIM_TEKKAI = GrandLine.id("tekkai");
    public static final ResourceLocation ANIM_CHARGE_PUNCH = GrandLine.id("charge_punch");
    public static final ResourceLocation ANIM_PUNCH = GrandLine.id("punch");
    public static final ResourceLocation ANIM_FINGER_GUNS = GrandLine.id("finger_guns");
    public static final ResourceLocation ANIM_RAISE_ARMS = GrandLine.id("raise_arms");
    public static final ResourceLocation ANIM_THROW_OVERHEAD = GrandLine.id("throw_overhead");
    public static final ResourceLocation ANIM_SWEEP = GrandLine.id("sweep");
    public static final ResourceLocation ANIM_CHARGE_BOTH = GrandLine.id("charge_both");
    public static final ResourceLocation ANIM_PALMS = GrandLine.id("palms");
    public static final ResourceLocation ANIM_GATLING = GrandLine.id("gatling");
    public static final ResourceLocation ANIM_ROCKET = GrandLine.id("rocket");
    public static final ResourceLocation ANIM_GEAR = GrandLine.id("gear_pose");
    public static final ResourceLocation ANIM_CLAW = GrandLine.id("claw_swipe");
    public static final ResourceLocation ANIM_POUNCE = GrandLine.id("pounce");
    public static final ResourceLocation ANIM_FRENZY = GrandLine.id("frenzy");
    public static final ResourceLocation ANIM_ROAR = GrandLine.id("roar");
    public static final ResourceLocation ANIM_STOP = GrandLine.id("stop");

    private GLEffects() {}
}
