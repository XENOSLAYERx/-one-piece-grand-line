package com.nerodev.grandline.arena;

import com.nerodev.grandline.creator.Battles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Arena fights: players who join an arena face round after round of enemies, each round harder (pirates, gunners,
 * Fish-Men and bounty hunters, then a boss every round from the ninth), with a short breather and a purse of
 * emeralds between rounds. A fight lasts while anyone is still standing inside the arena.
 */
public final class Arenas {
    /** Tag carried by every arena enemy; those left over after a restart leave. */
    public static final String TAG = "grandline_arena";
    /** Ticks between one round being cleared and the next beginning. */
    public static final int BREATHER_TICKS = 100;
    /** How far outside its edge a fighter may stray before they have left the arena. */
    private static final double MARGIN = 8;

    /** One arena's fight under way. */
    public static final class Session {
        private final Arena arena;
        private final ServerLevel level;
        private final Map<UUID, ServerPlayer> players = new LinkedHashMap<>();
        private final List<UUID> enemies = new ArrayList<>();
        private final ServerBossEvent bar;
        private int round;
        private int countdown = BREATHER_TICKS;

        Session(Arena arena, ServerLevel level) {
            this.arena = arena;
            this.level = level;
            this.bar = new ServerBossEvent(Component.literal("Arena " + arena.name()), BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.NOTCHED_10);
        }

        public Arena arena() {
            return arena;
        }

        public int round() {
            return round;
        }

        public boolean fighting() {
            return countdown <= 0;
        }

        public Set<UUID> players() {
            return Set.copyOf(players.keySet());
        }

        public ServerBossEvent bar() {
            return bar;
        }

        /** Enemies of this round still standing. */
        public List<LivingEntity> enemies() {
            List<LivingEntity> out = new ArrayList<>();
            for (UUID uuid : enemies) {
                if (level.getEntity(uuid) instanceof LivingEntity living && living.isAlive()) {
                    out.add(living);
                }
            }
            return out;
        }
    }

    private static final Map<String, Session> SESSIONS = new HashMap<>();

