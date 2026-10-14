package com.nerodev.grandline.worldevent;

import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.bounty.BountyHunts;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.Reaction;
import com.nerodev.grandline.ship.ShipEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Running world events: the raiders each one brought, its bar, and its end, in victory (every raider down) or when
 * the raiders give up. Events also start by themselves now and then for players out in the world.
 */
public final class WorldEvents {
    /** Tag carried by every raider; raiders left over from an event that no longer runs (after a restart) leave. */
    public static final String TAG = "grandline_event";
    /** How long raiders keep at it before they withdraw. */
    public static final int DURATION_TICKS = 6000;
    /** Players this close to a raid take part (see its bar, share the reward). */
    public static final double RANGE = 64;
    /** A player is not picked for a natural event again for this long. */
    private static final long COOLDOWN_TICKS = 24000;

    /** One event under way. */
    public static final class Active {
        private final UUID id = UUID.randomUUID();
        private final WorldEventType type;
        private final ServerLevel level;
        private final ServerPlayer target;
        private final List<UUID> raiders = new ArrayList<>();
        private final Set<UUID> fallen = new HashSet<>();
        private final ServerBossEvent bar;
        private final long started;
        private BlockPos centre;

        Active(WorldEventType type, ServerLevel level, ServerPlayer target, BlockPos centre) {
            this.type = type;
            this.level = level;
            this.target = target;
            this.centre = centre;
            this.started = level.getGameTime();
            this.bar = new ServerBossEvent(Component.literal(type.title()).withStyle(type.color()), type.barColor(),
                    BossEvent.BossBarOverlay.NOTCHED_6);
        }

        public UUID id() {
            return id;
        }

        public WorldEventType type() {
            return type;
        }

        public ServerBossEvent bar() {
            return bar;
        }

        /** Raiders still standing. */
        public List<LivingEntity> raiders() {
            List<LivingEntity> out = new ArrayList<>();
            for (UUID uuid : raiders) {
                if (level.getEntity(uuid) instanceof LivingEntity living && living.isAlive()) {
                    out.add(living);
                }
            }
            return out;
        }

        public int total() {
            return raiders.size();
        }

        /** Raiders brought down (as opposed to those who gave up the chase and left). */
        public int fallen() {
            return fallen.size();
        }
    }

    private static final List<Active> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Long> LAST_EVENT = new HashMap<>();

