package com.nerodev.grandline.sound;

import com.nerodev.grandline.GrandLine;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events. Files live in assets/grandline/sounds/ and are listed in assets/grandline/sounds.json. */
public final class GLSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GrandLine.MODID);

    public static final Supplier<SoundEvent> SORU = register("rokushiki.soru");
    public static final Supplier<SoundEvent> GEPPO = register("rokushiki.geppo");
    public static final Supplier<SoundEvent> TEKKAI_ON = register("rokushiki.tekkai_on");
    public static final Supplier<SoundEvent> TEKKAI_BLOCK = register("rokushiki.tekkai_block");
    public static final Supplier<SoundEvent> EVADE = register("combat.evade");

    // Flame-Flame Fruit
    public static final Supplier<SoundEvent> FLAME_CHARGE = register("flame.charge");
    public static final Supplier<SoundEvent> FIRE_FIST = register("flame.fire_fist");
    public static final Supplier<SoundEvent> FIRE_EXPLOSION = register("flame.explosion");
    public static final Supplier<SoundEvent> FIRE_GUN = register("flame.fire_gun");
    public static final Supplier<SoundEvent> FLAME_WALL = register("flame.wall");
    public static final Supplier<SoundEvent> FIRE_PILLAR = register("flame.pillar");
    public static final Supplier<SoundEvent> ENTEI_CHARGE = register("flame.entei_charge");
    public static final Supplier<SoundEvent> FIREFLY = register("flame.firefly");

    // Gum-Gum Fruit
    public static final Supplier<SoundEvent> RUBBER_STRETCH = register("gum.stretch");
    public static final Supplier<SoundEvent> RUBBER_SNAP = register("gum.snap");
    public static final Supplier<SoundEvent> PUNCH_HEAVY = register("gum.punch");
    public static final Supplier<SoundEvent> GATLING = register("gum.gatling");
    public static final Supplier<SoundEvent> STEAM = register("gum.steam");
    public static final Supplier<SoundEvent> INFLATE = register("gum.inflate");
    public static final Supplier<SoundEvent> BOUNCE = register("gum.bounce");
    public static final Supplier<SoundEvent> GIANT_IMPACT = register("gum.giant_impact");

    // Leopard
    public static final Supplier<SoundEvent> LEOPARD_GROWL = register("leopard.growl");
    public static final Supplier<SoundEvent> LEOPARD_ROAR = register("leopard.roar");
    public static final Supplier<SoundEvent> CLAW = register("leopard.claw");

    // Haki
    public static final Supplier<SoundEvent> HAKI_HARDEN = register("haki.harden");
    public static final Supplier<SoundEvent> HAKI_IMPACT = register("haki.impact");
    public static final Supplier<SoundEvent> HAKI_EMISSION = register("haki.emission");
    public static final Supplier<SoundEvent> HAKI_SENSE = register("haki.sense");
    public static final Supplier<SoundEvent> FUTURE_SIGHT = register("haki.future_sight");
    public static final Supplier<SoundEvent> CONQUEROR_CHARGE = register("haki.conqueror_charge");
    public static final Supplier<SoundEvent> CONQUEROR_WAVE = register("haki.conqueror_wave");
    public static final Supplier<SoundEvent> HAKI_CLASH = register("haki.clash");
    public static final Supplier<SoundEvent> HAKI_CRACKLE = register("haki.crackle");
    public static final Supplier<SoundEvent> KNOCKOUT = register("haki.knockout");
    // Logia elements
    public static final Supplier<SoundEvent> SMOKE_WHOOSH = register("smoke.whoosh");
    public static final Supplier<SoundEvent> SMOKE_BIND = register("smoke.bind");
    public static final Supplier<SoundEvent> SAND_STORM = register("sand.storm");
    public static final Supplier<SoundEvent> SAND_SLASH = register("sand.slash");
    public static final Supplier<SoundEvent> SAND_DRAIN = register("sand.drain");
    public static final Supplier<SoundEvent> RUMBLE_THUNDER = register("rumble.thunder");
    public static final Supplier<SoundEvent> RUMBLE_ZAP = register("rumble.zap");
    public static final Supplier<SoundEvent> ICE_FREEZE = register("ice.freeze");
    public static final Supplier<SoundEvent> ICE_SHATTER = register("ice.shatter");
    public static final Supplier<SoundEvent> DARK_VORTEX = register("dark.vortex");
    public static final Supplier<SoundEvent> DARK_PULSE = register("dark.pulse");
    public static final Supplier<SoundEvent> GLINT_LASER = register("glint.laser");
    public static final Supplier<SoundEvent> GLINT_FLASH = register("glint.flash");
    public static final Supplier<SoundEvent> MAGMA_ERUPTION = register("magma.eruption");
    public static final Supplier<SoundEvent> MAGMA_SIZZLE = register("magma.sizzle");
    public static final Supplier<SoundEvent> SWAMP_GULP = register("swamp.gulp");
    public static final Supplier<SoundEvent> GAS_HISS = register("gas.hiss");
    public static final Supplier<SoundEvent> GAS_BOOM = register("gas.boom");
    public static final Supplier<SoundEvent> SNOW_GUST = register("snow.gust");
    public static final Supplier<SoundEvent> FOREST_ROOTS = register("forest.roots");
    public static final Supplier<SoundEvent> FOREST_CREAK = register("forest.creak");

    private GLSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GrandLine.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
