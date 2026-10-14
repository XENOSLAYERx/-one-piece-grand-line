package com.nerodev.grandline.client;

import com.nerodev.grandline.client.anim.PlayerAnimator;
import com.nerodev.grandline.client.combat.ClientCombat;
import com.nerodev.grandline.client.fx.ClientEffects;
import com.nerodev.grandline.client.screen.CreatorMenuScreen;
import com.nerodev.grandline.network.AnimationPayload;
import com.nerodev.grandline.network.CombatSyncPayload;
import com.nerodev.grandline.network.FxPayload;
import net.minecraft.client.Minecraft;

/** Client-side handlers for server payloads. Only ever loaded on the physical client. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void onFx(FxPayload payload) {
        ClientEffects.play(payload.effect(), payload.entityId(), payload.params());
    }

    public static void onAnimation(AnimationPayload payload) {
        PlayerAnimator.play(payload.entityId(), payload.animation());
    }

    public static void onOpenCreatorMenu() {
        Minecraft.getInstance().setScreen(new CreatorMenuScreen());
    }

    public static void onCombatSync(CombatSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientCombat.update(payload, mc.level == null ? 0 : mc.level.getGameTime());
    }
}
