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

    private GLSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GrandLine.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
