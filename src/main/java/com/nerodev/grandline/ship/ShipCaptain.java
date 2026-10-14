package com.nerodev.grandline.ship;

import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.Reaction;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Commands a ship crewed by islanders. It keeps its course and steers clear of land; a fighting crew runs down what
 * it is hostile to (a wanted player, a ship of an enemy flag), turns its broadside toward its quarry at cannon range
 * and fires when the guns bear; a merchant runs from whoever attacks it. With its crew gone the captain is gone too,
 * and the ship is anyone's for the taking.
 */
public final class ShipCaptain {
    /** How far the lookout sees. */
    static final double SIGHT = 56;
    /** The range a captain likes to fight at. */
    static final double RANGE = 18;
    /** Ticks a merchant keeps running once attacked. */
    static final int FLEE_TICKS = 600;
    /** Ticks with no player within {@link #COMPANY} before the ship sails off for good. */
    static final int LONELY_TICKS = 1200;
    static final double COMPANY = 160;

    /** What the helm is told this tick, as a helmsman's keys would give it. */
    record Helm(float forward, float turn) {}

    private final ShipEntity ship;
    private final Encounter encounter;
    private float course;
    @Nullable
    private Entity target;
    private long fleeUntil;
    @Nullable
    private Vec3 fleeFrom;
    private float detour;
    private int lonely;

    ShipCaptain(ShipEntity ship, Encounter encounter, float course) {
        this.ship = ship;
        this.encounter = encounter;
        this.course = course;
    }

    public Encounter encounter() {
        return encounter;
    }

    /** What the ship is after, if anything. */
    @Nullable
    public Entity target() {
        return target;
    }

    public void setTarget(@Nullable Entity target) {
        this.target = target;
    }

    /** The islanders aboard, the helmsman first. */
    public List<NpcEntity> crew() {
        return ship.getPassengers().stream().filter(NpcEntity.class::isInstance).map(NpcEntity.class::cast).filter(LivingEntity::isAlive)
                .toList();
    }

    /** Who gives the orders and answers for the guns: the helmsman, or whoever is left. */
    @Nullable
    public NpcEntity officer() {
        List<NpcEntity> crew = crew();
        return crew.isEmpty() ? null : crew.get(0);
    }

    /** Whether the sea has emptied around the ship long enough for it to sail off. */
    boolean lonely() {
        return lonely >= LONELY_TICKS;
    }

    /** The ship was struck: a fighting crew turns on the attacker, a merchant runs from it. */
    void attackedBy(@Nullable Entity attacker) {
        Entity culprit = attacker instanceof ShipPart part ? part.getParent() : attacker;
        if (culprit == null || culprit == ship) {
            return;
        }
        if (culprit instanceof ShipEntity other && other.helmsman() instanceof Player player) {
            culprit = player;
        }
        if (!encounter.hunts()) {
            fleeUntil = ship.level().getGameTime() + FLEE_TICKS;
            fleeFrom = culprit.position();
        } else if (target == null || !target.isAlive()) {
            target = culprit;
        }
        if (culprit instanceof LivingEntity living) {
            crew().forEach(npc -> npc.provoke(living));
        }
    }

