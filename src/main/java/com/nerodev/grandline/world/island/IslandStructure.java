package com.nerodev.grandline.world.island;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nerodev.grandline.world.GLWorldgen;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * An island rising out of open sea. The structure JSON names which island ({@code "island": "dawn_island"}); the
 * island is only placed where the sea floor is deep enough all around it, so it never merges into a coast.
 */
public class IslandStructure extends Structure {
    public static final MapCodec<IslandStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            IslandType.CODEC.fieldOf("island").forGetter(IslandStructure::island)).apply(instance, IslandStructure::new));

    /** How far below the sea the floor must be at the centre and around the island's rim. */
    private static final int MIN_DEPTH = 5;

    private final IslandType island;

    public IslandStructure(StructureSettings settings, IslandType island) {
        super(settings);
        this.island = island;
    }

    public IslandType island() {
        return island;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        int sea = generator.getSeaLevel();
        int r = island.radius();
        int[][] probes = {{0, 0}, {r, 0}, {-r, 0}, {0, r}, {0, -r}, {r * 7 / 10, r * 7 / 10}, {-r * 7 / 10, -r * 7 / 10}};
        for (int[] probe : probes) {
            int floor = generator.getBaseHeight(x + probe[0], z + probe[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
                    context.randomState());
            if (floor > sea - MIN_DEPTH) {
                return Optional.empty();
            }
        }
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, sea, z), builder -> builder.addPiece(new IslandPiece(island, x, z, sea, seed))));
    }

    @Override
    public StructureType<?> type() {
        return GLWorldgen.ISLAND.get();
    }
}
