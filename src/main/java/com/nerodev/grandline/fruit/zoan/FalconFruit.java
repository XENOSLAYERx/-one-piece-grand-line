package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

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
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Tori Tori no Mi, Model: Falcon. Become a great falcon and rule the skies: winged in both bodies, with talons that
 * dive from above, wings that hurl gales and eyes that miss nothing.
 */
public class FalconFruit extends ZoanFruit {
    public static final int COLOR = 0xA87A4A;
    public static final int CREAM = 0xF0E0C0;
    static final String M = "fruit/grandline:falcon";

    /**
     * @hybrid Falcon-Man | Grow great wings and talons: you can fly, and you strike harder.
     * @beast Falcon Form | Become a great falcon: fast in the air and hard to bring down from a fall.
     */
    static final ZoanForms FORMS = bodies("falcon", COLOR, () -> SoundEvents.PARROT_AMBIENT, ZoanForms.Traits.WINGED,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 20, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.4, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 40, Operation.ADD_VALUE)
                    .dimensions(0.9f, 1.0f, 0.8f));

    public static final DeferredHolder<Ability, TalonDive> TALON_DIVE = ability("falcon_talon_dive", TalonDive::new);
    public static final DeferredHolder<Ability, WingGust> WING_GUST = ability("falcon_wing_gust", WingGust::new);
    public static final DeferredHolder<Ability, FalconEye> FALCON_EYE = ability("falcon_eye", FalconEye::new);
    public static final DeferredHolder<Ability, FeatherFlurry> FEATHER_FLURRY = ability("falcon_feather_flurry", FeatherFlurry::new);
    public static final DeferredHolder<Ability, SkyLift> SKY_LIFT = ability("falcon_sky_lift", SkyLift::new);
    public static final DeferredHolder<Ability, RoyalDive> ROYAL_DIVE = ability("falcon_royal_dive", RoyalDive::new);
    public static final DeferredHolder<Ability, Gale> GALE_MOVE = ability("falcon_gale", Gale::new);

    public static final ResourceLocation FEATHER = GrandLine.id("falcon_feather");
    public static final ResourceLocation GALE = GrandLine.id("falcon_gale");

    public FalconFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.RARE, "Tori Tori no Mi, Model: Falcon", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), TALON_DIVE, WING_GUST, FALCON_EYE, FEATHER_FLURRY, SKY_LIFT)
                .awakened(ROYAL_DIVE, GALE_MOVE), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FEATHER, Projectiles.piercing(AbilityDamage.SLASH, null));
        ProjectileBehavior.Registry.register(GALE, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                Vec3 to = p.position().subtract(target.position());
                Vec3 around = new Vec3(-to.z, 0, to.x).normalize().scale(0.5);
                Kit.launch(target, around.add(0, 0.25, 0));
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Dive on a foe talons first: it is torn and left bleeding. Needs a form. */
    public static final class TalonDive extends Ability {
        TalonDive() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(50).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 18).isPresent() ? Optional.empty() : Optional.of(Component.literal("No prey in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 18).ifPresent(target -> {
                Vec3 from = player.position();
                Vec3 to = target.position().subtract(target.position().subtract(from).normalize().scale(1.2));
                player.teleportTo(to.x, to.y, to.z);
                player.resetFallDistance();
                ZoanKit.maul(player, target, 10f * FORMS.boost(player) * ctx.power(), 4, 0);
                Vfx.trail(ctx.level(), player, from.add(0, 0.8, 0), to.add(0, 0.8, 0), CREAM, 0.8f, Kind.FEATHER);
                ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(target.getBoundingBox().getCenter(), player.getLookAngle())
                        .color(0xFFE6B0));
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(GLSounds.CLAW.get(), 1f, 1.3f);
        }
    }

    /** Beat your wings at everything in front of you: a gust that batters and blows it away. */
    public static final class WingGust extends Ability {
        WingGust() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, CREAM).cooldown(60).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 7 * ctx.power(), 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 5f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, look.scale(1.5).add(0, 0.4, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(3)), CREAM, 3f, Kind.FEATHER, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.4f, 0.8f);
        }
    }

    /**
     * A falcon's eyes: see in the dark, and every creature around you stands out for fifteen seconds.
     *
     * @name Falcon Eye
     */
    public static final class FalconEye extends Ability {
        FalconEye() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(400).stamina(8).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 32)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
            }
            Vfx.impact(ctx.level(), player, player.getEyePosition(), 0xF0C040, 1.2f, Kind.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.PARROT_AMBIENT, 1f, 0.7f);
        }
    }

    /** Loose a flurry of steel-hard feathers that cut through everything in their path. Needs a form. */
    public static final class FeatherFlurry extends Ability {
        FeatherFlurry() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, CREAM).cooldown(80).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.09)).normalize();
                AbilityProjectile.spawn(player, FEATHER, ctx.handPos(), dir.scale(1.9), 0.3f, CREAM, 5f * ctx.power(), 22);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1f, 1.4f);
        }
    }

    /** Seize the nearest foe in your talons and climb: you both shoot skyward, and it falls when you let go. */
    public static final class SkyLift extends Ability {
        SkyLift() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(240).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return AbilityDamage.around(player, player.position(), 4).isEmpty() ? Optional.of(Component.literal("Nothing within reach"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityDamage.around(player, player.position(), 4).stream().findFirst().ifPresent(target -> {
                ZoanKit.maul(player, target, 6f * ctx.power(), 3, 0);
                Kit.launch(target, new Vec3(0, 1.5, 0));
                Kit.launch(player, new Vec3(0, 1.3, 0));
            });
            Vfx.burst(ctx.level(), player, player.position(), CREAM, 2f, Kind.FEATHER, Vfx.RING);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.6f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Drop out of the sky onto the spot you point at: a royal dive that blasts everything there. */
    public static final class RoyalDive extends Ability {
        RoyalDive() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 10);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            for (LivingEntity target : AbilityDamage.around(player, at, 4.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.BLAST, 14f * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(1.1).add(0, 0.6, 0));
            }
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), at.add(0, 1, 0), COLOR, 1.4f, Kind.FEATHER);
            Vfx.burst(ctx.level(), player, at, CREAM, 4.5f, Kind.FEATHER, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 1.1f);
        }
    }

    /**
     * Whip up a whirling gale where you point: for five seconds it batters, blinds and spins everything inside.
     *
     * @name Falcon Gale
     */
    public static final class Gale extends Ability {
        Gale() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, CREAM).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 22), 8);
            AbilityProjectile.zone(player, GALE, at, 6 * ctx.power(), CREAM, 4f * ctx.power(), 100);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 2f, 0.5f);
        }
    }
}
