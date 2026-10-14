package com.nerodev.grandline.arena;

import com.mojang.serialization.Codec;
import com.nerodev.grandline.GrandLine;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Every arena on the server, saved with the overworld ({@code data/grandline_arenas.dat}). */
public final class ArenaData extends SavedData {
    private static final String NAME = GrandLine.MODID + "_arenas";
    private static final Codec<List<Arena>> CODEC = Arena.CODEC.listOf();
    private static final SavedData.Factory<ArenaData> FACTORY = new SavedData.Factory<>(ArenaData::new, ArenaData::load, null);

    private final Map<String, Arena> arenas = new TreeMap<>();

    public static ArenaData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static ArenaData load(CompoundTag tag, HolderLookup.Provider registries) {
        ArenaData data = new ArenaData();
        Tag list = tag.get("arenas");
        if (list != null) {
            CODEC.parse(NbtOps.INSTANCE, list).resultOrPartial(error -> GrandLine.LOGGER.error("Bad arena data: {}", error))
                    .ifPresent(loaded -> loaded.forEach(arena -> data.arenas.put(key(arena.name()), arena)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, List.copyOf(arenas.values()))
                .resultOrPartial(error -> GrandLine.LOGGER.error("Could not save arenas: {}", error))
                .ifPresent(list -> tag.put("arenas", list));
        return tag;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public Collection<Arena> all() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public Optional<Arena> byName(String name) {
        return Optional.ofNullable(arenas.get(key(name)));
    }

    public void put(Arena arena) {
        arenas.put(key(arena.name()), arena);
        setDirty();
    }

    public boolean remove(String name) {
        boolean removed = arenas.remove(key(name)) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }
}
