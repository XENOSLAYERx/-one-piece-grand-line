package com.nerodev.grandline.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's voyage so far: the quests under way (with progress on each objective), the quests done, the islands
 * found, and where their Log Pose points. Part of the saved, synced player data.
 */
public final class Journey {
    /** An island found at sea: which island it is ({@code dawn_island}) and its centre. */
    public record Island(String type, BlockPos centre) {
        public static final Codec<Island> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Island::type),
                BlockPos.CODEC.fieldOf("centre").forGetter(Island::centre)
        ).apply(i, Island::new));
    }

    /**
     * Where the Log Pose points: a known island, or (with an empty type) the pull of the nearest island not yet
     * found, whose name is a mystery until the ship arrives.
     */
    public record Heading(String type, BlockPos target) {
        public static final Codec<Heading> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("type", "").forGetter(Heading::type),
                BlockPos.CODEC.fieldOf("target").forGetter(Heading::target)
        ).apply(i, Heading::new));

        public boolean unknown() {
            return type.isEmpty();
        }
    }

    public static final Codec<Journey> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT.listOf()).optionalFieldOf("active", Map.of()).forGetter(j -> j.active),
            ResourceLocation.CODEC.listOf().optionalFieldOf("completed", List.of()).forGetter(j -> List.copyOf(j.completed)),
            ResourceLocation.CODEC.optionalFieldOf("tracked").forGetter(j -> j.tracked),
            Island.CODEC.listOf().optionalFieldOf("islands", List.of()).forGetter(j -> j.islands),
            Heading.CODEC.optionalFieldOf("heading").forGetter(j -> j.heading)
    ).apply(i, Journey::new));

    /** Islands closer than this to a recorded one are the same island. */
    public static final int SAME_ISLAND = 96;

    private final Map<ResourceLocation, List<Integer>> active = new LinkedHashMap<>();
    private final Set<ResourceLocation> completed = new LinkedHashSet<>();
    private Optional<ResourceLocation> tracked;
    private final List<Island> islands = new ArrayList<>();
    private Optional<Heading> heading;

    public Journey() {
        this(Map.of(), List.of(), Optional.empty(), List.of(), Optional.empty());
    }

    private Journey(Map<ResourceLocation, List<Integer>> active, List<ResourceLocation> completed, Optional<ResourceLocation> tracked,
            List<Island> islands, Optional<Heading> heading) {
        active.forEach((quest, progress) -> this.active.put(quest, new ArrayList<>(progress)));
        this.completed.addAll(completed);
        this.tracked = tracked;
        this.islands.addAll(islands);
        this.heading = heading;
    }

    public void copyFrom(Journey other) {
        active.clear();
        other.active.forEach((quest, progress) -> active.put(quest, new ArrayList<>(progress)));
        completed.clear();
        completed.addAll(other.completed);
        tracked = other.tracked;
        islands.clear();
        islands.addAll(other.islands);
        heading = other.heading;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Quests
    // ---------------------------------------------------------------------------------------------------------

    /** Quests under way, in the order they were taken. */
    public Set<ResourceLocation> activeQuests() {
        return Collections.unmodifiableSet(active.keySet());
    }

    public boolean isActive(ResourceLocation quest) {
        return active.containsKey(quest);
    }

    public boolean isCompleted(ResourceLocation quest) {
        return completed.contains(quest);
    }

    public Set<ResourceLocation> completedQuests() {
        return Collections.unmodifiableSet(completed);
    }

    /** Progress on objective {@code index} of an active quest (0 if it is not under way). */
    public int progress(ResourceLocation quest, int index) {
        List<Integer> progress = active.get(quest);
        return progress == null || index >= progress.size() ? 0 : progress.get(index);
    }

    /** Sets progress on an objective. @return whether it changed */
    public boolean setProgress(ResourceLocation quest, int index, int value) {
        List<Integer> progress = active.get(quest);
        if (progress == null) {
            return false;
        }
        while (progress.size() <= index) {
            progress.add(0);
        }
        if (progress.get(index) == value) {
            return false;
        }
        progress.set(index, value);
        return true;
    }

    public void start(ResourceLocation quest, int objectives) {
        List<Integer> progress = new ArrayList<>(objectives);
        for (int i = 0; i < objectives; i++) {
            progress.add(0);
        }
        active.put(quest, progress);
        completed.remove(quest);
        if (tracked.isEmpty()) {
            tracked = Optional.of(quest);
        }
    }

    public void complete(ResourceLocation quest) {
        active.remove(quest);
        completed.add(quest);
        if (tracked.map(quest::equals).orElse(false)) {
            tracked = active.keySet().stream().findFirst();
        }
    }

    /** Drops a quest, done or not, as if it had never been taken. */
    public void forget(ResourceLocation quest) {
        active.remove(quest);
        completed.remove(quest);
        if (tracked.map(quest::equals).orElse(false)) {
            tracked = active.keySet().stream().findFirst();
        }
    }

    public void clearQuests() {
        active.clear();
        completed.clear();
        tracked = Optional.empty();
    }

    /** The quest shown on screen. */
    public Optional<ResourceLocation> tracked() {
        return tracked.filter(active::containsKey);
    }

    public void track(ResourceLocation quest) {
        if (active.containsKey(quest)) {
            tracked = Optional.of(quest);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Islands and the Log Pose
    // ---------------------------------------------------------------------------------------------------------

    /** Islands found, in the order they were found. */
    public List<Island> islands() {
        return Collections.unmodifiableList(islands);
    }

    /** The recorded island at {@code pos}, if any. */
    @Nullable
    public Island islandAt(BlockPos pos) {
        for (Island island : islands) {
            if (horizontalDistanceSqr(island.centre(), pos) < (double) SAME_ISLAND * SAME_ISLAND) {
                return island;
            }
        }
        return null;
    }

    public boolean hasFound(String type) {
        return islands.stream().anyMatch(island -> island.type().equals(type));
    }

    /** Records an island. @return false if it was already known */
    public boolean record(Island island) {
        if (islandAt(island.centre()) != null) {
            return false;
        }
        islands.add(island);
        return true;
    }

    public void clearIslands() {
        islands.clear();
        heading = Optional.empty();
    }

    public Optional<Heading> heading() {
        return heading;
    }

    public void setHeading(Optional<Heading> heading) {
        this.heading = heading;
    }

    public static double horizontalDistanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
