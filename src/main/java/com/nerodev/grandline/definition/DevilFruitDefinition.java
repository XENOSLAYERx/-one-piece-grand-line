package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;

/**
 * JSON-defined Devil Fruit. Loaded from {@code data/<namespace>/grandline/devil_fruit/<id>.json}.
 * <p>
 * Milestone 1 holds identity data only. Milestone 4 adds the ability list, passives, mastery and awakening.
 *
 * @param displayName  English name shown in UIs, e.g. "Gum-Gum Fruit"
 * @param originalName romanized Japanese name, e.g. "Gomu Gomu no Mi"
 * @param description  short lore description
 * @param type         fruit class
 * @param rarity       how rare the fruit is in the world
 * @param color        theme colour used for UI accents
 */
public record DevilFruitDefinition(
        Component displayName,
        String originalName,
        Component description,
        FruitType type,
        GLRarity rarity,
        TextColor color) {

    public static final Codec<DevilFruitDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(DevilFruitDefinition::displayName),
            Codec.STRING.fieldOf("original_name").forGetter(DevilFruitDefinition::originalName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(DevilFruitDefinition::description),
            FruitType.CODEC.fieldOf("type").forGetter(DevilFruitDefinition::type),
            GLRarity.CODEC.fieldOf("rarity").forGetter(DevilFruitDefinition::rarity),
            TextColor.CODEC.optionalFieldOf("color", TextColor.fromRgb(0xFFFFFF)).forGetter(DevilFruitDefinition::color)
    ).apply(i, DevilFruitDefinition::new));
}
