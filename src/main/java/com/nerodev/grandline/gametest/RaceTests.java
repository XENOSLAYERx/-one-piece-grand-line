package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PublicState;
import com.nerodev.grandline.race.RaceAbilities;
import com.nerodev.grandline.race.RaceManager;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 8: races, their bonuses, abilities and traits, and the Mink's Sulong form. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class RaceTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation HUMAN = GrandLine.id("human");
    private static final ResourceLocation GIANT = GrandLine.id("giant");
    private static final ResourceLocation MINK = GrandLine.id("mink");
    private static final ResourceLocation SKYPIEAN = GrandLine.id("skypiean");

    private RaceTests() {}

    @GameTest(template = TEMPLATE)
    public static void raceBonusesApplyAndComeOff(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        RaceManager.setRace(player, GIANT);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 2.0) < 1.0E-6, "A Giant is twice the size");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MAX_HEALTH) - 40.0) < 1.0E-6, "A Giant has 40 health");
        helper.assertTrue(PublicState.of(player).race().equals(GIANT), "Others see the new race");
        RaceManager.setRace(player, HUMAN);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 1.0) < 1.0E-6, "Back to human size");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MAX_HEALTH) - 20.0) < 1.0E-6, "Back to human health");
        RaceManager.setRace(player, SKYPIEAN);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - 0.5) < 1.0E-6,
                "A Skypiean takes half fall damage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void racesBringTheirAbilities(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(AbilityAccess.canUse(player, RaceAbilities.SECOND_WIND.get()), "Humans can find a second wind");
        helper.assertFalse(AbilityAccess.canUse(player, RaceAbilities.ELECTRO_STRIKE.get()), "Humans have no Electro");
        RaceManager.setRace(player, MINK);
        helper.assertTrue(AbilityAccess.canUse(player, RaceAbilities.ELECTRO_STRIKE.get()), "Minks use Electro");
        helper.assertTrue(AbilityState.of(player).equipped().contains(RaceAbilities.ELECTRO_STRIKE.getId()), "Electro is put in a slot");
        helper.assertFalse(AbilityAccess.canUse(player, RaceAbilities.SECOND_WIND.get()), "A Mink loses the human ability");
        helper.assertTrue(RaceManager.masteryMultiplier(player) == 1f, "Minks train at the normal pace");
        RaceManager.setRace(player, HUMAN);
        helper.assertTrue(RaceManager.masteryMultiplier(player) > 1f, "Humans train a little faster");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void minkFistsCarryElectro(GameTestHelper helper) {
        ServerPlayer human = newPlayer(helper);
        ServerPlayer mink = newPlayer(helper);
        RaceManager.setRace(mink, MINK);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        LivingIncomingDamageEvent plain = swing(zombie, human, 4f);
        LivingIncomingDamageEvent electro = swing(zombie, mink, 4f);
        helper.assertTrue(Math.abs(electro.getAmount() - plain.getAmount() - 1.5f) < 0.01f,
                "A Mink's fist adds 1.5 Electro, dealt " + electro.getAmount() + " against " + plain.getAmount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sulongNeedsTheFullMoon(GameTestHelper helper) {
        ServerPlayer mink = newPlayer(helper);
        RaceManager.setRace(mink, MINK);
        GLPlayer.edit(mink, data -> data.setMastery(RaceManager.masteryKey(MINK), 40));
        // High in the open sky, so nothing hides the moon.
        Vec3 sky = helper.absoluteVec(new Vec3(1.5, 0, 1.5));
        mink.setPos(sky.x, mink.level().getMaxBuildHeight() - 2, sky.z);
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        try {
            level.setDayTime(6000);
            Result day = AbilityManager.tryStart(mink, RaceAbilities.SULONG_FORM.get());
            helper.assertTrue(day == Result.BLOCKED, "No Sulong by day, got " + day);
            level.setDayTime(18000);
            Result fullMoon = AbilityManager.tryStart(mink, RaceAbilities.SULONG_FORM.get());
            helper.assertTrue(fullMoon == Result.STARTED, "Sulong under the full moon, got " + fullMoon);
            helper.assertTrue(TransformationManager.isActive(mink, RaceAbilities.SULONG_BODY.get()), "The Sulong body is on");
            helper.assertTrue(RaceAbilities.inSulong(mink), "In Sulong");
            AbilityManager.end(mink, RaceAbilities.SULONG_FORM.get(), Ability.EndReason.TOGGLED_OFF);
            helper.assertFalse(TransformationManager.isActive(mink, RaceAbilities.SULONG_BODY.get()), "Sulong ends with the toggle");
        } finally {
            level.setDayTime(before);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static LivingIncomingDamageEvent swing(Zombie victim, ServerPlayer attacker, float amount) {
        NeoForge.EVENT_BUS.post(new AttackEntityEvent(attacker, victim));
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(victim,
                new DamageContainer(victim.damageSources().playerAttack(attacker), amount)));
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }
}
