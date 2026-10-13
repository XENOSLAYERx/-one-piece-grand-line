package com.nerodev.grandline.fruit.gum;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.entity.ProjectileBehavior;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Abilities of the Gum-Gum Fruit (Gomu Gomu no Mi). */
public final class GumAbilities {
    public static final String MASTERY = "fruit/grandline:gum_gum";
    public static final int COLOR = 0xD25BE0;

    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<Ability, GumMoves.Pistol> PISTOL = ABILITIES.register("gum_pistol", GumMoves.Pistol::new);
    public static final DeferredHolder<Ability, GumMoves.Rocket> ROCKET = ABILITIES.register("gum_rocket", GumMoves.Rocket::new);
    public static final DeferredHolder<Ability, GumMoves.Gatling> GATLING = ABILITIES.register("gum_gatling", GumMoves.Gatling::new);
    public static final DeferredHolder<Ability, GumMoves.Bazooka> BAZOOKA = ABILITIES.register("gum_bazooka", GumMoves.Bazooka::new);
    public static final DeferredHolder<Ability, GumMoves.GearSecond> GEAR_SECOND = ABILITIES.register("gear_second", GumMoves.GearSecond::new);
    public static final DeferredHolder<Ability, GumMoves.GiantPistol> GIANT_PISTOL = ABILITIES.register("gum_giant_pistol", GumMoves.GiantPistol::new);
    public static final DeferredHolder<Ability, GumMoves.GearFifth> GEAR_FIFTH = ABILITIES.register("gear_fifth", GumMoves.GearFifth::new);
    public static final DeferredHolder<Ability, GumMoves.DawnFist> DAWN_FIST = ABILITIES.register("gum_dawn_fist", GumMoves.DawnFist::new);

    public static final ResourceLocation DAWN_FIST_IMPACT = GrandLine.id("dawn_fist");

    private GumAbilities() {}

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        ProjectileBehavior.Registry.register(DAWN_FIST_IMPACT, new GumMoves.DawnFistImpact());
    }
}
