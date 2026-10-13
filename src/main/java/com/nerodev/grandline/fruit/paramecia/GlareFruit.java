package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Giro Giro no Mi. Eyes that see through everything: walls, distance, bodies to their weak points, and minds to their
 * next move.
 */
public class GlareFruit extends DevilFruit {
    public static final int COLOR = 0xE84A6A;
    public static final int DARK = 0x2A2A2A;
    static final String M = "fruit/grandline:glare";
    static final String WEAK_BY = "grandline_glare_weak_by";
    static final String WEAK_UNTIL = "grandline_glare_weak_until";

    public static final DeferredHolder<Ability, Clairvoyance> CLAIRVOYANCE = ability("glare_clairvoyance", Clairvoyance::new);
    public static final DeferredHolder<Ability, ReadMind> READ_MIND = ability("glare_read_mind", ReadMind::new);
    public static final DeferredHolder<Ability, ParalyzingGlare> PARALYZING_GLARE = ability("glare_paralyzing_glare",
            ParalyzingGlare::new);
    public static final DeferredHolder<Ability, WeakPoint> WEAK_POINT = ability("glare_weak_point", WeakPoint::new);
    public static final DeferredHolder<Ability, MindStrike> MIND_STRIKE = ability("glare_mind_strike", MindStrike::new);
    public static final DeferredHolder<Ability, AllSeeing> ALL_SEEING = ability("glare_all_seeing", AllSeeing::new);
    public static final DeferredHolder<Ability, Foresight> FORESIGHT = ability("glare_foresight", Foresight::new);

    public GlareFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Giro Giro no Mi", COLOR)
                .abilities(CLAIRVOYANCE, READ_MIND, PARALYZING_GLARE, WEAK_POINT, MIND_STRIKE)
                .awakened(ALL_SEEING, FORESIGHT));
    }

    static void init() {}

    static void behaviors() {}

    static void exposeWeakPoint(ServerPlayer player, LivingEntity target, int ticks) {
        target.getPersistentData().putUUID(WEAK_BY, player.getUUID());
        target.getPersistentData().putLong(WEAK_UNTIL, player.level().getGameTime() + ticks);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
    }

    /** Foes whose weak point you have seen take half again as much damage from you. */
    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (target.getPersistentData().hasUUID(WEAK_BY) && user.getUUID().equals(target.getPersistentData().getUUID(WEAK_BY))
                && target.getPersistentData().getLong(WEAK_UNTIL) >= user.level().getGameTime()) {
            event.setAmount(event.getAmount() * 1.5f);
        }
    }

    /** With Foresight active, you read what each attacker means to do and step aside from half of their blows. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (AbilityState.of(user).isActive(FORESIGHT.getId()) && event.getSource().getEntity() instanceof LivingEntity
                && user.getRandom().nextFloat() < 0.5f) {
            event.setCanceled(true);
            Vfx.impact(user.serverLevel(), user, user.getEyePosition(), COLOR, 0.8f, Kind.STAR);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** See through every wall: all living things within fifty blocks are outlined for you. */
    public static final class Clairvoyance extends Ability {
        Clairvoyance() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, COLOR).cooldown(100).stamina(8).staminaPerTick(0.08f)
                    .maxActive(1200).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.HAKI_SENSE.get(), 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 20 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50),
                        e -> e != player && e.isAlive())) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }
            return true;
        }
    }

    /** Look into a creature's mind: learn how hurt it is and what it means to do, and react faster for five seconds. */
    public static final class ReadMind extends Ability {
        ReadMind() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, DARK).cooldown(160).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 32).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 32).ifPresent(target -> {
                String intent = target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null
                        ? "hunting " + mob.getTarget().getName().getString() : "wary";
                player.displayClientMessage(Component.literal(target.getName().getString() + ": "
                        + String.format(Locale.ROOT, "%.0f/%.0f", target.getHealth(), target.getMaxHealth()) + " health, " + intent), true);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.FUTURE_SIGHT.get(), 0.8f, 1.4f);
        }
    }

    /** A piercing glare that freezes a creature where it stands (players only falter for a moment). */
    public static final class ParalyzingGlare extends Ability {
        ParalyzingGlare() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(200).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 20).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 20).ifPresent(target -> {
                if (target instanceof Player) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                } else if (!target.getType().is(Tags.EntityTypes.BOSSES)) {
                    target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 40, 0, false, false, true));
                }
                Vfx.beam(ctx.level(), player, player.getEyePosition(), target.getEyePosition(), COLOR, 0.15f, Kind.GLOW, Vfx.BEAM_GLOW);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.HAKI_CRACKLE.get(), 1f, 1.4f);
        }
    }

    /** See straight through a foe to its weak point: for ten seconds it takes half again as much damage from you. */
    public static final class WeakPoint extends Ability {
        WeakPoint() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(240).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                exposeWeakPoint(player, target, 200);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.RING, Vfx.RING);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.HAKI_SENSE.get(), 1.2f, 1.6f);
        }
    }

    /** Strike exactly where a foe's mind least expects it: damage that ignores armour and leaves it reeling. */
    public static final class MindStrike extends Ability {
        MindStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, DARK).cooldown(120).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DRAIN, 10f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                Vfx.impact(ctx.level(), target, target.getEyePosition(), COLOR, 1.2f, Kind.STAR);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.HAKI_CRACKLE.get(), 1.2f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** See every foe around you through and through: all of them are outlined, weak points exposed. */
    public static final class AllSeeing extends Ability {
        AllSeeing() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 16 * ctx.power())) {
                exposeWeakPoint(player, target, 300);
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 16f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.FUTURE_SIGHT.get(), 1.4f, 1f);
        }
    }

    /** Read every mind around you for fifteen seconds: half of all attacks against you simply miss. */
    public static final class Foresight extends Ability {
        Foresight() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, DARK).cooldown(800).stamina(24).staminaPerTick(0.2f).maxActive(300)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(GLSounds.FUTURE_SIGHT.get(), 1.2f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getEyePosition(), COLOR, 0.6f, Kind.GLOW);
            }
            return true;
        }
    }
}
