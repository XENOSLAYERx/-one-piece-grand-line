package com.nerodev.grandline.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Short-lived world-space visuals that are not particles: stretching limbs, giant fists. Ticked every client tick
 * and drawn after entities. Capped so a burst of effects can never pile up.
 */
public final class ClientVisuals {
    private static final int MAX = 64;

    public interface Visual {
        /** @return false when finished */
        boolean tick();

        void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float partialTick);
    }

    private static final List<Visual> ACTIVE = new ArrayList<>();

    private ClientVisuals() {}

    public static void add(Visual visual) {
        if (ACTIVE.size() < MAX) {
            ACTIVE.add(visual);
        }
    }

    public static void tick() {
        if (Minecraft.getInstance().level == null) {
            ACTIVE.clear();
            return;
        }
        Iterator<Visual> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (!it.next().tick()) {
                it.remove();
            }
        }
    }

    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        for (Visual visual : ACTIVE) {
            visual.render(pose, buffers, camera, partial);
        }
        buffers.endBatch();
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
