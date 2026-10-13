package com.nerodev.grandline.client.fruit;

import static com.nerodev.grandline.client.fruit.LogiaLooks.both;
import static com.nerodev.grandline.client.fruit.LogiaLooks.bubbles;
import static com.nerodev.grandline.client.fruit.LogiaLooks.falling;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparkle;
import static com.nerodev.grandline.client.fruit.LogiaLooks.sparse;
import static com.nerodev.grandline.client.fruit.LogiaLooks.spikes;
import static com.nerodev.grandline.client.render.ProjectileVisuals.particles;
import static com.nerodev.grandline.client.render.ProjectileVisuals.register;
import static com.nerodev.grandline.client.render.ProjectileVisuals.zone;

import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.client.render.ProjectileVisuals.Visual;
import com.nerodev.grandline.fruit.paramecia.BombFruit;
import com.nerodev.grandline.fruit.paramecia.ChopFruit;
import com.nerodev.grandline.fruit.paramecia.DiceFruit;
import com.nerodev.grandline.fruit.paramecia.FlowerFruit;
import com.nerodev.grandline.fruit.paramecia.KiloFruit;
import com.nerodev.grandline.fruit.paramecia.MochiFruit;
import com.nerodev.grandline.fruit.paramecia.MunchFruit;
import com.nerodev.grandline.fruit.paramecia.OpFruit;
import com.nerodev.grandline.fruit.paramecia.SlipFruit;
import com.nerodev.grandline.fruit.paramecia.SpikeFruit;
import com.nerodev.grandline.fruit.paramecia.StringFruit;
import com.nerodev.grandline.fruit.paramecia.TremorFruit;
import com.nerodev.grandline.fruit.paramecia.WaxFruit;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.fx.GLParticles.Kind;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** How the projectiles and areas of Paramecia batch A look on the client. */
public final class ParameciaLooksA {
    private ParameciaLooksA() {}

    public static void install() {
        // Op: the Room is a pale-blue sphere, its shell picked out in light.
        register(OpFruit.ROOM_ZONE, dome(OpFruit.ROOM_COLOR, 0.45f, 26));

        // Tremor: a ball of shuddering air.
        register(TremorFruit.BUBBLE, both(particles(1.3f, 0xF4FAFF, Kind.GLOW, 0.7f, 2),
                sparse(Kind.SHOCKWAVE, TremorFruit.CRACK, 1.0f, 3)));

        // String: needle-thin threads, and the Birdcage's wall of falling threads.
        register(StringFruit.SHOT, particles(0.5f, 0xFFFFFF, Kind.STREAK, 0.5f, 1));
        register(StringFruit.CAGE, cage(StringFruit.THREAD, 32, 16));

        // Mochi.
        register(MochiFruit.SPEAR, particles(0.7f, 0xFFFFFF, Kind.SMOKE, 1.0f, 2));
        register(MochiFruit.FLOOR, both(zone(Kind.SMOKE, 1.6f, 0.08f, true), bubbles(MochiFruit.COLOR, 0.04f)));
        register(MochiFruit.WORLD, both(zone(Kind.SMOKE, 2.4f, 0.03f, true), bubbles(MochiFruit.SHADE, 0.02f)));

        // Chop: a flying glove, a spinning disc of legs, and a rain of knives.
        register(ChopFruit.FIST, both(particles(0.9f, ChopFruit.GLOVE, Kind.SMOKE, 0.5f, 1), sparse(Kind.STAR, 0xFFFFFF, 0.6f, 4)));
        register(ChopFruit.DISC, both(particles(0f, 0, Kind.SHARD, 1.0f, 2), sparse(Kind.RING, ChopFruit.COLOR, 1.2f, 3)));
        register(ChopFruit.RAIN, both(falling(Kind.STREAK, ChopFruit.STEEL, 1.6f, 12, 12), sparkle(Kind.SPARK, 0xFFFFFF, 0.8f, 2)));

        // Slip: oily sheen.
        register(SlipFruit.SLICK, zone(Kind.STAR, 0.8f, 0.05f, true));
        register(SlipFruit.WORLD, both(zone(Kind.STAR, 1.0f, 0.02f, true), zone(Kind.BUBBLE, 1.0f, 0.01f, true)));

        // Bomb: explosive bogeys, a hanging cloud of explosive breath, blinking mines.
        register(BombFruit.SHELL, both(particles(0.6f, 0xC8E080, Kind.SMOKE, 0.6f, 1), sparse(Kind.SPARK, BombFruit.FIRE, 0.8f, 2)));
        register(BombFruit.BREATH, both(zone(Kind.SMOKE, 2.2f, 0.25f, false), sparkle(Kind.SPARK, BombFruit.FIRE, 0.8f, 1)));
        register(BombFruit.MINE, blinker(0xFF3A2A));

        // Kilo: crushing weight pressing down.
        register(KiloFruit.HEAVY_ZONE, both(falling(Kind.STREAK, KiloFruit.LEAF, 1.2f, 8, 4), zone(Kind.SHARD, 1.0f, 0.02f, true)));

        // Flower: petals and arms everywhere.
        register(FlowerFruit.FIELD, both(zone(Kind.LEAF, 1.1f, 0.05f, true), sparkle(Kind.HEART, FlowerFruit.PETAL, 0.8f, 3)));

        // Dice: blades.
        register(DiceFruit.FIELD, both(zone(Kind.SPARK, 1.0f, 0.06f, true), spikes(DiceFruit.EDGE, 0.04f)));
        register(DiceFruit.CROSS, both(particles(1.0f, 0xFFFFFF, Kind.SPARK, 1.2f, 3), sparse(Kind.STREAK, DiceFruit.EDGE, 1.4f, 1)));

        // Spike, Wax and Munch shots.
        register(SpikeFruit.NEEDLE, particles(0.5f, SpikeFruit.COLOR, Kind.SHARD, 0.6f, 1));
        register(WaxFruit.ARROW, particles(0.6f, 0xFFFFFF, Kind.DROP, 0.7f, 1));
        register(MunchFruit.SHELL, both(particles(0f, 0, Kind.SMOKE, 1.0f, 2), sparse(Kind.SPARK, 0xFF8A2A, 0.8f, 2)));
    }

