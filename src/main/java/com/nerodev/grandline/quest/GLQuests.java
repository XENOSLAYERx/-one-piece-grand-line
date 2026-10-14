package com.nerodev.grandline.quest;

import com.nerodev.grandline.GrandLine;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Quests and navigation: the Log Pose item, and the quest logic's event hooks. */
public final class GLQuests {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);

    public static final DeferredItem<LogPoseItem> LOG_POSE = ITEMS.register("log_pose",
            () -> new LogPoseItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    private GLQuests() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Quests.register();
    }
}
