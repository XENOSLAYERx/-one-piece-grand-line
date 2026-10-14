package com.nerodev.grandline.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.config.GLServerConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Island bosses come back. When a boss who holds an island falls, it is gone for a while; then, once no one is near
 * its post, it returns there (so every player on a server can play the story through). Saved with the overworld
 * ({@code data/grandline_boss_respawns.dat}).
 */
public final class BossRespawns extends SavedData {
    /** Tag on a boss who holds an island (and so returns after it falls). */
    public static final String RESIDENT_TAG = "grandline_island_boss";
    private static final String NAME = GrandLine.MODID + "_boss_respawns";
    /** No one may be this close to a boss's post when it returns. */
    private static final double CLEAR = 32;

    /** A fallen boss waiting to return to {@code home} in {@code dimension} at game time {@code due}. */
    public record Pending(ResourceLocation boss, ResourceKey<Level> dimension, BlockPos home, long due) {
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("boss").forGetter(Pending::boss),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Pending::dimension),
                BlockPos.CODEC.fieldOf("home").forGetter(Pending::home),
                Codec.LONG.fieldOf("due").forGetter(Pending::due)
        ).apply(i, Pending::new));
    }

    private static final Codec<List<Pending>> CODEC = Pending.CODEC.listOf();
    private static final SavedData.Factory<BossRespawns> FACTORY = new SavedData.Factory<>(BossRespawns::new, BossRespawns::load, null);

    private final List<Pending> pending = new ArrayList<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().overworld().getGameTime() % 200 == 0) {
                get(event.getServer()).tick(event.getServer());
            }
        });
    }

    public static BossRespawns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static BossRespawns load(CompoundTag tag, HolderLookup.Provider registries) {
        BossRespawns data = new BossRespawns();
        Tag list = tag.get("pending");
        if (list != null) {
            CODEC.parse(NbtOps.INSTANCE, list).resultOrPartial(error -> GrandLine.LOGGER.error("Bad boss respawn data: {}", error))
                    .ifPresent(data.pending::addAll);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, List.copyOf(pending))
                .resultOrPartial(error -> GrandLine.LOGGER.error("Could not save boss respawns: {}", error))
                .ifPresent(list -> tag.put("pending", list));
        return tag;
    }

    /** {@code boss} fell at its post {@code home}: it returns after the configured time (never, if that is 0). */
    public static void schedule(ServerLevel level, ResourceLocation boss, BlockPos home) {
        int minutes = GLServerConfig.bossRespawnMinutes();
        if (boss == null || minutes <= 0) {
            return;
        }
        BossRespawns data = get(level.getServer());
        data.pending.add(new Pending(boss, level.dimension(), home, level.getServer().overworld().getGameTime() + minutes * 1200L));
        data.setDirty();
    }

    public List<Pending> pending() {
        return Collections.unmodifiableList(pending);
    }

    /** Brings every fallen island boss back now (wherever their post is loaded and clear). @return how many returned */
    public int returnAll(MinecraftServer server) {
        pending.replaceAll(entry -> new Pending(entry.boss(), entry.dimension(), entry.home(), 0L));
        return tick(server);
    }

    /** Brings back every boss whose time has come, whose post is loaded, and who has the place to themselves. */
    public int tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        int returned = 0;
        for (Pending entry : List.copyOf(pending)) {
            ServerLevel level = server.getLevel(entry.dimension());
            if (level == null) {
                pending.remove(entry);
                setDirty();
                continue;
            }
            if (entry.due() > now || !level.isLoaded(entry.home())) {
                continue;
            }
            BlockPos home = entry.home();
            if (level.getNearestPlayer(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, CLEAR, false) != null) {
                continue;
            }
            BossEntity boss = BossEntity.spawn(level, entry.boss(), home, level.getRandom().nextFloat() * 360f, MobSpawnType.STRUCTURE);
            if (boss != null) {
                boss.addTag(RESIDENT_TAG);
                returned++;
            }
            pending.remove(entry);
            setDirty();
        }
        return returned;
    }
}
