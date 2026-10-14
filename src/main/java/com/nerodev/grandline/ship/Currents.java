package com.nerodev.grandline.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ocean currents: over the open ocean the water sets in a direction that drifts slowly from region to region (a
 * smooth field over cells a few hundred blocks wide, the same in every world and on both sides), at up to about a
 * seventh of a ship's top speed. A ship under way is carried by it; one lying at anchor is not.
 */
public final class Currents {
    /** Width of the cells the field is woven from, in blocks. */
    static final int CELL = 384;
    static final double MIN_STRENGTH = 0.015;
    static final double MAX_STRENGTH = 0.04;

    private static final String[] POINTS = {"east", "north-east", "north", "north-west", "west", "south-west", "south", "south-east"};

    private Currents() {}

    /** The current at {@code pos} in blocks per tick: nothing outside ocean biomes. */
    public static Vec3 at(Level level, BlockPos pos) {
        return level.getBiome(pos).is(BiomeTags.IS_OCEAN) ? field(pos.getX() + 0.5, pos.getZ() + 0.5) : Vec3.ZERO;
    }

    /** The open-ocean current at a point, blended smoothly between the cells around it. */
    public static Vec3 field(double x, double z) {
        double gx = x / CELL;
        double gz = z / CELL;
        int x0 = Mth.floor(gx);
        int z0 = Mth.floor(gz);
        double fx = smooth(gx - x0);
        double fz = smooth(gz - z0);
        Vec3 north = corner(x0, z0).lerp(corner(x0 + 1, z0), fx);
        Vec3 south = corner(x0, z0 + 1).lerp(corner(x0 + 1, z0 + 1), fx);
        return north.lerp(south, fz);
    }

    /** "setting north-east": the way the water flows. */
    public static String setting(Vec3 current) {
        double degrees = Math.toDegrees(Math.atan2(-current.z, current.x));
        int point = Math.floorMod(Math.round((float) (degrees / 45)), POINTS.length);
        return POINTS[point];
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /** The current fixed at a cell corner: a hashed direction and strength. */
    private static Vec3 corner(int x, int z) {
        long h = x * 341873128712L + z * 132897987541L;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        double angle = (h & 0xFFFF) / 65536.0 * Mth.TWO_PI;
        double strength = MIN_STRENGTH + ((h >>> 16) & 0xFFFF) / 65536.0 * (MAX_STRENGTH - MIN_STRENGTH);
        return new Vec3(Math.cos(angle) * strength, 0, Math.sin(angle) * strength);
    }
}
