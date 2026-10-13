package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.bubbles;
import static com.nerodev.grandline.client.fruit.LogiaLooks.cloud;
import static com.nerodev.grandline.client.fruit.LogiaLooks.falling;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparkle;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.swirl;
import static com.nerodev.grandline.client.fruit.LogiaLooks.vents;
import static com.nerodev.grandline.client.fruit.LogiaLooks.wave;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.paramecia.BigFruit;
import com.nerodev.grandline.fruit.paramecia.BrushFruit;
import com.nerodev.grandline.fruit.paramecia.ButterFruit;
import com.nerodev.grandline.fruit.paramecia.GenieFruit;
import com.nerodev.grandline.fruit.paramecia.GuzzleFruit;
import com.nerodev.grandline.fruit.paramecia.IslandFruit;
import com.nerodev.grandline.fruit.paramecia.PushFruit;
import com.nerodev.grandline.fruit.paramecia.ReturnFruit;
import com.nerodev.grandline.fruit.paramecia.SproutFruit;
import com.nerodev.grandline.fruit.paramecia.StrawFruit;
import com.nerodev.grandline.fruit.paramecia.ToneFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of Paramecia batch F look on the client. */
public final class ParameciaLooksF {
    private ParameciaLooksF() {}

    public static void install() {
        // Brush: a serpent of black ink; a black rain.
        register(BrushFruit.SERPENT, both(particles(0.5f, BrushFruit.INK, Kind.SMOKE, 0.9f, 3), sparse(Kind.DROP, BrushFruit.INK, 0.8f, 2)));
        register(BrushFruit.STORM, both(falling(Kind.DROP, BrushFruit.INK, 0.8f, 6, 8), cloud(BrushFruit.INK, 8, 2.4f)));

        // Genie: a golden spear; a cloud of lamp smoke.
        register(GenieFruit.SPEAR, both(particles(0.5f, GenieFruit.GOLD, Kind.STREAK, 0.6f, 1), sparse(Kind.SMOKE, GenieFruit.COLOR, 0.8f, 2)));
        register(GenieFruit.SMOKE, both(zone(Kind.SMOKE, 2.0f, 0.03f, false), sparkle(Kind.SPARK, GenieFruit.GOLD, 0.6f, 2)));

        // Butter: soft yellow globs, a greasy floor and a rolling flood.
        register(ButterFruit.BLOB, particles(0.6f, ButterFruit.COLOR, Kind.DROP, 0.8f, 2));
        register(ButterFruit.SLICK, both(zone(Kind.DROP, 0.8f, 0.04f, true), sparkle(Kind.GLOW, ButterFruit.COLOR, 0.5f, 3)));
        register(ButterFruit.SPEAR, both(particles(0.4f, ButterFruit.GOLD, Kind.STREAK, 0.5f, 1), sparse(Kind.DROP, ButterFruit.COLOR, 0.5f, 3)));
        register(ButterFruit.FLOOD, both(wave(Kind.DROP, 2.0f, 14, ButterFruit.COLOR), wave(Kind.GLOW, 1.0f, 6, ButterFruit.GOLD)));

        register(StrawFruit.NEEDLE, particles(0.3f, StrawFruit.COLOR, Kind.STREAK, 0.4f, 1));

        // Tone: rings of sound rising off the stage.
        register(ToneFruit.STAGE, both(rising(Kind.RING, ToneFruit.COLOR, 0.8f, 0.01f), sparkle(Kind.STAR, ToneFruit.BLUE, 0.6f, 2)));

        register(BigFruit.BOULDER, both(particles(0f, 0, Kind.SHARD, 1.4f, 3), sparse(Kind.SMOKE, BigFruit.DARK, 1.6f, 2)));

        // Guzzle: a flaming ball of liquor; a burning spill.
        register(GuzzleFruit.BOMB, both(particles(0.7f, GuzzleFruit.FLAME, Kind.FLAME, 0.9f, 3), sparse(Kind.SMOKE, 0x3A3A3A, 0.9f, 2)));
        register(GuzzleFruit.SEA, both(vents(Kind.FLAME, GuzzleFruit.FLAME, 0.9f, 3), zone(Kind.DROP, 0.7f, 0.02f, true)));

        register(IslandFruit.ISLAND, both(zone(Kind.LEAF, 0.7f, 0.015f, true), sparse(Kind.SHARD, IslandFruit.EARTH, 1.0f, 2)));

        register(PushFruit.WAVE, both(wave(Kind.SHARD, 1.6f, 12, PushFruit.COLOR), wave(Kind.SMOKE, 1.4f, 6, PushFruit.DARK)));

        register(ReturnFruit.FIELD, both(swirl(Kind.STAR, 0.6f, 3, -1, ReturnFruit.COLOR), zone(Kind.RING, 0.9f, 0.01f, true)));

        register(SproutFruit.SURGE, both(zone(Kind.LEAF, 0.8f, 0.03f, true), bubbles(SproutFruit.BLOOM, 0.01f)));
    }
}
