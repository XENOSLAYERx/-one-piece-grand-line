package com.nerodev.grandline.entity;

import com.nerodev.grandline.GrandLine;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types. */
public final class GLEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GrandLine.MODID);

    public static final Supplier<EntityType<AbilityProjectile>> ABILITY_PROJECTILE = ENTITIES.register("ability_projectile",
            () -> EntityType.Builder.<AbilityProjectile>of(AbilityProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ability_projectile"));

    private GLEntities() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