    private WorldEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, WorldEvents::onJoin);
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, event -> {
            if (event.getEntity().getTags().contains(TAG)) {
                for (Active active : ACTIVE) {
                    if (active.raiders.contains(event.getEntity().getUUID())) {
                        active.fallen.add(event.getEntity().getUUID());
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> LAST_EVENT.remove(event.getEntity().getUUID()));
        // Events belong to one server's run: none carry over into the next world opened in the same game.
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> {
            ACTIVE.forEach(active -> active.bar.removeAllPlayers());
            ACTIVE.clear();
            LAST_EVENT.clear();
        });
    }

    public static List<Active> active() {
        return List.copyOf(ACTIVE);
    }

    /** The event {@code player} is caught up in, if any. */
    @Nullable
    public static Active of(ServerPlayer player) {
        for (Active event : ACTIVE) {
            if (event.target.getUUID().equals(player.getUUID()) || event.bar.getPlayers().contains(player)) {
                return event;
            }
        }
        return null;
    }

    /**
     * Starts {@code type} against {@code player}: the band finds footing out of sight and comes for them.
     * @return the event, or null if the band could find no ground to land on
     */
    @Nullable
    public static Active start(ServerPlayer player, WorldEventType type) {
        ServerLevel level = player.serverLevel();
        if (type.boss() != null) {
            return surface(player, type);
        }
        BlockPos origin = BountyHunts.footing(level, player);
        if (origin == null) {
            return null;
        }
        Active event = new Active(type, level, player, origin);
        float yaw = (float) (Mth.atan2(player.getZ() - origin.getZ(), player.getX() - origin.getX()) * Mth.RAD_TO_DEG) - 90f;
        List<NpcEntity> band = new ArrayList<>();
        for (int i = 0; i < type.band().size(); i++) {
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.offset(i % 3 - 1, 0, i / 3 * 2 - 1));
            if (Math.abs(at.getY() - origin.getY()) > 2) {
                at = origin;
            }
            NpcEntity raider = NpcEntity.spawn(level, type.band().get(i), at, yaw, MobSpawnType.EVENT);
            if (raider != null) {
                raider.addTag(TAG);
                raider.hunt(player);
                event.raiders.add(raider.getUUID());
                band.add(raider);
            }
        }
        if (band.isEmpty()) {
            return null;
        }
        ACTIVE.add(event);
        LAST_EVENT.put(player.getUUID(), level.getGameTime());
        for (ServerPlayer near : level.getPlayers(p -> p.distanceToSqr(player) < RANGE * RANGE)) {
            near.sendSystemMessage(Component.literal(type.warning()).withStyle(type.color(), ChatFormatting.BOLD));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8f,
                type == WorldEventType.MARINE_RAID ? 1.2f : 0.9f);
        band.get(0).say(player, Reaction.HOSTILE);
        update(event);
        return event;
    }

    /** A Sea King surfaces in open water near the player. @return the event, or null if there is no deep water near */
    @Nullable
    private static Active surface(ServerPlayer player, WorldEventType type) {
        ServerLevel level = player.serverLevel();
        BlockPos spot = deepWater(level, player);
        if (spot == null) {
            return null;
        }
        float yaw = (float) (Mth.atan2(player.getZ() - spot.getZ(), player.getX() - spot.getX()) * Mth.RAD_TO_DEG) - 90f;
        BossEntity boss = BossEntity.spawn(level, type.boss(), spot, yaw, MobSpawnType.EVENT);
        if (boss == null) {
            return null;
        }
        Active event = new Active(type, level, player, spot);
        boss.addTag(TAG);
        boss.setTarget(player);
        event.raiders.add(boss.getUUID());
        ACTIVE.add(event);
        LAST_EVENT.put(player.getUUID(), level.getGameTime());
        for (ServerPlayer near : level.getPlayers(p -> p.distanceToSqr(player) < RANGE * RANGE)) {
            near.sendSystemMessage(Component.literal(type.warning()).withStyle(type.color(), ChatFormatting.BOLD));
        }
        level.playSound(null, spot.getX(), spot.getY(), spot.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5f, 0.5f);
        update(event);
        return event;
    }

    /** Open water at least four blocks deep, 14 to 22 blocks from the player, a few blocks under the surface. */
    @Nullable
    public static BlockPos deepWater(ServerLevel level, ServerPlayer player) {
        for (int tries = 0; tries < 32; tries++) {
            double angle = player.getRandom().nextDouble() * Mth.TWO_PI;
            double distance = 14 + player.getRandom().nextDouble() * 8;
            BlockPos probe = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, probe);
            BlockPos floor = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, probe);
            if (surface.getY() - floor.getY() >= 4 && level.getFluidState(surface.below()).is(FluidTags.WATER)) {
                return new BlockPos(probe.getX(), Math.max(floor.getY(), surface.getY() - 4), probe.getZ());
            }
        }
        return null;
    }

    /** Ends {@code event}: in victory every player taking part is paid; otherwise the raiders still standing leave. */
    public static void finish(Active event, boolean victory) {
        ACTIVE.remove(event);
        List<ServerPlayer> party = new ArrayList<>(event.bar.getPlayers());
        event.bar.removeAllPlayers();
        if (victory) {
            for (ServerPlayer player : party) {
                ItemStack pay = new ItemStack(Items.EMERALD, event.type.emeralds());
                if (!player.getInventory().add(pay)) {
                    player.drop(pay, false);
                }
                if (event.type.renown() > 0) {
                    Crews.addRenown(player, event.type.renown());
                }
                player.sendSystemMessage(Component.literal(event.type.title() + " beaten! ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        .append(Component.literal("+" + event.type.emeralds() + " emeralds").withStyle(ChatFormatting.WHITE)));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                        0.8f, 1.2f);
            }
        } else {
            for (LivingEntity raider : event.raiders()) {
                raider.discard();
            }
            for (ServerPlayer player : party) {
                player.sendSystemMessage(Component.literal("The " + event.type.title().toLowerCase(Locale.ROOT) + " is over: the raiders "
                        + "have withdrawn.").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** Ends every event (creators' stop button). @return how many there were */
    public static int stopAll() {
        List<Active> all = List.copyOf(ACTIVE);
        all.forEach(event -> finish(event, false));
        return all.size();
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 20 == 0) {
            for (Active event : List.copyOf(ACTIVE)) {
                update(event);
            }
        }
        if (now % 1200 == 0 && GLServerConfig.worldEvents()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                maybeStart(player, now);
            }
        }
    }

    private static void update(Active event) {
        List<LivingEntity> left = event.raiders();
        if (left.isEmpty()) {
            // Beaten if the raiders were brought down; if they all lost the trail and left, it simply ends.
            finish(event, event.fallen() > 0);
            return;
        }
        if (event.level.getGameTime() - event.started > DURATION_TICKS) {
            finish(event, false);
            return;
        }
        event.centre = left.get(0).blockPosition();
        event.bar.setProgress(Mth.clamp(left.size() / (float) Math.max(1, event.total()), 0f, 1f));
        event.bar.setName(Component.literal(event.type.title() + " - " + left.size() + " left").withStyle(event.type.color()));
        for (ServerPlayer player : List.copyOf(event.bar.getPlayers())) {
            if (!player.isAlive() || player.level() != event.level || player.distanceToSqr(event.centre.getX(), event.centre.getY(),
                    event.centre.getZ()) > RANGE * RANGE * 2) {
                event.bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : event.level.getPlayers(p -> p.distanceToSqr(event.centre.getX(), event.centre.getY(), event.centre.getZ())
                < RANGE * RANGE)) {
            event.bar.addPlayer(player);
        }
        if (!event.target.hasDisconnected() && event.target.isAlive() && event.target.level() == event.level) {
            event.bar.addPlayer(event.target);
        }
    }

    /** Now and then an event befalls a player who is out and about (not one already under way, not a creative builder). */
    private static void maybeStart(ServerPlayer player, long now) {
        if (player.isCreative() || player.isSpectator() || of(player) != null
                || LAST_EVENT.getOrDefault(player.getUUID(), -COOLDOWN_TICKS) + COOLDOWN_TICKS > now
                || player.getRandom().nextDouble() >= GLServerConfig.worldEventChance()) {
            return;
        }
        if (player.isInWater() || player.getVehicle() instanceof ShipEntity) {
            if (player.getRandom().nextFloat() < 0.5f) {
                start(player, WorldEventType.SEA_KING);
            }
            return;
        }
        long bounty = GLPlayer.data(player).bounty();
        WorldEventType type = bounty > 0 && player.getRandom().nextFloat() < 0.4f ? WorldEventType.MARINE_RAID
                : player.getRandom().nextFloat() < 0.25f ? WorldEventType.FISHMAN_RAID : WorldEventType.PIRATE_RAID;
        start(player, type);
    }

    /** Raiders saved with the world outlive their event; when they load again with no event running, they leave. */
    private static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getTags().contains(TAG)) {
            return;
        }
        for (Active active : ACTIVE) {
            if (active.raiders.contains(entity.getUUID())) {
                return;
            }
        }
        event.setCanceled(true);
    }
}
