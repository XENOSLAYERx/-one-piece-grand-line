package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;

/**
 * JSON-defined faction. Loaded from {@code data/<namespace>/grandline/faction/<id>.json}.
 * <p>
 * Milestone 1 holds identity data only. Milestone 12 adds reputation, NPC reactions and safe zones.
 */
public record FactionDefinition(Component displayName, Component description, TextColor color) {
    public static final Codec<FactionDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(FactionDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(FactionDefinition::description),
            TextColor.CODEC.optionalFieldOf("color", TextColor.fromRgb(0xFFFFFF)).forGetter(FactionDefinition::color)
    ).apply(i, FactionDefinition::new));
}
