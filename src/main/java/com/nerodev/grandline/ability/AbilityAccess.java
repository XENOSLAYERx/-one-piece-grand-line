package com.nerodev.grandline.ability;

import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.UnlockCategory;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Decides which abilities a player may use, and whether something currently blocks them.
 * <p>
 * An ability is usable if it was explicitly unlocked (creator unlock, training rewards) or a {@link Grant}
 * hands it out (the eaten Devil Fruit, Haki level, fighting style...). Mastery requirements only apply to granted
 * abilities; a creator unlock bypasses them on purpose.
 * <p>
 * {@link Rule}s can block use at the moment of activation: seawater and Seastone suppress fruit powers,
 * incompatible transformations block each other, and so on.
 */
public final class AbilityAccess {
    /** Hands out abilities based on player state. */
    @FunctionalInterface
    public interface Grant {
        /** Abilities this source currently grants the player, ignoring mastery. */
        Stream<ResourceLocation> granted(Player player);
    }

    /** Can veto an activation. */
    @FunctionalInterface
    public interface Rule {
        Optional<Component> blocks(ServerPlayer player, Ability ability);
    }

    private static final List<Grant> GRANTS = new CopyOnWriteArrayList<>();
    private static final List<Rule> RULES = new CopyOnWriteArrayList<>();

    private AbilityAccess() {}

    public static void addGrant(Grant grant) {
        GRANTS.add(grant);
    }

    public static void addRule(Rule rule) {
        RULES.add(rule);
    }

    public static boolean isUnlocked(Player player, ResourceLocation id) {
        return GLPlayer.data(player).isUnlocked(UnlockCategory.ABILITY, id);
    }

    public static boolean isGranted(Player player, ResourceLocation id) {
        for (Grant grant : GRANTS) {
            if (grant.granted(player).anyMatch(id::equals)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unlocked outright, or granted and the mastery requirement is met. Devil Fruit abilities are special: they
     * always need the fruit (you cannot use a power you do not have); an unlock only waives the mastery requirement.
     */
    public static boolean canUse(Player player, Ability ability) {
        ResourceLocation id = ability.id();
        boolean unlocked = isUnlocked(player, id);
        if (ability.properties().category() == AbilityCategory.FRUIT) {
            return isGranted(player, id) && (unlocked || meetsMastery(GLPlayer.data(player), ability));
        }
        return unlocked || (isGranted(player, id) && meetsMastery(GLPlayer.data(player), ability));
    }

    public static boolean meetsMastery(PlayerData data, Ability ability) {
        Ability.Properties props = ability.properties();
        return props.masteryKey().map(key -> data.mastery(key) >= props.requiredMastery()).orElse(true);
    }

    /** Every ability the player can currently use, in registry order. */
    public static List<Ability> usable(Player player) {
        return GLAbilities.REGISTRY.stream().filter(ability -> canUse(player, ability)).toList();
    }

    public static Optional<Component> blockedBy(ServerPlayer player, Ability ability) {
        for (Rule rule : RULES) {
            Optional<Component> reason = rule.blocks(player, ability);
            if (reason.isPresent()) {
                return reason;
            }
        }
        return Optional.empty();
    }

    /** Damage/size multiplier from mastery: 1.0 at 0 mastery, 1.5 at 100. */
    public static float power(Player player, Ability ability) {
        return ability.properties().masteryKey()
                .map(key -> 1f + (float) (GLPlayer.data(player).mastery(key) / PlayerData.MAX_MASTERY) * 0.5f)
                .orElse(1f);
    }
}
