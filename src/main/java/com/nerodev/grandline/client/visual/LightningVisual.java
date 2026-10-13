package com.nerodev.grandline.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A jagged bolt between two points: a dark core inside an additive glow (Conqueror's Haki: black lightning with
 * a red edge). The path is re-randomised every couple of ticks so the bolt flickers, and it may branch once.
 * Endpoints can follow moving entities through suppliers.
 */
public final class LightningVisual implements ClientVisuals.Visual {
    private static final RandomSource RANDOM = RandomSource.create();

    private final Supplier<Vec3> from;
    private final Supplier<Vec3> to;
    private final int glow;
    private final int core;
    private final float width;
    private final int duration;
    private final float jag;
    private final boolean branch;
    private List<Vec3> path = List.of();
    @Nullable
    private List<Vec3> fork;
    private int age;

    /**
     * @param glow  RGB of the additive outer glow
     * @param core  RGB of the opaque inner core
     * @param width core width in blocks (the glow is three times wider)
     * @param jag   sideways displacement as a fraction of the length
     */
    public LightningVisual(Supplier<Vec3> from, Supplier<Vec3> to, int glow, int core, float width, int duration, float jag,
            boolean branch) {
        this.from = from;
        this.to = to;
        this.glow = glow;
        this.core = core;
        this.width = width;
        this.duration = Math.max(1, duration);
        this.jag = jag;
        this.branch = branch;
        regenerate();
    }

    public static LightningVisual between(Vec3 a, Vec3 b, int glow, int core, float width, int duration) {
        return new LightningVisual(() -> a, () -> b, glow, core, width, duration, 0.18f, true);
    }

    @Override
    public boolean tick() {
        age++;
        if (age % 2 == 0) {
            regenerate();
        }
        return age < duration;
    }

    private void regenerate() {
        Vec3 a = from.get();
        Vec3 b = to.get();
        path = jagged(a, b, jag, 4);
        fork = null;
        if (branch && path.size() > 4 && RANDOM.nextFloat() < 0.6f) {
            Vec3 start = path.get(path.size() / 3 + RANDOM.nextInt(Math.max(1, path.size() / 3)));
            Vec3 dir = b.subtract(a);
            Vec3 offset = randomPerpendicular(dir).scale(dir.length() * 0.35).add(dir.scale(0.25));
            fork = jagged(start, start.add(offset), jag * 1.2f, 3);
        }
    }

    /** Midpoint displacement: {@code depth} levels of subdivision, each pushing the middle sideways. */
    private static List<Vec3> jagged(Vec3 a, Vec3 b, float jag, int depth) {
        List<Vec3> points = new ArrayList<>();
        points.add(a);
        points.add(b);
        double spread = a.distanceTo(b) * jag;
        for (int level = 0; level < depth; level++) {
            List<Vec3> next = new ArrayList<>(points.size() * 2);
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 p = points.get(i);
                Vec3 q = points.get(i + 1);
                next.add(p);
                Vec3 mid = p.add(q).scale(0.5).add(randomPerpendicular(q.subtract(p)).scale((RANDOM.nextDouble() - 0.5) * 2 * spread));
                next.add(mid);
            }
            next.add(points.getLast());
            points = next;
            spread *= 0.5;
        }
        return points;
    }

    private static Vec3 randomPerpendicular(Vec3 dir) {
        Vec3 random = new Vec3(RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5);
        Vec3 perp = random.cross(dir);
        return perp.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : perp.normalize();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float partialTick) {
        float life = 1f - Mth.clamp((age + partialTick) / duration, 0f, 1f);
        float fade = Math.min(1f, life * 2.5f);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer glowBuffer = buffers.getBuffer(RenderType.lightning());
        ribbon(glowBuffer, matrix, camera, path, width * 3f, glow, 0.55f * fade);
        if (fork != null) {
            ribbon(glowBuffer, matrix, camera, fork, width * 2f, glow, 0.4f * fade);
        }
        VertexConsumer coreBuffer = buffers.getBuffer(RenderType.debugQuads());
        ribbon(coreBuffer, matrix, camera, path, width, core, 0.95f * fade);
        if (fork != null) {
            ribbon(coreBuffer, matrix, camera, fork, width * 0.7f, core, 0.9f * fade);
        }
    }

    /** Camera-facing quads along the path, tapering toward both ends. */
    private static void ribbon(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, List<Vec3> points, float width, int rgb,
            float alpha) {
        if (alpha <= 0.01f || points.size() < 2) {
            return;
        }
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        int n = points.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3 p = points.get(i);
            Vec3 q = points.get(i + 1);
            Vec3 seg = q.subtract(p);
            Vec3 view = p.subtract(camera);
            Vec3 side = seg.cross(view);
            if (side.lengthSqr() < 1.0E-8) {
                continue;
            }
            float t0 = taper(i, n);
            float t1 = taper(i + 1, n);
            Vec3 s0 = side.normalize().scale(width * 0.5 * t0);
            Vec3 s1 = side.normalize().scale(width * 0.5 * t1);
            // Wound to face the camera: the lightning render type culls back faces.
            vertex(buffer, matrix, p.subtract(s0), camera, r, g, b, alpha);
            vertex(buffer, matrix, q.subtract(s1), camera, r, g, b, alpha);
            vertex(buffer, matrix, q.add(s1), camera, r, g, b, alpha);
            vertex(buffer, matrix, p.add(s0), camera, r, g, b, alpha);
        }
    }

    private static float taper(int i, int n) {
        float t = i / (float) (n - 1);
        return 0.35f + 0.65f * Mth.sin(t * Mth.PI);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 at, Vec3 camera, float r, float g, float b, float a) {
        buffer.addVertex(matrix, (float) (at.x - camera.x), (float) (at.y - camera.y), (float) (at.z - camera.z))
                .setColor(r, g, b, a);
    }
}
