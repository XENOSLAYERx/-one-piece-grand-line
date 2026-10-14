package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.fruit.LogiaFruit;
import com.nerodev.grandline.fruit.flame.FlameAbilities;
import com.nerodev.grandline.haki.HakiAbilities;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.weapon.GLWeapons;
import com.nerodev.grandline.weapon.WeaponManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 7: weapons, guns and Seastone. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class WeaponTests {
    private static final String TEMPLATE = "empty";

    private WeaponTests() {}

    @GameTest(template = TEMPLATE)
    public static void seastoneJitteStrikesALogia(GameTestHelper helper) {
        ServerPlayer logia = newPlayer(helper);
        FruitManager.setFruit(logia, GLFruits.FLAME.get());
        ServerPlayer marine = newPlayer(helper);

        LivingIncomingDamageEvent fist = incoming(logia, marine.damageSources().playerAttack(marine), 5f);
        GLFruits.FLAME.get().onIncomingDamage(logia, fist);
        helper.assertTrue(fist.isCanceled(), "A bare fist passes through fire");

        marine.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.SEASTONE_JITTE.get()));
        LivingIncomingDamageEvent jitte = incoming(logia, marine.damageSources().playerAttack(marine), 5f);
        GLFruits.FLAME.get().onIncomingDamage(logia, jitte);
        helper.assertFalse(jitte.isCanceled(), "A Seastone jitte strikes the Logia's real body");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void seastoneBulletsStrikeALogia(GameTestHelper helper) {
        ServerPlayer shooter = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        AbilityProjectile lead = AbilityProjectile.spawn(shooter, GLWeapons.BULLET_SHOT, at, Vec3.ZERO, 0.15f, 0, 7f, 5);
        AbilityProjectile seastone = AbilityProjectile.spawn(shooter, GLWeapons.SEASTONE_BULLET_SHOT, at, Vec3.ZERO, 0.15f, 0, 7f, 5);
        helper.assertFalse(LogiaFruit.canTouch(AbilityDamage.source(shooter, AbilityDamage.STRIKE, lead)),
                "A lead bullet passes through a Logia");
        helper.assertTrue(LogiaFruit.canTouch(AbilityDamage.source(shooter, AbilityDamage.STRIKE, seastone)),
                "A Seastone bullet strikes a Logia");
        lead.discard();
        seastone.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void seastoneCuffsSealTheFruit(GameTestHelper helper) {
        ServerPlayer pirate = newPlayer(helper);
        FruitManager.setFruit(pirate, GLFruits.FLAME.get());
        ServerPlayer marine = newPlayer(helper);
        ItemStack cuffs = new ItemStack(GLWeapons.SEASTONE_CUFFS.get(), 2);
        marine.setItemInHand(InteractionHand.MAIN_HAND, cuffs);
        cuffs.interactLivingEntity(marine, pirate, InteractionHand.MAIN_HAND);

        helper.assertTrue(pirate.hasEffect(GLMobEffects.FRUIT_SEALED), "Cuffs seal the fruit");
        helper.assertTrue(cuffs.getCount() == 1, "One pair of cuffs is used up");
        Result result = AbilityManager.tryStart(pirate, FlameAbilities.FIRE_FIST.get());
        helper.assertTrue(result == Result.BLOCKED, "Fruit powers are refused while cuffed, got " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void seastoneBarsSealWhoeverIsAgainstThem(GameTestHelper helper) {
        BlockPos bars = new BlockPos(2, 2, 1);
        helper.setBlock(bars, GLWeapons.SEASTONE_BARS.get());
        ServerPlayer prisoner = newPlayer(helper);
        FruitManager.setFruit(prisoner, GLFruits.FLAME.get());
        Vec3 cell = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        prisoner.setPos(cell.x, cell.y, cell.z);
        ServerPlayer free = newPlayer(helper);
        Vec3 away = helper.absoluteVec(new Vec3(1.5, 2, 4.5));
        free.setPos(away.x, away.y, away.z);

        helper.assertTrue(WeaponManager.touchingSeastone(prisoner), "Standing against Seastone bars");
        helper.assertFalse(WeaponManager.touchingSeastone(free), "Three blocks away is out of reach");
        free.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.SEASTONE_INGOT.get()));
        helper.assertTrue(WeaponManager.touchingSeastone(free), "Holding a Seastone ingot");

        WeaponManager.sap(prisoner);
        helper.assertTrue(prisoner.hasEffect(GLMobEffects.FRUIT_SEALED), "Seastone contact seals the fruit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void gunsNeedBullets(GameTestHelper helper) {
        ServerPlayer shooter = newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        shooter.setPos(at.x, at.y, at.z);
        ItemStack gun = new ItemStack(GLWeapons.FLINTLOCK.get());
        shooter.setItemInHand(InteractionHand.MAIN_HAND, gun);

        gun.getItem().use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
        helper.assertTrue(shots(helper, shooter).isEmpty(), "No bullets, no shot");

        shooter.getInventory().add(new ItemStack(GLWeapons.IRON_BULLET.get(), 2));
        gun.getItem().use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
        List<AbilityProjectile> fired = shots(helper, shooter);
        helper.assertTrue(fired.size() == 1, "One bullet fired, found " + fired.size());
        helper.assertTrue(shooter.getInventory().countItem(GLWeapons.IRON_BULLET.get()) == 1, "The shot used one bullet");
        helper.assertTrue(shooter.getCooldowns().isOnCooldown(GLWeapons.FLINTLOCK.get()), "The gun has to be reloaded");
        fired.forEach(AbilityProjectile::discard);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void kitetsuCutsBleed(GameTestHelper helper) {
        ServerPlayer swordsman = newPlayer(helper);
        swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.SANDAI_KITETSU.get()));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        zombie.hurt(swordsman.damageSources().playerAttack(swordsman), 4f);
        helper.assertTrue(zombie.hasEffect(GLMobEffects.BLEEDING), "A Kitetsu cut bleeds");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wadoCutsThroughArmour(GameTestHelper helper) {
        ServerPlayer swordsman = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.KATANA.get()));
        LivingIncomingDamageEvent plain = post(zombie, swordsman.damageSources().playerAttack(swordsman), 6f);
        swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.WADO_ICHIMONJI.get()));
        LivingIncomingDamageEvent wado = post(zombie, swordsman.damageSources().playerAttack(swordsman), 6f);
        float expected = 6f + zombie.getArmorValue() * 0.25f;
        helper.assertTrue(Math.abs(plain.getAmount() - 6f) < 0.01f, "A plain katana adds nothing, dealt " + plain.getAmount());
        helper.assertTrue(Math.abs(wado.getAmount() - expected) < 0.01f, "Wado adds a quarter of the armour, dealt " + wado.getAmount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blackBladesTakeHakiBest(GameTestHelper helper) {
        ServerPlayer swordsman = newPlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 3);
        HakiManager.unlock(swordsman, HakiType.ARMAMENT, false);
        Result coat = AbilityManager.tryStart(swordsman, HakiAbilities.ARMAMENT.get());
        helper.assertTrue(coat == Result.STARTED, "Armament should start, got " + coat);
        float m = HakiManager.armamentMultiplier(swordsman);

        swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.KATANA.get()));
        LivingIncomingDamageEvent katana = post(zombie, swordsman.damageSources().playerAttack(swordsman), 6f);
        swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GLWeapons.SHUSUI.get()));
        LivingIncomingDamageEvent shusui = post(zombie, swordsman.damageSources().playerAttack(swordsman), 6f);
        helper.assertTrue(Math.abs(katana.getAmount() - 6f * m) < 0.05f, "Armament through a katana, dealt " + katana.getAmount());
        helper.assertTrue(Math.abs(shusui.getAmount() - 6f * (1f + (m - 1f) * 1.3f)) < 0.05f,
                "Shusui's black blade takes Haki better, dealt " + shusui.getAmount());
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static List<AbilityProjectile> shots(GameTestHelper helper, ServerPlayer shooter) {
        return helper.getLevel().getEntitiesOfClass(AbilityProjectile.class, new AABB(shooter.position(), shooter.position()).inflate(8),
                p -> p.getOwner() == shooter);
    }

    private static LivingIncomingDamageEvent incoming(ServerPlayer victim, DamageSource source, float amount) {
        return new LivingIncomingDamageEvent(victim, new DamageContainer(source, amount));
    }

    private static LivingIncomingDamageEvent post(Zombie victim, DamageSource source, float amount) {
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(victim, new DamageContainer(source, amount)));
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "gl_" + id.toString().substring(0, 8)));
    }
}
