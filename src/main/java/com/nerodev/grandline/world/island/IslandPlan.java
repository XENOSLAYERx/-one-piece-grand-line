package com.nerodev.grandline.world.island;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Everything about one island that does not depend on which chunk is being built: its turned layout, the height of
 * the land at every column, where each building stands and at what floor height, the pier and the buried treasure.
 * It is a pure function of the island type, centre, sea level and seed, so every chunk computes the same island.
 */
public final class IslandPlan {
    /** A placed building: its footprint (inclusive), ground height under its floor and the side its door faces. */
    public record Placed(IslandType.Site.Kind kind, int x0, int z0, int x1, int z1, int ground, Direction door) {
        public int centerX() {
            return (x0 + x1) / 2;
        }

        public int centerZ() {
            return (z0 + z1) / 2;
        }

        boolean contains(int x, int z, int margin) {
            return x >= x0 - margin && x <= x1 + margin && z >= z0 - margin && z <= z1 + margin;
        }

        double distance(int x, int z) {
            double dx = Math.max(Math.max(x0 - x, 0), x - x1);
            double dz = Math.max(Math.max(z0 - z, 0), z - z1);
            return Math.sqrt(dx * dx + dz * dz);
        }

        /**
         * Buildings are designed facing north (front wall at local z = 0, door in its middle); this is the turn
         * that makes the front face the door's side.
         */
        public Rotation rotation() {
            return switch (door) {
                case SOUTH -> Rotation.CLOCKWISE_180;
                case EAST -> Rotation.CLOCKWISE_90;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        /** Width along the front wall. */
        public int width() {
            return door.getAxis() == Direction.Axis.Z ? x1 - x0 + 1 : z1 - z0 + 1;
        }

        /** Depth from the front wall to the back. */
        public int depth() {
            return door.getAxis() == Direction.Axis.Z ? z1 - z0 + 1 : x1 - x0 + 1;
        }

        /** World x of a local (across, back) position. */
        public int worldX(int lx, int lz) {
            return switch (door) {
                case SOUTH -> x1 - lx;
                case EAST -> x1 - lz;
                case WEST -> x0 + lz;
                default -> x0 + lx;
            };
        }

        /** World z of a local (across, back) position. */
        public int worldZ(int lx, int lz) {
            return switch (door) {
                case SOUTH -> z1 - lz;
                case EAST -> z0 + lx;
                case WEST -> z1 - lx;
                default -> z0 + lz;
            };
        }
    }

    /** A straight stretch of path, from a door step (or the pier) to the middle of the island. */
    public record Path(int x0, int z0, int x1, int z1) {
        double distance(double x, double z) {
            double dx = x1 - x0;
            double dz = z1 - z0;
            double length = dx * dx + dz * dz;
            double t = length == 0 ? 0 : Mth.clamp(((x - x0) * dx + (z - z0) * dz) / length, 0, 1);
            double px = x0 + dx * t - x;
            double pz = z0 + dz * t - z;
            return Math.sqrt(px * px + pz * pz);
        }
    }

    /** A straight walkway from a point on the coast out to sea. */
    public record Pier(int x, int z, Direction out, int length) {}

    public final IslandType type;
    public final int cx;
    public final int cz;
    public final int sea;
    public final long seed;
    private final int rotation;
    private final SimplexNoise noise;
    private final double shapeOffset;
    public final List<Placed> buildings = new ArrayList<>();
    public final Pier pier;
    public final List<Path> paths = new ArrayList<>();
    /** Buried treasure on the beach, or null. */
    public final int[] treasure;

    public IslandPlan(IslandType type, int cx, int cz, int sea, long seed) {
        this.type = type;
        this.cx = cx;
        this.cz = cz;
        this.sea = sea;
        this.seed = seed;
        this.rotation = (int) Math.floorMod(seed >>> 7, 4L);
        this.noise = new SimplexNoise(new LegacyRandomSource(seed));
        this.shapeOffset = (seed & 1023) * 0.37;
        for (IslandType.Site site : type.sites()) {
            // Turn the footprint's corners (not its centre), so a building covers exactly the same ground at any turn.
            int lx0 = site.x() - site.width() / 2;
            int lz0 = site.z() - site.depth() / 2;
            int[] a = turn(lx0, lz0);
            int[] b = turn(lx0 + site.width() - 1, lz0 + site.depth() - 1);
            int[] at = turn(site.x(), site.z());
            int x0 = cx + Math.min(a[0], b[0]);
            int z0 = cz + Math.min(a[1], b[1]);
            int x1 = cx + Math.max(a[0], b[0]);
            int z1 = cz + Math.max(a[1], b[1]);
            int ground = Math.max(sea + 1, (int) Math.floor(rawHeight((x0 + x1) / 2, (z0 + z1) / 2)));
            buildings.add(new Placed(site.kind(), x0, z0, x1, z1, ground, doorToward(at[0], at[1])));
        }
        this.pier = makePier();
        this.treasure = type.treasureAngle() < 0 ? null : coast(Math.toRadians(type.treasureAngle() + rotation * 90), 1);
        for (Placed b : buildings) {
            int lx = b.width() / 2;
            paths.add(new Path(b.worldX(lx, -2), b.worldZ(lx, -2), cx, cz));
        }
        paths.add(new Path(pier.x(), pier.z(), cx, cz));
    }

    /** Whether a column is part of a path or the little square in the middle of the island. */
    public boolean path(int x, int z) {
        double dx = x - cx;
        double dz = z - cz;
        if (dx * dx + dz * dz <= 12) {
            return true;
        }
        for (Placed b : buildings) {
            if (b.contains(x, z, 0)) {
                return false;
            }
        }
        for (Path path : paths) {
            if (path.distance(x, z) <= 1.0) {
                return true;
            }
        }
        return false;
    }

    /** Turns a layout offset by the island's rotation (quarter turns). */
    private int[] turn(int x, int z) {
        return switch (rotation) {
            case 1 -> new int[] {-z, x};
            case 2 -> new int[] {-x, -z};
            case 3 -> new int[] {z, -x};
            default -> new int[] {x, z};
        };
    }

    /** Doors face the middle of the island, where the paths meet. */
    private static Direction doorToward(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.WEST : Direction.EAST;
        }
        return dz > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    /** Coastline radius in a direction: the island is never quite round. */
    public double radiusAt(double angle) {
        double wobble = noise.getValue(Math.cos(angle) * 1.4 + shapeOffset, Math.sin(angle) * 1.4 + shapeOffset) * 0.12
                + noise.getValue(Math.cos(angle) * 3.1 + shapeOffset * 2, Math.sin(angle) * 3.1) * 0.06;
        return type.radius() * (0.86 + wobble);
    }

    /** Land height before buildings flatten it: hills inland, beaches, then a shelf dropping into the sea. */
    double rawHeight(int x, int z) {
        double dx = x - cx;
        double dz = z - cz;
        double d = Math.sqrt(dx * dx + dz * dz);
        double r = d / radiusAt(Math.atan2(dz, dx));
        if (r >= 1) {
            return sea - 1 - (r - 1) * 18;
        }
        double hills = noise.getValue(x * 0.035 + shapeOffset, z * 0.035) * 0.5 + 0.5;
        double bumps = noise.getValue(x * 0.11, z * 0.11 + shapeOffset) * 1.6;
        double inland = 1 - r * r;
        return sea + 0.6 + inland * type.peak() * (0.55 + 0.45 * hills) + bumps * inland;
    }

    /**
     * The height of the top solid block at a column, or {@link Integer#MIN_VALUE} if the island does not reach it.
     * Ground around a building is levelled to its floor (the nearest building wins) and blends back into the hills.
     */
    public int height(int x, int z) {
        double dx = x - cx;
        double dz = z - cz;
        if (dx * dx + dz * dz > Mth.square(type.radius() * 1.35)) {
            return Integer.MIN_VALUE;
        }
        double h = rawHeight(x, z);
        Placed nearest = null;
        double best = 7;
        for (Placed b : buildings) {
            double dist = b.distance(x, z);
            if (dist < best) {
                best = dist;
                nearest = b;
            }
        }
        if (nearest != null) {
            double t = best <= 1 ? 1 : 1 - (best - 1) / 6;
            h = Mth.lerp(t * t * (3 - 2 * t), h, nearest.ground);
        }
        return (int) Math.floor(h);
    }

    /** Whether a column is a sandy beach (low land right by the sea) rather than grass. */
    public boolean beach(int x, int z, int height) {
        return height <= sea + 1 || type.flora() == IslandType.Flora.PALM && height <= sea + 2;
    }

    /** True if a building, its yard or the pier occupies the column. */
    public boolean occupied(int x, int z) {
        for (Placed b : buildings) {
            if (b.contains(x, z, 2)) {
                return true;
            }
        }
        return false;
    }

    /** Walks out from the centre along an angle to the first column at most {@code above} blocks over the sea. */
    private int[] coast(double angle, int above) {
        for (int step = 4; step < type.radius() * 1.3; step++) {
            int x = cx + (int) Math.round(Math.cos(angle) * step);
            int z = cz + (int) Math.round(Math.sin(angle) * step);
            if (height(x, z) <= sea + above) {
                return new int[] {x, z};
            }
        }
        return new int[] {cx + (int) (Math.cos(angle) * type.radius()), cz + (int) (Math.sin(angle) * type.radius())};
    }

    private Pier makePier() {
        double angle = Math.toRadians(type.pierAngle() + rotation * 90);
        int[] start = coast(angle, 1);
        Direction out = Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle))
                ? (Math.cos(angle) > 0 ? Direction.EAST : Direction.WEST)
                : (Math.sin(angle) > 0 ? Direction.SOUTH : Direction.NORTH);
        return new Pier(start[0] - out.getStepX() * 3, start[1] - out.getStepZ() * 3, out, 16);
    }

    /** A cheap, well-mixed per-column random number in [0, 1). */
    public double hash(int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
