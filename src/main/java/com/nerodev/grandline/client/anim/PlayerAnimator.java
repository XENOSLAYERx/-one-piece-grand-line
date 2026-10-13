package com.nerodev.grandline.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nerodev.grandline.client.anim.BodyAnimation.Bone;
import com.nerodev.grandline.fx.GLEffects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Plays {@link BodyAnimation}s on humanoid entities. The model pose is applied after vanilla's own animation
 * (see HumanoidModelMixin) and whole-body motion through the renderer's rotation step (LivingEntityRendererMixin),
 * blended in and out so animations never snap.
 */
public final class PlayerAnimator {
    private static final float ROOT_PIVOT = 0.9f;
    private static final Map<Integer, Playing> PLAYING = new HashMap<>();

    private static final class Playing {
        final BodyAnimation animation;
        final float start;
        float stoppedAt = Float.NaN;

        Playing(BodyAnimation animation, float start) {
            this.animation = animation;
            this.start = start;
        }
    }

    private PlayerAnimator() {}

    public static void play(int entityId, ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        if (id.equals(GLEffects.ANIM_STOP)) {
            Playing playing = PLAYING.get(entityId);
            if (playing != null && Float.isNaN(playing.stoppedAt)) {
                playing.stoppedAt = entity.tickCount;
            }
            return;
        }
        BodyAnimation animation = AnimationLoader.INSTANCE.get(id);
        if (animation != null) {
            PLAYING.put(entityId, new Playing(animation, entity.tickCount));
        }
    }

    public static boolean isPlaying(Entity entity) {
        return PLAYING.containsKey(entity.getId());
    }

    /** Drops finished animations and animations of entities that are gone. Called once per client tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PLAYING.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Playing>> it = PLAYING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Playing> entry = it.next();
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive() || weight(entry.getValue(), entity.tickCount) < 0) {
                it.remove();
            }
        }
    }

    public static void clear() {
        PLAYING.clear();
    }

    /** Called at the end of HumanoidModel#setupAnim. */
    public static void applyModel(HumanoidModel<?> model, LivingEntity entity, float age) {
        Playing playing = PLAYING.get(entity.getId());
        if (playing == null) {
            return;
        }
        float weight = weight(playing, age);
        if (weight <= 0) {
            return;
        }
        float time = localTime(playing, age);
        BodyAnimation animation = playing.animation;
        apply(model.head, animation, Bone.HEAD, time, weight);
        apply(model.body, animation, Bone.BODY, time, weight);
        apply(model.rightArm, animation, Bone.RIGHT_ARM, time, weight);
        apply(model.leftArm, animation, Bone.LEFT_ARM, time, weight);
        apply(model.rightLeg, animation, Bone.RIGHT_LEG, time, weight);
        apply(model.leftLeg, animation, Bone.LEFT_LEG, time, weight);
        model.hat.copyFrom(model.head);
    }

    /** Called at the end of LivingEntityRenderer#setupRotations: whole-body lean, spin and offset. */
    public static void applyRoot(LivingEntity entity, PoseStack pose, float partialTick) {
        Playing playing = PLAYING.get(entity.getId());
        if (playing == null) {
            return;
        }
        BodyAnimation.Track track = playing.animation.tracks().get(Bone.ROOT);
        if (track == null) {
            return;
        }
        float age = entity.tickCount + partialTick;
        float weight = weight(playing, age);
        if (weight <= 0) {
            return;
        }
        float time = localTime(playing, age);
        float[] position = track.positionAt(time);
        if (position != null) {
            pose.translate(position[0] / 16f * weight, position[1] / 16f * weight, position[2] / 16f * weight);
        }
        float[] rotation = track.rotationAt(time);
        if (rotation != null) {
            pose.translate(0, ROOT_PIVOT, 0);
            pose.mulPose(Axis.XP.rotation(rotation[0] * weight));
            pose.mulPose(Axis.YP.rotation(rotation[1] * weight));
            pose.mulPose(Axis.ZP.rotation(rotation[2] * weight));
            pose.translate(0, -ROOT_PIVOT, 0);
        }
    }

    private static void apply(ModelPart part, BodyAnimation animation, Bone bone, float time, float weight) {
        BodyAnimation.Track track = animation.tracks().get(bone);
        if (track == null) {
            return;
        }
        float[] rotation = track.rotationAt(time);
        if (rotation != null) {
            if (animation.additive()) {
                part.xRot += rotation[0] * weight;
                part.yRot += rotation[1] * weight;
                part.zRot += rotation[2] * weight;
            } else {
                part.xRot = Mth.lerp(weight, part.xRot, rotation[0]);
                part.yRot = Mth.lerp(weight, part.yRot, rotation[1]);
                part.zRot = Mth.lerp(weight, part.zRot, rotation[2]);
            }
        }
        float[] position = track.positionAt(time);
        if (position != null) {
            part.x += position[0] * weight;
            part.y += position[1] * weight;
            part.z += position[2] * weight;
        }
    }

    private static float localTime(Playing playing, float age) {
        float elapsed = Math.max(0, age - playing.start);
        return playing.animation.loop() ? elapsed % playing.animation.length() : Math.min(elapsed, playing.animation.length());
    }

    /** Blend weight 0..1, or a negative number once the animation is completely over. */
    private static float weight(Playing playing, float age) {
        BodyAnimation animation = playing.animation;
        float elapsed = age - playing.start;
        float weight = animation.fadeIn() <= 0 ? 1 : Mth.clamp(elapsed / animation.fadeIn(), 0, 1);
        if (!animation.loop()) {
            if (elapsed >= animation.length()) {
                return -1;
            }
            if (animation.fadeOut() > 0) {
                weight *= Mth.clamp((animation.length() - elapsed) / animation.fadeOut(), 0, 1);
            }
        }
        if (!Float.isNaN(playing.stoppedAt)) {
            float since = age - playing.stoppedAt;
            if (since >= animation.fadeOut()) {
                return -1;
            }
            weight *= animation.fadeOut() <= 0 ? 0 : 1 - since / animation.fadeOut();
        }
        return weight;
    }
}
