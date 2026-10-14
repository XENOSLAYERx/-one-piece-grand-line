package com.nerodev.grandline.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nerodev.grandline.client.model.BeastModel;
import com.nerodev.grandline.client.model.BeastShape;
import com.nerodev.grandline.client.model.TransformationModels;
import com.nerodev.grandline.client.model.ZoanLooks;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.race.RaceAbilities;
import com.nerodev.grandline.transform.TransformationManager;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/**
 * Draws a player's race features on the player model (a Mink's ears and tail, a Fish-Man's fins, a Skypiean's wings)
 * and the white, moonlit glow of a Mink in Sulong form.
 */
public class RaceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final PlayerModel<AbstractClientPlayer> shell;

    public RaceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models,
            boolean slim) {
        super(parent);
        this.shell = new PlayerModel<>(models.bakeLayer(slim ? TransformationModels.SHELL_SLIM : TransformationModels.SHELL_WIDE), slim);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
        boolean sulong = TransformationManager.active(player) == RaceAbilities.SULONG_BODY.get();
        if (sulong) {
            getParentModel().copyPropertiesTo(shell);
            shell.hat.visible = false;
            shell.jacket.visible = false;
            shell.leftSleeve.visible = false;
            shell.rightSleeve.visible = false;
            shell.leftPants.visible = false;
            shell.rightPants.visible = false;
            float pulse = 0.5f + 0.12f * Mth.sin(ageInTicks * 0.3f);
            int color = ((int) (pulse * 255) << 24) | (RaceAbilities.SULONG & 0xFFFFFF);
            shell.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())),
                    LightTexture.FULL_BRIGHT, overlay, color);
        }
        BeastShape parts = ZoanLooks.raceParts(PublicState.of(player).race());
        if (parts == null) {
            return;
        }
        BeastModel model = ZoanLooks.model(parts);
        model.attackTime = player.getAttackAnim(partialTick);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.renderOn(getParentModel(), pose, buffers.getBuffer(RenderType.entityCutoutNoCull(parts.texture())),
                sulong ? LightTexture.FULL_BRIGHT : light, overlay);
    }
}
