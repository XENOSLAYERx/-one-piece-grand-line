package com.nerodev.grandline.client.model;

import com.nerodev.grandline.GrandLine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Geometry of one Zoan body (a full beast, or the parts a hybrid wears on the player model), parsed from the data
 * that tools/gen_zoans.py writes into {@link ZoanShapeData}. Every part carries an animation role.
 */
public final class BeastShape {
    /** How {@link BeastModel} animates a part. */
    public enum Role {
        FIXED, BODY, HEAD, NECK, JAW, LEG_FRONT, LEG_BACK, LEG, ARM, TAIL, WING, WING_TIP, SEGMENT, SPIDER_LEG, EAR, FIN
    }

    /** Which bone of the player model a hybrid part rides on (NONE for beasts and child parts). */
    public enum Anchor {
        NONE, HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG
    }

    public record Box(int u, int v, float x, float y, float z, int w, int h, int d, float inflate) {}

    public record Part(String name, int parent, Anchor anchor, float px, float py, float pz, float rx, float ry, float rz,
            Role role, float param, List<Box> boxes) {}

    private final String id;
    private final int texWidth;
    private final int texHeight;
    private final float scale;
    private final float gait;
    private final List<Part> parts;
    private final ModelLayerLocation layer;
    private final ResourceLocation texture;

    private BeastShape(String id, int texWidth, int texHeight, float scale, float gait, List<Part> parts) {
        this.id = id;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.scale = scale;
        this.gait = gait;
        this.parts = List.copyOf(parts);
        this.layer = new ModelLayerLocation(GrandLine.id("zoan/" + id), "main");
        this.texture = GrandLine.id("textures/entity/zoan/" + id + ".png");
    }

    public String id() {
        return id;
    }

    public float scale() {
        return scale;
    }

    public float gait() {
        return gait;
    }

    public List<Part> parts() {
        return parts;
    }

    public ModelLayerLocation layer() {
        return layer;
    }

    public ResourceLocation texture() {
        return texture;
    }

    /**
     * Parses {@code id,texW,texH,scale,gait|part|part...}; a part is
     * {@code name,parent,anchor,px,py,pz,rx,ry,rz,ROLE,param:box;box}, a box {@code u,v,x,y,z,w,h,d,inflate}.
     */
    public static BeastShape parse(String data) {
        String[] chunks = data.split("\\|");
        String[] head = chunks[0].split(",");
        List<Part> parts = new ArrayList<>();
        for (int i = 1; i < chunks.length; i++) {
            String[] halves = chunks[i].split(":", -1);
            String[] f = halves[0].split(",");
            List<Box> boxes = new ArrayList<>();
            if (!halves[1].isEmpty()) {
                for (String b : halves[1].split(";")) {
                    String[] v = b.split(",");
                    boxes.add(new Box(Integer.parseInt(v[0]), Integer.parseInt(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3]),
                            Float.parseFloat(v[4]), Integer.parseInt(v[5]), Integer.parseInt(v[6]), Integer.parseInt(v[7]),
                            Float.parseFloat(v[8])));
                }
            }
            parts.add(new Part(f[0], Integer.parseInt(f[1]), Anchor.valueOf(f[2]), Float.parseFloat(f[3]), Float.parseFloat(f[4]),
                    Float.parseFloat(f[5]), Float.parseFloat(f[6]), Float.parseFloat(f[7]), Float.parseFloat(f[8]), Role.valueOf(f[9]),
                    Float.parseFloat(f[10]), boxes));
        }
        return new BeastShape(head[0], Integer.parseInt(head[1]), Integer.parseInt(head[2]), Float.parseFloat(head[3]),
                Float.parseFloat(head[4]), parts);
    }

    public LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition[] defs = new PartDefinition[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            CubeListBuilder cubes = CubeListBuilder.create();
            for (Box b : p.boxes()) {
                cubes.texOffs(b.u(), b.v()).addBox(b.x(), b.y(), b.z(), b.w(), b.h(), b.d(), new CubeDeformation(b.inflate()));
            }
            PartDefinition parent = p.parent() < 0 ? mesh.getRoot() : defs[p.parent()];
            defs[i] = parent.addOrReplaceChild(p.name(), cubes, PartPose.offsetAndRotation(p.px(), p.py(), p.pz(), p.rx(), p.ry(), p.rz()));
        }
        return LayerDefinition.create(mesh, texWidth, texHeight);
    }
}
