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
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Hebi Hebi no Mi, Model: Anaconda. Become a great anaconda: coils that crush and hold, a tail like a whip, a strong
 * swimmer, and a jaw that can swallow a small foe whole.
 */
public class AnacondaFruit extends ZoanFruit {
    public static final int COLOR = 0x4A7A3A;
    public static final int BELLY = 0xC8C070;
    static final String M = "fruit/grandline:anaconda";

    /**
     * @hybrid Snake-Woman | Your legs become a great snake's tail: tougher, stronger, and at home in water.
     * @beast Anaconda Form | Become an anaconda: long, heavy, very tough, and a swift swimmer.
     */
    static final ZoanForms FORMS = bodies("anaconda", COLOR, () -> SoundEvents.CAT_HISS, ZoanForms.Traits.SWIMMER,
            b -> b.modifier(Attributes.SCALE, 0.2, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 3, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 14, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.6, Operation.ADD_VALUE)
                    .dimensions(1.0f, 0.9f, 0.7f));

    public static final DeferredHolder<Ability, Constrict> CONSTRICT = ability("anaconda_constrict", Constrict::new);
    public static final DeferredHolder<Ability, TailWhip> TAIL_WHIP = ability("anaconda_tail_whip", TailWhip::new);
    public static final DeferredHolder<Ability, StrikingFang> STRIKING_FANG = ability("anaconda_striking_fang", StrikingFang::new);
    public static final DeferredHolder<Ability, CoilGuard> COIL_GUARD = ability("anaconda_coil_guard", CoilGuard::new);
    public static final DeferredHolder<Ability, Swallow> SWALLOW = ability("anaconda_swallow", Swallow::new);
    public static final DeferredHolder<Ability, CrushingCoils> CRUSHING_COILS = ability("anaconda_crushing_coils", CrushingCoils::new);
    public static final DeferredHolder<Ability, SerpentDance> SERPENT_DANCE = ability("anaconda_serpent_dance", SerpentDance::new);

    public AnacondaFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.RARE, "Hebi Hebi no Mi, Model: Anaconda", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), CONSTRICT, TAIL_WHIP, STRIKING_FANG, COIL_GUARD, SWALLOW)
                .awakened(CRUSHING_COILS, SERPENT_DANCE), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Hold to wrap your coils around the nearest foe: it is held fast and squeezed harder every half second. Needs a
     * form.
     */
    public static final class Constrict extends Ability {
        Constrict() {
            super(props(ActivationType.HOLD, AbilityIcon.GRAB, COLOR).cooldown(160).stamina(10).staminaPerTick(0.3f).maxActive(80)
                    .masteryGain(0.8));
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
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.CAT_HISS, 1f, 0.7f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            var near = AbilityDamage.around(player, player.position(), 4);
            if (near.isEmpty()) {
                return false;
            }
            LivingEntity target = near.get(0);
            Kit.root(target, Vfx.StatusLook.STRINGS, 12);
            if (ctx.activeTicks() % 10 == 0) {
                float squeeze = (2f + ctx.activeTicks() / 20f) * FORMS.boost(player) * ctx.power();
                Kit.hit(player, target, AbilityDamage.CRUSH, squeeze);
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 1.2f, Kind.RING);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Lash your tail across everything in front of you. Needs a form. */
    public static final class TailWhip extends Ability {
        TailWhip() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, BELLY).cooldown(35).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 5, 70)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.right().scale(1.2).add(0, 0.35, 0));
            }
            Vfx.slash(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(2)).add(0, 0.6, 0), ctx.right(), COLOR, 3f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 0.8f, 0.8f);
        }
    }

    /** Strike like a snake: a lunging bite on a foe in reach that leaves it bleeding. Needs a form. */
    public static final class StrikingFang extends Ability {
        StrikingFang() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(50).stamina(9).mastery(M, 10).masteryGain(0.7));
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
                ZoanKit.maul(player, target, 8f * FORMS.boost(player) * ctx.power(), 4, 1);
                Vfx.trail(ctx.level(), player, player.getEyePosition(), target.getEyePosition(), COLOR, 0.6f, Kind.STREAK);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.CAT_HISS, 1.2f, 1.1f);
        }
    }

    /** Coil up tight for five seconds: you take far less harm, and anything that strikes you is squeezed for it. */
    public static final class CoilGuard extends Ability {
        CoilGuard() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(240).stamina(12).staminaPerTick(0.2f).maxActive(100)
                    .mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.player().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, false));
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), COLOR, 2f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.sound(SoundEvents.CAT_HISS, 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return true;
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if ((event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
                    && event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                Kit.hit(ctx.player(), attacker, AbilityDamage.CRUSH, 4f);
                Kit.root(attacker, Vfx.StatusLook.STRINGS, 20);
            }
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ctx.player().removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
    }

    /**
     * Swallow a small foe whole for three seconds: it can do nothing and is digested a little. Big foes and bosses
     * won't fit. Needs the full beast.
     */
    public static final class Swallow extends Ability {
        Swallow() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, BELLY).cooldown(400).stamina(18).mastery(M, 35).masteryGain(1.0));
        }

        static boolean fits(LivingEntity target) {
            return target.getBbHeight() <= 2.1f && target.getBbWidth() <= 1.2f && target.getMaxHealth() <= 40
                    && !target.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                    && !target.getType().is(EntityTypeTags.RAIDERS);
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.beast(player)) {
                return FORMS.needsBeast(player);
            }
            return Kit.target(player, 4).filter(Swallow::fits).isPresent() ? Optional.empty()
                    : Optional.of(Component.literal("Nothing small enough within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4).filter(Swallow::fits).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.MUD, 60);
                target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                Kit.hit(player, target, AbilityDamage.DRAIN, 8f * ctx.power());
                player.heal(4f);
            });
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.GENERIC_EAT, 1.4f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Throw your coils around everything nearby and squeeze: all are held fast and crushed. Needs a form. */
    public static final class CrushingCoils extends Ability {
        CrushingCoils() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 6 * ctx.power())) {
                Kit.root(target, Vfx.StatusLook.STRINGS, 80);
                Kit.hit(player, target, AbilityDamage.CRUSH, 12f * ctx.power());
            }
            Vfx.spiral(ctx.level(), player, player.position().add(0, 0.5, 0), COLOR, 6f, Kind.RING);
            ctx.animate(GLEffects.ANIM_CHARGE_BOTH);
            ctx.sound(SoundEvents.CAT_HISS, 1.6f, 0.5f);
        }
    }

    /** Whirl your whole body around: everything nearby is battered and flung away. Needs a form. */
    public static final class SerpentDance extends Ability {
        SerpentDance() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, BELLY).cooldown(500).stamina(24).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 14f * ctx.power());
                Vec3 away = target.position().subtract(player.position()).normalize();
                Kit.launch(target, away.scale(1.3).add(0, 0.6, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 0.6, 0), COLOR, 5f, Kind.SHOCKWAVE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.4f, 0.6f);
        }
    }
}
