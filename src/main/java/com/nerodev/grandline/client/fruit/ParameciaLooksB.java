package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.falling;
import static com.nerodev.grandline.client.fruit.LogiaLooks.rising;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparkle;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.swirl;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.fruit.paramecia.BerryFruit;
import com.nerodev.grandline.fruit.paramecia.BindFruit;
import com.nerodev.grandline.fruit.paramecia.BubbleFruit;
import com.nerodev.grandline.fruit.paramecia.HollowFruit;
import com.nerodev.grandline.fruit.paramecia.LoveFruit;
import com.nerodev.grandline.fruit.paramecia.PawFruit;
import com.nerodev.grandline.fruit.paramecia.ReviveFruit;
import com.nerodev.grandline.fruit.paramecia.RustFruit;
import com.nerodev.grandline.fruit.paramecia.ShadowFruit;
import com.nerodev.grandline.fruit.paramecia.SlowFruit;
import com.nerodev.grandline.fruit.paramecia.SpringFruit;
import com.nerodev.grandline.fruit.paramecia.WheelFruit;
import com.nerodev.grandline.fx.GLParticles.Kind;

/** How the projectiles and areas of Paramecia batch B look on the client. */
public final class ParameciaLooksB {
    private ParameciaLooksB() {}

    public static void install() {
        register(BindFruit.BAND, both(particles(0.6f, 0xFFFFFF, Kind.SPARK, 0.7f, 2), sparse(Kind.RING, BindFruit.IRON, 0.8f, 3)));
        register(BindFruit.PRISON_ZONE, both(zone(Kind.SPARK, 0.9f, 0.04f, true), swirl(Kind.SPARK, 0.6f, 6, -1)));

        register(SpringFruit.FIELD, both(zone(Kind.RING, 1.0f, 0.01f, true), rising(Kind.SPARK, SpringFruit.BLUE, 0.7f, 0.02f)));

        register(SlowFruit.FIELD, both(zone(Kind.GLOW, 0.8f, 0.05f, false), sparkle(Kind.STAR, SlowFruit.GLOW, 0.8f, 2)));

        register(BubbleFruit.BUBBLE, particles(1.0f, 0xFFFFFF, Kind.BUBBLE, 0.9f, 1));
        register(BubbleFruit.FLOOR, both(zone(Kind.BUBBLE, 1.2f, 0.06f, true), zone(Kind.STAR, 0.6f, 0.02f, true)));

        register(BerryFruit.BALL, particles(0.7f, BerryFruit.LIGHT, Kind.BUBBLE, 0.6f, 1));

        register(RustFruit.FIELD, both(zone(Kind.SHARD, 0.8f, 0.05f, true), zone(Kind.SMOKE, 1.4f, 0.01f, true)));

        register(WheelFruit.WHEEL, both(particles(0.9f, WheelFruit.RIM, Kind.SPARK, 0.8f, 3), sparse(Kind.RING, WheelFruit.COLOR, 1.0f, 2)));

        register(ReviveFruit.CHILL, both(falling(Kind.SNOW, ReviveFruit.SOUL, 1.2f, 8, 6), rising(Kind.GLOW, ReviveFruit.SOUL, 0.5f, 0.02f)));

        register(ShadowFruit.BAT, both(particles(0f, 0, Kind.SMOKE, 0.9f, 2), sparse(Kind.GLOW, ShadowFruit.VIOLET, 0.5f, 3)));

        register(HollowFruit.NEGATIVE, both(particles(1.0f, 0xFFFFFF, Kind.SMOKE, 0.8f, 2), sparse(Kind.STAR, HollowFruit.COLOR, 0.6f, 3)));
        register(HollowFruit.RAP, both(particles(1.1f, 0xFFE0F0, Kind.SMOKE, 0.8f, 2), sparse(Kind.SPARK, HollowFruit.COLOR, 0.7f, 2)));
        register(HollowFruit.MINI, particles(0.9f, 0xFFFFFF, Kind.SMOKE, 0.4f, 1));
        register(HollowFruit.TOKU, both(particles(1.0f, 0xFFF0F8, Kind.SMOKE, 1.6f, 4), sparse(Kind.STAR, HollowFruit.COLOR, 1.0f, 2)));

        register(PawFruit.PAW, both(particles(1.0f, PawFruit.PAD, Kind.SHOCKWAVE, 0.6f, 1), sparse(Kind.STAR, 0xFFFFFF, 0.6f, 3)));
        register(PawFruit.URSUS, both(particles(1.2f, 0xFFFFFF, Kind.SHOCKWAVE, 1.0f, 2), sparse(Kind.GLOW, PawFruit.PAD, 1.2f, 1)));

        register(LoveFruit.ARROW, both(particles(0.7f, LoveFruit.PINK, Kind.HEART, 0.5f, 1), sparse(Kind.STAR, 0xFFFFFF, 0.5f, 4)));
        register(LoveFruit.KISS, particles(1.2f, 0xFFFFFF, Kind.HEART, 0.7f, 2));
    }
}
