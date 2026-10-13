package com.nerodev.grandline.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One entity type for every ability projectile (fire fists, bullets, fireflies, walls...). The {@code kind}
 * selects a {@link ProjectileBehavior} on the server and a renderer/trail on the client. Size, colour and kind are
 * synced; damage and the rest stay on the server.
 * <p>
 * Collision is a swept sphere of radius {@code size / 2}, so big projectiles hit what they visibly touch.
 */
public class AbilityProjectile extends Projectile {
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(AbilityProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(AbilityProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(AbilityProjectile.class, EntityDataSerializers.INT);

    /** Client trail hook, installed by the client entry point. */
    public static Consumer<AbilityProjectile> clientTick = projectile -> {};

    private float damage;
    private float power = 1f;
    private int lifetime = 40;
    private double gravity;
    private final Set<Integer> alreadyHit = new HashSet<>();
    @Nullable
    private ResourceLocation kindCache;

    public AbilityProjectile(EntityType<? extends AbilityProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Spawns a projectile owned by {@code owner}. */
    public static AbilityProjectile spawn(ServerPlayer owner, ResourceLocation kind, Vec3 pos, Vec3 velocity, float size,
            int color, float damage, int lifetime) {
        ServerLevel level = owner.serverLevel();
        AbilityProjectile projectile = new AbilityProjectile(GLEntities.ABILITY_PROJECTILE.get(), level);
        projectile.setOwner(owner);
        projectile.setPos(pos);
        projectile.setDeltaMovement(velocity);
        projectile.entityData.set(KIND, kind.toString());
        projectile.entityData.set(SIZE, size);
        projectile.entityData.set(COLOR, color);
        projectile.damage = damage;
        projectile.lifetime = lifetime;
        projectile.updateRotation();
        level.addFreshEntity(projectile);
        return projectile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, "");
        builder.define(SIZE, 0.5f);
        builder.define(COLOR, 0xFFFFFF);
    }

    // ---------------------------------------------------------------------------------------------------------

    public ResourceLocation kind() {
        if (kindCache == null) {
            kindCache = ResourceLocation.tryParse(entityData.get(KIND));
            if (kindCache == null) {
                kindCache = ResourceLocation.withDefaultNamespace("missing");
            }
        }
        return kindCache;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (KIND.equals(key)) {
            kindCache = null;
        }
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public void setSize(float size) {
        entityData.set(SIZE, size);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public float damage() {
        return damage;
    }

    public float power() {
        return power;
    }

    public AbilityProjectile withPower(float power) {
        this.power = power;
        return this;
    }

    public AbilityProjectile withGravity(double gravity) {
        this.gravity = gravity;
        return this;
    }

    public int lifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    @Nullable
    public ServerPlayer ownerPlayer() {
        return getOwner() instanceof ServerPlayer player ? player : null;
    }

    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (level().isClientSide) {
            clientTick.accept(this);
        } else if (serverTick(motion)) {
            return;
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (gravity != 0) {
            setDeltaMovement(motion.add(0, -gravity, 0));
        }
        updateRotation();
    }

    /** @return true if the projectile was removed */
    private boolean serverTick(Vec3 motion) {
        ProjectileBehavior behavior = ProjectileBehavior.Registry.get(kind());
        if (behavior == null || ownerPlayer() == null) {
            discard();
            return true;
        }
        behavior.tick(this);
        if (isRemoved()) {
            return true;
        }

        Vec3 from = position();
        Vec3 to = from.add(motion);
        BlockHitResult blockHit = null;
        if (behavior.hitsBlocks() && motion.lengthSqr() > 1.0E-6) {
            BlockHitResult clip = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (clip.getType() != HitResult.Type.MISS) {
                blockHit = clip;
                to = clip.getLocation();
            }
        }

        double radius = Math.max(0.15, size() / 2.0);
        AABB sweep = new AABB(from, to).inflate(radius);
        List<Entity> candidates = level().getEntities(this, sweep, this::canHitEntity);
        candidates.sort((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from)));
        for (Entity target : candidates) {
            if (alreadyHit.contains(target.getId())) {
                continue;
            }
            AABB box = target.getBoundingBox().inflate(radius);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                alreadyHit.add(target.getId());
                if (!behavior.onHitEntity(this, target)) {
                    discard();
                    return true;
                }
            }
        }

        if (blockHit != null) {
            behavior.onHitBlock(this, blockHit);
            discard();
            return true;
        }
        if (tickCount >= lifetime) {
            behavior.onExpire(this);
            discard();
            return true;
        }
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && target.isAlive() && target.isPickable() && !target.isSpectator()
                && !(target instanceof AbilityProjectile);
    }

    @Override
    protected void updateRotation() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6) {
            double horizontal = motion.horizontalDistance();
            setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
            setXRot((float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG));
            yRotO = getYRot();
            xRotO = getXRot();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }
}
