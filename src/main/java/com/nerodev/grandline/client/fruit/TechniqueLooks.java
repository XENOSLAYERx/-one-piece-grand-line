package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;

import com.nerodev.grandline.ability.rokushiki.RankyakuAbility;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.style.FishmanKarateStyle;

/** How the projectiles of Rokushiki and the fighting styles look on the client. */
public final class TechniqueLooks {
    private TechniqueLooks() {}

    public static void install() {
        // Rankyaku: a pale crescent of cut air.
        register(RankyakuAbility.BLADE, both(particles(0.6f, 0xE8F0FF, Kind.STREAK, 0.9f, 3), sparse(Kind.SHOCKWAVE, 0xFFFFFF, 0.6f, 3)));
        // Uchimizu: a water droplet fired like a bullet.
        register(FishmanKarateStyle.DROPLET, both(particles(0.25f, FishmanKarateStyle.WATER, Kind.DROP, 0.5f, 1),
                sparse(Kind.STREAK, FishmanKarateStyle.WATER, 0.4f, 2)));
    }
}
