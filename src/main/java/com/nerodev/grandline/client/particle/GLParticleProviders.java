package com.nerodev.grandline.client.particle;

import com.nerodev.grandline.fx.GLParticleOptions;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.fx.GLParticles.Kind;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Hooks every particle type up to {@link GLParticle}. */
public final class GLParticleProviders {
    private GLParticleProviders() {}

    public static void register(RegisterParticleProvidersEvent event) {
        add(event, GLParticles.SPARK, Kind.SPARK);
        add(event, GLParticles.GLOW, Kind.GLOW);
        add(event, GLParticles.FLAME, Kind.FLAME);
        add(event, GLParticles.SMOKE, Kind.SMOKE);
        add(event, GLParticles.RING, Kind.RING);
        add(event, GLParticles.SHOCKWAVE, Kind.SHOCKWAVE);
        add(event, GLParticles.STAR, Kind.STAR);
        add(event, GLParticles.SHARD, Kind.SHARD);
        add(event, GLParticles.STREAK, Kind.STREAK);
        add(event, GLParticles.BUBBLE, Kind.BUBBLE);
        add(event, GLParticles.SNOW, Kind.SNOW);
        add(event, GLParticles.LEAF, Kind.LEAF);
        add(event, GLParticles.HEART, Kind.HEART);
        add(event, GLParticles.FEATHER, Kind.FEATHER);
        add(event, GLParticles.DROP, Kind.DROP);
    }

    private static void add(RegisterParticleProvidersEvent event, Supplier<ParticleType<GLParticleOptions>> type, Kind kind) {
        event.registerSpriteSet(type.get(), sprites -> (options, level, x, y, z, dx, dy, dz) ->
                new GLParticle(level, x, y, z, dx, dy, dz, options, kind, sprites));
    }
}
