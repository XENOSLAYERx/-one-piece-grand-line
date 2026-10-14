package com.nerodev.grandline.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * A quest, loaded from {@code data/<namespace>/grandline/quest/<id>.json}. Main quests make up the story chain and
 * start on their own once the quests before them are done; side quests are offered by an islander ({@code giver}) to
 * those who have done what they {@code require} and stand well enough with the factions named in {@code standing}.
 */
public record QuestDefinition(Component displayName, Component description, Category category, int order, Optional<ResourceLocation> giver,
        Optional<String> island, List<ResourceLocation> requires, Map<ResourceLocation, Integer> standing, List<QuestObjective> objectives,
        Rewards rewards, Lines lines) {
    public enum Category implements StringRepresentable {
        MAIN, SIDE;

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(QuestDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(QuestDefinition::description),
            Category.CODEC.optionalFieldOf("category", Category.SIDE).forGetter(QuestDefinition::category),
            Codec.INT.optionalFieldOf("order", 0).forGetter(QuestDefinition::order),
            ResourceLocation.CODEC.optionalFieldOf("giver").forGetter(QuestDefinition::giver),
            Codec.STRING.optionalFieldOf("island").forGetter(QuestDefinition::island),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(QuestDefinition::requires),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("standing", Map.of()).forGetter(QuestDefinition::standing),
            QuestObjective.CODEC.listOf().fieldOf("objectives").forGetter(QuestDefinition::objectives),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(QuestDefinition::rewards),
            Lines.CODEC.optionalFieldOf("lines", Lines.NONE).forGetter(QuestDefinition::lines)
    ).apply(i, QuestDefinition::new));

    /** Main quests with no giver begin by themselves when the quests they require are done. */
    public boolean startsByItself() {
        return giver.isEmpty();
    }

    /** What finishing the quest is worth. */
    public record Rewards(List<ItemStack> items, long bounty, long renown, Map<ResourceLocation, Integer> reputation,
            Optional<ResourceLocation> title) {
        public static final Rewards NONE = new Rewards(List.of(), 0, 0, Map.of(), Optional.empty());
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Rewards::items),
                Codec.LONG.optionalFieldOf("bounty", 0L).forGetter(Rewards::bounty),
                Codec.LONG.optionalFieldOf("renown", 0L).forGetter(Rewards::renown),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("reputation", Map.of()).forGetter(Rewards::reputation),
                ResourceLocation.CODEC.optionalFieldOf("title").forGetter(Rewards::title)
        ).apply(i, Rewards::new));
    }

    /** What the giver says when offering the quest, and when it is done. */
    public record Lines(String offer, String done) {
        public static final Lines NONE = new Lines("", "");
        public static final Codec<Lines> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("offer", "").forGetter(Lines::offer),
                Codec.STRING.optionalFieldOf("done", "").forGetter(Lines::done)
        ).apply(i, Lines::new));
    }
}
