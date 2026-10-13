package com.nerodev.grandline.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Rubber visuals: stretching limbs, an inflating fist, and the giant fist that drops from the sky. */
public final class RubberVisuals {
    public static final int RIGHT = 0;
    public static final int LEFT = 1;
    public static final int BOTH = 2;
    public static final int GIANT = 3;

    private RubberVisuals() {}

    /** Extension curve: snap out (35%), hold (25%), pull back (40%). */
    static float extension(float t) {
        if (t < 0.35f) {
            float k = t / 0.35f;
            return 1 - (1 - k) * (1 - k) * (1 - k);
        }
        if (t < 0.6f) {
            return 1f;
        }
        float k = (t - 0.6f) / 0.4f;
        return 1 - k * k;
    }

    /** One or two arms shooting out to fixed world points and snapping back. */
    public static final class Stretch implements ClientVisuals.Visual {
        private final int ownerId;
        private final Vec3 end;
        private final Vec3 end2;
        private final float fist;
        private final int limb;
        private final int duration;
        private int age;

        public Stretch(int ownerId, Vec3 end, Vec3 end2, float fist, int limb, int duration) {
            this.ownerId = ownerId;
            this.end = end;
            this.end2 = end2;
            this.fist = fist;
            this.limb = limb;
            this.duration = Math.max(2, duration);
        }

        @Override
        public boolean tick() {
            return ++age < duration;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float partialTick) {
            AbstractClientPlayer player = LimbRenderer.player(ownerId);
            if (player == null) {
                return;
            }
            float e = extension(Math.min(1f, (age + partialTick) / duration));
            boolean giant = limb == GIANT;
            if (limb == RIGHT || limb == BOTH || giant) {
                Vec3 shoulder = LimbRenderer.shoulder(player, true, partialTick);
                LimbRenderer.draw(pose, buffers, camera, player, true, shoulder, shoulder.lerp(end, e),
                        giant ? 1.8f : 1f, fist);
            }
            if (limb == LEFT || limb == BOTH) {
                Vec3 shoulder = LimbRenderer.shoulder(player, false, partialTick);
                Vec3 target = limb == BOTH ? end2 : end;
                LimbRenderer.draw(pose, buffers, camera, player, false, shoulder, shoulder.lerp(target, e), 1f, fist);
            }
        }
    }

    /** Gear Third windup: the fist balloons in front of the player. */
    public static final class Inflate implements ClientVisuals.Visual {
        private final int ownerId;
        private final float scale;
        private int age;

        public Inflate(int ownerId, float scale) {
            this.ownerId = ownerId;
            this.scale = scale;
        }

        @Override
        public boolean tick() {
            return ++age < 3;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float partialTick) {
            AbstractClientPlayer player = LimbRenderer.player(ownerId);
            if (player == null) {
                return;
            }
            Vec3 shoulder = LimbRenderer.shoulder(player, true, partialTick);
            Vec3 look = player.getViewVector(partialTick);
            Vec3 hand = shoulder.add(look.scale(0.45 + scale * 0.12)).add(0, 0.1 * scale, 0);
            LimbRenderer.draw(pose, buffers, camera, player, true, shoulder, hand, 1f + scale * 0.25f, scale);
        }
    }

    /** Gear Fifth Dawn Fist: an enormous fist (with arm reaching into the sky) falls onto the target. */
    public static final class DawnFist implements ClientVisuals.Visual {
        private final int ownerId;
        private final Vec3 target;
        private final float scale;
        private final int fallTicks;
        private int age;

        public DawnFist(int ownerId, Vec3 target, float scale, int fallTicks) {
            this.ownerId = ownerId;
            this.target = target;
            this.scale = scale;
            this.fallTicks = Math.max(2, fallTicks);
        }

        @Override
        public boolean tick() {
            return ++age < fallTicks + 8;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float partialTick) {
            AbstractClientPlayer player = LimbRenderer.player(ownerId);
            if (player == null) {
                return;
            }
            float t = Math.min(1f, (age + partialTick) / fallTicks);
            float fall = t * t;
            double height = 26 * (1 - fall) + scale * 0.35;
            Vec3 fist = target.add(0, height, 0);
            Vec3 top = fist.add(0, 40, 0);
            LimbRenderer.draw(pose, buffers, camera, player, true, top, fist, scale * 0.6f, 1.6f);
        }
    }
}
