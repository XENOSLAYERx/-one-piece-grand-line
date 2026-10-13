package com.nerodev.grandline.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.entity.AbilityProjectile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws an ability projectile as a glowing, additive-blended core facing the camera (outer halo in the projectile
 * colour, inner core near white). Particles from {@link ProjectileVisuals} do the rest.
 */
public class AbilityProjectileRenderer extends EntityRenderer<AbilityProjectile> {
    public static final ResourceLocation ORB = GrandLine.id("textures/entity/glow_orb.png");

    public AbilityProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AbilityProjectile projectile, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        ProjectileVisuals.Visual visual = ProjectileVisuals.get(projectile.kind());
        if (visual != null && visual.coreScale() > 0) {
            float size = projectile.size() * visual.coreScale();
            float pulse = 1f + 0.08f * (float) Math.sin((projectile.tickCount + partialTick) * 0.9f);
            pose.pushPose();
            pose.translate(0, projectile.getBbHeight() / 2, 0);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            VertexConsumer buffer = buffers.getBuffer(RenderType.eyes(ORB));
            quad(pose, buffer, size * 1.7f * pulse, projectile.color(), 160);
            quad(pose, buffer, size * pulse, visual.coreTint(), 230);
            pose.popPose();
        }
        super.render(projectile, yaw, partialTick, pose, buffers, light);
    }

    private static void quad(PoseStack pose, VertexConsumer buffer, float size, int rgb, int alpha) {
        float h = size / 2;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        PoseStack.Pose last = pose.last();
        vertex(buffer, last, -h, -h, 0, 1, r, g, b, alpha);
        vertex(buffer, last, h, -h, 1, 1, r, g, b, alpha);
        vertex(buffer, last, h, h, 1, 0, r, g, b, alpha);
        vertex(buffer, last, -h, h, 0, 0, r, g, b, alpha);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int r, int g,
            int b, int a) {
        buffer.addVertex(pose, x, y, 0)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(AbilityProjectile projectile) {
        return ORB;
    }
}
