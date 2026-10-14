package com.nerodev.grandline.client.model;

import com.nerodev.grandline.GrandLine;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client look of every Zoan body, found by transformation id: {@code grandline:<fruit>_beast} draws the beast model in
 * place of the player, {@code grandline:<fruit>_hybrid} wears its parts and a fur or scale overlay on the player. Race
 * features ({@code race_<race>_parts}) come from the same generated data.
 */
public final class ZoanLooks {
    /** Bodies made of light (blue flames and the like) are drawn fully bright. */
    private static final Set<String> GLOWING = Set.of("phoenix", "buddha");

    public record Beast(BeastShape shape, boolean glowing) {}

    public record Hybrid(BeastShape parts, ResourceLocation overlay, boolean glowing) {}

    private static final Map<ResourceLocation, Beast> BEASTS = new HashMap<>();
    private static final Map<ResourceLocation, Hybrid> HYBRIDS = new HashMap<>();
    /** Race features (Mink ears and tail, Fish-Man fins, Skypiean wings) by race id. */
    private static final Map<ResourceLocation, BeastShape> RACE_PARTS = new HashMap<>();
    private static final Map<BeastShape, BeastModel> MODELS = new HashMap<>();

    private ZoanLooks() {}

    private static void load() {
        if (!BEASTS.isEmpty()) {
            return;
        }
        Map<String, String> data = ZoanShapeData.all();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String id = e.getKey();
            if (id.startsWith("race_") && id.endsWith("_parts")) {
                String race = id.substring("race_".length(), id.length() - "_parts".length());
                RACE_PARTS.put(GrandLine.id(race), BeastShape.parse(e.getValue()));
                continue;
            }
            if (id.endsWith("_parts")) {
                continue;
            }
            BeastShape beast = BeastShape.parse(e.getValue());
            BeastShape parts = BeastShape.parse(data.get(id + "_parts"));
            boolean glowing = GLOWING.contains(id);
            BEASTS.put(GrandLine.id(id + "_beast"), new Beast(beast, glowing));
            HYBRIDS.put(GrandLine.id(id + "_hybrid"),
                    new Hybrid(parts, GrandLine.id("textures/entity/zoan/" + id + "_overlay.png"), glowing));
        }
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        load();
        for (Beast beast : BEASTS.values()) {
            event.registerLayerDefinition(beast.shape().layer(), beast.shape()::createLayer);
        }
        for (Hybrid hybrid : HYBRIDS.values()) {
            event.registerLayerDefinition(hybrid.parts().layer(), hybrid.parts()::createLayer);
        }
        for (BeastShape parts : RACE_PARTS.values()) {
            event.registerLayerDefinition(parts.layer(), parts::createLayer);
        }
    }

    /** The features every member of {@code race} wears, or null for races that look human. */
    @Nullable
    public static BeastShape raceParts(ResourceLocation race) {
        load();
        return RACE_PARTS.get(race);
    }

    @Nullable
    public static Beast beast(ResourceLocation transformation) {
        load();
        return BEASTS.get(transformation);
    }

    @Nullable
    public static Hybrid hybrid(ResourceLocation transformation) {
        load();
        return HYBRIDS.get(transformation);
    }

    /** The baked model for a shape, made on first use. */
    public static BeastModel model(BeastShape shape) {
        return MODELS.computeIfAbsent(shape, s -> new BeastModel(Minecraft.getInstance().getEntityModels().bakeLayer(s.layer()), s));
    }

    /** Forgets baked models (when the world is left), so they are rebuilt from the current model set. */
    public static void clearModels() {
        MODELS.clear();
    }
}
