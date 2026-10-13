package com.nerodev.grandline.client.hud;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.client.GLKeys;
import java.util.Locale;
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
 * Ability bar on the right edge of the screen: five slots (icon, key, cooldown sweep and timer, active glow,
 * charge progress) and a vertical stamina gauge. Everything is computed from synced timestamps, so the HUD stays
 * smooth without per-tick packets.
 */
public final class AbilityHud {
    public static final ResourceLocation ID = GrandLine.id("ability_hud");

    static final int SLOT = 22;
    static final int GAP = 2;
    private static final int BORDER = 0xFF3A4150;
    private static final int BACKGROUND = 0xB0101018;

    private AbilityHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ID, AbilityHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.options.hideGui || player == null || mc.level == null || player.isSpectator()) {
            return;
        }
        AbilityState state = AbilityState.of(player);
        long now = mc.level.getGameTime();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float stamina = state.stamina(now);
        boolean anyEquipped = !state.equipped().isEmpty();
        if (!anyEquipped && stamina >= state.maxStamina()) {
            return;
        }

        int total = AbilityState.SLOT_COUNT * SLOT + (AbilityState.SLOT_COUNT - 1) * GAP;
        int x = g.guiWidth() - SLOT - 4;
        int top = (g.guiHeight() - total) / 2;

        drawStamina(g, x - 7, top, total, stamina / state.maxStamina(), state.hasFlag(TestFlag.INFINITE_STAMINA));
        if (!anyEquipped) {
            return;
        }
        for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
            drawSlot(g, mc.font, state, i, x, top + i * (SLOT + GAP), now, partial, stamina);
        }
    }

    private static void drawStamina(GuiGraphics g, int x, int top, int height, float fraction, boolean infinite) {
        g.fill(x - 1, top - 1, x + 5, top + height + 1, 0xC0000000);
        int filled = Math.round(height * Mth.clamp(fraction, 0, 1));
        int low = infinite ? 0xFF4FD8E8 : (fraction < 0.25f ? 0xFFE0563A : 0xFFE8B83A);
        int high = infinite ? 0xFFB8F4FF : (fraction < 0.25f ? 0xFFF09070 : 0xFFF8E68C);
        g.fillGradient(x, top + height - filled, x + 4, top + height, high, low);
    }

    private static void drawSlot(GuiGraphics g, Font font, AbilityState state, int slot, int x, int y, long now,
            float partial, float stamina) {
        g.fill(x, y, x + SLOT, y + SLOT, BACKGROUND);
        ResourceLocation id = state.slot(slot);
        Ability ability = id == null ? null : GLAbilities.REGISTRY.get(id);

        int border = BORDER;
        if (ability != null) {
            Ability.Properties props = ability.properties();
            boolean active = state.isActive(id);
            long remaining = state.cooldownRemaining(id, now);
            boolean affordable = stamina >= props.staminaCost() || state.hasFlag(TestFlag.INFINITE_STAMINA);

            if (active) {
                float pulse = 0.65f + 0.35f * Mth.sin((now + partial) * 0.4f);
                border = withAlpha(props.color(), pulse);
            }

            float alpha = remaining > 0 || !affordable ? 0.45f : 1f;
            int c = props.color();
            g.setColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, alpha);
            g.blit(GrandLine.id(props.icon().texturePath()), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
            g.setColor(1f, 1f, 1f, 1f);

            if (!affordable && remaining == 0) {
                g.fill(x + 1, y + SLOT - 3, x + SLOT - 1, y + SLOT - 1, 0xC0E0563A);
            }
            if (remaining > 0) {
                float exact = Math.max(0, remaining - partial);
                float fraction = Mth.clamp(exact / Math.max(1, props.cooldownTicks()), 0, 1);
                int h = Math.round((SLOT - 2) * fraction);
                g.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + h, 0xA0000000);
                String text = exact >= 20 ? String.valueOf((int) Math.ceil(exact / 20f))
                        : String.format(Locale.ROOT, "%.1f", exact / 20f);
                g.drawCenteredString(font, text, x + SLOT / 2, y + 7, 0xFFFFFFFF);
            }
            if (active && props.activation() == ActivationType.CHARGE) {
                float charged = state.activeSince(id).map(start -> (now - start + partial)).orElse(0f);
                float fraction = Mth.clamp(charged / Math.max(1, props.maxActiveTicks()), 0, 1);
                int barColor = charged >= props.minChargeTicks() ? 0xFFFFE680 : 0xFF9AA4B4;
                g.fill(x + 1, y + SLOT + 1, x + 1 + Math.round((SLOT - 2) * fraction), y + SLOT + 2, barColor);
            }
        }

        // 1px border
        g.fill(x, y, x + SLOT, y + 1, border);
        g.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, border);
        g.fill(x, y, x + 1, y + SLOT, border);
        g.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, border);

        // Key label, bottom-left, small.
        g.pose().pushPose();
        g.pose().translate(x + 2, y + SLOT - 6, 200);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, GLKeys.label(slot), 0, 0, ability == null ? 0xFF707888 : 0xFFE8ECF4, true);
        g.pose().popPose();
    }

    static int withAlpha(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
