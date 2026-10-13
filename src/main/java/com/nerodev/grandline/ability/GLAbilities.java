package com.nerodev.grandline.ability;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.rokushiki.GeppoAbility;
import com.nerodev.grandline.ability.rokushiki.SoruAbility;
import com.nerodev.grandline.ability.rokushiki.TekkaiAbility;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The ability registry. Abilities are code (they carry behaviour), so this is a static registry synced to clients
 * for HUD names and icons. Each system registers its abilities here; fruits reference them by id.
 */
public final class GLAbilities {
    public static final ResourceKey<Registry<Ability>> KEY = ResourceKey.createRegistryKey(GrandLine.id("ability"));

    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(KEY, GrandLine.MODID);
    public static final Registry<Ability> REGISTRY = ABILITIES.makeRegistry(builder -> builder.sync(true));

    // Rokushiki (Six Powers). Usable without a Devil Fruit.
    public static final DeferredHolder<Ability, SoruAbility> SORU = register("soru", SoruAbility::new);
    public static final DeferredHolder<Ability, GeppoAbility> GEPPO = register("geppo", GeppoAbility::new);
    public static final DeferredHolder<Ability, TekkaiAbility> TEKKAI = register("tekkai", TekkaiAbility::new);

    private GLAbilities() {}

    private static <T extends Ability> DeferredHolder<Ability, T> register(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
    }
}
