package com.nerodev.grandline.npc;

import com.nerodev.grandline.weapon.GunItem;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** The islanders' own goals: walking a beat, fighting with a gun, and standing still to trade. */
final class NpcGoals {
    private NpcGoals() {}

    /** A soldier's beat: six points around home, walked in turn, with a pause at each. */
    static final class Patrol extends Goal {
        private final NpcEntity npc;
        private int point;
        private int pause;
        private BlockPos target;

        Patrol(NpcEntity npc) {
            this.npc = npc;
            this.point = npc.getRandom().nextInt(6);
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.getTarget() != null || npc.isVehicle()) {
                return false;
            }
            if (pause > 0) {
                pause--;
                return false;
            }
            for (int tries = 0; tries < 6; tries++) {
                BlockPos next = npc.beat(point);
                point = (point + 1) % 6;
                if (npc.level().getFluidState(next.below()).isEmpty() && npc.level().getFluidState(next).isEmpty()) {
                    target = next;
                    return true;
                }
            }
            pause = 100;
            return false;
        }

        @Override
        public void start() {
            npc.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.65);
        }

        @Override
        public boolean canContinueToUse() {
            return npc.getTarget() == null && !npc.getNavigation().isDone();
        }

        @Override
        public void stop() {
            pause = 40 + npc.getRandom().nextInt(60);
        }
    }

    /**
     * Fighting with a gun: keep at range, close in when out of sight, back off when rushed, and fire whenever the
     * gun is loaded and the target is in the open.
     */
    static final class Shoot extends Goal {
        private static final double FAR = 18;
        private static final double NEAR = 5;
        private final NpcEntity npc;
        private int reload = 20;
        private int seeTime;

        Shoot(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = npc.getTarget();
            return target != null && target.isAlive() && npc.getMainHandItem().getItem() instanceof GunItem;
        }

        @Override
        public void stop() {
            npc.setAiming(false);
            npc.setAggressive(false);
            npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = npc.getTarget();
            if (target == null || !(npc.getMainHandItem().getItem() instanceof GunItem gun)) {
                return;
            }
            npc.setAggressive(true);
            double distance = npc.distanceTo(target);
            boolean sees = npc.getSensing().hasLineOfSight(target);
            seeTime = sees ? seeTime + 1 : 0;
            npc.getLookControl().setLookAt(target, 30f, 30f);
            if (distance > FAR || !sees) {
                npc.getNavigation().moveTo(target, 1.0);
            } else if (distance < NEAR) {
                npc.getNavigation().stop();
                npc.getMoveControl().strafe(-0.6f, npc.getRandom().nextBoolean() ? 0.3f : -0.3f);
            } else {
                npc.getNavigation().stop();
            }
            npc.setAiming(sees && distance <= FAR + 4);
            if (reload > 0) {
                reload--;
            } else if (sees && seeTime > 8 && distance <= FAR + 4) {
                npc.shoot(target, gun);
                reload = gun.reload() * 2 + npc.getRandom().nextInt(20);
            }
        }
    }

    /** A merchant stands still and faces their customer while the trade window is open. */
    static final class Trade extends Goal {
        private final NpcEntity npc;

        Trade(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Player customer = npc.getTradingPlayer();
            if (customer == null) {
                return false;
            }
            if (!customer.isAlive() || customer.distanceToSqr(npc) > 64 || customer.containerMenu == customer.inventoryMenu) {
                npc.setTradingPlayer(null);
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player customer = npc.getTradingPlayer();
            if (customer != null) {
                npc.getLookControl().setLookAt(customer);
            }
        }
    }
}
