package com.nerodev.grandline.client.fx;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client implementations of server-triggered effects ({@link GLEffects}). Each effect composes {@link FxKit}
 * primitives; counts are kept small so many players using abilities at once stays cheap.
 */
public final class ClientEffects {
    @FunctionalInterface
    public interface Effect {
        void play(ClientLevel level, @Nullable Entity source, FxParams params);
    }

    private static final Map<ResourceLocation, Effect> EFFECTS = new HashMap<>();

    static {
        register(GLEffects.EVADE, ClientEffects::evade);
        register(GLEffects.SORU_DASH, ClientEffects::soruDash);
        register(GLEffects.GEPPO_STEP, ClientEffects::geppoStep);
        register(GLEffects.TEKKAI_GUARD, ClientEffects::tekkaiGuard);
        register(GLEffects.TEKKAI_BLOCK, ClientEffects::tekkaiBlock);
    }

    private ClientEffects() {}

    public static void register(ResourceLocation id, Effect effect) {
        EFFECTS.put(id, effect);
    }

    public static void play(ResourceLocation id, int entityId, FxParams params) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Effect effect = EFFECTS.get(id);
        if (effect == null) {
            GrandLine.LOGGER.debug("No client effect registered for {}", id);
            return;
        }
        effect.play(level, entityId < 0 ? null : level.getEntity(entityId), params);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Effects
    // ---------------------------------------------------------------------------------------------------------

    private static void evade(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 center = p.pos().add(0, 1.0, 0);
        FxKit.burst(level, GLParticles.STREAK, 0xE8F0FF, 1.0f, center, 6, 0.6);
        FxKit.burst(level, GLParticles.SMOKE, 0xD0D6DE, 0.6f, center, 4, 0.05);
    }

    /** Soru: afterimage streaks along the path, a ground ring where you left, a puff where you land. */
    private static void soruDash(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 from = p.pos();
        Vec3 to = from.add(p.dir());
        Vec3 dir = p.dir().lengthSqr() > 1.0E-4 ? p.dir().normalize() : Vec3.ZERO;
        int color = p.color();

        FxKit.groundRing(level, color, 1.6f, from);
        FxKit.radial(level, GLParticles.SMOKE, 0xBFC4CC, 0.7f, from.add(0, 0.1, 0), 10, 0.18, 0.02);
        for (double h : new double[] {0.4, 1.0, 1.5}) {
            FxKit.line(level, GLParticles.STREAK, color, 1.4f, from.add(0, h, 0), to.add(0, h, 0), 0.6, 0.0, 0.25);
        }
        FxKit.line(level, GLParticles.GLOW, color, 0.6f, from.add(0, 0.9, 0), to.add(0, 0.9, 0), 1.2, 0.02, 0.3);
        FxKit.burst(level, GLParticles.SMOKE, 0xD8DCE4, 0.5f, to.add(0, 0.3, 0), 5, 0.08);
        FxKit.shockwave(level, color, 1.2f, to.add(0, 1.0, 0), dir);
        CameraShake.add(from, 0.6f, 4);
    }

    /** Geppo: a ring of compressed air under the feet and puffs pushed down and out. */
    private static void geppoStep(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 feet = p.pos();
        FxKit.particle(level, GLParticles.RING, p.color(), 1.3f, feet, Vec3.ZERO);
        FxKit.particle(level, GLParticles.RING, p.color(), 0.8f, feet.add(0, -0.25, 0), Vec3.ZERO);
        FxKit.radial(level, GLParticles.SMOKE, 0xF0F6FF, 0.45f, feet, 8, 0.15, -0.06);
        FxKit.radial(level, GLParticles.STREAK, p.color(), 0.8f, feet, 6, 0.35, -0.1);
    }

    /** Tekkai: steel glints flicker over the body. Variant 0 = hardening, 1 = sustained shimmer. */
    private static void tekkaiGuard(ClientLevel level, @Nullable Entity source, FxParams p) {
        Vec3 base = source != null ? source.position() : p.pos();
        int count = p.variant() == 0 ? 10 : 3;
        for (int i = 0; i < count; i++) {
            Vec3 at = base.add((level.random.nextDouble() - 0.5) * 0.8, 0.2 + level.random.nextDouble() * 1.6,
                    (level.random.nextDouble() - 0.5) * 0.8);
            FxKit.particle(level, GLParticles.STAR, p.color(), 0.9f, at, Vec3.ZERO);
        }
        if (p.variant() == 0) {
            FxKit.groundRing(level, 0x9AA4AE, 1.1f, base);
        }
    }

    /** Tekkai block: sparks off steel skin and a small jolt. */
    private static void tekkaiBlock(ClientLevel level, @Nullable Entity source, FxParams p) {
        FxKit.burst(level, GLParticles.SPARK, 0xFFE2A8, 1.0f, p.pos(), 14, 0.45);
        FxKit.burst(level, GLParticles.STAR, p.color(), 1.2f, p.pos(), 3, 0.05);
        CameraShake.add(p.pos(), 1.2f, 5);
    }
}
