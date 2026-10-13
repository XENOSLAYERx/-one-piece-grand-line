package com.nerodev.grandline.fruit.paramecia;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Paramecia batch D: jacket, pop, swim, hobby, barrier, calm, heal, biscuit, cream, soul, mirror, lick, wring, memory. */
public final class ParameciaBatchD {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    private ParameciaBatchD() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {}

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
    }
}
