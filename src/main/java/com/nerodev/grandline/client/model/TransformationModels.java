package com.nerodev.grandline.client.model;

import com.nerodev.grandline.GrandLine;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Model layers used to draw transformations on top of the normal player model. */
public final class TransformationModels {
    /** Slightly inflated player shell for tints and fur overlays (wide and slim arms). */
    public static final ModelLayerLocation SHELL_WIDE = new ModelLayerLocation(GrandLine.id("player_shell"), "wide");
    public static final ModelLayerLocation SHELL_SLIM = new ModelLayerLocation(GrandLine.id("player_shell"), "slim");
    /** Leopard ears and tail for hybrid form, sharing the leopard texture layout. */
    public static final ModelLayerLocation HYBRID_PARTS = new ModelLayerLocation(GrandLine.id("leopard_hybrid"), "main");

    private TransformationModels() {}

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LeopardModel.LAYER, LeopardModel::createLayer);
        event.registerLayerDefinition(SHELL_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.12f), false), 64, 64));
        event.registerLayerDefinition(SHELL_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.12f), true), 64, 64));
        event.registerLayerDefinition(HYBRID_PARTS, TransformationModels::hybridParts);
    }

    private static LayerDefinition hybridParts() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Ears sit on top of the head (player head pivot is the neck; the head box spans y -8..0).
        root.addOrReplaceChild("ears", CubeListBuilder.create()
                        .texOffs(40, 26).addBox(-3.5f, -10, -1, 2, 2, 1)
                        .texOffs(46, 26).addBox(1.5f, -10, -1, 2, 2, 1),
                PartPose.ZERO);
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 52).addBox(-1, -1, 0, 2, 2, 9),
                PartPose.offsetAndRotation(0, 10.5f, 2, 0.9f, 0, 0));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(22, 52).addBox(-1, -1, 0, 2, 2, 8),
                PartPose.offsetAndRotation(0, 0, 8.5f, -0.6f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
