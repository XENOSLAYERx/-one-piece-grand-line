package com.nerodev.grandline.npc;

import com.nerodev.grandline.weapon.GLWeapons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A musket ball fired by an islander. It flies nearly flat, passes through the shooter's allies and hurts what it hits. */
public class NpcShot extends ThrowableItemProjectile {
    private float damage = 4f;

    public NpcShot(EntityType<? extends NpcShot> type, Level level) {
        super(type, level);
    }

    public NpcShot(Level level, LivingEntity shooter, float damage) {
        super(GLNpcs.NPC_SHOT.get(), shooter, level);
        this.damage = damage;
    }

    public float damage() {
        return damage;
    }

    @Override
    protected Item getDefaultItem() {
        return GLWeapons.IRON_BULLET.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.008;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 60) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) {
            return false;
        }
        if (getOwner() instanceof NpcEntity shooter && target instanceof NpcEntity other) {
            return shooter.faction().isEmpty() || !shooter.faction().equals(other.faction());
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity owner = getOwner();
        hit.getEntity().hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), damage);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 4, 0.05, 0.05, 0.05, 0.01);
            discard();
        }
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
