package com.nerodev.grandline.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.data.PublicState;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Haki on the player model. Armament blackens the arms (or the whole body when fully hardened) with a glossy
 * violet sheen sliding over it; Conqueror's infusion adds a crimson crackle over the arms.
 * <p>
 * Drawn on the model's outer layer (sleeves, jacket, trousers, hat): those boxes sit just outside the skin, so the
 * coating covers it without z-fighting, whatever the player's skin customisation hides.
 */
public class HakiLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation ARMAMENT = GrandLine.id("textures/entity/haki_armament.png");
    public static final ResourceLocation SHEEN = GrandLine.id("textures/entity/haki_sheen.png");

    public HakiLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static int bits(Player player) {
        return player.hasData(GLAttachments.PUBLIC_STATE) ? PublicState.of(player).haki() : 0;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int bits = bits(player);
        if (bits == 0 || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean full = (bits & PublicState.HAKI_FULL_BODY) != 0;
        boolean arms = full || (bits & PublicState.HAKI_ARM) != 0;
        boolean infused = (bits & PublicState.HAKI_INFUSION) != 0;
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
        List<ModelPart> armParts = List.of(model.rightSleeve, model.leftSleeve);
        if (arms) {
            List<ModelPart> parts = full
                    ? List.of(model.rightSleeve, model.leftSleeve, model.jacket, model.rightPants, model.leftPants, model.hat)
                    : armParts;
            draw(parts, pose, buffers.getBuffer(RenderType.entityTranslucent(ARMAMENT)), light, overlay, 0xF0FFFFFF);
            float scroll = ageInTicks * 0.01f;
            draw(parts, pose, buffers.getBuffer(RenderType.energySwirl(SHEEN, scroll, scroll * 0.5f)), light,
                    OverlayTexture.NO_OVERLAY, additive(0.55f, 0x9A7CFF));
        }
        if (infused) {
            float pulse = 0.55f + 0.25f * Mth.sin(ageInTicks * 0.7f);
            float scroll = ageInTicks * 0.04f;
            draw(armParts, pose, buffers.getBuffer(RenderType.energySwirl(SHEEN, -scroll, scroll)), light,
                    OverlayTexture.NO_OVERLAY, additive(pulse, 0xFF2030));
        }
    }

    /** Renders outer-layer parts even if the skin settings hide them, restoring visibility afterwards. */
    static void draw(List<ModelPart> parts, PoseStack pose, VertexConsumer buffer, int light,
            int overlay, int color) {
        for (ModelPart part : parts) {
            boolean visible = part.visible;
            part.visible = true;
            part.render(pose, buffer, light, overlay, color);
            part.visible = visible;
        }
    }

    /** Colour for additive render types (energy swirl), which ignore alpha: the RGB itself is dimmed. */
    static int additive(float strength, int rgb) {
        float k = Mth.clamp(strength, 0, 1);
        int r = (int) (((rgb >> 16) & 0xFF) * k);
        int g = (int) (((rgb >> 8) & 0xFF) * k);
        int b = (int) ((rgb & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
