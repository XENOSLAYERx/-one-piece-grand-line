package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Ishi Ishi no Mi. Merge with stone and command it: fists and spikes of rock out of the ground, walls on demand, and
 * a stone giant as tall as a castle.
 */
public class StoneFruit extends DevilFruit {
    public static final int COLOR = 0x8A8A80;
    public static final int DARK = 0x5A5A50;
    static final String M = "fruit/grandline:stone";

    public static final DeferredHolder<Ability, StoneFist> STONE_FIST = ability("stone_fist", StoneFist::new);
    public static final DeferredHolder<Ability, StoneWall> STONE_WALL = ability("stone_wall", StoneWall::new);
    public static final DeferredHolder<Ability, Merge> MERGE = ability("stone_merge", Merge::new);
    public static final DeferredHolder<Ability, SpikeLine> SPIKE_LINE = ability("stone_spike_line", SpikeLine::new);
    public static final DeferredHolder<Ability, StoneSkin> STONE_SKIN = ability("stone_skin", StoneSkin::new);
    public static final DeferredHolder<Ability, StoneGiant> STONE_GIANT = ability("stone_giant", StoneGiant::new);
    public static final DeferredHolder<Ability, CrushingHands> CRUSHING_HANDS = ability("stone_crushing_hands", CrushingHands::new);

    public StoneFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.EPIC, "Ishi Ishi no Mi", COLOR)
                .abilities(STONE_FIST, STONE_WALL, MERGE, SPIKE_LINE, STONE_SKIN)
                .awakened(STONE_GIANT, CRUSHING_HANDS));
    }

    static void init() {}

    static void behaviors() {}

    // ---------------------------------------------------------------------------------------------------------

    /** A fist of stone punches up out of the ground where you point. */
    public static final class StoneFist extends Ability {
        StoneFist() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(60).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 22), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 2.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 10f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.1, 0));
            }
            Vfx.eruption(ctx.level(), player, at, COLOR, 3.5f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.STONE_BREAK, 1.4f, 0.6f);
        }
    }

    /** Raise a wall of stone out of the ground in front of you. */
    public static final class StoneWall extends Ability {
        StoneWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, DARK).cooldown(160).stamina(14).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 5), Blocks.COBBLESTONE.defaultBlockState(), 220);
            Vfx.burst(ctx.level(), player, center, COLOR, 3.5f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.STONE_PLACE, 1.6f, 0.5f);
        }
    }

    /** Sink into the stone itself: hidden and nearly untouchable, though slow, until you step back out. */
    public static final class Merge extends Ability {
        Merge() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(240).stamina(14).staminaPerTick(0.2f)
                    .maxActive(200).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24), m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 1.5f, Kind.SHARD, Vfx.GROUND);
            ctx.sound(SoundEvents.STONE_BREAK, 1f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 3, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), player, player.position().add(0, 0.1, 0), COLOR, 0.8f, Kind.SHARD);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ctx.player().removeEffect(MobEffects.INVISIBILITY);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 1.5f, Kind.SHARD, Vfx.GROUND);
        }
    }

    /** A line of stone spikes bursts out of the ground ahead of you, tossing everyone on it. */
    public static final class SpikeLine extends Ability {
        SpikeLine() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(120).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 1.4)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.0, 0));
            }
            Vec3 dir = ctx.horizontalLook();
            for (int i = 2; i <= range; i += 2) {
                Vec3 at = Kit.ground(ctx.level(), player.position().add(dir.scale(i)).add(0, 2, 0), 6);
                Vfx.eruption(ctx.level(), player, at, COLOR, 2f, Kind.SHARD);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.STONE_BREAK, 1.6f, 0.7f);
        }
    }

    /** Harden your skin into stone: far tougher, slower on your feet. */
    public static final class StoneSkin extends Buffs.Toggle {
        StoneSkin() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, DARK).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(500).mastery(M, 35).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("stone_skin_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("stone_skin_toughness"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("stone_skin_knockback"), 0.6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("stone_skin_speed"), -0.2, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.STONE, 6);
            ctx.sound(SoundEvents.STONE_PLACE, 1.2f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Merge with the land and rise as a stone giant: towering, mighty and with an enormous reach. */
    public static final class StoneGiant extends Buffs.Toggle {
        StoneGiant() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(900).stamina(34).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("stone_giant_scale"), 2.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("stone_giant_damage"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("stone_giant_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ENTITY_INTERACTION_RANGE, GrandLine.id("stone_giant_reach"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("stone_giant_knockback"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("stone_giant_speed"), -0.1, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.eruption(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 8f, Kind.SHARD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 20 == 0 && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.003) {
                Vfx.burst(ctx.level(), player, player.position(), COLOR, 2.5f, Kind.SHARD, Vfx.GROUND);
            }
            return true;
        }
    }

    /** Two giant hands of stone rise on either side of the spot you point at and clap together. */
    public static final class CrushingHands extends Ability {
        CrushingHands() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, DARK).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 8);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 20f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
            Vec3 side = ctx.right().scale(3);
            Vfx.eruption(ctx.level(), player, at.add(side), COLOR, 7f, Kind.SHARD);
            Vfx.eruption(ctx.level(), player, at.subtract(side), COLOR, 7f, Kind.SHARD);
            Vfx.burst(ctx.level(), player, at.add(0, 3, 0), COLOR, 5f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.7f);
        }
    }
}
