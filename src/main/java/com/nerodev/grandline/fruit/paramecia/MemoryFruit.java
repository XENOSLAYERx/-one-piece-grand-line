package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Memo Memo no Mi. Pull memories out of a mind as strips of film, then cut, splice and rewrite them: a beast forgets
 * you, or remembers a friend as its enemy.
 */
public class MemoryFruit extends DevilFruit {
    public static final int COLOR = 0xF0C8A0;
    public static final int DARK = 0x8A3A3A;
    static final String M = "fruit/grandline:memory";
    static final String FORGOT = "grandline_forgot";
    static final String FORGOT_UNTIL = "grandline_forgot_until";
    public static final ResourceLocation TURNCOAT = GrandLine.id("memory_turncoat");

    public static final DeferredHolder<Ability, Erase> ERASE = ability("memory_erase", Erase::new);
    public static final DeferredHolder<Ability, FilmPull> FILM_PULL = ability("memory_film_pull", FilmPull::new);
    public static final DeferredHolder<Ability, Rewrite> REWRITE = ability("memory_rewrite", Rewrite::new);
    public static final DeferredHolder<Ability, CutScene> CUT_SCENE = ability("memory_cut_scene", CutScene::new);
    public static final DeferredHolder<Ability, MemoryCut> MEMORY_CUT = ability("memory_cut", MemoryCut::new);
    public static final DeferredHolder<Ability, MassWipe> MASS_WIPE = ability("memory_mass_wipe", MassWipe::new);
    public static final DeferredHolder<Ability, FalseLoyalty> FALSE_LOYALTY = ability("memory_false_loyalty", FalseLoyalty::new);

