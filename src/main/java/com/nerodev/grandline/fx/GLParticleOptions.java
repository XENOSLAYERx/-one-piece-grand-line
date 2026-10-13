package com.nerodev.grandline.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Options shared by every Grand Line particle: RGB tint, size multiplier and lifetime (0 = the particle's default).
 * The particle type itself decides the behaviour (spark, flame, ring...).
 */
public record GLParticleOptions(ParticleType<GLParticleOptions> type, int color, float scale, int lifetime)
        implements ParticleOptions {

    public static MapCodec<GLParticleOptions> codec(ParticleType<GLParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(GLParticleOptions::color),
                Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(GLParticleOptions::scale),
                Codec.INT.optionalFieldOf("lifetime", 0).forGetter(GLParticleOptions::lifetime)
        ).apply(i, (color, scale, lifetime) -> new GLParticleOptions(type, color, scale, lifetime)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, GLParticleOptions> streamCodec(ParticleType<GLParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, GLParticleOptions::color,
                ByteBufCodecs.FLOAT, GLParticleOptions::scale,
                ByteBufCodecs.VAR_INT, GLParticleOptions::lifetime,
                (color, scale, lifetime) -> new GLParticleOptions(type, color, scale, lifetime));
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }
}
