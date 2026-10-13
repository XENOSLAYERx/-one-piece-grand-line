package com.nerodev.grandline.client.anim;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nerodev.grandline.GrandLine;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads {@link BodyAnimation}s from resource packs ({@code assets/<ns>/grandline_animations}). */
public final class AnimationLoader extends SimpleJsonResourceReloadListener {
    public static final AnimationLoader INSTANCE = new AnimationLoader();

    private Map<ResourceLocation, BodyAnimation> animations = Map.of();

    private AnimationLoader() {
        super(new Gson(), "grandline_animations");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BodyAnimation> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, BodyAnimation.fromJson(json.getAsJsonObject()));
            } catch (RuntimeException e) {
                GrandLine.LOGGER.error("Invalid body animation {}: {}", id, e.getMessage());
            }
        });
        animations = Map.copyOf(loaded);
        GrandLine.LOGGER.info("Loaded {} body animations", animations.size());
    }

    @Nullable
    public BodyAnimation get(ResourceLocation id) {
        return animations.get(id);
    }
}
