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
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import com.nerodev.grandline.sound.GLSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Ope Ope no Mi. Everything happens inside a ROOM: a sphere the user controls like an operating theatre, where they
 * swap places with anything, cut bodies apart without killing, and take hearts out of chests.
 */
public class OpFruit extends DevilFruit {
    public static final int COLOR = 0xE8487A;
    public static final int ROOM_COLOR = 0x8AD0FF;
    static final String M = "fruit/grandline:op";

    public static final DeferredHolder<Ability, Room> ROOM = ability("op_room", Room::new);
    public static final DeferredHolder<Ability, Shambles> SHAMBLES = ability("op_shambles", Shambles::new);
    public static final DeferredHolder<Ability, Amputate> AMPUTATE = ability("op_amputate", Amputate::new);
    public static final DeferredHolder<Ability, Mes> MES = ability("op_mes", Mes::new);
    public static final DeferredHolder<Ability, CounterShock> COUNTER_SHOCK = ability("op_counter_shock", CounterShock::new);
    public static final DeferredHolder<Ability, GammaKnife> GAMMA_KNIFE = ability("op_gamma_knife", GammaKnife::new);
    public static final DeferredHolder<Ability, Takt> TAKT = ability("op_takt", Takt::new);

    public static final ResourceLocation ROOM_ZONE = GrandLine.id("op_room");

    private record Sphere(ResourceKey<Level> dimension, Vec3 center, double radius) {
        boolean contains(Entity entity) {
            return entity.level().dimension() == dimension
                    && entity.position().add(0, entity.getBbHeight() / 2, 0).distanceTo(center) <= radius;
        }
    }

    private static final Map<UUID, Sphere> ROOMS = new HashMap<>();
    private static final String HEART = "grandline_op_heart";
    private static final String HEART_UNTIL = "grandline_op_heart_until";

