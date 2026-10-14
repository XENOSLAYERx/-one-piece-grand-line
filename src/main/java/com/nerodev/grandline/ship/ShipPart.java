package com.nerodev.grandline.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * One segment of a ship's hull, bow to stern. Segments are what you stand on, bump into, hit and click: a long hull
 * at any heading is covered by a row of square boxes, since a single entity box cannot turn with the ship.
 */
public class ShipPart extends PartEntity<ShipEntity> {
    private final int index;
    private EntityDimensions size = EntityDimensions.fixed(1f, 1f);
    private boolean active;

    public ShipPart(ShipEntity ship, int index) {
        super(ship);
        this.index = index;
        this.noPhysics = true;
    }

    public int index() {
        return index;
    }

    public boolean active() {
        return active;
    }

    /** Sizes the segment (or switches it off when the ship has fewer segments). */
    void configure(boolean active, float width, float height) {
        this.active = active;
        this.size = EntityDimensions.fixed(active ? width : 0.1f, active ? height : 0.1f);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return active;
    }

    @Override
    public boolean canBeCollidedWith() {
        return active;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return active && !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return active ? getParent().interact(player, hand) : InteractionResult.PASS;
    }

    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
