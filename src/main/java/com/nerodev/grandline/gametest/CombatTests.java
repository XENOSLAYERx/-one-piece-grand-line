package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.combat.CombatManager;
import com.nerodev.grandline.config.GLServerConfig;
import com.nerodev.grandline.effect.GLMobEffects;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 6: combos and finishers, heavy attacks, guarding, parries and counters, dodging. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class CombatTests {
    private static final String TEMPLATE = "empty";

    private CombatTests() {}

    @GameTest(template = TEMPLATE)
    public static void comboChainsIntoFinisher(GameTestHelper helper) {
        ServerPlayer fighter = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        float last = 0f;
        for (int i = 1; i < CombatManager.FINISHER_EVERY; i++) {
            float dealt = CombatManager.strike(fighter, zombie, 4f);
            helper.assertTrue(CombatManager.combo(fighter) == i, "Combo should be " + i + ", is " + CombatManager.combo(fighter));
            helper.assertTrue(dealt >= last, "Each blow in a combo should hit at least as hard as the one before");
            last = dealt;
        }
        float finisher = CombatManager.strike(fighter, zombie, 4f);
        helper.assertTrue(finisher > last, "The finisher should hit hardest (" + finisher + " vs " + last + ")");
        helper.assertTrue(CombatManager.combo(fighter) == 0, "The finisher should end the combo");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void heavyAttackCostsStaminaAndHitsHarder(GameTestHelper helper) {
        ServerPlayer fighter = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        AbilityState state = AbilityState.of(fighter);
        float before = state.stamina(now(fighter));
        fighter.setShiftKeyDown(true);
        float dealt = CombatManager.strike(fighter, zombie, 4f);
        helper.assertTrue(dealt >= 5.99f, "A heavy attack should hit half again as hard, dealt " + dealt);
        float spent = before - state.stamina(now(fighter));
        helper.assertTrue(Math.abs(spent - GLServerConfig.heavyAttackStamina()) < 0.01f, "A heavy attack should cost stamina, cost " + spent);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void parryStopsTheBlowAndOpensACounter(GameTestHelper helper) {
        ServerPlayer defender = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        face(helper, defender, zombie);
        CombatManager.setGuard(defender, true);
        helper.assertTrue(CombatManager.isGuarding(defender), "The guard should be up");

        LivingIncomingDamageEvent hit = post(defender, defender.damageSources().mobAttack(zombie), 6f);
        helper.assertTrue(hit.isCanceled(), "A blow right as the guard goes up should be parried");
        helper.assertTrue(CombatManager.counterReady(defender), "A parry should open a counter window");
        helper.assertTrue(zombie.hasEffect(GLMobEffects.KNOCKED_OUT), "A parried attacker should stagger");

        float plain = 4f;
        float counter = CombatManager.strike(defender, zombie, plain);
        helper.assertTrue(counter >= plain * 1.49f, "The blow after a parry should be a counter, dealt " + counter);
        helper.assertTrue(!CombatManager.counterReady(defender), "A counter should use up the window");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void guardSoaksBlowsFromTheFrontOnly(GameTestHelper helper) {
        ServerPlayer defender = newPlayer(helper);
        Zombie front = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        face(helper, defender, front);
        CombatManager.setGuard(defender, true);
        helper.runAfterDelay(GLServerConfig.parryWindowTicks() + 2, () -> {
            AbilityState state = AbilityState.of(defender);
            float before = state.stamina(now(defender));
            LivingIncomingDamageEvent hit = post(defender, defender.damageSources().mobAttack(front), 10f);
            helper.assertTrue(!hit.isCanceled(), "Past the parry window a blow is guarded, not parried");
            float expected = 10f * (1f - GLServerConfig.guardReduction());
            helper.assertTrue(Math.abs(hit.getAmount() - expected) < 0.01f, "Guard should leave " + expected + ", left " + hit.getAmount());
            helper.assertTrue(state.stamina(now(defender)) < before, "Guarding a blow should cost stamina");

            Zombie behind = helper.spawn(EntityType.ZOMBIE, 1, 2, -2);
            LivingIncomingDamageEvent back = post(defender, defender.damageSources().mobAttack(behind), 10f);
            helper.assertTrue(Math.abs(back.getAmount() - 10f) < 0.01f, "A guard should not stop blows from behind");

            AttackEntityEvent swing = NeoForge.EVENT_BUS.post(new AttackEntityEvent(defender, front));
            helper.assertTrue(swing.isCanceled(), "A guarding player cannot attack");
            CombatManager.setGuard(defender, false);
            helper.assertTrue(!CombatManager.isGuarding(defender), "Releasing the key should lower the guard");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void guardBreaksWithoutStamina(GameTestHelper helper) {
        ServerPlayer defender = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        face(helper, defender, zombie);
        CombatManager.setGuard(defender, true);
        helper.runAfterDelay(GLServerConfig.parryWindowTicks() + 2, () -> {
            AbilityState state = AbilityState.of(defender);
            while (state.spendStamina(5f, now(defender))) {
                // drain
            }
            LivingIncomingDamageEvent hit = post(defender, defender.damageSources().mobAttack(zombie), 10f);
            helper.assertTrue(Math.abs(hit.getAmount() - 10f) < 0.01f, "A guard without stamina should not stop the blow");
            helper.assertTrue(!CombatManager.isGuarding(defender), "A guard without stamina should break");
            CombatManager.setGuard(defender, true);
            helper.assertTrue(!CombatManager.isGuarding(defender), "A broken guard cannot be raised again at once");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void dodgeDashesWithInvulnerableFrames(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 1.5)), 0, 0);
        AbilityState state = AbilityState.of(player);
        float before = state.stamina(now(player));
        helper.assertTrue(CombatManager.dodge(player, 0, 1), "A rested player should be able to dodge");
        helper.assertTrue(state.isEvading(now(player)), "A dodge should grant invulnerable frames");
        helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 0.5, "A dodge should dash");
        float spent = before - state.stamina(now(player));
        helper.assertTrue(Math.abs(spent - GLServerConfig.dodgeStamina()) < 0.01f, "A dodge should cost stamina, cost " + spent);
        helper.assertTrue(!CombatManager.dodge(player, 0, 1), "Dodging again at once should be refused");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Puts {@code player} two blocks from {@code target}, facing it. */
    private static void face(GameTestHelper helper, ServerPlayer player, LivingEntity target) {
        Vec3 at = target.position().add(0, 0, -2);
        player.moveTo(at.x, at.y, at.z, 0f, 0f);
        player.setYHeadRot(0f);
    }

    private static LivingIncomingDamageEvent post(LivingEntity victim, DamageSource source, float amount) {
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(victim, new DamageContainer(source, amount)));
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }

    private static long now(ServerPlayer player) {
        return AbilityManager.now(player);
    }
}
