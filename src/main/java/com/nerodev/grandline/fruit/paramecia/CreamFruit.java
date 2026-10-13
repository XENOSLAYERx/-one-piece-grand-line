package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchD.props;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.AbilityState;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Kuri Kuri no Mi. Whipped cream, scalding hot: it smothers, sticks and burns. The user is used to the heat.
 */
public class CreamFruit extends DevilFruit {
    public static final int COLOR = 0xFFF4E0;
    public static final int PINK = 0xF0A8C8;
    static final String M = "fruit/grandline:cream";

    public static final DeferredHolder<Ability, CreamShot> CREAM_SHOT = ability("cream_shot", CreamShot::new);
    public static final DeferredHolder<Ability, CreamSpray> CREAM_SPRAY = ability("cream_spray", CreamSpray::new);
    public static final DeferredHolder<Ability, CreamFloor> CREAM_FLOOR = ability("cream_floor", CreamFloor::new);
    public static final DeferredHolder<Ability, CreamCoat> CREAM_COAT = ability("cream_coat", CreamCoat::new);
    public static final DeferredHolder<Ability, Smother> SMOTHER = ability("cream_smother", Smother::new);
    public static final DeferredHolder<Ability, CreamFlood> CREAM_FLOOD = ability("cream_flood", CreamFlood::new);
    public static final DeferredHolder<Ability, CakeTomb> CAKE_TOMB = ability("cream_cake_tomb", CakeTomb::new);

    public static final ResourceLocation BLOB = GrandLine.id("cream_blob");
    public static final ResourceLocation FLOOR = GrandLine.id("cream_floor");
    public static final ResourceLocation FLOOD = GrandLine.id("cream_flood");

    public CreamFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Kuri Kuri no Mi", COLOR)
                .abilities(CREAM_SHOT, CREAM_SPRAY, CREAM_FLOOR, CREAM_COAT, SMOTHER)
                .awakened(CREAM_FLOOD, CAKE_TOMB));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(BLOB, Projectiles.blast(2, AbilityDamage.FIRE, (p, target) -> scald(target, 60), null));
        ProjectileBehavior.Registry.register(FLOOR, Projectiles.zone(10, (p, target) -> {
            scald(target, 30);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.FIRE, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(FLOOD, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.FIRE, p.damage());
                Kit.launch(target, p.getDeltaMovement().scale(1.2).add(0, 0.2, 0));
                scald(target, 80);
            }
        }, null));
    }

    /** Hot cream sticks to a creature: slowed and burning. */
    static void scald(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), Math.min(60, ticks)));
    }

    /** The user can't be scalded; in Cream Coat, bare-handed attackers burn their hands. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 0.5f);
        }
        if (AbilityState.of(user).isActive(CREAM_COAT.getId())
                && (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            scald(attacker, 60);
            Kit.hit(user, attacker, AbilityDamage.FIRE, 3f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Fling a dollop of scalding cream that splatters on impact. */
    public static final class CreamShot extends Ability {
        CreamShot() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, COLOR).cooldown(40).stamina(8).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BLOB, 1.3, 0.6f, COLOR, 7f, 30).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_SQUISH, 1f, 1.2f);
        }
    }

    /** Hold to spray a stream of boiling cream over everyone in front of you. */
    public static final class CreamSpray extends Ability {
        CreamSpray() {
            super(props(ActivationType.HOLD, AbilityIcon.WAVE, PINK).cooldown(120).stamina(10).staminaPerTick(0.3f).maxActive(50)
                    .masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 30)) {
                    Kit.hit(player, target, AbilityDamage.FIRE, 2f * ctx.power());
                    scald(target, 40);
                }
                Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(6)), COLOR, 1f,
                        Kind.SMOKE, Vfx.BEAM_PARTICLES);
                ctx.sound(GLSounds.STEAM.get(), 0.5f, 1.2f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Cover the ground where you point with hot cream: anyone wading through it is slowed and burned. */
    public static final class CreamFloor extends Ability {
        CreamFloor() {
            super(props(ActivationType.INSTANT, AbilityIcon.DROP, COLOR).cooldown(200).stamina(14).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 18), 6);
            AbilityProjectile.zone(player, FLOOR, at, 5 * ctx.power(), COLOR, 2f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, at, COLOR, 5f, Kind.SMOKE, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 1.2f, 1f);
        }
    }

    /** Cover yourself in scalding cream: attackers burn their hands, and you stay cool. */
    public static final class CreamCoat extends Ability {
        CreamCoat() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, PINK).cooldown(200).stamina(12).staminaPerTick(0.15f)
                    .maxActive(500).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));
            if (ctx.activeTicks() % 8 == 0) {
                Vfx.impact(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 0.8f, Kind.SMOKE);
            }
            return true;
        }
    }

    /** Smother a foe in a mound of hot cream: it is stuck fast and burning for three seconds. */
    public static final class Smother extends Ability {
        Smother() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(200).stamina(16).mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 14).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 14).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.CREAM, 60);
                Kit.hit(player, target, AbilityDamage.FIRE, 6f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 1.4f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Unleash a rolling wave of boiling cream that sweeps everything in front of you away. */
    public static final class CreamFlood extends Ability {
        CreamFlood() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(600).stamina(30).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, FLOOD, player.position().add(dir.scale(2)), dir.scale(0.55), 8f * ctx.power(), COLOR,
                    9f * ctx.power(), 50);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.STEAM.get(), 1.6f, 0.6f);
        }
    }

    /** Bury every foe around you in a cake of hot cream: they are stuck, burning, for four seconds. */
    public static final class CakeTomb extends Ability {
        CakeTomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, PINK).cooldown(700).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.CREAM, 80);
                Kit.hit(player, target, AbilityDamage.FIRE, 4f * ctx.power());
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
            }
            Vfx.burst(ctx.level(), player, player.position().add(0, 1, 0), COLOR, 8f, Kind.SMOKE, Vfx.RING | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.SLIME_BLOCK_PLACE, 2f, 0.4f);
        }
    }
}
