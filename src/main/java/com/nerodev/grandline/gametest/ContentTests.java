package com.nerodev.grandline.gametest;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.arena.ArenaData;
import com.nerodev.grandline.arena.Arenas;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.creator.Battles;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.worldevent.WorldEventType;
import com.nerodev.grandline.worldevent.WorldEvents;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 16: battle waves of islanders and bosses, world events, and arenas fought round by round. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class ContentTests {
    private static final String ARENA = "arena";

    private ContentTests() {}

    @GameTest(template = ARENA)
    public static void legendaryWavesBringABoss(GameTestHelper helper) {
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        int spawned = Battles.spawnWave(player, Battles.Difficulty.LEGENDARY);
        helper.assertTrue(spawned == Battles.Difficulty.LEGENDARY.size(), "The whole legendary wave spawns, got " + spawned);
        AABB area = new AABB(player.blockPosition()).inflate(24);
        List<BossEntity> bosses = helper.getLevel().getEntitiesOfClass(BossEntity.class, area, boss -> boss.getTags().contains(Battles.TAG));
        helper.assertTrue(bosses.size() == 1 && bosses.getFirst().getTarget() == player, "A boss leads it, already after the player");
        List<NpcEntity> crew = helper.getLevel().getEntitiesOfClass(NpcEntity.class, area, npc -> npc.getTags().contains(Battles.TAG));
        helper.assertTrue(crew.size() == Battles.Difficulty.LEGENDARY.wave().size() && crew.stream().allMatch(npc -> npc.getTarget() == player),
                "Its islanders are all after the player");
        // Practice is practice: beating a battle-test boss pays nothing and drops nothing.
        BossEntity boss = bosses.getFirst();
        boss.hurt(player.damageSources().playerAttack(player), 100_000f);
        helper.assertTrue(!boss.isAlive(), "The boss falls");
        helper.assertTrue(GLPlayer.data(player).bounty() == 0, "No bounty for a practice boss");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, boss.getBoundingBox().inflate(4)).isEmpty(),
                "and no loot");
        NpcEntity pirate = crew.getFirst();
        pirate.hurt(player.damageSources().playerAttack(player), 100_000f);
        helper.assertTrue(GLPlayer.data(player).reputation(Factions.CIVILIANS) == 0 && pirate.faction().map(f ->
                GLPlayer.data(player).reputation(f) == 0).orElse(true), "nor standing for practice kills");
        Battles.clear(player);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "world_events")
    public static void raidsAreBeatenForAReward(GameTestHelper helper) {
        Runnable release = NpcTests.keepLoaded(helper);
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        WorldEvents.Active raid = WorldEvents.start(player, WorldEventType.PIRATE_RAID);
        helper.assertTrue(raid != null, "Pirates find somewhere to land");
        List<LivingEntity> raiders = raid.raiders();
        helper.assertTrue(raiders.size() == WorldEventType.PIRATE_RAID.band().size(), "The whole band lands: " + raiders.size());
        helper.assertTrue(raiders.stream().allMatch(raider -> raider.getTags().contains(WorldEvents.TAG)), "Raiders are marked as raiders");
        helper.assertTrue(WorldEvents.of(player) == raid, "The player is caught up in it");
        for (LivingEntity raider : raiders) {
            raider.hurt(player.damageSources().playerAttack(player), 100_000f);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(!WorldEvents.active().contains(raid), "With every raider down the raid is over");
            helper.assertTrue(player.getInventory().countItem(Items.EMERALD) >= WorldEventType.PIRATE_RAID.emeralds(),
                    "and the player is paid: " + player.getInventory().countItem(Items.EMERALD));
            release.run();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "world_events_stop")
    public static void stoppedRaidsWithdraw(GameTestHelper helper) {
        Runnable release = NpcTests.keepLoaded(helper);
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        WorldEvents.Active raid = WorldEvents.start(player, WorldEventType.BOUNTY_HUNTERS);
        helper.assertTrue(raid != null, "Hunters find somewhere to land");
        List<LivingEntity> raiders = raid.raiders();
        helper.assertTrue(WorldEvents.stopAll() >= 1, "Stopping ends the event");
        helper.assertTrue(raiders.stream().allMatch(Entity::isRemoved), "and the raiders leave");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == 0, "with no reward");
        release.run();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "arena")
    public static void arenasAreFoughtRoundByRound(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Runnable release = NpcTests.keepLoaded(helper);
        BlockPos centre = helper.absolutePos(new BlockPos(8, 2, 8));
        String name = "test_" + Math.abs(centre.hashCode() % 100000);
        helper.assertTrue(Arenas.create(server, name, helper.getLevel(), centre, 7), "The arena is marked out");
        helper.assertTrue(!Arenas.create(server, name, helper.getLevel(), centre, 7), "Names are unique");
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        helper.assertTrue(Arenas.join(player, ArenaData.get(server).byName(name).orElseThrow()) == null,
                "The player joins");
        Arenas.Session session = Arenas.of(player);
        helper.assertTrue(session != null && !session.fighting() && session.round() == 0, "A breather before the first round");
        Arenas.startRound(server, session);
        List<LivingEntity> enemies = session.enemies();
        helper.assertTrue(session.round() == 1 && enemies.size() == Battles.Difficulty.EASY.size(), "Round 1 sends in an easy wave: "
                + enemies.size());
        helper.assertTrue(enemies.stream().allMatch(enemy -> enemy.getTags().contains(Arenas.TAG)), "Arena enemies are marked");
        helper.assertTrue(Arenas.difficulty(9) == Battles.Difficulty.LEGENDARY && Arenas.difficulty(3) == Battles.Difficulty.NORMAL,
                "Rounds get harder");
        for (LivingEntity enemy : enemies) {
            enemy.hurt(player.damageSources().playerAttack(player), 100_000f);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(!session.fighting(), "With the wave down the round is cleared");
            helper.assertTrue(player.getInventory().countItem(Items.EMERALD) >= 1, "and the fighters are paid");
            Arenas.leave(player);
            helper.assertTrue(Arenas.of(player) == null && Arenas.session(name) == null, "The last one out ends the fight");
            Arenas.delete(server, name);
            release.run();
        });
    }

    @GameTest(template = ARENA)
    public static void contentCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player)
                .withPosition(at);
        String name = "cmd_" + Math.abs(BlockPos.containing(at).hashCode() % 100000);
        NpcTests.run(helper, server, source, "onepiece event list");
        NpcTests.run(helper, server, source, "onepiece event stop");
        NpcTests.run(helper, server, source, "onepiece arena create " + name + " 8");
        NpcTests.run(helper, server, source, "onepiece arena list");
        NpcTests.run(helper, server, source, "onepiece arena join " + name);
        helper.assertTrue(Arenas.of(player) != null, "arena join");
        NpcTests.run(helper, server, source, "onepiece arena next");
        helper.assertTrue(Arenas.of(player).round() == 1, "arena next sends in a round");
        NpcTests.run(helper, server, source, "onepiece arena reset " + name);
        helper.assertTrue(Arenas.of(player) == null, "arena reset ends the fight");
        NpcTests.run(helper, server, source, "onepiece arena delete " + name);
        NpcTests.run(helper, server, source, "onepiece boss spawn random");
        List<BossEntity> bosses = helper.getLevel().getEntitiesOfClass(BossEntity.class, new AABB(BlockPos.containing(at)).inflate(10));
        helper.assertTrue(bosses.size() == 1, "boss spawn random");
        bosses.forEach(Entity::discard);
        helper.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(at)).inflate(16),
                entity -> entity.getTags().contains(Battles.TAG)).forEach(Entity::discard);
        helper.succeed();
    }
}
