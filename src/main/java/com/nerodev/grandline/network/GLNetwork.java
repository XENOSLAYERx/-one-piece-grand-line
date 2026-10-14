package com.nerodev.grandline.network;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.client.ClientPayloads;
import com.nerodev.grandline.combat.CombatManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration. Client handlers are reached only through lambdas, so the client classes they call are never
 * loaded on a dedicated server.
 */
public final class GLNetwork {
    private static final String VERSION = "1";

    private GLNetwork() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC, GLNetwork::onAbilityKey);
        registrar.playToServer(SetSlotPayload.TYPE, SetSlotPayload.STREAM_CODEC, GLNetwork::onSetSlot);
        registrar.playToServer(DodgePayload.TYPE, DodgePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                CombatManager.dodge(player, payload.forward(), payload.left());
            }
        });
        registrar.playToServer(GuardPayload.TYPE, GuardPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                CombatManager.setGuard(player, payload.down());
            }
        });

        registrar.playToClient(FxPayload.TYPE, FxPayload.STREAM_CODEC, (payload, context) -> ClientPayloads.onFx(payload));
        registrar.playToClient(AnimationPayload.TYPE, AnimationPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.onAnimation(payload));
        registrar.playToClient(CombatSyncPayload.TYPE, CombatSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.onCombatSync(payload));
        registrar.playToClient(OpenCreatorMenuPayload.TYPE, OpenCreatorMenuPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.onOpenCreatorMenu());
    }

    private static void onAbilityKey(AbilityKeyPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && payload.slot() >= 0 && payload.slot() < AbilityState.SLOT_COUNT) {
            AbilityManager.handleKey(player, payload.slot(), payload.down());
        }
    }

    private static void onSetSlot(SetSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || payload.slot() < 0 || payload.slot() >= AbilityState.SLOT_COUNT) {
            return;
        }
        if (payload.ability().isPresent()) {
            Ability ability = GLAbilities.REGISTRY.get(payload.ability().get());
            if (ability == null || !AbilityAccess.canUse(player, ability)) {
                return;
            }
        }
        AbilityManager.setSlot(player, payload.slot(), payload.ability().orElse(null));
    }
}
