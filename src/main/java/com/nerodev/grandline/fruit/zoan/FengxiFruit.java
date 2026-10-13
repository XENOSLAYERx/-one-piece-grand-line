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
import com.nerodev.grandline.effect.GLMobEffects;
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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Buta Buta no Mi, Model: Fengxi. Become the giant boar Fengxi, as Saint Topman Warcury does: a mountain of muscle
 * with tusks of iron that charges through anything, with the elders' body that will not stay wounded.
 */
public class FengxiFruit extends GoroseiFruit {
    public static final int COLOR = 0x5A4A3A;
    public static final int IRON = 0xC8C8D0;
    static final String M = "fruit/grandline:fengxi";

    /**
     * @hybrid Fengxi Hybrid | Grow iron tusks and a boar's bulk: far stronger, tougher and hard to push around.
     * @beast Fengxi Form | Become a gigantic iron-tusked boar: enormous, armoured, and a charge that flattens anything.
     */
    static final ZoanForms FORMS = bodies("fengxi", COLOR, () -> SoundEvents.HOGLIN_ANGRY, ZoanForms.Traits.NONE,
            b -> b.modifier(Attributes.ATTACK_DAMAGE, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 0.7, Operation.ADD_VALUE)
                    .modifier(Attributes.SCALE, 0.3, Operation.ADD_VALUE),
            b -> b.modifier(Attributes.MAX_HEALTH, 34, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 12, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 10, Operation.ADD_VALUE)
                    .modifier(Attributes.KNOCKBACK_RESISTANCE, 1, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 1.5, Operation.ADD_VALUE)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)
                    .dimensions(2.6f, 2.6f, 2.0f));

    public static final DeferredHolder<Ability, IronTusks> IRON_TUSKS = ability("fengxi_iron_tusks", IronTusks::new);
    public static final DeferredHolder<Ability, BoarRush> BOAR_RUSH = ability("fengxi_boar_rush", BoarRush::new);
    public static final DeferredHolder<Ability, Pentagram> PENTAGRAM = ability("fengxi_pentagram", Pentagram::new);
    public static final DeferredHolder<Ability, QuakeStomp> QUAKE_STOMP = ability("fengxi_quake_stomp", QuakeStomp::new);
    public static final DeferredHolder<Ability, CalamityCharge> CALAMITY_CHARGE = ability("fengxi_calamity_charge", CalamityCharge::new);
    public static final DeferredHolder<Ability, IronFortress> IRON_FORTRESS = ability("fengxi_iron_fortress", IronFortress::new);
    public static final DeferredHolder<Ability, Upheaval> UPHEAVAL = ability("fengxi_upheaval", Upheaval::new);

    public FengxiFruit() {
        super(Properties.of(FruitType.MYTHICAL_ZOAN, GLRarity.MYTHICAL, "Buta Buta no Mi, Model: Fengxi", COLOR)
                .abilities(FORMS.hybridAbility(), FORMS.beastAbility(), IRON_TUSKS, BOAR_RUSH, PENTAGRAM, QUAKE_STOMP, CALAMITY_CHARGE)
                .awakened(IRON_FORTRESS, UPHEAVAL), FORMS);
    }

    static void init() {}

    static void behaviors() {}

    static double size(ServerPlayer player) {
        return FORMS.beast(player) ? 1.5 : 1.0;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Hook your iron tusks through everything in front of you and fling it into the air. Needs a form. */
    public static final class IronTusks extends Ability {
        IronTusks() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLAW, IRON).cooldown(40).stamina(10).masteryGain(0.6));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 4.5 * size(player), 60)) {
                ZoanKit.gore(player, target, 14f * FORMS.boost(player) * ctx.power(), ctx.horizontalLook(), 0.6, 1.2);
            }
            Vfx.slash(ctx.level(), player, player.getEyePosition().add(ctx.horizontalLook().scale(2 * size(player))), new Vec3(0, 1, 0),
                    IRON, (float) (2.5 * size(player)), 1);
            ctx.animate(GLEffects.ANIM_CHARGE_PUNCH);
            ctx.sound(SoundEvents.HOGLIN_ATTACK, 1.4f, 0.6f);
        }
    }

    /** Charge headlong: everything in your path is gored aside. Needs a form. */
    public static final class BoarRush extends Ability {
        BoarRush() {
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
            double length = 14 * ctx.power();
            ZoanKit.charge(player, length, 1.8 * size(player), 14f * FORMS.boost(player) * ctx.power(), 1.6);
            Vfx.trail(ctx.level(), player, from, from.add(ctx.horizontalLook().scale(length)), 0x8A7A5A, 2f, Kind.SMOKE);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.HOGLIN_ANGRY, 1.4f, 0.7f);
        }
    }

    /** Step into a pentagram and out of another wherever you look, up to thirty-two blocks away. */
    public static final class Pentagram extends PentagramStep {
        Pentagram() {
            super(SIGIL, M);
        }
    }

    /** Stamp hard enough to shake the ground: everything around you is crushed, bounced and staggered. Needs a form. */
    public static final class QuakeStomp extends Ability {
        QuakeStomp() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 7 * ctx.power();
            for (LivingEntity target : ZoanKit.stomp(player, player.position(), radius, 15f * FORMS.boost(player) * ctx.power())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
            Vfx.burst(ctx.level(), player, player.position(), 0x8A7A5A, (float) radius, Kind.SHARD, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.6f, 0.6f);
        }
    }

    /** A charge twenty-four blocks long that nothing can stop: the ground heaves behind you. Needs a form. */
    public static final class CalamityCharge extends Ability {
        CalamityCharge() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, IRON).cooldown(240).stamina(20).mastery(M, 35).masteryGain(0.9));
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
            double length = 24 * ctx.power();
            ZoanKit.charge(player, length, 2.6 * size(player), 18f * FORMS.boost(player) * ctx.power(), 2.0);
            for (int i = 1; i <= 6; i++) {
                Vec3 at = Kit.ground(ctx.level(), from.add(dir.scale(i * length / 6)), 4);
                ZoanKit.stomp(player, at, 3, 5f * ctx.power());
                Vfx.burst(ctx.level(), player, at, 0x8A7A5A, 3f, Kind.SHARD, Vfx.GROUND);
            }
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Become an iron fortress for twenty seconds: blows lose most of their force and attackers bounce off you. */
    public static final class IronFortress extends Ability {
        IronFortress() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, IRON).cooldown(900).stamina(20).staminaPerTick(0.1f).maxActive(400)
                    .masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), IRON, 3f, Kind.SPARK, Vfx.RING | Vfx.FLASH);
            ctx.animate(GLEffects.ANIM_TEKKAI);
            ctx.sound(GLSounds.TEKKAI_ON.get(), 1.4f, 0.5f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            return FORMS.any(ctx.player());
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            event.setAmount(event.getAmount() * 0.4f);
            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.distanceToSqr(ctx.player()) < 36) {
                AbilityDamage.knockback(attacker, ctx.player().position(), 1.4, 0.4);
                Vfx.impact(ctx.level(), ctx.player(), attacker.getBoundingBox().getCenter(), IRON, 1.2f, Kind.SPARK);
            }
        }
    }

    /** Heave the ground up with your tusks: everything around you is crushed, thrown high and knocked senseless. */
    public static final class Upheaval extends Ability {
        Upheaval() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return FORMS.needsForm(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 10 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 22f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.6, 0));
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, 40, 0, false, false, true));
            }
            Vfx.eruption(ctx.level(), player, player.position(), 0x8A7A5A, 6f, Kind.SHARD);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, (float) radius, Kind.SHOCKWAVE, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }
}
