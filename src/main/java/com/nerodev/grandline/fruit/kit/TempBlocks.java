package com.nerodev.grandline.fruit.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Blocks that powers create for a while: ice floors, barrier walls, wax cages, sand mounds, candy spikes.
 * <p>
 * They only ever replace air, plants, snow layers or fluids (never anything a player built), revert to exactly
 * what was there when their time runs out, drop nothing when broken, and are all reverted when the server stops,
 * so no power can permanently change the world unless a fruit explicitly does it with griefing enabled.
 */
public final class TempBlocks {
    private record Entry(BlockState original, BlockState placed, long until, boolean unbreakable) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Entry>> PLACED = new HashMap<>();

    private TempBlocks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> revertAll(e.getServer()));
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, TempBlocks::onBreak);
        // Explosions leave temporary blocks alone, so they can never be blown into item drops.
        NeoForge.EVENT_BUS.addListener(ExplosionEvent.Detonate.class,
                e -> e.getAffectedBlocks().removeIf(pos -> isTemporary(e.getLevel(), pos)));
        NeoForge.EVENT_BUS.addListener(PistonEvent.Pre.class, TempBlocks::onPiston);
    }

    /** True if {@code state} may be replaced by a temporary block. */
    public static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced()
                || (!state.getFluidState().isEmpty() && state.getBlock() instanceof LiquidBlock);
    }

    /**
     * Places {@code state} at {@code pos} for {@code ticks}. If the spot already holds a temporary block, it is
     * swapped and its timer extended (the original block underneath is still what comes back).
     *
     * @return true if placed
     */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        return place(level, pos, state, ticks, false);
    }

    /**
     * As {@link #place(ServerLevel, BlockPos, BlockState, int)}; an {@code unbreakable} block (a barrier) cannot be
     * broken by players until it runs out.
     */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks, boolean unbreakable) {
        if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        Map<BlockPos, Entry> placed = PLACED.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        BlockPos key = pos.immutable();
        Entry existing = placed.get(key);
        BlockState current = level.getBlockState(key);
        BlockState original;
        if (existing != null && current == existing.placed()) {
            original = existing.original();
        } else if (replaceable(current)) {
            original = current;
        } else {
            return false;
        }
        if (!level.setBlock(key, state, Block.UPDATE_ALL)) {
            return false;
        }
        long until = level.getGameTime() + Math.max(1, ticks);
        placed.put(key, new Entry(original, state, existing != null ? Math.max(until, existing.until()) : until, unbreakable));
        return true;
    }

    public static boolean isTemporary(Level level, BlockPos pos) {
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        return placed != null && placed.containsKey(pos);
    }

    /** Reverts one temporary block now. */
    public static void revert(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        if (placed == null) {
            return;
        }
        Entry entry = placed.remove(pos);
        if (entry != null && level.getBlockState(pos) == entry.placed()) {
            level.setBlock(pos, entry.original(), Block.UPDATE_ALL);
        }
    }

    /** Reverts every temporary block in {@code level} within {@code radius} of {@code center}. */
    public static int revertNear(ServerLevel level, BlockPos center, double radius) {
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        if (placed == null) {
            return 0;
        }
        List<BlockPos> near = new ArrayList<>();
        for (BlockPos pos : placed.keySet()) {
            if (pos.distSqr(center) <= radius * radius) {
                near.add(pos);
            }
        }
        near.forEach(pos -> revert(level, pos));
        return near.size();
    }

    public static int count() {
        return PLACED.values().stream().mapToInt(Map::size).sum();
    }

    private static void tick(MinecraftServer server) {
        if (PLACED.isEmpty()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
            if (placed == null || placed.isEmpty()) {
                continue;
            }
            long now = level.getGameTime();
            Iterator<Map.Entry<BlockPos, Entry>> it = placed.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Entry> e = it.next();
                if (now < e.getValue().until()) {
                    continue;
                }
                it.remove();
                if (level.getBlockState(e.getKey()) == e.getValue().placed()) {
                    level.setBlock(e.getKey(), e.getValue().original(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void revertAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Map<BlockPos, Entry> placed = PLACED.remove(level.dimension());
            if (placed == null) {
                continue;
            }
            placed.forEach((pos, entry) -> {
                if (level.getBlockState(pos) == entry.placed()) {
                    level.setBlock(pos, entry.original(), Block.UPDATE_ALL);
                }
            });
        }
        PLACED.clear();
    }

    /** Breaking a temporary block restores what was under it and drops nothing. */
    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        if (placed == null) {
            return;
        }
        Entry entry = placed.get(event.getPos());
        if (entry != null && entry.unbreakable()) {
            event.setCanceled(true);
            return;
        }
        placed.remove(event.getPos());
        if (entry != null) {
            event.setCanceled(true);
            level.setBlock(event.getPos(), entry.original(), Block.UPDATE_ALL);
            level.levelEvent(2001, event.getPos(), Block.getId(entry.placed()));
        }
    }

    /** Pistons can't push, pull or crush temporary blocks, so they can never be carried off and kept. */
    private static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        if (placed == null || placed.isEmpty()) {
            return;
        }
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            return;
        }
        for (BlockPos pos : resolver.getToPush()) {
            if (placed.containsKey(pos)) {
                event.setCanceled(true);
                return;
            }
        }
        for (BlockPos pos : resolver.getToDestroy()) {
            if (placed.containsKey(pos)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
