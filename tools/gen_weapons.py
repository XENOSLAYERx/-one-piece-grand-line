#!/usr/bin/env python3
"""Generates the weapon, gun and Seastone sprites, block textures, models and blockstates.

Long items (swords, polearms, guns) are drawn along the sprite's diagonal like vanilla tools: every pixel is placed
by its distance along the weapon's axis (u, from the pommel at the bottom left) and across it (q, positive toward the
bottom right). Blocks are drawn from seeded noise so the output never changes between runs.

Usage: python3 tools/gen_weapons.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "grandline")
ITEM_TEX = os.path.join(ROOT, "textures", "item", "weapon")
BLOCK_TEX = os.path.join(ROOT, "textures", "block")
ITEM_MODELS = os.path.join(ROOT, "models", "item")
BLOCK_MODELS = os.path.join(ROOT, "models", "block")
BLOCKSTATES = os.path.join(ROOT, "blockstates")

R2 = math.sqrt(2)
# Axis from the bottom-left pixel centre to the top-right one.
ORIGIN = (0.5, 15.5)
LENGTH = 15 * R2


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def axis(x, y):
    cx, cy = x + 0.5, y + 0.5
    dx, dy = cx - ORIGIN[0], cy - ORIGIN[1]
    return (dx - dy) / R2, (dx + dy) / R2


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def paint(img, fn):
    """Calls fn(u, q, x, y) for every pixel; a returned colour is drawn."""
    px = img.load()
    for y in range(16):
        for x in range(16):
            u, q = axis(x, y)
            c = fn(u, q, x, y)
            if c is not None:
                px[x, y] = tuple(c) + (255,)
    return img


def outline(img, colour):
    """Adds a one-pixel outline around the drawn shape (4-neighbours)."""
    px = img.load()
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3] > 0}
    for y in range(16):
        for x in range(16):
            if (x, y) in solid:
                continue
            if any((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = colour + (255,)
    return img


def side(q):
    """Which side of the axis a pixel is on: -1 upper left, 0 on the axis, 1 lower right."""
    if q < -0.35:
        return -1
    if q > 0.35:
        return 1
    return 0


# ---------------------------------------------------------------------------------------------------------------
# Blades
# ---------------------------------------------------------------------------------------------------------------

STEEL = ((236, 240, 246), (184, 192, 202), (118, 126, 138))
BLACK_STEEL = ((96, 98, 110), (46, 48, 56), (24, 24, 30))
SEASTONE = ((150, 186, 182), (104, 140, 136), (62, 90, 88))


def sword(blade=STEEL, edge=None, handle=(40, 36, 44), wrap=(230, 226, 214), guard=(208, 168, 64), guard_kind="tsuba",
          pommel=None, tip=None, wide=False, line=(30, 30, 38), hamon=None):
    """A sword with its pommel at the bottom left and its tip toward the top right."""
    tip = LENGTH - 1.2 if tip is None else tip
    pommel = pommel or shade(guard, 0.8)
    light, mid, dark = blade
    edge = edge or light

    def fn(u, q, x, y):
        s = side(q)
        aq = abs(q)
        if u < 0.9 and aq < 0.4:
            return pommel
        if u < 4.6:
            if aq < 0.4:
                return wrap if int(u * 1.4) % 2 == 0 else handle
            return None
        if u < 5.8:
            if guard_kind == "tsuba" and aq <= 1.5:
                return guard if q <= 0.4 else shade(guard, 0.7)
            if guard_kind == "cross" and aq <= 2.2:
                return guard if q <= 0.4 else shade(guard, 0.7)
            if guard_kind == "basket" and -2.3 <= q <= 1.5:
                return guard if q <= 0 else shade(guard, 0.7)
            return None
        if u > tip:
            return None
        taper = (tip - u) < 1.6
        width = 0.75 if not wide else (1.45 if u > 8 else 0.75)
        if taper:
            width = min(width, 0.4)
        if aq > width:
            return None
        if wide and aq > 0.75:
            return light if q < 0 else dark
        if s < 0:
            if hamon is not None and int(u * 1.5) % 3 == 0:
                return hamon
            return edge
        if s == 0:
            return mid
        return dark

    return outline(paint(canvas(), fn), line)


def polearm(head, shaft=((150, 106, 62), (106, 72, 40)), line=(34, 26, 20), ring=None, head_from=9.5):
    """A long shaft with a head drawn by {@code head(u, q)} past {@code head_from}."""

    def fn(u, q, x, y):
        aq = abs(q)
        if u >= head_from:
            c = head(u, q)
            if c is not None:
                return c
        if u < LENGTH - 2.5 and -0.4 < q < 0.9 and u < head_from + 0.6:
            if ring is not None and head_from - 1.2 < u < head_from:
                return ring
            return shaft[0] if q < 0.35 else shaft[1]
        return None

    return outline(paint(canvas(), fn), line)


def naginata_head(u, q):
    t = u - 9.5
    if t < 0 or u > LENGTH - 0.8:
        return None
    # A curved single-edged blade bowing to the upper left.
    curve = -0.35 * t / 6
    qq = q - curve
    width = 0.8 if t < 4.5 else 0.45
    if -width - 0.35 <= qq <= 0.45:
        if qq < -0.35:
            return STEEL[0]
        return STEEL[1] if qq < 0.35 else STEEL[2]
    return None


def bisento_head(u, q):
    t = u - 11.0
    if t < -1.4:
        return None
    if t < 0:
        # Gold collar.
        return (226, 188, 72) if abs(q) <= 1.2 else None
    if u > LENGTH - 0.5:
        return None
    # A broad cleaver blade: wide on the upper-left side, curving to a point.
    reach = 2.6 - 0.25 * t * t
    if -max(0.4, reach) <= q <= 0.7:
        if q < -1.6:
            return STEEL[0]
        if q < -0.35:
            return mix(STEEL[1], STEEL[0], 0.4)
        return STEEL[1] if q < 0.35 else STEEL[2]
    return None


def axe_head(u, q):
    """Morgan's axe: a great half-moon blade on the upper-left of the haft, with a spike on the other side."""
    t = u - 9.0
    if t < 0 or u > LENGTH - 0.3:
        return None
    if q > 0.4:
        # Back spike.
        return STEEL[2] if q < 1.4 - t * 0.12 and 1.0 < t < 3.4 else None
    reach = 3.3 - 0.32 * (t - 2.6) ** 2
    if q < -max(0.4, reach):
        return None
    if q < -reach + 0.8:
        return (232, 236, 244)
    if q < -0.35:
        return STEEL[0] if t > 1.2 else STEEL[1]
    return STEEL[1] if q < 0.35 else STEEL[2]


