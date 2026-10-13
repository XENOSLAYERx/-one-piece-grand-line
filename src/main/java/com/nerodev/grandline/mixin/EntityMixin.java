package com.nerodev.grandline.mixin;

import com.nerodev.grandline.client.haki.HakiClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Colours Observation Haki outlines: hostile presences red, players cyan (client only). */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColor()I", at = @At("HEAD"), cancellable = true)
    private void grandline$observationColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (HakiClient.senses(self)) {
            cir.setReturnValue(HakiClient.senseColor(self));
        }
    }
}
