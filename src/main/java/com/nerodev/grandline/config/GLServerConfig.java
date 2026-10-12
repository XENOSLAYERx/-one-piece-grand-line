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
        SPEC = BUILDER.build();
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
