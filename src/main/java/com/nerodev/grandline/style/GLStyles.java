package com.nerodev.grandline.style;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.AbilityCategory;
import com.nerodev.grandline.ability.AbilityIcon;
import com.nerodev.grandline.ability.ActivationType;
import com.nerodev.grandline.ability.GLAbilities;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The fighting-style registry ({@code grandline:style}) and the styles' moves. */
public final class GLStyles {
    public static final ResourceKey<Registry<FightingStyle>> KEY = ResourceKey.createRegistryKey(GrandLine.id("style"));

    private static final DeferredRegister<FightingStyle> STYLES = DeferredRegister.create(KEY, GrandLine.MODID);
    public static final Registry<FightingStyle> REGISTRY = STYLES.makeRegistry(builder -> builder.sync(true));
    static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<FightingStyle, BrawlerStyle> BRAWLER = STYLES.register("brawler", BrawlerStyle::new);
    public static final DeferredHolder<FightingStyle, BlackLegStyle> BLACK_LEG = STYLES.register("black_leg", BlackLegStyle::new);
    public static final DeferredHolder<FightingStyle, FishmanKarateStyle> FISHMAN_KARATE = STYLES.register("fishman_karate",
            FishmanKarateStyle::new);
    public static final DeferredHolder<FightingStyle, SwordsmanStyle> SWORDSMAN = STYLES.register("swordsman", SwordsmanStyle::new);
    public static final DeferredHolder<FightingStyle, PolearmStyle> POLEARM = STYLES.register("polearm", PolearmStyle::new);
    public static final DeferredHolder<FightingStyle, GunslingerStyle> GUNSLINGER = STYLES.register("gunslinger", GunslingerStyle::new);

    private GLStyles() {}

    static <T extends Ability> DeferredHolder<Ability, T> ability(String name, Supplier<T> factory) {
        return ABILITIES.register(name, factory);
    }

    static Ability.Properties props(ActivationType type, AbilityIcon icon, int color) {
        return Ability.Properties.of(type, AbilityCategory.STYLE).icon(icon, color);
    }

    public static void register(IEventBus modBus) {
        BrawlerStyle.init();
        BlackLegStyle.init();
        FishmanKarateStyle.init();
        SwordsmanStyle.init();
        PolearmStyle.init();
        GunslingerStyle.init();
        STYLES.register(modBus);
        ABILITIES.register(modBus);
        FishmanKarateStyle.behaviors();
        SwordsmanStyle.behaviors();
        GunslingerStyle.behaviors();
    }
}
