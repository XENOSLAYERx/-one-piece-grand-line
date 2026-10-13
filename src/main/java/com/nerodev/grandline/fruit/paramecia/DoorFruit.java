package com.nerodev.grandline.fruit.paramecia;

import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.ability;
import static com.nerodev.grandline.fruit.paramecia.ParameciaBatchB.props;

import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.definition.GLRarity;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fx.GLEffects;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Doa Doa no Mi. Open a door in anything: walls, floors, people, even the air itself, and step through to the other
 * side or into the space between.
 */
public class DoorFruit extends DevilFruit {
    public static final int COLOR = 0xA87A4A;
    public static final int FRAME = 0xE8C890;
    static final String M = "fruit/grandline:door";

    public static final DeferredHolder<Ability, WallDoor> WALL_DOOR = ability("door_wall_door", WallDoor::new);
    public static final DeferredHolder<Ability, BodyDoor> BODY_DOOR = ability("door_body_door", BodyDoor::new);
    public static final DeferredHolder<Ability, DoorPrison> DOOR_PRISON = ability("door_prison", DoorPrison::new);
    public static final DeferredHolder<Ability, DoorSlam> DOOR_SLAM = ability("door_slam", DoorSlam::new);
    public static final DeferredHolder<Ability, AirDoor> AIR_DOOR = ability("door_air_door", AirDoor::new);
    public static final DeferredHolder<Ability, Gate> GATE = ability("door_gate", Gate::new);
    public static final DeferredHolder<Ability, Scatter> SCATTER = ability("door_scatter", Scatter::new);

    public DoorFruit() {
        super(Properties.of(FruitType.PARAMECIA, GLRarity.RARE, "Doa Doa no Mi", COLOR)
                .abilities(WALL_DOOR, BODY_DOOR, DOOR_PRISON, DOOR_SLAM, AIR_DOOR)
                .awakened(GATE, SCATTER));
    }

    static void init() {}

    static void behaviors() {}

    /** A door frame drawn in the air at {@code at}, facing {@code facing}. */
    static void door(AbilityContext ctx, Vec3 at, Vec3 facing) {
        Vec3 side = new Vec3(-facing.z, 0, facing.x).normalize().scale(0.6);
        Vec3 base = at.add(0, -0.1, 0);
        Vfx.beam(ctx.level(), ctx.player(), base.add(side), base.add(side).add(0, 2.2, 0), FRAME, 0.08f, Kind.SPARK, Vfx.BEAM_GLOW);
        Vfx.beam(ctx.level(), ctx.player(), base.subtract(side), base.subtract(side).add(0, 2.2, 0), FRAME, 0.08f, Kind.SPARK,
                Vfx.BEAM_GLOW);
        Vfx.beam(ctx.level(), ctx.player(), base.add(side).add(0, 2.2, 0), base.subtract(side).add(0, 2.2, 0), FRAME, 0.08f,
                Kind.SPARK, Vfx.BEAM_GLOW);
    }

    /** The farthest free spot (room for {@code entity}) along {@code dir} from {@code from}, up to {@code range}. */
    @Nullable
    static Vec3 farthestFree(Entity entity, Vec3 from, Vec3 dir, double range) {
        for (double d = range; d >= 1; d -= 0.5) {
            Vec3 to = from.add(dir.scale(d));
            AABB box = entity.getBoundingBox().move(to.subtract(entity.position()));
            if (entity.level().noCollision(entity, box)) {
                return to;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Open a door in the wall in front of you and walk straight through it, however thick.
     *
     * @name Door Door
     */
    public static final class WallDoor extends Ability {
        WallDoor() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, COLOR).cooldown(60).stamina(10).masteryGain(0.7));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 dir = ctx.horizontalLook();
            Vec3 to = farthestFree(player, player.position(), dir, 10);
            if (to == null) {
                return;
            }
            Vec3 from = player.position();
            door(ctx, from.add(dir), dir);
            player.teleportTo(to.x, to.y, to.z);
            door(ctx, to.subtract(dir.scale(0.5)), dir);
            Vfx.blink(ctx.level(), player, from, to, FRAME, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.WOODEN_DOOR_OPEN, 1f, 0.9f);
        }
    }