    /** A sphere shown as points of light scattered over its surface, plus a ring on the ground. */
    static Visual dome(int color, float scale, int perTick) {
        var glow = GLParticles.byKind(Kind.GLOW);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            for (int i = 0; i < perTick; i++) {
                Vec3 dir = FxKit.randomUnit(r);
                Vec3 at = p.position().add(0, 1, 0).add(dir.x * radius, Math.abs(dir.y) * radius - 1, dir.z * radius);
                FxKit.particle(level, glow, color, scale, at, Vec3.ZERO);
            }
            if (p.tickCount % 20 == 0) {
                FxKit.groundRing(level, color, (float) radius * 2f, p.position());
            }
        });
    }

    /** Vertical threads standing around the area's edge, shimmering from top to bottom. */
    static Visual cage(int color, int lines, double height) {
        var glow = GLParticles.byKind(Kind.GLOW);
        return new Visual(0f, 0, (level, p, r) -> {
            double radius = p.size() / 2.0;
            for (int i = 0; i < 40; i++) {
                double a = Mth.TWO_PI * r.nextInt(lines) / lines;
                double y = r.nextDouble() * height;
                // Threads lean in towards the top, like the bars of a cage.
                double inset = radius * (1 - 0.35 * (y / height));
                Vec3 at = p.position().add(Math.cos(a) * inset, y, Math.sin(a) * inset);
                FxKit.particle(level, glow, color, 0.35f, at, new Vec3(0, -0.05, 0));
            }
        });
    }

    /** A small light that blinks every second. */
    static Visual blinker(int color) {
        var glow = GLParticles.byKind(Kind.GLOW);
        return new Visual(0f, 0, (level, p, r) -> {
            if (p.tickCount % 20 == 0) {
                FxKit.particle(level, glow, color, 0.6f, p.position().add(0, 0.15, 0), Vec3.ZERO);
                FxKit.groundRing(level, color, 1.2f, p.position());
            }
        });
    }
}
