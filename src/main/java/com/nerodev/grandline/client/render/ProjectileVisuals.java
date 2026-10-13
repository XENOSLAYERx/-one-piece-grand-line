package com.nerodev.grandline.client.render;

import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fruit.flame.FlameAbilities;
import com.nerodev.grandline.fx.GLParticles;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Client look of each projectile kind: whether it draws a glowing core (and how big relative to its size) and
 * which particles it sheds every tick.
 */
public final class ProjectileVisuals {
    public record Visual(float coreScale, int coreTint, Trail trail) {}

    @FunctionalInterface
    public interface Trail {
        void tick(ClientLevel level, AbilityProjectile projectile, RandomSource random);
    }

    private static final Map<ResourceLocation, Visual> VISUALS = new HashMap<>();

    static {
        register(FlameAbilities.FIRE_FIST_SHOT, new Visual(1.1f, 0xFFE08A, ProjectileVisuals::fireTrail));
        register(FlameAbilities.FIRE_BULLET, new Visual(0.9f, 0xFFE08A, (level, p, r) -> {
            FxKit.particle(level, GLParticles.FLAME, p.color(), 0.6f, p.position(), Vec3.ZERO);
            if (r.nextInt(2) == 0) {
                FxKit.particle(level, GLParticles.SPARK, 0xFFD27A, 0.8f, p.position(), FxKit.randomUnit(r).scale(0.05));
            }
        }));
        register(FlameAbilities.ENTEI, new Visual(1.0f, 0xFFF0B0, ProjectileVisuals::enteiTrail));
        register(FlameAbilities.FIREFLY, new Visual(1.6f, 0xEFFFC0, (level, p, r) -> {
            if (r.nextInt(6) == 0) {
                FxKit.particle(level, GLParticles.STAR, p.color(), 0.5f, p.position(), Vec3.ZERO);
            }
        }));
        register(FlameAbilities.FLAME_WALL, new Visual(0f, 0, ProjectileVisuals::wallTrail));
        register(FlameAbilities.PILLAR, new Visual(0f, 0, ProjectileVisuals::pillarTrail));
    }

    private ProjectileVisuals() {}

    public static void register(ResourceLocation kind, Visual visual) {
        VISUALS.put(kind, visual);
    }

    @Nullable
    public static Visual get(ResourceLocation kind) {
        return VISUALS.get(kind);
    }

    /** Installed as {@link AbilityProjectile#clientTick}. */
    public static void tick(AbilityProjectile projectile) {
        Visual visual = VISUALS.get(projectile.kind());
        if (visual != null && projectile.level() instanceof ClientLevel level) {
            visual.trail().tick(level, projectile, level.random);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void fireTrail(ClientLevel level, AbilityProjectile p, RandomSource r) {
        Vec3 pos = p.position().add(0, p.getBbHeight() / 2, 0);
        Vec3 back = p.getDeltaMovement().scale(-0.12);
        float size = p.size();
        int count = 3 + (int) (size * 4);
        for (int i = 0; i < count; i++) {
            Vec3 offset = FxKit.randomUnit(r).scale(size * 0.45 * r.nextFloat());
            FxKit.particle(level, GLParticles.FLAME, p.color(), 0.9f + size * 0.6f, pos.add(offset),
                    back.add(FxKit.randomUnit(r).scale(0.03)));
        }
        if (r.nextInt(2) == 0) {
            FxKit.particle(level, GLParticles.SMOKE, 0x3A3030, 0.8f + size * 0.5f, pos, back.scale(0.5).add(0, 0.03, 0));
        }
        FxKit.particle(level, GLParticles.SPARK, 0xFFD27A, 1.0f, pos, FxKit.randomUnit(r).scale(0.15));
    }

    private static void enteiTrail(ClientLevel level, AbilityProjectile p, RandomSource r) {
        Vec3 pos = p.position().add(0, p.getBbHeight() / 2, 0);
        float size = p.size();
        // Many mid-sized flames read as one big fireball; a few huge sprites would look blocky.
        int count = 8 + (int) (size * 2.5f);
        for (int i = 0; i < count; i++) {
            Vec3 dir = FxKit.randomUnit(r);
            FxKit.particle(level, GLParticles.FLAME, p.color(), Math.min(2.4f, 1.2f + size * 0.25f),
                    pos.add(dir.scale(size * 0.5 * Math.cbrt(r.nextDouble()))),
                    dir.scale(0.06).add(p.getDeltaMovement().scale(-0.2)));
        }
        FxKit.particle(level, GLParticles.SMOKE, 0x2A2222, 1.5f + size * 0.6f, pos.add(FxKit.randomUnit(r).scale(size * 0.5)),
                new Vec3(0, 0.05, 0));
        if (r.nextInt(3) == 0) {
            FxKit.particle(level, GLParticles.SHOCKWAVE, 0xFFB050, size * 0.9f, pos, p.getDeltaMovement());
        }
    }

    private static void wallTrail(ClientLevel level, AbilityProjectile p, RandomSource r) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 along = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        double half = p.size() / 2;
        for (int i = 0; i < 9; i++) {
            double t = (r.nextDouble() * 2 - 1) * half;
            Vec3 at = p.position().add(along.scale(t)).add(0, r.nextDouble() * 0.6, 0);
            FxKit.particle(level, GLParticles.FLAME, p.color(), 1.6f, at, new Vec3(0, 0.12 + r.nextDouble() * 0.12, 0));
        }
        if (r.nextInt(2) == 0) {
            double t = (r.nextDouble() * 2 - 1) * half;
            FxKit.particle(level, GLParticles.SMOKE, 0x352A2A, 1.4f, p.position().add(along.scale(t)).add(0, 2.5, 0),
                    new Vec3(0, 0.05, 0));
        }
    }

    private static void pillarTrail(ClientLevel level, AbilityProjectile p, RandomSource r) {
        Vec3 base = p.position();
        float radius = p.size();
        if (p.tickCount < 12) {
            // Warning: a ring of embers where the pillar will erupt.
            double a = r.nextDouble() * Mth.TWO_PI;
            Vec3 at = base.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
            FxKit.particle(level, GLParticles.GLOW, 0xFF8030, 0.8f, at, new Vec3(0, 0.02, 0));
            if (p.tickCount % 4 == 0) {
                FxKit.groundRing(level, 0xFF6A1A, radius * 2.2f, base);
            }
            return;
        }
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Mth.TWO_PI;
            double d = r.nextDouble() * radius;
            Vec3 at = base.add(Math.cos(a) * d, r.nextDouble() * 0.5, Math.sin(a) * d);
            FxKit.particle(level, GLParticles.FLAME, p.color(), 2.0f, at, new Vec3(0, 0.45 + r.nextDouble() * 0.35, 0));
        }
    }
}
