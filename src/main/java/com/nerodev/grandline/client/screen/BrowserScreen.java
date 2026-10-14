package com.nerodev.grandline.client.screen;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A three-column browser: categories on the left, a searchable list in the middle, and the picked entry's details on
 * the right (scrolled with the mouse wheel) above its buttons. The Creator Menu and the Guide are both built on it.
 * Every button runs /onepiece commands, so the server checks each one.
 */
public abstract class BrowserScreen extends Screen {
    protected static final int CATEGORY_W = 104;
    protected static final int ROW_H = 20;
    protected static final int LIST_W = 170;
    protected static final int ACTION_W = 130;

    /** One button on an entry: its label and the commands it runs (without the slash). */
    record Action(Component label, List<String> commands, boolean confirm) {
        static Action of(String label, String... commands) {
            return new Action(Component.literal(label), List.of(commands), false);
        }

        static Action confirmed(String label, String... commands) {
            return new Action(Component.literal(label), List.of(commands), true);
        }
    }

    /** One line in the middle list. */
    record Entry(Component name, List<Component> details, int color, ItemStack icon, BooleanSupplier active, List<Action> actions) {}

    record Category(Component name, Supplier<List<Entry>> entries) {}

    protected final List<Category> categories = new ArrayList<>();
    /** Widgets that belong to the current entry or category; rebuilt whenever either changes. */
    private final List<AbstractWidget> entryWidgets = new ArrayList<>();
    protected int category;
    private List<Entry> entries = List.of();
    private List<Entry> shown = List.of();
    @Nullable
    protected Entry selected;
    private int scroll;
    private int detailScroll;
    private EditBox search;
    /** Ticks until the entries are read again after a button ran (so they show what the server changed). */
    private int refreshIn;

