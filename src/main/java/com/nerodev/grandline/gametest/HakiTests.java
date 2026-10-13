package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.haki.HakiAbilities;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 5: Haki unlocking and levels, Armament against Logia, hardening, Future Sight, Conqueror's. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class HakiTests {
    private static final String TEMPLATE = "empty";

    private HakiTests() {}

    @GameTest(template = TEMPLATE)
    public static void unlockingGrantsTechniquesByLevel(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertFalse(AbilityAccess.canUse(player, HakiAbilities.ARMAMENT.get()), "No Haki yet");
        HakiManager.unlock(player, HakiType.ARMAMENT, false);
        helper.assertTrue(AbilityAccess.canUse(player, HakiAbilities.ARMAMENT.get()), "Arm coating at level 0");
        helper.assertFalse(AbilityAccess.canUse(player, HakiAbilities.FULL_BODY.get()), "Full body needs level 35");
        HakiManager.setLevel(player, HakiType.ARMAMENT, 40);
        helper.assertTrue(AbilityAccess.canUse(player, HakiAbilities.FULL_BODY.get()), "Full body at 40");
        helper.assertFalse(AbilityAccess.canUse(player, HakiAbilities.EMISSION.get()), "Emission needs 70");
        helper.assertTrue(AbilityState.of(player).equipped().contains(HakiAbilities.ARMAMENT.getId()), "Coating auto-equipped");
        helper.assertTrue(HakiManager.remove(player, HakiType.ARMAMENT), "Removed");
        helper.assertFalse(AbilityAccess.canUse(player, HakiAbilities.ARMAMENT.get()), "Gone after removal");
        helper.assertTrue(HakiManager.level(player, HakiType.ARMAMENT) == 40, "Level kept after removal");
        helper.succeed();
    }

    /** The milestone's definition of done: Armament Haki lets blows land on the Logia test fruit. */
    @GameTest(template = TEMPLATE)
    public static void armamentStrikesALogia(GameTestHelper helper) {
        ServerPlayer logia = newPlayer(helper);
        ServerPlayer fighter = newPlayer(helper);
        FruitManager.setFruit(logia, GLFruits.FLAME.get());
        var flame = GLFruits.FLAME.get();

        LivingIncomingDamageEvent bare = event(logia, logia.damageSources().playerAttack(fighter), 6f);
        flame.onIncomingDamage(logia, bare);
        helper.assertTrue(bare.isCanceled(), "A bare fist passes through fire");

        HakiManager.unlock(fighter, HakiType.ARMAMENT, false);
        helper.assertTrue(AbilityManager.tryStart(fighter, HakiAbilities.ARMAMENT.get()) == Result.STARTED, "Coating on");
        helper.assertTrue(PublicState.of(fighter).hasHaki(PublicState.HAKI_ARM), "Coating is visible to others");
        LivingIncomingDamageEvent armed = event(logia, logia.damageSources().playerAttack(fighter), 6f);
        flame.onIncomingDamage(logia, armed);
        helper.assertFalse(armed.isCanceled(), "An Armament-coated fist strikes the Logia");

        AbilityManager.press(fighter, HakiAbilities.ARMAMENT.get());
        helper.assertFalse(PublicState.of(fighter).hasHaki(PublicState.HAKI_ARM), "Coating off when toggled");
        LivingIncomingDamageEvent again = event(logia, logia.damageSources().playerAttack(fighter), 6f);
        flame.onIncomingDamage(logia, again);
        helper.assertTrue(again.isCanceled(), "Without Haki the fist passes through again");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void armamentHitsHarderAndEmissionPierces(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);
        HakiManager.setLevel(player, HakiType.ARMAMENT, 70);
        AbilityState.of(player).setFlag(TestFlag.INFINITE_HAKI, true);

        LivingIncomingDamageEvent plain = post(zombie, player.damageSources().playerAttack(player), 4f);
        helper.assertTrue(plain.getAmount() == 4f, "No Haki, no bonus");

        AbilityManager.tryStart(player, HakiAbilities.ARMAMENT.get());
        LivingIncomingDamageEvent coated = post(zombie, player.damageSources().playerAttack(player), 4f);
        float expected = 4f * HakiManager.armamentMultiplier(player);
        helper.assertTrue(Math.abs(coated.getAmount() - expected) < 0.01f, "Coated blow x" + HakiManager.armamentMultiplier(player)
                + ", got " + coated.getAmount());

        helper.assertTrue(AbilityManager.tryStart(player, HakiAbilities.EMISSION.get()) == Result.STARTED, "Emission primed");
        helper.assertTrue(HakiManager.emissionPrimed(player), "Primed");
        LivingIncomingDamageEvent emission = post(zombie, player.damageSources().playerAttack(player), 4f);
        helper.assertTrue(emission.getAmount() >= expected + HakiManager.emissionBonus(player), "Emission adds internal damage, got "
                + emission.getAmount());
        helper.assertFalse(HakiManager.emissionPrimed(player), "Emission is spent on the blow");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullBodyHardeningCutsDamage(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        HakiManager.setLevel(player, HakiType.ARMAMENT, 40);
        helper.assertTrue(AbilityManager.tryStart(player, HakiAbilities.FULL_BODY.get()) == Result.STARTED, "Hardened");
        helper.assertTrue(HakiManager.armamentActive(player), "Full body counts as Armament");
        LivingIncomingDamageEvent hit = post(player, player.damageSources().mobAttack(zombie), 10f);
        float expected = 10f * (1f - HakiManager.hardeningReduction(player));
        helper.assertTrue(Math.abs(hit.getAmount() - expected) < 0.01f, "Damage cut to " + expected + ", got " + hit.getAmount());
        LivingIncomingDamageEvent magic = post(player, player.damageSources().magic(), 10f);
        helper.assertTrue(magic.getAmount() == 10f, "Hardening does not stop damage that ignores armour");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void futureSightReadsEveryAttack(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        HakiManager.setLevel(player, HakiType.OBSERVATION, 60);
        helper.assertTrue(AbilityManager.tryStart(player, HakiAbilities.FUTURE_SIGHT.get()) == Result.STARTED, "Future Sight on");
        float before = AbilityState.of(player).stamina(AbilityManager.now(player));
        for (int i = 0; i < 3; i++) {
            LivingIncomingDamageEvent hit = post(player, player.damageSources().mobAttack(zombie), 5f);
            helper.assertTrue(hit.isCanceled(), "Attack " + (i + 1) + " was foreseen and dodged");
        }
        helper.assertTrue(AbilityState.of(player).stamina(AbilityManager.now(player)) < before, "Each dodge costs stamina");
        LivingIncomingDamageEvent fall = post(player, player.damageSources().fall(), 5f);
        helper.assertFalse(fall.isCanceled(), "Falling has no attacker to read");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void conquerorsKnocksOutTheWeak(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        player.setPos(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        Chicken chicken = helper.spawn(EntityType.CHICKEN, 3, 2, 3);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);
        HakiManager.setLevel(player, HakiType.CONQUERORS, 0);

        // A quick, weak burst (smallest radius) so creatures from neighbouring tests stay out of range.
        int knocked = HakiManager.conquerorsWave(player, 0.2f);
        helper.assertTrue(knocked >= 1, "The chicken should faint, got " + knocked);
        helper.assertTrue(chicken.hasEffect(GLMobEffects.KNOCKED_OUT), "Chicken knocked out");
        helper.assertFalse(zombie.hasEffect(GLMobEffects.KNOCKED_OUT), "A zombie (20 hp) resists a level 0 king");
        helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "But it is intimidated");

        // Knocked out creatures cannot fight back.
        HakiManager.knockOut(zombie, 40);
        LivingIncomingDamageEvent bite = post(player, player.damageSources().mobAttack(zombie), 4f);
        helper.assertTrue(bite.isCanceled(), "An unconscious zombie cannot attack");

        HakiManager.setLevel(player, HakiType.CONQUERORS, 100);
        helper.assertTrue(HakiManager.knockoutThreshold(player, 1f) >= 99.9f, "A master king floors 100 hp foes");
        chicken.discard();
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fightingAwakensArmament(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        DamageSource punch = player.damageSources().playerAttack(player);
        int needed = GLServerConfig.armamentTrainingHits();
        for (int i = 0; i < needed - 1; i++) {
            NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(zombie, new DamageContainer(punch, 2f)));
        }
        helper.assertFalse(HakiManager.has(player, HakiType.ARMAMENT), "Not yet");
        NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(zombie, new DamageContainer(punch, 2f)));
        helper.assertTrue(HakiManager.has(player, HakiType.ARMAMENT), "Armament awakens after " + needed + " blows");
        helper.assertTrue(GLPlayer.data(player).counter(HakiManager.ARMAMENT_HITS) == needed, "Counter saved");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void infiniteHakiIsFree(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        HakiManager.unlock(player, HakiType.ARMAMENT, false);
        AbilityState state = AbilityState.of(player);
        state.setFlag(TestFlag.INFINITE_HAKI, true);
        long now = AbilityManager.now(player);
        float before = state.stamina(now);
        AbilityManager.tryStart(player, HakiAbilities.ARMAMENT.get());
        AbilityManager.press(player, HakiAbilities.ARMAMENT.get());
        helper.assertTrue(state.stamina(now) == before, "No stamina spent");
        helper.assertFalse(state.isOnCooldown(HakiAbilities.ARMAMENT.getId(), now), "No cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hakiCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player);
        run(helper, server, source, "onepiece haki");
        run(helper, server, source, "onepiece haki unlock observation");
        helper.assertTrue(HakiManager.has(player, HakiType.OBSERVATION), "haki unlock observation");
        run(helper, server, source, "onepiece haki set armament 55");
        helper.assertTrue(HakiManager.level(player, HakiType.ARMAMENT) == 55, "haki set armament 55");
        run(helper, server, source, "onepiece mastery set conquerors 30");
        helper.assertTrue(HakiManager.has(player, HakiType.CONQUERORS), "mastery set conquerors unlocks it");
        run(helper, server, source, "onepiece mastery max");
        helper.assertTrue(HakiManager.level(player, HakiType.OBSERVATION) == 100, "mastery max raises Haki");
        run(helper, server, source, "onepiece haki infinite");
        helper.assertTrue(AbilityState.of(player).hasFlag(TestFlag.INFINITE_HAKI), "haki infinite");
        run(helper, server, source, "onepiece haki infinite off");
        helper.assertFalse(AbilityState.of(player).hasFlag(TestFlag.INFINITE_HAKI), "haki infinite off");
        run(helper, server, source, "onepiece haki remove conquerors");
        helper.assertFalse(HakiManager.has(player, HakiType.CONQUERORS), "haki remove");
        run(helper, server, source, "onepiece unlock haki");
        helper.assertTrue(HakiManager.has(player, HakiType.CONQUERORS), "unlock haki");
        run(helper, server, source, "onepiece haki info");
        run(helper, server, source, "onepiece reset haki confirm");
        for (HakiType type : HakiType.values()) {
            helper.assertFalse(HakiManager.has(player, type), "reset haki clears " + type.getSerializedName());
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static LivingIncomingDamageEvent event(LivingEntity victim, DamageSource source, float amount) {
        return new LivingIncomingDamageEvent(victim, new DamageContainer(source, amount));
    }

    /** Fires the event through every listener, as a real hit would. */
    private static LivingIncomingDamageEvent post(LivingEntity victim, DamageSource source, float amount) {
        return NeoForge.EVENT_BUS.post(event(victim, source, amount));
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }

    private static void run(GameTestHelper helper, MinecraftServer server, CommandSourceStack source, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            helper.fail("/" + command + " failed: " + e.getMessage());
        }
    }
}