    private Arenas() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, Arenas::onJoin);
        // Fights belong to one server's run: none carry over into the next world opened in the same game.
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> {
            SESSIONS.values().forEach(session -> session.bar.removeAllPlayers());
            SESSIONS.clear();
        });
    }

    @Nullable
    public static Session session(String arena) {
        return SESSIONS.get(arena.toLowerCase(Locale.ROOT));
    }

    /** The fight {@code player} is in, if any. */
    @Nullable
    public static Session of(ServerPlayer player) {
        for (Session session : SESSIONS.values()) {
            if (session.players.containsKey(player.getUUID())) {
                return session;
            }
        }
        return null;
    }

    /** Marks out an arena of {@code radius} around {@code centre}. @return false if the name is taken */
    public static boolean create(MinecraftServer server, String name, ServerLevel level, BlockPos centre, int radius) {
        ArenaData data = ArenaData.get(server);
        if (data.byName(name).isPresent()) {
            return false;
        }
        data.put(new Arena(name, level.dimension(), centre, radius));
        return true;
    }

    /** Removes an arena, ending any fight in it. */
    public static boolean delete(MinecraftServer server, String name) {
        reset(name);
        return ArenaData.get(server).remove(name);
    }

    /** {@code player} steps into {@code arena}: brought to its edge, with the next round starting shortly. */
    public static String join(ServerPlayer player, Arena arena) {
        MinecraftServer server = player.getServer();
        ServerLevel level = server == null ? null : server.getLevel(arena.dimension());
        if (level == null) {
            return "That arena's world isn't loaded";
        }
        Session current = of(player);
        if (current != null && current.arena.name().equalsIgnoreCase(arena.name())) {
            return "You are already in the arena";
        }
        if (current != null) {
            leave(player);
        }
        Session session = SESSIONS.computeIfAbsent(arena.name().toLowerCase(Locale.ROOT), key -> new Session(arena, level));
        session.players.put(player.getUUID(), player);
        session.bar.addPlayer(player);
        // Stand fighters a few blocks in from the edge, facing the middle.
        double angle = player.getRandom().nextDouble() * Mth.TWO_PI;
        double r = Math.max(2, arena.radius() - 4);
        int x = Mth.floor(arena.centre().getX() + 0.5 + Math.cos(angle) * r);
        int z = Mth.floor(arena.centre().getZ() + 0.5 + Math.sin(angle) * r);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (Math.abs(ground.getY() - arena.centre().getY()) > 12) {
            ground = new BlockPos(x, arena.centre().getY(), z);
        }
        float yaw = (float) (Mth.atan2(arena.centre().getZ() - z, arena.centre().getX() - x) * Mth.RAD_TO_DEG) - 90f;
        player.teleportTo(level, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, yaw, 0f);
        player.sendSystemMessage(Component.literal("You enter the arena " + arena.name() + ". ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(session.fighting() ? "A round is under way!" : "Round " + (session.round + 1) + " begins shortly.")
                        .withStyle(ChatFormatting.YELLOW)));
        return null;
    }

    /** {@code player} leaves the fight they are in. */
    public static void leave(ServerPlayer player) {
        Session session = of(player);
        if (session == null) {
            return;
        }
        session.players.remove(player.getUUID());
        session.bar.removePlayer(player);
        player.sendSystemMessage(Component.literal("You leave the arena after round " + session.round + ".").withStyle(ChatFormatting.GRAY));
        if (session.players.isEmpty()) {
            end(session);
        }
    }

    /** Ends the fight in an arena and clears its enemies. @return whether one was under way */
    public static boolean reset(String arena) {
        Session session = session(arena);
        if (session == null) {
            return false;
        }
        end(session);
        return true;
    }

    private static void end(Session session) {
        SESSIONS.values().remove(session);
        for (LivingEntity enemy : session.enemies()) {
            enemy.discard();
        }
        session.bar.removeAllPlayers();
    }

    private static void tick(MinecraftServer server) {
        for (Session session : List.copyOf(SESSIONS.values())) {
            tick(server, session);
        }
    }

    private static void tick(MinecraftServer server, Session session) {
        for (ServerPlayer player : List.copyOf(session.players.values())) {
            if (player.hasDisconnected() || !player.isAlive() || !session.arena.contains(player.level(), player.getX(), player.getZ(), MARGIN)) {
                session.players.remove(player.getUUID());
                session.bar.removePlayer(player);
                if (!player.hasDisconnected()) {
                    player.sendSystemMessage(Component.literal(player.isAlive() ? "You left the arena." : "You fell in round " + session.round
                            + ".").withStyle(ChatFormatting.GRAY));
                }
            }
        }
        if (session.players.isEmpty()) {
            end(session);
            return;
        }
        if (session.countdown > 0) {
            session.countdown--;
            session.bar.setName(Component.literal("Arena " + session.arena.name() + ": round " + (session.round + 1) + " in "
                    + (session.countdown + 19) / 20 + "s").withStyle(ChatFormatting.GOLD));
            session.bar.setProgress(1f - session.countdown / (float) BREATHER_TICKS);
            if (session.countdown == 0) {
                startRound(server, session);
            }
            return;
        }
        if (server.overworld().getGameTime() % 10 != 0) {
            return;
        }
        List<LivingEntity> left = session.enemies();
        session.bar.setName(Component.literal("Arena " + session.arena.name() + ": round " + session.round + " - " + left.size() + " left")
                .withStyle(ChatFormatting.RED));
        session.bar.setProgress(Mth.clamp(left.size() / (float) Math.max(1, session.enemies.size()), 0f, 1f));
        if (left.isEmpty()) {
            roundCleared(server, session);
        }
    }

    /** Sends in the next round: harder every two rounds, with a boss from the ninth. */
    public static void startRound(MinecraftServer server, Session session) {
        session.round++;
        session.countdown = 0;
        session.enemies.clear();
        List<ServerPlayer> fighters = new ArrayList<>(session.players.values());
        Battles.Difficulty difficulty = difficulty(session.round);
        for (LivingEntity enemy : Battles.spawnWaveAt(session.level, session.arena.centre(), Math.max(3, session.arena.radius() * 0.7),
                difficulty, fighters)) {
            enemy.addTag(TAG);
            session.enemies.add(enemy.getUUID());
        }
        for (ServerPlayer player : fighters) {
            player.displayClientMessage(Component.literal("Round " + session.round + "!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE,
                    0.6f, 1.4f);
        }
    }

    private static void roundCleared(MinecraftServer server, Session session) {
        int purse = Math.min(16, session.round);
        for (ServerPlayer player : session.players.values()) {
            ItemStack pay = new ItemStack(Items.EMERALD, purse);
            if (!player.getInventory().add(pay)) {
                player.drop(pay, false);
            }
            player.sendSystemMessage(Component.literal("Round " + session.round + " cleared! ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(Component.literal("+" + purse + " emeralds").withStyle(ChatFormatting.WHITE)));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f,
                    1.3f);
        }
        session.enemies.clear();
        session.countdown = BREATHER_TICKS;
    }

    /** How hard round {@code round} is. */
    public static Battles.Difficulty difficulty(int round) {
        Battles.Difficulty[] all = Battles.Difficulty.values();
        return all[Math.min(all.length - 1, Math.max(0, (round - 1) / 2))];
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getTags().contains(TAG)) {
            return;
        }
        for (Session session : SESSIONS.values()) {
            if (session.enemies.contains(entity.getUUID())) {
                return;
            }
        }
        event.setCanceled(true);
    }
}
