package com.nerodev.grandline.quest;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.world.GLWorldgen;
import com.nerodev.grandline.world.island.IslandPiece;
import com.nerodev.grandline.world.island.IslandType;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * The Log Pose: islands are recorded as the player reaches them, and the needle points the way. Left to itself it
 * sets on the nearest island the player has not found (its name a mystery until they arrive); it can also be turned
 * back to any island already recorded.
 */
public final class LogPose {
    public static final ResourceKey<StructureSet> ISLAND_SET = ResourceKey.create(Registries.STRUCTURE_SET, GrandLine.id("east_blue_islands"));
    /** How many rings of island regions around the player the needle can feel (each ring is a region of islands). */
    private static final int SEARCH_RINGS = 6;

    private LogPose() {}

    /** Every couple of seconds: has the player reached an island, and what does the Log Pose in their hand read? */
    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), GLWorldgen.ISLANDS);
        if (start.isValid()) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece instanceof IslandPiece island) {
                    discover(player, island.island(), island.centre());
                    break;
                }
            }
        }
        if (player.getMainHandItem().getItem() instanceof LogPoseItem || player.getOffhandItem().getItem() instanceof LogPoseItem) {
            player.displayClientMessage(reading(player), true);
        }
    }

    /**
     * {@code player} has reached {@code island}: it is recorded, announced, counted towards quests, and if the Log
     * Pose was pulling towards it, it sets on the next island. @return false if it was already known
     */
    public static boolean discover(ServerPlayer player, IslandType island, BlockPos centre) {
        Journey.Island found = new Journey.Island(island.getSerializedName(), centre);
        if (GLPlayer.data(player).journey().islandAt(centre) != null) {
            return false;
        }
        GLPlayer.edit(player, data -> {
            Journey journey = data.journey();
            journey.record(found);
            boolean arrived = journey.heading().map(h -> Journey.horizontalDistanceSqr(h.target(), centre)
                    < (double) Journey.SAME_ISLAND * Journey.SAME_ISLAND).orElse(true);
            if (arrived) {
                journey.setHeading(Optional.empty());
            }
        });
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Island discovered").withStyle(ChatFormatting.AQUA)));
        player.connection.send(new ClientboundSetTitleTextPacket(island.displayName().copy().withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.literal("Your Log Pose records ").withStyle(ChatFormatting.AQUA)
                .append(island.displayName().copy().withStyle(ChatFormatting.GOLD)).append(Component.literal(".").withStyle(ChatFormatting.AQUA)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.8f);
        Quests.onDiscover(player, island);
        if (GLPlayer.data(player).journey().heading().isEmpty()) {
            aimAtUnknown(player);
        }
        return true;
    }

    /** Sets the needle on the nearest island not yet found. @return where it points, or null if no island is near */
    @Nullable
    public static BlockPos aimAtUnknown(ServerPlayer player) {
        BlockPos next = nextUnknown(player.serverLevel(), player.blockPosition(), GLPlayer.data(player).journey());
        GLPlayer.edit(player, data -> data.journey().setHeading(next == null ? Optional.empty()
                : Optional.of(new Journey.Heading("", next))));
        return next;
    }

    /** Turns the needle to the next recorded island, then to the pull of an unknown one, and round again. */
    public static void cycle(ServerPlayer player) {
        Journey journey = GLPlayer.data(player).journey();
        List<Journey.Island> islands = journey.islands();
        Optional<Journey.Heading> heading = journey.heading();
        int current = islands.size();
        if (heading.isPresent() && !heading.get().unknown()) {
            for (int i = 0; i < islands.size(); i++) {
                if (islands.get(i).centre().equals(heading.get().target())) {
                    current = i;
                }
            }
        }
        int next = (current + 1) % (islands.size() + 1);
        if (next == islands.size()) {
            aimAtUnknown(player);
        } else {
            Journey.Island island = islands.get(next);
            GLPlayer.edit(player, data -> data.journey().setHeading(Optional.of(new Journey.Heading(island.type(), island.centre()))));
        }
    }

    /** Points the needle at a recorded island. @return false if it has not been found */
    public static boolean aimAt(ServerPlayer player, IslandType island) {
        for (Journey.Island known : GLPlayer.data(player).journey().islands()) {
            if (known.type().equals(island.getSerializedName())) {
                GLPlayer.edit(player, data -> data.journey().setHeading(Optional.of(new Journey.Heading(known.type(), known.centre()))));
                return true;
            }
        }
        return false;
    }

    /**
     * The centre of the nearest island the journey has not recorded, judged as {@code /locate} does: the spot each
     * region of the island structure set would use, checked for an island that can (or did) rise there.
     */
    @Nullable
    public static BlockPos nextUnknown(ServerLevel level, BlockPos from, Journey journey) {
        StructureSet set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ISLAND_SET);
        if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return null;
        }
        long seed = level.getChunkSource().getGeneratorState().getLevelSeed();
        int spacing = spread.spacing();
        int rx = Math.floorDiv(from.getX() >> 4, spacing);
        int rz = Math.floorDiv(from.getZ() >> 4, spacing);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int ring = 0; ring <= SEARCH_RINGS; ring++) {
            if (best != null && (ring - 1) * spacing * 16.0 > Math.sqrt(bestDistance)) {
                break;
            }
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    ChunkPos chunk = spread.getPotentialStructureChunk(seed, rx + dx, rz + dz);
                    BlockPos centre = new BlockPos(chunk.getMiddleBlockX(), level.getSeaLevel(), chunk.getMiddleBlockZ());
                    double distance = Journey.horizontalDistanceSqr(centre, from);
                    if (distance >= bestDistance || journey.islandAt(centre) != null) {
                        continue;
                    }
                    for (StructureSet.StructureSelectionEntry entry : set.structures()) {
                        StructureCheckResult result = level.structureManager().checkStructurePresence(chunk, entry.structure().value(), spread,
                                false);
                        if (result != StructureCheckResult.START_NOT_PRESENT) {
                            best = centre;
                            bestDistance = distance;
                            break;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** What the Log Pose reads: where it points, how far, and which way. */
    public static MutableComponent reading(ServerPlayer player) {
        Optional<Journey.Heading> heading = GLPlayer.data(player).journey().heading();
        if (heading.isEmpty()) {
            return Component.literal("The needle drifts: no island pulls at it here").withStyle(ChatFormatting.GRAY);
        }
        BlockPos target = heading.get().target();
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        IslandType island = IslandType.byName(heading.get().type());
        Component name = island == null ? Component.literal("an unknown island").withStyle(ChatFormatting.LIGHT_PURPLE)
                : island.displayName().copy().withStyle(ChatFormatting.GOLD);
        return Component.literal("Log Pose: ").withStyle(ChatFormatting.AQUA).append(name)
                .append(Component.literal(String.format(Locale.ROOT, " - %,d blocks %s", distance, compass(dx, dz))).withStyle(ChatFormatting.WHITE));
    }

    /** The compass direction of (dx, dz): N, NE, E... */
    public static String compass(double dx, double dz) {
        double angle = Mth.wrapDegrees(Math.toDegrees(Math.atan2(dx, -dz)));
        String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return points[Math.floorMod((int) Math.round(angle / 45.0), 8)];
    }
}
