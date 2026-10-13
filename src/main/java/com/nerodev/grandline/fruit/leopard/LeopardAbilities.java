package com.nerodev.grandline.fruit.leopard;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.GLAbilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Abilities of the Cat-Cat Fruit, Model: Leopard (Neko Neko no Mi, Model: Leopard). */
public final class LeopardAbilities {
    public static final String MASTERY = "fruit/grandline:leopard";
    public static final int COLOR = 0xE0A43A;

    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    public static final DeferredHolder<Ability, LeopardMoves.Form> HYBRID_FORM = ABILITIES.register("leopard_hybrid",
            () -> new LeopardMoves.Form(true));
    public static final DeferredHolder<Ability, LeopardMoves.Form> BEAST_FORM = ABILITIES.register("leopard_beast",
            () -> new LeopardMoves.Form(false));
    public static final DeferredHolder<Ability, LeopardMoves.Pounce> POUNCE = ABILITIES.register("leopard_pounce", LeopardMoves.Pounce::new);
    public static final DeferredHolder<Ability, LeopardMoves.Claws> CLAWS = ABILITIES.register("leopard_claws", LeopardMoves.Claws::new);
    public static final DeferredHolder<Ability, LeopardMoves.Frenzy> FRENZY = ABILITIES.register("leopard_frenzy", LeopardMoves.Frenzy::new);
    public static final DeferredHolder<Ability, LeopardMoves.PrimalFury> PRIMAL_FURY = ABILITIES.register("leopard_primal_fury",
            LeopardMoves.PrimalFury::new);
    public static final DeferredHolder<Ability, LeopardMoves.Roar> ROAR = ABILITIES.register("leopard_roar", LeopardMoves.Roar::new);

    private LeopardAbilities() {}

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
    }
}
