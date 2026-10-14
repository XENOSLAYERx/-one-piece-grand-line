package com.nerodev.grandline.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nerodev.grandline.ship.ShipDefinition;
import com.nerodev.grandline.ship.ShipEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws ships from their generated meshes, rolling gently on the swell and shuddering when hit. */
public class ShipRenderer extends EntityRenderer<ShipEntity> {
    private static final Map<ShipDefinition, ShipMesh> MESHES = new HashMap<>();

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    /** The mesh for a definition, built on first use (and again if a datapack changes the definition). */
    public static ShipMesh mesh(ShipDefinition def) {
        return MESHES.computeIfAbsent(def, ShipMesh::build);
    }

    public static void clearMeshes() {
        MESHES.clear();
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity ship) {
        return ShipMesh.ATLAS;
    }

    @Override
    public void render(ShipEntity ship, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ShipDefinition def = ship.definition();
        if (def == null) {
            return;
        }
        float time = ship.tickCount + partialTick;
        float heading = Mth.rotLerp(partialTick, ship.yRotO, ship.getYRot());
        float roll = Mth.sin(time * 0.05f) * 1.2f * Math.min(1f, 8f / def.length() + 0.3f);
        float pitch = Mth.sin(time * 0.037f) * 0.6f;
        int hurt = ship.hurtTicks();
        if (hurt > 0) {
            roll += Mth.sin((hurt - partialTick) * 1.4f) * hurt * 0.35f;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-heading));
        pose.translate(0, def.draft() * 0.5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.translate(0, -def.draft() * 0.5, 0);
        int overlay = hurt > 6 ? OverlayTexture.pack(0, true) : OverlayTexture.NO_OVERLAY;
        mesh(def).render(pose, buffers, light, overlay, time, ship.speed(), ship.crewFlag().orElse(null));
        pose.popPose();
        super.render(ship, yaw, partialTick, pose, buffers, light);
    }
}
