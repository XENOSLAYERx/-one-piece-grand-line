package com.nerodev.grandline.config;

import com.nerodev.grandline.GrandLine;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Per-world server config. Synced to clients by NeoForge.
 * <p>
 * Only settings that a shipped system actually reads live here. Each milestone adds the settings for the
 * systems it introduces (cooldowns, costs, bounty, spawn rates, PvP...), so no setting in this file is a no-op.
 */
public final class GLServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MODERATOR_OP_LEVEL;
    public static final ModConfigSpec.IntValue ADMIN_OP_LEVEL;
    public static final ModConfigSpec.IntValue CREATOR_OP_LEVEL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CREATOR_UUIDS;
    public static final ModConfigSpec.BooleanValue SINGLEPLAYER_OWNER_IS_CREATOR;

    public static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STAMINA_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ABILITY_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MASTERY_GAIN_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ABILITY_PVP;
    public static final ModConfigSpec.BooleanValue ABILITY_GRIEFING;
    public static final ModConfigSpec.BooleanValue SECOND_FRUIT_KILLS;

    public static final ModConfigSpec.BooleanValue HAKI_TRAINING;
    public static final ModConfigSpec.IntValue ARMAMENT_TRAINING_HITS;
    public static final ModConfigSpec.IntValue OBSERVATION_TRAINING_HITS;
    public static final ModConfigSpec.DoubleValue CONQUERORS_CHANCE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment(
                "Permission tiers: PLAYER < MODERATOR < ADMIN < CREATOR.",
                "A player gets the highest tier whose vanilla op level they meet.",
                "Players listed in creatorUuids are always CREATOR, regardless of op level.")
                .push("permissions");

        MODERATOR_OP_LEVEL = BUILDER
                .comment("Minimum vanilla op level for the MODERATOR tier (debug and inspection commands).")
                .defineInRange("moderatorOpLevel", 2, 0, 4);
        ADMIN_OP_LEVEL = BUILDER
                .comment("Minimum vanilla op level for the ADMIN tier (config inspection, world-level tools).")
                .defineInRange("adminOpLevel", 3, 0, 4);
        CREATOR_OP_LEVEL = BUILDER
                .comment("Minimum vanilla op level for the CREATOR tier (instant unlocks and all creator tools).",
                        "Singleplayer worlds with cheats enabled give the owner op level 4.")
                .defineInRange("creatorOpLevel", 4, 0, 4);
        CREATOR_UUIDS = BUILDER
                .comment("Player UUIDs that are always CREATOR, even without op. Example: [\"069a79f4-44e9-4726-a5be-fca90e38aaf5\"]")
                .defineListAllowEmpty("creatorUuids", List.of(),
                        () -> "00000000-0000-0000-0000-000000000000", GLServerConfig::isValidUuid);
        SINGLEPLAYER_OWNER_IS_CREATOR = BUILDER
                .comment("If true, the owner of a singleplayer / LAN world is CREATOR even with cheats disabled.")
                .define("singleplayerOwnerIsCreator", false);

        BUILDER.pop();

        BUILDER.comment("Ability and Devil Fruit balance.").push("gameplay");
        COOLDOWN_MULTIPLIER = BUILDER
                .comment("Multiplies every ability cooldown. 0.5 = half as long.")
                .defineInRange("cooldownMultiplier", 1.0, 0.0, 10.0);
        STAMINA_COST_MULTIPLIER = BUILDER
                .comment("Multiplies every ability stamina cost.")
                .defineInRange("staminaCostMultiplier", 1.0, 0.0, 10.0);
        ABILITY_DAMAGE_MULTIPLIER = BUILDER
                .comment("Multiplies damage dealt by abilities.")
                .defineInRange("abilityDamageMultiplier", 1.0, 0.0, 10.0);
        MASTERY_GAIN_MULTIPLIER = BUILDER
                .comment("Multiplies mastery gained from using abilities.")
                .defineInRange("masteryGainMultiplier", 1.0, 0.0, 100.0);
        ABILITY_PVP = BUILDER
                .comment("If false, abilities never damage other players.")
                .define("abilityPvp", true);
        ABILITY_GRIEFING = BUILDER
                .comment("If true, abilities may set blocks on fire or break weak blocks.")
                .define("abilityGriefing", false);
        SECOND_FRUIT_KILLS = BUILDER
                .comment("If true, deliberately eating a second Devil Fruit (sneak + eat) kills you in normal death mode,",
                        "as in the story. Creator death mode always swaps fruits instead.")
                .define("secondFruitKills", true);
        BUILDER.pop();

        BUILDER.comment("Haki: how players earn it without commands.").push("haki");
        HAKI_TRAINING = BUILDER
                .comment("If true, Armament and Observation Haki awaken through fighting (see the hit counts below).")
                .define("hakiTraining", true);
        ARMAMENT_TRAINING_HITS = BUILDER
                .comment("Unarmed or melee hits a player must land before Armament Haki awakens.")
                .defineInRange("armamentTrainingHits", 150, 1, 100000);
        OBSERVATION_TRAINING_HITS = BUILDER
                .comment("Hits a player must take from living attackers before Observation Haki awakens.")
                .defineInRange("observationTrainingHits", 100, 1, 100000);
        CONQUERORS_CHANCE = BUILDER
                .comment("Chance (0-1) that a new player is born with Conqueror's Haki. Rolled once per player.")
                .defineInRange("conquerorsChance", 0.05, 0.0, 1.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static <T> T value(ModConfigSpec.ConfigValue<T> value) {
        return isLoaded() ? value.get() : value.getDefault();
    }

    public static double cooldownMultiplier() {
        return value(COOLDOWN_MULTIPLIER);
    }

    public static double staminaCostMultiplier() {
        return value(STAMINA_COST_MULTIPLIER);
    }

    public static double abilityDamageMultiplier() {
        return value(ABILITY_DAMAGE_MULTIPLIER);
    }

    public static double masteryGainMultiplier() {
        return value(MASTERY_GAIN_MULTIPLIER);
    }

    public static boolean abilityPvp() {
        return value(ABILITY_PVP);
    }

    public static boolean abilityGriefing() {
        return value(ABILITY_GRIEFING);
    }

    public static boolean secondFruitKills() {
        return value(SECOND_FRUIT_KILLS);
    }

    public static boolean hakiTraining() {
        return value(HAKI_TRAINING);
    }

    public static int armamentTrainingHits() {
        return value(ARMAMENT_TRAINING_HITS);
    }

    public static int observationTrainingHits() {
        return value(OBSERVATION_TRAINING_HITS);
    }

    public static double conquerorsChance() {
        return value(CONQUERORS_CHANCE);
    }

    /** Parsed copy of {@link #CREATOR_UUIDS}; rebuilt whenever the config (re)loads. */
    private static volatile Set<UUID> creatorUuidCache = Set.of();

    private GLServerConfig() {}

    // ---------------------------------------------------------------------------------------------------------
    // Safe accessors. Server config only exists while a world is loaded; fall back to defaults otherwise.
    // ---------------------------------------------------------------------------------------------------------

    public static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    public static int moderatorOpLevel() {
        return isLoaded() ? MODERATOR_OP_LEVEL.get() : MODERATOR_OP_LEVEL.getDefault();
    }

    public static int adminOpLevel() {
        return isLoaded() ? ADMIN_OP_LEVEL.get() : ADMIN_OP_LEVEL.getDefault();
    }

    public static int creatorOpLevel() {
        return isLoaded() ? CREATOR_OP_LEVEL.get() : CREATOR_OP_LEVEL.getDefault();
    }

    public static boolean singleplayerOwnerIsCreator() {
        return isLoaded() ? SINGLEPLAYER_OWNER_IS_CREATOR.get() : SINGLEPLAYER_OWNER_IS_CREATOR.getDefault();
    }

    public static Set<UUID> creatorUuids() {
        return creatorUuidCache;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Events (mod bus)
    // ---------------------------------------------------------------------------------------------------------

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildCaches();
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        rebuildCaches();

        // Permission tiers may have changed: resend command trees so tab-complete matches the new tiers.
        // Reload fires on the config file-watcher thread, so hop onto the server thread first.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> server.getPlayerList().getPlayers()
                    .forEach(player -> server.getCommands().sendCommands(player)));
        }
    }

    private static void rebuildCaches() {
        Set<UUID> parsed = new HashSet<>();
        for (String raw : CREATOR_UUIDS.get()) {
            try {
                parsed.add(UUID.fromString(raw.trim()));
            } catch (IllegalArgumentException e) {
                GrandLine.LOGGER.warn("Ignoring invalid creator UUID in grandline-server.toml: '{}'", raw);
            }
        }
        creatorUuidCache = Set.copyOf(parsed);
        GrandLine.LOGGER.debug("Loaded {} creator UUID(s) from config", parsed.size());
    }

    private static boolean isValidUuid(Object value) {
        if (!(value instanceof String s)) {
            return false;
        }
        try {
            UUID.fromString(s.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