    protected BrowserScreen(Component title) {
        super(title);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------------------------

    protected int left() {
        return Math.max(8, (width - (CATEGORY_W + LIST_W + ACTION_W + 180)) / 2);
    }

    protected int listLeft() {
        return left() + CATEGORY_W + 8;
    }

    protected int detailLeft() {
        return listLeft() + LIST_W + 8;
    }

    protected int top() {
        return 40;
    }

    protected int listTop() {
        return top() + 24;
    }

    protected int visibleRows() {
        return Math.max(1, (height - listTop() - 12) / ROW_H);
    }

    private int detailWidth() {
        return Math.max(120, width - detailLeft() - 12);
    }

    /** Where the details stop: above the selected entry's buttons. */
    private int detailBottom() {
        return height - 18 - (selected == null ? 0 : selected.actions().size() * 22);
    }

    @Override
    protected void init() {
        // init runs again on a resize or on coming back from a confirmation: keep the picked entry.
        String picked = selected == null ? null : selected.name().getString();
        entryWidgets.clear();
        int rowStep = Math.min(21, Math.max(14, (height - top() - 8) / Math.max(1, categories.size())));
        for (int i = 0; i < categories.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(categories.get(i).name(), b -> selectCategory(index))
                    .bounds(left(), top() + i * rowStep, CATEGORY_W, rowStep - 1).build());
        }
        search = new EditBox(font, listLeft(), top(), LIST_W, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search...").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> filter());
        addRenderableWidget(search);
        selectCategory(category);
        if (picked != null) {
            select(picked);
        }
    }

    protected void selectCategory(int index) {
        category = index;
        entries = categories.get(index).entries().get();
        scroll = 0;
        selected = null;
        detailScroll = 0;
        if (search != null) {
            search.setValue("");
        }
        filter();
        rebuildActions();
    }

    /** Picks an entry of the current category by name (the autotest uses it). */
    protected void select(String name) {
        for (Entry entry : entries) {
            if (entry.name().getString().equalsIgnoreCase(name)) {
                selected = entry;
                detailScroll = 0;
                rebuildActions();
                return;
            }
        }
    }

    private void filter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        shown = query.isEmpty() ? entries
                : entries.stream().filter(e -> e.name().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        scroll = Math.min(scroll, Math.max(0, shown.size() - visibleRows()));
    }

    protected final String categoryName() {
        return categories.get(category).name().getString();
    }

    /** Adds a widget that lives only as long as the current entry and category. */
    protected final <T extends AbstractWidget> T addEntryWidget(T widget) {
        entryWidgets.add(addRenderableWidget(widget));
        return widget;
    }

    /** Category-wide widgets (beside the entry buttons), placed with their left edge at {@code x}. */
    protected void addCategoryWidgets(int x) {}

    private void rebuildActions() {
        entryWidgets.forEach(this::removeWidget);
        entryWidgets.clear();
        int x = detailLeft();
        addCategoryWidgets(x);
        if (selected == null) {
            return;
        }
        List<Action> actions = selected.actions();
        int y = height - 12 - actions.size() * 22;
        for (Action action : actions) {
            addEntryWidget(Button.builder(action.label(), b -> {
                if (action.confirm()) {
                    confirm(Component.literal(action.label().getString() + "?"), selected.name(), action.commands());
                } else {
                    run(action.commands());
                }
            }).bounds(x, y, ACTION_W, 20).build());
            y += 22;
        }
    }

    protected void run(List<String> commands) {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            commands.forEach(player.connection::sendCommand);
            refreshIn = 10;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (refreshIn > 0 && --refreshIn == 0) {
            refresh();
        }
    }

    /** Reads the current category's entries again, keeping the picked entry and the scroll positions. */
    private void refresh() {
        String picked = selected == null ? null : selected.name().getString();
        int keepDetail = detailScroll;
        entries = categories.get(category).entries().get();
        filter();
        selected = picked == null ? null
                : entries.stream().filter(e -> e.name().getString().equals(picked)).findFirst().orElse(null);
        detailScroll = keepDetail;
        rebuildActions();
    }

    protected void confirm(Component title, Component message, List<String> commands) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                run(commands);
            }
            minecraft.setScreen(this);
        }, title, message));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing and input
    // ---------------------------------------------------------------------------------------------------------

    /** Shown in the details column while nothing is picked; null for nothing. */
    @Nullable
    protected Component emptyHint() {
        return Component.literal("Pick an entry to see more.").withStyle(ChatFormatting.GRAY);
    }

    /** Draws in the free space under the list rows (when the list is short). */
    protected void renderListFooter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFD27A);
        g.drawString(font, categories.get(category).name().copy().withStyle(ChatFormatting.BOLD), detailLeft(), top() + 4, 0xFFFFFFFF);

        int rows = visibleRows();
        int drawn = 0;
        for (int row = 0; row < rows && row + scroll < shown.size(); row++) {
            Entry entry = shown.get(row + scroll);
            int y = listTop() + row * ROW_H;
            boolean hover = mouseX >= listLeft() && mouseX < listLeft() + LIST_W && mouseY >= y && mouseY < y + ROW_H - 2;
            int background = entry == selected ? 0xC0405070 : hover ? 0x80303848 : 0x60181C24;
            g.fill(listLeft(), y, listLeft() + LIST_W, y + ROW_H - 2, background);
            g.fill(listLeft(), y, listLeft() + 2, y + ROW_H - 2, 0xFF000000 | entry.color());
            if (!entry.icon().isEmpty()) {
                g.renderItem(entry.icon(), listLeft() + 4, y + 1);
            }
            int textX = listLeft() + (entry.icon().isEmpty() ? 6 : 22);
            String name = font.plainSubstrByWidth(entry.name().getString(), LIST_W - (textX - listLeft()) - 12);
            boolean active = entry.active().getAsBoolean();
            g.drawString(font, name, textX, y + 5, active ? 0xFF8CF08C : 0xFFE0E4EC);
            if (active) {
                g.drawString(font, "✔", listLeft() + LIST_W - 10, y + 5, 0xFF8CF08C);
            }
            drawn++;
        }
        if (shown.isEmpty()) {
            g.drawString(font, Component.literal("Nothing here").withStyle(ChatFormatting.GRAY), listLeft() + 4, listTop() + 4, 0xFFFFFFFF);
        }
        if (shown.size() > rows) {
            int barHeight = Math.max(10, rows * ROW_H * rows / shown.size());
            int barTop = listTop() + (rows * ROW_H - barHeight) * scroll / Math.max(1, shown.size() - rows);
            g.fill(listLeft() + LIST_W + 2, barTop, listLeft() + LIST_W + 4, barTop + barHeight, 0xA0FFFFFF);
        } else {
            int footerTop = listTop() + drawn * ROW_H + 4;
            renderListFooter(g, listLeft(), footerTop, LIST_W, height - 12 - footerTop, mouseX, mouseY);
        }

        if (selected != null) {
            renderDetails(g);
        } else {
            Component hint = emptyHint();
            if (hint != null) {
                g.drawWordWrap(font, hint, detailLeft(), listTop(), detailWidth(), 0xFFFFFFFF);
            }
        }
    }

    /** Every wrapped line of the selected entry's details, with a small gap after each paragraph (null). */
    private List<FormattedCharSequence> detailLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : selected.details()) {
            lines.addAll(font.split(line, detailWidth()));
            lines.add(null);
        }
        return lines;
    }

    private static int lineHeight(@Nullable FormattedCharSequence line) {
        return line == null ? 3 : 10;
    }

    private void renderDetails(GuiGraphics g) {
        int top = listTop();
        g.drawString(font, selected.name(), detailLeft(), top, 0xFF000000 | selected.color());
        top += 14;
        int bottom = detailBottom();
        List<FormattedCharSequence> lines = detailLines();
        int total = lines.stream().mapToInt(BrowserScreen::lineHeight).sum();
        int room = bottom - top;
        detailScroll = Math.max(0, Math.min(detailScroll, total - room));
        g.enableScissor(detailLeft(), top, detailLeft() + detailWidth() + 4, bottom);
        int y = top - detailScroll;
        for (FormattedCharSequence line : lines) {
            if (line != null && y + 10 > top && y < bottom) {
                g.drawString(font, line, detailLeft(), y, 0xFFC8CCD8);
            }
            y += lineHeight(line);
        }
        g.disableScissor();
        if (total > room) {
            int barHeight = Math.max(10, room * room / total);
            int barTop = top + (room - barHeight) * detailScroll / Math.max(1, total - room);
            int barX = detailLeft() + detailWidth() + 6;
            g.fill(barX, barTop, barX + 2, barTop + barHeight, 0xA0FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (mouseX >= listLeft() && mouseX < listLeft() + LIST_W && mouseY >= listTop()) {
            int row = (int) ((mouseY - listTop()) / ROW_H);
            if (row < visibleRows() && row + scroll < shown.size()) {
                selected = shown.get(row + scroll);
                detailScroll = 0;
                rebuildActions();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listLeft() && mouseX < listLeft() + LIST_W + 6) {
            scroll = (int) Math.max(0, Math.min(Math.max(0, shown.size() - visibleRows()), scroll - Math.signum(scrollY)));
            return true;
        }
        if (selected != null && mouseX >= detailLeft() && mouseY < detailBottom()) {
            detailScroll = Math.max(0, detailScroll - (int) Math.signum(scrollY) * 20);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers for building entries
    // ---------------------------------------------------------------------------------------------------------

    @Nullable
    protected PlayerData data() {
        return minecraft == null || minecraft.player == null ? null : GLPlayer.data(minecraft.player);
    }

    protected static Component line(String label, Component value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY).append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    protected static Component names(List<Ability> abilities) {
        return Component.literal(abilities.isEmpty() ? "-"
                : String.join(", ", abilities.stream().map(a -> a.displayName().getString()).toList()));
    }

    protected static String title(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }

    protected static ItemStack stack(@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
