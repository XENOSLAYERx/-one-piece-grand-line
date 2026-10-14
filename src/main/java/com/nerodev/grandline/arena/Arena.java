package com.nerodev.grandline.arena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** A battle arena a creator has marked out: a name, the dimension and centre, and how far it reaches. */
public record Arena(String name, ResourceKey<Level> dimension, BlockPos centre, int radius) {
    public static final Codec<Arena> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Arena::name),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Arena::dimension),
            BlockPos.CODEC.fieldOf("centre").forGetter(Arena::centre),
            Codec.INT.fieldOf("radius").forGetter(Arena::radius)
    ).apply(i, Arena::new));

    public boolean contains(Level level, double x, double z, double margin) {
        if (level.dimension() != dimension) {
            return false;
        }
        double dx = x - (centre.getX() + 0.5);
        double dz = z - (centre.getZ() + 0.5);
        return dx * dx + dz * dz <= (radius + margin) * (radius + margin);
    }
}
