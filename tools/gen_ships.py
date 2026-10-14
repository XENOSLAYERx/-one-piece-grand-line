#!/usr/bin/env python3
"""Generates the ship textures: the material atlas the ship meshes are built from, the sails and flags of each
faction, and the Ship in a Bottle and cannonball item sprites.

The atlas is a 4x4 grid of 16x16 tiles; tinted tiles (hull, trim, logs) are drawn in greys so a ship's JSON colours
show through. Usage: python3 tools/gen_ships.py
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "grandline")
SHIP_TEX = os.path.join(ROOT, "textures", "entity", "ship")
ITEM_TEX = os.path.join(ROOT, "textures", "item")
ITEM_MODELS = os.path.join(ROOT, "models", "item")


def clamp(v):
    return max(0, min(255, int(v)))


def grey(v):
    return (clamp(v), clamp(v), clamp(v), 255)


def rgb(c, f=1.0, a=255):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), a)


# ---------------------------------------------------------------------------------------------------------------
# Atlas tiles
# ---------------------------------------------------------------------------------------------------------------

def tile_hull(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        plank = y // 4
        offset = (plank * 7) % 16
        for x in range(16):
            v = 205 + rnd.uniform(-10, 10) + 8 * math.sin((x + plank * 5) * 0.9)
            if y % 4 == 3:
                v = 140
            if (x + offset) % 16 == 0:
                v = 150
            if (x + offset) % 16 in (2, 14) and y % 4 == 1:
                v = 120
            px[x, y] = grey(v)
    return img


def tile_deck(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (196, 160, 112)
    for x in range(16):
        plank = x // 4
        offset = (plank * 5) % 16
        for y in range(16):
            f = 1 + rnd.uniform(-0.05, 0.05) + 0.04 * math.sin((y + plank * 3) * 0.7)
            if x % 4 == 3:
                f = 0.68
            if (y + offset) % 16 == 0:
                f = 0.75
            px[x, y] = rgb(base, f)
    return img


def tile_trim(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 190 + rnd.uniform(-6, 6) + 6 * math.sin(y * 0.8 + x * 0.1)
            if y in (0, 15):
                v -= 40
            if y in (1,):
                v += 20
            px[x, y] = grey(v)
    return img


def tile_mast(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (126, 88, 54)
    for x in range(16):
        for y in range(16):
            f = 1 + 0.12 * math.sin(x * 1.3 + math.sin(y * 0.4) * 2) + rnd.uniform(-0.05, 0.05)
            px[x, y] = rgb(base, f)
    return img


def tile_metal(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (64, 66, 76)
    for y in range(16):
        for x in range(16):
            f = 1 + rnd.uniform(-0.08, 0.08) + (0.25 if x + y < 6 else 0)
            px[x, y] = rgb(base, f)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = rgb((150, 152, 160))
    return img


def tile_gold(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (232, 188, 72)
    for y in range(16):
        for x in range(16):
            f = 1 + rnd.uniform(-0.06, 0.06) + 0.2 * math.cos((x - y) * 0.4)
            px[x, y] = rgb(base, f)
    return img


def tile_window(rnd):
    img = Image.new("RGBA", (16, 16), rgb((90, 60, 36)))
    d = ImageDraw.Draw(img)
    for x0, y0 in ((2, 2), (9, 2), (2, 9), (9, 9)):
        d.rectangle([x0, y0, x0 + 4, y0 + 4], fill=rgb((52, 74, 104)))
        d.point((x0 + 1, y0 + 1), fill=rgb((170, 200, 230)))
        d.point((x0 + 2, y0 + 1), fill=rgb((120, 150, 190)))
    return img


def tile_log(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 170 + rnd.uniform(-22, 22) + 14 * math.sin(x * 0.9 + y * 0.3)
            px[x, y] = grey(v)
    return img


def tile_lion(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rgb((240, 200, 90), 1 + rnd.uniform(-0.05, 0.05))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 5, 5, 7], fill=rgb((30, 24, 20)))
    d.rectangle([10, 5, 12, 7], fill=rgb((30, 24, 20)))
    d.point((4, 5), fill=rgb((255, 255, 255)))
    d.point((11, 5), fill=rgb((255, 255, 255)))
    d.rectangle([7, 8, 8, 9], fill=rgb((120, 60, 40)))
    d.line([(5, 11), (7, 12), (8, 12), (10, 11)], fill=rgb((60, 30, 20)))
    return img


def tile_mane(rnd):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            ang = math.atan2(y - 7.5, x - 7.5)
            petal = 0.5 + 0.5 * math.cos(ang * 8)
            px[x, y] = rgb((236, 136, 44), 0.8 + 0.3 * petal + rnd.uniform(-0.04, 0.04))
    return img


def atlas():
    rnd = random.Random(42)
    tiles = [tile_hull, tile_deck, tile_trim, tile_mast, tile_metal, tile_gold, tile_window, tile_log, tile_lion, tile_mane]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for i, fn in enumerate(tiles):
        img.paste(fn(rnd), ((i % 4) * 16, (i // 4) * 16))
    return img


# ---------------------------------------------------------------------------------------------------------------
# Sails and flags
# ---------------------------------------------------------------------------------------------------------------

def canvas(size, base, seed):
    rnd = random.Random(seed)
    w, h = size
    img = Image.new("RGBA", size)
    px = img.load()
    for y in range(h):
        for x in range(w):
            f = 1 + rnd.uniform(-0.035, 0.035) + (0.03 if (x + y) % 2 == 0 else 0)
            if y % 16 == 15:
                f -= 0.12
            if x in (0, w - 1) or y in (0, h - 1):
                f -= 0.18
            px[x, y] = rgb(base, f)
    return img


def skull(d, cx, cy, s, bone=(244, 244, 236), dark=(24, 24, 28), crossbones=True):
    """A skull (and crossbones) centred at cx, cy; s is the skull's half width."""
    if crossbones:
        for sx in (-1, 1):
            d.line([(cx - sx * s * 1.9, cy - s * 0.6), (cx + sx * s * 1.9, cy + s * 2.0)], fill=bone, width=max(2, int(s * 0.45)))
            for ex, ey in ((cx - sx * s * 1.9, cy - s * 0.6), (cx + sx * s * 1.9, cy + s * 2.0)):
                r = s * 0.32
                d.ellipse([ex - r, ey - r, ex + r, ey + r], fill=bone)
    d.ellipse([cx - s, cy - s * 1.1, cx + s, cy + s * 0.8], fill=bone)
    d.rectangle([cx - s * 0.6, cy + s * 0.4, cx + s * 0.6, cy + s * 1.1], fill=bone)
    r = s * 0.32
    for ex in (cx - s * 0.42, cx + s * 0.42):
        d.ellipse([ex - r, cy - r * 0.6, ex + r, cy + r * 1.2], fill=dark)
    d.polygon([(cx, cy + s * 0.35), (cx - s * 0.14, cy + s * 0.6), (cx + s * 0.14, cy + s * 0.6)], fill=dark)
    for tx in range(-2, 3):
        d.line([(cx + tx * s * 0.2, cy + s * 0.8), (cx + tx * s * 0.2, cy + s * 1.1)], fill=dark, width=1)


