package com.nerodev.grandline.fruit.logia;

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

/** The Logia fruits (except Flame, which came first): every element a body can become. */
public final class LogiaBatch {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, SmokeFruit> SMOKE = GLFruits.register("smoke", SmokeFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, SandFruit> SAND = GLFruits.register("sand", SandFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, RumbleFruit> RUMBLE = GLFruits.register("rumble", RumbleFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, IceFruit> ICE = GLFruits.register("ice", IceFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, DarkFruit> DARK = GLFruits.register("dark", DarkFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, GlintFruit> GLINT = GLFruits.register("glint", GlintFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, MagmaFruit> MAGMA = GLFruits.register("magma", MagmaFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, SwampFruit> SWAMP = GLFruits.register("swamp", SwampFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, GasFruit> GAS = GLFruits.register("gas", GasFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SnowFruit> SNOW = GLFruits.register("snow", SnowFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ForestFruit> FOREST = GLFruits.register("forest", ForestFruit::new, Rarity.EPIC);

    private LogiaBatch() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        SmokeFruit.init();
        SandFruit.init();
        RumbleFruit.init();
        IceFruit.init();
        DarkFruit.init();
        GlintFruit.init();
        MagmaFruit.init();
        SwampFruit.init();
        GasFruit.init();
        SnowFruit.init();
        ForestFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        SmokeFruit.behaviors();
        SandFruit.behaviors();
        RumbleFruit.behaviors();
        IceFruit.behaviors();
        DarkFruit.behaviors();
        GlintFruit.behaviors();
        MagmaFruit.behaviors();
        SwampFruit.behaviors();
        GasFruit.behaviors();
        SnowFruit.behaviors();
        ForestFruit.behaviors();
    }
}
