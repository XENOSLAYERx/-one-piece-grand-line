package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Juku Juku no Mi. Ripen anything you touch, and keep going until it rots: crops ripen in a moment, armour crumbles,
 * and living things wither.
 */
public class RipeFruit extends DevilFruit {
    public static final int COLOR = 0xE8A040;
    public static final int ROT = 0x6A3A1A;
    static final String M = "fruit/grandline:ripe";
    static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static final DeferredHolder<Ability, Ripen> RIPEN = ability("ripe_ripen", Ripen::new);
    public static final DeferredHolder<Ability, RottenGround> ROTTEN_GROUND = ability("ripe_rotten_ground", RottenGround::new);
    public static final DeferredHolder<Ability, Harvest> HARVEST = ability("ripe_harvest", Harvest::new);
    public static final DeferredHolder<Ability, RotArmor> ROT_ARMOR = ability("ripe_rot_armor", RotArmor::new);
    public static final DeferredHolder<Ability, Overripe> OVERRIPE = ability("ripe_overripe", Overripe::new);
    public static final DeferredHolder<Ability, AgeOfDecay> AGE_OF_DECAY = ability("ripe_age_of_decay", AgeOfDecay::new);
    public static final DeferredHolder<Ability, RottingField> ROTTING_FIELD = ability("ripe_rotting_field", RottingField::new);

    public static final ResourceLocation GROUND = GrandLine.id("ripe_ground");
    public static final ResourceLocation FIELD = GrandLine.id("ripe_field");

    public RipeFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Juku Juku no Mi", COLOR)
                .abilities(RIPEN, ROTTEN_GROUND, HARVEST, ROT_ARMOR, OVERRIPE)
                .awakened(AGE_OF_DECAY, ROTTING_FIELD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(GROUND, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0));
            }
        }, null));
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(20, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1));
                if (p.tickCount % 60 == 0) {
                    rotArmor(target, 6);
                }
            }
        }, null));
    }

    /** Rots the armour a creature is wearing: each piece loses {@code amount} durability. */
    static void rotArmor(LivingEntity target, int amount) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.hurtAndBreak(amount, target, slot);
            }
        }
    }

    /** Ripens crops and saplings within {@code radius}: one growth step each. @return how many grew */
    static int ripenCrops(ServerLevel level, BlockPos center, int radius) {
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if ((state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)) && state.getBlock() instanceof BonemealableBlock crop
                    && crop.isValidBonemealTarget(level, pos, state)) {
                crop.performBonemeal(level, level.random, pos.immutable(), state);
                grown++;
                if (grown % 4 == 0) {
                    Vfx.impact(level, null, Vec3.atCenterOf(pos), COLOR, 0.8f, Kind.LEAF);
                }
            }
        }
        return grown;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Touch a foe and ripen it past its prime: it is hurt, weakened and slowed. */
    public static final class Ripen extends Ability {
        Ripen() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.5, 45)) {
                Kit.hit(player, target, AbilityDamage.DRAIN, 8f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.2)), ROT, 1.2f, Kind.LEAF);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.COMPOSTER_FILL, 1.2f, 0.8f);
        }
    }

    /** Ripen the ground where you point until it rots: anyone standing on it sickens and slows. */
    public static final class RottenGround extends Ability {
        RottenGround() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, ROT).cooldown(200).stamina(14).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, GROUND, at, 5 * ctx.power(), ROT, 3f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, at, ROT, 5f, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.COMPOSTER_READY, 1.4f, 0.6f);
        }
    }

    /** Ripen every crop and sapling around you in a moment. */
    public static final class Harvest extends Ability {
        Harvest() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, COLOR).cooldown(600).stamina(16).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ripenCrops(ctx.level(), player.blockPosition(), 5);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 5f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BONE_MEAL_USE, 1.4f, 1f);
        }
    }

    /** Rot a foe's armour on its body: every piece crumbles, and it protects far less for ten seconds. */
    public static final class RotArmor extends Ability {
        RotArmor() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, ROT).cooldown(240).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                rotArmor(target, 20);
                Buffs.timed(target, Buffs.mod(Attributes.ARMOR, GrandLine.id("ripe_rot_armor"), -0.5, Operation.ADD_MULTIPLIED_TOTAL),
                        200);
                Kit.hit(player, target, AbilityDamage.DRAIN, 6f * ctx.power());
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), ROT, 1.4f, Kind.SMOKE, 0);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.ITEM_BREAK, 1f, 0.6f);
        }
    }

    /** Ripen a foe until it bursts: a heavy wound, sickness and poison. */
    public static final class Overripe extends Ability {
        Overripe() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(260).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 8).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 8).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DRAIN, 12f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                Vfx.burst(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 2f, Kind.DROP, Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1.6f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Bring an age of decay on everyone around you: their armour crumbles, their strength fades, their bodies wither. */
    public static final class AgeOfDecay extends Ability {
        AgeOfDecay() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, ROT).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                rotArmor(target, 12);
                Kit.hit(player, target, AbilityDamage.DRAIN, 8f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), ROT, 10f, Kind.SMOKE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SAND_DRAIN.get(), 1.6f, 0.6f);
        }
    }

    /**
     * Spread ripening through the land around you for ten seconds: crops leap up, and every foe inside rots where it
     * stands.
     */
    public static final class RottingField extends Ability {
        RottingField() {
            super(props(ActivationType.INSTANT, AbilityIcon.LEAF, ROT).cooldown(900).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, FIELD);
            AbilityProjectile.zone(player, FIELD, player.position(), 10 * ctx.power(), ROT, 4f * ctx.power(), 200);
            ripenCrops(ctx.level(), player.blockPosition(), 8);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 10f, Kind.LEAF, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.COMPOSTER_READY, 2f, 0.5f);
        }
    }
}
