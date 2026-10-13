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
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.FxParams;
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
 * Inu Inu no Mi, Model: Wolf. Become a wolf: speed, fangs and a hunter's nose, combined with the Six Powers of a
 * government assassin. A wolf is strongest under the moon.
 */
public class WolfFruit extends ZoanFruit {
    public static final int COLOR = 0x8A8A98;
    public static final int DARK = 0x3A3A48;
    static final String M = "fruit/grandline:wolf";

    /**
     * @hybrid Wolf-Man | Grow a wolf's head, claws and fangs: quicker, tougher and much harder-hitting.
     * @beast Wolf Form | Become a grey wolf: very fast on four legs and a strong jumper.
     */
    static final ZoanForms FORMS = bodies("wolf", COLOR, () -> SoundEvents.WOLF_GROWL, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 3, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.2, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 4, Operation.ADD_VALUE)
                    .dimensions(0.9f, 1.1f, 0.9f));

    public static final DeferredHolder<Ability, WolfFang> WOLF_FANG = ability("wolf_fang", WolfFang::new);
    public static final DeferredHolder<Ability, Pounce> POUNCE = ability("wolf_pounce", Pounce::new);
    public static final DeferredHolder<Ability, Howl> HOWL = ability("wolf_howl", Howl::new);
    public static final DeferredHolder<Ability, IronFang> IRON_FANG = ability("wolf_iron_fang", IronFang::new);
    public static final DeferredHolder<Ability, WolfGale> WOLF_GALE = ability("wolf_gale", WolfGale::new);
    public static final DeferredHolder<Ability, LoneWolf> LONE_WOLF = ability("wolf_lone_wolf", LoneWolf::new);
    public static final DeferredHolder<Ability, MoonHowl> MOON_HOWL = ability("wolf_moon_howl", MoonHowl::new);

    public static final ResourceLocation GALE = GrandLine.id("wolf_gale");

    public WolfFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.RARE, "Inu Inu no Mi, Model: Wolf", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), WOLF_FANG, POUNCE, HOWL, IRON_FANG, WOLF_GALE)
                .awakened(LONE_WOLF, MOON_HOWL), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(GALE, Projectiles.piercing(AbilityDamage.SLASH, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Bite and rake whatever is in front of you: the wounds keep bleeding. Needs a form.
     *
     * @name Wolf Fang
     */
    public static final class WolfFang extends Ability {
        WolfFang() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, COLOR).cooldown(22).stamina(7).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.6, 50)) {
                ZoanKit.maul(player, target, 9f * FORMS.boost(player) * ctx.power(), 4, 0);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.of(player.getEyePosition().add(player.getLookAngle().scale(1.5)), player.getLookAngle())
                    .color(0xE8E8F0));
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.WOLF_GROWL, 1f, 1.2f);
        }
    }

    /** Leap at your prey; the first foe you land on is mauled and pinned for a moment. Needs a form. */
    public static final class Pounce extends Ability {
        Pounce() {
            super(props(ActivationType.TOGGLE, AbilityIcon.DASH, DARK).cooldown(45).stamina(14).maxActive(14).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            double speed = FORMS.beast(player) ? 1.7 : 1.4;
            player.setDeltaMovement(look.x * speed, Math.max(0.42, look.y * speed + 0.3), look.z * speed);
            player.hurtMarked = true;
            ctx.fx(GLEffects.POUNCE, FxParams.of(player.position(), look).color(COLOR));
            ctx.animate(GLEffects.ANIM_POUNCE);
            ctx.sound(SoundEvents.WOLF_GROWL, 0.8f, 1.3f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.getBoundingBox().getCenter(), 1.4)) {
                ZoanKit.maul(player, target, 9f * FORMS.boost(player) * ctx.power(), 3, 0);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                ctx.sound(GLSounds.CLAW.get(), 1f, 0.9f);
                return false;
            }
            return !(ctx.activeTicks() > 3 && player.onGround());
        }
    }

    /** Howl: you and your friends nearby fight harder and faster, and foes around you lose heart. */
    public static final class Howl extends Ability {
        Howl() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(400).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity friend : ZoanKit.friends(player, 10)) {
                friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            }
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            }
            ctx.fx(GLEffects.ROAR, FxParams.of(player.getEyePosition(), player.getLookAngle()).color(COLOR).scale(8f));
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.WOLF_HOWL, 1.4f, 1f);
        }
    }

    /** Harden your body like iron and bite through anything in front of you, shrugging off blows for three seconds. */
    public static final class IronFang extends Ability {
        IronFang() {
            super(props(ActivationType.INSTANT, AbilityIcon.SHIELD, DARK).cooldown(160).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
            for (LivingEntity target : AbilityDamage.cone(player, 3.8, 55)) {
                ZoanKit.maul(player, target, 12f * FORMS.boost(player) * ctx.power(), 5, 1);
            }
            Vfx.status(ctx.level(), player, Vfx.StatusLook.IRON_BANDS, 6);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1f, 1f);
        }
    }

    /**
     * Kick so fast the air itself cuts: three slicing waves fly out and run through everything in their path.
     *
     * @name Wolf Gale
     */
    public static final class WolfGale extends Ability {
        WolfGale() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(90).stamina(14).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.12)).normalize();
                AbilityProjectile.spawn(player, GALE, ctx.handPos(), dir.scale(1.8), 1.2f, 0xE8E8F0, 8f * FORMS.boost(player) * ctx.power(),
                        24);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.3f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Hold to tear into everything around you in a frenzy of fangs and claws. Needs a form. */
    public static final class LoneWolf extends Ability {
        LoneWolf() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, DARK).cooldown(300).stamina(18).staminaPerTick(0.6f).maxActive(60)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FRENZY);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 != 0) {
                return true;
            }
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 3.4)) {
                ZoanKit.maul(player, target, 3.5f * ctx.power(), 3, 1);
            }
            ctx.fx(GLEffects.CLAW_SLASH, FxParams.at(player.position().add(0, 1, 0)).color(0xE8E8F0).variant(1).scale(3.4f));
            if (ctx.activeTicks() % 6 == 0) {
                ctx.sound(GLSounds.CLAW.get(), 0.8f, 1.2f);
            }
            return FORMS.any(player);
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Howl at the moon: thirty seconds of strength, speed and healing, twice as strong at night. */
    public static final class MoonHowl extends Ability {
        MoonHowl() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, COLOR).cooldown(1200).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int level = ctx.level().isNight() ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, level + 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, level + 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, level));
            Vfx.burst(ctx.level(), player, player.getEyePosition(), 0xE8E8FF, 3f, Kind.GLOW, Vfx.FLASH | Vfx.RING);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.WOLF_HOWL, 2f, 0.8f);
        }
    }
}
