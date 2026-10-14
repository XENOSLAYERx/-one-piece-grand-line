package com.nerodev.grandline.world.island;

import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.boss.BossRespawns;
import com.nerodev.grandline.boss.GLBosses;
import com.nerodev.grandline.npc.GLNpcs;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.world.GLWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Places one chunk's share of an island: land and beaches, paths, buildings, the pier, the buried treasure and the
 * trees. Every block goes through {@link #set}, which drops anything outside the chunk being generated, so each
 * chunk can run the whole recipe and only ever touch itself.
 */
final class IslandBuilder {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final IslandPlan plan;
    private final WorldGenLevel level;
    private final BoundingBox box;
    private final RandomSource random;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    IslandBuilder(IslandPlan plan, WorldGenLevel level, BoundingBox box, RandomSource random) {
        this.plan = plan;
        this.level = level;
        this.box = box;
        this.random = random;
    }

    void build() {
        terrain();
        for (IslandPlan.Placed b : plan.buildings) {
            if (b.x0() - 8 <= box.maxX() && b.x1() + 8 >= box.minX() && b.z0() - 8 <= box.maxZ() && b.z1() + 8 >= box.minZ()) {
                building(b);
            }
        }
        pier();
        treasure();
        flora();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Blocks
    // ---------------------------------------------------------------------------------------------------------

    private void set(int x, int y, int z, BlockState state) {
        if (box.isInside(x, y, z)) {
            level.setBlock(cursor.set(x, y, z), state, 2);
        }
    }

    private BlockState get(int x, int y, int z) {
        return level.getBlockState(cursor.set(x, y, z));
    }

    /** Places a block only into air, water or plants (for leaves, flowers and other soft things). */
    private void soft(int x, int y, int z, BlockState state) {
        if (box.isInside(x, y, z) && replaceable(get(x, y, z))) {
            level.setBlock(cursor.set(x, y, z), state, 2);
        }
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    /** A chest, barrel or other container filled from {@code loot} when first opened. */
    private void container(int x, int y, int z, BlockState state, ResourceKey<LootTable> loot) {
        if (!box.isInside(x, y, z)) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        level.setBlock(pos, state, 2);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(loot);
            container.setLootTableSeed(random.nextLong());
        }
    }

    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState stairs(Block block, Direction facing) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    /** A fence post joined to its neighbours on the given sides (fences placed by generation do not join up by themselves). */
    private static BlockState fence(boolean north, boolean east, boolean south, boolean west) {
        return Blocks.OAK_FENCE.defaultBlockState().setValue(CrossCollisionBlock.NORTH, north).setValue(CrossCollisionBlock.EAST, east)
                .setValue(CrossCollisionBlock.SOUTH, south).setValue(CrossCollisionBlock.WEST, west);
    }

    /** A glass pane set into a wall running along {@code axis}. */
    private static BlockState pane(Direction.Axis axis) {
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        return axis == Direction.Axis.X ? pane.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
                : pane.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
    }

    private static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Land
    // ---------------------------------------------------------------------------------------------------------

    private enum Surface {
        GRASS, PATH, BEACH, SEABED
    }

    private void terrain() {
        int reach = (int) Math.ceil(plan.type.radius() * 1.35);
        int minX = Math.max(box.minX(), plan.cx - reach);
        int maxX = Math.min(box.maxX(), plan.cx + reach);
        int minZ = Math.max(box.minZ(), plan.cz - reach);
        int maxZ = Math.min(box.maxZ(), plan.cz + reach);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int h = plan.height(x, z);
                if (h == Integer.MIN_VALUE) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                if (h < top) {
                    continue;
                }
                Surface surface = h < plan.sea - 1 ? Surface.SEABED
                        : plan.beach(x, z, h) ? Surface.BEACH
                        : plan.path(x, z) ? Surface.PATH : Surface.GRASS;
                for (int y = Math.max(top, box.minY()); y <= h; y++) {
                    set(x, y, z, ground(surface, h - y, x, z));
                }
                if (surface == Surface.GRASS && !plan.occupied(x, z)) {
                    double roll = plan.hash(x, z, 5);
                    if (roll < 0.08) {
                        set(x, h + 1, z, Blocks.SHORT_GRASS.defaultBlockState());
                    } else if (roll < 0.1) {
                        set(x, h + 1, z, flower(plan.hash(x, z, 6)).defaultBlockState());
                    }
                }
            }
        }
    }

    private BlockState ground(Surface surface, int depth, int x, int z) {
        return switch (surface) {
            case GRASS -> depth == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : depth <= 3 ? Blocks.DIRT.defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
            case PATH -> depth == 0 ? pathBlock(x, z) : depth <= 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
            case BEACH -> depth <= 2 ? Blocks.SAND.defaultBlockState() : depth <= 4 ? Blocks.SANDSTONE.defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
            case SEABED -> depth == 0 && plan.hash(x, z, 7) < 0.15 ? Blocks.GRAVEL.defaultBlockState()
                    : depth <= 2 ? Blocks.SAND.defaultBlockState() : Blocks.STONE.defaultBlockState();
        };
    }

    /** Shells Town and Orange Town have paved streets; the villages have trodden earth. */
    private BlockState pathBlock(int x, int z) {
        if (plan.type == IslandType.SHELLS_TOWN || plan.type == IslandType.ORANGE_TOWN || plan.type == IslandType.LOGUETOWN) {
            double roll = plan.hash(x, z, 8);
            return (roll < 0.55 ? Blocks.STONE_BRICKS : roll < 0.8 ? Blocks.COBBLESTONE : Blocks.ANDESITE).defaultBlockState();
        }
        return Blocks.DIRT_PATH.defaultBlockState();
    }

    private Block flower(double roll) {
        if (plan.type.flora() == IslandType.Flora.PALM) {
            return roll < 0.5 ? Blocks.ORANGE_TULIP : Blocks.ALLIUM;
        }
        return roll < 0.4 ? Blocks.DANDELION : roll < 0.7 ? Blocks.POPPY : Blocks.CORNFLOWER;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Buildings
    // ---------------------------------------------------------------------------------------------------------

    /** Materials of a house. */
    private record Palette(Block floor, Block wall, Block frame, Block roof, Block roofSlab, Block door, Block base) {}

    private static final Palette VILLAGE = new Palette(Blocks.OAK_PLANKS, Blocks.WHITE_TERRACOTTA, Blocks.STRIPPED_OAK_LOG,
            Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.OAK_DOOR, Blocks.COBBLESTONE);
    private static final Palette BAR = new Palette(Blocks.SPRUCE_PLANKS, Blocks.OAK_PLANKS, Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_DOOR, Blocks.COBBLESTONE);
    private static final Palette TOWN = new Palette(Blocks.SPRUCE_PLANKS, Blocks.BRICKS, Blocks.STONE_BRICKS,
            Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR, Blocks.STONE_BRICKS);
    private static final Palette VILLA = new Palette(Blocks.BIRCH_PLANKS, Blocks.SMOOTH_SANDSTONE, Blocks.STRIPPED_BIRCH_LOG,
            Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB, Blocks.BIRCH_DOOR, Blocks.SANDSTONE);
    private static final Palette RESTAURANT = new Palette(Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB, Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_PLANKS);
    private static final Palette MANSION = new Palette(Blocks.DARK_OAK_PLANKS, Blocks.QUARTZ_BLOCK, Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DARK_OAK_DOOR, Blocks.STONE_BRICKS);

    /** A building's local frame: x across the front, y up from its floor, z from the front wall to the back. */
    private final class Frame {
        final IslandPlan.Placed b;
        final Rotation rotation;
        final int w;
        final int d;
        final int y0;

        Frame(IslandPlan.Placed b) {
            this.b = b;
            this.rotation = b.rotation();
            this.w = b.width();
            this.d = b.depth();
            this.y0 = b.ground();
        }

        void set(int lx, int ly, int lz, BlockState state) {
            IslandBuilder.this.set(b.worldX(lx, lz), y0 + ly, b.worldZ(lx, lz), state.rotate(rotation));
        }

        void set(int lx, int ly, int lz, Block block) {
            set(lx, ly, lz, block.defaultBlockState());
        }

        void fill(int lx0, int ly0, int lz0, int lx1, int ly1, int lz1, BlockState state) {
            for (int lx = lx0; lx <= lx1; lx++) {
                for (int ly = ly0; ly <= ly1; ly++) {
                    for (int lz = lz0; lz <= lz1; lz++) {
                        set(lx, ly, lz, state);
                    }
                }
            }
        }

        void fill(int lx0, int ly0, int lz0, int lx1, int ly1, int lz1, Block block) {
            fill(lx0, ly0, lz0, lx1, ly1, lz1, block.defaultBlockState());
        }

        /** Walls around the rectangle from (lx0, lz0) to (lx1, lz1). */
        void ring(int lx0, int lz0, int lx1, int lz1, int ly0, int ly1, BlockState state) {
            for (int ly = ly0; ly <= ly1; ly++) {
                for (int lx = lx0; lx <= lx1; lx++) {
                    set(lx, ly, lz0, state);
                    set(lx, ly, lz1, state);
                }
                for (int lz = lz0; lz <= lz1; lz++) {
                    set(lx0, ly, lz, state);
                    set(lx1, ly, lz, state);
                }
            }
        }

        /** Fills the gap under the floor (the land around a building can dip a little) with a footing. */
        void foundation(int lx0, int lz0, int lx1, int lz1, Block base) {
            for (int lx = lx0; lx <= lx1; lx++) {
                for (int lz = lz0; lz <= lz1; lz++) {
                    int x = b.worldX(lx, lz);
                    int z = b.worldZ(lx, lz);
                    for (int y = y0 - 1; y >= y0 - 6; y--) {
                        if (!box.isInside(x, y, z) || !replaceable(get(x, y, z))) {
                            break;
                        }
                        IslandBuilder.this.set(x, y, z, base.defaultBlockState());
                    }
                }
            }
        }

        void door(int lx, int lz, Block door, Direction facing) {
            BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing);
            set(lx, 1, lz, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            set(lx, 2, lz, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        void bed(int lx, int ly, int lz, Block bed) {
            BlockState state = bed.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
            set(lx, ly, lz, state.setValue(BedBlock.PART, BedPart.FOOT));
            set(lx, ly, lz + 1, state.setValue(BedBlock.PART, BedPart.HEAD));
        }

        void container(int lx, int ly, int lz, BlockState state, ResourceKey<LootTable> loot) {
            IslandBuilder.this.container(b.worldX(lx, lz), y0 + ly, b.worldZ(lx, lz), state.rotate(rotation), loot);
        }

        void chest(int lx, int ly, int lz, Direction facing, ResourceKey<LootTable> loot) {
            container(lx, ly, lz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), loot);
        }

        /** An islander living here, standing at a local position (spawned once, by the chunk that holds the spot). */
        void resident(ResourceLocation npc, int lx, int ly, int lz) {
            int x = b.worldX(lx, lz);
            int y = y0 + ly;
            int z = b.worldZ(lx, lz);
            if (box.isInside(x, y, z)) {
                NpcEntity.spawn(level, npc, new BlockPos(x, y, z), random.nextFloat() * 360f, MobSpawnType.STRUCTURE);
            }
        }

        /** A boss holding this ground, facing local (faceX, faceZ) (spawned once, by the chunk that holds the spot). */
        void boss(ResourceLocation boss, int lx, int ly, int lz, int faceX, int faceZ) {
            int x = b.worldX(lx, lz);
            int y = y0 + ly;
            int z = b.worldZ(lx, lz);
            if (box.isInside(x, y, z)) {
                double dx = b.worldX(faceX, faceZ) - x;
                double dz = b.worldZ(faceX, faceZ) - z;
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
                BossEntity spawned = BossEntity.spawn(level, boss, new BlockPos(x, y, z), yaw, MobSpawnType.STRUCTURE);
                if (spawned != null) {
                    spawned.addTag(BossRespawns.RESIDENT_TAG);
                }
            }
        }

        /** A flag on a pole: {@code colors} are the rows of the flag from the top. */
        void flag(int lx, int ly, int lz, int height, Block[] colors) {
            for (int i = 0; i < height; i++) {
                set(lx, ly + i, lz, Blocks.OAK_FENCE);
            }
            for (int row = 0; row < colors.length; row++) {
                for (int i = 1; i <= 3; i++) {
                    set(lx + i, ly + height - 1 - row, lz, colors[row]);
                }
            }
        }
    }

    private void building(IslandPlan.Placed b) {
        Frame f = new Frame(b);
        switch (b.kind()) {
            case HOUSE -> house(f, plan.type == IslandType.CONOMI_ISLANDS ? VILLA : VILLAGE, 1);
            case TOWN_HOUSE -> house(f, TOWN, 2);
            case VILLA -> house(f, VILLA, 1);
            case BAR -> bar(f);
            case WINDMILL -> windmill(f);
            case MARINE_BASE -> marineBase(f);
            case ARLONG_PARK -> arlongPark(f);
            case GROVE -> grove(f);
            case BUGGY_HQ -> buggyHq(f);
            case MANSION -> mansion(f);
            case BARATIE -> baratie(f);
            case SCAFFOLD -> scaffold(f);
            case SWORD_SHOP -> swordShop(f);
        }
    }

    /** Walls, windows, a door and a gabled roof: the shell every house shares. */
    private void shell(Frame f, Palette p, int floors) {
        int w = f.w;
        int d = f.d;
        int wall = floors * 4 - 1;
        f.foundation(0, 0, w - 1, d - 1, p.base());
        f.fill(0, 0, 0, w - 1, 0, d - 1, p.floor());
        f.fill(1, 1, 1, w - 2, wall + d / 2 + 2, d - 2, AIR);
        f.ring(0, 0, w - 1, d - 1, 1, wall, p.wall().defaultBlockState());
        for (int ly = 1; ly <= wall; ly++) {
            f.set(0, ly, 0, p.frame());
            f.set(w - 1, ly, 0, p.frame());
            f.set(0, ly, d - 1, p.frame());
            f.set(w - 1, ly, d - 1, p.frame());
        }
        for (int floor = 1; floor <= floors; floor++) {
            int ly = floor * 4 - 2;
            for (int lz = 2; lz < d - 2; lz += 3) {
                f.set(0, ly, lz, pane(Direction.Axis.Z));
                f.set(w - 1, ly, lz, pane(Direction.Axis.Z));
            }
            for (int lx = 2; lx < w - 2; lx += 3) {
                f.set(lx, ly, d - 1, pane(Direction.Axis.X));
                if (Math.abs(lx - w / 2) > 1) {
                    f.set(lx, ly, 0, pane(Direction.Axis.X));
                }
            }
            if (floor > 1) {
                f.fill(1, ly - 2, 1, w - 2, ly - 2, d - 2, p.floor().defaultBlockState());
                for (int ladder = ly - 5; ladder <= ly - 2; ladder++) {
                    f.set(w - 2, ladder, d / 2, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
                }
            }
        }
        f.door(w / 2, 0, p.door(), Direction.NORTH);
        roof(f, p, wall + 1);
    }

    /** A gabled roof with its ridge running across the front, overhanging the walls by one. */
    private void roof(Frame f, Palette p, int base) {
        int w = f.w;
        int d = f.d;
        for (int k = 0; ; k++) {
            int front = k - 1;
            int back = d - k;
            int ly = base + k;
            if (front > back) {
                break;
            }
            if (front == back) {
                f.fill(-1, ly, front, w, ly, front, p.roofSlab().defaultBlockState());
                break;
            }
            for (int lx = -1; lx <= w; lx++) {
                f.set(lx, ly, front, stairs(p.roof(), Direction.SOUTH));
                f.set(lx, ly, back, stairs(p.roof(), Direction.NORTH));
            }
            for (int lz = Math.max(0, front + 1); lz <= Math.min(d - 1, back - 1); lz++) {
                f.set(0, ly, lz, p.wall());
                f.set(w - 1, ly, lz, p.wall());
            }
        }
    }

    private void house(Frame f, Palette p, int floors) {
        int w = f.w;
        int d = f.d;
        shell(f, p, floors);
        f.bed(1, 1, d - 3, p == VILLA ? Blocks.ORANGE_BED : Blocks.RED_BED);
        f.chest(w - 2, 1, d - 2, Direction.NORTH, GLWorldgen.VILLAGE_HOUSE);
        f.set(w - 2, 1, 1, Blocks.CRAFTING_TABLE);
        f.set(1, 1, 1, Blocks.LANTERN);
        if (w >= 7) {
            f.set(w - 3, 1, d - 2, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        }
        if (floors > 1) {
            f.bed(1, 5, d - 3, Blocks.BLUE_BED);
            f.set(1, 5, 1, Blocks.LANTERN);
        }
        f.resident(p == VILLAGE ? GLNpcs.VILLAGER : GLNpcs.TOWNSPERSON, w / 2, 1, d / 2);
    }

    /** Partys Bar: a long room with a counter, barrels behind it and tables by the windows. */
    private void bar(Frame f) {
        int w = f.w;
        int d = f.d;
        shell(f, BAR, 1);
        for (int lx = 2; lx <= w - 3; lx++) {
            f.set(lx, 1, d - 3, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        for (int lx = 1; lx <= w - 2; lx += 2) {
            if (lx == w / 2 || lx == w / 2 + 1) {
                f.container(lx, 1, d - 2, Blocks.BARREL.defaultBlockState(), GLWorldgen.PARTYS_BAR);
            } else {
                f.set(lx, 1, d - 2, Blocks.BARREL);
            }
        }
        for (int lx : new int[] {2, w - 3}) {
            f.set(lx, 1, 2, Blocks.OAK_FENCE);
            f.set(lx, 2, 2, Blocks.LANTERN);
            f.set(lx - 1, 1, 2, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
            f.set(lx + 1, 1, 2, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
        }
        for (int lx = 3; lx <= w - 4; lx += 2) {
            f.set(lx, 1, d - 4, Blocks.OAK_FENCE);
        }
        f.resident(GLNpcs.BARKEEPER, 2 * (w / 4), 1, d - 2);
        f.resident(GLNpcs.VILLAGER, 2, 1, 3);
    }

    /** The windmill above Foosha Village: stone below, timber above, a cap and four sails facing the sea. */
    private void windmill(Frame f) {
        int w = f.w;
        int d = f.d;
        double mx = (w - 1) / 2.0;
        double mz = (d - 1) / 2.0;
        f.foundation(0, 0, w - 1, d - 1, Blocks.COBBLESTONE);
        for (int lx = 0; lx < w; lx++) {
            for (int lz = 0; lz < d; lz++) {
                double r = Math.sqrt((lx - mx) * (lx - mx) + (lz - mz) * (lz - mz));
                if (r <= 3.3) {
                    f.set(lx, 0, lz, Blocks.COBBLESTONE);
                }
                for (int ly = 1; ly <= 9; ly++) {
                    double radius = ly <= 4 ? 3.3 : 2.5;
                    if (r <= radius) {
                        boolean wall = r > radius - 1.2;
                        Block block = ly <= 4 ? Blocks.COBBLESTONE : (ly % 4 == 0 ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.WHITE_TERRACOTTA);
                        f.set(lx, ly, lz, wall ? block.defaultBlockState() : AIR);
                    }
                }
                if (r <= 2.6) {
                    f.set(lx, 10, lz, Blocks.SPRUCE_PLANKS);
                }
                if (r <= 1.6) {
                    f.set(lx, 11, lz, Blocks.SPRUCE_PLANKS);
                }
            }
        }
        int cx = w / 2;
        f.set(cx, 12, d / 2, Blocks.SPRUCE_SLAB);
        f.door(cx, 0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        f.set(cx - 1, 1, d / 2 + 1, Blocks.LANTERN);
        f.chest(cx + 1, 1, d / 2 + 1, Direction.NORTH, GLWorldgen.VILLAGE_HOUSE);
        // Hub and sails, clear of the stone base.
        int hub = 9;
        f.set(cx, hub, 0, log(Blocks.OAK_LOG, Direction.Axis.Z));
        f.set(cx, hub, 1, log(Blocks.OAK_LOG, Direction.Axis.Z));
        BlockState sail = Blocks.WHITE_WOOL.defaultBlockState();
        for (int i = 1; i <= 4; i++) {
            f.set(cx, hub + i, 0, Blocks.OAK_FENCE);
            f.set(cx + 1, hub + i, 0, sail);
            f.set(cx, hub - i, 0, Blocks.OAK_FENCE);
            f.set(cx - 1, hub - i, 0, sail);
            f.set(cx + i, hub, 0, fence(false, i < 4, false, true));
            f.set(cx + i, hub - 1, 0, sail);
            f.set(cx - i, hub, 0, fence(false, true, false, i < 4));
            f.set(cx - i, hub + 1, 0, sail);
        }
        f.resident(GLNpcs.VILLAGER, cx, 1, d / 2);
    }

    /** The Marine base of Shells Town: a walled yard, the tall tower, an armoury, barracks and the yard's stake. */
    private void marineBase(Frame f) {
        int w = f.w;
        int d = f.d;
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState blue = Blocks.BLUE_CONCRETE.defaultBlockState();
        f.foundation(0, 0, w - 1, d - 1, Blocks.STONE_BRICKS);
        f.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.STONE_BRICKS);
        f.fill(1, 1, 1, w - 2, 20, d - 2, AIR);
        // Outer wall with a blue band and battlements, and the gate.
        f.ring(0, 0, w - 1, d - 1, 1, 3, white);
        f.ring(0, 0, w - 1, d - 1, 4, 4, blue);
        for (int lx = 0; lx < w; lx += 2) {
            f.set(lx, 5, 0, white);
            f.set(lx, 5, d - 1, white);
        }
        for (int lz = 0; lz < d; lz += 2) {
            f.set(0, 5, lz, white);
            f.set(w - 1, 5, lz, white);
        }
        int gate = w / 2;
        f.fill(gate - 1, 1, 0, gate + 1, 3, 0, AIR);
        for (int side : new int[] {-2, 2}) {
            f.set(gate + side, 5, 0, white);
            f.set(gate + side, 6, 0, Blocks.LANTERN);
        }
        // The tower.
        int tw = Math.min(9, w - 8);
        int td = Math.min(9, d - 10);
        int tx0 = (w - tw) / 2;
        int tz0 = d - 2 - td;
        int tx1 = tx0 + tw - 1;
        int tz1 = tz0 + td - 1;
        f.ring(tx0, tz0, tx1, tz1, 1, 13, white);
        f.ring(tx0, tz0, tx1, tz1, 7, 7, blue);
        f.fill(tx0, 14, tz0, tx1, 14, tz1, blue);
        for (int lx = tx0; lx <= tx1; lx += 2) {
            f.set(lx, 15, tz0, white);
            f.set(lx, 15, tz1, white);
        }
        for (int lz = tz0; lz <= tz1; lz += 2) {
            f.set(tx0, 15, lz, white);
            f.set(tx1, 15, lz, white);
        }
        for (int ly : new int[] {3, 10, 12}) {
            for (int lx = tx0 + 2; lx < tx1 - 1; lx += 2) {
                if (ly != 3 || Math.abs(lx - (tx0 + tx1) / 2) > 1) {
                    f.set(lx, ly, tz0, pane(Direction.Axis.X));
                }
                f.set(lx, ly, tz1, pane(Direction.Axis.X));
            }
            for (int lz = tz0 + 2; lz < tz1 - 1; lz += 2) {
                f.set(tx0, ly, lz, pane(Direction.Axis.Z));
                f.set(tx1, ly, lz, pane(Direction.Axis.Z));
            }
        }
        f.fill(tx0 + 1, 5, tz0 + 1, tx1 - 1, 5, tz1 - 1, Blocks.SPRUCE_PLANKS);
        f.fill(tx0 + 1, 9, tz0 + 1, tx1 - 1, 9, tz1 - 1, Blocks.SPRUCE_PLANKS);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int ly = 1; ly <= 13; ly++) {
            f.set(tx1 - 1, ly, tz1 - 1, ladder);
        }
        f.set(tx1 - 1, 14, tz1 - 1, Blocks.SPRUCE_TRAPDOOR);
        int tc = (tx0 + tx1) / 2;
        f.door(tc, tz0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        f.set(tc, 3, tz0, blue);
        f.set(tx0 + 1, 1, tz1 - 1, Blocks.LANTERN);
        f.set(tx0 + 1, 6, tz1 - 1, Blocks.LANTERN);
        f.set(tx0 + 1, 10, tz1 - 1, Blocks.LANTERN);
        f.chest(tx0 + 2, 6, tz1 - 1, Direction.NORTH, GLWorldgen.MARINE_ARMORY);
        f.set(tc, 1, tz1 - 2, Blocks.SPRUCE_PLANKS);
        f.set(tc, 1, tz1 - 1, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
        f.flag(tc, 15, (tz0 + tz1) / 2, 6, new Block[] {Blocks.WHITE_WOOL, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL});
        // Armoury (front left) and barracks (front right).
        int ax1 = Math.min(6, tx0 - 2);
        f.ring(2, 2, ax1, 6, 1, 3, Blocks.STONE_BRICKS.defaultBlockState());
        f.fill(2, 4, 2, ax1, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        f.door((2 + ax1) / 2, 6, Blocks.SPRUCE_DOOR, Direction.SOUTH);
        f.chest(3, 1, 3, Direction.SOUTH, GLWorldgen.MARINE_ARMORY);
        f.container(ax1 - 1, 1, 3, Blocks.BARREL.defaultBlockState(), GLWorldgen.MARINE_ARMORY);
        int bx0 = Math.max(w - 7, tx1 + 2);
        f.ring(bx0, 2, w - 3, 8, 1, 3, white);
        f.fill(bx0, 4, 2, w - 3, 4, 8, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        f.door((bx0 + w - 3) / 2, 8, Blocks.SPRUCE_DOOR, Direction.SOUTH);
        for (int lz = 3; lz <= 5; lz += 2) {
            f.bed(bx0 + 1, 1, lz, Blocks.WHITE_BED);
            f.bed(w - 4, 1, lz, Blocks.BLUE_BED);
        }
        // The stake in the yard, where Zoro was tied for three weeks.
        int sx = Math.min(gate - 4, 6);
        for (int ly = 1; ly <= 2; ly++) {
            f.set(sx, ly, 10, Blocks.OAK_FENCE);
        }
        f.set(sx, 3, 10, fence(false, true, false, true));
        f.set(sx - 1, 3, 10, fence(false, true, false, false));
        f.set(sx + 1, 3, 10, fence(false, false, false, true));
        for (int[] corner : new int[][] {{1, 1}, {w - 2, 1}, {1, d - 2}, {w - 2, d - 2}}) {
            f.set(corner[0], 1, corner[1], Blocks.LANTERN);
        }
        // The garrison: the lieutenant in the tower, the quartermaster in the armoury, recruits and riflemen in the yard.
        f.resident(GLNpcs.MARINE_LIEUTENANT, tc, 1, tz0 + 2);
        f.resident(GLNpcs.MARINE_QUARTERMASTER, ax1 - 1, 1, 5);
        f.resident(GLNpcs.MARINE_RECRUIT, gate - 3, 1, 8);
        f.resident(GLNpcs.MARINE_RECRUIT, gate + 3, 1, 8);
        f.resident(GLNpcs.MARINE_RECRUIT, gate, 1, 11);
        f.resident(GLNpcs.MARINE_RIFLEMAN, gate - 2, 1, 3);
        f.resident(GLNpcs.MARINE_RIFLEMAN, gate + 2, 1, 3);
        if (plan.type == IslandType.LOGUETOWN) {
            // Loguetown's base belongs to Captain Smoker, waiting in the yard for pirates bound for the Grand Line.
            f.resident(GLNpcs.MARINE_LIEUTENANT, gate + 4, 1, 10);
            f.boss(GLBosses.SMOKER, gate, 1, tz0 - 3, gate, 0);
        } else {
            // And Captain Morgan himself, before the tower he had built in his own honour.
            f.boss(GLBosses.MORGAN, tc + 2, 1, tz0 - 2, gate, 0);
        }
    }

    /** Arlong Park: sea-stone walls, a pool open to the sea, the hall with Arlong's seat (and Arlong), and the shark-tooth tower. */
    private void arlongPark(Frame f) {
        int w = f.w;
        int d = f.d;
        BlockState dark = Blocks.DARK_PRISMARINE.defaultBlockState();
        BlockState bricks = Blocks.PRISMARINE_BRICKS.defaultBlockState();
        f.foundation(0, 0, w - 1, d - 1, Blocks.PRISMARINE);
        f.fill(0, 0, 0, w - 1, 0, d - 1, bricks);
        f.fill(1, 1, 1, w - 2, 22, d - 2, AIR);
        f.ring(0, 0, w - 1, d - 1, 1, 5, dark);
        for (int lx = 0; lx < w; lx += 2) {
            f.set(lx, 6, 0, bricks);
            f.set(lx, 6, d - 1, bricks);
        }
        for (int lz = 0; lz < d; lz += 2) {
            f.set(0, 6, lz, bricks);
            f.set(w - 1, 6, lz, bricks);
        }
        int gate = w / 2;
        f.fill(gate - 1, 1, 0, gate + 1, 4, 0, AIR);
        // The pool.
        int px0 = 4;
        int px1 = w - 5;
        f.fill(px0, -3, 3, px1, -3, 8, Blocks.PRISMARINE);
        f.fill(px0, -2, 3, px1, 0, 8, Blocks.WATER.defaultBlockState());
        for (int lx = px0; lx <= px1; lx += 3) {
            f.set(lx, -3, 5, Blocks.SEA_LANTERN);
        }
        // The hall.
        int hz0 = d - 9;
        int hz1 = d - 3;
        f.ring(3, hz0, w - 4, hz1, 1, 6, dark);
        f.fill(3, 7, hz0, w - 4, 7, hz1, Blocks.DARK_PRISMARINE_SLAB.defaultBlockState());
        for (int lx = 5; lx < w - 5; lx += 3) {
            f.set(lx, 3, hz0, Blocks.SEA_LANTERN);
            f.set(lx, 3, hz1, Blocks.SEA_LANTERN);
        }
        f.fill(gate - 1, 1, hz0, gate + 1, 3, hz0, AIR);
        f.set(gate, 1, hz1 - 1, stairs(Blocks.PRISMARINE_BRICK_STAIRS, Direction.SOUTH));
        f.set(gate - 1, 1, hz1 - 1, stairs(Blocks.PRISMARINE_BRICK_STAIRS, Direction.EAST));
        f.set(gate + 1, 1, hz1 - 1, stairs(Blocks.PRISMARINE_BRICK_STAIRS, Direction.WEST));
        f.chest(4, 1, hz1 - 1, Direction.NORTH, GLWorldgen.ARLONG_HOARD);
        f.chest(w - 5, 1, hz1 - 1, Direction.NORTH, GLWorldgen.ARLONG_HOARD);
        // The tower on the hall roof: a ladder in the hall leads up to the roof, a door into the tower and a
        // second ladder to its top.
        int tx0 = gate - 2;
        int tx1 = gate + 2;
        int tz0 = hz0 + 1;
        int tz1 = tz0 + 4;
        f.ring(tx0, tz0, tx1, tz1, 8, 16, dark);
        f.fill(tx0 + 1, 8, tz0 + 1, tx1 - 1, 16, tz1 - 1, AIR);
        for (int ly : new int[] {11, 14}) {
            f.set(gate, ly, tz0, Blocks.SEA_LANTERN);
            f.set(gate, ly, tz1, Blocks.SEA_LANTERN);
            f.set(tx0, ly, (tz0 + tz1) / 2, Blocks.SEA_LANTERN);
        }
        f.fill(tx0, 17, tz0, tx1, 17, tz1, bricks);
        for (int lx = tx0; lx <= tx1; lx += 2) {
            f.set(lx, 18, tz0, Blocks.PRISMARINE_WALL);
            f.set(lx, 18, tz1, Blocks.PRISMARINE_WALL);
        }
        f.set(tx0, 18, (tz0 + tz1) / 2, Blocks.PRISMARINE_WALL);
        f.set(tx1, 18, (tz0 + tz1) / 2, Blocks.PRISMARINE_WALL);
        f.fill(tx1, 8, tz0 + 2, tx1, 9, tz0 + 2, AIR);
        for (int ly = 8; ly <= 16; ly++) {
            f.set(gate, ly, tz0 + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        f.set(gate, 17, tz0 + 1, Blocks.SPRUCE_TRAPDOOR);
        for (int ly = 1; ly <= 7; ly++) {
            f.set(gate + 4, ly, hz1 - 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        f.flag((tx0 + tx1) / 2, 18, (tz0 + tz1) / 2, 6, new Block[] {Blocks.BLACK_WOOL, Blocks.WHITE_WOOL, Blocks.BLACK_WOOL});
        // Arlong's crew holds the park.
        f.resident(GLNpcs.FISHMAN_PIRATE, 2, 1, 2);
        f.resident(GLNpcs.FISHMAN_PIRATE, w - 3, 1, 2);
        f.resident(GLNpcs.FISHMAN_PIRATE, gate - 3, 1, 10);
        f.resident(GLNpcs.FISHMAN_PIRATE, gate + 3, 1, 10);
        f.resident(GLNpcs.FISHMAN_PIRATE, gate - 3, 1, hz1 - 3);
        // Arlong waits in his hall, before his seat.
        f.boss(GLBosses.ARLONG, gate, 1, hz1 - 3, gate, hz0);
    }

    /**
     * Buggy's headquarters in Orange Town: a brick tavern under a red-and-white striped circus tent with the Buggy
     * Pirates' flag on its pole, his stash in the back, his crew at the door and Buggy himself in the middle of it.
     */
    private void buggyHq(Frame f) {
        int w = f.w;
        int d = f.d;
        BlockState brick = Blocks.BRICKS.defaultBlockState();
        f.foundation(0, 0, w - 1, d - 1, Blocks.STONE_BRICKS);
        f.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        f.fill(1, 1, 1, w - 2, 13, d - 2, AIR);
        f.ring(0, 0, w - 1, d - 1, 1, 4, brick);
        for (int ly = 1; ly <= 4; ly++) {
            f.set(0, ly, 0, Blocks.STRIPPED_SPRUCE_LOG);
            f.set(w - 1, ly, 0, Blocks.STRIPPED_SPRUCE_LOG);
            f.set(0, ly, d - 1, Blocks.STRIPPED_SPRUCE_LOG);
            f.set(w - 1, ly, d - 1, Blocks.STRIPPED_SPRUCE_LOG);
        }
        for (int lx = 2; lx < w - 2; lx += 3) {
            if (Math.abs(lx - w / 2) > 1) {
                f.set(lx, 2, 0, pane(Direction.Axis.X));
            }
            f.set(lx, 2, d - 1, pane(Direction.Axis.X));
        }
        for (int lz = 2; lz < d - 2; lz += 3) {
            f.set(0, 2, lz, pane(Direction.Axis.Z));
            f.set(w - 1, 2, lz, pane(Direction.Axis.Z));
        }
        f.door(w / 2, 0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        // The circus tent: shrinking rings of red and white stripes, open below so the tavern is one tall room.
        double cx = (w - 1) / 2.0;
        double cz = (d - 1) / 2.0;
        int layers = 7;
        for (int k = 0; k < layers; k++) {
            double shrink = 1 - k / (double) layers;
            double rx = (w / 2.0 + 0.6) * shrink;
            double rz = (d / 2.0 + 0.6) * shrink;
            for (int lx = -1; lx <= w; lx++) {
                for (int lz = -1; lz <= d; lz++) {
                    double nx = (lx - cx) / rx;
                    double nz = (lz - cz) / rz;
                    double outer = nx * nx + nz * nz;
                    double inner = Math.pow(Math.max(0, 1 - 1.4 / Math.min(rx, rz)), 2);
                    if (outer <= 1 && (outer >= inner || k == layers - 1)) {
                        double angle = Math.atan2(lz - cz, lx - cx) + Math.PI;
                        boolean red = ((int) Math.floor(angle / (Math.PI / 6))) % 2 == 0;
                        f.set(lx, 5 + k, lz, red ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
                    }
                }
            }
        }
        f.flag((int) cx, 5 + layers, (int) cz, 4, new Block[] {Blocks.BLACK_WOOL, Blocks.RED_WOOL, Blocks.BLACK_WOOL});
        // The bar along the back wall, the stash beside it, and tables.
        for (int lx = 2; lx <= w - 3; lx++) {
            f.set(lx, 1, d - 3, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        f.chest(1, 1, d - 2, Direction.NORTH, GLWorldgen.BUGGY_STASH);
        f.chest(w - 2, 1, d - 2, Direction.NORTH, GLWorldgen.BUGGY_STASH);
        for (int lx = 3; lx <= w - 4; lx += 2) {
            f.set(lx, 1, d - 2, Blocks.BARREL);
        }
        for (int lx : new int[] {3, w - 4}) {
            f.set(lx, 1, 3, Blocks.OAK_FENCE);
            f.set(lx, 2, 3, Blocks.LANTERN);
        }
        // A Buggy Ball cannon by the door.
        f.set(1, 1, 1, Blocks.POLISHED_BLACKSTONE);
        f.set(2, 1, 1, Blocks.POLISHED_BLACKSTONE_WALL);
        f.set(2, 2, 1, Blocks.COAL_BLOCK);
        f.resident(GLNpcs.PIRATE_GRUNT, 3, 1, 5);
        f.resident(GLNpcs.PIRATE_GRUNT, w - 4, 1, 5);
        f.resident(GLNpcs.PIRATE_GUNNER, w / 2 + 3, 1, d - 5);
        f.boss(GLBosses.BUGGY, w / 2, 1, d / 2, w / 2, 0);
    }

    /**
     * Kaya's mansion above Syrup Village: two white storeys under a dark tiled roof, a hall with a staircase, a
     * library and her room upstairs, and her butler "Klahadore" (Captain Kuro) keeping watch in the hall.
     */
    private void mansion(Frame f) {
        int w = f.w;
        int d = f.d;
        shell(f, MANSION, 2);
        for (int lx = 1; lx < w - 1; lx += 2) {
            f.set(lx, 1, d - 2, Blocks.BOOKSHELF);
            f.set(lx, 2, d - 2, Blocks.BOOKSHELF);
        }
        f.set(1, 1, 1, Blocks.LANTERN);
        f.set(w - 2, 1, 1, Blocks.LANTERN);
        f.chest(2, 1, d - 3, Direction.NORTH, GLWorldgen.KAYA_MANSION);
        f.bed(1, 5, d - 4, Blocks.WHITE_BED);
        f.chest(3, 5, d - 2, Direction.NORTH, GLWorldgen.KAYA_MANSION);
        f.set(w - 3, 5, d - 2, Blocks.BOOKSHELF);
        f.set(2, 5, 1, Blocks.LANTERN);
        f.set(w - 3, 1, 3, Blocks.RED_CARPET);
        f.set(w - 4, 1, 3, Blocks.RED_CARPET);
        f.resident(GLNpcs.VILLAGER, 3, 5, 3);
        f.resident(GLNpcs.TOWNSPERSON, w - 4, 1, d / 2);
        f.boss(GLBosses.KURO, w / 2, 1, d / 2, w / 2, 0);
    }

    /**
     * The Baratie, the sea restaurant: a ship-shaped dining hall moored over its reef on a hull of dark planks, a fish's
     * head for a bow and its tail at the stern, the kitchen at the back, the chefs' quarters above, its fighting cooks,
     * and Don Krieg come to take it.
     */
    private void baratie(Frame f) {
        int w = f.w;
        int d = f.d;
        int mid = d / 2;
        // The hull and the deck around the hall, with a rail.
        f.foundation(-2, -1, w + 1, d, Blocks.DARK_OAK_PLANKS);
        for (int lx = -2; lx <= w + 1; lx++) {
            for (int lz = -1; lz <= d; lz++) {
                boolean corner = (lx < 0 || lx > w - 1) && (lz < 0 || lz > d - 1);
                if (!corner) {
                    f.set(lx, 0, lz, Blocks.SPRUCE_PLANKS);
                    boolean rail = lx == -2 || lx == w + 1 || lz == -1 || lz == d;
                    if (rail && Math.abs(lx - w / 2) > 1) {
                        f.set(lx, 1, lz, lz == -1 || lz == d ? fence(false, true, false, true) : fence(true, false, true, false));
                    }
                }
            }
        }
        shell(f, RESTAURANT, 2);
        // The fish's head for a bow: orange and yellow, with round black eyes and a red mouth.
        for (int k = 0; k < 4; k++) {
            int lx = w + 2 + k;
            int hw = Math.max(1, mid - 1 - k);
            int hh = Math.max(1, 5 - k);
            for (int lz = mid - hw; lz <= mid + hw; lz++) {
                for (int ly = -1; ly <= hh; ly++) {
                    boolean shell = ly == hh || Math.abs(lz - mid) == hw;
                    f.set(lx, ly, lz, shell ? Blocks.ORANGE_TERRACOTTA : Blocks.YELLOW_TERRACOTTA);
                }
            }
        }
        int eyeSide = Math.max(1, mid - 2);
        f.set(w + 3, 3, mid - eyeSide, Blocks.BLACK_CONCRETE);
        f.set(w + 3, 3, mid + eyeSide, Blocks.BLACK_CONCRETE);
        for (int lz = mid - 1; lz <= mid + 1; lz++) {
            f.set(w + 5, 0, lz, Blocks.RED_TERRACOTTA);
        }
        // Its tail at the stern, a fan widening outward.
        for (int k = 0; k < 3; k++) {
            for (int ly = -k; ly <= 2 + k; ly++) {
                f.set(-3 - k, ly, mid, Blocks.ORANGE_TERRACOTTA);
            }
        }
        // The dining room: tables with white cloths and chairs.
        for (int lx = 3; lx <= w - 6; lx += 4) {
            for (int lz : new int[] {2, d - 3}) {
                f.set(lx, 1, lz, Blocks.OAK_FENCE);
                f.set(lx, 2, lz, Blocks.WHITE_CARPET);
                f.set(lx - 1, 1, lz, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
                f.set(lx + 1, 1, lz, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
            }
        }
        // The kitchen along the far end: stoves, smokers and the pantry.
        for (int lz = 1; lz <= d - 2; lz++) {
            boolean pantry = lz == mid;
            if (pantry) {
                f.chest(w - 2, 1, lz, Direction.WEST, GLWorldgen.BARATIE_PANTRY);
            } else {
                f.set(w - 2, 1, lz, (lz % 2 == 0 ? Blocks.SMOKER : Blocks.FURNACE).defaultBlockState()
                        .setValue(FurnaceBlock.FACING, Direction.WEST));
            }
        }
        f.container(w - 3, 1, 1, Blocks.BARREL.defaultBlockState(), GLWorldgen.BARATIE_PANTRY);
        f.set(1, 1, 1, Blocks.LANTERN);
        f.set(1, 1, d - 2, Blocks.LANTERN);
        // The chefs' quarters upstairs.
        f.bed(1, 5, d - 3, Blocks.WHITE_BED);
        f.chest(3, 5, d - 2, Direction.NORTH, GLWorldgen.BARATIE_PANTRY);
        f.set(2, 5, 1, Blocks.LANTERN);
        f.resident(GLNpcs.BARATIE_COOK, w - 4, 1, mid - 1);
        f.resident(GLNpcs.BARATIE_COOK, w - 4, 1, mid + 1);
        f.resident(GLNpcs.BARATIE_COOK, 4, 1, mid);
        f.resident(GLNpcs.TOWNSPERSON, 6, 1, 3);
        f.resident(GLNpcs.PIRATE_GRUNT, w / 2 - 2, 1, 2);
        f.resident(GLNpcs.PIRATE_GUNNER, w / 2 + 2, 1, 2);
        f.boss(GLBosses.KRIEG, w / 2, 1, mid, w / 2, 0);
    }

    /** Loguetown's execution platform, where Gol D. Roger died: a raised wooden scaffold with steps, posts and a beam. */
    private void scaffold(Frame f) {
        int w = f.w;
        int d = f.d;
        f.foundation(0, 0, w - 1, d - 1, Blocks.STONE_BRICKS);
        f.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.STONE_BRICKS);
        f.fill(0, 1, 0, w - 1, 12, d - 1, AIR);
        // The platform on its legs, four blocks up.
        for (int lx = 1; lx <= w - 2; lx++) {
            for (int lz = 1; lz <= d - 2; lz++) {
                f.set(lx, 4, lz, Blocks.SPRUCE_PLANKS);
            }
        }
        for (int[] leg : new int[][] {{1, 1}, {w - 2, 1}, {1, d - 2}, {w - 2, d - 2}}) {
            for (int ly = 1; ly <= 3; ly++) {
                f.set(leg[0], ly, leg[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            }
        }
        // Steps up from the front.
        for (int k = 0; k < 4; k++) {
            f.set(w / 2, k, -3 + k, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
            f.set(w / 2 + 1, k, -3 + k, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
        }
        // The posts and crossbeam at the back of the platform.
        int back = d - 3;
        for (int ly = 5; ly <= 10; ly++) {
            f.set(2, ly, back, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
            f.set(w - 3, ly, back, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        for (int lx = 2; lx <= w - 3; lx++) {
            f.set(lx, 11, back, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
        }
        f.set(w / 2, 5, back, Blocks.OAK_FENCE);
        f.set(w / 2, 6, back, Blocks.OAK_FENCE);
        f.set(2, 5, 1, Blocks.LANTERN);
        f.set(w - 3, 5, 1, Blocks.LANTERN);
        f.resident(GLNpcs.TOWNSPERSON, 0, 1, -2);
        f.resident(GLNpcs.VILLAGER, w - 1, 1, -2);
    }

    /** A sword shop in Loguetown: racks of blades, a counter and the dealer behind it. */
    private void swordShop(Frame f) {
        int w = f.w;
        int d = f.d;
        shell(f, TOWN, 1);
        for (int lx = 1; lx <= w - 2; lx++) {
            f.set(lx, 1, d - 3, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        for (int lx = 1; lx <= w - 2; lx += 2) {
            f.set(lx, 2, d - 2, Blocks.IRON_BARS);
            f.set(lx, 1, d - 2, Blocks.BARREL);
        }
        f.chest(w - 2, 1, 1, Direction.SOUTH, GLWorldgen.SWORD_SHOP);
        f.set(1, 1, 1, Blocks.LANTERN);
        f.set(1, 1, d - 2, Blocks.GRINDSTONE);
        f.resident(GLNpcs.SWORD_DEALER, w / 2, 1, d - 2);
    }

    /** Bell-mère's tangerine grove: rows of small trees behind a fence. */
    private void grove(Frame f) {
        int w = f.w;
        int d = f.d;
        f.foundation(0, 0, w - 1, d - 1, Blocks.DIRT);
        f.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.GRASS_BLOCK);
        f.fill(0, 1, 0, w - 1, 7, d - 1, AIR);
        for (int lx = 0; lx < w; lx++) {
            for (int lz = 0; lz < d; lz++) {
                boolean xEdge = lx == 0 || lx == w - 1;
                boolean zEdge = lz == 0 || lz == d - 1;
                if (xEdge || zEdge) {
                    f.set(lx, 1, lz, fence(xEdge && lz > 0, zEdge && lx < w - 1, xEdge && lz < d - 1, zEdge && lx > 0));
                }
            }
        }
        f.set(w / 2, 1, 0, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        f.fill(w / 2, 0, 1, w / 2, 0, d - 2, Blocks.DIRT_PATH);
        BlockState leaves = leaves(GLWorldgen.TANGERINE_LEAVES.get());
        for (int lx = 2; lx < w - 1; lx += 3) {
            if (lx == w / 2) {
                continue;
            }
            for (int lz = 2; lz < d - 1; lz += 3) {
                f.set(lx, 1, lz, Blocks.OAK_LOG);
                f.set(lx, 2, lz, Blocks.OAK_LOG);
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        if (lx + ox != w / 2 && lx + ox > 0 && lx + ox < w - 1 && lz + oz > 0 && lz + oz < d - 1) {
                            f.set(lx + ox, 3, lz + oz, leaves);
                            if (ox == 0 || oz == 0) {
                                f.set(lx + ox, 4, lz + oz, leaves);
                            }
                        }
                    }
                }
                f.set(lx, 3, lz, Blocks.OAK_LOG);
            }
        }
        f.resident(GLNpcs.TANGERINE_FARMER, w / 2, 1, d / 2);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Pier and treasure
    // ---------------------------------------------------------------------------------------------------------

    private void pier() {
        IslandPlan.Pier pier = plan.pier;
        Direction out = pier.out();
        Direction side = out.getClockWise();
        int deck = plan.sea;
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState post = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        for (int i = 0; i < pier.length(); i++) {
            int x = pier.x() + out.getStepX() * i;
            int z = pier.z() + out.getStepZ() * i;
            for (int s = -1; s <= 1; s++) {
                int px = x + side.getStepX() * s;
                int pz = z + side.getStepZ() * s;
                int h = plan.height(px, pz);
                if (h > deck) {
                    continue;
                }
                set(px, deck, pz, planks);
                set(px, deck + 1, pz, AIR);
                if (s != 0 && i % 4 == 3) {
                    for (int y = deck - 1; y > deck - 30 && box.isInside(px, y, pz) && replaceable(get(px, y, pz)); y--) {
                        set(px, y, pz, post);
                    }
                    set(px, deck + 1, pz, Blocks.OAK_FENCE.defaultBlockState());
                    if (i == pier.length() - 1) {
                        set(px, deck + 2, pz, Blocks.LANTERN.defaultBlockState());
                    }
                }
            }
        }
        // A travelling merchant has moored at Shells Town's pier.
        int mx = pier.x() + out.getStepX() * 4;
        int mz = pier.z() + out.getStepZ() * 4;
        if (plan.type == IslandType.SHELLS_TOWN && box.isInside(mx, deck + 1, mz) && plan.height(mx, mz) <= deck) {
            NpcEntity.spawn(level, GLNpcs.TRAVELLING_MERCHANT, new BlockPos(mx, deck + 1, mz), out.toYRot() + 180, MobSpawnType.STRUCTURE);
        }
    }

    private void treasure() {
        int[] at = plan.treasure;
        if (at == null) {
            return;
        }
        int x = at[0];
        int z = at[1];
        int h = plan.height(x, z);
        if (h == Integer.MIN_VALUE) {
            return;
        }
        container(x, h - 2, z, Blocks.CHEST.defaultBlockState(), GLWorldgen.BURIED_TREASURE);
        set(x, h - 1, z, Blocks.SAND.defaultBlockState());
        BlockState mark = Blocks.RED_SAND.defaultBlockState();
        for (int[] o : new int[][] {{0, 0}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
            if (plan.height(x + o[0], z + o[1]) == h) {
                set(x + o[0], h, z + o[1], mark);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Trees
    // ---------------------------------------------------------------------------------------------------------

    private static final int CELL = 7;

    private void flora() {
        double density = switch (plan.type) {
            case DAWN_ISLAND -> 0.75;
            case SHELLS_TOWN -> 0.35;
            case CONOMI_ISLANDS -> 0.5;
            case ORANGE_TOWN -> 0.3;
            case SYRUP_VILLAGE -> 0.65;
            case BARATIE -> 0.15;
            case LOGUETOWN -> 0.25;
        };
        int gx0 = Math.floorDiv(box.minX() - 4, CELL);
        int gx1 = Math.floorDiv(box.maxX() + 4, CELL);
        int gz0 = Math.floorDiv(box.minZ() - 4, CELL);
        int gz1 = Math.floorDiv(box.maxZ() + 4, CELL);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                if (plan.hash(gx, gz, 11) > density) {
                    continue;
                }
                int x = gx * CELL + 1 + (int) (plan.hash(gx, gz, 12) * (CELL - 2));
                int z = gz * CELL + 1 + (int) (plan.hash(gx, gz, 13) * (CELL - 2));
                int h = plan.height(x, z);
                if (h == Integer.MIN_VALUE || h < plan.sea || plan.occupied(x, z) || plan.path(x, z) || nearPierOrTreasure(x, z)) {
                    continue;
                }
                boolean beach = plan.beach(x, z, h);
                if (plan.type.flora() == IslandType.Flora.PALM) {
                    palm(x, h + 1, z, plan.hash(gx, gz, 14), plan.hash(gx, gz, 15));
                } else if (!beach) {
                    oak(x, h + 1, z, plan.hash(gx, gz, 14));
                }
            }
        }
    }

    private boolean nearPierOrTreasure(int x, int z) {
        IslandPlan.Pier pier = plan.pier;
        int ex = pier.x() + pier.out().getStepX() * pier.length();
        int ez = pier.z() + pier.out().getStepZ() * pier.length();
        if (new IslandPlan.Path(pier.x(), pier.z(), ex, ez).distance(x, z) < 4) {
            return true;
        }
        return plan.treasure != null && Math.abs(x - plan.treasure[0]) <= 3 && Math.abs(z - plan.treasure[1]) <= 3;
    }

    private void oak(int x, int y, int z, double roll) {
        int height = 4 + (int) (roll * 3);
        BlockState leaves = leaves(Blocks.OAK_LEAVES);
        int top = y + height - 1;
        for (int ly = top - 2; ly <= top + 1; ly++) {
            int radius = ly <= top - 1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (corner && (ly == top + 1 || radius == 1 && ly == top || plan.hash(x + dx, z + dz, ly) < 0.5)) {
                        continue;
                    }
                    soft(x + dx, ly, z + dz, leaves);
                }
            }
        }
        for (int i = 0; i < height; i++) {
            set(x, y + i, z, Blocks.OAK_LOG.defaultBlockState());
        }
    }

    /** A leaning palm with a crown of drooping fronds. */
    private void palm(int x, int y, int z, double roll, double lean) {
        int height = 6 + (int) (roll * 3);
        Direction toward = Direction.from2DDataValue((int) (lean * 4));
        BlockState trunk = Blocks.JUNGLE_LOG.defaultBlockState();
        int ox = 0;
        int oz = 0;
        for (int i = 0; i < height; i++) {
            if (i >= 3 && i % 3 == 0) {
                set(x + ox + toward.getStepX(), y + i - 1, z + oz + toward.getStepZ(), trunk);
                ox += toward.getStepX();
                oz += toward.getStepZ();
            }
            set(x + ox, y + i, z + oz, trunk);
        }
        int tx = x + ox;
        int ty = y + height;
        int tz = z + oz;
        BlockState leaves = leaves(Blocks.JUNGLE_LEAVES);
        soft(tx, ty, tz, leaves);
        soft(tx, ty + 1, tz, leaves);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= 3; k++) {
                soft(tx + dir.getStepX() * k, ty - (k == 3 ? 1 : 0), tz + dir.getStepZ() * k, leaves);
            }
            Direction next = dir.getClockWise();
            for (int k = 1; k <= 2; k++) {
                soft(tx + (dir.getStepX() + next.getStepX()) * k, ty - (k == 2 ? 1 : 0), tz + (dir.getStepZ() + next.getStepZ()) * k, leaves);
            }
        }
        if (roll > 0.4) {
            soft(tx + 1, ty - 1, tz, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, Direction.WEST).setValue(CocoaBlock.AGE, 2));
        }
    }
}
