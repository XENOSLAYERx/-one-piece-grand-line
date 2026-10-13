package com.nerodev.grandline.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * Parameters for a client effect. What each field means is up to the effect; most use pos as the origin, dir as
 * the facing/travel direction, scale as size, color as an RGB tint and variant as a sub-type or count.
 */
public record FxParams(Vec3 pos, Vec3 dir, float scale, int color, int variant) {
    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    public static final StreamCodec<ByteBuf, FxParams> STREAM_CODEC = StreamCodec.composite(
            VEC3, FxParams::pos,
            VEC3, FxParams::dir,
            ByteBufCodecs.FLOAT, FxParams::scale,
            ByteBufCodecs.INT, FxParams::color,
            ByteBufCodecs.VAR_INT, FxParams::variant,
            FxParams::new);

    public static FxParams at(Vec3 pos) {
        return new FxParams(pos, Vec3.ZERO, 1f, 0xFFFFFF, 0);
    }

    public static FxParams of(Vec3 pos, Vec3 dir) {
        return new FxParams(pos, dir, 1f, 0xFFFFFF, 0);
    }

    public FxParams scale(float scale) {
        return new FxParams(pos, dir, scale, color, variant);
    }

    public FxParams color(int color) {
        return new FxParams(pos, dir, scale, color, variant);
    }

    public FxParams variant(int variant) {
        return new FxParams(pos, dir, scale, color, variant);
    }
}
