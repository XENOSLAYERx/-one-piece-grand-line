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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Ushi Ushi no Mi, Model: Gyuki. Become the ox demon of legend, as Saint Jaygarcia Saturn does: an ox's horned head
 * on a spider's body, legs that stab like spears, poisonous breath, and the elders' body that will not stay wounded.
 */
public class GyukiFruit extends GoroseiFruit {
    public static final int COLOR = 0x2A2A2A;
    public static final int BONE = 0xE8E8E8;
    public static final int POISON = 0x8AC83A;
    static final String M = "fruit/grandline:gyuki";

    /**
     * @hybrid Gyuki Hybrid | Grow an ox demon's horns and spider legs from your back: far stronger and very hard to hurt.
     * @beast Gyuki Form | Become the ox demon: a giant spider's body under an ox's torso, climbing walls and healing fast.
     */
    static final ZoanForms FORMS = bodies("gyuki", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.CLIMBER,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.6, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 30, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 9, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.STEP_HEIGHT, 1.5, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .dimensions(3.0f, 3.2f, 2.8f));

    public static final DeferredHolder<Ability, LegImpale> LEG_IMPALE = ability("gyuki_leg_impale", LegImpale::new);
    public static final DeferredHolder<Ability, DemonHorns> DEMON_HORNS = ability("gyuki_demon_horns", DemonHorns::new);
    public static final DeferredHolder<Ability, Pentagram> PENTAGRAM = ability("gyuki_pentagram", Pentagram::new);
    public static final DeferredHolder<Ability, PoisonBreath> POISON_BREATH = ability("gyuki_poison_breath", PoisonBreath::new);
    public static final DeferredHolder<Ability, LegStorm> LEG_STORM = ability("gyuki_leg_storm", LegStorm::new);
    public static final DeferredHolder<Ability, DemonRitual> DEMON_RITUAL = ability("gyuki_demon_ritual", DemonRitual::new);
    public static final DeferredHolder<Ability, UndyingBody> UNDYING_BODY = ability("gyuki_undying_body", UndyingBody::new);

    public GyukiFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Ushi Ushi no Mi, Model: Gyuki", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), LEG_IMPALE, DEMON_HORNS, PENTAGRAM, POISON_BREATH, LEG_STORM)
                .awakened(DEMON_RITUAL, UNDYING_BODY), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double size(ServerPlayer player) {
        return FORMS.beast(player) ? 1.5 : 1.0;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Drive a spider leg straight through everything in front of you like a spear. Needs a form. */
    public static final class LegImpale extends Ability {
        LegImpale() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, BONE).cooldown(35).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 7 * size(player);
            for (LivingEntity target : AbilityDamage.line(player, length, 1.2)) {
                ZoanKit.maul(player, target, 13f * FORMS.boost(player) * ctx.power(), 4, 0);
            }
            Vec3 from = player.getEyePosition().add(0, 0.6, 0).add(ctx.right().scale(0.8));
            Vfx.trail(ctx.level(), player, from, player.getEyePosition().add(player.getLookAngle().scale(length)), COLOR, 0.6f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.CLAW.get(), 1.2f, 0.6f);
        }
    }

    /** Lower your demon's horns and charge: everything in your path is gored aside. Needs a form. */
    public static final class DemonHorns extends Ability {
        DemonHorns() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, BONE).cooldown(90).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 12 * ctx.power();
            ZoanKit.charge(player, length, 1.8 * size(player), 15f * FORMS.boost(player) * ctx.power(), 1.6);
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), COLOR, 2f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.6f);
        }
    }

    /** Step into a pentagram and out of another wherever you look, up to thirty-two blocks away. */
    public static final class Pentagram extends PentagramStep {
        Pentagram() {
            super(SIGIL, M);
        }
    }

    /** Breathe out the ox demon's poison: everything in front of you is sickened, weakened and wasting. Needs a form. */
    public static final class PoisonBreath extends Ability {
        PoisonBreath() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, POISON).cooldown(160).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = 9 * size(player) * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, reach, 50)) {
                Kit.hit(player, target, AbilityDamage.POISON, 8f * FORMS.boost(player) * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            }
            Vfx.beam(ctx.level(), player, player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(reach)), POISON,
                    2.2f, Kind.SMOKE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.GAS_HISS.get(), 1.4f, 0.6f);
        }
    }

    /** Stab every leg down around you, again and again: everything nearby is run through. Needs a form. */
    public static final class LegStorm extends Ability {
        LegStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(200).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 6 * size(player) * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                for (int i = 0; i < 4; i++) {
                    ZoanKit.maul(player, target, 4.5f * FORMS.boost(player) * ctx.power(), 4, 1);
                }
                Vfx.trail(ctx.level(), player, player.getEyePosition().add(0, 1.5, 0), target.position(), COLOR, 0.5f, Kind.STREAK);
            }
            Vfx.burst(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(GLSounds.CLAW.get(), 1.6f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Draw a great pentagram where you point: everything on it is struck by dark power and pinned in place. */
    public static final class DemonRitual extends Ability {
        DemonRitual() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, SIGIL).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 10);
            double radius = 8 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.hit(player, target, AbilityDamage.DARK, 22f * ctx.power());
                Kit.root(target, Vfx.StatusLook.STRINGS, 60);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            sigil(ctx, at, (float) radius);
            Vfx.eruption(ctx.level(), player, at, SIGIL, 6f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 2f, 0.5f);
        }
    }

    /** For twenty seconds the elder's body refuses to be hurt: blows lose much of their force and wounds close fast. */
    public static final class UndyingBody extends Ability {
        UndyingBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.HEART, SIGIL).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), SIGIL, 3f, Kind.SMOKE, Vfx.RING | Vfx.FLASH);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 1.2f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            if (ctx.activeTicks() % 10 == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1f);
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), SIGIL, 1.2f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.6f);
        }
    }
}
