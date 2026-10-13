package com.nerodev.grandline.client.haki;

import com.nerodev.grandline.client.fx.CameraShake;
import com.nerodev.grandline.client.fx.ClientEffects;
import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.client.visual.ClientVisuals;
import com.nerodev.grandline.client.visual.LightningVisual;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Looks of Haki. Armament is dark and heavy (violet-black bursts and rings), Observation is pale and quiet
 * (ripples, afterimages), Conqueror's is black lightning with a red edge and shaking ground. Particle counts stay
 * low; the lightning is a few quads.
 */
final class HakiEffects {
    static final int BLACK = 0x0A0006;
    static final int CRIMSON = 0xE0182C;
    static final int VIOLET = 0x8A63D2;

    private HakiEffects() {}

    static void register() {
        ClientEffects.register(GLEffects.HAKI_COAT, HakiEffects::coat);
        ClientEffects.register(GLEffects.HAKI_IMPACT, HakiEffects::impact);
        ClientEffects.register(GLEffects.HAKI_EMISSION, HakiEffects::emission);
        ClientEffects.register(GLEffects.OBSERVATION_PULSE, HakiEffects::observationPulse);
        ClientEffects.register(GLEffects.FUTURE_SIGHT, HakiEffects::futureSight);
        ClientEffects.register(GLEffects.FORESEEN_DODGE, HakiEffects::foreseenDodge);
        ClientEffects.register(GLEffects.CONQUEROR_CHARGE, HakiEffects::conquerorCharge);
        ClientEffects.register(GLEffects.CONQUEROR_WAVE, HakiEffects::conquerorWave);
        ClientEffects.register(GLEffects.CONQUEROR_CLASH, HakiEffects::clash);
        ClientEffects.register(GLEffects.CONQUEROR_STRIKE, HakiEffects::strike);
        ClientEffects.register(GLEffects.KNOCKOUT, HakiEffects::knockout);
    }

    private static Vec3 base(@Nullable Entity source, FxParams p) {
        return source != null ? source.position() : p.pos();
    }

