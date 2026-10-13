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
 * Hito Hito no Mi, Model: Daibutsu. Become a towering golden Buddha, as Fleet Admiral Sengoku does: a body that is
 * almost impossible to hurt, and palms that release shockwaves strong enough to shatter a fortress wall.
 */
public class BuddhaFruit extends ZoanFruit {
    public static final int COLOR = 0xF0C040;
    public static final int GLOW = 0xFFF0A0;
    static final String M = "fruit/grandline:buddha";

    /**
     * @hybrid Golden Buddha Aspect | Your skin turns to gold and a halo rises behind you: stronger, and very hard to hurt.
     * @beast Great Buddha Form | Become a towering golden Buddha: gigantic, nearly unbreakable, every palm a shockwave.
     */
    static final ZoanForms FORMS = bodies("buddha", COLOR, () -> SoundEvents.BELL_RESONATE, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.SCALE, 0.25, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 8, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.6, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 40, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR_TOUGHNESS, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1.5, Operation.ADD_VALUE)
                    .modifier(Attributes.ENTITY_INTERACTION_RANGE, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.BLOCK_INTERACTION_RANGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, -0.15, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(2.0f, 4.0f, 3.6f));

    public static final DeferredHolder<Ability, ImpactPalm> IMPACT_PALM = ability("buddha_impact_palm", ImpactPalm::new);
    public static final DeferredHolder<Ability, GreatSmash> GREAT_SMASH = ability("buddha_great_smash", GreatSmash::new);
    public static final DeferredHolder<Ability, GoldenBody> GOLDEN_BODY = ability("buddha_golden_body", GoldenBody::new);
    public static final DeferredHolder<Ability, PalmBarrage> PALM_BARRAGE = ability("buddha_palm_barrage", PalmBarrage::new);
    public static final DeferredHolder<Ability, HeavenlyPalm> HEAVENLY_PALM = ability("buddha_heavenly_palm", HeavenlyPalm::new);
    public static final DeferredHolder<Ability, HaloOfMercy> HALO = ability("buddha_halo_of_mercy", HaloOfMercy::new);
    public static final DeferredHolder<Ability, Enlightenment> ENLIGHTENMENT = ability("buddha_enlightenment", Enlightenment::new);

    public BuddhaFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Hito Hito no Mi, Model: Daibutsu", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), IMPACT_PALM, GREAT_SMASH, GOLDEN_BODY, PALM_BARRAGE, HEAVENLY_PALM)
                .awakened(HALO, ENLIGHTENMENT), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double size(ServerPlayer player) {
        return FORMS.beast(player) ? 1.8 : 1.0;
    }

    /** A palm's shockwave along the user's look: everything in it is struck and blown straight back. */
    static void shockwave(AbilityContext ctx, double length, double width, float damage, double push) {
        ServerPlayer player = ctx.player();
        Vec3 look = ctx.horizontalLook();
        for (LivingEntity target : AbilityDamage.line(player, length, width)) {
            Kit.hit(player, target, AbilityDamage.STRIKE, damage);
            Kit.launch(target, look.scale(push).add(0, 0.4, 0));
        }
        Vec3 from = player.getEyePosition().add(look.scale(1.5));
        for (int i = 1; i <= 3; i++) {
            Vfx.burst(ctx.level(), player, from.add(look.scale(length * i / 4)), GLOW, (float) width * (1 + i * 0.25f), Kind.SHOCKWAVE,
                    i == 1 ? Vfx.FLASH : 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Thrust out a palm: a shockwave rolls forward and hurls everything in its path away. */
    public static final class ImpactPalm extends Ability {
        ImpactPalm() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, GLOW).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            double size = size(ctx.player());
            shockwave(ctx, 12 * size * ctx.power(), 1.6 * size, 13f * FORMS.boost(ctx.player()) * ctx.power(), 2.0);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.4f, 0.6f);
        }
    }

    /** Bring both palms down on the ground: everything around you is crushed and bounced into the air. Needs a form. */
    public static final class GreatSmash extends Ability {
        GreatSmash() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(90).stamina(14).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 5 * size(player) * ctx.power();
            ZoanKit.stomp(player, player.position(), radius, 15f * FORMS.boost(player) * ctx.power());
            Vfx.burst(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.SHOCKWAVE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.6f);
        }
    }

    /** Harden your golden body: for twelve seconds blows barely touch you and nothing can move you. */
    public static final class GoldenBody extends Ability {
        GoldenBody() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, COLOR).cooldown(400).stamina(14).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 2));
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), GLOW, 2.6f, Kind.STAR, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(SoundEvents.BELL_BLOCK, 1.4f, 0.6f);
        }
    }

    /** Strike with palm after palm: three shockwaves batter everything in front of you. */
    public static final class PalmBarrage extends Ability {
        PalmBarrage() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(160).stamina(16).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = 10 * size(player) * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, reach, 45)) {
                for (int i = 0; i < 3; i++) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 7f * FORMS.boost(player) * ctx.power());
                }
                Kit.launch(target, ctx.horizontalLook().scale(2.2).add(0, 0.5, 0));
            }
            for (int i = 0; i < 3; i++) {
                Vec3 at = player.getEyePosition().add(ctx.horizontalLook().scale(2 + i * reach / 4)).add(ctx.right().scale((i - 1) * 1.2));
                Vfx.burst(ctx.level(), player, at, GLOW, (float) (2 + i), Kind.SHOCKWAVE, 0);
            }
            ctx.animate(GLEffects.ANIM_GATLING);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 1.4f, 0.8f);
        }
    }

    /** Bring a palm of light down from above onto the spot you point at, flattening everything beneath it. */
    public static final class HeavenlyPalm extends Ability {
        HeavenlyPalm() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, GLOW).cooldown(240).stamina(20).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 32), 10);
            double radius = 6 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 20f * FORMS.boost(player) * ctx.power());
                target.setDeltaMovement(0, -1.2, 0);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            }
            Vfx.beam(ctx.level(), player, at.add(0, 18, 0), at, GLOW, (float) radius, Kind.GLOW, Vfx.BEAM_GLOW);
            Vfx.burst(ctx.level(), player, at, COLOR, (float) radius, Kind.SHOCKWAVE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Shine with a halo of mercy for thirty seconds: you and your allies nearby are shielded and slowly healed. */
    public static final class HaloOfMercy extends Ability {
        HaloOfMercy() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SUN, GLOW).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(600)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getEyePosition(), GLOW, 4f, Kind.GLOW, Vfx.RING | Vfx.FLASH);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1.2f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            if (!FORMS.any(player)) {
                return false;
            }
            if (ctx.activeTicks() % 20 == 0) {
                for (LivingEntity friend : ZoanKit.friends(player, 10)) {
                    friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, true));
                    friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, true));
                }
                Vfx.burst(ctx.level(), player, player.position(), GLOW, 10f, Kind.GLOW, Vfx.GROUND);
            }
            return true;
        }
    }

    /** One great shockwave of enlightenment from both palms: everything around you is struck and blown far away. */
    public static final class Enlightenment extends Ability {
        Enlightenment() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, GLOW).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 14 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 24f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 3.0, 0.8);
            }
            Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), GLOW, (float) radius, Kind.SHOCKWAVE,
                    Vfx.RING | Vfx.HEAVY | Vfx.FLASH);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.STAR, Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.HAKI_IMPACT.get(), 2f, 0.4f);
        }
    }
}
