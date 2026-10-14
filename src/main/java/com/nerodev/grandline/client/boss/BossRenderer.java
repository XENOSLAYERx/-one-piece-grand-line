package com.nerodev.grandline.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.boss.BossMove;
import com.nerodev.grandline.boss.BossMoves;
import com.nerodev.grandline.client.model.BeastModel;
import com.nerodev.grandline.client.model.BeastShape;
import com.nerodev.grandline.client.model.ZoanLooks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Bosses are drawn on the player model with their own skin, grown to their size, wearing their features (Morgan's
 * iron jaw, Arlong's saw nose and fins) and posed through every move: the wind-up the player should read, the blow,
 * and the recovery. Spins turn the whole body; Shark on Darts lays it out flat like a torpedo.
 */
public class BossRenderer extends HumanoidMobRenderer<BossEntity, BossRenderer.Model> {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    public BossRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new PartsLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(BossEntity boss) {
        return TEXTURES.computeIfAbsent(boss.skin(), skin -> GrandLine.id("textures/entity/npc/" + skin + ".png"));
    }

    @Override
    protected void scale(BossEntity boss, PoseStack pose, float partialTick) {
        float size = boss.scale();
        pose.scale(size, size, size);
        shadowRadius = 0.5f * size;
    }

    @Override
    protected void setupRotations(BossEntity boss, PoseStack pose, float bob, float bodyYaw, float partialTick, float scale) {
        super.setupRotations(boss, pose, bob, bodyYaw, partialTick, scale);
        BossMove move = boss.currentMove();
        if (move == null) {
            return;
        }
        float t = boss.moveTime(partialTick);
        float into = t - move.windup();
        boolean active = into >= 0 && into < move.active();
        if (active && (move == BossMoves.WHIRLWIND || move == BossMoves.KIRIBACHI_SPIN)) {
            pose.mulPose(Axis.YP.rotationDegrees(into * (move == BossMoves.KIRIBACHI_SPIN ? 46f : 38f)));
        } else if (move == BossMoves.SHARK_ON_DARTS && into >= 0 && into < move.active() + 4) {
            float flat = Mth.clamp(into / 3f, 0f, 1f) * (into > move.active() ? 1f - (into - move.active()) / 4f : 1f);
            float size = boss.scale();
            pose.mulPose(Axis.XP.rotationDegrees(-90f * flat));
            pose.translate(0f, -1.0f * size * flat, 0.3f * size * flat);
            pose.mulPose(Axis.YP.rotationDegrees(into * 50f * flat));
        }
    }

    /** The player model, posed for the boss's current move. */
    public static final class Model extends PlayerModel<BossEntity> {
        Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void prepareMobModel(BossEntity boss, float limbSwing, float limbSwingAmount, float partialTick) {
            rightArmPose = boss.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            leftArmPose = HumanoidModel.ArmPose.EMPTY;
            super.prepareMobModel(boss, limbSwing, limbSwingAmount, partialTick);
        }

        @Override
        public void setupAnim(BossEntity boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(boss, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            BossMove move = boss.currentMove();
            if (move != null) {
                float t = boss.moveTime(ageInTicks - boss.tickCount);
                pose(move, t, ageInTicks);
                hat.copyFrom(head);
                jacket.copyFrom(body);
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
                rightPants.copyFrom(rightLeg);
                leftPants.copyFrom(leftLeg);
            }
        }

        /**
         * How far into each stage of a move: {@code w} rises 0..1 through the windup, {@code a} through the active
         * stretch, {@code r} through the recovery; {@code hold} is 1 until recovery and then eases back to 0.
         */
        private void pose(BossMove move, float t, float ageInTicks) {
            float w = Mth.clamp(t / Math.max(1, move.windup()), 0f, 1f);
            float a = Mth.clamp((t - move.windup()) / Math.max(1, move.active()), 0f, 1f);
            float r = Mth.clamp((t - move.windup() - move.active()) / Math.max(1, move.recovery()), 0f, 1f);
            float hold = 1f - ease(r);
            float swing = ease(a);
            // Blows that land on the first active tick are swung through in two ticks, so the pose meets the hit.
            float strike = ease(Mth.clamp((t - move.windup()) / 2f, 0f, 1f));
            switch (move.id()) {
                case "knife_throw" -> {
                    // A knife drawn back past the ear, then flicked out; one throw per two ticks.
                    float flick = t >= move.windup() && t < move.windup() + move.active() ? Mth.sin((t - move.windup()) * Mth.PI) : 0f;
                    rightArm.xRot = (-2.4f * ease(w) + 1.2f * Math.abs(flick)) * hold;
                    rightArm.yRot = -0.3f * hold;
                    twist(0.3f * ease(w) * hold);
                }
                case "bara_bara_cannon", "buggy_ball" -> {
                    // Arm thrown out at the prey (Buggy's fist flies from it).
                    rightArm.xRot = (-1.6f * ease(w) - 0.1f * strike) * hold;
                    rightArm.yRot = -0.1f * hold;
                    leftArm.xRot = -0.6f * ease(w) * hold;
                    lean(0.2f * strike * hold);
                }
                case "claw_rake" -> {
                    // Both claws raised, then raked down across.
                    rightArm.xRot = Mth.lerp(strike, -2.4f * ease(w), -0.6f) * hold;
                    leftArm.xRot = Mth.lerp(strike, -2.4f * ease(w), -0.6f) * hold;
                    rightArm.zRot = 0.3f * hold;
                    leftArm.zRot = -0.3f * hold;
                    lean(0.4f * strike * hold);
                }
                case "silent_step", "shakushi" -> {
                    // Low and swaying, claws swept back for speed.
                    float sway = move.id().equals("shakushi") ? Mth.sin(ageInTicks * 0.9f) * 0.3f : 0f;
                    lean(0.7f * ease(w) * hold);
                    rightArm.xRot = (1.2f + sway) * ease(w) * hold;
                    leftArm.xRot = (1.2f - sway) * ease(w) * hold;
                    rightArm.zRot = 0.4f * hold;
                    leftArm.zRot = -0.4f * hold;
                    head.xRot = -0.3f * hold;
                }
                case "cleave", "saw_sweep" -> {
                    // Weapon drawn back over the shoulder, then swept across the body.
                    float back = ease(w) * (1 - strike);
                    twist((0.7f * back - 0.8f * strike) * hold);
                    rightArm.xRot = Mth.lerp(strike, -2.3f * ease(w), -1.2f) * hold;
                    rightArm.yRot += (0.7f * back - 1.1f * strike) * hold;
                    leftArm.xRot = -0.5f * ease(w) * hold;
                }
                case "slam" -> {
                    // Both hands up over the head, then brought down with the whole body behind it.
                    float up = ease(w) * (1 - strike);
                    rightArm.xRot = (-3.0f * up - 0.7f * strike) * hold;
                    leftArm.xRot = (-3.0f * up - 0.7f * strike) * hold;
                    rightArm.yRot = leftArm.yRot = 0f;
                    lean((0.9f * strike - 0.3f * up) * hold);
                }
                case "charge" -> {
                    // Head down and arms back, then a bull's run.
                    float run = t >= move.windup() && t < move.windup() + move.active() ? 1f : 0f;
                    lean(0.8f * ease(w) * hold);
                    rightArm.xRot = 0.9f * ease(w) * hold + run * Mth.cos(ageInTicks * 1.4f) * 0.9f;
                    leftArm.xRot = 0.9f * ease(w) * hold - run * Mth.cos(ageInTicks * 1.4f) * 0.9f;
                    rightArm.zRot = 0.3f * hold;
                    leftArm.zRot = -0.3f * hold;
                    head.xRot = -0.3f * hold;
                }
                case "jaw_bash" -> {
                    // Head reared back, then driven forward like a hammer.
                    head.xRot = (-0.8f * ease(w) * (1 - strike) + 0.7f * strike) * hold;
                    lean((-0.25f * ease(w) * (1 - strike) + 0.5f * strike) * hold);
                    rightArm.xRot += 0.4f * hold;
                    leftArm.xRot += 0.4f * hold;
                }
                case "reinforcements" -> {
                    // An arm thrown up to call the Marines.
                    rightArm.xRot = -2.9f * ease(w) * hold;
                    rightArm.zRot = 0.2f * hold;
                    head.xRot = -0.3f * hold;
                }
                case "whirlwind", "kiribachi_spin" -> {
                    // Arms held straight out; the renderer turns the body.
                    float out = ease(w) * hold;
                    rightArm.xRot = -0.2f * out;
                    rightArm.zRot = 1.45f * out;
                    leftArm.xRot = -0.2f * out;
                    leftArm.zRot = -1.45f * out;
                    lean(0.15f * out);
                }
                case "roar" -> {
                    // Chest out, arms flung wide and down, head thrown back.
                    float roar = Math.min(1f, t / 5f) * (1f - Mth.clamp((t - move.length() + 6) / 6f, 0f, 1f));
                    rightArm.xRot = 0.4f * roar;
                    rightArm.zRot = 0.9f * roar;
                    leftArm.xRot = 0.4f * roar;
                    leftArm.zRot = -0.9f * roar;
                    head.xRot = -0.7f * roar + Mth.sin(ageInTicks * 2.5f) * 0.05f * roar;
                    lean(-0.2f * roar);
                }
                case "guard" -> {
                    // Forearms crossed in front of the face.
                    float up = Math.min(1f, t / 3f) * hold;
                    rightArm.xRot = -1.5f * up;
                    rightArm.yRot = -0.6f * up;
                    leftArm.xRot = -1.5f * up;
                    leftArm.yRot = 0.6f * up;
                    lean(0.15f * up);
                }
                case "bite" -> {
                    // Reared back, then the lunge with jaws first and hands reaching.
                    lean((-0.3f * ease(w) * (1 - swing) + 0.7f * swing) * hold);
                    head.xRot = (-0.6f * ease(w) * (1 - swing) + 0.4f * swing) * hold;
                    rightArm.xRot = -1.6f * swing * hold;
                    leftArm.xRot = -1.6f * swing * hold;
                }
                case "shark_on_darts" -> {
                    // A crouch, then arms swept back along the body while it flies like a torpedo.
                    boolean flying = t >= move.windup() && t < move.windup() + move.active();
                    lean((flying ? 0f : 0.7f * ease(w)) * hold);
                    float arms = flying ? 1f : 0.8f * ease(w) * hold;
                    rightArm.xRot = (flying ? -3.0f : 0.9f) * arms;
                    leftArm.xRot = (flying ? -3.0f : 0.9f) * arms;
                    rightArm.zRot = 0f;
                    leftArm.zRot = 0f;
                }
                case "tooth_gun" -> {
                    // Fingers at the jaw, flicking teeth out.
                    float aim = ease(w) * hold;
                    rightArm.xRot = -1.9f * aim + (a > 0 && a < 1 ? Mth.sin(ageInTicks * 3f) * 0.25f : 0f);
                    rightArm.yRot = -0.5f * aim;
                    head.xRot = 0.1f * aim;
                }
                default -> {
                }
            }
        }

        private static float ease(float x) {
            return x * x * (3 - 2 * x);
        }

        /** Turns the upper body about the spine, carrying the shoulders with it (as the vanilla attack swing does). */
        private void twist(float yaw) {
            body.yRot = yaw;
            rightArm.z = Mth.sin(yaw) * 5f;
            rightArm.x = -Mth.cos(yaw) * 5f;
            leftArm.z = -Mth.sin(yaw) * 5f;
            leftArm.x = Mth.cos(yaw) * 5f;
            rightArm.yRot += yaw;
            leftArm.yRot += yaw;
        }

        /** Bends forward at the waist ({@code amount} 1 is a sneak's crouch; negative leans back). */
        private void lean(float amount) {
            body.xRot = 0.5f * amount;
            float forward = Math.max(0f, amount);
            body.y = 3.2f * forward;
            head.y = 4.2f * forward;
            rightArm.y = 2f + 3.2f * forward;
            leftArm.y = 2f + 3.2f * forward;
            rightArm.xRot += 0.4f * amount;
            leftArm.xRot += 0.4f * amount;
            rightLeg.z = 0.1f + 4f * forward;
            leftLeg.z = 0.1f + 4f * forward;
            rightLeg.y = 12f + 0.2f * forward;
            leftLeg.y = 12f + 0.2f * forward;
        }
    }

    /** The boss's own features, riding the bones they are anchored to. */
    private static final class PartsLayer extends RenderLayer<BossEntity, Model> {
        PartsLayer(RenderLayerParent<BossEntity, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, BossEntity boss, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            BeastShape parts = ZoanLooks.bossParts(boss.parts());
            if (parts == null || boss.isInvisible()) {
                return;
            }
            BeastModel model = ZoanLooks.model(parts);
            model.attackTime = 0f;
            model.animate(false, !boss.onGround() && !boss.isInWater(), (float) boss.getDeltaMovement().y, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch);
            model.renderOn(getParentModel(), pose, buffers.getBuffer(RenderType.entityCutoutNoCull(parts.texture())), light,
                    LivingEntityRenderer.getOverlayCoords(boss, 0));
        }
    }
}
