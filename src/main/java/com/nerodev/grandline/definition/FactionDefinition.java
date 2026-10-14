package com.nerodev.grandline.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

/**
 * JSON-defined faction. Loaded from {@code data/<namespace>/grandline/faction/<id>.json}.
 *
 * @param enemies      factions whose members and NPCs this faction's NPCs fight on sight
 * @param wantedBounty if present, this faction's NPCs also attack any player whose bounty is at least this much
 */
public record FactionDefinition(Component displayName, Component description, TextColor color, List<ResourceLocation> enemies,
        Optional<Long> wantedBounty) {
    public static final Codec<FactionDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(FactionDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(FactionDefinition::description),
            TextColor.CODEC.optionalFieldOf("color", TextColor.fromRgb(0xFFFFFF)).forGetter(FactionDefinition::color),
            ResourceLocation.CODEC.listOf().optionalFieldOf("enemies", List.of()).forGetter(FactionDefinition::enemies),
            Codec.LONG.optionalFieldOf("wanted_bounty").forGetter(FactionDefinition::wantedBounty)
    ).apply(i, FactionDefinition::new));

    public boolean isEnemy(ResourceLocation faction) {
        return enemies.contains(faction);
    }
}
