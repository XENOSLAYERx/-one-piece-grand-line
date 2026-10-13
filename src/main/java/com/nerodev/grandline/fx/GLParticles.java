package com.nerodev.grandline.fx;

import com.mojang.serialization.MapCodec;
import com.nerodev.grandline.GrandLine;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle types. All share {@link GLParticleOptions}; the client gives each kind its own motion and shape.
 */
public final class GLParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, GrandLine.MODID);

    /** Behaviour of each particle kind, interpreted by the client particle class. */
    public enum Kind {
        /** Small hot point that flies out and shrinks. */
        SPARK,
        /** Soft glowing orb that drifts and fades. */
        GLOW,
        /** Animated flame lick that rises. */
        FLAME,
        /** Lit (non-glowing) smoke puff that grows and fades. */
        SMOKE,
        /** Flat horizontal ring that expands (ground shockwaves). */
        RING,
        /** Ring facing along the particle's velocity vector, which is not used as motion (air blasts). */
        SHOCKWAVE,
        /** Twinkling four-point glint. */
        STAR,
        /** Heavy tumbling fragment with gravity (rubble, ice, metal). */
        SHARD,
        /** Motion line aligned with its velocity. */
        STREAK;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Supplier<ParticleType<GLParticleOptions>> SPARK = register(Kind.SPARK);
    public static final Supplier<ParticleType<GLParticleOptions>> GLOW = register(Kind.GLOW);
    public static final Supplier<ParticleType<GLParticleOptions>> FLAME = register(Kind.FLAME);
    public static final Supplier<ParticleType<GLParticleOptions>> SMOKE = register(Kind.SMOKE);
    public static final Supplier<ParticleType<GLParticleOptions>> RING = register(Kind.RING);
    public static final Supplier<ParticleType<GLParticleOptions>> SHOCKWAVE = register(Kind.SHOCKWAVE);
    public static final Supplier<ParticleType<GLParticleOptions>> STAR = register(Kind.STAR);
    public static final Supplier<ParticleType<GLParticleOptions>> SHARD = register(Kind.SHARD);
    public static final Supplier<ParticleType<GLParticleOptions>> STREAK = register(Kind.STREAK);

    private GLParticles() {}

    private static Supplier<ParticleType<GLParticleOptions>> register(Kind kind) {
        return TYPES.register(kind.id(), () -> new ParticleType<GLParticleOptions>(false) {
            private final MapCodec<GLParticleOptions> codec = GLParticleOptions.codec(this);
            private final StreamCodec<RegistryFriendlyByteBuf, GLParticleOptions> streamCodec = GLParticleOptions.streamCodec(this);

            @Override
            public MapCodec<GLParticleOptions> codec() {
                return codec;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, GLParticleOptions> streamCodec() {
                return streamCodec;
            }
        });
    }

    public static GLParticleOptions options(Supplier<ParticleType<GLParticleOptions>> type, int color, float scale) {
        return new GLParticleOptions(type.get(), color, scale, 0);
    }

    public static GLParticleOptions options(Supplier<ParticleType<GLParticleOptions>> type, int color, float scale, int lifetime) {
        return new GLParticleOptions(type.get(), color, scale, lifetime);
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
