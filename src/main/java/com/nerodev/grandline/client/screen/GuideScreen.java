package com.nerodev.grandline.client.screen;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.boss.BossDefinition;
import com.nerodev.grandline.boss.GLBosses;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.client.GLKeys;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.Titles;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.RaceDefinition;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.guide.Sources;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.network.OpenGuidePayload;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.quest.Journey;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.race.RaceTrait;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.ship.GLShips;
import com.nerodev.grandline.ship.ShipDefinition;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.ship.ShipItem;
import com.nerodev.grandline.style.FightingStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.world.island.IslandType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Guide (/onepiece guide, default key U): your profile, and everything in the mod with what it does, what it
 * needs, what counters it and how to get it in survival play. A creator also sees buttons that grant things at once;
 * a player sees the choices that are theirs to make: their race (once) and joining a faction that trusts them.
 */
public class GuideScreen extends BrowserScreen {
    /** The boss who holds each island. */
    private static final Map<String, ResourceLocation> ISLAND_BOSSES = Map.of("shells_town", GLBosses.MORGAN,
            "conomi_islands", GLBosses.ARLONG, "orange_town", GLBosses.BUGGY, "syrup_village", GLBosses.KURO);

    private final OpenGuidePayload info;
    @Nullable
    private String pendingEntry;

    public GuideScreen(OpenGuidePayload info) {
        super(Component.translatable("screen.grandline.guide"));
        this.info = info;
        categories.add(new Category(Component.literal("Profile"), this::profile));
        categories.add(new Category(Component.literal("Basics"), this::basics));
        categories.add(new Category(Component.literal("Devil Fruits"), this::fruits));
        categories.add(new Category(Component.literal("Haki"), this::haki));
        categories.add(new Category(Component.literal("Fighting Styles"), this::styles));
        categories.add(new Category(Component.literal("Rokushiki"), this::rokushiki));
        categories.add(new Category(Component.literal("Races"), this::races));
        categories.add(new Category(Component.literal("Weapons"), this::weapons));
        categories.add(new Category(Component.literal("Ships"), this::ships));
        categories.add(new Category(Component.literal("Factions"), this::factions));
        categories.add(new Category(Component.literal("Bosses"), this::bosses));
        categories.add(new Category(Component.literal("Islands"), this::islands));
    }

