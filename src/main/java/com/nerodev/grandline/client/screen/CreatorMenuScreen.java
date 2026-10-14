package com.nerodev.grandline.client.screen;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.boss.BossDefinition;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.bounty.BountyHunts;
import com.nerodev.grandline.creator.Battles;
import com.nerodev.grandline.crew.JollyRoger;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.Loadout;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.definition.RaceDefinition;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcDefinition;
import com.nerodev.grandline.quest.GLQuests;
import com.nerodev.grandline.quest.QuestDefinition;
import com.nerodev.grandline.quest.QuestObjective;
import com.nerodev.grandline.quest.Quests;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.ship.ShipDefinition;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.ship.ShipItem;
import com.nerodev.grandline.style.FightingStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import com.nerodev.grandline.transform.TransformationManager;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.weapon.WeaponItem;
import com.nerodev.grandline.world.island.IslandType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Creator Menu (/onepiece menu, default key N). Pick a category on the left and an entry in the middle; the
 * buttons on the right unlock, equip, remove, max out or test it. Every button runs the matching /onepiece command,
 * so the server checks the CREATOR permission on every click and the chat shows what happened.
 */
public class CreatorMenuScreen extends Screen {
    private static final int CATEGORY_W = 104;
    private static final int ROW_H = 20;
    private static final int LIST_W = 170;
    private static final int ACTION_W = 130;

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

    private final List<Category> categories = new ArrayList<>();
    private final List<Button> actionButtons = new ArrayList<>();
    private int category;
    private List<Entry> entries = List.of();
    private List<Entry> shown = List.of();
    @Nullable
    private Entry selected;
    private int scroll;
    private EditBox search;
    @Nullable
    private EditBox loadoutName;

