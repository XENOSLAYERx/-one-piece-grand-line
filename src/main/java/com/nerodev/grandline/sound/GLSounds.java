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

    private GLSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GrandLine.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
