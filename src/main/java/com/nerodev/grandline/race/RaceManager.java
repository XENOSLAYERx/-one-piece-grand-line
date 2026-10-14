package com.nerodev.grandline.race;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.definition.RaceDefinition;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Makes a player's race matter: the race's attribute bonuses (a Giant's size, a Longarm's reach...), its abilities,
 * its passives ({@link RaceTrait}) and how fast it trains. Everything is re-applied whenever the race changes, on
 * login, respawn and dimension change.
 */
public final class RaceManager {
    /** The modifier id of every race attribute bonus (at most one per attribute). */
    public static final ResourceLocation MODIFIER = GrandLine.id("race");

    private RaceManager() {}

    public static void register() {
        AbilityAccess.addGrant(RaceManager::granted);
        AbilityManager.addUseListener(RaceManager::onAbilityUsed);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> refresh(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> refresh(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> refresh(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, RaceManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingIncomingDamageEvent.class, RaceManager::onIncomingDamage);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------------------------------------------

    /** The player's race definition, or null if the race id is not (or no longer) defined. */
    @Nullable
    public static RaceDefinition definition(Player player) {
        ResourceLocation id = GLPlayer.data(player).race();
        return player.level().registryAccess().registry(GLRegistries.RACE).map(registry -> registry.get(id)).orElse(null);
    }

    public static boolean has(Player player, RaceTrait trait) {
        RaceDefinition race = definition(player);
        return race != null && race.has(trait);
    }

    /** How fast this player's race trains fruit, Haki, style and race mastery. */
    public static float masteryMultiplier(Player player) {
        RaceDefinition race = definition(player);
        return race == null ? 1f : race.masteryMultiplier();
    }

    /** Mastery key of the player's race abilities, e.g. "race/grandline:mink". */
    public static String masteryKey(ResourceLocation race) {
        return "race/" + race;
    }

    private static Stream<ResourceLocation> granted(Player player) {
        RaceDefinition race = definition(player);
        return race == null ? Stream.empty() : race.abilities().stream();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Changing race
    // ---------------------------------------------------------------------------------------------------------

    /** Counter set once a player has chosen their race for themselves. */
    public static final String CHOSEN = "race/chosen";

    /** Whether {@code player} may still choose their race themselves (once, unless a creator resets it). */
    public static boolean canChoose(Player player) {
        return GLPlayer.data(player).counter(CHOSEN) == 0;
    }

    /**
     * {@code player} chooses to be {@code race}: allowed once, so a body is not swapped at will in survival play.
     *
     * @return why not, or empty when it is done
     */
    public static Optional<Component> choose(ServerPlayer player, ResourceLocation race) {
        if (!canChoose(player)) {
            return Optional.of(Component.literal("You have already chosen your race."));
        }
        if (player.level().registryAccess().registry(GLRegistries.RACE).map(r -> r.get(race)).isEmpty()) {
            return Optional.of(Component.literal("There is no such race."));
        }
        setRace(player, race);
        GLPlayer.edit(player, data -> data.increment(CHOSEN));
        return Optional.empty();
    }

    /**
     * Makes {@code race} the player's race at once: old race bonuses and running race abilities end, the new bonuses
     * apply and its abilities go into free slots.
     */
    public static void setRace(ServerPlayer player, ResourceLocation race) {
        AbilityState state = AbilityState.of(player);
        for (ResourceLocation id : List.copyOf(state.activeAbilities())) {
            Ability ability = GLAbilities.REGISTRY.get(id);
            if (ability != null && ability.properties().category() == AbilityCategory.RACE) {
                AbilityManager.end(player, ability, Ability.EndReason.INTERRUPTED);
            }
        }
        GLPlayer.edit(player, data -> data.setRace(race));
        refresh(player);
        equipUsable(player);
    }

    /** Puts every race ability the player can use into a free slot. */
    public static void equipUsable(ServerPlayer player) {
        RaceDefinition definition = definition(player);
        if (definition != null) {
            AbilityState state = AbilityState.of(player);
            for (ResourceLocation ability : definition.abilities()) {
                Ability move = GLAbilities.REGISTRY.get(ability);
                if (move != null && AbilityAccess.canUse(player, move)) {
                    state.equipInFreeSlot(ability);
                }
            }
        }
        AbilityManager.sync(player);
    }

    /** Re-applies the player's race: attribute bonuses and what others see. */
    public static void refresh(Player entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        RaceDefinition race = definition(player);
        for (Holder<Attribute> attribute : BuiltInRegistries.ATTRIBUTE.asHolderIdMap()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MODIFIER);
            }
        }
        if (race != null) {
            for (RaceDefinition.Bonus bonus : race.attributes()) {
                AttributeInstance instance = player.getAttribute(bonus.attribute());
                if (instance != null) {
                    instance.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, bonus.amount(), bonus.operation()));
                }
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        FruitManager.refreshPublicState(player);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Race mastery
    // ---------------------------------------------------------------------------------------------------------

    private static void onAbilityUsed(ServerPlayer player, Ability ability) {
        if (ability.properties().category() != AbilityCategory.RACE) {
            return;
        }
        String key = masteryKey(GLPlayer.data(player).race());
        double gain = ability.properties().masteryGain() * GLServerConfig.masteryGainMultiplier() * masteryMultiplier(player);
        PlayerData data = GLPlayer.data(player);
        double before = data.mastery(key);
        if (gain <= 0 || before >= PlayerData.MAX_MASTERY) {
            return;
        }
        double after = Math.min(PlayerData.MAX_MASTERY, before + gain);
        GLPlayer.edit(player, d -> d.setMastery(key, after));
        RaceDefinition race = definition(player);
        if (race == null) {
            return;
        }
        for (ResourceLocation id : race.abilities()) {
            Ability move = GLAbilities.REGISTRY.get(id);
            if (move == null) {
                continue;
            }
            double needed = move.properties().requiredMastery();
            if (move.properties().masteryKey().filter(key::equals).isPresent() && before < needed && after >= needed) {
                player.displayClientMessage(Component.literal("New technique: ").append(move.displayName()), false);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Traits
    // ---------------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        if (has(player, RaceTrait.AQUATIC)) {
            aquatic(player);
        }
    }

    /** A Fish-Man in the water breathes it, sees clearly and swims like a fish. */
    public static void aquatic(ServerPlayer player) {
        if (!player.isInWater()) {
            return;
        }
        player.setAirSupply(player.getMaxAirSupply());
        if (player.isEyeInFluid(FluidTags.WATER)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        }
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 30, 0, true, false, false));
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        // Minks shrug off lightning: their own bodies carry Electro.
        if (target instanceof ServerPlayer victim && source.is(AbilityDamage.SHOCK) && has(victim, RaceTrait.ELECTRO)) {
            event.setAmount(event.getAmount() * 0.5f);
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == target) {
            return;
        }
        // A Fish-Man fights harder in the water.
        if (attacker.isInWater() && source.is(AbilityDamage.PHYSICAL) && has(attacker, RaceTrait.AQUATIC)) {
            event.setAmount(event.getAmount() * 1.2f);
        }
        // A Mink's bare-handed blow carries Electro (more in Sulong).
        if (source.getDirectEntity() == attacker && source.is(DamageTypes.PLAYER_ATTACK) && attacker.getMainHandItem().isEmpty()
                && CombatManager.isSwing(attacker, target) && has(attacker, RaceTrait.ELECTRO)) {
            boolean sulong = RaceAbilities.inSulong(attacker);
            event.setAmount(event.getAmount() + (sulong ? 3f : 1.5f));
            Vfx.impact(attacker.serverLevel(), attacker, target.getBoundingBox().getCenter(), RaceAbilities.ELECTRO, sulong ? 1.2f : 0.8f,
                    Kind.SPARK);
        }
    }
}
