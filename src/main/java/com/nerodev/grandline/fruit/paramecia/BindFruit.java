package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

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
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
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
 * Ori Ori no Mi. Anything that passes through the user's body, an arm swinging through a foe, a fist that strikes
 * them, comes out locked in iron bonds.
 */
public class BindFruit extends DevilFruit {
    public static final int COLOR = 0x8A9098;
    public static final int IRON = 0xD0D4DA;
    static final String M = "fruit/grandline:bind";

    public static final DeferredHolder<Ability, AwaseBaori> AWASE_BAORI = ability("bind_awase_baori", AwaseBaori::new);
    public static final DeferredHolder<Ability, BlackCage> BLACK_CAGE = ability("bind_black_cage", BlackCage::new);
    public static final DeferredHolder<Ability, BandShot> BAND_SHOT = ability("bind_band_shot", BandShot::new);
    public static final DeferredHolder<Ability, ShackleRing> SHACKLE_RING = ability("bind_shackle_ring", ShackleRing::new);
    public static final DeferredHolder<Ability, IronGrip> IRON_GRIP = ability("bind_iron_grip", IronGrip::new);
    public static final DeferredHolder<Ability, Prison> PRISON = ability("bind_prison", Prison::new);
    public static final DeferredHolder<Ability, Tighten> TIGHTEN = ability("bind_tighten", Tighten::new);

    public static final ResourceLocation BAND = GrandLine.id("bind_band");
    public static final ResourceLocation PRISON_ZONE = GrandLine.id("bind_prison");

    public BindFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Ori Ori no Mi", COLOR)
                .abilities(AWASE_BAORI, BLACK_CAGE, BAND_SHOT, SHACKLE_RING, IRON_GRIP)
                .awakened(PRISON, TIGHTEN));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BAND, Projectiles.bolt(AbilityDamage.STRIKE,
                (p, target) -> Kit.root(target, Vfx.StatusLook.IRON_BANDS, 60)));
        ProjectileBehavior.Registry.register(PRISON_ZONE, Projectiles.zone(30, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.IRON_BANDS, 25);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
        }, null));
    }

    static boolean bound(LivingEntity target) {
        return target.hasEffect(GLMobEffects.ROOTED) || target.hasEffect(GLMobEffects.ENCASED);
    }

    /** A bare-handed attacker's arm passes through the body and comes out bound (always, with Iron Grip up). */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        boolean melee = event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK);
        if (!melee || LogiaFruit.canTouch(event.getSource()) || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) {
            return;
        }
        boolean grip = AbilityState.of(user).isActive(IRON_GRIP.getId());
        if (grip || user.getRandom().nextFloat() < 0.3f) {
            Kit.root(attacker, Vfx.StatusLook.IRON_BANDS, grip ? 60 : 35);
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.CHAIN_PLACE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Sweep your arm through everyone in front of you: it comes out the other side in iron bands.
     *
     * @name Awase Baori
     */
    public static final class AwaseBaori extends Ability {
        AwaseBaori() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, IRON).cooldown(80).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * ctx.power(), 50)) {
                Kit.root(target, Vfx.StatusLook.IRON_BANDS, 80);
                Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.6)), player.getLookAngle(), IRON, 2f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.8f);
        }
    }

    /**
     * Lock a foe from head to toe in iron bonds: it can neither move nor fight.
     *
     * @name Black Cage
     */
    public static final class BlackCage extends Ability {
        BlackCage() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(200).stamina(16).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 16).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 16).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.IRON_BANDS, 70);
                Vfx.trail(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), IRON, 0.4f, Kind.SPARK);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.CHAIN_BREAK, 1f, 0.6f);
        }
    }

    /** Throw a ring of iron that snaps shut around the first foe it hits. */
    public static final class BandShot extends Ability {
        BandShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, IRON).cooldown(50).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BAND, 1.8, 0.5f, IRON, 4f, 30);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.CHAIN_HIT, 1f, 1.2f);
        }
    }

    /** Iron bands spring up in a ring where you point and clamp every pair of legs inside it. */
    public static final class ShackleRing extends Ability {
        ShackleRing() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, IRON).cooldown(180).stamina(18).mastery(M, 20)
                    .masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.root(target, Vfx.StatusLook.IRON_BANDS, 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            Vfx.burst(ctx.level(), player, at, IRON, 4f, Kind.SPARK, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.4f, 0.6f);
        }
    }

    /** Harden your stance: every bare-handed blow that reaches you leaves its owner bound, and you can't be pushed. */
    public static final class IronGrip extends Buffs.Toggle {
        IronGrip() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(10).staminaPerTick(0.15f)
                            .maxActive(400).mastery(M, 35).masteryGain(0.9),
                    List.of(Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("bind_grip_knockback"), 0.8, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("bind_grip_armor"), 4, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.status(ctx.level(), ctx.player(), Vfx.StatusLook.IRON_BANDS, 10);
            ctx.sound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground around you into a prison: everyone in it is bound again and again. */
    public static final class Prison extends Ability {
        Prison() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(700).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            AbilityProjectile.zone(player, PRISON_ZONE, player.position(), radius, IRON, 0f, 200);
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.2, 0), IRON, (float) radius, Kind.SPARK,
                    Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.CHAIN_PLACE, 2f, 0.4f);
        }
    }

    /** Every iron bond you have placed nearby tightens at once, crushing whoever wears it. */
    public static final class Tighten extends Ability {
        Tighten() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, IRON).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 24)) {
                boolean held = bound(target);
                if (held || target.distanceTo(player) < 6) {
                    Kit.hit(player, target, AbilityDamage.CRUSH, (held ? 16f : 6f) * ctx.power());
                    Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), IRON, 1.2f, Kind.SPARK, Vfx.FLASH);
                }
            }
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.CHAIN_BREAK, 2f, 0.5f);
        }
    }
}
