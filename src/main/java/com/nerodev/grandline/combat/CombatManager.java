package com.nerodev.grandline.combat;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.network.CombatSyncPayload;
import com.nerodev.grandline.sound.GLSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Melee combat on top of vanilla's: combos, heavy, air, dash and counter attacks, dodging and guarding. Only the blow
 * a player actually swings at its target is graded (not sweep damage to bystanders).
 * <p>
 * Everything is decided on the server. The client only reports "dodge pressed (with this movement input)" and
 * "guard key down/up"; attacks are ordinary vanilla attacks that this class grades as they land:
 * <ul>
 * <li><b>Combo:</b> full-strength blows landed within the combo window chain into a combo that hits a little harder
 * each step. Every fifth blow is the fighting style's <b>finisher</b>.</li>
 * <li><b>Heavy:</b> attacking while sneaking costs stamina and hits half again as hard; after three or more chained
 * blows it ends the combo with the finisher instead.</li>
 * <li><b>Air</b> blows (while falling) and <b>dash</b> blows (right after a dodge) hit harder.</li>
 * <li><b>Guard:</b> holding the guard key stops most of a blow from the front at a stamina cost. Raising the guard
 * just as the blow lands is a <b>parry</b>: no damage, the attacker staggers, and the next blow within the counter
 * window is a <b>counter</b>. A guard that runs out of stamina breaks.</li>
 * <li><b>Dodge:</b> a quick dash in the direction you are moving with a few invulnerable frames.</li>
 * </ul>
 * Being hit (not guarded) drops your combo.
 */
public final class CombatManager {
    /** A fighting style's combo finisher: what the chain's last blow does to the target. */
    @FunctionalInterface
    public interface Finisher {
        /** Called as the finishing blow lands; {@code combo} is the chain length it ended. */
        void finish(ServerPlayer attacker, LivingEntity target, int combo);
    }

    public static final int FINISHER_EVERY = 5;
    public static final int AIR_DODGE_LIMIT = 1;
    static final int DASH_WINDOW = 10;
    static final int COUNTER_WINDOW = 30;
    static final int GUARD_BREAK_TICKS = 50;
    static final float COUNTER_MULTIPLIER = 1.5f;
    static final float HEAVY_MULTIPLIER = 1.5f;
    static final float AIR_MULTIPLIER = 1.2f;
    static final float DASH_MULTIPLIER = 1.25f;
    static final float GUARD_STAMINA_PER_DAMAGE = 1.5f;
    static final float GUARD_UPKEEP = 0.08f;

