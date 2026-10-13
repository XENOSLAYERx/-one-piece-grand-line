package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchF.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Deka Deka no Mi. Grow to an enormous size: every stomp, sweep and thrown boulder lands a third harder while you're
 * grown, and the awakened colossus tramples whatever is underfoot.
 */
public class BigFruit extends DevilFruit {
    public static final int COLOR = 0x7A8A5A;
    public static final int DARK = 0x3A4A2A;
    static final String M = "fruit/grandline:big";

    public static final DeferredHolder<Ability, GiantStomp> GIANT_STOMP = ability("big_giant_stomp", GiantStomp::new);
    public static final DeferredHolder<Ability, Grow> GROW = ability("big_grow", Grow::new);
    public static final DeferredHolder<Ability, GiantSweep> GIANT_SWEEP = ability("big_giant_sweep", GiantSweep::new);
    public static final DeferredHolder<Ability, BoulderToss> BOULDER_TOSS = ability("big_boulder_toss", BoulderToss::new);
    public static final DeferredHolder<Ability, QuakeStep> QUAKE_STEP = ability("big_quake_step", QuakeStep::new);
    public static final DeferredHolder<Ability, Colossus> COLOSSUS = ability("big_colossus", Colossus::new);
    public static final DeferredHolder<Ability, GiantCharge> GIANT_CHARGE = ability("big_giant_charge", GiantCharge::new);

    public static final ResourceLocation BOULDER = GrandLine.id("big_boulder");

    public BigFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Deka Deka no Mi", COLOR)
                .abilities(GIANT_STOMP, GROW, GIANT_SWEEP, BOULDER_TOSS, QUAKE_STEP)
                .awakened(COLOSSUS, GIANT_CHARGE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BOULDER, Projectiles.blast(3, AbilityDamage.BLAST, null,
                p -> Vfx.burst((net.minecraft.server.level.ServerLevel) p.level(), p, p.position(), COLOR, 3f, Kind.SHARD,
                        Vfx.GROUND | Vfx.HEAVY)));
    }

    /** How much harder the user hits while grown (Grow or Colossus). */
    static float size(ServerPlayer player) {
        AbilityState state = AbilityState.of(player);
        if (state.isActive(COLOSSUS.getId())) {
            return 1.6f;
        }
        return state.isActive(GROW.getId()) ? 1.3f : 1f;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Stomp the ground: everything around you is crushed and bounced into the air. */
    public static final class GiantStomp extends Ability {
        GiantStomp() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, COLOR).cooldown(60).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float big = size(player);
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * big * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 9f * big * ctx.power());
                Kit.launch(target, new Vec3(0, 0.7, 0));
            }
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 5f * big, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 0.8f);
        }
    }

    /** Grow to a giant's size: far bigger, longer-reaching, harder-hitting and tougher, though a little slower. */
    public static final class Grow extends Buffs.Toggle {
        Grow() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(200).stamina(14).staminaPerTick(0.15f)
                            .maxActive(900).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("big_grow_scale"), 2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("big_grow_damage"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("big_grow_health"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ENTITY_INTERACTION_RANGE, GrandLine.id("big_grow_reach"), 2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.BLOCK_INTERACTION_RANGE, GrandLine.id("big_grow_block_reach"), 2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.STEP_HEIGHT, GrandLine.id("big_grow_step"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("big_grow_speed"), -0.1, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 3f, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(GLSounds.INFLATE.get(), 1.2f, 0.5f);
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 2f, Kind.SMOKE, 0);
        }
    }

    /** Sweep your huge arm across the ground in front of you, batting everything away. */
    public static final class GiantSweep extends Ability {
        GiantSweep() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, DARK).cooldown(80).stamina(11).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float big = size(player);
            for (LivingEntity target : AbilityDamage.cone(player, 7 * big * ctx.power(), 60)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 11f * big * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.6).add(ctx.right().scale(0.6)).add(0, 0.5, 0));
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(2.5 * big)), ctx.right(), COLOR, 3f * big, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
        }
    }

    /** Rip up a boulder and hurl it: it bursts on whatever it hits. */
    public static final class BoulderToss extends Ability {
        BoulderToss() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            float big = size(ctx.player());
            Kit.shoot(ctx, BOULDER, 1.3, 1.4f * big, 0x6A6A6A, 12f * big, 50).withGravity(0.04);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.STONE_BREAK, 1.6f, 0.5f);
        }
    }

    /** Bring your foot down so hard the ground splits ahead of you: everything along the crack is crushed and slowed. */
    public static final class QuakeStep extends Ability {
        QuakeStep() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, DARK).cooldown(180).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float big = size(player);
            for (LivingEntity target : AbilityDamage.line(player, 12 * big * ctx.power(), 1.6)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 12f * big * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                Kit.launch(target, new Vec3(0, 0.5, 0));
            }
            Vec3 dir = ctx.horizontalLook();
            for (int i = 1; i <= 6; i++) {
                Vec3 at = Kit.ground(ctx.level(), player.position().add(dir.scale(i * 2.0 * big)), 4);
                Vfx.eruption(ctx.level(), player, at, COLOR, 1.4f, Kind.SHARD);
            }
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Grow into a colossus: five times your size, immensely strong, and trampling every foe at your feet. */
    public static final class Colossus extends Buffs.Toggle {
        Colossus() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, DARK).cooldown(900).stamina(32).staminaPerTick(0.35f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("big_colossus_scale"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("big_colossus_damage"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("big_colossus_health"), 20, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ENTITY_INTERACTION_RANGE, GrandLine.id("big_colossus_reach"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.STEP_HEIGHT, GrandLine.id("big_colossus_step"), 2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("big_colossus_kb"), 1, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 6f, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 20 == 0 && player.onGround()) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 4)) {
                    Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
                }
            }
            return true;
        }
    }

    /** Charge like a stampeding giant: everything in your path is flattened and flung aside. */
    public static final class GiantCharge extends Ability {
        GiantCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float big = size(player);
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 16 * ctx.power(), 2.0 * big)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 18f * big * ctx.power());
                Kit.launch(target, ctx.right().scale(target.position().subtract(from).dot(ctx.right()) >= 0 ? 1.2 : -1.2)
                        .add(0, 0.7, 0));
            }
            Kit.dash(player, 2.6, 0.1);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(ctx.horizontalLook().scale(16)).add(0, 1, 0), COLOR, 2f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.7f);
        }
    }
}
