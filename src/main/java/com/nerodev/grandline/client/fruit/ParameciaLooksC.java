package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.bubbles;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparkle;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.tornado;
import static com.nerodev.grandline.client.fruit.LogiaLooks.wave;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.paramecia.ArmsFruit;
import com.nerodev.grandline.fruit.paramecia.ArtFruit;
import com.nerodev.grandline.fruit.paramecia.FloatFruit;
import com.nerodev.grandline.fruit.paramecia.HormoneFruit;
import com.nerodev.grandline.fruit.paramecia.MarkFruit;
import com.nerodev.grandline.fruit.paramecia.RippleFruit;
import com.nerodev.grandline.fruit.paramecia.SnipFruit;
import com.nerodev.grandline.fruit.paramecia.SpinFruit;
import com.nerodev.grandline.fruit.paramecia.StickFruit;
import com.nerodev.grandline.fruit.paramecia.StitchFruit;
import com.nerodev.grandline.fruit.paramecia.VenomFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of Paramecia batch C look on the client. */
public final class ParameciaLooksC {
    private ParameciaLooksC() {}

    public static void install() {
        // Venom: dark purple poison, never a bright core.
        register(VenomFruit.DRAGON, both(particles(0f, 0, Kind.SMOKE, 1.6f, 5), sparse(Kind.DROP, VenomFruit.TOXIC, 1.2f, 1)));
        register(VenomFruit.BALL, both(particles(0.6f, VenomFruit.TOXIC, Kind.SMOKE, 1.0f, 2), sparse(Kind.BUBBLE, VenomFruit.TOXIC, 1f, 3)));
        register(VenomFruit.CLOUD, both(zone(Kind.SMOKE, 2.4f, 0.2f, false), bubbles(VenomFruit.TOXIC, 0.02f)));
        register(VenomFruit.FLOOD, both(wave(Kind.SMOKE, 2.6f, 14), wave(Kind.DROP, 1.4f, 10, VenomFruit.TOXIC)));

        register(HormoneFruit.NEEDLE, particles(0.6f, HormoneFruit.COLOR, Kind.STREAK, 0.4f, 1));
        register(SnipFruit.SHEET, both(particles(0f, 0, Kind.SHARD, 1.2f, 3), sparse(Kind.SPARK, 0xFFFFFF, 0.8f, 2)));

        register(FloatFruit.ROCK, both(particles(0f, 0, Kind.SHARD, 1.4f, 3), sparse(Kind.SMOKE, FloatFruit.EARTH, 1.4f, 2)));
        register(FloatFruit.LION, both(particles(0f, 0, Kind.SHARD, 1.6f, 4), sparse(Kind.SMOKE, FloatFruit.EARTH, 2.0f, 1)));

        register(MarkFruit.KNIFE, both(particles(0.4f, 0xFFFFFF, Kind.STREAK, 0.5f, 1), sparse(Kind.SPARK, MarkFruit.COLOR, 0.6f, 3)));
        register(MarkFruit.ANCHOR, both(particles(0f, 0, Kind.SHARD, 1.2f, 2), sparse(Kind.SMOKE, 0x5A5A5A, 1.6f, 2)));

        register(ArmsFruit.BULLET, particles(0.5f, 0xFFF0C0, Kind.STREAK, 0.4f, 1));
        register(ArmsFruit.ROCKET, both(particles(0.8f, 0xFFD080, Kind.FLAME, 0.8f, 2), sparse(Kind.SMOKE, 0x4A4A4A, 1.2f, 1)));
        register(ArmsFruit.SHELL, both(particles(0.6f, 0xFFD080, Kind.SMOKE, 1.4f, 3), sparse(Kind.SPARK, ArmsFruit.BRASS, 1f, 1)));

        register(SpinFruit.TWISTER_KIND, tornado(Kind.SMOKE, 1.4f, 16, Kind.SHARD));

        register(StickFruit.GLOB, particles(0.5f, StickFruit.COLOR, Kind.DROP, 1.0f, 2));
        register(StickFruit.POOL, both(zone(Kind.DROP, 0.9f, 0.05f, true), bubbles(StickFruit.COLOR, 0.03f)));

        register(RippleFruit.WAVES, both(zone(Kind.SHARD, 1.0f, 0.03f, true), rising(Kind.SPARK, RippleFruit.COLOR, 0.6f, 0.02f)));

        register(StitchFruit.NEEDLE, particles(0.4f, 0xFFFFFF, Kind.STREAK, 0.4f, 1));

        register(ArtFruit.PAINT, particles(0.8f, 0xFFFFFF, Kind.DROP, 1.0f, 2));
        register(ArtFruit.CANVAS_ZONE, both(zone(Kind.STAR, 1.0f, 0.06f, true), sparkle(Kind.STAR, ArtFruit.PINK, 1.0f, 1)));
    }
}
