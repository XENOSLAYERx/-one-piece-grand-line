#!/usr/bin/env python3
"""Generates the Log Pose item: a glass globe on a wrist strap with a floating needle, in 16 needle positions.

Run from the repository root: python3 tools/gen_log_pose.py

Writes
  src/main/resources/assets/grandline/textures/item/log_pose/log_pose_<00..15>.png  needle turned k * 22.5 degrees clockwise
  src/main/resources/assets/grandline/models/item/log_pose.json                    picks a frame by grandline:heading
  src/main/resources/assets/grandline/models/item/log_pose_<00..15>.json
"""
import json
import math
import os

from PIL import Image

ROOT = "src/main/resources/assets/grandline"
TEX = ROOT + "/textures/item/log_pose"
MODELS = ROOT + "/models/item"
FRAMES = 16

STRAP = (122, 78, 44, 255)
STRAP_DARK = (84, 52, 28, 255)
STITCH = (196, 160, 112, 255)
BRASS = (204, 160, 64, 255)
BRASS_DARK = (138, 100, 34, 255)
GLASS = (168, 214, 236, 150)
GLASS_EDGE = (120, 178, 210, 200)
SHINE = (240, 250, 255, 220)
NEEDLE_RED = (214, 40, 40, 255)
NEEDLE_WHITE = (236, 236, 236, 255)
PIVOT = (60, 50, 40, 255)


def frame(k):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # The wrist strap across the bottom, with stitching.
    for x in range(1, 15):
        for y in range(12, 15):
            px[x, y] = STRAP_DARK if y == 14 or x in (1, 14) else STRAP
        if x % 2 == 0:
            px[x, 13] = STITCH
    # The brass base the globe sits in.
    for x in range(4, 12):
        px[x, 11] = BRASS_DARK if x in (4, 11) else BRASS
        px[x, 12] = BRASS_DARK
    # The glass globe.
    cx, cy, r = 7.5, 6.0, 5.2
    for y in range(0, 12):
        for x in range(1, 15):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                px[x, y] = GLASS_EDGE if d > r - 1.0 else GLASS
    px[5, 3] = SHINE
    px[5, 4] = SHINE
    px[6, 2] = SHINE
    # The needle: red end towards the island, white tail, turned k * 22.5 degrees clockwise from straight up.
    a = math.radians(k * 360.0 / FRAMES)
    dx, dy = math.sin(a), -math.cos(a)
    for step in range(-6, 7):
        t = step / 2.0
        x = int(round(cx + dx * t))
        y = int(round(cy + dy * t))
        if 0 <= x < 16 and 0 <= y < 12 and math.hypot(x - cx, y - cy) <= r - 0.6:
            px[x, y] = NEEDLE_RED if step > 0 else NEEDLE_WHITE
    px[int(cx), int(cy)] = PIVOT
    return img


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    for k in range(FRAMES):
        frame(k).save(f"{TEX}/log_pose_{k:02d}.png")
        with open(f"{MODELS}/log_pose_{k:02d}.json", "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"grandline:item/log_pose/log_pose_{k:02d}"}}, f, indent=2)
            f.write("\n")
    overrides = []
    for k in range(FRAMES):
        threshold = 0.0 if k == 0 else (k - 0.5) / FRAMES
        overrides.append({"predicate": {"grandline:heading": round(threshold, 5)}, "model": f"grandline:item/log_pose_{k:02d}"})
    overrides.append({"predicate": {"grandline:heading": round((FRAMES - 0.5) / FRAMES, 5)}, "model": "grandline:item/log_pose_00"})
    model = {"parent": "minecraft:item/generated", "textures": {"layer0": "grandline:item/log_pose/log_pose_00"}, "overrides": overrides}
    with open(f"{MODELS}/log_pose.json", "w") as f:
        json.dump(model, f, indent=2)
        f.write("\n")
    print(f"{FRAMES} log pose frames")


if __name__ == "__main__":
    main()
