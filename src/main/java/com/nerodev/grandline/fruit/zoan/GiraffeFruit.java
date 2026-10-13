package com.nerodev.grandline.fruit.zoan;

import static com.nerodev.grandline.fruit.zoan.ZoanBatch.ability;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.forms;
import static com.nerodev.grandline.fruit.zoan.ZoanBatch.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Ushi Ushi no Mi, Model: Giraffe. Become a giraffe: a neck like a battering ram, legs like blades and a lookout's
 * view over everything. Its user also fights with the Six Powers' flying kick.
 */
public class GiraffeFruit extends ZoanFruit {
    public static final int COLOR = 0xE8C060;
    public static final int BROWN = 0x8A5A2A;
    static final String M = "fruit/grandline:giraffe";

    /**
     * @hybrid Giraffe-Man | Grow ossicones, a giraffe's hide and a long reach: harder-hitting and a little bigger.
     * @beast Giraffe Form | Become a towering giraffe: tall, fast, and able to step over walls.
     */
    static final ZoanForms FORMS = forms("giraffe", COLOR, () -> SoundEvents.HORSE_AMBIENT, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.2, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.35, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.5, Operation.ADD_VALUE)
                    .dimensions(1.1f, 3.4f, 3.2f));

    public static final DeferredHolder<Ability, NeckWhip> NECK_WHIP = ability("giraffe_neck_whip", NeckWhip::new);
    public static final DeferredHolder<Ability, BladeKick> BLADE_KICK = ability("giraffe_blade_kick", BladeKick::new);
    public static final DeferredHolder<Ability, Lookout> LOOKOUT = ability("giraffe_lookout", Lookout::new);
    public static final DeferredHolder<Ability, StormLeg> STORM_LEG = ability("giraffe_storm_leg", StormLeg::new);
    public static final DeferredHolder<Ability, BishopNeck> BISHOP_NECK = ability("giraffe_bishop_neck", BishopNeck::new);
    public static final DeferredHolder<Ability, NeckCannon> NECK_CANNON = ability("giraffe_neck_cannon", NeckCannon::new);
    public static final DeferredHolder<Ability, StormCross> STORM_CROSS = ability("giraffe_storm_cross", StormCross::new);

    public static final ResourceLocation WAVE = GrandLine.id("giraffe_storm_leg");

    public GiraffeFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.RARE, "Ushi Ushi no Mi, Model: Giraffe", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), NECK_WHIP, BLADE_KICK, LOOKOUT, STORM_LEG, BISHOP_NECK)
                .awakened(NECK_CANNON, STORM_CROSS), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WAVE, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Swing your long neck like a club through everything in front of you, sweeping it aside. Needs a form. */
    public static final class NeckWhip extends Ability {
        NeckWhip() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(40).stamina(9).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = FORMS.beast(player) ? 7 : 5;
            for (LivingEntity target : AbilityDamage.cone(player, reach, 60)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 10f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.right().scale(-1.1).add(ctx.horizontalLook().scale(0.4)).add(0, 0.4, 0));
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2.5)), ctx.right(), COLOR, 3f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.PUNCH_HEAVY.get(), 1f, 0.7f);
        }
    }

    /** A giraffe's kick, sharp as a blade: it hits hard and throws the foe back. Needs a form. */
    public static final class BladeKick extends Ability {
        BladeKick() {
            super(props(ActivationType.INSTANT, AbilityIcon.KICK, BROWN).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.8, 40)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 12f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, ctx.horizontalLook().scale(1.3).add(0, 0.4, 0));
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.4)), 0xFFFFFF, 1.4f, Kind.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
        }
    }

    /** Look out over everything: creatures within forty blocks stand out for fifteen seconds. */
    public static final class Lookout extends Ability {
        Lookout() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, COLOR).cooldown(400).stamina(6).mastery(M, 10).masteryGain(0.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 40)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
            }
            Vfx.impact(ctx.level(), player, player.getEyePosition(), COLOR, 1.4f, Kind.RING);
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.SPYGLASS_USE, 1f, 0.8f);
        }
    }

    /** Kick so fast the air cuts: a blade of wind flies out and runs through everything in its path. */
    public static final class StormLeg extends Ability {
        StormLeg() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, BROWN).cooldown(60).stamina(10).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.spawn(player, WAVE, ctx.handPos(), player.getLookAngle().scale(1.9), 1.6f, 0xFFFFFF,
                    10f * FORMS.boost(player) * ctx.power(), 26);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);
        }
    }

    /** Ram your head down on a foe from above: a crushing blow that leaves it dazed. Needs a form. */
    public static final class BishopNeck extends Ability {
        BishopNeck() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(160).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            if (!FORMS.any(player)) {
                return FORMS.needsForm(player);
            }
            return Kit.target(player, 8).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 8).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.CRUSH, 14f * FORMS.boost(player) * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                Vfx.burst(ctx.level(), player, target.position(), COLOR, 2f, Kind.SHOCKWAVE, Vfx.GROUND | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Fire your whole neck forward like a cannon: everything in a long line is blasted away. Needs a form. */
    public static final class NeckCannon extends Ability {
        NeckCannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, COLOR).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : AbilityDamage.line(player, 14 * ctx.power(), 1.6)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 18f * ctx.power());
                Kit.launch(target, look.scale(1.8).add(0, 0.4, 0));
            }
            Vfx.beam(ctx.level(), player, player.getEyePosition(), player.getEyePosition().add(look.scale(14 * ctx.power())), COLOR, 1.6f,
                    Kind.SHOCKWAVE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.4f, 0.9f);
        }
    }

    /** Two crossing blades of wind from a pair of kicks, each running through everything in its way. */
    public static final class StormCross extends Ability {
        StormCross() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(500).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.1)).normalize();
                AbilityProjectile.spawn(player, WAVE, ctx.handPos(), dir.scale(2.0), 2.2f, 0xFFFFFF, 14f * ctx.power(), 30);
            }
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.9f);
        }
    }
}