    /** Whether the crew would attack {@code player} on sight. */
    public boolean hostileTo(Player player) {
        if (!encounter.hunts() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        Reaction reaction = Factions.reaction(player, encounter.faction());
        return encounter == Encounter.PIRATE_RAIDER ? reaction != Reaction.FRIENDLY : reaction == Reaction.HOSTILE;
    }

    /** Whether the crew fights {@code other}'s crew on sight. */
    boolean hostileTo(ShipEntity other) {
        ShipCaptain them = other.captain();
        return them != null && encounter.hunts()
                && Factions.enemies(ship.level().registryAccess(), encounter.faction(), them.encounter.faction());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Each tick
    // ---------------------------------------------------------------------------------------------------------

    Helm steer(ShipDefinition def) {
        ServerLevel level = (ServerLevel) ship.level();
        if (ship.tickCount % 20 == 0) {
            lookout(level);
        }
        if (ship.tickCount % 10 == 0) {
            detour = 0;
        }
        Vec3 pos = ship.position();
        float yaw = ship.getYRot();
        float desired = course;
        float throttle = 0.6f;
        if (fleeUntil > level.getGameTime() && fleeFrom != null) {
            desired = bearing(fleeFrom, pos);
            throttle = 1f;
        } else if (target != null) {
            Vec3 at = target.position();
            double distance = Math.sqrt(horizontalDistanceSqr(pos, at));
            float toTarget = bearing(pos, at);
            if (distance > RANGE + 12) {
                desired = toTarget;
                throttle = 1f;
            } else {
                // Lay the ship across the line to the quarry, on whichever tack needs the smaller turn, edging in or
                // out to hold the range.
                float right = toTarget - 90;
                float left = toTarget + 90;
                boolean quarryToStarboard = Math.abs(Mth.wrapDegrees(right - yaw)) <= Math.abs(Mth.wrapDegrees(left - yaw));
                desired = quarryToStarboard ? right : left;
                float away = distance < RANGE - 6 ? 25 : distance > RANGE + 4 ? -25 : 0;
                desired += quarryToStarboard ? -away : away;
                throttle = 0.55f;
                fire(def, at, distance);
            }
        } else if (ship.tickCount % 400 == 0) {
            course += (ship.getRandom().nextFloat() - 0.5f) * 40;
        }
        if (ship.tickCount % 10 == 0 || detour != 0) {
            detour = detour(def, desired);
        }
        desired += detour;
        if (target == null && detour != 0) {
            course = desired;
        }
        float diff = Mth.wrapDegrees(desired - yaw);
        return new Helm(Math.abs(diff) > 100 ? throttle * 0.5f : throttle, -Mth.clamp(diff / 12f, -1f, 1f));
    }

    private void lookout(ServerLevel level) {
        if (level.getNearestPlayer(ship.getX(), ship.getY(), ship.getZ(), COMPANY, false) == null) {
            lonely += 20;
        } else {
            lonely = 0;
        }
        if (target != null && (!target.isAlive() || target.isRemoved() || target.distanceToSqr(ship) > SIGHT * SIGHT * 2
                || target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            target = null;
        }
        if (target != null || !encounter.hunts()) {
            return;
        }
        Optional<ServerPlayer> prey = level.getPlayers(player -> player.distanceToSqr(ship) < SIGHT * SIGHT && hostileTo(player)).stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(ship)));
        if (prey.isPresent()) {
            target = prey.get();
            return;
        }
        target = level.getEntitiesOfClass(ShipEntity.class, ship.getBoundingBox().inflate(SIGHT), other -> other != ship && hostileTo(other))
                .stream().min(Comparator.comparingDouble(other -> other.distanceToSqr(ship))).orElse(null);
    }

    /** Fires whichever broadside bears on {@code at}, if its guns are loaded. */
    private void fire(ShipDefinition def, Vec3 at, double distance) {
        NpcEntity gunner = officer();
        if (gunner == null || def.cannons() <= 0 || distance < 6 || distance > RANGE + 16) {
            return;
        }
        Vec3 toTarget = at.subtract(ship.position()).multiply(1, 0, 1).normalize();
        if (Math.abs(toTarget.dot(ShipEntity.forward(ship.getYRot()))) > 0.45) {
            return;
        }
        int side = toTarget.dot(ShipEntity.port(ship.getYRot())) >= 0 ? 1 : -1;
        if (ship.level().getGameTime() >= ship.readyAt(side)) {
            ship.fireAt(gunner, side, at.add(0, 0.6, 0));
        }
    }

    /** How far to turn off {@code heading} to keep clear of land ahead (0 when the way is open). */
    private float detour(ShipDefinition def, float heading) {
        if (clear(def, heading)) {
            return 0;
        }
        for (int step = 30; step <= 180; step += 30) {
            if (clear(def, heading + step)) {
                return step;
            }
            if (clear(def, heading - step)) {
                return -step;
            }
        }
        return 180;
    }

    /** Whether there is open water along {@code heading} for a few lengths ahead. */
    private boolean clear(ShipDefinition def, float heading) {
        Level level = ship.level();
        Vec3 dir = ShipEntity.forward(heading);
        double waterline = ship.getY() + def.draft();
        for (double d = def.length() / 2 + 2; d <= def.length() / 2 + 12; d += 3) {
            BlockPos at = BlockPos.containing(ship.getX() + dir.x * d, waterline - 0.5, ship.getZ() + dir.z * d);
            if (!level.isLoaded(at) || !level.getFluidState(at).is(FluidTags.WATER)
                    || !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The heading (yaw) from {@code from} toward {@code to}. */
    static float bearing(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Saving
    // ---------------------------------------------------------------------------------------------------------

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Encounter", encounter.getSerializedName());
        tag.putFloat("Course", course);
        return tag;
    }

    @Nullable
    static ShipCaptain load(ShipEntity ship, CompoundTag tag) {
        Encounter encounter = Encounter.byName(tag.getString("Encounter"));
        return encounter == null ? null : new ShipCaptain(ship, encounter, tag.getFloat("Course"));
    }

    /** The faction whose colours the crew sails under, empty for townsfolk. */
    public Optional<ResourceLocation> faction() {
        return encounter.faction();
    }
}