    /** Open a door in a foe's chest and shove: it tumbles out of a door six blocks behind itself. */
    public static final class BodyDoor extends Ability {
        BodyDoor() {
            super(props(ActivationType.INSTANT, AbilityIcon.HAND, FRAME).cooldown(80).stamina(12).masteryGain(0.7));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 4.5).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 4.5).ifPresent(target -> {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                Vec3 dir = ctx.horizontalLook();
                Vec3 to = farthestFree(target, target.position(), dir, 6);
                door(ctx, target.position().subtract(dir.scale(0.4)), dir);
                if (to != null) {
                    target.teleportTo(to.x, to.y, to.z);
                    door(ctx, to, dir);
                }
                Kit.launch(target, dir.scale(0.8).add(0, 0.3, 0));
            });
            ctx.animate(GLEffects.ANIM_PUNCH);
            ctx.sound(SoundEvents.WOODEN_DOOR_CLOSE, 1.2f, 0.7f);
        }
    }

    /** Push a foe through a door into the empty space behind the world: it is gone for three seconds, then falls out. */
    public static final class DoorPrison extends Ability {
        DoorPrison() {
            super(props(ActivationType.INSTANT, AbilityIcon.CAGE, COLOR).cooldown(240).stamina(18).mastery(M, 10)
                    .masteryGain(0.9));
        }

        @Override
        public Optional<Component> checkUse(ServerPlayer player) {
            return Kit.target(player, 6).isPresent() ? Optional.empty() : Optional.of(Component.literal("Nothing within reach"));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Kit.target(player, 6).ifPresent(target -> {
                Kit.encase(target, Vfx.StatusLook.BARRIER, 60);
                target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
                door(ctx, target.position(), ctx.horizontalLook());
            });
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.WOODEN_DOOR_CLOSE, 1.4f, 0.5f);
        }
    }

    /** Throw a door open in front of you so hard it slams everyone ahead backwards. */
    public static final class DoorSlam extends Ability {
        DoorSlam() {
            super(props(ActivationType.INSTANT, AbilityIcon.WALL, FRAME).cooldown(100).stamina(14).mastery(M, 20)
                    .masteryGain(0.8));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.cone(player, 6 * ctx.power(), 45)) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 9f * ctx.power());
                AbilityDamage.knockback(target, player.position(), 1.8, 0.4);
            }
            door(ctx, player.position().add(ctx.horizontalLook().scale(1.5)), ctx.horizontalLook());
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.IRON_DOOR_CLOSE, 1.4f, 0.6f);
        }
    }

    /** Step through a door into the air itself and hide there, unseen and untouchable, for up to five seconds. */
    public static final class AirDoor extends Ability {
        AirDoor() {
            super(props(ActivationType.TOGGLE, AbilityIcon.EYE, FRAME).cooldown(300).stamina(16).staminaPerTick(0.2f)
                    .maxActive(100).mastery(M, 35).masteryGain(1.0));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            door(ctx, player.position(), ctx.horizontalLook());
            for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
            ctx.sound(SoundEvents.WOODEN_DOOR_OPEN, 1f, 0.6f);
        }

        @Override
        public boolean onTick(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 0, false, false));
            player.resetFallDistance();
            return true;
        }

        @Override
        public void onEnd(AbilityContext ctx, EndReason reason) {
            ServerPlayer player = ctx.player();
            player.removeEffect(MobEffects.INVISIBILITY);
            door(ctx, player.position(), ctx.horizontalLook());
            ctx.sound(SoundEvents.WOODEN_DOOR_CLOSE, 1f, 0.8f);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Awakened
    // ---------------------------------------------------------------------------------------------------------

    /** Open a door to anywhere you can see, up to sixty blocks away; whoever stands beside you comes too. */
    public static final class Gate extends Ability {
        Gate() {
            super(props(ActivationType.INSTANT, AbilityIcon.DASH, FRAME).cooldown(400).stamina(28).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            Vec3 aim = Kit.ground(ctx.level(), Kit.aimPoint(player, 60), 10);
            Vec3 from = player.position();
            Vec3 shift = aim.subtract(from);
            door(ctx, from.add(ctx.horizontalLook()), ctx.horizontalLook());
            for (ServerPlayer other : ctx.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(3),
                    o -> o != player)) {
                other.teleportTo(other.getX() + shift.x, other.getY() + shift.y, other.getZ() + shift.z);
            }
            player.teleportTo(aim.x, aim.y, aim.z);
            player.resetFallDistance();
            door(ctx, aim, ctx.horizontalLook());
            Vfx.blink(ctx.level(), player, from, aim, FRAME, Kind.SPARK);
            ctx.animate(GLEffects.ANIM_PALMS);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.8f);
        }
    }

    /** Doors open under every foe around you at once: they drop through and land scattered far apart. */
    public static final class Scatter extends Ability {
        Scatter() {
            super(props(ActivationType.INSTANT, AbilityIcon.BURST, COLOR).cooldown(600).stamina(32).masteryGain(1.5));
        }

        @Override
        public void onActivate(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            for (LivingEntity target : AbilityDamage.around(player, player.position(), 10 * ctx.power())) {
                Kit.hit(player, target, AbilityDamage.STRIKE, 8f * ctx.power());
                double a = target.getRandom().nextDouble() * Math.PI * 2;
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 to = farthestFree(target, target.position().add(0, 3, 0), dir, 10);
                door(ctx, target.position(), dir);
                if (to != null) {
                    target.teleportTo(to.x, to.y, to.z);
                }
            }
            ctx.animate(GLEffects.ANIM_RAISE_ARMS);
            ctx.sound(SoundEvents.IRON_DOOR_OPEN, 2f, 0.5f);
        }
    }
}
