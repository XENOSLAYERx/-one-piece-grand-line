package com.nerodev.grandline.client.fx;

import com.nerodev.grandline.client.visual.ClientVisuals;
import com.nerodev.grandline.client.visual.RubberVisuals;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client looks of Devil Fruit effects. Registered into {@link ClientEffects}. */
public final class FruitEffects {
    private FruitEffects() {}

    public static void register() {
        ClientEffects.register(GLEffects.FRUIT_EATEN, FruitEffects::fruitEaten);
        ClientEffects.register(GLEffects.AWAKENING, FruitEffects::awakening);
        ClientEffects.register(GLEffects.LOGIA_DODGE, FruitEffects::logiaDodge);
        ClientEffects.register(GLEffects.TRANSFORM_PUFF, FruitEffects::transformPuff);
        // Flame
        ClientEffects.register(GLEffects.FLAME_CHARGE, FruitEffects::flameCharge);
        ClientEffects.register(GLEffects.FLAME_BURST, FruitEffects::flameBurst);
        ClientEffects.register(GLEffects.FIRE_EXPLOSION, FruitEffects::fireExplosion);
        ClientEffects.register(GLEffects.FIRE_PILLAR, FruitEffects::firePillar);
        ClientEffects.register(GLEffects.ENTEI_CHARGE, FruitEffects::enteiCharge);
        ClientEffects.register(GLEffects.FLAME_FIELD, FruitEffects::flameField);
        // Gum
        ClientEffects.register(GLEffects.STRETCH, FruitEffects::stretch);
        ClientEffects.register(GLEffects.PUNCH_IMPACT, FruitEffects::punchImpact);
        ClientEffects.register(GLEffects.RUBBER_BOUNCE, FruitEffects::rubberBounce);
        ClientEffects.register(GLEffects.GEAR_STEAM, FruitEffects::gearSteam);
        ClientEffects.register(GLEffects.INFLATE, FruitEffects::inflate);
        ClientEffects.register(GLEffects.RUBBER_WORLD, FruitEffects::rubberWorld);
        ClientEffects.register(GLEffects.DAWN_FIST, FruitEffects::dawnFist);
        // Leopard
        ClientEffects.register(GLEffects.CLAW_SLASH, FruitEffects::clawSlash);
        ClientEffects.register(GLEffects.POUNCE, FruitEffects::pounce);
        ClientEffects.register(GLEffects.ROAR, FruitEffects::roar);
    }

    private static Vec3 center(@Nullable Entity source, FxParams p) {
        return source != null ? source.position().add(0, source.getBbHeight() * 0.55, 0) : p.pos();
    }

    // ---------------------------------------------------------------------------------------------------------
    // General
    // ---------------------------------------------------------------------------------------------------------

    private static void fruitEaten(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 c = center(source, p);
        FxKit.burst(level, GLParticles.GLOW, p.color(), 1.2f, c, 24, 0.25);
        FxKit.burst(level, GLParticles.STAR, 0xFFFFFF, 1.0f, c, 10, 0.15);
        FxKit.groundRing(level, p.color(), 3f, p.pos());
        CameraShake.add(p.pos(), 1.0f, 8);
    }

    private static void awakening(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 c = center(source, p);
        for (int i = 0; i < 3; i++) {
            FxKit.groundRing(level, p.color(), 4f + i * 3f, p.pos());
        }
        FxKit.burst(level, GLParticles.GLOW, p.color(), 1.8f, c, 40, 0.5);
        FxKit.burst(level, GLParticles.SPARK, 0xFFFFFF, 1.2f, c, 30, 0.7);
        for (int i = 0; i < 24; i++) {
            double a = Mth.TWO_PI * i / 24;
            FxKit.particle(level, GLParticles.STREAK, p.color(), 2f, c, new Vec3(Math.cos(a), 0.4, Math.sin(a)).scale(0.8));
        }
        CameraShake.add(p.pos(), 3f, 20);
    }