    /** Opens on {@code category}, with the entry named {@code entry} picked (the autotest uses it). */
    public GuideScreen showing(String categoryName, @Nullable String entry) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).name().getString().equalsIgnoreCase(categoryName)) {
                category = i;
            }
        }
        pendingEntry = entry;
        return this;
    }

    @Override
    protected void init() {
        super.init();
        if (pendingEntry != null) {
            select(pendingEntry);
            pendingEntry = null;
        }
    }

    @Override
    @Nullable
    protected Component emptyHint() {
        return Component.literal("Pick an entry to read about it. The mouse wheel scrolls long pages.").withStyle(ChatFormatting.GRAY);
    }

    /** The Profile page shows you under its list. */
    @Override
    protected void renderListFooter(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (category != 0 || minecraft == null || minecraft.player == null || h < 70) {
            return;
        }
        int size = Math.min(h, 120);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + w / 2 - 40, y, x + w / 2 + 40, y + size, size * 2 / 5, 0.0625f,
                mouseX, mouseY, minecraft.player);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Text helpers
    // ---------------------------------------------------------------------------------------------------------

    private static Component heading(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    private static Component text(String text) {
        return Component.literal(text);
    }

    private static Component note(String text) {
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? ticks / 20 + "s" : String.format(Locale.ROOT, "%.1fs", ticks / 20f);
    }

    private static String activation(ActivationType type) {
        return switch (type) {
            case INSTANT -> "Press";
            case HOLD -> "Hold";
            case TOGGLE -> "Toggle";
            case CHARGE -> "Charge";
        };
    }

    /** An ability as one paragraph: its name and costs, then what it does. */
    private static Component ability(Ability ability) {
        Ability.Properties p = ability.properties();
        List<String> facts = new ArrayList<>();
        facts.add(activation(p.activation()));
        if (p.cooldownTicks() > 0) {
            facts.add(seconds(p.cooldownTicks()) + " cooldown");
        }
        if (p.staminaCost() > 0) {
            facts.add(String.format(Locale.ROOT, "%.0f stamina", p.staminaCost()));
        }
        if (p.staminaPerTick() > 0) {
            facts.add(String.format(Locale.ROOT, "%.0f stamina/s", p.staminaPerTick() * 20));
        }
        if (p.requiredMastery() > 0) {
            facts.add(String.format(Locale.ROOT, "mastery %.0f", p.requiredMastery()));
        }
        return Component.literal("- ").append(ability.displayName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  " + String.join(", ", facts)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n")).append(ability.description());
    }

    private static String percent(double mastery) {
        return String.format(Locale.ROOT, "%.1f / 100", mastery);
    }

    private List<Action> creator(Action... actions) {
        return info.creator() ? List.of(actions) : List.of();
    }

    @Nullable
    private RegistryAccess access() {
        return minecraft == null || minecraft.level == null ? null : minecraft.level.registryAccess();
    }

    /** Every way to get an item matching {@code wanted}, or a line saying there is none outside creator tools. */
    private List<Component> sources(Predicate<ItemStack> wanted, String foundKey) {
        List<Component> out = new ArrayList<>();
        out.add(heading("How to get it"));
        if (minecraft != null && minecraft.level != null) {
            out.addAll(Sources.of(wanted, foundKey, minecraft.level.getRecipeManager(), minecraft.level.registryAccess()));
        }
        if (out.size() == 1) {
            out.add(text("Not found in the wild: only creators can hand it out."));
        }
        return out;
    }

    private List<Component> sources(Item item) {
        return sources(stack -> stack.is(item), BuiltInRegistries.ITEM.getKey(item).getPath());
    }

    private <T> List<Map.Entry<ResourceKey<T>, T>> sorted(ResourceKey<Registry<T>> key) {
        RegistryAccess access = access();
        if (access == null) {
            return List.of();
        }
        return access.registry(key)
                .map(registry -> registry.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().location())).toList())
                .orElse(List.of());
    }

    private Component raceName(ResourceLocation id) {
        RegistryAccess access = access();
        return access == null ? Component.literal(title(id.getPath()))
                : access.registry(GLRegistries.RACE).map(r -> r.get(id)).map(RaceDefinition::displayName)
                        .orElse(Component.literal(title(id.getPath())));
    }

    private Component factionName(ResourceLocation id) {
        RegistryAccess access = access();
        Optional<FactionDefinition> faction = access == null ? Optional.empty() : Factions.definition(access, id);
        return faction.map(f -> f.displayName().copy().withStyle(style -> style.withColor(f.color())))
                .orElse(Component.literal(title(id.getPath())));
    }

    private static String standing(int value) {
        String mood = value <= Factions.HOSTILE ? "hostile" : value >= Factions.FRIENDLY ? "friendly" : "neutral";
        return value + " (" + mood + ")";
    }

    // ---------------------------------------------------------------------------------------------------------
    // Profile
    // ---------------------------------------------------------------------------------------------------------

    private List<Entry> profile() {
        List<Entry> out = new ArrayList<>();
        PlayerData data = data();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (data == null || player == null) {
            return out;
        }

        List<Component> overview = new ArrayList<>();
        overview.add(line("Race", raceName(data.race())));
        overview.add(line("Faction", data.faction().map(this::factionName).orElse(Component.literal("None"))));
        overview.add(line("Bounty", Component.literal(data.bounty() > 0 ? Bounties.format(data.bounty()) : "None")));
        if (info.crewName().isEmpty()) {
            overview.add(line("Crew", Component.literal("None")));
        } else {
            overview.add(line("Crew", Component.literal(info.crewName() + ", captain " + info.crewCaptain() + ", "
                    + info.crewSize() + (info.crewSize() == 1 ? " member" : " members"))));
            overview.add(line("Crew bounty", Component.literal(Bounties.format(info.crewBounty()))));
        }
        overview.add(line("Title", data.title().<Component>map(Titles::name).orElse(Component.literal("None"))));
        overview.add(line("Death mode", data.deathMode().display()));
        if (RaceManager.canChoose(player)) {
            overview.add(note("You have not chosen your race yet. Pick one under Races: it is yours for good."));
        }
        out.add(new Entry(Component.literal(player.getGameProfile().getName()), overview, 0xF0D890, ItemStack.EMPTY, () -> false,
                List.of()));

        DevilFruit fruit = data.fruit().map(GLFruits.REGISTRY::get).orElse(null);
        List<Component> fruitLines = new ArrayList<>();
        if (fruit == null) {
            fruitLines.add(text("You have not eaten a Devil Fruit. See Devil Fruits for where they are found."));
        } else {
            fruitLines.add(line("Fruit", fruit.displayName()));
            fruitLines.add(line("Type", fruit.type().display()));
            fruitLines.add(line("Mastery", Component.literal(percent(data.mastery(fruit.masteryKey())))));
            fruitLines.add(line("Awakened", Component.literal(data.isAwakened() ? "Yes"
                    : String.format(Locale.ROOT, "Not yet (at %.0f mastery)", FruitManager.AWAKENING_MASTERY))));
            fruitLines.add(line("Abilities", names(fruit.abilities())));
        }
        out.add(new Entry(Component.literal("Devil Fruit"), fruitLines, fruit == null ? 0x909090 : fruit.color(),
                fruit == null ? ItemStack.EMPTY : stack(GLItems.itemFor(fruit.id())), () -> false, List.of()));

        List<Component> titleLines = new ArrayList<>();
        List<Action> wear = new ArrayList<>();
        for (ResourceLocation title : data.unlocked(UnlockCategory.TITLE)) {
            boolean worn = data.title().map(title::equals).orElse(false);
            titleLines.add(Component.literal(worn ? "* " : "- ").append(Titles.name(title).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(worn ? "  (worn)" : "").withStyle(ChatFormatting.GRAY)));
            if (!worn) {
                wear.add(Action.of("Wear " + Titles.name(title).getString(), "onepiece title wear " + title));
            }
        }
        if (titleLines.isEmpty()) {
            titleLines.add(text("None yet."));
        }
        titleLines.add(text("Titles are earned by finishing chapters of the story, and worn before your name in chat and in the "
                + "player list."));
        if (data.title().isPresent()) {
            wear.add(Action.of("Wear none", "onepiece title clear"));
        }
        out.add(new Entry(Component.literal("Titles"), titleLines, 0xF0C050, new ItemStack(Items.NAME_TAG), () -> false, wear));

        List<Component> hakiLines = new ArrayList<>();
        for (HakiType type : HakiType.values()) {
            boolean has = HakiManager.has(player, type);
            String state = has ? percent(data.mastery(type.masteryKey())) : switch (type) {
                case ARMAMENT -> "training, " + data.counter(HakiManager.ARMAMENT_HITS) + " of " + GLServerConfig.armamentTrainingHits()
                        + " blows landed";
                case OBSERVATION -> "training, " + data.counter(HakiManager.OBSERVATION_HITS) + " of "
                        + GLServerConfig.observationTrainingHits() + " blows taken";
                case CONQUERORS -> "not born with it";
            };
            hakiLines.add(line(type.displayName().getString(), Component.literal(state)));
        }
        out.add(new Entry(Component.literal("Haki"), hakiLines, 0xC8A0F0, ItemStack.EMPTY, () -> false, List.of()));

        List<Component> fighting = new ArrayList<>();
        FightingStyle style = data.style().map(GLStyles.REGISTRY::get).orElse(GLStyles.BRAWLER.get());
        fighting.add(line("Style", style.displayName()));
        fighting.add(line("Style mastery", Component.literal(percent(data.mastery(style.masteryKey())))));
        List<String> known = new ArrayList<>();
        GLStyles.REGISTRY.forEach(s -> {
            if (s == GLStyles.BRAWLER.get() || data.isUnlocked(UnlockCategory.STYLE, s.id())) {
                known.add(s.displayName().getString());
            }
        });
        fighting.add(line("Styles known", Component.literal(String.join(", ", known))));
        List<Ability> rokushiki = new ArrayList<>();
        GLAbilities.REGISTRY.forEach(a -> {
            if (a.properties().category() == AbilityCategory.ROKUSHIKI && data.isUnlocked(UnlockCategory.ABILITY, a.id())) {
                rokushiki.add(a);
            }
        });
        fighting.add(line("Rokushiki", names(rokushiki)));
        out.add(new Entry(Component.literal("Fighting"), fighting, style.color(), ItemStack.EMPTY, () -> false, List.of()));

        List<Component> mastery = new ArrayList<>();
        Map<String, Double> all = data.allMastery();
        if (all.isEmpty()) {
            mastery.add(text("Nothing trained yet. Every use of an ability trains what it belongs to."));
        }
        all.forEach((key, value) -> mastery.add(line(masteryName(key), Component.literal(percent(value)))));
        out.add(new Entry(Component.literal("Mastery"), mastery, 0x8CC8F0, ItemStack.EMPTY, () -> false, List.of()));

        Journey journey = data.journey();
        List<Component> travels = new ArrayList<>();
        travels.add(line("Quests done", Component.literal(String.valueOf(journey.completedQuests().size()))));
        travels.add(line("Quests under way", Component.literal(String.valueOf(journey.activeQuests().size()))));
        List<String> islands = new ArrayList<>();
        for (IslandType island : IslandType.values()) {
            if (journey.hasFound(island.getSerializedName())) {
                islands.add(island.displayName().getString());
            }
        }
        travels.add(line("Islands recorded", Component.literal(islands.size() + " of " + IslandType.values().length
                + (islands.isEmpty() ? "" : ": " + String.join(", ", islands)))));
        travels.add(text("Open the quest journal with " + GLKeys.QUEST_JOURNAL.getTranslatedKeyMessage().getString() + "."));
        out.add(new Entry(Component.literal("Journey"), travels, 0x5AA6C8, new ItemStack(Items.MAP), () -> false, List.of()));
        return out;
    }

    /** "fruit/grandline:gum_gum" reads "Gum-Gum Fruit", "haki/armament" reads "Armament Haki". */
    private String masteryName(String key) {
        int slash = key.indexOf('/');
        String kind = slash < 0 ? key : key.substring(0, slash);
        String rest = slash < 0 ? "" : key.substring(slash + 1);
        ResourceLocation id = ResourceLocation.tryParse(rest);
        return switch (kind) {
            case "fruit" -> id != null && GLFruits.REGISTRY.get(id) != null ? GLFruits.REGISTRY.get(id).displayName().getString()
                    : title(rest);
            case "style" -> id != null && GLStyles.REGISTRY.get(id) != null ? GLStyles.REGISTRY.get(id).displayName().getString()
                    : title(rest);
            case "haki" -> HakiType.byName(rest).map(t -> t.displayName().getString()).orElse(title(rest));
            case "race" -> id != null ? raceName(id).getString() + " (race)" : title(rest);
            default -> title(key.replace('/', '_'));
        };
    }

    // ---------------------------------------------------------------------------------------------------------
    // Basics
    // ---------------------------------------------------------------------------------------------------------

    private static String key(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private List<Entry> basics() {
        List<Entry> out = new ArrayList<>();
        List<Component> controls = new ArrayList<>();
        List<String> slots = new ArrayList<>();
        for (KeyMapping slot : GLKeys.SLOTS) {
            slots.add(key(slot));
        }
        controls.add(line("Ability slots", Component.literal(String.join(" ", slots))));
        controls.add(line("Ability menu", Component.literal(key(GLKeys.ABILITY_MENU) + ": put your abilities in the slots")));
        controls.add(line("Dodge", Component.literal(key(GLKeys.DODGE) + " while moving")));
        controls.add(line("Guard", Component.literal("hold " + key(GLKeys.GUARD))));
        controls.add(line("Broadside", Component.literal(key(GLKeys.SHIP_FIRE) + " aboard a ship")));
        controls.add(line("Coup de Burst", Component.literal(key(GLKeys.SHIP_BURST) + " at the helm")));
        controls.add(line("Quest journal", Component.literal(key(GLKeys.QUEST_JOURNAL))));
        controls.add(line("Guide", Component.literal(key(GLKeys.GUIDE))));
        controls.add(line("Creator Menu", Component.literal(key(GLKeys.CREATOR_MENU) + " (creators only)")));
        controls.add(text("Change any of them under Options, Controls, One Piece: Grand Line."));
        out.add(new Entry(Component.literal("Controls"), controls, 0xA8C8F0, new ItemStack(Items.COMPASS), () -> false, List.of()));

        out.add(new Entry(Component.literal("Combat"), List.of(
                text("Blows landed in quick succession build a combo that hits a little harder each step. Every "
                        + CombatManager.FINISHER_EVERY + "th blow is your fighting style's finisher."),
                text("Attack while sneaking for a heavy blow: it costs stamina, hits half again as hard and, after a few "
                        + "chained blows, ends the combo with the finisher."),
                text("Blows struck while falling, or right after a dodge, hit harder."),
                text("Hold " + key(GLKeys.GUARD) + " to guard: most of a blow from the front is stopped, at a stamina cost. Raise "
                        + "it just as the blow lands to parry: no damage, the attacker staggers, and your next blow is a counter "
                        + "that hits half again as hard. A guard that runs out of stamina breaks."),
                text("Press " + key(GLKeys.DODGE) + " to dash the way you are moving, untouchable for a moment. Being hit drops your "
                        + "combo.")), 0xE8A040, new ItemStack(Items.IRON_SWORD), () -> false, List.of()));

        out.add(new Entry(Component.literal("Stamina and mastery"), List.of(
                text("Abilities cost stamina and have cooldowns; the bar refills when you rest from them."),
                text("Every use of an ability trains what it belongs to: your fruit, your style, your Haki or your race, from 0 "
                        + "to 100. Stronger moves open as mastery grows, and a fruit awakens at 100."),
                text("See Profile for how far you have come.")), 0x8CC8F0, new ItemStack(Items.EXPERIENCE_BOTTLE), () -> false,
                List.of()));

        out.add(new Entry(Component.literal("Devil Fruits"), List.of(
                text("Rare fruits hidden in island treasure. Eat one and its power is yours for life, even past death."),
                text(GLServerConfig.secondFruitKills()
                        ? "One fruit per body: eating a second (you must sneak to try) kills you."
                        : "One fruit per body: a second one (eaten while sneaking) replaces the first."),
                text("The sea and Seastone drain any fruit user: in water, or touching Seastone, the power fails and the body "
                        + "grows weak.")), 0xC04040, stack(GLItems.itemFor(GLFruits.GUM_GUM.getId())), () -> false, List.of()));

        out.add(new Entry(Component.literal("Haki"), List.of(
                text("Armament awakens in those who fight: land " + GLServerConfig.armamentTrainingHits() + " blows yourself."),
                text("Observation awakens in those who are tested: take " + GLServerConfig.observationTrainingHits()
                        + " blows from living foes."),
                text(String.format(Locale.ROOT, "Conqueror's is something you are born with or not: about %.0f%% of players are.",
                        GLServerConfig.conquerorsChance() * 100)),
                text("Armament Haki strikes a Logia's elemental body.")), 0xC8A0F0, ItemStack.EMPTY, () -> false, List.of()));

        out.add(new Entry(Component.literal("Bounty and factions"), List.of(
                text("Crimes against the Marines and the townsfolk raise your bounty; the higher it climbs, the stronger the "
                        + "bounty hunters and Marines who come for you."),
                text("Every faction remembers how you treat its people and its enemies. Reach a standing of " + Factions.FRIENDLY
                        + " and you may join it (Factions); the Marines take no one with a bounty.")), 0xE0B83A,
                new ItemStack(Items.PAPER), () -> false, List.of()));

        out.add(new Entry(Component.literal("Crews"), List.of(
                text("Found a crew with /onepiece crew create <name>, invite friends with /onepiece crew invite."),
                text("Recruit islanders by sneak-right-clicking them with emeralds; they follow and fight for you."),
                text("Crewmates' blows glance off each other. The captain can claim a ship and fly a Jolly Roger from it.")),
                0xC62828, new ItemStack(Items.BLACK_BANNER), () -> false, List.of()));

        out.add(new Entry(Component.literal("Ships and the sea"), List.of(
                text("Use a Ship in a Bottle on open water to launch it. Right-click to board, sneak to leave; at the helm the "
                        + "movement keys sail it."),
                text("Load cannonballs into the hold (right-click with them), then " + key(GLKeys.SHIP_FIRE) + " fires a broadside "
                        + "toward the side you look at. Planks repair the hull, a water bucket puts out a fire."),
                text("Sneak-right-click opens the hold; sneak-right-click with an empty bottle (with no one aboard) bottles "
                        + "the ship up again."),
                text("The sea is not empty. Sailors meet Marine patrols (which run down anyone wanted), pirate raiders (which "
                        + "attack anyone they do not count as a friend) and merchant ships heavy with cargo that flee when attacked. "
                        + "Sink one and its hold spills; kill its crew and the ship is yours to sail or bottle."),
                text("Storms make the sea heave and push the bow about, and lightning finds masts. Currents set across the "
                        + "open ocean and carry a ship under way (the ship's display shows them). Sea Kings ram ships.")), 0x8B5A2B,
                ShipItem.of(ShipEntity.DEFAULT_TYPE, 1f), () -> false, List.of()));

        out.add(new Entry(Component.literal("Islands and the Log Pose"), List.of(
                text("The East Blue's islands rise from the ocean: Dawn Island, Shells Town, Orange Town, Syrup Village and the "
                        + "Conomi Islands, each with its own people, buildings and treasure."),
                text("A Log Pose records every island you reach and points the way to the next. The story starts on Dawn "
                        + "Island; follow it in the quest journal (" + key(GLKeys.QUEST_JOURNAL) + ")."),
                text("The bosses who hold islands return a while after they fall, so everyone on a server can play the "
                        + "story; they grow tougher when fought by a party.")), 0x5AA6C8, new ItemStack(Items.MAP), () -> false,
                List.of()));
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Devil Fruits
    // ---------------------------------------------------------------------------------------------------------

    private List<Entry> fruits() {
        List<Entry> out = new ArrayList<>();
        GLFruits.REGISTRY.stream().sorted(Comparator.comparing(f -> f.displayName().getString())).forEach(fruit -> {
            List<Component> details = new ArrayList<>();
            details.add(line("Type", fruit.type().display()));
            details.add(line("Rarity", fruit.rarity().display()));
            details.add(line("Name", Component.literal(fruit.originalName())));
            details.add(fruit.description());
            details.add(heading("Abilities"));
            fruit.abilities().forEach(a -> details.add(ability(a)));
            if (!fruit.awakenedAbilities().isEmpty()) {
                details.add(heading(String.format(Locale.ROOT, "Awakening (at %.0f mastery)", FruitManager.AWAKENING_MASTERY)));
                fruit.awakenedAbilities().forEach(a -> details.add(ability(a)));
            }
            details.add(heading("Counters"));
            details.add(text("Sea water and Seastone: in water or touching Seastone the power fails and the body grows weak."));
            if (fruit.type() == FruitType.LOGIA) {
                details.add(text("Armament Haki and Seastone weapons strike the elemental body; out of stamina it cannot scatter; "
                        + "explosions and other harm that is not a blow always land."));
            }
            String counter = "guide.grandline.counter." + fruit.id().getPath();
            if (Language.getInstance().has(counter)) {
                details.add(Component.translatable(counter));
            }
            details.add(heading("How to get it"));
            details.add(text("Hidden in island treasure, one fruit of all " + GLFruits.REGISTRY.size() + " at random: buried "
                    + "treasure (about 1 chest in 12), Arlong's hoard (1 in 20) and Buggy's stash (1 in 24). Eat it to gain its "
                    + "power."));
            String id = fruit.id().toString();
            out.add(new Entry(fruit.displayName(), details, fruit.color(), stack(GLItems.itemFor(fruit.id())),
                    () -> data() != null && data().fruit().map(fruit.id()::equals).orElse(false),
                    creator(Action.of("Unlock (eat it)", "onepiece fruit set " + id), Action.of("Give the fruit", "onepiece fruit give " + id))));
        });
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Haki, styles and Rokushiki
    // ---------------------------------------------------------------------------------------------------------

    private List<Entry> haki() {
        List<Entry> out = new ArrayList<>();
        for (HakiType type : HakiType.values()) {
            List<Component> details = new ArrayList<>();
            details.add(Component.translatable("haki.grandline." + type.getSerializedName() + ".desc"));
            details.add(heading("Techniques"));
            List<Ability> moves = new ArrayList<>();
            GLAbilities.REGISTRY.forEach(a -> {
                if (a.properties().category() == AbilityCategory.HAKI && a.properties().masteryKey().filter(type.masteryKey()::equals).isPresent()) {
                    moves.add(a);
                }
            });
            moves.sort(Comparator.comparingDouble(a -> a.properties().requiredMastery()));
            moves.forEach(a -> details.add(ability(a)));
            details.add(heading("How to get it"));
            details.add(text(switch (type) {
                case ARMAMENT -> "Fight: it awakens after " + GLServerConfig.armamentTrainingHits() + " blows you land yourself.";
                case OBSERVATION -> "Be tested: it awakens after " + GLServerConfig.observationTrainingHits()
                        + " blows taken from living foes.";
                case CONQUERORS -> String.format(Locale.ROOT, "Born with it: about %.0f%% of players are, decided the first time "
                        + "they join. It cannot be trained into being.", GLServerConfig.conquerorsChance() * 100);
            }));
            details.add(text("Each use trains it from 0 to 100, and its techniques open as it grows."));
            String name = type.getSerializedName();
            out.add(new Entry(type.displayName(), details, type.color(), ItemStack.EMPTY,
                    () -> minecraft != null && minecraft.player != null && HakiManager.has(minecraft.player, type),
                    creator(Action.of("Unlock", "onepiece haki unlock " + name))));
        }
        return out;
    }

    @Nullable
    private static Item scrollFor(ResourceLocation id) {
        return GLItems.scrolls().stream().map(holder -> holder.get()).filter(scroll -> scroll.teaches(id)).findFirst().orElse(null);
    }

    private List<Component> scrollSources(ResourceLocation id) {
        Item scroll = scrollFor(id);
        if (scroll == null) {
            return List.of(heading("How to get it"), text("Every fighter knows it from the start."));
        }
        List<Component> out = sources(scroll);
        out.add(text("Read the scroll (use it) to learn it for good."));
        return out;
    }

    private List<Entry> styles() {
        List<Entry> out = new ArrayList<>();
        GLStyles.REGISTRY.stream().sorted(Comparator.comparing(s -> s.displayName().getString())).forEach(style -> {
            List<Component> details = new ArrayList<>();
            details.add(style.description());
            details.add(heading("Moves"));
            style.moves().forEach(a -> details.add(ability(a)));
            details.addAll(scrollSources(style.id()));
            String id = style.id().toString();
            out.add(new Entry(style.displayName(), details, style.color(), stack(scrollFor(style.id())),
                    () -> data() != null && data().style().map(style.id()::equals).orElse(style == GLStyles.BRAWLER.get()),
                    creator(Action.of("Unlock", "onepiece style give " + id))));
        });
        return out;
    }

    private List<Entry> rokushiki() {
        List<Entry> out = new ArrayList<>();
        GLAbilities.REGISTRY.forEach(technique -> {
            if (technique.properties().category() != AbilityCategory.ROKUSHIKI) {
                return;
            }
            List<Component> details = new ArrayList<>();
            details.add(ability(technique));
            details.addAll(scrollSources(technique.id()));
            out.add(new Entry(technique.displayName(), details, technique.properties().color(), stack(scrollFor(technique.id())),
                    () -> data() != null && data().isUnlocked(UnlockCategory.ABILITY, technique.id()),
                    creator(Action.of("Unlock", "onepiece ability unlock " + technique.id()))));
        });
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Races, weapons and ships
    // ---------------------------------------------------------------------------------------------------------

    private static String trait(RaceTrait trait) {
        return switch (trait) {
            case AQUATIC -> "Breathes water, sees and swims well under it, and fights a fifth harder in it.";
            case ELECTRO -> "Bare-handed blows carry Electro, and lightning does half the harm.";
        };
    }

    private static Component bonus(RaceDefinition.Bonus bonus) {
        boolean flat = bonus.operation() == AttributeModifier.Operation.ADD_VALUE;
        String amount = flat ? String.format(Locale.ROOT, "%+.1f", bonus.amount()).replace(".0", "")
                : String.format(Locale.ROOT, "%+.0f%%", bonus.amount() * 100);
        return Component.literal("- " + amount + " ").append(Component.translatable(bonus.attribute().value().getDescriptionId()));
    }

    private List<Entry> races() {
        List<Entry> out = new ArrayList<>();
        boolean canChoose = minecraft != null && minecraft.player != null && RaceManager.canChoose(minecraft.player);
        for (Map.Entry<ResourceKey<RaceDefinition>, RaceDefinition> e : sorted(GLRegistries.RACE)) {
            ResourceLocation id = e.getKey().location();
            RaceDefinition race = e.getValue();
            List<Component> details = new ArrayList<>();
            details.add(race.description());
            if (!race.attributes().isEmpty()) {
                details.add(heading("Body"));
                race.attributes().forEach(b -> details.add(bonus(b)));
            }
            race.traits().forEach(t -> details.add(text("- " + trait(t))));
            if (!race.abilities().isEmpty()) {
                details.add(heading("Abilities"));
                race.abilities().stream().map(GLAbilities.REGISTRY::get).filter(a -> a != null).forEach(a -> details.add(ability(a)));
            }
            details.add(line("Mastery rate", Component.literal(String.format(Locale.ROOT, "x%.2f", race.masteryMultiplier()))));
            details.add(heading("How to get it"));
            details.add(text("Everyone starts Human. Each player may choose their race once, here or with /onepiece race choose."));
            if (canChoose) {
                details.add(note("You may still choose."));
            }
            List<Action> actions = new ArrayList<>();
            if (canChoose) {
                actions.add(Action.confirmed("Choose this race", "onepiece race choose " + id));
            }
            if (info.creator()) {
                actions.add(Action.of("Become (creator)", "onepiece race set " + id));
            }
            out.add(new Entry(race.displayName(), details, 0xD8B888, ItemStack.EMPTY,
                    () -> data() != null && data().race().equals(id), actions));
        }
        return out;
    }

    private List<Entry> weapons() {
        List<Entry> out = new ArrayList<>();
        GLWeapons.weapons().forEach((name, holder) -> {
            Item item = holder.get();
            ItemStack stack = new ItemStack(item);
            List<Component> details = new ArrayList<>();
            if (minecraft != null && minecraft.level != null) {
                List<Component> tooltip = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
                tooltip.stream().skip(1).filter(lineText -> !lineText.getString().isBlank()).forEach(details::add);
            }
            details.addAll(sources(item));
            out.add(new Entry(item.getDescription(), details, 0x8A9098, stack,
                    () -> minecraft != null && minecraft.player != null && minecraft.player.getInventory().countItem(item) > 0,
                    creator(Action.of("Give", "onepiece weapon give " + name))));
        });
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
            details.add(ship.description());
            details.add(line("Size", Component.literal(String.format(Locale.ROOT, "%.0f x %.0f blocks, %d masts", ship.length(),
                    ship.width(), ship.masts()))));
            details.add(line("Hull", Component.literal(String.format(Locale.ROOT, "%.0f", ship.maxHealth()))));
            details.add(line("Cannons", Component.literal(ship.cannons() + " a side")));
            details.add(line("Crew", Component.literal(ship.crew() + ", " + ship.storageRows() + " rows of cargo")));
            details.addAll(sources(stack -> stack.is(GLShips.SHIP_ITEM.get()) && ShipItem.type(stack).equals(id), "ship." + id.getPath()));
            out.add(new Entry(ship.displayName(), details, ship.hullColor(), ShipItem.of(id, ship.maxHealth()), () -> false,
                    creator(Action.of("Give in a bottle", "onepiece ship give " + id))));
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Factions, bosses and islands
    // ---------------------------------------------------------------------------------------------------------

    private List<Entry> factions() {
        List<Entry> out = new ArrayList<>();
        PlayerData data = data();
        for (Map.Entry<ResourceKey<FactionDefinition>, FactionDefinition> e : sorted(GLRegistries.FACTION)) {
            ResourceLocation id = e.getKey().location();
            FactionDefinition faction = e.getValue();
            int standing = data == null ? 0 : data.reputation(id);
            boolean member = data != null && data.faction().map(id::equals).orElse(false);
            List<Component> details = new ArrayList<>();
            details.add(faction.description());
            details.add(line("Your standing", Component.literal(standing(standing))));
            details.add(line("Enemies", Component.literal(faction.enemies().isEmpty() ? "-"
                    : String.join(", ", faction.enemies().stream().map(enemy -> factionName(enemy).getString()).toList()))));
            faction.wantedBounty().ifPresent(bounty -> details.add(line("Hunts anyone wanted for",
                    Component.literal(Bounties.format(bounty) + " or more"))));
            details.add(heading("How to join"));
            details.add(text("Win their trust: fight their enemies, help their people and take on their quests. At a standing of "
                    + Factions.FRIENDLY + " they will have you." + (id.equals(Factions.MARINES) ? " The Marines take no one with a "
                    + "bounty." : "") + " Leaving costs you " + Factions.FRIENDLY + " standing with them."));
            List<Action> actions = new ArrayList<>();
            if (member) {
                actions.add(Action.confirmed("Leave", "onepiece faction leave"));
            } else if (data != null && data.faction().isEmpty() && standing >= Factions.FRIENDLY) {
                actions.add(Action.of("Join", "onepiece faction join " + id));
            }
            if (info.creator() && !member) {
                actions.add(Action.of("Join now (creator)", "onepiece faction set " + id));
            }
            out.add(new Entry(faction.displayName(), details, faction.color().getValue(), ItemStack.EMPTY,
                    () -> data() != null && data().faction().map(id::equals).orElse(false), actions));
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
            String home = ISLAND_BOSSES.entrySet().stream().filter(i -> i.getValue().equals(id)).findFirst()
                    .map(i -> IslandType.byName(i.getKey())).map(island -> island.displayName().getString())
                    .orElse(boss.body() == BossDefinition.Body.SEA_KING ? "the open sea, rising to meet ships" : "anywhere");
            details.add(line("Found", Component.literal(home)));
            details.add(line("Strength", Component.literal(String.format(Locale.ROOT, "%.0f health, %.0f attack, %.0f armour",
                    boss.stats().health(), boss.stats().attack(), boss.stats().armor()))));
            details.add(heading("Phases"));
            for (int i = 0; i < boss.phases().size(); i++) {
                BossDefinition.Phase phase = boss.phases().get(i);
                List<String> moves = phase.moves().keySet().stream().filter(m -> !m.equals("roar") && !m.equals("guard"))
                        .map(GuideScreen::moveName).toList();
                details.add(Component.literal((i + 1) + ". ").append(phase.name().copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" - " + String.join(", ", moves)).withStyle(ChatFormatting.GRAY)));
            }
            String tip = "boss.grandline." + id.getPath() + ".tip";
            if (Language.getInstance().has(tip)) {
                details.add(heading("How to beat it"));
                details.add(Component.translatable(tip));
            }
            details.add(heading("Reward"));
            List<String> reward = new ArrayList<>();
            if (boss.rewards().bounty() > 0) {
                reward.add(Bounties.format(boss.rewards().bounty()) + " bounty");
            }
            if (boss.rewards().renown() > 0) {
                reward.add(boss.rewards().renown() + " crew renown");
            }
            boss.weapon().ifPresent(weapon -> reward.add(weapon.getDescription().getString()));
            details.add(text(reward.isEmpty() ? "Its treasure." : String.join(", ", reward)));
            ItemStack icon = boss.weapon().map(ItemStack::new).orElseGet(() -> new ItemStack(Items.WITHER_SKELETON_SKULL));
            Integer color = boss.barColor().getFormatting().getColor();
            out.add(new Entry(boss.displayName(), details, color == null ? 0xF2EEDC : color, icon, () -> false,
                    creator(Action.of("Spawn here", "onepiece boss spawn " + id))));
        }
        return out;
    }

    /** A boss move's name ("bossmove.grandline.<id>"), or its id in words. */
    private static String moveName(String id) {
        String key = "bossmove.grandline." + id;
        return Language.getInstance().has(key) ? Language.getInstance().getOrDefault(key) : title(id);
    }

    private List<Entry> islands() {
        List<Entry> out = new ArrayList<>();
        RegistryAccess access = access();
        for (IslandType island : IslandType.values()) {
            String name = island.getSerializedName();
            List<Component> details = new ArrayList<>();
            details.add(Component.translatable("island.grandline." + name + ".desc"));
            details.add(line("Size", Component.literal("about " + island.radius() * 2 + " blocks across")));
            ResourceLocation bossId = ISLAND_BOSSES.get(name);
            if (bossId != null && access != null) {
                access.registry(GLRegistries.BOSS).map(r -> r.get(bossId))
                        .ifPresent(boss -> details.add(line("Held by", boss.displayName())));
            }
            details.add(heading("How to find it"));
            details.add(text("Islands rise from the East Blue's oceans, a few hundred blocks apart. A Log Pose points to the "
                    + "nearest one you have not recorded."));
            MutableComponent named = island.displayName().copy();
            out.add(new Entry(named, details, 0x6FAF5A, new ItemStack(Items.GRASS_BLOCK),
                    () -> data() != null && data().journey().hasFound(name),
                    creator(Action.of("Teleport", "onepiece island teleport " + name))));
        }
        return out;
    }
}
