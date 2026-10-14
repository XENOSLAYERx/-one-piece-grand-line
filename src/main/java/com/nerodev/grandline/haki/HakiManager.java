package com.nerodev.grandline.haki;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.sound.GLSounds;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Everything about having Haki: unlocking and levels, training through combat, the passive effects of active
 * Haki on every blow (harder hits, touching a Logia, emission, Conqueror's infusion), knock-outs and clashes.
 * <p>
 * Levels are mastery values (0-100) per {@link HakiType}. Using Haki raises its level; higher levels widen
 * Observation, strengthen Armament and let Conqueror's floor tougher foes.
 */
public final class HakiManager {
    /** Training counters in {@link PlayerData}. */
    public static final String ARMAMENT_HITS = "haki/armament_hits";
    public static final String OBSERVATION_HITS = "haki/observation_hits";
    public static final String CONQUERORS_ROLLED = "haki/conquerors_rolled";

    /** Ticks an Emission stays primed waiting for a blow. */
    public static final int EMISSION_WINDOW = 100;
    /** Conqueror's level a user needs above a player's strongest Haki to knock that player out. */
    public static final double PLAYER_KNOCKOUT_GAP = 40;

    private static final Map<UUID, Long> EMISSION_PRIMED = new HashMap<>();

    private HakiManager() {}

    public static void register() {
        AbilityAccess.addGrant(HakiManager::granted);
        AbilityManager.addUseListener(HakiManager::onAbilityUsed);
        // Armament and Conqueror's infusion strike a Logia's real body.
        LogiaFruit.TOUCH.add(source -> source.getEntity() instanceof Player attacker
                && (armamentActive(attacker) || infusionActive(attacker)));

        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingIncomingDamageEvent.class, HakiManager::onAttack);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, HakiManager::onDamaged);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> onLogin(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> EMISSION_PRIMED.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> EMISSION_PRIMED.clear());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Queries and scaling
    // ---------------------------------------------------------------------------------------------------------

    public static boolean has(Player player, HakiType type) {
        return GLPlayer.data(player).isUnlocked(UnlockCategory.HAKI, type.id());
    }

    /** Haki level 0-100. */
    public static double level(Player player, HakiType type) {
        return GLPlayer.data(player).mastery(type.masteryKey());
    }

    /** The highest level among the Haki a player has unlocked (their overall willpower). */
    public static double strongestLevel(Player player) {
        double best = 0;
        for (HakiType type : HakiType.values()) {
            if (has(player, type)) {
                best = Math.max(best, level(player, type));
            }
        }
        return best;
    }

    @Nullable
    public static HakiType typeOf(Ability ability) {
        for (HakiType type : HakiType.values()) {
            if (HakiAbilities.of(type).contains(ability)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isActive(Player player, Ability ability) {
        return AbilityState.of(player).isActive(ability.id());
    }

    /** Arms or full body hardened (server side; clients read {@link PublicState} bits). */
    public static boolean armamentActive(Player player) {
        return isActive(player, HakiAbilities.ARMAMENT.get()) || isActive(player, HakiAbilities.FULL_BODY.get());
    }

    public static boolean infusionActive(Player player) {
        return isActive(player, HakiAbilities.INFUSION.get());
    }

    /** Observation range in blocks: 16 at level 0, 40 at 100. */
    public static double senseRadius(Player player) {
        return 16 + level(player, HakiType.OBSERVATION) * 0.24;
    }

    /** Chance to read and dodge an attack while Observation is active: 12% to 40%. */
    public static double dodgeChance(Player player) {
        return 0.12 + level(player, HakiType.OBSERVATION) * 0.0028;
    }

    /** How long Future Sight lasts: 3 s at level 60, up to 7 s. */
    public static int futureSightTicks(Player player) {
        return 60 + (int) (level(player, HakiType.OBSERVATION) * 0.8);
    }

    /** Damage multiplier of an Armament-coated blow: x1.25 to x1.5. */
    public static float armamentMultiplier(Player player) {
        return 1.25f + (float) level(player, HakiType.ARMAMENT) * 0.0025f;
    }

    /** Damage taken is reduced by this fraction under full-body hardening: 30% to 45%. */
    public static float hardeningReduction(Player player) {
        return 0.3f + (float) level(player, HakiType.ARMAMENT) * 0.0015f;
    }

    /** Extra armour-piercing damage of an Emission blow: 6 to 12 (before mastery power). */
    public static float emissionBonus(Player player) {
        return 6f + (float) level(player, HakiType.ARMAMENT) * 0.06f;
    }

    /** Damage multiplier of a Conqueror-infused blow: x1.3 to x1.5. */
    public static float infusionMultiplier(Player player) {
        return 1.3f + (float) level(player, HakiType.CONQUERORS) * 0.002f;
    }

    /** Radius of a Conqueror's wave: 8 blocks for a quick burst at level 0, up to 24. */
    public static double conquerorsRadius(Player player, float charge) {
        return 6 + 10 * charge + level(player, HakiType.CONQUERORS) * 0.08;
    }

    /** Mobs with at most this much max health are knocked out: ~8 to 100 depending on level and charge. */
    public static float knockoutThreshold(Player player, float charge) {
        return (float) ((14 + level(player, HakiType.CONQUERORS) * 0.86) * (0.55 + 0.45 * charge));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Unlocking and levels
    // ---------------------------------------------------------------------------------------------------------

    private static Stream<ResourceLocation> granted(Player player) {
        Stream<ResourceLocation> out = Stream.empty();
        for (HakiType type : HakiType.values()) {
            if (has(player, type)) {
                out = Stream.concat(out, HakiAbilities.of(type).stream().map(Ability::id));
            }
        }
        return out;
    }

    /** Awakens a Haki for the player. @return false if they already had it */
    public static boolean unlock(ServerPlayer player, HakiType type, boolean announce) {
        boolean added = GLPlayer.data(player).unlock(UnlockCategory.HAKI, type.id());
        GLPlayer.sync(player);
        if (!added) {
            return false;
        }
        equipUsable(player, type);
        if (announce) {
            String text = switch (type) {
                case OBSERVATION -> "The voices of everything around you reach you now. Observation Haki has awakened.";
                case ARMAMENT -> "Your will hardens into invisible armour. Armament Haki has awakened.";
                case CONQUERORS -> "You carry the qualities of a king. Conqueror's Haki sleeps within you.";
            };
            player.sendSystemMessage(Component.literal(text).withStyle(type == HakiType.CONQUERORS
                    ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            Fx.play(player.serverLevel(), GLEffects.AWAKENING, player, FxParams.at(player.position()).color(type.color()).scale(1.5f));
            player.serverLevel().playSound(null, player.blockPosition(), GLSounds.HAKI_HARDEN.get(), SoundSource.PLAYERS, 1f, 0.7f);
        }
        return true;
    }

    /** Unlocks a Haki (if needed) and sets its level. */
    public static void setLevel(ServerPlayer player, HakiType type, double value) {
        unlock(player, type, false);
        double before = level(player, type);
        GLPlayer.edit(player, data -> data.setMastery(type.masteryKey(), value));
        announceTechniques(player, type, before, level(player, type));
        equipUsable(player, type);
    }

    /** Removes a Haki. Its level is kept, so unlocking it again restores progress. */
    public static boolean remove(ServerPlayer player, HakiType type) {
        if (!has(player, type)) {
            return false;
        }
        AbilityState state = AbilityState.of(player);
        for (Ability ability : HakiAbilities.of(type)) {
            if (state.isActive(ability.id())) {
                AbilityManager.end(player, ability, Ability.EndReason.INTERRUPTED);
            }
        }
        GLPlayer.edit(player, data -> data.lock(UnlockCategory.HAKI, type.id()));
        for (Ability ability : HakiAbilities.of(type)) {
            if (!AbilityAccess.canUse(player, ability)) {
                for (int i = 0; i < AbilityState.SLOT_COUNT; i++) {
                    if (ability.id().equals(state.slot(i))) {
                        state.setSlot(i, null);
                    }
                }
            }
        }
        AbilityManager.sync(player);
        return true;
    }

    /** Removes every Haki and its level and training progress ({@code /onepiece reset haki}). */
    public static void reset(ServerPlayer player) {
        for (HakiType type : HakiType.values()) {
            remove(player, type);
        }
        GLPlayer.edit(player, data -> {
            for (HakiType type : HakiType.values()) {
                data.setMastery(type.masteryKey(), 0);
            }
            data.setCounter(ARMAMENT_HITS, 0);
            data.setCounter(OBSERVATION_HITS, 0);
        });
    }

    /** Raises a Haki's level through use (only for Haki the player has actually awakened). */
    public static void train(ServerPlayer player, HakiType type, double amount) {
        if (!has(player, type)) {
            return;
        }
        double gain = amount * GLServerConfig.masteryGainMultiplier() * RaceManager.masteryMultiplier(player);
        PlayerData data = GLPlayer.data(player);
        double before = data.mastery(type.masteryKey());
        if (gain <= 0 || before >= PlayerData.MAX_MASTERY) {
            return;
        }
        double after = Math.min(PlayerData.MAX_MASTERY, before + gain);
        data.setMastery(type.masteryKey(), after);
        if ((int) before != (int) after) {
            GLPlayer.sync(player);
        }
        announceTechniques(player, type, before, after);
    }

    private static void onAbilityUsed(ServerPlayer player, Ability ability) {
        if (ability.properties().category() != AbilityCategory.HAKI) {
            return;
        }
        HakiType type = typeOf(ability);
        if (type != null) {
            train(player, type, ability.properties().masteryGain());
        }
    }

    private static void announceTechniques(ServerPlayer player, HakiType type, double before, double after) {
        for (Ability ability : HakiAbilities.of(type)) {
            double required = ability.properties().requiredMastery();
            if (required > 0 && before < required && after >= required) {
                player.sendSystemMessage(Component.literal("New Haki technique: ").append(ability.displayName())
                        .withStyle(ChatFormatting.AQUA));
                AbilityState.of(player).equipInFreeSlot(ability.id());
                AbilityManager.sync(player);
            }
        }
    }

    private static void equipUsable(ServerPlayer player, HakiType type) {
        AbilityState state = AbilityState.of(player);
        for (Ability ability : HakiAbilities.of(type)) {
            if (AbilityAccess.canUse(player, ability)) {
                state.equipInFreeSlot(ability.id());
            }
        }
        AbilityManager.sync(player);
    }

    /** Conqueror's Haki is something you are born with: rolled once for each new player. */
    private static void onLogin(Player entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        PlayerData data = GLPlayer.data(player);
        if (data.counter(CONQUERORS_ROLLED) == 0) {
            data.setCounter(CONQUERORS_ROLLED, 1);
            if (player.getRandom().nextDouble() < GLServerConfig.conquerorsChance()) {
                unlock(player, HakiType.CONQUERORS, true);
            }
            GLPlayer.sync(player);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Combat
    // ---------------------------------------------------------------------------------------------------------

    /** Spends stamina for a Haki reaction (dodge, block). Free under the {@code haki infinite} test flag. */
    static boolean spend(ServerPlayer player, Ability ability, float amount) {
        AbilityState state = AbilityState.of(player);
        if (state.hasFlag(TestFlag.INFINITE_HAKI)) {
            return true;
        }
        boolean paid = state.spendStamina((float) (amount * GLServerConfig.staminaCostMultiplier()), AbilityManager.now(player));
        if (paid) {
            AbilityManager.sync(player);
        }
        return paid;
    }

    /** Sets a Haki render bit visible to every client. */
    static void setBit(ServerPlayer player, int bit, boolean on) {
        PublicState state = PublicState.of(player);
        if (state.hasHaki(bit) != on) {
            state.setHaki(bit, on);
            TransformationManager.sync(player);
        }
    }

    /** Steps aside from an attack that was read in advance. */
    static void sidestep(ServerPlayer player, Entity attacker) {
        Vec3 away = player.position().subtract(attacker.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(-player.getLookAngle().x, 0, -player.getLookAngle().z);
        }
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x).scale(player.getRandom().nextBoolean() ? 1 : -1);
        Vec3 step = side.scale(0.55).add(flat.scale(0.15));
        Fx.play(player.serverLevel(), GLEffects.FORESEEN_DODGE, player, FxParams.of(player.position(), side).color(0x7FD8FF));
        player.setDeltaMovement(step.x, Math.max(player.getDeltaMovement().y, 0.12), step.z);
        player.hurtMarked = true;
        Fx.animate(player, GLEffects.ANIM_SIDESTEP);
        player.serverLevel().playSound(null, player.blockPosition(), GLSounds.EVADE.get(), SoundSource.PLAYERS, 0.8f, 1.25f);
    }

    static void primeEmission(ServerPlayer player) {
        EMISSION_PRIMED.put(player.getUUID(), AbilityManager.now(player) + EMISSION_WINDOW);
    }

    public static boolean emissionPrimed(ServerPlayer player) {
        Long until = EMISSION_PRIMED.get(player.getUUID());
        return until != null && until > AbilityManager.now(player);
    }

    /** Active Haki changes every physical blow its user lands, abilities included. */
    private static void onAttack(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker) || !source.is(AbilityDamage.PHYSICAL)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == attacker) {
            return;
        }
        boolean melee = source.getDirectEntity() == attacker;
        boolean armed = armamentActive(attacker);
        boolean infused = infusionActive(attacker);
        if (!armed && !infused) {
            return;
        }
        ServerLevel level = attacker.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        float amount = event.getAmount();
        if (armed) {
            amount *= armamentMultiplier(attacker);
            if (melee) {
                Fx.play(level, GLEffects.HAKI_IMPACT, target, FxParams.of(center, center.subtract(attacker.getEyePosition()))
                        .color(HakiType.ARMAMENT.color()));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), GLSounds.HAKI_IMPACT.get(),
                        SoundSource.PLAYERS, 0.8f, 0.9f + attacker.getRandom().nextFloat() * 0.2f);
                train(attacker, HakiType.ARMAMENT, 0.05);
            }
            if (melee && emissionPrimed(attacker)) {
                EMISSION_PRIMED.remove(attacker.getUUID());
                amount += emissionBonus(attacker) * AbilityAccess.power(attacker, HakiAbilities.EMISSION.get());
                // The shock travels inside the body: armour does nothing against it.
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0f);
                AbilityDamage.knockback(target, attacker.position(), 1.6, 0.35);
                Vec3 dir = center.subtract(attacker.getEyePosition());
                Fx.play(level, GLEffects.HAKI_EMISSION, target, FxParams.of(center, dir).color(0xA77BFF));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), GLSounds.HAKI_EMISSION.get(),
                        SoundSource.PLAYERS, 1.2f, 1.0f);
                train(attacker, HakiType.ARMAMENT, 0.4);
            }
        }
        if (infused) {
            amount *= infusionMultiplier(attacker);
            if (melee) {
                AbilityDamage.knockback(target, attacker.position(), 0.8, 0.25);
                Fx.play(level, GLEffects.CONQUEROR_STRIKE, target, FxParams.at(center).color(HakiType.CONQUERORS.color()));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), GLSounds.HAKI_CRACKLE.get(),
                        SoundSource.PLAYERS, 0.9f, 0.9f + attacker.getRandom().nextFloat() * 0.2f);
                train(attacker, HakiType.CONQUERORS, 0.05);
                if (target instanceof ServerPlayer other && infusionActive(other)) {
                    clash(attacker, other);
                }
            }
        }
        event.setAmount(amount);
    }

    /** Fighting is how Haki awakens: landing blows hardens the will, taking them sharpens the senses. */
    private static void onDamaged(LivingDamageEvent.Post event) {
        if (!GLServerConfig.hakiTraining() || event.getNewDamage() <= 0) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && source.is(DamageTypes.PLAYER_ATTACK) && !has(attacker, HakiType.ARMAMENT)) {
            int hits = GLPlayer.data(attacker).increment(ARMAMENT_HITS);
            int needed = GLServerConfig.armamentTrainingHits();
            if (hits == needed / 2) {
                attacker.displayClientMessage(Component.literal("Your fists feel heavier than before...")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            if (hits >= needed) {
                unlock(attacker, HakiType.ARMAMENT, true);
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim && source.getEntity() instanceof LivingEntity attacker
                && attacker != victim && !has(victim, HakiType.OBSERVATION)) {
            int hits = GLPlayer.data(victim).increment(OBSERVATION_HITS);
            int needed = GLServerConfig.observationTrainingHits();
            if (hits == needed / 2) {
                victim.displayClientMessage(Component.literal("For a moment you felt that blow coming...")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            if (hits >= needed) {
                unlock(victim, HakiType.OBSERVATION, true);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Conqueror's
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Unleashes Conqueror's Haki around {@code player}: weak creatures are knocked out, others intimidated,
     * other Conqueror's users clash. @return how many were knocked out
     */
    public static int conquerorsWave(ServerPlayer player, float charge) {
        ServerLevel level = player.serverLevel();
        double userLevel = level(player, HakiType.CONQUERORS);
        double radius = conquerorsRadius(player, charge);
        float threshold = knockoutThreshold(player, charge);
        int koTicks = (int) (100 + 140 * charge);
        int knocked = 0;
        List<LivingEntity> targets = AbilityDamage.around(player, player.position(), radius);
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer other) {
                if (has(other, HakiType.CONQUERORS)) {
                    clash(player, other);
                } else if (!other.isCreative() && userLevel - strongestLevel(other) >= PLAYER_KNOCKOUT_GAP) {
                    knockOut(other, koTicks / 2);
                    knocked++;
                } else {
                    intimidate(target, player, charge);
                }
            } else if (target.getType().is(Tags.EntityTypes.BOSSES) || target.getMaxHealth() > threshold) {
                intimidate(target, player, charge);
            } else {
                knockOut(target, koTicks);
                knocked++;
            }
        }
        Fx.play(level, GLEffects.CONQUEROR_WAVE, player, FxParams.at(player.position()).color(HakiType.CONQUERORS.color())
                .scale((float) radius));
        level.playSound(null, player.blockPosition(), GLSounds.CONQUEROR_WAVE.get(), SoundSource.PLAYERS, 2.5f, 0.9f);
        if (knocked > 0) {
            player.displayClientMessage(Component.literal("Knocked out " + knocked).withStyle(ChatFormatting.RED), true);
            train(player, HakiType.CONQUERORS, 0.25 * Math.min(knocked, 8));
        }
        return knocked;
    }

    public static void knockOut(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, ticks, 0, false, false, true));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (target.level() instanceof ServerLevel level) {
            Fx.play(level, GLEffects.KNOCKOUT, target, FxParams.at(target.position()).scale(target.getBbHeight()).variant(ticks));
            level.playSound(null, target.getX(), target.getY(), target.getZ(), GLSounds.KNOCKOUT.get(), SoundSource.HOSTILE,
                    0.8f, 0.85f + target.getRandom().nextFloat() * 0.3f);
        }
    }

    private static void intimidate(LivingEntity target, ServerPlayer source, float charge) {
        int ticks = (int) (60 + 80 * charge);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        if (target instanceof ServerPlayer) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
        }
        if (target instanceof Mob mob && mob.getTarget() == source) {
            mob.setTarget(null);
        }
        AbilityDamage.knockback(target, source.position(), 0.6 + charge * 0.6, 0.15);
    }

    /** Two Conqueror's users collide: both are thrown back and the air between them splits with lightning. */
    public static void clash(ServerPlayer a, ServerPlayer b) {
        AbilityDamage.knockback(a, b.position(), 1.2, 0.3);
        AbilityDamage.knockback(b, a.position(), 1.2, 0.3);
        Vec3 from = a.position().add(0, 1, 0);
        Fx.play(a.serverLevel(), GLEffects.CONQUEROR_CLASH, a, FxParams.of(from, b.position().add(0, 1, 0).subtract(from))
                .color(HakiType.CONQUERORS.color()));
        Vec3 mid = from.add(b.position().add(0, 1, 0)).scale(0.5);
        a.serverLevel().playSound(null, mid.x, mid.y, mid.z, GLSounds.HAKI_CLASH.get(), SoundSource.PLAYERS, 2.0f, 1.0f);
        train(a, HakiType.CONQUERORS, 0.5);
        train(b, HakiType.CONQUERORS, 0.5);
    }
}
