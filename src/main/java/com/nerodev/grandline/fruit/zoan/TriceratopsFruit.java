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
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.Fx;
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
 * Ryu Ryu no Mi, Model: Triceratops. Become a triceratops: three horns that gore through anything, a frill as hard as
 * a shield, and (as Sasaki showed) a frill that spins like a rotor and carries its owner into the air.
 */
public class TriceratopsFruit extends ZoanFruit {
    public static final int COLOR = 0x5A6A3A;
    public static final int HORN = 0xE8DCC0;
    public static final int FRILL = 0xC86A3A;
    static final String M = "fruit/grandline:triceratops";

    static final Buffs.Mod CRACKED = Buffs.mod(Attributes.ARMOR, GrandLine.id("triceratops_cracked_armor"), -0.4,
            Operation.ADD_MULTIPLIED_TOTAL);

    /**
     * @hybrid Triceratops-Man | Grow three horns and a bony frill: much stronger and tougher, and hard to push around.
     * @beast Triceratops Form | Become a triceratops: a huge, heavily armoured charger that is almost impossible to move.
     */
    static final ZoanForms FORMS = bodies("triceratops", COLOR, () -> SoundEvents.HOGLIN_ANGRY, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.5, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.ARMOR, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(2.0f, 2.4f, 1.8f));

    public static final DeferredHolder<Ability, HornCharge> HORN_CHARGE = ability("triceratops_horn_charge", HornCharge::new);
    public static final DeferredHolder<Ability, FrillShield> FRILL_SHIELD = ability("triceratops_frill_shield", FrillShield::new);
    public static final DeferredHolder<Ability, ThreeHornGore> THREE_HORN_GORE = ability("triceratops_three_horn_gore",
            ThreeHornGore::new);
    public static final DeferredHolder<Ability, FrillSpin> FRILL_SPIN = ability("triceratops_frill_spin", FrillSpin::new);
    public static final DeferredHolder<Ability, Stampede> STAMPEDE = ability("triceratops_stampede", Stampede::new);
    public static final DeferredHolder<Ability, HelicopterCeratops> HELICOPTER = ability("triceratops_helicopter",
            HelicopterCeratops::new);
    public static final DeferredHolder<Ability, TripleImpact> TRIPLE_IMPACT = ability("triceratops_triple_impact", TripleImpact::new);

    public TriceratopsFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Ryu Ryu no Mi, Model: Triceratops", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), HORN_CHARGE, FRILL_SHIELD, THREE_HORN_GORE, FRILL_SPIN, STAMPEDE)
                .awakened(HELICOPTER, TRIPLE_IMPACT), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double size(ServerPlayer player) {
        return FORMS.beast(player) ? 1.3 : 1.0;
    }

