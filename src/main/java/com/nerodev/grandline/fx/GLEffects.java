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

    // Haki
    /** Black coating spreading over the body. Variant 0 = arms, 1 = full body, 2 = coating fades. */
    public static final ResourceLocation HAKI_COAT = GrandLine.id("haki_coat");
    /** A hardened blow landing: black-violet burst and a ring. */
    public static final ResourceLocation HAKI_IMPACT = GrandLine.id("haki_impact");
    /** Emission: the shock passes through the target and bursts out of its back. dir = blow direction. */
    public static final ResourceLocation HAKI_EMISSION = GrandLine.id("haki_emission");
    /** Observation switched on: a sensing ripple. */
    public static final ResourceLocation OBSERVATION_PULSE = GrandLine.id("observation_pulse");
    /** Future Sight switched on: a flash of the coming moment. */
    public static final ResourceLocation FUTURE_SIGHT = GrandLine.id("future_sight");
    /** An attack read in advance and avoided: afterimage where the user stood. dir = step direction. */
    public static final ResourceLocation FORESEEN_DODGE = GrandLine.id("foreseen_dodge");
    /** Conqueror's Haki gathering (sent every few ticks while charging). scale = charge 0-1. */
    public static final ResourceLocation CONQUEROR_CHARGE = GrandLine.id("conqueror_charge");
    /** Conqueror's Haki released. scale = radius. */
    public static final ResourceLocation CONQUEROR_WAVE = GrandLine.id("conqueror_wave");
    /** Two Conqueror's users collide. pos = first, dir = offset to the second. */
    public static final ResourceLocation CONQUEROR_CLASH = GrandLine.id("conqueror_clash");
    /** A Conqueror-infused strike landing. */
    public static final ResourceLocation CONQUEROR_STRIKE = GrandLine.id("conqueror_strike");
    /** The source entity was knocked out. variant = duration in ticks. */
    public static final ResourceLocation KNOCKOUT = GrandLine.id("knockout");

    // Generic, parameterised effects shared by every fruit (see Vfx for what each parameter means)
    public static final ResourceLocation G_BEAM = GrandLine.id("g_beam");
    public static final ResourceLocation G_BURST = GrandLine.id("g_burst");
    public static final ResourceLocation G_TRAIL = GrandLine.id("g_trail");
    public static final ResourceLocation G_BLINK = GrandLine.id("g_blink");
    public static final ResourceLocation G_ERUPTION = GrandLine.id("g_eruption");
    public static final ResourceLocation G_RAIN = GrandLine.id("g_rain");
    public static final ResourceLocation G_SLASH = GrandLine.id("g_slash");
    public static final ResourceLocation G_SPIRAL = GrandLine.id("g_spiral");
    public static final ResourceLocation G_IMPACT = GrandLine.id("g_impact");
    public static final ResourceLocation G_STATUS = GrandLine.id("g_status");

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
    public static final ResourceLocation ANIM_HAKI_COAT = GrandLine.id("haki_coat");
    public static final ResourceLocation ANIM_CONQUEROR = GrandLine.id("conqueror");
    public static final ResourceLocation ANIM_CONQUEROR_RELEASE = GrandLine.id("conqueror_release");
    public static final ResourceLocation ANIM_SIDESTEP = GrandLine.id("sidestep");
    public static final ResourceLocation ANIM_FOCUS = GrandLine.id("focus");
    public static final ResourceLocation ANIM_GUARD = GrandLine.id("guard");
    public static final ResourceLocation ANIM_PARRY = GrandLine.id("parry");
    public static final ResourceLocation ANIM_UPPERCUT = GrandLine.id("uppercut");
    public static final ResourceLocation ANIM_KICK_HIGH = GrandLine.id("kick_high");
    public static final ResourceLocation ANIM_SPIN_KICK = GrandLine.id("spin_kick");
    public static final ResourceLocation ANIM_AXE_KICK = GrandLine.id("axe_kick");
    public static final ResourceLocation ANIM_PISTOL_SHOT = GrandLine.id("pistol_shot");
    public static final ResourceLocation ANIM_RIFLE_SHOT = GrandLine.id("rifle_shot");
    public static final ResourceLocation ANIM_SWORD_SLASH = GrandLine.id("sword_slash");
    public static final ResourceLocation ANIM_SWORD_IAI = GrandLine.id("sword_iai");
    public static final ResourceLocation ANIM_SWORD_OVERHEAD = GrandLine.id("sword_overhead");
    public static final ResourceLocation ANIM_SWORD_SPIN = GrandLine.id("sword_spin");
    public static final ResourceLocation ANIM_POLEARM_THRUST = GrandLine.id("polearm_thrust");
    public static final ResourceLocation ANIM_STOP = GrandLine.id("stop");

    private GLEffects() {}
}
