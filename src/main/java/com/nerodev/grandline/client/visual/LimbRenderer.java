package com.nerodev.grandline.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Draws a player's arm stretched between two world points, textured with that player's own skin, plus a fist at the
 * far end. Used by every rubber move, so it must stay cheap: two boxes per limb.
 */
public final class LimbRenderer {
    private LimbRenderer() {}

    /**
     * Arm texture regions in the 64x64 skin layout: {frontU, frontV, topU, topV}. The palm (bottom face) sits right
     * after the top face, at topU + arm width.
     */
    private static int[] uv(boolean right) {
        return right ? new int[] {44, 20, 44, 16} : new int[] {36, 52, 36, 48};
    }

    /** World position of the shoulder for {@code player}. */
    public static Vec3 shoulder(AbstractClientPlayer player, boolean right, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot) * Mth.DEG_TO_RAD;
        float scale = player.getScale();
        Vec3 rightDir = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 base = player.getPosition(partialTick).add(0, 1.375 * scale, 0);
        return base.add(rightDir.scale((right ? 0.3125 : -0.3125) * scale));
    }

    /**
     * @param thickness 1 = normal arm; bigger for inflated limbs
     * @param fistScale size of the fist relative to the arm width
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 camera, AbstractClientPlayer player,
            boolean right, Vec3 from, Vec3 to, float thickness, float fistScale) {
        Vec3 delta = to.subtract(from);
        float length = (float) delta.length();
        if (length < 0.05f) {
            return;
        }
        PlayerSkin skin = player.getSkin();
        boolean slim = skin.model() == PlayerSkin.Model.SLIM;
        ResourceLocation texture = skin.texture();
        int armPx = slim ? 3 : 4;
        float width = armPx / 16f * player.getScale() * thickness;
        int light = LevelRenderer.getLightColor(player.level(), BlockPos.containing(from.add(to).scale(0.5)));

        pose.pushPose();
        pose.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
        Vec3 dir = delta.scale(1 / length);
        pose.mulPose(new Quaternionf().rotationTo(0, 0, 1, (float) dir.x, (float) dir.y, (float) dir.z));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        int[] uv = uv(right);
        float h = width / 2;
        int palmU = uv[2] + armPx;
        // Arm: side texture stretched along the whole length.
        box(buffer, pose.last(), -h, -h, 0, h, h, length, uv[0], uv[1], armPx, 12, palmU, uv[3], armPx, light);
        // Fist: hand texture (lower arm) on the sides, palm on both ends (never an empty part of the skin, or the
        // fist looks hollow when seen end-on).
        float f = h * 1.3f * fistScale;
        box(buffer, pose.last(), -f, -f, length - f * 0.6f, f, f, length + f * 1.4f, uv[0], uv[1] + 8, armPx, 4,
                palmU, uv[3], armPx, light);
        pose.popPose();
    }

    /** Axis-aligned box in the current pose; sides use the side region, both ends the end region. */
    private static void box(VertexConsumer b, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
            int su, int sv, int sw, int sh, int eu, int ev, int ew, int light) {
        float u0 = su / 64f;
        float u1 = (su + sw) / 64f;
        float v0 = sv / 64f;
        float v1 = (sv + sh) / 64f;
        // Sides run along z: v follows the length.
        quad(b, p, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1);
        quad(b, p, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v1, u1, v0);
        quad(b, p, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, u0, v1, u1, v0);
        quad(b, p, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, u0, v0, u1, v1);
        float eu0 = eu / 64f;
        float eu1 = (eu + ew) / 64f;
        float ev0 = ev / 64f;
        float ev1 = (ev + ew) / 64f;
        quad(b, p, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, eu0, ev0, eu1, ev1);
        quad(b, p, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, eu0, ev0, eu1, ev1);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose p, int light, float nx, float ny, float nz,
            float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
            float u0, float v0, float u1, float v1) {
        vertex(b, p, ax, ay, az, u0, v0, light, nx, ny, nz);
        vertex(b, p, bx, by, bz, u1, v0, light, nx, ny, nz);
        vertex(b, p, cx, cy, cz, u1, v1, light, nx, ny, nz);
        vertex(b, p, dx, dy, dz, u0, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float u, float v, int light,
            float nx, float ny, float nz) {
        b.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, nx, ny, nz);
    }

    static AbstractClientPlayer player(int id) {
        var level = Minecraft.getInstance().level;
        return level != null && level.getEntity(id) instanceof AbstractClientPlayer player ? player : null;
    }
}
