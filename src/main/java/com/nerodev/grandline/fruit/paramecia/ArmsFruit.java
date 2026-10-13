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
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Buki Buki no Mi. Any part of the body can become any weapon: blade arms, gun fingers, missile launchers, an axe for
 * a leg, armour plates, and finally the whole body as a cannon.
 */
public class ArmsFruit extends DevilFruit {
    public static final int COLOR = 0x5A6A5A;
    public static final int BRASS = 0xE8C040;
    static final String M = "fruit/grandline:arms";

    public static final DeferredHolder<Ability, BladeArm> BLADE_ARM = ability("arms_blade_arm", BladeArm::new);
    public static final DeferredHolder<Ability, GunFingers> GUN_FINGERS = ability("arms_gun_fingers", GunFingers::new);
    public static final DeferredHolder<Ability, Missile> MISSILE = ability("arms_missile", Missile::new);
    public static final DeferredHolder<Ability, AxeLeg> AXE_LEG = ability("arms_axe_leg", AxeLeg::new);
    public static final DeferredHolder<Ability, ArmourPlates> ARMOUR_PLATES = ability("arms_armour_plates", ArmourPlates::new);
    public static final DeferredHolder<Ability, Arsenal> ARSENAL = ability("arms_arsenal", Arsenal::new);
    public static final DeferredHolder<Ability, BodyCannon> BODY_CANNON = ability("arms_body_cannon", BodyCannon::new);

    public static final ResourceLocation BULLET = GrandLine.id("arms_bullet");
    public static final ResourceLocation ROCKET = GrandLine.id("arms_missile");
    public static final ResourceLocation SHELL = GrandLine.id("arms_cannon_shell");

    public ArmsFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Buki Buki no Mi", COLOR)
                .abilities(BLADE_ARM, GUN_FINGERS, MISSILE, AXE_LEG, ARMOUR_PLATES)
                .awakened(ARSENAL, BODY_CANNON));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BULLET, Projectiles.bolt(AbilityDamage.STRIKE, null));
        ProjectileBehavior.Registry.register(ROCKET, Projectiles.homing(18, 0.12, Projectiles.blast(3, AbilityDamage.BLAST, null,
                ArmsFruit::explosion)));
        ProjectileBehavior.Registry.register(SHELL, Projectiles.blast(5, AbilityDamage.BLAST,
                (p, target) -> AbilityDamage.knockback(target, p.position(), 1.8, 0.5), ArmsFruit::explosion));
    }

    static void explosion(AbilityProjectile p) {
        if (p.level() instanceof ServerLevel level) {
            Vfx.burst(level, p, p.position(), 0xFF8A2A, p.size() + 2f, Kind.FLAME, Vfx.SHOCKWAVE | Vfx.FLASH);
            Vfx.burst(level, p, p.position(), COLOR, p.size() + 1.5f, Kind.SMOKE, 0);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Turn your arm into a sword and cut through everything in front of you. */
    public static final class BladeArm extends Ability {
        BladeArm() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.2 * ctx.power(), 50)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), player.getLookAngle(), 0xE8F0F8, 2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        }
    }

    /** Hold to turn your fingers into gun barrels and fire a stream of bullets. */
    public static final class GunFingers extends Ability {
        GunFingers() {
            super(props(ActivationType.HOLD, AbilityIcon.BEAM, BRASS).cooldown(80).stamina(8).staminaPerTick(0.3f).maxActive(40)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 == 0) {
                ServerPlayer player = ctx.player();
                Vec3 spread = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian(),
                        player.getRandom().nextGaussian()).scale(0.03);
                AbilityProjectile.spawn(player, BULLET, ctx.handPos(), player.getLookAngle().add(spread).normalize().scale(2.8), 0.2f,
                        BRASS, 2.5f * ctx.power(), 15);
                ctx.sound(SoundEvents.DISPENSER_LAUNCH, 0.5f, 1.8f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Your arm opens into a launcher and fires a missile that steers toward the nearest foe. */
    public static final class Missile extends Ability {
        Missile() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, BRASS).cooldown(80).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, ROCKET, 1.0, 0.5f, BRASS, 10f, 50);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.7f);
        }
    }

    /** Turn your leg into a great axe and bring it down on whatever stands in front of you. */
    public static final class AxeLeg extends Ability {
        AxeLeg() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, COLOR).cooldown(90).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 4.5 * ctx.power(), 1.2)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 14f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            Vec3 at = player.position().add(ctx.horizontalLook().scale(2));
            Vfx.slash(ctx.level(), player, at.add(0, 1, 0), new Vec3(0, -1, 0), 0xE8F0F8, 2.4f, 1);
            Vfx.burst(ctx.level(), player, at, COLOR, 1.8f, Kind.SHARD, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.7f);
        }
    }

    /** Grow steel plates over your body: much harder to hurt, a little slower. */
    public static final class ArmourPlates extends Buffs.Toggle {
        ArmourPlates() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(500).mastery(M, 35).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("arms_plates_armor"), 10, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("arms_plates_toughness"), 3, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("arms_plates_speed"), -0.1, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.IRON_BANDS, 10);
            ctx.sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Every limb opens into a launcher at once: six missiles streak out and hunt down your foes. */
    public static final class Arsenal extends Ability {
        Arsenal() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, BRASS).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Vec3 dir = player.getLookAngle().add(Math.cos(a) * 0.5, 0.4 + Math.sin(a) * 0.3, Math.sin(a) * 0.5).normalize();
                AbilityProjectile.spawn(player, ROCKET, player.position().add(0, 1.4, 0), dir.scale(0.8), 0.5f, BRASS, 9f * ctx.power(),
                        60).withPower(ctx.power());
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2f, 0.6f);
        }
    }

    /** Charge, then turn your whole body into a cannon and fire one enormous shell. */
    public static final class BodyCannon extends Ability {
        BodyCannon() {
            super(props(ActivationType.CHARGE, AbilityIcon.BURST, COLOR).cooldown(500).stamina(30).minCharge(15).maxActive(50)
                    .masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.CROSSBOW_LOADING_START.value(), 1.4f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 5 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.handPos(), BRASS, 0.6f, Kind.SPARK);
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Mth.clamp(ctx.activeTicks() / 50f, 0.4f, 1f);
            Kit.shoot(ctx, SHELL, 1.3, 1.2f + charge, COLOR, 22f * charge, 40).withPower(ctx.power() * (0.6f + 0.4f * charge));
            Kit.launch(ctx.player(), ctx.player().getLookAngle().scale(-0.8).add(0, 0.2, 0));
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.6f);
        }
    }
}