def kiribachi():
    """Arlong's saw-sword: a long, broad blade with a row of teeth on its edge."""
    blade = ((196, 202, 210), (150, 156, 166), (104, 110, 120))

    def fn(u, q, x, y):
        aq = abs(q)
        if u < 3.2:
            if aq < 0.45:
                return (62, 44, 30) if int(u * 1.6) % 2 == 0 else (42, 28, 18)
            return None
        if u < 3.9:
            return (172, 140, 60) if aq < 1.6 else None
        if u > LENGTH - 0.6:
            return None
        width = 1.25
        if q < -width:
            # Teeth along the edge.
            return (226, 230, 238) if q > -width - 0.9 and int(u * 1.3) % 2 == 0 else None
        if q > width:
            return None
        if q < -0.5:
            return blade[0]
        return blade[1] if q < 0.4 else blade[2]

    return outline(paint(canvas(), fn), (28, 28, 34))


def cat_claws():
    """Captain Kuro's Neko no Te: a black glove with five long, thin blades on its fingers."""
    glove = ((44, 40, 48), (24, 22, 28))
    steel = ((222, 226, 234), (156, 160, 170))

    def fn(u, q, x, y):
        aq = abs(q)
        if u < 5.5:
            if aq < 2.1 - max(0.0, 1.6 - u) * 0.6:
                return glove[0] if q < 0.5 else glove[1]
            return None
        if u > LENGTH - 0.8:
            return None
        for k in (-2, -1, 0, 1, 2):
            centre = k * (0.9 + (u - 5.5) * 0.06)
            if abs(q - centre) < 0.34:
                return steel[0] if k <= 0 else steel[1]
        return None

    return outline(paint(canvas(), fn), (18, 18, 22))


