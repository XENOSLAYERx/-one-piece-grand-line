package com.nerodev.grandline.worldevent;

import com.nerodev.grandline.GrandLine;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;

/**
 * The world events that can befall a player: a band of fighters that lands nearby and comes for them (or, at sea, a
 * Sea King that surfaces), with an event bar counting down those left. Beat them all for a reward; hold out and they withdraw when the time runs out.
 */
public enum WorldEventType implements StringRepresentable {
    PIRATE_RAID("Pirate Raid", "Pirates have come ashore to raid!", ChatFormatting.RED, BossEvent.BossBarColor.RED, 6, 10, null,
            "pirate_captain", "pirate_grunt", "pirate_grunt", "pirate_grunt", "pirate_gunner", "pirate_gunner"),
    MARINE_RAID("Marine Raid", "A Marine raiding party has landed!", ChatFormatting.BLUE, BossEvent.BossBarColor.BLUE, 8, 15, null,
            "marine_lieutenant", "marine_recruit", "marine_recruit", "marine_recruit", "marine_rifleman", "marine_rifleman"),
    FISHMAN_RAID("Fish-Man Raid", "Fish-Man pirates are coming up out of the sea!", ChatFormatting.DARK_AQUA, BossEvent.BossBarColor.BLUE,
            10, 20, null, "fishman_pirate", "fishman_pirate", "fishman_pirate", "fishman_pirate"),
    BOUNTY_HUNTERS("Bounty Hunters", "A band of bounty hunters has picked up your trail!", ChatFormatting.GOLD, BossEvent.BossBarColor.YELLOW,
            8, 15, null, "bounty_hunter_veteran", "bounty_hunter", "bounty_hunter", "bounty_hunter"),
    /** At sea only: a Sea King surfaces beside the ship. */
    SEA_KING("Sea King", "The sea heaves: a Sea King is rising!", ChatFormatting.DARK_PURPLE, BossEvent.BossBarColor.PURPLE, 20, 60,
            "lord_of_the_coast");

    private final String title;
    private final String warning;
    private final ChatFormatting color;
    private final BossEvent.BossBarColor bar;
    private final int emeralds;
    private final long renown;
    private final List<ResourceLocation> band;
    @Nullable
    private final ResourceLocation boss;

    WorldEventType(String title, String warning, ChatFormatting color, BossEvent.BossBarColor bar, int emeralds, long renown, @Nullable String boss,
            String... band) {
        this.title = title;
        this.warning = warning;
        this.color = color;
        this.bar = bar;
        this.emeralds = emeralds;
        this.renown = renown;
        this.band = Arrays.stream(band).map(GrandLine::id).toList();
        this.boss = boss == null ? null : GrandLine.id(boss);
    }

    /** The boss that comes instead of a band (a Sea King), or null. */
    @Nullable
    public ResourceLocation boss() {
        return boss;
    }

    /** Whether the event happens out at sea rather than ashore. */
    public boolean atSea() {
        return boss != null;
    }

    public String title() {
        return title;
    }

    public String warning() {
        return warning;
    }

    public ChatFormatting color() {
        return color;
    }

    public BossEvent.BossBarColor barColor() {
        return bar;
    }

    /** Emeralds paid to each player who sees the event through. */
    public int emeralds() {
        return emeralds;
    }

    public long renown() {
        return renown;
    }

    /** The islanders who come. */
    public List<ResourceLocation> band() {
        return band;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WorldEventType byName(String name) {
        for (WorldEventType type : values()) {
            if (type.getSerializedName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
