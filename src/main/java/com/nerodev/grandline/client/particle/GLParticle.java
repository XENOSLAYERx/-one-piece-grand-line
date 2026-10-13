package com.nerodev.grandline.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nerodev.grandline.fx.GLParticleOptions;
import com.nerodev.grandline.fx.GLParticles.Kind;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One particle class for every Grand Line particle kind. Kinds differ in motion, size curve, lighting and how the
 * quad is oriented (camera-facing, flat on the ground, facing a direction, or stretched along velocity).
 */
public class GLParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = 0xF000F0;

    private final Kind kind;
    private final SpriteSet sprites;
    private final float baseSize;
    private final Vector3f facing = new Vector3f(0, 1, 0);
    private final float spin;

    public GLParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
            GLParticleOptions options, Kind kind, SpriteSet sprites) {
        super(level, x, y, z);
        this.kind = kind;
        this.sprites = sprites;
        int color = options.color();
        this.rCol = ((color >> 16) & 0xFF) / 255f;
        this.gCol = ((color >> 8) & 0xFF) / 255f;
        this.bCol = (color & 0xFF) / 255f;
        this.hasPhysics = false;
        this.spin = (random.nextFloat() - 0.5f) * 0.6f;

        float scale = options.scale() <= 0 ? 1f : options.scale();
        int life;
        switch (kind) {
            case SPARK -> {
                setMotion(dx, dy, dz);
                friction = 0.86f;
                gravity = 0.4f;
                baseSize = 0.08f * scale;
                life = 8 + random.nextInt(6);
            }
            case GLOW -> {
                setMotion(dx, dy, dz);
                friction = 0.9f;
                baseSize = 0.25f * scale;
                life = 10 + random.nextInt(8);
            }
            case FLAME -> {
                setMotion(dx, dy, dz);
                friction = 0.9f;
                gravity = -0.25f;
                baseSize = 0.22f * scale;
                life = 10 + random.nextInt(8);
            }
            case SMOKE -> {
                setMotion(dx, dy, dz);
                friction = 0.92f;
                gravity = -0.05f;
                baseSize = 0.3f * scale;
                life = 18 + random.nextInt(12);
            }
            case RING -> {
                baseSize = 1.0f * scale;
                life = 10;
            }
            case SHOCKWAVE -> {
                if (dx * dx + dy * dy + dz * dz > 1.0E-6) {
                    facing.set((float) dx, (float) dy, (float) dz).normalize();
                }
                baseSize = 1.0f * scale;
                life = 8;
            }
            case STAR -> {
                setMotion(dx, dy, dz);
                friction = 0.8f;
                baseSize = 0.18f * scale;
                life = 8 + random.nextInt(6);
            }
            case SHARD -> {
                setMotion(dx, dy, dz);
                friction = 0.98f;
                gravity = 1.0f;
                hasPhysics = true;
                baseSize = 0.12f * scale;
                life = 20 + random.nextInt(15);
            }
            case STREAK -> {
                setMotion(dx, dy, dz);
                friction = 0.75f;
                baseSize = 0.35f * scale;
                life = 5 + random.nextInt(4);
            }
            default -> throw new IllegalStateException("Unhandled particle kind " + kind);
        }
        this.lifetime = options.lifetime() > 0 ? options.lifetime() : life;
        this.quadSize = baseSize;
        if (kind == Kind.FLAME || kind == Kind.SMOKE) {
            setSpriteFromAge(sprites);
        } else {
            pickSprite(sprites);
        }
    }

    private void setMotion(double dx, double dy, double dz) {
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) {
            return;
        }
        float t = (float) age / lifetime;
        if (kind == Kind.FLAME || kind == Kind.SMOKE) {
            setSpriteFromAge(sprites);
        }
        if (kind == Kind.SHARD) {
            oRoll = roll;
            roll += spin;
        }
        alpha = switch (kind) {
            case RING, SHOCKWAVE -> 1f - t;
            case SMOKE -> 0.7f * (1f - t);
            case STAR -> 0.5f + 0.5f * Mth.sin(t * Mth.PI);
            default -> 1f - t * t;
        };
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = Mth.clamp((age + partialTick) / lifetime, 0f, 1f);
        return switch (kind) {
            case SPARK, STREAK -> baseSize * (1f - t);
            case GLOW -> baseSize * (0.6f + 0.4f * Mth.sin(t * Mth.PI));
            case FLAME -> baseSize * (1f - 0.6f * t);
            case SMOKE -> baseSize * (0.6f + 0.9f * t);
            case RING, SHOCKWAVE -> baseSize * (0.15f + 0.85f * (1f - (1f - t) * (1f - t)));
            case STAR -> baseSize * (0.4f + 0.6f * Mth.sin(t * Mth.PI));
            case SHARD -> baseSize;
        };
    }

    @Override
    protected int getLightColor(float partialTick) {
        return kind == Kind.SMOKE || kind == Kind.SHARD ? super.getLightColor(partialTick) : FULL_BRIGHT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        switch (kind) {
            case RING -> {
                // Flat on the ground, visible from above and below.
                renderRotatedQuad(buffer, camera, new Quaternionf().rotationX(-Mth.HALF_PI), partialTick);
                renderRotatedQuad(buffer, camera, new Quaternionf().rotationX(Mth.HALF_PI), partialTick);
            }
            case SHOCKWAVE -> {
                Quaternionf front = new Quaternionf().rotationTo(0, 0, 1, facing.x, facing.y, facing.z);
                renderRotatedQuad(buffer, camera, front, partialTick);
                renderRotatedQuad(buffer, camera, new Quaternionf(front).rotateY(Mth.PI), partialTick);
            }
            case STREAK -> {
                // Turn the camera-facing quad so its long axis follows the on-screen direction of travel.
                Vector3f left = camera.getLeftVector();
                Vector3f up = camera.getUpVector();
                float sx = -(float) (xd * left.x() + yd * left.y() + zd * left.z());
                float sy = (float) (xd * up.x() + yd * up.y() + zd * up.z());
                roll = oRoll = (float) Math.atan2(sy, sx);
                super.render(buffer, camera, partialTick);
            }
            default -> super.render(buffer, camera, partialTick);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
