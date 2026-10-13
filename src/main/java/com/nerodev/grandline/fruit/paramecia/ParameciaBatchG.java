package com.nerodev.grandline.fruit.paramecia;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import java.util.function.Supplier;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Paramecia batch G: ton, sick, inspire, egg, cook, more. */
public final class ParameciaBatchG {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, TonFruit> TON = GLFruits.register("ton", TonFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, SickFruit> SICK = GLFruits.register("sick", SickFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, InspireFruit> INSPIRE = GLFruits.register("inspire", InspireFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, EggFruit> EGG = GLFruits.register("egg", EggFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, CookFruit> COOK = GLFruits.register("cook", CookFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, MoreFruit> MORE = GLFruits.register("more", MoreFruit::new, Rarity.RARE);

    private ParameciaBatchG() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        TonFruit.init();
        SickFruit.init();
        InspireFruit.init();
        EggFruit.init();
        CookFruit.init();
        MoreFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        TonFruit.behaviors();
        SickFruit.behaviors();
        InspireFruit.behaviors();
        EggFruit.behaviors();
        CookFruit.behaviors();
        MoreFruit.behaviors();
    }
}
