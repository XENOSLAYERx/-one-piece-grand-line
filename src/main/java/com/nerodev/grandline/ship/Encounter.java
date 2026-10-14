package com.nerodev.grandline.ship;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.crew.JollyRoger;
import com.nerodev.grandline.npc.Factions;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The ships met at sea: who crews them, what they sail, what is in their hold, and how they behave. The first crew
 * member listed takes the helm.
 */
public enum Encounter implements StringRepresentable {
    /** A Marine warship on patrol: it runs down anyone the Marines want and leaves everyone else be. */
    MARINE_PATROL("A Marine warship is on patrol nearby.", ChatFormatting.AQUA, Optional.of(Factions.MARINES), null,
            List.of("marine_warship"), List.of("marine_lieutenant", "marine_rifleman", "marine_recruit", "marine_rifleman", "marine_recruit")),
    /** A pirate ship looking for prey: it attacks any sailor its crew does not count as a friend. */
    PIRATE_RAIDER("Pirates! A pirate ship has you in its sights.", ChatFormatting.RED, Optional.of(GrandLine.id("pirate")),
            JollyRoger.CLASSIC, List.of("caravel", "galleon"),
            List.of("pirate_captain", "pirate_gunner", "pirate_grunt", "pirate_gunner", "pirate_grunt")),
    /** A merchant ship heavy with cargo: it never fights, and runs from anyone who attacks it. */
    MERCHANT("A merchant ship sails by, heavy with cargo.", ChatFormatting.GREEN, Optional.empty(), null, List.of("sloop", "caravel"),
            List.of("travelling_merchant", "townsperson", "townsperson"));

    private final String warning;
    private final ChatFormatting color;
    private final Optional<ResourceLocation> faction;
    @Nullable
    private final JollyRoger flag;
    private final List<String> ships;
    private final List<String> crew;

    Encounter(String warning, ChatFormatting color, Optional<ResourceLocation> faction, @Nullable JollyRoger flag, List<String> ships,
            List<String> crew) {
        this.warning = warning;
        this.color = color;
        this.faction = faction;
        this.flag = flag;
        this.ships = ships;
        this.crew = crew;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Encounter byName(String name) {
        for (Encounter encounter : values()) {
            if (encounter.getSerializedName().equals(name)) {
                return encounter;
            }
        }
        return null;
    }

    /** What sailors nearby are told when it comes into view. */
    public String warning() {
        return warning;
    }

    public ChatFormatting color() {
        return color;
    }

    /** The crew's faction; empty for townsfolk. */
    public Optional<ResourceLocation> faction() {
        return faction;
    }

    /** The flag it flies, if not its ship's own colours. */
    public Optional<JollyRoger> flag() {
        return Optional.ofNullable(flag);
    }

    /** Whether it goes after prey (a merchant never does; it runs). */
    public boolean hunts() {
        return this != MERCHANT;
    }

    public ResourceLocation ship(RandomSource random) {
        return GrandLine.id(ships.get(random.nextInt(ships.size())));
    }

    public List<ResourceLocation> crew() {
        return crew.stream().map(GrandLine::id).toList();
    }

    /** What its hold carries. */
    public ResourceKey<LootTable> cargo() {
        return ResourceKey.create(Registries.LOOT_TABLE, GrandLine.id("chests/ship_" + getSerializedName()));
    }

    /** "Marine Patrol" */
    public String title() {
        String name = getSerializedName().replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String word : name.split(" ")) {
            out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