    public MemoryFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Memo Memo no Mi", COLOR)
                .abilities(ERASE, FILM_PULL, REWRITE, CUT_SCENE, MEMORY_CUT)
                .awakened(MASS_WIPE, FALSE_LOYALTY));
    }

    static void init() {}

    static void behaviors() {
        NeoForge.EVENT_BUS.addListener(LivingChangeTargetEvent.class, MemoryFruit::onTarget);
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, TURNCOAT);
    }

    /**
     * Cuts {@code player} out of a creature's memory for {@code ticks}: a beast stops chasing them and won't pick them
     * again unless they hurt it anew; a person is left dazed and lost.
     */
    static void forget(ServerPlayer player, LivingEntity target, int ticks) {
        if (target instanceof Mob mob) {
            if (mob instanceof NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            if (mob.getLastHurtByMob() == player) {
                mob.setLastHurtByMob(null);
            }
            mob.setAggressive(false);
            mob.getNavigation().stop();
            CompoundTag data = mob.getPersistentData();
            data.putUUID(FORGOT, player.getUUID());
            data.putLong(FORGOT_UNTIL, mob.level().getGameTime() + ticks);
        } else if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(60, ticks), 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks / 2, 1));
        }
        if (target.level() instanceof ServerLevel level) {
            Vfx.impact(level, target, target.getEyePosition().add(0, 0.4, 0), COLOR, 0.9f, Kind.STAR);
        }
    }

    /** A beast whose memory of a player was cut won't take them as a target again until they hurt it. */
    private static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player target) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (data.hasUUID(FORGOT) && target.getUUID().equals(data.getUUID(FORGOT))
                && data.getLong(FORGOT_UNTIL) >= mob.level().getGameTime() && mob.getLastHurtByMob() != target) {
            event.setCanceled(true);
        }
    }

    /** Splices a false memory into a beast: it now hates the nearest other creature (never the user). */
    static boolean turn(ServerPlayer player, Mob mob, int ticks) {
        LivingEntity victim = null;
        double best = 14 * 14;
        for (LivingEntity other : mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(14),
                e -> e != mob && e != player && e.isAlive() && !e.isSpectator() && !Minions.isAlly(player, e))) {
            double d = other.distanceToSqr(mob);
            if (d < best) {
                best = d;
                victim = other;
            }
        }
        forget(player, mob, ticks);
        if (victim == null) {
            return false;
        }
        mob.setTarget(victim);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Pluck out a foe's memory of you: a beast forgets the fight for ten seconds; a person is left dazed and lost. */
    public static final class Erase extends Ability {
        Erase() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(140).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                forget(player, target, 200);
                Vfx.beam(ctx.level(), player, target.getEyePosition(), ctx.handPos(), COLOR, 0.2f, Kind.STAR, Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.4f, 0.8f);
        }
    }

    /** Pull a strip of memory film out of a foe's head: it stands rooted and weakened while it tries to remember. */
    public static final class FilmPull extends Ability {
        FilmPull() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, DARK).cooldown(160).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Kit.root(target, Vfx.StatusLook.STRINGS, 50);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                Kit.hit(player, target, AbilityDamage.DRAIN, 4f * ctx.power());
                Vfx.beam(ctx.level(), player, target.getEyePosition(), ctx.handPos(), DARK, 0.3f, Kind.STREAK, Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.6f, 0.6f);
        }
    }

    /** Splice a false memory into a beast: it now remembers the nearest other creature as its enemy. */
    public static final class Rewrite extends Ability {
        Rewrite() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(240).stamina(16).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).filter(t -> t instanceof Mob).isPresent() ? Optional.empty()
                    : Optional.of(Component.literal("No beast in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                if (target instanceof Mob mob) {
                    turn(player, mob, 300);
                    Vfx.spiral(ctx.level(), player, mob.getEyePosition(), COLOR, 0.8f, Kind.STAR);
                }
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BOOK_PUT, 1.2f, 0.7f);
        }
    }

    /** Snip the fight out of the memory of every beast around you: they all forget you for ten seconds. */
    public static final class CutScene extends Ability {
        CutScene() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(400).stamina(20).mastery(M, 20).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 12 * ctx.power())) {
                if (target instanceof Mob) {
                    forget(player, target, 200);
                }
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 12f, Kind.STAR, Vfx.RING);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.4f, 0.8f);
        }
    }

    /**
     * Cut the memory of strength out of a foe: every boost it has is gone, a fighter forgets the technique it was
     * using, and the shock of the cut hurts.
     *
     * @name Memory Cut
     */
    public static final class MemoryCut extends Ability {
        MemoryCut() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, DARK).cooldown(300).stamina(20).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
                    if (effect.getEffect().value().isBeneficial()) {
                        target.removeEffect(effect.getEffect());
                    }
                }
                if (target instanceof ServerPlayer victim) {
                    AbilityManager.endAll(victim, Ability.EndReason.INTERRUPTED, true);
                }
                Kit.hit(player, target, AbilityDamage.DRAIN, 8f * ctx.power());
                Vfx.slash(ctx.level(), player, target.getEyePosition(), player.getLookAngle(), DARK, 1.4f, 1);
            });
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.SHEEP_SHEAR, 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Wipe the memory of everyone nearby: beasts forget you for fifteen seconds and wander; people stagger, lost. */
    public static final class MassWipe extends Ability {
        MassWipe() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16 * ctx.power())) {
                forget(player, target, 300);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 16f, Kind.STAR, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.6f, 0.8f);
        }
    }

    /** Rewrite the loyalties of every beast around you: for fifteen seconds they remember you as their master. */
    public static final class FalseLoyalty extends Ability {
        FalseLoyalty() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, DARK).cooldown(1000).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                if (target instanceof Mob mob && Minions.bind(player, TURNCOAT, mob, 300)) {
                    forget(player, mob, 300);
                    Vfx.impact(ctx.level(), player, mob.getEyePosition().add(0, 0.5, 0), DARK, 1f, Kind.HEART);
                }
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), DARK, 10f, Kind.STAR, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_CONQUEROR_RELEASE);
            ctx.sound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.6f, 0.7f);
        }
    }
}
