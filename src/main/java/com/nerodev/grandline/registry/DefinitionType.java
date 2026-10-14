package com.nerodev.grandline.registry;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.definition.RaceDefinition;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Describes one content registry for generic tooling (debug inspection, tab-completion, later the guide and
 * creator menu). Adding a registry to {@link #ALL} makes it show up in all of those automatically.
 *
 * @param commandName singular name used in commands, e.g. {@code fruit}
 * @param key         registry key
 * @param name        display name of an entry
 * @param details     detail lines of an entry, shown by {@code /onepiece debug <type> <id>}
 */
public record DefinitionType<T>(
        String commandName,
        ResourceKey<Registry<T>> key,
        Function<T, Component> name,
        Function<T, List<Component>> details) {

    public static final DefinitionType<DevilFruit> FRUIT = new DefinitionType<>(
            "fruit", GLFruits.KEY, DevilFruit::displayName,
            fruit -> {
                List<Component> lines = new ArrayList<>();
                lines.add(line("Original name", Component.literal(fruit.originalName())));
                lines.add(line("Type", fruit.type().display()));
                lines.add(line("Rarity", fruit.rarity().display()));
                lines.add(line("Abilities", names(fruit.abilities())));
                lines.add(line("Awakened", names(fruit.awakenedAbilities())));
                lines.add(line("Description", fruit.description()));
                return lines;
            });

    public static final DefinitionType<RaceDefinition> RACE = new DefinitionType<>(
            "race", GLRegistries.RACE, RaceDefinition::displayName,
            race -> List.of(
                    line("Abilities", Component.literal(race.abilities().isEmpty() ? "-"
                            : String.join(", ", race.abilities().stream().map(ResourceLocation::getPath).toList()))),
                    line("Traits", Component.literal(race.traits().isEmpty() ? "-"
                            : String.join(", ", race.traits().stream().map(t -> t.getSerializedName()).toList()))),
                    line("Mastery", Component.literal(String.format(Locale.ROOT, "x%.2f", race.masteryMultiplier()))),
                    line("Description", race.description())));

    public static final DefinitionType<FactionDefinition> FACTION = new DefinitionType<>(
            "faction", GLRegistries.FACTION, FactionDefinition::displayName,
            faction -> List.of(
                    line("Colour", Component.literal(faction.color().serialize())
                            .withStyle(s -> s.withColor(faction.color()))),
                    line("Description", faction.description())));

    public static final DefinitionType<Ability> ABILITY = new DefinitionType<>(
            "ability", GLAbilities.KEY, Ability::displayName,
            ability -> {
                Ability.Properties props = ability.properties();
                return List.of(
                        line("Activation", Component.literal(props.activation().name().toLowerCase(Locale.ROOT))),
                        line("Category", Component.literal(props.category().name().toLowerCase(Locale.ROOT))),
                        line("Cooldown", Component.literal(String.format(Locale.ROOT, "%.1fs", props.cooldownTicks() / 20f))),
                        line("Stamina", Component.literal(String.format(Locale.ROOT, "%.0f (+%.1f/tick)",
                                props.staminaCost(), props.staminaPerTick()))),
                        line("Description", ability.description()));
            });

    public static final List<DefinitionType<?>> ALL = List.of(FRUIT, RACE, FACTION, ABILITY);

    public Registry<T> registry(RegistryAccess access) {
        return access.registryOrThrow(key);
    }

    /** All entry ids, sorted alphabetically for stable output. */
    public List<ResourceLocation> sortedIds(RegistryAccess access) {
        List<ResourceLocation> ids = new ArrayList<>(registry(access).keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return ids;
    }

    /**
     * Looks up an entry. Accepts full ids ({@code grandline:gum_gum}) and bare paths ({@code gum_gum}); a bare
     * path parses as {@code minecraft:gum_gum}, so if nothing matches we look for a unique entry with that path
     * in any namespace.
     */
    public Optional<Map.Entry<ResourceLocation, T>> find(RegistryAccess access, ResourceLocation id) {
        Registry<T> registry = registry(access);
        T direct = registry.get(id);
        if (direct != null) {
            return Optional.of(Map.entry(id, direct));
        }
        if (!id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return Optional.empty();
        }
        List<ResourceLocation> matches = registry.keySet().stream()
                .filter(candidate -> candidate.getPath().equals(id.getPath()))
                .toList();
        if (matches.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(Map.entry(matches.getFirst(), registry.get(matches.getFirst())));
    }

    public String pluralName() {
        return commandName.toLowerCase(Locale.ROOT) + "s";
    }

    private static Component names(List<Ability> abilities) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        for (int i = 0; i < abilities.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            out.append(abilities.get(i).displayName().copy().withStyle(ChatFormatting.AQUA));
        }
        return abilities.isEmpty() ? Component.literal("-") : out;
    }

    private static Component line(String label, Component value) {
        return Component.literal("  " + label + ": ").withStyle(ChatFormatting.GRAY).append(value.copy());
    }
}
