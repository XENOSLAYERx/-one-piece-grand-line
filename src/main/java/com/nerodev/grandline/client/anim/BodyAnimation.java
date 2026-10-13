package com.nerodev.grandline.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/**
 * A keyframed body animation loaded from {@code assets/<ns>/grandline_animations/<name>.json}.
 *
 * <pre>
 * {
 *   "length": 10,            // ticks
 *   "loop": false,
 *   "mode": "replace",       // "replace" blends toward the keyframes, "add" adds on top of the vanilla pose
 *   "fade_in": 2, "fade_out": 3,
 *   "bones": {
 *     "right_arm": {
 *       "ease": "out",                                 // linear | in | out | inout (default inout)
 *       "rotation": { "0": [0, 0, 0], "3": [-90, 0, 10] },   // degrees, x y z
 *       "position": { "0": [0, 0, 0], "3": [0, -1, 2] }      // model pixels (root: 1/16 block)
 *     },
 *     "root": { ... }                                   // the whole body, pivoting at the hips
 *   }
 * }
 * </pre>
 */
public record BodyAnimation(float length, boolean loop, boolean additive, float fadeIn, float fadeOut,
        Map<Bone, Track> tracks) {

    public enum Bone {
        HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, ROOT
    }

    public enum Ease {
        LINEAR, IN, OUT, INOUT;

        float apply(float t) {
            return switch (this) {
                case LINEAR -> t;
                case IN -> t * t;
                case OUT -> 1 - (1 - t) * (1 - t);
                case INOUT -> t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            };
        }
    }

    /** Keyframes for one bone. Either list may be empty. */
    public record Track(Ease ease, List<Key> rotation, List<Key> position) {
        /** @return rotation in radians at {@code time}, or null if this track has no rotation keys */
        float[] rotationAt(float time) {
            float[] degrees = sample(rotation, time);
            if (degrees == null) {
                return null;
            }
            return new float[] {degrees[0] * Mth.DEG_TO_RAD, degrees[1] * Mth.DEG_TO_RAD, degrees[2] * Mth.DEG_TO_RAD};
        }

        float[] positionAt(float time) {
            return sample(position, time);
        }

        private float[] sample(List<Key> keys, float time) {
            if (keys.isEmpty()) {
                return null;
            }
            if (time <= keys.getFirst().time()) {
                return keys.getFirst().value();
            }
            for (int i = 0; i < keys.size() - 1; i++) {
                Key a = keys.get(i);
                Key b = keys.get(i + 1);
                if (time <= b.time()) {
                    float t = ease.apply((time - a.time()) / Math.max(1.0E-4f, b.time() - a.time()));
                    return new float[] {
                            Mth.lerp(t, a.value()[0], b.value()[0]),
                            Mth.lerp(t, a.value()[1], b.value()[1]),
                            Mth.lerp(t, a.value()[2], b.value()[2])};
                }
            }
            return keys.getLast().value();
        }
    }

    public record Key(float time, float[] value) {}

    public static BodyAnimation fromJson(JsonObject json) {
        float length = GsonHelper.getAsFloat(json, "length");
        boolean loop = GsonHelper.getAsBoolean(json, "loop", false);
        boolean additive = "add".equals(GsonHelper.getAsString(json, "mode", "replace"));
        float fadeIn = GsonHelper.getAsFloat(json, "fade_in", 2f);
        float fadeOut = GsonHelper.getAsFloat(json, "fade_out", 3f);
        Map<Bone, Track> tracks = new EnumMap<>(Bone.class);
        JsonObject bones = GsonHelper.getAsJsonObject(json, "bones");
        for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
            Bone bone = Bone.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            JsonObject trackJson = entry.getValue().getAsJsonObject();
            Ease ease = Ease.valueOf(GsonHelper.getAsString(trackJson, "ease", "inout").toUpperCase(Locale.ROOT));
            tracks.put(bone, new Track(ease, keys(trackJson, "rotation"), keys(trackJson, "position")));
        }
        return new BodyAnimation(length, loop, additive, fadeIn, fadeOut, tracks);
    }

    private static List<Key> keys(JsonObject track, String name) {
        List<Key> keys = new ArrayList<>();
        if (!track.has(name)) {
            return keys;
        }
        for (Map.Entry<String, JsonElement> frame : GsonHelper.getAsJsonObject(track, name).entrySet()) {
            JsonArray v = frame.getValue().getAsJsonArray();
            keys.add(new Key(Float.parseFloat(frame.getKey()),
                    new float[] {v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat()}));
        }
        keys.sort(Comparator.comparingDouble(Key::time));
        return keys;
    }
}
