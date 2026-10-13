package com.nerodev.grandline.transform;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.fruit.gum.GumAbilities;
import com.nerodev.grandline.fruit.leopard.LeopardAbilities;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registered body transformations. Synced so clients can resolve ids for rendering. */
public final class GLTransformations {
    public static final ResourceKey<Registry<Transformation>> KEY = ResourceKey.createRegistryKey(GrandLine.id("transformation"));

    private static final DeferredRegister<Transformation> TRANSFORMATIONS = DeferredRegister.create(KEY, GrandLine.MODID);
    public static final Registry<Transformation> REGISTRY = TRANSFORMATIONS.makeRegistry(builder -> builder.sync(true));

    // Gum-Gum: Gear Second pumps blood for speed; Gear Fifth (awakened) turns the body into cartoon rubber.
    public static final DeferredHolder<Transformation, Transformation> GEAR_SECOND = register("gear_second", () ->
            Transformation.builder(Transformation.Group.BODY)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.35, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_SPEED, 1.0, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.15, Operation.ADD_VALUE)
                    .source(GumAbilities.GEAR_SECOND)
                    .build());
    public static final DeferredHolder<Transformation, Transformation> GEAR_FIFTH = register("gear_fifth", () ->
            Transformation.builder(Transformation.Group.BODY)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.25, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.MAX_HEALTH, 10.0, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4.0, Operation.ADD_VALUE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.3, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 40.0, Operation.ADD_VALUE)
                    .source(GumAbilities.GEAR_FIFTH)
                    .build());

    // Cat-Cat Fruit, Model: Leopard. Hybrid keeps the human shape; full beast is low and fast.
    public static final DeferredHolder<Transformation, Transformation> LEOPARD_HYBRID = register("leopard_hybrid", () ->
            Transformation.builder(Transformation.Group.BODY)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.ATTACK_DAMAGE, 4.0, Operation.ADD_VALUE)
                    .modifier(Attributes.ARMOR, 6.0, Operation.ADD_VALUE)
                    .modifier(Attributes.MAX_HEALTH, 8.0, Operation.ADD_VALUE)
                    .modifier(Attributes.SCALE, 0.2, Operation.ADD_VALUE)
                    .source(LeopardAbilities.HYBRID_FORM)
                    .build());
    public static final DeferredHolder<Transformation, Transformation> LEOPARD_BEAST = register("leopard_beast", () ->
            Transformation.builder(Transformation.Group.BODY)
                    .modifier(Attributes.MOVEMENT_SPEED, 0.6, Operation.ADD_MULTIPLIED_BASE)
                    .modifier(Attributes.JUMP_STRENGTH, 0.25, Operation.ADD_VALUE)
                    .modifier(Attributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE)
                    .modifier(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)
                    .modifier(Attributes.SAFE_FALL_DISTANCE, 6.0, Operation.ADD_VALUE)
                    .dimensions(0.9f, 1.05f, 0.85f)
                    .source(LeopardAbilities.BEAST_FORM)
                    .build());

    private GLTransformations() {}

    private static DeferredHolder<Transformation, Transformation> register(String name, Supplier<Transformation> factory) {
        return TRANSFORMATIONS.register(name, factory);
    }

    public static void register(IEventBus modBus) {
        TRANSFORMATIONS.register(modBus);
    }
}
