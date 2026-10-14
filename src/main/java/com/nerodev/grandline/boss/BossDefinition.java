package com.nerodev.grandline.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;

/**
 * A boss, loaded from {@code data/<namespace>/grandline/boss/<id>.json}: who they are, how they look, how tough they
 * are, the phases of the fight (each with its own moves, pace and words) and what beating them is worth. The moves
 * themselves are code ({@link BossMoves}); the definition picks which ones each phase uses and how often. An
 * {@code aggressive} boss attacks anyone who comes near unless they stand well with its faction; others fight only
 * those their faction is hostile to, or who strike first. A {@code bladeproof} boss (Buggy, with his Chop-Chop body)
 * comes apart around blades: cuts barely touch him unless the blade is clad in Armament Haki. An {@code intangible}
 * boss (Smoker, a Logia) lets every blow pass through his body unless it is backed by Armament Haki or Seastone.
 */
public record BossDefinition(Component displayName, Component title, Body body, String skin, String parts, Optional<Item> weapon, Stats stats,
        BossEvent.BossBarColor barColor, Optional<ResourceLocation> faction, boolean aggressive, boolean bladeproof, boolean intangible,
        List<Phase> phases, Rewards rewards, Lines lines) {
    public static final Codec<BossEvent.BossBarColor> BAR_COLOR = Codec.STRING.xmap(BossEvent.BossBarColor::byName,
            BossEvent.BossBarColor::getName);

    public static final Codec<BossDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("display_name").forGetter(BossDefinition::displayName),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(BossDefinition::title),
            Body.CODEC.optionalFieldOf("body", Body.HUMANOID).forGetter(BossDefinition::body),
            Codec.STRING.fieldOf("skin").forGetter(BossDefinition::skin),
            Codec.STRING.optionalFieldOf("parts", "").forGetter(BossDefinition::parts),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("weapon").forGetter(BossDefinition::weapon),
            Stats.CODEC.fieldOf("stats").forGetter(BossDefinition::stats),
            BAR_COLOR.optionalFieldOf("bar_color", BossEvent.BossBarColor.RED).forGetter(BossDefinition::barColor),
            ResourceLocation.CODEC.optionalFieldOf("faction").forGetter(BossDefinition::faction),
            Codec.BOOL.optionalFieldOf("aggressive", false).forGetter(BossDefinition::aggressive),
            Codec.BOOL.optionalFieldOf("bladeproof", false).forGetter(BossDefinition::bladeproof),
            Codec.BOOL.optionalFieldOf("intangible", false).forGetter(BossDefinition::intangible),
            Phase.CODEC.listOf().fieldOf("phases").forGetter(BossDefinition::phases),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(BossDefinition::rewards),
            Lines.CODEC.optionalFieldOf("lines", Lines.NONE).forGetter(BossDefinition::lines)
    ).apply(i, BossDefinition::new));

    /**
     * What kind of body the boss has: a person (drawn on the player model with their skin and features) or a Sea King
     * (a great sea serpent that swims and never leaves the water willingly; its look is its {@code parts} body).
     */
    public enum Body implements StringRepresentable {
        HUMANOID, SEA_KING;

        public static final Codec<Body> CODEC = StringRepresentable.fromEnum(Body::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The phase a boss is in at {@code healthFraction} (phases are ordered by falling threshold). */
    public int phaseAt(float healthFraction) {
        int phase = 0;
        for (int p = 1; p < phases.size(); p++) {
            if (healthFraction <= phases.get(p).threshold()) {
                phase = p;
            }
        }
        return phase;
    }

    /**
     * Body and strength. {@code scale} sizes the model and hitbox; {@code dodge} is the chance to sidestep a blow,
     * {@code guard} the chance to raise a guard after being hit.
     */
    public record Stats(float health, float attack, float armor, float speed, float scale, float dodge, float guard,
            float knockbackResistance) {
        public static final Codec<Stats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("health").forGetter(Stats::health),
                Codec.FLOAT.fieldOf("attack").forGetter(Stats::attack),
                Codec.FLOAT.optionalFieldOf("armor", 0f).forGetter(Stats::armor),
                Codec.FLOAT.optionalFieldOf("speed", 0.3f).forGetter(Stats::speed),
                Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(Stats::scale),
                Codec.FLOAT.optionalFieldOf("dodge", 0f).forGetter(Stats::dodge),
                Codec.FLOAT.optionalFieldOf("guard", 0f).forGetter(Stats::guard),
                Codec.FLOAT.optionalFieldOf("knockback_resistance", 0.5f).forGetter(Stats::knockbackResistance)
        ).apply(i, Stats::new));
    }

    /**
     * One stage of the fight, entered when health falls to {@code threshold} (the first phase's is ignored). Moves
     * are weighted ({@code "cleave": 3}); {@code pace} speeds up movement and shortens the pause between moves.
     */
    public record Phase(float threshold, Component name, Map<String, Integer> moves, float pace, String line) {
        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("threshold", 1f).forGetter(Phase::threshold),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Phase::name),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("moves").forGetter(Phase::moves),
                Codec.FLOAT.optionalFieldOf("pace", 1f).forGetter(Phase::pace),
                Codec.STRING.optionalFieldOf("line", "").forGetter(Phase::line)
        ).apply(i, Phase::new));
    }

    /** What the one who lands the final blow earns: bounty, crew renown and standing with factions or the townsfolk. */
    public record Rewards(long bounty, long renown, Map<ResourceLocation, Integer> reputation) {
        public static final Rewards NONE = new Rewards(0, 0, Map.of());
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("bounty", 0L).forGetter(Rewards::bounty),
                Codec.LONG.optionalFieldOf("renown", 0L).forGetter(Rewards::renown),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("reputation", Map.of()).forGetter(Rewards::reputation)
        ).apply(i, Rewards::new));
    }

    /** What the boss says when a fight starts and when they fall. */
    public record Lines(String intro, String defeat) {
        public static final Lines NONE = new Lines("", "");
        public static final Codec<Lines> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("intro", "").forGetter(Lines::intro),
                Codec.STRING.optionalFieldOf("defeat", "").forGetter(Lines::defeat)
        ).apply(i, Lines::new));
    }
}