    static final Buffs.Mod GUARD_SLOW = Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("guard_slow"), -0.5,
            Operation.ADD_MULTIPLIED_TOTAL);

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** The default finisher (and the Brawler's): an uppercut that launches the target. */
    public static final Finisher UPPERCUT = (attacker, target, combo) -> {
        Vec3 away = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        Vec3 push = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.5);
        target.setDeltaMovement(push.x, 0.85, push.z);
        target.hurtMarked = true;
        Vfx.burst(attacker.serverLevel(), attacker, target.getBoundingBox().getCenter(), 0xFFF2C0, 1.6f, Kind.SHOCKWAVE, Vfx.FLASH);
        Fx.animate(attacker, GLEffects.ANIM_UPPERCUT);
    };

    private static Function<ServerPlayer, Finisher> finishers = player -> UPPERCUT;

    private CombatManager() {}

    /** Per-player combat state. Not saved: combos, guards and dodges only matter in the moment. */
    static final class State {
        int combo;
        long lastHit = Long.MIN_VALUE / 2;
        long lastDodge = Long.MIN_VALUE / 2;
        long dodgeReady;
        int airDodges;
        boolean guarding;
        long guardStart;
        long guardBrokenUntil;
        long counterUntil;
        float pendingStrength = 1f;
        long pendingTick = Long.MIN_VALUE / 2;
        int pendingTarget = -1;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AttackEntityEvent.class, CombatManager::onAttack);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingIncomingDamageEvent.class, CombatManager::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LivingKnockBackEvent.class, CombatManager::onKnockBack);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, CombatManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> STATES.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                endGuard(player, state(player));
                STATES.remove(player.getUUID());
                sync(player);
            }
        });
    }

    /** Lets the fighting-style system decide each player's combo finisher. */
    public static void setFinishers(Function<ServerPlayer, Finisher> resolver) {
        finishers = resolver;
    }

    static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Queries (also used by GameTests)
    // ---------------------------------------------------------------------------------------------------------

    public static int combo(ServerPlayer player) {
        State s = state(player);
        return AbilityManager.now(player) - s.lastHit <= GLServerConfig.comboWindowTicks() ? s.combo : 0;
    }

    public static boolean isGuarding(ServerPlayer player) {
        return state(player).guarding;
    }

    public static boolean counterReady(ServerPlayer player) {
        return AbilityManager.now(player) <= state(player).counterUntil;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Attacking
    // ---------------------------------------------------------------------------------------------------------

    /** Vanilla is about to work out a melee blow: remember how charged it was. Guarding players cannot attack. */
    private static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State s = state(player);
        if (s.guarding) {
            event.setCanceled(true);
            return;
        }
        s.pendingStrength = player.getAttackStrengthScale(0.5f);
        s.pendingTick = AbilityManager.now(player);
        s.pendingTarget = event.getTarget().getId();
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (target instanceof ServerPlayer defender) {
            defend(defender, event);
            if (event.isCanceled()) {
                return;
            }
        }
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target && source.getDirectEntity() == attacker
                && source.is(DamageTypes.PLAYER_ATTACK)) {
            // Only the blow the player actually swung at is graded, once: sweep damage to bystanders is not.
            State s = state(attacker);
            if (s.pendingTarget == target.getId() && AbilityManager.now(attacker) - s.pendingTick <= 1) {
                s.pendingTarget = -1;
                event.setAmount(strike(attacker, target, event.getAmount()));
            }
        }
    }

    /**
     * Grades a melee blow as it lands and returns its new damage.
     *
     * @param amount the damage vanilla worked out
     */
    public static float strike(ServerPlayer attacker, LivingEntity target, float amount) {
        State s = state(attacker);
        long now = AbilityManager.now(attacker);
        boolean fullSwing = now - s.pendingTick > 1 || s.pendingStrength >= 0.85f;
        if (now - s.lastHit > GLServerConfig.comboWindowTicks()) {
            s.combo = 0;
        }
        ServerLevel level = attacker.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        float multiplier = 1f;

        if (now <= s.counterUntil) {
            s.counterUntil = 0;
            multiplier *= COUNTER_MULTIPLIER;
            Vfx.burst(level, attacker, center, 0xFFE070, 1.8f, Kind.STAR, Vfx.FLASH | Vfx.RING);
            sound(attacker, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.4f);
            attacker.displayClientMessage(Component.literal("Counter!"), true);
        }
        if (now - s.lastDodge <= DASH_WINDOW) {
            multiplier *= DASH_MULTIPLIER;
            AbilityDamage.knockback(target, attacker.position(), 0.8, 0.2);
            Vfx.impact(level, attacker, center, 0xFFFFFF, 1.2f, Kind.STREAK);
        }
        if (!attacker.onGround() && attacker.fallDistance > 0.3f && !attacker.isInWater() && !attacker.getAbilities().flying) {
            multiplier *= AIR_MULTIPLIER;
            Vec3 v = target.getDeltaMovement();
            target.setDeltaMovement(v.x, Math.min(v.y, -0.6), v.z);
            target.hurtMarked = true;
            Vfx.impact(level, attacker, center, 0xC8E0FF, 1.2f, Kind.SHOCKWAVE);
        }

        boolean heavy = fullSwing && attacker.isShiftKeyDown()
                && AbilityState.of(attacker).spendStamina(GLServerConfig.heavyAttackStamina(), now);
        int combo = s.combo;
        boolean finisher;
        if (heavy) {
            multiplier *= HEAVY_MULTIPLIER;
            finisher = combo >= 3;
            if (finisher) {
                multiplier *= 1f + 0.08f * Math.min(combo, 8);
            }
            AbilityDamage.knockback(target, attacker.position(), 1.2, 0.3);
            sound(attacker, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.1f, 0.7f);
            Vfx.impact(level, attacker, center, 0xFFD8A0, 1.6f, Kind.SHOCKWAVE);
            Fx.animate(attacker, GLEffects.ANIM_CHARGE_PUNCH);
        } else if (fullSwing) {
            combo++;
            multiplier *= 1f + 0.05f * Math.min(combo - 1, 5);
            finisher = combo % FINISHER_EVERY == 0;
            if (combo > 1 && !finisher) {
                Vfx.impact(level, attacker, center, 0xFFFFFF, 0.6f + 0.1f * Math.min(combo, 6), Kind.SPARK);
            }
        } else {
            finisher = false;
        }

        if (finisher) {
            multiplier *= 1.25f;
            finishers.apply(attacker).finish(attacker, target, Math.max(combo, FINISHER_EVERY));
            sound(attacker, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.8f);
            s.combo = 0;
        } else if (heavy) {
            s.combo = 0;
        } else {
            s.combo = combo;
        }
        if (fullSwing || heavy) {
            s.lastHit = now;
        }
        sync(attacker);
        return amount * multiplier;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Guarding
    // ---------------------------------------------------------------------------------------------------------

    /** Guard key went down or up. */
    public static void setGuard(ServerPlayer player, boolean down) {
        State s = state(player);
        long now = AbilityManager.now(player);
        if (down) {
            if (s.guarding || now < s.guardBrokenUntil || GLMobEffects.incapacitated(player) || player.isSpectator()) {
                return;
            }
            s.guarding = true;
            s.guardStart = now;
            Buffs.apply(player, GUARD_SLOW);
            player.setSprinting(false);
            Fx.animate(player, GLEffects.ANIM_GUARD);
            sound(player, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8f, 1.3f);
        } else if (s.guarding) {
            endGuard(player, s);
        } else {
            return;
        }
        sync(player);
    }

    static void endGuard(ServerPlayer player, State s) {
        if (!s.guarding) {
            return;
        }
        s.guarding = false;
        Buffs.remove(player, GUARD_SLOW);
        Fx.stopAnimation(player);
    }

    /** True if the hit comes from somewhere in front of {@code defender}. */
    static boolean fromFront(ServerPlayer defender, Vec3 from) {
        Vec3 to = from.subtract(defender.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-4) {
            return true;
        }
        Vec3 look = defender.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 1.0E-4 || look.normalize().dot(to.normalize()) > 0.1;
    }

    /** Guard, parry and combo-breaking for a player about to take damage. */
    static void defend(ServerPlayer defender, LivingIncomingDamageEvent event) {
        State s = state(defender);
        DamageSource source = event.getSource();
        long now = AbilityManager.now(defender);
        if (s.guarding && source.getSourcePosition() != null && !source.is(DamageTypeTags.BYPASSES_SHIELD)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && fromFront(defender, source.getSourcePosition())) {
            ServerLevel level = defender.serverLevel();
            Vec3 at = defender.getEyePosition().add(defender.getLookAngle().scale(0.8));
            if (now - s.guardStart <= GLServerConfig.parryWindowTicks()) {
                event.setCanceled(true);
                s.counterUntil = now + COUNTER_WINDOW;
                if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                        && attacker.distanceToSqr(defender) < 36) {
                    attacker.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 12, 0, false, false, true));
                    attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    AbilityDamage.knockback(attacker, defender.position(), 0.9, 0.2);
                }
                Vfx.burst(level, defender, at, 0xFFFFFF, 1.6f, Kind.STAR, Vfx.FLASH | Vfx.RING);
                sound(defender, SoundEvents.SHIELD_BLOCK, 1.2f, 1.5f);
                sound(defender, GLSounds.HAKI_IMPACT.get(), 0.6f, 1.6f);
                Fx.animate(defender, GLEffects.ANIM_PARRY);
                defender.displayClientMessage(Component.literal("Parry!"), true);
                sync(defender);
                return;
            }
            float blocked = event.getAmount() * GLServerConfig.guardReduction();
            if (AbilityState.of(defender).spendStamina(blocked * GUARD_STAMINA_PER_DAMAGE, now)) {
                event.setAmount(event.getAmount() - blocked);
                Vfx.impact(level, defender, at, 0xE8E8F0, 1.0f, Kind.SPARK);
                sound(defender, SoundEvents.SHIELD_BLOCK, 0.9f, 1.0f);
            } else {
                endGuard(defender, s);
                s.guardBrokenUntil = now + GUARD_BREAK_TICKS;
                defender.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GUARD_BREAK_TICKS, 2));
                Vfx.burst(level, defender, at, 0xF0A060, 1.4f, Kind.SHARD, Vfx.FLASH);
                sound(defender, SoundEvents.SHIELD_BREAK, 1.2f, 0.8f);
                defender.displayClientMessage(Component.literal("Guard broken!"), true);
            }
        }
        if (event.getAmount() > 0 && source.getEntity() != null && s.combo > 0) {
            s.combo = 0;
        }
        sync(defender);
    }

    /** A guard soaks most of the knockback of blows from the front. */
    private static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && state(player).guarding) {
            // knockback(x, z) points from the target towards whatever hit it.
            Vec3 towards = player.position().add(event.getRatioX(), 0, event.getRatioZ());
            if (fromFront(player, towards)) {
                event.setStrength(event.getStrength() * 0.35f);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Dodging
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Dodge key pressed with the given movement input (vanilla's: {@code forward} +1 forwards, {@code left} +1 to the
     * left). No input dodges backwards. @return true if the dodge happened
     */
    public static boolean dodge(ServerPlayer player, float forward, float left) {
        State s = state(player);
        AbilityState abilities = AbilityState.of(player);
        long now = AbilityManager.now(player);
        if (now < s.dodgeReady || s.guarding || GLMobEffects.incapacitated(player) || player.hasEffect(GLMobEffects.ROOTED)
                || player.isPassenger() || player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        if (!player.onGround() && !player.isInWater() && s.airDodges >= AIR_DODGE_LIMIT) {
            return false;
        }
        if (!abilities.spendStamina(GLServerConfig.dodgeStamina(), now)) {
            return false;
        }
        forward = Mth.clamp(forward, -1, 1);
        left = Mth.clamp(left, -1, 1);
        if (Math.abs(forward) < 0.1f && Math.abs(left) < 0.1f) {
            forward = -1;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);
        Vec3 dir = new Vec3(left * cos - forward * sin, 0, forward * cos + left * sin).normalize();
        Vec3 from = player.position();
        double lift = player.onGround() ? 0.18 : 0.1;
        player.setDeltaMovement(dir.x * 1.15, lift, dir.z * 1.15);
        player.hurtMarked = true;
        player.resetFallDistance();
        abilities.evadeFor(now, 7);
        s.lastDodge = now;
        s.dodgeReady = now + GLServerConfig.dodgeCooldownTicks();
        if (!player.onGround()) {
            s.airDodges++;
        }
        Fx.play(player.serverLevel(), GLEffects.EVADE, player, FxParams.at(from));
        Vfx.trail(player.serverLevel(), player, from.add(0, 0.9, 0), from.add(dir.scale(3)).add(0, 0.9, 0), 0xE8F0FF, 0.6f, Kind.STREAK);
        Fx.animate(player, GLEffects.ANIM_SIDESTEP);
        sound(player, GLSounds.EVADE.get(), 0.9f, 1.2f);
        AbilityManager.sync(player);
        sync(player);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State s = STATES.get(player.getUUID());
        if (s == null) {
            return;
        }
        if (player.onGround() || player.isInWater()) {
            s.airDodges = 0;
        }
        if (s.guarding) {
            long now = AbilityManager.now(player);
            player.setSprinting(false);
            boolean upkeep = AbilityState.of(player).spendStamina(GUARD_UPKEEP, now);
            if (!upkeep || GLMobEffects.incapacitated(player) || !player.isAlive()) {
                endGuard(player, s);
                sync(player);
            }
        }
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Sends the owning client what its combat HUD shows. */
    static void sync(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }
        State s = state(player);
        PacketDistributor.sendToPlayer(player, new CombatSyncPayload(s.combo, s.lastHit + GLServerConfig.comboWindowTicks(),
                s.guarding, s.counterUntil, s.dodgeReady));
    }
}