    /** A horn blow that also cracks the target's armour for five seconds. */
    static void pierce(ServerPlayer player, LivingEntity target, float damage, Vec3 dir, double toss) {
        ZoanKit.gore(player, target, damage, dir, toss, 0.5);
        Buffs.timed(target, CRACKED, 100);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Lower your horns and charge: everything in your path is gored and its armour cracked. Needs a form. */
    public static final class HornCharge extends Ability {
        HornCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, HORN).cooldown(80).stamina(12).masteryGain(0.6));
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
            for (LivingEntity target : ZoanKit.charge(player, length, 1.6 * size(player), 14f * FORMS.boost(player) * ctx.power(), 1.4)) {
                Buffs.timed(target, CRACKED, 100);
            }
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), 0x8A7A5A, 1.8f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.HOGLIN_ATTACK, 1.4f, 0.6f);
        }
    }

    /** Hold your frill up as a shield: you can barely move, but blows that reach you lose three quarters of their force. */
    public static final class FrillShield extends Ability {
        FrillShield() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, FRILL).cooldown(160).stamina(10).staminaPerTick(0.3f).maxActive(120)
                    .masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FRILL, 2.2f, Kind.SHARD, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false));
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.25f);
            Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FRILL, 1.2f, Kind.SPARK);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /**
     * Thrust all three horns forward: everything in front of you is gored, thrown and left with cracked armour.
     *
     * @name Three-Horn Gore
     */
    public static final class ThreeHornGore extends Ability {
        ThreeHornGore() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, HORN).cooldown(70).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4 * size(player), 50)) {
                pierce(player, target, 14f * FORMS.boost(player) * ctx.power(), ctx.horizontalLook(), 1.6);
            }
            Vec3 tip = player.getEyePosition().add(ctx.horizontalLook().scale(2 * size(player)));
            for (int i = -1; i <= 1; i++) {
                Vec3 off = ctx.right().scale(i * 0.6).add(0, i == 0 ? 0.5 : 0, 0);
                Vfx.trail(ctx.level(), player, player.getEyePosition().add(off), tip.add(ctx.horizontalLook().scale(2)).add(off), HORN, 0.5f,
                        Kind.STREAK);
            }
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.7f);
        }
    }

    /** Spin your frill like a rotor blade: it cuts everything around you while the lift carries you up. */
    public static final class FrillSpin extends Ability {
        FrillSpin() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, FRILL).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 3.5 * size(player))) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * FORMS.boost(player) * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.0, 0.3);
            }
            Kit.launch(player, new Vec3(0, 1.4, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
            Vfx.spiral(ctx.level(), player, player.position(), FRILL, 3f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BREEZE_WHIRL, 1.4f, 0.6f);
        }
    }

    /** A thundering stampede of horns: everything in a long path is gored, cracked and stunned. Needs a form. */
    public static final class Stampede extends Ability {
        Stampede() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(200).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 16 * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 2.2 * size(player), 16f * FORMS.boost(player) * ctx.power(), 1.8)) {
                Buffs.timed(target, CRACKED, 100);
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 30, 0, false, false, true));
            }
            Vec3 dir = ctx.horizontalLook();
            for (int i = 1; i <= 4; i++) {
                Vfx.burst(ctx.level(), player, Kit.ground(ctx.level(), from.add(dir.scale(i * length / 4)), 4), 0x8A7A5A, 2.2f, Kind.SHARD,
                        Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Spin your frill without stopping and fly like a helicopter for up to thirty seconds. Needs a form.
     *
     * @name Helicopter Ceratops
     */
    public static final class HelicopterCeratops extends Ability {
        HelicopterCeratops() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TORNADO, FRILL).cooldown(600).stamina(16).staminaPerTick(0.15f).maxActive(600)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getEyePosition(), FRILL, 2.4f, Kind.STREAK, Vfx.RING);
            ctx.sound(SoundEvents.BREEZE_WHIRL, 1.4f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            player.resetFallDistance();
            if (ctx.activeTicks() % 5 == 0 && !player.onGround()) {
                Vfx.impact(ctx.level(), player, player.getEyePosition().add(0, 0.4, 0), FRILL, 1.4f, Kind.STREAK);
            }
            if (ctx.activeTicks() % 20 == 0) {
                ctx.sound(SoundEvents.BREEZE_WHIRL, 0.6f, 1.2f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /** Drive all three horns home with your full weight: a crushing gore across a wide cone that shatters armour. */
    public static final class TripleImpact extends Ability {
        TripleImpact() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, HORN).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * size(player) * ctx.power(), 70)) {
                pierce(player, target, 22f * ctx.power(), look, 2.4);
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 40, 0, false, false, true));
            }
            Vec3 at = player.position().add(look.scale(3 * size(player)));
            Vfx.burst(ctx.level(), player, at, HORN, 5f, Kind.SHOCKWAVE, Vfx.RING | Vfx.HEAVY);
            Vfx.eruption(ctx.level(), player, at, 0x8A7A5A, 3f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.8f, 0.6f);
        }
    }
}