    private static void logiaDodge(ClientLevel level, @Nullable Entity source, FxParams p) {
        FxKit.burst(level, GLParticles.FLAME, p.color(), 1.3f, p.pos(), 14, 0.18);
        FxKit.burst(level, GLParticles.GLOW, p.color(), 1.0f, p.pos(), 4, 0.05);
    }

    private static void transformPuff(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 c = center(source, p);
        if (p.variant() == 1) {
            FxKit.burst(level, GLParticles.GLOW, p.color(), 0.8f, c, 6, 0.08);
            return;
        }
        FxKit.burst(level, GLParticles.SMOKE, p.color(), 1.4f * p.scale(), c, 20, 0.12);
        FxKit.burst(level, GLParticles.STAR, 0xFFF0C0, 1.0f, c, 8, 0.2);
        FxKit.groundRing(level, p.color(), 2.2f * p.scale(), p.pos());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Flame
    // ---------------------------------------------------------------------------------------------------------

    private static void flameCharge(ClientLevel level, @Nullable Entity source, FxParams p) {
        RandomSource r = level.random;
        int count = 3 + (int) (p.scale() * 3);
        for (int i = 0; i < count; i++) {
            Vec3 dir = FxKit.randomUnit(r);
            Vec3 from = p.pos().add(dir.scale(0.6 * p.scale()));
            FxKit.particle(level, GLParticles.FLAME, p.color(), 0.6f + p.scale() * 0.4f, from, dir.scale(-0.05).add(0, 0.03, 0));
        }
        FxKit.particle(level, GLParticles.GLOW, 0xFFD27A, 0.5f * p.scale(), p.pos(), Vec3.ZERO);
    }

    private static void flameBurst(ClientLevel level, @Nullable Entity source, FxParams p) {
        float s = p.scale();
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : Vec3.ZERO;
        int count = (int) (8 + 14 * s);
        for (int i = 0; i < count; i++) {
            Vec3 v = FxKit.randomUnit(level.random).scale(0.15 * s).add(dir.scale(0.25 * s));
            FxKit.particle(level, GLParticles.FLAME, p.color(), 0.9f + s * 0.6f, p.pos(), v);
        }
        if (s > 0.6f) {
            FxKit.shockwave(level, 0xFFB050, s * 1.4f, p.pos(), dir.lengthSqr() > 0 ? dir : new Vec3(0, 1, 0));
        }
    }

    private static void fireExplosion(ClientLevel level, @Nullable Entity source, FxParams p) {
        float s = p.scale();
        Vec3 c = p.pos();
        FxKit.burst(level, GLParticles.FLAME, p.color(), Math.min(2.6f, 1.6f + s * 0.4f), c, (int) (24 + 16 * s), 0.25 + 0.12 * s);
        FxKit.burst(level, GLParticles.SMOKE, 0x2E2626, 1.6f + s * 0.5f, c, (int) (10 + 6 * s), 0.12);
        FxKit.burst(level, GLParticles.SPARK, 0xFFD27A, 1.2f, c, (int) (16 + 8 * s), 0.6 + 0.2 * s);
        FxKit.particle(level, GLParticles.GLOW, 0xFFE8A0, 3f + s * 2f, c, Vec3.ZERO);
        FxKit.groundRing(level, 0xFF8030, 2f + s * 1.6f, c.add(0, -0.4, 0));
        FxKit.shockwave(level, 0xFFB050, 2f + s * 1.5f, c, new Vec3(0, 1, 0));
        CameraShake.add(c, 1.2f + s * 0.6f, 8 + (int) (s * 3));
    }

    private static void firePillar(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 base = p.pos();
        RandomSource r = level.random;
        for (int i = 0; i < 60; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double d = r.nextDouble() * p.scale();
            Vec3 at = base.add(Math.cos(a) * d, r.nextDouble() * 2, Math.sin(a) * d);
            FxKit.particle(level, GLParticles.FLAME, p.color(), 2.2f, at, new Vec3(0, 0.6 + r.nextDouble() * 0.6, 0));
        }
        FxKit.groundRing(level, 0xFF8030, p.scale() * 3f, base);
        FxKit.radial(level, GLParticles.SMOKE, 0x3A3030, 1.4f, base.add(0, 0.2, 0), 14, 0.25, 0.05);
        CameraShake.add(base, 2f, 10);
    }

    private static void enteiCharge(ClientLevel level, @Nullable Entity source, FxParams p) {
        float s = p.scale();
        RandomSource r = level.random;
        FxKit.particle(level, GLParticles.GLOW, 0xFFD27A, s * 1.2f, p.pos(), Vec3.ZERO);
        for (int i = 0; i < 8 + s * 5; i++) {
            Vec3 dir = FxKit.randomUnit(r);
            FxKit.particle(level, GLParticles.FLAME, p.color(), Math.min(2.4f, 1.0f + s * 0.25f),
                    p.pos().add(dir.scale(s * 0.5 * Math.cbrt(r.nextDouble()))), dir.scale(0.04));
        }
        // flames drawn up from the ground into the sun
        if (source != null) {
            Vec3 feet = source.position();
            for (int i = 0; i < 3; i++) {
                Vec3 at = feet.add((r.nextDouble() - 0.5) * 3, 0.1, (r.nextDouble() - 0.5) * 3);
                FxKit.particle(level, GLParticles.FLAME, p.color(), 1.0f, at, p.pos().subtract(at).scale(0.08));
            }
        }
    }

    private static void flameField(ClientLevel level, @Nullable Entity source, FxParams p) {
        RandomSource r = level.random;
        float radius = p.scale();
        for (int i = 0; i < 40; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double d = Math.sqrt(r.nextDouble()) * radius;
            Vec3 at = p.pos().add(Math.cos(a) * d, 0.05, Math.sin(a) * d);
            FxKit.particle(level, GLParticles.FLAME, p.color(), 1.1f, at, new Vec3(0, 0.08 + r.nextDouble() * 0.1, 0));
        }
        FxKit.groundRing(level, 0xFF6A1A, radius * 2.1f, p.pos());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Gum
    // ---------------------------------------------------------------------------------------------------------

    private static void stretch(ClientLevel level, @Nullable Entity source, FxParams p) {
        if (source != null) {
            ClientVisuals.add(new RubberVisuals.Stretch(source.getId(), p.pos(), p.dir(), p.scale(), p.variant(), p.color()));
        }
    }

    private static void punchImpact(ClientLevel level, @Nullable Entity source, FxParams p) {
        float s = p.scale();
        FxKit.shockwave(level, 0xFFFFFF, 1.2f * s, p.pos(), source != null ? p.pos().subtract(source.getEyePosition()) : new Vec3(0, 0, 1));
        FxKit.burst(level, GLParticles.STREAK, 0xFFFFFF, 1.2f, p.pos(), (int) (6 + 4 * s), 0.5 * s);
        FxKit.burst(level, GLParticles.SMOKE, 0xE0E0E0, 0.8f * s, p.pos(), (int) (4 + 2 * s), 0.08);
        CameraShake.add(p.pos(), 0.5f + s * 0.5f, 5 + (int) s);
    }

    private static void rubberBounce(ClientLevel level, @Nullable Entity source, FxParams p) {
        float s = p.scale();
        FxKit.groundRing(level, 0xF0D8FF, 1.5f + s, p.pos());
        FxKit.radial(level, GLParticles.SMOKE, 0xE8E0EC, 0.6f, p.pos().add(0, 0.1, 0), (int) (8 + 6 * s), 0.12 + 0.05 * s, 0.03);
    }

    private static void gearSteam(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 c = center(source, p);
        boolean fifth = p.variant() == 2;
        FxKit.burst(level, GLParticles.SMOKE, fifth ? 0xFFFFFF : 0xF2E6E8, fifth ? 1.4f : 1.0f, c, fifth ? 30 : 20, 0.18);
        if (fifth) {
            FxKit.burst(level, GLParticles.STAR, 0xFFF8D0, 1.2f, c, 14, 0.3);
            FxKit.groundRing(level, 0xFFFFFF, 4f, p.pos());
            CameraShake.add(p.pos(), 1.5f, 10);
        }
    }

    private static void inflate(ClientLevel level, @Nullable Entity source, FxParams p) {
        if (source != null) {
            ClientVisuals.add(new RubberVisuals.Inflate(source.getId(), p.scale()));
        }
    }

    private static void rubberWorld(ClientLevel level, @Nullable Entity source, FxParams p) {
        float radius = p.scale();
        FxKit.groundRing(level, 0xFFFFFF, radius * 2f, p.pos());
        FxKit.groundRing(level, 0xF0D8FF, radius * 1.3f, p.pos());
        FxKit.radial(level, GLParticles.SMOKE, 0xFFFFFF, 0.7f, p.pos().add(0, 0.1, 0), 18, radius * 0.06, 0.02);
    }

    private static void dawnFist(ClientLevel level, @Nullable Entity source, FxParams p) {
        if (source != null) {
            ClientVisuals.add(new RubberVisuals.DawnFist(source.getId(), p.pos(), p.scale(), p.color()));
        }
        FxKit.groundRing(level, 0xFFFFFF, 8f, p.pos());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Leopard
    // ---------------------------------------------------------------------------------------------------------

    /** Variant 0: three diagonal rakes facing dir. Variant 1: a full circle of slashes (frenzy). */
    private static void clawSlash(ClientLevel level, @Nullable Entity source, FxParams p) {
        if (p.variant() == 1) {
            float radius = p.scale();
            float offset = level.random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i < 16; i++) {
                double a = offset + Mth.TWO_PI * i / 16;
                Vec3 at = p.pos().add(Math.cos(a) * radius * 0.8, (level.random.nextDouble() - 0.5) * 0.8, Math.sin(a) * radius * 0.8);
                Vec3 tangent = new Vec3(-Math.sin(a), 0, Math.cos(a));
                FxKit.particle(level, GLParticles.STREAK, p.color(), 1.4f, at, tangent.scale(0.6));
            }
            return;
        }
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : new Vec3(0, 0, 1);
        Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize();
        for (int claw = -1; claw <= 1; claw++) {
            Vec3 origin = p.pos().add(side.scale(claw * 0.28));
            for (int i = 0; i < 6; i++) {
                double t = (i - 2.5) / 2.5;
                Vec3 at = origin.add(side.scale(t * 0.6)).add(0, -t * 0.6, 0);
                FxKit.particle(level, GLParticles.STREAK, p.color(), 1.0f, at, side.scale(0.25).add(0, -0.25, 0));
            }
        }
        FxKit.burst(level, GLParticles.SPARK, 0xC0302A, 0.8f, p.pos(), 6, 0.2);
    }

    private static void pounce(ClientLevel level, @Nullable Entity source, FxParams p) {
        FxKit.radial(level, GLParticles.SMOKE, 0xBFAF90, 0.7f, p.pos().add(0, 0.1, 0), 10, 0.15, 0.02);
        Vec3 back = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize().scale(-0.4) : Vec3.ZERO;
        FxKit.burst(level, GLParticles.STREAK, 0xFFE6B0, 1.0f, p.pos().add(0, 0.6, 0), 6, 0.2);
        FxKit.particle(level, GLParticles.SHOCKWAVE, 0xFFE6B0, 1.4f, p.pos().add(0, 0.6, 0), back.reverse());
    }

    private static void roar(ClientLevel level, @Nullable Entity source, FxParams p) {
        float radius = p.scale();
        Vec3 c = p.pos();
        for (int i = 0; i < 3; i++) {
            FxKit.shockwave(level, p.color(), 1.5f + i * radius * 0.35f, c, p.dir().lengthSqr() > 1.0E-4 ? p.dir() : new Vec3(0, 1, 0));
        }
        FxKit.groundRing(level, p.color(), radius * 2f, source != null ? source.position() : c);
        FxKit.radial(level, GLParticles.SMOKE, 0xC8B898, 1.0f, (source != null ? source.position() : c).add(0, 0.2, 0), 20, 0.3, 0.02);
        CameraShake.add(c, radius > 2 ? 2.5f : 1f, 14);
    }
}
