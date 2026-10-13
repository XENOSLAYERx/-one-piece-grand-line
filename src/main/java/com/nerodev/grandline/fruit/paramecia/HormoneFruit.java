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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Horu Horu no Mi. Inject hormones from the fingertips that rewrite a body: heal it, fire it up, grow it, shrink it.
 * Used on yourself it is medicine; used on a foe it is a weapon.
 */
public class HormoneFruit extends DevilFruit {
    public static final int COLOR = 0xF06AC0;
    public static final int DARK = 0x3A3A3A;
    static final String M = "fruit/grandline:hormone";

    static final Buffs.Mod SHRUNK = Buffs.mod(Attributes.SCALE, GrandLine.id("hormone_shrunk_scale"), -0.5, Operation.ADD_MULTIPLIED_TOTAL);
    static final Buffs.Mod SHRUNK_DAMAGE = Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("hormone_shrunk_damage"), -0.4,
            Operation.ADD_MULTIPLIED_TOTAL);

    public static final DeferredHolder<Ability, HealingHormone> HEALING = ability("hormone_healing", HealingHormone::new);
    public static final DeferredHolder<Ability, TensionHormone> TENSION = ability("hormone_tension", TensionHormone::new);
    public static final DeferredHolder<Ability, NeedleShot> NEEDLE_SHOT = ability("hormone_needle_shot", NeedleShot::new);
    public static final DeferredHolder<Ability, GrowthHormone> GROWTH = ability("hormone_growth", GrowthHormone::new);
    public static final DeferredHolder<Ability, ShrinkHormone> SHRINK = ability("hormone_shrink", ShrinkHormone::new);
    public static final DeferredHolder<Ability, HormoneSurge> SURGE = ability("hormone_surge", HormoneSurge::new);
    public static final DeferredHolder<Ability, DeathWink> DEATH_WINK = ability("hormone_death_wink", DeathWink::new);

    public static final ResourceLocation NEEDLE = GrandLine.id("hormone_needle");

    public HormoneFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Horu Horu no Mi", COLOR)
                .abilities(HEALING, TENSION, NEEDLE_SHOT, GROWTH, SHRINK)
                .awakened(SURGE, DEATH_WINK));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(NEEDLE, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
        }));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Inject healing hormones into yourself, or into the player in front of you: wounds close at once. */
    public static final class HealingHormone extends Ability {
        HealingHormone() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(300).stamina(16).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            LivingEntity patient = Kit.target(player, 4).filter(t -> t instanceof Player && !AbilityDamage.canHarm(player, t))
                    .orElse(player);
            patient.heal(10f * ctx.power());
            patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            // The body pays for it a little later.
            patient.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
            Vfx.burst(ctx.level(), patient, patient.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.HEART, 0);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.BREWING_STAND_BREW, 1f, 1.4f);
        }
    }

    /** Pump yourself full of tension hormones: faster, stronger, tireless, until the crash when it ends. */
    public static final class TensionHormone extends Buffs.Toggle {
        TensionHormone() {
            super(props(ActivationType.TOGGLE, AbilityIcon.BOLT, COLOR).cooldown(400).stamina(14).staminaPerTick(0.1f)
                            .maxActive(400).masteryGain(0.8),
                    List.of(Buffs.mod(Attributes.MOVEMENT_SPEED, GrandLine.id("hormone_tension_speed"), 0.2, Operation.ADD_MULTIPLIED_BASE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("hormone_tension_damage"), 3, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.KNOCKBACK_RESISTANCE, GrandLine.id("hormone_tension_knockback"), 0.4,
                                    Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.6f, Kind.SPARK, Vfx.FLASH);
            ctx.sound(SoundEvents.BREWING_STAND_BREW, 1f, 0.8f);
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            ctx.player().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
            ctx.player().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0));
        }
    }

    /** Fire hormone-filled needles from your fingers: a foe they hit grows weak, sluggish and dizzy. */
    public static final class NeedleShot extends Ability {
        NeedleShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, DARK).cooldown(60).stamina(10).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, NEEDLE, 2.2, 0.25f, COLOR, 4f, 20);
            ctx.animate(GLEffects.ANIM_FINGER_GUNS);
            ctx.sound(SoundEvents.ARROW_SHOOT, 0.8f, 1.8f);
        }
    }

    /** Inject growth hormones into yourself: you swell to a giant's size and strength for a while. */
    public static final class GrowthHormone extends Buffs.Toggle {
        GrowthHormone() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TRANSFORM, COLOR).cooldown(500).stamina(20).staminaPerTick(0.2f)
                            .maxActive(400).mastery(M, 20).masteryGain(0.9),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("hormone_growth_scale"), 0.6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("hormone_growth_damage"), 4, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.MAX_HEALTH, GrandLine.id("hormone_growth_health"), 6, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 2f, Kind.HEART, Vfx.FLASH);
            ctx.sound(GLSounds.INFLATE.get(), 1f, 0.8f);
        }

        @Override
        protected void stopped(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        }
    }

    /** Stab a foe with shrinking hormones: it dwindles to half its size and hits far more weakly for fifteen seconds. */
    public static final class ShrinkHormone extends Ability {
        ShrinkHormone() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, DARK).cooldown(300).stamina(18).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Buffs.timed(target, SHRUNK, 300);
                Buffs.timed(target, SHRUNK_DAMAGE, 300);
                Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.SPARK, 0);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.INFLATE.get(), 1f, 1.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Flood everyone around you with hormones: friends are healed and fired up, foes are left feeble. */
    public static final class HormoneSurge extends Ability {
        HormoneSurge() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(700).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (Player friend : ctx.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius),
                    p -> p == player || !AbilityDamage.canHarm(player, p))) {
                friend.heal(8f);
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0));
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            }
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.HEART, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BREWING_STAND_BREW, 2f, 0.6f);
        }
    }

    /**
     * Wink with hormone-charged force: a gale of wind blasts everything in front of you away.
     *
     * @name Death Wink
     */
    public static final class DeathWink extends Ability {
        DeathWink() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 14 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, range, 35)) {
                Kit.hit(player, target, AbilityDamage.BLAST, 18f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.6, 0.6);
            }
            Vfx.beam(ctx.level(), player, player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(range)), COLOR,
                    3f, Kind.SHOCKWAVE, Vfx.BEAM_PARTICLES);
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(player.getLookAngle()), COLOR, 2f, Kind.SHOCKWAVE,
                    Vfx.SHOCKWAVE | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 1.4f);
        }
    }
}
