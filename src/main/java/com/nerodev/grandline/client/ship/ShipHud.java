package com.nerodev.grandline.client.ship;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.client.GLKeys;
import com.nerodev.grandline.ship.ShipDefinition;
import com.nerodev.grandline.ship.ShipEntity;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The ship's read-out while you are aboard: name, hull, speed, whether each broadside is loaded, the Coup de Burst
 * charge, and the keys to use them.
 */
public final class ShipHud {
    public static final ResourceLocation ID = GrandLine.id("ship_hud");

    private ShipHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ID, ShipHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) {
            return;
        }
        ShipDefinition def = ship.definition();
        if (def == null) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        int cx = g.guiWidth() / 2;
        int y = 4;
        String title = def.displayName().getString() + (ship.helmsman() == mc.player ? "  (at the helm)" : "");
        g.drawString(font, title, cx - font.width(title) / 2, y, 0xFFFFE6A0, true);
        y += 11;
        int width = 120;
        float hull = Mth.clamp(ship.health() / ship.maxHealth(), 0f, 1f);
        int color = hull > 0.5f ? 0xFF6AD86A : hull > 0.25f ? 0xFFE8C040 : 0xFFE84A3A;
        g.fill(cx - width / 2 - 1, y - 1, cx + width / 2 + 1, y + 5, 0xA0000000);
        g.fill(cx - width / 2, y, cx - width / 2 + Math.round(width * hull), y + 4, color);
        y += 7;
        String status = String.format(Locale.ROOT, "Hull %.0f/%.0f   %.1f knots%s", ship.health(), ship.maxHealth(),
                Math.abs(ship.speed()) * 20 * 1.944, ship.burning() > 0 ? "   ON FIRE" : "");
        g.drawString(font, status, cx - font.width(status) / 2, y, ship.burning() > 0 ? 0xFFFF7A4A : 0xFFE0E4EC, true);
        y += 11;
        if (def.cannons() > 0) {
            String port = gun("Port", ship.readyAt(1), now, def.cannonReload());
            String starboard = gun("Starboard", ship.readyAt(-1), now, def.cannonReload());
            String guns = port + "   " + starboard + "   [" + GLKeys.SHIP_FIRE.getTranslatedKeyMessage().getString() + "] fire";
            g.drawString(font, guns, cx - font.width(guns) / 2, y, 0xFFC8D0E0, true);
            y += 11;
        }
        if (def.burst()) {
            long ready = ship.burstReadyAt();
            String burst = now >= ready ? "Coup de Burst ready [" + GLKeys.SHIP_BURST.getTranslatedKeyMessage().getString() + "]"
                    : String.format(Locale.ROOT, "Coup de Burst in %ds", (ready - now + 19) / 20);
            g.drawString(font, burst, cx - font.width(burst) / 2, y, now >= ready ? 0xFF7AD8F8 : 0xFF8A98A8, true);
        }
    }

    private static String gun(String side, long ready, long now, int reload) {
        if (now >= ready) {
            return side + ": loaded";
        }
        return String.format(Locale.ROOT, "%s: %d%%", side, Math.round(100f * (1f - (ready - now) / (float) reload)));
    }
}
