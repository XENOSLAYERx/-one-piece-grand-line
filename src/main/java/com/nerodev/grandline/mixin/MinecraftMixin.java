package com.nerodev.grandline.mixin;

import com.nerodev.grandline.client.haki.HakiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observation Haki: presences the local player senses get the glowing outline, visible through walls. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void grandline$observationGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (HakiClient.senses(entity)) {
            cir.setReturnValue(true);
        }
    }
}
