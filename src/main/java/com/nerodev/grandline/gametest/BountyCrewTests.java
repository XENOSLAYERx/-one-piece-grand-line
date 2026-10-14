package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.bounty.Bounties;
import com.nerodev.grandline.bounty.BountyHunts;
import com.nerodev.grandline.crew.Crew;
import com.nerodev.grandline.crew.CrewData;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.crew.JollyRoger;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.ship.ShipEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 13: crimes raise bounties, bounties draw hunters, and crews form, sail, recruit and last. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class BountyCrewTests {
    private static final String ARENA = "arena";

    private BountyCrewTests() {}

    // ---------------------------------------------------------------------------------------------------------
    // Bounty
    // ---------------------------------------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void crimesRaiseBounties(GameTestHelper helper) {
        ServerPlayer outlaw = newPlayer(helper);
        NpcEntity marine = npc(helper, "marine_recruit", 4, 4);
        marine.hurt(helper.getLevel().damageSources().playerAttack(outlaw), 1f);
        long afterAssault = GLPlayer.data(outlaw).bounty();
        helper.assertTrue(afterAssault == Bounties.MARINE_ASSAULT, "Striking a Marine is a crime, bounty " + afterAssault);
        marine.hurt(helper.getLevel().damageSources().playerAttack(outlaw), 1f);
        helper.assertTrue(GLPlayer.data(outlaw).bounty() == afterAssault, "Once a fight is on, blows are not counted again");
        marine.hurt(helper.getLevel().damageSources().playerAttack(outlaw), 1000f);
        helper.assertTrue(GLPlayer.data(outlaw).bounty() >= afterAssault + Bounties.MARINE_KILL, "Killing a Marine raises a bounty");

        ServerPlayer marineMember = newPlayer(helper);
        GLPlayer.edit(marineMember, data -> data.setFaction(Optional.of(Factions.MARINES)));
        npc(helper, "pirate_grunt", 10, 10).hurt(helper.getLevel().damageSources().playerAttack(marineMember), 1000f);
        helper.assertTrue(GLPlayer.data(marineMember).bounty() == 0, "Marines earn no bounty for doing their job");
        npc(helper, "villager", 12, 4).hurt(helper.getLevel().damageSources().playerAttack(marineMember), 1000f);
        helper.assertTrue(GLPlayer.data(marineMember).bounty() >= Bounties.CIVILIAN_KILL, "...but murder makes even a Marine wanted");
        clear(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerKillsPayOrRaise(GameTestHelper helper) {
        ServerPlayer wanted = newPlayer(helper);
        GLPlayer.edit(wanted, data -> data.setBounty(40_000_000L));
        ServerPlayer marine = newPlayer(helper);
        GLPlayer.edit(marine, data -> data.setFaction(Optional.of(Factions.MARINES)));
        Bounties.onPlayerKill(marine, wanted);
        helper.assertTrue(marine.getInventory().countItem(Items.EMERALD) == 40, "A Marine collects the reward, "
                + marine.getInventory().countItem(Items.EMERALD) + " emeralds");
        ServerPlayer pirate = newPlayer(helper);
        Bounties.onPlayerKill(pirate, wanted);
        helper.assertTrue(GLPlayer.data(pirate).bounty() == 10_000_000L, "A pirate takes on a quarter of the bounty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void biggerBountiesDrawBiggerHunts(GameTestHelper helper) {
        helper.assertTrue(BountyHunts.tier(0) == null && BountyHunts.tier(999_999) == null, "Small fry are left alone");
        BountyHunts.Tier first = BountyHunts.tier(1_000_000L);
        BountyHunts.Tier big = BountyHunts.tier(60_000_000L);
        BountyHunts.Tier huge = BountyHunts.tier(300_000_000L);
        helper.assertTrue(first != null && big != null && huge != null, "Wanted players draw hunters");
        helper.assertTrue(first.hunters().size() < big.hunters().size() && big.hunters().contains(GrandLine.id("bounty_hunter_veteran")),
                "Bigger bounties draw veterans");
        helper.assertTrue(huge.marines().contains(GrandLine.id("marine_captain")), "The biggest draw a Marine captain");
        var random = helper.getLevel().getRandom();
        long small = 0;
        long large = 0;
        for (int i = 0; i < 50; i++) {
            small += BountyHunts.interval(2_000_000L, random);
            large += BountyHunts.interval(500_000_000L, random);
        }
        helper.assertTrue(large < small, "Big bounties are hunted more often");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "bounty_hunt")
    public static void huntersComeForTheWanted(GameTestHelper helper) {
        ServerPlayer wanted = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(0.5, 1, 0.5));
        wanted.moveTo(at.x, at.y, at.z);
        GLPlayer.edit(wanted, data -> data.setBounty(60_000_000L));
        // Hunters only arrive on loaded ground, so load the land around the test first.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                helper.getLevel().getChunk(((int) at.x >> 4) + dx, ((int) at.z >> 4) + dz);
            }
        }
        List<NpcEntity> party = BountyHunts.hunt(wanted);
        helper.assertTrue(party.size() >= 3, "A party of hunters arrives, " + party.size());
        for (NpcEntity hunter : party) {
            helper.assertTrue(wanted.getUUID().equals(hunter.quarry()) && hunter.hostileTo(wanted) && !hunter.hasRestriction(),
                    "Every hunter is on the wanted player's trail");
            double distance = hunter.distanceTo(wanted);
            helper.assertTrue(distance > 15 && distance < 40, "Hunters arrive out of sight, " + (int) distance + " blocks away");
        }
        party.forEach(NpcEntity::discard);
        GLPlayer.edit(wanted, data -> data.setBounty(0));
        helper.assertTrue(BountyHunts.hunt(wanted).isEmpty(), "Nobody hunts an honest player");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Crews
    // ---------------------------------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void crewsFormAndBreakUp(GameTestHelper helper) {
        ServerPlayer captain = newPlayer(helper);
        ServerPlayer mate = newPlayer(helper);
        String name = "Test Crew " + captain.getUUID().toString().substring(0, 6);
        helper.assertTrue(Crews.create(captain, name) == null, "A crew is founded");
        Crew crew = Crews.of(captain).orElseThrow();
        helper.assertTrue(crew.isCaptain(captain.getUUID()) && GLPlayer.data(captain).crew().equals(Optional.of(crew.id())),
                "The founder is captain, and their player data knows the crew");
        helper.assertTrue(Crews.create(mate, name) != null, "Crew names are unique");
        helper.assertTrue(Crews.accept(mate) != null, "Nobody joins uninvited");
        helper.assertTrue(Crews.invite(mate, captain) != null, "Only a captain invites");
        helper.assertTrue(Crews.invite(captain, mate) == null && Crews.accept(mate) == null, "Invited and aboard");
        helper.assertTrue(crew.has(mate.getUUID()) && Crews.crewmates(captain, mate), "Crewmates");
        helper.assertTrue(Crews.promote(captain, mate.getUUID()) == null && crew.isCaptain(mate.getUUID()), "The captaincy changes hands");
        helper.assertTrue(Crews.leave(mate) == null && crew.isCaptain(captain.getUUID()), "When the captain leaves, the next takes over");
        helper.assertTrue(GLPlayer.data(mate).crew().isEmpty(), "Leaving clears the player's crew");
        helper.assertTrue(Crews.leave(captain) == null && CrewData.get(helper.getLevel().getServer()).byName(name).isEmpty(),
                "The last one out disbands the crew");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crewsAreSaved(GameTestHelper helper) {
        ServerPlayer captain = newPlayer(helper);
        String name = "Saved Crew " + captain.getUUID().toString().substring(0, 6);
        Crews.create(captain, name);
        Crews.setFlag(captain, JollyRoger.SHARK, 0x2244AA);
        Crews.addRenown(captain, 42);
        Crew crew = Crews.of(captain).orElseThrow();
        Tag saved = Crew.CODEC.encodeStart(NbtOps.INSTANCE, crew).getOrThrow();
        Crew loaded = Crew.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.id().equals(crew.id()) && loaded.name().equals(name) && loaded.captain().equals(captain.getUUID())
                        && loaded.jollyRoger() == JollyRoger.SHARK && loaded.color() == 0x2244AA && loaded.renown() == 42
                        && loaded.members().equals(crew.members()), "A crew comes back from disk as it was saved");
        Tag data = PlayerData.CODEC.encodeStart(NbtOps.INSTANCE, GLPlayer.data(captain)).getOrThrow();
        helper.assertTrue(PlayerData.CODEC.parse(NbtOps.INSTANCE, data).getOrThrow().crew().equals(Optional.of(crew.id())),
                "A player's crew is saved with them");
        Crews.disband(captain);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void recruitsJoinAndCrewmatesHoldFire(GameTestHelper helper) {
        ServerPlayer captain = newPlayer(helper);
        Crews.create(captain, "Recruiters " + captain.getUUID().toString().substring(0, 6));
        NpcEntity pirate = npc(helper, "pirate_grunt", 6, 6);
        helper.assertTrue(Crews.recruit(captain, pirate) != null, "Recruits must be paid");
        captain.getInventory().add(new ItemStack(Items.EMERALD, 64));
        int cost = Crews.recruitCost(pirate);
        helper.assertTrue(Crews.recruit(captain, pirate) == null, "Paid, the pirate joins the crew");
        helper.assertTrue(captain.getInventory().countItem(Items.EMERALD) == 64 - cost, "The fee is taken");
        helper.assertTrue(pirate.crew().equals(Crews.of(captain).map(Crew::id)) && !pirate.hasRestriction(), "A recruit follows the crew");
        GLPlayer.edit(captain, data -> data.setReputation(GrandLine.id("pirate"), -500));
        helper.assertFalse(pirate.hostileTo(captain), "A recruit never turns on their captain");
        float health = pirate.getHealth();
        pirate.hurt(helper.getLevel().damageSources().playerAttack(captain), 5f);
        helper.assertTrue(pirate.getHealth() == health, "Crewmates' blows glance off");
        NpcEntity marine = npc(helper, "marine_recruit", 10, 10);
        helper.assertFalse(Crews.recruit(captain, marine) == null, "Marines don't desert to pirates");
        NpcEntity outsider = npc(helper, "pirate_grunt", 12, 4);
        float before = outsider.getHealth();
        outsider.hurt(helper.getLevel().damageSources().playerAttack(captain), 5f);
        helper.assertTrue(outsider.getHealth() < before, "...but anyone else can be hurt");
        helper.assertTrue(Crews.dismiss(captain) == null && pirate.crew().isEmpty(), "Dismissed recruits go home");
        Crews.disband(captain);
        clear(helper);
        helper.succeed();
    }

    @GameTest(template = "ship_pool")
    public static void claimedShipsFlyTheCrewsColours(GameTestHelper helper) {
        ServerPlayer captain = ShipTests.sailor(helper);
        ServerPlayer stranger = ShipTests.sailor(helper);
        Crews.create(captain, "Sailors " + captain.getUUID().toString().substring(0, 6));
        ShipEntity sloop = ShipTests.launch(helper, "sloop");
        helper.assertTrue(Crews.claimShip(captain) != null, "Board before claiming");
        helper.assertTrue(captain.startRiding(sloop), "The captain boards");
        helper.assertTrue(Crews.claimShip(captain) == null, "The captain claims the ship");
        helper.assertTrue(sloop.crew().equals(Crews.of(captain).map(Crew::id)) && sloop.crewFlag().equals(Optional.of(JollyRoger.CLASSIC)),
                "The ship flies the crew's Jolly Roger");
        Crews.setFlag(captain, JollyRoger.STRAW_HAT, 0xFFCC00);
        helper.assertTrue(sloop.crewFlag().equals(Optional.of(JollyRoger.STRAW_HAT)), "A new Jolly Roger goes up the mast");
        captain.stopRiding();
        InteractionResult boarding = sloop.interact(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(boarding == InteractionResult.FAIL && stranger.getVehicle() != sloop, "Strangers can't take another crew's helm");
        Crews.disband(captain);
        helper.assertTrue(sloop.crewFlag().isEmpty(), "A disbanded crew's flag comes down");
        sloop.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void crewCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player)
                .withPosition(at);
        String name = "Cmd " + player.getUUID().toString().substring(0, 6);
        run(helper, server, source, "onepiece crew create " + name);
        helper.assertTrue(Crews.of(player).map(Crew::name).equals(Optional.of(name)), "crew create");
        run(helper, server, source, "onepiece crew flag heart #FFDD00");
        helper.assertTrue(Crews.of(player).map(Crew::jollyRoger).equals(Optional.of(JollyRoger.HEART)), "crew flag");
        run(helper, server, source, "onepiece crew info");
        run(helper, server, source, "onepiece crew list");
        run(helper, server, source, "onepiece bounty set 2000000");
        run(helper, server, source, "onepiece crew leave");
        helper.assertTrue(Crews.of(player).isEmpty(), "crew leave");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static NpcEntity npc(GameTestHelper helper, String id, int x, int z) {
        NpcEntity npc = NpcEntity.spawn(helper.getLevel(), GrandLine.id(id), helper.absolutePos(new BlockPos(x, 2, z)), 0f,
                MobSpawnType.COMMAND);
        helper.assertTrue(npc != null, "Spawned " + id);
        return npc;
    }

    private static void clear(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24);
        helper.getLevel().getEntitiesOfClass(NpcEntity.class, area).forEach(NpcEntity::discard);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
    }

    private static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }
}
