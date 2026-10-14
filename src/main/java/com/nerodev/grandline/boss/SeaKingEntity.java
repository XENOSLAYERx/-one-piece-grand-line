package com.nerodev.grandline.boss;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * A Sea King: a boss in the body of a great sea serpent. It swims (and only swims: stranded on land it slowly dries
 * out), surfaces to bite and lash its tail, spits jets of water, rams ships and dives to burst up beneath its prey.
 */
public class SeaKingEntity extends BossEntity {
    public SeaKingEntity(EntityType<? extends SeaKingEntity> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.08f, 0.1f, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        setPathfindingMalus(PathType.WATER, 0f);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new BossGoals.Fight(this));
        goalSelector.addGoal(3, new BossGoals.GoHome(this));
        goalSelector.addGoal(6, new RandomSwimmingGoal(this, 0.8, 60));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::hostileTo));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // A Sea King out of the water slowly dries out.
        if (!level().isClientSide && isAlive() && !isInWaterOrBubble() && tickCount % 20 == 0) {
            hurt(damageSources().dryOut(), 4f);
        }
    }
}
