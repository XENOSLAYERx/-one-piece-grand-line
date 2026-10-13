package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchA.props;

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
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.Fx;
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
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Bara Bara no Mi. The body splits into pieces that float and fight on their own; blades pass straight through it.
 */
public class ChopFruit extends DevilFruit {
    public static final int COLOR = 0xD84A3A;
    public static final int GLOVE = 0xF0E8E0;
    public static final int STEEL = 0xC8D0D8;
    static final String M = "fruit/grandline:chop";

    public static final DeferredHolder<Ability, Cannon> CANNON = ability("chop_cannon", Cannon::new);
    public static final DeferredHolder<Ability, Grab> GRAB = ability("chop_grab", Grab::new);
    public static final DeferredHolder<Ability, Festival> FESTIVAL = ability("chop_festival", Festival::new);
    public static final DeferredHolder<Ability, Escape> ESCAPE = ability("chop_escape", Escape::new);
    public static final DeferredHolder<Ability, Senbei> SENBEI = ability("chop_senbei", Senbei::new);
    public static final DeferredHolder<Ability, Cyclone> CYCLONE = ability("chop_cyclone", Cyclone::new);
    public static final DeferredHolder<Ability, KnifeRain> KNIFE_RAIN = ability("chop_knife_rain", KnifeRain::new);

    public static final ResourceLocation FIST = GrandLine.id("chop_fist");
    public static final ResourceLocation DISC = GrandLine.id("chop_disc");
    public static final ResourceLocation RAIN = GrandLine.id("chop_knife_rain");

    public ChopFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.UNCOMMON, "Bara Bara no Mi", COLOR)
                .abilities(CANNON, GRAB, FESTIVAL, ESCAPE, SENBEI)
                .awakened(CYCLONE, KNIFE_RAIN));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(FIST, Projectiles.bolt(AbilityDamage.STRIKE,
                (p, target) -> AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 0.8, 0.2)));
        ProjectileBehavior.Registry.register(DISC, Projectiles.piercing(AbilityDamage.STRIKE,
                (p, target) -> AbilityDamage.knockback(target, p.position().subtract(p.getDeltaMovement()), 0.6, 0.3)));
        ProjectileBehavior.Registry.register(RAIN, Projectiles.zone(6, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.SLASH, p.damage());
            }
        }, null));
    }

    /** Blades cannot cut a body that simply comes apart where they strike. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        boolean blade = event.getSource().is(AbilityDamage.SHARP);
        if (!blade && event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            var held = attacker.getMainHandItem().getItem();
            blade = held instanceof SwordItem || held instanceof AxeItem;
        }
        if (blade) {
            event.setCanceled(true);
            Vfx.burst(user.serverLevel(), user, user.getBoundingBox().getCenter(), COLOR, 1f, Kind.SHARD, 0);
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.4f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Fire your detached fist at a foe like a cannonball.
     *
     * @name Chop-Chop Cannon
     */
    public static final class Cannon extends Ability {
        Cannon() {
            super(props(ActivationType.INSTANT, AbilityIcon.FIST, COLOR).cooldown(30).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, FIST, 1.6, 0.5f, GLOVE, 7f, 25);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.RUBBER_SNAP.get(), 0.8f, 1.5f);
        }
    }

    /** Send your hand flying to grab a distant foe and drag it back to you. */
    public static final class Grab extends Ability {
        Grab() {
            super(props(ActivationType.INSTANT, AbilityIcon.GRAB, GLOVE).cooldown(80).stamina(10).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 22).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 22).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                Kit.pull(target, player.position().add(player.getLookAngle().scale(1.5)), 1.4);
                Vfx.trail(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), GLOVE, 0.5f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
        }
    }

    /**
     * Hold to burst into pieces that whirl around you, battering everything close by.
     *
     * @name Chop-Chop Festival
     */
    public static final class Festival extends Ability {
        Festival() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, COLOR).cooldown(140).stamina(12).staminaPerTick(0.35f)
                    .maxActive(60).mastery(M, 10).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.2f, 0.7f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
            if (ctx.activeTicks() % 5 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, player.position().add(0, 1, 0), 4 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                }
                Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 2.5f, Kind.SHARD);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /**
     * Fly apart as a blow lands and reassemble a few steps back, untouched.
     *
     * @name Chop-Chop Escape
     */
    public static final class Escape extends Ability {
        Escape() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, GLOVE).cooldown(160).stamina(14).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 4, false, false, true));
            Vec3 from = player.position();
            Vec3 back = ctx.horizontalLook().scale(-1);
            for (double d = 6; d > 0.5; d -= 0.5) {
                Vec3 to = from.add(back.scale(d));
                AABB box = player.getBoundingBox().move(to.subtract(from));
                if (player.level().noCollision(player, box)) {
                    player.teleportTo(to.x, to.y, to.z);
                    break;
                }
            }
            Vfx.burst(ctx.level(), player, from.add(0, 1, 0), COLOR, 1.6f, Kind.SHARD, 0);
            Vfx.blink(ctx.level(), player, from, player.position(), COLOR, Kind.SHARD);
            ctx.animate(GLEffects.ANIM_SIDESTEP);
            ctx.sound(GLSounds.EVADE.get(), 1f, 1.1f);
        }
    }

    /**
     * Spin your legs into a disc that skims along the ground through every foe in its way.
     *
     * @name Chop-Chop Senbei
     */
    public static final class Senbei extends Ability {
        Senbei() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWIRL, COLOR).cooldown(90).stamina(14).mastery(M, 35)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.spawn(player, DISC, player.position().add(0, 0.4, 0), ctx.horizontalLook().scale(1.1), 1.0f, COLOR,
                    8f * ctx.power(), 30);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.3f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Hold to become a whirlwind of body parts that drags foes in and pounds them.
     *
     * @name Chop-Chop Cyclone
     */
    public static final class Cyclone extends Ability {
        Cyclone() {
            super(props(ActivationType.HOLD, AbilityIcon.TORNADO, COLOR).cooldown(500).stamina(28).staminaPerTick(0.4f)
                    .maxActive(80).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 center = player.position().add(0, 1, 0);
            if (ctx.activeTicks() % 4 == 0) {
                for (LivingEntity target : AbilityDamage.around(player, center, 8 * ctx.power())) {
                    Kit.pull(target, center, 0.35);
                    if (target.distanceToSqr(center) < 9) {
                        Kit.hit(player, target, AbilityDamage.STRIKE, 4f * ctx.power());
                    }
                }
                Vfx.spiral(ctx.level(), player, center, COLOR, 4f, Kind.SHARD);
                ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 0.8f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Dozens of detached hands carry knives over the area you point at, then let them rain down. */
    public static final class KnifeRain extends Ability {
        KnifeRain() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, STEEL).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 26), 8);
            AbilityProjectile.zone(player, RAIN, at, 5.5 * ctx.power(), STEEL, 4f * ctx.power(), 80);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.4f, 0.5f);
        }
    }
}
