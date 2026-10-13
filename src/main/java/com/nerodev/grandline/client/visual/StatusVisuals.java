package com.nerodev.grandline.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.client.fx.FxKit;
import com.nerodev.grandline.fx.GLParticles;
import com.nerodev.grandline.fx.Vfx.StatusLook;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * Encased and bound creatures: a shell around the whole body (ice, stone, wax, candy, gold, mochi, sand) or bands
 * around the legs (iron bonds, strings, roots, glue). Driven by the status packet, since the server does not send
 * other entities' effects to clients.
 */
public final class StatusVisuals {
    public static final ResourceLocation TEXTURE = GrandLine.id("textures/entity/encase.png");

    private record Status(StatusLook look, long until) {}

    private static final Map<Integer, Status> ACTIVE = new HashMap<>();

    private StatusVisuals() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Post.class, StatusVisuals::onRenderPost);
    }

    public static void apply(Entity entity, StatusLook look, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && ticks > 0) {
            ACTIVE.put(entity.getId(), new Status(look, level.getGameTime() + ticks));
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Status>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Status> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive()) {
                it.remove();
            } else if (now >= entry.getValue().until()) {
                it.remove();
                shatter(level, entity, entry.getValue().look());
            }
        }
    }

    private static void shatter(ClientLevel level, Entity entity, StatusLook look) {
        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        var kind = switch (look) {
            case ICE, SNOW -> GLParticles.SNOW;
            case SMOKE -> GLParticles.SMOKE;
            case CANDY, STONE, GOLD, SAND -> GLParticles.SHARD;
            case ROOTS -> GLParticles.LEAF;
            default -> GLParticles.DROP;
        };
        FxKit.burst(level, kind, look.color(), 1.1f, center, 14, 0.2);
    }

    private static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Status status = ACTIVE.get(entity.getId());
        if (status == null || entity.isInvisible()) {
            return;
        }
        StatusLook look = status.look();
        PoseStack pose = event.getPoseStack();
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(TEXTURE));
        float w = entity.getBbWidth() / 2f + 0.1f;
        float h = entity.getBbHeight() + 0.1f;
        int light = event.getPackedLight();
        if (look.shell()) {
            box(pose.last(), buffer, -w, -0.02f, -w, w, h, w, look, light);
        } else {
            for (float y : new float[] {0.12f, 0.32f, 0.52f}) {
                float at = h * y;
                box(pose.last(), buffer, -w - 0.02f, at, -w - 0.02f, w + 0.02f, at + 0.09f, w + 0.02f, look, light);
            }
        }
    }

    private static void box(PoseStack.Pose pose, VertexConsumer b, float x0, float y0, float z0, float x1, float y1, float z1,
            StatusLook look, int light) {
        int c = look.color();
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int bl = c & 0xFF;
        int a = (int) (look.alpha() * 255);
        Matrix4f m = pose.pose();
        float u = 1f;
        float v = 1f;
        // six faces, outward normals
        quad(b, pose, m, r, g, bl, a, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u, v);
        quad(b, pose, m, r, g, bl, a, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u, v);
        quad(b, pose, m, r, g, bl, a, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u, v);
        quad(b, pose, m, r, g, bl, a, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u, v);
        quad(b, pose, m, r, g, bl, a, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u, u);
        quad(b, pose, m, r, g, bl, a, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u, u);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, Matrix4f m, int r, int g, int bl, int a, int light,
            float nx, float ny, float nz, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
            float cz, float dx, float dy, float dz, float u, float v) {
        vertex(b, pose, m, r, g, bl, a, light, nx, ny, nz, ax, ay, az, 0, v);
        vertex(b, pose, m, r, g, bl, a, light, nx, ny, nz, bx, by, bz, u, v);
        vertex(b, pose, m, r, g, bl, a, light, nx, ny, nz, cx, cy, cz, u, 0);
        vertex(b, pose, m, r, g, bl, a, light, nx, ny, nz, dx, dy, dz, 0, 0);
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose pose, Matrix4f m, int r, int g, int bl, int a, int light,
            float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        b.addVertex(m, x, y, z).setColor(r, g, bl, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
