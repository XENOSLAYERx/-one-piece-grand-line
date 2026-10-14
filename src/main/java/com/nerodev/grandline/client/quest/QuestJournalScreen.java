package com.nerodev.grandline.client.quest;

import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.quest.Journey;
import com.nerodev.grandline.quest.LogPose;
import com.nerodev.grandline.quest.QuestDefinition;
import com.nerodev.grandline.quest.QuestObjective;
import com.nerodev.grandline.quest.Quests;
import com.nerodev.grandline.world.island.IslandType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The quest journal (default key K): quests under way and done on the left, the chosen quest's story, objectives
 * and rewards on the right; a second page lists the islands the Log Pose has recorded and where it points.
 */
public class QuestJournalScreen extends Screen {
    private static final int WIDTH = 360;
    private static final int HEIGHT = 224;
    private static final int LIST_W = 128;
    private static final int ROW_H = 14;

    private enum Page { QUESTS, ISLANDS }

    private record Row(ResourceLocation id, QuestDefinition quest, boolean done) {}

    private Page page = Page.QUESTS;
    private final List<Row> rows = new ArrayList<>();
    @Nullable
    private ResourceLocation selected;
    private int scroll;
    private int left;
    private int top;
    /** A digest of the journey the buttons were laid out for; they are laid out again when it changes. */
    private int laidOutFor;

