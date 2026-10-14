package com.nerodev.grandline.weapon;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.combat.AbilityDamage;
import com.nerodev.grandline.entity.AbilityProjectile;
import com.nerodev.grandline.entity.ProjectileBehavior;
import com.nerodev.grandline.fruit.kit.Kit;
import com.nerodev.grandline.fruit.kit.Projectiles;
import com.nerodev.grandline.fx.GLParticles.Kind;
import com.nerodev.grandline.fx.Vfx;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Weapons, guns and Seastone: the swords of the world (ordinary steel up to the Supreme Grade blades), firearms
 * and their bullets, and Seastone, the ocean-floor ore that holds the power of the sea itself.
 */
public final class GLWeapons {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GrandLine.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GrandLine.MODID);

    /** Every weapon by its short name ({@code /onepiece weapon give <name>}), in creative-tab order. */
    private static final Map<String, DeferredItem<? extends Item>> WEAPONS = new LinkedHashMap<>();
    private static final List<DeferredItem<? extends Item>> TAB_ITEMS = new ArrayList<>();

    /** The colour of Seastone, used by its effects. */
    public static final int SEASTONE_COLOR = 0x6E8C8A;

    // Projectile kinds.
    public static final ResourceLocation BULLET_SHOT = GrandLine.id("bullet");
    public static final ResourceLocation SEASTONE_BULLET_SHOT = GrandLine.id("seastone_bullet");
    public static final ResourceLocation NET_THROWN = GrandLine.id("seastone_net");

    // ---------------------------------------------------------------------------------------------------------
    // Seastone
    // ---------------------------------------------------------------------------------------------------------

    public static final DeferredBlock<DropExperienceBlock> SEASTONE_ORE = block("seastone_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .mapColor(MapColor.WARPED_NYLIUM).strength(4f, 6f)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_SEASTONE_ORE = block("deepslate_seastone_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)
                    .strength(5.5f, 6f)));
    public static final DeferredBlock<Block> SEASTONE_BLOCK = block("seastone_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.WARPED_NYLIUM)));
    public static final DeferredBlock<IronBarsBlock> SEASTONE_BARS = block("seastone_bars",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).mapColor(MapColor.WARPED_NYLIUM)));

    public static final DeferredItem<SeastoneItem> RAW_SEASTONE = item("raw_seastone", () -> new SeastoneItem(new Item.Properties()));
    public static final DeferredItem<SeastoneItem> SEASTONE_INGOT = item("seastone_ingot", () -> new SeastoneItem(new Item.Properties()));

    // ---------------------------------------------------------------------------------------------------------
    // Melee weapons
    // ---------------------------------------------------------------------------------------------------------

    public static final DeferredItem<WeaponItem> CUTLASS = weapon("cutlass",
            WeaponItem.Spec.of(Tiers.IRON, WeaponKind.SWORD, WeaponGrade.ORDINARY, 6.5f, 1.6f));
    public static final DeferredItem<WeaponItem> KATANA = weapon("katana",
            WeaponItem.Spec.of(Tiers.IRON, WeaponKind.SWORD, WeaponGrade.ORDINARY, 6.5f, 1.7f));
    public static final DeferredItem<WeaponItem> NAGINATA = weapon("naginata",
            WeaponItem.Spec.of(Tiers.IRON, WeaponKind.POLEARM, WeaponGrade.ORDINARY, 7.5f, 1.1f).reach(1f));
    public static final DeferredItem<WeaponItem> KANABO = weapon("kanabo",
            WeaponItem.Spec.of(Tiers.IRON, WeaponKind.CLUB, WeaponGrade.ORDINARY, 9f, 0.8f).special(WeaponSpecials.STAGGER));
    public static final DeferredItem<WeaponItem> SEASTONE_JITTE = weapon("seastone_jitte",
            WeaponItem.Spec.of(Tiers.IRON, WeaponKind.CLUB, WeaponGrade.ORDINARY, 5.5f, 1.6f).special(WeaponSpecials.SEAL));

    public static final DeferredItem<WeaponItem> SANDAI_KITETSU = weapon("sandai_kitetsu",
            legendary(Tiers.DIAMOND, WeaponKind.SWORD, WeaponGrade.SKILLED, 8.5f, 1.7f).special(WeaponSpecials.CURSED));
    public static final DeferredItem<WeaponItem> WADO_ICHIMONJI = weapon("wado_ichimonji",
            legendary(Tiers.DIAMOND, WeaponKind.SWORD, WeaponGrade.GREAT, 9f, 1.7f).haki(1.1f).special(WeaponSpecials.TRUE_EDGE));
    public static final DeferredItem<WeaponItem> SHUSUI = weapon("shusui",
            legendary(Tiers.NETHERITE, WeaponKind.SWORD, WeaponGrade.GREAT, 9.5f, 1.5f).haki(1.3f));
    public static final DeferredItem<WeaponItem> ENMA = weapon("enma",
            legendary(Tiers.NETHERITE, WeaponKind.SWORD, WeaponGrade.GREAT, 10f, 1.5f).haki(1.2f).special(WeaponSpecials.ENMA));
    public static final DeferredItem<WeaponItem> KIKOKU = weapon("kikoku",
            legendary(Tiers.NETHERITE, WeaponKind.SWORD, WeaponGrade.GREAT, 10f, 1.4f).reach(0.75f).special(WeaponSpecials.KIKOKU));
    public static final DeferredItem<WeaponItem> GRYPHON = weapon("gryphon",
            legendary(Tiers.NETHERITE, WeaponKind.SWORD, WeaponGrade.GREAT, 10f, 1.6f).special(WeaponSpecials.GRYPHON));
    public static final DeferredItem<WeaponItem> YORU = weapon("yoru",
            legendary(Tiers.NETHERITE, WeaponKind.SWORD, WeaponGrade.SUPREME, 12f, 1.2f).reach(0.75f).haki(1.3f)
                    .special(WeaponSpecials.YORU));
    /** Dropped by Captain "Axe-Hand" Morgan. */
    public static final DeferredItem<WeaponItem> MORGANS_AXE = weapon("morgans_axe",
            legendary(Tiers.DIAMOND, WeaponKind.CLUB, WeaponGrade.SKILLED, 11f, 0.85f).special(WeaponSpecials.CLEAVE));
    /** Dropped by Arlong the Saw. */
    public static final DeferredItem<WeaponItem> KIRIBACHI = weapon("kiribachi",
            legendary(Tiers.DIAMOND, WeaponKind.POLEARM, WeaponGrade.SKILLED, 10.5f, 1.0f).reach(1f).special(WeaponSpecials.SAW));
    /** Dropped by Buggy the Clown. */
    public static final DeferredItem<WeaponItem> BUGGYS_KNIFE = weapon("buggys_knife",
            legendary(Tiers.DIAMOND, WeaponKind.SWORD, WeaponGrade.SKILLED, 7.5f, 1.9f).special(WeaponSpecials.BARA_BARA));
    /** Dropped by Captain Kuro. */
    public static final DeferredItem<WeaponItem> CAT_CLAWS = weapon("cat_claws",
            legendary(Tiers.DIAMOND, WeaponKind.SWORD, WeaponGrade.SKILLED, 8f, 2.0f).special(WeaponSpecials.NEKO_NO_TE));
    public static final DeferredItem<WeaponItem> MURAKUMOGIRI = weapon("murakumogiri",
            legendary(Tiers.NETHERITE, WeaponKind.POLEARM, WeaponGrade.SUPREME, 12.5f, 0.9f).reach(1.5f)
                    .special(WeaponSpecials.MURAKUMOGIRI));

    // ---------------------------------------------------------------------------------------------------------
    // Guns and Seastone tools
    // ---------------------------------------------------------------------------------------------------------

    public static final DeferredItem<GunItem> FLINTLOCK = gun("flintlock",
            () -> new GunItem(7f, 25, 3.0f, 0.02f, false, new Item.Properties()));
    public static final DeferredItem<GunItem> MUSKET = gun("musket",
            () -> new GunItem(13f, 45, 4.0f, 0.005f, true, new Item.Properties()));
    public static final DeferredItem<AmmoItem> IRON_BULLET = item("iron_bullet", () -> new AmmoItem(false, new Item.Properties()));
    public static final DeferredItem<AmmoItem> SEASTONE_BULLET = item("seastone_bullet",
            () -> new AmmoItem(true, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SeastoneCuffsItem> SEASTONE_CUFFS = weapon("seastone_cuffs",
            () -> new SeastoneCuffsItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SeastoneNetItem> SEASTONE_NET = weapon("seastone_net",
            () -> new SeastoneNetItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grandline.weapons"))
            .icon(() -> new ItemStack(WADO_ICHIMONJI.get()))
            .displayItems((params, output) -> TAB_ITEMS.forEach(item -> output.accept(item.get())))
            .build());

    private GLWeapons() {}

    // ---------------------------------------------------------------------------------------------------------

    private static WeaponItem.Spec legendary(Tier tier, WeaponKind kind, WeaponGrade grade, float damage, float speed) {
        Rarity rarity = switch (grade) {
            case SUPREME, GREAT -> Rarity.EPIC;
            case SKILLED -> Rarity.RARE;
            case ORDINARY -> Rarity.COMMON;
        };
        return WeaponItem.Spec.of(tier, kind, grade, damage, speed).legendary(rarity);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Supplier<B> factory) {
        DeferredBlock<B> block = BLOCKS.register(name, factory);
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(name, block));
        return block;
    }

    private static <I extends Item> DeferredItem<I> item(String name, Supplier<I> factory) {
        DeferredItem<I> item = ITEMS.register(name, factory);
        TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<WeaponItem> weapon(String name, WeaponItem.Spec spec) {
        return weapon(name, () -> new WeaponItem(spec));
    }

    private static <I extends Item> DeferredItem<I> weapon(String name, Supplier<I> factory) {
        DeferredItem<I> item = item(name, factory);
        WEAPONS.put(name, item);
        return item;
    }

    private static DeferredItem<GunItem> gun(String name, Supplier<GunItem> factory) {
        return weapon(name, factory);
    }

    /** Every weapon, gun and Seastone tool by its short name. */
    public static Map<String, DeferredItem<? extends Item>> weapons() {
        return WEAPONS;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Projectiles
    // ---------------------------------------------------------------------------------------------------------

    private static void behaviors() {
        ProjectileBehavior.Registry.register(BULLET_SHOT, bullet(false));
        ProjectileBehavior.Registry.register(SEASTONE_BULLET_SHOT, bullet(true));
        ProjectileBehavior.Registry.register(NET_THROWN, Projectiles.blast(1.6, AbilityDamage.STRIKE, (net, target) -> {
            SeastoneCuffsItem.cuff(target, 100);
            Kit.root(target, Vfx.StatusLook.SEASTONE, 100);
        }, net -> {
            ServerLevel level = (ServerLevel) net.level();
            Vfx.burst(level, net.getOwner(), net.position(), SEASTONE_COLOR, 1.6f, Kind.SHARD, Vfx.RING);
            level.playSound(null, net.getX(), net.getY(), net.getZ(), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 1.2f, 0.7f);
        }));
    }

    /** A lead or Seastone ball: strikes one target; Seastone seals its fruit for five seconds. */
    public static ProjectileBehavior bullet(boolean seastone) {
        ProjectileBehavior hit = Projectiles.bolt(AbilityDamage.STRIKE, (shot, target) -> {
            ServerLevel level = (ServerLevel) shot.level();
            Vfx.impact(level, shot.getOwner(), shot.position(), seastone ? SEASTONE_COLOR : 0xD0C0A0, 0.7f, Kind.SPARK);
            if (seastone || shot.extra().getBoolean(GunItem.SEASTONE_TAG)) {
                SeastoneCuffsItem.cuff(target, 100);
                Vfx.status(level, target, Vfx.StatusLook.SEASTONE, 100);
            }
        });
        return new ProjectileBehavior() {
            @Override
            public boolean onHitEntity(AbilityProjectile shot, Entity target) {
                return hit.onHitEntity(shot, target);
            }

            @Override
            public void onHitBlock(AbilityProjectile shot, BlockHitResult result) {
                ServerLevel level = (ServerLevel) shot.level();
                Vec3 at = result.getLocation();
                Vfx.impact(level, shot.getOwner(), at, 0xB0A890, 0.5f, Kind.SPARK);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        behaviors();
    }
}
