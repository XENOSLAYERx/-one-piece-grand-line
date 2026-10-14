package com.nerodev.grandline.world.island;

import com.mojang.serialization.Codec;
import com.nerodev.grandline.GrandLine;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * The islands of the East Blue that can rise out of the sea, each with its own shape, plants and settlement:
 * Dawn Island's windmill village, Shells Town under its Marine base, and the Conomi Islands with Arlong Park and
 * the tangerine groves of Cocoyasi, Orange Town under Buggy's circus tent, and Syrup Village below Kaya's mansion.
 */
public enum IslandType implements StringRepresentable {
    DAWN_ISLAND(46, 16, Flora.OAK, List.of(
            Site.of(Site.Kind.WINDMILL, 8, -15, 7, 7),
            Site.of(Site.Kind.BAR, -6, 6, 11, 8),
            Site.of(Site.Kind.HOUSE, -17, -3, 7, 6),
            Site.of(Site.Kind.HOUSE, -15, 16, 7, 7),
            Site.of(Site.Kind.HOUSE, 6, 16, 6, 6),
            Site.of(Site.Kind.HOUSE, 13, 6, 7, 6),
            Site.of(Site.Kind.HOUSE, 15, -4, 6, 7)), 90, 225),
    SHELLS_TOWN(44, 10, Flora.OAK, List.of(
            Site.of(Site.Kind.MARINE_BASE, 0, -14, 23, 23),
            Site.of(Site.Kind.TOWN_HOUSE, -15, 7, 7, 6),
            Site.of(Site.Kind.TOWN_HOUSE, -5, 10, 6, 6),
            Site.of(Site.Kind.TOWN_HOUSE, 5, 10, 6, 6),
            Site.of(Site.Kind.TOWN_HOUSE, 15, 7, 6, 7),
            Site.of(Site.Kind.TOWN_HOUSE, -9, 20, 6, 6),
            Site.of(Site.Kind.TOWN_HOUSE, 9, 20, 7, 6)), 90, -1),
    CONOMI_ISLANDS(52, 14, Flora.PALM, List.of(
            Site.of(Site.Kind.ARLONG_PARK, 17, -10, 21, 21),
            Site.of(Site.Kind.VILLA, -14, 5, 6, 6),
            Site.of(Site.Kind.VILLA, -5, 14, 6, 6),
            Site.of(Site.Kind.VILLA, -19, -7, 6, 7),
            Site.of(Site.Kind.VILLA, 3, 19, 6, 6),
            Site.of(Site.Kind.HOUSE, -24, 15, 6, 6),
            Site.of(Site.Kind.GROVE, -15, 25, 11, 8)), 180, 250),
    ORANGE_TOWN(44, 9, Flora.OAK, List.of(
            Site.of(Site.Kind.BUGGY_HQ, 0, -12, 15, 13),
            Site.of(Site.Kind.TOWN_HOUSE, -15, 7, 7, 6),
            Site.of(Site.Kind.TOWN_HOUSE, -5, 10, 6, 6),
            Site.of(Site.Kind.TOWN_HOUSE, 5, 10, 6, 6),
            Site.of(Site.Kind.TOWN_HOUSE, 15, 7, 6, 7),
            Site.of(Site.Kind.TOWN_HOUSE, -9, 20, 6, 6),
            Site.of(Site.Kind.TOWN_HOUSE, 9, 20, 7, 6)), 270, 135),
    SYRUP_VILLAGE(48, 18, Flora.OAK, List.of(
            Site.of(Site.Kind.MANSION, -4, -17, 17, 13),
            Site.of(Site.Kind.HOUSE, -18, 4, 7, 6),
            Site.of(Site.Kind.HOUSE, -7, 12, 7, 6),
            Site.of(Site.Kind.HOUSE, 6, 12, 6, 6),
            Site.of(Site.Kind.HOUSE, 17, 3, 6, 7),
            Site.of(Site.Kind.HOUSE, 13, 22, 6, 6)), 90, 300);

    public static final Codec<IslandType> CODEC = StringRepresentable.fromEnum(IslandType::values);

    /** The plants that grow wild on an island. */
    public enum Flora {
        OAK, PALM
    }

    private final int radius;
    private final int peak;
    private final Flora flora;
    private final List<Site> sites;
    private final int pierAngle;
    private final int treasureAngle;

    /**
     * @param radius        rough radius of the island in blocks
     * @param peak          height of its highest hill above the sea
     * @param sites         buildings, as offsets from the centre before the island is turned
     * @param pierAngle     compass direction of the pier (degrees, 0 = east, 90 = south)
     * @param treasureAngle direction of the buried treasure on the beach, or -1 for none
     */
    IslandType(int radius, int peak, Flora flora, List<Site> sites, int pierAngle, int treasureAngle) {
        this.radius = radius;
        this.peak = peak;
        this.flora = flora;
        this.sites = sites;
        this.pierAngle = pierAngle;
        this.treasureAngle = treasureAngle;
    }

    public ResourceLocation id() {
        return GrandLine.id(getSerializedName());
    }

    public Component displayName() {
        return Component.translatable("island.grandline." + getSerializedName());
    }

    public int radius() {
        return radius;
    }

    public int peak() {
        return peak;
    }

    public Flora flora() {
        return flora;
    }

    public List<Site> sites() {
        return sites;
    }

    public int pierAngle() {
        return pierAngle;
    }

    public int treasureAngle() {
        return treasureAngle;
    }

    /** How far the island (and its shelf, pier and buildings) can reach from its centre. */
    public int reach() {
        return radius + 20;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static IslandType byName(String name) {
        for (IslandType type : values()) {
            if (type.getSerializedName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static IslandType byId(ResourceLocation id) {
        for (IslandType type : values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return null;
    }

    /** A building on an island: what it is and where, relative to the centre (before turning). */
    public record Site(Kind kind, int x, int z, int width, int depth) {
        public enum Kind {
            HOUSE, BAR, WINDMILL, TOWN_HOUSE, MARINE_BASE, VILLA, ARLONG_PARK, GROVE, BUGGY_HQ, MANSION
        }

        static Site of(Kind kind, int x, int z, int width, int depth) {
            return new Site(kind, x, z, width, depth);
        }
    }
}
