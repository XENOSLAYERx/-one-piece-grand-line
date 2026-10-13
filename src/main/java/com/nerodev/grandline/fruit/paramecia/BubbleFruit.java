package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

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
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Awa Awa no Mi. Soap bubbles that wash things clean, including the strength out of a body, the shine off armour
 * and the footing from under a foe.
 */
public class BubbleFruit extends DevilFruit {
    public static final int COLOR = 0xC8E8FF;
    public static final int PINK = 0xFFC8F0;
    static final String M = "fruit/grandline:bubble";

    static final Buffs.Mod SCRUBBED = Buffs.mod(Attributes.ARMOR, GrandLine.id("bubble_scrubbed_armor"), -4, Operation.ADD_VALUE);

    public static final DeferredHolder<Ability, GoldenBubbles> GOLDEN_BUBBLES = ability("bubble_golden_bubbles", GoldenBubbles::new);
    public static final DeferredHolder<Ability, SoapSheep> SOAP_SHEEP = ability("bubble_soap_sheep", SoapSheep::new);
    public static final DeferredHolder<Ability, SoapFloor> SOAP_FLOOR = ability("bubble_soap_floor", SoapFloor::new);
    public static final DeferredHolder<Ability, BubbleShield> BUBBLE_SHIELD = ability("bubble_shield", BubbleShield::new);
    public static final DeferredHolder<Ability, BubbleRide> BUBBLE_RIDE = ability("bubble_ride", BubbleRide::new);
    public static final DeferredHolder<Ability, CleansingStorm> CLEANSING_STORM = ability("bubble_cleansing_storm", CleansingStorm::new);
    public static final DeferredHolder<Ability, BubblePrison> BUBBLE_PRISON = ability("bubble_prison", BubblePrison::new);

    public static final ResourceLocation BUBBLE = GrandLine.id("bubble_bubble");
    public static final ResourceLocation FLOOR = GrandLine.id("bubble_floor");

    public BubbleFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Awa Awa no Mi", COLOR)
                .abilities(GOLDEN_BUBBLES, SOAP_SHEEP, SOAP_FLOOR, BUBBLE_SHIELD, BUBBLE_RIDE)
                .awakened(CLEANSING_STORM, BUBBLE_PRISON));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BUBBLE, Projectiles.bolt(AbilityDamage.STRIKE, (p, target) -> wash(target, 100)));
        ProjectileBehavior.Registry.register(FLOOR, Projectiles.zone(10, (p, target) -> {
            SlipFruit.slide(target, 0.35);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
        }, null));
    }

    /** Washes the strength out of a creature: weakened, slowed and its armour scrubbed thin for a while. */
    static void wash(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
        Buffs.timed(target, SCRUBBED, ticks);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Blow a stream of golden soap bubbles: each one that bursts on a foe washes its strength away. */
    public static final class GoldenBubbles extends Ability {
        GoldenBubbles() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(50).stamina(10).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < 5; i++) {
                Vec3 spread = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian(),
                        player.getRandom().nextGaussian()).scale(0.08);
                AbilityProjectile.spawn(player, BUBBLE, ctx.handPos(), player.getLookAngle().add(spread).normalize().scale(0.8),
                        0.45f, i % 2 == 0 ? 0xFFE08A : COLOR, 3f * ctx.power(), 50).withGravity(-0.002);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.2f, 1.4f);
        }
    }

    /**
     * Smother a foe in a woolly cloud of soap: it is stuck inside, then comes out scrubbed weak.
     *
     * @name Soap Sheep
     */
    public static final class SoapSheep extends Ability {
        SoapSheep() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, PINK).cooldown(160).stamina(16).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.SOAP, 50);
                wash(target, 200);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), COLOR, 1.6f, Kind.BUBBLE, 0);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.4f, 0.8f);
        }
    }

    /** Lather the ground where you point: anyone on it slips and loses their grip. */
    public static final class SoapFloor extends Ability {
        SoapFloor() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(200).stamina(14).mastery(M, 10)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, FLOOR, at, 5 * ctx.power(), COLOR, 0f, 160);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.BUBBLE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1f, 1.4f);
        }
    }

    /** Hold a big bubble around yourself: projectiles pop against it and anything that touches it is washed weak. */
    public static final class BubbleShield extends Ability {
        BubbleShield() {
            super(props(ActivationType.HOLD, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(10).staminaPerTick(0.25f)
                    .maxActive(100).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_FOCUS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1f, 1.2f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = player.getBoundingBox().getCenter();
            for (Projectile p : ctx.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(2.5),
                    e -> e.getOwner() != player)) {
                p.discard();
                Vfx.impact(ctx.level(), player, p.position(), COLOR, 0.8f, Kind.BUBBLE);
            }
            if (ctx.activeTicks() % 10 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, center, 2.5)) {
                    wash(target, 60);
                }
                Vfx.status(ctx.level(), player, Vfx.StatusLook.SOAP, 12);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 1.5f, Kind.BUBBLE, 0);
        }
    }

    /** Float inside a big soap bubble and drift through the air. */
    public static final class BubbleRide extends Ability {
        BubbleRide() {
            super(props(ActivationType.TOGGLE, AbilityIcon.ORB, PINK).cooldown(160).stamina(12).staminaPerTick(0.2f)
                    .maxActive(600).mastery(M, 35).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.resetFallDistance();
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 1.4f, Kind.BUBBLE);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1f, 1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A storm of bubbles washes everyone around you: weakened, slowed, and scrubbed clean of every blessing. */
    public static final class CleansingStorm extends Ability {
        CleansingStorm() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, COLOR).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = 12 * ctx.power();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), radius)) {
                for (Holder<MobEffect> effect : new ArrayList<>(target.getActiveEffectsMap().keySet())) {
                    if (effect.value().isBeneficial()) {
                        target.removeEffect(effect);
                    }
                }
                wash(target, 240);
                Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, (float) radius, Kind.BUBBLE, Vfx.RING | Vfx.HEAVY);
            Vfx.rain(ctx.level(), player, player.position(), PINK, (float) radius, Kind.BUBBLE);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 2f, 0.8f);
        }
    }

    /** Trap every foe near you in a giant soap bubble that lifts them helplessly off the ground. */
    public static final class BubblePrison extends Ability {
        BubblePrison() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, PINK).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 7 * ctx.power())) {
                Kit.launch(target, new Vec3(0, 0.9, 0));
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0));
                Kit.encase(target, Vfx.StatusLook.SOAP, 60);
                wash(target, 160);
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), PINK, 7f, Kind.BUBBLE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 2f, 0.6f);
        }
    }
}
