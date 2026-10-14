package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.style.BlackLegStyle;
import com.nerodev.grandline.style.BrawlerStyle;
import com.nerodev.grandline.style.GLStyles;
import com.nerodev.grandline.style.GunslingerStyle;
import com.nerodev.grandline.style.StyleManager;
import com.nerodev.grandline.style.SwordsmanStyle;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.weapon.GunItem;
import com.nerodev.grandline.weapon.WeaponManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestones 6 and 7: fighting styles (unarmed and armed), their mastery and scrolls, and the full Rokushiki. */
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

    @GameTest(template = TEMPLATE)
    public static void swordsmanCountsItsSwords(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(SwordsmanStyle.swords(player) == 0, "No sword in hand, no swords");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.KATANA.get()));
        helper.assertTrue(SwordsmanStyle.swords(player) == 1, "One sword in hand");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(GLWeapons.WADO_ICHIMONJI.get()));
        helper.assertTrue(SwordsmanStyle.swords(player) == 2, "Two swords");
        player.getInventory().setItem(player.getInventory().selected == 4 ? 5 : 4, new ItemStack(GLWeapons.SANDAI_KITETSU.get()));
        helper.assertTrue(SwordsmanStyle.swords(player) == 3, "Santoryu: a third sword on the hotbar");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void swordTechniquesNeedSwords(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        StyleManager.learn(player, GLStyles.SWORDSMAN.get());
        StyleManager.equip(player, GLStyles.SWORDSMAN.get());
        Result bare = AbilityManager.tryStart(player, SwordsmanStyle.SHISHI_SONSON.get());
        helper.assertTrue(bare == Result.BLOCKED, "Shishi Sonson without a sword should be refused, got " + bare);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.KATANA.get()));
        GLPlayer.edit(player, data -> data.setMastery(GLStyles.SWORDSMAN.get().masteryKey(), 40));
        Result oneSword = AbilityManager.tryStart(player, SwordsmanStyle.ONI_GIRI.get());
        helper.assertTrue(oneSword == Result.BLOCKED, "Oni Giri with one sword should be refused, got " + oneSword);
        Result phoenix = AbilityManager.tryStart(player, SwordsmanStyle.POUND_PHOENIX.get());
        helper.assertTrue(phoenix == Result.STARTED, "36 Pound Phoenix with a sword should start, got " + phoenix);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everySwordAddsToTheBlow(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        StyleManager.learn(player, GLStyles.SWORDSMAN.get());
        StyleManager.equip(player, GLStyles.SWORDSMAN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.KATANA.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(GLWeapons.CUTLASS.get()));
        LivingIncomingDamageEvent cut = post(zombie, player, 4f);
        helper.assertTrue(Math.abs(cut.getAmount() - 6f) < 0.01f, "Two swords add 2, dealt " + cut.getAmount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void gunslingersReloadFasterAndHitHarder(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(WeaponManager.reloadTicks(player, 25) == 25, "A Brawler reloads at the gun's own pace");
        StyleManager.learn(player, GLStyles.GUNSLINGER.get());
        StyleManager.equip(player, GLStyles.GUNSLINGER.get());
        helper.assertTrue(WeaponManager.reloadTicks(player, 25) == 15, "A Gunslinger reloads 40% faster");
        helper.assertTrue(Math.abs(WeaponManager.gunDamage(player, 8f) - 10f) < 0.01f, "A Gunslinger's bullets hit a quarter harder");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void trickShotsSpendBullets(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        StyleManager.learn(player, GLStyles.GUNSLINGER.get());
        StyleManager.equip(player, GLStyles.GUNSLINGER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.FLINTLOCK.get()));
        Result empty = AbilityManager.tryStart(player, GunslingerStyle.QUICK_DRAW.get());
        helper.assertTrue(empty == Result.BLOCKED, "Quick Draw without bullets should be refused, got " + empty);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(GLWeapons.SEASTONE_BULLET.get(), 2));
        Result shot = AbilityManager.tryStart(player, GunslingerStyle.QUICK_DRAW.get());
        helper.assertTrue(shot == Result.STARTED, "Quick Draw should fire, got " + shot);
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "One bullet spent");
        List<AbilityProjectile> bullets = helper.getLevel().getEntitiesOfClass(AbilityProjectile.class,
                new AABB(player.position(), player.position()).inflate(8), p -> p.getOwner() == player);
        helper.assertTrue(bullets.size() == 1 && bullets.get(0).extra().getBoolean(GunItem.SEASTONE_TAG),
                "One Seastone bullet in flight, found " + bullets.size());
        bullets.forEach(AbilityProjectile::discard);
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
