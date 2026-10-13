package com.nerodev.grandline.style;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.UnlockCategory;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Wires fighting styles into the rest of the mod: the equipped style grants its moves, decides the combo finisher
 * and shapes bare-handed blows, and using its moves trains style mastery.
 * <p>
 * Everyone knows the Brawler style. Other styles have to be learned (a technique scroll, or a creator unlock) before
 * they can be equipped.
 */
public final class StyleManager {
    private StyleManager() {}

    public static void register() {
        AbilityAccess.addGrant(player -> style(player).moves().stream().map(Ability::id));
        CombatManager.setFinishers(player -> style(player).finisher());
        AbilityManager.addUseListener(StyleManager::onAbilityUsed);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingIncomingDamageEvent.class, StyleManager::onIncomingDamage);
    }

    /** The equipped style, or Brawler if none (or if the equipped one was never learned). */
    public static FightingStyle style(Player player) {
        PlayerData data = GLPlayer.data(player);
        Optional<FightingStyle> equipped = data.style().map(GLStyles.REGISTRY::get);
        return equipped.filter(style -> knows(player, style)).orElse(GLStyles.BRAWLER.get());
    }

    public static boolean knows(Player player, FightingStyle style) {
        return style == GLStyles.BRAWLER.get() || GLPlayer.data(player).isUnlocked(UnlockCategory.STYLE, style.id());
    }

    /** Learns {@code style} (no equip). @return true if it was new */
    public static boolean learn(ServerPlayer player, FightingStyle style) {
        boolean[] learned = {false};
        GLPlayer.edit(player, data -> learned[0] = data.unlock(UnlockCategory.STYLE, style.id()));
        return learned[0];
    }

    /**
     * Equips {@code style} if the player knows it, putting its moves into free ability slots.
     *
     * @return why it could not be equipped, or empty on success
     */
    public static Optional<Component> equip(ServerPlayer player, FightingStyle style) {
        if (!knows(player, style)) {
            return Optional.of(Component.literal("You have not learned ").append(style.displayName()).append(" yet"));
        }
        GLPlayer.edit(player, data -> data.setStyle(style == GLStyles.BRAWLER.get() ? Optional.empty() : Optional.of(style.id())));
        AbilityState state = AbilityState.of(player);
        for (Ability move : style.moves()) {
            if (AbilityAccess.canUse(player, move)) {
                state.equipInFreeSlot(move.id());
            }
        }
        AbilityManager.sync(player);
        return Optional.empty();
    }

    /** Style moves teach their style; mastery unlocks the harder techniques. */
    private static void onAbilityUsed(ServerPlayer player, Ability ability) {
        if (ability.properties().category() != AbilityCategory.STYLE) {
            return;
        }
        FightingStyle style = style(player);
        if (!style.moves().contains(ability)) {
            return;
        }
        String key = style.masteryKey();
        double gain = ability.properties().masteryGain() * GLServerConfig.masteryGainMultiplier();
        PlayerData data = GLPlayer.data(player);
        double before = data.mastery(key);
        if (gain <= 0 || before >= PlayerData.MAX_MASTERY) {
            return;
        }
        double after = Math.min(PlayerData.MAX_MASTERY, before + gain);
        GLPlayer.edit(player, d -> d.setMastery(key, after));
        for (Ability move : style.moves()) {
            double needed = move.properties().requiredMastery();
            if (move.properties().masteryKey().filter(key::equals).isPresent() && before < needed && after >= needed) {
                player.displayClientMessage(Component.literal("New technique: ").append(move.displayName()), false);
            }
        }
    }

    /** Bare-handed blows take on the style's character. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target && source.getDirectEntity() == attacker
                && source.is(DamageTypes.PLAYER_ATTACK) && attacker.getMainHandItem().isEmpty()) {
            style(attacker).onUnarmedHit(attacker, target, event);
        }
    }

    /** Looks a style up by id. */
    public static Optional<FightingStyle> byId(ResourceLocation id) {
        return Optional.ofNullable(GLStyles.REGISTRY.get(id));
    }
}
