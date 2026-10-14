#!/usr/bin/env python3
"""Generates the crew Jolly Roger textures: a flag (32x16) and a sail (64x64) for every emblem in JollyRoger.java.

Run from the repository root: python3 tools/gen_crews.py
"""
import os
import sys

from PIL import ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from gen_ships import canvas, skull, straw_hat  # noqa: E402

OUT = "src/main/resources/assets/grandline/textures/entity/ship"
BLACK = (28, 28, 32)
BONE = (244, 244, 236)


def crossed_swords(d, cx, cy, s):
    for sx in (-1, 1):
        d.line([(cx - sx * s * 2.2, cy - s * 1.6), (cx + sx * s * 2.0, cy + s * 2.2)], fill=(200, 204, 212), width=max(1, int(s * 0.3)))
        hx, hy = cx + sx * s * 2.0, cy + s * 2.2
        d.line([(hx - s * 0.5, hy - sx * s * 0.5 * 0), (hx + s * 0.5, hy)], fill=(150, 110, 50), width=max(1, int(s * 0.35)))


def beard(d, cx, cy, s):
    """A great crescent moustache, its tips curling up past the skull."""
    d.arc([cx - s * 2.8, cy - s * 0.6, cx + s * 2.8, cy + s * 2.6], 195, 345, fill=BONE, width=max(2, int(s * 0.55)))
    for sx in (-1, 1):
        d.line([(cx + sx * s * 2.6, cy + s * 0.2), (cx + sx * s * 2.9, cy - s * 0.6)], fill=BONE, width=max(2, int(s * 0.45)))


def shark(d, cx, cy, s):
    for i in range(-3, 4):
        x = cx + i * s * 0.55
        d.polygon([(x - s * 0.25, cy + s * 1.2), (x + s * 0.25, cy + s * 1.2), (x, cy + s * 2.1)], fill=BONE)
    d.polygon([(cx - s * 2.2, cy + s * 0.2), (cx - s * 1.4, cy - s * 1.6), (cx - s * 0.9, cy + s * 0.2)], fill=(90, 150, 200))


def flame(d, cx, cy, s):
    for i, (ox, h) in enumerate(((-1.3, 2.4), (0, 3.0), (1.3, 2.4))):
        x = cx + ox * s
        d.polygon([(x - s * 0.7, cy - s * 0.4), (x, cy - s * h), (x + s * 0.7, cy - s * 0.4)], fill=(232, 120, 36))
        d.polygon([(x - s * 0.35, cy - s * 0.5), (x, cy - s * h * 0.65), (x + s * 0.35, cy - s * 0.5)], fill=(250, 210, 80))


def heart(d, cx, cy, s):
    d.ellipse([cx - s * 1.6, cy - s * 1.4, cx, cy + s * 0.2], fill=(240, 220, 60))
    d.ellipse([cx, cy - s * 1.4, cx + s * 1.6, cy + s * 0.2], fill=(240, 220, 60))
    d.polygon([(cx - s * 1.55, cy - s * 0.4), (cx + s * 1.55, cy - s * 0.4), (cx, cy + s * 1.8)], fill=(240, 220, 60))
    d.arc([cx - s * 0.9, cy - s * 0.6, cx + s * 0.9, cy + s * 0.9], 20, 160, fill=BLACK, width=max(1, int(s * 0.25)))
    for ex in (cx - s * 0.5, cx + s * 0.5):
        d.ellipse([ex - s * 0.18, cy - s * 0.7, ex + s * 0.18, cy - s * 0.3], fill=BLACK)


def emblem(name, d, cx, cy, s):
    if name == "classic":
        skull(d, cx, cy, s)
    elif name == "straw_hat":
        skull(d, cx, cy + s * 0.3, s)
        straw_hat(d, cx, cy - s * 0.7, s)
    elif name == "crossed_swords":
        crossed_swords(d, cx, cy, s)
        skull(d, cx, cy, s, crossbones=False)
    elif name == "beard":
        skull(d, cx, cy - s * 0.2, s, crossbones=False)
        beard(d, cx, cy + s * 0.9, s)
    elif name == "shark":
        skull(d, cx, cy - s * 0.3, s, crossbones=False)
        shark(d, cx, cy - s * 0.2, s)
    elif name == "flame":
        flame(d, cx, cy, s)
        skull(d, cx, cy + s * 0.2, s)
    elif name == "heart":
        heart(d, cx, cy, s)
    else:
        raise ValueError(name)


EMBLEMS = ["classic", "straw_hat", "crossed_swords", "beard", "shark", "flame", "heart"]


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, name in enumerate(EMBLEMS):
        flag = canvas((32, 16), BLACK, 40 + i)
        emblem(name, ImageDraw.Draw(flag), 16, 7, 3.2)
        flag.save(f"{OUT}/flag_crew_{name}.png")
        sail = canvas((64, 64), (44, 44, 50), 60 + i)
        emblem(name, ImageDraw.Draw(sail), 32, 30, 8.5)
        sail.save(f"{OUT}/sail_crew_{name}.png")
    print(f"{len(EMBLEMS)} Jolly Rogers written")


if __name__ == "__main__":
    main()
