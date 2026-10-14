package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nerodev.grandline.race.RaceTrait;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * JSON-defined race. Loaded from {@code data/<namespace>/grandline/race/<id>.json}.
 *
 * <pre>
 * {
 *   "display_name": "Giant",
 *   "description": "...",
 *   "attributes": [ { "attribute": "minecraft:generic.scale", "amount": 1.0 },
 *                   { "attribute": "minecraft:generic.movement_speed", "amount": 0.1, "operation": "add_multiplied_base" } ],
 *   "traits": [ "aquatic" ],                    // code-driven passives, see RaceTrait
 *   "abilities": [ "grandline:giant_stomp" ],   // granted to every member of the race
 *   "mastery_multiplier": 1.0                   // how fast the race trains fruit, Haki and style mastery
 * }
 * </pre>
 */
public record RaceDefinition(Component displayName, Component description, List<Bonus> attributes, List<RaceTrait> traits,
        List<ResourceLocation> abilities, float masteryMultiplier) {

    /** A permanent attribute change every member of the race has. */
    public record Bonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        public static final Codec<Bonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(Bonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Bonus::amount),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                        .forGetter(Bonus::operation)
        ).apply(i, Bonus::new));
    }

    public static final Codec<RaceDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(RaceDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(RaceDefinition::description),
            Bonus.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(RaceDefinition::attributes),
            RaceTrait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(RaceDefinition::traits),
            ResourceLocation.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(RaceDefinition::abilities),
            Codec.FLOAT.optionalFieldOf("mastery_multiplier", 1f).forGetter(RaceDefinition::masteryMultiplier)
    ).apply(i, RaceDefinition::new));

    public boolean has(RaceTrait trait) {
        return traits.contains(trait);
    }
}
