package com.nerodev.grandline.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/**
 * One kind of islander, loaded from {@code data/<namespace>/grandline/npc/<id>.json}: who they are, which faction
 * they serve, how they behave, how tough they are, what they carry, what they say and what they sell.
 *
 * @param skin          texture name under {@code textures/entity/npc/}
 * @param patrolRadius  how far a soldier's beat or a guard's post reaches from home
 * @param wanderRadius  how far civilians and merchants stroll from home
 * @param swimmer       Fish-Men and other natural swimmers move fast in water and never drown
 */
public record NpcDefinition(Component displayName, Optional<ResourceLocation> faction, NpcRole role, String skin, float health,
        float attack, float speed, float armor, Map<EquipmentSlot, Item> equipment, int patrolRadius, int wanderRadius, boolean swimmer,
        Lines lines, List<Trade> trades) {
    public static final Codec<NpcDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(NpcDefinition::displayName),
            ResourceLocation.CODEC.optionalFieldOf("faction").forGetter(NpcDefinition::faction),
            NpcRole.CODEC.fieldOf("role").forGetter(NpcDefinition::role),
            Codec.STRING.fieldOf("skin").forGetter(NpcDefinition::skin),
            Codec.FLOAT.optionalFieldOf("health", 20f).forGetter(NpcDefinition::health),
            Codec.FLOAT.optionalFieldOf("attack", 2f).forGetter(NpcDefinition::attack),
            Codec.FLOAT.optionalFieldOf("speed", 0.3f).forGetter(NpcDefinition::speed),
            Codec.FLOAT.optionalFieldOf("armor", 0f).forGetter(NpcDefinition::armor),
            Codec.unboundedMap(EquipmentSlot.CODEC, BuiltInRegistries.ITEM.byNameCodec()).optionalFieldOf("equipment", Map.of())
                    .forGetter(NpcDefinition::equipment),
            Codec.INT.optionalFieldOf("patrol_radius", 10).forGetter(NpcDefinition::patrolRadius),
            Codec.INT.optionalFieldOf("wander_radius", 8).forGetter(NpcDefinition::wanderRadius),
            Codec.BOOL.optionalFieldOf("swimmer", false).forGetter(NpcDefinition::swimmer),
            Lines.CODEC.optionalFieldOf("lines", Lines.NONE).forGetter(NpcDefinition::lines),
            Trade.CODEC.listOf().optionalFieldOf("trades", List.of()).forGetter(NpcDefinition::trades)
    ).apply(i, NpcDefinition::new));

    /** What an islander says when spoken to, by how they feel about you. */
    public record Lines(List<String> friendly, List<String> neutral, List<String> hostile) {
        public static final Lines NONE = new Lines(List.of(), List.of(), List.of());
        public static final Codec<Lines> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("friendly", List.of()).forGetter(Lines::friendly),
                Codec.STRING.listOf().optionalFieldOf("neutral", List.of()).forGetter(Lines::neutral),
                Codec.STRING.listOf().optionalFieldOf("hostile", List.of()).forGetter(Lines::hostile)
        ).apply(i, Lines::new));

        /** A line for the given reaction, falling back to neutral talk; null if the islander has nothing to say. */
        public String pick(Reaction reaction, RandomSource random) {
            List<String> pool = switch (reaction) {
                case FRIENDLY -> friendly.isEmpty() ? neutral : friendly;
                case HOSTILE -> hostile.isEmpty() ? neutral : hostile;
                case NEUTRAL -> neutral;
            };
            return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
        }
    }

    /**
     * One offer a merchant makes. {@code membersOnly} offers are only shown to members of the merchant's faction.
     */
    public record Trade(Item buy, int buyCount, Optional<Item> buyB, int buyBCount, Item sell, int sellCount, int maxUses,
            boolean membersOnly) {
        public static final Codec<Trade> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("buy").forGetter(Trade::buy),
                Codec.intRange(1, 64).optionalFieldOf("buy_count", 1).forGetter(Trade::buyCount),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("buy_b").forGetter(Trade::buyB),
                Codec.intRange(1, 64).optionalFieldOf("buy_b_count", 1).forGetter(Trade::buyBCount),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("sell").forGetter(Trade::sell),
                Codec.intRange(1, 64).optionalFieldOf("sell_count", 1).forGetter(Trade::sellCount),
                Codec.intRange(1, 1000).optionalFieldOf("max_uses", 12).forGetter(Trade::maxUses),
                Codec.BOOL.optionalFieldOf("members_only", false).forGetter(Trade::membersOnly)
        ).apply(i, Trade::new));
    }
}
