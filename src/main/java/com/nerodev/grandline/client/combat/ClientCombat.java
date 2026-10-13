package com.nerodev.grandline.client.combat;

import com.nerodev.grandline.network.CombatSyncPayload;

/** The local player's combat state as last sent by the server, for the HUD. */
public final class ClientCombat {
    private static int combo;
    private static long comboUntil;
    private static boolean guarding;
    private static long counterUntil;
    private static long dodgeReady;
    /** Client tick the combo last grew, to pulse the counter. */
    private static long comboGrewAt = Long.MIN_VALUE / 2;

    private ClientCombat() {}

    public static void update(CombatSyncPayload payload, long now) {
        if (payload.combo() > combo) {
            comboGrewAt = now;
        }
        combo = payload.combo();
        comboUntil = payload.comboUntil();
        guarding = payload.guarding();
        counterUntil = payload.counterUntil();
        dodgeReady = payload.dodgeReady();
    }

    public static void reset() {
        combo = 0;
        comboUntil = 0;
        guarding = false;
        counterUntil = 0;
        dodgeReady = 0;
    }

    public static int combo(long now) {
        return now <= comboUntil ? combo : 0;
    }

    public static long comboUntil() {
        return comboUntil;
    }

    public static long comboGrewAt() {
        return comboGrewAt;
    }

    public static boolean guarding() {
        return guarding;
    }

    public static boolean counterReady(long now) {
        return now <= counterUntil;
    }

    public static long dodgeReady() {
        return dodgeReady;
    }
}
