package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.crew.Crews;
import com.nerodev.grandline.guide.Sources;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.ship.GLShips;
import com.nerodev.grandline.ship.ShipItem;
import com.nerodev.grandline.weapon.GLWeapons;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Guide: anyone may open it, and its "how to get it" lines come from the game's own recipes, trades and bosses. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class GuideTests {
    private static final String TEMPLATE = "empty";

    private GuideTests() {}

    @GameTest(template = TEMPLATE)
    public static void everyoneCanOpenTheGuide(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(0).withEntity(player);
        helper.assertTrue(execute(helper, server, source, "onepiece guide") == 1, "A player should open the Guide");
        Crews.create(player, "Guide Readers");
        helper.assertTrue(execute(helper, server, source, "onepiece guide") == 1, "A captain should open the Guide");
        Crews.disband(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sourcesComeFromTheGamesOwnData(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        RegistryAccess access = helper.getLevel().registryAccess();

        List<Component> galleon = Sources.of(stack -> stack.is(GLShips.SHIP_ITEM.get())
                && ShipItem.type(stack).equals(GrandLine.id("galleon")), "ship.galleon", recipes, access);
        helper.assertTrue(has(galleon, "Craft: "), "A galleon is crafted: " + text(galleon));

        List<Component> scroll = sources(GLItems.SWORDSMAN_SCROLL.get(), recipes, access);
        helper.assertTrue(has(scroll, "Craft: ") && has(scroll, "Sold by "), "The Swordsman scroll is crafted and sold: " + text(scroll));

        List<Component> ingot = sources(GLWeapons.SEASTONE_INGOT.get(), recipes, access);
        helper.assertTrue(has(ingot, "Smelt: "), "Seastone ingots are smelted: " + text(ingot));

        List<Component> axe = sources(GLWeapons.MORGANS_AXE.get(), recipes, access);
        helper.assertTrue(has(axe, "Always dropped by "), "Morgan drops his axe: " + text(axe));

        List<Component> kitetsu = sources(GLWeapons.SANDAI_KITETSU.get(), recipes, access);
        helper.assertTrue(has(kitetsu, "Carried by "), "Veteran bounty hunters carry Sandai Kitetsu: " + text(kitetsu));

        List<Component> yoru = sources(GLWeapons.YORU.get(), recipes, access);
        helper.assertFalse(has(yoru, "Craft: "), "Yoru is never crafted: " + text(yoru));
        helper.succeed();
    }

    private static List<Component> sources(Item item, RecipeManager recipes, RegistryAccess access) {
        return Sources.of(stack -> stack.is(item), "", recipes, access);
    }

    private static boolean has(List<Component> lines, String prefix) {
        return lines.stream().anyMatch(line -> line.getString().startsWith(prefix));
    }

    private static String text(List<Component> lines) {
        return String.join(" | ", lines.stream().map(Component::getString).toList());
    }

    private static int execute(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            return server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
            return 0;
        }
    }
}
