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
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Ryu Ryu no Mi, Model: Pachycephalosaurus. Become a pachycephalosaurus: a domed skull as thick as a fortress wall,
 * and (as Ulti fights) headbutts thrown like mortar shells that leave even giants seeing stars.
 */
public class PachycephalosaurusFruit extends ZoanFruit {
    public static final int COLOR = 0x6A8AA8;
    public static final int DOME = 0xE8C890;
    static final String M = "fruit/grandline:pachycephalosaurus";

    /**
     * @hybrid Pachycephalosaurus-Woman | Grow a domed skull and a tail: stronger and tougher, with a head like a battering ram.
     * @beast Pachycephalosaurus Form | Become a pachycephalosaurus: fast, armoured and built around a skull that flattens walls.
     */
    static final ZoanForms FORMS = bodies("pachycephalosaurus", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.5, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.3, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.8, Operation.ADD_VALUE)
                    .dimensions(1.2f, 1.9f, 1.6f));

    public static final DeferredHolder<Ability, Headbutt> HEADBUTT = ability("pachycephalosaurus_headbutt", Headbutt::new);
    public static final DeferredHolder<Ability, SkullCharge> SKULL_CHARGE = ability("pachycephalosaurus_skull_charge", SkullCharge::new);
    public static final DeferredHolder<Ability, IronSkull> IRON_SKULL = ability("pachycephalosaurus_iron_skull", IronSkull::new);
    public static final DeferredHolder<Ability, Ricochet> RICOCHET = ability("pachycephalosaurus_ricochet", Ricochet::new);
    public static final DeferredHolder<Ability, MeteorHead> METEOR_HEAD = ability("pachycephalosaurus_meteor_head", MeteorHead::new);
    public static final DeferredHolder<Ability, UltiMortar> ULTI_MORTAR = ability("pachycephalosaurus_ulti_mortar", UltiMortar::new);
    public static final DeferredHolder<Ability, HardHeaded> HARD_HEADED = ability("pachycephalosaurus_hard_headed", HardHeaded::new);

    public PachycephalosaurusFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Ryu Ryu no Mi, Model: Pachycephalosaurus", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), HEADBUTT, SKULL_CHARGE, IRON_SKULL, RICOCHET, METEOR_HEAD)
                .awakened(ULTI_MORTAR, HARD_HEADED), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    /** A skull blow: crushing damage, a toss along {@code dir}, and the target is left dazed and seeing stars. */
    static void butt(ServerPlayer player, LivingEntity target, float damage, Vec3 dir, double toss, int dazed) {
        ZoanKit.gore(player, target, damage, dir, toss, 0.4);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        if (dazed > 0) {
            target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, dazed, 0, false, false, true));
        }
        if (player.level() instanceof ServerLevel level) {
            Vfx.impact(level, player, target.getEyePosition(), DOME, 1.2f, Kind.STAR);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Slam your domed skull into whatever is in front of you: a crushing blow that leaves it dizzy. Needs a form. */
    public static final class Headbutt extends Ability {
        Headbutt() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, DOME).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 4).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).ifPresent(target -> butt(player, target, 13f * FORMS.boost(player) * ctx.power(), ctx.horizontalLook(), 1.2, 10));
            Kit.dash(player, 0.6, 0);
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 1.1f);
        }
    }

    /** Put your head down and charge: everything in your path is rammed aside and left dizzy. Needs a form. */
    public static final class SkullCharge extends Ability {
        SkullCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(80).stamina(12).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            double length = 11 * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 1.4, 12f * FORMS.boost(player) * ctx.power(), 1.5)) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            }
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), COLOR, 1.4f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ATTACK, 1.2f, 1.2f);
        }
    }

    /** Harden your skull and neck: for ten seconds you take far less damage and cannot be knocked about. */
    public static final class IronSkull extends Ability {
        IronSkull() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, DOME).cooldown(400).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            Vfx.burst(ctx.level(), player, player.getEyePosition(), DOME, 1.8f, Kind.SPARK, Vfx.RING);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 0.9f);
        }
    }

    /** Ricochet from skull to skull: headbutt up to three nearby foes in turn, bouncing to each one. Needs a form. */
    public static final class Ricochet extends Ability {
        Ricochet() {
            super(props(ActivationType.INSTANT, AbilityIcon.STAR, DOME).cooldown(160).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return AbilityDamage.around(player, player.position(), 8).isEmpty() ? Optional.of(Component.literal("No one to bounce off"))
                    : Optional.empty();
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> left = new ArrayList<>(AbilityDamage.around(player, player.position(), 8));
            Vec3 at = player.position();
            int bounces = Math.min(3 + (ctx.power() > 1.2f ? 1 : 0), left.size());
            for (int i = 0; i < bounces; i++) {
                final Vec3 from = at;
                LivingEntity next = left.stream().min((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from))).orElseThrow();
                left.remove(next);
                Vec3 dir = next.position().subtract(from);
                butt(player, next, 11f * FORMS.boost(player) * ctx.power(), dir, 1.0, 10);
                Vec3 to = next.position().subtract(dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : dir.normalize().scale(1.2));
                Vfx.trail(ctx.level(), player, from.add(0, 1, 0), to.add(0, 1, 0), DOME, 0.8f, Kind.STREAK);
                at = to;
            }
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(GLSounds.BOUNCE.get(), 1.2f, 0.8f);
        }
    }

    /** Leap high and come down skull first on the spot you point at: a crater, and everything there is flattened. */
    public static final class MeteorHead extends Ability {
        MeteorHead() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, DOME).cooldown(240).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 10);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            double radius = 4 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                butt(player, target, 18f * FORMS.boost(player) * ctx.power(), target.position().subtract(at), 1.2, 20);
            }
            Vfx.trail(ctx.level(), player, from.add(0, 3, 0), at.add(0, 1, 0), DOME, 1.0f, Kind.STREAK);
            Vfx.burst(ctx.level(), player, at, 0x8A7A5A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.9f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Fire yourself head first like a mortar shell, crushing and knocking out everything along fourteen blocks. */
    public static final class UltiMortar extends Ability {
        UltiMortar() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 look = player.getLookAngle();
            double length = 14 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.8)) {
                butt(player, target, 22f * ctx.power(), look, 2.2, 40);
            }
            Vec3 end = Kit.ground(ctx.level(), from.add(ctx.horizontalLook().scale(length - 1)), 4);
            player.teleportTo(end.x, end.y, end.z);
            player.resetFallDistance();
            Vfx.beam(ctx.level(), player, from.add(0, 1, 0), end.add(0, 1, 0), DOME, 1.2f, Kind.STREAK, Vfx.BEAM_PARTICLES);
            Vfx.burst(ctx.level(), player, end, DOME, 4f, Kind.SHOCKWAVE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.8f, 1.0f);
        }
    }

    /**
     * Become impossibly stubborn for twenty seconds: blows lose half their force and your skull mends itself.
     *
     * @name Hard-Headed
     */
    public static final class HardHeaded extends Ability {
        HardHeaded() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SKULL, DOME).cooldown(800).stamina(20).staminaPerTick(0.1f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getEyePosition(), DOME, 2.2f, Kind.STAR, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.3f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            if (ctx.activeTicks() % 20 == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1f);
            }
            if (ctx.activeTicks() % 10 == 0) {
                Vfx.impact(ctx.level(), player, player.getEyePosition().add(0, 0.5, 0), DOME, 0.6f, Kind.STAR);
            }
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.5f);
        }
    }
}
