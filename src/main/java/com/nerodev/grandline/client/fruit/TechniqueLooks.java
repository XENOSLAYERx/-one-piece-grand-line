package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;

import com.nerodev.grandline.ability.rokushiki.RankyakuAbility;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.style.FishmanKarateStyle;
import com.nerodev.grandline.style.GunslingerStyle;
import com.nerodev.grandline.style.SwordsmanStyle;
import com.nerodev.grandline.weapon.GLWeapons;

/** How the projectiles of Rokushiki, the fighting styles and weapons look on the client. */
public final class TechniqueLooks {
    private TechniqueLooks() {}

    public static void install() {
        // Rankyaku: a pale crescent of cut air.
        register(RankyakuAbility.BLADE, both(particles(0.6f, 0xE8F0FF, Kind.STREAK, 0.9f, 3), sparse(Kind.SHOCKWAVE, 0xFFFFFF, 0.6f, 3)));
        // Uchimizu: a water droplet fired like a bullet.
        register(FishmanKarateStyle.DROPLET, both(particles(0.25f, FishmanKarateStyle.WATER, Kind.DROP, 0.5f, 1),
                sparse(Kind.STREAK, FishmanKarateStyle.WATER, 0.4f, 2)));
        // Swordsman: a flying slash, a pale crescent of steel.
        register(SwordsmanStyle.FLYING_SLASH, both(particles(0.7f, SwordsmanStyle.STEEL, Kind.STREAK, 0.9f, 3),
                sparse(Kind.SHOCKWAVE, SwordsmanStyle.COLOR, 0.6f, 3)));
        // Gunslinger: a heavy piercing round, a smoke shell and homing shots.
        register(GunslingerStyle.PIERCING_SHOT, both(particles(0.25f, 0x5A5A60, Kind.STREAK, 0.4f, 2),
                sparse(Kind.SPARK, GunslingerStyle.FLASH, 0.3f, 2)));
        register(GunslingerStyle.SMOKE_SHOT, particles(0.3f, 0x7A7E84, Kind.SMOKE, 0.5f, 2));
        register(GunslingerStyle.HOMING_SHOT, both(particles(0.2f, 0x5A5A60, Kind.SMOKE, 0.25f, 1),
                sparse(Kind.SPARK, 0xFF5A3A, 0.35f, 2)));
        // Bullets: a small dark ball with a faint smoke trail; Seastone ones glint sea-green.
        register(GLWeapons.BULLET_SHOT, both(particles(0.18f, 0x5A5A60, Kind.SMOKE, 0.25f, 1), sparse(Kind.SPARK, 0xF0C060, 0.25f, 6)));
        register(GLWeapons.SEASTONE_BULLET_SHOT, both(particles(0.2f, GLWeapons.SEASTONE_COLOR, Kind.SMOKE, 0.25f, 1),
                sparse(Kind.SPARK, 0xA8E0D8, 0.3f, 3)));
        // Seastone net: a spinning tangle of sea-green threads.
        register(GLWeapons.NET_THROWN, both(particles(0.9f, GLWeapons.SEASTONE_COLOR, Kind.STREAK, 0.45f, 2),
                sparse(Kind.SHARD, 0xD6C496, 0.4f, 2)));
    }
}
