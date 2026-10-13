package com.nerodev.grandline.item;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import java.util.ArrayList;
import java.util.List;
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

    public static final DeferredItem<DevilFruitItem> GUM_GUM_FRUIT = fruit(GLFruits.GUM_GUM, Rarity.EPIC);
    public static final DeferredItem<DevilFruitItem> LEOPARD_FRUIT = fruit(GLFruits.LEOPARD, Rarity.RARE);
    public static final DeferredItem<DevilFruitItem> FLAME_FRUIT = fruit(GLFruits.FLAME, Rarity.EPIC);

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
        return item;
    }

    public static List<DeferredItem<DevilFruitItem>> fruitItems() {
        return FRUIT_ITEMS;
    }

    /** The item for a fruit id, or null. */
    public static Item itemFor(ResourceLocation fruit) {
        for (DeferredItem<DevilFruitItem> item : FRUIT_ITEMS) {
            if (item.get().fruit().id().equals(fruit)) {
                return item.get();
            }
        }
        return null;
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