def straw_hat(d, cx, cy, s):
    d.ellipse([cx - s * 1.6, cy - s * 0.45, cx + s * 1.6, cy + s * 0.3], fill=(232, 196, 92))
    d.rectangle([cx - s * 0.9, cy - s * 1.0, cx + s * 0.9, cy - s * 0.1], fill=(232, 196, 92))
    d.ellipse([cx - s * 0.9, cy - s * 1.3, cx + s * 0.9, cy - s * 0.7], fill=(232, 196, 92))
    d.rectangle([cx - s * 0.92, cy - s * 0.45, cx + s * 0.92, cy - s * 0.18], fill=(200, 40, 40))


def seagull(d, cx, cy, s, color):
    d.arc([cx - s * 2, cy - s, cx, cy + s], 200, 340, fill=color, width=max(2, int(s * 0.4)))
    d.arc([cx, cy - s, cx + s * 2, cy + s], 200, 340, fill=color, width=max(2, int(s * 0.4)))
    d.ellipse([cx - s * 0.3, cy - s * 0.2, cx + s * 0.3, cy + s * 0.4], fill=color)


FONT = {
    "M": ["10001", "11011", "10101", "10001", "10001"],
    "A": ["01110", "10001", "11111", "10001", "10001"],
    "R": ["11110", "10001", "11110", "10010", "10001"],
    "I": ["111", "010", "010", "010", "111"],
    "N": ["10001", "11001", "10101", "10011", "10001"],
    "E": ["1111", "1000", "1110", "1000", "1111"],
}


def text(d, x, y, word, color, scale=1):
    for ch in word:
        rows = FONT[ch]
        for j, row in enumerate(rows):
            for i, bit in enumerate(row):
                if bit == "1":
                    d.rectangle([x + i * scale, y + j * scale, x + (i + 1) * scale - 1, y + (j + 1) * scale - 1], fill=color)
        x += (len(rows[0]) + 1) * scale


