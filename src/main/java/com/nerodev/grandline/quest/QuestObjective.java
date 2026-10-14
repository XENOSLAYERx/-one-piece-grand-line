package com.nerodev.grandline.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * One thing a quest asks for. {@code target} names what it is about: an islander kind ({@code grandline:villager}), a
 * faction, a boss, an item, or an island ({@code grandline:shells_town}); {@code count} is how many (or, for a
 * bounty, the amount). Collected items are taken when the quest is handed in if {@code consume} is set.
 */
public record QuestObjective(Kind kind, Optional<ResourceLocation> target, int count, boolean consume, Optional<Component> text) {
    public enum Kind implements StringRepresentable {
        /** Defeat islanders of one kind. */
        DEFEAT_NPC,
        /** Defeat fighters of a faction (any kind). */
        DEFEAT_FACTION,
        /** Defeat a boss. */
        DEFEAT_BOSS,
        /** Speak with an islander of one kind. */
        TALK,
        /** Find an island (or, with no target, any {@code count} islands). */
        DISCOVER,
        /** Have items in your bag. */
        COLLECT,
        /** Have a bounty of at least {@code count} berries. */
        BOUNTY,
        /** Go aboard a ship at sea. */
        SAIL;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<QuestObjective> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.fieldOf("type").forGetter(QuestObjective::kind),
            ResourceLocation.CODEC.optionalFieldOf("target").forGetter(QuestObjective::target),
            Codec.INT.optionalFieldOf("count", 1).forGetter(QuestObjective::count),
            Codec.BOOL.optionalFieldOf("consume", false).forGetter(QuestObjective::consume),
            ComponentSerialization.CODEC.optionalFieldOf("text").forGetter(QuestObjective::text)
    ).apply(i, QuestObjective::new));

    /** Whether {@code id} is what this objective is about (an objective with no target takes anything). */
    public boolean matches(ResourceLocation id) {
        return target.map(id::equals).orElse(true);
    }

    /** How far along an objective is shown as done (a bounty counts as one step). */
    public int goal() {
        return kind == Kind.BOUNTY || kind == Kind.SAIL || kind == Kind.DISCOVER && target.isPresent() ? 1 : Math.max(1, count);
    }
}
