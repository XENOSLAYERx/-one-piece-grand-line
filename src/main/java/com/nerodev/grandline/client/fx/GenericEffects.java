package com.nerodev.grandline.client.fx;

import com.nerodev.grandline.client.visual.ClientVisuals;
import com.nerodev.grandline.client.visual.LightningVisual;
import com.nerodev.grandline.client.visual.StatusVisuals;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticleOptions;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client side of {@link Vfx}: generic shapes (beam, burst, trail, blink, eruption, rain, slash, spiral, impact,
 * status) drawn with whatever particle kind and colour the server picked.
 */
public final class GenericEffects {
    private GenericEffects() {}

    public static void register() {
        ClientEffects.register(GLEffects.G_BEAM, GenericEffects::beam);
        ClientEffects.register(GLEffects.G_BURST, GenericEffects::burst);
        ClientEffects.register(GLEffects.G_TRAIL, GenericEffects::trail);
        ClientEffects.register(GLEffects.G_BLINK, GenericEffects::blink);
        ClientEffects.register(GLEffects.G_ERUPTION, GenericEffects::eruption);
        ClientEffects.register(GLEffects.G_RAIN, GenericEffects::rain);
        ClientEffects.register(GLEffects.G_SLASH, GenericEffects::slash);
        ClientEffects.register(GLEffects.G_SPIRAL, GenericEffects::spiral);
        ClientEffects.register(GLEffects.G_IMPACT, GenericEffects::impact);
        ClientEffects.register(GLEffects.G_STATUS, GenericEffects::status);
    }

    static Kind kind(FxParams p) {
        int index = p.variant() & 0xFF;
        Kind[] kinds = Kind.values();
        return index < kinds.length ? kinds[index] : Kind.GLOW;
    }

    static int style(FxParams p) {
        return p.variant() >> 8;
    }

    static Supplier<ParticleType<GLParticleOptions>> type(Kind kind) {
        return GLParticles.byKind(kind);
    }

    /** Particles that read better a little smaller or larger than the default. */
    static float baseScale(Kind kind) {
        return switch (kind) {
            case SPARK, STAR -> 1.0f;
            case SMOKE -> 0.8f;
            case FLAME -> 1.1f;
            case SNOW, LEAF, FEATHER, HEART, BUBBLE -> 1.2f;
            case DROP, SHARD -> 1.0f;
            default -> 0.9f;
        };
    }

