package com.nerodev.grandline.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.definition.DevilFruitDefinition;
import com.nerodev.grandline.definition.FruitType;
import com.nerodev.grandline.permission.PermissionTier;
import com.nerodev.grandline.permission.Permissions;
import com.nerodev.grandline.registry.GLRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Milestone 1 automated checks. Run with {@code ./gradlew runGameTestServer} (CI does this on every push).
 * GameTests only load in development environments, never in the released jar.
 */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class CoreFrameworkTests {
    private static final String TEMPLATE = "empty";

    private CoreFrameworkTests() {}

    @GameTest(template = TEMPLATE)
    public static void registriesLoadFromJson(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Registry<DevilFruitDefinition> fruits = server.registryAccess().registryOrThrow(GLRegistries.DEVIL_FRUIT);

        helper.assertTrue(fruits.size() == 3, "Expected 3 fruits, found " + fruits.size());
        assertFruitType(helper, fruits, "gum_gum", FruitType.PARAMECIA);
        assertFruitType(helper, fruits, "leopard", FruitType.ZOAN);
        assertFruitType(helper, fruits, "flame", FruitType.LOGIA);

        int races = server.registryAccess().registryOrThrow(GLRegistries.RACE).size();
        helper.assertTrue(races == 7, "Expected 7 races, found " + races);
        int factions = server.registryAccess().registryOrThrow(GLRegistries.FACTION).size();
        helper.assertTrue(factions == 4, "Expected 4 factions, found " + factions);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tiersFollowOpLevels(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        PermissionTier[] expected = {
                PermissionTier.PLAYER, PermissionTier.PLAYER, PermissionTier.MODERATOR,
                PermissionTier.ADMIN, PermissionTier.CREATOR};
        for (int level = 0; level <= 4; level++) {
            PermissionTier actual = Permissions.tierOf(server.createCommandSourceStack().withPermission(level));
            helper.assertTrue(actual == expected[level],
                    "Op level " + level + " should be " + expected[level] + " but was " + actual);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creatorCanRunEveryMilestoneOneCommand(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Capture out = new Capture();
        CommandSourceStack source = out.source(server, 4);

        run(helper, server, source, "onepiece");
        run(helper, server, source, "onepiece help 99");
        for (int page = 1; page <= 5; page++) {
            run(helper, server, source, "onepiece help " + page);
        }
        helper.assertTrue(out.contains("/onepiece debug config"), "Creator help should list debug config");
        helper.assertTrue(out.contains("/onepiece race set"), "Creator help should list race set");

        out.clear();
        run(helper, server, source, "onepiece debug");
        helper.assertTrue(out.contains("fruits 3"), "Debug overview should report 3 fruits: " + out.lines);

        run(helper, server, source, "onepiece debug config");
        run(helper, server, source, "onepiece debug permissions");

        out.clear();
        run(helper, server, source, "onepiece debug fruit");
        helper.assertTrue(out.contains("grandline:gum_gum"), "Fruit list should include gum_gum: " + out.lines);

        out.clear();
        run(helper, server, source, "onepiece debug fruit grandline:flame");
        helper.assertTrue(out.contains("Logia"), "Flame should be a Logia: " + out.lines);

        out.clear();
        run(helper, server, source, "onepiece debug fruit leopard");
        helper.assertTrue(out.contains("Zoan"), "Bare id 'leopard' should resolve to grandline:leopard: " + out.lines);

        run(helper, server, source, "onepiece debug race");
        run(helper, server, source, "onepiece debug race grandline:mink");
        run(helper, server, source, "onepiece debug faction");
        run(helper, server, source, "onepiece debug faction marine");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unknownIdsAreRejected(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack source = new Capture().source(server, 4);
        assertFails(helper, server, source, "onepiece debug fruit grandline:not_a_fruit");
        assertFails(helper, server, source, "onepiece debug race nope");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playersCannotUseStaffCommands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Capture out = new Capture();
        CommandSourceStack player = out.source(server, 0);

        run(helper, server, player, "onepiece help");
        helper.assertTrue(out.contains("/onepiece help"), "Players should see help: " + out.lines);
        helper.assertFalse(out.contains("/onepiece debug"), "Players must not see debug in help: " + out.lines);
        assertFails(helper, server, player, "onepiece debug");
        assertFails(helper, server, player, "onepiece debug fruit");

        CommandSourceStack moderator = new Capture().source(server, 2);
        run(helper, server, moderator, "onepiece debug");
        assertFails(helper, server, moderator, "onepiece debug config");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void commandsAutocomplete(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack creator = new Capture().source(server, 4);
        CommandSourceStack player = new Capture().source(server, 0);

        List<String> root = suggestions(server, creator, "onepiece ");
        helper.assertTrue(root.contains("help") && root.contains("debug"), "Root suggestions: " + root);

        // Clients only receive the nodes they can use (Commands#sendCommands filters with canUse), so that is what
        // decides whether a player ever sees "debug" in tab-complete.
        var onepiece = dispatcher(server).getRoot().getChild("onepiece");
        helper.assertTrue(onepiece.canUse(player), "Players should be able to see /onepiece");
        helper.assertTrue(onepiece.getChild("help").canUse(player), "Players should see help");
        helper.assertFalse(onepiece.getChild("debug").canUse(player), "Players must not receive the debug node");
        helper.assertTrue(onepiece.getChild("debug").canUse(creator), "Creators must receive the debug node");

        List<String> debug = suggestions(server, creator, "onepiece debug ");
        for (String expected : List.of("config", "permissions", "fruit", "race", "faction")) {
            helper.assertTrue(debug.contains(expected), "Debug suggestions missing " + expected + ": " + debug);
        }

        List<String> fruits = suggestions(server, creator, "onepiece debug fruit ");
        helper.assertTrue(fruits.equals(List.of("grandline:flame", "grandline:gum_gum", "grandline:leopard")),
                "Fruit suggestions: " + fruits);
        List<String> partial = suggestions(server, creator, "onepiece debug fruit gum");
        helper.assertTrue(partial.equals(List.of("grandline:gum_gum")), "Partial fruit suggestions: " + partial);
        List<String> races = suggestions(server, creator, "onepiece debug race ");
        helper.assertTrue(races.size() == 7, "Race suggestions: " + races);
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void assertFruitType(GameTestHelper helper, Registry<DevilFruitDefinition> fruits, String path,
            FruitType type) {
        DevilFruitDefinition fruit = fruits.get(GrandLine.id(path));
        helper.assertTrue(fruit != null, "Missing fruit grandline:" + path);
        helper.assertTrue(fruit.type() == type, "grandline:" + path + " should be " + type + " but is " + fruit.type());
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher(MinecraftServer server) {
        return server.getCommands().getDispatcher();
    }

    private static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            int result = dispatcher(server).execute(command, source);
            helper.assertTrue(result > 0, "/" + command + " returned " + result);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }

    private static void assertFails(GameTestHelper helper, MinecraftServer server, CommandSourceStack source,
            String command) {
        try {
            dispatcher(server).execute(command, source);
            helper.fail("/" + command + " should have failed");
        } catch (CommandSyntaxException expected) {
            // expected
        }
    }

    private static List<String> suggestions(MinecraftServer server, CommandSourceStack source, String input) {
        var parse = dispatcher(server).parse(input, source);
        return dispatcher(server).getCompletionSuggestions(parse).join().getList().stream()
                .map(Suggestion::getText)
                .toList();
    }

    /** Command output sink that records every message as plain text. */
    private static final class Capture implements CommandSource {
        private final List<String> lines = new ArrayList<>();

        CommandSourceStack source(MinecraftServer server, int opLevel) {
            return server.createCommandSourceStack().withSource(this).withPermission(opLevel);
        }

        boolean contains(String text) {
            return lines.stream().anyMatch(line -> line.contains(text));
        }

        void clear() {
            lines.clear();
        }

        @Override
        public void sendSystemMessage(Component message) {
            lines.add(message.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }
}
