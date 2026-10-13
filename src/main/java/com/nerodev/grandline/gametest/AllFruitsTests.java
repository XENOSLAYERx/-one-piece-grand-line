package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.fruit.kit.Minions;
import com.nerodev.grandline.fruit.kit.TempBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every ability of every Devil Fruit, run end to end: start, a long tick (charge/channel), release, stop. One fruit
 * per game tick, then projectiles and zones are left to fly and hit a target for a while. Any exception fails the
 * test with the fruit and ability named, so new fruit batches are covered automatically.
 */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class AllFruitsTests {
    private static final String TEMPLATE = "empty";

    private AllFruitsTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 1200, batch = "all_fruits")
    public static void everyFruitAbilityRuns(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gl_allfruits"));
        player.setPos(helper.absoluteVec(new Vec3(1.5, 2, 0.5)));
        AbilityState state = AbilityState.of(player);
        state.setFlag(TestFlag.INFINITE_STAMINA, true);
        state.setFlag(TestFlag.NO_COOLDOWNS, true);
        Zombie target = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        target.setNoAi(true);

        List<DevilFruit> fruits = new ArrayList<>();
        GLFruits.REGISTRY.forEach(fruits::add);
        int[] next = {0};
        int[] idle = {0};
        List<String> ran = new ArrayList<>();
        helper.onEachTick(() -> {
            if (next[0] < fruits.size()) {
                DevilFruit fruit = fruits.get(next[0]++);
                runFruit(helper, player, fruit, ran);
            } else if (++idle[0] == 80) {
                // Let projectiles, zones and temporary blocks play out, then clean up.
                for (Entity e : helper.getLevel().getEntitiesOfClass(AbilityProjectile.class,
                        new AABB(player.blockPosition()).inflate(96))) {
                    e.discard();
                }
                target.discard();
                Minions.dismiss(player, null);
                helper.assertTrue(ran.size() >= fruits.size(), "Ran abilities of " + ran.size() + " fruits");
                helper.succeed();
            }
            AbilityManager.tickPlayer(player);
        });
    }

    private static void runFruit(GameTestHelper helper, ServerPlayer player, DevilFruit fruit, List<String> ran) {
        String current = fruit.id() + " (setup)";
        try {
            FruitManager.setFruit(player, fruit);
            FruitManager.setMastery(player, 100);
            List<Ability> abilities = new ArrayList<>(fruit.abilities());
            abilities.addAll(fruit.awakenedAbilities());
            for (Ability ability : abilities) {
                current = fruit.id() + " / " + ability.id();
                exercise(player, ability);
            }
            AbilityManager.endAll(player, Ability.EndReason.INTERRUPTED, false);
            ran.add(fruit.id().toString());
        } catch (Throwable t) {
            GrandLine.LOGGER.error("Ability failed: {}", current, t);
            helper.fail(current + " threw " + t);
        }
    }

    private static void exercise(ServerPlayer player, Ability ability) {
        AbilityManager.Result result = AbilityManager.tryStart(player, ability);
        if (result != AbilityManager.Result.STARTED) {
            return;
        }
        ActivationType type = ability.properties().activation();
        if (type == ActivationType.INSTANT) {
            return;
        }
        AbilityManager.tickPlayer(player);
        int age = Math.min(ability.properties().maxActiveTicks() - 1, Math.max(ability.properties().minChargeTicks() + 2, 20));
        AbilityManager.backdate(player, ability, age);
        AbilityManager.tickPlayer(player);
        if (type == ActivationType.HOLD || type == ActivationType.CHARGE) {
            AbilityManager.release(player, ability);
        } else {
            AbilityManager.press(player, ability);
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void temporaryBlocksRevert(GameTestHelper helper) {
        // Find open air above the (tiny) test structure.
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        for (int i = 0; i < 12 && !helper.getLevel().getBlockState(pos).isAir(); i++) {
            pos = pos.above();
        }
        BlockPos at = pos;
        String found = helper.getLevel().getBlockState(at).toString();
        helper.assertTrue(TempBlocks.place(helper.getLevel(), at, Blocks.ICE.defaultBlockState(), 10),
                "Placed (found " + found + " at " + at.toShortString() + ")");
        helper.assertTrue(helper.getLevel().getBlockState(at).is(Blocks.ICE), "Ice is there");
        helper.assertTrue(TempBlocks.isTemporary(helper.getLevel(), at), "Tracked");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(at).isAir(), "Ice reverted to air");
            helper.assertFalse(TempBlocks.isTemporary(helper.getLevel(), at), "No longer tracked");
            helper.succeed();
        });
    }
}
