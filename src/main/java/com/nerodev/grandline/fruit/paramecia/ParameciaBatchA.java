package com.nerodev.grandline.fruit.paramecia;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import java.util.function.Supplier;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Paramecia batch A: op, tremor, string, mochi, chop, slip, bomb, kilo, flower, wax, munch, clone, dice, spike. */
public final class ParameciaBatchA {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, OpFruit> OP = GLFruits.register("op", OpFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, TremorFruit> TREMOR = GLFruits.register("tremor", TremorFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, StringFruit> STRING = GLFruits.register("string", StringFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, MochiFruit> MOCHI = GLFruits.register("mochi", MochiFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, ChopFruit> CHOP = GLFruits.register("chop", ChopFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, SlipFruit> SLIP = GLFruits.register("slip", SlipFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, BombFruit> BOMB = GLFruits.register("bomb", BombFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, KiloFruit> KILO = GLFruits.register("kilo", KiloFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, FlowerFruit> FLOWER = GLFruits.register("flower", FlowerFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, WaxFruit> WAX = GLFruits.register("wax", WaxFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, MunchFruit> MUNCH = GLFruits.register("munch", MunchFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, CloneFruit> CLONE = GLFruits.register("clone", CloneFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, DiceFruit> DICE = GLFruits.register("dice", DiceFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SpikeFruit> SPIKE = GLFruits.register("spike", SpikeFruit::new, Rarity.UNCOMMON);

    private ParameciaBatchA() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        OpFruit.init();
        TremorFruit.init();
        StringFruit.init();
        MochiFruit.init();
        ChopFruit.init();
        SlipFruit.init();
        BombFruit.init();
        KiloFruit.init();
        FlowerFruit.init();
        WaxFruit.init();
        MunchFruit.init();
        CloneFruit.init();
        DiceFruit.init();
        SpikeFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        OpFruit.behaviors();
        TremorFruit.behaviors();
        StringFruit.behaviors();
        MochiFruit.behaviors();
        ChopFruit.behaviors();
        SlipFruit.behaviors();
        BombFruit.behaviors();
        KiloFruit.behaviors();
        FlowerFruit.behaviors();
        WaxFruit.behaviors();
        MunchFruit.behaviors();
        CloneFruit.behaviors();
        DiceFruit.behaviors();
        SpikeFruit.behaviors();
    }
}
