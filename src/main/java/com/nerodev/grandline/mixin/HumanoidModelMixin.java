package com.nerodev.grandline.mixin;

import com.nerodev.grandline.client.anim.PlayerAnimator;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets {@link PlayerAnimator} pose humanoid models after vanilla has set up its own animation.
 *
 * <p>Vanilla resets every rotation and some positions each frame but not all (the body's and head's x and z, the
 * arms' outside an attack swing), and one model is shared by every entity it draws. So each frame starts from the
 * resting positions the model was built with: offsets an animation adds then last that frame only, instead of piling
 * up until the torso floats away from the legs.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Unique
    private float[] grandline$rest;

    @Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;Ljava/util/function/Function;)V", at = @At("TAIL"))
    private void grandline$recordRest(ModelPart root, Function<ResourceLocation, RenderType> renderType, CallbackInfo ci) {
        ModelPart[] parts = grandline$parts();
        float[] rest = new float[parts.length * 3];
        for (int i = 0; i < parts.length; i++) {
            rest[i * 3] = parts[i].x;
            rest[i * 3 + 1] = parts[i].y;
            rest[i * 3 + 2] = parts[i].z;
        }
        grandline$rest = rest;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void grandline$restorePositions(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        float[] rest = grandline$rest;
        if (rest == null) {
            return;
        }
        ModelPart[] parts = grandline$parts();
        for (int i = 0; i < parts.length; i++) {
            parts[i].x = rest[i * 3];
            parts[i].y = rest[i * 3 + 1];
            parts[i].z = rest[i * 3 + 2];
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void grandline$applyBodyAnimation(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimator.applyModel((HumanoidModel<?>) (Object) this, entity, ageInTicks);
    }

    @Unique
    private ModelPart[] grandline$parts() {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        return new ModelPart[] {model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
    }
}
