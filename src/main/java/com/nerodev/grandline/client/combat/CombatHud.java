package com.nerodev.grandline.client.combat;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.config.GLServerConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Combat read-outs next to the crosshair: the combo counter (it swells with each blow and drains as the window runs
 * out), a "COUNTER" cue after a parry, a guard marker while the guard is up, and a small dodge-recharge arc.
 */
public final class CombatHud {
    public static final ResourceLocation ID = GrandLine.id("combat_hud");

    private CombatHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ID, CombatHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.options.hideGui || player == null || mc.level == null || player.isSpectator()) {
            return;
        }
        long now = mc.level.getGameTime();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        Font font = mc.font;
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;

        int combo = ClientCombat.combo(now);
        if (combo >= 2) {
            float since = (now - ClientCombat.comboGrewAt()) + partial;
            float pulse = 1f + 0.5f * Math.max(0f, 1f - since / 6f);
            int color = combo >= 5 ? 0xFFFFC040 : combo >= 3 ? 0xFFFFE6A0 : 0xFFFFFFFF;
            String text = combo + " HIT" + (combo > 1 ? "S" : "");
            g.pose().pushPose();
            g.pose().translate(cx + 18, cy - 22, 0);
            g.pose().scale(pulse, pulse, 1f);
            g.drawString(font, text, 0, 0, color, true);
            g.pose().popPose();
            float left = Mth.clamp((ClientCombat.comboUntil() - now - partial) / (float) GLServerConfig.comboWindowTicks(), 0f, 1f);
            int width = 30;
            g.fill(cx + 18, cy - 12, cx + 18 + width, cy - 11, 0x80000000);
            g.fill(cx + 18, cy - 12, cx + 18 + Math.round(width * left), cy - 11, color);
        }

        if (ClientCombat.counterReady(now) && (now / 3) % 2 == 0) {
            String text = "COUNTER";
            g.drawString(font, text, cx - font.width(text) / 2, cy + 14, 0xFFFFD040, true);
        }

        if (ClientCombat.guarding()) {
            g.fill(cx - 9, cy + 8, cx - 7, cy + 13, 0xC0E8E8F0);
            g.fill(cx + 7, cy + 8, cx + 9, cy + 13, 0xC0E8E8F0);
            g.fill(cx - 9, cy + 12, cx + 9, cy + 13, 0xC0E8E8F0);
        }

        long ready = ClientCombat.dodgeReady();
        int cooldown = GLServerConfig.dodgeCooldownTicks();
        if (now < ready && cooldown > 0) {
            float left = Mth.clamp((ready - now - partial) / cooldown, 0f, 1f);
            int width = 16;
            g.fill(cx - width / 2, cy + 18, cx + width / 2, cy + 19, 0x60000000);
            g.fill(cx - width / 2, cy + 18, cx - width / 2 + Math.round(width * (1 - left)), cy + 19, 0xC0A0D8FF);
        }
    }
}
