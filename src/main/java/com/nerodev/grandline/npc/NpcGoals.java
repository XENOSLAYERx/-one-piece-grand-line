package com.nerodev.grandline.npc;

import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.weapon.GunItem;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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
            if (npc.getTarget() != null || npc.isPassenger() || npc.crew().isPresent() || npc.quarry() != null) {
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

    /**
     * A recruit keeps close to their captain: walks after them, catches up by slipping through when left far
     * behind, and goes aboard (or ashore) with them.
     */
    static final class FollowCaptain extends Goal {
        private static final double NEAR = 4;
        private static final double FAR = 32;
        private final NpcEntity npc;
        private ServerPlayer captain;
        private int repath;

        FollowCaptain(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.crew().isEmpty() || npc.getTarget() != null) {
                return false;
            }
            captain = Crews.captainOf(npc);
            if (captain == null) {
                return false;
            }
            followAboard();
            return !npc.isPassenger() && npc.distanceToSqr(captain) > NEAR * NEAR;
        }

        @Override
        public boolean canContinueToUse() {
            return captain != null && captain.isAlive() && npc.getTarget() == null && !npc.isPassenger()
                    && npc.distanceToSqr(captain) > NEAR * NEAR * 0.5;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            npc.getLookControl().setLookAt(captain, 10f, npc.getMaxHeadXRot());
            if (--repath > 0) {
                return;
            }
            repath = 10;
            if (npc.distanceToSqr(captain) > FAR * FAR) {
                catchUp();
            } else {
                npc.getNavigation().moveTo(captain, 1.15);
            }
        }

        /** Boards the captain's ship, or steps ashore when the captain does. */
        private void followAboard() {
            Entity vehicle = captain.getVehicle();
            if (vehicle instanceof ShipEntity ship && npc.getVehicle() != ship && npc.distanceToSqr(ship) < 18 * 18) {
                npc.startRiding(ship);
            } else if (npc.getVehicle() instanceof ShipEntity ship && captain.getVehicle() != ship) {
                npc.stopRiding();
            }
        }

        /** Appears beside the captain on the first free footing, as pets do. */
        private void catchUp() {
            BlockPos around = captain.blockPosition();
            for (int i = 0; i < 10; i++) {
                int x = around.getX() + npc.getRandom().nextInt(7) - 3;
                int z = around.getZ() + npc.getRandom().nextInt(7) - 3;
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = new BlockPos(x, around.getY() + dy, z);
                    if (Math.abs(x - around.getX()) < 2 && Math.abs(z - around.getZ()) < 2) {
                        continue;
                    }
                    if (npc.level().getBlockState(pos.below()).isFaceSturdy(npc.level(), pos.below(), Direction.UP)
                            && npc.level().noCollision(npc, npc.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(npc.position())))) {
                        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, npc.getYRot(), npc.getXRot());
                        npc.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }

    /** A hunter on a trail closes in on their quarry until they are close enough to see and fight. */
    static final class Track extends Goal {
        private final NpcEntity npc;
        private ServerPlayer quarry;
        private int repath;

        Track(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.quarry() == null || npc.getTarget() != null) {
                return false;
            }
            quarry = npc.quarryPlayer();
            return quarry != null && npc.distanceToSqr(quarry) > 10 * 10;
        }

        @Override
        public boolean canContinueToUse() {
            return quarry != null && quarry.isAlive() && npc.getTarget() == null && npc.distanceToSqr(quarry) > 6 * 6;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath <= 0) {
                repath = 20;
                npc.getNavigation().moveTo(quarry, 1.1);
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