def chop_fist():
    """Buggy's fist flying free (a Chop-Chop cannon): a white-gloved fist with a cuff and a speed streak."""
    img = canvas()
    px = img.load()
    glove, shadow, cuff = (238, 236, 230, 255), (196, 192, 184, 255), (176, 40, 40, 255)
    for y in range(4, 12):
        for x in range(5, 13):
            if (x - 8.5) ** 2 / 14 + (y - 7.5) ** 2 / 12 <= 1:
                px[x, y] = shadow if x > 10 or y > 10 else glove
    for y in range(6, 10):
        px[4, y] = cuff
        px[3, y] = cuff
    for x in (6, 8, 10):
        px[x, 5] = shadow
    for x, y in ((1, 7), (0, 8), (1, 9), (2, 6), (2, 10)):
        px[x, y] = (220, 230, 240, 160)
    return outline(img, (40, 34, 30))


def kanabo():
    iron = ((122, 124, 134), (84, 86, 96), (54, 56, 64))
    stud = (196, 198, 206)

    def fn(u, q, x, y):
        aq = abs(q)
        if u < 0.9 and aq < 0.4:
            return (90, 60, 36)
        if u < 5.0:
            if aq < 0.4:
                return (176, 40, 40) if int(u * 1.4) % 2 == 0 else (104, 24, 24)
            return None
        if u > LENGTH - 1.0:
            return None
        width = 0.8 + min(1.0, (u - 5.0) / 5.0) * 0.75
        if aq > width:
            return None
        if aq > 0.9 and (x + y) % 3 == 0:
            return stud
        if q < -0.35:
            return iron[0]
        return iron[1] if q < 0.35 else iron[2]

    return outline(paint(canvas(), fn), (24, 24, 30))


def jitte():
    light, mid, dark = SEASTONE

    def fn(u, q, x, y):
        aq = abs(q)
        if u < 0.9 and aq < 0.4:
            return (196, 160, 72)
        if u < 5.0:
            if aq < 0.4:
                return (60, 44, 34) if int(u * 1.4) % 2 == 0 else (126, 34, 34)
            return None
        # The hook: a short prong standing off the upper-left side, pointing toward the tip.
        if 5.0 <= u < 6.4 and -2.2 <= q < 0:
            return mid if q > -1.6 else dark
        if 6.4 <= u < 8.4 and -2.2 <= q <= -1.3:
            return light
        if u > LENGTH - 2.4:
            return None
        if aq > 0.75:
            return None
        if q < -0.35:
            return light
        return mid if q < 0.35 else dark

    return outline(paint(canvas(), fn), (22, 34, 34))


# ---------------------------------------------------------------------------------------------------------------
# Guns
# ---------------------------------------------------------------------------------------------------------------

WOOD = ((168, 112, 62), (128, 82, 44), (92, 58, 30))
BRASS = (214, 176, 80)
GUNMETAL = ((150, 156, 166), (98, 104, 116), (60, 64, 74))


def flintlock():
    def fn(u, q, x, y):
        # A pistol is short: shift its axis toward the top left so the grip has room to hang below the barrel.
        u, q = u - 0.4, q + 2.6
        # Barrel, with a dark muzzle at the front.
        if 6.0 <= u < LENGTH - 1.6 and -0.75 < q < 0.4:
            if u > LENGTH - 2.4:
                return (36, 38, 44)
            return GUNMETAL[0] if q < -0.35 else GUNMETAL[1]
        # Wooden forestock under the barrel.
        if 6.0 <= u < 11.0 and 0.35 < q < 1.1:
            return WOOD[1]
        # Lock plate and hammer.
        if 4.6 <= u < 6.6 and -0.75 < q < 1.1:
            return BRASS if q < 0.35 else shade(BRASS, 0.75)
        if 5.2 <= u < 6.2 and -1.6 < q <= -0.75:
            return (64, 64, 72)
        # Trigger guard.
        if 6.6 <= u < 7.6 and 1.1 <= q < 2.2:
            return shade(BRASS, 0.8)
        # Grip, sloping back and down from the lock, with a brass butt cap.
        if 0.3 < q < 4.4:
            back = 5.4 - 0.75 * q
            if back - 1.6 < u < back + 0.6:
                if q > 3.6:
                    return BRASS
                return WOOD[0] if u > back - 0.6 else WOOD[2]
        return None

    return outline(paint(canvas(), fn), (30, 24, 20))


