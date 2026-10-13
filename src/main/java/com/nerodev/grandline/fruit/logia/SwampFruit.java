package com.nerodev.grandline.fruit.logia;

import static com.nerodev.grandline.fruit.logia.LogiaBatch.ability;
import static com.nerodev.grandline.fruit.logia.LogiaBatch.props;

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
import com.nerodev.grandline.fruit.LogiaFruit;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Numa Numa no Mi. A bottomless swamp: it swallows people and projectiles alike and spits them back out. Anyone
 * caught in its mud sinks and cannot walk away.
 */
public class SwampFruit extends LogiaFruit {
    public static final int COLOR = 0x5A4A2A;
    public static final int MUD = 0x3A2E1A;
    static final String M = "fruit/grandline:swamp";
    static final String STORED = GrandLine.MODID + ":swamp_stored";

    public static final DeferredHolder<Ability, SwampPit> SWAMP_PIT = ability("swamp_pit", SwampPit::new);
    public static final DeferredHolder<Ability, Swallow> SWALLOW = ability("swamp_swallow", Swallow::new);
    public static final DeferredHolder<Ability, MudShot> MUD_SHOT = ability("mud_shot", MudShot::new);
    public static final DeferredHolder<Ability, BottomlessBody> BOTTOMLESS = ability("bottomless_body", BottomlessBody::new);
    public static final DeferredHolder<Ability, SwampSpew> SPEW = ability("swamp_spew", SwampSpew::new);
    public static final DeferredHolder<Ability, EndlessSwamp> ENDLESS_SWAMP = ability("endless_swamp", EndlessSwamp::new);
    public static final DeferredHolder<Ability, SwampGrave> SWAMP_GRAVE = ability("swamp_grave", SwampGrave::new);

    public static final ResourceLocation PIT = GrandLine.id("swamp_pit");
    public static final ResourceLocation MUD_BALL = GrandLine.id("mud_shot");

    public SwampFruit() {
        super(Properties.of(FruitType.LOGIA, GLRarity.RARE, "Numa Numa no Mi", COLOR)
                .abilities(SWAMP_PIT, SWALLOW, MUD_SHOT, BOTTOMLESS, SPEW)
                .awakened(ENDLESS_SWAMP, SWAMP_GRAVE));
    }

    static void init() {}

