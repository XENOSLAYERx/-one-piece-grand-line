#!/usr/bin/env python3
"""Generates original 16x16 Devil Fruit item sprites and their item models.

Usage: python3 tools/gen_fruits.py   (writes into src/main/resources/assets/grandline)

Each fruit is described by a palette, a body shape, a surface pattern (the characteristic Devil Fruit swirls,
spots, flames...) and a stem style. The renderer supersamples every pixel, shades the body as a lit sphere and
adds a dark outline so the result reads like hand-made pixel art.
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "grandline")
TEX_DIR = os.path.join(ROOT, "textures", "item", "fruit")
MODEL_DIR = os.path.join(ROOT, "models", "item")
SIZE = 16
SS = 4  # supersampling per axis


def rgb(hex_color):
    return ((hex_color >> 16) & 255, (hex_color >> 8) & 255, hex_color & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def scale(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


# ---------------------------------------------------------------------------------------------------------------
# Shapes: return True if (x, y) in [0,16) float coords is inside the fruit body
# ---------------------------------------------------------------------------------------------------------------

def shape_round(x, y):
    return (x - 8) ** 2 + (y - 9.2) ** 2 <= 6.3 ** 2


def shape_pear(x, y):
    top = (x - 8) ** 2 / 3.6 ** 2 + (y - 6.2) ** 2 / 3.8 ** 2 <= 1
    bottom = (x - 8) ** 2 + (y - 10.3) ** 2 <= 5.2 ** 2
    return top or bottom


def shape_oval(x, y):
    return (x - 8) ** 2 / 5.2 ** 2 + (y - 9) ** 2 / 6.6 ** 2 <= 1


def shape_heart(x, y):
    nx, ny = (x - 8) / 6.4, (9.4 - y) / 6.4
    return (nx * nx + ny * ny - 0.42) ** 3 - nx * nx * ny ** 3 * 1.2 <= 0


def shape_wide(x, y):
    return (x - 8) ** 2 / 6.8 ** 2 + (y - 9.6) ** 2 / 5.4 ** 2 <= 1


def shape_star(x, y):
    a = math.atan2(y - 9, x - 8)
    r = math.hypot(x - 8, y - 9)
    return r <= 4.4 + 2.0 * (0.5 + 0.5 * math.cos(5 * a))


def shape_banana(x, y):
    # crescent: big circle minus offset circle
    return (x - 8) ** 2 + (y - 9) ** 2 <= 6.5 ** 2 and (x - 10.5) ** 2 + (y - 6.5) ** 2 >= 5.0 ** 2


SHAPES = {"round": shape_round, "pear": shape_pear, "oval": shape_oval, "heart": shape_heart, "wide": shape_wide,
          "star": shape_star, "banana": shape_banana}


# ---------------------------------------------------------------------------------------------------------------
# Patterns: return 0..1 strength of the pattern colour at (x, y)
# ---------------------------------------------------------------------------------------------------------------

def pattern_swirls(x, y, seed):
    """Classic Devil Fruit curls: a few bold Archimedean spirals."""
    centres = [(5.3, 7.4), (10.8, 7.6), (7.8, 11.9), (11.6, 12.2), (4.6, 11.6)]
    for i, (cx, cy) in enumerate(centres):
        cx += math.sin(seed * 1.7 + i) * 0.5
        cy += math.cos(seed * 1.3 + i) * 0.5
        dx, dy = x - cx, y - cy
        r = math.hypot(dx, dy)
        if r > 2.9:
            continue
        theta0 = (math.atan2(dy, dx) + (seed + i) * 1.1) % (2 * math.pi)
        for k in range(2):
            theta = theta0 + 2 * math.pi * k
            if theta > 2 * math.pi * 1.35:
                continue
            spiral_r = 0.25 + 0.36 * theta
            if abs(r - spiral_r) < 0.42:
                return 1.0
    return 0.0


def pattern_flames(x, y, seed):
    """Big upward flame licks."""
    v = math.sin(x * 0.85 + seed) * 2.2 + (16 - y) * 0.75
    return 1.0 if (v % 4.2) < 1.7 and y > 4.5 else 0.0


def pattern_spots(x, y, seed):
    """Leopard rosettes: dark broken rings."""
    spots = [(5.0, 6.8), (10.2, 6.5), (7.4, 10.0), (11.6, 11.2), (4.3, 12.0), (8.4, 14.0)]
    for i, (cx, cy) in enumerate(spots):
        r = math.hypot(x - cx, y - cy)
        a = math.atan2(y - cy, x - cx) + seed + i
        if 0.75 < r < 1.6 and math.sin(a * 3) > -0.6:
            return 1.0
    return 0.0


def pattern_stripes(x, y, seed):
    return 1.0 if ((x + y * 0.6 + seed) % 3.4) < 1.1 else 0.0


def pattern_rings(x, y, seed):
    r = math.hypot(x - 8, y - 9)
    return 1.0 if (r + seed) % 2.6 < 0.9 else 0.0


def pattern_dots(x, y, seed):
    return 1.0 if (int(x * 0.7 + seed) + int(y * 0.7)) % 3 == 0 and (x * 3 + y * 5 + seed) % 4 < 1.5 else 0.0


PATTERNS = {"swirls": pattern_swirls, "flames": pattern_flames, "spots": pattern_spots, "stripes": pattern_stripes,
            "rings": pattern_rings, "dots": pattern_dots}


# ---------------------------------------------------------------------------------------------------------------

def render(spec):
    body = rgb(spec["color"])
    accent = rgb(spec["accent"])
    stem = rgb(spec.get("stem", 0x3E6B2A))
    shape = SHAPES[spec.get("shape", "round")]
    pattern = PATTERNS[spec.get("pattern", "swirls")]
    seed = spec.get("seed", 1)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    inside = [[False] * SIZE for _ in range(SIZE)]

    for y in range(SIZE):
        for x in range(SIZE):
            hits = 0
            pat = 0.0
            for sy in range(SS):
                for sx in range(SS):
                    fx = x + (sx + 0.5) / SS
                    fy = y + (sy + 0.5) / SS
                    if shape(fx, fy):
                        hits += 1
                        pat += pattern(fx, fy, seed)
            if hits < SS * SS * 0.45:
                continue
            inside[y][x] = True
            pat /= hits
            # sphere-ish lighting from the top-left
            nx, ny = (x + 0.5 - 8) / 7.0, (y + 0.5 - 9) / 7.0
            light = 0.62 + 0.38 * max(0.0, 1.0 - math.hypot(nx + 0.35, ny + 0.4))
            colour = mix(body, accent, min(1.0, pat * 1.4))
            colour = scale(colour, light + 0.15)
            px[x, y] = colour + (255,)

    # highlight pixel
    for (hx, hy) in [(5, 6), (6, 5)]:
        if inside[hy][hx]:
            r, g, b, a = px[hx, hy]
            px[hx, hy] = mix((r, g, b), (255, 255, 255), 0.55) + (255,)

    # stem + leaf (drawn before outline so they get outlined too)
    stem_style = spec.get("stem_style", "curl")
    stem_pixels = {
        "curl": [(8, 3), (8, 2), (9, 1), (10, 1), (11, 2)],
        "straight": [(8, 3), (8, 2), (8, 1)],
        "hook": [(8, 3), (7, 2), (7, 1), (8, 0)],
    }[stem_style]
    leaf = [(9, 3), (10, 3), (10, 4)] if spec.get("leaf", True) else []
    for (sx, sy) in stem_pixels:
        if not inside[sy][sx]:
            px[sx, sy] = scale(stem, 0.85) + (255,)
            inside[sy][sx] = True
    for (lx, ly) in leaf:
        if not inside[ly][lx] or ly <= 3:
            px[lx, ly] = mix(stem, (120, 200, 90), 0.35) + (255,)
            inside[ly][lx] = True

    # outline
    outline = scale(mix(body, (0, 0, 0), 0.5), 0.55)
    out = img.copy()
    opx = out.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if inside[y][x]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx_, ny_ = x + dx, y + dy
                if 0 <= nx_ < SIZE and 0 <= ny_ < SIZE and inside[ny_][nx_]:
                    opx[x, y] = outline + (255,)
                    break
    return out


# fruit id -> look. Colours are picked to echo each fruit's power.
FRUITS = {
    "gum_gum": {"color": 0xA45AD0, "accent": 0x3E1660, "pattern": "swirls", "shape": "round", "seed": 3, "stem": 0x2F6B3A},
    "leopard": {"color": 0xE2B04A, "accent": 0x5A3A16, "pattern": "spots", "shape": "pear", "seed": 2, "stem": 0x4D3A1C},
    "flame": {"color": 0xF0602A, "accent": 0xFFC23A, "pattern": "flames", "shape": "round", "seed": 5, "stem": 0x7A2A10,
              "stem_style": "hook"},
}


def write_model(fruit_id):
    os.makedirs(MODEL_DIR, exist_ok=True)
    model = {"parent": "minecraft:item/generated", "textures": {"layer0": "grandline:item/fruit/" + fruit_id}}
    with open(os.path.join(MODEL_DIR, fruit_id + "_fruit.json"), "w") as f:
        json.dump(model, f, indent=2)
        f.write("\n")


def main(only=None):
    os.makedirs(TEX_DIR, exist_ok=True)
    for fruit_id, spec in FRUITS.items():
        if only and fruit_id not in only:
            continue
        render(spec).save(os.path.join(TEX_DIR, fruit_id + ".png"))
        write_model(fruit_id)
    print("fruit sprites written:", len(FRUITS), file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1:])
