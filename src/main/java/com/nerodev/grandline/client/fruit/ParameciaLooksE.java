package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.bubbles;
import static com.nerodev.grandline.client.fruit.LogiaLooks.falling;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparkle;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.swirl;
import static com.nerodev.grandline.client.fruit.LogiaLooks.vents;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.paramecia.CastleFruit;
import com.nerodev.grandline.fruit.paramecia.GoldFruit;
import com.nerodev.grandline.fruit.paramecia.GravityFruit;
import com.nerodev.grandline.fruit.paramecia.HeatFruit;
import com.nerodev.grandline.fruit.paramecia.MagnetFruit;
import com.nerodev.grandline.fruit.paramecia.MilletFruit;
import com.nerodev.grandline.fruit.paramecia.RipeFruit;
import com.nerodev.grandline.fruit.paramecia.SongFruit;
import com.nerodev.grandline.fruit.paramecia.SparkleFruit;
import com.nerodev.grandline.fruit.paramecia.TimeFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of Paramecia batch E look on the client. */
public final class ParameciaLooksE {
    private ParameciaLooksE() {}

    public static void install() {
        // Heat: the ground steams and glows.
        register(HeatFruit.SEA, both(vents(Kind.SMOKE, 0xFFFFFF, 1.4f, 3), zone(Kind.GLOW, 0.6f, 0.02f, true)));

        // Castle: iron cannonballs trailing smoke; a bombarded square full of blasts.
        register(CastleFruit.CANNONBALL, both(particles(0.5f, CastleFruit.IRON, Kind.SMOKE, 0.6f, 1), sparse(Kind.SPARK, 0xFFB060, 0.5f, 3)));
        register(CastleFruit.SIEGE, both(particles(0.7f, CastleFruit.IRON, Kind.SMOKE, 1.2f, 3), sparse(Kind.FLAME, 0xFFB060, 0.8f, 2)));
        register(CastleFruit.BARRAGE, both(falling(Kind.SMOKE, CastleFruit.IRON, 1.0f, 3, 8), zone(Kind.FLAME, 1.0f, 0.02f, true)));

        // Sparkle: glinting diamond.
        register(SparkleFruit.SHARD, both(particles(0.4f, SparkleFruit.COLOR, Kind.SHARD, 0.5f, 1), sparse(Kind.STAR, 0xFFFFFF, 0.4f, 2)));
        register(SparkleFruit.BOULDER, both(particles(1.0f, SparkleFruit.BLUE, Kind.SHARD, 1.4f, 3), sparkle(Kind.STAR, 0xFFFFFF, 1.0f, 1)));

        register(TimeFruit.STILL, both(zone(Kind.RING, 0.8f, 0.015f, true), swirl(Kind.STAR, 0.5f, 2, -1, TimeFruit.COLOR)));

        register(RipeFruit.GROUND, both(zone(Kind.SMOKE, 1.0f, 0.03f, true), rising(Kind.LEAF, RipeFruit.ROT, 0.6f, 0.02f)));
        register(RipeFruit.FIELD, both(zone(Kind.LEAF, 0.7f, 0.02f, true), rising(Kind.SMOKE, RipeFruit.ROT, 0.9f, 0.015f)));

        register(MilletFruit.DUMPLING, particles(0.4f, MilletFruit.COLOR, Kind.STAR, 0.3f, 1));

        register(GoldFruit.DRAGON, both(particles(1.0f, GoldFruit.COLOR, Kind.SHARD, 1.4f, 5), sparkle(Kind.SPARK, GoldFruit.PALE, 1.0f, 1)));

        // Song: bright notes; a world of floating stars.
        register(SongFruit.NOTE, both(particles(0.6f, SongFruit.RED, Kind.STAR, 0.5f, 1), sparse(Kind.GLOW, 0xFFFFFF, 0.4f, 2)));
        register(SongFruit.WORLD, both(rising(Kind.STAR, SongFruit.RED, 0.6f, 0.02f), sparkle(Kind.GLOW, SongFruit.COLOR, 0.8f, 1)));

        register(MagnetFruit.SCRAP, both(particles(0f, 0, Kind.SHARD, 0.7f, 2), sparse(Kind.SPARK, 0xFF4A4A, 0.5f, 2)));

        // Gravity: a well that swirls inward; a burning meteor; ground that buckles.
        register(GravityFruit.WELL, both(swirl(Kind.SHARD, 0.8f, 4, 1, GravityFruit.COLOR), zone(Kind.RING, 0.9f, 0.01f, true)));
        register(GravityFruit.ROCK, both(particles(1.0f, 0xFF8A3A, Kind.FLAME, 1.8f, 6), sparse(Kind.SMOKE, 0x3A3A3A, 2.4f, 1)));
        register(GravityFruit.FIELD, both(falling(Kind.SHARD, GravityFruit.COLOR, 0.6f, 4, 5), bubbles(GravityFruit.DEEP, 0.01f)));
    }
}
