package com.nerodev.grandline.bounty;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.Reaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wanted players are hunted. Every few minutes (sooner the bigger the bounty) a party of bounty hunters or a Marine
 * squad finds footing out of sight around a wanted player and comes after them. The bigger the bounty, the bigger
 * and stronger the party. Hunters give up when the trail goes cold.
 */
public final class BountyHunts {
    /** A band of hunters for bounties from {@code bounty} up. */
    public record Tier(long bounty, List<ResourceLocation> hunters, List<ResourceLocation> marines) {}

    private static final ResourceLocation HUNTER = GrandLine.id("bounty_hunter");
    private static final ResourceLocation VETERAN = GrandLine.id("bounty_hunter_veteran");
    private static final ResourceLocation RECRUIT = GrandLine.id("marine_recruit");
    private static final ResourceLocation RIFLEMAN = GrandLine.id("marine_rifleman");
    private static final ResourceLocation LIEUTENANT = GrandLine.id("marine_lieutenant");
    private static final ResourceLocation CAPTAIN = GrandLine.id("marine_captain");

    public static final List<Tier> TIERS = List.of(
            new Tier(1_000_000L, List.of(HUNTER), List.of(RECRUIT, RECRUIT)),
            new Tier(10_000_000L, List.of(HUNTER, HUNTER), List.of(RECRUIT, RIFLEMAN, RECRUIT)),
            new Tier(50_000_000L, List.of(VETERAN, HUNTER, HUNTER), List.of(LIEUTENANT, RIFLEMAN, RECRUIT, RECRUIT)),
            new Tier(200_000_000L, List.of(VETERAN, VETERAN, HUNTER, HUNTER), List.of(CAPTAIN, LIEUTENANT, RIFLEMAN, RIFLEMAN)));

    /** How often the hunt schedule is checked, in ticks. */
    private static final int CHECK_TICKS = 200;
    private static final Map<UUID, Long> NEXT_HUNT = new HashMap<>();

    private BountyHunts() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BountyHunts::onServerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> NEXT_HUNT.remove(event.getEntity().getUUID()));
    }

    /** The strongest band a bounty draws, or null if it is too small to be hunted. */
    @Nullable
    public static Tier tier(long bounty) {
        if (bounty < GLServerConfig.hunterMinBounty()) {
            return null;
        }
        Tier best = null;
        for (Tier tier : TIERS) {
            if (bounty >= tier.bounty()) {
                best = tier;
            }
        }
        return best == null ? TIERS.get(0) : best;
    }

    /** Ticks until the next hunt: the configured average, shorter for bigger bounties, give or take a quarter. */
    public static long interval(long bounty, RandomSource random) {
        double factor = bounty >= 100_000_000L ? 0.5 : bounty >= 10_000_000L ? 0.75 : 1.0;
        return (long) (GLServerConfig.huntIntervalTicks() * factor * (0.75 + random.nextDouble() * 0.5));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_TICKS != 0 || !GLServerConfig.bountyHunters()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long bounty = GLPlayer.data(player).bounty();
            if (tier(bounty) == null || player.isCreative() || player.isSpectator() || !player.isAlive()
                    || player.serverLevel().getDifficulty() == Difficulty.PEACEFUL) {
                NEXT_HUNT.remove(player.getUUID());
                continue;
            }
            long now = player.serverLevel().getGameTime();
            Long next = NEXT_HUNT.get(player.getUUID());
            if (next == null) {
                NEXT_HUNT.put(player.getUUID(), now + interval(bounty, player.getRandom()));
            } else if (now >= next) {
                NEXT_HUNT.put(player.getUUID(), now + interval(bounty, player.getRandom()));
                hunt(player);
            }
        }
    }

    /**
     * Sends a band after {@code player} at once (Marines are likelier to come for pirates).
     *
     * @return the hunters who found footing (none if the player is far out at sea or the bounty is too small)
     */
    public static List<NpcEntity> hunt(ServerPlayer player) {
        long bounty = GLPlayer.data(player).bounty();
        Tier tier = tier(bounty);
        List<NpcEntity> party = new ArrayList<>();
        if (tier == null) {
            return party;
        }
        ServerLevel level = player.serverLevel();
        boolean pirate = GLPlayer.data(player).faction().map(GrandLine.id("pirate")::equals).orElse(false);
        boolean marines = player.getRandom().nextFloat() < (pirate ? 0.65f : 0.4f);
        BlockPos origin = footing(level, player);
        if (origin == null) {
            return party;
        }
        float yaw = (float) (Mth.atan2(player.getZ() - origin.getZ(), player.getX() - origin.getX()) * Mth.RAD_TO_DEG) - 90f;
        List<ResourceLocation> band = marines ? tier.marines() : tier.hunters();
        for (int i = 0; i < band.size(); i++) {
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.offset(i % 3 - 1, 0, i / 3 * 2 - 1));
            if (Math.abs(at.getY() - origin.getY()) > 2) {
                at = origin;
            }
            NpcEntity hunter = NpcEntity.spawn(level, band.get(i), at, yaw, MobSpawnType.EVENT);
            if (hunter != null) {
                hunter.hunt(player);
                party.add(hunter);
            }
        }
        if (!party.isEmpty()) {
            player.displayClientMessage(Component.literal(marines ? "A Marine squad has come to arrest you!" : "Bounty hunters are on your trail!")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.5f, 1.3f);
            party.get(0).say(player, Reaction.HOSTILE);
        }
        return party;
    }

    /** Solid, dry ground 20 to 30 blocks from the player, near their height, in a loaded chunk. */
    @Nullable
    public static BlockPos footing(ServerLevel level, ServerPlayer player) {
        RandomSource random = player.getRandom();
        for (int tries = 0; tries < 24; tries++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = 20 + random.nextDouble() * 10;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos probe = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            BlockPos below = top.below();
            if (Math.abs(top.getY() - player.getBlockY()) > 12 || !level.getFluidState(below).isEmpty() || !level.getFluidState(top).isEmpty()
                    || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    || !level.getBlockState(top.above()).isAir()) {
                continue;
            }
            return top;
        }
        return null;
    }
}
