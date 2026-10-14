package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.ship.Encounter;
import com.nerodev.grandline.ship.GLShips;
import com.nerodev.grandline.ship.SeaEncounters;
import com.nerodev.grandline.ship.ShipCaptain;
import com.nerodev.grandline.ship.ShipDefinition;
import com.nerodev.grandline.ship.ShipEntity;
import com.nerodev.grandline.ship.ShipItem;
import com.nerodev.grandline.ship.ShipShot;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 10: ships sail, shoot, take damage, sink, get repaired and carry a crew. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class ShipTests {
    private static final String POOL = "ship_pool";

    private ShipTests() {}

    @GameTest(template = POOL, timeoutTicks = 100)
    public static void shipsSailWhenSteered(GameTestHelper helper) {
        ShipEntity ship = launch(helper, "raft");
        ServerPlayer captain = sailor(helper);
        helper.assertTrue(captain.startRiding(ship), "The captain boards");
        helper.assertTrue(ship.helmsman() == captain, "The first aboard takes the helm");
        Vec3 start = ship.position();
        double rest = ship.getY();
        captain.zza = 1f;
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(ship.position().subtract(start).horizontalDistance() > 0.4,
                    "Full sail moves the ship, moved " + ship.position().subtract(start).horizontalDistance());
            helper.assertTrue(Math.abs(ship.getY() - rest) < 0.5, "The ship stays afloat at its draft");
            helper.assertTrue(ship.speed() > 0, "The ship has way on");
            captain.stopRiding();
            ship.discard();
            helper.succeed();
        });
    }

    @GameTest(template = POOL)
    public static void hullsBreakAndSpillTheirCargo(GameTestHelper helper) {
        ShipEntity ship = launch(helper, "sloop");
        ship.cargo().addItem(new ItemStack(Items.COBBLESTONE, 5));
        float full = ship.health();
        ship.hurt(helper.getLevel().damageSources().explosion(null, null), 50f);
        helper.assertTrue(Math.abs(ship.health() - (full - 50f)) < 0.01f, "An explosion takes 50 off the hull, now " + ship.health());
        ship.hurt(helper.getLevel().damageSources().explosion(null, null), 10_000f);
        helper.assertTrue(ship.isRemoved(), "The ship sinks at zero");
        List<ItemEntity> spilled = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(ship.blockPosition()).inflate(8),
                item -> item.getItem().is(Items.COBBLESTONE));
        helper.assertTrue(!spilled.isEmpty(), "The cargo spills out of the wreck");
        spilled.forEach(ItemEntity::discard);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(ship.blockPosition()).inflate(8)).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void cannonsFireAndReload(GameTestHelper helper) {
        ShipEntity ship = launch(helper, "sloop");
        ServerPlayer gunner = sailor(helper);
        helper.assertTrue(gunner.startRiding(ship), "The gunner boards");
        ship.cargo().addItem(new ItemStack(GLShips.CANNONBALL.get(), 4));
        gunner.setYRot(-90f);
        gunner.setXRot(0f);
        int fired = ship.fire(gunner);
        helper.assertTrue(fired == 1, "The sloop fires its one port cannon, fired " + fired);
        helper.assertTrue(ship.cargo().countItem(GLShips.CANNONBALL.get()) == 3, "One cannonball used from the hold");
        List<AbilityProjectile> shots = helper.getLevel().getEntitiesOfClass(AbilityProjectile.class,
                new AABB(ship.blockPosition()).inflate(10), p -> GLShips.CANNONBALL_SHOT.equals(p.kind()));
        helper.assertTrue(shots.size() == 1, "One cannonball in flight, found " + shots.size());
        helper.assertTrue(ship.fire(gunner) == 0, "The port cannons need to reload");
        shots.forEach(AbilityProjectile::discard);
        gunner.stopRiding();
        ship.discard();
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void planksRepairAndWaterPutsOutFires(GameTestHelper helper) {
        ShipEntity ship = launch(helper, "sloop");
        ServerPlayer carpenter = newPlayer(helper);
        ship.setHealth(50f);
        carpenter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
        ship.interact(carpenter, InteractionHand.MAIN_HAND);
        helper.assertTrue(ship.health() > 50f, "Planks repair the hull, now " + ship.health());
        helper.assertTrue(carpenter.getMainHandItem().getCount() == 3, "One plank used");
        ship.setBurning(200);
        carpenter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        ship.interact(carpenter, InteractionHand.MAIN_HAND);
        helper.assertTrue(ship.burning() == 0, "A bucket of water puts out the fire");
        ship.discard();
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void crewPlacesAreLimited(GameTestHelper helper) {
        ShipEntity raft = launch(helper, "raft");
        ServerPlayer first = sailor(helper);
        ServerPlayer second = sailor(helper);
        ServerPlayer third = sailor(helper);
        helper.assertTrue(raft.interact(first, InteractionHand.MAIN_HAND).consumesAction(), "Right-click boards");
        helper.assertTrue(first.getVehicle() == raft, "The first is aboard");
        helper.assertTrue(second.startRiding(raft), "A raft takes two");
        helper.assertFalse(third.startRiding(raft), "A third does not fit on a raft");
        first.stopRiding();
        second.stopRiding();
        raft.discard();
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void shipsGoBackInTheirBottle(GameTestHelper helper) {
        ShipEntity ship = launch(helper, "sloop");
        ServerPlayer sailor = newPlayer(helper);
        sailor.setShiftKeyDown(true);
        sailor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        ship.interact(sailor, InteractionHand.MAIN_HAND);
        helper.assertTrue(ship.isRemoved(), "The ship is bottled");
        boolean found = false;
        for (int i = 0; i < sailor.getInventory().getContainerSize(); i++) {
            ItemStack stack = sailor.getInventory().getItem(i);
            if (stack.is(GLShips.SHIP_ITEM.get()) && ShipItem.type(stack).equals(GrandLine.id("sloop"))) {
                found = true;
            }
        }
        helper.assertTrue(found, "The sailor carries the sloop in a bottle");
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void coupDeBurstNeedsTheRightShip(GameTestHelper helper) {
        ShipEntity sunny = launch(helper, "thousand_sunny");
        ServerPlayer helm = sailor(helper);
        helper.assertTrue(helm.startRiding(sunny), "The helmsman boards the Sunny");
        helper.assertTrue(sunny.burst(helm), "The Thousand Sunny bursts");
        helper.assertFalse(sunny.burst(helm), "The cola tanks need time to refill");
        helm.stopRiding();
        sunny.discard();
        ShipEntity raft = launch(helper, "raft");
        ServerPlayer rafter = sailor(helper);
        helper.assertTrue(rafter.startRiding(raft), "The rafter boards");
        helper.assertFalse(raft.burst(rafter), "A raft has no Coup de Burst");
        rafter.stopRiding();
        raft.discard();
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void shipCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(6, 3, 6));
        player.setPos(at.x, at.y, at.z);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player).withPosition(at);
        run(helper, server, source, "onepiece ship list");
        run(helper, server, source, "onepiece ship give grandline:galleon");
        helper.assertTrue(player.getInventory().countItem(GLShips.SHIP_ITEM.get()) == 1, "ship give");
        run(helper, server, source, "onepiece ship spawn grandline:raft");
        List<ShipEntity> ships = helper.getLevel().getEntitiesOfClass(ShipEntity.class, new AABB(player.blockPosition()).inflate(12));
        helper.assertTrue(!ships.isEmpty(), "ship spawn");
        ships.get(0).setHealth(5f);
        run(helper, server, source, "onepiece ship repair");
        helper.assertTrue(ships.get(0).health() == ships.get(0).maxHealth(), "ship repair");
        run(helper, server, source, "onepiece ship remove");
        helper.assertTrue(ships.get(0).isRemoved(), "ship remove");
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void crewedShipsFireOnWantedSailors(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ShipEntity ship = launch(helper, "sloop");
        ship.commandBy(Encounter.MARINE_PATROL, 0);
        SeaEncounters.man(level, ship, Encounter.MARINE_PATROL);
        SeaEncounters.stock(level, ship, Encounter.MARINE_PATROL);
        ShipCaptain captain = ship.captain();
        helper.assertTrue(captain != null && captain.crew().size() == 4, "Four Marines crew the sloop: "
                + (captain == null ? 0 : captain.crew().size()));
        helper.assertTrue(!ship.cargo().isEmpty(), "Her hold is stocked");

        ServerPlayer outlaw = sailor(helper);
        GLPlayer.data(outlaw).setBounty(30_000_000L);
        ServerPlayer honest = sailor(helper);
        helper.assertTrue(captain.hostileTo(outlaw), "Marines go after a wanted pirate");
        helper.assertFalse(captain.hostileTo(honest), "and leave honest sailors be");

        Vec3 abeam = ship.position().add(ShipEntity.port(ship.getYRot()).scale(12));
        helper.assertTrue(ship.fireAt(captain.officer(), 1, abeam) == 1, "The port gun fires");
        List<ShipShot> shots = level.getEntitiesOfClass(ShipShot.class, new AABB(ship.blockPosition()).inflate(12));
        helper.assertTrue(shots.size() == 1, "One cannonball in flight, found " + shots.size());
        helper.assertTrue(ship.fireAt(captain.officer(), 1, abeam) == 0, "and needs to reload");
        shots.forEach(ShipShot::discard);
        captain.crew().forEach(NpcEntity::discard);
        ship.discard();
        helper.succeed();
    }

    @GameTest(template = POOL)
    public static void crewlessShipsCanBeTaken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ShipEntity ship = launch(helper, "sloop");
        ship.commandBy(Encounter.PIRATE_RAIDER, 0);
        SeaEncounters.man(level, ship, Encounter.PIRATE_RAIDER);
        helper.assertTrue(ship.crewFlag().isPresent(), "Pirates fly the Jolly Roger");
        ServerPlayer boarder = sailor(helper);
        ship.interact(boarder, InteractionHand.MAIN_HAND);
        helper.assertFalse(boarder.isPassenger(), "Her crew keeps boarders off");
        ShipCaptain captain = ship.captain();
        helper.assertTrue(captain != null, "Pirates command her");
        captain.crew().forEach(NpcEntity::discard);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(ship.captain() == null, "With her crew gone, so is her captain");
            ship.interact(boarder, InteractionHand.MAIN_HAND);
            helper.assertTrue(boarder.getVehicle() == ship, "and anyone may take her helm");
            boarder.stopRiding();
            ship.discard();
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------------------------------------------------

    static ShipEntity launch(GameTestHelper helper, String type) {
        Vec3 at = helper.absoluteVec(new Vec3(6, 3, 6));
        ShipEntity ship = GLShips.SHIP.get().create(helper.getLevel());
        ship.setShipType(GrandLine.id(type));
        ship.moveTo(at.x, at.y, at.z, 0, 0);
        ShipDefinition def = ship.definition();
        helper.assertTrue(def != null, "Ship type " + type + " is defined");
        double surface = ship.waterSurface(def);
        helper.assertTrue(!Double.isNaN(surface), "The pool has water under the ship");
        ship.moveTo(at.x, surface - def.draft(), at.z, 0, 0);
        helper.getLevel().addFreshEntity(ship);
        return ship;
    }

    private static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }

    /**
     * A test player that can go aboard. NeoForge's fake players refuse to ride anything, so this is a plain server
     * player borrowing a fake player's (silent) connection; like a fake player it is never ticked by the server.
     */
    static ServerPlayer sailor(GameTestHelper helper) {
        ServerPlayer fake = newPlayer(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer sailor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "gl_sailor"),
                ClientInformation.createDefault()) {
            @Override
            public void tick() {}

            @Override
            public boolean isFakePlayer() {
                return true;
            }

            @Override
            public void displayClientMessage(Component message, boolean actionBar) {}

            @Override
            public void awardStat(Stat<?> stat, int amount) {}
        };
        sailor.connection = fake.connection;
        Vec3 at = helper.absoluteVec(new Vec3(6, 3, 6));
        sailor.moveTo(at.x, at.y, at.z, 0, 0);
        return sailor;
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }
}
