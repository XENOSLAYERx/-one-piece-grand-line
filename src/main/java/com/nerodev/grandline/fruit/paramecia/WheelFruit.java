package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

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
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Shari Shari no Mi. Arms and legs turn into wheels: race along the ground, grind foes with spinning arms, and roll
 * right over anything in the way.
 */
public class WheelFruit extends DevilFruit {
    public static final int COLOR = 0x6A6A72;
    public static final int RIM = 0xE8C040;
    static final String M = "fruit/grandline:wheel";

    public static final DeferredHolder<Ability, WheelLegs> WHEEL_LEGS = ability("wheel_legs", WheelLegs::new);
    public static final DeferredHolder<Ability, Ram> RAM = ability("wheel_ram", Ram::new);
    public static final DeferredHolder<Ability, Grinder> GRINDER = ability("wheel_grinder", Grinder::new);
    public static final DeferredHolder<Ability, WheelThrow> WHEEL_THROW = ability("wheel_throw", WheelThrow::new);
    public static final DeferredHolder<Ability, Drift> DRIFT = ability("wheel_drift", Drift::new);
    public static final DeferredHolder<Ability, RoadRoller> ROAD_ROLLER = ability("wheel_road_roller", RoadRoller::new);
    public static final DeferredHolder<Ability, WheelStorm> WHEEL_STORM = ability("wheel_storm", WheelStorm::new);

    public static final ResourceLocation WHEEL = GrandLine.id("wheel_wheel");

    public WheelFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Shari Shari no Mi", COLOR)
                .abilities(WHEEL_LEGS, RAM, GRINDER, WHEEL_THROW, DRIFT)
                .awakened(ROAD_ROLLER, WHEEL_STORM));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WHEEL, Projectiles.piercing(AbilityDamage.SLASH,
                (p, target) -> AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 0.6, 0.2)));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Turn your legs into wheels and race along the ground, rolling up steps as you go. */
    public static final class WheelLegs extends Buffs.Toggle {
        WheelLegs() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH, RIM).cooldown(60).stamina(8).staminaPerTick(0.12f)
                            .maxActive(1200).masteryGain(0.6),
                    List.of(Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("wheel_legs_speed"), 0.6, Operation.ADD_MULTIPLIED_BASE),
                            Buffs.mod(Attributes.STEP_HEIGHT, GrandLine.id("wheel_legs_step"), 0.6, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(SoundEvents.MINECART_RIDING, 0.6f, 1.4f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (ctx.activeTicks() % 3 == 0 && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                Vfx.impact(ctx.level(), player, player.position().add(0, 0.1, 0), COLOR, 0.5f, Kind.SMOKE);
            }
            return true;
        }
    }

    /** Charge forward on your wheels and ram straight through a line of foes. */
    public static final class Ram extends Ability {
        Ram() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 10 * ctx.power(), 1.1)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.4, 0.4);
            }
            Vec3 start = player.position().add(0, 0.3, 0);
            Kit.launch(player, ctx.horizontalLook().scale(1.8).add(0, 0.1, 0));
            Vfx.trail(ctx.level(), player, start, start.add(ctx.horizontalLook().scale(10)), COLOR, 0.6f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(SoundEvents.MINECART_RIDING, 1.2f, 1.8f);
        }
    }

    /** Spin your arms as grinding wheels and shred whatever is in front of you. */
    public static final class Grinder extends Ability {
        Grinder() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, RIM).cooldown(50).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
            }
            Vfx.spiral(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.4)), RIM, 1.2f, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(SoundEvents.GRINDSTONE_USE, 1.2f, 1.2f);
        }
    }

    /** Fling a spinning wheel that cuts through every foe in its path. */
    public static final class WheelThrow extends Ability {
        WheelThrow() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, RIM).cooldown(60).stamina(10).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.spawn(player, WHEEL, ctx.handPos(), ctx.horizontalLook().scale(1.3), 0.9f, RIM, 7f * ctx.power(), 30);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.4f);
        }
    }

    /** Hold to spin out in tight circles, battering everything around you. */
    public static final class Drift extends Ability {
        Drift() {
            super(props(ActivationType.HOLD, AbilityIcon.TORNADO, COLOR).cooldown(140).stamina(12).staminaPerTick(0.3f)
                    .maxActive(50).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_SWEEP);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.around(player, player.position().add(0, 1, 0), 3.2 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                    AbilityDamage.knockback(target, player.position(), 0.5, 0.2);
                }
                Vfx.spiral(ctx.level(), player, player.position().add(0, 0.3, 0), COLOR, 2.5f, Kind.SMOKE);
                ctx.sound(SoundEvents.MINECART_RIDING, 0.5f, 2f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Become one giant wheel and roll over everything in front of you. */
    public static final class RoadRoller extends Buffs.Toggle {
        RoadRoller() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, RIM).cooldown(600).stamina(28).staminaPerTick(0.3f)
                            .maxActive(300).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("wheel_roller_scale"), 0.5, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("wheel_roller_speed"), 0.4, Operation.ADD_MULTIPLIED_BASE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("wheel_roller_knockback"), 1, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.STEP_HEIGHT, GrandLine.id("wheel_roller_step"), 1, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            ctx.sound(SoundEvents.MINECART_RIDING, 1.2f, 0.6f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 8 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.cone(player, 3 * ctx.power(), 60)) {
                    Kit.hit(player, target, AbilityDamage.CRUSH, 6f * ctx.power());
                    AbilityDamage.knockback(target, player.position(), 1.2, 0.3);
                }
                Vfx.impact(ctx.level(), player, player.position().add(0, 0.2, 0), COLOR, 1.2f, Kind.SMOKE);
            }
            return true;
        }
    }

    /** Six wheels fly out from you in every direction, cutting through everyone around. */
    public static final class WheelStorm extends Ability {
        WheelStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, RIM).cooldown(500).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (Vec3 at : Kit.circle(Vec3.ZERO, 1, 8, player.getYRot() * Math.PI / 180)) {
                AbilityProjectile.spawn(player, WHEEL, player.position().add(0, 1, 0).add(at), at.scale(1.2), 1.0f, RIM,
                        10f * ctx.power(), 30);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.8f);
        }
    }
}
