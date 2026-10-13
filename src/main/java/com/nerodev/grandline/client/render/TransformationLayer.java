package com.nerodev.grandline.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.client.model.BeastModel;
import com.nerodev.grandline.client.model.TransformationModels;
import com.nerodev.grandline.client.model.ZoanLooks;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws body transformations on the player model: leopard hybrid fur, ears and tail; Gear Second's flushed red
 * steam-heated skin; Gear Fifth's white glow; every other Zoan hybrid's fur or scales and the parts it grows (ears,
 * horns, tails, wings). Uses a slightly inflated copy of the player model so overlays never
 * z-fight with the skin underneath.
 */
public class TransformationLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation LEOPARD_TEXTURE = GrandLine.id("textures/entity/leopard.png");
    public static final ResourceLocation SPOTS_TEXTURE = GrandLine.id("textures/entity/leopard_spots.png");

    private final PlayerModel<AbstractClientPlayer> shell;
    private final ModelPart ears;
    private final ModelPart tail;
    private final ModelPart tailTip;

    public TransformationLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            EntityModelSet models, boolean slim) {
        super(parent);
        this.shell = new PlayerModel<>(models.bakeLayer(slim ? TransformationModels.SHELL_SLIM : TransformationModels.SHELL_WIDE), slim);
        ModelPart parts = models.bakeLayer(TransformationModels.HYBRID_PARTS);
        this.ears = parts.getChild("ears");
        this.tail = parts.getChild("tail");
        this.tailTip = tail.getChild("tail_tip");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        Transformation active = TransformationManager.active(player);
        if (active == null) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
        if (active == GLTransformations.LEOPARD_HYBRID.get()) {
            drawShell(pose, buffers, light, overlay, SPOTS_TEXTURE, 0xFFFFFFFF);
            drawHybridParts(pose, buffers, light, overlay, ageInTicks, limbSwingAmount);
        } else if (active == GLTransformations.GEAR_SECOND.get()) {
            float pulse = 0.35f + 0.1f * Mth.sin(ageInTicks * 0.5f);
            drawShell(pose, buffers, light, overlay, player.getSkin().texture(), argb(pulse, 0xFF5A6A));
        } else if (active == GLTransformations.GEAR_FIFTH.get()) {
            drawShell(pose, buffers, light, overlay, player.getSkin().texture(), argb(0.75f, 0xFFFFFF));
        } else {
            ZoanLooks.Hybrid hybrid = ZoanLooks.hybrid(active.id());
            if (hybrid != null) {
                drawShell(pose, buffers, light, overlay, hybrid.overlay(), 0xFFFFFFFF);
                BeastModel parts = ZoanLooks.model(hybrid.parts());
                parts.attackTime = player.getAttackAnim(partialTick);
                parts.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                parts.renderOn(getParentModel(), pose, buffers.getBuffer(RenderType.entityCutoutNoCull(hybrid.parts().texture())),
                        hybrid.glowing() ? LightTexture.FULL_BRIGHT : light, overlay);
            }
        }
    }

    private void drawShell(PoseStack pose, MultiBufferSource buffers, int light, int overlay, ResourceLocation texture, int color) {
        getParentModel().copyPropertiesTo(shell);
        shell.jacket.visible = false;
        shell.leftSleeve.visible = false;
        shell.rightSleeve.visible = false;
        shell.leftPants.visible = false;
        shell.rightPants.visible = false;
        shell.hat.visible = false;
        shell.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay, color);
    }

    private void drawHybridParts(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age, float speed) {
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(LEOPARD_TEXTURE));
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        ears.render(pose, buffer, light, overlay);
        pose.popPose();

        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        tail.xRot = 0.9f - Math.min(1f, speed) * 0.5f + Mth.sin(age * 0.08f) * 0.08f;
        tail.yRot = Mth.sin(age * 0.12f) * 0.4f;
        tailTip.xRot = -0.6f + Mth.sin(age * 0.15f + 1) * 0.15f;
        tail.render(pose, buffer, light, overlay);
        pose.popPose();
    }

    private static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
