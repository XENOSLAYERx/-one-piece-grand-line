package com.nerodev.grandline.fruit;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * A Devil Fruit: identity, its abilities, and passive behaviour. One subclass per fruit.
 * <p>
 * Abilities listed here are granted to whoever has eaten the fruit (subject to each ability's mastery
 * requirement). Awakened abilities are added once the fruit awakens at 100 mastery. Passive hooks are called by
 * {@link FruitManager} only for the fruit's current user.
 */
public abstract class DevilFruit {
    private final Properties properties;
    @Nullable
    private String descriptionId;

    protected DevilFruit(Properties properties) {
        this.properties = properties;
    }

    public final ResourceLocation id() {
        return Objects.requireNonNull(GLFruits.REGISTRY.getKey(this), "Devil Fruit is not registered");
    }

    /** Mastery key, e.g. "fruit/grandline:flame". Abilities of this fruit use it in their mastery requirement. */
    public final String masteryKey() {
        return masteryKey(id());
    }

    public static String masteryKey(ResourceLocation fruit) {
        return "fruit/" + fruit;
    }

    public String descriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("fruit", id());
        }
        return descriptionId;
    }

    public Component displayName() {
        return Component.translatable(descriptionId());
    }

    public Component description() {
        return Component.translatable(descriptionId() + ".desc");
    }

    public FruitType type() {
        return properties.type;
    }

    public GLRarity rarity() {
        return properties.rarity;
    }

    public int color() {
        return properties.color;
    }

    /** Romanised Japanese name, e.g. "Mera Mera no Mi". Not translated. */
    public String originalName() {
        return properties.originalName;
    }

    public List<Ability> abilities() {
        return properties.abilities.stream().<Ability>map(Supplier::get).toList();
    }

    public List<Ability> awakenedAbilities() {
        return properties.awakened.stream().<Ability>map(Supplier::get).toList();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Passive hooks (server side, user only)
    // ---------------------------------------------------------------------------------------------------------

    /** The player just ate this fruit (or got it by command). */
    public void onGained(ServerPlayer player) {}

    /** The player lost this fruit (removed, replaced). Undo anything onGained did. */
    public void onLost(ServerPlayer player) {}

    /** The user is about to take damage. Logia intangibility, rubber resistances etc. live here. */
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {}

    /** The user is about to deal damage to {@code target}. */
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {}

    /** The user is landing after a fall. */
    public void onFall(ServerPlayer user, LivingFallEvent event) {}

    /** Called every tick for the user only if {@link #ticks()} is true. Keep it cheap. */
    public void tick(ServerPlayer user, boolean awakened) {}

    public boolean ticks() {
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------

    public static final class Properties {
        FruitType type = FruitType.PARAMECIA;
        GLRarity rarity = GLRarity.RARE;
        int color = 0xFFFFFF;
        String originalName = "";
        final List<Supplier<? extends Ability>> abilities = new ArrayList<>();
        final List<Supplier<? extends Ability>> awakened = new ArrayList<>();

        public static Properties of(FruitType type, GLRarity rarity, String originalName, int color) {
            Properties p = new Properties();
            p.type = type;
            p.rarity = rarity;
            p.originalName = originalName;
            p.color = color;
            return p;
        }

        @SafeVarargs
        public final Properties abilities(Supplier<? extends Ability>... abilities) {
            this.abilities.addAll(List.of(abilities));
            return this;
        }

        @SafeVarargs
        public final Properties awakened(Supplier<? extends Ability>... abilities) {
            this.awakened.addAll(List.of(abilities));
            return this;
        }
    }
}
