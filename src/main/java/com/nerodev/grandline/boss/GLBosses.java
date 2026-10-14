package com.nerodev.grandline.boss;

import com.nerodev.grandline.GrandLine;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Bosses: the one entity type every boss shares (their definitions give each its identity) and the bosses' ids. */
public final class GLBosses {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GrandLine.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);

    /** What a Chop-Chop cannon looks like in flight: Buggy's fist. Not an item anyone carries. */
    public static final DeferredItem<Item> CHOP_FIST = ITEMS.registerSimpleItem("chop_fist");

    public static final Supplier<EntityType<BossEntity>> BOSS = ENTITIES.register("boss",
            () -> EntityType.Builder.<BossEntity>of(BossEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("boss"));

    /** Sea Kings: the same bosses' fights in a great sea serpent's body, swimming. */
    public static final Supplier<EntityType<SeaKingEntity>> SEA_KING = ENTITIES.register("sea_king",
            () -> EntityType.Builder.<SeaKingEntity>of(SeaKingEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 1.4f)
                    .eyeHeight(1.0f)
                    .clientTrackingRange(16)
                    .build("sea_king"));

    /** Captain "Axe-Hand" Morgan, who rules Shells Town from the Marine base. */
    public static final ResourceLocation MORGAN = GrandLine.id("morgan");
    /** Arlong the Saw, who holds the Conomi Islands from Arlong Park. */
    public static final ResourceLocation ARLONG = GrandLine.id("arlong");
    /** Buggy the Clown, who holds Orange Town from its tavern. */
    public static final ResourceLocation BUGGY = GrandLine.id("buggy");
    /** Captain Kuro, the butler of Syrup Village's mansion. */
    public static final ResourceLocation KURO = GrandLine.id("kuro");
    /** Don Krieg, the pirate admiral who came to take the Baratie. */
    public static final ResourceLocation KRIEG = GrandLine.id("krieg");
    /** Captain Smoker, the White Hunter of Loguetown, of the Smoke-Smoke Fruit. */
    public static final ResourceLocation SMOKER = GrandLine.id("smoker");
    /** The Lord of the Coast, the Sea King of the East Blue. */
    public static final ResourceLocation LORD_OF_THE_COAST = GrandLine.id("lord_of_the_coast");

    private GLBosses() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(GLBosses::onAttributes);
        BossRespawns.register();
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BOSS.get(), BossEntity.createAttributes().build());
        event.put(SEA_KING.get(), BossEntity.createAttributes().build());
    }
}