    public CreatorMenuScreen() {
        super(Component.translatable("screen.grandline.creator"));
        categories.add(new Category(Component.literal("Devil Fruits"), this::fruits));
        categories.add(new Category(Component.literal("Awakenings"), this::awakenings));
        categories.add(new Category(Component.literal("Haki"), this::haki));
        categories.add(new Category(Component.literal("Fighting Styles"), this::styles));
        categories.add(new Category(Component.literal("Rokushiki"), this::rokushiki));
        categories.add(new Category(Component.literal("Races"), this::races));
        categories.add(new Category(Component.literal("Transformations"), this::transformations));
        categories.add(new Category(Component.literal("Weapons"), this::weapons));
        categories.add(new Category(Component.literal("Factions"), this::factions));
        categories.add(new Category(Component.literal("NPCs"), this::npcs));
        categories.add(new Category(Component.literal("Bosses"), this::bosses));
        categories.add(new Category(Component.literal("Quests"), this::quests));
        categories.add(new Category(Component.literal("Crew & Bounty"), this::crewAndBounty));
        categories.add(new Category(Component.literal("Ships"), this::ships));
        categories.add(new Category(Component.literal("Islands"), this::islands));
        categories.add(new Category(Component.literal("Battle Test"), this::battleTest));
        categories.add(new Category(Component.literal("Loadouts"), this::loadouts));
        categories.add(new Category(Component.literal("Player Data"), this::playerData));
        categories.add(new Category(Component.literal("Debug"), this::debug));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------------------------

    private int left() {
        return Math.max(8, (width - (CATEGORY_W + LIST_W + ACTION_W + 180)) / 2);
    }

    private int listLeft() {
        return left() + CATEGORY_W + 8;
    }

    private int detailLeft() {
        return listLeft() + LIST_W + 8;
    }

    private int top() {
        return 40;
    }

    private int listTop() {
        return top() + 24;
    }

    private int visibleRows() {
        return Math.max(1, (height - listTop() - 12) / ROW_H);
    }

    @Override
    protected void init() {
        actionButtons.clear();
        for (int i = 0; i < categories.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(categories.get(i).name(), b -> selectCategory(index))
                    .bounds(left(), top() + i * 21, CATEGORY_W, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("UNLOCK EVERYTHING").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                b -> confirm(Component.literal("Unlock everything?"),
                        Component.literal("Every fruit, ability, style, weapon, transformation, awakening and Haki will be "
                                + "unlocked for you."),
                        List.of("onepiece unlock all")))
                .bounds(width - 150, 8, 140, 20).build());
        search = new EditBox(font, listLeft(), top(), LIST_W, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search...").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> filter());
        addRenderableWidget(search);
        selectCategory(category);
    }

    private void selectCategory(int index) {
        category = index;
        entries = categories.get(index).entries().get();
        scroll = 0;
        selected = null;
        if (search != null) {
            search.setValue("");
        }
        filter();
        rebuildActions();
    }

    private void filter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        shown = query.isEmpty() ? entries
                : entries.stream().filter(e -> e.name().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        scroll = Math.min(scroll, Math.max(0, shown.size() - visibleRows()));
    }

    private void rebuildActions() {
        actionButtons.forEach(this::removeWidget);
        actionButtons.clear();
        if (loadoutName != null) {
            removeWidget(loadoutName);
            loadoutName = null;
        }
        int x = detailLeft();
        int y = height - 12;
        if (categories.get(category).name().getString().equals("Loadouts")) {
            loadoutName = new EditBox(font, x, listTop() + 120, ACTION_W, 18, Component.literal("Name"));
            loadoutName.setHint(Component.literal("new loadout name").withStyle(ChatFormatting.DARK_GRAY));
            loadoutName.setMaxLength(24);
            addRenderableWidget(loadoutName);
            Button save = Button.builder(Component.literal("Save current build"), b -> {
                String name = loadoutName.getValue().toLowerCase(Locale.ROOT).trim();
                if (Loadout.validName(name)) {
                    run(List.of("onepiece loadout save " + name));
                    loadoutName.setValue("");
                }
            }).bounds(x, listTop() + 142, ACTION_W, 20).build();
            actionButtons.add(addRenderableWidget(save));
        }
        if (selected == null) {
            return;
        }
        List<Action> actions = selected.actions();
        y -= actions.size() * 22;
        for (Action action : actions) {
            Button button = Button.builder(action.label(), b -> {
                if (action.confirm()) {
                    confirm(Component.literal(action.label().getString() + "?"), selected.name(), action.commands());
                } else {
                    run(action.commands());
                }
            }).bounds(x, y, ACTION_W, 20).build();
            actionButtons.add(addRenderableWidget(button));
            y += 22;
        }
    }

    private void run(List<String> commands) {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            commands.forEach(player.connection::sendCommand);
        }
    }

    private void confirm(Component title, Component message, List<String> commands) {
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFD27A);
        g.drawString(font, categories.get(category).name().copy().withStyle(ChatFormatting.BOLD), detailLeft(), top() + 4, 0xFFFFFFFF);

        int rows = visibleRows();
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
            g.drawString(font, name, textX, y + 5, entry.active().getAsBoolean() ? 0xFF8CF08C : 0xFFE0E4EC);
            if (entry.active().getAsBoolean()) {
                g.drawString(font, "✔", listLeft() + LIST_W - 10, y + 5, 0xFF8CF08C);
            }
        }
        if (shown.isEmpty()) {
            g.drawString(font, Component.literal("Nothing here").withStyle(ChatFormatting.GRAY), listLeft() + 4, listTop() + 4, 0xFFFFFFFF);
        }
        if (shown.size() > rows) {
            int barHeight = Math.max(10, rows * ROW_H * rows / shown.size());
            int barTop = listTop() + (rows * ROW_H - barHeight) * scroll / Math.max(1, shown.size() - rows);
            g.fill(listLeft() + LIST_W + 2, barTop, listLeft() + LIST_W + 4, barTop + barHeight, 0xA0FFFFFF);
        }

