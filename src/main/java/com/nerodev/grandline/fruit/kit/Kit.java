package com.nerodev.grandline.fruit.kit;

import com.nerodev.grandline.ability.AbilityContext;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Gameplay building blocks for Devil Fruit moves: aiming, movement, status, block shapes and projectiles. Moves
 * combine these with their own logic and look; nothing here decides what a fruit does.
 */
public final class Kit {
    private Kit() {}

    // ---------------------------------------------------------------------------------------------------------
    // Aiming
    // ---------------------------------------------------------------------------------------------------------

    /** Where the player is looking, up to {@code range}: the first block or harmable entity hit. */
    public static Vec3 aimPoint(ServerPlayer player, double range) {
        Vec3 block = AbilityDamage.lookTarget(player, range);
        Optional<LivingEntity> target = AbilityDamage.raycast(player, range, 0.3);
        if (target.isPresent()) {
            Vec3 at = target.get().getBoundingBox().getCenter();
            if (at.distanceToSqr(player.getEyePosition()) < block.distanceToSqr(player.getEyePosition())) {
                return at;
            }
        }
        return block;
    }

    /** The harmable creature under the crosshair, if any. */
    public static Optional<LivingEntity> target(ServerPlayer player, double range) {
        return AbilityDamage.raycast(player, range, 0.4);
    }

