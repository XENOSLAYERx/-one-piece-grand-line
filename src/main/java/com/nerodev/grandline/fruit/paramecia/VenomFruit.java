package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

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
import com.nerodev.grandline.fruit.kit.Buffs;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Doku Doku no Mi. Create and control every kind of poison: dragons of venom, clouds of nerve gas, and finally a
 * demon of poison that rots everything near it. The user is immune to all of it.
 */
public class VenomFruit extends DevilFruit {
    public static final int COLOR = 0x6A2A8A;
    public static final int TOXIC = 0xA8E040;
    static final String M = "fruit/grandline:venom";

    public static final DeferredHolder<Ability, Hydra> HYDRA = ability("venom_hydra", Hydra::new);
    public static final DeferredHolder<Ability, VenomRoad> VENOM_ROAD = ability("venom_road", VenomRoad::new);
    public static final DeferredHolder<Ability, ChloroBall> CHLORO_BALL = ability("venom_chloro_ball", ChloroBall::new);
    public static final DeferredHolder<Ability, PoisonCloud> POISON_CLOUD = ability("venom_poison_cloud", PoisonCloud::new);
    public static final DeferredHolder<Ability, VenomCoat> VENOM_COAT = ability("venom_coat", VenomCoat::new);
    public static final DeferredHolder<Ability, VenomDemon> VENOM_DEMON = ability("venom_demon", VenomDemon::new);
    public static final DeferredHolder<Ability, VenomFlood> VENOM_FLOOD = ability("venom_flood", VenomFlood::new);

    public static final ResourceLocation DRAGON = GrandLine.id("venom_hydra");
    public static final ResourceLocation BALL = GrandLine.id("venom_chloro_ball");
    public static final ResourceLocation CLOUD = GrandLine.id("venom_cloud");
    public static final ResourceLocation FLOOD = GrandLine.id("venom_flood");

