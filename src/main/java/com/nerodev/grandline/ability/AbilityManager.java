package com.nerodev.grandline.ability;

import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLAttachments;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side ability engine. The only place that starts, ticks and stops abilities.
 * <p>
 * Every activation is validated here: the ability must exist, be usable by the player (unlocked or granted with
 * enough mastery), be off cooldown, be affordable, not be blocked by a rule (seawater, Seastone, incompatible
 * transformation) and pass the ability's own check. Clients never decide any of this.
 */
public final class AbilityManager {
    /** While a draining ability is active, resync stamina this often (ticks) so the client bar follows. */
    private static final int ACTIVE_SYNC_INTERVAL = 5;

    public enum Result {
        STARTED,
        STOPPED,
        UNKNOWN_ABILITY,
        NOT_USABLE,
        ON_COOLDOWN,
        NO_STAMINA,
        BLOCKED,
        DEAD
    }

    /** Called after an ability starts successfully (mastery training, statistics...). */
    @FunctionalInterface
    public interface UseListener {
        void onUsed(ServerPlayer player, Ability ability);
    }

    private static final List<UseListener> USE_LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private AbilityManager() {}

    public static void addUseListener(UseListener listener) {
        USE_LISTENERS.add(listener);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, AbilityManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> onJoin(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> onJoin(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                endAll(player, Ability.EndReason.INTERRUPTED, false);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                endAll(player, Ability.EndReason.INTERRUPTED, true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                endAll(player, Ability.EndReason.INTERRUPTED, true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingIncomingDamageEvent.class, AbilityManager::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, AbilityManager::onDamaged);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------------------------------------------------

    /** A slot key went down or up on the client. */
    public static void handleKey(ServerPlayer player, int slot, boolean down) {
        ResourceLocation id = AbilityState.of(player).slot(slot);
        if (id == null) {
            return;
        }
        Ability ability = GLAbilities.REGISTRY.get(id);
        if (ability == null) {
            return;
        }
        if (down) {
            press(player, ability);
        } else {
            release(player, ability);
        }
    }

    /** Key down: start the ability, or turn a running TOGGLE off. Failures are explained on the action bar. */
    public static Result press(ServerPlayer player, Ability ability) {
        AbilityState state = AbilityState.of(player);
        if (ability.properties().activation() == ActivationType.TOGGLE && state.isActive(ability.id())) {
            end(player, ability, Ability.EndReason.TOGGLED_OFF);
            return Result.STOPPED;
        }
        Result result = tryStart(player, ability);
        if (result != Result.STARTED && result != Result.STOPPED) {
            explain(player, ability, result);
        }
        return result;
    }

    /** Key up: fires CHARGE abilities and stops HOLD abilities. */
    public static void release(ServerPlayer player, Ability ability) {
        AbilityState state = AbilityState.of(player);
        ActivationType type = ability.properties().activation();
        if (!state.isActive(ability.id()) || (type != ActivationType.HOLD && type != ActivationType.CHARGE)) {
            return;
        }
        int held = activeTicks(player, state, ability);
        if (type == ActivationType.CHARGE && held < ability.properties().minChargeTicks()) {
            // Let go too early: fizzle without a cooldown.
            stop(player, ability, Ability.EndReason.CANCELLED, false);
            return;
        }
        ability.onRelease(context(player, ability, held));
        end(player, ability, Ability.EndReason.RELEASED);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Core
    // ---------------------------------------------------------------------------------------------------------

    public static Result tryStart(ServerPlayer player, Ability ability) {
        if (GLAbilities.REGISTRY.getKey(ability) == null) {
            return Result.UNKNOWN_ABILITY;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return Result.DEAD;
        }
        if (!AbilityAccess.canUse(player, ability)) {
            return Result.NOT_USABLE;
        }
        AbilityState state = AbilityState.of(player);
        long now = now(player);
        ResourceLocation id = ability.id();
        if (state.isActive(id)) {
            return Result.BLOCKED;
        }
        if (state.isOnCooldown(id, now)) {
            return Result.ON_COOLDOWN;
        }
        Ability.Properties props = ability.properties();
        float cost = staminaCost(player, ability);
        if (!state.hasStamina(cost, now)) {
            return Result.NO_STAMINA;
        }
        Optional<Component> blocked = AbilityAccess.blockedBy(player, ability).or(() -> ability.checkUse(player));
        if (blocked.isPresent()) {
            player.displayClientMessage(blocked.get().copy().withStyle(ChatFormatting.RED), true);
            return Result.BLOCKED;
        }

        state.spendStamina(cost, now);
        if (props.activation() == ActivationType.INSTANT) {
            state.startCooldown(id, now, cooldownTicks(player, ability));
            ability.onActivate(context(player, ability, 0));
        } else {
            state.markActive(id, now);
            ability.onActivate(context(player, ability, 0));
        }
        for (UseListener listener : USE_LISTENERS) {
            listener.onUsed(player, ability);
        }
        sync(player);
        return Result.STARTED;
    }

    /** Stops a sustained ability normally: runs onEnd and starts its cooldown. */
    public static void end(ServerPlayer player, Ability ability, Ability.EndReason reason) {
        stop(player, ability, reason, true);
    }

    private static void stop(ServerPlayer player, Ability ability, Ability.EndReason reason, boolean cooldown) {
        AbilityState state = AbilityState.of(player);
        ResourceLocation id = ability.id();
        if (!state.isActive(id)) {
            return;
        }
        int ticks = activeTicks(player, state, ability);
        state.markInactive(id);
        if (cooldown) {
            state.startCooldown(id, now(player), cooldownTicks(player, ability));
        }
        ability.onEnd(context(player, ability, ticks), reason);
        if (reason != Ability.EndReason.RELEASED) {
            // Charge-up and channel poses must not outlive the ability.
            Fx.stopAnimation(player);
        }
        sync(player);
    }

    /** Stops every running ability (death, logout, dimension change, slot edits). */
    public static void endAll(ServerPlayer player, Ability.EndReason reason, boolean cooldown) {
        AbilityState state = AbilityState.of(player);
        if (!state.hasActive()) {
            return;
        }
        for (ResourceLocation id : new ArrayList<>(state.activeAbilities())) {
            Ability ability = GLAbilities.REGISTRY.get(id);
            if (ability == null) {
                state.markInactive(id);
            } else {
                stop(player, ability, reason, cooldown);
            }
        }
        sync(player);
    }

    /**
     * Puts {@code ability} (or nothing) in a slot. Only the abilities this touches are stopped: the one being
     * replaced, and the incoming one if it is running from another slot (its key would change mid-use). Everything
     * else keeps running, so re-arranging slots never cancels a transformation.
     */
    public static void setSlot(ServerPlayer player, int slot, @Nullable ResourceLocation ability) {
        AbilityState state = AbilityState.of(player);
        ResourceLocation previous = state.slot(slot);
        for (ResourceLocation id : new ResourceLocation[] {previous, ability}) {
            Ability running = id == null ? null : GLAbilities.REGISTRY.get(id);
            if (running != null && state.isActive(id) && !(id.equals(ability) && id.equals(previous))) {
                end(player, running, Ability.EndReason.INTERRUPTED);
            }
        }
        state.setSlot(slot, ability);
        sync(player);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Events
    // ---------------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tickPlayer(player);
        }
    }

    /** Per-tick work for one player: drain and tick active abilities, reset air jumps on landing. */
    public static void tickPlayer(ServerPlayer player) {
        AbilityState state = AbilityState.of(player);
        if (state.airJumps() > 0 && player.onGround()) {
            state.setAirJumps(0);
        }
        if (!state.hasActive()) {
            return;
        }
        long now = now(player);
        boolean draining = false;
        List<ResourceLocation> active = new ArrayList<>(state.activeAbilities());
        for (ResourceLocation id : active) {
            Ability ability = GLAbilities.REGISTRY.get(id);
            if (ability == null) {
                state.markInactive(id);
                continue;
            }
            Ability.Properties props = ability.properties();
            int ticks = activeTicks(player, state, ability);
            if (props.staminaPerTick() > 0 && !freeHaki(state, ability)) {
                draining = true;
                float drain = (float) (props.staminaPerTick() * GLServerConfig.staminaCostMultiplier());
                if (!state.spendStamina(drain, now)) {
                    if (props.activation() == ActivationType.CHARGE) {
                        ability.onRelease(context(player, ability, ticks));
                    }
                    end(player, ability, Ability.EndReason.OUT_OF_STAMINA);
                    continue;
                }
            }
            if (!ability.onTick(context(player, ability, ticks))) {
                end(player, ability, Ability.EndReason.CANCELLED);
                continue;
            }
            if (ticks >= props.maxActiveTicks()) {
                if (props.activation() == ActivationType.HOLD || props.activation() == ActivationType.CHARGE) {
                    // Fully charged / held to the limit: it fires exactly as if the key had been let go.
                    ability.onRelease(context(player, ability, ticks));
                    end(player, ability, Ability.EndReason.RELEASED);
                } else {
                    end(player, ability, Ability.EndReason.TIMED_OUT);
                }
            }
        }
        if (draining && now % ACTIVE_SYNC_INTERVAL == 0) {
            sync(player);
        }
    }

    private static void onJoin(net.minecraft.world.entity.player.Player entity) {
        if (entity instanceof ServerPlayer player) {
            AbilityState state = AbilityState.of(player);
            state.pruneCooldowns(now(player));
            state.setMaxStamina(AbilityState.BASE_MAX_STAMINA);
            sync(player);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        AbilityState state = AbilityState.of(player);
        if (state.hasFlag(TestFlag.INVINCIBLE)) {
            event.setCanceled(true);
            return;
        }
        long now = now(player);
        if (state.isEvading(now) && event.getSource().getEntity() != null) {
            event.setCanceled(true);
            Fx.play(player.serverLevel(), GLEffects.EVADE, player, FxParams.at(player.position()));
            return;
        }
        for (ResourceLocation id : new ArrayList<>(state.activeAbilities())) {
            Ability ability = GLAbilities.REGISTRY.get(id);
            if (ability != null) {
                ability.onOwnerHurt(context(player, ability, activeTicks(player, state, ability)), event);
                if (event.isCanceled()) {
                    return;
                }
            }
        }
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && AbilityState.of(player).hasFlag(TestFlag.INFINITE_HEALTH)) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------

    public static AbilityContext context(ServerPlayer player, Ability ability, int activeTicks) {
        return new AbilityContext(player, ability, activeTicks, AbilityAccess.power(player, ability));
    }

    private static int activeTicks(ServerPlayer player, AbilityState state, Ability ability) {
        return state.activeSince(ability.id()).map(start -> (int) Math.max(0, now(player) - start)).orElse(0);
    }

    /** Cooldown after server balance multipliers. */
    public static int cooldownTicks(ServerPlayer player, Ability ability) {
        if (freeHaki(AbilityState.of(player), ability)) {
            return 0;
        }
        return (int) Math.round(ability.cooldownFor(player) * GLServerConfig.cooldownMultiplier());
    }

    /** Stamina cost after server balance multipliers. */
    public static float staminaCost(ServerPlayer player, Ability ability) {
        if (freeHaki(AbilityState.of(player), ability)) {
            return 0f;
        }
        return (float) (ability.properties().staminaCost() * GLServerConfig.staminaCostMultiplier());
    }

    /** The {@code haki infinite} test flag: Haki costs nothing and has no cooldown. */
    private static boolean freeHaki(AbilityState state, Ability ability) {
        return ability.properties().category() == AbilityCategory.HAKI && state.hasFlag(TestFlag.INFINITE_HAKI);
    }

    public static long now(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }

    public static void sync(ServerPlayer player) {
        if (GLAttachments.canSync(player)) {
            player.syncData(GLAttachments.ABILITY_STATE);
        }
    }

    private static void explain(ServerPlayer player, Ability ability, Result result) {
        AbilityState state = AbilityState.of(player);
        Component message = switch (result) {
            case ON_COOLDOWN -> Component.literal(String.format(Locale.ROOT, "%s is on cooldown (%.1fs)",
                    ability.displayName().getString(), state.cooldownRemaining(ability.id(), now(player)) / 20.0));
            case NO_STAMINA -> Component.literal("Not enough stamina");
            case NOT_USABLE -> Component.literal("You can't use ").append(ability.displayName()).append(" yet");
            default -> null;
        };
        if (message != null) {
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
        }
    }
}
