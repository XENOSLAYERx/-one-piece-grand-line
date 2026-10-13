package com.nerodev.grandline.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.GrandLine;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Full leopard form. An original quadruped model (head with ears and muzzle, long body, four legs, two-part tail)
 * with its own 64x64 texture. Animated from the player's walk cycle, head look and air time.
 */
public class LeopardModel extends EntityModel<AbstractClientPlayer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GrandLine.id("leopard"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart frontRight;
    private final ModelPart frontLeft;
    private final ModelPart backRight;
    private final ModelPart backLeft;
    private final ModelPart tail;
    private final ModelPart tailTip;

    public LeopardModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.frontRight = root.getChild("front_right");
        this.frontLeft = root.getChild("front_left");
        this.backRight = root.getChild("back_right");
        this.backLeft = root.getChild("back_left");
        this.tail = root.getChild("tail");
        this.tailTip = tail.getChild("tail_tip");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -9, 8, 8, 18),
                PartPose.offset(0, 11, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 26).addBox(-4, -4, -6, 8, 7, 6)
                        .texOffs(28, 26).addBox(-2, 0, -8, 4, 3, 2)
                        .texOffs(40, 26).addBox(-3.5f, -6, -3, 2, 2, 1)
                        .texOffs(46, 26).addBox(1.5f, -6, -3, 2, 2, 1),
                PartPose.offset(0, 9, -8.5f));
        root.addOrReplaceChild("front_right", CubeListBuilder.create().texOffs(0, 39).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(-2.5f, 14, -6.5f));
        root.addOrReplaceChild("front_left", CubeListBuilder.create().texOffs(12, 39).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(2.5f, 14, -6.5f));
        root.addOrReplaceChild("back_right", CubeListBuilder.create().texOffs(24, 39).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(-2.5f, 14, 7));
        root.addOrReplaceChild("back_left", CubeListBuilder.create().texOffs(36, 39).addBox(-1.5f, 0, -1.5f, 3, 10, 3),
                PartPose.offset(2.5f, 14, 7));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 52).addBox(-1, -1, 0, 2, 2, 9),
                PartPose.offsetAndRotation(0, 8.5f, 8.5f, -0.75f, 0, 0));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(22, 52).addBox(-1, -1, 0, 2, 2, 8),
                PartPose.offsetAndRotation(0, 0, 8.5f, 0.9f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float swing = limbSwing * 0.9f;
        float amount = Math.min(1f, limbSwingAmount * 1.4f);
        boolean airborne = !player.onGround() && !player.isInWater();

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.8f;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.8f;

        if (airborne) {
            // Leap pose: front legs reach forward, back legs trail.
            frontRight.xRot = frontLeft.xRot = -1.1f;
            backRight.xRot = backLeft.xRot = 1.0f;
            body.xRot = Mth.clamp((float) -player.getDeltaMovement().y * 0.6f, -0.35f, 0.35f);
        } else {
            // Gallop-ish gait: diagonal pairs, faster legs at speed.
            frontRight.xRot = Mth.cos(swing) * 1.1f * amount;
            backLeft.xRot = Mth.cos(swing) * 1.1f * amount;
            frontLeft.xRot = Mth.cos(swing + Mth.PI) * 1.1f * amount;
            backRight.xRot = Mth.cos(swing + Mth.PI) * 1.1f * amount;
            body.xRot = Mth.sin(swing * 2) * 0.04f * amount;
        }

        // Crouch = stalking low.
        float crouch = player.isCrouching() ? 3f : 0f;
        body.y = 11 + crouch;
        head.y = 9 + crouch;
        tail.y = 8.5f + crouch;

        tail.xRot = -0.75f + Mth.sin(ageInTicks * 0.08f) * 0.08f + amount * 0.5f;
        tail.yRot = Mth.sin(ageInTicks * 0.12f) * 0.35f;
        tailTip.xRot = 0.9f - amount * 0.6f + Mth.sin(ageInTicks * 0.15f + 1) * 0.12f;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
