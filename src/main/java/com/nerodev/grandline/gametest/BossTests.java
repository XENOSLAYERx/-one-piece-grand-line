package com.nerodev.grandline.gametest;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.AbilityManager.Result;
import com.nerodev.grandline.ability.AbilityManager;
import com.nerodev.grandline.boss.BossDefinition;
import com.nerodev.grandline.boss.BossEntity;
import com.nerodev.grandline.boss.BossMove;
import com.nerodev.grandline.boss.BossMoves;
import com.nerodev.grandline.boss.BossRespawns;
import com.nerodev.grandline.boss.GLBosses;
import com.nerodev.grandline.data.GLPlayer;
import com.nerodev.grandline.data.PlayerData;
import com.nerodev.grandline.effect.GLMobEffects;
import com.nerodev.grandline.haki.HakiAbilities;
import com.nerodev.grandline.haki.HakiManager;
import com.nerodev.grandline.haki.HakiType;
import com.nerodev.grandline.npc.Factions;
import com.nerodev.grandline.npc.NpcEntity;
import com.nerodev.grandline.registry.GLRegistries;
import com.nerodev.grandline.weapon.GLWeapons;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Milestone 14: two complete bosses, each fought in phases with telegraphed moves, that pay out when beaten. */
@GameTestHolder(GrandLine.MODID)
@PrefixGameTestTemplate(false)
public final class BossTests {
    private static final String ARENA = "arena";

    private BossTests() {}