def musket():
    def fn(u, q, x, y):
        if u < 4.0:
            # Butt stock, deep on the lower-right side.
            if -0.4 < q < 1.9 - u * 0.12:
                if u < 0.8:
                    return WOOD[2]
                return WOOD[0] if q < 0.35 else WOOD[1]
            return None
        if u < 5.6:
            if -1.4 < q < -0.6 and u > 4.6:
                return (60, 60, 66)
            if -0.75 < q < 1.1:
                return BRASS if q < 0.35 else shade(BRASS, 0.75)
            return None
        if u < LENGTH - 4.0 and 0.35 < q < 1.1:
            return WOOD[1]
        if u < LENGTH - 0.8 and -0.75 < q < 0.4:
            if abs(u - 9.5) < 0.5 or abs(u - 14.5) < 0.5:
                return BRASS
            return GUNMETAL[0] if q < -0.35 else GUNMETAL[1]
        return None

    return outline(paint(canvas(), fn), (30, 24, 20))


# ---------------------------------------------------------------------------------------------------------------
# Small items
# ---------------------------------------------------------------------------------------------------------------

def balls(colours, centres):
    img = canvas()
    px = img.load()
    light, mid, dark = colours
    for cx, cy in centres:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= 2.1:
                    t = (x + 0.5 - cx) + (y + 0.5 - cy)
                    px[x, y] = (light if t < -1 else (mid if t < 1.2 else dark)) + (255,)
        px[int(cx - 1), int(cy - 1)] = (250, 250, 250, 255)
    return outline(img, (26, 28, 32))


def cuffs():
    img = canvas()
    px = img.load()
    light, mid, dark = SEASTONE
    rings = ((5.0, 10.5), (11.0, 5.0))
    for cx, cy in rings:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if 1.6 <= d <= 3.3:
                    t = (x + 0.5 - cx) + (y + 0.5 - cy)
                    px[x, y] = (light if t < -1.5 else (mid if t < 1.5 else dark)) + (255,)
    # Chain between the rings.
    for i, (x, y) in enumerate(((7, 8), (8, 7), (9, 7), (8, 8))):
        px[x, y] = ((168, 172, 180) if i % 2 == 0 else (110, 114, 124)) + (255,)
    return outline(img, (22, 34, 34))


def net():
    img = canvas()
    px = img.load()
    rope = (214, 196, 150)
    thread = SEASTONE[0]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8.5)
            if d > 6.8:
                continue
            on_a = (x + y) % 4 == 0
            on_b = (x - y) % 4 == 0
            if d > 5.8:
                px[x, y] = rope + (255,)
            elif on_a or on_b:
                px[x, y] = (thread if (x + y) % 8 < 4 else SEASTONE[1]) + (255,)
            if on_a and on_b and d <= 5.8:
                px[x, y] = SEASTONE[2] + (255,)
    # Weights around the rim.
    for x, y in ((2, 8), (13, 8), (8, 2), (8, 14)):
        px[x, y] = (84, 86, 96, 255)
    return outline(img, (40, 34, 24))


def raw_seastone():
    img = canvas()
    px = img.load()
    rnd = random.Random(7)
    light, mid, dark = SEASTONE
    blobs = ((7.0, 8.5, 4.6), (10.0, 6.0, 3.2), (5.5, 11.0, 3.0))
    for y in range(16):
        for x in range(16):
            inside = [b for b in blobs if math.hypot(x + 0.5 - b[0], y + 0.5 - b[1]) <= b[2]]
            if not inside:
                continue
            t = (x - 7) + (y - 8) + rnd.uniform(-2, 2)
            c = light if t < -3 else (mid if t < 3 else dark)
            if rnd.random() < 0.08:
                c = (210, 236, 232)
            px[x, y] = c + (255,)
    return outline(img, (24, 36, 36))


def ingot():
    img = canvas()
    px = img.load()
    light, mid, dark = SEASTONE
    # A vanilla-style ingot: a slanted bar with a lit top face.
    for y in range(5, 12):
        for x in range(2, 15):
            left = 2 + (11 - y) // 2
            right = 13 + (11 - y) // 2 - 1
            if left <= x <= right:
                if y <= 6:
                    c = (196, 226, 222)
                elif y <= 8:
                    c = light
                elif y <= 10:
                    c = mid
                else:
                    c = dark
                if x == right:
                    c = dark
                px[x, y] = c + (255,)
    return outline(img, (24, 36, 36))


# ---------------------------------------------------------------------------------------------------------------
# Blocks
# ---------------------------------------------------------------------------------------------------------------

