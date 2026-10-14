package com.nerodev.grandline.world;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.world.island.IslandPiece;
import com.nerodev.grandline.world.island.IslandStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World generation: the islands of the sea, the pieces they are built from, and what grows on them. */
public final class GLWorldgen {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE,
            GrandLine.MODID);
    private static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE,
            GrandLine.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GrandLine.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);
    private static final DeferredRegister<LootItemConditionType> CONDITIONS = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE,
            GrandLine.MODID);

    /** The Devil Fruit chance of a treasure chest, scaled by the server config. */
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> FRUIT_CHANCE = CONDITIONS.register("fruit_chance",
            () -> new LootItemConditionType(FruitChance.CODEC));

    public static final DeferredHolder<StructureType<?>, StructureType<IslandStructure>> ISLAND = STRUCTURE_TYPES.register("island",
            () -> () -> IslandStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ISLAND_PIECE = PIECE_TYPES.register("island",
            () -> (StructurePieceType.ContextlessType) IslandPiece::new);

    /** The leaves of Bell-mère's tangerine trees on the Conomi Islands. */
    public static final DeferredBlock<LeavesBlock> TANGERINE_LEAVES = BLOCKS.register("tangerine_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredItem<BlockItem> TANGERINE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("tangerine_leaves", TANGERINE_LEAVES);
    public static final DeferredItem<Item> TANGERINE = ITEMS.register("tangerine", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build())));

    /** Every island structure, for {@code /locate structure #grandline:islands}. */
    public static final TagKey<Structure> ISLANDS = TagKey.create(Registries.STRUCTURE, GrandLine.id("islands"));

    public static final ResourceKey<LootTable> VILLAGE_HOUSE = loot("village_house");
    public static final ResourceKey<LootTable> PARTYS_BAR = loot("partys_bar");
    public static final ResourceKey<LootTable> MARINE_ARMORY = loot("marine_armory");
    public static final ResourceKey<LootTable> ARLONG_HOARD = loot("arlong_hoard");
    public static final ResourceKey<LootTable> BURIED_TREASURE = loot("buried_treasure");
    public static final ResourceKey<LootTable> BUGGY_STASH = loot("buggy_stash");
    public static final ResourceKey<LootTable> KAYA_MANSION = loot("kaya_mansion");

    private GLWorldgen() {}

    private static ResourceKey<LootTable> loot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, GrandLine.id("chests/" + name));
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PIECE_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        CONDITIONS.register(modBus);
        modBus.addListener(GLWorldgen::onCreativeTabs);
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(TANGERINE_LEAVES_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(TANGERINE.get());
        }
    }
}