    public VenomFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Doku Doku no Mi", COLOR)
                .abilities(HYDRA, VENOM_ROAD, CHLORO_BALL, POISON_CLOUD, VENOM_COAT)
                .awakened(VENOM_DEMON, VENOM_FLOOD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(DRAGON, Projectiles.homing(20, 0.15, Projectiles.blast(2.8, AbilityDamage.POISON,
                (p, target) -> envenom(target, 160, 1), p -> {
                    if (p.level() instanceof ServerLevel level) {
                        Vfx.burst(level, p, p.position(), COLOR, 2.8f, Kind.SMOKE, Vfx.SHOCKWAVE);
                    }
                })));
        ProjectileBehavior.Registry.register(BALL, Projectiles.blast(2.5, AbilityDamage.POISON, (p, target) -> envenom(target, 100, 0),
                p -> {
                    ServerPlayer owner = p.ownerPlayer();
                    if (owner != null) {
                        AbilityProjectile.zone(owner, CLOUD, p.position(), 3, TOXIC, 1.5f, 100);
                    }
                }));
        ProjectileBehavior.Registry.register(CLOUD, Projectiles.zone(10, (p, target) -> {
            envenom(target, 60, 1);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.damage() > 0 && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(FLOOD, Projectiles.zone(4, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.hit(owner, target, AbilityDamage.POISON, p.damage());
                Kit.launch(target, p.getDeltaMovement().scale(1.3).add(0, 0.25, 0));
                envenom(target, 160, 2);
            }
        }, null));
    }

    static void envenom(LivingEntity target, int ticks, int level) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, level));
        if (level >= 2) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks / 2, 0));
        }
    }

    /** No poison affects the man who makes them all. */
    @Override
    public void onIncomingDamage(ServerPlayer user, LivingIncomingDamageEvent event) {
        if (event.getSource().is(AbilityDamage.POISON)) {
            event.setCanceled(true);
        }
    }

    @Override
    public void tick(ServerPlayer user, boolean awakened) {
        if (user.hasEffect(MobEffects.POISON)) {
            user.removeEffect(MobEffects.POISON);
        }
        if (user.hasEffect(MobEffects.WITHER)) {
            user.removeEffect(MobEffects.WITHER);
        }
    }

    @Override
    public boolean ticks() {
        return true;
    }

    /** Venom Coat and Venom Demon: every blow you land poisons. */
    @Override
    public void onDealDamage(ServerPlayer user, LivingEntity target, LivingIncomingDamageEvent event) {
        AbilityState state = AbilityState.of(user);
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && (state.isActive(VENOM_COAT.getId()) || state.isActive(VENOM_DEMON.getId()))) {
            envenom(target, 120, state.isActive(VENOM_DEMON.getId()) ? 2 : 1);
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Loose a dragon of venom that hunts down a foe and bursts over it in a spray of poison.
     *
     * @name Hydra
     */
    public static final class Hydra extends Ability {
        Hydra() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, COLOR).cooldown(70).stamina(14).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, DRAGON, 0.9, 1.4f, COLOR, 9f, 60);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1.2f, 0.7f);
        }
    }

    /**
     * Ride a stream of venom forward; everyone along the way is splashed and poisoned.
     *
     * @name Venom Road
     */
    public static final class VenomRoad extends Ability {
        VenomRoad() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, TOXIC).cooldown(80).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 12 * ctx.power(), 1.2)) {
                Kit.hit(player, target, AbilityDamage.POISON, 6f * ctx.power());
                envenom(target, 120, 1);
            }
            Vec3 start = player.position().add(0, 0.5, 0);
            Kit.dash(player, 1.8, 0.15);
            Vfx.trail(ctx.level(), player, start, start.add(player.getLookAngle().scale(12)), COLOR, 0.9f, Kind.DROP);
            ctx.animate(GLEffects.ANIM_SORU);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1f, 1.2f);
        }
    }

    /**
     * Throw a ball of poison gas that bursts into a lingering, choking cloud.
     *
     * @name Chloro Ball
     */
    public static final class ChloroBall extends Ability {
        ChloroBall() {
            super(props(ActivationType.INSTANT, AbilityIcon.ORB, TOXIC).cooldown(90).stamina(12).mastery(M, 10).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.shoot(ctx, BALL, 1.1, 0.8f, TOXIC, 6f, 40).withGravity(0.03);
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(GLSounds.GAS_HISS.get(), 1f, 1.2f);
        }
    }

    /** Release a great cloud of nerve gas where you point: everyone in it is poisoned and reels. */
    public static final class PoisonCloud extends Ability {
        PoisonCloud() {
            super(props(ActivationType.INSTANT, AbilityIcon.CLOUD, COLOR).cooldown(240).stamina(20).mastery(M, 20)
                    .masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, CLOUD, at, 6 * ctx.power(), COLOR, 2f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, at.add(0, 1, 0), COLOR, 5f, Kind.SMOKE, Vfx.RING);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GAS_HISS.get(), 1.6f, 0.7f);
        }
    }

    /** Coat your body in venom: every blow you land poisons, and you shrug off a little more damage. */
    public static final class VenomCoat extends Buffs.Toggle {
        VenomCoat() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.15f)
                            .maxActive(600).mastery(M, 35).masteryGain(0.9),
                    List.of(Buffs.mod(Attributes.ARMOR, GrandLine.id("venom_coat_armor"), 4, Operation.ADD_VALUE)));
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 6 == 0) {
                Vfx.impact(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 0.8f, Kind.DROP);
            }
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Become a towering demon of venom: everything near you rots, and your blows poison deeply.
     *
     * @name Venom Demon
     */
    public static final class VenomDemon extends Buffs.Toggle {
        VenomDemon() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SKULL, COLOR).cooldown(900).stamina(32).staminaPerTick(0.3f)
                            .maxActive(400).masteryGain(1.5),
                    List.of(Buffs.mod(Attributes.SCALE, GrandLine.id("venom_demon_scale"), 1.0, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ATTACK_DAMAGE, GrandLine.id("venom_demon_damage"), 6, Operation.ADD_VALUE),
                            Buffs.mod(Attributes.ARMOR, GrandLine.id("venom_demon_armor"), 8, Operation.ADD_VALUE)));
        }

        @Override
        protected void started(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().getBoundingBox().getCenter(), COLOR, 4f, Kind.SMOKE, Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROAR);
            ctx.sound(GLSounds.GAS_BOOM.get(), 1.5f, 0.5f);
        }

        @Override
        protected boolean whileActive(AbilityContext ctx) {
            if (ctx.activeTicks() % 10 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.around(player, player.getBoundingBox().getCenter(), 5 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.POISON, 4f * ctx.power());
                    envenom(target, 80, 2);
                }
                Vfx.burst(ctx.level(), player, player.getBoundingBox().getCenter(), COLOR, 4f, Kind.SMOKE, 0);
            }
            return true;
        }
    }

    /**
     * Unleash a tidal wave of venom that sweeps everything in front of you away.
     *
     * @name Venom Flood
     */
    public static final class VenomFlood extends Ability {
        VenomFlood() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, TOXIC).cooldown(700).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            AbilityProjectile.spawn(player, FLOOD, player.position().add(dir.scale(2)), dir.scale(0.55), 9f * ctx.power(), COLOR,
                    9f * ctx.power(), 50);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 2f, 0.5f);
        }
    }
}