    /** The ground (top of the first solid block) at or below {@code at}, searching {@code depth} blocks. */
    public static Vec3 ground(ServerLevel level, Vec3 at, int depth) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
        for (int i = 0; i <= depth; i++) {
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return new Vec3(at.x, pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y), at.z);
            }
            pos.move(0, -1, 0);
        }
        return at;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Movement
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Teleports the player toward where they look (stopping before walls), with a vanish/appear effect.
     *
     * @return the distance travelled (0 if there was nowhere to go)
     */
    public static double blink(ServerPlayer player, double range, int color, Kind kind) {
        Vec3 from = player.position();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = AbilityDamage.lookTarget(player, range);
        double distance = Math.max(0, end.distanceTo(eye) - 0.8);
        Vec3 dest = null;
        for (double d = distance; d > 0.5; d -= 0.5) {
            Vec3 candidate = from.add(look.scale(d));
            AABB box = player.getBoundingBox().move(candidate.subtract(from));
            if (player.level().noCollision(player, box)) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) {
            return 0;
        }
        player.teleportTo(dest.x, dest.y, dest.z);
        player.resetFallDistance();
        Vfx.blink(player.serverLevel(), player, from, dest, color, kind);
        return from.distanceTo(dest);
    }

    /** Launches the player along their look direction. */
    public static void dash(ServerPlayer player, double speed, double lift) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * speed, Math.max(lift, look.y * speed + lift * 0.5), look.z * speed);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    /** Pulls {@code entity} toward {@code to} with the given speed (blocks per tick), lifting it a little. */
    public static void pull(Entity entity, Vec3 to, double speed) {
        Vec3 delta = to.subtract(entity.position());
        double length = delta.length();
        if (length < 0.2) {
            return;
        }
        Vec3 motion = delta.scale(Math.min(speed, length * 0.5) / length);
        entity.setDeltaMovement(motion.x, Math.max(motion.y, 0.15), motion.z);
        entity.hurtMarked = true;
    }

    /**
     * Lets a survival player fly (or stops it). Creative and spectator players are left alone. Remember to switch
     * it off when the ability ends.
     */
    public static void setFlight(ServerPlayer player, boolean on) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.getAbilities().mayfly = on;
        if (!on) {
            player.getAbilities().flying = false;
        } else {
            player.getAbilities().flying = true;
        }
        player.onUpdateAbilities();
        player.resetFallDistance();
    }

    /** Replaces an entity's motion. */
    public static void launch(Entity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Damage and status
    // ---------------------------------------------------------------------------------------------------------

    /** Ability damage that ignores the usual hurt cooldown (for multi-hit moves). @return true if it landed */
    public static boolean hit(ServerPlayer attacker, LivingEntity target, ResourceKey<DamageType> type, float amount) {
        target.invulnerableTime = 0;
        return AbilityDamage.hurt(attacker, target, type, amount, attacker);
    }

    /** Seals {@code target} (frozen, petrified, waxed...) for {@code ticks}: it can neither move nor act. */
    public static void encase(LivingEntity target, Vfx.StatusLook look, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.ENCASED, ticks, 0, false, false, true));
        target.setDeltaMovement(Vec3.ZERO);
        if (target.level() instanceof ServerLevel level) {
            Vfx.status(level, target, look, ticks);
        }
    }

    /** Binds {@code target}'s legs for {@code ticks}: it cannot move but can still fight. */
    public static void root(LivingEntity target, Vfx.StatusLook look, int ticks) {
        target.addEffect(new MobEffectInstance(GLMobEffects.ROOTED, ticks, 0, false, false, true));
        if (target.level() instanceof ServerLevel level) {
            Vfx.status(level, target, look, ticks);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Block shapes (temporary)
    // ---------------------------------------------------------------------------------------------------------

    /** Places {@code state} at every position for {@code ticks}. @return how many were placed */
    public static int place(ServerLevel level, Iterable<BlockPos> positions, BlockState state, int ticks) {
        int placed = 0;
        for (BlockPos pos : positions) {
            if (TempBlocks.place(level, pos, state, ticks)) {
                placed++;
            }
        }
        return placed;
    }

    /** Places {@code state} at every position for {@code ticks}; players cannot break these (barriers). */
    public static int placeUnbreakable(ServerLevel level, Iterable<BlockPos> positions, BlockState state, int ticks) {
        int placed = 0;
        for (BlockPos pos : positions) {
            if (TempBlocks.place(level, pos, state, ticks, true)) {
                placed++;
            }
        }
        return placed;
    }

    /** A flat wall {@code width} wide and {@code height} tall centred on {@code center}, facing {@code facing}. */
    public static List<BlockPos> wall(Vec3 center, Vec3 facing, int width, int height) {
        Vec3 flat = new Vec3(facing.x, 0, facing.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        List<BlockPos> out = new ArrayList<>();
        for (int w = -width / 2; w <= width / 2; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = BlockPos.containing(center.add(side.scale(w)).add(0, h, 0));
                if (!out.contains(pos)) {
                    out.add(pos);
                }
            }
        }
        return out;
    }

    /** A filled disc of radius {@code radius} at the height of {@code center}. */
    public static List<BlockPos> disc(BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius + radius) {
                    out.add(center.offset(x, 0, z));
                }
            }
        }
        return out;
    }

    /** A ring (one block thick) of radius {@code radius}, {@code height} tall. */
    public static List<BlockPos> ring(BlockPos center, int radius, int height) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (Math.abs(d - radius) < 0.6) {
                    for (int y = 0; y < height; y++) {
                        out.add(center.offset(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    /** A hollow dome (upper half-sphere shell) of radius {@code radius}. */
    public static List<BlockPos> dome(BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (Math.abs(d - radius) < 0.55) {
                        out.add(center.offset(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    /** A 1x1 column {@code height} tall. */
    public static List<BlockPos> pillar(BlockPos base, int height) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            out.add(base.above(y));
        }
        return out;
    }

    /** A box cage around a creature: walls and a roof, leaving its own space free. */
    public static List<BlockPos> cage(LivingEntity target) {
        BlockPos base = target.blockPosition();
        int half = Math.max(1, Mth.ceil(target.getBbWidth() / 2));
        int height = Math.max(2, Mth.ceil(target.getBbHeight())) + 1;
        List<BlockPos> out = new ArrayList<>();
        for (int x = -half; x <= half; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = -half; z <= half; z++) {
                    boolean edge = Math.abs(x) == half || Math.abs(z) == half || y == height;
                    if (edge) {
                        out.add(base.offset(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Projectiles
    // ---------------------------------------------------------------------------------------------------------

    /** Fires a projectile of {@code kind} from the user's hand along their look. */
    public static AbilityProjectile shoot(AbilityContext ctx, ResourceLocation kind, double speed, float size, int color,
            float damage, int lifetime) {
        ServerPlayer player = ctx.player();
        Vec3 velocity = player.getLookAngle().scale(speed);
        return AbilityProjectile.spawn(player, kind, ctx.handPos(), velocity, size, color, damage * ctx.power(), lifetime)
                .withPower(ctx.power());
    }

    /** Removes the owner's projectiles/zones of {@code kind} (when the toggle that made them ends). */
    public static void discard(ServerPlayer owner, ResourceLocation kind) {
        for (AbilityProjectile p : owner.level().getEntitiesOfClass(AbilityProjectile.class,
                owner.getBoundingBox().inflate(96), p -> p.getOwner() == owner && kind.equals(p.kind()))) {
            p.discard();
        }
    }

    /** Points spread evenly on a horizontal circle (for volleys and rings of spikes). */
    public static List<Vec3> circle(Vec3 center, double radius, int count, double phase) {
        List<Vec3> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = phase + Mth.TWO_PI * i / count;
            out.add(center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
        }
        return out;
    }
}
