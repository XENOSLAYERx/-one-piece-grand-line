package com.nerodev.grandline.client.screen;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.client.GLKeys;
import com.nerodev.grandline.network.SetSlotPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ability loadout screen (default key R). Pick an ability on the left, then click a slot to equip it.
 * Right-click a slot to clear it. The server re-checks every change.
 */
public class AbilityScreen extends Screen {
    private static final int ENTRY_W = 150;
    private static final int ENTRY_H = 22;
    private static final int SLOT = 26;

    private List<Ability> abilities = List.of();
    @Nullable
    private Ability selected;
    private int scroll;

    public AbilityScreen() {
        super(Component.translatable("screen.grandline.abilities"));
    }

    @Override
    protected void init() {
        abilities = minecraft != null && minecraft.player != null ? AbilityAccess.usable(minecraft.player) : List.of();
    }

    private int listLeft() {
        return width / 2 - ENTRY_W - 20;
    }

    private int listTop() {
        return 50;
    }

    private int visibleRows() {
        return Math.max(1, (height - listTop() - 20) / ENTRY_H);
    }

    private int slotsLeft() {
        return width / 2 + 20;
    }

    private int slotsTop() {
        return 60;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.grandline.abilities.hint").withStyle(ChatFormatting.GRAY),
                width / 2, 30, 0xFFFFFFFF);

        Ability hovered = null;
        if (abilities.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("screen.grandline.abilities.none"), listLeft(), listTop(),
                    ENTRY_W, 0xFFB0B8C8);
        }
        int rows = visibleRows();
        for (int row = 0; row < rows && row + scroll < abilities.size(); row++) {
            Ability ability = abilities.get(row + scroll);
            int x = listLeft();
            int y = listTop() + row * ENTRY_H;
            boolean over = mouseX >= x && mouseX < x + ENTRY_W && mouseY >= y && mouseY < y + ENTRY_H - 2;
            int bg = ability == selected ? 0xC0405070 : over ? 0xA0303848 : 0x90101018;
            g.fill(x, y, x + ENTRY_W, y + ENTRY_H - 2, bg);
            drawIcon(g, ability, x + 2, y + 2, 1f);
            g.drawString(font, ability.displayName(), x + 22, y + 6, 0xFFFFFFFF, true);
            if (over) {
                hovered = ability;
            }
        }

        AbilityState state = minecraft != null && minecraft.player != null ? AbilityState.of(minecraft.player) : null;
        g.drawString(font, Component.translatable("screen.grandline.abilities.slots"), slotsLeft(), slotsTop() - 14, 0xFFFFFFFF, true);
        for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
            int x = slotsLeft() + i * (SLOT + 4);
            int y = slotsTop();
            boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
            g.fill(x, y, x + SLOT, y + SLOT, over ? 0xC0304060 : 0xB0101018);
            g.renderOutline(x, y, SLOT, SLOT, selected != null && over ? 0xFFFFE680 : 0xFF3A4150);
            ResourceLocation id = state == null ? null : state.slot(i);
            Ability equipped = id == null ? null : GLAbilities.REGISTRY.get(id);
            if (equipped != null) {
                drawIcon(g, equipped, x + 5, y + 5, 1f);
                if (over) {
                    hovered = equipped;
                }
            }
            g.drawCenteredString(font, GLKeys.label(i), x + SLOT / 2, y + SLOT + 3, 0xFFB0B8C8);
        }

        if (hovered != null) {
            g.renderTooltip(font, tooltip(hovered), mouseX, mouseY);
        }
    }

    private void drawIcon(GuiGraphics g, Ability ability, int x, int y, float alpha) {
        int c = ability.properties().color();
        g.setColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, alpha);
        g.blit(GrandLine.id(ability.properties().icon().texturePath()), x, y, 0, 0, 16, 16, 16, 16);
        g.setColor(1f, 1f, 1f, 1f);
    }

    private List<FormattedCharSequence> tooltip(Ability ability) {
        Ability.Properties props = ability.properties();
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(ability.displayName().copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
        lines.addAll(font.split(ability.description().copy().withStyle(ChatFormatting.GRAY), 200));
        String type = props.activation().name().charAt(0) + props.activation().name().substring(1).toLowerCase(Locale.ROOT);
        lines.add(Component.literal(type + "  |  " + props.category().name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.DARK_AQUA).getVisualOrderText());
        lines.add(Component.literal(String.format(Locale.ROOT, "Cooldown %.1fs  |  Stamina %.0f%s",
                        props.cooldownTicks() / 20f, props.staminaCost(),
                        props.staminaPerTick() > 0 ? String.format(Locale.ROOT, " + %.0f/s", props.staminaPerTick() * 20) : ""))
                .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rows = visibleRows();
        for (int row = 0; row < rows && row + scroll < abilities.size(); row++) {
            int x = listLeft();
            int y = listTop() + row * ENTRY_H;
            if (mouseX >= x && mouseX < x + ENTRY_W && mouseY >= y && mouseY < y + ENTRY_H - 2) {
                Ability clicked = abilities.get(row + scroll);
                selected = clicked == selected ? null : clicked;
                return true;
            }
        }
        for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
            int x = slotsLeft() + i * (SLOT + 4);
            int y = slotsTop();
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                if (button == 1) {
                    PacketDistributor.sendToServer(new SetSlotPayload(i, Optional.empty()));
                } else if (selected != null) {
                    PacketDistributor.sendToServer(new SetSlotPayload(i, Optional.of(selected.id())));
                    selected = null;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, abilities.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (GLKeys.ABILITY_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
