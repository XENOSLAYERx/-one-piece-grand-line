package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Modo Modo no Mi. Send anything you touch back in time: foes grow twelve years younger and weaker, wounds and ailments
 * are undone, and you can return yourself to where, and how whole, you were moments ago.
 */
public class ReturnFruit extends DevilFruit {
    public static final int COLOR = 0x6AC8E8;
    public static final int WHITE = 0xF0F0F0;
    static final String M = "fruit/grandline:return";
    /** Snapshots of each user, every half second, for the last twelve seconds. */
    static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();
    static final int KEEP = 24;

    public static final DeferredHolder<Ability, Rewind> REWIND = ability("return_rewind", Rewind::new);
    public static final DeferredHolder<Ability, TwelveYears> TWELVE_YEARS = ability("return_twelve_years", TwelveYears::new);
    public static final DeferredHolder<Ability, Mend> MEND = ability("return_mend", Mend::new);
    public static final DeferredHolder<Ability, Unmake> UNMAKE = ability("return_unmake", Unmake::new);
    public static final DeferredHolder<Ability, RewindField> REWIND_FIELD = ability("return_rewind_field", RewindField::new);
    public static final DeferredHolder<Ability, FullReturn> FULL_RETURN = ability("return_full_return", FullReturn::new);
    public static final DeferredHolder<Ability, RegressionWave> REGRESSION_WAVE = ability("return_regression_wave",
            RegressionWave::new);

    public static final ResourceLocation FIELD = GrandLine.id("return_field");

    record Snapshot(long time, ResourceKey<Level> dimension, Vec3 pos, float health) {}

    public ReturnFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Modo Modo no Mi", COLOR)
                .abilities(REWIND, TWELVE_YEARS, MEND, UNMAKE, REWIND_FIELD)
                .awakened(FULL_RETURN, REGRESSION_WAVE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(10, (p, target) ->
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2)), p -> {
                    ServerPlayer owner = p.ownerPlayer();
                    if (owner == null || p.tickCount % 20 != 0) {
                        return;
                    }
                    double r = p.size() / 2.0;
                    for (LivingEntity friend : ClothesFruit.friends(owner, r + 16)) {
                        if (friend.distanceToSqr(p.position()) <= r * r) {
                            friend.heal(1.5f);
                            unwind(friend);
                        }
                    }
                }));
    }

    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % 10 != 0) {
            return;
        }
        Deque<Snapshot> history = HISTORY.computeIfAbsent(user.getUUID(), k -> new ArrayDeque<>());
        history.addLast(new Snapshot(user.level().getGameTime(), user.level().dimension(), user.position(), user.getHealth()));
        while (history.size() > KEEP) {
            history.removeFirst();
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        HISTORY.remove(player.getUUID());
    }

    /** Where the user was {@code ticks} ago (the newest snapshot at least that old, in this dimension). */
    @Nullable
    static Snapshot ago(ServerPlayer player, int ticks) {
        Deque<Snapshot> history = HISTORY.get(player.getUUID());
        if (history == null) {
            return null;
        }
        long cutoff = player.level().getGameTime() - ticks;
        Iterator<Snapshot> it = history.descendingIterator();
        while (it.hasNext()) {
            Snapshot s = it.next();
            if (s.time() <= cutoff) {
                return s.dimension() == player.level().dimension() ? s : null;
            }
        }
        return null;
    }

    /** Returns the user to a snapshot: back where they stood, and at least as healthy as they were then. */
    static void returnTo(ServerPlayer player, Snapshot then) {
        Vec3 from = player.position();
        player.teleportTo(then.pos().x, then.pos().y, then.pos().z);
        player.resetFallDistance();
        player.setHealth(Math.max(player.getHealth(), Math.min(then.health(), player.getMaxHealth())));
        Vfx.blink(player.serverLevel(), player, from, then.pos(), COLOR, Kind.STAR);
    }

    /** Clears every harmful or neutral effect: the body returns to before it was afflicted. */
    static void unwind(LivingEntity target) {
        for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial()) {
                target.removeEffect(effect.getEffect());
            }
        }
    }

    /** Twelve years taken off a creature: smaller and much weaker for {@code ticks}. */
    static void younger(LivingEntity target, int ticks) {
        Buffs.timed(target, Buffs.mod(Attributes.SCALE, GrandLine.id("return_younger_scale"), -0.35, Operation.ADD_MULTIPLIED_TOTAL),
                ticks);
        Buffs.timed(target, Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("return_younger_damage"), -0.4,
                Operation.ADD_MULTIPLIED_TOTAL), ticks);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        if (target.level() instanceof ServerLevel level) {
            Vfx.burst(level, target, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.STAR, Vfx.FLASH);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Return yourself five seconds: back where you stood then, and at least as healthy as you were. */
    public static final class Rewind extends Ability {
        Rewind() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(300).stamina(14).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return ago(player, 100) == null ? Optional.of(Component.literal("Nothing to return to yet")) : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Snapshot then = ago(ctx.player(), 100);
            if (then != null) {
                returnTo(ctx.player(), then);
            }
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.6f);
        }
    }

    /** Touch a foe and send twelve years of it back: smaller and far weaker for ten seconds. */
    public static final class TwelveYears extends Ability {
        TwelveYears() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, WHITE).cooldown(140).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                younger(target, 200);
                Kit.hit(player, target, AbilityDamage.DRAIN, 4f * ctx.power());
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.6f);
        }
    }

    /** Return the bodies of you and everyone beside you to before they were hurt: wounds close and ailments vanish. */
    public static final class Mend extends Ability {
        Mend() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(300).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ClothesFruit.friends(ctx.player(), 4)) {
                friend.heal(6f);
                unwind(friend);
                Vfx.impact(ctx.level(), friend, friend.getBoundingBox().getCenter(), COLOR, 1f, Kind.STAR);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1f, 1.6f);
        }
    }

    /** Return a foe's strength to before it had any: every boost on it is gone, and the shock hurts. */
    public static final class Unmake extends Ability {
        Unmake() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, WHITE).cooldown(220).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
                    if (effect.getEffect().value().isBeneficial()) {
                        target.removeEffect(effect.getEffect());
                    }
                }
                Kit.hit(player, target, AbilityDamage.DRAIN, 12f * ctx.power());
                Vfx.spiral(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.STAR);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.6f);
        }
    }

    /** Open a field where time runs backward for ten seconds: friends inside heal and shed ailments, foes are held back. */
    public static final class RewindField extends Ability {
        RewindField() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(500).stamina(18).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, FIELD);
            AbilityProjectile.zone(player, FIELD, player.position(), 6 * ctx.power(), COLOR, 0f, 200);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 6f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Return yourself ten seconds, wounds and ailments and all, and pull every friend beside you back from their
     * injuries too.
     */
    public static final class FullReturn extends Ability {
        FullReturn() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, WHITE).cooldown(1000).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity friend : ClothesFruit.friends(player, 8)) {
                friend.heal(8f);
                unwind(friend);
            }
            Snapshot then = ago(player, 200);
            if (then != null) {
                returnTo(player, then);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.STAR, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1.6f, 0.8f);
        }
    }

    /** Sweep twelve years off everything in front of you: foes shrink, weaken and stagger. */
    public static final class RegressionWave extends Ability {
        RegressionWave() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 45)) {
                younger(target, 240);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                Kit.hit(player, target, AbilityDamage.DRAIN, 8f * ctx.power());
            }
            for (int i = 1; i <= 3; i++) {
                Vfx.burst(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(i * 3)), COLOR, 1.4f + i,
                        Kind.STAR, 0);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.4f);
        }
    }
}
