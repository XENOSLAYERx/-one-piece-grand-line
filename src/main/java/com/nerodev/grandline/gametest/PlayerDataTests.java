package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.data.DeathMode;
import com.nerodev.grandline.data.DeathModeHandler;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.npc.Factions;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 2: player data persistence, death copy, editing commands and creator death mode. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class PlayerDataTests {
    private static final String TEMPLATE = "empty";

    private PlayerDataTests() {}

    @GameTest(template = TEMPLATE)
    public static void dataSurvivesSaveAndLoad(GameTestHelper helper) {
        ServerPlayer original = newPlayer(helper);
        fill(GLPlayer.data(original));

        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        ServerPlayer reloaded = newPlayer(helper);
        reloaded.load(saved);

        assertSame(helper, GLPlayer.data(original), GLPlayer.data(reloaded), "save/load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dataSurvivesDeath(GameTestHelper helper) {
        ServerPlayer dead = newPlayer(helper);
        fill(GLPlayer.data(dead));

        ServerPlayer respawned = newPlayer(helper);
        respawned.copyAttachmentsFrom(dead, true);

        assertSame(helper, GLPlayer.data(dead), GLPlayer.data(respawned), "death copy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bountyAndMasteryAreClamped(GameTestHelper helper) {
        PlayerData data = new PlayerData();
        data.setBounty(-50);
        helper.assertTrue(data.bounty() == 0, "Bounty must not go negative");
        data.setBounty(Long.MAX_VALUE);
        helper.assertTrue(data.bounty() == PlayerData.MAX_BOUNTY, "Bounty must cap at MAX_BOUNTY");
        data.addBounty(Long.MAX_VALUE);
        helper.assertTrue(data.bounty() == PlayerData.MAX_BOUNTY, "Bounty add must not overflow");
        data.setMastery("haki/armament", 250);
        helper.assertTrue(data.mastery("haki/armament") == 100.0, "Mastery must cap at 100");
        data.setMastery("haki/armament", Double.NaN);
        helper.assertTrue(data.mastery("haki/armament") == 0.0, "NaN mastery must reset to 0");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creatorCommandsEditData(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput()
                .withPermission(4).withEntity(player);

        run(helper, server, source, "onepiece race set fishman");
        run(helper, server, source, "onepiece faction set grandline:marine");
        run(helper, server, source, "onepiece bounty set 1000");
        run(helper, server, source, "onepiece bounty add 500");
        run(helper, server, source, "onepiece deathmode creator");
        run(helper, server, source, "onepiece profile");
        run(helper, server, source, "onepiece debug player");

        PlayerData data = GLPlayer.data(player);
        helper.assertTrue(data.race().equals(GrandLine.id("fishman")), "Race should be fishman: " + data.race());
        helper.assertTrue(data.faction().equals(Optional.of(GrandLine.id("marine"))), "Faction: " + data.faction());
        helper.assertTrue(data.bounty() == 1500, "Bounty should be 1500: " + data.bounty());
        helper.assertTrue(data.deathMode() == DeathMode.CREATOR, "Death mode should be creator");

        // Without "confirm" nothing changes.
        run(helper, server, source, "onepiece reset all");
        helper.assertTrue(data.bounty() == 1500, "Unconfirmed reset must not change data");

        run(helper, server, source, "onepiece reset faction confirm");
        helper.assertTrue(data.faction().isEmpty(), "Faction reset");
        helper.assertTrue(data.bounty() == 1500, "Faction reset must not touch bounty");

        run(helper, server, source, "onepiece reset all confirm");
        helper.assertTrue(data.race().equals(PlayerData.DEFAULT_RACE) && data.bounty() == 0
                && data.deathMode() == DeathMode.NORMAL, "Full reset should restore defaults");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playersCannotEditData(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput()
                .withPermission(0).withEntity(player);

        assertFails(helper, server, source, "onepiece race set fishman");
        assertFails(helper, server, source, "onepiece bounty set 5");
        assertFails(helper, server, source, "onepiece deathmode creator");
        run(helper, server, source, "onepiece profile");
        helper.assertTrue(GLPlayer.data(player).race().equals(PlayerData.DEFAULT_RACE), "Race must be unchanged");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playersChooseARaceOnceAndJoinFactionsThatTrustThem(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput()
                .withPermission(0).withEntity(player);
        PlayerData data = GLPlayer.data(player);

        helper.assertTrue(result(helper, server, source, "onepiece race choose mink") == 1, "A player should choose their race");
        helper.assertTrue(data.race().equals(GrandLine.id("mink")), "Race should be mink: " + data.race());
        helper.assertTrue(result(helper, server, source, "onepiece race choose fishman") == 0, "A race is chosen only once");
        helper.assertTrue(data.race().equals(GrandLine.id("mink")), "Race must stay mink: " + data.race());

        ResourceLocation marines = GrandLine.id("marine");
        helper.assertTrue(result(helper, server, source, "onepiece faction join grandline:marine") == 0,
                "The Marines must not take a stranger");
        data.addReputation(marines, Factions.FRIENDLY);
        data.setBounty(1000);
        helper.assertTrue(result(helper, server, source, "onepiece faction join grandline:marine") == 0,
                "The Marines must not take a wanted player");
        data.setBounty(0);
        helper.assertTrue(result(helper, server, source, "onepiece faction join grandline:marine") == 1,
                "The Marines should take a trusted player");
        helper.assertTrue(data.faction().equals(Optional.of(marines)), "Faction: " + data.faction());
        helper.assertTrue(result(helper, server, source, "onepiece faction join grandline:pirate") == 0,
                "A member must leave before joining another faction");

        helper.assertTrue(result(helper, server, source, "onepiece faction leave") == 1, "A member should be able to leave");
        helper.assertTrue(data.faction().isEmpty(), "Faction should be empty after leaving");
        helper.assertTrue(data.reputation(marines) == 0, "Leaving should cost standing: " + data.reputation(marines));
        helper.assertTrue(result(helper, server, source, "onepiece faction leave") == 0, "Nothing to leave");
        assertFails(helper, server, source, "onepiece faction set grandline:pirate");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unknownRaceIsRejected(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput()
                .withPermission(4).withEntity(player);
        assertFails(helper, server, source, "onepiece race set grandline:cyborg_dragon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creatorDeathKeepsInventoryAndXp(GameTestHelper helper) {
        ServerPlayer dead = newPlayer(helper);
        dead.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        dead.experienceLevel = 30;

        DeathModeHandler.stash(dead);
        helper.assertTrue(dead.getInventory().isEmpty(), "Stashed inventory must be empty so nothing drops");

        ServerPlayer respawned = newPlayer(helper);
        helper.assertTrue(DeathModeHandler.restore(dead, respawned), "Stash should restore");
        helper.assertTrue(respawned.getInventory().countItem(Items.DIAMOND) == 5, "Diamonds should be restored");
        helper.assertTrue(respawned.experienceLevel == 30, "XP level should be restored");
        helper.assertFalse(DeathModeHandler.restore(dead, respawned), "A stash is used only once");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }

    private static void fill(PlayerData data) {
        data.setRace(GrandLine.id("mink"));
        data.setFaction(Optional.of(GrandLine.id("revolutionary")));
        data.setBounty(320_000_000L);
        data.setDeathMode(DeathMode.CREATOR);
        data.unlock(UnlockCategory.FRUIT, GrandLine.id("gum_gum"));
        data.unlock(UnlockCategory.STYLE, GrandLine.id("black_leg"));
        data.setMastery("fruit/grandline:gum_gum", 42.5);
    }

    private static void assertSame(GameTestHelper helper, PlayerData expected, PlayerData actual, String what) {
        String a = PlayerData.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow().toString();
        String b = PlayerData.CODEC.encodeStart(JsonOps.INSTANCE, actual).getOrThrow().toString();
        helper.assertTrue(a.equals(b), what + " lost data.\nexpected " + a + "\nactual   " + b);
        helper.assertTrue(actual.race().equals(GrandLine.id("mink")), what + ": race should be mink");
    }

    private static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }

    private static int result(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            return server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
            return 0;
        }
    }

    private static void assertFails(GameTestHelper helper, MinecraftServer server, CommandSourceStack source,
            String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
            helper.fail("/" + command + " should have failed");
        } catch (CommandSyntaxException expected) {
            // expected
        }
    }
}
