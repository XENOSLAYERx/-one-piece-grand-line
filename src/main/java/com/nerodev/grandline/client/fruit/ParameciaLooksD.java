package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.bubbles;
import static com.nerodev.grandline.client.fruit.LogiaLooks.falling;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparkle;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.swirl;
import static com.nerodev.grandline.client.fruit.LogiaLooks.wave;
import static com.nerodev.grandline.client.fruit.ParameciaLooksA.dome;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.paramecia.BarrierFruit;
import com.nerodev.grandline.fruit.paramecia.BiscuitFruit;
import com.nerodev.grandline.fruit.paramecia.CalmFruit;
import com.nerodev.grandline.fruit.paramecia.CreamFruit;
import com.nerodev.grandline.fruit.paramecia.HealFruit;
import com.nerodev.grandline.fruit.paramecia.LickFruit;
import com.nerodev.grandline.fruit.paramecia.MirrorFruit;
import com.nerodev.grandline.fruit.paramecia.PopFruit;
import com.nerodev.grandline.fruit.paramecia.SoulFruit;
import com.nerodev.grandline.fruit.paramecia.SwimFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of Paramecia batch D look on the client. */
public final class ParameciaLooksD {
    private ParameciaLooksD() {}

    public static void install() {
        // Pop: dark seeds with a fizzing fuse; a field of sparks; a storm of little blasts.
        register(PopFruit.BURSTER, both(particles(0.5f, PopFruit.DARK, Kind.SMOKE, 0.7f, 1), sparse(Kind.SPARK, PopFruit.COLOR, 0.6f, 1)));
        register(PopFruit.FIELD, both(zone(Kind.SPARK, 0.6f, 0.15f, true), sparkle(Kind.GLOW, PopFruit.COLOR, 0.7f, 2)));
        register(PopFruit.STORM, both(falling(Kind.SPARK, PopFruit.COLOR, 0.8f, 4, 6), zone(Kind.SMOKE, 1.4f, 0.04f, true)));

        // Swim: the ground ripples like water.
        register(SwimFruit.POOL, both(zone(Kind.RING, 1.0f, 0.02f, true), bubbles(SwimFruit.COLOR, 0.03f)));

        // Barrier: a bright translucent slab.
        register(BarrierFruit.SLAB, both(particles(1.0f, BarrierFruit.COLOR, Kind.GLOW, 0.6f, 3), sparse(Kind.SHARD, 0xFFFFFF, 0.7f, 2)));

        // Calm: a still, pale haze.
        register(CalmFruit.QUIET, both(zone(Kind.RING, 0.8f, 0.01f, true), rising(Kind.GLOW, CalmFruit.COLOR, 0.4f, 0.02f)));

        // Heal: a tear of light; a meadow of drifting hearts.
        register(HealFruit.TEAR, particles(0.6f, HealFruit.COLOR, Kind.DROP, 0.6f, 1));
        register(HealFruit.FIELD, both(zone(Kind.HEART, 0.7f, 0.02f, true), rising(Kind.GLOW, HealFruit.GREEN, 0.5f, 0.03f)));

        register(BiscuitFruit.SPEAR, both(particles(0f, 0, Kind.SHARD, 0.6f, 1), sparse(Kind.SMOKE, BiscuitFruit.COLOR, 0.6f, 3)));

        // Cream: soft white blobs and sticky, steaming floors.
        register(CreamFruit.BLOB, both(particles(0.7f, 0xFFFFFF, Kind.DROP, 1.0f, 2), sparse(Kind.SMOKE, CreamFruit.COLOR, 0.8f, 2)));
        register(CreamFruit.FLOOR, both(zone(Kind.DROP, 0.9f, 0.04f, true), rising(Kind.SMOKE, CreamFruit.COLOR, 0.8f, 0.02f)));
        register(CreamFruit.FLOOD, both(wave(Kind.DROP, 2.2f, 14, CreamFruit.COLOR), wave(Kind.SMOKE, 1.8f, 8, CreamFruit.PINK)));

        // Soul: Prometheus is a small burning sun; the spear is a bolt of soul-fire.
        register(SoulFruit.SUN_KIND, both(particles(0.7f, SoulFruit.SUN, Kind.FLAME, 1.2f, 3), swirl(Kind.FLAME, 0.9f, 3, 1, SoulFruit.SUN)));
        register(SoulFruit.SPEAR, both(particles(0.8f, SoulFruit.COLOR, Kind.FLAME, 1.0f, 3), sparse(Kind.SPARK, SoulFruit.SUN, 0.8f, 1)));

        // Mirror: glinting shards; a hall of mirrors; a rain of glass.
        register(MirrorFruit.SHARD, both(particles(0.4f, 0xFFFFFF, Kind.SHARD, 0.5f, 1), sparse(Kind.SPARK, MirrorFruit.COLOR, 0.5f, 2)));
        register(MirrorFruit.HALL, both(dome(MirrorFruit.COLOR, 0.5f, 10), sparkle(Kind.SPARK, MirrorFruit.VIOLET, 0.8f, 2)));
        register(MirrorFruit.RAIN, both(falling(Kind.SHARD, MirrorFruit.COLOR, 0.7f, 6, 7), zone(Kind.SPARK, 0.5f, 0.03f, true)));

        // Lick: candy spears, sugary floors and a wave of molten candy.
        register(LickFruit.SPEAR, both(particles(0.5f, LickFruit.COLOR, Kind.STREAK, 0.5f, 1), sparse(Kind.STAR, 0xFFFFFF, 0.5f, 2)));
        register(LickFruit.FLOOR, both(zone(Kind.STAR, 0.6f, 0.04f, true), bubbles(LickFruit.COLOR, 0.02f)));
        register(LickFruit.LAND, both(zone(Kind.STAR, 0.7f, 0.03f, true), sparkle(Kind.GLOW, LickFruit.COLOR, 0.8f, 1)));
        register(LickFruit.WAVE, both(wave(Kind.DROP, 2.0f, 14, LickFruit.COLOR), wave(Kind.STAR, 1.2f, 6, 0xFFFFFF)));
    }
}
