package com.nerodev.grandline.haki;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Haki techniques. Numbers scale with the Haki level (0-100) through {@link HakiManager}; the passive parts (bonus
 * damage, touching Logia, knock-outs) live there too so they apply to every attack, not only to these abilities.
 */
public final class HakiMoves {
    static final String OBS = HakiType.OBSERVATION.masteryKey();
    static final String ARM = HakiType.ARMAMENT.masteryKey();
    static final String CONQ = HakiType.CONQUERORS.masteryKey();

    private HakiMoves() {}

    private static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.HAKI).icon(icon, color);
    }

    private static void addModifier(ServerPlayer player, Holder<Attribute> attribute,
            ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute,
            ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Observation
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Sense every living presence nearby, through walls (hostile ones outlined red). While active, attacks from
     * living enemies can be read and dodged: the chance grows with Observation level and each dodge costs stamina.
     */
    public static final class Observation extends Ability {
        static final float DODGE_COST = 6f;

        Observation() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, 0x7FD8FF).cooldown(40).stamina(5).staminaPerTick(0.03f)
                    .maxActive(20 * 60 * 10).mastery(OBS, 0).masteryGain(0.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ctx.fx(GLEffects.OBSERVATION_PULSE, FxParams.at(player.position()).color(0x7FD8FF)
                    .scale((float) HakiManager.senseRadius(player)));
            ctx.sound(GLSounds.HAKI_SENSE.get(), 0.8f, 1.0f);
            ctx.animate(GLEffects.ANIM_FOCUS);
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            ServerPlayer player = ctx.player();
            Entity attacker = event.getSource().getEntity();
            if (attacker == null || attacker == player || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return;
            }
            if (player.getRandom().nextDouble() >= HakiManager.dodgeChance(player)) {
                return;
            }
            if (!HakiManager.spend(player, this, DODGE_COST)) {
                return;
            }
            event.setCanceled(true);
            HakiManager.sidestep(player, attacker);
            HakiManager.train(player, HakiType.OBSERVATION, 0.3);
        }
    }

    /**
     * See a few seconds into the future: every attack aimed at you is read before it lands and avoided, at a small
     * stamina cost each. Nearby enemies show where they are about to move. Needs Observation level 60.
     */
    public static final class FutureSight extends Ability {
        static final float DODGE_COST = 5f;

        FutureSight() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, 0xC8F4FF).cooldown(600).stamina(25)
                    .maxActive(140).mastery(OBS, 60).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            HakiManager.setBit(player, PublicState.HAKI_FUTURE_SIGHT, true);
            ctx.fx(GLEffects.FUTURE_SIGHT, FxParams.at(player.getEyePosition()).color(0xC8F4FF));
            ctx.sound(GLSounds.FUTURE_SIGHT.get(), 1.0f, 1.0f);
            ctx.animate(GLEffects.ANIM_FOCUS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return ctx.activeTicks() < HakiManager.futureSightTicks(ctx.player());
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            ServerPlayer player = ctx.player();
            Entity attacker = event.getSource().getEntity();
            if (attacker == null || attacker == player || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return;
            }
            if (!HakiManager.spend(player, this, DODGE_COST)) {
                return;
            }
            event.setCanceled(true);
            HakiManager.sidestep(player, attacker);
            HakiManager.train(player, HakiType.OBSERVATION, 0.2);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            HakiManager.setBit(ctx.player(), PublicState.HAKI_FUTURE_SIGHT, false);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Armament
    // ---------------------------------------------------------------------------------------------------------

    /** Coat your arms (and whatever they hold) in black Haki: harder blows that can strike a Logia's true body. */
    public static final class ArmamentArms extends Ability {
        ArmamentArms() {
            super(props(ActivationType.TOGGLE, AbilityIcon.FIST, 0x8A63D2).cooldown(40).stamina(5).staminaPerTick(0.025f)
                    .maxActive(20 * 60 * 10).mastery(ARM, 0).masteryGain(0.2));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            HakiManager.setBit(player, PublicState.HAKI_ARM, true);
            ctx.fx(GLEffects.HAKI_COAT, FxParams.at(player.position()).color(0x8A63D2).variant(0));
            ctx.sound(GLSounds.HAKI_HARDEN.get(), 0.9f, 1.1f);
            ctx.animate(GLEffects.ANIM_HAKI_COAT);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            if (!HakiManager.isActive(player, HakiAbilities.FULL_BODY.get())) {
                HakiManager.setBit(player, PublicState.HAKI_ARM, false);
                ctx.fx(GLEffects.HAKI_COAT, FxParams.at(player.position()).color(0x8A63D2).variant(2));
            }
        }
    }

    /**
     * Harden the whole body: incoming damage is reduced (more at higher level) and knockback mostly ignored, and
     * every blow counts as Armament. Drains stamina faster. Needs Armament level 35.
     */
    public static final class FullBody extends Ability {
        static final ResourceLocation ANCHOR = GrandLine.id("haki_full_body_anchor");
        static final float STAMINA_PER_BLOCK = 1.5f;

        FullBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, 0x6A4FB0).cooldown(200).stamina(12).staminaPerTick(0.09f)
                    .maxActive(20 * 60 * 3).mastery(ARM, 35).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            HakiManager.setBit(player, PublicState.HAKI_ARM, true);
            HakiManager.setBit(player, PublicState.HAKI_FULL_BODY, true);
            addModifier(player, Attributes.KNOCKBACK_RESISTANCE, ANCHOR, 0.6, AttributeModifier.Operation.ADD_VALUE);
            ctx.fx(GLEffects.HAKI_COAT, FxParams.at(player.position()).color(0x6A4FB0).variant(1));
            ctx.sound(GLSounds.HAKI_HARDEN.get(), 1.0f, 0.8f);
            ctx.animate(GLEffects.ANIM_HAKI_COAT);
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
                return;
            }
            ServerPlayer player = ctx.player();
            event.setAmount(event.getAmount() * (1f - HakiManager.hardeningReduction(player)));
            HakiManager.spend(player, this, STAMINA_PER_BLOCK);
            Fx.play(player.serverLevel(), GLEffects.HAKI_IMPACT, player,
                    FxParams.at(player.position().add(0, player.getBbHeight() * 0.6, 0)).color(0x6A4FB0).variant(1));
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 0.6f, 1.5f);
            HakiManager.train(player, HakiType.ARMAMENT, 0.1);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, ANCHOR);
            HakiManager.setBit(player, PublicState.HAKI_FULL_BODY, false);
            boolean arms = HakiManager.isActive(player, HakiAbilities.ARMAMENT.get());
            HakiManager.setBit(player, PublicState.HAKI_ARM, arms);
            ctx.fx(GLEffects.HAKI_COAT, FxParams.at(player.position()).color(0x6A4FB0).variant(arms ? 4 : 2));
        }
    }

    /**
     * Advanced Armament (emission): your next blow within a few seconds sends Haki through the target, ignoring
     * armour and blasting it back from the inside. Needs Armament level 70 and an active coating.
     */
    public static final class Emission extends Ability {
        Emission() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, 0xA77BFF).cooldown(240).stamina(15)
                    .mastery(ARM, 70).masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return HakiManager.armamentActive(player) ? Optional.empty()
                    : Optional.of(Component.literal("Coat yourself with Armament Haki first"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            HakiManager.primeEmission(player);
            ctx.fx(GLEffects.HAKI_COAT, FxParams.of(ctx.handPos(), player.getLookAngle()).color(0xA77BFF).variant(3));
            ctx.sound(GLSounds.HAKI_HARDEN.get(), 0.8f, 1.6f);
            ctx.animate(GLEffects.ANIM_HAKI_COAT);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Conqueror's
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Hold to gather your will, release to unleash it. Weak-willed creatures within range collapse unconscious,
     * stronger ones are intimidated, and another Conqueror's user clashes with you. Longer charge and higher level
     * mean a wider wave that floors tougher foes.
     */
    public static final class ConquerorsHaki extends Ability {
        public static final int FULL_CHARGE = 60;
        static final ResourceLocation ROOTED = GrandLine.id("conquerors_rooted");

        ConquerorsHaki() {
            super(props(ActivationType.CHARGE, AbilityIcon.CROWN, 0xE0182C).cooldown(600).stamina(30).minCharge(10)
                    .maxActive(FULL_CHARGE).mastery(CONQ, 0).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            addModifier(player, Attributes.MOVEMENT_SPEED, ROOTED, -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            ctx.animate(GLEffects.ANIM_CONQUEROR);
            ctx.sound(GLSounds.CONQUEROR_CHARGE.get(), 1.0f, 1.0f);
            ctx.fx(GLEffects.CONQUEROR_CHARGE, FxParams.at(player.position()).color(0xE0182C).scale(0f));
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() > 0 && ctx.activeTicks() % 4 == 0) {
                float charge = Mth.clamp(ctx.activeTicks() / (float) FULL_CHARGE, 0f, 1f);
                ctx.fx(GLEffects.CONQUEROR_CHARGE, FxParams.at(ctx.player().position()).color(0xE0182C).scale(charge));
            }
            return true;
        }

        @Override
        public void onRelease(AbilityContext ctx) {
            float charge = Mth.clamp(ctx.activeTicks() / (float) FULL_CHARGE, 0.2f, 1f);
            HakiManager.conquerorsWave(ctx.player(), charge);
            ctx.animate(GLEffects.ANIM_CONQUEROR_RELEASE);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            removeModifier(ctx.player(), Attributes.MOVEMENT_SPEED, ROOTED);
        }
    }

    /**
     * Wrap your attacks in Conqueror's Haki: black lightning crackles off every blow, which hit much harder, hurl
     * the target back and touch a Logia. Two infused fighters clash on contact. Needs Conqueror's level 60.
     */
    public static final class Infusion extends Ability {
        Infusion() {
            super(props(ActivationType.TOGGLE, AbilityIcon.BOLT, 0xFF2A3A).cooldown(400).stamina(20).staminaPerTick(0.12f)
                    .maxActive(20 * 60).mastery(CONQ, 60).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            HakiManager.setBit(player, PublicState.HAKI_INFUSION, true);
            ctx.fx(GLEffects.CONQUEROR_STRIKE, FxParams.at(player.position().add(0, 1, 0)).color(0xFF2A3A).scale(0.6f));
            ctx.sound(GLSounds.HAKI_CRACKLE.get(), 1.0f, 0.8f);
            ctx.animate(GLEffects.ANIM_HAKI_COAT);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            HakiManager.setBit(ctx.player(), PublicState.HAKI_INFUSION, false);
        }
    }
}
