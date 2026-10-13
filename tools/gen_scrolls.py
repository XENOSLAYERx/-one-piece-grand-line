#!/usr/bin/env python3
"""Generates the 16x16 technique-scroll sprites and item models.

Each scroll is a rolled parchment drawn on a slant, with wooden end caps and a coloured ribbon and seal that tell
the techniques apart. Usage: python3 tools/gen_scrolls.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "grandline")
TEX_DIR = os.path.join(ROOT, "textures", "item", "scroll")
MODEL_DIR = os.path.join(ROOT, "models", "item")

# id -> (ribbon colour, seal colour)
SCROLLS = {
    "black_leg_scroll": (0x2A2A3A, 0xF08A2A),
    "fishman_karate_scroll": (0x3A8AC8, 0x6AC8F0),
    "soru_scroll": (0x9AB0E8, 0xFFFFFF),
    "geppo_scroll": (0x8AC8E8, 0xF0F8FF),
    "tekkai_scroll": (0x6A707A, 0xC8CCD4),
    "shigan_scroll": (0xB83A3A, 0xF0D0D0),
    "rankyaku_scroll": (0x5AA88A, 0xE0F8F0),
    "kami_e_scroll": (0xE8E0C8, 0x8A7A5A),
    "rokuogan_scroll": (0x3A2A6A, 0xF0C040),
}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def draw(ribbon, seal):
    """Draws the scroll by distance from its axis (the diagonal x + y = 15): along-axis u, across-axis q."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    paper_light = (244, 228, 190)
    paper = (226, 204, 156)
    paper_dark = (186, 158, 108)
    wood = (124, 80, 44)
    wood_dark = (86, 54, 30)
    outline = (52, 36, 22)
    rc, sc = rgb(ribbon), rgb(seal)
    root2 = 2 ** 0.5
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            u = (cx - cy) / root2
            q = (cx + cy - 16) / root2
            colour = None
            if abs(u) <= 6.4 and abs(q) <= 1.6:
                colour = paper_light if q < -0.6 else (paper if q < 0.6 else paper_dark)
                if abs(u) <= 0.75:
                    colour = rc if q < 0.6 else shade(rc, 0.75)
            elif 6.4 < abs(u) <= 7.6 and abs(q) <= 2.0:
                colour = wood if q < 0.4 else wood_dark
            elif abs(u) <= 7.6 and abs(q) <= 2.4:
                colour = outline
            if colour is not None:
                px[x, y] = colour + (255,)
    # Wax seal on the ribbon, and the ribbon's loose ends hanging below.
    px[7, 7] = sc + (255,)
    px[8, 7] = shade(sc, 0.8) + (255,)
    px[7, 8] = shade(sc, 0.8) + (255,)
    for (x, y), c in (((8, 10), rc), ((8, 11), shade(rc, 0.8)), ((9, 11), rc), ((7, 11), shade(rc, 0.8)), ((6, 12), rc)):
        if px[x, y][3] == 0 or True:
            px[x, y] = c + (255,)
    return img


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(MODEL_DIR, exist_ok=True)
    for sid, (ribbon, seal) in SCROLLS.items():
        draw(ribbon, seal).save(os.path.join(TEX_DIR, sid + ".png"))
        with open(os.path.join(MODEL_DIR, sid + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "grandline:item/scroll/" + sid}}, f, indent=2)
            f.write("\n")
    print("scrolls written:", len(SCROLLS))


if __name__ == "__main__":
    main()
