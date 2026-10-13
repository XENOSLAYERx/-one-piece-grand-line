package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.LogiaFruit;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Supa Supa no Mi. Any part of the body can become a steel blade, and the whole body steel: arms that cut like
 * swords, a hide that cuts whoever strikes it.
 */
public class DiceFruit extends DevilFruit {
    public static final int COLOR = 0xA8B0B8;
    public static final int EDGE = 0xE8F0F8;
    static final String M = "fruit/grandline:dice";

    public static final DeferredHolder<Ability, AtomicSpar> ATOMIC_SPAR = ability("dice_atomic_spar", AtomicSpar::new);
    public static final DeferredHolder<Ability, SparClaw> SPAR_CLAW = ability("dice_spar_claw", SparClaw::new);
    public static final DeferredHolder<Ability, Spider> SPIDER = ability("dice_spider", Spider::new);
    public static final DeferredHolder<Ability, BladeSpin> BLADE_SPIN = ability("dice_blade_spin", BladeSpin::new);
    public static final DeferredHolder<Ability, SteelThrust> STEEL_THRUST = ability("dice_steel_thrust", SteelThrust::new);
    public static final DeferredHolder<Ability, BladeField> BLADE_FIELD = ability("dice_blade_field", BladeField::new);
    public static final DeferredHolder<Ability, Guillotine> GUILLOTINE = ability("dice_guillotine", Guillotine::new);

    public static final ResourceLocation FIELD = GrandLine.id("dice_blade_field");
    public static final ResourceLocation CROSS = GrandLine.id("dice_cross");

    public DiceFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Supa Supa no Mi", COLOR)
                .abilities(ATOMIC_SPAR, SPAR_CLAW, SPIDER, BLADE_SPIN, STEEL_THRUST)
                .awakened(BLADE_FIELD, GUILLOTINE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(20, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(CROSS, Projectiles.piercing(AbilityDamage.SLASH,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1))));
    }

    /** A plain melee blow (never an ability's damage, so two steel bodies can't bounce cuts back and forth). */
    static boolean meleeHit(LivingIncomingDamageEvent event) {
        return event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK);
    }

    /** In Spider form, anyone who strikes the steel body with bare hands cuts themselves on it. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (AbilityState.of(user).isActive(SPIDER.getId()) && meleeHit(event)
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != user
                && !LogiaFruit.canTouch(event.getSource())) {
            Kit.hit(user, attacker, AbilityDamage.SLASH, 3f);
            Vfx.impact(user.serverLevel(), user, attacker.getBoundingBox().getCenter(), EDGE, 0.8f, Kind.SPARK);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Cross your blade-arms and cut everything in front of you twice. */
    public static final class AtomicSpar extends Ability {
        AtomicSpar() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, EDGE).cooldown(45).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * ctx.power(), 50)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 6f * ctx.power());
                Kit.hit(player, target, AbilityDamage.SLASH, 5f * ctx.power());
            }
            Vec3 at = ctx.handPos().add(ctx.horizontalLook().scale(1.8));
            Vfx.slash(ctx.level(), player, at, player.getLookAngle().add(ctx.right().scale(0.4)), EDGE, 2f, 1);
            Vfx.slash(ctx.level(), player, at, player.getLookAngle().add(ctx.right().scale(-0.4)), EDGE, 2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        }
    }

    /**
     * Dash through a line of foes with your blade-arms out, slicing everyone you pass.
     *
     * @name Spar Claw
     */
    public static final class SparClaw extends Ability {
        SparClaw() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(70).stamina(12).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 7 * ctx.power(), 1.1)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
            }
            Vec3 start = player.position().add(0, 1, 0);
            Kit.dash(player, 1.6, 0.1);
            Vfx.trail(ctx.level(), player, start, start.add(player.getLookAngle().scale(7)), EDGE, 0.5f, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.SAND_SLASH.get(), 1f, 1.4f);
        }
    }

    /**
     * Turn your whole body to steel: tougher than armour, and bare-handed attackers cut themselves on you.
     *
     * @name Spider
     */
    public static final class Spider extends Buffs.Toggle {
        Spider() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(500).mastery(M, 10).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("dice_spider_armor"), 12, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR_TOUGHNESS, GrandLine.id("dice_spider_toughness"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("dice_spider_knockback"), 0.6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("dice_spider_speed"), -0.15, Operation.ADD_MULTIPLIED_TOTAL)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.IRON_BANDS, 10);
            ctx.sound(SoundEvents.ANVIL_USE, 0.8f, 1.4f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 12 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), EDGE, 0.6f, Kind.SPARK);
            }
            return true;
        }
    }

    /** Hold to spin with blades out on every side, cutting everything close to you. */
    public static final class BladeSpin extends Ability {
        BladeSpin() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, EDGE).cooldown(140).stamina(12).staminaPerTick(0.35f)
                    .maxActive(50).mastery(M, 20).masteryGain(0.9));
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
                    Kit.hit(player, target, AbilityDamage.SLASH, 3f * ctx.power());
                }
                Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), EDGE, 2.2f, Kind.SPARK);
                ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 1.6f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Lunge with your arm as a single long blade, piercing everything in a straight line. */
    public static final class SteelThrust extends Ability {
        SteelThrust() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, EDGE).cooldown(90).stamina(14).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, range, 0.7)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 13f * ctx.power());
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, range), EDGE, 0.3f, Kind.SPARK,
                    Vfx.BEAM_GLOW);
            Kit.launch(player, ctx.horizontalLook().scale(0.8).add(0, player.getDeltaMovement().y, 0));
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground around you into a bed of blades: anyone standing on it is cut and slowed. */
    public static final class BladeField extends Ability {
        BladeField() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 9 * ctx.power();
            AbilityProjectile.zone(player, FIELD, player.position(), radius, EDGE, 5f * ctx.power(), 140);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), EDGE, (float) radius, Kind.SPARK,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ANVIL_LAND, 1.4f, 1.4f);
        }
    }

    /**
     * Throw a giant cross of steel that cuts through everything in its path.
     *
     * @name Atomic Spar: Guillotine
     */
    public static final class Guillotine extends Ability {
        Guillotine() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, EDGE).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, CROSS, 1.4, 3f, EDGE, 18f, 30);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.6f);
        }
    }
}
