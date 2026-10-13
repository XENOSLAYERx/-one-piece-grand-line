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

/** Paramecia batch F: brush, genie, butter, straw, tone, big, guzzle, island, push, return, sprout, luck, pass. */
public final class ParameciaBatchF {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, BrushFruit> BRUSH = GLFruits.register("brush", BrushFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, GenieFruit> GENIE = GLFruits.register("genie", GenieFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, ButterFruit> BUTTER = GLFruits.register("butter", ButterFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, StrawFruit> STRAW = GLFruits.register("straw", StrawFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ToneFruit> TONE = GLFruits.register("tone", ToneFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, BigFruit> BIG = GLFruits.register("big", BigFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, GuzzleFruit> GUZZLE = GLFruits.register("guzzle", GuzzleFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, IslandFruit> ISLAND = GLFruits.register("island", IslandFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, PushFruit> PUSH = GLFruits.register("push", PushFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ReturnFruit> RETURN_FRUIT = GLFruits.register("return", ReturnFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SproutFruit> SPROUT = GLFruits.register("sprout", SproutFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, LuckFruit> LUCK = GLFruits.register("luck", LuckFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, PassFruit> PASS = GLFruits.register("pass", PassFruit::new, Rarity.UNCOMMON);

    private ParameciaBatchF() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        BrushFruit.init();
        GenieFruit.init();
        ButterFruit.init();
        StrawFruit.init();
        ToneFruit.init();
        BigFruit.init();
        GuzzleFruit.init();
        IslandFruit.init();
        PushFruit.init();
        ReturnFruit.init();
        SproutFruit.init();
        LuckFruit.init();
        PassFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        BrushFruit.behaviors();
        GenieFruit.behaviors();
        ButterFruit.behaviors();
        StrawFruit.behaviors();
        ToneFruit.behaviors();
        BigFruit.behaviors();
        GuzzleFruit.behaviors();
        IslandFruit.behaviors();
        PushFruit.behaviors();
        ReturnFruit.behaviors();
        SproutFruit.behaviors();
        LuckFruit.behaviors();
        PassFruit.behaviors();
    }
}
