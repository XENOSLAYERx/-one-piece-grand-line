package com.nerodev.grandline.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nerodev.grandline.GrandLine;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A player's persistent Grand Line profile: identity and progression.
 * <p>
 * Stored as a NeoForge data attachment ({@link GLAttachments#PLAYER_DATA}): saved with the player, copied on death,
 * and synced to the owning client. The server is the only writer; always mutate through
 * {@link GLPlayer#edit} so the client receives the change.
 * <p>
 * Fast-changing combat state (stamina, cooldowns) lives in its own attachment so it never forces a full profile
 * sync.
 */
public final class PlayerData {
    public static final ResourceLocation DEFAULT_RACE = GrandLine.id("human");
    public static final long MAX_BOUNTY = 99_999_999_999L;
    public static final double MAX_MASTERY = 100.0;

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("race", DEFAULT_RACE).forGetter(d -> d.race),
            ResourceLocation.CODEC.optionalFieldOf("faction").forGetter(d -> d.faction),
            Codec.LONG.optionalFieldOf("bounty", 0L).forGetter(d -> d.bounty),
            DeathMode.CODEC.optionalFieldOf("death_mode", DeathMode.NORMAL).forGetter(d -> d.deathMode),
            Codec.unboundedMap(UnlockCategory.CODEC, ResourceLocation.CODEC.listOf())
                    .optionalFieldOf("unlocks", Map.of()).forGetter(PlayerData::unlocksForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                    .optionalFieldOf("mastery", Map.of()).forGetter(d -> d.mastery),
            ResourceLocation.CODEC.optionalFieldOf("fruit").forGetter(d -> d.fruit),
            Codec.BOOL.optionalFieldOf("awakened", false).forGetter(d -> d.awakened)
    ).apply(i, PlayerData::new));

    /** Owner-only network sync. The profile is small, so the whole object is sent when it changes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private ResourceLocation race;
    private Optional<ResourceLocation> faction;
    private long bounty;
    private DeathMode deathMode;
    private final EnumMap<UnlockCategory, Set<ResourceLocation>> unlocks = new EnumMap<>(UnlockCategory.class);
    private final Map<String, Double> mastery = new HashMap<>();
    private Optional<ResourceLocation> fruit;
    private boolean awakened;

    public PlayerData() {
        this(DEFAULT_RACE, Optional.empty(), 0L, DeathMode.NORMAL, Map.of(), Map.of(), Optional.empty(), false);
    }

    private PlayerData(ResourceLocation race, Optional<ResourceLocation> faction, long bounty, DeathMode deathMode,
            Map<UnlockCategory, List<ResourceLocation>> unlocks, Map<String, Double> mastery,
            Optional<ResourceLocation> fruit, boolean awakened) {
        this.fruit = fruit;
        this.awakened = awakened;
        this.race = race;
        this.faction = faction;
        this.bounty = clampBounty(bounty);
        this.deathMode = deathMode;
        unlocks.forEach((category, ids) -> this.unlocks.put(category, new LinkedHashSet<>(ids)));
        mastery.forEach((key, value) -> this.mastery.put(key, clampMastery(value)));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------------------------------------------------

    public ResourceLocation race() {
        return race;
    }

    public void setRace(ResourceLocation race) {
        this.race = race;
    }

    public Optional<ResourceLocation> faction() {
        return faction;
    }

    public void setFaction(Optional<ResourceLocation> faction) {
        this.faction = faction;
    }

    public long bounty() {
        return bounty;
    }

    public void setBounty(long bounty) {
        this.bounty = clampBounty(bounty);
    }

    public void addBounty(long amount) {
        // Saturating add: never wraps around on overflow.
        long sum = amount > 0 && bounty > Long.MAX_VALUE - amount ? Long.MAX_VALUE : bounty + amount;
        this.bounty = clampBounty(sum);
    }

    public DeathMode deathMode() {
        return deathMode;
    }

    public void setDeathMode(DeathMode deathMode) {
        this.deathMode = deathMode;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Devil Fruit
    // ---------------------------------------------------------------------------------------------------------

    /** The Devil Fruit this player has eaten, if any. Change through FruitManager, not directly. */
    public Optional<ResourceLocation> fruit() {
        return fruit;
    }

    public void setFruit(Optional<ResourceLocation> fruit) {
        this.fruit = fruit;
        if (fruit.isEmpty()) {
            this.awakened = false;
        }
    }

    public boolean isAwakened() {
        return awakened;
    }

    public void setAwakened(boolean awakened) {
        this.awakened = awakened && fruit.isPresent();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Unlocks
    // ---------------------------------------------------------------------------------------------------------

    public boolean isUnlocked(UnlockCategory category, ResourceLocation id) {
        Set<ResourceLocation> set = unlocks.get(category);
        return set != null && set.contains(id);
    }

    /** @return true if this was newly unlocked */
    public boolean unlock(UnlockCategory category, ResourceLocation id) {
        return unlocks.computeIfAbsent(category, c -> new LinkedHashSet<>()).add(id);
    }

    /** @return true if it was unlocked before */
    public boolean lock(UnlockCategory category, ResourceLocation id) {
        Set<ResourceLocation> set = unlocks.get(category);
        return set != null && set.remove(id);
    }

    public Set<ResourceLocation> unlocked(UnlockCategory category) {
        return Collections.unmodifiableSet(unlocks.getOrDefault(category, Set.of()));
    }

    public void clearUnlocks(UnlockCategory category) {
        unlocks.remove(category);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mastery (0-100 per system key, e.g. "fruit/grandline:gum_gum", "haki/armament")
    // ---------------------------------------------------------------------------------------------------------

    public double mastery(String key) {
        return mastery.getOrDefault(key, 0.0);
    }

    public void setMastery(String key, double value) {
        double clamped = clampMastery(value);
        if (clamped <= 0) {
            mastery.remove(key);
        } else {
            mastery.put(key, clamped);
        }
    }

    public Map<String, Double> allMastery() {
        return Collections.unmodifiableMap(new TreeMap<>(mastery));
    }

    public void clearMastery() {
        mastery.clear();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Copies every field from {@code other}. Used by loadouts and full resets. */
    public void copyFrom(PlayerData other) {
        this.race = other.race;
        this.faction = other.faction;
        this.bounty = other.bounty;
        this.deathMode = other.deathMode;
        this.unlocks.clear();
        other.unlocks.forEach((category, ids) -> this.unlocks.put(category, new LinkedHashSet<>(ids)));
        this.mastery.clear();
        this.mastery.putAll(other.mastery);
        this.fruit = other.fruit;
        this.awakened = other.awakened;
    }

    private Map<UnlockCategory, List<ResourceLocation>> unlocksForCodec() {
        Map<UnlockCategory, List<ResourceLocation>> out = new EnumMap<>(UnlockCategory.class);
        unlocks.forEach((category, ids) -> {
            if (!ids.isEmpty()) {
                out.put(category, List.copyOf(ids));
            }
        });
        return out;
    }

    private static long clampBounty(long value) {
        return Math.max(0L, Math.min(MAX_BOUNTY, value));
    }

    private static double clampMastery(double value) {
        return Double.isNaN(value) ? 0.0 : Mth.clamp(value, 0.0, MAX_MASTERY);
    }
}
