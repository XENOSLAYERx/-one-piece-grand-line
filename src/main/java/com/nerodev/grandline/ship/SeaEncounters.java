package com.nerodev.grandline.ship;

import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Ships met at sea. Now and then a player sailing the open ocean sees a ship crewed by islanders come over the
 * horizon: a Marine patrol (likelier the more wanted they are), a pirate raider, or a merchant heavy with cargo. Each
 * sails on its own ({@link ShipCaptain}) and leaves once no one has come near it for a while.
 */
public final class SeaEncounters {
    /** Tag on every islander crewing a ship met at sea. */
    public static final String TAG = "grandline_sea_crew";
    private static final int CHECK_TICKS = 600;
    /** No new ship comes while one crewed by islanders is this close. */
    private static final double CLEAR = 160;

    private SeaEncounters() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            ServerLevel level = event.getServer().overworld();
            if (level.getGameTime() % CHECK_TICKS == 0 && GLServerConfig.seaEncounters()) {
                tick(level);
            }
        });
    }

    private static void tick(ServerLevel level) {
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isSpectator() || !(player.getVehicle() instanceof ShipEntity)
                    || level.random.nextDouble() >= GLServerConfig.seaEncounterChance()
                    || !crewedShipsNear(level, player.position(), CLEAR).isEmpty()) {
                continue;
            }
            launchNear(level, player, pick(player));
        }
    }

    /** Ships crewed by islanders within {@code radius} of {@code at}. */
    public static List<ShipEntity> crewedShipsNear(ServerLevel level, Vec3 at, double radius) {
        return level.getEntitiesOfClass(ShipEntity.class, new AABB(at, at).inflate(radius), ship -> ship.captain() != null);
    }

    /** Marines come looking for wanted sailors; otherwise pirates and merchants are as likely. */
    static Encounter pick(ServerPlayer player) {
        int marine = GLPlayer.data(player).bounty() > 0 ? 6 : 3;
        int pirate = 3;
        int merchant = 2;
        int roll = player.getRandom().nextInt(marine + pirate + merchant);
        return roll < marine ? Encounter.MARINE_PATROL : roll < marine + pirate ? Encounter.PIRATE_RAIDER : Encounter.MERCHANT;
    }

    /**
     * Launches a ship of {@code kind} on open water 60 to 80 blocks from {@code player}, sailing across their way, and
     * warns everyone near.
     *
     * @return the ship, or null if there is no open water around
     */
    @Nullable
    public static ShipEntity launchNear(ServerLevel level, ServerPlayer player, Encounter kind) {
        for (int tries = 0; tries < 24; tries++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double distance = 60 + level.random.nextDouble() * 20;
            Vec3 at = player.position().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            Vec3 aim = player.position().add((level.random.nextDouble() - 0.5) * 40, 0, (level.random.nextDouble() - 0.5) * 40);
            ShipEntity ship = launch(level, kind, at, ShipCaptain.bearing(at, aim));
            if (ship != null) {
                for (ServerPlayer near : level.getPlayers(p -> p.distanceToSqr(at) < CLEAR * CLEAR)) {
                    near.sendSystemMessage(Component.literal(kind.warning()).withStyle(kind.color()));
                    if (kind.hunts()) {
                        near.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.6f, 1.2f);
                    }
                }
                return ship;
            }
        }
        return null;
    }

    /**
     * Puts a ship of {@code kind} on the water at {@code at} heading {@code yaw}, with her crew aboard and her hold
     * stocked.
     *
     * @return the ship, or null if there is no open water there for her
     */
    @Nullable
    public static ShipEntity launch(ServerLevel level, Encounter kind, Vec3 at, float yaw) {
        ResourceLocation type = kind.ship(level.random);
        ShipDefinition def = level.registryAccess().registry(GLRegistries.SHIP).map(registry -> registry.get(type)).orElse(null);
        if (def == null) {
            return null;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, BlockPos.containing(at));
        if (!openWater(level, at, top.getY() - 1, def, yaw)) {
            return null;
        }
        ShipEntity ship = GLShips.SHIP.get().create(level);
        if (ship == null) {
            return null;
        }
        ship.setShipType(type);
        ship.moveTo(at.x, top.getY(), at.z, yaw, 0);
        double surface = ship.waterSurface(def);
        if (Double.isNaN(surface)) {
            return null;
        }
        ship.moveTo(at.x, surface - def.draft(), at.z, yaw, 0);
        level.addFreshEntity(ship);
        ship.commandBy(kind, yaw);
        man(level, ship, kind);
        stock(level, ship, kind);
        return ship;
    }

    /** Puts {@code kind}'s crew aboard, the first at the helm, as many as she has places for. */
    public static void man(ServerLevel level, ShipEntity ship, Encounter kind) {
        ShipDefinition def = ship.definition();
        int places = def == null ? 0 : def.crew();
        for (ResourceLocation id : kind.crew()) {
            if (ship.getPassengers().size() >= places) {
                break;
            }
            NpcEntity sailor = NpcEntity.spawn(level, id, ship.blockPosition().above(2), ship.getYRot(), MobSpawnType.EVENT);
            if (sailor != null) {
                sailor.addTag(TAG);
                if (!sailor.startRiding(ship)) {
                    sailor.discard();
                }
            }
        }
    }

    /** Fills the hold with what ships of {@code kind} carry. */
    public static void stock(ServerLevel level, ShipEntity ship, Encounter kind) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(kind.cargo());
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, ship.position())
                .create(LootContextParamSets.CHEST);
        table.fill(ship.cargo(), params, level.random.nextLong());
    }

    /** Open water at least two blocks deep under the whole hull (and a little around it), with nothing above. */
    private static boolean openWater(ServerLevel level, Vec3 at, int waterY, ShipDefinition def, float yaw) {
        Vec3 forward = ShipEntity.forward(yaw);
        Vec3 port = ShipEntity.port(yaw);
        double halfWidth = def.width() / 2 + 1;
        for (double along = -def.length() / 2 - 2; along <= def.length() / 2 + 2; along += 2) {
            for (double across = -halfWidth; across <= halfWidth; across += halfWidth) {
                Vec3 point = at.add(forward.scale(along)).add(port.scale(across));
                BlockPos column = BlockPos.containing(point.x, waterY, point.z);
                if (!level.hasChunkAt(column) || !level.getFluidState(column).is(FluidTags.WATER)
                        || !level.getFluidState(column.below()).is(FluidTags.WATER) || !level.getBlockState(column.above()).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }
}
