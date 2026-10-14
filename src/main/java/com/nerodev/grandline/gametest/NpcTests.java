package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.GLNpcs;
import com.nerodev.grandline.npc.NpcDefinition;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.npc.NpcRole;
import com.nerodev.grandline.npc.NpcShot;
import com.nerodev.grandline.npc.Reaction;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 12: islanders spawn ready, patrol, take sides, react to deeds, raise the alarm and trade by standing. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class NpcTests {
    private static final String ARENA = "arena";
    private static final ResourceLocation MARINE = GrandLine.id("marine");
    private static final ResourceLocation PIRATE = GrandLine.id("pirate");

    private NpcTests() {}

    @GameTest(template = "empty")
    public static void islanderDefinitionsAreValid(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<NpcDefinition> npcs = access.registryOrThrow(GLRegistries.NPC);
        helper.assertTrue(npcs.size() >= 15, "Expected at least 15 kinds of islander, found " + npcs.size());
        for (var entry : npcs.entrySet()) {
            NpcDefinition npc = entry.getValue();
            npc.faction().ifPresent(faction -> helper.assertTrue(access.registryOrThrow(GLRegistries.FACTION).containsKey(faction),
                    entry.getKey().location() + " serves an existing faction"));
            helper.assertTrue(npc.health() > 0 && npc.speed() > 0, entry.getKey().location() + " has sane stats");
            helper.assertTrue(npc.role() != NpcRole.MERCHANT || !npc.trades().isEmpty(), entry.getKey().location() + " has wares");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void islandersSpawnReady(GameTestHelper helper) {
        NpcEntity recruit = spawn(helper, "marine_recruit", 8, 8);
        helper.assertTrue(GrandLine.id("cutlass").equals(BuiltInRegistries.ITEM.getKey(recruit.getMainHandItem().getItem())),
                "The recruit carries a cutlass");
        helper.assertTrue(Math.abs(recruit.getMaxHealth() - 24f) < 0.01f && recruit.getHealth() == recruit.getMaxHealth(),
                "The recruit has 24 health, has " + recruit.getMaxHealth());
        helper.assertTrue(recruit.role() == NpcRole.SOLDIER && recruit.faction().equals(Optional.of(MARINE)), "A Marine soldier");
        helper.assertTrue(recruit.hasCustomName() && "marine".equals(recruit.skin()), "Named and dressed as a Marine");
        CompoundTag saved = new CompoundTag();
        recruit.saveWithoutId(saved);
        NpcEntity copy = new NpcEntity(GLNpcs.NPC.get(), helper.getLevel());
        copy.load(saved);
        helper.assertTrue(GrandLine.id("marine_recruit").equals(copy.npcId()) && copy.home().equals(recruit.home())
                && copy.role() == NpcRole.SOLDIER, "An islander keeps its kind and home through a save");
        recruit.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "npc_patrol")
    public static void soldiersWalkTheirBeat(GameTestHelper helper) {
        Runnable release = keepLoaded(helper);
        NpcEntity recruit = spawn(helper, "marine_recruit", 8, 8);
        Vec3 home = recruit.position();
        helper.succeedWhen(() -> {
            helper.assertTrue(recruit.position().distanceTo(home) > 3, "The recruit walks away from his post on patrol: " + describe(recruit));
            recruit.discard();
            release.run();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "npc_fight")
    public static void marinesAndPiratesFight(GameTestHelper helper) {
        NpcEntity marine = spawn(helper, "marine_recruit", 3, 8);
        NpcEntity pirate = spawn(helper, "pirate_grunt", 12, 8);
        helper.succeedWhen(() -> {
            helper.assertTrue(marine.getTarget() == pirate || pirate.getTarget() == marine, "Marine and pirate go for each other: "
                    + describe(marine) + " / hostile=" + marine.hostileTo(pirate));
            marine.discard();
            pirate.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "npc_gunfire")
    public static void gunmenOpenFire(GameTestHelper helper) {
        NpcEntity rifleman = spawn(helper, "marine_rifleman", 2, 8);
        NpcEntity pirate = spawn(helper, "pirate_grunt", 13, 8);
        pirate.setNoAi(true);
        helper.succeedWhen(() -> {
            boolean shot = !helper.getLevel().getEntitiesOfClass(NpcShot.class, rifleman.getBoundingBox().inflate(20)).isEmpty();
            helper.assertTrue(shot || pirate.getHealth() < pirate.getMaxHealth(), "The rifleman fires on the pirate: " + describe(rifleman));
            helper.getLevel().getEntitiesOfClass(NpcShot.class, rifleman.getBoundingBox().inflate(30)).forEach(NpcShot::discard);
            rifleman.discard();
            pirate.discard();
        });
    }

    @GameTest(template = "empty")
    public static void reactionsFollowStanding(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Optional<ResourceLocation> marines = Optional.of(MARINE);
        helper.assertTrue(Factions.reaction(player, marines) == Reaction.NEUTRAL, "Strangers are neutral");
        GLPlayer.edit(player, data -> data.setReputation(MARINE, 50));
        helper.assertTrue(Factions.reaction(player, marines) == Reaction.FRIENDLY, "Good standing makes friends");
        GLPlayer.edit(player, data -> data.setReputation(MARINE, -40));
        helper.assertTrue(Factions.reaction(player, marines) == Reaction.HOSTILE, "Bad standing makes enemies");
        GLPlayer.edit(player, data -> {
            data.setReputation(MARINE, 0);
            data.setFaction(Optional.of(PIRATE));
        });
        helper.assertTrue(Factions.reaction(player, marines) == Reaction.HOSTILE, "Marines fight pirates");
        helper.assertTrue(Factions.reaction(player, Optional.of(PIRATE)) == Reaction.FRIENDLY, "Pirates welcome their own");
        GLPlayer.edit(player, data -> {
            data.setFaction(Optional.empty());
            data.setBounty(5_000);
        });
        helper.assertTrue(Factions.reaction(player, marines) == Reaction.HOSTILE, "Marines arrest anyone with a bounty");
        helper.assertTrue(Factions.reaction(player, Optional.of(GrandLine.id("bounty_hunter"))) == Reaction.NEUTRAL,
                "Bounty hunters ignore small fry");
        GLPlayer.edit(player, data -> data.setBounty(50_000_000));
        helper.assertTrue(Factions.reaction(player, Optional.of(GrandLine.id("bounty_hunter"))) == Reaction.HOSTILE,
                "Bounty hunters come for a big bounty");
        helper.assertTrue(Factions.reaction(player, Optional.empty()) == Reaction.NEUTRAL, "Townsfolk start neutral");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void deedsChangeStanding(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        NpcEntity pirate = spawn(helper, "pirate_grunt", 8, 8);
        pirate.hurt(helper.getLevel().damageSources().playerAttack(player), 1000f);
        helper.assertTrue(!pirate.isAlive(), "The pirate falls");
        var data = GLPlayer.data(player);
        helper.assertTrue(data.reputation(PIRATE) <= -Factions.KILL_PENALTY, "Pirates resent it, standing " + data.reputation(PIRATE));
        helper.assertTrue(data.reputation(MARINE) >= Factions.KILL_REWARD, "Marines approve, standing " + data.reputation(MARINE));
        NpcEntity villager = spawn(helper, "villager", 4, 4);
        villager.hurt(helper.getLevel().damageSources().playerAttack(player), 1000f);
        helper.assertTrue(GLPlayer.data(player).reputation(Factions.CIVILIANS) <= -Factions.MURDER_PENALTY, "Townsfolk never forget a murder");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20))
                .forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anAttackRaisesTheAlarm(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        NpcEntity villager = spawn(helper, "villager", 4, 8);
        NpcEntity bystander = spawn(helper, "townsperson", 6, 10);
        NpcEntity marine = spawn(helper, "marine_recruit", 11, 8);
        helper.assertFalse(villager.fears(player), "Villagers don't fear a stranger");
        villager.hurt(helper.getLevel().damageSources().playerAttack(player), 1f);
        helper.assertTrue(villager.fears(player) && bystander.fears(player), "Townsfolk run from whoever struck one of them");
        helper.assertTrue(marine.holdsGrudge(player) && marine.hostileTo(player), "The Marines answer a cry for help");
        helper.assertTrue(GLPlayer.data(player).reputation(Factions.CIVILIANS) < 0, "Striking a villager costs standing");
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(zombie != null && villager.fears(zombie), "Townsfolk run from monsters");
        helper.assertTrue(marine.hostileTo(spawn(helper, "pirate_grunt", 14, 14)), "Marines fight pirates on sight");
        helper.getLevel().getEntitiesOfClass(NpcEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24))
                .forEach(NpcEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void merchantsTradeByStanding(GameTestHelper helper) {
        NpcEntity quartermaster = spawn(helper, "marine_quartermaster", 8, 8);
        ServerPlayer stranger = newPlayer(helper);
        ServerPlayer marine = newPlayer(helper);
        GLPlayer.edit(marine, data -> data.setFaction(Optional.of(MARINE)));
        MerchantOffers forStranger = quartermaster.offersFor(stranger);
        int strangerOffers = forStranger.size();
        int strangerPrice = forStranger.get(2).getCostA().getCount();
        MerchantOffers forMember = quartermaster.offersFor(marine);
        helper.assertTrue(forMember.size() > strangerOffers, "Marines get " + forMember.size() + " offers, strangers " + strangerOffers);
        helper.assertTrue(forMember.get(2).getCostA().getCount() < strangerPrice, "Friends pay less: "
                + forMember.get(2).getCostA().getCount() + " vs " + strangerPrice);
        ServerPlayer pirate = newPlayer(helper);
        GLPlayer.edit(pirate, data -> data.setFaction(Optional.of(PIRATE)));
        helper.assertTrue(quartermaster.reactionTo(pirate) == Reaction.HOSTILE, "The quartermaster won't arm a pirate");
        quartermaster.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void islanderCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 8));
        player.setPos(at.x, at.y, at.z);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player)
                .withPosition(at);
        run(helper, server, source, "onepiece npc list");
        run(helper, server, source, "onepiece npc spawn grandline:villager 3");
        List<NpcEntity> spawned = helper.getLevel().getEntitiesOfClass(NpcEntity.class, new AABB(BlockPos.containing(at)).inflate(8));
        helper.assertTrue(spawned.size() == 3, "npc spawn made 3, found " + spawned.size());
        run(helper, server, source, "onepiece npc remove 8");
        helper.assertTrue(spawned.stream().allMatch(NpcEntity::isRemoved), "npc remove");
        run(helper, server, source, "onepiece reputation set grandline:marine 40");
        run(helper, server, source, "onepiece reputation add grandline:civilians -10");
        run(helper, server, source, "onepiece reputation");
        helper.assertTrue(GLPlayer.data(player).reputation(MARINE) == 40, "reputation set");
        helper.assertTrue(GLPlayer.data(player).reputation(Factions.CIVILIANS) == -10, "reputation add");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Keeps the chunks around the test loaded and ticking (islanders walk beyond the arena), and returns what
     * lets them go again.
     */
    static Runnable keepLoaded(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                helper.getLevel().setChunkForced(cx + dx, cz + dz, true);
            }
        }
        return () -> {
            for (int dx = -2; dx <= 3; dx++) {
                for (int dz = -2; dz <= 3; dz++) {
                    helper.getLevel().setChunkForced(cx + dx, cz + dz, false);
                }
            }
        };
    }

    /** What an islander's AI is up to, for failure messages. */
    private static String describe(NpcEntity npc) {
        return "tick " + npc.tickCount + ", goals " + npc.goalSelector.getAvailableGoals().size() + "/" + npc.targetSelector.getAvailableGoals().size()
                + ", running " + npc.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning())
                        .map(g -> g.getGoal().getClass().getSimpleName()).toList()
                + ", target " + npc.getTarget() + ", path " + (npc.getNavigation().getPath() == null ? "none" : npc.getNavigation().getPath().getNodeCount() + " nodes")
                + ", at " + npc.blockPosition() + ", ground " + npc.onGround() + ", noAi " + npc.isNoAi() + ", removed " + npc.isRemoved()
                + ", speed " + npc.getSpeed() + ", effectiveAi " + npc.isEffectiveAi();
    }

    private static NpcEntity spawn(GameTestHelper helper, String id, int x, int z) {
        NpcEntity npc = NpcEntity.spawn(helper.getLevel(), GrandLine.id(id), helper.absolutePos(new BlockPos(x, 2, z)), 0f,
                MobSpawnType.COMMAND);
        helper.assertTrue(npc != null, "Spawned " + id);
        return npc;
    }

    static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }

    static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }
}
