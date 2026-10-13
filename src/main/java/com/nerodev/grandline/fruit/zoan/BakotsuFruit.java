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
 * Uma Uma no Mi, Model: Bakotsu. Become a skeletal phantom horse, as Saint Ethanbaron V. Nusjuro does: impossibly
 * fast, with a swordsman's cuts that freeze whatever they touch, and the elders' body that will not stay wounded.
 */
public class BakotsuFruit extends GoroseiFruit {
    public static final int COLOR = 0xE8F0F8;
    public static final int FROST = 0x8AD0F8;
    static final String M = "fruit/grandline:bakotsu";

    /**
     * @hybrid Bakotsu Hybrid | Grow a skeletal horse's ears and tail: much faster, and your cuts carry frost.
     * @beast Bakotsu Form | Become a skeletal phantom horse: blindingly fast, tough, and cold to the bone.
     */
    static final ZoanForms FORMS = bodies("bakotsu", COLOR, () -> SoundEvents.SKELETON_HORSE_AMBIENT, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.25, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.6, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.3, Operation.ADD_VALUE)
                    .dimensions(1.6f, 2.4f, 2.2f));

    public static final DeferredHolder<Ability, FreezingIai> FREEZING_IAI = ability("bakotsu_freezing_iai", FreezingIai::new);
    public static final DeferredHolder<Ability, BoneGallop> BONE_GALLOP = ability("bakotsu_bone_gallop", BoneGallop::new);
    public static final DeferredHolder<Ability, Pentagram> PENTAGRAM = ability("bakotsu_pentagram", Pentagram::new);
    public static final DeferredHolder<Ability, FrostCrescent> FROST_CRESCENT = ability("bakotsu_frost_crescent", FrostCrescent::new);
    public static final DeferredHolder<Ability, GlacierSeverance> GLACIER_SEVERANCE = ability("bakotsu_glacier_severance",
            GlacierSeverance::new);
    public static final DeferredHolder<Ability, AbsoluteZeroCut> ABSOLUTE_ZERO = ability("bakotsu_absolute_zero_cut",
            AbsoluteZeroCut::new);
    public static final DeferredHolder<Ability, PhantomRider> PHANTOM_RIDER = ability("bakotsu_phantom_rider", PhantomRider::new);

    public static final ResourceLocation CRESCENT = GrandLine.id("bakotsu_crescent");

    public BakotsuFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Uma Uma no Mi, Model: Bakotsu", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), FREEZING_IAI, BONE_GALLOP, PENTAGRAM, FROST_CRESCENT,
                        GLACIER_SEVERANCE)
                .awakened(ABSOLUTE_ZERO, PHANTOM_RIDER), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(CRESCENT, Projectiles.piercing(AbilityDamage.SLASH, (p, target) -> chill(target, 100)));
    }

    /** The cut freezes: frost creeps over {@code target} and it slows to a crawl. */
    static void chill(LivingEntity target, int ticks) {
        target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + ticks));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Draw and cut in one motion: a freezing slash through everything in a line in front of you. */
    public static final class FreezingIai extends Ability {
        FreezingIai() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, FROST).cooldown(45).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.4)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 13f * FORMS.boost(player) * ctx.power());
                chill(target, 80);
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(length / 2)), ctx.right(), FROST,
                    (float) length / 2, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1f, 1.5f);
        }
    }

    /** Gallop straight through the enemy line: everything in your path is trampled aside. Needs a form. */
    public static final class BoneGallop extends Ability {
        BoneGallop() {
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
            double length = 16 * ctx.power();
            for (LivingEntity target : ZoanKit.charge(player, length, 1.6, 13f * FORMS.boost(player) * ctx.power(), 1.4)) {
                chill(target, 40);
            }
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), FROST, 1.4f, Kind.SNOW);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.SKELETON_HORSE_AMBIENT, 1.4f, 0.8f);
        }
    }

    /** Step into a pentagram and out of another wherever you look, up to thirty-two blocks away. */
    public static final class Pentagram extends PentagramStep {
        Pentagram() {
            super(SIGIL, M);
        }
    }

    /** Send a crescent of freezing air flying: it cuts through everything in its path and leaves it frozen. */
    public static final class FrostCrescent extends Ability {
        FrostCrescent() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, FROST).cooldown(70).stamina(12).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, CRESCENT, 2.0, 1.6f, FROST, 12f * FORMS.boost(ctx.player()), 25);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.2f, 1.2f);
        }
    }

    /** A wide, freezing cut: everything in front of you is slashed and sealed in ice. */
    public static final class GlacierSeverance extends Ability {
        GlacierSeverance() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(220).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 8 * ctx.power(), 70)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 18f * FORMS.boost(player) * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 40);
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(3)), ctx.right(), FROST, 5f, 2);
            Vfx.burst(ctx.level(), player, player.position().add(ctx.horizontalLook().scale(4)), FROST, 4f, Kind.SNOW, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 1.6f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A single cut thirty blocks long at absolute zero: everything along it is slashed and frozen solid. */
    public static final class AbsoluteZeroCut extends Ability {
        AbsoluteZeroCut() {
            super(props(ActivationType.INSTANT, AbilityIcon.BEAM, FROST).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double length = 30 * ctx.power();
            for (LivingEntity target : AbilityDamage.line(player, length, 1.8)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 26f * ctx.power());
                Kit.encase(target, Vfx.StatusLook.ICE, 60);
            }
            Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(length));
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, FROST, 0.6f, Kind.SNOW, Vfx.BEAM_GLOW);
            Vfx.beam(ctx.level(), player, ctx.handPos(), end, COLOR, 1.2f, Kind.SNOW, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(GLSounds.ICE_FREEZE.get(), 2f, 0.6f);
        }
    }

    /** Ride as a phantom for thirty seconds: you run far faster, and the cold you trail freezes everyone you pass. */
    public static final class PhantomRider extends Ability {
        PhantomRider() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH, FROST).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(600)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position(), FROST, 2.6f, Kind.SNOW, Vfx.RING | Vfx.FLASH);
            ctx.sound(SoundEvents.SKELETON_HORSE_AMBIENT, 1.4f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 2, false, false));
            if (ctx.activeTicks() % 5 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position(), 3)) {
                    Kit.hit(player, target, AbilityDamage.FREEZE, 4f * ctx.power());
                    chill(target, 40);
                }
                Vfx.impact(ctx.level(), player, player.position(), FROST, 1.4f, Kind.SNOW);
            }
            return true;
        }
    }
}
