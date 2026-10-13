package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.bubbles;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.swirl;
import static com.nerodev.grandline.client.fruit.LogiaLooks.tornado;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.zoan.AlbatrossFruit;
import com.nerodev.grandline.fruit.zoan.AxolotlFruit;
import com.nerodev.grandline.fruit.zoan.FalconFruit;
import com.nerodev.grandline.fruit.zoan.GiraffeFruit;
import com.nerodev.grandline.fruit.zoan.KingCobraFruit;
import com.nerodev.grandline.fruit.zoan.TanukiFruit;
import com.nerodev.grandline.fruit.zoan.WolfFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of the Zoan fruits look on the client. */
public final class ZoanProjectileLooks {
    private ZoanProjectileLooks() {}

    public static void install() {
        // Feathers and gales.
        register(FalconFruit.FEATHER, both(particles(0.3f, FalconFruit.CREAM, Kind.FEATHER, 0.6f, 1), sparse(Kind.STREAK, 0xFFFFFF, 0.4f, 2)));
        register(FalconFruit.GALE, tornado(Kind.FEATHER, 1.2f, 12, Kind.SMOKE));
        register(AlbatrossFruit.FEATHER, both(particles(0.3f, 0xFFFFFF, Kind.FEATHER, 0.6f, 1), sparse(Kind.STREAK, 0xFFFFFF, 0.4f, 2)));
        register(AlbatrossFruit.STORM, both(swirl(Kind.SMOKE, 1.4f, 6, 1, 0xC8D0D8), zone(Kind.FEATHER, 0.8f, 0.02f, false)));

        // Blades of wind from the Six Powers' kicks.
        register(WolfFruit.GALE, both(particles(0.6f, 0xE8E8F0, Kind.STREAK, 0.8f, 2), sparse(Kind.SPARK, 0xFFFFFF, 0.5f, 2)));
        register(GiraffeFruit.WAVE, both(particles(0.6f, 0xFFFFFF, Kind.STREAK, 0.9f, 2), sparse(Kind.SPARK, GiraffeFruit.COLOR, 0.5f, 2)));

        // Venom and gas.
        register(KingCobraFruit.SPIT, particles(0.4f, KingCobraFruit.VENOM, Kind.DROP, 0.6f, 2));
        register(KingCobraFruit.POOL, both(zone(Kind.DROP, 0.8f, 0.04f, true), bubbles(KingCobraFruit.VENOM, 0.03f)));
        register(AxolotlFruit.SLIME, both(particles(0.6f, AxolotlFruit.GAS, Kind.DROP, 0.8f, 2), sparse(Kind.SMOKE, AxolotlFruit.GAS, 0.8f, 2)));
        register(AxolotlFruit.CLOUD, both(zone(Kind.SMOKE, 2.2f, 0.03f, false), rising(Kind.BUBBLE, AxolotlFruit.COLOR, 0.5f, 0.02f)));

        register(TanukiFruit.LEAF_KIND, particles(0.3f, TanukiFruit.LEAF, Kind.LEAF, 0.7f, 1));
    }
}
