package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * JSON-defined race. Loaded from {@code data/<namespace>/grandline/race/<id>.json}.
 * <p>
 * Milestone 1 holds identity data only. Milestone 8 adds the race mechanics (swimming, electro, size, reach...).
 */
public record RaceDefinition(Component displayName, Component description) {
    public static final Codec<RaceDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(RaceDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(RaceDefinition::description)
    ).apply(i, RaceDefinition::new));
}
