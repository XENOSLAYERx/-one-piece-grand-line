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
 * Ryu Ryu no Mi, Model: Brachiosaurus. Become a towering brachiosaurus: the biggest and toughest of the ancient
 * bodies, with a neck that hammers from on high and a tail and feet that shake the earth.
 */
public class BrachiosaurusFruit extends ZoanFruit {
    public static final int COLOR = 0x6A8A9A;
    public static final int DARK = 0x2A3A4A;
    static final String M = "fruit/grandline:brachiosaurus";

    /**
     * @hybrid Brachiosaurus-Man | Grow a long tail and a giant's bulk: much bigger and far tougher, if a little slower.
     * @beast Brachiosaurus Form | Become a towering brachiosaurus: enormous, armoured, and nearly impossible to move.
     */
    static final ZoanForms FORMS = bodies("brachiosaurus", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.6, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 14, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.7, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.05, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MAX_HEALTH, 40, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1.5, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.15, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(2.4f, 4.4f, 4.0f));

    public static final DeferredHolder<Ability, NeckHammer> NECK_HAMMER = ability("brachiosaurus_neck_hammer", NeckHammer::new);
    public static final DeferredHolder<Ability, TailWhip> TAIL_WHIP = ability("brachiosaurus_tail_whip", TailWhip::new);
    public static final DeferredHolder<Ability, TitanStomp> TITAN_STOMP = ability("brachiosaurus_titan_stomp", TitanStomp::new);
    public static final DeferredHolder<Ability, LivingFortress> LIVING_FORTRESS = ability("brachiosaurus_living_fortress",
            LivingFortress::new);
    public static final DeferredHolder<Ability, TreetopStrike> TREETOP_STRIKE = ability("brachiosaurus_treetop_strike",
            TreetopStrike::new);
    public static final DeferredHolder<Ability, EarthShatter> EARTH_SHATTER = ability("brachiosaurus_earth_shatter", EarthShatter::new);
    public static final DeferredHolder<Ability, NeckSweep> NECK_SWEEP = ability("brachiosaurus_neck_sweep", NeckSweep::new);

    public BrachiosaurusFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Ryu Ryu no Mi, Model: Brachiosaurus", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), NECK_HAMMER, TAIL_WHIP, TITAN_STOMP, LIVING_FORTRESS, TREETOP_STRIKE)
                .awakened(EARTH_SHATTER, NECK_SWEEP), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double size(ServerPlayer player) {
        return FORMS.beast(player) ? 1.6 : 1.0;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Swing your head down from on high like a hammer on everything in front of you. Needs a form. */
    public static final class NeckHammer extends Ability {
        NeckHammer() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(45).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5 * size(player), 40)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 14f * FORMS.boost(player) * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            Vfx.burst(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(3 * size(player))), COLOR, 3f, Kind.SHOCKWAVE,
                    Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 0.8f);
        }
    }

    /** Crack your long tail around you like a whip: everything nearby is battered and flung. Needs a form. */
    public static final class TailWhip extends Ability {
        TailWhip() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, DARK).cooldown(70).stamina(11).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * size(player))) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.4).add(0, 0.5, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.8, 0), COLOR, (float) (5 * size(player)), Kind.SHOCKWAVE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.4f, 0.5f);
        }
    }

    /** A titan's stomp: everything around you is crushed and bounced. Needs a form. */
    public static final class TitanStomp extends Ability {
        TitanStomp() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(120).stamina(14).mastery(M, 10).masteryGain(0.7));
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

    /** Settle in like a living fortress for ten seconds: you take far less harm, shield yourself and heal. */
    public static final class LivingFortress extends Ability {
        LivingFortress() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(500).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 3f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 0.5f);
        }
    }

    /** Strike down from the treetops at a foe far off: a crushing blow that launches it. Needs a form. */
    public static final class TreetopStrike extends Ability {
        TreetopStrike() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, DARK).cooldown(160).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 12).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 12).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 18f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.0).add(0, 1.0, 0));
                Vfx.beam(ctx.level(), player, player.getEyePosition(), target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.SHOCKWAVE,
                        Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Rear up and come down with your whole weight: the earth shatters far around you. Needs a form. */
    public static final class EarthShatter extends Ability {
        EarthShatter() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, DARK).cooldown(800).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            ZoanKit.stomp(player, player.position(), radius, 22f * ctx.power());
            for (Vec3 at : Kit.circle(player.position(), radius * 0.6, 8, 0)) {
                Vfx.eruption(ctx.level(), player, Kit.ground(ctx.level(), at, 4), 0x8A6A4A, 2.5f, Kind.SHARD);
            }
            Vfx.burst(ctx.level(), player, player.position(), 0x8A6A4A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }

    /** Swing your great neck in a long arc: everything in front of you is swept away. Needs a form. */
    public static final class NeckSweep extends Ability {
        NeckSweep() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 14 * ctx.power(), 60)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 18f * ctx.power());
                Kit.launch(target, ctx.right().scale(1.6).add(0, 0.6, 0));
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(6)), ctx.right(), COLOR, 8f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 2f, 0.4f);
        }
    }
}
