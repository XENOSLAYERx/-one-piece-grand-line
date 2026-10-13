#!/usr/bin/env python3
"""Generates the mod's original procedural textures: particle sprites and HUD ability glyphs.

Usage: python3 tools/gen_textures.py   (writes into src/main/resources/assets/grandline/textures)

Particle sprites are white/greyscale so the game can tint them per effect. Ability glyphs are white with a soft
dark outline; the HUD tints them with each ability's colour.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "grandline")
PARTICLE_DIR = os.path.join(ROOT, "textures", "particle")
ICON_DIR = os.path.join(ROOT, "textures", "gui", "ability")


def save(img, folder, name):
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, name + ".png"))


def radial(size, falloff, core=0.0):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / (size / 2)
            a = max(0.0, 1.0 - d) ** falloff
            if d < core:
                a = 1.0
            v = int(255 * min(1.0, 0.75 + 0.25 * (1 - d)))
            px[x, y] = (v, v, v, int(255 * a))
    return img


# ---------------------------------------------------------------------------------------------------------------
# Particles
# ---------------------------------------------------------------------------------------------------------------

def particles():
    rnd = random.Random(1337)

    save(radial(8, 1.6, core=0.18), PARTICLE_DIR, "spark")
    save(radial(16, 2.2), PARTICLE_DIR, "glow")

    # Flame: 4 animation frames of a flickering teardrop, hot (bright) at the bottom centre.
    for f in range(4):
        size = 16
        img = Image.new("RGBA", (size, size))
        px = img.load()
        for y in range(size):
            for x in range(size):
                ny = y / (size - 1)               # 0 top .. 1 bottom
                width = 0.15 + 0.32 * (ny ** 0.8)  # narrow tip, wide base
                wobble = 0.06 * math.sin((ny * 6 + f * 1.7)) * (1 - ny)
                dx = abs((x - (size - 1) / 2) / (size - 1) - wobble)
                if dx < width and ny > 0.08 + 0.05 * math.sin(f * 2.1):
                    edge = 1 - dx / width
                    heat = min(1.0, edge * 0.8 + ny * 0.5)
                    v = int(150 + 105 * heat)
                    a = int(255 * min(1.0, edge * 1.6) * min(1.0, (ny - 0.05) * 3))
                    px[x, y] = (v, v, v, a)
        save(img, PARTICLE_DIR, "flame_%d" % f)

    # Smoke: 4 frames of a lumpy puff that gets thinner.
    for f in range(4):
        size = 16
        img = Image.new("RGBA", (size, size))
        draw = ImageDraw.Draw(img)
        for _ in range(7):
            r = rnd.uniform(2.5, 5.0)
            cx = rnd.uniform(5, 11)
            cy = rnd.uniform(5, 11)
            shade = rnd.randint(170, 235)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(shade, shade, shade, 200 - f * 40))
        img = img.filter(ImageFilter.GaussianBlur(0.8))
        save(img, PARTICLE_DIR, "smoke_%d" % f)

    # Ring: soft annulus, used flat on the ground or facing a direction for shockwaves.
    size = 32
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / (size / 2)
            a = math.exp(-((d - 0.82) ** 2) / 0.006)
            a += 0.25 * math.exp(-((d - 0.6) ** 2) / 0.02)
            px[x, y] = (255, 255, 255, int(255 * min(1.0, a)))
    save(img, PARTICLE_DIR, "ring")

    # Star glint: 4-point sparkle.
    size = 16
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = abs(x - c), abs(y - c)
            a = max(math.exp(-dx * 1.4) * math.exp(-dy * 0.25), math.exp(-dy * 1.4) * math.exp(-dx * 0.25))
            a = max(a, math.exp(-(dx * dx + dy * dy) / 3.0))
            px[x, y] = (255, 255, 255, int(255 * min(1.0, a * 1.3)))
    save(img, PARTICLE_DIR, "star")

    # Shard: small angular fragment.
    size = 8
    img = Image.new("RGBA", (size, size))
    draw = ImageDraw.Draw(img)
    draw.polygon([(1, 6), (3, 0), (7, 2), (5, 7)], fill=(230, 230, 230, 255))
    draw.line([(3, 0), (5, 7)], fill=(255, 255, 255, 255))
    save(img, PARTICLE_DIR, "shard")

    # Streak: horizontal motion line, brightest in the middle.
    size = 16
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            vy = math.exp(-((y - 7.5) ** 2) / 1.2)
            vx = math.sin(math.pi * x / (size - 1)) ** 0.7
            px[x, y] = (255, 255, 255, int(255 * vy * vx))
    save(img, PARTICLE_DIR, "streak")


# ---------------------------------------------------------------------------------------------------------------
# Ability glyphs (drawn at 4x, downsampled to 16x16, then outlined)
# ---------------------------------------------------------------------------------------------------------------

S = 64
W = (255, 255, 255, 255)


def glyph_fist(d):
    d.rounded_rectangle([14, 18, 50, 46], radius=8, fill=W)
    for i in range(4):
        d.line([(20 + i * 8, 20), (20 + i * 8, 30)], fill=(0, 0, 0, 0), width=3)
    d.rounded_rectangle([8, 26, 20, 42], radius=5, fill=W)
    d.rectangle([22, 44, 42, 56], fill=W)


def glyph_kick(d):
    d.line([(14, 8), (26, 34), (52, 42)], fill=W, width=10, joint="curve")
    d.ellipse([46, 36, 58, 48], fill=W)
    for i in range(3):
        d.line([(8, 40 + i * 7), (22, 40 + i * 7)], fill=W, width=3)


def glyph_dash(d):
    d.polygon([(30, 12), (56, 32), (30, 52), (36, 32)], fill=W)
    for i, y in enumerate([20, 32, 44]):
        d.line([(6 + i * 2, y), (26, y)], fill=W, width=4)


def glyph_jump(d):
    d.polygon([(32, 6), (52, 30), (40, 30), (40, 44), (24, 44), (24, 30), (12, 30)], fill=W)
    d.arc([12, 46, 52, 62], 180, 360, fill=W, width=4)


def glyph_shield(d):
    d.polygon([(32, 6), (54, 14), (52, 36), (32, 58), (12, 36), (10, 14)], fill=W)
    d.polygon([(32, 14), (46, 19), (45, 34), (32, 49), (32, 14)], fill=(255, 255, 255, 110))


def glyph_beam(d):
    d.ellipse([4, 22, 24, 42], fill=W)
    d.polygon([(14, 26), (60, 28), (60, 36), (14, 38)], fill=W)


def glyph_burst(d):
    pts = []
    for i in range(16):
        r = 28 if i % 2 == 0 else 12
        a = math.pi * 2 * i / 16
        pts.append((32 + r * math.cos(a), 32 + r * math.sin(a)))
    d.polygon(pts, fill=W)


def glyph_wave(d):
    for k, y in enumerate([18, 32, 46]):
        pts = [(x, y + 6 * math.sin(x / 7.0 + k)) for x in range(6, 60, 2)]
        d.line(pts, fill=W, width=6)


def glyph_claw(d):
    for i in range(3):
        x = 16 + i * 14
        d.line([(x, 6), (x + 10, 58)], fill=W, width=7)


def glyph_flame(d):
    d.polygon([(32, 4), (46, 24), (50, 40), (42, 56), (22, 56), (14, 40), (20, 26), (26, 34)], fill=W)
    d.polygon([(32, 30), (38, 42), (34, 52), (28, 52), (26, 44)], fill=(255, 255, 255, 120))


def glyph_swirl(d):
    pts = []
    for i in range(80):
        t = i / 79 * math.pi * 4
        r = 4 + 3.4 * t
        pts.append((32 + r * math.cos(t), 32 + r * math.sin(t)))
    d.line(pts, fill=W, width=6, joint="curve")


def glyph_blade(d):
    d.polygon([(52, 6), (58, 12), (22, 48), (16, 42)], fill=W)
    d.line([(10, 36), (28, 54)], fill=W, width=6)
    d.line([(8, 56), (18, 46)], fill=W, width=6)


def glyph_eye(d):
    d.ellipse([6, 18, 58, 46], fill=W)
    d.ellipse([22, 20, 42, 44], fill=(0, 0, 0, 0))
    d.ellipse([27, 25, 37, 39], fill=W)


def glyph_crown(d):
    d.polygon([(8, 50), (8, 18), (20, 32), (32, 12), (44, 32), (56, 18), (56, 50)], fill=W)
    d.rectangle([8, 52, 56, 58], fill=W)


def glyph_grab(d):
    d.rounded_rectangle([16, 28, 48, 56], radius=8, fill=W)
    for i in range(4):
        d.rounded_rectangle([16 + i * 8, 8 + (i % 2) * 4, 22 + i * 8, 32], radius=3, fill=W)


def glyph_orb(d):
    d.ellipse([10, 10, 54, 54], fill=W)
    d.ellipse([18, 16, 30, 28], fill=(0, 0, 0, 0))


def glyph_transform(d):
    d.arc([8, 8, 56, 56], 200, 340, fill=W, width=7)
    d.polygon([(50, 14), (58, 30), (42, 28)], fill=W)
    d.arc([8, 8, 56, 56], 20, 160, fill=W, width=7)
    d.polygon([(14, 50), (6, 34), (22, 36)], fill=W)


def glyph_bolt(d):
    d.polygon([(36, 4), (14, 36), (30, 36), (24, 60), (50, 24), (34, 24), (42, 4)], fill=W)


def glyph_drop(d):
    d.polygon([(32, 6), (48, 34), (16, 34)], fill=W)
    d.ellipse([16, 22, 48, 56], fill=W)


def glyph_cloud(d):
    for box in ([8, 28, 32, 52], [20, 14, 46, 40], [34, 24, 58, 50], [16, 34, 50, 54]):
        d.ellipse(box, fill=W)


def glyph_star(d):
    pts = []
    for i in range(10):
        r = 28 if i % 2 == 0 else 12
        a = -math.pi / 2 + math.pi * 2 * i / 10
        pts.append((32 + r * math.cos(a), 33 + r * math.sin(a)))
    d.polygon(pts, fill=W)


GLYPHS = {
    "fist": glyph_fist, "kick": glyph_kick, "dash": glyph_dash, "jump": glyph_jump, "shield": glyph_shield,
    "beam": glyph_beam, "burst": glyph_burst, "wave": glyph_wave, "claw": glyph_claw, "flame": glyph_flame,
    "swirl": glyph_swirl, "blade": glyph_blade, "eye": glyph_eye, "crown": glyph_crown, "grab": glyph_grab,
    "orb": glyph_orb, "transform": glyph_transform, "bolt": glyph_bolt, "drop": glyph_drop, "cloud": glyph_cloud,
    "star": glyph_star,
}


def icons():
    for name, fn in GLYPHS.items():
        big = Image.new("RGBA", (S, S))
        fn(ImageDraw.Draw(big))
        small = big.resize((16, 16), Image.LANCZOS)
        # Dark outline so the glyph reads on any slot colour.
        alpha = small.split()[3]
        outline = alpha.filter(ImageFilter.MaxFilter(3))
        out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        shadow = Image.new("RGBA", (16, 16), (20, 20, 28, 255))
        out.paste(shadow, (0, 0), outline.point(lambda a: int(a * 0.85)))
        out.alpha_composite(small)
        save(out, ICON_DIR, name)


# ---------------------------------------------------------------------------------------------------------------
# Entity textures
# ---------------------------------------------------------------------------------------------------------------

ENTITY_DIR = os.path.join(ROOT, "textures", "entity")
EFFECT_DIR = os.path.join(ROOT, "textures", "mob_effect")

FUR = (217, 164, 65)
FUR_DARK = (180, 128, 44)
CREAM = (242, 226, 184)
SPOT = (40, 28, 18)


def cube_faces(u, v, w, h, d):
    """UV rectangles (x, y, width, height) of a Minecraft cube: top, bottom, east, north, west, south."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def paint_fur(px, rect, rnd, base=FUR, spots=True):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            n = rnd.uniform(-14, 14)
            c = tuple(max(0, min(255, int(base[i] + n))) for i in range(3))
            px[x, y] = c + (255,)
    if spots and w * h > 6:
        for _ in range(max(1, w * h // 14)):
            sx = rnd.randint(x0, x0 + w - 1)
            sy = rnd.randint(y0, y0 + h - 1)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
                if x0 <= sx + dx < x0 + w and y0 <= sy + dy < y0 + h and rnd.random() < 0.8:
                    px[sx + dx, sy + dy] = SPOT + (255,)
            if x0 <= sx < x0 + w and y0 <= sy < y0 + h:
                px[sx, sy] = FUR_DARK + (255,)  # rosette centre


def leopard():
    rnd = random.Random(42)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    cubes = {
        "body": (0, 0, 8, 8, 18),
        "head": (0, 26, 8, 7, 6),
        "snout": (28, 26, 4, 3, 2),
        "ear_r": (40, 26, 2, 2, 1),
        "ear_l": (46, 26, 2, 2, 1),
        "leg_fr": (0, 39, 3, 10, 3),
        "leg_fl": (12, 39, 3, 10, 3),
        "leg_br": (24, 39, 3, 10, 3),
        "leg_bl": (36, 39, 3, 10, 3),
        "tail": (0, 52, 2, 2, 9),
        "tail_tip": (22, 52, 2, 2, 8),
    }
    for name, (u, v, w, h, d) in cubes.items():
        faces = cube_faces(u, v, w, h, d)
        for face, rect in faces.items():
            belly = face == "bottom" and name in ("body", "head")
            paint_fur(px, rect, rnd, CREAM if belly else FUR, spots=not belly and not name.startswith("ear"))
    # face details on the head front (north face)
    hx, hy, hw, hh = cube_faces(0, 26, 8, 7, 6)["north"]
    for (ex, ey) in ((hx + 1, hy + 2), (hx + 5, hy + 2)):
        px[ex, ey] = (40, 30, 20, 255)
        px[ex + 1, ey] = (120, 200, 90, 255)
        px[ex, ey + 1] = (230, 220, 160, 255)
        px[ex + 1, ey + 1] = (40, 30, 20, 255)
    for x in range(hx + 2, hx + 6):
        px[x, hy + hh - 1] = CREAM + (255,)
    # muzzle: cream with a dark nose
    sx, sy, sw, sh = cube_faces(28, 26, 4, 3, 2)["north"]
    for y in range(sy, sy + sh):
        for x in range(sx, sx + sw):
            px[x, y] = CREAM + (255,)
    px[sx + 1, sy] = (60, 40, 40, 255)
    px[sx + 2, sy] = (60, 40, 40, 255)
    px[sx + 1, sy + 2] = (90, 60, 50, 255)
    px[sx + 2, sy + 2] = (90, 60, 50, 255)
    # dark ear backs and tail rings
    for ear in ((40, 26), (46, 26)):
        rect = cube_faces(ear[0], ear[1], 2, 2, 1)["south"]
        for y in range(rect[1], rect[1] + rect[3]):
            for x in range(rect[0], rect[0] + rect[2]):
                px[x, y] = (30, 22, 16, 255)
    u, v = 22, 52
    for face in cube_faces(u, v, 2, 2, 8).values():
        x0, y0, w, h = face
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if (x - x0 + y - y0) % 4 == 0:
                    px[x, y] = SPOT + (255,)
    save(img, ENTITY_DIR, "leopard")


def leopard_spots():
    """Fur overlay in the player skin layout: semi-transparent golden fur with rosettes, face kept clear."""
    rnd = random.Random(7)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for y in range(64):
        for x in range(64):
            if 8 <= x < 16 and 8 <= y < 16:  # face front: leave the face readable
                continue
            n = rnd.uniform(-10, 10)
            px[x, y] = (int(FUR[0] + n), int(FUR[1] + n), int(FUR[2] + n), 120)
    for _ in range(170):
        x = rnd.randint(0, 62)
        y = rnd.randint(0, 62)
        if 8 <= x < 16 and 8 <= y < 16:
            continue
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if rnd.random() < 0.75:
                px[x + dx, y + dy] = SPOT + (215,)
    save(img, ENTITY_DIR, "leopard_spots")


def glow_orb():
    save(radial(32, 1.8, core=0.12), ENTITY_DIR, "glow_orb")


def bleeding_icon():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(9, 1), (14, 9), (4, 9)], fill=(170, 20, 24, 255))
    d.ellipse([3, 6, 15, 17], fill=(170, 20, 24, 255))
    d.ellipse([6, 9, 9, 12], fill=(240, 120, 120, 255))
    save(img, EFFECT_DIR, "bleeding")


if __name__ == "__main__":
    particles()
    icons()
    leopard()
    leopard_spots()
    glow_orb()
    bleeding_icon()
    print("textures written to", os.path.normpath(ROOT), file=sys.stderr)
