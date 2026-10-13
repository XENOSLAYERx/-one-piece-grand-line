package com.nerodev.grandline.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Short, decaying screen shake for heavy impacts. Strength falls off with distance from the source. */
public final class CameraShake {
    private static final RandomSource RANDOM = RandomSource.create();
    private static float strength;
    private static int ticksLeft;
    private static int duration;

    private CameraShake() {}

    /** @param strength degrees of shake at the source; ~0.5 subtle, 3 heavy */
    public static void add(Vec3 source, float strength, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        double distance = mc.player.position().distanceTo(source);
        float scaled = strength * (float) Mth.clamp(1.0 - distance / 24.0, 0.0, 1.0);
        if (scaled <= 0.01f) {
            return;
        }
        if (scaled >= CameraShake.strength * ((float) ticksLeft / Math.max(1, duration))) {
            CameraShake.strength = scaled;
            CameraShake.ticksLeft = ticks;
            CameraShake.duration = ticks;
        }
    }

    public static void tick() {
        if (ticksLeft > 0) {
            ticksLeft--;
        }
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ticksLeft <= 0) {
            return;
        }
        float fade = (ticksLeft - (float) event.getPartialTick()) / duration;
        float amount = strength * Math.max(0, fade);
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5f) * amount);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5f) * amount);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5f) * amount * 0.5f);
    }
}
