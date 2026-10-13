package com.nerodev.grandline.ability;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Base class for every active ability: Devil Fruit moves, Haki, fighting style specials, Rokushiki.
 * <p>
 * All game logic runs on the server. The client only reports key presses; {@link AbilityManager} validates
 * unlocks, cooldowns, stamina and compatibility before calling any hook here. Visuals are triggered from the
 * server hooks with {@link AbilityContext#fx} and {@link AbilityContext#animate}, which broadcast to nearby clients.
 * <p>
 * Lifecycle by {@link ActivationType}:
 * <ul>
 *   <li>INSTANT: {@link #onActivate}</li>
 *   <li>HOLD / TOGGLE: {@link #onActivate}, {@link #onTick} each tick, {@link #onEnd}</li>
 *   <li>CHARGE: {@link #onActivate}, {@link #onTick} while charging, {@link #onRelease} fires, {@link #onEnd}</li>
 * </ul>
 */
public abstract class Ability {
    private final Properties properties;
    @Nullable
    private String descriptionId;

    protected Ability(Properties properties) {
        this.properties = properties;
    }

    public final Properties properties() {
        return properties;
    }

    public final ResourceLocation id() {
        return Objects.requireNonNull(GLAbilities.REGISTRY.getKey(this), "Ability is not registered");
    }

    public String descriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("ability", id());
        }
        return descriptionId;
    }

    public Component displayName() {
        return Component.translatable(descriptionId());
    }

    public Component description() {
        return Component.translatable(descriptionId() + ".desc");
    }

    // ---------------------------------------------------------------------------------------------------------
    // Server hooks
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Ability-specific conditions (e.g. "must be airborne"). Return a reason to refuse, or empty to allow.
     * Generic checks (unlock, cooldown, stamina, suppression) have already passed.
     */
    public Optional<Component> checkUse(ServerPlayer player) {
        return Optional.empty();
    }

    /** Called once when the ability starts. For INSTANT abilities this is the whole effect. */
    public void onActivate(AbilityContext ctx) {}

    /**
     * Called every tick while a HOLD, TOGGLE or CHARGE ability is active (stamina per tick already paid).
     *
     * @return false to end the ability now
     */
    public boolean onTick(AbilityContext ctx) {
        return true;
    }

    /** HOLD and CHARGE: the key was released (or max time reached). CHARGE abilities fire here. */
    public void onRelease(AbilityContext ctx) {}

    /** Always called when a sustained ability stops, for any reason. Undo temporary changes here. */
    public void onEnd(AbilityContext ctx, EndReason reason) {}

    /**
     * Called while this ability is active and its user is about to take damage. Defensive abilities change or
     * cancel the damage here (e.g. Tekkai).
     */
    public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {}

    public enum EndReason {
        RELEASED,
        TOGGLED_OFF,
        TIMED_OUT,
        OUT_OF_STAMINA,
        CANCELLED,
        INTERRUPTED
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Static tuning for an ability. Built fluently in the ability's constructor. */
    public static final class Properties {
        ActivationType activation = ActivationType.INSTANT;
        AbilityCategory category = AbilityCategory.STYLE;
        AbilityIcon icon = AbilityIcon.STAR;
        int color = 0xFFFFFF;
        int cooldownTicks = 20;
        float staminaCost;
        float staminaPerTick;
        int maxActiveTicks = 20 * 60;
        int minChargeTicks;
        @Nullable
        String masteryKey;
        double requiredMastery;

        public static Properties of(ActivationType activation, AbilityCategory category) {
            Properties properties = new Properties();
            properties.activation = activation;
            properties.category = category;
            return properties;
        }

        public Properties icon(AbilityIcon icon, int color) {
            this.icon = icon;
            this.color = color;
            return this;
        }

        public Properties cooldown(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Properties stamina(float cost) {
            this.staminaCost = cost;
            return this;
        }

        public Properties staminaPerTick(float perTick) {
            this.staminaPerTick = perTick;
            return this;
        }

        public Properties maxActive(int ticks) {
            this.maxActiveTicks = ticks;
            return this;
        }

        public Properties minCharge(int ticks) {
            this.minChargeTicks = ticks;
            return this;
        }

        /** Requires {@code level} mastery (0-100) in {@code key}, e.g. "fruit/grandline:gum_gum". */
        public Properties mastery(String key, double level) {
            this.masteryKey = key;
            this.requiredMastery = level;
            return this;
        }

        public ActivationType activation() {
            return activation;
        }

        public AbilityCategory category() {
            return category;
        }

        public AbilityIcon icon() {
            return icon;
        }

        public int color() {
            return color;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public float staminaCost() {
            return staminaCost;
        }

        public float staminaPerTick() {
            return staminaPerTick;
        }

        public int maxActiveTicks() {
            return maxActiveTicks;
        }

        public int minChargeTicks() {
            return minChargeTicks;
        }

        public Optional<String> masteryKey() {
            return Optional.ofNullable(masteryKey);
        }

        public double requiredMastery() {
            return requiredMastery;
        }
    }
}