def revolutionary_emblem(d, cx, cy, s):
    d.ellipse([cx - s * 1.6, cy - s * 1.6, cx + s * 1.6, cy + s * 1.6], outline=(20, 16, 16), width=max(2, int(s * 0.3)))
    d.polygon([(cx, cy - s * 1.2), (cx + s * 0.5, cy), (cx, cy + s * 1.2), (cx - s * 0.5, cy)], fill=(20, 16, 16))
    d.line([(cx - s * 1.2, cy - s * 0.4), (cx + s * 1.2, cy + s * 0.4)], fill=(20, 16, 16), width=max(2, int(s * 0.25)))


def sails():
    out = {}
    out["sail_canvas"] = canvas((64, 64), (236, 232, 222), 1)
    img = canvas((64, 64), (44, 44, 50), 2)
    skull(ImageDraw.Draw(img), 32, 28, 9)
    out["sail_pirate"] = img
    img = canvas((64, 64), (246, 246, 250), 3)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 3, 63, 7], fill=(40, 90, 168))
    d.rectangle([0, 56, 63, 60], fill=(40, 90, 168))
    seagull(d, 32, 26, 6, (40, 90, 168))
    text(d, 15, 40, "MARINE", (40, 90, 168), 1)
    out["sail_marine"] = img
    img = canvas((64, 64), (120, 28, 28), 4)
    revolutionary_emblem(ImageDraw.Draw(img), 32, 32, 9)
    out["sail_revolutionary"] = img
    img = canvas((64, 64), (246, 244, 236), 5)
    d = ImageDraw.Draw(img)
    skull(d, 32, 32, 8, bone=(28, 28, 32), dark=(246, 244, 236))
    straw_hat(d, 32, 23, 8)
    out["sail_straw_hat"] = img
    return out


def flags():
    out = {}
    img = canvas((32, 16), (28, 28, 32), 6)
    skull(ImageDraw.Draw(img), 16, 6, 3.5)
    out["flag_pirate"] = img
    img = canvas((32, 16), (28, 28, 32), 7)
    d = ImageDraw.Draw(img)
    skull(d, 16, 7, 3.5)
    straw_hat(d, 16, 3.5, 3.5)
    out["flag_straw_hat"] = img
    img = canvas((32, 16), (246, 246, 250), 8)
    seagull(ImageDraw.Draw(img), 16, 8, 3, (40, 90, 168))
    out["flag_marine"] = img
    img = canvas((32, 16), (150, 30, 30), 9)
    revolutionary_emblem(ImageDraw.Draw(img), 16, 8, 3.5)
    out["flag_revolutionary"] = img
    return out


# ---------------------------------------------------------------------------------------------------------------
# Items
# ---------------------------------------------------------------------------------------------------------------

def bottle_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    glass = (190, 226, 240, 150)
    rim = (120, 170, 200, 255)
    # A bottle lying on its side, neck to the right.
    d.rounded_rectangle([1, 5, 11, 13], radius=3, fill=glass, outline=rim)
    d.rectangle([11, 7, 14, 10], fill=glass, outline=rim)
    d.rectangle([14, 7, 15, 10], fill=(160, 110, 60, 255))
    # The ship inside: hull, mast and sail.
    d.polygon([(3, 10), (9, 10), (8, 12), (4, 12)], fill=(122, 82, 46, 255))
    d.line([(6, 6), (6, 10)], fill=(90, 60, 36, 255))
    d.polygon([(6, 6), (9, 9), (6, 9)], fill=(244, 240, 228, 255))
    d.point((4, 6), fill=(255, 255, 255, 220))
    return img


def cannonball_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dist = math.hypot(x + 0.5 - 8, y + 0.5 - 8.5)
            if dist <= 5.6:
                t = (x - 8) + (y - 8)
                v = 70 if t < -3 else (48 if t < 3 else 30)
                px[x, y] = (v, v, v + 6, 255)
            elif dist <= 6.4:
                px[x, y] = (16, 16, 20, 255)
    px[6, 6] = (170, 172, 180, 255)
    px[7, 6] = (120, 122, 130, 255)
    return img


def main():
    os.makedirs(SHIP_TEX, exist_ok=True)
    atlas().save(os.path.join(SHIP_TEX, "atlas.png"))
    for name, img in {**sails(), **flags()}.items():
        img.save(os.path.join(SHIP_TEX, name + ".png"))
    bottle_icon().save(os.path.join(ITEM_TEX, "ship_in_a_bottle.png"))
    cannonball_icon().save(os.path.join(ITEM_TEX, "cannonball.png"))
    for name in ("ship_in_a_bottle", "cannonball"):
        with open(os.path.join(ITEM_MODELS, name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "grandline:item/" + name}}, f, indent=2)
            f.write("\n")
    print("ship textures written")


if __name__ == "__main__":
    main()
