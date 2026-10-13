package com.nerodev.grandline.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityAccess;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityState;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.ability.TestFlag;
import com.nerodev.grandline.data.DeathMode;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.UnlockCategory;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.fruit.FruitManager;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.fruit.flame.FlameAbilities;
import com.nerodev.grandline.fruit.gum.GumAbilities;
import com.nerodev.grandline.fruit.gum.GumFruit;
import com.nerodev.grandline.fruit.leopard.LeopardAbilities;
import com.nerodev.grandline.item.GLItems;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.TransformationManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 4: Devil Fruits, mastery, awakening, transformations, passives and fruit projectiles. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class FruitTests {
    private static final String TEMPLATE = "empty";

    private FruitTests() {}

    @GameTest(template = TEMPLATE)
    public static void eatingGrantsThePower(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        ItemStack fruit = new ItemStack(GLItems.GUM_GUM_FRUIT.get());
        GLItems.GUM_GUM_FRUIT.get().finishUsingItem(fruit, helper.getLevel(), player);

        helper.assertTrue(GLPlayer.data(player).fruit().map(GLFruits.GUM_GUM.getId()::equals).orElse(false), "Ate Gum-Gum");
        helper.assertTrue(AbilityAccess.canUse(player, GumAbilities.PISTOL.get()), "Pistol is usable at 0 mastery");
        helper.assertFalse(AbilityAccess.canUse(player, GumAbilities.GATLING.get()), "Gatling needs mastery 10");
        helper.assertFalse(AbilityAccess.canUse(player, GumAbilities.GEAR_FIFTH.get()), "Gear Fifth needs awakening");
        helper.assertTrue(AbilityState.of(player).equipped().contains(GumAbilities.PISTOL.getId()), "Pistol auto-equipped");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oneFruitPerBody(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        FruitManager.eat(player, GLFruits.FLAME.get());
        // Normal mode: a second fruit is fatal (the fake player is invulnerable, so it survives to be checked).
        helper.assertTrue(FruitManager.eat(player, GLFruits.GUM_GUM.get()) == FruitManager.EatResult.KILLED,
                "Second fruit in normal mode should kill");
        helper.assertTrue(FruitManager.fruitOf(player) == GLFruits.FLAME.get(), "The first fruit stays");

        GLPlayer.data(player).setDeathMode(DeathMode.CREATOR);
        helper.assertTrue(FruitManager.eat(player, GLFruits.GUM_GUM.get()) == FruitManager.EatResult.REPLACED,
                "Creator death mode swaps fruits");
        helper.assertTrue(FruitManager.fruitOf(player) == GLFruits.GUM_GUM.get(), "Fruit replaced");
        helper.assertFalse(AbilityState.of(player).equipped().contains(FlameAbilities.FIRE_FIST.getId()),
                "Old fruit abilities leave the slots");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void masteryUnlocksAndAwakens(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        FruitManager.setFruit(player, GLFruits.GUM_GUM.get());
        FruitManager.setMastery(player, 30);
        helper.assertTrue(AbilityAccess.canUse(player, GumAbilities.GATLING.get()), "Gatling at 30");
        helper.assertTrue(AbilityAccess.canUse(player, GumAbilities.GEAR_SECOND.get()), "Gear Second at 30");
        helper.assertFalse(AbilityAccess.canUse(player, GumAbilities.GIANT_PISTOL.get()), "Giant Pistol needs 50");
        helper.assertFalse(FruitManager.isAwakened(player), "Not awakened yet");

        FruitManager.setMastery(player, 100);
        helper.assertTrue(FruitManager.isAwakened(player), "100 mastery awakens the fruit");
        helper.assertTrue(AbilityAccess.canUse(player, GumAbilities.GEAR_FIFTH.get()), "Gear Fifth after awakening");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void usingAbilitiesTrainsMastery(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        FruitManager.setFruit(player, GLFruits.GUM_GUM.get());
        AbilityState.of(player).setFlag(TestFlag.NO_COOLDOWNS, true);
        AbilityState.of(player).setFlag(TestFlag.INFINITE_STAMINA, true);
        for (int i = 0; i < 4; i++) {
            AbilityManager.tryStart(player, GumAbilities.PISTOL.get());
        }
        double mastery = GLPlayer.data(player).mastery(GumAbilities.MASTERY);
        helper.assertTrue(mastery > 2.0, "Four pistols should train mastery, got " + mastery);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fruitAbilitiesNeedTheFruit(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        GLPlayer.data(player).unlock(UnlockCategory.ABILITY, GumAbilities.PISTOL.getId());
        helper.assertFalse(AbilityAccess.canUse(player, GumAbilities.PISTOL.get()), "No fruit, no Gum powers");
        helper.assertTrue(AbilityManager.tryStart(player, GumAbilities.PISTOL.get()) == Result.NOT_USABLE, "Refused");

        FruitManager.setFruit(player, GLFruits.GUM_GUM.get());
        GLPlayer.data(player).unlock(UnlockCategory.ABILITY, GumAbilities.GIANT_PISTOL.getId());
        helper.assertTrue(AbilityAccess.canUse(player, GumAbilities.GIANT_PISTOL.get()), "Unlock waives mastery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void transformationsAreExclusive(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        TransformationManager.start(player, GLTransformations.GEAR_SECOND.get());
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > baseSpeed, "Gear Second is faster");
        TransformationManager.start(player, GLTransformations.LEOPARD_HYBRID.get());
        helper.assertTrue(TransformationManager.isActive(player, GLTransformations.LEOPARD_HYBRID.get()), "Hybrid active");
        helper.assertFalse(TransformationManager.isActive(player, GLTransformations.GEAR_SECOND.get()), "Gear Second ended");
        helper.assertTrue(player.getAttributeValue(Attributes.SCALE) > 1.0, "Hybrid is bigger");
        TransformationManager.end(player);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 1.0E-6, "Speed restored");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 1.0) < 1.0E-6, "Scale restored");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void leopardFormsSwitchCleanly(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        FruitManager.setFruit(player, GLFruits.LEOPARD.get());
        AbilityState state = AbilityState.of(player);
        state.setFlag(TestFlag.NO_COOLDOWNS, true);

        helper.assertTrue(AbilityManager.tryStart(player, LeopardAbilities.POUNCE.get()) == Result.BLOCKED, "Pounce needs a form");
        AbilityManager.tryStart(player, LeopardAbilities.HYBRID_FORM.get());
        helper.assertTrue(TransformationManager.isActive(player, GLTransformations.LEOPARD_HYBRID.get()), "Hybrid on");
        AbilityManager.tryStart(player, LeopardAbilities.BEAST_FORM.get());
        helper.assertTrue(TransformationManager.isActive(player, GLTransformations.LEOPARD_BEAST.get()), "Beast on");
        helper.assertFalse(state.isActive(LeopardAbilities.HYBRID_FORM.getId()), "Hybrid toggle ended with its form");
        helper.assertTrue(player.getBbHeight() < 1.2f, "Beast hitbox is low: " + player.getBbHeight());

        AbilityManager.press(player, LeopardAbilities.BEAST_FORM.get());
        helper.assertTrue(TransformationManager.active(player) == null, "Toggling off ends the form");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rubberSoftensBluntHits(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        GumFruit gum = GLFruits.GUM_GUM.get();
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());

        LivingIncomingDamageEvent fist = event(player, player.damageSources().mobAttack(zombie), 10f);
        gum.onIncomingDamage(player, fist);
        helper.assertTrue(Math.abs(fist.getAmount() - 5f) < 1.0E-3, "Fists do half: " + fist.getAmount());

        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        LivingIncomingDamageEvent blade = event(player, player.damageSources().mobAttack(zombie), 10f);
        gum.onIncomingDamage(player, blade);
        helper.assertTrue(Math.abs(blade.getAmount() - 10f) < 1.0E-3, "Blades cut rubber: " + blade.getAmount());

        LivingIncomingDamageEvent lightning = event(player, player.damageSources().lightningBolt(), 10f);
        gum.onIncomingDamage(player, lightning);
        helper.assertTrue(lightning.isCanceled(), "Rubber ignores lightning");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void logiaBodyLetsBlowsPass(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        var flame = GLFruits.FLAME.get();
        long now = AbilityManager.now(player);
        float before = AbilityState.of(player).stamina(now);

        LivingIncomingDamageEvent punch = event(player, player.damageSources().mobAttack(zombie), 6f);
        flame.onIncomingDamage(player, punch);
        helper.assertTrue(punch.isCanceled(), "Punches pass through fire");
        helper.assertTrue(AbilityState.of(player).stamina(now) < before, "Dodging costs stamina");

        LivingIncomingDamageEvent fire = event(player, player.damageSources().inFire(), 4f);
        flame.onIncomingDamage(player, fire);
        helper.assertTrue(fire.isCanceled(), "Fire cannot burn fire");

        LivingIncomingDamageEvent magic = event(player, player.damageSources().magic(), 4f);
        flame.onIncomingDamage(player, magic);
        helper.assertFalse(magic.isCanceled(), "Magic still hurts a Logia");

        // Exhausted: no stamina, no intangibility.
        while (AbilityState.of(player).spendStamina(1f, now)) {
            // drain
        }
        LivingIncomingDamageEvent tired = event(player, player.damageSources().mobAttack(zombie), 6f);
        flame.onIncomingDamage(player, tired);
        helper.assertFalse(tired.isCanceled(), "An exhausted Logia can be hit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void fireFistLaunchesAProjectile(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        player.setPos(helper.absoluteVec(new Vec3(0.5, 2, 0.5)));
        FruitManager.setFruit(player, GLFruits.FLAME.get());
        helper.assertTrue(AbilityManager.tryStart(player, FlameAbilities.FIRE_FIST.get()) == Result.STARTED, "Charge started");
        helper.onEachTick(() -> AbilityManager.tickPlayer(player));
        helper.runAfterDelay(10, () -> {
            AbilityManager.release(player, FlameAbilities.FIRE_FIST.get());
            List<AbilityProjectile> shots = helper.getLevel().getEntitiesOfClass(AbilityProjectile.class,
                    new AABB(player.position(), player.position()).inflate(8),
                    p -> FlameAbilities.FIRE_FIST_SHOT.equals(p.kind()) && p.getOwner() == player);
            helper.assertTrue(shots.size() == 1, "One fire fist should fly, found " + shots.size());
            helper.assertTrue(AbilityState.of(player).isOnCooldown(FlameAbilities.FIRE_FIST.getId(), AbilityManager.now(player)),
                    "Cooldown after release");
            shots.forEach(AbilityProjectile::discard);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fruitCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = newPlayer(helper);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player);
        run(helper, server, source, "onepiece fruit list");
        run(helper, server, source, "onepiece fruit info grandline:flame");
        run(helper, server, source, "onepiece fruit give leopard");
        helper.assertTrue(player.getInventory().countItem(GLItems.LEOPARD_FRUIT.get()) == 1, "Fruit item given");
        run(helper, server, source, "onepiece fruit set grandline:leopard");
        helper.assertTrue(FruitManager.fruitOf(player) == GLFruits.LEOPARD.get(), "Fruit set");
        run(helper, server, source, "onepiece mastery set fruit 40");
        helper.assertTrue(AbilityAccess.canUse(player, LeopardAbilities.FRENZY.get()), "Mastery 40 unlocks Frenzy");
        run(helper, server, source, "onepiece fruit awaken");
        helper.assertTrue(FruitManager.isAwakened(player), "Awakened by command");
        run(helper, server, source, "onepiece debug fruit gum_gum");
        run(helper, server, source, "onepiece reset fruit confirm");
        helper.assertFalse(FruitManager.hasFruit(player), "Fruit reset");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------

    private static LivingIncomingDamageEvent event(ServerPlayer player, DamageSource source, float amount) {
        return new LivingIncomingDamageEvent(player, new DamageContainer(source, amount));
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
