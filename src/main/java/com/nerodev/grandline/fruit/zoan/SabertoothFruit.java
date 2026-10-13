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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Neko Neko no Mi, Model: Sabertooth. Become an ancient sabertooth tiger: an Ancient Zoan's toughness, a pounce that
 * covers a field, and fangs made to finish wounded prey.
 */
public class SabertoothFruit extends ZoanFruit {
    public static final int COLOR = 0xE8A040;
    public static final int DARK = 0x3A2A1A;
    static final String M = "fruit/grandline:sabertooth";

    /**
     * @hybrid Sabertooth-Man | Grow sabre fangs and a tiger's strength: much harder-hitting, quicker and tougher.
     * @beast Sabertooth Form | Become an ancient sabertooth: a heavy, fast predator and a great leaper.
     */
    static final ZoanForms FORMS = bodies("sabertooth", COLOR, GLSounds.LEOPARD_GROWL, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.2, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 6, Operation.ADD_VALUE)
                    .dimensions(1.2f, 1.4f, 1.2f));

    public static final DeferredHolder<Ability, SaberFangs> SABER_FANGS = ability("sabertooth_saber_fangs", SaberFangs::new);
    public static final DeferredHolder<Ability, Pounce> POUNCE = ability("sabertooth_pounce", Pounce::new);
    public static final DeferredHolder<Ability, AncientRoar> ANCIENT_ROAR = ability("sabertooth_ancient_roar", AncientRoar::new);
    public static final DeferredHolder<Ability, RendingClaws> RENDING_CLAWS = ability("sabertooth_rending_claws", RendingClaws::new);
    public static final DeferredHolder<Ability, KillingBite> KILLING_BITE = ability("sabertooth_killing_bite", KillingBite::new);
    public static final DeferredHolder<Ability, SaberStorm> SABER_STORM = ability("sabertooth_saber_storm", SaberStorm::new);
    public static final DeferredHolder<Ability, ApexPredator> APEX_PREDATOR = ability("sabertooth_apex_predator", ApexPredator::new);

    public SabertoothFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Neko Neko no Mi, Model: Sabertooth", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), SABER_FANGS, POUNCE, ANCIENT_ROAR, RENDING_CLAWS, KILLING_BITE)
                .awakened(SABER_STORM, APEX_PREDATOR), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    // ---------------------------------------------------------------------------------------------------------

    /** Drive your sabres into whatever is in front of you: deep wounds that bleed hard. Needs a form. */
    public static final class SaberFangs extends Ability {
        SaberFangs() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(25).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.6, 45)) {
                ZoanKit.maul(player, target, 11f * FORMS.boost(player) * ctx.power(), 5, 1);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(player.getEyePosition().add(player.getLookAngle().scale(1.5)), player.getLookAngle())
                    .color(0xFFF0D0));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.LEOPARD_GROWL.get(), 1f, 0.8f);
        }
    }

    /** A great leap at your prey; the first foe you land on is mauled and pinned. Needs a form. */
    public static final class Pounce extends Ability {
        Pounce() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH, DARK).cooldown(45).stamina(14).maxActive(16).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            double speed = FORMS.beast(player) ? 1.9 : 1.5;
            player.setDeltaMovement(look.x * speed, Math.max(0.45, look.y * speed + 0.35), look.z * speed);
            player.hurtMarked = true;
            ctx.fx(GLEffects.POUNCE, FxParams.of(player.position(), look).color(COLOR));
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(GLSounds.LEOPARD_GROWL.get(), 0.9f, 1.1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.getBoundingBox().getCenter(), 1.5)) {
                ZoanKit.maul(player, target, 11f * FORMS.boost(player) * ctx.power(), 4, 1);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
                ctx.sound(GLSounds.CLAW.get(), 1f, 0.8f);
                return false;
            }
            return !(ctx.activeTicks() > 3 && player.onGround());
        }
    }

    /** An ancient roar: everything around you is driven back, weakened and slowed. Needs a form. */
    public static final class AncientRoar extends Ability {
        AncientRoar() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(240).stamina(14).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ZoanKit.roar(player, 9 * ctx.power(), 3f * ctx.power(), 140);
            ctx.fx(GLEffects.ROAR, FxParams.of(player.getEyePosition(), player.getLookAngle()).color(COLOR).scale(9f));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1.6f, 0.7f);
        }
    }

    /** Rake with every claw: a flurry of cuts that tear armour and leave foes bleeding badly. Needs a form. */
    public static final class RendingClaws extends Ability {
        RendingClaws() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, DARK).cooldown(80).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4, 60)) {
                ZoanKit.maul(player, target, 14f * FORMS.boost(player) * ctx.power(), 6, 2);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.at(player.position().add(player.getLookAngle().scale(1.8)).add(0, 1, 0)).color(0xFFF0D0)
                    .variant(1).scale(2.6f));
            ctx.animate(GLEffects.ANIM_FRENZY);
            ctx.sound(GLSounds.CLAW.get(), 1.2f, 0.7f);
        }
    }

    /** A killing bite on a foe in reach: wounded prey (below half health) takes half again as much. Needs a form. */
    public static final class KillingBite extends Ability {
        KillingBite() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, COLOR).cooldown(200).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                boolean wounded = target.getHealth() < target.getMaxHealth() / 2;
                ZoanKit.maul(player, target, 20f * (wounded ? 1.5f : 1f) * FORMS.boost(player) * ctx.power(), 6, 2);
                Vfx.burst(ctx.level(), player, target.getEyePosition(), 0xC82A2A, wounded ? 2f : 1.2f, Kind.DROP, wounded ? Vfx.HEAVY : 0);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Hold to whirl into a storm of sabres and claws, cutting everything around you. Needs a form. */
    public static final class SaberStorm extends Ability {
        SaberStorm() {
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
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 3.8)) {
                ZoanKit.maul(player, target, 4f * ctx.power(), 4, 1);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.at(player.position().add(0, 1, 0)).color(0xFFF0D0).variant(1).scale(3.8f));
            if (ctx.activeTicks() % 6 == 0) {
                ctx.sound(GLSounds.CLAW.get(), 0.8f, 1.1f);
            }
            return FORMS.any(player);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Become the apex predator for thirty seconds: stronger, faster, tougher and healing. */
    public static final class ApexPredator extends Ability {
        ApexPredator() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, DARK).cooldown(1200).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
            ctx.fx(GLEffects.TRANSFORM_PUFF, FxParams.at(player.position()).color(0xFF4A2A).scale(1.2f).variant(1));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.LEOPARD_ROAR.get(), 1.4f, 0.8f);
        }
    }
}
