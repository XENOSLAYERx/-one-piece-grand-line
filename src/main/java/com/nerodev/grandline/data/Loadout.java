package com.nerodev.grandline.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A saved build: race, fruit (and whether it is awakened), fighting style, Haki levels, the weapon to carry and the
 * five ability slots. Saved with {@code /onepiece loadout save <name>} and restored with {@code loadout load}.
 *
 * @param haki  level of each Haki the build has, by type name (types missing from the map are not unlocked)
 * @param slots ability id in each slot, "" for an empty slot
 */
public record Loadout(ResourceLocation race, Optional<ResourceLocation> fruit, boolean awakened, Optional<ResourceLocation> style,
        Map<String, Double> haki, Optional<ResourceLocation> weapon, List<String> slots) {

    public static final Codec<Loadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("race", PlayerData.DEFAULT_RACE).forGetter(Loadout::race),
            ResourceLocation.CODEC.optionalFieldOf("fruit").forGetter(Loadout::fruit),
            Codec.BOOL.optionalFieldOf("awakened", false).forGetter(Loadout::awakened),
            ResourceLocation.CODEC.optionalFieldOf("style").forGetter(Loadout::style),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("haki", Map.of()).forGetter(Loadout::haki),
            ResourceLocation.CODEC.optionalFieldOf("weapon").forGetter(Loadout::weapon),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(Loadout::slots)
    ).apply(i, Loadout::new));

    /** Names are short and simple so they work as command arguments. */
    public static boolean validName(String name) {
        return name.matches("[a-z0-9_\\-]{1,24}");
    }
}