def noise_block(seed, base, spread, bands=False):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = rnd.uniform(-spread, spread)
            if bands and y % 4 == 0:
                v -= spread * 0.8
            px[x, y] = shade(base, 1 + v) + (255,)
    return img


def ore(seed, background, bands=False):
    img = noise_block(seed, background, 0.12, bands)
    px = img.load()
    rnd = random.Random(seed + 1)
    light, mid, dark = SEASTONE
    for cx, cy in ((4, 4), (11, 6), (6, 11), (12, 12)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
            if rnd.random() < 0.85:
                x, y = (cx + dx) % 16, (cy + dy) % 16
                px[x, y] = (light if dx + dy < 0 else (mid if dx + dy == 0 else dark)) + (255,)
        px[cx % 16, cy % 16] = (206, 236, 230, 255)
    return img


def seastone_block():
    img = noise_block(41, SEASTONE[1], 0.05)
    px = img.load()
    for i in range(16):
        px[i, 0] = SEASTONE[0] + (255,)
        px[0, i] = SEASTONE[0] + (255,)
        px[i, 15] = SEASTONE[2] + (255,)
        px[15, i] = SEASTONE[2] + (255,)
    # Faint waves, the sea held in the stone.
    for row in (5, 10):
        for x in range(1, 15):
            y = row + (1 if x % 4 in (1, 2) else 0)
            px[x, y] = shade(SEASTONE[1], 1.12) + (255,)
    return img


def bars():
    img = canvas()
    px = img.load()
    light, mid, dark = SEASTONE
    for x in (3, 7, 11):
        for y in range(16):
            px[x, y] = light + (255,)
            px[x + 1, y] = dark + (255,)
    for y in (1, 14):
        for x in range(16):
            px[x, y] = mid + (255,)
            px[x, y + 1 if y == 1 else y - 1] = dark + (255,)
    return img


# ---------------------------------------------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------------------------------------------

LONG_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 6.5, 0.5], "scale": [1.25, 1.25, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 6.5, 0.5], "scale": [1.25, 1.25, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 4.2, 1.13], "scale": [0.8, 0.8, 0.8]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 4.2, 1.13], "scale": [0.8, 0.8, 0.8]},
}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def item_model(name, handheld=True, long=False):
    model = {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
             "textures": {"layer0": "grandline:item/weapon/" + name}}
    if long:
        model["display"] = LONG_DISPLAY
    write_json(os.path.join(ITEM_MODELS, name + ".json"), model)


def cube_block(name):
    write_json(os.path.join(BLOCK_MODELS, name + ".json"),
               {"parent": "minecraft:block/cube_all", "textures": {"all": "grandline:block/" + name}})
    write_json(os.path.join(ITEM_MODELS, name + ".json"), {"parent": "grandline:block/" + name})
    write_json(os.path.join(BLOCKSTATES, name + ".json"), {"variants": {"": {"model": "grandline:block/" + name}}})


