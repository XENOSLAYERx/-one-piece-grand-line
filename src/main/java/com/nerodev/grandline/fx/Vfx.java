package com.nerodev.grandline.fx;

import com.nerodev.grandline.fx.GLParticles.Kind;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side shortcuts for the generic effects every fruit can compose. Each call is one small packet; clients
 * build the look from the colour, size and particle kind, so two fruits using the same shape still look different
 * (ice shards vs. sand vs. bubbles vs. light).
 * <p>
 * Encoding: {@code variant = kind.ordinal() | style << 8}.
 */
public final class Vfx {
    /** Beam styles. */
    public static final int BEAM_GLOW = 0;
    public static final int BEAM_PARTICLES = 1;
    public static final int BEAM_BOLT = 2;
    public static final int BEAM_DARK = 3;

    /** Burst flags (combine with |). */
    public static final int RING = 1;
    public static final int SHOCKWAVE = 2;
    public static final int HEAVY = 4;
    public static final int GROUND = 8;
    public static final int FLASH = 16;

    private Vfx() {}

    private static int encode(Kind kind, int style) {
        return kind.ordinal() | (style << 8);
    }

    /** A beam or stream from {@code from} to {@code to}. */
    public static void beam(ServerLevel level, @Nullable Entity source, Vec3 from, Vec3 to, int color, float width, Kind kind,
            int style) {
        Fx.play(level, GLEffects.G_BEAM, source, new FxParams(from, to.subtract(from), width, color, encode(kind, style)));
    }

    /** An explosion of particles of {@code kind} with optional rings, shockwave, flash and screen shake. */
    public static void burst(ServerLevel level, @Nullable Entity source, Vec3 at, int color, float radius, Kind kind, int flags) {
        Fx.play(level, GLEffects.G_BURST, source, new FxParams(at, Vec3.ZERO, radius, color, encode(kind, flags)));
    }

    /** Particles laid along a path (dashes, thrown things, ground cracks). */
    public static void trail(ServerLevel level, @Nullable Entity source, Vec3 from, Vec3 to, int color, float size, Kind kind) {
        Fx.play(level, GLEffects.G_TRAIL, source, new FxParams(from, to.subtract(from), size, color, encode(kind, 0)));
    }

    /** Vanishing at {@code from} and appearing at {@code to}. */
    public static void blink(ServerLevel level, @Nullable Entity source, Vec3 from, Vec3 to, int color, Kind kind) {
        Fx.play(level, GLEffects.G_BLINK, source, new FxParams(from, to.subtract(from), 1f, color, encode(kind, 0)));
    }

    /** A column bursting up from the ground at {@code base}. */
    public static void eruption(ServerLevel level, @Nullable Entity source, Vec3 base, int color, float height, Kind kind) {
        Fx.play(level, GLEffects.G_ERUPTION, source, new FxParams(base, Vec3.ZERO, height, color, encode(kind, 0)));
    }

    /** Particles falling over a circular area (snowfall, ash, meteor sparks, petals). */
    public static void rain(ServerLevel level, @Nullable Entity source, Vec3 center, int color, float radius, Kind kind) {
        Fx.play(level, GLEffects.G_RAIN, source, new FxParams(center, Vec3.ZERO, radius, color, encode(kind, 0)));
    }

    /** {@code count} crescent cuts facing {@code dir}. */
    public static void slash(ServerLevel level, @Nullable Entity source, Vec3 at, Vec3 dir, int color, float size, int count) {
        Fx.play(level, GLEffects.G_SLASH, source, new FxParams(at, dir, size, color, encode(Kind.STREAK, count)));
    }

    /** A rising vortex around {@code center}. */
    public static void spiral(ServerLevel level, @Nullable Entity source, Vec3 center, int color, float radius, Kind kind) {
        Fx.play(level, GLEffects.G_SPIRAL, source, new FxParams(center, Vec3.ZERO, radius, color, encode(kind, 0)));
    }

    /** A small hit spark. */
    public static void impact(ServerLevel level, @Nullable Entity source, Vec3 at, int color, float size, Kind kind) {
        Fx.play(level, GLEffects.G_IMPACT, source, new FxParams(at, Vec3.ZERO, size, color, encode(kind, 0)));
    }

    /** Tells clients that {@code target} is encased or bound for {@code ticks} (see {@link StatusLook}). */
    public static void status(ServerLevel level, Entity target, StatusLook look, int ticks) {
        Fx.play(level, GLEffects.G_STATUS, target, new FxParams(target.position(), Vec3.ZERO, ticks, look.color(), look.ordinal()));
    }

    /** How an encased or bound entity looks on clients. */
    public enum StatusLook {
        ICE(0x9FE2FF, 0.55f, true),
        STONE(0x8A8A82, 1f, true),
        WAX(0xF2EEE2, 1f, true),
        CANDY(0xF06A9A, 0.7f, true),
        GOLD(0xF0C030, 1f, true),
        MOCHI(0xF4ECE0, 0.95f, true),
        SAND(0xD8B46A, 1f, true),
        SNOW(0xF4F8FF, 0.95f, true),
        SMOKE(0xD8DCE0, 0.75f, true),
        MUD(0x5A4A2A, 1f, true),
        IRON_BANDS(0x8A9098, 1f, false),
        STRINGS(0xF4F4F4, 1f, false),
        ROOTS(0x6A4A2A, 1f, false),
        GLUE(0xA8C83A, 0.8f, false),
        HANDS(0xE8B898, 1f, false),
        BISCUIT(0xD8A060, 1f, true),
        SOAP(0xE8F4FF, 0.45f, true),
        CREAM(0xFFF4E8, 0.9f, true),
        BARRIER(0xBFE8FF, 0.35f, true),
        STITCHES(0x3A2A20, 1f, false),
        COILS(0x4A3A5A, 1f, false);

        private final int color;
        private final float alpha;
        private final boolean shell;

        StatusLook(int color, float alpha, boolean shell) {
            this.color = color;
            this.alpha = alpha;
            this.shell = shell;
        }

        public int color() {
            return color;
        }

        public float alpha() {
            return alpha;
        }

        /** True: a box around the whole body. False: bands around the legs. */
        public boolean shell() {
            return shell;
        }
    }
}
