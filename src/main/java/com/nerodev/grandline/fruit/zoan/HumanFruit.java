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
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
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
 * Hito Hito no Mi. The fruit that gave a reindeer a human mind; in a person it unlocks the "points" of a fighting
 * doctor: a hulking Heavy Point, a fur ball that shrugs off blows, antlers, a scope that finds weak points, and the
 * rampaging Monster Point.
 */
public class HumanFruit extends ZoanFruit {
    public static final int COLOR = 0x8AC8E8;
    public static final int FUR = 0x8A5A3A;
    static final String M = "fruit/grandline:human";
    static final String SCOPED_BY = "grandline_scoped_by";
    static final String SCOPED_UNTIL = "grandline_scoped_until";

    /**
     * @hybrid Heavy Point | Bulk up into Heavy Point: half again your size, much stronger and tougher.
     * @beast Monster Point | Become the Monster Point: a towering, shaggy giant with antlers, immensely strong but slow.
     */
    static final ZoanForms FORMS = bodies("human", COLOR, GLSounds.INFLATE, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.5, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.8, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.1, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(1.4f, 3.25f, 2.9f));

    public static final DeferredHolder<Ability, HoofPunch> HOOF_PUNCH = ability("human_hoof_punch", HoofPunch::new);
    public static final DeferredHolder<Ability, GuardPoint> GUARD_POINT = ability("human_guard_point", GuardPoint::new);
    public static final DeferredHolder<Ability, AntlerCharge> ANTLER_CHARGE = ability("human_antler_charge", AntlerCharge::new);
    public static final DeferredHolder<Ability, Scope> SCOPE = ability("human_scope", Scope::new);
    public static final DeferredHolder<Ability, RumbleBall> RUMBLE_BALL = ability("human_rumble_ball", RumbleBall::new);
    public static final DeferredHolder<Ability, Rampage> RAMPAGE = ability("human_rampage", Rampage::new);
    public static final DeferredHolder<Ability, DoctorsCare> DOCTORS_CARE = ability("human_doctors_care", DoctorsCare::new);

    public HumanFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Hito Hito no Mi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), HOOF_PUNCH, GUARD_POINT, ANTLER_CHARGE, SCOPE, RUMBLE_BALL)
                .awakened(RAMPAGE, DOCTORS_CARE), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static boolean scoped(ServerPlayer player, LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        return data.hasUUID(SCOPED_BY) && player.getUUID().equals(data.getUUID(SCOPED_BY))
                && data.getLong(SCOPED_UNTIL) >= target.level().getGameTime();
    }

    /** A weak point found with the scope takes every blow half again as hard. */
    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        super.onDealDamage(user, target, event);
        if (scoped(user, target)) {
            event.setAmount(event.getAmount() * 1.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A hoof-fisted punch that knocks a foe flying. Needs a form. */
    public static final class HoofPunch extends Ability {
        HoofPunch() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, FUR).cooldown(30).stamina(7).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            float boost = FORMS.boost(player);
            for (LivingEntity target : AbilityDamage.cone(player, 3.8 * boost, 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * boost * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.2).add(0, 0.35, 0));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.4)), 0xFFE0B0, 1.6f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 0.9f);
        }
    }

    /** Hold to puff up into a ball of fur: blows barely hurt and can't push you around, but you can hardly move. */
    public static final class GuardPoint extends Ability {
        GuardPoint() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, FUR).cooldown(160).stamina(10).staminaPerTick(0.3f).maxActive(100)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), FUR, 2f, Kind.SMOKE, 0);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.INFLATE.get(), 1f, 0.8f);
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
            event.setAmount(event.getAmount() * 0.3f);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Lower your antlers and charge: everything in your path is gored aside. Needs a form. */
    public static final class AntlerCharge extends Ability {
        AntlerCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, FUR).cooldown(100).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            ZoanKit.charge(player, 9 * ctx.power(), 1.4, 11f * FORMS.boost(player) * ctx.power(), 1.1);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(ctx.horizontalLook().scale(9)).add(0, 1, 0), FUR, 1.2f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 0.7f, 1.4f);
        }
    }

    /** Study a foe through your scope: it glows, and every blow you land on it for fifteen seconds hits half again as hard. */
    public static final class Scope extends Ability {
        Scope() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(200).stamina(8).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 24).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 24).ifPresent(target -> {
                CompoundTag data = target.getPersistentData();
                data.putUUID(SCOPED_BY, player.getUUID());
                data.putLong(SCOPED_UNTIL, ctx.level().getGameTime() + 300);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                Vfx.impact(ctx.level(), player, target.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.RING);
            });
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.SPYGLASS_USE, 1f, 1f);
        }
    }

    /** Swallow a Rumble Ball: you heal and fight stronger and faster for thirty seconds. */
    public static final class RumbleBall extends Ability {
        RumbleBall() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, 0xE84A4A).cooldown(900).stamina(6).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.heal(6f);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), 0xE84A4A, 2f, Kind.STAR, Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.GENERIC_EAT, 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Rampage as the Monster: a crushing blow on everything around you. Needs the Monster Point. */
    public static final class Rampage extends Ability {
        Rampage() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, FUR).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsBeast(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            List<LivingEntity> hit = AbilityDamage.around(player, player.position(), 6 * ctx.power());
            for (LivingEntity target : hit) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 16f * ctx.power());
                Kit.launch(target, target.position().subtract(player.position()).normalize().scale(1.3).add(0, 0.6, 0));
            }
            Vfx.burst(ctx.level(), player, player.position(), FUR, 6f, Kind.SHOCKWAVE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.7f);
        }
    }

    /**
     * A doctor's care for you and every friend nearby: wounds close and poison, wither and sickness are cured.
     *
     * @name Doctor's Care
     */
    public static final class DoctorsCare extends Ability {
        DoctorsCare() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(900).stamina(20).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ZoanKit.friends(ctx.player(), 8)) {
                friend.heal(12f);
                friend.removeEffect(MobEffects.POISON);
                friend.removeEffect(MobEffects.WITHER);
                friend.removeEffect(MobEffects.HUNGER);
                friend.removeEffect(MobEffects.CONFUSION);
                friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
                Vfx.impact(ctx.level(), friend, friend.getBoundingBox().getCenter(), 0x8AE08A, 1.2f, Kind.HEART);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BREWING_STAND_BREW, 1.2f, 1.2f);
        }
    }
}
