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
import com.nerodev.grandline.fx.Fx;
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
 * Ryu Ryu no Mi, Model: Allosaurus. Become a ferocious allosaurus: a towering hunter with jaws that crush, a tail
 * that clears a circle, and a roar out of deep time.
 */
public class AllosaurusFruit extends ZoanFruit {
    public static final int COLOR = 0xA84A3A;
    public static final int DARK = 0x3A1A1A;
    static final String M = "fruit/grandline:allosaurus";

    /**
     * @hybrid Allosaurus-Man | Grow a dinosaur's tail, horns and hide: much stronger, tougher and bigger.
     * @beast Allosaurus Form | Become a towering allosaurus: huge, armoured, and a crushing biter.
     */
    static final ZoanForms FORMS = bodies("allosaurus", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.3, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 8, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.8, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .dimensions(1.8f, 3.0f, 2.6f));

    public static final DeferredHolder<Ability, Jaws> JAWS = ability("allosaurus_jaws", Jaws::new);
    public static final DeferredHolder<Ability, TailLash> TAIL_LASH = ability("allosaurus_tail_lash", TailLash::new);
    public static final DeferredHolder<Ability, PrimalRoar> PRIMAL_ROAR = ability("allosaurus_primal_roar", PrimalRoar::new);
    public static final DeferredHolder<Ability, MandibleCrush> MANDIBLE_CRUSH = ability("allosaurus_mandible_crush", MandibleCrush::new);
    public static final DeferredHolder<Ability, HuntingCharge> HUNTING_CHARGE = ability("allosaurus_hunting_charge", HuntingCharge::new);
    public static final DeferredHolder<Ability, DinoFrenzy> DINO_FRENZY = ability("allosaurus_dino_frenzy", DinoFrenzy::new);
    public static final DeferredHolder<Ability, PredatorBite> PREDATOR_BITE = ability("allosaurus_predator_bite", PredatorBite::new);

    public AllosaurusFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Ryu Ryu no Mi, Model: Allosaurus", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), JAWS, TAIL_LASH, PRIMAL_ROAR, MANDIBLE_CRUSH, HUNTING_CHARGE)
                .awakened(DINO_FRENZY, PREDATOR_BITE), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double reach(ServerPlayer player) {
        return FORMS.beast(player) ? 1.4 : 1.0;
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Snap your great jaws on whatever is in front of you: crushing, tearing wounds. Needs a form.
     *
     * @name Allosaurus Jaws
     */
    public static final class Jaws extends Ability {
        Jaws() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(30).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * reach(player), 45)) {
                ZoanKit.maul(player, target, 13f * FORMS.boost(player) * ctx.power(), 4, 1);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(player.getEyePosition().add(player.getLookAngle().scale(2)), player.getLookAngle())
                    .color(0xFFF0E0).scale(1.6f));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.2f, 0.8f);
        }
    }

    /** Spin and lash your tail around you: everything nearby is battered and flung away. Needs a form. */
    public static final class TailLash extends Ability {
        TailLash() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, DARK).cooldown(60).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * reach(player))) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 11f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.2).add(0, 0.5, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.8, 0), COLOR, (float) (5 * reach(player)), Kind.SHOCKWAVE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.6f);
        }
    }

    /** A roar out of deep time: everything around you is driven back, weakened and slowed. Needs a form. */
    public static final class PrimalRoar extends Ability {
        PrimalRoar() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(240).stamina(14).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ZoanKit.roar(player, 10 * ctx.power(), 3f * ctx.power(), 160);
            ctx.fx(GLEffects.ROAR, FxParams.of(player.getEyePosition(), player.getLookAngle()).color(COLOR).scale(10f));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
        }
    }

    /** Clamp a foe in your jaws and crush: a terrible wound, and it is held fast for a moment. Needs a form. */
    public static final class MandibleCrush extends Ability {
        MandibleCrush() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, DARK).cooldown(160).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 5 * reach(player)).isPresent() ? Optional.empty()
                    : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 5 * reach(player)).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 18f * FORMS.boost(player) * ctx.power());
                Kit.root(target, Vfx.StatusLook.ROOTS, 30);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), 0xC82A2A, 1.6f, Kind.SHOCKWAVE);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.6f);
        }
    }

    /** Charge down your prey: everything in the way is bitten and bowled aside. Needs a form. */
    public static final class HuntingCharge extends Ability {
        HuntingCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(160).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 14 * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 1.8, 10f * FORMS.boost(player) * ctx.power(), 1.2)) {
                ZoanKit.maul(player, target, 5f * ctx.power(), 4, 1);
            }
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), COLOR, 1.8f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.2f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Hold to rage: jaws, claws and tail tear into everything around you. Needs a form. */
    public static final class DinoFrenzy extends Ability {
        DinoFrenzy() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, COLOR).cooldown(300).stamina(18).staminaPerTick(0.6f).maxActive(60)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FRENZY);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 4.5)) {
                ZoanKit.maul(player, target, 4.5f * ctx.power(), 3, 1);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.at(player.position().add(0, 1.4, 0)).color(0xFFF0E0).variant(1).scale(4.5f));
            if (ctx.activeTicks() % 9 == 0) {
                ctx.sound(SoundEvents.RAVAGER_ATTACK, 0.8f, 1f);
            }
            return FORMS.any(player);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** The predator's bite: one enormous bite on a foe in reach, leaving it bleeding and broken. Needs a form. */
    public static final class PredatorBite extends Ability {
        PredatorBite() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, DARK).cooldown(500).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                ZoanKit.maul(player, target, 26f * ctx.power(), 8, 2);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                Vfx.burst(ctx.level(), player, target.getEyePosition(), 0xC82A2A, 2.2f, Kind.DROP, Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.9f);
        }
    }
}
