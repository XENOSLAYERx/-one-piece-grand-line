package com.nerodev.grandline.item;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.quest.GLQuests;
import com.nerodev.grandline.style.GLStyles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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

    private static final List<DeferredItem<TechniqueScrollItem>> SCROLLS = new ArrayList<>();

    public static final DeferredItem<TechniqueScrollItem> BLACK_LEG_SCROLL = scroll("black_leg_scroll",
            () -> TechniqueScrollItem.style(GLStyles.BLACK_LEG));
    public static final DeferredItem<TechniqueScrollItem> FISHMAN_KARATE_SCROLL = scroll("fishman_karate_scroll",
            () -> TechniqueScrollItem.style(GLStyles.FISHMAN_KARATE));
    public static final DeferredItem<TechniqueScrollItem> SWORDSMAN_SCROLL = scroll("swordsman_scroll",
            () -> TechniqueScrollItem.style(GLStyles.SWORDSMAN));
    public static final DeferredItem<TechniqueScrollItem> POLEARM_SCROLL = scroll("polearm_scroll",
            () -> TechniqueScrollItem.style(GLStyles.POLEARM));
    public static final DeferredItem<TechniqueScrollItem> GUNSLINGER_SCROLL = scroll("gunslinger_scroll",
            () -> TechniqueScrollItem.style(GLStyles.GUNSLINGER));
    public static final DeferredItem<TechniqueScrollItem> SORU_SCROLL = scroll("soru_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.SORU));
    public static final DeferredItem<TechniqueScrollItem> GEPPO_SCROLL = scroll("geppo_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.GEPPO));
    public static final DeferredItem<TechniqueScrollItem> TEKKAI_SCROLL = scroll("tekkai_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.TEKKAI));
    public static final DeferredItem<TechniqueScrollItem> SHIGAN_SCROLL = scroll("shigan_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.SHIGAN));
    public static final DeferredItem<TechniqueScrollItem> RANKYAKU_SCROLL = scroll("rankyaku_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.RANKYAKU));
    public static final DeferredItem<TechniqueScrollItem> KAMI_E_SCROLL = scroll("kami_e_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.KAMIE));
    public static final DeferredItem<TechniqueScrollItem> ROKUOGAN_SCROLL = scroll("rokuogan_scroll",
            () -> TechniqueScrollItem.technique(GLAbilities.ROKUOGAN));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grandline"))
            .icon(() -> new ItemStack(GUM_GUM_FRUIT.get()))
            .displayItems((params, output) -> {
                FRUIT_ITEMS.forEach(item -> output.accept(item.get()));
                SCROLLS.forEach(item -> output.accept(item.get()));
                output.accept(GLQuests.LOG_POSE.get());
            })
            .build());

    private GLItems() {}

    private static DeferredItem<DevilFruitItem> fruit(DeferredHolder<DevilFruit, ? extends DevilFruit> fruit, Rarity rarity) {
        DeferredItem<DevilFruitItem> item = ITEMS.register(fruit.getId().getPath() + "_fruit",
                () -> new DevilFruitItem(fruit, rarity));
        FRUIT_ITEMS.add(item);
        BY_FRUIT.put(fruit.getId(), item);
        return item;
    }

    private static DeferredItem<TechniqueScrollItem> scroll(String name, Supplier<TechniqueScrollItem> factory) {
        DeferredItem<TechniqueScrollItem> item = ITEMS.register(name, factory);
        SCROLLS.add(item);
        return item;
    }

    public static List<DeferredItem<TechniqueScrollItem>> scrolls() {
        return SCROLLS;
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
