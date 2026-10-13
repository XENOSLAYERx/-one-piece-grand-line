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
 * Uma Uma no Mi. Become a horse: the fastest runner and highest jumper of the regular Zoans, with kicks that throw a
 * man across a field. Awakened, the horse grows wings, like the pegasus the fruit made of a bird.
 */
public class HorseFruit extends ZoanFruit {
    public static final int COLOR = 0xE8E0D0;
    public static final int MANE = 0x8A6A4A;
    static final String M = "fruit/grandline:horse";

    /**
     * @hybrid Horse-Man | Grow a horse's ears, mane and legs: quick on your feet, a high jumper and a hard kicker.
     * @beast Horse Form | Become a horse: a very fast galloper that jumps high and steps over obstacles.
     */
    static final ZoanForms FORMS = bodies("horse", COLOR, () -> SoundEvents.HORSE_AMBIENT, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.2, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.55, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.35, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 6, Operation.ADD_VALUE)
                    .dimensions(1.3f, 1.9f, 1.7f));

    public static final DeferredHolder<Ability, HoofKick> HOOF_KICK = ability("horse_hoof_kick", HoofKick::new);
    public static final DeferredHolder<Ability, Gallop> GALLOP = ability("horse_gallop", Gallop::new);
    public static final DeferredHolder<Ability, RearUp> REAR_UP = ability("horse_rear_up", RearUp::new);
    public static final DeferredHolder<Ability, Trample> TRAMPLE = ability("horse_trample", Trample::new);
    public static final DeferredHolder<Ability, Steed> STEED = ability("horse_steed", Steed::new);
    public static final DeferredHolder<Ability, PegasusWings> PEGASUS_WINGS = ability("horse_pegasus_wings", PegasusWings::new);
    public static final DeferredHolder<Ability, ThunderHooves> THUNDER_HOOVES = ability("horse_thunder_hooves", ThunderHooves::new);

    public HorseFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Uma Uma no Mi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), HOOF_KICK, GALLOP, REAR_UP, TRAMPLE, STEED)
                .awakened(PEGASUS_WINGS, THUNDER_HOOVES), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    // ---------------------------------------------------------------------------------------------------------

    /** Kick with your hooves: whatever is in front of you is thrown well away. Needs a form. */
    public static final class HoofKick extends Ability {
        HoofKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, MANE).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.6, 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.7).add(0, 0.5, 0));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.4)), 0xFFFFFF, 1.4f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(SoundEvents.HORSE_ANGRY, 1f, 1f);
        }
    }

    /** Break into a gallop: much faster for ten seconds. */
    public static final class Gallop extends Ability {
        Gallop() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(240).stamina(8).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, FORMS.beast(player) ? 2 : 1));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 0));
            Vfx.burst(ctx.level(), player, player.position(), MANE, 1.6f, Kind.SMOKE, Vfx.GROUND);
            ctx.sound(SoundEvents.HORSE_GALLOP, 1.2f, 1f);
        }
    }

    /** Rear up and slam your forehooves down: everything around you is crushed and bounced. Needs a form. */
    public static final class RearUp extends Ability {
        RearUp() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, MANE).cooldown(120).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ZoanKit.stomp(player, player.position().add(ctx.horizontalLook().scale(1.5)), 4 * ctx.power(),
                    9f * FORMS.boost(player) * ctx.power());
            Vfx.burst(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(1.5)), 0x8A6A4A, 4f, Kind.SHARD,
                    Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.HORSE_ANGRY, 1.2f, 0.8f);
        }
    }

    /** Charge straight through: everything in your path is trampled and knocked aside. Needs a form. */
    public static final class Trample extends Ability {
        Trample() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, MANE).cooldown(100).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            ZoanKit.charge(player, 11 * ctx.power(), 1.5, 10f * FORMS.boost(player) * ctx.power(), 1.0);
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(11)), 0x8A6A4A, 1.2f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.HORSE_GALLOP, 1.6f, 0.8f);
        }
    }

    /** Lend your speed to your friends: everyone beside you runs and jumps like a horse for fifteen seconds. */
    public static final class Steed extends Ability {
        Steed() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(500).stamina(14).mastery(M, 35).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            for (LivingEntity friend : ZoanKit.friends(ctx.player(), 6)) {
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                friend.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 1));
                Vfx.impact(ctx.level(), friend, friend.position(), MANE, 1f, Kind.SMOKE);
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.HORSE_AMBIENT, 1.2f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Grow a pegasus's wings: fly for thirty seconds. */
    public static final class PegasusWings extends Ability {
        PegasusWings() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, 0xFFFFFF).cooldown(600).stamina(16).staminaPerTick(0.15f)
                    .maxActive(600).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), 0xFFFFFF, 2.4f, Kind.FEATHER, Vfx.RING);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.4f, 0.8f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ctx.player().resetFallDistance();
            if (ctx.activeTicks() % 6 == 0 && !ctx.player().onGround()) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), 0xFFFFFF, 0.9f, Kind.FEATHER);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /** Bring all four hooves down like thunder: a shockwave crushes and throws everything around you. Needs a form. */
    public static final class ThunderHooves extends Ability {
        ThunderHooves() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, MANE).cooldown(600).stamina(26).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : ZoanKit.stomp(player, player.position(), 7 * ctx.power(), 14f * ctx.power())) {
                AbilityDamage.knockback(target, player.position(), 1.4, 0.4);
            }
            Vfx.burst(ctx.level(), player, player.position(), 0xFFFFFF, 7f, Kind.SHOCKWAVE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.8f);
        }
    }
}
