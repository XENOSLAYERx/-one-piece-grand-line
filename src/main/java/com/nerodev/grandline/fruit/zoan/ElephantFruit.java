package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.forms;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Zou Zou no Mi. Become an elephant: tusks, a trunk that slams, grabs and sprays, and stomps that shake the earth.
 * The beast is huge and hard to topple.
 */
public class ElephantFruit extends ZoanFruit {
    public static final int COLOR = 0x9A9AA8;
    public static final int TUSK = 0xF0E8D8;
    static final String M = "fruit/grandline:elephant";

    /**
     * @hybrid Elephant-Man | Grow tusks and great ears: bigger, stronger, armoured and hard to push.
     * @beast Elephant Form | Become an elephant: huge, very tough, immovable and crushing.
     */
    static final ZoanForms FORMS = forms("elephant", COLOR, () -> SoundEvents.RAVAGER_ROAR, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.3, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.5, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 16, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.05, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(2.0f, 2.8f, 2.4f));

    public static final DeferredHolder<Ability, TrunkSlam> TRUNK_SLAM = ability("elephant_trunk_slam", TrunkSlam::new);
    public static final DeferredHolder<Ability, TuskGore> TUSK_GORE = ability("elephant_tusk_gore", TuskGore::new);
    public static final DeferredHolder<Ability, Trumpet> TRUMPET = ability("elephant_trumpet", Trumpet::new);
    public static final DeferredHolder<Ability, Earthshaker> EARTHSHAKER = ability("elephant_earthshaker", Earthshaker::new);
    public static final DeferredHolder<Ability, TrunkToss> TRUNK_TOSS = ability("elephant_trunk_toss", TrunkToss::new);
    public static final DeferredHolder<Ability, WaterCannon> WATER_CANNON = ability("elephant_water_cannon", WaterCannon::new);
    public static final DeferredHolder<Ability, HerdStampede> HERD_STAMPEDE = ability("elephant_herd_stampede", HerdStampede::new);

    public ElephantFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Zou Zou no Mi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), TRUNK_SLAM, TUSK_GORE, TRUMPET, EARTHSHAKER, TRUNK_TOSS)
                .awakened(WATER_CANNON, HERD_STAMPEDE), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    // ---------------------------------------------------------------------------------------------------------

    /** Bring your trunk down like a club on everything in front of you, bouncing it into the air. Needs a form. */
    public static final class TrunkSlam extends Ability {
        TrunkSlam() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(35).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = FORMS.beast(player) ? 6 : 4.5;
            for (LivingEntity target : AbilityDamage.cone(player, reach, 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 11f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(0.6).add(0, 0.8, 0));
            }
            Vfx.burst(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(reach * 0.7)), COLOR, 2.5f, Kind.SHOCKWAVE,
                    Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1.2f, 0.6f);
        }
    }

    /** Drive your tusks through whatever is in front of you and toss it. Needs a form. */
    public static final class TuskGore extends Ability {
        TuskGore() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, TUSK).cooldown(40).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, FORMS.beast(player) ? 5 : 3.6, 40)) {
                ZoanKit.gore(player, target, 12f * FORMS.boost(player) * ctx.power(), ctx.horizontalLook(), 1.0, 0.7);
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.5)), TUSK, 1.4f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 0.8f);
        }
    }

    /** Trumpet: the blast of sound shoves back, weakens and slows everything around you. */
    public static final class Trumpet extends Ability {
        Trumpet() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(240).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ZoanKit.roar(player, 8 * ctx.power(), 2f * ctx.power(), 120);
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 8f, Kind.RING, Vfx.RING);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(), 1.6f, 0.8f);
        }
    }

    /** Stamp the earth: everything around you is crushed and thrown up by the shock. Needs a form. */
    public static final class Earthshaker extends Ability {
        Earthshaker() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = (FORMS.beast(player) ? 7 : 5) * ctx.power();
            ZoanKit.stomp(player, player.position(), radius, 12f * FORMS.boost(player) * ctx.power());
            Vfx.burst(ctx.level(), player, player.position(), 0x8A6A4A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.6f);
        }
    }

    /** Wrap your trunk around a foe and hurl it far away. Needs a form. */
    public static final class TrunkToss extends Ability {
        TrunkToss() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, TUSK).cooldown(160).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(2.2).add(0, 0.9, 0));
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Blast a torrent of water from your trunk: it batters everything in a long line, puts out fires and blows foes away. */
    public static final class WaterCannon extends Ability {
        WaterCannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, 0x6AA8E8).cooldown(500).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : AbilityDamage.line(player, 14 * ctx.power(), 1.6)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 12f * ctx.power());
                target.clearFire();
                Kit.launch(target, look.scale(1.8).add(0, 0.3, 0));
            }
            player.clearFire();
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(look.scale(14 * ctx.power())), 0x6AA8E8, 1.4f,
                    Kind.DROP, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.GENERIC_SPLASH, 1.6f, 0.6f);
        }
    }

    /** Charge like a whole herd: everything in your path is gored aside, and the ground shakes behind you. Needs a form. */
    public static final class HerdStampede extends Ability {
        HerdStampede() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, COLOR).cooldown(700).stamina(30).masteryGain(1.5));
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
            ZoanKit.charge(player, 16 * ctx.power(), 2.4, 18f * ctx.power(), 1.6);
            for (int i = 1; i <= 4; i++) {
                Vec3 at = Kit.ground(ctx.level(), from.add(dir.scale(i * 4)), 4);
                ZoanKit.stomp(player, at, 3, 5f * ctx.power());
                Vfx.burst(ctx.level(), player, at, 0x8A6A4A, 3f, Kind.SHARD, Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.9f);
        }
    }
}
