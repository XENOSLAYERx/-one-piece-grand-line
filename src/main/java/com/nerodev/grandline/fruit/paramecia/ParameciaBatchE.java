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

/** Paramecia batch E: heat, castle, sparkle, clothes, time, ripe, millet, gold, song, warp, magnet, gravity, age, brain. */
public final class ParameciaBatchE {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, HeatFruit> HEAT = GLFruits.register("heat", HeatFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, CastleFruit> CASTLE = GLFruits.register("castle", CastleFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SparkleFruit> SPARKLE = GLFruits.register("sparkle", SparkleFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ClothesFruit> CLOTHES = GLFruits.register("clothes", ClothesFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, TimeFruit> TIME = GLFruits.register("time", TimeFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, RipeFruit> RIPE = GLFruits.register("ripe", RipeFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, MilletFruit> MILLET = GLFruits.register("millet", MilletFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, GoldFruit> GOLD = GLFruits.register("gold", GoldFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SongFruit> SONG = GLFruits.register("song", SongFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, WarpFruit> WARP = GLFruits.register("warp", WarpFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, MagnetFruit> MAGNET = GLFruits.register("magnet", MagnetFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, GravityFruit> GRAVITY = GLFruits.register("gravity", GravityFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, AgeFruit> AGE = GLFruits.register("age", AgeFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, BrainFruit> BRAIN = GLFruits.register("brain", BrainFruit::new, Rarity.EPIC);

    private ParameciaBatchE() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        HeatFruit.init();
        CastleFruit.init();
        SparkleFruit.init();
        ClothesFruit.init();
        TimeFruit.init();
        RipeFruit.init();
        MilletFruit.init();
        GoldFruit.init();
        SongFruit.init();
        WarpFruit.init();
        MagnetFruit.init();
        GravityFruit.init();
        AgeFruit.init();
        BrainFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        HeatFruit.behaviors();
        CastleFruit.behaviors();
        SparkleFruit.behaviors();
        ClothesFruit.behaviors();
        TimeFruit.behaviors();
        RipeFruit.behaviors();
        MilletFruit.behaviors();
        GoldFruit.behaviors();
        SongFruit.behaviors();
        WarpFruit.behaviors();
        MagnetFruit.behaviors();
        GravityFruit.behaviors();
        AgeFruit.behaviors();
        BrainFruit.behaviors();
    }
}
