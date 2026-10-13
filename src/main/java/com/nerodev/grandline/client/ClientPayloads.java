package com.nerodev.grandline.client;

import com.nerodev.grandline.client.anim.PlayerAnimator;
import com.nerodev.grandline.client.fx.ClientEffects;
import com.nerodev.grandline.network.AnimationPayload;
import com.nerodev.grandline.network.FxPayload;

/** Client-side handlers for server payloads. Only ever loaded on the physical client. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void onFx(FxPayload payload) {
        ClientEffects.play(payload.effect(), payload.entityId(), payload.params());
    }

    public static void onAnimation(AnimationPayload payload) {
        PlayerAnimator.play(payload.entityId(), payload.animation());
    }
}
