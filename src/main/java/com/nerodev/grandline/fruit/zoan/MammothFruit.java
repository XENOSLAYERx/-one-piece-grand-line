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
 * Zou Zou no Mi, Model: Mammoth. Become a colossal ancient mammoth: a mountain of wool and muscle with tusks that
 * sweep a battlefield clear and stomps that freeze the ground like the ice age.
 */
public class MammothFruit extends ZoanFruit {
    public static final int COLOR = 0x6A4A3A;
    public static final int TUSK = 0xE8E0C8;
    static final String M = "fruit/grandline:mammoth";

    /**
     * @hybrid Mammoth-Man | Grow tusks, wool and a mammoth's bulk: far bigger, stronger and tougher.
     * @beast Mammoth Form | Become a colossal mammoth: enormous, armoured, immovable and crushing, though slow.
     */
    static final ZoanForms FORMS = bodies("mammoth", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.5, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.6, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 30, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1.5, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.1, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(2.8f, 4.0f, 3.4f));

    public static final DeferredHolder<Ability, TuskSweep> TUSK_SWEEP = ability("mammoth_tusk_sweep", TuskSweep::new);
    public static final DeferredHolder<Ability, MammothStomp> MAMMOTH_STOMP = ability("mammoth_stomp", MammothStomp::new);
    public static final DeferredHolder<Ability, WoollyHide> WOOLLY_HIDE = ability("mammoth_woolly_hide", WoollyHide::new);
    public static final DeferredHolder<Ability, TrunkHammer> TRUNK_HAMMER = ability("mammoth_trunk_hammer", TrunkHammer::new);
    public static final DeferredHolder<Ability, MammothCharge> MAMMOTH_CHARGE = ability("mammoth_charge", MammothCharge::new);
    public static final DeferredHolder<Ability, IceAge> ICE_AGE = ability("mammoth_ice_age", IceAge::new);
    public static final DeferredHolder<Ability, Rampage> RAMPAGE = ability("mammoth_rampage", Rampage::new);

    public MammothFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Zou Zou no Mi, Model: Mammoth", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), TUSK_SWEEP, MAMMOTH_STOMP, WOOLLY_HIDE, TRUNK_HAMMER, MAMMOTH_CHARGE)
                .awakened(ICE_AGE, RAMPAGE), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double size(ServerPlayer player) {
        return FORMS.beast(player) ? 1.6 : 1.0;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Sweep your great tusks across everything in front of you, hurling it aside. Needs a form. */
    public static final class TuskSweep extends Ability {
        TuskSweep() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, TUSK).cooldown(40).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * size(player), 65)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.right().scale(1.3).add(ctx.horizontalLook().scale(0.5)).add(0, 0.6, 0));
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2.5 * size(player))), ctx.right(), TUSK,
                    (float) (3.5 * size(player)), 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.4f, 0.5f);
        }
    }

    /** Stamp the earth: everything around you is crushed, bounced and staggered. Needs a form. */
    public static final class MammothStomp extends Ability {
        MammothStomp() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(100).stamina(14).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 5 * size(player) * ctx.power();
            ZoanKit.stomp(player, player.position(), radius, 14f * FORMS.boost(player) * ctx.power());
            Vfx.burst(ctx.level(), player, player.position(), 0x8A6A4A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.5f);
        }
    }

    /** Brace in your thick wool: you shrug off blows, cold and fire for ten seconds. */
    public static final class WoollyHide extends Ability {
        WoollyHide() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(400).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
            player.setTicksFrozen(0);
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 2.4f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 0.6f);
        }
    }

    /** Bring your trunk down on a foe like a hammer: a crushing blow that drives it into the ground. Needs a form. */
    public static final class TrunkHammer extends Ability {
        TrunkHammer() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 6 * size(player)).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6 * size(player)).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 18f * FORMS.boost(player) * ctx.power());
                target.setDeltaMovement(0, -1.5, 0);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                Vfx.burst(ctx.level(), player, target.position(), COLOR, 2.5f, Kind.SHOCKWAVE, Vfx.GROUND | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 0.8f);
        }
    }

    /** Charge like a landslide: everything in your path is gored aside. Needs a form. */
    public static final class MammothCharge extends Ability {
        MammothCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, TUSK).cooldown(180).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 12 * size(player) * ctx.power();
            ZoanKit.charge(player, length, 2.0 * size(player), 18f * FORMS.boost(player) * ctx.power(), 1.6);
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), 0x8A6A4A, 2.4f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A stomp that brings back the ice age: everything around you is crushed and frozen to the ground. Needs a form. */
    public static final class IceAge extends Ability {
        IceAge() {
            super(props(ActivationType.INSTANT, AbilityIcon.SNOWFLAKE, 0xA8D8F0).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : ZoanKit.stomp(player, player.position(), 10 * ctx.power(), 20f * ctx.power())) {
                Kit.root(target, Vfx.StatusLook.ICE, 60);
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 200));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            }
            Vfx.burst(ctx.level(), player, player.position(), 0xA8D8F0, 10f, Kind.SNOW, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.6f, 0.6f);
        }
    }

    /** A colossal rampage twenty-four blocks long: everything in the way is gored, and the ground heaves behind you. */
    public static final class Rampage extends Ability {
        Rampage() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 dir = ctx.horizontalLook();
            ZoanKit.charge(player, 24 * ctx.power(), 3.0, 22f * ctx.power(), 1.8);
            for (int i = 1; i <= 6; i++) {
                Vec3 at = Kit.ground(ctx.level(), from.add(dir.scale(i * 4)), 4);
                ZoanKit.stomp(player, at, 3.5, 6f * ctx.power());
                Vfx.burst(ctx.level(), player, at, 0x8A6A4A, 3.5f, Kind.SHARD, Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }
}
