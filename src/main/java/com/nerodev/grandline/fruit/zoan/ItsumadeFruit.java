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
 * Tori Tori no Mi, Model: Itsumade. Become the demon bird Itsumade, as Saint Marcus Mars does: a huge dark bird that
 * breathes fire, dives like a lance, and cries out over battlefields, with the elders' body that will not stay wounded.
 */
public class ItsumadeFruit extends GoroseiFruit {
    public static final int COLOR = 0x4A2A2A;
    public static final int FIRE = 0xE84A2A;
    static final String M = "fruit/grandline:itsumade";

    /**
     * @hybrid Itsumade Hybrid | Grow a demon bird's wings: you can fly, and you are much stronger and tougher.
     * @beast Itsumade Form | Become the demon bird Itsumade: a huge, fast flier that breathes fire.
     */
    static final ZoanForms FORMS = bodies("itsumade", COLOR, () -> SoundEvents.PHANTOM_AMBIENT, ZoanForms.Traits.WINGED,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 30, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 20, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 60, Operation.ADD_VALUE)
                    .dimensions(2.2f, 1.8f, 1.4f));

    public static final DeferredHolder<Ability, DemonFlame> DEMON_FLAME = ability("itsumade_demon_flame", DemonFlame::new);
    public static final DeferredHolder<Ability, BeakLance> BEAK_LANCE = ability("itsumade_beak_lance", BeakLance::new);
    public static final DeferredHolder<Ability, Pentagram> PENTAGRAM = ability("itsumade_pentagram", Pentagram::new);
    public static final DeferredHolder<Ability, BurningWings> BURNING_WINGS = ability("itsumade_burning_wings", BurningWings::new);
    public static final DeferredHolder<Ability, PlungingInferno> PLUNGING_INFERNO = ability("itsumade_plunging_inferno",
            PlungingInferno::new);
    public static final DeferredHolder<Ability, HellfireRain> HELLFIRE_RAIN = ability("itsumade_hellfire_rain", HellfireRain::new);
    public static final DeferredHolder<Ability, Cry> CRY = ability("itsumade_cry", Cry::new);

    public static final ResourceLocation HELLFIRE = GrandLine.id("itsumade_hellfire");

    public ItsumadeFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Tori Tori no Mi, Model: Itsumade", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), DEMON_FLAME, BEAK_LANCE, PENTAGRAM, BURNING_WINGS, PLUNGING_INFERNO)
                .awakened(HELLFIRE_RAIN, CRY), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(HELLFIRE, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FIRE, p.damage());
                target.igniteForSeconds(3);
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Breathe demon fire over everything in front of you. Needs a form. */
    public static final class DemonFlame extends Ability {
        DemonFlame() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, FIRE).cooldown(60).stamina(11).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double reach = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, reach, 45)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 10f * FORMS.boost(player) * ctx.power());
                target.igniteForSeconds(5);
            }
            Vfx.beam(ctx.level(), player, player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(reach)), FIRE,
                    2.0f, Kind.FLAME, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.FIRE_GUN.get(), 1.2f, 0.7f);
        }
    }

    /** Shoot forward beak first: everything in your path is run through. Needs a form. */
    public static final class BeakLance extends Ability {
        BeakLance() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            for (LivingEntity target : AbilityDamage.line(player, 8, 1.4)) {
                ZoanKit.maul(player, target, 13f * FORMS.boost(player) * ctx.power(), 3, 0);
            }
            Kit.dash(player, 2.0, 0.1);
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), from.add(player.getLookAngle().scale(8)).add(0, 1, 0), COLOR, 1.2f, Kind.STREAK);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.PHANTOM_SWOOP, 1.4f, 0.8f);
        }
    }

    /** Step into a pentagram and out of another wherever you look, up to thirty-two blocks away. */
    public static final class Pentagram extends PentagramStep {
        Pentagram() {
            super(SIGIL, M);
        }
    }

    /** Beat your wings and fan a burning gale: everything in front of you is set alight and blown away. */
    public static final class BurningWings extends Ability {
        BurningWings() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, FIRE).cooldown(140).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 10 * ctx.power(), 55)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 9f * FORMS.boost(player) * ctx.power());
                target.igniteForSeconds(4);
                Kit.launch(target, look.scale(2.0).add(0, 0.5, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(3)), FIRE, 4.5f, Kind.FLAME, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 2f, 0.6f);
        }
    }

    /** Dive wreathed in fire onto the spot you point at: the impact bursts into flames around you. Needs a form. */
    public static final class PlungingInferno extends Ability {
        PlungingInferno() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, FIRE).cooldown(200).stamina(18).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 from = player.position();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 12);
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            double radius = 5 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 18f * FORMS.boost(player) * ctx.power());
                target.igniteForSeconds(6);
                Kit.launch(target, target.position().subtract(at).normalize().scale(1.2).add(0, 0.6, 0));
            }
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), at.add(0, 1, 0), FIRE, 1.4f, Kind.FLAME);
            Vfx.burst(ctx.level(), player, at, FIRE, (float) radius, Kind.FLAME, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.FIRE_EXPLOSION.get(), 1.4f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Rain demon fire on the place you point at for six seconds: everything under it keeps burning. */
    public static final class HellfireRain extends Ability {
        HellfireRain() {
            super(props(ActivationType.INSTANT, AbilityIcon.FLAME, FIRE).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 28), 10);
            AbilityProjectile.zone(player, HELLFIRE, at, 8 * ctx.power(), FIRE, 6f * ctx.power(), 120);
            Vfx.rain(ctx.level(), player, at.add(0, 8, 0), FIRE, (float) (8 * ctx.power()), Kind.FLAME);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.FLAME_WALL.get(), 1.6f, 0.6f);
        }
    }

    /**
     * "Itsumade?" The demon bird's cry: everything around you is scorched, shaken and too afraid to fight back.
     *
     * @name Cry of Itsumade
     */
    public static final class Cry extends Ability {
        Cry() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(800).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.FIRE, 14f * ctx.power());
                target.igniteForSeconds(4);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition(), FIRE, (float) radius, Kind.SHOCKWAVE, Vfx.RING | Vfx.HEAVY | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(SoundEvents.PHANTOM_DEATH, 2f, 0.5f);
        }
    }
}
