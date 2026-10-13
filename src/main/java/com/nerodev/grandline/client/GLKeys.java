package com.nerodev.grandline.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.client.screen.AbilityScreen;
import com.nerodev.grandline.network.AbilityKeyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Ability keys. Defaults Z X C V G for the five slots and R for the ability menu; all remappable in Controls.
 * The client only reports key edges (down/up). The server decides what happens.
 */
public final class GLKeys {
    public static final String CATEGORY = "key.categories.grandline";

    private static final int[] DEFAULT_SLOT_KEYS = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_G};

    public static final KeyMapping[] SLOTS = new KeyMapping[AbilityState.SLOT_COUNT];
    public static final KeyMapping ABILITY_MENU = new KeyMapping("key.grandline.ability_menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    static {
        for (int i = 0; i < SLOTS.length; i++) {
            SLOTS[i] = new KeyMapping("key.grandline.ability_" + (i + 1), KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, DEFAULT_SLOT_KEYS[i], CATEGORY);
        }
    }

    private static final boolean[] wasDown = new boolean[AbilityState.SLOT_COUNT];

    private GLKeys() {}

    public static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : SLOTS) {
            event.register(key);
        }
        event.register(ABILITY_MENU);
    }

    /** Sends a payload whenever a slot key changes state. Opening any screen counts as releasing every key. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.player != null && mc.getConnection() != null;
        boolean listening = inGame && mc.screen == null;
        for (int i = 0; i < SLOTS.length; i++) {
            boolean down = listening && SLOTS[i].isDown();
            if (down != wasDown[i]) {
                wasDown[i] = down;
                if (inGame) {
                    PacketDistributor.sendToServer(new AbilityKeyPayload(i, down));
                }
            }
        }
        while (ABILITY_MENU.consumeClick()) {
            if (listening) {
                mc.setScreen(new AbilityScreen());
            }
        }
    }

    /** Key label for HUD display, e.g. "Z". */
    public static String label(int slot) {
        String text = SLOTS[slot].getTranslatedKeyMessage().getString();
        return text.length() > 3 ? text.substring(0, 3) : text;
    }
}
