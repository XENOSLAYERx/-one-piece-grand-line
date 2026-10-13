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

/** Paramecia batch D: jacket, pop, swim, hobby, barrier, calm, heal, biscuit, cream, soul, mirror, lick, wring, memory. */
public final class ParameciaBatchD {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, JacketFruit> JACKET = GLFruits.register("jacket", JacketFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, PopFruit> POP = GLFruits.register("pop", PopFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, SwimFruit> SWIM = GLFruits.register("swim", SwimFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, HobbyFruit> HOBBY = GLFruits.register("hobby", HobbyFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, BarrierFruit> BARRIER = GLFruits.register("barrier", BarrierFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, CalmFruit> CALM = GLFruits.register("calm", CalmFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, HealFruit> HEAL = GLFruits.register("heal", HealFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, BiscuitFruit> BISCUIT = GLFruits.register("biscuit", BiscuitFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, CreamFruit> CREAM = GLFruits.register("cream", CreamFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, SoulFruit> SOUL = GLFruits.register("soul", SoulFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, MirrorFruit> MIRROR = GLFruits.register("mirror", MirrorFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, LickFruit> LICK = GLFruits.register("lick", LickFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, WringFruit> WRING = GLFruits.register("wring", WringFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, MemoryFruit> MEMORY = GLFruits.register("memory", MemoryFruit::new, Rarity.UNCOMMON);

    private ParameciaBatchD() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        JacketFruit.init();
        PopFruit.init();
        SwimFruit.init();
        HobbyFruit.init();
        BarrierFruit.init();
        CalmFruit.init();
        HealFruit.init();
        BiscuitFruit.init();
        CreamFruit.init();
        SoulFruit.init();
        MirrorFruit.init();
        LickFruit.init();
        WringFruit.init();
        MemoryFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        JacketFruit.behaviors();
        PopFruit.behaviors();
        SwimFruit.behaviors();
        HobbyFruit.behaviors();
        BarrierFruit.behaviors();
        CalmFruit.behaviors();
        HealFruit.behaviors();
        BiscuitFruit.behaviors();
        CreamFruit.behaviors();
        SoulFruit.behaviors();
        MirrorFruit.behaviors();
        LickFruit.behaviors();
        WringFruit.behaviors();
        MemoryFruit.behaviors();
    }
}
