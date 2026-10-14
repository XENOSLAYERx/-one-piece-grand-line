package com.nerodev.grandline.gametest;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.boss.GLBosses;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.world.GLWorldgen;
import com.nerodev.grandline.world.island.IslandPiece;
import com.nerodev.grandline.world.island.IslandPlan;
import com.nerodev.grandline.world.island.IslandStructure;
import com.nerodev.grandline.world.island.IslandType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 11: the first islands of the East Blue rise from the sea, whole and the same every time. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class IslandTests {
    private static final String TEMPLATE = "empty";
    private static final int SEA = 63;

    private IslandTests() {}

    @GameTest(template = TEMPLATE)
    public static void islandPlansHoldTogether(GameTestHelper helper) {
        for (IslandType type : IslandType.values()) {
            for (long n = 0; n < 24; n++) {
                long seed = n * 0x9E3779B97F4A7C15L + type.ordinal();
                IslandPlan plan = new IslandPlan(type, 0, 0, SEA, seed);
                String where = type.getSerializedName() + " (seed " + seed + ")";
                helper.assertTrue(plan.height(0, 0) > SEA, "The middle of " + where + " is dry land");
                List<IslandPlan.Placed> buildings = plan.buildings;
                helper.assertTrue(buildings.size() == type.sites().size(), "Every building of " + where + " is placed");
                for (int i = 0; i < buildings.size(); i++) {
                    IslandPlan.Placed a = buildings.get(i);
                    helper.assertTrue(a.ground() > SEA, "The " + a.kind() + " of " + where + " stands above the sea");
                    helper.assertTrue(plan.height(a.centerX(), a.centerZ()) == a.ground() && plan.height(a.x0(), a.z0()) == a.ground()
                            && plan.height(a.x1(), a.z1()) == a.ground(), "The ground under the " + a.kind() + " of " + where + " is level");
                    for (int j = i + 1; j < buildings.size(); j++) {
                        IslandPlan.Placed b = buildings.get(j);
                        boolean overlap = a.x0() <= b.x1() && b.x0() <= a.x1() && a.z0() <= b.z1() && b.z0() <= a.z1();
                        helper.assertFalse(overlap, "The " + a.kind() + " and " + b.kind() + " of " + where + " overlap");
                    }
                }
                IslandPlan.Pier pier = plan.pier;
                int endX = pier.x() + pier.out().getStepX() * (pier.length() - 1);
                int endZ = pier.z() + pier.out().getStepZ() * (pier.length() - 1);
                helper.assertTrue(plan.height(pier.x(), pier.z()) >= SEA - 1, "The pier of " + where + " starts on the shore");
                helper.assertTrue(plan.height(endX, endZ) < SEA - 1, "The pier of " + where + " reaches out over the sea");
                if (plan.treasure != null) {
                    int h = plan.height(plan.treasure[0], plan.treasure[1]);
                    helper.assertTrue(h >= SEA - 1 && plan.beach(plan.treasure[0], plan.treasure[1], h),
                            "The treasure of " + where + " is buried on the beach, at height " + h);
                }
                IslandPlan again = new IslandPlan(type, 0, 0, SEA, seed);
                helper.assertTrue(again.pier.equals(plan.pier) && again.height(17, -9) == plan.height(17, -9)
                        && again.height(-30, 22) == plan.height(-30, 22), "The plan of " + where + " is the same every time");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void islandStructuresAreRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<Structure> structures = access.registryOrThrow(Registries.STRUCTURE);
        for (IslandType type : IslandType.values()) {
            Structure structure = structures.get(type.id());
            helper.assertTrue(structure instanceof IslandStructure island && island.island() == type, "Structure " + type.id() + " raises "
                    + type.getSerializedName());
        }
        StructureSet set = access.registryOrThrow(Registries.STRUCTURE_SET).get(GrandLine.id("east_blue_islands"));
        helper.assertTrue(set != null && set.structures().size() == IslandType.values().length, "The East Blue structure set spreads them");
        helper.assertTrue(structures.getTag(GLWorldgen.ISLANDS).map(HolderSet::size).orElse(0) == IslandType.values().length,
                "#grandline:islands lists every island");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void islandChestsHaveLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ResourceKey<LootTable>> tables = List.of(GLWorldgen.VILLAGE_HOUSE, GLWorldgen.PARTYS_BAR, GLWorldgen.MARINE_ARMORY,
                GLWorldgen.ARLONG_HOARD, GLWorldgen.BURIED_TREASURE, GLWorldgen.BUGGY_STASH, GLWorldgen.KAYA_MANSION);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        for (ResourceKey<LootTable> key : tables) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            helper.assertTrue(table != LootTable.EMPTY, "Loot table " + key.location() + " exists");
            int items = 0;
            for (int i = 0; i < 4; i++) {
                items += table.getRandomItems(params).size();
            }
            helper.assertTrue(items > 0, "Loot table " + key.location() + " gives items");
        }
        LootTable leaves = level.getServer().reloadableRegistries().getLootTable(GLWorldgen.TANGERINE_LEAVES.get().getLootTable());
        helper.assertTrue(leaves != LootTable.EMPTY, "Tangerine leaves have a loot table");
        helper.succeed();
    }

    /** Buried treasure holds a Devil Fruit about 1 time in 12 and a legendary blade about 1 time in 40, never more. */
    @GameTest(template = TEMPLATE)
    public static void treasureIsRare(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(GLWorldgen.BURIED_TREASURE);
        TagKey<Item> fruitTag = TagKey.create(Registries.ITEM, GrandLine.id("devil_fruits"));
        TagKey<Item> bladeTag = TagKey.create(Registries.ITEM, GrandLine.id("legendary_blades"));
        int chests = 1200;
        int fruits = 0;
        int blades = 0;
        for (int i = 0; i < chests; i++) {
            List<ItemStack> loot = table.getRandomItems(params);
            int chestFruits = (int) loot.stream().filter(stack -> stack.is(fruitTag)).count();
            helper.assertTrue(chestFruits <= 1, "A chest holds at most one fruit: " + loot);
            fruits += chestFruits;
            blades += (int) loot.stream().filter(stack -> stack.is(bladeTag)).count();
        }
        helper.assertTrue(fruits >= 50 && fruits <= 160, "About 100 fruits in " + chests + " chests, got " + fruits);
        helper.assertTrue(blades >= 8 && blades <= 60, "About 30 legendary blades in " + chests + " chests, got " + blades);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void islandsRiseFromTheSea(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int index = 0;
        for (IslandType type : IslandType.values()) {
            BlockPos centre = new BlockPos(origin.getX() + 6000 + index * 400, origin.getY() + 12, origin.getZ() + 6000);
            index++;
            IslandPlan plan = IslandPiece.placeNow(level, type, centre, 1234L + index);
            int sea = centre.getY();
            String name = type.getSerializedName();
            int top = plan.height(centre.getX(), centre.getZ());
            helper.assertTrue(top > sea && solid(level.getBlockState(new BlockPos(centre.getX(), top, centre.getZ()))),
                    "The middle of " + name + " is land");
            BlockPos offshore = new BlockPos(centre.getX() + type.reach() - 3, sea - 1, centre.getZ());
            helper.assertTrue(level.getBlockState(offshore).is(Blocks.WATER), name + " is surrounded by sea");
            for (IslandPlan.Placed b : plan.buildings) {
                BlockState floor = level.getBlockState(new BlockPos(b.centerX(), b.ground(), b.centerZ()));
                helper.assertTrue(solid(floor), "The " + b.kind() + " of " + name + " has a floor, found " + floor);
            }
            IslandPlan.Pier pier = plan.pier;
            int planks = 0;
            for (int i = 0; i < pier.length(); i++) {
                BlockPos deck = new BlockPos(pier.x() + pier.out().getStepX() * i, sea, pier.z() + pier.out().getStepZ() * i);
                if (level.getBlockState(deck).is(Blocks.SPRUCE_PLANKS)) {
                    planks++;
                }
            }
            helper.assertTrue(planks >= pier.length() / 2, "The pier of " + name + " has a deck, " + planks + " planks");
            int chests = 0;
            int reach = type.reach();
            for (int cx = (centre.getX() - reach) >> 4; cx <= (centre.getX() + reach) >> 4; cx++) {
                for (int cz = (centre.getZ() - reach) >> 4; cz <= (centre.getZ() + reach) >> 4; cz++) {
                    for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                        if (entity instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null) {
                            chests++;
                        }
                    }
                }
            }
            helper.assertTrue(chests >= 3, name + " has chests of loot, found " + chests);
            AABB area = new AABB(centre).inflate(reach, 40, reach);
            List<NpcEntity> residents = level.getEntitiesOfClass(NpcEntity.class, area);
            helper.assertTrue(residents.size() >= plan.buildings.size(), name + " is inhabited, " + residents.size() + " islanders");
            residents.forEach(NpcEntity::discard);
            List<BossEntity> bosses = level.getEntitiesOfClass(BossEntity.class, area);
            ResourceLocation ruler = switch (type) {
                case SHELLS_TOWN -> GLBosses.MORGAN;
                case CONOMI_ISLANDS -> GLBosses.ARLONG;
                case ORANGE_TOWN -> GLBosses.BUGGY;
                case SYRUP_VILLAGE -> GLBosses.KURO;
                default -> null;
            };
            helper.assertTrue(ruler == null ? bosses.isEmpty() : bosses.size() == 1 && ruler.equals(bosses.getFirst().bossId()),
                    name + " is held by " + ruler + ", found " + bosses.stream().map(BossEntity::bossId).toList());
            bosses.forEach(BossEntity::discard);
            if (plan.treasure != null) {
                int h = plan.height(plan.treasure[0], plan.treasure[1]);
                BlockEntity buried = level.getBlockEntity(new BlockPos(plan.treasure[0], h - 2, plan.treasure[1]));
                helper.assertTrue(buried instanceof RandomizableContainerBlockEntity chest
                        && GLWorldgen.BURIED_TREASURE.equals(chest.getLootTable()), "Treasure is buried on " + name);
            }
        }
        helper.succeed();
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }
}
