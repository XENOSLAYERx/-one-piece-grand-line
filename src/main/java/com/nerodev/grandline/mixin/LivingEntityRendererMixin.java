package com.nerodev.grandline.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nerodev.grandline.client.anim.PlayerAnimator;
import com.nerodev.grandline.client.haki.HakiClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies whole-body animation (lean, spin, offset) and the knocked-out topple inside the renderer's own pose
 * push/pop.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void grandline$applyRootAnimation(LivingEntity entity, PoseStack poseStack, float bob, float yBodyRot,
            float partialTick, float scale, CallbackInfo ci) {
        PlayerAnimator.applyRoot(entity, poseStack, partialTick);
        HakiClient.applyKnockout(entity, poseStack, partialTick);
    }
}