    static void behaviors() {
        ProjectileBehavior.Registry.register(PIT, Projectiles.zone(3, (p, target) -> {
            Kit.root(target, Vfx.StatusLook.MUD, 8);
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 1, 0.2).add(0, -0.12, 0));
            ServerPlayer owner = p.ownerPlayer();
            if (owner != null && p.tickCount % 21 == 0) {
                Kit.hit(owner, target, AbilityDamage.DRAIN, p.damage());
            }
        }, null));
        ProjectileBehavior.Registry.register(MUD_BALL, Projectiles.bolt(AbilityDamage.STRIKE,
                (p, target) -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2))));
    }

    static int stored(ServerPlayer player) {
        return player.getPersistentData().getInt(STORED);
    }

    static void setStored(ServerPlayer player, int value) {
        player.getPersistentData().putInt(STORED, Math.max(0, Math.min(40, value)));
    }

    @Override
    protected boolean immuneTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Turn the ground where you look into a bottomless swamp: anyone in it sinks and cannot get out. */
    public static final class SwampPit extends Ability {
        SwampPit() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, COLOR).cooldown(160).stamina(20).masteryGain(0.9));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 at = Kit.ground(ctx.level(), Kit.aimPoint(player, 20), 6);
            AbilityProjectile.zone(player, PIT, at, 4 * ctx.power(), MUD, 2f * ctx.power(), 110);
            Vfx.burst(ctx.level(), player, at, MUD, 4f, Kind.DROP, Vfx.RING | Vfx.GROUND);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1.2f, 0.8f);
        }
    }

    /** The swamp in your body opens up and swallows a nearby foe whole, then holds it. */
    public static final class Swallow extends Ability {
        Swallow() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, COLOR).cooldown(140).stamina(18).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 10).isPresent() ? Optional.empty() : Optional.of(Component.literal("No target in sight"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 10).ifPresent(target -> {
                Kit.pull(target, player.position(), 1.5);
                Kit.encase(target, Vfx.StatusLook.MUD, 60);
                Kit.hit(player, target, AbilityDamage.DRAIN, 4f * ctx.power());
                setStored(player, stored(player) + 2);
                Vfx.beam(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), MUD, 1f, Kind.DROP,
                        Vfx.BEAM_PARTICLES);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1.4f, 0.6f);
        }
    }

    /** Hold to spray a stream of heavy mud that bogs down whoever it hits. */
    public static final class MudShot extends Ability {
        MudShot() {
            super(props(ActivationType.HOLD, AbilityIcon.DROP, COLOR).cooldown(100).stamina(10).staminaPerTick(0.4f)
                    .maxActive(40).mastery(M, 20).masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ctx.animate(GLEffects.ANIM_PALMS);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            if (ctx.activeTicks() % 3 == 0) {
                Kit.shoot(ctx, MUD_BALL, 1.6, 0.5f, MUD, 3f, 20).withGravity(0.03);
                if (ctx.activeTicks() % 9 == 0) {
                    ctx.sound(GLSounds.SWAMP_GULP.get(), 0.6f, 1.5f);
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            Fx.stopAnimation(ctx.player());
        }
    }

    /** Let your body turn to swamp: arrows and other projectiles sink into you harmlessly (and are kept). */
    public static final class BottomlessBody extends Ability {
        BottomlessBody() {
            super(props(ActivationType.TOGGLE, AbilityIcon.SHIELD, COLOR).cooldown(200).stamina(12).staminaPerTick(0.1f)
                    .maxActive(20 * 30).mastery(M, 35).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            Vfx.burst(ctx.level(), ctx.player(), ctx.player().position().add(0, 1, 0), MUD, 2f, Kind.DROP, Vfx.RING);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1f, 1f);
        }

        @Override
        public void onOwnerHurt(AbilityContext ctx, LivingIncomingDamageEvent event) {
            if (event.getSource().getDirectEntity() instanceof Projectile projectile) {
                event.setCanceled(true);
                projectile.discard();
                ServerPlayer player = ctx.player();
                setStored(player, stored(player) + 1);
                Vfx.impact(ctx.level(), player, player.position().add(0, 1, 0), MUD, 1f, Kind.DROP);
                ctx.sound(GLSounds.SWAMP_GULP.get(), 0.8f, 1.4f);
            }
        }
    }

    /** Spit out everything the swamp swallowed in a crushing torrent. More stored, more damage. */
    public static final class SwampSpew extends Ability {
        SwampSpew() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, MUD).cooldown(120).stamina(16).mastery(M, 50)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            int stored = stored(player);
            float damage = (6f + Math.min(20, stored) * 1.5f) * ctx.power();
            for (LivingEntity target : AbilityDamage.cone(player, 8, 45)) {
                Kit.hit(player, target, AbilityDamage.CRUSH, damage);
                AbilityDamage.knockback(target, player.position(), 1.4, 0.3);
            }
            setStored(player, 0);
            Vec3 eye = player.getEyePosition();
            Vfx.beam(ctx.level(), player, ctx.handPos(), eye.add(player.getLookAngle().scale(8)), MUD, 2f + stored * 0.05f,
                    Kind.SHARD, Vfx.BEAM_DARK);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 1.6f, 0.5f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** The whole area around you becomes swamp. */
    public static final class EndlessSwamp extends Ability {
        EndlessSwamp() {
            super(props(ActivationType.INSTANT, AbilityIcon.VORTEX, MUD).cooldown(600).stamina(35).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            AbilityProjectile.zone(player, PIT, player.position(), 11 * ctx.power(), MUD, 3f * ctx.power(), 160);
            Vfx.burst(ctx.level(), player, player.position(), MUD, 11f, Kind.DROP, Vfx.RING | Vfx.GROUND | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 2f, 0.4f);
        }
    }

    /** Everyone around you is dragged under the mud and held there. */
    public static final class SwampGrave extends Ability {
        SwampGrave() {
            super(props(ActivationType.INSTANT, AbilityIcon.SKULL, MUD).cooldown(700).stamina(40).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 12 * ctx.power())) {
                Kit.encase(target, Vfx.StatusLook.MUD, 80);
                Kit.hit(player, target, AbilityDamage.DRAIN, 7f * ctx.power());
            }
            Vfx.burst(ctx.level(), player, player.position(), MUD, 12f, Kind.DROP, Vfx.RING | Vfx.SHOCKWAVE | Vfx.HEAVY);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.SWAMP_GULP.get(), 2f, 0.5f);
        }
    }
}