    public QuestJournalScreen() {
        super(Component.literal("Quest Journal"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        laidOutFor = digest();
        refresh();
        addRenderableWidget(Button.builder(Component.literal("Quests"), b -> setPage(Page.QUESTS)).bounds(left + 6, top + 6, 60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Islands"), b -> setPage(Page.ISLANDS)).bounds(left + 68, top + 6, 60, 16).build());
        if (page == Page.QUESTS && selected != null && journey().isActive(selected)) {
            ResourceLocation id = selected;
            addRenderableWidget(Button.builder(Component.literal("Track"), b -> command("onepiece quest track " + id))
                    .bounds(left + WIDTH - 128, top + HEIGHT - 22, 60, 16).build());
            QuestDefinition quest = quest(id);
            if (quest != null && quest.category() == QuestDefinition.Category.SIDE) {
                addRenderableWidget(Button.builder(Component.literal("Abandon"), b -> command("onepiece quest abandon " + id))
                        .bounds(left + WIDTH - 66, top + HEIGHT - 22, 60, 16).build());
            }
        }
        if (page == Page.ISLANDS) {
            addRenderableWidget(Button.builder(Component.literal("Find next island"), b -> command("onepiece logpose next"))
                    .bounds(left + WIDTH - 110, top + HEIGHT - 22, 104, 16).build());
            List<Journey.Island> islands = journey().islands();
            for (int i = 0; i < Math.min(islands.size(), 9); i++) {
                String type = islands.get(i).type();
                addRenderableWidget(Button.builder(Component.literal("Set"), b -> command("onepiece logpose set " + type))
                        .bounds(left + WIDTH - 46, top + 58 + i * 16, 40, 14).build());
            }
        }
    }

    private void setPage(Page next) {
        page = next;
        rebuildWidgets();
    }

    private void command(String command) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand(command);
        }
    }

    private int digest() {
        Journey journey = journey();
        return ((journey.activeQuests().hashCode() * 31 + journey.completedQuests().size()) * 31 + journey.islands().size()) * 31
                + journey.heading().hashCode();
    }

    private Journey journey() {
        return GLPlayer.data(minecraft.player).journey();
    }

    private RegistryAccess access() {
        return minecraft.level.registryAccess();
    }

    @Nullable
    private QuestDefinition quest(ResourceLocation id) {
        return Quests.get(access(), id);
    }

    private void refresh() {
        rows.clear();
        Journey journey = journey();
        List<Map.Entry<ResourceLocation, QuestDefinition>> all = Quests.all(access());
        for (Map.Entry<ResourceLocation, QuestDefinition> e : all) {
            if (journey.isActive(e.getKey())) {
                rows.add(new Row(e.getKey(), e.getValue(), false));
            }
        }
        for (Map.Entry<ResourceLocation, QuestDefinition> e : all) {
            if (journey.isCompleted(e.getKey())) {
                rows.add(new Row(e.getKey(), e.getValue(), true));
            }
        }
        if (selected == null || rows.stream().noneMatch(row -> row.id().equals(selected))) {
            selected = journey.tracked().orElse(rows.isEmpty() ? null : rows.getFirst().id());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        g.fill(left, top + 26, left + WIDTH, top + HEIGHT, 0xD0141820);
        g.renderOutline(left, top + 26, WIDTH, HEIGHT - 26, 0xFF8A6A3A);
        g.drawString(font, title, left + WIDTH - font.width(title) - 8, top + 10, 0xFFE8C878, true);
        if (digest() != laidOutFor) {
            // The journey synced back after a button was used: lay the buttons out again to match.
            minecraft.tell(this::rebuildWidgets);
        }
        if (page == Page.QUESTS) {
            renderQuests(g, mouseX, mouseY);
        } else {
            renderIslands(g);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private void renderQuests(GuiGraphics g, int mouseX, int mouseY) {
        refresh();
        int x = left + 6;
        int y = top + 32;
        g.fill(left + LIST_W + 8, top + 30, left + LIST_W + 9, top + HEIGHT - 4, 0xFF5A4A30);
        if (rows.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.literal("No quests yet. Set out to sea, and speak to the islanders you meet."),
                    LIST_W - 4)) {
                g.drawString(font, line, x, y, 0xFFA0A0A0, false);
                y += 10;
            }
            return;
        }
        int visible = (HEIGHT - 40) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, rows.size() - visible));
        Journey journey = journey();
        for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++) {
            Row row = rows.get(i);
            boolean hover = mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROW_H;
            if (row.id().equals(selected)) {
                g.fill(x - 2, y - 2, x + LIST_W, y + ROW_H - 3, 0x60C8A050);
            } else if (hover) {
                g.fill(x - 2, y - 2, x + LIST_W, y + ROW_H - 3, 0x30FFFFFF);
            }
            boolean main = row.quest().category() == QuestDefinition.Category.MAIN;
            boolean tracked = journey.tracked().map(row.id()::equals).orElse(false);
            String mark = row.done() ? "✔ " : tracked ? "> " : main ? "* " : "- ";
            int color = row.done() ? 0xFF7A8A7A : main ? 0xFFF0C050 : 0xFFF0F0D8;
            g.drawString(font, font.plainSubstrByWidth(mark + row.quest().displayName().getString(), LIST_W - 4), x, y, color, false);
            y += ROW_H;
        }
        Row row = rows.stream().filter(r -> r.id().equals(selected)).findFirst().orElse(null);
        if (row != null) {
            renderDetails(g, row, journey);
        }
    }

