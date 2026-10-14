package com.nerodev.grandline.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.GrandLine;
import com.nerodev.grandline.ship.SailStyle;
import com.nerodev.grandline.ship.ShipDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A ship's model, built from its {@link ShipDefinition} instead of a hand-made model: hull, bow, castle, deck,
 * rails, masts, yards, sails, cannons, wheel and figurehead, in ship space (x to port, y up from the keel, z toward
 * the bow, all in blocks). Solid parts are textured from a tile atlas, one tile per block so the wood stays crisp at
 * any size, and tinted with the ship's colours. Sails are cloth grids that billow with the ship's speed.
 */
public final class ShipMesh {
    public static final ResourceLocation ATLAS = GrandLine.id("textures/entity/ship/atlas.png");
    public static final ResourceLocation CANVAS = GrandLine.id("textures/entity/ship/sail_canvas.png");

    /** Tiles of the 4x4 atlas. */
    enum Tile {
        HULL(0, true), DECK(1, false), TRIM(2, true), MAST(3, false), METAL(4, false), GOLD(5, false), WINDOW(6, false), LOG(7, true),
        LION(8, false), MANE(9, false);

        final int index;
        final boolean tinted;

        Tile(int index, boolean tinted) {
            this.index = index;
            this.tinted = tinted;
        }
    }

    private record Quad(float[] xyz, float[] uv, float nx, float ny, float nz, int color) {}

    /** A sail: a cloth rectangle hanging at {@code z} between two heights, bulging toward the bow. */
    private record Sail(float halfWidthTop, float halfWidthBottom, float top, float bottom, float z, boolean emblem) {}

    private record Flag(float x, float y, float z) {}

    private final List<Quad> quads = new ArrayList<>();
    private final List<Sail> sails = new ArrayList<>();
    private final List<Flag> flags = new ArrayList<>();
    private final SailStyle style;
    private final int sailColor;
    private final int hullColor;
    private final int trimColor;

