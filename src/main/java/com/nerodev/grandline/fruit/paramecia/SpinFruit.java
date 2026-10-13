package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchC.props;

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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Guru Guru no Mi. Spin any part of the body like a propeller: fly, drill, blow foes away with the wind, and turn the
 * whole body into a whirling saw.
 */
public class SpinFruit extends DevilFruit {
    public static final int COLOR = 0x4A8AC8;
    public static final int WIND = 0xF0F0F0;
    static final String M = "fruit/grandline:spin";

    public static final DeferredHolder<Ability, Propeller> PROPELLER = ability("spin_propeller", Propeller::new);
    public static final DeferredHolder<Ability, Drill> DRILL = ability("spin_drill", Drill::new);
    public static final DeferredHolder<Ability, Gale> GALE = ability("spin_gale", Gale::new);
    public static final DeferredHolder<Ability, Liftoff> LIFTOFF = ability("spin_liftoff", Liftoff::new);
    public static final DeferredHolder<Ability, Buzzsaw> BUZZSAW = ability("spin_buzzsaw", Buzzsaw::new);
    public static final DeferredHolder<Ability, Twister> TWISTER = ability("spin_twister", Twister::new);
    public static final DeferredHolder<Ability, DiveBomb> DIVE_BOMB = ability("spin_dive_bomb", DiveBomb::new);

    public static final ResourceLocation TWISTER_KIND = GrandLine.id("spin_twister");

    public SpinFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.COMMON, "Guru Guru no Mi", COLOR)
                .abilities(PROPELLER, DRILL, GALE, LIFTOFF, BUZZSAW)
                .awakened(TWISTER, DIVE_BOMB));
    }

    static void init() {}

    static void behaviors() {
        // A travelling twister that sucks foes in and carries them up.
        ProjectileBehavior.Registry.register(TWISTER_KIND, Projectiles.zone(5, (p, target) -> {
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null) {
                Kit.pull(target, p.position().add(0, 3, 0), 0.5);
                Kit.hit(owner, target, AbilityDamage.STRIKE, p.damage());
            }
        }, null));
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Spin your arms like propellers and fly. */
    public static final class Propeller extends Ability {
        Propeller() {
            super(props(ActivationType.TOGGLE, AbilityIcon.TORNADO, COLOR).cooldown(80).stamina(10).staminaPerTick(0.15f)
                    .maxActive(1200).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Kit.setFlight(ctx.player(), true);
            ctx.sound(SoundEvents.ELYTRA_FLYING, 0.4f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.resetFallDistance();
            if (ctx.activeTicks() % 4 == 0 && !player.onGround()) {
                Vfx.spiral(ctx.level(), player, player.position().add(0, 2.1, 0), WIND, 0.8f, Kind.SMOKE);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Kit.setFlight(ctx.player(), false);
        }
    }

    /** Spin your fist into a drill and drive it through the foes in front of you. */
    public static final class Drill extends Ability {
        Drill() {
            super(props(ActivationType.INSTANT, AbilityIcon.SPEAR, WIND).cooldown(45).stamina(9).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.line(player, 6 * ctx.power(), 0.9)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 0.8, 0.2);
            }
            Vfx.spiral(ctx.level(), player, ctx.handPos().add(player.getLookAngle().scale(1.5)), WIND, 0.8f, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.GRINDSTONE_USE, 1f, 1.6f);
        }
    }

    /** Whirl your arms to whip up a gale that blows everything in front of you far away. */
    public static final class Gale extends Ability {
        Gale() {
            super(props(ActivationType.INSTANT, AbilityIcon.WAVE, WIND).cooldown(80).stamina(12).mastery(M, 10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double range = 9 * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, range, 40)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 3f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 2.2, 0.4);
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), player.getEyePosition().add(player.getLookAngle().scale(range)), WIND, 1.8f,
                    Kind.SMOKE, Vfx.BEAM_PARTICLES);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ELYTRA_FLYING, 0.8f, 2f);
        }
    }

    /** Grab a foe and spin it straight up into the sky. */
    public static final class Liftoff extends Ability {
        Liftoff() {
            super(props(ActivationType.INSTANT, AbilityIcon.JUMP, COLOR).cooldown(120).stamina(14).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 6f * ctx.power());
                Kit.launch(target, new Vec3(0, 1.8, 0));
                Vfx.spiral(ctx.level(), target, target.position(), WIND, 1.2f, Kind.SMOKE);
            });
            ctx.animate(GLEffects.ANIM_THROW_OVERHEAD);
            ctx.sound(SoundEvents.ELYTRA_FLYING, 0.8f, 1.4f);
        }
    }

    /** Hold to spin your whole body like a saw blade, cutting everything around you. */
    public static final class Buzzsaw extends Ability {
        Buzzsaw() {
            super(props(ActivationType.HOLD, AbilityIcon.SWIRL, COLOR).cooldown(140).stamina(12).staminaPerTick(0.35f).maxActive(50)
                    .mastery(M, 35).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_SWEEP);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 4 == 0) {
                ServerPlayer player = ctx.player();
                for (LivingEntity target : AbilityDamage.around(player, player.position().add(0, 1, 0), 3.2 * ctx.power())) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 3.5f * ctx.power());
                }
                Vfx.spiral(ctx.level(), player, player.position().add(0, 1, 0), WIND, 2.2f, Kind.SPARK);
                ctx.sound(SoundEvents.GRINDSTONE_USE, 0.5f, 2f);
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Spin up a twister and send it rolling forward, sucking foes in and carrying them aloft. */
    public static final class Twister extends Ability {
        Twister() {
            super(props(ActivationType.INSTANT, AbilityIcon.TORNADO, WIND).cooldown(500).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.spawn(player, TWISTER_KIND, player.position().add(ctx.horizontalLook().scale(2)),
                    ctx.horizontalLook().scale(0.35), 6f * ctx.power(), WIND, 4f * ctx.power(), 80);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.ELYTRA_FLYING, 1.4f, 0.8f);
        }
    }

    /** Spin like a falling drill and dive onto the spot you point at, smashing everything there. */
    public static final class DiveBomb extends Ability {
        DiveBomb() {
            super(props(ActivationType.INSTANT, AbilityIcon.METEOR, COLOR).cooldown(400).stamina(26).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 30), 12);
            Vec3 from = player.position();
            player.teleportTo(at.x, at.y, at.z);
            player.resetFallDistance();
            for (LivingEntity target : AbilityDamage.around(player, at.add(0, 1, 0), 4 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.CRUSH, 16f * ctx.power());
                Kit.launch(target, target.position().subtract(at).normalize().scale(0.6).add(0, 0.6, 0));
            }
            Vfx.trail(ctx.level(), player, from.add(0, 1, 0), at.add(0, 1, 0), WIND, 1f, Kind.SMOKE);
            Vfx.burst(ctx.level(), player, at, COLOR, 4f, Kind.SHARD, Vfx.SHOCKWAVE | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_ROCKET);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 1.2f);
        }
    }
}