        if (selected != null) {
            int y = listTop();
            g.drawString(font, selected.name(), detailLeft(), y, 0xFF000000 | selected.color());
            y += 14;
            int wrap = Math.max(120, width - detailLeft() - 12);
            for (Component line : selected.details()) {
                for (FormattedCharSequence part : font.split(line, wrap)) {
                    if (y > height - 30 - selected.actions().size() * 22) {
                        break;
                    }
                    g.drawString(font, part, detailLeft(), y, 0xFFC8CCD8);
                    y += 10;
                }
                y += 3;
            }
        } else if (loadoutName == null) {
            g.drawWordWrap(font, Component.literal("Pick an entry to see what you can do with it.").withStyle(ChatFormatting.GRAY),
                    detailLeft(), listTop(), Math.max(120, width - detailLeft() - 12), 0xFFFFFFFF);
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
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Categories
    // ---------------------------------------------------------------------------------------------------------

    @Nullable
    private PlayerData data() {
        return minecraft == null || minecraft.player == null ? null : GLPlayer.data(minecraft.player);
    }

    private static Component line(String label, Component value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY).append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    private static Component names(List<Ability> abilities) {
        return Component.literal(abilities.isEmpty() ? "-"
                : String.join(", ", abilities.stream().map(a -> a.displayName().getString()).toList()));
    }

    private static String title(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static ItemStack stack(@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private List<Entry> fruits() {
        List<Entry> out = new ArrayList<>();
        GLFruits.REGISTRY.stream().sorted(Comparator.comparing(f -> f.displayName().getString())).forEach(fruit -> {
            String id = fruit.id().toString();
            out.add(new Entry(fruit.displayName(),
                    List.of(line("Type", fruit.type().display()), line("Rarity", fruit.rarity().display()),
                            line("Abilities", names(fruit.abilities())), line("Awakened", names(fruit.awakenedAbilities())),
                            fruit.description()),
                    fruit.color(), stack(GLItems.itemFor(fruit.id())),
                    () -> data() != null && data().fruit().map(fruit.id()::equals).orElse(false),
                    List.of(Action.of("Eat (set fruit)", "onepiece fruit set " + id),
                            Action.of("Test (max mastery)", "onepiece fruit set " + id, "onepiece mastery set fruit 100"),
                            Action.of("Give fruit item", "onepiece fruit give " + id),
                            Action.of("Awaken", "onepiece fruit set " + id, "onepiece fruit awaken"),
                            Action.of("Remove my fruit", "onepiece fruit remove"))));
        });
        return out;
    }

    private List<Entry> awakenings() {
        List<Entry> out = new ArrayList<>();
        GLFruits.REGISTRY.stream().filter(fruit -> !fruit.awakenedAbilities().isEmpty())
                .sorted(Comparator.comparing(f -> f.displayName().getString())).forEach(fruit -> {
                    String id = fruit.id().toString();
                    out.add(new Entry(fruit.displayName(),
                            List.of(line("Awakened abilities", names(fruit.awakenedAbilities())), fruit.description()),
                            fruit.color(), stack(GLItems.itemFor(fruit.id())),
                            () -> data() != null && data().isUnlocked(UnlockCategory.AWAKENING, fruit.id()),
                            List.of(Action.of("Eat and awaken", "onepiece fruit set " + id, "onepiece fruit awaken"),
                                    Action.of("Unlock every awakening", "onepiece unlock awakenings"))));
                });
        return out;
    }

    private List<Entry> haki() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(Component.literal("All Haki"), List.of(Component.literal("Observation, Armament and Conqueror's at once.")),
                0xC8A0F0, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Awaken all", "onepiece haki unlock all"),
                        Action.of("Max all", "onepiece haki set observation 100", "onepiece haki set armament 100",
                                "onepiece haki set conquerors 100"),
                        Action.of("Infinite Haki on", "onepiece haki infinite on"),
                        Action.of("Infinite Haki off", "onepiece haki infinite off"),
                        Action.of("Remove all", "onepiece haki remove all"))));
        for (HakiType type : HakiType.values()) {
            String name = type.getSerializedName();
            out.add(new Entry(Component.literal(title(name)), List.of(Component.literal("Level 0-100. Using it trains it.")),
                    type.color(), ItemStack.EMPTY,
                    () -> minecraft != null && minecraft.player != null && HakiManager.has(minecraft.player, type),
                    List.of(Action.of("Awaken", "onepiece haki unlock " + name),
                            Action.of("Level 50", "onepiece haki set " + name + " 50"),
                            Action.of("Max (100)", "onepiece haki set " + name + " 100"),
                            Action.of("Remove", "onepiece haki remove " + name))));
        }
        return out;
    }

    private List<Entry> styles() {
        List<Entry> out = new ArrayList<>();
        GLStyles.REGISTRY.stream().sorted(Comparator.comparing(s -> s.displayName().getString())).forEach(style -> {
            String id = style.id().toString();
            out.add(new Entry(style.displayName(), List.of(line("Moves", names(style.moves())), style.description()),
                    style.color(), ItemStack.EMPTY, () -> data() != null && data().style().map(style.id()::equals).orElse(false),
                    List.of(Action.of("Learn and equip", "onepiece style give " + id),
                            Action.of("Equip", "onepiece style equip " + id),
                            Action.of("Max style mastery", "onepiece style give " + id, "onepiece mastery set style " + id + " 100"),
                            Action.of("Forget", "onepiece style remove " + id))));
        });
        return out;
    }