    private ShipMesh(ShipDefinition def) {
        this.style = def.sail();
        this.sailColor = 0xFF000000 | def.sailColor();
        this.hullColor = 0xFF000000 | def.hullColor();
        this.trimColor = 0xFF000000 | def.trimColor();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Building
    // ---------------------------------------------------------------------------------------------------------

    public static ShipMesh build(ShipDefinition def) {
        ShipMesh mesh = new ShipMesh(def);
        if (def.length() <= 5) {
            mesh.raft(def);
        } else {
            mesh.hull(def);
        }
        mesh.rigging(def);
        mesh.fittings(def);
        return mesh;
    }

    private void raft(ShipDefinition def) {
        float l = def.length() / 2;
        float w = def.width() / 2;
        int logs = Math.max(3, Math.round(def.width() / 0.5f));
        float step = def.width() / logs;
        for (int i = 0; i < logs; i++) {
            float x0 = -w + i * step;
            box(x0 + 0.02f, 0, -l + (i % 2) * 0.2f, x0 + step - 0.02f, def.hullHeight(), l - ((i + 1) % 2) * 0.2f, Tile.LOG, hullColor);
        }
        box(-w + 0.1f, def.hullHeight() * 0.4f, -l + 0.4f, w - 0.1f, def.hullHeight() * 0.6f, -l + 0.6f, Tile.TRIM, trimColor);
        box(-w + 0.1f, def.hullHeight() * 0.4f, l - 0.6f, w - 0.1f, def.hullHeight() * 0.6f, l - 0.4f, Tile.TRIM, trimColor);
    }

    private void hull(ShipDefinition def) {
        float l = def.length() / 2;
        float w = def.width() / 2;
        float h = def.hullHeight();
        float bowStart = l - def.length() * 0.2f;
        // Keel and the body of the hull.
        box(-w + 0.4f, 0, -l + 0.8f, w - 0.4f, 0.45f, bowStart, Tile.HULL, darker(hullColor, 0.7f));
        box(-w, 0.4f, -l, w, h, bowStart, Tile.HULL, hullColor);
        // The bow narrows and rises in steps toward the stem.
        float segment = (l - bowStart) / 4;
        for (int k = 0; k < 4; k++) {
            float f = (k + 1) / 5f;
            float half = w * (1 - f * 0.85f);
            float z0 = bowStart + k * segment;
            box(-half, 0.45f + k * 0.12f * h, z0, half, h + k * 0.08f, z0 + segment, Tile.HULL, hullColor);
        }
        // Deck planking and a painted band along the top of the hull.
        box(-w + 0.12f, h, -l + 0.12f, w - 0.12f, h + 0.02f, bowStart, Tile.DECK, 0xFFFFFFFF);
        box(-w - 0.03f, h - 0.32f, -l - 0.03f, w + 0.03f, h - 0.12f, bowStart, Tile.TRIM, trimColor);
        // Rails.
        box(w - 0.12f, h, -l, w, h + 0.55f, bowStart, Tile.TRIM, trimColor);
        box(-w, h, -l, -w + 0.12f, h + 0.55f, bowStart, Tile.TRIM, trimColor);
        box(-w, h, -l, w, h + 0.55f, -l + 0.12f, Tile.TRIM, trimColor);
        if (def.castle()) {
            float end = -l + def.length() * 0.22f;
            box(-w, h, -l, w, h + 1.4f, end, Tile.HULL, hullColor);
            box(-w + 0.12f, h + 1.4f, -l + 0.12f, w - 0.12f, h + 1.42f, end - 0.05f, Tile.DECK, 0xFFFFFFFF);
            box(w - 0.12f, h + 1.4f, -l, w, h + 1.9f, end, Tile.TRIM, trimColor);
            box(-w, h + 1.4f, -l, -w + 0.12f, h + 1.9f, end, Tile.TRIM, trimColor);
            box(-w, h + 1.4f, end - 0.12f, w, h + 1.9f, end, Tile.TRIM, trimColor);
            box(-w + 0.4f, h + 0.3f, -l - 0.04f, w - 0.4f, h + 1.1f, -l, Tile.WINDOW, 0xFFFFFFFF);
            if (def.length() >= 16) {
                box(-w * 0.85f, h, bowStart - def.length() * 0.1f, w * 0.85f, h + 0.8f, bowStart, Tile.HULL, hullColor);
            }
        }
        // Gun ports along both sides.
        for (int side = -1; side <= 1; side += 2) {
            for (Vec3 cannon : def.cannonPositions(side)) {
                float z = (float) cannon.z;
                float y = (float) cannon.y;
                float x = side * (w + 0.01f);
                box(Math.min(x, x + side * 0.02f), y - 0.3f, z - 0.3f, Math.max(x, x + side * 0.02f), y + 0.3f, z + 0.3f, Tile.METAL,
                        0xFF101010);
            }
        }
    }

    private void rigging(ShipDefinition def) {
        if (def.masts() <= 0) {
            return;
        }
        float deck = def.hullHeight();
        float l = def.length() / 2;
        float w = def.width() / 2;
        for (int i = 0; i < def.masts(); i++) {
            float z = def.masts() == 1 ? def.length() * 0.05f : -def.length() * 0.18f + def.length() * 0.46f * i / (def.masts() - 1);
            boolean main = def.masts() == 1 || (def.masts() == 3 ? i == 1 : i == 0);
            float height = def.mastHeight() * (main ? 1f : 0.82f);
            float thick = Math.max(0.12f, Math.min(0.24f, def.length() / 80));
            box(-thick, deck, z - thick, thick, deck + height, z + thick, Tile.MAST, 0xFFFFFFFF);
            float yardTop = deck + height * 0.92f;
            float yardMid = deck + height * 0.55f;
            float span = Math.max(1.2f, w * 1.15f);
            box(-span, yardTop - 0.07f, z - 0.07f, span, yardTop + 0.07f, z + 0.07f, Tile.MAST, 0xFFFFFFFF);
            if (height > 3) {
                box(-span * 1.1f, yardMid - 0.08f, z - 0.08f, span * 1.1f, yardMid + 0.08f, z + 0.08f, Tile.MAST, 0xFFFFFFFF);
                sails.add(new Sail(span * 0.92f, span * 1.0f, yardTop - 0.1f, yardMid + 0.15f, z + 0.12f, false));
                sails.add(new Sail(span * 1.02f, span * 1.1f, yardMid - 0.1f, deck + height * 0.16f, z + 0.12f, main));
            } else {
                sails.add(new Sail(span * 0.95f, span, yardTop - 0.1f, deck + 0.5f, z + 0.12f, main));
            }
            if (main && def.length() >= 12) {
                float nest = deck + height * 0.78f;
                box(-0.6f, nest, z - 0.6f, 0.6f, nest + 0.4f, z + 0.6f, Tile.TRIM, trimColor);
            }
            if (main && style != SailStyle.PLAIN) {
                flags.add(new Flag(0, deck + height, z));
            }
        }
        // Bowsprit.
        if (def.length() > 5) {
            box(-0.09f, deck + 0.2f, l - 0.6f, 0.09f, deck + 0.38f, l + def.length() * 0.12f, Tile.MAST, 0xFFFFFFFF);
        }
    }

    private void fittings(ShipDefinition def) {
        float l = def.length() / 2;
        float h = def.hullHeight();
        // Cannons poking out of the gun ports.
        for (int side = -1; side <= 1; side += 2) {
            for (Vec3 cannon : def.cannonPositions(side)) {
                float x = (float) cannon.x;
                float y = (float) cannon.y;
                float z = (float) cannon.z;
                box(Math.min(x - side * 0.5f, x + side * 0.45f), y - 0.16f, z - 0.16f, Math.max(x - side * 0.5f, x + side * 0.45f), y + 0.16f,
                        z + 0.16f, Tile.METAL, 0xFFFFFFFF);
            }
        }
        // The wheel at the helm.
        Vec3 helm = def.seats().get(0);
        float hz = (float) helm.z + 0.75f;
        float hy = (float) helm.y;
        box(-0.07f, hy, hz - 0.07f, 0.07f, hy + 0.9f, hz + 0.07f, Tile.MAST, 0xFFFFFFFF);
        box(-0.45f, hy + 0.55f, hz + 0.07f, 0.45f, hy + 1.45f, hz + 0.14f, Tile.TRIM, darker(trimColor, 0.8f));
        if (def.length() <= 5) {
            return;
        }
        // Figurehead.
        if (style == SailStyle.STRAW_HAT) {
            box(-0.75f, h - 0.3f, l - 0.6f, 0.75f, h + 1.2f, l + 0.5f, Tile.MANE, 0xFFFFFFFF);
            box(-0.55f, h - 0.15f, l + 0.5f, 0.55f, h + 1.0f, l + 0.75f, Tile.LION, 0xFFFFFFFF);
        } else if (style == SailStyle.MARINE) {
            box(-0.3f, h - 0.2f, l - 0.2f, 0.3f, h + 0.6f, l + 0.6f, Tile.HULL, 0xFFF0F0F0);
            box(-0.12f, h + 0.3f, l + 0.6f, 0.12f, h + 0.45f, l + 0.95f, Tile.GOLD, 0xFFFFFFFF);
        } else {
            box(-0.3f, h * 0.5f, l - 0.3f, 0.3f, h + 0.4f, l + 0.5f, Tile.GOLD, 0xFFFFFFFF);
        }
    }

    private static int darker(int argb, float f) {
        int r = (int) (((argb >> 16) & 255) * f);
        int g = (int) (((argb >> 8) & 255) * f);
        int b = (int) ((argb & 255) * f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Adds a box, each face cut into block-sized cells so the tile repeats once per block. */
    private void box(float x0, float y0, float z0, float x1, float y1, float z1, Tile tile, int color) {
        int tint = color;
        // +Y and -Y faces span x and z.
        face(x0, x1, z0, z1, (a, b) -> new float[] {a, y1, b}, 0, 1, 0, tile, tint, true);
        face(x0, x1, z0, z1, (a, b) -> new float[] {a, y0, b}, 0, -1, 0, tile, shade(tint, 0.6f), false);
        // +Z and -Z faces span x and y.
        face(x0, x1, y0, y1, (a, b) -> new float[] {a, b, z1}, 0, 0, 1, tile, shade(tint, 0.9f), false);
        face(x0, x1, y0, y1, (a, b) -> new float[] {a, b, z0}, 0, 0, -1, tile, shade(tint, 0.9f), true);
        // +X and -X faces span z and y.
        face(z0, z1, y0, y1, (a, b) -> new float[] {x1, b, a}, 1, 0, 0, tile, shade(tint, 0.8f), true);
        face(z0, z1, y0, y1, (a, b) -> new float[] {x0, b, a}, -1, 0, 0, tile, shade(tint, 0.8f), false);
    }

    private interface Point {
        float[] at(float a, float b);
    }

    private void face(float a0, float a1, float b0, float b1, Point point, float nx, float ny, float nz, Tile tile, int color, boolean flip) {
        float tu = (tile.index % 4) * 0.25f;
        float tv = (tile.index / 4) * 0.25f;
        for (float a = a0; a < a1 - 1.0E-4f; a += 1f) {
            float an = Math.min(a1, a + 1f);
            for (float b = b0; b < b1 - 1.0E-4f; b += 1f) {
                float bn = Math.min(b1, b + 1f);
                float u0 = tu;
                float u1 = tu + 0.25f * (an - a);
                float v1 = tv + 0.25f;
                float v0 = v1 - 0.25f * (bn - b);
                float[] p00 = point.at(a, b);
                float[] p10 = point.at(an, b);
                float[] p11 = point.at(an, bn);
                float[] p01 = point.at(a, bn);
                float[] xyz;
                float[] uv;
                if (flip) {
                    xyz = concat(p00, p01, p11, p10);
                    uv = new float[] {u0, v1, u0, v0, u1, v0, u1, v1};
                } else {
                    xyz = concat(p00, p10, p11, p01);
                    uv = new float[] {u0, v1, u1, v1, u1, v0, u0, v0};
                }
                quads.add(new Quad(xyz, uv, nx, ny, nz, color));
            }
        }
    }

    private static float[] concat(float[]... points) {
        float[] out = new float[12];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(points[i], 0, out, i * 3, 3);
        }
        return out;
    }

    private static int shade(int argb, float f) {
        return darker(argb, f);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------------------------

    public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float time, float speed) {
        PoseStack.Pose last = pose.last();
        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
        for (Quad q : quads) {
            for (int i = 0; i < 4; i++) {
                solid.addVertex(last, q.xyz[i * 3], q.xyz[i * 3 + 1], q.xyz[i * 3 + 2]).setColor(q.color).setUv(q.uv[i * 2], q.uv[i * 2 + 1])
                        .setOverlay(overlay).setLight(light).setNormal(last, q.nx, q.ny, q.nz);
            }
        }
        float billow = 0.25f + Math.min(1f, Math.abs(speed) * 3f) * 0.6f;
        for (Sail sail : sails) {
            drawSail(pose, buffers, sail, light, overlay, time, billow);
        }
        for (Flag flag : flags) {
            drawFlag(pose, buffers, flag, light, overlay, time);
        }
    }

    private ResourceLocation emblem() {
        return GrandLine.id("textures/entity/ship/sail_" + style.getSerializedName().toLowerCase(Locale.ROOT) + ".png");
    }

    private void drawSail(PoseStack pose, MultiBufferSource buffers, Sail sail, int light, int overlay, float time, float billow) {
        boolean emblem = sail.emblem && style != SailStyle.PLAIN;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(emblem ? emblem() : CANVAS));
        int color = emblem ? 0xFFFFFFFF : sailColor;
        PoseStack.Pose last = pose.last();
        int n = 6;
        float flutter = Mth.sin(time * 0.15f) * 0.04f;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float[] u = {i / (float) n, (i + 1) / (float) n};
                float[] v = {j / (float) n, (j + 1) / (float) n};
                float[][] corners = {{u[0], v[0]}, {u[1], v[0]}, {u[1], v[1]}, {u[0], v[1]}};
                for (float[] c : corners) {
                    float y = Mth.lerp(c[1], sail.top, sail.bottom);
                    float half = Mth.lerp(c[1], sail.halfWidthTop, sail.halfWidthBottom);
                    float x = Mth.lerp(c[0], half, -half);
                    float bulge = (billow + flutter) * Mth.sin(c[0] * Mth.PI) * Mth.sin(Math.min(1f, c[1] * 1.2f) * Mth.PI * 0.85f);
                    buffer.addVertex(last, x, y, sail.z + bulge).setColor(color).setUv(c[0], c[1]).setOverlay(overlay).setLight(light)
                            .setNormal(last, 0, 0, 1);
                }
            }
        }
    }

    private void drawFlag(PoseStack pose, MultiBufferSource buffers, Flag flag, int light, int overlay, float time) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(
                GrandLine.id("textures/entity/ship/flag_" + style.getSerializedName() + ".png")));
        PoseStack.Pose last = pose.last();
        int n = 4;
        float width = 1.4f;
        float height = 0.8f;
        for (int i = 0; i < n; i++) {
            float[] u = {i / (float) n, (i + 1) / (float) n};
            float[][] corners = {{u[0], 0}, {u[1], 0}, {u[1], 1}, {u[0], 1}};
            for (float[] c : corners) {
                float wave = Mth.sin(time * 0.3f - c[0] * 4f) * 0.15f * c[0];
                buffer.addVertex(last, flag.x + wave, flag.y - c[1] * height + 0.1f, flag.z - 0.1f - c[0] * width).setColor(0xFFFFFFFF)
                        .setUv(c[0], c[1]).setOverlay(overlay).setLight(light).setNormal(last, 1, 0, 0);
            }
        }
    }
}
