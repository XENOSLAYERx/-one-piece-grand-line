package com.nerodev.grandline.crew;

import com.mojang.serialization.Codec;
import com.nerodev.grandline.GrandLine;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Every crew on the server, saved with the overworld ({@code data/grandline_crews.dat}). */
public final class CrewData extends SavedData {
    private static final String NAME = GrandLine.MODID + "_crews";
    private static final Codec<List<Crew>> CODEC = Crew.CODEC.listOf();
    private static final SavedData.Factory<CrewData> FACTORY = new SavedData.Factory<>(CrewData::new, CrewData::load, null);

    private final Map<UUID, Crew> crews = new LinkedHashMap<>();

    public static CrewData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static CrewData load(CompoundTag tag, HolderLookup.Provider registries) {
        CrewData data = new CrewData();
        Tag list = tag.get("crews");
        if (list != null) {
            CODEC.parse(NbtOps.INSTANCE, list).resultOrPartial(error -> GrandLine.LOGGER.error("Bad crew data: {}", error))
                    .ifPresent(loaded -> loaded.forEach(crew -> data.crews.put(crew.id(), crew)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, List.copyOf(crews.values()))
                .resultOrPartial(error -> GrandLine.LOGGER.error("Could not save crews: {}", error))
                .ifPresent(list -> tag.put("crews", list));
        return tag;
    }

    public Collection<Crew> all() {
        return Collections.unmodifiableCollection(crews.values());
    }

    public Optional<Crew> byId(UUID id) {
        return Optional.ofNullable(crews.get(id));
    }

    public Optional<Crew> of(UUID player) {
        for (Crew crew : crews.values()) {
            if (crew.has(player)) {
                return Optional.of(crew);
            }
        }
        return Optional.empty();
    }

    public Optional<Crew> byName(String name) {
        for (Crew crew : crews.values()) {
            if (crew.name().equalsIgnoreCase(name)) {
                return Optional.of(crew);
            }
        }
        return Optional.empty();
    }

    void put(Crew crew) {
        crews.put(crew.id(), crew);
        setDirty();
    }

    void remove(UUID id) {
        crews.remove(id);
        setDirty();
    }

    /** Marks the data as changed after editing a crew. */
    void changed() {
        setDirty();
    }
}