    private void renderDetails(GuiGraphics g, Row row, Journey journey) {
        QuestDefinition quest = row.quest();
        int x = left + LIST_W + 16;
        int w = WIDTH - LIST_W - 24;
        int y = top + 32;
        g.drawString(font, quest.displayName().copy().withStyle(ChatFormatting.BOLD), x, y, 0xFFF0C050, true);
        y += 11;
        String kind = (quest.category() == QuestDefinition.Category.MAIN ? "Story" : "Side quest")
                + quest.island().map(island -> {
                    IslandType type = IslandType.byName(island);
                    return " - " + (type == null ? island : type.displayName().getString());
                }).orElse("") + (row.done() ? " - done" : "");
        g.drawString(font, kind, x, y, 0xFF8A98A8, false);
        y += 13;
        for (FormattedCharSequence line : font.split(quest.description(), w)) {
            g.drawString(font, line, x, y, 0xFFD8D8C8, false);
            y += 10;
        }
        y += 4;
        g.drawString(font, "Objectives", x, y, 0xFFE8C878, false);
        y += 11;
        for (int i = 0; i < quest.objectives().size(); i++) {
            QuestObjective objective = quest.objectives().get(i);
            int progress = row.done() ? objective.goal() : journey.progress(row.id(), i);
            for (FormattedCharSequence line : font.split(Quests.objectiveLine(access(), objective, progress), w)) {
                g.drawString(font, line, x, y, 0xFFFFFFFF, false);
                y += 10;
            }
        }
        QuestDefinition.Rewards rewards = quest.rewards();
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : rewards.items()) {
            parts.add(stack.getCount() + " " + stack.getHoverName().getString());
        }
        if (rewards.renown() > 0) {
            parts.add(rewards.renown() + " renown");
        }
        if (rewards.bounty() > 0) {
            parts.add("bounty");
        }
        rewards.reputation().forEach((faction, amount) -> parts.add((amount > 0 ? "+" : "") + amount + " " + faction.getPath()));
        if (!parts.isEmpty() && y < top + HEIGHT - 36) {
            y += 4;
            g.drawString(font, "Rewards", x, y, 0xFFE8C878, false);
            y += 11;
            for (FormattedCharSequence line : font.split(Component.literal(String.join(", ", parts)), w)) {
                if (y > top + HEIGHT - 26) {
                    break;
                }
                g.drawString(font, line, x, y, 0xFFB8D8A8, false);
                y += 10;
            }
        }
    }

    private void renderIslands(GuiGraphics g) {
        Journey journey = journey();
        int x = left + 10;
        int y = top + 32;
        Component reading = heading(journey);
        g.drawString(font, reading, x, y, 0xFFFFFFFF, true);
        y += 14;
        g.drawString(font, "Islands recorded: " + journey.islands().size(), x, y, 0xFFE8C878, false);
        y += 12;
        if (journey.islands().isEmpty()) {
            g.drawString(font, "None yet. Islands are recorded as you reach them.", x, y, 0xFFA0A0A0, false);
        }
        for (int i = 0; i < Math.min(journey.islands().size(), 9); i++) {
            Journey.Island island = journey.islands().get(i);
            IslandType type = IslandType.byName(island.type());
            String name = type == null ? island.type() : type.displayName().getString();
            BlockPos at = island.centre();
            double dx = at.getX() + 0.5 - minecraft.player.getX();
            double dz = at.getZ() + 0.5 - minecraft.player.getZ();
            String line = String.format(Locale.ROOT, "%s  (%d, %d)  %,d blocks %s", name, at.getX(), at.getZ(), (int) Math.sqrt(dx * dx + dz * dz),
                    LogPose.compass(dx, dz));
            boolean aimed = journey.heading().map(h -> h.target().equals(at)).orElse(false);
            g.drawString(font, (aimed ? "> " : "  ") + line, x, y + 3, aimed ? 0xFFF0C050 : 0xFFE0E0D0, false);
            y += 16;
        }
    }

    /** The Log Pose reading, worked out on the client from the synced heading. */
    private Component heading(Journey journey) {
        if (journey.heading().isEmpty()) {
            return Component.literal("Log Pose: the needle drifts").withStyle(ChatFormatting.GRAY);
        }
        Journey.Heading heading = journey.heading().get();
        double dx = heading.target().getX() + 0.5 - minecraft.player.getX();
        double dz = heading.target().getZ() + 0.5 - minecraft.player.getZ();
        IslandType type = IslandType.byName(heading.type());
        Component name = type == null ? Component.literal("an unknown island").withStyle(ChatFormatting.LIGHT_PURPLE)
                : type.displayName().copy().withStyle(ChatFormatting.GOLD);
        return Component.literal("Log Pose: ").withStyle(ChatFormatting.AQUA).append(name).append(Component.literal(String.format(Locale.ROOT,
                " - %,d blocks %s", (int) Math.sqrt(dx * dx + dz * dz), LogPose.compass(dx, dz))).withStyle(ChatFormatting.WHITE));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.QUESTS && mouseX >= left + 6 && mouseX < left + 6 + LIST_W && mouseY >= top + 30) {
            int index = scroll + (int) ((mouseY - (top + 30)) / ROW_H);
            if (index >= 0 && index < rows.size()) {
                selected = rows.get(index).id();
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
