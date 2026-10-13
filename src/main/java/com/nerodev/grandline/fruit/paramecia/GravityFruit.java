package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchE.props;

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
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * Zushi Zushi no Mi. Command gravity: press foes flat, lift the ground and ride it, open a well that drags everything
 * in, and pull meteors down from space.
 */
public class GravityFruit extends DevilFruit {
    public static final int COLOR = 0x6A3AA8;
    public static final int DEEP = 0x1A1A3A;
    static final String M = "fruit/grandline:gravity";

    public static final DeferredHolder<Ability, GravityBlade> GRAVITY_BLADE = ability("gravity_blade", GravityBlade::new);
    public static final DeferredHolder<Ability, Press> PRESS = ability("gravity_press", Press::new);
    public static final DeferredHolder<Ability, Levitate> LEVITATE = ability("gravity_levitate", Levitate::new);
    public static final DeferredHolder<Ability, Uplift> UPLIFT = ability("gravity_uplift", Uplift::new);
    public static final DeferredHolder<Ability, GravityWell> GRAVITY_WELL = ability("gravity_well", GravityWell::new);
    public static final DeferredHolder<Ability, Meteor> METEOR = ability("gravity_meteor", Meteor::new);
    public static final DeferredHolder<Ability, HeavyField> HEAVY_FIELD = ability("gravity_heavy_field", HeavyField::new);

    public static final ResourceLocation WELL = GrandLine.id("gravity_well");
    public static final ResourceLocation ROCK = GrandLine.id("gravity_meteor");
    public static final ResourceLocation FIELD = GrandLine.id("gravity_field");

    public GravityFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.LEGENDARY, "Zushi Zushi no Mi", COLOR)
                .abilities(GRAVITY_BLADE, PRESS, LEVITATE, UPLIFT, GRAVITY_WELL)
                .awakened(METEOR, HEAVY_FIELD));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(WELL, Projectiles.zone(5, (p, target) -> {
            Kit.pull(target, p.position().add(0, 0.5, 0), 0.5);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(ROCK, Projectiles.blast(5, AbilityDamage.BLAST,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)),
                p -> {
                    ServerLevel level = (ServerLevel) p.level();
                    Vfx.burst(level, p, p.position(), 0xFF8A3A, 6f, Kind.SMOKE, Vfx.RING | Vfx.HEAVY | Vfx.FLASH | Vfx.GROUND);
                    Vfx.eruption(level, p, p.position(), 0xFF8A3A, 5f, Kind.FLAME);
                }));
        ProjectileBehavior.Registry.register(FIELD, Projectiles.zone(10, (p, target) -> {
            press(target, 15);
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 20 == 0) {
                Kit.hit(owner, target, AbilityDamage.CRUSH, p.damage());
            }
        }, null));
    }

    /** Presses a creature down: it is slammed to the ground and can barely move or jump. */
    static void press(LivingEntity target, int ticks) {
        target.setDeltaMovement(target.getDeltaMovement().x * 0.2, -1.2, target.getDeltaMovement().z * 0.2);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3));
        Buffs.timed(target, Buffs.mod(Attributes.JUMP_STRENGTH, GrandLine.id("gravity_press_jump"), -0.9,
                Operation.ADD_MULTIPLIED_TOTAL), ticks);
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Draw and cut: the slash carries gravity with it and presses everything it hits into the ground.
     *
     * @name Gravity Blade
     */
    public static final class GravityBlade extends Ability {
        GravityBlade() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, COLOR).cooldown(40).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.SLASH, 9f * ctx.power());
                press(target, 30);
            }
            Vfx.slash(ctx.level(), player, ctx.handPos().add(ctx.horizontalLook().scale(2)), player.getLookAngle(), COLOR, 2.4f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
        }
    }

    /** Crush the spot you point at under heavy gravity: everything there is slammed down and pinned. */
    public static final class Press extends Ability {
        Press() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, DEEP).cooldown(100).stamina(12).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 24), 8);
            for (LivingEntity target : AbilityDamage.around(player, at, 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                press(target, 60);
            }
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.RING, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1f, 0.6f);
        }
    }

    /** Lift yourself on your own gravity and fly. */
    public static final class Levitate extends Ability {
        Levitate() {
            super(props(ActivationType.TOGGLE, AbilityIcon.CLOUD, COLOR).cooldown(160).stamina(10).staminaPerTick(0.2f).maxActive(900)
                    .mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 0.8f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.resetFallDistance();
            if (ctx.activeTicks() % 6 == 0 && !player.onGround()) {
                Vfx.impact(ctx.level(), player, player.position(), COLOR, 0.9f, Kind.SHARD);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /** Reverse gravity around you: every foe nearby is flung up into the air, to come crashing down. */
    public static final class Uplift extends Ability {
        Uplift() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(200).stamina(16).mastery(M, 20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 8 * ctx.power())) {
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 3));
                Kit.hit(player, target, AbilityDamage.CRUSH, 4f * ctx.power());
            }
            Vfx.eruption(ctx.level(), player, player.position(), COLOR, 6f, Kind.SHARD);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 8f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 1.2f, 1.2f);
        }
    }

    /**
     * Open a gravity well where you point: for six seconds it drags everything around into its crushing centre.
     *
     * @name Gravity Well
     */
    public static final class GravityWell extends Ability {
        GravityWell() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, DEEP).cooldown(300).stamina(18).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, WELL, at.add(0, 0.5, 0), 7 * ctx.power(), DEEP, 4f * ctx.power(), 120);
            Vfx.burst(ctx.level(), player, at, COLOR, 7f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_CLAW);
            ctx.sound(GLSounds.DARK_VORTEX.get(), 1.4f, 0.7f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Pull a meteor down from space onto the spot you point at. */
    public static final class Meteor extends Ability {
        Meteor() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, 0xFF8A3A).cooldown(900).stamina(34).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 40), 10);
            Vec3 from = at.add(-6, 30, -6);
            Vec3 velocity = at.subtract(from).normalize().scale(1.6);
            AbilityProjectile.spawn(player, ROCK, from, velocity, 3.5f, 0xFF8A3A, 22f * ctx.power(), 60).withPower(ctx.power());
            Vfx.burst(ctx.level(), player, at, 0xFF8A3A, 5f, Kind.RING, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.WITHER_SPAWN, 0.6f, 1.6f);
        }
    }

    /** Lay crushing gravity over the land around you for ten seconds: every foe inside is pinned flat and ground down. */
    public static final class HeavyField extends Ability {
        HeavyField() {
            super(props(ActivationType.INSTANT, AbilityIcon.MOON, DEEP).cooldown(800).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.discard(player, FIELD);
            AbilityProjectile.zone(player, FIELD, player.position(), 12 * ctx.power(), DEEP, 5f * ctx.power(), 200);
            Vfx.burst(ctx.level(), player, player.position(), COLOR, 12f, Kind.RING, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_CONQUEROR_RELEASE);
            ctx.sound(GLSounds.GIANT_IMPACT.get(), 2f, 0.4f);
        }
    }
}