    private static boolean isLocal(@Nullable Entity source) {
        return source != null && source == Minecraft.getInstance().player;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Armament
    // ---------------------------------------------------------------------------------------------------------

    /** Variant 0 arms harden, 1 whole body, 2 coating fades, 3 emission primed at the fist, 4 body back to arms. */
    private static void coat(ClientLevel level, @Nullable Entity source, FxParams p) {
        RandomSource r = level.random;
        Vec3 feet = base(source, p);
        switch (p.variant()) {
            case 0, 1 -> {
                boolean full = p.variant() == 1;
                int count = full ? 22 : 12;
                for (int i = 0; i < count; i++) {
                    // Dark wisps drawn in toward the limbs.
                    double h = full ? r.nextDouble() * 1.9 : 0.7 + r.nextDouble() * 0.8;
                    Vec3 dir = new Vec3(r.nextDouble() - 0.5, 0, r.nextDouble() - 0.5).normalize();
                    Vec3 at = feet.add(dir.scale(1.0 + r.nextDouble() * 0.5)).add(0, h, 0);
                    FxKit.particle(level, GLParticles.SMOKE, 0x1A1026, 0.45f, at, dir.scale(-0.09));
                }
                for (int i = 0; i < (full ? 8 : 4); i++) {
                    Vec3 at = feet.add((r.nextDouble() - 0.5) * 0.8, 0.6 + r.nextDouble() * (full ? 1.3 : 0.8), (r.nextDouble() - 0.5) * 0.8);
                    FxKit.particle(level, GLParticles.STAR, 0xB49CFF, 0.8f, at, Vec3.ZERO);
                }
                if (full) {
                    FxKit.groundRing(level, p.color(), 2.2f, feet);
                }
                CameraShake.add(feet, full ? 0.8f : 0.4f, 6);
            }
            case 2, 4 -> {
                for (int i = 0; i < 6; i++) {
                    Vec3 at = feet.add((r.nextDouble() - 0.5) * 0.7, 0.8 + r.nextDouble() * 0.7, (r.nextDouble() - 0.5) * 0.7);
                    FxKit.particle(level, GLParticles.SMOKE, 0x2A1E3A, 0.35f, at, new Vec3(0, 0.03, 0));
                }
            }
            default -> {
                Vec3 fist = p.pos();
                FxKit.particle(level, GLParticles.GLOW, 0xA77BFF, 1.2f, fist, Vec3.ZERO);
                FxKit.shockwave(level, 0xA77BFF, 0.8f, fist, p.dir());
                FxKit.burst(level, GLParticles.STAR, 0xD8C8FF, 0.6f, fist, 4, 0.05);
            }
        }
    }

    /** Variant 0: a hardened blow lands. Variant 1: a hardened body shrugs a hit off. */
    private static void impact(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 at = p.pos();
        if (p.variant() == 1) {
            FxKit.burst(level, GLParticles.SPARK, 0xC8B4FF, 0.9f, at, 8, 0.35);
            FxKit.particle(level, GLParticles.STAR, 0xE8E0FF, 1.1f, at, Vec3.ZERO);
            return;
        }
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : Vec3.ZERO;
        FxKit.shockwave(level, p.color(), 1.3f, at, dir);
        FxKit.burst(level, GLParticles.SPARK, 0x2A1840, 1.0f, at, 10, 0.4);
        FxKit.burst(level, GLParticles.SPARK, 0xB49CFF, 0.7f, at, 5, 0.3);
        FxKit.particle(level, GLParticles.STAR, 0xFFFFFF, 1.0f, at, Vec3.ZERO);
        CameraShake.add(at, 0.9f, 5);
    }

    /** Emission: rings ripple through the body and the shock bursts out of the far side. */
    private static void emission(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : new Vec3(0, 0, 1);
        Vec3 entry = p.pos().subtract(dir.scale(0.4));
        for (int i = 0; i < 3; i++) {
            FxKit.shockwave(level, i == 2 ? 0xFFFFFF : p.color(), 0.9f + i * 0.8f, entry.add(dir.scale(0.6 * i)), dir);
        }
        Vec3 exit = p.pos().add(dir.scale(0.6));
        RandomSource r = level.random;
        for (int i = 0; i < 14; i++) {
            Vec3 spread = dir.add(FxKit.randomUnit(r).scale(0.45)).normalize();
            FxKit.particle(level, GLParticles.STREAK, 0xC8B4FF, 1.4f, exit, spread.scale(0.6 + r.nextDouble() * 0.4));
        }
        FxKit.burst(level, GLParticles.SPARK, 0x1A0E28, 1.1f, exit, 10, 0.5);
        FxKit.particle(level, GLParticles.GLOW, p.color(), 2.2f, p.pos(), Vec3.ZERO);
        CameraShake.add(p.pos(), 2.2f, 10);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Observation
    // ---------------------------------------------------------------------------------------------------------

    private static void observationPulse(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 feet = base(source, p);
        float radius = Math.max(4f, p.scale());
        FxKit.groundRing(level, p.color(), radius * 0.25f, feet);
        FxKit.groundRing(level, 0xFFFFFF, radius * 0.12f, feet);
        if (source != null) {
            Vec3 eyes = source.getEyePosition();
            FxKit.particle(level, GLParticles.STAR, p.color(), 0.9f, eyes, Vec3.ZERO);
        }
        FxKit.radial(level, GLParticles.GLOW, p.color(), 0.4f, feet.add(0, 0.2, 0), 12, 0.25, 0.0);
    }

    private static void futureSight(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 eyes = source != null ? source.getEyePosition() : p.pos();
        FxKit.particle(level, GLParticles.STAR, 0xFFFFFF, 1.6f, eyes, Vec3.ZERO);
        FxKit.burst(level, GLParticles.STREAK, p.color(), 1.0f, eyes, 10, 0.35);
        if (source != null) {
            FxKit.groundRing(level, p.color(), 3.5f, source.position());
        }
        if (isLocal(source)) {
            HakiClient.flash(0.8f);
        }
    }

    /** An afterimage of the dodger where the blow was aimed, smeared away from the step. */
    private static void foreseenDodge(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 feet = p.pos();
        Vec3 step = p.dir();
        RandomSource r = level.random;
        double[][] body = {{0, 1.6}, {0, 1.35}, {0, 1.1}, {0, 0.85}, {0.12, 0.45}, {-0.12, 0.45}, {0.12, 0.15}, {-0.12, 0.15},
                {0.32, 1.15}, {-0.32, 1.15}};
        Vec3 side = new Vec3(-step.z, 0, step.x);
        for (double[] point : body) {
            Vec3 at = feet.add(side.scale(point[0])).add(0, point[1], 0);
            FxKit.particle(level, GLParticles.GLOW, p.color(), 0.55f, at, step.scale(-0.02));
        }
        for (int i = 0; i < 5; i++) {
            Vec3 at = feet.add(0, 0.3 + r.nextDouble() * 1.4, 0);
            FxKit.particle(level, GLParticles.STREAK, 0xFFFFFF, 1.1f, at, step.scale(0.5));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Conqueror's
    // ---------------------------------------------------------------------------------------------------------

    /** Gathering: arcs of black lightning snap off the body, dust and pebbles lift. scale = charge 0-1. */
    private static void conquerorCharge(ClientLevel level, @Nullable Entity source, FxParams p) {
        float charge = Mth.clamp(p.scale(), 0, 1);
        Vec3 feet = base(source, p);
        RandomSource r = level.random;
        int arcs = 1 + (int) (charge * 3);
        for (int i = 0; i < arcs; i++) {
            Vec3 start = feet.add((r.nextDouble() - 0.5) * 0.6, 0.4 + r.nextDouble() * 1.4, (r.nextDouble() - 0.5) * 0.6);
            Vec3 end = start.add(FxKit.randomUnit(r).scale(1.2 + charge * 1.8));
            ClientVisuals.add(new LightningVisual(() -> start, () -> end, CRIMSON, BLACK, 0.06f + 0.04f * charge, 4, 0.22f, false));
        }
        FxKit.radial(level, GLParticles.SMOKE, 0x4A3A3A, 0.6f + charge * 0.4f, feet.add(0, 0.1, 0), 6, 0.12 + charge * 0.12, 0.01);
        if (charge > 0.4f) {
            for (int i = 0; i < 3; i++) {
                Vec3 at = feet.add((r.nextDouble() - 0.5) * 4, 0.1, (r.nextDouble() - 0.5) * 4);
                FxKit.particle(level, GLParticles.SHARD, 0x5A5050, 0.6f, at, new Vec3(0, 0.15 + r.nextDouble() * 0.2, 0));
            }
        }
        CameraShake.add(feet, 0.2f + charge * 0.9f, 5);
    }

    /** Release: rolling rings, black bolts racing out across the ground, and a heavy quake. scale = radius. */
    private static void conquerorWave(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 feet = base(source, p);
        float radius = Math.max(4f, p.scale());
        RandomSource r = level.random;
        FxKit.groundRing(level, CRIMSON, radius * 0.35f, feet);
        FxKit.groundRing(level, 0x200006, radius * 0.5f, feet);
        FxKit.groundRing(level, CRIMSON, radius * 0.7f, feet);
        Vec3 chest = feet.add(0, 1.1, 0);
        FxKit.shockwave(level, CRIMSON, radius * 0.4f, chest, new Vec3(0, 1, 0));
        int bolts = 10;
        for (int i = 0; i < bolts; i++) {
            double a = Mth.TWO_PI * i / bolts + r.nextDouble() * 0.4;
            Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 start = chest.add(out.scale(0.4));
            Vec3 end = feet.add(out.scale(radius * (0.55 + r.nextDouble() * 0.35))).add(0, 0.2 + r.nextDouble() * 1.5, 0);
            ClientVisuals.add(new LightningVisual(() -> start, () -> end, CRIMSON, BLACK, 0.12f, 10 + r.nextInt(6), 0.16f, true));
        }
        for (int i = 0; i < 3; i++) {
            Vec3 start = chest.add(0, 0.5, 0);
            Vec3 end = start.add((r.nextDouble() - 0.5) * 4, 5 + r.nextDouble() * 4, (r.nextDouble() - 0.5) * 4);
            ClientVisuals.add(new LightningVisual(() -> start, () -> end, CRIMSON, BLACK, 0.1f, 12, 0.2f, true));
        }
        FxKit.radial(level, GLParticles.SMOKE, 0x3A3030, 1.2f, feet.add(0, 0.2, 0), 18, 0.5, 0.02);
        FxKit.burst(level, GLParticles.SPARK, CRIMSON, 1.0f, chest, 20, 0.6);
        CameraShake.add(feet, 3.5f, 22);
        Entity self = Minecraft.getInstance().player;
        if (self != null && self != source && self.position().distanceTo(feet) <= radius) {
            HakiClient.pressure(1f);
        } else if (isLocal(source)) {
            HakiClient.pressure(0.35f);
        }
    }

    /** Two wills collide: thick lightning between them and the air splits at the middle. */
    private static void clash(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 a = p.pos();
        Vec3 b = a.add(p.dir());
        Vec3 mid = a.add(b).scale(0.5);
        ClientVisuals.add(LightningVisual.between(a, b, CRIMSON, BLACK, 0.2f, 14));
        ClientVisuals.add(LightningVisual.between(a, b, CRIMSON, BLACK, 0.12f, 10));
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) {
            Vec3 end = mid.add(FxKit.randomUnit(r).scale(2.5 + r.nextDouble() * 3));
            ClientVisuals.add(new LightningVisual(() -> mid, () -> end, CRIMSON, BLACK, 0.1f, 8 + r.nextInt(6), 0.2f, false));
        }
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : new Vec3(1, 0, 0);
        FxKit.shockwave(level, CRIMSON, 3f, mid, dir);
        FxKit.shockwave(level, 0x200006, 4.5f, mid, dir);
        FxKit.burst(level, GLParticles.SPARK, CRIMSON, 1.2f, mid, 24, 0.7);
        FxKit.burst(level, GLParticles.SPARK, BLACK, 1.2f, mid, 16, 0.6);
        CameraShake.add(mid, 4f, 20);
        HakiClient.pressure(0.6f);
    }

    /** A Conqueror-infused blow: short arcs leap off the point of impact. */
    private static void strike(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 at = p.pos();
        RandomSource r = level.random;
        int arcs = p.scale() < 1f ? 2 : 4;
        for (int i = 0; i < arcs; i++) {
            Vec3 end = at.add(FxKit.randomUnit(r).scale(1.2 + r.nextDouble() * 1.4));
            ClientVisuals.add(new LightningVisual(() -> at, () -> end, CRIMSON, BLACK, 0.07f, 5, 0.25f, false));
        }
        FxKit.burst(level, GLParticles.SPARK, CRIMSON, 0.8f, at, 10, 0.45);
        FxKit.shockwave(level, CRIMSON, 1.2f, at, FxKit.randomUnit(r));
        CameraShake.add(at, 1.2f, 6);
    }

    private static void knockout(ClientLevel level, @Nullable Entity source, FxParams p) {
        if (source != null) {
            HakiClient.knockOut(source, Math.max(20, p.variant()));
        }
        Vec3 feet = base(source, p);
        FxKit.radial(level, GLParticles.SMOKE, 0xBFB8B0, 0.6f, feet.add(0, 0.1, 0), 8, 0.12, 0.02);
        FxKit.burst(level, GLParticles.STAR, 0xFFE68A, 0.7f, feet.add(0, Math.max(0.6, p.scale() * 0.5), 0), 5, 0.08);
    }
}