    /** Kinds that should drift outward/upward on their own rather than be thrown. */
    static boolean gentle(Kind kind) {
        return kind == Kind.SNOW || kind == Kind.LEAF || kind == Kind.FEATHER || kind == Kind.HEART || kind == Kind.BUBBLE;
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void beam(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 from = p.pos();
        Vec3 to = from.add(p.dir());
        Kind kind = kind(p);
        float width = Math.max(0.1f, p.scale());
        int style = style(p);
        double length = p.dir().length();
        if (length < 0.01) {
            return;
        }
        Vec3 dir = p.dir().scale(1 / length);
        switch (style) {
            case Vfx.BEAM_BOLT -> {
                ClientVisuals.add(LightningVisual.between(from, to, p.color(), 0xFFFFFF, width * 0.25f, 6));
                FxKit.line(level, GLParticles.GLOW, p.color(), width * 0.8f, from, to, 1.2, 0.0, width * 0.3);
            }
            case Vfx.BEAM_DARK -> {
                FxKit.line(level, GLParticles.SMOKE, p.color(), width * 1.4f, from, to, 0.5, 0.02, width * 0.4);
                FxKit.line(level, type(kind), p.color(), width, from, to, 0.6, 0.05, width * 0.3);
            }
            case Vfx.BEAM_PARTICLES -> FxKit.line(level, type(kind), p.color(), width * baseScale(kind), from, to,
                    0.35, 0.08, width * 0.5);
            default -> {
                // Glowing core with a soft halo and sparks of the chosen kind along it.
                FxKit.line(level, GLParticles.GLOW, 0xFFFFFF, width * 0.7f, from, to, 0.4, 0.0, 0.02);
                FxKit.line(level, GLParticles.GLOW, p.color(), width * 1.6f, from, to, 0.5, 0.0, width * 0.15);
                if (kind != Kind.GLOW) {
                    FxKit.line(level, type(kind), p.color(), width * baseScale(kind), from, to, 1.0, 0.04, width * 0.6);
                }
            }
        }
        FxKit.shockwave(level, p.color(), width * 1.6f, from.add(dir.scale(0.3)), dir);
        FxKit.burst(level, type(kind == Kind.GLOW ? Kind.SPARK : kind), p.color(), baseScale(kind), to, 8, 0.25);
    }

    private static void burst(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 at = p.pos();
        Kind kind = kind(p);
        int flags = style(p);
        float radius = Math.max(0.5f, p.scale());
        int count = Mth.clamp((int) (10 + radius * 6), 10, 60);
        double speed = gentle(kind) ? 0.08 + radius * 0.02 : 0.15 + radius * 0.08;
        FxKit.burst(level, type(kind), p.color(), baseScale(kind) * (1f + radius * 0.08f), at, count, speed);
        FxKit.burst(level, GLParticles.GLOW, p.color(), 0.8f + radius * 0.15f, at, 3 + (int) radius, 0.05);
        if ((flags & Vfx.RING) != 0) {
            FxKit.groundRing(level, p.color(), radius * 0.6f, groundAt(level, at));
        }
        if ((flags & Vfx.SHOCKWAVE) != 0) {
            FxKit.shockwave(level, p.color(), radius * 0.7f, at, new Vec3(0, 1, 0));
            FxKit.radial(level, GLParticles.STREAK, p.color(), 1.2f, at, 12, 0.5 + radius * 0.05, 0.0);
        }
        if ((flags & Vfx.GROUND) != 0) {
            Vec3 ground = groundAt(level, at);
            FxKit.radial(level, GLParticles.SMOKE, 0xB0A898, 0.8f, ground.add(0, 0.1, 0), 12, 0.15 + radius * 0.04, 0.02);
            for (int i = 0; i < 6; i++) {
                Vec3 rock = ground.add((level.random.nextDouble() - 0.5) * radius, 0.2, (level.random.nextDouble() - 0.5) * radius);
                FxKit.particle(level, GLParticles.SHARD, 0x6A625A, 0.8f, rock, new Vec3(0, 0.25 + level.random.nextDouble() * 0.2, 0));
            }
        }
        if ((flags & Vfx.FLASH) != 0) {
            FxKit.particle(level, GLParticles.GLOW, 0xFFFFFF, radius * 1.4f, at, Vec3.ZERO);
        }
        float shake = (flags & Vfx.HEAVY) != 0 ? 1.5f + radius * 0.25f : 0.3f + radius * 0.08f;
        CameraShake.add(at, shake, (flags & Vfx.HEAVY) != 0 ? 14 : 6);
    }

    private static void trail(ClientLevel level, @Nullable Entity source, FxParams p) {
        Kind kind = kind(p);
        Vec3 from = p.pos();
        Vec3 to = from.add(p.dir());
        float size = Math.max(0.2f, p.scale());
        FxKit.line(level, type(kind), p.color(), baseScale(kind) * size, from, to, 0.45, gentle(kind) ? 0.0 : 0.03, size * 0.4);
        if (kind != Kind.STREAK && p.dir().lengthSqr() > 1) {
            FxKit.line(level, GLParticles.STREAK, p.color(), 1.2f * size, from.add(0, 0.5, 0), to.add(0, 0.5, 0), 0.8, 0.0, 0.2);
        }
    }

    private static void blink(ClientLevel level, @Nullable Entity source, FxParams p) {
        Kind kind = kind(p);
        Vec3 from = p.pos();
        Vec3 to = from.add(p.dir());
        for (Vec3 at : new Vec3[] {from, to}) {
            Vec3 body = at.add(0, 1, 0);
            FxKit.burst(level, type(kind), p.color(), baseScale(kind), body, 16, gentle(kind) ? 0.08 : 0.2);
            FxKit.particle(level, GLParticles.GLOW, p.color(), 2.0f, body, Vec3.ZERO);
            FxKit.groundRing(level, p.color(), 1.4f, at);
        }
        FxKit.line(level, GLParticles.STREAK, p.color(), 1.4f, from.add(0, 1, 0), to.add(0, 1, 0), 0.7, 0.0, 0.3);
        CameraShake.add(to, 0.5f, 4);
    }

    private static void eruption(ClientLevel level, @Nullable Entity source, FxParams p) {
        Kind kind = kind(p);
        Vec3 base = p.pos();
        float height = Math.max(1f, p.scale());
        RandomSource r = level.random;
        int count = Mth.clamp((int) (height * 6), 8, 48);
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double rad = r.nextDouble() * 0.6;
            Vec3 at = base.add(Math.cos(a) * rad, r.nextDouble() * 0.5, Math.sin(a) * rad);
            FxKit.particle(level, type(kind), p.color(), baseScale(kind) * 1.3f, at,
                    new Vec3(Math.cos(a) * 0.05, 0.25 + height * 0.06 * r.nextDouble(), Math.sin(a) * 0.05));
        }
        FxKit.groundRing(level, p.color(), 1.6f + height * 0.2f, base);
        FxKit.radial(level, GLParticles.SMOKE, 0xA09890, 0.7f, base.add(0, 0.1, 0), 8, 0.15, 0.03);
        CameraShake.add(base, 0.6f + height * 0.1f, 8);
    }

