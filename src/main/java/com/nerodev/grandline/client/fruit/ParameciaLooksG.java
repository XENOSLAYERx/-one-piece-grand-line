package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.paramecia.EggFruit;
import com.nerodev.grandline.fruit.paramecia.MoreFruit;
import com.nerodev.grandline.fruit.paramecia.SickFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of Paramecia batch G look on the client. */
public final class ParameciaLooksG {
    private ParameciaLooksG() {}

    public static void install() {
        // Sick: Doc Q's apples (the ones that explode spark as they fly); a green cloud of contagion.
        register(SickFruit.BOMB_APPLE, both(particles(0.45f, SickFruit.APPLE, Kind.GLOW, 0.4f, 1), sparse(Kind.SPARK, 0xF0C040, 0.5f, 2)));
        register(SickFruit.ROTTEN_APPLE, both(particles(0.45f, SickFruit.APPLE, Kind.GLOW, 0.4f, 1), sparse(Kind.DROP, SickFruit.COLOR, 0.5f, 3)));
        register(SickFruit.MIASMA, both(zone(Kind.SMOKE, 1.6f, 0.03f, false), rising(Kind.BUBBLE, SickFruit.MIASMA_COLOR, 0.5f, 0.01f)));

        // Egg: a thrown egg.
        register(EggFruit.EGG, particles(0.4f, EggFruit.COLOR, Kind.GLOW, 0.3f, 1));

        // More: a pebble at bullet speed; a rock growing into a meteor as it flies.
        register(MoreFruit.PEBBLE, both(particles(0.25f, MoreFruit.STONE, Kind.STREAK, 0.5f, 2), sparse(Kind.SPARK, MoreFruit.GLOW, 0.4f, 2)));
        register(MoreFruit.ROCK, both(particles(2.2f, MoreFruit.STONE, Kind.SHARD, 1.0f, 3), sparse(Kind.SHOCKWAVE, MoreFruit.COLOR, 1.2f, 4)));
    }
}
