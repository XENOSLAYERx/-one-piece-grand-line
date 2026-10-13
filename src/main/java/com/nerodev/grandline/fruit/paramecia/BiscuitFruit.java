package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

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
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Bisu Bisu no Mi. Clap your hands and make biscuits, hard as iron: soldiers to fight for you, armour to hide in, walls
 * and spears, and finally a biscuit giant.
 */
public class BiscuitFruit extends DevilFruit {
    public static final int COLOR = 0xE0B070;
    public static final int CRUST = 0x8A5A2A;
    static final String M = "fruit/grandline:biscuit";
    public static final ResourceLocation SOLDIER = GrandLine.id("biscuit_soldier");

    public static final DeferredHolder<Ability, BiscuitSoldiers> SOLDIERS = ability("biscuit_soldiers", BiscuitSoldiers::new);
    public static final DeferredHolder<Ability, Pretzel> PRETZEL = ability("biscuit_pretzel", Pretzel::new);
    public static final DeferredHolder<Ability, BiscuitArmor> BISCUIT_ARMOR = ability("biscuit_armor", BiscuitArmor::new);
    public static final DeferredHolder<Ability, BiscuitWall> BISCUIT_WALL = ability("biscuit_wall", BiscuitWall::new);
    public static final DeferredHolder<Ability, SpearVolley> SPEAR_VOLLEY = ability("biscuit_spear_volley", SpearVolley::new);
    public static final DeferredHolder<Ability, Legion> LEGION = ability("biscuit_legion", Legion::new);
    public static final DeferredHolder<Ability, BiscuitGiant> BISCUIT_GIANT = ability("biscuit_giant", BiscuitGiant::new);

    public static final ResourceLocation SPEAR = GrandLine.id("biscuit_spear");

    public BiscuitFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Bisu Bisu no Mi", COLOR)
                .abilities(SOLDIERS, PRETZEL, BISCUIT_ARMOR, BISCUIT_WALL, SPEAR_VOLLEY)
                .awakened(LEGION, BISCUIT_GIANT));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(SPEAR, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    /** Biscuit soldiers: hard-baked warriors with spears that never tire. */
    static void bake(ServerPlayer player, int count, int ticks, boolean strong) {
        for (Vec3 at : Kit.circle(player.position(), 2.2, count, player.getYRot() * Math.PI / 180)) {
            Minions.spawn(player, SOLDIER, EntityType.HUSK, at, ticks, mob -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
                mob.setDropChance(EquipmentSlot.CHEST, 0f);
                if (strong) {
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
                }
            });
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, SOLDIER);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Clap out two biscuit soldiers that fight for you for twenty seconds. */
    public static final class BiscuitSoldiers extends Ability {
        BiscuitSoldiers() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(400).stamina(18).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            bake(ctx.player(), 2, 400, false);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.CROP_BREAK, 1.4f, 0.6f);
        }
    }

    /** Swing Pretzel, your sword, in a wide arc. */
    public static final class Pretzel extends Ability {
        Pretzel() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, CRUST).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * ctx.power(), 55)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.8)), player.getLookAngle(), COLOR, 2.4f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 0.9f);
        }
    }

    /** Bake a suit of biscuit armour around yourself: very hard, and it crumbles when it ends. */
    public static final class BiscuitArmor extends Buffs.Toggle {
        BiscuitArmor() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(500).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("biscuit_armor_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("biscuit_armor_toughness"), 2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.SCALE, GrandLine.id("biscuit_armor_scale"), 0.15, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.BISCUIT, 8);
            ctx.sound(SoundEvents.CROP_BREAK, 1.2f, 0.5f);
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.6f, Kind.SHARD, 0);
        }
    }

    /** Bake a wall of hard biscuit in front of you. */
    public static final class BiscuitWall extends Ability {
        BiscuitWall() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, CRUST).cooldown(160).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 center = player.position().add(dir.scale(3));
            Kit.place(ctx.level(), Kit.wall(center, dir, 7, 4), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 220);
            Vfx.burst(ctx.level(), player, center.add(0, 1.5, 0), COLOR, 3f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.CROP_BREAK, 1.6f, 0.5f);
        }
    }

    /** Clap out a volley of biscuit spears and hurl them through the ranks in front of you. */
    public static final class SpearVolley extends Ability {
        SpearVolley() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(90).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.1)).normalize();
                AbilityProjectile.spawn(player, SPEAR, ctx.handPos(), dir.scale(1.8), 0.4f, COLOR, 6f * ctx.power(), 25)
                        .withGravity(0.015);
            }
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.9f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Clap out a legion: six hardened biscuit soldiers, stronger and tougher than the usual batch. */
    public static final class Legion extends Ability {
        Legion() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, CRUST).cooldown(900).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), SOLDIER);
            bake(ctx.player(), 6, 500, true);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.CROP_BREAK, 2f, 0.4f);
        }
    }

    /** Bake a towering biscuit giant that smashes your foes for fifteen seconds. */
    public static final class BiscuitGiant extends Ability {
        BiscuitGiant() {
            super(props(ActivationType.INSTANT, AbilityIcon.TRANSFORM, COLOR).cooldown(900).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = player.position().add(ctx.horizontalLook().scale(3));
            Minions.spawn(player, SOLDIER, EntityType.IRON_GOLEM, at, 300,
                    golem -> golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1)));
            Vfx.eruption(ctx.level(), player, at, COLOR, 4f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.IRON_GOLEM_REPAIR, 1.4f, 0.6f);
        }
    }
}
