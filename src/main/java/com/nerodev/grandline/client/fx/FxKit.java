package com.nerodev.grandline.client.fx;

import com.nerodev.grandline.fx.GLParticleOptions;
import com.nerodev.grandline.fx.GLParticles;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Building blocks for client effects: bursts, rings, trails, spirals. Effects are composed from these so each
 * ability gets a distinct look with a bounded particle count.
 */
public final class FxKit {
    private FxKit() {}

    public static void particle(ClientLevel level, Supplier<ParticleType<GLParticleOptions>> type, int color, float scale,
            Vec3 pos, Vec3 motion) {
        level.addParticle(GLParticles.options(type, color, scale), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
    }

    /** Particles flying outwards in random directions. */
    public static void burst(ClientLevel level, Supplier<ParticleType<GLParticleOptions>> type, int color, float scale,
            Vec3 center, int count, double speed) {
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            Vec3 dir = randomUnit(random);
            double s = speed * (0.5 + random.nextDouble() * 0.5);
            particle(level, type, color, scale, center, dir.scale(s));
        }
    }

    /** Particles flying outwards in a horizontal circle (dust kicked up, air pushed out). */
    public static void radial(ClientLevel level, Supplier<ParticleType<GLParticleOptions>> type, int color, float scale,
            Vec3 center, int count, double speed, double up) {
        for (int i = 0; i < count; i++) {
            double a = Mth.TWO_PI * i / count + level.random.nextDouble() * 0.3;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            particle(level, type, color, scale, center.add(dir.scale(0.3)), dir.scale(speed).add(0, up, 0));
        }
    }

    /** Particles spaced along a line, optionally drifting along it. */
    public static void line(ClientLevel level, Supplier<ParticleType<GLParticleOptions>> type, int color, float scale,
            Vec3 from, Vec3 to, double spacing, double drift, double jitter) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 dir = delta.scale(1 / length);
        int count = Math.min(80, (int) Math.ceil(length / spacing));
        RandomSource random = level.random;
        for (int i = 0; i <= count; i++) {
            Vec3 p = from.add(dir.scale(length * i / Math.max(1, count)))
                    .add((random.nextDouble() - 0.5) * jitter, (random.nextDouble() - 0.5) * jitter, (random.nextDouble() - 0.5) * jitter);
            particle(level, type, color, scale, p, dir.scale(drift));
        }
    }

    /** A ring particle lying flat at {@code pos}. */
    public static void groundRing(ClientLevel level, int color, float scale, Vec3 pos) {
        particle(level, GLParticles.RING, color, scale, pos.add(0, 0.05, 0), Vec3.ZERO);
    }

    /** A ring particle facing {@code normal}. */
    public static void shockwave(ClientLevel level, int color, float scale, Vec3 pos, Vec3 normal) {
        level.addParticle(GLParticles.options(GLParticles.SHOCKWAVE, color, scale), pos.x, pos.y, pos.z,
                normal.x, normal.y, normal.z);
    }

    public static Vec3 randomUnit(RandomSource random) {
        double z = random.nextDouble() * 2 - 1;
        double a = random.nextDouble() * Mth.TWO_PI;
        double r = Math.sqrt(1 - z * z);
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }
}
