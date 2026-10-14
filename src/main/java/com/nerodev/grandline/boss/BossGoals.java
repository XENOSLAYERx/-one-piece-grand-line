package com.nerodev.grandline.boss;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** How a boss fights and how it goes back to its post once a fight is over. */
final class BossGoals {
    /** How far from home a boss will chase before it lets its prey go. */
    static final double LEASH = 40;

    private BossGoals() {}

    /**
     * The fight: between moves the boss closes in, or circles its prey while it catches its breath; whenever a move's
     * range and cooldown allow, it starts one (picked by the phase's weights). Moves steer the boss themselves.
     */
    static final class Fight extends Goal {
        private final BossEntity boss;
        private int repath;
        private int strafeTicks;
        private float strafeSide = 1;

        Fight(BossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return boss.inMove() || inReach(boss.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean inReach(LivingEntity target) {
            if (target == null || !target.isAlive()) {
                return false;
            }
            if (!boss.hasRestriction()) {
                return true;
            }
            BlockPos home = boss.getRestrictCenter();
            return target.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) < LEASH * LEASH;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            boss.getNavigation().stop();
            if (!inReach(boss.getTarget())) {
                boss.setTarget(null);
            }
        }

        @Override
        public void tick() {
            if (boss.inMove()) {
                return;
            }
            LivingEntity target = boss.getTarget();
            if (target == null) {
                return;
            }
            boss.getLookControl().setLookAt(target, 30f, 30f);
            double distance = boss.distanceTo(target);
            BossMove next = boss.pickMove(distance);
            if (next != null) {
                boss.getNavigation().stop();
                boss.startMove(next, target);
                return;
            }
            float pace = boss.pace();
            if (boss.resting() && distance < 5) {
                // Catching its breath up close: circle the prey instead of standing still to be hit.
                if (--strafeTicks <= 0) {
                    strafeTicks = 20 + boss.getRandom().nextInt(30);
                    strafeSide = boss.getRandom().nextBoolean() ? 1 : -1;
                }
                boss.getNavigation().stop();
                boss.getMoveControl().strafe(distance < 2.5 ? -0.4f : 0.1f, 0.45f * strafeSide);
                return;
            }
            if (--repath <= 0 || boss.getNavigation().isDone()) {
                repath = 8 + boss.getRandom().nextInt(6);
                if (distance > 2) {
                    boss.getNavigation().moveTo(target, 1.0 * pace);
                } else {
                    boss.getNavigation().stop();
                }
            }
        }
    }

    /** With no one left to fight, the boss walks back to its post. */
    static final class GoHome extends Goal {
        private final BossEntity boss;

        GoHome(BossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (boss.getTarget() != null || boss.inMove() || !boss.hasRestriction()) {
                return false;
            }
            BlockPos home = boss.getRestrictCenter();
            return boss.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 9;
        }

        @Override
        public void start() {
            BlockPos home = boss.getRestrictCenter();
            boss.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
        }

        @Override
        public boolean canContinueToUse() {
            return boss.getTarget() == null && !boss.inMove() && !boss.getNavigation().isDone();
        }
    }
}
