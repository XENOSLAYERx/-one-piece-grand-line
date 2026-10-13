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
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Raki Raki no Mi. Steal the luck of anyone you touch: they stumble into misfortune while you bank it, up to five
 * times over. Banked luck makes your blows land critically and attacks miss you; spend it all for a jackpot.
 */
public class LuckFruit extends DevilFruit {
    public static final int COLOR = 0xF0E040;
    public static final int GREEN = 0x3A8A3A;
    static final String M = "fruit/grandline:luck";
    static final String LUCK = "grandline_luck";
    static final int MAX = 5;

    public static final DeferredHolder<Ability, StealLuck> STEAL_LUCK = ability("luck_steal", StealLuck::new);
    public static final DeferredHolder<Ability, LuckyStrike> LUCKY_STRIKE = ability("luck_lucky_strike", LuckyStrike::new);
    public static final DeferredHolder<Ability, LuckyDodge> LUCKY_DODGE = ability("luck_lucky_dodge", LuckyDodge::new);
    public static final DeferredHolder<Ability, Jackpot> JACKPOT = ability("luck_jackpot", Jackpot::new);
    public static final DeferredHolder<Ability, Misfortune> MISFORTUNE = ability("luck_misfortune", Misfortune::new);
    public static final DeferredHolder<Ability, AllIn> ALL_IN = ability("luck_all_in", AllIn::new);
    public static final DeferredHolder<Ability, LuckyStreak> LUCKY_STREAK = ability("luck_lucky_streak", LuckyStreak::new);

    public LuckFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Raki Raki no Mi", COLOR)
                .abilities(STEAL_LUCK, LUCKY_STRIKE, LUCKY_DODGE, JACKPOT, MISFORTUNE)
                .awakened(ALL_IN, LUCKY_STREAK));
    }

    static void init() {}

    static void behaviors() {}

    static int luck(ServerPlayer player) {
        return player.getPersistentData().getInt(LUCK);
    }

    static void setLuck(ServerPlayer player, int stacks) {
        player.getPersistentData().putInt(LUCK, Math.max(0, Math.min(MAX, stacks)));
        player.displayClientMessage(Component.literal("Luck: " + "☘".repeat(luck(player))), true);
    }

    /** Rolls a critical hit: 20% plus 10% for every banked luck. */
    static float crit(ServerPlayer player, LivingEntity target) {
        if (player.getRandom().nextFloat() < 0.2f + 0.1f * luck(player)) {
            Vfx.impact(player.serverLevel(), player, target.getEyePosition(), COLOR, 1.2f, Kind.STAR);
            return 2f;
        }
        return 1f;
    }

    @Override
    public void onLost(ServerPlayer player) {
        player.getPersistentData().remove(LUCK);
    }

    /** Banked luck fades: one stack every thirty seconds. */
    @Override
    public boolean ticks() {
        return true;
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.tickCount % 600 == 0 && luck(user) > 0) {
            setLuck(user, luck(user) - 1);
        }
    }

    /** Every blow of the user's has a small chance per banked luck to land critically (half again as hard). */
    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        if (luck(user) > 0 && user.getRandom().nextFloat() < 0.05f * luck(user)) {
            event.setAmount(event.getAmount() * 1.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Touch foes and steal their luck: they are cursed with bad luck, and you bank one luck for each.
     *
     * @name Steal Luck
     */
    public static final class StealLuck extends Ability {
        StealLuck() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(60).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int stolen = 0;
            for (LivingEntity target : AbilityDamage.cone(player, 3.8, 50)) {
                target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 600, 1));
                Kit.hit(player, target, AbilityDamage.DRAIN, 4f * ctx.power());
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), COLOR, 0.2f, Kind.STAR,
                        Vfx.BEAM_PARTICLES);
                stolen++;
            }
            if (stolen > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 0));
                setLuck(player, luck(player) + stolen);
            }
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
        }
    }

    /** Strike, and let luck guide the blow: with luck banked, it often lands for double. */
    public static final class LuckyStrike extends Ability {
        LuckyStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, GREEN).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4, 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 7f * ctx.power() * crit(player, target));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.2)), COLOR, 1.2f, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 0.8f, 1.2f);
        }
    }

    /** Trust your luck: while this lasts, attacks often just miss you, and more often the more luck you've banked. */
    public static final class LuckyDodge extends Ability {
        LuckyDodge() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, COLOR).cooldown(200).stamina(10).staminaPerTick(0.15f).maxActive(400)
                    .mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 12 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), GREEN, 0.7f, Kind.STAR);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            ServerPlayer player = ctx.player();
            if (event.getSource().getEntity() != null && player.getRandom().nextFloat() < 0.25f + 0.06f * luck(player)) {
                event.setCanceled(true);
                ctx.sound(GLSounds.EVADE.get(), 1f, 1.4f);
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1f, Kind.STAR);
            }
        }
    }

    /** Cash in every luck you've banked: heal for each, and fight stronger and faster for ten seconds. */
    public static final class Jackpot extends Ability {
        Jackpot() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, COLOR).cooldown(300).stamina(10).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return luck(player) > 0 ? Optional.empty() : Optional.of(Component.literal("You have no luck banked"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int stacks = luck(player);
            player.heal(3f * stacks);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, Math.max(0, stacks / 2)));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            setLuck(player, 0);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.4f + stacks * 0.4f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PLAYER_LEVELUP, 1f, 1.4f);
        }
    }

    /** Curse a foe with misfortune: bad luck, and two ailments the dice choose. */
    public static final class Misfortune extends Ability {
        static final List<Holder<MobEffect>> CURSES = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.BLINDNESS,
                MobEffects.POISON, MobEffects.DIG_SLOWDOWN);

        Misfortune() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, GREEN).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 600, 2));
                int first = player.getRandom().nextInt(CURSES.size());
                int second = (first + 1 + player.getRandom().nextInt(CURSES.size() - 1)) % CURSES.size();
                target.addEffect(new MobEffectInstance(CURSES.get(first), 120, 1));
                target.addEffect(new MobEffectInstance(CURSES.get(second), 120, 1));
                Kit.hit(player, target, AbilityDamage.DRAIN, 6f * ctx.power());
                Vfx.burst(ctx.level(), player, target.getEyePosition(), GREEN, 1.2f, Kind.SMOKE, 0);
            });
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.EVOKER_CAST_SPELL, 1f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Go all in: every banked luck is spent on one blast around you, harder for each, and every hit may crit.
     *
     * @name All-In
     */
    public static final class AllIn extends Ability {
        AllIn() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int stacks = luck(player);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, (6f + 3f * stacks) * ctx.power() * crit(player, target));
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.0).add(0, 0.4, 0));
            }
            setLuck(player, 0);
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.STAR, Vfx.RING | Vfx.FLASH | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PLAYER_LEVELUP, 1.4f, 0.8f);
        }
    }

    /** Ride a lucky streak: your luck is full, the world's luck favours you, and you move quicker for thirty seconds. */
    public static final class LuckyStreak extends Ability {
        LuckyStreak() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, GREEN).cooldown(1000).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            setLuck(player, MAX);
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 2));
            Buffs.timed(player, Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("luck_streak_speed"), 0.2,
                    Operation.ADD_MULTIPLIED_TOTAL), 600);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 3f, Kind.STAR, Vfx.FLASH | Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.2f);
        }
    }
}
