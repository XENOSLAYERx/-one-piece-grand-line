#!/usr/bin/env python3
"""Generates every Zoan body: the full-beast model and the parts a hybrid wears, with their textures.

Usage: python3 tools/gen_zoans.py [--preview DIR]

Writes
  src/main/java/com/nerodev/grandline/client/model/ZoanShapeData.java  (geometry as compact strings, parsed on the client)
  src/main/resources/assets/grandline/textures/entity/zoan/<id>.png         beast texture
  src/main/resources/assets/grandline/textures/entity/zoan/<id>_parts.png   hybrid parts texture
  src/main/resources/assets/grandline/textures/entity/zoan/<id>_overlay.png hybrid fur/scale overlay (player skin layout)
With --preview, also renders every body to DIR for a quick visual check.

All geometry is original box modelling in Minecraft model space (see tools/zoanlib.py).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from zoanlib import Cube as C, Shape, paint_overlay, paint_shape, player_polys, render, shape_polys, PLAYER  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "nerodev", "grandline", "client", "model", "ZoanShapeData.java")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "grandline", "textures", "entity", "zoan")

PI = math.pi


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


# ---------------------------------------------------------------------------------------------------------------
# Body plans
# ---------------------------------------------------------------------------------------------------------------

def quadruped(s, leg=10, legw=3, body=(8, 8, 18), neck=None, head=(8, 7, 7), snout=(4, 3, 3), ears="pointy", tail=(2, 3, 8),
              feet="paw", head_drop=0, body_mat="fur", legs_front=None, spread=0.0):
    """A four-legged animal. Returns a dict of useful anchor names and positions for extras."""
    bw, bh, bl = body
    leg_front = legs_front or leg
    top = 24 - leg  # top of the back legs
    by = top - bh / 2 + 1
    tilt = math.atan2(leg_front - leg, bl) if leg_front != leg else 0.0
    s.add("body", pivot=(0, by, 0), rot=(-tilt, 0, 0), role="BODY",
          cubes=[C(-bw / 2, -bh / 2, -bl / 2, bw, bh, bl, body_mat)])
    lx = bw / 2 - legw / 2 - 0.5 + spread
    fz, bz = -bl / 2 + legw / 2 + 1, bl / 2 - legw / 2 - 1
    for name, x, z, length, role, phase in (("leg_fr", -lx, fz, leg_front, "LEG_FRONT", PI), ("leg_fl", lx, fz, leg_front, "LEG_FRONT", 0),
                                          ("leg_br", -lx, bz, leg, "LEG_BACK", 0), ("leg_bl", lx, bz, leg, "LEG_BACK", PI)):
        foot = 2 if feet in ("hoof", "claw") else 0
        cubes = [C(-legw / 2, 0, -legw / 2, legw, length - foot, legw, body_mat)]
        if foot:
            cubes.append(C(-legw / 2, length - foot, -legw / 2, legw, foot, legw, "hoof" if feet == "hoof" else "claw"))
        s.add(name, pivot=(x, 24 - length, z), role=role, param=phase, cubes=cubes)
    hw, hh, hl = head
    front_y = 24 - leg_front - bh + 1
    if neck:
        nw, nh, nl, pitch = neck
        s.add("neck", pivot=(0, front_y + nh / 2 + 1, -bl / 2 + 2), rot=(pitch, 0, 0), role="NECK", param=0.35,
              cubes=[C(-nw / 2, -nh / 2, -nl, nw, nh, nl, body_mat)])
        s.add("head", "neck", pivot=(0, -1, -nl + 1), rot=(-pitch + 0.15, 0, 0), role="HEAD", param=0.6,
              cubes=[C(-hw / 2, -hh / 2, -hl, hw, hh, hl, "head")])
    else:
        s.add("head", pivot=(0, front_y + hh / 2 + head_drop, -bl / 2 + 1), role="HEAD", param=0.8,
              cubes=[C(-hw / 2, -hh / 2, -hl, hw, hh, hl, "head")])
    if snout:
        sw, sh, sl = snout
        s.add("snout", "head", pivot=(0, hh / 2 - sh, -hl), cubes=[C(-sw / 2, 0, -sl, sw, sh, sl, "fur"),
                                                                C(-1, -0.01, -sl - 0.6, 2, 1, 1, "nose")])
    if ears == "pointy":
        for side in (-1, 1):
            s.add("ear_%s" % ("l" if side > 0 else "r"), "head", pivot=(side * (hw / 2 - 1.5), -hh / 2, -hl / 2),
                  role="EAR", param=side, cubes=[C(-1, -3, -0.5, 2, 3, 1, "fur")])
    elif ears == "round":
        for side in (-1, 1):
            s.add("ear_%s" % ("l" if side > 0 else "r"), "head", pivot=(side * (hw / 2 - 1.5), -hh / 2, -hl / 2),
                  role="EAR", param=side, cubes=[C(-1, -2, -0.5, 2, 2, 1, "fur")])
    elif ears == "big":
        for side in (-1, 1):
            s.add("ear_%s" % ("l" if side > 0 else "r"), "head", pivot=(side * (hw / 2 - 1), -hh / 2, -hl / 2),
                  role="EAR", param=side, cubes=[C(-1.5, -5, -0.5, 3, 5, 1, "fur")])
    elif ears == "long":
        for side in (-1, 1):
            s.add("ear_%s" % ("l" if side > 0 else "r"), "head", pivot=(side * (hw / 2 - 1), -hh / 2, -hl + 2),
                  rot=(0.2, 0, side * 0.15), role="EAR", param=side, cubes=[C(-1, -4, -0.5, 2, 4, 1, "fur")])
    if tail:
        tw, n, tl = tail
        parent = "body"
        pivot = (0, -bh / 2 + 1.5, bl / 2 - 0.5)
        for i in range(n):
            w = max(1, tw - (i if tw > 2 else 0))
            s.add("tail%d" % i, parent, pivot=pivot, rot=(-0.6 if i == 0 else 0.25, 0, 0), role="TAIL", param=i,
                  cubes=[C(-w / 2, -w / 2, 0, w, w, tl, "fur")])
            parent, pivot = "tail%d" % i, (0, 0, tl - 0.5)
    return {"by": by, "bh": bh, "bl": bl, "bw": bw, "hh": hh, "hl": hl, "hw": hw}


def bird(s, body=(6, 6, 10), leg=5, head=(4, 4, 4), beak=(2, 2, 3), wing=(8, 9), tip=(9, 7), tail=(6, 1, 7),
         beak_mat="beak", crest=None, body_mat="feather"):
    bw, bh, bl = body
    by = 24 - leg - bh / 2
    s.add("body", pivot=(0, by, 0), rot=(-0.18, 0, 0), role="BODY", cubes=[C(-bw / 2, -bh / 2, -bl / 2, bw, bh, bl, body_mat)])
    hw, hh, hl = head
    s.add("head", pivot=(0, by - bh / 2 + 0.5, -bl / 2 + 1), role="HEAD", param=0.9,
          cubes=[C(-hw / 2, -hh, -hl, hw, hh, hl, "head_side")])
    if beak:
        bkw, bkh, bkl = beak
        s.add("beak", "head", pivot=(0, -hh / 2, -hl), cubes=[C(-bkw / 2, -bkh / 2, -bkl, bkw, bkh, bkl, beak_mat)])
    if crest:
        cw, ch, cl = crest
        s.add("crest", "head", pivot=(0, -hh, -1), rot=(-0.5, 0, 0), cubes=[C(-cw / 2, -ch, 0, cw, ch, cl, "accent")])
    ww, wd = wing
    tw_, td = tip
    for side in (-1, 1):
        n = "l" if side > 0 else "r"
        x0 = 0 if side > 0 else -ww
        # Folded: span laid back along the body, chord hanging down its side; the tip folds back over the outside.
        s.add("wing_" + n, pivot=(side * bw / 2, by - bh / 2 + 0.5, -bl / 2 + 1.5), rot=(0, -side * PI / 2, -side * 1.3),
              role="WING", param=side, cubes=[C(x0, 0, 0, ww, 1, wd, "wing")])
        x1 = 0 if side > 0 else -tw_
        s.add("tip_" + n, "wing_" + n, pivot=(side * ww, 2.02, 0.5), rot=(0, side * 0.1, PI), role="WING_TIP", param=side,
              cubes=[C(x1, 0, 0, tw_, 1, td, "wing")])
    if tail:
        tw, th, tl = tail
        s.add("tail0", pivot=(0, by - bh / 2 + 1.5, bl / 2 - 1), rot=(-0.25, 0, 0), role="TAIL", param=0,
              cubes=[C(-tw / 2, 0, 0, tw, th, tl, "wing")])
    for side in (-1, 1):
        s.add("leg_" + ("l" if side > 0 else "r"), pivot=(side * 1.5, 24 - leg, 1), role="LEG", param=0 if side > 0 else PI,
              cubes=[C(-0.5, 0, -0.5, 1, leg - 1, 1, "claw"), C(-1.5, leg - 1, -2.5, 3, 1, 3, "claw")])
    return {"by": by, "bh": bh, "bl": bl, "bw": bw}


def serpent(s, n=10, seg=(6, 5, 8), taper=0.4, head=(7, 5, 9), lift=(), mat="scale", jaw=True, head_mat="head"):
    """Head first, then the body trailing back in a chain. lift: per-segment xRot for the raised front of a cobra."""
    sw, sh, sl = seg
    y0 = 24 - sh / 2
    raise_h, cum = 0.0, 0.0
    for a in lift:
        cum += a
        raise_h += math.sin(-cum) * sl
    parent = None
    pivot = (0, y0 - raise_h, -sl * 0.5)
    for i in range(n):
        t = i / max(1, n - 1)
        w = max(2, int(round(sw * (1 - taper * max(0.0, t - 0.4) / 0.6))))
        h = max(2, int(round(sh * (1 - taper * max(0.0, t - 0.4) / 0.6))))
        rx = lift[i] if i < len(lift) else (-sum(lift) if i == len(lift) and lift else 0.0)
        s.add("seg%d" % i, parent, pivot=pivot, rot=(rx, 0, 0), role="SEGMENT", param=i,
              cubes=[C(-w / 2, -h / 2, 0, w, h, sl, mat)])
        parent, pivot = "seg%d" % i, (0, 0, sl - 0.5)
    hw, hh, hl = head
    s.add("head", "seg0", pivot=(0, 0, 0.5), rot=(sum(lift[:1]) * -1 if lift else 0, 0, 0), role="HEAD", param=0.9,
          cubes=[C(-hw / 2, -hh / 2, -hl, hw, hh - (2 if jaw else 0), hl, head_mat)])
    if jaw:
        s.add("jaw", "head", pivot=(0, hh / 2 - 2, -1), role="JAW", cubes=[C(-hw / 2 + 0.5, 0, -hl + 1, hw - 1, 2, hl - 1, mat)])
    return {"y0": y0, "raise": raise_h}


def theropod(s, leg=14, legw=5, body=(12, 13, 22), neck=(7, 7, 8, -0.8), head=(9, 9, 15), arms=True, tail=(9, 5, 8),
             mat="scale"):
    bw, bh, bl = body
    hip = 24 - leg
    by = hip - bh / 2 + 3
    s.add("body", pivot=(0, by, 0), rot=(-0.12, 0, 0), role="BODY", cubes=[C(-bw / 2, -bh / 2, -bl / 2, bw, bh, bl, mat)])
    for side in (-1, 1):
        s.add("leg_" + ("l" if side > 0 else "r"), pivot=(side * (bw / 2 - legw / 2), hip, 3), role="LEG",
              param=0 if side > 0 else PI,
              cubes=[C(-legw / 2, 0, -legw / 2, legw, leg - 3, legw + 1, mat), C(-legw / 2 - 0.5, leg - 3, -legw / 2 - 3, legw + 1, 3, legw + 4, "claw")])
    nw, nh, nl, pitch = neck
    s.add("neck", pivot=(0, by - bh / 2 + 3, -bl / 2 + 2), rot=(pitch, 0, 0), role="NECK", param=0.3,
          cubes=[C(-nw / 2, -nh / 2, -nl, nw, nh, nl, mat)])
    hw, hh, hl = head
    s.add("head", "neck", pivot=(0, -1, -nl + 1), rot=(-pitch, 0, 0), role="HEAD", param=0.6,
          cubes=[C(-hw / 2, -hh / 2, -hl, hw, hh - 3, hl, "head_side"), C(-hw / 2 + 1, hh / 2 - 3.01, -hl + 1, hw - 2, 1, hl - 2, "tooth")])
    s.add("jaw", "head", pivot=(0, hh / 2 - 3, -1), role="JAW",
          cubes=[C(-hw / 2 + 1, 0, -hl + 2, hw - 2, 3, hl - 2, mat), C(-hw / 2 + 1.5, -0.99, -hl + 2.5, hw - 3, 1, hl - 3, "tooth")])
    if arms:
        for side in (-1, 1):
            s.add("arm_" + ("l" if side > 0 else "r"), pivot=(side * (bw / 2 - 1), by + 2, -bl / 2 + 4), rot=(0.7, 0, 0),
                  role="ARM", param=0 if side > 0 else PI, cubes=[C(-1, 0, -1, 2, 6, 2, mat), C(-1, 5.5, -2, 2, 1, 2, "claw")])
    tw, n, tl = tail
    parent, pivot = "body", (0, -bh / 2 + 4, bl / 2 - 1)
    for i in range(n):
        w = max(2, tw - i * 2)
        s.add("tail%d" % i, parent, pivot=pivot, rot=(0.1 if i == 0 else -0.05, 0, 0), role="TAIL", param=i,
              cubes=[C(-w / 2, -w / 2, 0, w, w, tl, mat)])
        parent, pivot = "tail%d" % i, (0, 0, tl - 0.5)
    return {"by": by, "bh": bh, "bl": bl, "bw": bw, "hh": hh, "hl": hl, "hw": hw}


def spider(s, body=(12, 8, 12), abdomen=(18, 15, 20), leg=(16, 18), legw=2, mat="fur", ox_head=False):
    bw, bh, bl = body
    by = 24 - 10
    s.add("body", pivot=(0, by, 0), role="BODY", cubes=[C(-bw / 2, -bh / 2, -bl / 2, bw, bh, bl, mat)])
    aw, ah, al = abdomen
    s.add("abdomen", "body", pivot=(0, -1, bl / 2 - 1), rot=(0.35, 0, 0), role="TAIL", param=0,
          cubes=[C(-aw / 2, -ah / 2 - 2, 0, aw, ah, al, mat)])
    if not ox_head:
        s.add("head", pivot=(0, by, -bl / 2), role="HEAD", param=0.4,
              cubes=[C(-4, -3, -5, 8, 6, 5, "head")])
        for side in (-1, 1):
            s.add("fang_" + ("l" if side > 0 else "r"), "head", pivot=(side * 1.8, 2, -5), rot=(-0.3, 0, 0),
                  cubes=[C(-0.5, 0, -1, 1, 3, 1, "tooth")])
    up, down = leg
    for i, z in enumerate((-bl / 2 + 2, -bl / 2 + 5, bl / 2 - 5, bl / 2 - 2)):
        fan = (-0.5 + i * 0.33)
        for side in (-1, 1):
            n = "%s%d" % ("l" if side > 0 else "r", i)
            x0 = 0 if side > 0 else -up
            s.add("sleg_" + n, pivot=(side * (bw / 2 - 1), by - 1, z), rot=(0, side * fan, -side * 0.75), role="SPIDER_LEG",
                  param=(i * 1.6 + (PI if side > 0 else 0)),
                  cubes=[C(x0, -legw / 2, -legw / 2, up, legw, legw, mat)])
            x1 = 0 if side > 0 else -down
            s.add("sfoot_" + n, "sleg_" + n, pivot=(side * up, 0, 0), rot=(0, 0, side * 1.95),
                  cubes=[C(x1, -legw / 2, -legw / 2, down, legw, legw, "dark")])
    return {"by": by}


def humanoid(s, height=64, k=None, mat="fur", head_mat="head"):
    """A giant biped proportioned like a player scaled by k (player is 32 px tall)."""
    k = k or height / 32.0
    hip = 24 - 12 * k
    s.add("torso", pivot=(0, hip - 12 * k, 0), role="BODY", cubes=[C(-4 * k, 0, -2 * k, 8 * k, 12 * k, 4 * k, mat)])
    s.add("head", pivot=(0, hip - 12 * k, 0), role="HEAD", param=1.0, cubes=[C(-4 * k, -8 * k, -4 * k, 8 * k, 8 * k, 8 * k, head_mat)])
    for side in (-1, 1):
        n = "l" if side > 0 else "r"
        s.add("arm_" + n, pivot=(side * 5 * k, hip - 10 * k, 0), role="ARM", param=0 if side > 0 else PI,
              cubes=[C((-1 if side > 0 else -3) * k, -2 * k, -2 * k, 4 * k, 12 * k, 4 * k, mat)])
        s.add("leg_" + n, pivot=(side * 1.9 * k, hip, 0), role="LEG", param=PI if side > 0 else 0,
              cubes=[C(-2 * k, 0, -2 * k, 4 * k, 12 * k, 4 * k, mat)])
    return {"k": k, "hip": hip}


# ---------------------------------------------------------------------------------------------------------------
# Hybrid parts (anchored to the player model)
# ---------------------------------------------------------------------------------------------------------------

def h_ears(s, kind="pointy", mat="fur"):
    for side in (-1, 1):
        n = "ear_" + ("l" if side > 0 else "r")
        if kind == "pointy":
            s.add(n, anchor="HEAD", pivot=(side * 2.5, -8, -0.5), role="EAR", param=side, cubes=[C(-1, -3, -0.5, 2, 3, 1, mat)])
        elif kind == "round":
            s.add(n, anchor="HEAD", pivot=(side * 2.8, -8, -0.5), role="EAR", param=side, cubes=[C(-1, -2, -0.5, 2, 2, 1, mat)])
        elif kind == "big":
            s.add(n, anchor="HEAD", pivot=(side * 3, -7, 0), role="EAR", param=side, cubes=[C(-1.5, -5, -0.5, 3, 5, 1, mat)])
        elif kind == "long":
            s.add(n, anchor="HEAD", pivot=(side * 2.5, -8, 0), rot=(0.2, 0, side * 0.2), role="EAR", param=side,
                  cubes=[C(-1, -4, -0.5, 2, 4, 1, mat)])
        elif kind == "flap":
            s.add(n, anchor="HEAD", pivot=(side * 4, -6, 0), rot=(0, side * -0.3, 0), role="EAR", param=side,
                  cubes=[C(0 if side > 0 else -1, 0, -3, 1, 7, 6, mat)])


def h_tail(s, w=2, n=3, l=6, mat="fur", pitch=-0.9, name="tail", y=10.5, x=0.0, yaw=0.0):
    parent, pivot = None, (x, y, 2)
    for i in range(n):
        ww = max(1, w - (i if w > 2 else 0))
        s.add("%s%d" % (name, i), parent, anchor="BODY" if parent is None else "NONE", pivot=pivot,
              rot=(pitch if i == 0 else 0.3, yaw if i == 0 else 0, 0), role="TAIL", param=i,
              cubes=[C(-ww / 2, -ww / 2, 0, ww, ww, l, mat)])
        parent, pivot = "%s%d" % (name, i), (0, 0, l - 0.5)


def h_horns(s, kind="bull", mat="horn"):
    for side in (-1, 1):
        n = "horn_" + ("l" if side > 0 else "r")
        if kind == "bull":
            s.add(n, anchor="HEAD", pivot=(side * 4, -6.5, -1), rot=(0, 0, -side * 0.25),
                  cubes=[C(0 if side > 0 else -3, -1, -1, 3, 2, 2, mat)])
            s.add(n + "_tip", n, pivot=(side * 3, 0, 0), rot=(0, 0, -side * 1.1), cubes=[C(0 if side > 0 else -3, -1, -0.5, 3, 1, 1, mat)])
        elif kind == "ossicone":
            s.add(n, anchor="HEAD", pivot=(side * 2, -8, 0), cubes=[C(-0.5, -3, -0.5, 1, 3, 1, mat), C(-1, -4, -1, 2, 1, 2, "dark")])
        elif kind == "antler":
            s.add(n, anchor="HEAD", pivot=(side * 3, -8, 0), rot=(0, 0, -side * 0.35),
                  cubes=[C(-0.5, -6, -0.5, 1, 6, 1, mat), C(0 if side > 0 else -3, -5, -0.5, 3, 1, 1, mat),
                         C(0 if side > 0 else -2, -3, -0.5, 2, 1, 1, mat)])
        elif kind == "brow":
            s.add(n, anchor="HEAD", pivot=(side * 2.5, -8, -2), rot=(-0.6, 0, 0), cubes=[C(-0.5, -3, -0.5, 1, 3, 1, mat)])
        elif kind == "dragon":
            s.add(n, anchor="HEAD", pivot=(side * 2.5, -8, 1), rot=(0.7, 0, -side * 0.2), cubes=[C(-0.5, -6, -0.5, 1, 6, 1, mat),
                                                                                                C(-0.5, -4, -2, 1, 1, 2, mat)])


def h_tusks(s, mat="tusk", length=4):
    for side in (-1, 1):
        s.add("tusk_" + ("l" if side > 0 else "r"), anchor="HEAD", pivot=(side * 2.5, -1, -4), rot=(-0.6, 0, 0),
              cubes=[C(-0.5, 0, -length, 1, 1, length, mat)])


def h_wings(s, span=12, depth=7, mat="wing", membrane=False):
    for side in (-1, 1):
        n = "wing_" + ("l" if side > 0 else "r")
        x0 = 0 if side > 0 else -span
        s.add(n, anchor="BODY", pivot=(side * 2, 1, 2.2), rot=(0, -side * 0.3, side * 1.22), role="WING", param=side,
              cubes=[C(x0, 0, 0, span, 1, depth, "membrane" if membrane else mat)])


def h_spider_legs(s, mat="dark", n=3):
    for i in range(n):
        for side in (-1, 1):
            nm = "sleg_%s%d" % ("l" if side > 0 else "r", i)
            x0 = 0 if side > 0 else -9
            s.add(nm, anchor="BODY", pivot=(side * 3, 3 + i * 3, 2), rot=(0, side * (0.4 - i * 0.4), -side * 0.6),
                  role="SPIDER_LEG", param=i * 1.6 + (PI if side > 0 else 0), cubes=[C(x0, -0.5, -0.5, 9, 1, 1, mat)])
            x1 = 0 if side > 0 else -8
            s.add(nm + "_f", nm, pivot=(side * 9, 0, 0), rot=(0, 0, side * 1.8), cubes=[C(x1, -0.5, -0.5, 8, 1, 1, mat)])


# ---------------------------------------------------------------------------------------------------------------
# Species
# ---------------------------------------------------------------------------------------------------------------

SPECIES = {}


def species(sid, palette, pattern="none", overlay="fur", eyes="front", scale=1.0, gait=1.0, seed=1):
    def wrap(fn):
        beast = Shape(sid, palette, pattern, eyes, scale, gait, seed)
        parts = Shape(sid + "_parts", palette, pattern, eyes, 1.0, 1.0, seed + 100)
        parts.overlay_mat = overlay
        fn(beast, parts)
        SPECIES[sid] = (beast, parts)
        return fn
    return wrap


def pal(base, accent, belly=None, **kw):
    p = {"base": rgb(base), "accent": rgb(accent), "belly": rgb(belly) if belly else rgb(base)}
    for k, v in kw.items():
        p[k] = rgb(v) if isinstance(v, int) else v
    return p


# -- Regular Zoans ----------------------------------------------------------------------------------------------

@species("human", pal(0x8A5A3A, 0x5A3A22, 0xC89A6A, eye=0x3A2A1A), pattern="shaggy")
def _(b, h):
    # Monster Point: a hulking, shaggy giant with antlers.
    humanoid(b, height=52, mat="fur")
    b.add("antler_l", "head", pivot=(4, -12, 0), rot=(0, 0, -0.4), cubes=[C(-1, -10, -1, 2, 10, 2, "horn"), C(0, -8, -1, 5, 2, 2, "horn")])
    b.add("antler_r", "head", pivot=(-4, -12, 0), rot=(0, 0, 0.4), cubes=[C(-1, -10, -1, 2, 10, 2, "horn"), C(-5, -8, -1, 5, 2, 2, "horn")])
    b.add("nose", "head", pivot=(0, -6, -6.6), cubes=[C(-1.5, 0, -1, 3, 2, 1, "nose")])
    h_horns(h, "antler")


@species("falcon", pal(0xA87A4A, 0x5A3A22, 0xF0E0C0, beak=0xE8C040, eye=0xF0C040), overlay="feather", eyes="side", seed=3)
def _(b, h):
    bird(b, body=(7, 7, 12), leg=6, head=(5, 5, 5), beak=(2, 2, 2), wing=(10, 10), tip=(11, 8), tail=(6, 1, 8))
    h_wings(h, span=12, depth=7)


@species("jackal", pal(0x6A5A3A, 0x2A2218, 0xE0C870, eye=0xE8B030), seed=4)
def _(b, h):
    quadruped(b, leg=9, legw=2, body=(7, 7, 15), head=(6, 6, 6), snout=(3, 3, 4), ears="big", tail=(2, 3, 6))
    h_ears(h, "big")
    h_tail(h, w=2, n=3, l=5)


@species("mole", pal(0x5A4A4A, 0x2A2222, 0x7A6A6A, nose=0xE8A8A8, eye=0x1A1A1A), seed=5)
def _(b, h):
    quadruped(b, leg=3, legw=3, body=(9, 7, 14), head=(7, 6, 5), snout=(3, 2, 4), ears=None, tail=(1, 1, 3), feet="claw", spread=1)
    for side in (-1, 1):
        b.add("digger_" + ("l" if side > 0 else "r"), "leg_f" + ("l" if side > 0 else "r"), pivot=(0, 2, 0),
              cubes=[C(-2.5, 0, -3, 5, 1, 4, "claw")])
    for side in (-1, 1):
        h.add("claws_" + ("l" if side > 0 else "r"), anchor="LEFT_ARM" if side > 0 else "RIGHT_ARM",
              pivot=(side * 1 + (1 if side > 0 else -1), 10, 0), cubes=[C(-2.5, 0, -2.5, 5, 2, 5, "claw")])
    h.add("nose", anchor="HEAD", pivot=(0, -3, -4), cubes=[C(-1.5, 0, -1.5, 3, 2, 2, "nose")])


@species("bison", pal(0x6A4A2A, 0x2A1A0A, 0x4A3220, horn=0x2A2420, eye=0x1A1210), pattern="shaggy", seed=6)
def _(b, h):
    q = quadruped(b, leg=11, legw=4, body=(14, 14, 24), head=(10, 10, 8), snout=(6, 4, 3), ears="round", tail=(2, 2, 6),
                  feet="hoof", head_drop=5)
    b.add("hump", "body", pivot=(0, -7, -6), cubes=[C(-6, -4, -6, 12, 4, 12, "mane")])
    b.add("beard", "head", pivot=(0, 5, -6), cubes=[C(-3, 0, -2, 6, 4, 4, "mane")])
    for side in (-1, 1):
        n = "horn_" + ("l" if side > 0 else "r")
        b.add(n, "head", pivot=(side * 5, -3, -4), rot=(0, 0, -side * 0.3), cubes=[C(0 if side > 0 else -4, -1, -1, 4, 2, 2, "horn")])
        b.add(n + "t", n, pivot=(side * 4, 0, 0), rot=(0, 0, -side * 1.2), cubes=[C(0 if side > 0 else -3, -1, -0.5, 3, 1, 1, "horn")])
    h_horns(h, "bull")
    h_tail(h, w=1, n=2, l=6)


@species("wolf", pal(0x8A8A98, 0x3A3A48, 0xD0D0D8, eye=0xE8C040), seed=7)
def _(b, h):
    q = quadruped(b, leg=10, legw=3, body=(8, 8, 17), head=(7, 7, 6), snout=(4, 3, 4), ears="pointy", tail=(3, 3, 6))
    b.add("ruff", "body", pivot=(0, 0, -7), cubes=[C(-5, -5, -2, 10, 9, 4, "mane")])
    h_ears(h, "pointy")
    h_tail(h, w=3, n=3, l=5)


@species("giraffe", pal(0xE8C060, 0x8A5A2A, 0xF4E4B4, spot=0x8A5A2A, horn=0x6A4A2A, eye=0x2A1A10), pattern="patches", seed=8)
def _(b, h):
    quadruped(b, leg=22, legw=3, body=(10, 10, 18), neck=(5, 5, 26, -1.05), head=(5, 6, 7), snout=(4, 4, 3), ears="pointy",
              tail=(1, 3, 5), feet="hoof", legs_front=24)
    b.add("ossicone_l", "head", pivot=(1.5, -3, -3), cubes=[C(-0.5, -3, -0.5, 1, 3, 1, "horn"), C(-1, -4, -1, 2, 1, 2, "dark")])
    b.add("ossicone_r", "head", pivot=(-1.5, -3, -3), cubes=[C(-0.5, -3, -0.5, 1, 3, 1, "horn"), C(-1, -4, -1, 2, 1, 2, "dark")])
    b.add("mane", "neck", pivot=(0, -2.5, 0), cubes=[C(-0.5, -1, -26, 1, 1, 26, "dark")])
    h_horns(h, "ossicone")
    h_tail(h, w=1, n=2, l=6)


@species("horse", pal(0xE8E0D0, 0x8A6A4A, 0xF4F0E8, eye=0x2A1A10), seed=9)
def _(b, h):
    quadruped(b, leg=14, legw=3, body=(10, 11, 20), neck=(5, 8, 12, -0.95), head=(5, 6, 10), snout=None, ears="long",
              tail=None, feet="hoof")
    b.add("mane", "neck", pivot=(0, -4, 0), cubes=[C(-1, -2, -12, 2, 2, 12, "mane")])
    parent, pivot = "body", (0, -4, 9.5)
    for i in range(3):
        b.add("tail%d" % i, parent, pivot=pivot, rot=(-0.9 if i == 0 else 0.15, 0, 0), role="TAIL", param=i,
              cubes=[C(-1.5, -1.5, 0, 3, 3, 5, "mane")])
        parent, pivot = "tail%d" % i, (0, 0, 4.5)
    h_ears(h, "long")
    h.add("mane", anchor="HEAD", pivot=(0, -8, 3), rot=(0.25, 0, 0), cubes=[C(-1, 0, 0, 2, 9, 2, "mane")])
    h_tail(h, w=3, n=3, l=5, mat="mane", pitch=-1.1)


@species("anaconda", pal(0x4A7A3A, 0x1A2A1A, 0xC8C070, spot=0x1A2A1A, eye=0xE8C040), pattern="diamonds", overlay="scale", seed=10)
def _(b, h):
    serpent(b, n=11, seg=(9, 8, 9), head=(9, 7, 11), lift=(-0.35,))
    h_tail(h, w=4, n=5, l=7, mat="scale", pitch=-1.25, y=10)


@species("king_cobra", pal(0xA88A3A, 0x2A2A1A, 0xE8D8A0, eye=0xE8E0A0), pattern="bands", overlay="scale", seed=11)
def _(b, h):
    serpent(b, n=11, seg=(5, 5, 7), head=(5, 4, 7), lift=(-1.25, -0.25))
    b.add("hood", "seg0", pivot=(0, 0, 2.5), rot=(1.25, 0, 0), cubes=[C(-6, -3, 0, 12, 11, 1, "scale")])
    h.add("hood", anchor="HEAD", pivot=(0, -2, 4.3), cubes=[C(-6, -3, 0, 12, 9, 1, "scale")])
    h_tail(h, w=3, n=5, l=6, mat="scale", pitch=-1.25, y=10)


@species("axolotl", pal(0xF0A8C8, 0x8A3A6A, 0xF8D0E0, eye=0x2A1A2A), overlay="skin", seed=12)
def _(b, h):
    q = quadruped(b, leg=3, legw=2, body=(8, 5, 16), head=(9, 5, 6), snout=None, ears=None, tail=None, body_mat="skin", spread=1.5)
    for side in (-1, 1):
        for j, y in enumerate((-2, 0, 2)):
            n = "gill_%s%d" % ("l" if side > 0 else "r", j)
            b.add(n, "head", pivot=(side * 4.5, y - 1, -2), rot=(0, 0, side * (-0.6 + j * 0.6)), role="FIN", param=j + (3 if side > 0 else 0),
                  cubes=[C(0 if side > 0 else -4, -0.5, -0.5, 4, 1, 1, "accent")])
    b.add("tail0", "body", pivot=(0, -1, 7.5), rot=(0.05, 0, 0), role="TAIL", param=0, cubes=[C(-1, -3, 0, 2, 5, 9, "skin")])
    b.add("tail1", "tail0", pivot=(0, 0, 8.5), role="TAIL", param=1, cubes=[C(-0.5, -2, 0, 1, 4, 6, "accent")])
    for side in (-1, 1):
        for j, y in enumerate((-6, -4, -2)):
            h.add("gill_%s%d" % ("l" if side > 0 else "r", j), anchor="HEAD", pivot=(side * 4, y, -1),
                  rot=(0, 0, side * (-0.5 + j * 0.5)), role="FIN", param=j + (3 if side > 0 else 0),
                  cubes=[C(0 if side > 0 else -3, -0.5, -0.5, 3, 1, 1, "accent")])
    h_tail(h, w=2, n=2, l=6, mat="skin")


@species("albatross", pal(0xF0F0F0, 0x3A3A3A, 0xFFFFFF, beak=0xF0D080, eye=0x1A1A1A), overlay="feather", eyes="side", seed=13)
def _(b, h):
    bird(b, body=(7, 7, 12), leg=5, head=(5, 5, 6), beak=(2, 2, 4), wing=(13, 8), tip=(14, 6), tail=(6, 1, 5))
    h_wings(h, span=14, depth=6)


@species("tanuki", pal(0x8A6A4A, 0x2A1A0A, 0xC8A880, eye=0x1A1210), seed=14)
def _(b, h):
    quadruped(b, leg=6, legw=3, body=(9, 8, 12), head=(7, 6, 5), snout=(3, 2, 2), ears="round", tail=(4, 2, 6))
    b.add("mask", "head", pivot=(0, -1, -5.01), cubes=[C(-3.5, 0, 0, 7, 2, 0, "dark")])
    b.add("leaf", "head", pivot=(0, -3, -2), rot=(0.4, 0, 0.3), cubes=[C(-2, -3, 0, 4, 3, 0, "accent")])
    h_ears(h, "round")
    h.add("leaf", anchor="HEAD", pivot=(0, -8, -1), rot=(0.3, 0, 0.3), cubes=[C(-2, -3, 0, 4, 3, 0, "accent")])
    h_tail(h, w=4, n=2, l=6)


@species("elephant", pal(0x9A9AA8, 0x4A4A58, 0xB0B0BA, tusk=0xF0E8D8, eye=0x1A1A1A), overlay="skin", seed=15)
def _(b, h):
    quadruped(b, leg=16, legw=6, body=(18, 17, 28), head=(12, 12, 9), snout=None, ears="none", tail=(1, 2, 7), feet="hoof",
              body_mat="skin", head_drop=-3)
    for side in (-1, 1):
        b.add("ear_" + ("l" if side > 0 else "r"), "head", pivot=(side * 6, -5, -3), rot=(0, side * -0.35, 0), role="EAR", param=side,
              cubes=[C(0 if side > 0 else -1, 0, -1, 1, 11, 9, "skin")])
        b.add("tusk_" + ("l" if side > 0 else "r"), "head", pivot=(side * 4, 4, -9), rot=(0.9, 0, 0),
              cubes=[C(-1, 0, -1, 2, 7, 2, "tusk")])
    parent, pivot = "head", (0, 2, -9)
    for i in range(4):
        b.add("trunk%d" % i, parent, pivot=pivot, rot=(0.5 if i == 0 else 0.15, 0, 0), role="TAIL", param=i + 2,
              cubes=[C(-1.5 + i * 0.25, 0, -1.5 + i * 0.25, 3 - i * 0.5 if False else max(1, 3 - (i // 2)), 5, max(1, 3 - (i // 2)), "skin")])
        parent, pivot = "trunk%d" % i, (0, 4.5, 0)
    h_ears(h, "flap", "skin")
    h_tusks(h)


@species("turtle", pal(0x7A9A5A, 0x3A5A2A, 0xD8C890, shell=0x5A6A3A, eye=0x1A1A1A), overlay="scale", seed=36, gait=0.6)
def _(b, h):
    quadruped(b, leg=5, legw=4, body=(13, 6, 15), head=(5, 5, 6), snout=None, ears="none", tail=(2, 1, 3), body_mat="skin",
              spread=1.5)
    b.add("shell", "body", pivot=(0, -3, 0), cubes=[C(-8, -4, -9, 16, 5, 18, "shell"), C(-6, -6, -7, 12, 2, 14, "shell")])
    b.add("plastron", "body", pivot=(0, 3, 0), cubes=[C(-6.5, 0, -7.5, 13, 1, 15, "belly")])
    h.add("shell", anchor="BODY", pivot=(0, 0, 2), cubes=[C(-5, -0.5, 0, 10, 12, 3, "shell"), C(-4, 1, 3, 8, 9, 1, "shell")])
    h.add("plastron", anchor="BODY", pivot=(0, 1, -2.2), cubes=[C(-3.5, 0, -0.5, 7, 9, 1, "belly")])


@species("dachshund", pal(0xA86A3A, 0x3A2A1A, 0xC89A6A, eye=0x1A1A1A), seed=37, gait=1.4)
def _(b, h):
    quadruped(b, leg=4, legw=3, body=(7, 7, 20), head=(6, 6, 6), snout=(3, 3, 5), ears="none", tail=(2, 2, 6))
    for side in (-1, 1):
        b.add("ear_" + ("l" if side > 0 else "r"), "head", pivot=(side * 3, -3, -3), rot=(0, 0, side * 0.25), role="EAR", param=side,
              cubes=[C(0 if side > 0 else -1, 0, -1.5, 1, 5, 3, "dark")])
    h.add("ear_l", anchor="HEAD", pivot=(4, -7, 0), rot=(0, 0, 0.25), role="EAR", param=1, cubes=[C(0, 0, -1.5, 1, 5, 3, "dark")])
    h.add("ear_r", anchor="HEAD", pivot=(-4, -7, 0), rot=(0, 0, -0.25), role="EAR", param=-1, cubes=[C(-1, 0, -1.5, 1, 5, 3, "dark")])
    h_tail(h, w=2, n=3, l=4)


# -- Ancient Zoans ----------------------------------------------------------------------------------------------

@species("sabertooth", pal(0xE8A040, 0x3A2A1A, 0xF4DCA8, spot=0x5A3A1A, eye=0xE8D040), pattern="stripes", seed=16)
def _(b, h):
    quadruped(b, leg=11, legw=4, body=(11, 11, 21), head=(9, 8, 7), snout=(5, 3, 3), ears="round", tail=(2, 2, 5))
    for side in (-1, 1):
        b.add("saber_" + ("l" if side > 0 else "r"), "snout", pivot=(side * 1.6, 2.5, -2), cubes=[C(-0.5, 0, -0.5, 1, 5, 1, "tooth")])
        h.add("saber_" + ("l" if side > 0 else "r"), anchor="HEAD", pivot=(side * 1.5, -1, -4), cubes=[C(-0.5, 0, -0.5, 1, 3, 1, "tooth")])
    h_ears(h, "round")
    h_tail(h, w=2, n=2, l=5)


@species("mammoth", pal(0x6A4A3A, 0x3A2A1A, 0x5A3A2A, tusk=0xE8E0C8, eye=0x1A1A1A), pattern="shaggy", seed=17)
def _(b, h):
    quadruped(b, leg=20, legw=8, body=(24, 24, 34), head=(16, 16, 11), snout=None, ears="none", tail=(2, 2, 8), feet="hoof", head_drop=-6)
    b.add("hump", "body", pivot=(0, -12, -8), cubes=[C(-9, -6, -7, 18, 6, 14, "mane")])
    b.add("skirt", "body", pivot=(0, 12, 0), cubes=[C(-12.5, -4, -17.5, 25, 6, 35, "mane")])
    for side in (-1, 1):
        n = "tusk_" + ("l" if side > 0 else "r")
        b.add(n, "head", pivot=(side * 5, 6, -10), rot=(1.2, side * 0.3, 0), cubes=[C(-1.5, 0, -1.5, 3, 12, 3, "tusk")])
        b.add(n + "t", n, pivot=(0, 11.5, 0), rot=(-1.6, 0, 0), cubes=[C(-1, 0, -1, 2, 9, 2, "tusk")])
        b.add("ear_" + ("l" if side > 0 else "r"), "head", pivot=(side * 8, -5, -3), role="EAR", param=side,
              cubes=[C(0 if side > 0 else -1, 0, -1, 1, 7, 6, "fur")])
    parent, pivot = "head", (0, 4, -11)
    for i in range(5):
        b.add("trunk%d" % i, parent, pivot=pivot, rot=(0.3 if i == 0 else 0.15, 0, 0), role="TAIL", param=i + 2,
              cubes=[C(-2 + i * 0.25, 0, -2 + i * 0.25, max(2, 4 - i // 2), 6, max(2, 4 - i // 2), "fur")])
        parent, pivot = "trunk%d" % i, (0, 5.5, 0)
    h_tusks(h, length=6)
    h_ears(h, "flap", "fur")


@species("allosaurus", pal(0xA84A3A, 0x3A1A1A, 0xE0B090, eye=0xF0D040, horn=0x5A2A20), pattern="mottle", overlay="scale", seed=18)
def _(b, h):
    theropod(b, leg=18, legw=6, body=(14, 15, 26), neck=(8, 8, 9, -0.9), head=(10, 10, 17), tail=(10, 5, 9))
    for side in (-1, 1):
        b.add("brow_" + ("l" if side > 0 else "r"), "head", pivot=(side * 3, -5, -6), rot=(-0.4, 0, 0), cubes=[C(-1, -3, -1, 2, 3, 2, "horn")])
    h_horns(h, "brow")
    h_tail(h, w=6, n=4, l=6, mat="scale", pitch=-0.5)


@species("spinosaurus", pal(0x4A8A6A, 0x1A3A2A, 0xC8D8A8, eye=0xF0D040, membrane=0xE8C060), pattern="mottle", overlay="scale", seed=19)
def _(b, h):
    theropod(b, leg=18, legw=6, body=(14, 15, 28), neck=(7, 7, 10, -0.8), head=(8, 8, 20), tail=(10, 5, 10))
    b.add("sail", "body", pivot=(0, -7.5, 0), cubes=[C(-0.5, -18, -12, 1, 18, 24, "membrane")])
    h.add("sail", anchor="BODY", pivot=(0, 1, 2), cubes=[C(-0.5, -4, 0, 1, 12, 4, "membrane")])
    h_tail(h, w=6, n=4, l=6, mat="scale", pitch=-0.5)


@species("pteranodon", pal(0x3A3A4A, 0xE84A2A, 0x6A6A7A, beak=0xC8B890, membrane=0x8A5A4A, eye=0xF0D040), overlay="skin", eyes="side", seed=20, scale=1.4)
def _(b, h):
    bird(b, body=(6, 6, 11), leg=5, head=(4, 4, 5), beak=(2, 2, 9), wing=(13, 10), tip=(15, 7), tail=None, crest=(1, 7, 2), body_mat="skin")
    for side in (-1, 1):
        for n in ("wing_", "tip_"):
            p = b.parts[b.index(n + ("l" if side > 0 else "r"))]
            for c in p.cubes:
                c.mat = "membrane"
    h_wings(h, span=15, depth=8, membrane=True)
    h.add("crest", anchor="HEAD", pivot=(0, -8, 2), rot=(-0.6, 0, 0), cubes=[C(-0.5, -6, 0, 1, 6, 3, "accent")])


@species("brachiosaurus", pal(0x6A8A9A, 0x2A3A4A, 0xA8B8C0, eye=0x1A1A1A), pattern="mottle", overlay="skin", seed=21, gait=0.7)
def _(b, h):
    quadruped(b, leg=18, legw=7, body=(18, 18, 30), neck=(6, 6, 38, -1.15), head=(6, 6, 8), snout=None, ears=None, tail=(8, 5, 9),
              feet="hoof", body_mat="skin", legs_front=24)
    h_tail(h, w=5, n=5, l=6, mat="skin", pitch=-0.8)


@species("triceratops", pal(0x8A6A3A, 0x4A3A1A, 0xC8B080, horn=0xE8D0A0, eye=0x1A1A1A), pattern="mottle", overlay="skin", seed=22)
def _(b, h):
    quadruped(b, leg=11, legw=6, body=(18, 16, 30), head=(12, 10, 12), snout=(6, 6, 4), ears=None, tail=(7, 4, 8), feet="hoof",
              body_mat="skin", head_drop=3)
    b.add("frill", "head", pivot=(0, -4, -1), rot=(-0.5, 0, 0), cubes=[C(-11, -12, 0, 22, 14, 2, "accent")])
    for side in (-1, 1):
        b.add("horn_" + ("l" if side > 0 else "r"), "head", pivot=(side * 3.5, -4, -8), rot=(1.15, 0, 0), cubes=[C(-1, -12, -1, 2, 12, 2, "horn")])
    b.add("nose_horn", "snout", pivot=(0, 0, -3), rot=(0.5, 0, 0), cubes=[C(-1, -5, -1, 2, 5, 2, "horn")])
    b.add("beak", "snout", pivot=(0, 3, -4), cubes=[C(-2, 0, -2, 4, 3, 2, "dark")])
    h.add("frill", anchor="HEAD", pivot=(0, -5, 4.2), rot=(-0.4, 0, 0), cubes=[C(-6, -6, 0, 12, 7, 1, "accent")])
    for side in (-1, 1):
        h.add("horn_" + ("l" if side > 0 else "r"), anchor="HEAD", pivot=(side * 2.5, -7, -3), rot=(1.0, 0, 0), cubes=[C(-0.5, -5, -0.5, 1, 5, 1, "horn")])
    h_tail(h, w=5, n=3, l=6, mat="skin", pitch=-0.6)


@species("pachycephalosaurus", pal(0x5A8AC8, 0x2A3A6A, 0xF0E0C0, horn=0xE8E0C8, eye=0xF0E040), pattern="speckle", overlay="skin", seed=23)
def _(b, h):
    theropod(b, leg=14, legw=5, body=(11, 12, 20), neck=(6, 6, 7, -1.0), head=(9, 8, 9), tail=(8, 4, 8), mat="skin")
    b.add("dome", "head", pivot=(0, -4, -4.5), cubes=[C(-5, -5, -5, 10, 5, 10, "accent")])
    for i, (x, z) in enumerate(((-4, -8), (4, -8), (-5, -1), (5, -1))):
        b.add("knob%d" % i, "head", pivot=(x, -4, z), cubes=[C(-0.5, -2, -0.5, 1, 2, 1, "horn")])
    h.add("dome", anchor="HEAD", pivot=(0, -8, 0), cubes=[C(-4.5, -3, -4.5, 9, 3, 9, "accent")])
    h_tail(h, w=5, n=3, l=6, mat="skin", pitch=-0.6)


@species("spider", pal(0x3A2A3A, 0xE84A8A, 0x5A3A5A, eye=0xE84A8A, dark=0x1A1218), pattern="speckle", seed=24, gait=1.4)
def _(b, h):
    spider(b, body=(12, 9, 13), abdomen=(20, 16, 22), leg=(16, 19), legw=2)
    h_spider_legs(h)


# -- Mythical Zoans ---------------------------------------------------------------------------------------------

@species("phoenix", pal(0x3A8AE8, 0xF0E040, 0x6AB8F8, flame_lo=0x2A6AE8, flame_hi=0xF0F080, beak=0xF0E040, eye=0xF0F0F0),
         overlay="flame", eyes="side", seed=25, scale=1.8)
def _(b, h):
    bird(b, body=(7, 7, 12), leg=6, head=(5, 5, 5), beak=(2, 2, 2), wing=(12, 10), tip=(13, 8), tail=None)
    for n in ("wing_l", "wing_r", "tip_l", "tip_r"):
        for c in b.parts[b.index(n)].cubes:
            c.mat = "flame"
    for i, yaw in enumerate((-0.25, 0, 0.25)):
        b.add("plume%d" % i, "body", pivot=(0, -2, 5.5), rot=(-0.3, yaw, 0), role="TAIL", param=i,
              cubes=[C(-1, 0, 0, 2, 1, 14, "flame")])
    b.add("crest", "head", pivot=(0, -5, -2), rot=(-0.6, 0, 0), cubes=[C(-0.5, -4, 0, 1, 4, 3, "flame")])
    h_wings(h, span=14, depth=8, mat="flame")
    for c in [c for p in h.parts for c in p.cubes]:
        c.mat = "flame"


@species("azure_dragon", pal(0x3A6AC8, 0xE8E0A0, 0xE8E0A0, horn=0xE8E0A0, eye=0xF0D040), overlay="scale", seed=26, gait=0.8)
def _(b, h):
    serpent(b, n=14, seg=(9, 9, 10), taper=0.6, head=(10, 9, 14), lift=(-0.3, 0.1))
    for side in (-1, 1):
        n = "l" if side > 0 else "r"
        b.add("horn_" + n, "head", pivot=(side * 3, -4.5, -4), rot=(0.9, 0, -side * 0.2), cubes=[C(-1, -9, -1, 2, 9, 2, "horn")])
        b.add("whisker_" + n, "head", pivot=(side * 4.5, 1, -13), rot=(0, side * 0.8, 0), role="FIN", param=side,
              cubes=[C(0 if side > 0 else -9, -0.5, -0.5, 9, 1, 1, "accent")])
        for seg, z in (("seg2", 3), ("seg8", 3)):
            b.add("claw_%s_%s" % (seg, n), seg, pivot=(side * 4, 3, z), rot=(0.3, 0, -side * 0.6), role="LEG", param=0 if side > 0 else PI,
                  cubes=[C(-1, 0, -1, 2, 6, 2, "scale"), C(-1.5, 5.5, -2.5, 3, 1, 3, "claw")])
    b.add("mane", "seg0", pivot=(0, -4.5, 1), cubes=[C(-1, -2, 0, 2, 2, 16, "accent")])
    h_horns(h, "dragon")
    h_tail(h, w=4, n=5, l=7, mat="scale", pitch=-1.1)


@species("yamata", pal(0x5A3A6A, 0xE8C040, 0x8A6A9A, eye=0xE8C040), pattern="bands", overlay="scale", seed=27)
def _(b, h):
    by = 24 - 9
    b.add("body", pivot=(0, by, 0), role="BODY", cubes=[C(-9, -8, -10, 18, 16, 22, "scale")])
    for side in (-1, 1):
        for j, z in enumerate((-6, 6)):
            b.add("leg_%s%d" % ("l" if side > 0 else "r", j), pivot=(side * 7, 24 - 4, z), role="LEG_FRONT" if j == 0 else "LEG_BACK",
                  param=(0 if side > 0 else PI) + j * PI, cubes=[C(-2, 0, -2, 4, 4, 4, "scale")])
    for i in range(8):
        yaw = (i - 3.5) * 0.28
        root = "neck%d_0" % i
        b.add(root, "body", pivot=((i - 3.5) * 2.2, -6, -9), rot=(-0.6 - 0.25 * (i % 2), yaw, 0), role="SEGMENT", param=i * 0.7,
              cubes=[C(-2, -2, -9, 4, 4, 9, "scale")])
        b.add("neck%d_1" % i, root, pivot=(0, 0, -8.5), rot=(0.3, 0, 0), role="SEGMENT", param=i * 0.7 + 1,
              cubes=[C(-1.5, -1.5, -8, 3, 3, 8, "scale")])
        b.add("head%d" % i, "neck%d_1" % i, pivot=(0, 0, -7.5), rot=(0.3, 0, 0), role="FIN", param=i,
              cubes=[C(-2, -2, -5, 4, 3, 5, "head")])
    for i in range(8):
        yaw = (i - 3.5) * 0.18
        b.add("tail%d" % i, "body", pivot=((i - 3.5) * 2, 2, 11.5), rot=(-0.5, yaw, 0), role="TAIL", param=i,
              cubes=[C(-1, -1, 0, 2, 2, 12, "scale")])
    for i, (x, y) in enumerate(((-3, 4), (3, 4), (-3, 8), (3, 8))):
        root = "hneck%d" % i
        h.add(root, anchor="BODY", pivot=(x, y - 3, 2), rot=(0.4, x * 0.12, 0), role="SEGMENT", param=i,
              cubes=[C(-1, -1, 0, 2, 2, 7, "scale")])
        h.add("hhead%d" % i, root, pivot=(0, 0, 7), rot=(-1.4, 0, 0), role="FIN", param=i, cubes=[C(-1.5, -1.5, -3, 3, 2, 4, "head")])


@species("nine_tailed_fox", pal(0xE8B060, 0xFFFFFF, 0xF8F0E0, eye=0xE84A2A), seed=28)
def _(b, h):
    quadruped(b, leg=10, legw=3, body=(8, 8, 16), head=(7, 6, 6), snout=(3, 3, 4), ears="pointy", tail=None)
    for i in range(9):
        yaw = (i - 4) * 0.22
        root = "tail%d" % i
        b.add(root, "body", pivot=(0, -2.5, 7.5), rot=(-0.3 + abs(i - 4) * 0.05, yaw, 0), role="TAIL", param=i,
              cubes=[C(-1.5, -1.5, 0, 3, 3, 8, "fur")])
        b.add(root + "t", root, pivot=(0, 0, 7.5), rot=(0.5, 0, 0), role="TAIL", param=i + 1, cubes=[C(-1, -1, 0, 2, 2, 5, "accent")])
    h_ears(h, "pointy")
    for i in range(9):
        h_tail(h, w=2, n=2, l=6, name="t%d_" % i, pitch=-0.9 + abs(i - 4) * 0.12, yaw=(i - 4) * 0.3, y=10.5)


@species("okuchi_wolf", pal(0xF0F4FF, 0x6AA8E8, 0xFFFFFF, eye=0x6AA8E8), pattern="none", seed=29)
def _(b, h):
    quadruped(b, leg=15, legw=4, body=(12, 12, 24), head=(9, 9, 8), snout=(5, 4, 5), ears="pointy", tail=(4, 3, 8))
    b.add("ruff", "body", pivot=(0, 0, -10), cubes=[C(-7, -7, -2, 14, 13, 5, "accent")])
    b.add("mark", "head", pivot=(0, -4.6, -6), cubes=[C(-1, 0, -1, 2, 0, 4, "accent")])
    h_ears(h, "pointy")
    h_tail(h, w=4, n=3, l=6)
    h_horns(h, "brow", "ice")


@species("buddha", pal(0xF0C040, 0xFFF0A0, 0xF8D870, eye=0x5A3A10), overlay="gold", seed=30, gait=0.6)
def _(b, h):
    humanoid(b, height=64, mat="gold", head_mat="head")
    for p in b.parts:
        for c in p.cubes:
            if c.mat == "head":
                c.mat = "head"
    b.palette["base"] = (232, 182, 50)
    b.add("ushnisha", "head", pivot=(0, -16, 0), cubes=[C(-4, -4, -4, 8, 4, 8, "gold")])
    b.add("halo", "head", pivot=(0, -8, 9), cubes=[C(-12, -12, 0, 24, 24, 1, "glow")])
    b.palette["glow"] = (255, 236, 150)
    h.add("halo", anchor="HEAD", pivot=(0, -5, 5.5), cubes=[C(-5, -5, 0, 10, 10, 1, "glow")])
    h.palette["glow"] = (255, 236, 150)


@species("gyuki", pal(0x2A2A2A, 0xE8E8E8, 0x3A3A3A, horn=0xE8E8E8, eye=0xE84A4A, dark=0x1A1A1A), seed=31, gait=1.3)
def _(b, h):
    spider(b, body=(14, 10, 14), abdomen=(16, 12, 16), leg=(18, 20), legw=3, ox_head=True)
    b.add("torso", "body", pivot=(0, -4, -5), rot=(-0.3, 0, 0), cubes=[C(-6, -14, -4, 12, 14, 8, "fur")])
    b.add("head", "torso", pivot=(0, -14, -1), role="HEAD", param=0.6, cubes=[C(-4, -8, -6, 8, 8, 8, "head"), C(-2.5, -3, -8, 5, 3, 2, "fur")])
    for side in (-1, 1):
        n = "horn_" + ("l" if side > 0 else "r")
        b.add(n, "head", pivot=(side * 4, -6, -2), rot=(0, 0, -side * 0.4), cubes=[C(0 if side > 0 else -6, -1, -1, 6, 2, 2, "horn")])
        b.add(n + "t", n, pivot=(side * 6, 0, 0), rot=(0, 0, -side * 1.3), cubes=[C(0 if side > 0 else -5, -1, -0.5, 5, 1, 1, "horn")])
    h_horns(h, "bull")
    h_spider_legs(h, n=4)


@species("itsumade", pal(0x4A2A2A, 0xE84A2A, 0x6A3A3A, beak=0x2A1A1A, eye=0xF0D040, flame_lo=0xE84A2A, flame_hi=0xF0D040),
         overlay="feather", eyes="side", seed=32, scale=2.0)
def _(b, h):
    bird(b, body=(8, 8, 13), leg=6, head=(5, 5, 6), beak=(2, 2, 4), wing=(13, 11), tip=(14, 8), tail=None)
    for i, yaw in enumerate((-0.15, 0.15)):
        b.add("tail%d" % i, "body", pivot=(0, -2, 6), rot=(-0.2, yaw, 0), role="TAIL", param=i, cubes=[C(-1.5, 0, 0, 3, 1, 16, "flame")])
    b.add("crest", "head", pivot=(0, -5, -2), rot=(-0.7, 0, 0), cubes=[C(-0.5, -5, 0, 1, 5, 3, "flame")])
    h_wings(h, span=14, depth=8)


@species("bakotsu", pal(0xE8F0F8, 0x3A3A4A, 0xE8F0F8, bone=0xE8ECF0, eye=0x8AD0F8), overlay="bone", seed=33)
def _(b, h):
    quadruped(b, leg=15, legw=2, body=(8, 10, 20), neck=(4, 6, 12, -0.95), head=(5, 6, 10), snout=None, ears="long", tail=None,
              feet="hoof", body_mat="bone")
    for p in b.parts:
        for c in p.cubes:
            if c.mat in ("fur", "head"):
                c.mat = "bone" if c.mat == "fur" else "head"
    b.palette["base"] = (232, 236, 240)
    b.add("mane", "neck", pivot=(0, -3.5, 0), cubes=[C(-1, -2, -12, 2, 2, 12, "ice")])
    parent, pivot = "body", (0, -4, 9.5)
    for i in range(4):
        b.add("tail%d" % i, parent, pivot=pivot, rot=(-0.9 if i == 0 else 0.1, 0, 0), role="TAIL", param=i,
              cubes=[C(-0.5, -0.5, 0, 1, 1, 4, "bone")])
        parent, pivot = "tail%d" % i, (0, 0, 3.5)
    h_ears(h, "long", "bone")
    h_tail(h, w=1, n=4, l=4, mat="bone", pitch=-1.1)


@species("fengxi", pal(0x5A4A3A, 0xE8E0C8, 0x7A6A5A, tusk=0xC8C8D0, eye=0xE84A2A), pattern="shaggy", seed=34)
def _(b, h):
    quadruped(b, leg=12, legw=6, body=(18, 18, 28), head=(13, 12, 10), snout=(8, 6, 5), ears="round", tail=(1, 3, 4), feet="hoof")
    b.add("ridge", "body", pivot=(0, -9, 0), cubes=[C(-1.5, -4, -13, 3, 4, 26, "mane")])
    for side in (-1, 1):
        n = "tusk_" + ("l" if side > 0 else "r")
        b.add(n, "snout", pivot=(side * 3.5, 4, -3), rot=(-0.4, 0, -side * 0.3), cubes=[C(-1, -8, -1, 2, 8, 2, "tusk")])
    h_tusks(h, length=4)
    h_ears(h, "round")
    h_tail(h, w=1, n=2, l=4)


@species("sandworm", pal(0xC8A87A, 0x6A4A2A, 0xD8C090, eye=0x2A1A0A), pattern="bands", overlay="skin", seed=35, gait=0.7)
def _(b, h):
    serpent(b, n=10, seg=(14, 14, 11), taper=0.5, head=(16, 16, 8), jaw=False, mat="skin", head_mat="skin")
    for i in range(4):
        a = i * PI / 2
        b.add("mandible%d" % i, "head", pivot=(math.cos(a) * 6, math.sin(a) * 6, -8), rot=(math.sin(a) * 0.5, -math.cos(a) * 0.5, 0),
              role="FIN", param=i, cubes=[C(-1.5, -1.5, -6, 3, 3, 6, "tooth")])
    b.add("maw", "head", pivot=(0, 0, -8.01), cubes=[C(-5, -5, 0, 10, 10, 0, "mouth")])
    h_tail(h, w=6, n=5, l=7, mat="skin", pitch=-1.2, y=10)


# ---------------------------------------------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------------------------------------------

# ---------------------------------------------------------------------------------------------------------------
# Race features: parts every member of a race wears on the player model (no beast body)
# ---------------------------------------------------------------------------------------------------------------

RACES = {}


def race_parts(rid, palette, seed=1):
    def wrap(fn):
        parts = Shape("race_" + rid + "_parts", palette, "none", "front", 1.0, 1.0, seed)
        parts.overlay_mat = "fur"
        fn(parts)
        RACES[rid] = parts
        return fn
    return wrap


@race_parts("mink", pal(0xD8B888, 0x8A6A4A, 0xF0E4C8), seed=901)
def _(h):
    # Furry ears on top of the head and a short, bushy tail.
    h_ears(h, "pointy")
    h_tail(h, w=3, n=2, l=4, pitch=-1.0)


@race_parts("fishman", pal(0x3A7AA8, 0x1A4A6A, 0x8AC8E8, scale=0x3A7AA8), seed=902)
def _(h):
    # A dorsal fin down the back and a fin on each forearm.
    h.add("dorsal", anchor="BODY", pivot=(0, 1, 2), rot=(0.25, 0, 0), cubes=[C(-0.5, 0, 0, 1, 8, 4, "scale")])
    h.add("fin_r", anchor="RIGHT_ARM", pivot=(-3, 5, 0), rot=(0, 0, 0.3), cubes=[C(-0.5, 0, 0, 1, 4, 3, "scale")])
    h.add("fin_l", anchor="LEFT_ARM", pivot=(3, 5, 0), rot=(0, 0, -0.3), cubes=[C(-0.5, 0, 0, 1, 4, 3, "scale")])


@race_parts("skypiean", pal(0xF4F4F8, 0xC8CCD8, 0xFFFFFF, feather=0xF4F4F8), seed=903)
def _(h):
    # Small white wings on the back.
    h_wings(h, span=7, depth=5, mat="feather")


BOSSES = {}


def boss_parts(bid, palette, seed=1):
    def wrap(fn):
        parts = Shape("boss_" + bid + "_parts", palette, "none", "front", 1.0, 1.0, seed)
        fn(parts)
        BOSSES[bid] = parts
        return fn
    return wrap


@boss_parts("morgan", pal(0x9AA0AA, 0x5A606A, 0xC8CCD4, metal=0x9AA0AA), seed=951)
def _(h):
    # Captain Morgan's iron jaw: a riveted steel plate over his chin and cheeks with a jutting edge, and the steel cuff
    # his axe-hand is bolted into.
    h.add("iron_jaw", anchor="HEAD", cubes=[C(-4.5, -3.5, -5, 9, 4, 2, "metal"), C(-5, -3.5, -4, 1, 4, 6, "metal"),
                                            C(4, -3.5, -4, 1, 4, 6, "metal"), C(-3, 0.5, -5.5, 6, 1, 2, "metal")])
    h.add("axe_cuff", anchor="RIGHT_ARM", cubes=[C(-3.5, 6, -2.5, 5, 4, 5, "metal")])


@boss_parts("arlong", pal(0x86A2B8, 0x46647A, 0xC8D8E4, tooth=0xF4F2EA), seed=952)
def _(h):
    # Arlong the Saw: a sawshark's long serrated nose, a shark's grin, a tall dorsal fin and fins on his forearms.
    teeth = [C(side, -4.5, -10.5 + i * 1.6, 1, 1, 1, "tooth") for i in range(4) for side in (-2.5, 1.5)]
    h.add("saw_nose", anchor="HEAD", cubes=[C(-1.5, -4.75, -11, 3, 2, 7, "skin")] + teeth)
    h.add("grin", anchor="HEAD", cubes=[C(-3, -2, -4.6, 6, 1, 1, "tooth")])
    h.add("dorsal", anchor="BODY", pivot=(0, 1, 2), rot=(0.35, 0, 0), role="FIN", param=0,
          cubes=[C(-0.5, 0, 0, 1, 10, 5, "skin")])
    h.add("fin_r", anchor="RIGHT_ARM", pivot=(-3, 5, 0), rot=(0, 0, 0.3), role="FIN", param=1,
          cubes=[C(-0.5, 0, 0, 1, 5, 3, "skin")])
    h.add("fin_l", anchor="LEFT_ARM", pivot=(3, 5, 0), rot=(0, 0, -0.3), role="FIN", param=2,
          cubes=[C(-0.5, 0, 0, 1, 5, 3, "skin")])


BOSS_BODIES = {}


def boss_body(bid, palette, pattern="none", seed=1):
    def wrap(fn):
        body = Shape("boss_" + bid + "_body", palette, pattern, "side", 1.0, 1.0, seed)
        fn(body)
        BOSS_BODIES[bid] = body
        return fn
    return wrap


@boss_body("sea_king", pal(0x5A4A8A, 0x2A2050, 0xD8C8A0, spot=0x2A2050, eye=0xF0D040, tooth=0xF4F2EA), pattern="bands", seed=960)
def _(b):
    # A Sea King: a great banded sea serpent with a crested head, fangs, whiskers and fins along its back.
    serpent(b, n=10, seg=(10, 9, 11), taper=0.55, head=(12, 9, 15), lift=(-0.5, 0.25))
    for i in range(1, 8, 2):
        b.add("fin%d" % i, "seg%d" % i, pivot=(0, -4.5, 2), rot=(-0.45, 0, 0), cubes=[C(-0.5, -6, 0, 1, 6, 7, "accent")])
    b.add("fang_l", "head", pivot=(3.5, 2, -13), cubes=[C(-0.5, 0, 0, 1, 3, 1, "tooth")])
    b.add("fang_r", "head", pivot=(-3.5, 2, -13), cubes=[C(-0.5, 0, 0, 1, 3, 1, "tooth")])
    b.add("whisker_l", "head", pivot=(6, 0, -12), rot=(0, -0.6, 0.2), cubes=[C(0, -0.5, -0.5, 8, 1, 1, "accent")])
    b.add("whisker_r", "head", pivot=(-6, 0, -12), rot=(0, 0.6, -0.2), cubes=[C(-8, -0.5, -0.5, 8, 1, 1, "accent")])
    b.add("crest", "head", pivot=(0, -4.5, -6), rot=(0.3, 0, 0), cubes=[C(-0.5, -5, 0, 1, 5, 8, "accent")])


def write_java(entries):
    lines = [
        "package com.nerodev.grandline.client.model;",
        "",
        "import java.util.Map;",
        "",
        "/**",
        " * GENERATED by tools/gen_zoans.py: do not edit by hand. Geometry of every Zoan body as compact strings that",
        " * {@link BeastShape#parse} turns into models (kept as data so the class stays small).",
        " */",
        "final class ZoanShapeData {",
        "    private ZoanShapeData() {}",
        "",
        "    static Map<String, String> all() {",
        "        return Map.ofEntries(",
    ]
    for i, (key, data) in enumerate(entries):
        chunks = [data[j:j + 100] for j in range(0, len(data), 100)]
        body = "\n                        + ".join('"%s"' % c for c in chunks)
        lines.append('                Map.entry("%s",\n                        %s)%s' % (key, body, "," if i < len(entries) - 1 else ");"))
    lines += ["    }", "}", ""]
    with open(JAVA, "w") as f:
        f.write("\n".join(lines))


def main():
    preview = None
    if "--preview" in sys.argv:
        preview = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(preview, exist_ok=True)
    os.makedirs(TEX, exist_ok=True)
    entries = []
    for sid, (beast, parts) in SPECIES.items():
        beast.pack()
        parts.pack()
        img = paint_shape(beast)
        img.save(os.path.join(TEX, sid + ".png"))
        pimg = paint_shape(parts)
        pimg.save(os.path.join(TEX, sid + "_parts.png"))
        overlay = paint_overlay(parts, alpha=150 if parts.overlay_mat in ("gold",) else 115)
        overlay.save(os.path.join(TEX, sid + "_overlay.png"))
        entries.append((sid, beast.encode()))
        entries.append((sid + "_parts", parts.encode()))
        if preview:
            a = render(shape_polys(beast, img), yaw=-35, pitch=18, label=sid)
            c = render(shape_polys(beast, img), yaw=90, pitch=0, label=sid + " side")
            hp = player_polys() + shape_polys(parts, pimg, anchors=PLAYER)
            d = render(hp, yaw=150, pitch=15, label=sid + " hybrid")
            sheet = __import__("PIL.Image", fromlist=["Image"]).new("RGB", (960, 320))
            sheet.paste(a, (0, 0))
            sheet.paste(c, (320, 0))
            sheet.paste(d, (640, 0))
            sheet.save(os.path.join(preview, sid + ".png"))
    for rid, parts in RACES.items():
        parts.pack()
        paint_shape(parts).save(os.path.join(TEX, "race_" + rid + "_parts.png"))
        entries.append(("race_" + rid + "_parts", parts.encode()))
        if preview:
            hp = player_polys() + shape_polys(parts, paint_shape(parts), anchors=PLAYER)
            render(hp, yaw=150, pitch=15, label="race " + rid).save(os.path.join(preview, "race_" + rid + ".png"))
    for bid, parts in BOSSES.items():
        parts.pack()
        paint_shape(parts).save(os.path.join(TEX, "boss_" + bid + "_parts.png"))
        entries.append(("boss_" + bid + "_parts", parts.encode()))
        if preview:
            hp = player_polys() + shape_polys(parts, paint_shape(parts), anchors=PLAYER)
            render(hp, yaw=150, pitch=15, label="boss " + bid).save(os.path.join(preview, "boss_" + bid + ".png"))
            render(hp, yaw=-60, pitch=10, label="boss " + bid + " front").save(os.path.join(preview, "boss_" + bid + "_front.png"))
    for bid, body in BOSS_BODIES.items():
        body.pack()
        img = paint_shape(body)
        img.save(os.path.join(TEX, "boss_" + bid + "_body.png"))
        entries.append(("boss_" + bid + "_body", body.encode()))
        if preview:
            render(shape_polys(body, img), yaw=-35, pitch=18, label="boss " + bid).save(os.path.join(preview, "boss_" + bid + "_body.png"))
            render(shape_polys(body, img), yaw=90, pitch=0, label="boss " + bid + " side").save(
                os.path.join(preview, "boss_" + bid + "_body_side.png"))
    write_java(entries)
    print("zoan bodies:", len(SPECIES), "race parts:", len(RACES), "boss parts:", len(BOSSES), "boss bodies:", len(BOSS_BODIES),
          file=sys.stderr)


if __name__ == "__main__":
    main()
