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

/** Paramecia batch C: venom, hormone, snip, wash, float, mark, arms, spin, stick, ripple, stone, stitch, glare, art. */
public final class ParameciaBatchC {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<DevilFruit, VenomFruit> VENOM = GLFruits.register("venom", VenomFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, HormoneFruit> HORMONE = GLFruits.register("hormone", HormoneFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SnipFruit> SNIP = GLFruits.register("snip", SnipFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, WashFruit> WASH = GLFruits.register("wash", WashFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, FloatFruit> FLOAT = GLFruits.register("float", FloatFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, MarkFruit> MARK = GLFruits.register("mark", MarkFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, ArmsFruit> ARMS = GLFruits.register("arms", ArmsFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, SpinFruit> SPIN = GLFruits.register("spin", SpinFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, StickFruit> STICK = GLFruits.register("stick", StickFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, RippleFruit> RIPPLE = GLFruits.register("ripple", RippleFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, StoneFruit> STONE = GLFruits.register("stone", StoneFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, StitchFruit> STITCH = GLFruits.register("stitch", StitchFruit::new, Rarity.COMMON);
    public static final DeferredHolder<DevilFruit, GlareFruit> GLARE = GLFruits.register("glare", GlareFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, ArtFruit> ART = GLFruits.register("art", ArtFruit::new, Rarity.UNCOMMON);

    private ParameciaBatchC() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /** Loads every fruit class so its abilities are queued before the register goes to the bus. */
    public static void load() {
        VenomFruit.init();
        HormoneFruit.init();
        SnipFruit.init();
        WashFruit.init();
        FloatFruit.init();
        MarkFruit.init();
        ArmsFruit.init();
        SpinFruit.init();
        StickFruit.init();
        RippleFruit.init();
        StoneFruit.init();
        StitchFruit.init();
        GlareFruit.init();
        ArtFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        VenomFruit.behaviors();
        HormoneFruit.behaviors();
        SnipFruit.behaviors();
        WashFruit.behaviors();
        FloatFruit.behaviors();
        MarkFruit.behaviors();
        ArmsFruit.behaviors();
        SpinFruit.behaviors();
        StickFruit.behaviors();
        RippleFruit.behaviors();
        StoneFruit.behaviors();
        StitchFruit.behaviors();
        GlareFruit.behaviors();
        ArtFruit.behaviors();
    }
}
