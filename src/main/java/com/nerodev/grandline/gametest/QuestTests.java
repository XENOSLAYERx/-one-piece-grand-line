package com.nerodev.grandline.gametest;

import com.mojang.serialization.JsonOps;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.boss.GLBosses;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.quest.GLQuests;
import com.nerodev.grandline.quest.Journey;
import com.nerodev.grandline.quest.LogPose;
import com.nerodev.grandline.quest.QuestDefinition;
import com.nerodev.grandline.quest.QuestObjective;
import com.nerodev.grandline.quest.Quests;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.world.island.IslandType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 15: a quest chain that moves itself on, quests offered by islanders, and the Log Pose's records. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class QuestTests {
    private static final String ARENA = "arena";
    private static final ResourceLocation ROMANCE_DAWN = GrandLine.id("romance_dawn");
    private static final ResourceLocation LAND_HO = GrandLine.id("land_ho");
    private static final ResourceLocation MARINE_BASE = GrandLine.id("the_marine_base");
    private static final ResourceLocation ARLONG_PARK = GrandLine.id("arlong_park");
    private static final ResourceLocation BAR_SUPPLIES = GrandLine.id("partys_bar_supplies");

    private QuestTests() {}

    @GameTest(template = "empty")
    public static void questsAreWellFormed(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        List<Map.Entry<ResourceLocation, QuestDefinition>> quests = Quests.all(access);
        long main = quests.stream().filter(e -> e.getValue().category() == QuestDefinition.Category.MAIN).count();
        helper.assertTrue(main >= 5 && quests.size() - main >= 5, "A story chain and side quests: " + main + " story, " + (quests.size() - main)
                + " side");
        ResourceLocation previous = null;
        for (Map.Entry<ResourceLocation, QuestDefinition> e : quests) {
            QuestDefinition quest = e.getValue();
            String id = e.getKey().toString();
            helper.assertTrue(!quest.objectives().isEmpty(), id + " asks for something");
            if (quest.category() == QuestDefinition.Category.MAIN) {
                helper.assertTrue(quest.startsByItself(), id + ": story quests start by themselves");
                helper.assertTrue(previous == null ? quest.requires().isEmpty() : quest.requires().contains(previous),
                        id + " follows the story quest before it");
                previous = e.getKey();
            } else {
                helper.assertTrue(quest.giver().isPresent() && access.registryOrThrow(GLRegistries.NPC).containsKey(quest.giver().get()),
                        id + " is offered by a real islander");
            }
            for (ResourceLocation before : quest.requires()) {
                helper.assertTrue(Quests.get(access, before) != null, id + " requires a real quest: " + before);
            }
            for (QuestObjective objective : quest.objectives()) {
                objective.target().ifPresent(target -> helper.assertTrue(switch (objective.kind()) {
                    case DEFEAT_NPC, TALK -> access.registryOrThrow(GLRegistries.NPC).containsKey(target);
                    case DEFEAT_BOSS -> access.registryOrThrow(GLRegistries.BOSS).containsKey(target);
                    case DEFEAT_FACTION -> access.registryOrThrow(GLRegistries.FACTION).containsKey(target);
                    case DISCOVER -> IslandType.byId(target) != null;
                    case COLLECT -> BuiltInRegistries.ITEM.containsKey(target);
                    default -> true;
                }, id + " names a real " + objective.kind().getSerializedName() + " target: " + target));
                helper.assertTrue(!Quests.describe(access, objective).getString().isBlank(), id + " objectives can be described");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theStoryMovesItselfOn(GameTestHelper helper) {
        ServerPlayer player = NpcTests.newPlayer(helper);
        Quests.startDue(player);
        Journey journey = GLPlayer.data(player).journey();
        helper.assertTrue(journey.isActive(ROMANCE_DAWN) && journey.activeQuests().size() == 1, "A new player begins with Romance Dawn: "
                + journey.activeQuests());
        helper.assertTrue(journey.tracked().equals(Optional.of(ROMANCE_DAWN)), "and it is on screen");
        QuestDefinition dawn = Quests.get(helper.getLevel().registryAccess(), ROMANCE_DAWN);
        Quests.complete(player, ROMANCE_DAWN, dawn, false);
        journey = GLPlayer.data(player).journey();
        helper.assertTrue(journey.isCompleted(ROMANCE_DAWN) && journey.isActive(LAND_HO), "Finishing it starts Land Ho!: " + journey.activeQuests());
        helper.assertTrue(player.getInventory().countItem(GLQuests.LOG_POSE.get()) == 1, "Romance Dawn pays a Log Pose");
        helper.assertTrue(!journey.isActive(MARINE_BASE), "Later chapters wait their turn");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void objectivesFollowDeeds(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 4));
        player.setPos(at.x, at.y, at.z);
        GLPlayer.edit(player, data -> {
            data.journey().complete(ROMANCE_DAWN);
            data.journey().complete(LAND_HO);
        });
        Quests.start(player, MARINE_BASE, Quests.get(access, MARINE_BASE));
        // Find the town.
        LogPose.discover(player, IslandType.SHELLS_TOWN, helper.absolutePos(new BlockPos(8, 1, 8)));
        helper.assertTrue(GLPlayer.data(player).journey().progress(MARINE_BASE, 0) == 1, "Reaching Shells Town counts");
        // Hear the townsfolk out.
        NpcEntity local = NpcEntity.spawn(helper.getLevel(), GrandLine.id("townsperson"), helper.absolutePos(new BlockPos(4, 2, 4)), 0f,
                MobSpawnType.COMMAND);
        helper.assertTrue(local != null && Quests.interact(player, local), "Speaking to a townsperson takes the conversation");
        helper.assertTrue(GLPlayer.data(player).journey().progress(MARINE_BASE, 1) == 1, "and counts");
        local.discard();
        // Bring Morgan down.
        BossEntity morgan = BossEntity.spawn(helper.getLevel(), GLBosses.MORGAN, helper.absolutePos(new BlockPos(8, 2, 8)), 0f,
                MobSpawnType.COMMAND);
        helper.assertTrue(morgan != null, "Spawned Morgan");
        int emeralds = player.getInventory().countItem(Items.EMERALD);
        morgan.hurt(player.damageSources().playerAttack(player), 100_000f);
        Journey journey = GLPlayer.data(player).journey();
        helper.assertTrue(journey.isCompleted(MARINE_BASE), "Beating Morgan finishes The Marine Base: " + journey.activeQuests());
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) >= emeralds + 12, "and pays its reward");
        helper.assertTrue(journey.isActive(ARLONG_PARK), "and the story moves on to Arlong Park");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, morgan.getBoundingBox().inflate(6)).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gatheredItemsAreHandedIn(GameTestHelper helper) {
        ServerPlayer player = NpcTests.newPlayer(helper);
        Quests.start(player, BAR_SUPPLIES, Quests.get(helper.getLevel().registryAccess(), BAR_SUPPLIES));
        player.getInventory().add(new ItemStack(Items.WHEAT, 5));
        Quests.check(player);
        helper.assertTrue(GLPlayer.data(player).journey().progress(BAR_SUPPLIES, 0) == 5, "Wheat in the bag counts: 5/12");
        player.getInventory().add(new ItemStack(Items.WHEAT, 10));
        Quests.check(player);
        Journey journey = GLPlayer.data(player).journey();
        helper.assertTrue(journey.isCompleted(BAR_SUPPLIES), "With 12 the supplies are delivered");
        helper.assertTrue(player.getInventory().countItem(Items.WHEAT) == 3, "Exactly 12 wheat were handed over, 3 left: "
                + player.getInventory().countItem(Items.WHEAT));
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) >= 6, "The barkeeper pays in bread and emeralds");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void islandersOfferQuests(GameTestHelper helper) {
        ServerPlayer player = NpcTests.newPlayer(helper);
        NpcEntity barkeeper = NpcEntity.spawn(helper.getLevel(), GrandLine.id("barkeeper"), helper.absolutePos(new BlockPos(8, 2, 8)), 0f,
                MobSpawnType.COMMAND);
        helper.assertTrue(barkeeper != null, "Spawned the barkeeper");
        helper.assertTrue(Quests.interact(player, barkeeper), "The barkeeper offers a quest");
        helper.assertTrue(!Quests.interact(player, barkeeper), "but doesn't repeat herself at once, so you can trade");
        helper.assertTrue(!GLPlayer.data(player).journey().isActive(BAR_SUPPLIES), "An offer is not a promise");
        GLPlayer.edit(player, data -> data.setReputation(GrandLine.id("marine"), -10));
        helper.assertTrue(Quests.blocker(player, GrandLine.id("marine_drills"), Quests.get(helper.getLevel().registryAccess(),
                GrandLine.id("marine_drills"))) != null, "The Marines only ask those in good standing");
        barkeeper.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theJourneyIsSaved(GameTestHelper helper) {
        PlayerData data = new PlayerData();
        Journey journey = data.journey();
        journey.start(BAR_SUPPLIES, 1);
        journey.setProgress(BAR_SUPPLIES, 0, 7);
        journey.complete(ROMANCE_DAWN);
        journey.record(new Journey.Island("shells_town", new BlockPos(100, 63, -200)));
        journey.setHeading(Optional.of(new Journey.Heading("", new BlockPos(900, 63, 40))));
        var encoded = PlayerData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        Journey copy = PlayerData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().journey();
        helper.assertTrue(copy.isActive(BAR_SUPPLIES) && copy.progress(BAR_SUPPLIES, 0) == 7, "Progress survives a save");
        helper.assertTrue(copy.isCompleted(ROMANCE_DAWN), "Finished quests survive a save");
        helper.assertTrue(copy.islands().size() == 1 && copy.hasFound("shells_town"), "Recorded islands survive a save");
        helper.assertTrue(copy.heading().map(h -> h.unknown() && h.target().equals(new BlockPos(900, 63, 40))).orElse(false),
                "The Log Pose's heading survives a save");
        PlayerData fresh = new PlayerData();
        helper.assertTrue(fresh.journey() != new PlayerData().journey() && fresh.journey().activeQuests().isEmpty(),
                "Every player starts their own journey");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theLogPoseRecordsIslands(GameTestHelper helper) {
        ServerPlayer player = NpcTests.newPlayer(helper);
        BlockPos dawn = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos shells = dawn.offset(700, 0, 0);
        helper.assertTrue(LogPose.discover(player, IslandType.DAWN_ISLAND, dawn), "Reaching Dawn Island records it");
        helper.assertTrue(!LogPose.discover(player, IslandType.DAWN_ISLAND, dawn.offset(30, 0, 30)), "It is the same island a little way on");
        helper.assertTrue(LogPose.discover(player, IslandType.SHELLS_TOWN, shells), "Shells Town is a new island");
        Journey journey = GLPlayer.data(player).journey();
        helper.assertTrue(journey.islands().size() == 2 && journey.hasFound("dawn_island") && journey.hasFound("shells_town"),
                "Both are recorded");
        helper.assertTrue(LogPose.aimAt(player, IslandType.DAWN_ISLAND), "The Log Pose can be turned back to Dawn Island");
        helper.assertTrue(GLPlayer.data(player).journey().heading().map(h -> h.target().equals(dawn)).orElse(false), "and points there");
        LogPose.cycle(player);
        helper.assertTrue(GLPlayer.data(player).journey().heading().map(h -> h.target().equals(shells)).orElse(false),
                "Turning it again points to Shells Town");
        helper.assertTrue(!LogPose.aimAt(player, IslandType.CONOMI_ISLANDS), "It can't point to an island never reached");
        helper.assertTrue("N".equals(LogPose.compass(0, -10)) && "E".equals(LogPose.compass(10, 0)) && "SW".equals(LogPose.compass(-10, 10)),
                "Compass directions read true");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void questCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player)
                .withPosition(at);
        NpcTests.run(helper, server, source, "onepiece quest list");
        NpcTests.run(helper, server, source, "onepiece quest accept grandline:partys_bar_supplies");
        helper.assertTrue(GLPlayer.data(player).journey().isActive(BAR_SUPPLIES), "quest accept");
        NpcTests.run(helper, server, source, "onepiece quest track grandline:partys_bar_supplies");
        helper.assertTrue(GLPlayer.data(player).journey().tracked().equals(Optional.of(BAR_SUPPLIES)), "quest track");
        NpcTests.run(helper, server, source, "onepiece quest");
        NpcTests.run(helper, server, source, "onepiece quest abandon grandline:partys_bar_supplies");
        helper.assertTrue(!GLPlayer.data(player).journey().isActive(BAR_SUPPLIES), "quest abandon");
        NpcTests.run(helper, server, source, "onepiece quest complete grandline:romance_dawn");
        helper.assertTrue(GLPlayer.data(player).journey().isCompleted(ROMANCE_DAWN) && GLPlayer.data(player).journey().isActive(LAND_HO),
                "quest complete moves the story on");
        NpcTests.run(helper, server, source, "onepiece logpose record dawn_island");
        helper.assertTrue(GLPlayer.data(player).journey().isCompleted(LAND_HO), "Recording an island finishes Land Ho!");
        NpcTests.run(helper, server, source, "onepiece logpose set dawn_island");
        NpcTests.run(helper, server, source, "onepiece logpose");
        NpcTests.run(helper, server, source, "onepiece logpose forget");
        helper.assertTrue(GLPlayer.data(player).journey().islands().isEmpty(), "logpose forget");
        NpcTests.run(helper, server, source, "onepiece quest reset");
        Journey journey = GLPlayer.data(player).journey();
        helper.assertTrue(journey.completedQuests().isEmpty() && journey.isActive(ROMANCE_DAWN), "quest reset starts the story over");
        helper.succeed();
    }
}
