package com.nerodev.grandline.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nerodev.grandline.config.GLServerConfig;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * Loot condition {@code grandline:fruit_chance}: passes with the given chance scaled by the server's
 * {@code fruitFindMultiplier}, so a server can make Devil Fruits rarer or commoner without editing loot tables.
 *
 * @param chance the chance at a multiplier of 1
 */
public record FruitChance(float chance) implements LootItemCondition {
    public static final MapCodec<FruitChance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(FruitChance::chance)
    ).apply(i, FruitChance::new));

    /** The chance in effect now. */
    public double scaled() {
        return Math.min(1.0, chance * GLServerConfig.fruitFindMultiplier());
    }

    @Override
    public LootItemConditionType getType() {
        return GLWorldgen.FRUIT_CHANCE.get();
    }

    @Override
    public boolean test(LootContext context) {
        return context.getRandom().nextFloat() < scaled();
    }
}
