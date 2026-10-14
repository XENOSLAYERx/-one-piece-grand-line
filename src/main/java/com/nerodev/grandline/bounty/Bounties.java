package com.nerodev.grandline.bounty;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcEntity;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bounties: the World Government's price on a player's head. Crimes against the Marines and the townsfolk raise it,
 * the Marines arrest anyone wanted, and the higher it climbs the stronger the hunters who come for it
 * ({@link BountyHunts}). Marines earn no bounty for doing their job, only for turning on their own or the people.
 */
public final class Bounties {
    /** A notorious pirate's bounty is announced to every player when it crosses one of these. */
    public static final long[] MILESTONES = {10_000_000L, 50_000_000L, 100_000_000L, 300_000_000L, 500_000_000L, 1_000_000_000L,
            3_000_000_000L};

    public static final long MARINE_KILL = 3_000_000L;
    public static final long MARINE_ASSAULT = 150_000L;
    public static final long CIVILIAN_KILL = 5_000_000L;
    public static final long CIVILIAN_ASSAULT = 300_000L;
    public static final long HUNTER_KILL = 1_000_000L;
    public static final long MARINE_SHIP_SUNK = 8_000_000L;
    /** Share of a wanted player's bounty a pirate who kills them adds to their own. */
    public static final double PLAYER_KILL_SHARE = 0.25;
    /** Emeralds a lawful player is paid per this much of a wanted player's bounty when they bring them down. */
    public static final long REWARD_PER_EMERALD = 1_000_000L;

    private static final ResourceLocation BOUNTY_HUNTERS = GrandLine.id("bounty_hunter");

    private Bounties() {}

    /** Formats a bounty the way a wanted poster does: {@code ฿30,000,000}. */
    public static String format(long bounty) {
        return "฿" + String.format(Locale.ROOT, "%,d", bounty);
    }

    /**
     * Raises {@code player}'s bounty for a crime.
     *
     * @param treason crimes the Marines punish even in their own ranks (killing Marines or civilians)
     * @return the bounty actually added
     */
    public static long crime(ServerPlayer player, long amount, boolean treason, String what) {
        PlayerData data = GLPlayer.data(player);
        boolean marine = data.faction().map(Factions.MARINES::equals).orElse(false);
        long added = (long) (amount * GLServerConfig.bountyGainMultiplier());
        if (added <= 0 || marine && !treason) {
            return 0;
        }
        long before = data.bounty();
        GLPlayer.edit(player, d -> d.addBounty(added));
        long after = GLPlayer.data(player).bounty();
        if (after <= before) {
            return 0;
        }
        player.displayClientMessage(Component.literal("Bounty raised for " + what + ": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(format(after)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.6f, 0.7f);
        for (long milestone : MILESTONES) {
            if (before < milestone && after >= milestone && player.getServer() != null) {
                Component news = Component.literal("WANTED: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" - dead or alive - " + format(after)).withStyle(ChatFormatting.GOLD));
                player.getServer().getPlayerList().broadcastSystemMessage(news, false);
                break;
            }
        }
        return after - before;
    }

    /** {@code player} killed an islander: Marines, townsfolk and bounty hunters are crimes in the World Government's eyes. */
    public static void onKill(ServerPlayer player, NpcEntity npc) {
        Optional<ResourceLocation> faction = npc.faction();
        float rank = Math.max(1f, npc.getMaxHealth() / 24f);
        if (faction.isEmpty()) {
            crime(player, CIVILIAN_KILL, true, "murdering a civilian");
        } else if (faction.get().equals(Factions.MARINES)) {
            crime(player, (long) (MARINE_KILL * rank), true, "killing a Marine");
        } else if (faction.get().equals(BOUNTY_HUNTERS)) {
            crime(player, (long) (HUNTER_KILL * rank), false, "killing a bounty hunter");
        }
    }

    /** {@code player} struck an islander who meant them no harm. */
    public static void onAssault(ServerPlayer player, NpcEntity npc) {
        Optional<ResourceLocation> faction = npc.faction();
        if (faction.isEmpty()) {
            crime(player, CIVILIAN_ASSAULT, false, "attacking a civilian");
        } else if (faction.get().equals(Factions.MARINES)) {
            crime(player, MARINE_ASSAULT, false, "attacking a Marine");
        }
    }

    /**
     * {@code killer} brought down {@code victim}. A pirate who kills a wanted player takes on part of their
     * notoriety; a Marine or bounty hunter collects the reward in emeralds.
     */
    public static void onPlayerKill(ServerPlayer killer, ServerPlayer victim) {
        long wanted = GLPlayer.data(victim).bounty();
        if (wanted <= 0 || killer == victim) {
            return;
        }
        Optional<ResourceLocation> faction = GLPlayer.data(killer).faction();
        boolean lawful = faction.map(f -> f.equals(Factions.MARINES) || f.equals(BOUNTY_HUNTERS)).orElse(false);
        if (lawful) {
            int emeralds = (int) Math.min(64, Math.max(1, wanted / REWARD_PER_EMERALD));
            ItemStack reward = new ItemStack(Items.EMERALD, emeralds);
            if (!killer.getInventory().add(reward)) {
                killer.drop(reward, false);
            }
            killer.displayClientMessage(Component.literal("Bounty collected on " + victim.getGameProfile().getName() + ": " + emeralds
                    + " emeralds").withStyle(ChatFormatting.GREEN), true);
        } else {
            crime(killer, (long) (wanted * PLAYER_KILL_SHARE), false, "defeating " + victim.getGameProfile().getName());
        }
    }
}
