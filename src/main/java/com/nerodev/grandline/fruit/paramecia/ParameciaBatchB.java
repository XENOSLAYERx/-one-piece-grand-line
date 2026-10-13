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

/** Paramecia batch B: bind, spring, slow, door, bubble, berry, rust, wheel, revive, shadow, hollow, clear, paw, love. */
public final class ParameciaBatchB {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, BindFruit> BIND = GLFruits.register("bind", BindFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SpringFruit> SPRING = GLFruits.register("spring", SpringFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, SlowFruit> SLOW = GLFruits.register("slow", SlowFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, DoorFruit> DOOR = GLFruits.register("door", DoorFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, BubbleFruit> BUBBLE = GLFruits.register("bubble", BubbleFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, BerryFruit> BERRY = GLFruits.register("berry", BerryFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, RustFruit> RUST = GLFruits.register("rust", RustFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, WheelFruit> WHEEL = GLFruits.register("wheel", WheelFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, ReviveFruit> REVIVE = GLFruits.register("revive", ReviveFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ShadowFruit> SHADOW = GLFruits.register("shadow", ShadowFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, HollowFruit> HOLLOW = GLFruits.register("hollow", HollowFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ClearFruit> CLEAR = GLFruits.register("clear", ClearFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, PawFruit> PAW = GLFruits.register("paw", PawFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, LoveFruit> LOVE = GLFruits.register("love", LoveFruit::new, Rarity.RARE);

    private ParameciaBatchB() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        BindFruit.init();
        SpringFruit.init();
        SlowFruit.init();
        DoorFruit.init();
        BubbleFruit.init();
        BerryFruit.init();
        RustFruit.init();
        WheelFruit.init();
        ReviveFruit.init();
        ShadowFruit.init();
        HollowFruit.init();
        ClearFruit.init();
        PawFruit.init();
        LoveFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        BindFruit.behaviors();
        SpringFruit.behaviors();
        SlowFruit.behaviors();
        DoorFruit.behaviors();
        BubbleFruit.behaviors();
        BerryFruit.behaviors();
        RustFruit.behaviors();
        WheelFruit.behaviors();
        ReviveFruit.behaviors();
        ShadowFruit.behaviors();
        HollowFruit.behaviors();
        ClearFruit.behaviors();
        PawFruit.behaviors();
        LoveFruit.behaviors();
    }
}
