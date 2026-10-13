package com.nerodev.grandline.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.client.model.BeastShape.Anchor;
import com.nerodev.grandline.client.model.BeastShape.Part;
import com.nerodev.grandline.client.model.BeastShape.Role;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * A Zoan body built from a {@link BeastShape}. Every part is animated by its role from the player's own movement: legs
 * walk in gait (front and back pairs, bipeds, eight spider legs), serpent bodies slither in a travelling wave, wings
 * fold at rest and beat in flight, tails and ears sway, the head follows where the player looks and jaws snap on attack.
 */
public class BeastModel extends EntityModel<AbstractClientPlayer> {
    private final BeastShape shape;
    private final ModelPart root;
    private final ModelPart[] parts;
    private final float[][] base;

    public BeastModel(ModelPart root, BeastShape shape) {
        this.shape = shape;
        this.root = root;
        List<Part> defs = shape.parts();
        this.parts = new ModelPart[defs.size()];
        this.base = new float[defs.size()][];
        for (int i = 0; i < defs.size(); i++) {
            Part p = defs.get(i);
            ModelPart parent = p.parent() < 0 ? root : parts[p.parent()];
            parts[i] = parent.getChild(p.name());
            base[i] = new float[] {p.px(), p.py(), p.pz(), p.rx(), p.ry(), p.rz()};
        }
    }

    public BeastShape shape() {
        return shape;
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        float swing = limbSwing * 0.9f * shape.gait();
        float amount = Math.min(1f, limbSwingAmount * 1.4f);
        boolean flying = player.getAbilities().flying || (!player.onGround() && !player.isInWater() && player.getDeltaMovement().y < -0.25);
        boolean airborne = !player.onGround() && !player.isInWater() && !player.onClimbable();
        float fall = (float) player.getDeltaMovement().y;
        float attack = this.attackTime;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float flap = ageInTicks * 0.55f;
        List<Part> defs = shape.parts();
        for (int i = 0; i < parts.length; i++) {
            ModelPart m = parts[i];
            float[] b = base[i];
            m.x = b[0];
            m.y = b[1];
            m.z = b[2];
            m.xRot = b[3];
            m.yRot = b[4];
            m.zRot = b[5];
            Part p = defs.get(i);
            float k = p.param();
            switch (p.role()) {
                case BODY -> {
                    if (airborne && !flying) {
                        m.xRot = b[3] + Mth.clamp(-fall * 0.6f, -0.35f, 0.35f);
                    } else {
                        m.y = b[1] + Mth.sin(swing * 2) * 0.35f * amount;
                    }
                }
                case HEAD, NECK -> {
                    m.yRot = b[4] + yaw * k;
                    m.xRot = b[3] + pitch * k;
                }
                case JAW -> m.xRot = b[3] + attack * 0.8f + Math.max(0f, Mth.sin(ageInTicks * 0.05f) - 0.96f) * 6f;
                case LEG_FRONT, LEG_BACK, LEG -> {
                    if (flying && p.role() == Role.LEG) {
                        m.xRot = b[3] + 1.2f;
                    } else if (airborne) {
                        m.xRot = b[3] + (p.role() == Role.LEG_FRONT ? -0.9f : p.role() == Role.LEG_BACK ? 0.8f : 0.35f);
                    } else {
                        m.xRot = b[3] + Mth.cos(swing + k) * 0.9f * amount;
                    }
                }
                case ARM -> {
                    m.xRot = b[3] + Mth.cos(swing + k) * 0.6f * amount - (k == 0 ? attack * 1.4f : 0f);
                    m.zRot = b[5] + Mth.sin(ageInTicks * 0.07f + k) * 0.04f;
                }
                case TAIL -> {
                    m.yRot = b[4] + Mth.sin(ageInTicks * 0.1f + k * 0.6f) * (0.12f + 0.06f * Math.min(k, 3f)) * (1f + amount);
                    if (k == 0) {
                        m.xRot = b[3] + amount * 0.25f;
                    }
                }
                case WING -> {
                    if (flying) {
                        m.xRot = 0f;
                        m.yRot = -k * 0.15f;
                        m.zRot = k * (Mth.sin(flap) * 0.75f - 0.1f);
                    }
                }
                case WING_TIP -> {
                    if (flying) {
                        m.y = 0f;
                        m.xRot = 0f;
                        m.yRot = 0f;
                        m.zRot = k * Mth.sin(flap - 0.7f) * 0.45f;
                    }
                }
                case SEGMENT -> m.yRot = b[4] + Mth.sin(swing * 1.2f + ageInTicks * 0.08f - k * 0.75f) * (0.05f + 0.18f * amount);
                case SPIDER_LEG -> {
                    m.yRot = b[4] + Mth.cos(swing * 1.5f + k) * 0.3f * amount;
                    m.zRot = b[5] + Mth.sin(swing * 1.5f + k) * 0.2f * amount;
                }
                case EAR -> m.zRot = b[5] + Mth.sin(ageInTicks * 0.07f + k) * 0.05f;
                case FIN -> m.yRot = b[4] + Mth.sin(ageInTicks * 0.15f + k) * 0.15f;
                case FIXED -> {
                }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }

    /** Draws a hybrid's parts on the player model: each top-level part rides the bone it is anchored to. */
    public void renderOn(PlayerModel<?> player, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        List<Part> defs = shape.parts();
        for (int i = 0; i < parts.length; i++) {
            Part p = defs.get(i);
            if (p.parent() >= 0) {
                continue;
            }
            ModelPart bone = bone(player, p.anchor());
            pose.pushPose();
            if (bone != null) {
                bone.translateAndRotate(pose);
            }
            parts[i].render(pose, buffer, light, overlay);
            pose.popPose();
        }
    }

    private static ModelPart bone(PlayerModel<?> player, Anchor anchor) {
        return switch (anchor) {
            case HEAD -> player.head;
            case BODY -> player.body;
            case RIGHT_ARM -> player.rightArm;
            case LEFT_ARM -> player.leftArm;
            case RIGHT_LEG -> player.rightLeg;
            case LEFT_LEG -> player.leftLeg;
            case NONE -> null;
        };
    }
}
