package com.nerodev.grandline.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.boss.BossMove;
import com.nerodev.grandline.boss.BossMoves;
import com.nerodev.grandline.boss.SeaKingEntity;
import com.nerodev.grandline.client.model.BeastModel;
import com.nerodev.grandline.client.model.BeastShape;
import com.nerodev.grandline.client.model.ZoanLooks;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Sea Kings are drawn as their generated serpent body, swaying as they swim, jaws opening as they bite, rearing up to
 * spit water and diving head first.
 */
public class SeaKingRenderer extends MobRenderer<SeaKingEntity, SeaKingRenderer.Model> {
    private static final ResourceLocation MISSING = GrandLine.id("textures/entity/zoan/boss_sea_king_body.png");

    public SeaKingRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 1.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SeaKingEntity boss) {
        BeastShape body = ZoanLooks.bossBody(boss.parts());
        return body == null ? MISSING : body.texture();
    }

    @Override
    protected void scale(SeaKingEntity boss, PoseStack pose, float partialTick) {
        float size = boss.scale();
        pose.scale(size, size, size);
        shadowRadius = 1.2f * size;
    }

    @Override
    protected void setupRotations(SeaKingEntity boss, PoseStack pose, float bob, float bodyYaw, float partialTick, float scale) {
        super.setupRotations(boss, pose, bob, bodyYaw, partialTick, scale);
        BossMove move = boss.currentMove();
        if (move == null) {
            return;
        }
        float t = boss.moveTime(partialTick);
        float w = Mth.clamp(t / Math.max(1, move.windup()), 0f, 1f);
        float done = Mth.clamp((t - move.windup() - move.active()) / Math.max(1, move.recovery()), 0f, 1f);
        float hold = 1f - done;
        if (move == BossMoves.WATER_JET || move == BossMoves.SEA_BITE) {
            // Rear up out of the water.
            pose.mulPose(Axis.XP.rotationDegrees(-25f * w * hold));
        } else if (move == BossMoves.DIVE) {
            pose.mulPose(Axis.XP.rotationDegrees((t < move.windup() + move.active() ? 60f : -50f) * Math.min(1f, t / 6f) * hold));
        } else if (move == BossMoves.TAIL_SWEEP && t >= move.windup()) {
            pose.mulPose(Axis.YP.rotationDegrees(Math.min(1f, (t - move.windup()) / 3f) * 360f));
        }
    }

    /** Wraps the generated body model for the Sea King entity. */
    public static final class Model extends EntityModel<SeaKingEntity> {
        @Nullable
        private BeastModel body;

        @Override
        public void prepareMobModel(SeaKingEntity boss, float limbSwing, float limbSwingAmount, float partialTick) {
            BeastShape shape = ZoanLooks.bossBody(boss.parts());
            body = shape == null ? null : ZoanLooks.model(shape);
            if (body != null) {
                BossMove move = boss.currentMove();
                float t = boss.moveTime(partialTick);
                // The jaw (driven by the attack swing) gapes through a bite's windup and snaps shut as it lands.
                body.attackTime = move == BossMoves.SEA_BITE ? Mth.clamp(t / move.windup(), 0f, 1f) * (t < move.windup() + 2 ? 1f : 0f)
                        : move == BossMoves.WATER_JET && t >= move.windup() ? 0.8f : 0f;
            }
        }

        @Override
        public void setupAnim(SeaKingEntity boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            if (body != null) {
                // Always undulating a little, more as it swims.
                body.animate(false, false, 0f, ageInTicks * 0.6f + limbSwing, Math.max(0.35f, limbSwingAmount), ageInTicks, netHeadYaw,
                        headPitch);
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            if (body != null) {
                body.renderToBuffer(pose, buffer, light, overlay, color);
            }
        }
    }
}
