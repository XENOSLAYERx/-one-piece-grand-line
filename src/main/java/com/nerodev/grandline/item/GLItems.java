package com.nerodev.grandline.item;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items and the creative tab. Every Devil Fruit gets an item named {@code <fruit>_fruit}. */
public final class GLItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GrandLine.MODID);

    private static final List<DeferredItem<DevilFruitItem>> FRUIT_ITEMS = new ArrayList<>();
    private static final Map<ResourceLocation, DeferredItem<DevilFruitItem>> BY_FRUIT = new LinkedHashMap<>();

    static {
        // One item per registered fruit (GLFruits.register has already loaded every batch).
        for (GLFruits.Entry entry : GLFruits.entries()) {
            fruit(entry.holder(), entry.itemRarity());
        }
    }

    public static final DeferredItem<DevilFruitItem> GUM_GUM_FRUIT = BY_FRUIT.get(GLFruits.GUM_GUM.getId());
    public static final DeferredItem<DevilFruitItem> LEOPARD_FRUIT = BY_FRUIT.get(GLFruits.LEOPARD.getId());
    public static final DeferredItem<DevilFruitItem> FLAME_FRUIT = BY_FRUIT.get(GLFruits.FLAME.getId());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grandline"))
            .icon(() -> new ItemStack(GUM_GUM_FRUIT.get()))
            .displayItems((params, output) -> FRUIT_ITEMS.forEach(item -> output.accept(item.get())))
            .build());

    private GLItems() {}

    private static DeferredItem<DevilFruitItem> fruit(DeferredHolder<DevilFruit, ? extends DevilFruit> fruit, Rarity rarity) {
        DeferredItem<DevilFruitItem> item = ITEMS.register(fruit.getId().getPath() + "_fruit",
                () -> new DevilFruitItem(fruit, rarity));
        FRUIT_ITEMS.add(item);
        BY_FRUIT.put(fruit.getId(), item);
        return item;
    }

    public static List<DeferredItem<DevilFruitItem>> fruitItems() {
        return FRUIT_ITEMS;
    }

    /** The item for a fruit id, or null. */
    public static Item itemFor(ResourceLocation fruit) {
        DeferredItem<DevilFruitItem> item = BY_FRUIT.get(fruit);
        return item == null ? null : item.get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
