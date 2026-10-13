package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.bodies;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
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
 * Ryu Ryu no Mi, Model: Spinosaurus. Become a sail-backed spinosaurus: a river hunter as much at home in water as on
 * land, with a long snapping snout, a sail that cuts, and a tail that sweeps.
 */
public class SpinosaurusFruit extends ZoanFruit {
    public static final int COLOR = 0x4A8A6A;
    public static final int SAIL = 0xE8C060;
    static final String M = "fruit/grandline:spinosaurus";

    /**
     * @hybrid Spinosaurus-Man | Grow a sail, tail and hide: stronger, tougher and a natural swimmer.
     * @beast Spinosaurus Form | Become a sail-backed spinosaurus: huge, armoured, and fast in the water.
     */
    static final ZoanForms FORMS = bodies("spinosaurus", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.SWIMMER,
            b -> b.modifier(Attributes.SCALE, 0.25, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 8, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 7, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.8, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .dimensions(2.0f, 3.2f, 2.7f));

    public static final DeferredHolder<Ability, SnappingJaws> SNAPPING_JAWS = ability("spinosaurus_snapping_jaws", SnappingJaws::new);
    public static final DeferredHolder<Ability, SailSlash> SAIL_SLASH = ability("spinosaurus_sail_slash", SailSlash::new);
    public static final DeferredHolder<Ability, RiverLunge> RIVER_LUNGE = ability("spinosaurus_river_lunge", RiverLunge::new);
    public static final DeferredHolder<Ability, TailSweep> TAIL_SWEEP = ability("spinosaurus_tail_sweep", TailSweep::new);
    public static final DeferredHolder<Ability, SpineBreaker> SPINE_BREAKER = ability("spinosaurus_spine_breaker", SpineBreaker::new);
    public static final DeferredHolder<Ability, SailCharge> SAIL_CHARGE = ability("spinosaurus_sail_charge", SailCharge::new);
    public static final DeferredHolder<Ability, TidalRoar> TIDAL_ROAR = ability("spinosaurus_tidal_roar", TidalRoar::new);

    public SpinosaurusFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Ryu Ryu no Mi, Model: Spinosaurus", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), SNAPPING_JAWS, SAIL_SLASH, RIVER_LUNGE, TAIL_SWEEP, SPINE_BREAKER)
                .awakened(SAIL_CHARGE, TIDAL_ROAR), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    /** A river hunter: half again as strong in the water. */
    static float wet(ServerPlayer player) {
        return player.isInWaterOrRain() ? 1.5f : 1f;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Snap your long snout shut on whatever is in front of you. Stronger in the water. Needs a form. */
    public static final class SnappingJaws extends Ability {
        SnappingJaws() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(28).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, FORMS.beast(player) ? 6 : 4.5, 35)) {
                ZoanKit.maul(player, target, 12f * wet(player) * FORMS.boost(player) * ctx.power(), 4, 0);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(player.getEyePosition().add(player.getLookAngle().scale(2)), player.getLookAngle())
                    .color(0xF0F0E0));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.1f, 1f);
        }
    }

    /** Swing round so your sail cuts through everything beside you. Needs a form. */
    public static final class SailSlash extends Ability {
        SailSlash() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, SAIL).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 4.2)) {
                ZoanKit.maul(player, target, 10f * FORMS.boost(player) * ctx.power(), 5, 1);
            }
            Vfx.slash(ctx.level(), player, player.position().add(0, 1.5, 0), ctx.right(), SAIL, 4f, 2);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        }
    }

    /** Lunge forward like a hunter breaking the surface: a long dash that bites everything in the way. Needs a form. */
    public static final class RiverLunge extends Ability {
        RiverLunge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(90).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 10 * wet(player) * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.6)) {
                ZoanKit.maul(player, target, 12f * wet(player) * FORMS.boost(player) * ctx.power(), 4, 0);
            }
            Kit.dash(player, 1.8 * wet(player), 0.15);
            Vfx.trail(ctx.level(), player, from, from.add(player.getLookAngle().scale(length)), 0x6AA8E8, 1.4f, Kind.DROP);
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.GENERIC_SPLASH, 1.2f, 0.8f);
        }
    }

    /** Sweep your long tail around: everything nearby is battered and flung away. Needs a form. */
    public static final class TailSweep extends Ability {
        TailSweep() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(120).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.3).add(0, 0.5, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.8, 0), COLOR, 6f, Kind.SHOCKWAVE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.6f);
        }
    }

    /** Bite down on a foe's back and wrench: a crushing wound that leaves it weak. Needs a form. */
    public static final class SpineBreaker extends Ability {
        SpineBreaker() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, SAIL).cooldown(200).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 20f * FORMS.boost(player) * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), SAIL, 1.6f, Kind.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Charge sail-first: everything in a long path is slashed and thrown aside. Needs a form. */
    public static final class SailCharge extends Ability {
        SailCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, SAIL).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 18 * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 2.2, 12f * ctx.power(), 1.4)) {
                ZoanKit.maul(player, target, 8f * ctx.power(), 5, 1);
            }
            Vfx.trail(ctx.level(), player, from.add(0, 1.5, 0), from.add(ctx.horizontalLook().scale(length)).add(0, 1.5, 0), SAIL, 2f,
                    Kind.STREAK);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.4f, 0.8f);
        }
    }

    /** A roar that hits like a wave: everything around you is swept away, soaked and slowed. Needs a form. */
    public static final class TidalRoar extends Ability {
        TidalRoar() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, 0x6AA8E8).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : ZoanKit.roar(player, 12 * ctx.power(), 10f * ctx.power(), 160)) {
                AbilityDamage.knockback(target, player.position(), 2.2, 0.5);
                target.clearFire();
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), 0x6AA8E8, 12f, Kind.DROP, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GENERIC_SPLASH, 2f, 0.5f);
        }
    }
}
