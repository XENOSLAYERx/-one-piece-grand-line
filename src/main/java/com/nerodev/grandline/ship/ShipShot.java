package com.nerodev.grandline.ship;

import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A cannonball fired by a ship crewed by islanders (a Marine patrol, a pirate raider). Like a player's it flies in an
 * arc and bursts on whatever it meets, hurting everything near and every hull it lands against, but never its own
 * ship or crew. Its owner is the crew member who gave the order, so a sunk ship's crew answers for it.
 */
public class ShipShot extends ThrowableItemProjectile {
    private static final double RADIUS = 2.6;

    private float damage = 14f;
    private int shipId = -1;

    public ShipShot(EntityType<? extends ShipShot> type, Level level) {
        super(type, level);
    }

    public ShipShot(Level level, LivingEntity gunner, ShipEntity ship, float damage) {
        super(GLShips.SHIP_SHOT.get(), gunner, level);
        this.damage = damage;
        this.shipId = ship.getId();
    }

    @Override
    protected Item getDefaultItem() {
        return GLShips.CANNONBALL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.025;
    }

    @Nullable
    private ShipEntity ownShip() {
        return level().getEntity(shipId) instanceof ShipEntity ship ? ship : null;
    }

    /** True for the ship that fired, its parts and everyone aboard. */
    private boolean ours(Entity entity) {
        ShipEntity ship = ownShip();
        if (ship == null) {
            return false;
        }
        Entity root = entity instanceof ShipPart part ? part.getParent() : entity;
        return root == ship || root.getVehicle() == ship;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !ours(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        // Into the sea, or against another ship's hull: it bursts there.
        if (isInWater() || tickCount > 100 || hitsHull()) {
            burst();
        }
    }

    private boolean hitsHull() {
        for (ShipEntity ship : level().getEntitiesOfClass(ShipEntity.class, getBoundingBox().inflate(24))) {
            if (ship == ownShip()) {
                continue;
            }
            for (var part : ship.getParts()) {
                if (part instanceof ShipPart segment && segment.active() && segment.getBoundingBox().inflate(0.3).intersects(getBoundingBox())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // The burst does the harm.
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            burst();
        }
    }

    private void burst() {
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        Vec3 at = position();
        Entity owner = getOwner();
        DamageSource source = damageSources().explosion(this, owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RADIUS),
                target -> target.isAlive() && !ours(target) && target != owner && target.distanceToSqr(at) <= RADIUS * RADIUS)) {
            target.invulnerableTime = 0;
            target.hurt(source, damage * 0.7f);
            Vec3 push = target.position().subtract(at).multiply(1, 0, 1);
            if (push.lengthSqr() > 1.0E-4) {
                push = push.normalize().scale(0.8);
                target.push(push.x, 0.3, push.z);
            }
        }
        ShipEntity mine = ownShip();
        for (ShipEntity ship : level.getEntitiesOfClass(ShipEntity.class, new AABB(at, at).inflate(30))) {
            if (ship == mine) {
                continue;
            }
            for (var part : ship.getParts()) {
                if (part instanceof ShipPart segment && segment.active() && segment.getBoundingBox().inflate(RADIUS).contains(at)) {
                    ship.hurt(source, damage);
                    if (level.random.nextFloat() < 0.2f) {
                        ship.setBurning(Math.max(ship.burning(), 160));
                    }
                    break;
                }
            }
        }
        boolean wet = level.getFluidState(BlockPos.containing(at)).is(FluidTags.WATER);
        Vfx.burst(level, null, at, wet ? 0xC8E8F8 : 0xF0A040, (float) RADIUS, wet ? Kind.DROP : Kind.FLAME, Vfx.FLASH | Vfx.HEAVY);
        Vfx.burst(level, null, at, 0x606060, (float) RADIUS * 0.8f, Kind.SMOKE, 0);
        level.playSound(null, at.x, at.y, at.z, wet ? SoundEvents.GENERIC_SPLASH : SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.4f, wet ? 0.6f : 0.9f);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }
}