    private List<Entry> rokushiki() {
        List<Entry> out = new ArrayList<>();
        for (Ability ability : GLAbilities.REGISTRY) {
            if (ability.properties().category() != AbilityCategory.ROKUSHIKI) {
                continue;
            }
            String id = ability.id().toString();
            List<Action> actions = new ArrayList<>();
            actions.add(Action.of("Unlock", "onepiece ability unlock " + id));
            for (int slot = 1; slot <= AbilityState.SLOT_COUNT; slot++) {
                actions.add(Action.of("Unlock into slot " + slot, "onepiece ability unlock " + id,
                        "onepiece ability equip " + slot + " " + id));
            }
            actions.add(Action.of("Lock", "onepiece ability lock " + id));
            out.add(new Entry(ability.displayName(), List.of(ability.description()), ability.properties().color(), ItemStack.EMPTY,
                    () -> data() != null && data().isUnlocked(UnlockCategory.ABILITY, ability.id()), actions));
        }
        return out;
    }

    private <T> List<Map.Entry<ResourceKey<T>, T>> sorted(ResourceKey<Registry<T>> key) {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }
        return minecraft.level.registryAccess().registry(key)
                .map(registry -> registry.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().location())).toList())
                .orElse(List.of());
    }

    private List<Entry> races() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<ResourceKey<RaceDefinition>, RaceDefinition> e : sorted(GLRegistries.RACE)) {
            ResourceLocation id = e.getKey().location();
            RaceDefinition race = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(line("Abilities", Component.literal(race.abilities().isEmpty() ? "-"
                    : String.join(", ", race.abilities().stream().map(a -> title(a.getPath())).toList()))));
            details.add(line("Mastery rate", Component.literal(String.format(Locale.ROOT, "x%.2f", race.masteryMultiplier()))));
            details.add(race.description());
            out.add(new Entry(race.displayName(), details, 0xD8B888, ItemStack.EMPTY,
                    () -> data() != null && data().race().equals(id),
                    List.of(Action.of("Become this race", "onepiece race set " + id),
                            Action.of("Max race mastery", "onepiece race set " + id, "onepiece mastery set race 100"))));
        }
        return out;
    }

    private List<Entry> transformations() {
        List<Entry> out = new ArrayList<>();
        GLTransformations.REGISTRY.keySet().stream().sorted().forEach(id -> {
            Transformation form = GLTransformations.REGISTRY.get(id);
            Ability source = form.sourceAbility();
            List<Component> details = new ArrayList<>();
            details.add(line("Bonuses", Component.literal(form.modifiers().size() + " attribute changes")));
            if (source != null) {
                details.add(line("Normally from", source.displayName()));
            }
            details.add(Component.literal("Forcing a form skips its ability, so it lasts until you end it here."));
            out.add(new Entry(Component.literal(title(id.getPath())), details, 0xB8A0E8, ItemStack.EMPTY,
                    () -> minecraft != null && minecraft.player != null && TransformationManager.active(minecraft.player) == form,
                    List.of(Action.of("Transform", "onepiece transform " + id),
                            Action.of("End transformation", "onepiece transform off"))));
        });
        return out;
    }

    private List<Entry> weapons() {
        List<Entry> out = new ArrayList<>();
        GLWeapons.weapons().forEach((name, holder) -> {
            Item item = holder.get();
            List<Component> details = new ArrayList<>();
            if (item instanceof WeaponItem weapon) {
                details.add(line("Kind", Component.literal(weapon.kind().label())));
                details.add(line("Grade", weapon.grade().display()));
            }
            out.add(new Entry(item.getDescription(), details, 0x8A9098, new ItemStack(item),
                    () -> minecraft != null && minecraft.player != null && minecraft.player.getInventory().countItem(item) > 0,
                    List.of(Action.of("Give", "onepiece weapon give " + name),
                            Action.of("Unlock every weapon", "onepiece unlock weapons"))));
        });
        return out;
    }

    private List<Entry> factions() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<ResourceKey<FactionDefinition>, FactionDefinition> e : sorted(GLRegistries.FACTION)) {
            ResourceLocation id = e.getKey().location();
            FactionDefinition faction = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(line("Standing", Component.literal(data() == null ? "-" : String.valueOf(data().reputation(id)))));
            details.add(line("Enemies", Component.literal(faction.enemies().isEmpty() ? "-"
                    : String.join(", ", faction.enemies().stream().map(ResourceLocation::getPath).toList()))));
            faction.wantedBounty().ifPresent(bounty -> details.add(line("Hunts bounties from", Component.literal(String.valueOf(bounty)))));
            details.add(faction.description());
            out.add(new Entry(faction.displayName(), details, faction.color().getValue(), ItemStack.EMPTY,
                    () -> data() != null && data().faction().map(id::equals).orElse(false),
                    List.of(Action.of("Join", "onepiece faction set " + id), Action.of("Leave faction", "onepiece faction clear"),
                            Action.of("Make friends", "onepiece reputation set " + id + " 60"),
                            Action.of("Make enemies", "onepiece reputation set " + id + " -60"),
                            Action.of("Reset standing", "onepiece reputation set " + id + " 0"))));
        }
        out.add(new Entry(Component.literal("Townsfolk"), List.of(line("Standing", Component.literal(data() == null ? "-"
                : String.valueOf(data().reputation(GrandLine.id("civilians"))))), Component.literal("Villagers and merchants of the islands. "
                + "They remember who helps them and who hurts them.")), 0xF2EEDC, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Make friends", "onepiece reputation set grandline:civilians 60"),
                        Action.of("Reset standing", "onepiece reputation set grandline:civilians 0"))));
        return out;
    }

    private List<Entry> npcs() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<ResourceKey<NpcDefinition>, NpcDefinition> e : sorted(GLRegistries.NPC)) {
            ResourceLocation id = e.getKey().location();
            NpcDefinition npc = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(line("Faction", Component.literal(npc.faction().map(ResourceLocation::getPath).orElse("townsfolk"))));
            details.add(line("Role", Component.literal(npc.role().getSerializedName())));
            details.add(line("Stats", Component.literal(String.format(Locale.ROOT, "%.0f health, %.0f attack, %.0f armour", npc.health(),
                    npc.attack(), npc.armor()))));
            if (!npc.trades().isEmpty()) {
                details.add(line("Sells", Component.literal(npc.trades().size() + " wares")));
            }
            int color = minecraft != null && minecraft.level != null
                    ? Factions.color(minecraft.level.registryAccess(), npc.faction()).getValue() : 0xF2EEDC;
            ItemStack icon = npc.equipment().containsKey(EquipmentSlot.MAINHAND) ? new ItemStack(npc.equipment().get(EquipmentSlot.MAINHAND))
                    : npc.trades().isEmpty() ? new ItemStack(Items.PLAYER_HEAD) : new ItemStack(Items.EMERALD);
            out.add(new Entry(npc.displayName(), details, color, icon, () -> false,
                    List.of(Action.of("Spawn here", "onepiece npc spawn " + id), Action.of("Spawn 3", "onepiece npc spawn " + id + " 3"),
                            Action.confirmed("Remove nearby", "onepiece npc remove 16"))));
        }
        return out;
    }

    private List<Entry> bosses() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<ResourceKey<BossDefinition>, BossDefinition> e : sorted(GLRegistries.BOSS)) {
            ResourceLocation id = e.getKey().location();
            BossDefinition boss = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(boss.title().copy().withStyle(ChatFormatting.GRAY));
            details.add(line("Stats", Component.literal(String.format(Locale.ROOT, "%.0f health, %.0f attack, %.0f armour", boss.stats().health(),
                    boss.stats().attack(), boss.stats().armor()))));
            for (int i = 0; i < boss.phases().size(); i++) {
                BossDefinition.Phase phase = boss.phases().get(i);
                details.add(line("Phase " + (i + 1), phase.name().copy().append(Component.literal(" - " + String.join(", ",
                        phase.moves().keySet())).withStyle(ChatFormatting.DARK_GRAY))));
            }
            details.add(line("Beaten", Component.literal(Bounties.format(boss.rewards().bounty()) + " bounty, " + boss.rewards().renown()
                    + " renown")));
            List<Action> actions = new ArrayList<>();
            actions.add(Action.of("Spawn here", "onepiece boss spawn " + id));
            actions.add(Action.of("Nearest boss info", "onepiece boss info"));
            for (int i = 1; i < boss.phases().size(); i++) {
                actions.add(Action.of("Skip to phase " + (i + 1), "onepiece boss phase " + (i + 1)));
            }
            actions.add(Action.confirmed("Remove nearby", "onepiece boss remove 32"));
            ItemStack icon = boss.weapon().map(ItemStack::new).orElseGet(() -> new ItemStack(Items.WITHER_SKELETON_SKULL));
            Integer color = boss.barColor().getFormatting().getColor();
            out.add(new Entry(boss.displayName(), details, color == null ? 0xF2EEDC : color, icon, () -> false, actions));
        }
        return out;
    }

    private List<Entry> quests() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(Component.literal("Log Pose and journal"), List.of(Component.literal("The Log Pose records islands as you reach "
                + "them and points to the next. Open the quest journal with K.")), 0x5AA6C8, new ItemStack(GLQuests.LOG_POSE.get()),
                () -> false, List.of(Action.of("Give a Log Pose", "give @s grandline:log_pose"), Action.of("Find next island", "onepiece logpose next"),
                        Action.of("Show journal", "onepiece quest"), Action.confirmed("Forget islands", "onepiece logpose forget"),
                        Action.confirmed("Reset all quests", "onepiece quest reset"))));
        if (minecraft == null || minecraft.level == null) {
            return out;
        }
        for (Map.Entry<ResourceLocation, QuestDefinition> e : Quests.all(minecraft.level.registryAccess())) {
            ResourceLocation id = e.getKey();
            QuestDefinition quest = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(quest.description().copy().withStyle(ChatFormatting.GRAY));
            details.add(line("Kind", Component.literal(quest.category() == QuestDefinition.Category.MAIN ? "story, part " + quest.order()
                    : "side quest" + quest.giver().map(g -> ", from " + g.getPath()).orElse(""))));
            for (QuestObjective objective : quest.objectives()) {
                details.add(Component.literal("- ").append(Quests.describe(minecraft.level.registryAccess(), objective)));
            }
            if (!quest.requires().isEmpty()) {
                details.add(line("After", Component.literal(String.join(", ", quest.requires().stream().map(ResourceLocation::getPath).toList()))));
            }
            out.add(new Entry(quest.displayName(), details, quest.category() == QuestDefinition.Category.MAIN ? 0xF0C050 : 0xE8E8D0,
                    new ItemStack(quest.category() == QuestDefinition.Category.MAIN ? Items.MAP : Items.WRITABLE_BOOK),
                    () -> data() != null && data().journey().isCompleted(id),
                    List.of(Action.of("Take it on", "onepiece quest accept " + id), Action.of("Complete", "onepiece quest complete " + id),
                            Action.of("Reset", "onepiece quest reset " + id))));
        }
        return out;
    }

    private List<Entry> crewAndBounty() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(Component.literal("Found a crew"), List.of(Component.literal("Founds a crew with you as captain. Invite others with "
                + "/onepiece crew invite, recruit islanders by sneak-right-clicking them with emeralds.")), 0xC62828,
                new ItemStack(Items.BLACK_BANNER), () -> data() != null && data().crew().isPresent(),
                List.of(Action.of("Found \"Creator Crew\"", "onepiece crew create Creator Crew"), Action.of("Crew info", "onepiece crew info"),
                        Action.of("Claim ship (aboard)", "onepiece crew ship"), Action.of("Dismiss recruits", "onepiece crew dismiss"),
                        Action.of("Leave crew", "onepiece crew leave"))));
        for (JollyRoger roger : JollyRoger.values()) {
            out.add(new Entry(Component.literal("Jolly Roger: " + roger.label()), List.of(Component.literal(
                    "Flies on your crew's claimed ship, on its flag and sails.")), 0x303034, ItemStack.EMPTY, () -> false,
                    List.of(Action.of("Fly this flag", "onepiece crew flag " + roger.getSerializedName()))));
        }
        for (long bounty : new long[] {0L, 1_000_000L, 30_000_000L, 120_000_000L, 500_000_000L}) {
            String label = bounty == 0 ? "No bounty" : "Bounty " + Bounties.format(bounty);
            BountyHunts.Tier tier = BountyHunts.TIERS.stream().filter(t -> bounty >= t.bounty()).reduce((a, b) -> b).orElse(null);
            out.add(new Entry(Component.literal(label), List.of(line("Hunted by", Component.literal(tier == null ? "nobody"
                    : tier.hunters().size() + " bounty hunters or " + tier.marines().size() + " Marines"))), 0xE0B83A,
                    new ItemStack(Items.PAPER), () -> data() != null && data().bounty() == bounty,
                    List.of(Action.of("Set bounty", "onepiece bounty set " + bounty), Action.of("Send hunters now", "onepiece bounty hunt"))));
        }
        return out;
    }

    private List<Entry> islands() {
        List<Entry> out = new ArrayList<>();
        for (IslandType island : IslandType.values()) {
            String name = island.getSerializedName();
            out.add(new Entry(island.displayName(), List.of(line("Size", Component.literal("about " + island.radius() * 2 + " blocks across")),
                    line("Buildings", Component.literal(String.valueOf(island.sites().size()))),
                    Component.translatable("island.grandline." + name + ".desc")), 0x6FAF5A, new ItemStack(Items.GRASS_BLOCK), () -> false,
                    List.of(Action.of("Locate", "onepiece island locate " + name), Action.of("Teleport", "onepiece island teleport " + name),
                            Action.confirmed("Raise one here", "onepiece island place " + name))));
        }
        return out;
    }

    private List<Entry> ships() {
        List<Entry> out = new ArrayList<>();
        List<Map.Entry<ResourceKey<ShipDefinition>, ShipDefinition>> ships = new ArrayList<>(sorted(GLRegistries.SHIP));
        ships.sort(Comparator.comparing(e -> e.getValue().length()));
        for (Map.Entry<ResourceKey<ShipDefinition>, ShipDefinition> e : ships) {
            ResourceLocation id = e.getKey().location();
            ShipDefinition ship = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(line("Size", Component.literal(String.format(Locale.ROOT, "%.0f x %.0f blocks, %d masts", ship.length(),
                    ship.width(), ship.masts()))));
            details.add(line("Hull", Component.literal(String.format(Locale.ROOT, "%.0f", ship.maxHealth()))));
            details.add(line("Cannons", Component.literal(ship.cannons() + " a side")));
            details.add(line("Crew", Component.literal(ship.crew() + ", " + ship.storageRows() + " rows of cargo")));
            details.add(ship.description());
            out.add(new Entry(ship.displayName(), details, ship.hullColor(), ShipItem.of(id, ship.maxHealth()),
                    () -> minecraft != null && minecraft.player != null && minecraft.player.getVehicle() instanceof ShipEntity aboard
                            && aboard.typeId().equals(id),
                    List.of(Action.of("Launch here", "onepiece ship spawn " + id), Action.of("Give in a bottle", "onepiece ship give " + id),
                            Action.of("Repair nearest", "onepiece ship repair"), Action.of("Fire (aboard)", "onepiece ship fire"),
                            Action.confirmed("Remove nearest", "onepiece ship remove"))));
        }
        return out;
    }

    private List<Entry> battleTest() {
        List<Entry> out = new ArrayList<>();
        for (Battles.Difficulty difficulty : Battles.Difficulty.values()) {
            String name = difficulty.getSerializedName();
            List<String> wave = difficulty.wave().stream().map(type -> type.getDescription().getString()).toList();
            out.add(new Entry(Component.literal(title(name) + " wave"),
                    List.of(line("Enemies", Component.literal(String.join(", ", wave)))),
                    0xE86A5A, new ItemStack(Items.IRON_SWORD), () -> false,
                    List.of(Action.of("Spawn wave", "onepiece battle random " + name),
                            Action.of("Clear enemies", "onepiece battle clear"))));
        }
        for (EntityType<?> type : List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.RAVAGER, EntityType.WITHER_SKELETON, EntityType.IRON_GOLEM)) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            out.add(new Entry(type.getDescription(), List.of(Component.literal("Spawns around you, already hunting you.")), 0xC88A5A,
                    ItemStack.EMPTY, () -> false,
                    List.of(Action.of("Spawn 1", "onepiece battle spawn " + id), Action.of("Spawn 5", "onepiece battle spawn " + id + " 5"),
                            Action.of("Clear enemies", "onepiece battle clear"))));
        }
        out.add(new Entry(Component.literal("Reset me"), List.of(Component.literal("Full health, no cooldowns, full stamina.")),
                0x8CF08C, new ItemStack(Items.GOLDEN_APPLE), () -> false,
                List.of(Action.of("Heal and reset", "onepiece cooldown reset", "effect give @s minecraft:instant_health 1 10 true",
                        "effect clear @s"),
                        Action.of("Max stats", "onepiece mastery max", "onepiece haki unlock all", "onepiece stamina infinite"),
                        Action.of("Random build", "onepiece random build"),
                        Action.of("Teleport 40 ahead", "tp @s ^ ^ ^40"),
                        Action.of("Teleport 30 up", "tp @s ~ ~30 ~"))));
        return out;
    }

    private List<Entry> loadouts() {
        List<Entry> out = new ArrayList<>();
        PlayerData data = data();
        if (data == null) {
            return out;
        }
        data.loadouts().forEach((name, loadout) -> {
            List<Component> details = new ArrayList<>();
            details.add(line("Race", Component.literal(title(loadout.race().getPath()))));
            details.add(line("Fruit", Component.literal(loadout.fruit().map(f -> title(f.getPath())).orElse("-")
                    + (loadout.awakened() ? " (awakened)" : ""))));
            details.add(line("Style", Component.literal(loadout.style().map(s -> title(s.getPath())).orElse("Brawler"))));
            details.add(line("Haki", Component.literal(loadout.haki().isEmpty() ? "-" : String.join(", ", loadout.haki().keySet()))));
            details.add(line("Weapon", Component.literal(loadout.weapon().map(w -> title(w.getPath())).orElse("-"))));
            out.add(new Entry(Component.literal(name), details, 0x7AC8F0, ItemStack.EMPTY, () -> false,
                    List.of(Action.of("Load", "onepiece loadout load " + name),
                            Action.confirmed("Delete", "onepiece loadout delete " + name))));
        });
        return out;
    }

    private List<Entry> playerData() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(Component.literal("Profile"), List.of(Component.literal("Your race, faction, bounty and more in chat.")),
                0xF0D890, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Show profile", "onepiece profile"), Action.of("Dump saved data", "onepiece debug player"))));
        out.add(new Entry(Component.literal("Death mode"), List.of(Component.literal("Creator mode keeps items and XP on death.")),
                0xF0D890, ItemStack.EMPTY, () -> data() != null && data().deathMode().getSerializedName().equals("creator"),
                List.of(Action.of("Creator", "onepiece deathmode creator"), Action.of("Normal", "onepiece deathmode normal"))));
        out.add(new Entry(Component.literal("Bounty"), List.of(Component.literal("Change your bounty for testing.")),
                0xF0D890, ItemStack.EMPTY, () -> false,
                List.of(Action.of("+1,000,000", "onepiece bounty add 1000000"), Action.of("+100,000,000", "onepiece bounty add 100000000"),
                        Action.of("Reset bounty", "onepiece bounty reset"))));
        for (String part : List.of("race", "fruit", "haki", "faction", "bounty", "mastery", "all")) {
            out.add(new Entry(Component.literal("Reset " + part), List.of(Component.literal("Asks before resetting. Cannot be undone.")),
                    0xE86A5A, ItemStack.EMPTY, () -> false,
                    List.of(Action.confirmed("Reset " + part, "onepiece reset " + part + " confirm"))));
        }
        return out;
    }

    private List<Entry> debug() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(Component.literal("Cooldowns"), List.of(), 0xA8C8F0, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Off", "onepiece cooldown off"), Action.of("On", "onepiece cooldown on"),
                        Action.of("Reset", "onepiece cooldown reset"))));
        out.add(new Entry(Component.literal("Stamina"), List.of(), 0xA8C8F0, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Infinite", "onepiece stamina infinite"), Action.of("Normal", "onepiece stamina normal"))));
        out.add(new Entry(Component.literal("Health"), List.of(), 0xA8C8F0, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Infinite", "onepiece health infinite"), Action.of("Normal", "onepiece health normal"),
                        Action.of("Invincible on", "onepiece invincible on"), Action.of("Invincible off", "onepiece invincible off"))));
        out.add(new Entry(Component.literal("Overview"), List.of(Component.literal("Mod, registries, permissions and config in chat.")),
                0xA8C8F0, ItemStack.EMPTY, () -> false,
                List.of(Action.of("Debug", "onepiece debug"), Action.of("Permissions", "onepiece debug permissions"),
                        Action.of("Config", "onepiece debug config"), Action.of("Test toggles", "onepiece test"))));
        return out;
    }
}
