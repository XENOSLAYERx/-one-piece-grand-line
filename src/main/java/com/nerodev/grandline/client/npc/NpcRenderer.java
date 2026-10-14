package com.nerodev.grandline.client.npc;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.weapon.GunItem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Islanders are drawn on the player model with their own skin (both layers: hats, coats and capes included), their
 * armour and what they hold; gunmen raise their pistol or shoulder their musket while they take aim.
 */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, NpcRenderer.Model> {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity npc) {
        return TEXTURES.computeIfAbsent(npc.skin(), skin -> GrandLine.id("textures/entity/npc/" + skin + ".png"));
    }

    /** The player model, posing the arms for what the islander holds. */
    public static final class Model extends PlayerModel<NpcEntity> {
        Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void prepareMobModel(NpcEntity npc, float limbSwing, float limbSwingAmount, float partialTick) {
            ItemStack held = npc.getMainHandItem();
            leftArmPose = npc.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            if (npc.aiming() && held.getItem() instanceof GunItem gun) {
                rightArmPose = gun.longGun() ? HumanoidModel.ArmPose.CROSSBOW_HOLD : HumanoidModel.ArmPose.BOW_AND_ARROW;
            } else {
                rightArmPose = held.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            }
            super.prepareMobModel(npc, limbSwing, limbSwingAmount, partialTick);
        }
    }
}