def bars_block(name):
    tex = "grandline:block/" + name
    for part in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
        write_json(os.path.join(BLOCK_MODELS, name + "_" + part + ".json"),
                   {"parent": "minecraft:block/iron_bars_" + part, "render_type": "minecraft:cutout_mipped",
                    "textures": {"particle": tex, "bars": tex, "edge": tex}})
    write_json(os.path.join(ITEM_MODELS, name + ".json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    m = "grandline:block/" + name + "_"
    write_json(os.path.join(BLOCKSTATES, name + ".json"), {"multipart": [
        {"apply": {"model": m + "post_ends"}},
        {"when": {"north": "false", "west": "false", "south": "false", "east": "false"}, "apply": {"model": m + "post"}},
        {"when": {"north": "true", "west": "false", "south": "false", "east": "false"}, "apply": {"model": m + "cap"}},
        {"when": {"north": "false", "west": "false", "south": "false", "east": "true"}, "apply": {"model": m + "cap", "y": 90}},
        {"when": {"north": "false", "west": "false", "south": "true", "east": "false"}, "apply": {"model": m + "cap_alt"}},
        {"when": {"north": "false", "west": "true", "south": "false", "east": "false"}, "apply": {"model": m + "cap_alt", "y": 90}},
        {"when": {"north": "true"}, "apply": {"model": m + "side"}},
        {"when": {"east": "true"}, "apply": {"model": m + "side", "y": 90}},
        {"when": {"south": "true"}, "apply": {"model": m + "side_alt"}},
        {"when": {"west": "true"}, "apply": {"model": m + "side_alt", "y": 90}},
    ]})


# ---------------------------------------------------------------------------------------------------------------

def items():
    gold = (220, 182, 70)
    return {
        "cutlass": (sword(guard=gold, guard_kind="basket", handle=(70, 44, 26), wrap=(110, 72, 40), wide=True,
                          tip=LENGTH - 2.4), False),
        "katana": (sword(handle=(30, 28, 36), wrap=(220, 214, 200), guard=(150, 150, 160)), False),
        "naginata": (polearm(naginata_head, ring=(180, 150, 70)), True),
        "kanabo": (kanabo(), False),
        "seastone_jitte": (jitte(), False),
        "sandai_kitetsu": (sword(handle=(150, 26, 30), wrap=(70, 14, 18), guard=gold, hamon=(250, 210, 210)), False),
        "wado_ichimonji": (sword(handle=(242, 242, 246), wrap=(190, 196, 206), guard=gold, edge=(250, 252, 255)), False),
        "shusui": (sword(blade=BLACK_STEEL, edge=(170, 60, 66), handle=(90, 20, 26), wrap=(30, 24, 26), guard=(60, 54, 40)),
                   False),
        "enma": (sword(handle=(84, 40, 98), wrap=(30, 20, 36), guard=gold, edge=(246, 236, 255)), False),
        "kikoku": (sword(handle=(240, 240, 240), wrap=(30, 30, 30), guard=(70, 70, 76), guard_kind="cross",
                         tip=LENGTH - 0.4), True),
        "gryphon": (sword(handle=(26, 24, 28), wrap=(60, 56, 62), guard=gold, guard_kind="basket",
                          tip=LENGTH - 1.4), False),
        "yoru": (sword(blade=BLACK_STEEL, edge=(120, 170, 140), handle=(30, 28, 30), wrap=(200, 170, 60), guard=gold,
                       guard_kind="cross", tip=LENGTH - 0.4), True),
        "murakumogiri": (polearm(bisento_head, shaft=((70, 60, 54), (40, 34, 30)), head_from=11.0), True),
        "morgans_axe": (polearm(axe_head, shaft=((110, 112, 122), (74, 76, 86)), head_from=9.0), True),
        "kiribachi": (kiribachi(), True),
        "buggys_knife": (sword(handle=(150, 30, 34), wrap=(232, 200, 70), guard=(212, 176, 70), guard_kind="cross", tip=LENGTH - 5.5),
                         False),
        "cat_claws": (cat_claws(), False),
        "flintlock": (flintlock(), False),
        "musket": (musket(), True),
    }


def main():
    for d in (ITEM_TEX, BLOCK_TEX, ITEM_MODELS, BLOCK_MODELS, BLOCKSTATES):
        os.makedirs(d, exist_ok=True)
    for name, (img, long) in items().items():
        img.save(os.path.join(ITEM_TEX, name + ".png"))
        item_model(name, True, long)
    flat = {
        "iron_bullet": balls(((214, 216, 222), (150, 154, 162), (94, 98, 108)), ((5.5, 10.5), (10.5, 9.5), (8.0, 5.5))),
        "seastone_bullet": balls(SEASTONE, ((5.5, 10.5), (10.5, 9.5), (8.0, 5.5))),
        "seastone_cuffs": cuffs(),
        "seastone_net": net(),
        "raw_seastone": raw_seastone(),
        "seastone_ingot": ingot(),
        "chop_fist": chop_fist(),
    }
    for name, img in flat.items():
        img.save(os.path.join(ITEM_TEX, name + ".png"))
        item_model(name, False)
    ore(11, (124, 124, 126)).save(os.path.join(BLOCK_TEX, "seastone_ore.png"))
    ore(23, (76, 76, 84), bands=True).save(os.path.join(BLOCK_TEX, "deepslate_seastone_ore.png"))
    seastone_block().save(os.path.join(BLOCK_TEX, "seastone_block.png"))
    bars().save(os.path.join(BLOCK_TEX, "seastone_bars.png"))
    for name in ("seastone_ore", "deepslate_seastone_ore", "seastone_block"):
        cube_block(name)
    bars_block("seastone_bars")
    print("Wrote", len(items()) + len(flat), "item sprites and 4 block textures")


if __name__ == "__main__":
    main()
