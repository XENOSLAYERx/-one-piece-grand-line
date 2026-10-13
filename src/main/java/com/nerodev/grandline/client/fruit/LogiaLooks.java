package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.client.render.ProjectileVisuals.Trail;
import com.nerodev.grandline.client.render.ProjectileVisuals.Visual;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fruit.logia.DarkFruit;
import com.nerodev.grandline.fruit.logia.ForestFruit;
import com.nerodev.grandline.fruit.logia.GasFruit;
import com.nerodev.grandline.fruit.logia.GlintFruit;
import com.nerodev.grandline.fruit.logia.IceFruit;
import com.nerodev.grandline.fruit.logia.MagmaFruit;
import com.nerodev.grandline.fruit.logia.RumbleFruit;
import com.nerodev.grandline.fruit.logia.SandFruit;
import com.nerodev.grandline.fruit.logia.SmokeFruit;
import com.nerodev.grandline.fruit.logia.SnowFruit;
import com.nerodev.grandline.fruit.logia.SwampFruit;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.fx.GLParticles.Kind;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** How every Logia projectile and area looks on the client. */
public final class LogiaLooks {
    private LogiaLooks() {}

    public static void install() {
        // Smoke: thick rolling smoke, never a glowing core.
        register(SmokeFruit.BLOW, both(particles(0f, 0, Kind.SMOKE, 1.8f, 5), sparse(Kind.SMOKE, 0x9A9EA4, 2.4f, 3)));
        register(SmokeFruit.SNAKE, particles(0f, 0, Kind.SMOKE, 1.2f, 4));
        register(SmokeFruit.SCREEN, zone(Kind.SMOKE, 3.0f, 0.2f, false));

        // Sand: whirling grit.
        register(SandFruit.SABLES_STORM, tornado(Kind.SMOKE, 1.6f, 14, Kind.SHARD));
        register(SandFruit.QUICKSAND, both(swirl(Kind.SMOKE, 0.9f, 10, -1), zone(Kind.SHARD, 0.8f, 0.04f, true)));
        register(SandFruit.SANDSTORM_FIELD, both(zone(Kind.SMOKE, 2.6f, 0.05f, false), zone(Kind.SHARD, 0.8f, 0.03f, false)));

        // Rumble: crackling electricity and a dark storm cloud.
        register(RumbleFruit.SPEAR, both(particles(1.5f, 0xFFFFFF, Kind.SPARK, 1.0f, 3), sparse(Kind.STREAK, RumbleFruit.COLOR,
                1.2f, 2)));
        register(RumbleFruit.STORM_CLOUD, both(cloud(0x3C404C, 10, 3.6f), sparkle(Kind.SPARK, RumbleFruit.CORE, 1.0f, 3)));

        // Ice: shards and frost; Pheasant Beak sheds icy feathers.
        register(IceFruit.SPEAR, both(particles(1.0f, 0xF0FCFF, Kind.SNOW, 0.9f, 2), sparse(Kind.SHARD, IceFruit.COLOR, 0.8f, 3)));
        register(IceFruit.PHEASANT, both(particles(0.9f, 0xF0FCFF, Kind.FEATHER, 1.6f, 4), particles(0f, 0, Kind.SNOW, 1.2f, 4)));

        // Dark: darkness swallows light, so no glow at all, only black mist pulled inwards.
        register(DarkFruit.HOLE, both(swirl(Kind.SMOKE, 1.6f, 14, -1, DarkFruit.VOID), sparkle(Kind.GLOW, DarkFruit.COLOR, 0.8f,
                3)));
        register(DarkFruit.MATTER, both(particles(0f, 0, Kind.SMOKE, 2.0f, 6), sparse(Kind.GLOW, DarkFruit.COLOR, 0.9f, 2)));
        register(DarkFruit.WORLD, both(zone(Kind.SMOKE, 3.2f, 0.035f, true), swirl(Kind.SMOKE, 2.0f, 8, -1, DarkFruit.VOID)));

        // Glint: pure light.
        register(GlintFruit.BULLET, particles(1.8f, 0xFFFFFF, Kind.GLOW, 0.6f, 2));
        register(GlintFruit.RAIN_FIELD, both(falling(Kind.STREAK, GlintFruit.COLOR, 1.6f, 6, 14), zone(Kind.GLOW, 0.7f, 0.03f,
                true)));

        // Magma: molten rock, heavy smoke, embers.
        register(MagmaFruit.FIST, both(particles(1.0f, 0xFFC060, Kind.FLAME, 1.4f, 6), sparse(Kind.SMOKE, 0x2A2020, 2.0f, 2)));
        register(MagmaFruit.POOL, both(zone(Kind.GLOW, 1.0f, 0.12f, true), bubbles(MagmaFruit.GLOW, 0.06f)));
        register(MagmaFruit.FIELD, both(vents(Kind.FLAME, MagmaFruit.GLOW, 2.2f, 3), zone(Kind.SMOKE, 2.6f, 0.02f, false)));
        register(MagmaFruit.WAVE, both(wave(Kind.FLAME, 2.0f, 16), wave(Kind.SMOKE, 2.6f, 4, 0x2A2020)));

        // Swamp: sucking mud.
        register(SwampFruit.PIT, both(swirl(Kind.SMOKE, 1.0f, 8, -1), bubbles(SwampFruit.COLOR, 0.05f)));
        register(SwampFruit.MUD_BALL, both(particles(0f, 0, Kind.DROP, 1.2f, 3), particles(0f, 0, Kind.SMOKE, 0.8f, 1)));

        // Gas: drifting poison.
        register(GasFruit.CLOUD, zone(Kind.SMOKE, 2.6f, 0.25f, false));
        register(GasFruit.CANISTER, both(particles(0.6f, 0xEFFFC0, Kind.SMOKE, 0.8f, 2), sparse(Kind.BUBBLE, GasFruit.TOXIC, 1f, 4)));
        register(GasFruit.DOMAIN, both(zone(Kind.SMOKE, 3.4f, 0.04f, false), zone(Kind.BUBBLE, 1.2f, 0.01f, true)));

        // Snow.
        register(SnowFruit.RABBIT, both(particles(0.8f, 0xFFFFFF, Kind.SNOW, 1.2f, 3), particles(0f, 0, Kind.SMOKE, 0.6f, 1)));
        register(SnowFruit.FIELD, both(falling(Kind.SNOW, SnowFruit.COLOR, 1.4f, 8, 6), zone(Kind.SMOKE, 1.4f, 0.01f, true)));
        register(SnowFruit.SLIDE, both(wave(Kind.SMOKE, 3.0f, 12), wave(Kind.SNOW, 1.6f, 10)));

        // Forest: leaves, spores and thorns.
        register(ForestFruit.THORNS, both(zone(Kind.LEAF, 1.0f, 0.06f, true), spikes(ForestFruit.BARK, 0.06f)));
        register(ForestFruit.WILDWOOD, both(falling(Kind.LEAF, ForestFruit.COLOR, 1.4f, 8, 7), rising(Kind.GLOW,
                ForestFruit.LIFE, 0.6f, 0.03f)));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Building blocks
    // ---------------------------------------------------------------------------------------------------------

    /** Two looks at once; the core comes from the first. */
    static Visual both(Visual first, Visual second) {
        Trail a = first.trail();
        Trail b = second.trail();
        return new Visual(first.coreScale(), first.coreTint(), (level, p, r) -> {
            a.tick(level, p, r);
            b.tick(level, p, r);
        });
    }

    private static Vec3 center(AbilityProjectile p) {
        return p.position().add(0, p.getBbHeight() / 2, 0);
    }

    /** One particle in a fixed colour every {@code every} ticks on average. */
    static Visual sparse(Kind kind, int color, float scale, int every) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            if (r.nextInt(every) == 0) {
                FxKit.particle(level, type, color, scale, center(p).add(FxKit.randomUnit(r).scale(p.size() * 0.4)),
                        FxKit.randomUnit(r).scale(0.08));
            }
        });
    }

    /** For areas: a particle somewhere inside the dome every {@code every} ticks on average. */
    static Visual sparkle(Kind kind, int color, float scale, int every) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            if (r.nextInt(every) == 0) {
                FxKit.particle(level, type, color, scale, inside(p, r, 0.5), FxKit.randomUnit(r).scale(0.1));
            }
        });
    }

    /** A random point in the area's disc, at most {@code height} × radius above it. */
    private static Vec3 inside(AbilityProjectile p, RandomSource r, double height) {
        double radius = p.size() / 2.0;
        double a = r.nextDouble() * Mth.TWO_PI;
        double d = Math.sqrt(r.nextDouble()) * radius;
        return p.position().add(Math.cos(a) * d, r.nextDouble() * radius * height, Math.sin(a) * d);
    }

    /** A funnel of particles spinning around the projectile and climbing it: a moving whirlwind. */
    static Visual tornado(Kind kind, float scale, int perTick, Kind debris) {
        var type = GLParticles.byKind(kind);
        var debrisType = GLParticles.byKind(debris);
        return new Visual(0f, 0, (level, p, r) -> {
            double height = p.size() * 1.4;
            for (int i = 0; i < perTick; i++) {
                double h = r.nextDouble() * height;
                double radius = 0.3 + h * 0.32;
                double a = (p.tickCount * 0.6) + r.nextDouble() * Mth.TWO_PI;
                Vec3 at = p.position().add(Math.cos(a) * radius, h, Math.sin(a) * radius);
                Vec3 spin = new Vec3(-Math.sin(a), 0.06, Math.cos(a)).scale(0.25);
                FxKit.particle(level, type, p.color(), scale * (0.6f + (float) (h / height)), at, spin);
            }
            if (r.nextInt(2) == 0) {
                double a = r.nextDouble() * Mth.TWO_PI;
                FxKit.particle(level, debrisType, p.color(), 1.0f, p.position().add(Math.cos(a), 0.3, Math.sin(a)),
                        new Vec3(Math.cos(a) * 0.2, 0.35, Math.sin(a) * 0.2));
            }
        });
    }

    /** Particles circling over the ground, drifting inwards ({@code direction} -1) or outwards (+1). */
    static Visual swirl(Kind kind, float scale, int perTick, int direction) {
        return swirl(kind, scale, perTick, direction, -1);
    }

    /** As {@link #swirl(Kind, float, int, int)}, in a fixed colour ({@code -1}: the projectile's own). */
    static Visual swirl(Kind kind, float scale, int perTick, int direction, int color) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            int count = Math.max(2, Math.min(24, (int) (perTick * radius / 4)));
            for (int i = 0; i < count; i++) {
                double a = r.nextDouble() * Mth.TWO_PI;
                double d = (0.2 + 0.8 * Math.sqrt(r.nextDouble())) * radius;
                Vec3 at = p.position().add(Math.cos(a) * d, 0.15 + r.nextDouble() * 0.4, Math.sin(a) * d);
                Vec3 tangent = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.12 + 0.03 * radius);
                Vec3 radial = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.05 * direction * Math.min(3, radius / 2));
                FxKit.particle(level, type, color == -1 ? p.color() : color, scale, at, tangent.add(radial));
            }
            if (p.tickCount % 10 == 0) {
                FxKit.groundRing(level, color == -1 ? p.color() : color, (float) radius * 2f, p.position());
            }
        });
    }

    /** Particles falling from {@code height} blocks above the area: rain, snow, leaves, beams of light. */
    static Visual falling(Kind kind, int color, float scale, int perTick, double height) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            int count = Math.max(1, Math.min(30, (int) (perTick * radius * radius / 50)));
            double speed = kind == Kind.STREAK ? -1.6 : -0.12;
            for (int i = 0; i < count; i++) {
                Vec3 at = inside(p, r, 0).add(0, height * (0.6 + r.nextDouble() * 0.4), 0);
                FxKit.particle(level, type, color, scale, at, new Vec3((r.nextDouble() - 0.5) * 0.05, speed, (r.nextDouble() - 0.5) * 0.05));
            }
        });
    }

    /** Particles drifting up out of the ground across the area. */
    static Visual rising(Kind kind, int color, float scale, float density) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            int count = Math.max(1, Math.min(20, (int) (Math.PI * radius * radius * density)));
            for (int i = 0; i < count; i++) {
                FxKit.particle(level, type, color, scale, inside(p, r, 0).add(0, 0.2, 0), new Vec3(0, 0.04 + r.nextDouble() * 0.04, 0));
            }
        });
    }

    /** A low, dark cloud hanging {@code height} blocks above the area. */
    static Visual cloud(int color, double height, float scale) {
        var type = GLParticles.byKind(Kind.SMOKE);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            int count = Math.max(2, Math.min(16, (int) (radius * 1.5)));
            for (int i = 0; i < count; i++) {
                Vec3 at = inside(p, r, 0).add(0, height + r.nextDouble() * 1.5, 0);
                FxKit.particle(level, type, color, scale, at, new Vec3(0, -0.04, 0));
            }
        });
    }

    /** Bubbles that swell up and pop across a molten or muddy surface. */
    static Visual bubbles(int color, float density) {
        var bubble = GLParticles.byKind(Kind.BUBBLE);
        var drop = GLParticles.byKind(Kind.DROP);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            int count = Math.max(1, Math.min(12, (int) (Math.PI * radius * radius * density)));
            for (int i = 0; i < count; i++) {
                Vec3 at = inside(p, r, 0).add(0, 0.1, 0);
                FxKit.particle(level, bubble, color, 1.4f, at, new Vec3(0, 0.02, 0));
                if (r.nextInt(3) == 0) {
                    FxKit.particle(level, drop, color, 1.2f, at, new Vec3((r.nextDouble() - 0.5) * 0.2, 0.25, (r.nextDouble() - 0.5) * 0.2));
                }
            }
        });
    }

    /** Fire spouting out of random vents across the area. */
    static Visual vents(Kind kind, int color, float scale, int perTick) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            for (int i = 0; i < perTick; i++) {
                Vec3 at = inside(p, r, 0);
                for (int j = 0; j < 3; j++) {
                    FxKit.particle(level, type, color, scale, at.add(0, j * 0.4, 0), new Vec3(0, 0.25 + r.nextDouble() * 0.3, 0));
                }
            }
        });
    }

    /** Thorns jutting up out of the ground and falling back. */
    static Visual spikes(int color, float density) {
        var shard = GLParticles.byKind(Kind.SHARD);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            int count = Math.max(1, Math.min(10, (int) (Math.PI * radius * radius * density)));
            for (int i = 0; i < count; i++) {
                FxKit.particle(level, shard, color, 1.6f, inside(p, r, 0).add(0, 0.1, 0), new Vec3(0, 0.3, 0));
            }
        });
    }

    /** A wall of particles across the direction of travel, as tall as half the projectile's size: a wave. */
    static Visual wave(Kind kind, float scale, int perTick) {
        return wave(kind, scale, perTick, -1);
    }

    static Visual wave(Kind kind, float scale, int perTick, int color) {
        var type = GLParticles.byKind(kind);
        return new Visual(0f, 0, (level, p, r) -> {
            Vec3 motion = p.getDeltaMovement();
            Vec3 flat = new Vec3(motion.x, 0, motion.z);
            flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
            Vec3 side = new Vec3(-flat.z, 0, flat.x);
            double half = p.size() / 2;
            for (int i = 0; i < perTick; i++) {
                double t = (r.nextDouble() * 2 - 1) * half;
                // Taller in the middle, curling forwards at the crest.
                double crest = half * 0.55 * (1 - (t / half) * (t / half) * 0.6);
                double h = r.nextDouble() * crest;
                Vec3 at = p.position().add(side.scale(t)).add(flat.scale(h / crest * 0.8)).add(0, h, 0);
                FxKit.particle(level, type, color == -1 ? p.color() : color, scale, at,
                        flat.scale(0.25 + 0.2 * h / crest).add(0, 0.05, 0));
            }
        });
    }
}
