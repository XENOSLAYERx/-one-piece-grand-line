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
 * Tori Tori no Mi, Model: Albatross. Become an albatross and soar over the sea: winged in both bodies, a gliding
 * master of updrafts and storm winds, and a bird who always knows the news first.
 */
public class AlbatrossFruit extends ZoanFruit {
    public static final int COLOR = 0xF0F0F0;
    public static final int DARK = 0x3A3A3A;
    static final String M = "fruit/grandline:albatross";

    /**
     * @hybrid Albatross-Man | Grow great white wings: you can fly, and you are quicker on the ground.
     * @beast Albatross Form | Become an albatross: a tireless flier, fast and light.
     */
    static final ZoanForms FORMS = forms("albatross", COLOR, () -> SoundEvents.PARROT_AMBIENT, ZoanForms.Traits.WINGED,
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 20, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.35, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 40, Operation.ADD_VALUE)
                    .dimensions(1.0f, 1.0f, 0.8f));

    public static final DeferredHolder<Ability, BeakJab> BEAK_JAB = ability("albatross_beak_jab", BeakJab::new);
    public static final DeferredHolder<Ability, WingBuffet> WING_BUFFET = ability("albatross_wing_buffet", WingBuffet::new);
    public static final DeferredHolder<Ability, Updraft> UPDRAFT = ability("albatross_updraft", Updraft::new);
    public static final DeferredHolder<Ability, Headline> HEADLINE = ability("albatross_headline", Headline::new);
    public static final DeferredHolder<Ability, DiveBomb> DIVE_BOMB = ability("albatross_dive_bomb", DiveBomb::new);
    public static final DeferredHolder<Ability, StormFront> STORM_FRONT = ability("albatross_storm_front", StormFront::new);
    public static final DeferredHolder<Ability, FeatherVolley> FEATHER_VOLLEY = ability("albatross_feather_volley", FeatherVolley::new);

    public static final ResourceLocation FEATHER = GrandLine.id("albatross_feather");
    public static final ResourceLocation STORM = GrandLine.id("albatross_storm");

    public AlbatrossFruit() {
        super(Properties.of(FruitType.ZOAN, GLRarity.UNCOMMON, "Tori Tori no Mi, Model: Albatross", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), BEAK_JAB, WING_BUFFET, UPDRAFT, HEADLINE, DIVE_BOMB)
                .awakened(STORM_FRONT, FEATHER_VOLLEY), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FEATHER, Projectiles.piercing(AbilityDamage.SLASH, null));
        ProjectileBehavior.Registry.register(STORM, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
                Vec3 away = target.position().subtract(p.position());
                Vec3 flat = new Vec3(away.x, 0, away.z);
                Kit.launch(target, (flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : flat.normalize().scale(0.7)).add(0, 0.35, 0));
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Jab with your hooked beak at whatever is in front of you. Needs a form. */
    public static final class BeakJab extends Ability {
        BeakJab() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, 0xF0D080).cooldown(25).stamina(6).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 3.4, 35)) {
                ZoanKit.maul(player, target, 8f * FORMS.boost(player) * ctx.power(), 2, 0);
            }
            Vfx.impact(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(1.3)), 0xF0D080, 1f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.CLAW.get(), 1f, 1.4f);
        }
    }

    /** Buffet everything in front of you with your great wings, knocking it away. Needs a form. */
    public static final class WingBuffet extends Ability {
        WingBuffet() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(50).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 7, 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, look.scale(1.6).add(0, 0.4, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(3)), COLOR, 3f, Kind.FEATHER, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.4f, 0.7f);
        }
    }

    /** Catch an updraft: you are lifted high and glide down slowly. */
    public static final class Updraft extends Ability {
        Updraft() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(100).stamina(8).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.launch(player, ctx.horizontalLook().scale(0.4).add(0, 1.6, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 2f, Kind.FEATHER, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.2f, 0.8f);
        }
    }

    /**
     * Spread the headline: every foe within twenty blocks is exposed for fifteen seconds, glowing and shaken.
     */
    public static final class Headline extends Ability {
        Headline() {
            super(props(ActivationType.INSTANT, AbilityIcon.EYE, DARK).cooldown(400).stamina(10).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 20)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), COLOR, 3f, Kind.FEATHER, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BOOK_PAGE_TURN, 1.4f, 1f);
        }
    }

    /** Fold your wings and drop onto the spot you point at: a diving blast that scatters everything there. Needs a form. */
    public static final class DiveBomb extends Ability {
        DiveBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(180).stamina(14).mastery(M, 35).masteryGain(0.9));
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
            for (LivingEntity target : AbilityDamage.around(player, at, 3.5 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.BLAST, 12f * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(1.0).add(0, 0.5, 0));
            }
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), at.add(0, 1, 0), COLOR, 1.2f, Kind.FEATHER);
            Vfx.burst(ctx.level(), player, at, COLOR, 3.5f, Kind.FEATHER, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.2f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Bring a sea storm's winds down where you point: for six seconds they batter and blow back everything inside. */
    public static final class StormFront extends Ability {
        StormFront() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, DARK).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, STORM, Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8), 8 * ctx.power(), COLOR,
                    4f * ctx.power(), 120);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ELYTRA_FLYING, 1.4f, 0.6f);
        }
    }

    /** Shake loose a volley of quill-hard feathers in a wide fan, twice over. */
    public static final class FeatherVolley extends Ability {
        FeatherVolley() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(500).stamina(24).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = -4; i <= 4; i++) {
                Vec3 dir = player.getLookAngle().add(ctx.right().scale(i * 0.08)).add(0, (i % 2) * 0.05, 0).normalize();
                AbilityProjectile.spawn(player, FEATHER, ctx.handPos(), dir.scale(1.9), 0.3f, COLOR, 6f * ctx.power(), 24);
            }
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.6f, 1.2f);
        }
    }
}
