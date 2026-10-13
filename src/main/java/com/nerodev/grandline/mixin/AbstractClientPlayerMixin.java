package com.nerodev.grandline.mixin;

import com.nerodev.grandline.client.render.DisguiseRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player wearing someone else's face (Clone-Clone Fruit) shows that player's skin (client only). */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void grandline$disguisedSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin skin = DisguiseRenderer.borrowedSkin((AbstractClientPlayer) (Object) this);
        if (skin != null) {
            cir.setReturnValue(skin);
        }
    }
}
