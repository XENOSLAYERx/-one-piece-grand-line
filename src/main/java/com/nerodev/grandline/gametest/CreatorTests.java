package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.creator.Battles;
import com.nerodev.grandline.creator.Builds;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.Loadout;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.StyleManager;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.TransformationManager;
import com.nerodev.grandline.weapon.GLWeapons;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 9: the creator workflow (loadouts, random builds, battle tests, forced transformations, unlocks). */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class CreatorTests {
    private static final String TEMPLATE = "empty";

    private CreatorTests() {}

    @GameTest(template = TEMPLATE)
    public static void loadoutsRestoreABuild(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        RaceManager.setRace(player, GrandLine.id("mink"));
        FruitManager.setFruit(player, GLFruits.FLAME.get());
        StyleManager.learn(player, GLStyles.BLACK_LEG.get());
        StyleManager.equip(player, GLStyles.BLACK_LEG.get());
        HakiManager.setLevel(player, HakiType.ARMAMENT, 40);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.KATANA.get()));
        Loadout saved = Builds.capture(player);
        GLPlayer.edit(player, data -> data.saveLoadout("cook", saved));

        RaceManager.setRace(player, GrandLine.id("giant"));
        FruitManager.setFruit(player, GLFruits.GUM_GUM.get());
        StyleManager.equip(player, GLStyles.BRAWLER.get());
        HakiManager.setLevel(player, HakiType.OBSERVATION, 10);
        HakiManager.remove(player, HakiType.ARMAMENT);

        Builds.apply(player, GLPlayer.data(player).loadouts().get("cook"));
        helper.assertTrue(GLPlayer.data(player).race().equals(GrandLine.id("mink")), "Race restored");
        helper.assertTrue(FruitManager.fruitOf(player) == GLFruits.FLAME.get(), "Fruit restored");
        helper.assertTrue(StyleManager.style(player) == GLStyles.BLACK_LEG.get(), "Style restored");
        helper.assertTrue(HakiManager.has(player, HakiType.ARMAMENT) && HakiManager.level(player, HakiType.ARMAMENT) == 40,
                "Armament restored at 40");
        helper.assertFalse(HakiManager.has(player, HakiType.OBSERVATION), "Haki the build lacked is taken away");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void randomBuildsRollEverything(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Builds.apply(player, Builds.random(player));
        helper.assertTrue(FruitManager.hasFruit(player), "A random build has a fruit");
        helper.assertTrue(GLPlayer.data(player).style().isPresent() || StyleManager.style(player) == GLStyles.BRAWLER.get(),
                "A random build has a style");
        helper.assertTrue(RaceManager.definition(player) != null, "A random build has a real race");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void battleWavesSpawnAndClear(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        player.setPos(at.x, at.y, at.z);
        int spawned = Battles.spawnWave(player, Battles.Difficulty.EASY);
        helper.assertTrue(spawned == Battles.Difficulty.EASY.wave().size(), "Every enemy of the wave spawns, got " + spawned);
        long tagged = helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(player.blockPosition()).inflate(32),
                entity -> entity.getTags().contains(Battles.TAG)).size();
        helper.assertTrue(tagged >= spawned, "Battle enemies are tagged");
        helper.assertTrue(Battles.clear(player) >= spawned, "Clearing removes them");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creatorCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player);
        run(helper, server, source, "onepiece loadout save starter");
        helper.assertTrue(GLPlayer.data(player).loadouts().containsKey("starter"), "loadout save");
        run(helper, server, source, "onepiece loadout load starter");
        run(helper, server, source, "onepiece loadout list");
        run(helper, server, source, "onepiece loadout delete starter");
        helper.assertFalse(GLPlayer.data(player).loadouts().containsKey("starter"), "loadout delete");
        run(helper, server, source, "onepiece transform grandline:gear_second");
        helper.assertTrue(TransformationManager.isActive(player, GLTransformations.GEAR_SECOND.get()), "transform");
        run(helper, server, source, "onepiece transform off");
        helper.assertTrue(TransformationManager.active(player) == null, "transform off");
        run(helper, server, source, "onepiece unlock weapons");
        helper.assertTrue(GLPlayer.data(player).isUnlocked(UnlockCategory.WEAPON, GLWeapons.YORU.getId()), "unlock weapons");
        run(helper, server, source, "onepiece unlock styles");
        helper.assertTrue(StyleManager.knows(player, GLStyles.SWORDSMAN.get()), "unlock styles");
        run(helper, server, source, "onepiece mastery set style grandline:swordsman 50");
        helper.assertTrue(GLPlayer.data(player).mastery(GLStyles.SWORDSMAN.get().masteryKey()) == 50, "mastery set style");
        run(helper, server, source, "onepiece mastery set race 30");
        helper.assertTrue(GLPlayer.data(player).mastery(RaceManager.masteryKey(GLPlayer.data(player).race())) == 30, "mastery set race");
        run(helper, server, source, "onepiece random build");
        run(helper, server, source, "onepiece menu");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

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
