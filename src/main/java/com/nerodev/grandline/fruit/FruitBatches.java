package com.nerodev.grandline.fruit;

import com.nerodev.grandline.fruit.logia.LogiaBatch;
import net.neoforged.bus.api.IEventBus;

/**
 * Loads every fruit batch class so their fruits and abilities are registered before the registries are handed to
 * the mod bus. A batch is a class whose static fields register fruits through {@link GLFruits#register} and
 * abilities through its own deferred register; it exposes {@code register(IEventBus)} for the latter.
 */
final class FruitBatches {
    private FruitBatches() {}

    static void load() {
        LogiaBatch.load();
    }

    static void register(IEventBus modBus) {
        LogiaBatch.register(modBus);
    }
}