    @GameTest(template = "empty")
    public static void bossDefinitionsAreComplete(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<BossDefinition> bosses = access.registryOrThrow(GLRegistries.BOSS);
        helper.assertTrue(bosses.containsKey(GLBosses.MORGAN) && bosses.containsKey(GLBosses.ARLONG), "Morgan and Arlong are defined");
        for (var entry : bosses.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            BossDefinition boss = entry.getValue();
            helper.assertTrue(boss.phases().size() >= 3, id + " fights in at least three phases");
            for (int i = 1; i < boss.phases().size(); i++) {
                helper.assertTrue(boss.phases().get(i).threshold() < boss.phases().get(i - 1).threshold(),
                        id + " phases come in order of falling health");
                helper.assertTrue(boss.phases().get(i).pace() >= boss.phases().get(i - 1).pace(), id + " grows fiercer phase by phase");
            }
            for (BossDefinition.Phase phase : boss.phases()) {
                helper.assertTrue(phase.moves().size() >= 3, id + " has at least three moves in " + phase.name().getString());
                for (String move : phase.moves().keySet()) {
                    helper.assertTrue(BossMoves.get(move) != null, id + " uses a real move: " + move);
                }
                // Whatever the distance, the boss has something it can do (close in, or strike from range).
                helper.assertTrue(phase.moves().keySet().stream().map(BossMoves::get).anyMatch(m -> m.inRange(1.5)),
                        id + " can strike up close in " + phase.name().getString());
            }
            helper.assertTrue(boss.weapon().isPresent() || boss.body() == BossDefinition.Body.SEA_KING, id + " carries a signature weapon");
            boss.faction().ifPresent(f -> helper.assertTrue(access.registryOrThrow(GLRegistries.FACTION).containsKey(f), id + " faction exists"));
            ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "entities/boss/" + id.getPath()));
            helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(loot) != LootTable.EMPTY, id + " has loot");
        }
        for (BossMove move : BossMoves.all()) {
            helper.assertTrue(move.length() > 0 && move.minRange() <= move.maxRange(), move.id() + " has sane timing and range");
            helper.assertTrue(move.shield() || move.recovery() > 0 || move == BossMoves.GUARD,
                    move.id() + " leaves the boss open to punishment afterwards");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bossesSpawnReady(GameTestHelper helper) {
        BossEntity morgan = spawn(helper, GLBosses.MORGAN, 8, 8);
        helper.assertTrue(Math.abs(morgan.getMaxHealth() - 260f) < 0.01f && morgan.getHealth() == morgan.getMaxHealth(),
                "Morgan has 260 health, has " + morgan.getMaxHealth());
        helper.assertTrue(morgan.getMainHandItem().is(GLWeapons.MORGANS_AXE.get()), "Morgan wields his axe");
        helper.assertTrue(morgan.getBbHeight() > 2.1f && morgan.getBbHeight() < 2.2f, "Morgan stands 1.2 times as tall as a man, "
                + morgan.getBbHeight());
        helper.assertTrue("morgan".equals(morgan.skin()) && "morgan".equals(morgan.parts()), "Morgan looks like himself");
        helper.assertTrue(morgan.phase() == 0 && morgan.currentMove() == null, "Morgan starts in his first phase, at rest");
        CompoundTag saved = new CompoundTag();
        morgan.saveWithoutId(saved);
        BossEntity copy = new BossEntity(GLBosses.BOSS.get(), helper.getLevel());
        copy.load(saved);
        helper.assertTrue(GLBosses.MORGAN.equals(copy.bossId()) && copy.getRestrictCenter().equals(morgan.getRestrictCenter())
                && Math.abs(copy.getMaxHealth() - 260f) < 0.01f, "A boss keeps who they are and their post through a save");
        morgan.discard();
        BossEntity arlong = spawn(helper, GLBosses.ARLONG, 8, 8);
        helper.assertTrue(arlong.getMainHandItem().is(GLWeapons.KIRIBACHI.get()) && arlong.getBbHeight() > 2.3f, "Arlong, tall, with Kiribachi");
        ServerPlayer stranger = NpcTests.newPlayer(helper);
        helper.assertTrue(arlong.hostileTo(stranger), "Arlong attacks any human who comes near");
        arlong.discard();
        BossEntity captain = spawn(helper, GLBosses.MORGAN, 4, 4);
        helper.assertTrue(!captain.hostileTo(stranger), "Morgan leaves law-abiding strangers alone");
        GLPlayer.edit(stranger, data -> data.setBounty(5_000_000));
        helper.assertTrue(captain.hostileTo(stranger), "Morgan arrests a wanted pirate");
        captain.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 260, batch = "boss_phase")
    public static void bossesFightInPhases(GameTestHelper helper) {
        BossEntity morgan = spawn(helper, GLBosses.MORGAN, 8, 8);
        ServerPlayer player = NpcTests.newPlayer(helper);
        helper.startSequence()
                .thenExecute(() -> morgan.forcePhase(1))
                .thenWaitUntil(() -> helper.assertTrue(morgan.currentMove() == BossMoves.ROAR, "Morgan roars into his second phase"))
                .thenExecute(() -> {
                    float before = morgan.getHealth();
                    morgan.hurt(player.damageSources().playerAttack(player), 10f);
                    helper.assertTrue(morgan.getHealth() == before, "Nothing gets through the roar");
                })
                .thenWaitUntil(() -> helper.assertTrue(morgan.phase() == 1 && morgan.currentMove() == null,
                        "After the roar Morgan is in phase 2, phase " + morgan.phase()))
                .thenExecute(() -> helper.assertTrue(morgan.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.27 * 1.1,
                        "Morgan moves faster in phase 2"))
                .thenExecute(() -> morgan.forcePhase(2))
                .thenWaitUntil(() -> helper.assertTrue(morgan.phase() == 2, "Morgan reaches his last phase, phase " + morgan.phase()))
                .thenExecute(() -> {
                    helper.assertTrue(morgan.history().stream().filter("roar"::equals).count() == 2, "A roar for each new phase: "
                            + morgan.history());
                    helper.assertTrue(morgan.bar().getName().toString().contains("boss.grandline.morgan.phase3"),
                            "The boss bar names the phase: " + morgan.bar().getName());
                    morgan.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "boss_fight_morgan")
    public static void morganFights(GameTestHelper helper) {
        fight(helper, GLBosses.MORGAN, Set.of("cleave", "jaw_bash", "charge", "slam"));
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "boss_fight_arlong")
    public static void arlongFights(GameTestHelper helper) {
        fight(helper, GLBosses.ARLONG, Set.of("bite", "tooth_gun", "shark_on_darts"));
    }

    /** The boss goes after a sturdy dummy with the moves of its first phase and hurts it. */
    private static void fight(GameTestHelper helper, ResourceLocation id, Set<String> firstPhase) {
        Runnable release = NpcTests.keepLoaded(helper);
        BossEntity boss = spawn(helper, id, 3, 8);
        Husk dummy = EntityType.HUSK.create(helper.getLevel());
        helper.assertTrue(dummy != null, "Made a dummy");
        Vec3 at = helper.absoluteVec(new Vec3(12.5, 2, 8.5));
        dummy.moveTo(at.x, at.y, at.z, 90f, 0f);
        dummy.setNoAi(true);
        dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        dummy.setHealth(1000);
        helper.getLevel().addFreshEntity(dummy);
        boss.setTarget(dummy);
        helper.succeedWhen(() -> {
            List<String> moves = boss.history();
            helper.assertTrue(moves.stream().anyMatch(firstPhase::contains), id + " uses its moves: " + moves + ", target " + boss.getTarget());
            helper.assertTrue(dummy.getHealth() < 1000 || !dummy.isAlive(), id + " lands a blow, dummy at " + dummy.getHealth() + ", moves "
                    + moves);
            boss.discard();
            dummy.discard();
            release.run();
        });
    }

    @GameTest(template = ARENA)
    public static void beatingABossPaysOut(GameTestHelper helper) {
        BossEntity morgan = spawn(helper, GLBosses.MORGAN, 8, 8);
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 6));
        player.setPos(at.x, at.y, at.z);
        morgan.hurt(player.damageSources().playerAttack(player), 10f);
        PlayerData struck = GLPlayer.data(player);
        helper.assertTrue(struck.bounty() > 0, "Striking a Marine captain is a crime");
        helper.assertTrue(morgan.hostileTo(player), "Morgan turns on whoever strikes him");
        morgan.hurt(player.damageSources().playerAttack(player), 10_000f);
        helper.assertTrue(!morgan.isAlive(), "Morgan falls");
        PlayerData data = GLPlayer.data(player);
        helper.assertTrue(data.bounty() >= 8_000_000, "Beating Morgan earns a bounty, " + data.bounty());
        helper.assertTrue(data.reputation(Factions.CIVILIANS) >= 40, "Shells Town is grateful, " + data.reputation(Factions.CIVILIANS));
        helper.assertTrue(data.reputation(Factions.MARINES) < 0, "The Marines are not");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, morgan.getBoundingBox().inflate(4));
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(GLWeapons.MORGANS_AXE.get())), "Morgan drops his axe: "
                + drops.stream().map(item -> item.getItem().toString()).toList());
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void islandBossesReturn(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        BossEntity morgan = spawn(helper, GLBosses.MORGAN, 8, 8);
        morgan.addTag(BossRespawns.RESIDENT_TAG);
        BlockPos home = morgan.getRestrictCenter();
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1, 2, 1));
        player.setPos(at.x, at.y, at.z);
        int waiting = BossRespawns.get(server).pending().size();
        morgan.hurt(player.damageSources().playerAttack(player), 100_000f);
        helper.assertTrue(BossRespawns.get(server).pending().size() == waiting + 1, "A fallen island boss is waiting to return");
        BossRespawns.get(server).returnAll(server);
        List<BossEntity> back = helper.getLevel().getEntitiesOfClass(BossEntity.class, new AABB(home).inflate(3),
                boss -> GLBosses.MORGAN.equals(boss.bossId()) && boss.isAlive());
        helper.assertTrue(back.size() == 1 && back.getFirst().getTags().contains(BossRespawns.RESIDENT_TAG),
                "Morgan is back at his post, still holding the island");
        back.forEach(BossEntity::discard);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(home).inflate(6)).forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void buggyShrugsOffBlades(GameTestHelper helper) {
        BossEntity buggy = spawn(helper, GLBosses.BUGGY, 8, 8);
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1, 2, 1));
        player.setPos(at.x, at.y, at.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        float before = buggy.getHealth();
        buggy.hurt(player.damageSources().playerAttack(player), 20f);
        float cut = before - buggy.getHealth();
        helper.assertTrue(cut > 0 && cut < 4, "A sword passes through a Chop-Chop body: " + cut + " damage");
        buggy.invulnerableTime = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        before = buggy.getHealth();
        buggy.hurt(player.damageSources().playerAttack(player), 20f);
        float punched = before - buggy.getHealth();
        helper.assertTrue(punched > 10, "A fist lands in full: " + punched + " damage");
        buggy.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void smokerIsSmoke(GameTestHelper helper) {
        BossEntity smoker = spawn(helper, GLBosses.SMOKER, 8, 8);
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1, 2, 1));
        player.setPos(at.x, at.y, at.z);
        float before = smoker.getHealth();
        helper.assertFalse(smoker.hurt(player.damageSources().playerAttack(player), 20f), "A bare fist passes through smoke");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertFalse(smoker.hurt(player.damageSources().playerAttack(player), 20f), "So does a plain sword");
        helper.assertTrue(smoker.getHealth() == before, "Smoker is untouched");
        HakiManager.unlock(player, HakiType.ARMAMENT, false);
        helper.assertTrue(AbilityManager.tryStart(player, HakiAbilities.ARMAMENT.get()) == Result.STARTED, "Coating on");
        smoker.invulnerableTime = 0;
        smoker.hurt(player.damageSources().playerAttack(player), 20f);
        helper.assertTrue(smoker.getHealth() < before, "Armament Haki strikes the Logia: " + (before - smoker.getHealth()) + " damage");
        smoker.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80, batch = "boss_krieg")
    public static void kriegsCapeAndGas(GameTestHelper helper) {
        BossEntity krieg = spawn(helper, GLBosses.KRIEG, 8, 8);
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8.5, 2, 6.5));
        player.setPos(at.x, at.y, at.z);
        krieg.perform(BossMoves.PORCUPINE_CAPE);
        helper.runAfterDelay(12, () -> {
            float before = krieg.getHealth();
            krieg.invulnerableTime = 0;
            krieg.hurt(player.damageSources().playerAttack(player), 20f);
            float taken = before - krieg.getHealth();
            helper.assertTrue(taken < 4f, "The spiked cape turns the blow: " + taken + " damage");
            helper.assertTrue(player.hasEffect(GLMobEffects.BLEEDING), "...and its spikes bite the one who struck");
            krieg.perform(BossMoves.MH5);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(krieg.getTags().contains(BossEntity.GAS_MASK), "Krieg masks up for MH5");
                helper.assertFalse(krieg.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "His mask keeps out the gas");
                krieg.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 140, batch = "boss_kuro")
    public static void kuroCutsFriendAndFoe(GameTestHelper helper) {
        BossEntity kuro = spawn(helper, GLBosses.KURO, 8, 8);
        NpcEntity crewman = NpcEntity.spawn(helper.getLevel(), GrandLine.id("pirate_grunt"), helper.absolutePos(new BlockPos(9, 2, 8)), 0f,
                MobSpawnType.COMMAND);
        helper.assertTrue(crewman != null && crewman.faction().equals(kuro.faction()), "A pirate of Kuro's own side stands by");
        crewman.setNoAi(true);
        float health = crewman.getMaxHealth();
        kuro.perform(BossMoves.SHAKUSHI);
        helper.succeedWhen(() -> {
            helper.assertTrue(!crewman.isAlive() || crewman.getHealth() < health, "Out of Bounds cuts Kuro's own crew too");
            kuro.discard();
            crewman.discard();
        });
    }

    @GameTest(template = ARENA)
    public static void bossCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = NpcTests.newPlayer(helper);
        Vec3 at = helper.absoluteVec(new Vec3(8, 2, 3));
        player.setPos(at.x, at.y, at.z);
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4).withEntity(player)
                .withPosition(at);
        NpcTests.run(helper, server, source, "onepiece boss list");
        NpcTests.run(helper, server, source, "onepiece boss spawn grandline:arlong");
        List<BossEntity> spawned = helper.getLevel().getEntitiesOfClass(BossEntity.class, new AABB(BlockPos.containing(at)).inflate(10));
        helper.assertTrue(spawned.size() == 1 && GLBosses.ARLONG.equals(spawned.getFirst().bossId()), "boss spawn made Arlong");
        BossEntity arlong = spawned.getFirst();
        NpcTests.run(helper, server, source, "onepiece boss info");
        NpcTests.run(helper, server, source, "onepiece boss move tooth_gun");
        helper.assertTrue(arlong.currentMove() == BossMoves.TOOTH_GUN, "boss move starts the move");
        NpcTests.run(helper, server, source, "onepiece boss phase 3");
        helper.assertTrue(arlong.getHealth() <= arlong.getMaxHealth() * 0.3f, "boss phase drops the boss into its last phase");
        NpcTests.run(helper, server, source, "onepiece boss remove 16");
        helper.assertTrue(arlong.isRemoved(), "boss remove");
        helper.succeed();
    }

    private static BossEntity spawn(GameTestHelper helper, ResourceLocation id, int x, int z) {
        BossEntity boss = BossEntity.spawn(helper.getLevel(), id, helper.absolutePos(new BlockPos(x, 2, z)), 0f, MobSpawnType.COMMAND);
        helper.assertTrue(boss != null, "Spawned " + id);
        return boss;
    }
}
