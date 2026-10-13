package com.nerodev.grandline.fruit;

import com.nerodev.grandline.fruit.logia.LogiaBatch;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchA;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchB;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchC;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchD;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchE;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchF;
import com.nerodev.grandline.fruit.paramecia.ParameciaBatchG;
import com.nerodev.grandline.fruit.zoan.ZoanBatch;
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
        ParameciaBatchA.load();
        ParameciaBatchB.load();
        ParameciaBatchC.load();
        ParameciaBatchD.load();
        ParameciaBatchE.load();
        ParameciaBatchF.load();
        ParameciaBatchG.load();
        ZoanBatch.load();
    }

    static void register(IEventBus modBus) {
        LogiaBatch.register(modBus);
        ParameciaBatchA.register(modBus);
        ParameciaBatchB.register(modBus);
        ParameciaBatchC.register(modBus);
        ParameciaBatchD.register(modBus);
        ParameciaBatchE.register(modBus);
        ParameciaBatchF.register(modBus);
        ParameciaBatchG.register(modBus);
        ZoanBatch.register(modBus);
    }
}
