package com.nerodev.grandline.fruit;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.DeathMode;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Everything about having a Devil Fruit: eating (one fruit per body), losing it, mastery gain, awakening,
 * granting abilities, passive hooks, and the sea's weakness.
 */
public final class FruitManager {
    /** Mastery needed for the fruit to awaken. */
    public static final double AWAKENING_MASTERY = PlayerData.MAX_MASTERY;

    public enum EatResult {
        GAINED,
        REPLACED,
        KILLED,
        REFUSED
    }

    private FruitManager() {}

    public static void register() {
        // Fruit abilities are granted to whoever ate the fruit.
        AbilityAccess.addGrant(FruitManager::grantedAbilities);
        AbilityManager.addUseListener(FruitManager::onAbilityUsed);
        // The sea (any water) cancels Devil Fruit powers.
        AbilityAccess.addRule((player, ability) -> ability.properties().category() == AbilityCategory.FRUIT
                && isWeakenedByWater(player)
                ? Optional.of(Component.literal("The water drains your Devil Fruit power"))
                : Optional.empty());

        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, FruitManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingIncomingDamageEvent.class, FruitManager::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LivingFallEvent.class, FruitManager::onFall);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> refreshPublicState(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> refreshPublicState(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> refreshPublicState(e.getEntity()));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------------------------------------------

    @Nullable
    public static DevilFruit fruitOf(Player player) {
        return GLPlayer.data(player).fruit().map(GLFruits.REGISTRY::get).orElse(null);
    }

    public static boolean hasFruit(Player player) {
        return GLPlayer.data(player).fruit().isPresent();
    }

    public static boolean isAwakened(Player player) {
        return GLPlayer.data(player).isAwakened();
    }

    public static boolean isWeakenedByWater(Player player) {
        return hasFruit(player) && player.isInWater();
    }

    private static Stream<ResourceLocation> grantedAbilities(Player player) {
        DevilFruit fruit = fruitOf(player);
        if (fruit == null) {
            return Stream.empty();
        }
        Stream<Ability> abilities = fruit.abilities().stream();
        if (isAwakened(player)) {
            abilities = Stream.concat(abilities, fruit.awakenedAbilities().stream());
        }
        return abilities.map(Ability::id);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Gaining and losing
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Eating a fruit. One fruit per body: a second fruit kills a normal-mode player (if the config says so) and
     * simply replaces the fruit in creator death mode.
     */
    public static EatResult eat(ServerPlayer player, DevilFruit fruit) {
        PlayerData data = GLPlayer.data(player);
        if (data.fruit().isPresent()) {
            if (data.deathMode() == DeathMode.CREATOR || !GLServerConfig.secondFruitKills()) {
                setFruit(player, fruit);
                return EatResult.REPLACED;
            }
            player.sendSystemMessage(Component.literal("Your body tears itself apart. No one can hold two Devil Fruits.")
                    .withStyle(ChatFormatting.DARK_RED));
            player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
            return EatResult.KILLED;
        }
        setFruit(player, fruit);
        player.sendSystemMessage(Component.literal("You ate the ").append(fruit.displayName())
                .append(". Its power is yours, but the sea will never accept you again.").withStyle(ChatFormatting.GOLD));
        return EatResult.GAINED;
    }

    /** Gives the player {@code fruit}, replacing any current fruit without killing. Used by eating and commands. */
    public static void setFruit(ServerPlayer player, DevilFruit fruit) {
        DevilFruit old = fruitOf(player);
        if (old != null) {
            clearFruitState(player, old);
        }
        GLPlayer.edit(player, data -> {
            data.setFruit(Optional.of(fruit.id()));
            data.unlock(UnlockCategory.FRUIT, fruit.id());
        });
        fruit.onGained(player);
        AbilityState state = AbilityState.of(player);
        for (Ability ability : fruit.abilities()) {
            if (AbilityAccess.canUse(player, ability)) {
                state.equipInFreeSlot(ability.id());
            }
        }
        AbilityManager.sync(player);
        refreshPublicState(player);
        Fx.play(player.serverLevel(), GLEffects.FRUIT_EATEN, player,
                FxParams.at(player.position()).color(fruit.color()));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    /** Removes the player's fruit. Mastery for it is kept so re-eating later restores progress. */
    public static boolean removeFruit(ServerPlayer player) {
        DevilFruit old = fruitOf(player);
        if (old == null) {
            return false;
        }
        clearFruitState(player, old);
        GLPlayer.edit(player, data -> data.setFruit(Optional.empty()));
        refreshPublicState(player);
        return true;
    }

    private static void clearFruitState(ServerPlayer player, DevilFruit fruit) {
        AbilityManager.endAll(player, Ability.EndReason.INTERRUPTED, false);
        TransformationManager.end(player);
        fruit.onLost(player);
        AbilityState state = AbilityState.of(player);
        Stream.concat(fruit.abilities().stream(), fruit.awakenedAbilities().stream()).forEach(ability -> {
            if (!AbilityAccess.isUnlocked(player, ability.id())) {
                for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
                    if (ability.id().equals(state.slot(i))) {
                        state.setSlot(i, null);
                    }
                }
            }
        });
        AbilityManager.sync(player);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mastery and awakening
    // ---------------------------------------------------------------------------------------------------------

    /** Called after a fruit ability was used successfully. Big moves teach more than small ones. */
    public static void onAbilityUsed(ServerPlayer player, Ability ability) {
        DevilFruit fruit = fruitOf(player);
        if (fruit == null || ability.properties().category() != AbilityCategory.FRUIT) {
            return;
        }
        String key = fruit.masteryKey();
        double gain = ability.properties().masteryGain() * GLServerConfig.masteryGainMultiplier()
                * RaceManager.masteryMultiplier(player);
        if (gain <= 0) {
            return;
        }
        PlayerData data = GLPlayer.data(player);
        double before = data.mastery(key);
        if (before >= PlayerData.MAX_MASTERY) {
            return;
        }
        double after = Math.min(PlayerData.MAX_MASTERY, before + gain);
        GLPlayer.edit(player, d -> d.setMastery(key, after));
        announceUnlocks(player, fruit, before, after);
        if (after >= AWAKENING_MASTERY && !data.isAwakened()) {
            awaken(player);
        }
    }

    /** Sets mastery directly (commands). Awakens at 100. */
    public static void setMastery(ServerPlayer player, double value) {
        DevilFruit fruit = fruitOf(player);
        if (fruit == null) {
            return;
        }
        double before = GLPlayer.data(player).mastery(fruit.masteryKey());
        GLPlayer.edit(player, d -> d.setMastery(fruit.masteryKey(), value));
        announceUnlocks(player, fruit, before, value);
        if (value >= AWAKENING_MASTERY && !isAwakened(player)) {
            awaken(player);
        }
        equipNewlyUsable(player, fruit);
    }

    /** Awakens the player's fruit: awakened abilities become available and the passive changes. */
    public static boolean awaken(ServerPlayer player) {
        DevilFruit fruit = fruitOf(player);
        if (fruit == null || isAwakened(player)) {
            return false;
        }
        GLPlayer.edit(player, d -> {
            d.setAwakened(true);
            d.unlock(UnlockCategory.AWAKENING, fruit.id());
        });
        refreshPublicState(player);
        equipNewlyUsable(player, fruit);
        player.sendSystemMessage(Component.literal("Your ").append(fruit.displayName())
                .append(" has AWAKENED. Its power now reaches beyond your body.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        Fx.play(player.serverLevel(), GLEffects.AWAKENING, player, FxParams.at(player.position()).color(fruit.color()).scale(2f));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7f, 1.4f);
        return true;
    }

    private static void announceUnlocks(ServerPlayer player, DevilFruit fruit, double before, double after) {
        for (Ability ability : fruit.abilities()) {
            double required = ability.properties().requiredMastery();
            if (required > 0 && before < required && after >= required) {
                player.sendSystemMessage(Component.literal("New technique mastered: ").append(ability.displayName())
                        .withStyle(ChatFormatting.AQUA));
                AbilityState.of(player).equipInFreeSlot(ability.id());
                AbilityManager.sync(player);
            }
        }
    }

    private static void equipNewlyUsable(ServerPlayer player, DevilFruit fruit) {
        AbilityState state = AbilityState.of(player);
        Stream.concat(fruit.abilities().stream(), fruit.awakenedAbilities().stream())
                .filter(ability -> AbilityAccess.canUse(player, ability))
                .forEach(ability -> state.equipInFreeSlot(ability.id()));
        AbilityManager.sync(player);
    }

    public static void refreshPublicState(Player entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerData data = GLPlayer.data(player);
            PublicState.of(player).setFruit(data.fruit(), data.isAwakened());
            PublicState.of(player).setRace(data.race());
            TransformationManager.sync(player);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Events
    // ---------------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DevilFruit fruit = fruitOf(player);
        if (fruit == null) {
            return;
        }
        if (player.isInWater()) {
            applySeaWeakness(player);
            return;
        }
        if (fruit.ticks()) {
            fruit.tick(player, isAwakened(player));
        }
    }

    /** Devil Fruit users sink and lose their strength in water. Toggles and transformations end. */
    private static void applySeaWeakness(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > -0.12) {
            player.setDeltaMovement(motion.x * 0.8, Math.min(motion.y, 0) - 0.05, motion.z * 0.8);
            player.hurtMarked = true;
        }
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, true, false, true));
            if (AbilityState.of(player).hasActive()) {
                AbilityManager.endAll(player, Ability.EndReason.INTERRUPTED, true);
            }
            TransformationManager.end(player);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            DevilFruit fruit = fruitOf(victim);
            if (fruit != null && !victim.isInWater()) {
                fruit.onIncomingDamage(victim, event);
                if (event.isCanceled()) {
                    return;
                }
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DevilFruit fruit = fruitOf(attacker);
            if (fruit != null) {
                fruit.onDealDamage(attacker, event.getEntity(), event);
            }
        }
    }

    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DevilFruit fruit = fruitOf(player);
            if (fruit != null && !player.isInWater()) {
                fruit.onFall(player, event);
            }
        }
    }
}
