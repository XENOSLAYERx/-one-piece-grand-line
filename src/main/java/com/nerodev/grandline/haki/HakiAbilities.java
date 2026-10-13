package com.nerodev.grandline.haki;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ability.Ability;
import com.nerodev.grandline.ability.GLAbilities;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Haki abilities. Each Haki type grants its own set once unlocked; advanced ones need a Haki level. */
public final class HakiAbilities {
    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(GLAbilities.KEY, GrandLine.MODID);

    // Observation
    public static final DeferredHolder<Ability, HakiMoves.Observation> OBSERVATION =
            ABILITIES.register("observation_haki", HakiMoves.Observation::new);
    public static final DeferredHolder<Ability, HakiMoves.FutureSight> FUTURE_SIGHT =
            ABILITIES.register("future_sight", HakiMoves.FutureSight::new);

    // Armament
    public static final DeferredHolder<Ability, HakiMoves.ArmamentArms> ARMAMENT =
            ABILITIES.register("armament_haki", HakiMoves.ArmamentArms::new);
    public static final DeferredHolder<Ability, HakiMoves.FullBody> FULL_BODY =
            ABILITIES.register("full_body_hardening", HakiMoves.FullBody::new);
    public static final DeferredHolder<Ability, HakiMoves.Emission> EMISSION =
            ABILITIES.register("armament_emission", HakiMoves.Emission::new);

    // Conqueror's
    public static final DeferredHolder<Ability, HakiMoves.ConquerorsHaki> CONQUERORS =
            ABILITIES.register("conquerors_haki", HakiMoves.ConquerorsHaki::new);
    public static final DeferredHolder<Ability, HakiMoves.Infusion> INFUSION =
            ABILITIES.register("conquerors_infusion", HakiMoves.Infusion::new);

    private HakiAbilities() {}

    /** The abilities a Haki type grants, weakest first. */
    public static List<Ability> of(HakiType type) {
        return switch (type) {
            case OBSERVATION -> List.of(OBSERVATION.get(), FUTURE_SIGHT.get());
            case ARMAMENT -> List.of(ARMAMENT.get(), FULL_BODY.get(), EMISSION.get());
            case CONQUERORS -> List.of(CONQUERORS.get(), INFUSION.get());
        };
    }

    public static void register(IEventBus modBus) {
        ABILITIES.register(modBus);
    }
}
