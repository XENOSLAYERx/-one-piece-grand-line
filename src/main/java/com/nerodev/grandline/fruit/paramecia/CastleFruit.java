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
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * Shiro Shiro no Mi. The user's body is a fortress: cannons fire from it, a garrison marches out of it, and it can grow
 * into a walking castle.
 */
public class CastleFruit extends DevilFruit {
    public static final int COLOR = 0x8A7A6A;
    public static final int IRON = 0x3A3A3A;
    static final String M = "fruit/grandline:castle";

    public static final DeferredHolder<Ability, CannonVolley> CANNON_VOLLEY = ability("castle_cannon_volley", CannonVolley::new);
    public static final DeferredHolder<Ability, Garrison> GARRISON = ability("castle_garrison", Garrison::new);
    public static final DeferredHolder<Ability, FortressBody> FORTRESS_BODY = ability("castle_fortress_body", FortressBody::new);
    public static final DeferredHolder<Ability, Rampart> RAMPART = ability("castle_rampart", Rampart::new);
    public static final DeferredHolder<Ability, SiegeCannon> SIEGE_CANNON = ability("castle_siege_cannon", SiegeCannon::new);
    public static final DeferredHolder<Ability, Bombardment> BOMBARDMENT = ability("castle_bombardment", Bombardment::new);
    public static final DeferredHolder<Ability, BigFather> BIG_FATHER = ability("castle_big_father", BigFather::new);

    public static final ResourceLocation CANNONBALL = GrandLine.id("castle_cannonball");
    public static final ResourceLocation SIEGE = GrandLine.id("castle_siege");
    public static final ResourceLocation BARRAGE = GrandLine.id("castle_barrage");
    public static final ResourceLocation SOLDIER = GrandLine.id("castle_soldier");

    public CastleFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Shiro Shiro no Mi", COLOR)
                .abilities(CANNON_VOLLEY, GARRISON, FORTRESS_BODY, RAMPART, SIEGE_CANNON)
                .awakened(BOMBARDMENT, BIG_FATHER));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(CANNONBALL, Projectiles.blast(2, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), 0xFFB060, 2f, Kind.SMOKE, Vfx.FLASH)));
        ProjectileBehavior.Registry.register(SIEGE, Projectiles.blast(4, AbilityDamage.BLAST, null,
                p -> Vfx.burst((ServerLevel) p.level(), p, p.position(), 0xFFB060, 4f, Kind.SMOKE,
                        Vfx.FLASH | Vfx.HEAVY | Vfx.RING)));
        ProjectileBehavior.Registry.register(BARRAGE, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.level().random.nextFloat() < 0.5f) {
                Kit.hit(owner, target, AbilityDamage.BLAST, p.damage());
                Vfx.impact((ServerLevel) p.level(), p, target.position().add(0, 0.5, 0), 0xFFB060, 1.6f,
                        Kind.SMOKE);
            }
        }, null));
    }

    /** Fires one cannonball from the user's chest toward {@code dir}. */
    static void fire(ServerPlayer player, Vec3 dir, float damage, float power) {
        Vec3 from = player.getEyePosition().add(0, -0.4, 0).add(dir.scale(0.8));
        AbilityProjectile.spawn(player, CANNONBALL, from, dir.scale(1.6), 0.5f, IRON, damage, 40).withGravity(0.03).withPower(power);
    }

    /** Musket-armed soldiers march out of the castle; helmets keep the sun off them. */
    static void muster(ServerPlayer player, int count, int ticks) {
        for (Vec3 at : Kit.circle(player.position(), 2, count, player.getYRot() * Math.PI / 180)) {
            Minions.spawn(player, SOLDIER, EntityType.SKELETON, at, ticks, mob -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
                mob.setDropChance(EquipmentSlot.HEAD, 0f);
            });
        }
    }

    @Override
    public void onLost(ServerPlayer player) {
        Minions.dismiss(player, SOLDIER);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Open the gun ports in your body and fire a spread of three cannonballs. */
    public static final class CannonVolley extends Ability {
        CannonVolley() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, IRON).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                fire(player, player.getLookAngle().add(ctx.right().scale(i * 0.12)).normalize(), 7f * ctx.power(), ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(player.getLookAngle()), 0xFFB060, 1.2f, Kind.SMOKE, 0);
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.4f);
        }
    }

    /** Let your garrison march out: three musket soldiers fight for you for twenty seconds. */
    public static final class Garrison extends Ability {
        Garrison() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, COLOR).cooldown(500).stamina(18).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), SOLDIER);
            muster(ctx.player(), 3, 400);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 3f, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.IRON_DOOR_OPEN, 1.2f, 0.6f);
        }
    }

    /** Harden into a fortress: much tougher and immovable, but slower. */
    public static final class FortressBody extends Buffs.Toggle {
        FortressBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("castle_fortress_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("castle_fortress_toughness"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("castle_fortress_kb"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.SCALE, GrandLine.id("castle_fortress_scale"), 0.2, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("castle_fortress_speed"), -0.25,
                                    Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.STONE, 6);
            ctx.sound(SoundEvents.STONE_PLACE, 1.4f, 0.5f);
        }
    }

    /** Raise castle ramparts in a ring around you for twelve seconds. */
    public static final class Rampart extends Ability {
        Rampart() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(300).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            BlockPos center = player.blockPosition();
            Kit.place(ctx.level(), Kit.ring(center, 4, 3), Blocks.STONE_BRICKS.defaultBlockState(), 240);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 4.5f, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.STONE_PLACE, 1.6f, 0.6f);
        }
    }

    /** Fire the great siege cannon: one huge shell that blasts a wide area. */
    public static final class SiegeCannon extends Ability {
        SiegeCannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, IRON).cooldown(160).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.shoot(ctx, SIEGE, 1.5, 1.1f, IRON, 14f, 50).withGravity(0.02);
            Kit.launch(player, player.getLookAngle().scale(-0.5));
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(player.getLookAngle().scale(1.5)), 0xFFB060, 2f, Kind.SMOKE,
                    Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Call down a bombardment from every gun in the castle on the spot you point at, for five seconds. */
    public static final class Bombardment extends Ability {
        Bombardment() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, IRON).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 32), 8);
            AbilityProjectile.zone(player, BARRAGE, at, 7 * ctx.power(), IRON, 6f * ctx.power(), 100);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.6f);
        }
    }

    /**
     * Become Big Father: a walking castle twice your size, heavily armoured, whose cannons fire on the nearest foe every
     * second and a half.
     */
    public static final class BigFather extends Buffs.Toggle {
        BigFather() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(900).stamina(30).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("castle_big_father_scale"), 1.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("castle_big_father_armor"), 14, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("castle_big_father_damage"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("castle_big_father_kb"), 1, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Minions.dismiss(ctx.player(), SOLDIER);
            muster(ctx.player(), 4, 400);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 5f, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_GEAR);
            ctx.sound(SoundEvents.IRON_GOLEM_REPAIR, 1.6f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 30 == 0) {
                LivingEntity target = Projectiles.nearest(player, player.position(), 20);
                if (target != null) {
                    Vec3 dir = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
                    if (dir.lengthSqr() > 1.0E-4) {
                        fire(player, dir.normalize(), 8f * ctx.power(), ctx.power());
                        ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.3f);
                    }
                }
            }
            return true;
        }
    }
}
