package com.nerodev.grandline.world.island;

import com.nerodev.grandline.world.GLWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The whole island as one structure piece. It stores only the island type, centre, sea level and seed; every chunk
 * the island reaches rebuilds the same {@link IslandPlan} from those and places its own slice of land, buildings
 * and trees, so generation stays chunk-local and cheap.
 */
public class IslandPiece extends StructurePiece {
    private final IslandType island;
    private final int cx;
    private final int cz;
    private final int sea;
    private final long seed;
    private volatile IslandPlan plan;

    public IslandPiece(IslandType island, int cx, int cz, int sea, long seed) {
        super(GLWorldgen.ISLAND_PIECE.get(), 0, bounds(island, cx, cz, sea));
        this.island = island;
        this.cx = cx;
        this.cz = cz;
        this.sea = sea;
        this.seed = seed;
    }

    public IslandPiece(CompoundTag tag) {
        super(GLWorldgen.ISLAND_PIECE.get(), tag);
        IslandType type = IslandType.byName(tag.getString("island"));
        this.island = type == null ? IslandType.DAWN_ISLAND : type;
        this.cx = tag.getInt("cx");
        this.cz = tag.getInt("cz");
        this.sea = tag.getInt("sea");
        this.seed = tag.getLong("seed");
    }

    /** Everything the island can touch: its shelf, pier, hills and tallest towers. */
    public static BoundingBox bounds(IslandType island, int cx, int cz, int sea) {
        int reach = island.reach();
        return new BoundingBox(cx - reach, sea - 48, cz - reach, cx + reach, sea + island.peak() + 36, cz + reach);
    }

    public IslandType island() {
        return island;
    }

    /** The island's centre at sea level. */
    public BlockPos centre() {
        return new BlockPos(cx, sea, cz);
    }

    public IslandPlan plan() {
        IslandPlan plan = this.plan;
        if (plan == null) {
            plan = new IslandPlan(island, cx, cz, sea, seed);
            this.plan = plan;
        }
        return plan;
    }

    /**
     * Raises an island at once with its centre at {@code centre}, whose height is taken as sea level. Open air
     * below that level within the island's reach is flooded first, so the island always stands in the sea.
     */
    public static IslandPlan placeNow(ServerLevel level, IslandType island, BlockPos centre, long seed) {
        int sea = centre.getY();
        IslandPiece piece = new IslandPiece(island, centre.getX(), centre.getZ(), sea, seed);
        BoundingBox bounds = piece.getBoundingBox();
        int reach = island.reach();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int chunkX = bounds.minX() >> 4; chunkX <= bounds.maxX() >> 4; chunkX++) {
            for (int chunkZ = bounds.minZ() >> 4; chunkZ <= bounds.maxZ() >> 4; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                level.getChunk(chunkX, chunkZ);
                for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                    for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                        if (Mth.square(x - centre.getX()) + Mth.square(z - centre.getZ()) > reach * reach) {
                            continue;
                        }
                        for (int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z); y < sea; y++) {
                            if (level.getBlockState(pos.set(x, y, z)).isAir()) {
                                level.setBlock(pos, water, 2);
                            }
                        }
                    }
                }
                BoundingBox box = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, chunk, centre);
            }
        }
        return piece.plan();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("island", island.getSerializedName());
        tag.putInt("cx", cx);
        tag.putInt("cz", cz);
        tag.putInt("sea", sea);
        tag.putLong("seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        new IslandBuilder(plan(), level, box, random).build();
    }
}
