package com.nerodev.grandline.registry;

import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.definition.FactionDefinition;
import com.nerodev.grandline.definition.RaceDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Registry keys for every Grand Line content type.
 * <p>
 * Content definitions are datapack registries: each entry is one JSON file, so adding a race or faction means
 * adding a file, not editing a switch statement. (Devil Fruits and abilities carry behaviour, so they are code
 * registries: see GLFruits and GLAbilities.) Datapacks can add or override entries.
 * Entries are synced to clients so client UIs (guide, creator menu) can read them.
 * <p>
 * Datapack registries load when a world loads. Editing a JSON needs a world rejoin / server restart;
 * vanilla {@code /reload} does not reload registries.
 */
public final class GLRegistries {
    public static final ResourceKey<Registry<RaceDefinition>> RACE = key("race");
    public static final ResourceKey<Registry<FactionDefinition>> FACTION = key("faction");

    private GLRegistries() {}

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(GrandLine.id(name));
    }

    public static void onNewDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(RACE, RaceDefinition.CODEC, RaceDefinition.CODEC);
        event.dataPackRegistry(FACTION, FactionDefinition.CODEC, FactionDefinition.CODEC);
    }
}
