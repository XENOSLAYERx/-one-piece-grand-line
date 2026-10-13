package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchG.props;

import com.nerodev.grandline.GrandLine;
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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Ton Ton no Mi. Grow heavier by the ton at will, as Machvise does: fists that land like anvils, drops that flatten
 * whatever is underneath, foes pinned under a weight they cannot lift, and a body nothing can push.
 */
public class TonFruit extends DevilFruit {
    public static final int COLOR = 0x6A6A7A;
    public static final int DUST = 0x9A8A6A;
    static final String M = "fruit/grandline:ton";
    static final String METEOR_MARK = "grandline_ton_meteor";

    static final List<Buffs.Mod> HEAVY = List.of(
            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("ton_heavy_knockback"), 1, Operation.ADD_VALUE),
            Buffs.mod(Attributes.ARMOR, GrandLine.id("ton_heavy_armor"), 6, Operation.ADD_VALUE),
            Buffs.mod(Attributes.GRAVITY, GrandLine.id("ton_heavy_gravity"), 0.06, Operation.ADD_VALUE),
            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("ton_heavy_speed"), -0.35, Operation.ADD_MULTIPLIED_TOTAL));
    static final Buffs.Mod METEOR_GRAVITY = Buffs.mod(Attributes.GRAVITY, GrandLine.id("ton_meteor_gravity"), 0.3, Operation.ADD_VALUE);
    static final Buffs.Mod CRUSHED = Buffs.mod(Attributes.JUMP_STRENGTH, GrandLine.id("ton_crushed_jump"), -1, Operation.ADD_MULTIPLIED_TOTAL);

    public static final DeferredHolder<Ability, TonFist> TON_FIST = ability("ton_fist", TonFist::new);
    public static final DeferredHolder<Ability, FiveTonDrop> FIVE_TON_DROP = ability("ton_five_ton_drop", FiveTonDrop::new);
    public static final DeferredHolder<Ability, HeavyBody> HEAVY_BODY = ability("ton_heavy_body", HeavyBody::new);
    public static final DeferredHolder<Ability, TenTonPin> TEN_TON_PIN = ability("ton_ten_ton_pin", TenTonPin::new);
    public static final DeferredHolder<Ability, HammerFall> HAMMER_FALL = ability("ton_hammer_fall", HammerFall::new);
    public static final DeferredHolder<Ability, ThousandTonMeteor> METEOR = ability("ton_thousand_ton_meteor", ThousandTonMeteor::new);
    public static final DeferredHolder<Ability, ImmovableMountain> MOUNTAIN = ability("ton_immovable_mountain", ImmovableMountain::new);

    public TonFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Ton Ton no Mi", COLOR)
                .abilities(TON_FIST, FIVE_TON_DROP, HEAVY_BODY, TEN_TON_PIN, HAMMER_FALL)
                .awakened(METEOR, MOUNTAIN));
    }

    static void init() {}

    static void behaviors() {}

    /** Crushes everything around {@code at}: damage, a stagger, and it is pressed flat for a moment. */
    static void crush(ServerPlayer player, Vec3 at, double radius, float damage, int pinned) {
        for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
            Kit.hit(player, target, AbilityDamage.CRUSH, damage);
            target.setDeltaMovement(0, -0.8, 0);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            if (pinned > 0) {
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, pinned, 0, false, false, true));
            }
        }
        Vfx.burst(player.serverLevel(), player, at.add(0, 0.1, 0), DUST, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
    }

    /**
     * Landing heavy: with Heavy Body or Immovable Mountain held, a fall of more than three blocks costs the user nothing
     * and crushes everything around the landing; a Thousand-Ton Meteor lands far harder still.
     */
    @Override
    public void onFall(ServerPlayer user, LivingFallEvent event) {
        boolean meteor = user.getPersistentData().getBoolean(METEOR_MARK);
        AbilityState state = AbilityState.of(user);
        boolean heavy = state.isActive(HEAVY_BODY.getId()) || state.isActive(MOUNTAIN.getId());
        if (meteor) {
            user.getPersistentData().remove(METEOR_MARK);
            Buffs.remove(user, METEOR_GRAVITY);
            event.setDamageMultiplier(0f);
            crush(user, user.position(), 9, Math.min(32f, 14f + event.getDistance()), 40);
            Vfx.eruption(user.serverLevel(), user, user.position(), DUST, 5f, Kind.SHARD);
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(), GLSounds.GIANT_IMPACT.get(), SoundSource.PLAYERS, 2f, 0.4f);
        } else if (heavy && event.getDistance() > 3) {
            event.setDamageMultiplier(0f);
            crush(user, user.position(), Math.min(5, 2 + event.getDistance() * 0.2), Math.min(22f, 4f + event.getDistance() * 1.5f), 0);
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(), GLSounds.GIANT_IMPACT.get(), SoundSource.PLAYERS, 1.2f, 0.7f);
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        Buffs.remove(player, HEAVY);
        Buffs.remove(player, METEOR_GRAVITY);
        player.getPersistentData().remove(METEOR_MARK);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Make your fist weigh a ton as it lands: a crushing blow that leaves the foe staggering under the weight.
     *
     * @name Ton Fist
     */
    public static final class TonFist extends Ability {
        TonFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 13f * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.2).add(0, -0.2, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), DUST, 1.4f, Kind.SHARD);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.6f);
        }
    }

    /**
     * Drop onto the foe you look at from above with five tons behind you: it is flattened and dazed.
     *
     * @name Five-Ton Drop
     */
    public static final class FiveTonDrop extends Ability {
        FiveTonDrop() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(100).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No one to drop on"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 at = target.position();
                player.teleportTo(at.x, at.y + target.getBbHeight(), at.z);
                player.resetFallDistance();
                crush(player, at, 3, 15f * ctx.power(), 20);
                Vfx.trail(ctx.level(), player, from.add(0, 4, 0), at.add(0, 1, 0), COLOR, 1.2f, Kind.STREAK);
            });
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 0.9f);
        }
    }

    /**
     * Hold yourself at ten tons: nothing can push you, blows dent you less and long falls only hurt whatever you land on,
     * but you move slowly.
     */
    public static final class HeavyBody extends Ability {
        HeavyBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(100).stamina(10).staminaPerTick(0.04f).maxActive(1200)
                    .mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Buffs.apply(ctx.player(), HEAVY);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), DUST, 2f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            if (!AbilityState.of(ctx.player()).isActive(MOUNTAIN.getId())) {
                Buffs.remove(ctx.player(), HEAVY);
            }
            Fx.stopAnimation(ctx.player());
        }
    }

    /**
     * Pin the foe you face under ten tons: it cannot move or jump for three seconds while the weight grinds it down.
     *
     * @name Ten-Ton Pin
     */
    public static final class TenTonPin extends Ability {
        TenTonPin() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, DUST).cooldown(200).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 16f * ctx.power());
                Kit.root(target, Vfx.StatusLook.STONE, 60);
                Buffs.timed(target, CRUSHED, 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                Vfx.burst(ctx.level(), player, target.position(), DUST, 1.6f, Kind.SHARD, Vfx.GROUND);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.1f);
        }
    }

    /**
     * Leap and come down on the spot you point at as a hundred-ton hammer: a crater, and everything there knocked flat.
     *
     * @name Hundred-Ton Hammer
     */
    public static final class HammerFall extends Ability {
        HammerFall() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, DUST).cooldown(240).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 10);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            crush(player, at, 5 * ctx.power(), 22f * ctx.power(), 30);
            Vfx.trail(ctx.level(), player, from.add(0, 4, 0), at.add(0, 1, 0), COLOR, 1.4f, Kind.STREAK);
            Vfx.eruption(ctx.level(), player, at, DUST, 3f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.8f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Launch yourself high, then turn to a thousand tons: wherever you land is crushed for nine blocks around.
     *
     * @name Thousand-Ton Meteor
     */
    public static final class ThousandTonMeteor extends Ability {
        ThousandTonMeteor() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.getPersistentData().putBoolean(METEOR_MARK, true);
            Kit.launch(player, ctx.horizontalLook().scale(0.8).add(0, 2.4, 0));
            Buffs.timed(player, METEOR_GRAVITY, 200);
            Vfx.burst(ctx.level(), player, player.position(), DUST, 2.5f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.3f);
        }
    }

    /**
     * Become an immovable mountain for twenty seconds: blows lose half their force, and your weight drags at every
     * foe near you so they can barely move or jump.
     */
    public static final class ImmovableMountain extends Ability {
        ImmovableMountain() {
            super(props(ActivationType.TOGGLE, AbilityIcon.WALL, DUST).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Buffs.apply(ctx.player(), HEAVY);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), DUST, 6f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 10 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 6)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                    Buffs.timed(target, CRUSHED, 20);
                }
                Vfx.burst(ctx.level(), player, player.position(), DUST, 6f, Kind.RING, Vfx.GROUND);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.5f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            if (!AbilityState.of(ctx.player()).isActive(HEAVY_BODY.getId())) {
                Buffs.remove(ctx.player(), HEAVY);
            }
            Fx.stopAnimation(ctx.player());
        }
    }
}
