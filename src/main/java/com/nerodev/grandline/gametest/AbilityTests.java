package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.ability.rokushiki.GeppoAbility;
import com.nerodev.grandline.ability.rokushiki.SoruAbility;
import com.nerodev.grandline.ability.rokushiki.TekkaiAbility;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 3: ability validation, cooldowns, stamina, sustained abilities, slots and commands. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class AbilityTests {
    private static final String TEMPLATE = "empty";

    private AbilityTests() {}

    @GameTest(template = TEMPLATE)
    public static void lockedAbilitiesAreRefused(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        expect(helper, AbilityManager.tryStart(player, GLAbilities.SORU.get()), Result.NOT_USABLE);
        helper.assertTrue(AbilityState.of(player).cooldownRemaining(GLAbilities.SORU.getId(), now(player)) == 0,
                "A refused ability must not start a cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void instantAbilityCostsStaminaAndStartsCooldown(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        unlock(player, GLAbilities.SORU.getId());
        AbilityState state = AbilityState.of(player);
        float before = state.stamina(now(player));

        expect(helper, AbilityManager.tryStart(player, GLAbilities.SORU.get()), Result.STARTED);
        float spent = before - state.stamina(now(player));
        helper.assertTrue(Math.abs(spent - 18f) < 0.01f, "Soru should cost 18 stamina, cost " + spent);
        helper.assertTrue(state.isOnCooldown(GLAbilities.SORU.getId(), now(player)), "Soru should be on cooldown");
        helper.assertTrue(state.isEvading(now(player)), "Soru should grant an evade window");
        expect(helper, AbilityManager.tryStart(player, GLAbilities.SORU.get()), Result.ON_COOLDOWN);

        state.setFlag(TestFlag.NO_COOLDOWNS, true);
        expect(helper, AbilityManager.tryStart(player, GLAbilities.SORU.get()), Result.STARTED);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void noStaminaNoAbility(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        unlock(player, GLAbilities.SORU.getId());
        AbilityState state = AbilityState.of(player);
        state.setFlag(TestFlag.NO_COOLDOWNS, true);
        while (state.spendStamina(18f, now(player))) {
            // drain
        }
        expect(helper, AbilityManager.tryStart(player, GLAbilities.SORU.get()), Result.NO_STAMINA);

        state.setFlag(TestFlag.INFINITE_STAMINA, true);
        expect(helper, AbilityManager.tryStart(player, GLAbilities.SORU.get()), Result.STARTED);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staminaRegeneratesFromTimestamps(GameTestHelper helper) {
        AbilityState state = new AbilityState();
        state.spendStamina(60f, 1000);
        helper.assertTrue(Math.abs(state.stamina(1000) - 40f) < 0.01f, "Stamina right after spending");
        helper.assertTrue(Math.abs(state.stamina(1000 + AbilityState.REGEN_DELAY) - 40f) < 0.01f, "No regen during delay");
        float later = state.stamina(1000 + AbilityState.REGEN_DELAY + 40);
        helper.assertTrue(Math.abs(later - (40f + 40 * AbilityState.REGEN_PER_TICK)) < 0.01f, "Regen after delay: " + later);
        helper.assertTrue(state.stamina(1_000_000) == state.maxStamina(), "Regen caps at max");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void soruStopsAtWalls(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        for (int x = 0; x <= 10; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 0), Blocks.AIR);
            }
        }
        Vec3 start = helper.absoluteVec(new Vec3(0.5, 1, 0.5));
        player.setPos(start);
        Vec3 east = new Vec3(1, 0, 0);
        Vec3 open = SoruAbility.findDestination(helper.getLevel(), player, start, east);
        helper.assertTrue(Math.abs(open.distanceTo(start) - SoruAbility.DISTANCE) < 0.3,
                "Open ground: expected ~" + SoruAbility.DISTANCE + " blocks, got " + open.distanceTo(start));

        helper.setBlock(new BlockPos(4, 1, 0), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 0), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 0), Blocks.STONE);
        Vec3 walled = SoruAbility.findDestination(helper.getLevel(), player, start, east);
        helper.assertTrue(walled.x < helper.absoluteVec(new Vec3(4, 0, 0)).x, "Soru must stop before a wall: " + walled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void geppoNeedsAirAndHasALimit(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        unlock(player, GLAbilities.GEPPO.getId());
        AbilityState.of(player).setFlag(TestFlag.NO_COOLDOWNS, true);

        player.setOnGround(true);
        expect(helper, AbilityManager.tryStart(player, GLAbilities.GEPPO.get()), Result.BLOCKED);

        player.setOnGround(false);
        for (int i = 0; i < GeppoAbility.MAX_AIR_JUMPS; i++) {
            expect(helper, AbilityManager.tryStart(player, GLAbilities.GEPPO.get()), Result.STARTED);
        }
        expect(helper, AbilityManager.tryStart(player, GLAbilities.GEPPO.get()), Result.BLOCKED);

        player.setOnGround(true);
        AbilityManager.tickPlayer(player);
        helper.assertTrue(AbilityState.of(player).airJumps() == 0, "Landing should reset air jumps");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void tekkaiHoldsDrainsAndReleases(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        unlock(player, GLAbilities.TEKKAI.getId());
        ResourceLocation id = GLAbilities.TEKKAI.getId();
        AbilityState state = AbilityState.of(player);

        expect(helper, AbilityManager.tryStart(player, GLAbilities.TEKKAI.get()), Result.STARTED);
        helper.assertTrue(state.isActive(id), "Tekkai should be active while held");
        helper.assertTrue(TekkaiAbility.isHardened(player), "Tekkai should slow the player");
        helper.assertFalse(state.isOnCooldown(id, now(player)), "Hold abilities start their cooldown on release");

        helper.onEachTick(() -> AbilityManager.tickPlayer(player));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(state.isActive(id), "Tekkai should still be active");
            float stamina = state.stamina(now(player));
            helper.assertTrue(stamina < 100f - 5f - 0.4f * 15, "Tekkai should drain stamina while held: " + stamina);

            AbilityManager.release(player, GLAbilities.TEKKAI.get());
            helper.assertFalse(state.isActive(id), "Released Tekkai should end");
            helper.assertFalse(TekkaiAbility.isHardened(player), "Ending Tekkai must remove the slow");
            helper.assertTrue(state.isOnCooldown(id, now(player)), "Cooldown should start on release");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void slotKeysTriggerEquippedAbility(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        unlock(player, GLAbilities.SORU.getId());
        AbilityState state = AbilityState.of(player);
        state.setSlot(2, GLAbilities.SORU.getId());

        AbilityManager.handleKey(player, 0, true);
        helper.assertFalse(state.isOnCooldown(GLAbilities.SORU.getId(), now(player)), "Empty slot must do nothing");
        AbilityManager.handleKey(player, 2, true);
        helper.assertTrue(state.isOnCooldown(GLAbilities.SORU.getId(), now(player)), "Slot 3 should fire Soru");

        state.setSlot(0, GLAbilities.SORU.getId());
        helper.assertTrue(state.slot(2) == null && GLAbilities.SORU.getId().equals(state.slot(0)),
                "An ability lives in one slot only");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void abilityStateSurvivesSaving(GameTestHelper helper) {
        AbilityState state = new AbilityState();
        state.setSlot(0, GLAbilities.SORU.getId());
        state.setSlot(4, GLAbilities.TEKKAI.getId());
        state.startCooldown(GLAbilities.GEPPO.getId(), 500, 40);
        state.setFlag(TestFlag.INVINCIBLE, true);
        var json = AbilityState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        AbilityState loaded = AbilityState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        helper.assertTrue(GLAbilities.SORU.getId().equals(loaded.slot(0)), "Slot 1 lost");
        helper.assertTrue(GLAbilities.TEKKAI.getId().equals(loaded.slot(4)), "Slot 5 lost");
        helper.assertTrue(loaded.slot(1) == null, "Empty slot should stay empty");
        helper.assertTrue(loaded.cooldownUntil(GLAbilities.GEPPO.getId()) == 540, "Cooldown lost");
        helper.assertTrue(loaded.hasFlag(TestFlag.INVINCIBLE), "Flag lost");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void abilityCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player);

        run(helper, server, source, "onepiece ability list");
        run(helper, server, source, "onepiece ability unlock grandline:geppo");
        helper.assertTrue(GLPlayer.data(player).isUnlocked(UnlockCategory.ABILITY, GLAbilities.GEPPO.getId()), "Geppo unlock");
        run(helper, server, source, "onepiece ability equip 5 geppo");
        helper.assertTrue(GLAbilities.GEPPO.getId().equals(AbilityState.of(player).slot(4)), "Geppo in slot 5");
        assertFails(helper, server, source, "onepiece ability equip 1 grandline:tekkai");

        run(helper, server, source, "onepiece unlock all");
        long count = GLAbilities.REGISTRY.keySet().size();
        helper.assertTrue(GLPlayer.data(player).unlocked(UnlockCategory.ABILITY).size() == count, "Unlock all");
        run(helper, server, source, "onepiece ability equip 1 grandline:tekkai");

        run(helper, server, source, "onepiece stamina infinite");
        helper.assertTrue(AbilityState.of(player).hasFlag(TestFlag.INFINITE_STAMINA), "Infinite stamina flag");
        run(helper, server, source, "onepiece cooldown off");
        run(helper, server, source, "onepiece invincible on");
        run(helper, server, source, "onepiece health infinite");
        run(helper, server, source, "onepiece test");
        run(helper, server, source, "onepiece cooldown reset");
        run(helper, server, source, "onepiece ability use grandline:soru");
        run(helper, server, source, "onepiece debug ability grandline:tekkai");

        run(helper, server, source, "onepiece ability lock grandline:geppo");
        helper.assertTrue(AbilityState.of(player).slot(4) == null, "Locking removes the ability from its slot");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }

    private static void unlock(ServerPlayer player, ResourceLocation ability) {
        GLPlayer.data(player).unlock(UnlockCategory.ABILITY, ability);
    }

    private static long now(ServerPlayer player) {
        return AbilityManager.now(player);
    }

    private static void expect(GameTestHelper helper, Result actual, Result expected) {
        helper.assertTrue(actual == expected, "Expected " + expected + " but got " + actual);
    }

    private static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }

    private static void assertFails(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
            helper.fail("/" + command + " should have failed");
        } catch (CommandSyntaxException expected) {
            // expected
        }
    }
}
