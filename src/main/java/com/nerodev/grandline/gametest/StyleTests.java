package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.style.BlackLegStyle;
import com.nerodev.grandline.style.BrawlerStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.StyleManager;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 6: fighting styles, their mastery and scrolls, and the full Rokushiki. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class StyleTests {
    private static final String TEMPLATE = "empty";

    private StyleTests() {}

    @GameTest(template = TEMPLATE)
    public static void everyoneStartsAsABrawler(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(StyleManager.style(player) == GLStyles.BRAWLER.get(), "A new player should fight as a Brawler");
        helper.assertTrue(AbilityAccess.canUse(player, BrawlerStyle.HAYMAKER.get()), "Haymaker needs no mastery");
        helper.assertTrue(!AbilityAccess.canUse(player, BrawlerStyle.HEADBUTT.get()), "Headbutt needs style mastery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stylesMustBeLearnedBeforeEquipping(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(StyleManager.equip(player, GLStyles.BLACK_LEG.get()).isPresent(), "An unlearned style cannot be equipped");
        StyleManager.learn(player, GLStyles.BLACK_LEG.get());
        helper.assertTrue(StyleManager.equip(player, GLStyles.BLACK_LEG.get()).isEmpty(), "A learned style can be equipped");
        helper.assertTrue(StyleManager.style(player) == GLStyles.BLACK_LEG.get(), "Black Leg should be the style now");
        helper.assertTrue(AbilityAccess.canUse(player, BlackLegStyle.MOUTON_SHOT.get()), "Black Leg grants its moves");
        helper.assertTrue(!AbilityAccess.canUse(player, BrawlerStyle.HAYMAKER.get()), "Switching style takes the old moves away");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void scrollTeachesAndEquipsAStyle(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        ItemStack scroll = new ItemStack(GLItems.BLACK_LEG_SCROLL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        scroll.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GLPlayer.data(player).isUnlocked(UnlockCategory.STYLE, GLStyles.BLACK_LEG.getId()), "Reading the scroll teaches the style");
        helper.assertTrue(StyleManager.style(player) == GLStyles.BLACK_LEG.get(), "Reading the scroll equips the style");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "The scroll is used up");

        ItemStack rokushiki = new ItemStack(GLItems.SHIGAN_SCROLL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rokushiki);
        rokushiki.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(AbilityAccess.canUse(player, GLAbilities.SHIGAN.get()), "A Rokushiki scroll teaches its technique");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void styleMovesTrainStyleMastery(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        StyleManager.learn(player, GLStyles.BLACK_LEG.get());
        StyleManager.equip(player, GLStyles.BLACK_LEG.get());
        String key = GLStyles.BLACK_LEG.get().masteryKey();
        double before = GLPlayer.data(player).mastery(key);
        Result result = AbilityManager.tryStart(player, BlackLegStyle.MOUTON_SHOT.get());
        helper.assertTrue(result == Result.STARTED, "Mouton Shot should start, got " + result);
        helper.assertTrue(GLPlayer.data(player).mastery(key) > before, "Using a style move should raise style mastery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blackLegKicksHitHarder(GameTestHelper helper) {
        ServerPlayer fighter = newPlayer(helper);
        ServerPlayer cook = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        LivingIncomingDamageEvent brawler = post(zombie, fighter, 4f);
        StyleManager.learn(cook, GLStyles.BLACK_LEG.get());
        StyleManager.equip(cook, GLStyles.BLACK_LEG.get());
        LivingIncomingDamageEvent kick = post(zombie, cook, 4f);
        helper.assertTrue(Math.abs(brawler.getAmount() - 5f) < 0.01f, "A Brawler's bare fist adds 1, dealt " + brawler.getAmount());
        helper.assertTrue(Math.abs(kick.getAmount() - 7f) < 0.01f, "A Black Leg kick adds 3, dealt " + kick.getAmount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rokuoganNeedsTheOtherSix(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        GLPlayer.edit(player, data -> data.unlock(UnlockCategory.ABILITY, GLAbilities.ROKUOGAN.getId()));
        Result alone = AbilityManager.tryStart(player, GLAbilities.ROKUOGAN.get());
        helper.assertTrue(alone == Result.BLOCKED, "Rokuogan without the other six should be refused, got " + alone);
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    /** A real swing: the attack event vanilla fires first, then the blow landing. */
    private static LivingIncomingDamageEvent post(Zombie victim, ServerPlayer attacker, float amount) {
        NeoForge.EVENT_BUS.post(new AttackEntityEvent(attacker, victim));
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(victim,
                new DamageContainer(victim.damageSources().playerAttack(attacker), amount)));
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }
}