    private static void rain(ClientLevel level, @Nullable Entity source, FxParams p) {
        Kind kind = kind(p);
        float radius = Math.max(1f, p.scale());
        RandomSource r = level.random;
        int count = Mth.clamp((int) (radius * radius * 0.8f), 8, 80);
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double d = Math.sqrt(r.nextDouble()) * radius;
            Vec3 at = p.pos().add(Math.cos(a) * d, 3 + r.nextDouble() * 4, Math.sin(a) * d);
            Vec3 motion = gentle(kind) ? new Vec3(0, -0.05, 0) : new Vec3(0, -0.6, 0);
            FxKit.particle(level, type(kind), p.color(), baseScale(kind), at, motion);
        }
    }

    /** Crescent cuts: arcs of streaks perpendicular to the facing direction. variant style = number of cuts. */
    private static void slash(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : new Vec3(0, 0, 1);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(dir).normalize();
        int cuts = Math.max(1, style(p));
        float size = Math.max(0.5f, p.scale());
        RandomSource r = level.random;
        for (int c = 0; c < cuts; c++) {
            double tilt = (r.nextDouble() - 0.5) * 1.6;
            Vec3 a = side.scale(Math.cos(tilt)).add(up.scale(Math.sin(tilt)));
            Vec3 b = up.scale(Math.cos(tilt)).subtract(side.scale(Math.sin(tilt)));
            for (int i = 0; i <= 10; i++) {
                double t = (i / 10.0 - 0.5) * 2.2;
                Vec3 at = p.pos().add(a.scale(Math.sin(t) * size)).add(b.scale((Math.cos(t) - 0.6) * size * 0.5))
                        .add(dir.scale(c * 0.2));
                FxKit.particle(level, GLParticles.STREAK, p.color(), 1.3f, at, a.scale(Math.cos(t) * 0.25));
            }
        }
        FxKit.burst(level, GLParticles.SPARK, p.color(), 0.8f, p.pos(), 6, 0.3);
    }

    private static void spiral(ClientLevel level, @Nullable Entity source, FxParams p) {
        Kind kind = kind(p);
        float radius = Math.max(0.5f, p.scale());
        for (int i = 0; i < 36; i++) {
            double t = i / 36.0;
            double a = t * Mth.TWO_PI * 3;
            double r = radius * (1 - t * 0.6);
            Vec3 at = p.pos().add(Math.cos(a) * r, t * radius * 2.2, Math.sin(a) * r);
            Vec3 tangent = new Vec3(-Math.sin(a), 0.15, Math.cos(a)).scale(0.12 + radius * 0.02);
            FxKit.particle(level, type(kind), p.color(), baseScale(kind), at, tangent);
        }
    }

    private static void impact(ClientLevel level, @Nullable Entity source, FxParams p) {
        Kind kind = kind(p);
        float size = Math.max(0.3f, p.scale());
        FxKit.burst(level, type(kind), p.color(), baseScale(kind) * size, p.pos(), (int) (6 + size * 4), 0.2 * size);
        FxKit.shockwave(level, p.color(), size, p.pos(), FxKit.randomUnit(level.random));
        CameraShake.add(p.pos(), 0.3f * size, 4);
    }

    private static void status(ClientLevel level, @Nullable Entity source, FxParams p) {
        if (source == null) {
            return;
        }
        Vfx.StatusLook[] looks = Vfx.StatusLook.values();
        Vfx.StatusLook look = p.variant() >= 0 && p.variant() < looks.length ? looks[p.variant()] : Vfx.StatusLook.ICE;
        StatusVisuals.apply(source, look, (int) p.scale());
        Vec3 center = source.position().add(0, source.getBbHeight() * 0.5, 0);
        Kind puff = switch (look) {
            case ICE, SNOW -> Kind.SNOW;
            case SMOKE -> Kind.SMOKE;
            case STONE, SAND -> Kind.SHARD;
            case CANDY -> Kind.HEART;
            case GOLD -> Kind.STAR;
            case ROOTS -> Kind.LEAF;
            case GLUE, WAX, MOCHI, MUD -> Kind.DROP;
            default -> Kind.SPARK;
        };
        FxKit.burst(level, type(puff), look.color(), 1f, center, 12, 0.15);
    }

    /** The first solid surface at or below {@code at} (within 6 blocks), so rings sit on the ground. */
    static Vec3 groundAt(ClientLevel level, Vec3 at) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = net.minecraft.core.BlockPos.containing(at).mutable();
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(pos.below()).isAir()) {
                return new Vec3(at.x, pos.getY(), at.z);
            }
            pos.move(0, -1, 0);
        }
        return at;
    }
}
