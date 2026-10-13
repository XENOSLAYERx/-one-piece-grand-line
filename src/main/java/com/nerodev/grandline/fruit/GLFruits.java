package com.nerodev.grandline.fruit;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.fruit.flame.FlameAbilities;
import com.nerodev.grandline.fruit.flame.FlameFruit;
import com.nerodev.grandline.fruit.gum.GumAbilities;
import com.nerodev.grandline.fruit.gum.GumFruit;
import com.nerodev.grandline.fruit.leopard.LeopardAbilities;
import com.nerodev.grandline.fruit.leopard.LeopardFruit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Devil Fruit registry. Each fruit is one class plus its abilities; adding a fruit means adding its package,
 * one line here, a texture and lang entries.
 */
public final class GLFruits {
    public static final ResourceKey<Registry<DevilFruit>> KEY = ResourceKey.createRegistryKey(GrandLine.id("devil_fruit"));

    private static final DeferredRegister<DevilFruit> FRUITS = DeferredRegister.create(KEY, GrandLine.MODID);
    public static final Registry<DevilFruit> REGISTRY = FRUITS.makeRegistry(builder -> builder.sync(true));

    /** A registered fruit and the rarity its item shows. */
    public record Entry(DeferredHolder<DevilFruit, ? extends DevilFruit> holder, Rarity itemRarity) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static final DeferredHolder<DevilFruit, GumFruit> GUM_GUM = register("gum_gum", GumFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, LeopardFruit> LEOPARD = register("leopard", LeopardFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, FlameFruit> FLAME = register("flame", FlameFruit::new, Rarity.EPIC);

    private GLFruits() {}

    /** Registers a fruit; its {@code <name>_fruit} item is created automatically by {@code GLItems}. */
    public static <T extends DevilFruit> DeferredHolder<DevilFruit, T> register(String name, Supplier<T> factory, Rarity itemRarity) {
        DeferredHolder<DevilFruit, T> holder = FRUITS.register(name, factory);
        ENTRIES.add(new Entry(holder, itemRarity));
        return holder;
    }

    /** Every fruit in registration order (complete once {@link #register(IEventBus)} has run). */
    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static void register(IEventBus modBus) {
        FruitBatches.load();
        FRUITS.register(modBus);
        GumAbilities.register(modBus);
        LeopardAbilities.register(modBus);
        FlameAbilities.register(modBus);
        FruitBatches.register(modBus);
    }
}
