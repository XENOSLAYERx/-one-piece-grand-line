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
 * Ryu Ryu no Mi, Model: Pteranodon. Become a pteranodon: flight in both bodies, blinding speed in the air, a beak
 * like a lance and wings that cleave. An Ancient Zoan's toughness rides along.
 */
public class PteranodonFruit extends ZoanFruit {
    public static final int COLOR = 0x3A3A4A;
    public static final int CREST = 0xE84A2A;
    static final String M = "fruit/grandline:pteranodon";

    /**
     * @hybrid Pteranodon-Man | Grow a crest and leathery wings: you can fly, and you are much tougher and stronger.
     * @beast Pteranodon Form | Become a pteranodon: a very fast flier with a lance of a beak.
     */
    static final ZoanForms FORMS = bodies("pteranodon", COLOR, () -> SoundEvents.PHANTOM_AMBIENT, ZoanForms.Traits.WINGED,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 5, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 20, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MOVEMENT_SPEED, 0.6, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 40, Operation.ADD_VALUE)
                    .dimensions(1.3f, 1.3f, 1.0f));

    public static final DeferredHolder<Ability, BeakPierce> BEAK_PIERCE = ability("pteranodon_beak_pierce", BeakPierce::new);
    public static final DeferredHolder<Ability, SkyDive> SKY_DIVE = ability("pteranodon_sky_dive", SkyDive::new);
    public static final DeferredHolder<Ability, WingCleave> WING_CLEAVE = ability("pteranodon_wing_cleave", WingCleave::new);
    public static final DeferredHolder<Ability, GaleWings> GALE_WINGS = ability("pteranodon_gale_wings", GaleWings::new);
    public static final DeferredHolder<Ability, Snatch> SNATCH = ability("pteranodon_snatch", Snatch::new);
    public static final DeferredHolder<Ability, ImperialDive> IMPERIAL_DIVE = ability("pteranodon_imperial_dive", ImperialDive::new);
    public static final DeferredHolder<Ability, Hurricane> HURRICANE = ability("pteranodon_hurricane", Hurricane::new);

    public static final ResourceLocation WHIRL = GrandLine.id("pteranodon_hurricane");

    public PteranodonFruit() {
        super(Properties.of(FruitType.ANCIENT_ZOAN, GLRarity.EPIC, "Ryu Ryu no Mi, Model: Pteranodon", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), BEAK_PIERCE, SKY_DIVE, WING_CLEAVE, GALE_WINGS, SNATCH)
                .awakened(IMPERIAL_DIVE, HURRICANE), FORMS);
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WHIRL, Projectiles.zone(10, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
                Kit.pull(target, p.position().add(0, 2, 0), 0.4);
            }
        }, null));
    }

    static void dive(AbilityContext ctx, double range, double radius, float damage, int flags) {
        ServerPlayer player = ctx.player();
        Vec3 from = player.position();
        Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, range), 10);
        player.teleportTo(at.x, at.y, at.z);
        player.resetFallDistance();
        for (LivingEntity target : AbilityDamage.around(player, at, radius)) {
            Kit.hit(player, target, AbilityDamage.BLAST, damage);
            Kit.launch(target, target.position().subtract(at).normalize().scale(1.2).add(0, 0.6, 0));
        }
        Vfx.trail(ctx.level(), player, from.add(0, 1, 0), at.add(0, 1, 0), CREST, 1.2f, Kind.STREAK);
        Vfx.burst(ctx.level(), player, at, CREST, (float) radius, Kind.SHOCKWAVE, flags);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Drive your lance of a beak straight through whatever is in front of you. Needs a form. */
    public static final class BeakPierce extends Ability {
        BeakPierce() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, CREST).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 5, 1.0)) {
                ZoanKit.maul(player, target, 12f * FORMS.boost(player) * ctx.power(), 3, 0);
            }
            Vfx.trail(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(5)), 0xE8D8B0, 0.6f,
                    Kind.STREAK);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.CLAW.get(), 1f, 1.3f);
        }
    }

    /** Dive from the sky onto the spot you point at: a blow that blasts everything there. Needs a form. */
    public static final class SkyDive extends Ability {
        SkyDive() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(120).stamina(14).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            dive(ctx, 24, 4 * ctx.power(), 14f * FORMS.boost(ctx.player()) * ctx.power(), Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 1.2f);
        }
    }

    /** Sweep a wing's edge through everything in front of you: a wide, cutting blow. Needs a form. */
    public static final class WingCleave extends Ability {
        WingCleave() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(50).stamina(9).mastery(M, 10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 7, 60)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 10f * FORMS.boost(player) * ctx.power());
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2.5)), ctx.right(), 0x8A5A4A, 3.5f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.4f, 0.8f);
        }
    }

    /** Beat your wings hard: a gale that batters and hurls everything in front of you. */
    public static final class GaleWings extends Ability {
        GaleWings() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, CREST).cooldown(100).stamina(12).mastery(M, 20).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 look = ctx.horizontalLook();
            for (LivingEntity target : AbilityDamage.cone(player, 9 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * FORMS.boost(player) * ctx.power());
                Kit.launch(target, look.scale(2.0).add(0, 0.5, 0));
            }
            Vfx.burst(ctx.level(), player, player.getEyePosition().add(look.scale(3)), 0xC8D0D8, 4f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 2f, 0.6f);
        }
    }

    /** Snatch the nearest foe in your claws and climb: you both shoot skyward, and it falls when you let go. */
    public static final class Snatch extends Ability {
        Snatch() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, COLOR).cooldown(200).stamina(14).mastery(M, 35).masteryGain(0.9));
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
            ServerPlayer player = ctx.player();
            AbilityDamage.around(player, player.position(), 4).stream().findFirst().ifPresent(target -> {
                ZoanKit.maul(player, target, 8f * ctx.power(), 3, 0);
                Kit.launch(target, new Vec3(0, 1.9, 0));
                Kit.launch(player, new Vec3(0, 1.6, 0));
            });
            ctx.animate(GLEffects.ANIM_GEPPO);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 1.6f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** The imperial dive: drop on the spot you point at so hard the ground erupts around it. Needs a form. */
    public static final class ImperialDive extends Ability {
        ImperialDive() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, CREST).cooldown(600).stamina(28).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            dive(ctx, 32, 6 * ctx.power(), 22f * ctx.power(), Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            Vfx.eruption(ctx.level(), ctx.player(), ctx.player().position(), CREST, 4f, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.8f, 0.8f);
        }
    }

    /** Spin up a hurricane where you point: for five seconds it slices and lifts everything inside. */
    public static final class Hurricane extends Ability {
        Hurricane() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, COLOR).cooldown(700).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, WHIRL, Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8), 8 * ctx.power(), 0xC8D0D8,
                    4f * ctx.power(), 100);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.PHANTOM_FLAP, 2f, 0.4f);
        }
    }
}