    public OpFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.MYTHICAL, "Ope Ope no Mi", COLOR)
                .abilities(ROOM, SHAMBLES, AMPUTATE, MES, COUNTER_SHOCK)
                .awakened(GAMMA_KNIFE, TAKT));
    }

    static void init() {}

    static void behaviors() {
        // The Room itself only needs to exist (and be seen); everything it does is in the abilities.
        ProjectileBehavior.Registry.register(ROOM_ZONE, Projectiles.zone(Integer.MAX_VALUE, (p, target) -> {}, null));
    }

    /** The user's active Room, if any. */
    static Optional<Sphere> room(ServerPlayer player) {
        Sphere sphere = ROOMS.get(player.getUUID());
        return sphere != null && sphere.dimension() == player.level().dimension() ? Optional.of(sphere) : Optional.empty();
    }

    static Optional<Component> needsRoom(ServerPlayer player) {
        return room(player).isPresent() ? Optional.empty() : Optional.of(Component.literal("Open a Room first"));
    }

    /** The creature under the crosshair, if it stands inside the user's Room. */
    static Optional<LivingEntity> patient(ServerPlayer player) {
        return room(player).flatMap(sphere -> Kit.target(player, sphere.radius() * 2).filter(sphere::contains));
    }

    @Override
    public void onLost(ServerPlayer player) {
        ROOMS.remove(player.getUUID());
        player.getPersistentData().remove(HEART);
        player.getPersistentData().remove(HEART_UNTIL);
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Open a sphere of space you control. Inside it you see every living thing, and your other techniques work. */
    public static final class Room extends Ability {
        Room() {
            super(props(ActivationType.TOGGLE, AbilityIcon.ORB, ROOM_COLOR).cooldown(100).stamina(15).staminaPerTick(0.12f)
                    .maxActive(1200).masteryGain(0.6));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            double radius = (FruitManager.isAwakened(player) ? 18 : 12) * ctx.power();
            Vec3 center = player.position().add(0, 1, 0);
            ROOMS.put(player.getUUID(), new Sphere(player.level().dimension(), center, radius));
            AbilityProjectile.zone(player, ROOM_ZONE, player.position(), radius, ROOM_COLOR, 0f, 1220);
            Vfx.burst(ctx.level(), player, center, ROOM_COLOR, (float) radius, Kind.RING, Vfx.RING | Vfx.SHOCKWAVE);
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<Sphere> sphere = room(player);
            if (sphere.isEmpty() || !sphere.get().contains(player)) {
                return false;
            }
            if (ctx.activeTicks() % 20 == 0) {
                // Scan: everything alive in the Room is outlined for the surgeon.
                for (LivingEntity e : AbilityDamage.around(player, sphere.get().center(), sphere.get().radius())) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ROOMS.remove(ctx.player().getUUID());
            Kit.discard(ctx.player(), ROOM_ZONE);
            ctx.sound(SoundEvents.BEACON_DEACTIVATE, 1f, 1.6f);
        }
    }

    /** Swap places with the creature you point at inside your Room, or jump to any spot in it. */
    public static final class Shambles extends Ability {
        Shambles() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, ROOM_COLOR).cooldown(40).stamina(8).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsRoom(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<Sphere> sphere = room(player);
            if (sphere.isEmpty()) {
                return;
            }
            Vec3 from = player.position();
            Optional<LivingEntity> target = patient(player);
            if (target.isPresent()) {
                Vec3 to = target.get().position();
                target.get().teleportTo(from.x, from.y, from.z);
                player.teleportTo(to.x, to.y, to.z);
                Vfx.blink(ctx.level(), target.get(), to, from, ROOM_COLOR, Kind.RING);
                Vfx.blink(ctx.level(), player, from, to, ROOM_COLOR, Kind.RING);
            } else {
                Vec3 to = Kit.aimPoint(player, sphere.get().radius() * 2);
                if (!sphere.get().contains(player) || to.distanceTo(sphere.get().center()) > sphere.get().radius()) {
                    return;
                }
                Vec3 back = to.subtract(player.getEyePosition()).normalize().scale(-0.8);
                Vec3 spot = to.add(back);
                player.teleportTo(spot.x, spot.y, spot.z);
                player.resetFallDistance();
                Vfx.blink(ctx.level(), player, from, spot, ROOM_COLOR, Kind.RING);
            }
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.4f);
        }
    }

    /** Sweep a blade through everyone in front of you inside the Room: bodies come apart, slowed and weakened. */
    public static final class Amputate extends Ability {
        Amputate() {
            super(props(ActivationType.INSTANT, AbilityIcon.BLADE, COLOR).cooldown(60).stamina(12).masteryGain(0.8));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsRoom(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<Sphere> sphere = room(player);
            if (sphere.isEmpty()) {
                return;
            }
            double reach = Math.min(16, sphere.get().radius() * 1.5);
            for (LivingEntity target : AbilityDamage.cone(player, reach, 40)) {
                if (sphere.get().contains(target)) {
                    Kit.hit(player, target, AbilityDamage.SLASH, 8f * ctx.power());
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                    Vfx.slash(ctx.level(), target, target.getBoundingBox().getCenter(), player.getLookAngle(), ROOM_COLOR, 1.6f, 1);
                }
            }
            Vec3 dir = player.getLookAngle();
            Vfx.slash(ctx.level(), player, ctx.handPos().add(dir.scale(3)), dir, ROOM_COLOR, (float) reach * 0.5f, 1);
            ctx.animate(GLEffects.ANIM_SWEEP);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
        }
    }

    /**
     * Take the heart out of a creature in your Room: it weakens and glows wherever it runs. Use Mes
     * again while holding the heart to squeeze it.
     */
    public static final class Mes extends Ability {
        Mes() {
            super(props(ActivationType.INSTANT, AbilityIcon.HEART, COLOR).cooldown(80).stamina(14).mastery(M, 15)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return heldHeart(player) != null || patient(player).isPresent() ? Optional.empty()
                    : Optional.of(Component.literal("Point at someone inside your Room"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            LivingEntity held = heldHeart(player);
            if (held != null) {
                Kit.hit(player, held, AbilityDamage.DRAIN, 12f * ctx.power());
                held.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                Vfx.burst(ctx.level(), held, held.getBoundingBox().getCenter(), COLOR, 1.5f, Kind.HEART, Vfx.FLASH);
                Vfx.burst(ctx.level(), player, ctx.handPos(), COLOR, 0.8f, Kind.HEART, 0);
                player.getPersistentData().remove(HEART);
                ctx.animate(GLEffects.ANIM_CLAW);
                ctx.sound(SoundEvents.SLIME_SQUISH, 1.2f, 0.6f);
                return;
            }
            patient(player).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.DRAIN, 6f * ctx.power());
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                player.getPersistentData().putUUID(HEART, target.getUUID());
                player.getPersistentData().putLong(HEART_UNTIL, ctx.level().getGameTime() + 200);
                Vfx.trail(ctx.level(), player, target.getBoundingBox().getCenter(), ctx.handPos(), COLOR, 0.6f, Kind.HEART);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), ROOM_COLOR, 1.2f, Kind.RING, 0);
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.ITEM_PICKUP, 1f, 0.5f);
        }

        /** The creature whose heart the player holds, if still alive and in reach. */
        private static LivingEntity heldHeart(ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            if (!data.hasUUID(HEART) || data.getLong(HEART_UNTIL) < player.level().getGameTime()) {
                return null;
            }
            Entity entity = player.serverLevel().getEntity(data.getUUID(HEART));
            return entity instanceof LivingEntity living && living.isAlive() && living.distanceToSqr(player) < 64 * 64 ? living
                    : null;
        }
    }

    /** Press both palms to a foe and release a defibrillator's jolt that knocks it out cold. */
    public static final class CounterShock extends Ability {
        CounterShock() {
            super(props(ActivationType.INSTANT, AbilityIcon.BOLT, 0xFFF27A).cooldown(160).stamina(18).mastery(M, 30)
                    .masteryGain(1.0));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.SHOCK, 14f * ctx.power());
                target.addEffect(new MobEffectInstance(GLMobEffects.KNOCKED_OUT, target instanceof Player ? 20 : 50, 0, false, false,
                        true));
                Vfx.beam(ctx.level(), player, ctx.handPos(), target.getBoundingBox().getCenter(), 0xFFF27A, 0.5f, Kind.SPARK,
                        Vfx.BEAM_BOLT);
                Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), 0xFFF27A, 2f, Kind.SPARK, Vfx.FLASH);
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(GLSounds.RUMBLE_ZAP.get(), 1.4f, 1.1f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** A blade of energy that cuts the organs inside a body without touching the skin. */
    public static final class GammaKnife extends Ability {
        GammaKnife() {
            super(props(ActivationType.INSTANT, AbilityIcon.SWORD, 0xB0FFC8).cooldown(500).stamina(30).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsRoom(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Optional<Sphere> sphere = room(player);
            for (LivingEntity target : AbilityDamage.line(player, 10, 0.9)) {
                if (sphere.isPresent() && sphere.get().contains(target)) {
                    Kit.hit(player, target, AbilityDamage.DRAIN, 20f * ctx.power());
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                    Vfx.burst(ctx.level(), target, target.getBoundingBox().getCenter(), 0xB0FFC8, 1.5f, Kind.GLOW, Vfx.FLASH);
                }
            }
            Vfx.beam(ctx.level(), player, ctx.handPos(), AbilityDamage.lookTarget(player, 10), 0xB0FFC8, 0.7f, Kind.GLOW,
                    Vfx.BEAM_GLOW);
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(GLSounds.GLINT_LASER.get(), 1.4f, 0.7f);
        }
    }

    /** Lift every creature in your Room into the air and hold it there, then let it fall. */
    public static final class Takt extends Ability {
        Takt() {
            super(props(ActivationType.INSTANT, AbilityIcon.CROWN, ROOM_COLOR).cooldown(600).stamina(34).masteryGain(1.5));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return needsRoom(player);
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            room(player).ifPresent(sphere -> {
                for (LivingEntity target : AbilityDamage.around(player, sphere.center(), sphere.radius())) {
                    Kit.hit(player, target, AbilityDamage.CRUSH, 8f * ctx.power());
                    Kit.launch(target, new Vec3(0, 1.1, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
                    Vfx.burst(ctx.level(), target, target.position(), ROOM_COLOR, 1.5f, Kind.RING, Vfx.GROUND);
                }
                Vfx.burst(ctx.level(), player, sphere.center(), ROOM_COLOR, (float) sphere.radius(), Kind.RING, Vfx.RING | Vfx.HEAVY);
            });
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1.5f, 0.8f);
        }
    }
}
