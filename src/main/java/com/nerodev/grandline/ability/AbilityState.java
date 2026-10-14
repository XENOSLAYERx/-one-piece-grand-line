package com.nerodev.grandline.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nerodev.grandline.data.GLAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player ability state: equipped slots, cooldowns, stamina, active abilities and creator test flags.
 * <p>
 * Stamina and cooldowns are stored as game-time timestamps, so nothing needs to tick to regenerate them and the
 * client can compute the same values locally from the last sync. A sync is only sent when something changes
 * (ability used, slot changed, flag toggled), never every tick.
 */
public final class AbilityState {
    public static final int SLOT_COUNT = 5;
    public static final float BASE_MAX_STAMINA = 100f;
    /** Ticks after spending before stamina starts to regenerate. */
    public static final int REGEN_DELAY = 20;
    /** Stamina regenerated per tick once regeneration has started (10 per second). */
    public static final float REGEN_PER_TICK = 0.5f;

    /** Saved form. Active abilities are not saved: they are always stopped on logout. */
    public static final Codec<AbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(AbilityState::slotsForCodec),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                    .forGetter(s -> s.cooldownUntil),
            Codec.FLOAT.optionalFieldOf("stamina", BASE_MAX_STAMINA).forGetter(s -> s.staminaBase),
            Codec.LONG.optionalFieldOf("stamina_tick", 0L).forGetter(s -> s.staminaTick),
            TestFlag.CODEC.listOf().optionalFieldOf("flags", List.of()).forGetter(s -> List.copyOf(s.flags))
    ).apply(i, AbilityState::new));

    /** Network form for the owning client: the saved fields plus active abilities. */
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityState> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                ByteBufCodecs.fromCodecWithRegistries(CODEC).encode(buf, state);
                buf.writeFloat(state.maxStamina);
                buf.writeVarInt(state.active.size());
                state.active.forEach((id, start) -> {
                    buf.writeResourceLocation(id);
                    buf.writeVarLong(start);
                });
            },
            buf -> {
                AbilityState state = ByteBufCodecs.fromCodecWithRegistries(CODEC).decode(buf);
                state.maxStamina = buf.readFloat();
                int count = buf.readVarInt();
                for (int i = 0; i < count; i++) {
                    state.active.put(buf.readResourceLocation(), buf.readVarLong());
                }
                return state;
            });

    private final ResourceLocation[] slots = new ResourceLocation[SLOT_COUNT];
    private final Map<ResourceLocation, Long> cooldownUntil = new HashMap<>();
    private float staminaBase;
    private long staminaTick;
    private float maxStamina = BASE_MAX_STAMINA;
    private final EnumSet<TestFlag> flags = EnumSet.noneOf(TestFlag.class);

    // Not saved
    private final Map<ResourceLocation, Long> active = new LinkedHashMap<>();
    private long evadeUntil = Long.MIN_VALUE;
    private int airJumps;

    public AbilityState() {
        this(List.of(), Map.of(), BASE_MAX_STAMINA, 0L, List.of());
    }

    private AbilityState(List<String> slots, Map<ResourceLocation, Long> cooldowns, float stamina, long staminaTick,
            List<TestFlag> flags) {
        for (int i = 0; i < Math.min(SLOT_COUNT, slots.size()); i++) {
            this.slots[i] = slots.get(i).isEmpty() ? null : ResourceLocation.tryParse(slots.get(i));
        }
        this.cooldownUntil.putAll(cooldowns);
        this.staminaBase = stamina;
        this.staminaTick = staminaTick;
        this.flags.addAll(flags);
    }

    public static AbilityState of(Player player) {
        return player.getData(GLAttachments.ABILITY_STATE);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Slots
    // ---------------------------------------------------------------------------------------------------------

    @Nullable
    public ResourceLocation slot(int index) {
        return index >= 0 && index < SLOT_COUNT ? slots[index] : null;
    }

    public void setSlot(int index, @Nullable ResourceLocation ability) {
        if (index < 0 || index >= SLOT_COUNT) {
            return;
        }
        // An ability lives in at most one slot.
        if (ability != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (ability.equals(slots[i])) {
                    slots[i] = null;
                }
            }
        }
        slots[index] = ability;
    }

    /** Puts {@code ability} in the first empty slot. @return the slot index, or -1 if full or already equipped */
    public int equipInFreeSlot(ResourceLocation ability) {
        for (ResourceLocation equipped : slots) {
            if (ability.equals(equipped)) {
                return -1;
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] == null) {
                slots[i] = ability;
                return i;
            }
        }
        return -1;
    }

    public void clearSlots() {
        java.util.Arrays.fill(slots, null);
    }

    public List<ResourceLocation> equipped() {
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation slot : slots) {
            if (slot != null) {
                out.add(slot);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Cooldowns
    // ---------------------------------------------------------------------------------------------------------

    public long cooldownUntil(ResourceLocation ability) {
        return cooldownUntil.getOrDefault(ability, 0L);
    }

    public boolean isOnCooldown(ResourceLocation ability, long now) {
        return !flags.contains(TestFlag.NO_COOLDOWNS) && cooldownUntil(ability) > now;
    }

    /** Remaining cooldown in ticks (0 if ready). */
    public long cooldownRemaining(ResourceLocation ability, long now) {
        return isOnCooldown(ability, now) ? cooldownUntil(ability) - now : 0;
    }

    public void startCooldown(ResourceLocation ability, long now, int ticks) {
        if (ticks > 0) {
            cooldownUntil.put(ability, now + ticks);
        }
    }

    public void clearCooldowns() {
        cooldownUntil.clear();
    }

    /** Drops expired entries so saved data does not grow forever. */
    public void pruneCooldowns(long now) {
        cooldownUntil.values().removeIf(until -> until <= now);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Stamina
    // ---------------------------------------------------------------------------------------------------------

    public float maxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(float max) {
        this.maxStamina = Math.max(1f, max);
    }

    public float stamina(long now) {
        if (flags.contains(TestFlag.INFINITE_STAMINA)) {
            return maxStamina;
        }
        long regenTicks = now - staminaTick - REGEN_DELAY;
        float regenerated = regenTicks > 0 ? regenTicks * REGEN_PER_TICK : 0f;
        return Math.min(maxStamina, Math.max(0f, staminaBase + regenerated));
    }

    public boolean hasStamina(float amount, long now) {
        return amount <= 0 || stamina(now) >= amount;
    }

    /** @return false (and spends nothing) if there is not enough stamina */
    public boolean spendStamina(float amount, long now) {
        if (amount <= 0 || flags.contains(TestFlag.INFINITE_STAMINA)) {
            return true;
        }
        float current = stamina(now);
        if (current < amount) {
            return false;
        }
        staminaBase = current - amount;
        staminaTick = now;
        return true;
    }

    /** Gives back {@code amount} stamina, up to the maximum. */
    public void restoreStamina(float amount, long now) {
        staminaBase = Math.min(maxStamina, stamina(now) + amount);
        staminaTick = now - REGEN_DELAY;
    }

    public void refillStamina(long now) {
        staminaBase = maxStamina;
        staminaTick = now - REGEN_DELAY;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Active abilities
    // ---------------------------------------------------------------------------------------------------------

    public boolean isActive(ResourceLocation ability) {
        return active.containsKey(ability);
    }

    public Optional<Long> activeSince(ResourceLocation ability) {
        return Optional.ofNullable(active.get(ability));
    }

    public Set<ResourceLocation> activeAbilities() {
        return Collections.unmodifiableSet(active.keySet());
    }

    public boolean hasActive() {
        return !active.isEmpty();
    }

    void markActive(ResourceLocation ability, long now) {
        active.put(ability, now);
    }

    void markInactive(ResourceLocation ability) {
        active.remove(ability);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Short-lived combat flags (not saved, not synced)
    // ---------------------------------------------------------------------------------------------------------

    public boolean isEvading(long now) {
        return now < evadeUntil;
    }

    public void evadeFor(long now, int ticks) {
        evadeUntil = Math.max(evadeUntil, now + ticks);
    }

    public int airJumps() {
        return airJumps;
    }

    public void setAirJumps(int airJumps) {
        this.airJumps = airJumps;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Test flags
    // ---------------------------------------------------------------------------------------------------------

    public boolean hasFlag(TestFlag flag) {
        return flags.contains(flag);
    }

    public void setFlag(TestFlag flag, boolean on) {
        if (on) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    public Set<TestFlag> flags() {
        return Collections.unmodifiableSet(flags);
    }

    private List<String> slotsForCodec() {
        List<String> out = new ArrayList<>(SLOT_COUNT);
        for (ResourceLocation slot : slots) {
            out.add(slot == null ? "" : slot.toString());
        }
        return out;
    }
}
