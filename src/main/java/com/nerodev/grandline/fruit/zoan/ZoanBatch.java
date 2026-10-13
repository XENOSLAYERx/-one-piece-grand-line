package com.nerodev.grandline.fruit.zoan;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import com.nerodev.grandline.fruit.DevilFruit;
import com.nerodev.grandline.fruit.GLFruits;
import com.nerodev.grandline.transform.GLTransformations;
import com.nerodev.grandline.transform.Transformation;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers every Zoan fruit (beyond the original Leopard): their abilities and their two bodies each.
 * Ability and transformation ids for the bodies are {@code <fruit>_hybrid} and {@code <fruit>_beast}; the client
 * finds each body's model by that id.
 */
public final class ZoanBatch {
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);
    static final DeferredRegister<Transformation> FORMS = DeferredRegister.create(GLTransformations.KEY, GrandLine.MODID);

    // Regular Zoans
    public static final DeferredHolder<DevilFruit, HumanFruit> HUMAN = GLFruits.register("human", HumanFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, FalconFruit> FALCON = GLFruits.register("falcon", FalconFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, JackalFruit> JACKAL = GLFruits.register("jackal", JackalFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, MoleFruit> MOLE = GLFruits.register("mole", MoleFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, BisonFruit> BISON = GLFruits.register("bison", BisonFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, WolfFruit> WOLF = GLFruits.register("wolf", WolfFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, GiraffeFruit> GIRAFFE = GLFruits.register("giraffe", GiraffeFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, HorseFruit> HORSE = GLFruits.register("horse", HorseFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, AnacondaFruit> ANACONDA = GLFruits.register("anaconda", AnacondaFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, KingCobraFruit> KING_COBRA = GLFruits.register("king_cobra", KingCobraFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, AxolotlFruit> AXOLOTL = GLFruits.register("axolotl", AxolotlFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, AlbatrossFruit> ALBATROSS = GLFruits.register("albatross", AlbatrossFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, TanukiFruit> TANUKI = GLFruits.register("tanuki", TanukiFruit::new, Rarity.UNCOMMON);
    public static final DeferredHolder<DevilFruit, ElephantFruit> ELEPHANT = GLFruits.register("elephant", ElephantFruit::new, Rarity.UNCOMMON);

    // Ancient Zoans
    public static final DeferredHolder<DevilFruit, SabertoothFruit> SABERTOOTH = GLFruits.register("sabertooth", SabertoothFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, MammothFruit> MAMMOTH = GLFruits.register("mammoth", MammothFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, AllosaurusFruit> ALLOSAURUS = GLFruits.register("allosaurus", AllosaurusFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SpinosaurusFruit> SPINOSAURUS = GLFruits.register("spinosaurus", SpinosaurusFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, PteranodonFruit> PTERANODON = GLFruits.register("pteranodon", PteranodonFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, BrachiosaurusFruit> BRACHIOSAURUS = GLFruits.register("brachiosaurus", BrachiosaurusFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, TriceratopsFruit> TRICERATOPS = GLFruits.register("triceratops", TriceratopsFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, PachycephalosaurusFruit> PACHYCEPHALOSAURUS = GLFruits.register("pachycephalosaurus", PachycephalosaurusFruit::new, Rarity.RARE);
    public static final DeferredHolder<DevilFruit, SpiderFruit> SPIDER = GLFruits.register("spider", SpiderFruit::new, Rarity.RARE);

    // Mythical Zoans
    public static final DeferredHolder<DevilFruit, PhoenixFruit> PHOENIX = GLFruits.register("phoenix", PhoenixFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, AzureDragonFruit> AZURE_DRAGON = GLFruits.register("azure_dragon", AzureDragonFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, YamataFruit> YAMATA = GLFruits.register("yamata", YamataFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, NineTailedFoxFruit> NINE_TAILED_FOX = GLFruits.register("nine_tailed_fox", NineTailedFoxFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, OkuchiWolfFruit> OKUCHI_WOLF = GLFruits.register("okuchi_wolf", OkuchiWolfFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, BuddhaFruit> BUDDHA = GLFruits.register("buddha", BuddhaFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, GyukiFruit> GYUKI = GLFruits.register("gyuki", GyukiFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, ItsumadeFruit> ITSUMADE = GLFruits.register("itsumade", ItsumadeFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, BakotsuFruit> BAKOTSU = GLFruits.register("bakotsu", BakotsuFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, FengxiFruit> FENGXI = GLFruits.register("fengxi", FengxiFruit::new, Rarity.EPIC);
    public static final DeferredHolder<DevilFruit, SandwormFruit> SANDWORM = GLFruits.register("sandworm", SandwormFruit::new, Rarity.EPIC);

    private ZoanBatch() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.FRUIT).icon(icon, color);
    }

    /**
     * Registers a Zoan's two bodies and the toggles that hold them. The builders get a {@link Transformation.Group#BODY}
     * builder to add attribute changes and, for the beast, its hitbox.
     */
    static ZoanForms bodies(String fruitId, int color, Supplier<SoundEvent> sound, ZoanForms.Traits traits,
            UnaryOperator<Transformation.Builder> hybrid, UnaryOperator<Transformation.Builder> beast) {
        ZoanForms forms = new ZoanForms(fruitId, color, sound, traits);
        forms.hybridAbility = ABILITIES.register(fruitId + "_hybrid", () -> new ZoanForm(forms, true));
        forms.beastAbility = ABILITIES.register(fruitId + "_beast", () -> new ZoanForm(forms, false));
        forms.hybrid = FORMS.register(fruitId + "_hybrid", () -> hybrid.apply(Transformation.builder(Transformation.Group.BODY))
                .source(forms.hybridAbility).build());
        forms.beast = FORMS.register(fruitId + "_beast", () -> beast.apply(Transformation.builder(Transformation.Group.BODY))
                .source(forms.beastAbility).build());
        return forms;
    }

    /** Loads every Zoan fruit class so its abilities and bodies are queued before the registers go to the bus. */
    public static void load() {
        HumanFruit.init();
        FalconFruit.init();
        JackalFruit.init();
        MoleFruit.init();
        BisonFruit.init();
        WolfFruit.init();
        GiraffeFruit.init();
        HorseFruit.init();
        AnacondaFruit.init();
        KingCobraFruit.init();
        AxolotlFruit.init();
        AlbatrossFruit.init();
        TanukiFruit.init();
        ElephantFruit.init();
        SabertoothFruit.init();
        MammothFruit.init();
        AllosaurusFruit.init();
        SpinosaurusFruit.init();
        PteranodonFruit.init();
        BrachiosaurusFruit.init();
        TriceratopsFruit.init();
        PachycephalosaurusFruit.init();
        SpiderFruit.init();
        PhoenixFruit.init();
        AzureDragonFruit.init();
        YamataFruit.init();
        NineTailedFoxFruit.init();
        OkuchiWolfFruit.init();
        BuddhaFruit.init();
        GyukiFruit.init();
        ItsumadeFruit.init();
        BakotsuFruit.init();
        FengxiFruit.init();
        SandwormFruit.init();
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
        FORMS.register(modBus);
        HumanFruit.behaviors();
        FalconFruit.behaviors();
        JackalFruit.behaviors();
        MoleFruit.behaviors();
        BisonFruit.behaviors();
        WolfFruit.behaviors();
        GiraffeFruit.behaviors();
        HorseFruit.behaviors();
        AnacondaFruit.behaviors();
        KingCobraFruit.behaviors();
        AxolotlFruit.behaviors();
        AlbatrossFruit.behaviors();
        TanukiFruit.behaviors();
        ElephantFruit.behaviors();
        SabertoothFruit.behaviors();
        MammothFruit.behaviors();
        AllosaurusFruit.behaviors();
        SpinosaurusFruit.behaviors();
        PteranodonFruit.behaviors();
        BrachiosaurusFruit.behaviors();
        TriceratopsFruit.behaviors();
        PachycephalosaurusFruit.behaviors();
        SpiderFruit.behaviors();
        PhoenixFruit.behaviors();
        AzureDragonFruit.behaviors();
        YamataFruit.behaviors();
        NineTailedFoxFruit.behaviors();
        OkuchiWolfFruit.behaviors();
        BuddhaFruit.behaviors();
        GyukiFruit.behaviors();
        ItsumadeFruit.behaviors();
        BakotsuFruit.behaviors();
        FengxiFruit.behaviors();
        SandwormFruit.behaviors();
    }
}
