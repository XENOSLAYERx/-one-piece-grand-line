"""Engine for tools/gen_zoans.py: box geometry for Zoan bodies, UV packing, procedural texture painting, the data
string the client parses (ZoanShapeData.java), and a software preview renderer for checking shapes without the game.

Model space is Minecraft's: pixels (1/16 block), +y is down, the ground is y = 24, the head faces -z. Rotations are
radians, applied like ModelPart does (Z, then Y, then X, about the part's pivot).
"""
import math
import random

from PIL import Image, ImageDraw

ROLES = ("FIXED", "BODY", "HEAD", "NECK", "JAW", "LEG_FRONT", "LEG_BACK", "LEG", "ARM", "TAIL", "WING", "WING_TIP",
         "SEGMENT", "SPIDER_LEG", "EAR", "FIN")
ANCHORS = ("NONE", "HEAD", "BODY", "RIGHT_ARM", "LEFT_ARM", "RIGHT_LEG", "LEFT_LEG")


class Cube:
    def __init__(self, x, y, z, w, h, d, mat="fur", inflate=0.0):
        self.x, self.y, self.z = float(x), float(y), float(z)
        self.w, self.h, self.d = int(w), int(h), int(d)
        assert self.w >= 0 and self.h >= 0 and self.d >= 0
        self.mat = mat
        self.inflate = inflate
        self.u = self.v = 0

    def block(self):
        return 2 * (self.d + self.w), self.h + self.d


class Part:
    def __init__(self, name, parent, pivot, rot, role, param, anchor, cubes):
        self.name = name
        self.parent = parent
        self.pivot = tuple(float(c) for c in pivot)
        self.rot = tuple(float(c) for c in rot)
        assert role in ROLES, role
        assert anchor in ANCHORS, anchor
        self.role = role
        self.param = float(param)
        self.anchor = anchor
        self.cubes = list(cubes)


class Shape:
    """One model: a beast body, or the parts a hybrid wears on the player model."""

    def __init__(self, sid, palette, pattern="none", eyes="front", scale=1.0, gait=1.0, seed=1):
        self.sid = sid
        self.palette = palette
        self.pattern = pattern
        self.eyes = eyes
        self.scale = scale
        self.gait = gait
        self.seed = seed
        self.parts = []
        self.tex = (64, 64)

    def add(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0), role="FIXED", param=0.0, anchor="NONE",
            cubes=()):
        assert all(p.name != name for p in self.parts), "duplicate part " + name
        if parent is not None:
            assert any(p.name == parent for p in self.parts), "unknown parent " + parent
        self.parts.append(Part(name, parent, pivot, rot, role, param, anchor, cubes))
        return name

    def index(self, name):
        for i, p in enumerate(self.parts):
            if p.name == name:
                return i
        return -1

    # -----------------------------------------------------------------------------------------------------------
    # UV packing

    def pack(self):
        cubes = [c for p in self.parts for c in p.cubes]
        order = sorted(cubes, key=lambda c: (-c.block()[1], -c.block()[0]))
        size = 64
        while True:
            x = y = row = 0
            ok = True
            for c in order:
                bw, bh = c.block()
                if bw > size:
                    ok = False
                    break
                if x + bw > size:
                    x, y, row = 0, y + row, 0
                if y + bh > size:
                    ok = False
                    break
                c.u, c.v = x, y
                x += bw
                row = max(row, bh)
            if ok:
                self.tex = (size, size)
                return
            size *= 2
            assert size <= 512, "shape too big to pack: " + self.sid

    # -----------------------------------------------------------------------------------------------------------
    # Data string for the client

    def encode(self):
        out = ["%s,%d,%d,%.3f,%.3f" % (self.sid, self.tex[0], self.tex[1], self.scale, self.gait)]
        for p in self.parts:
            head = [p.name, str(self.index(p.parent) if p.parent else -1), p.anchor]
            head += ["%g" % round(v, 3) for v in p.pivot + p.rot]
            head += [p.role, "%g" % round(p.param, 3)]
            boxes = []
            for c in p.cubes:
                boxes.append(",".join(["%d" % c.u, "%d" % c.v] + ["%g" % round(v, 3) for v in (c.x, c.y, c.z)]
                                      + ["%d" % c.w, "%d" % c.h, "%d" % c.d, "%g" % c.inflate]))
            out.append(",".join(head) + ":" + ";".join(boxes))
        return "|".join(out)


# ---------------------------------------------------------------------------------------------------------------
# Texture painting
# ---------------------------------------------------------------------------------------------------------------

def faces(c):
    u, v, w, h, d = c.u, c.v, c.w, c.h, c.d
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def clamp(c):
    return tuple(max(0, min(255, int(round(x)))) for x in c)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def shade(c, f):
    return tuple(x * f for x in c)


def fill(px, rect, fn):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            col = fn(x - x0, y - y0, w, h)
            if col is not None:
                px[x, y] = clamp(col[:3]) + ((col[3],) if len(col) > 3 else (255,))


def paint_shape(shape):
    """Paints every cube of a packed shape by material and the species pattern. Returns an RGBA image."""
    pal = shape.palette
    rnd = random.Random(shape.seed)
    img = Image.new("RGBA", shape.tex, (0, 0, 0, 0))
    px = img.load()
    for part in shape.parts:
        for c in part.cubes:
            for face, rect in faces(c).items():
                if rect[2] <= 0 or rect[3] <= 0:
                    continue
                paint_face(px, rect, face, c.mat, pal, shape.pattern, rnd)
            if c.mat in ("head", "head_side") and c.w >= 2 and c.h >= 2:
                paint_eyes(px, c, shape.eyes if c.mat == "head" else "side", pal)
    return img


def noisy(base, rnd, amount):
    n = rnd.uniform(-amount, amount)
    return (base[0] + n, base[1] + n, base[2] + n)


def paint_face(px, rect, face, mat, pal, pattern, rnd):
    base = pal["base"]
    belly = pal.get("belly", base)
    accent = pal.get("accent", shade(base, 0.6))
    dark = pal.get("dark", shade(base, 0.35))
    under = face == "bottom"

    if mat in ("fur", "head", "head_side", "skin"):
        col = belly if under and mat != "skin" else base
        if mat == "skin":
            col = mix(base, belly, 0.6) if under else base

        def fn(x, y, w, h):
            c = noisy(col, rnd, 10)
            # a little top-lit shading so boxes read as solid
            c = shade(c, 1.06 - 0.12 * (y / max(1, h - 1)) if face not in ("top", "bottom") else 1.0)
            return c
        fill(px, rect, fn)
        if not under:
            apply_pattern(px, rect, pattern, pal, rnd)
    elif mat == "belly":
        fill(px, rect, lambda x, y, w, h: noisy(belly, rnd, 8))
    elif mat == "accent":
        fill(px, rect, lambda x, y, w, h: noisy(accent, rnd, 10))
    elif mat == "mane":
        fill(px, rect, lambda x, y, w, h: shade(noisy(accent, rnd, 8), 0.85 + 0.25 * ((x * 7 + y * 3) % 5 == 0)))
    elif mat == "dark":
        fill(px, rect, lambda x, y, w, h: noisy(dark, rnd, 6))
    elif mat in ("horn", "tusk", "tooth", "claw"):
        hc = pal.get(mat, {"horn": (226, 214, 180), "tusk": (240, 232, 214), "tooth": (250, 248, 240),
                           "claw": (60, 52, 48)}[mat])
        fill(px, rect, lambda x, y, w, h: shade(noisy(hc, rnd, 6), 0.9 + 0.15 * (y / max(1, h - 1))))
    elif mat == "beak":
        bc = pal.get("beak", (232, 176, 48))
        fill(px, rect, lambda x, y, w, h: noisy(bc, rnd, 6))
    elif mat == "hoof":
        fill(px, rect, lambda x, y, w, h: noisy((54, 44, 38), rnd, 5))
    elif mat == "mouth":
        fill(px, rect, lambda x, y, w, h: noisy((120, 36, 40), rnd, 6))
    elif mat == "nose":
        fill(px, rect, lambda x, y, w, h: noisy(pal.get("nose", (40, 32, 30)), rnd, 4))
    elif mat == "feather":
        fc = belly if under else base

        def fn(x, y, w, h):
            c = noisy(fc, rnd, 7)
            if (y % 3 == 2) or ((x + y // 3) % 4 == 0 and y % 3 == 1):
                c = shade(c, 0.78)
            return c
        fill(px, rect, fn)
    elif mat == "wing":
        # flight feathers: light at the root, the accent colour at the tips, with feather lines
        def fn(x, y, w, h):
            t = x / max(1, w - 1) if face in ("top", "bottom") else y / max(1, h - 1)
            c = mix(base, accent, min(1.0, t * 1.2))
            c = noisy(c, rnd, 6)
            if x % 2 == 0:
                c = shade(c, 0.86)
            return c
        fill(px, rect, fn)
    elif mat == "membrane":
        mc = pal.get("membrane", mix(base, accent, 0.5))
        fill(px, rect, lambda x, y, w, h: shade(noisy(mc, rnd, 5), 0.9 + 0.2 * ((x + y) % 6 == 0)))
    elif mat == "flame":
        lo = pal.get("flame_lo", (60, 140, 255))
        hi = pal.get("flame_hi", (250, 236, 120))

        def fn(x, y, w, h):
            t = 1 - y / max(1, h - 1) if face not in ("top", "bottom") else 0.5
            flick = rnd.uniform(-0.15, 0.15)
            return mix(lo, hi, max(0.0, min(1.0, t + flick)))
        fill(px, rect, fn)
    elif mat == "bone":
        bc = pal.get("bone", (232, 228, 214))

        def fn(x, y, w, h):
            c = noisy(bc, rnd, 8)
            if face not in ("top", "bottom") and (y % 4 == 3) and w > 3:
                c = shade(c, 0.45)  # gaps between ribs and vertebrae
            return c
        fill(px, rect, fn)
    elif mat == "gold":
        def fn(x, y, w, h):
            t = 0.5 + 0.5 * math.sin((x + y * 0.7) * 0.6)
            return mix((200, 150, 30), (255, 226, 110), t * 0.8 + rnd.uniform(0, 0.2))
        fill(px, rect, fn)
    elif mat == "scale":
        sc = belly if under else base

        def fn(x, y, w, h):
            c = noisy(sc, rnd, 6)
            if (x + (y // 2) % 2) % 2 == 0 and y % 2 == 0:
                c = shade(c, 0.8)
            return c
        fill(px, rect, fn)
        if not under:
            apply_pattern(px, rect, pattern, pal, rnd)
    elif mat == "ice":
        fill(px, rect, lambda x, y, w, h: mix((200, 230, 255), (120, 180, 240), rnd.uniform(0, 0.4)) + (230,))
    elif mat == "glow":
        gc = pal.get("glow", accent)
        fill(px, rect, lambda x, y, w, h: noisy(gc, rnd, 4))
    else:
        raise ValueError("unknown material " + mat)


def apply_pattern(px, rect, pattern, pal, rnd):
    x0, y0, w, h = rect
    if w * h < 4 or pattern == "none":
        return
    spot = pal.get("spot", pal.get("dark", (40, 30, 20)))
    accent = pal.get("accent", spot)

    def put(x, y, c, a=255):
        if x0 <= x < x0 + w and y0 <= y < y0 + h:
            px[x, y] = clamp(c) + (a,)

    if pattern in ("spots", "rosettes"):
        for _ in range(max(1, w * h // 12)):
            sx, sy = rnd.randint(x0, x0 + w - 1), rnd.randint(y0, y0 + h - 1)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                if rnd.random() < 0.75:
                    put(sx + dx, sy + dy, spot)
            if pattern == "rosettes":
                put(sx, sy, mix(pal["base"], spot, 0.4))
    elif pattern == "patches":  # giraffe: big polygons separated by pale lines
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                cell = ((x - x0) // 4 + ((y - y0) // 4) * 3) % 5
                if (x - x0) % 4 == 0 or (y - y0) % 4 == 0:
                    put(x, y, pal.get("belly", (240, 220, 170)))
                elif cell == 0:
                    put(x, y, shade(pal["base"], 0.9))
    elif pattern == "stripes":
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if (x - x0 + (y - y0) // 2) % 5 == 0:
                    put(x, y, spot)
    elif pattern == "bands":  # snakes: cross bands
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if (y - y0 + (x - x0) // 3) % 6 in (0, 1):
                    put(x, y, accent)
    elif pattern == "diamonds":  # anaconda blotches
        for _ in range(max(1, w * h // 18)):
            sx, sy = rnd.randint(x0, x0 + w - 1), rnd.randint(y0, y0 + h - 1)
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                put(sx + dx, sy + dy, spot)
            put(sx, sy, accent)
    elif pattern == "mottle":
        for _ in range(max(1, w * h // 6)):
            sx, sy = rnd.randint(x0, x0 + w - 1), rnd.randint(y0, y0 + h - 1)
            put(sx, sy, mix(pal["base"], spot, rnd.uniform(0.3, 0.7)))
    elif pattern == "speckle":
        for _ in range(max(1, w * h // 10)):
            sx, sy = rnd.randint(x0, x0 + w - 1), rnd.randint(y0, y0 + h - 1)
            put(sx, sy, accent)
    elif pattern == "shaggy":
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if (x * 3 + y * 5) % 7 == 0:
                    put(x, y, shade(pal["base"], 0.75))
    else:
        raise ValueError("unknown pattern " + pattern)


def paint_eyes(px, c, where, pal):
    eye = pal.get("eye", (230, 200, 60))
    pupil = pal.get("pupil", (20, 16, 14))
    f = faces(c)
    if where == "front":
        x0, y0, w, h = f["north"]
        ey = y0 + max(0, min(h - 2, h // 3))
        for ex in (x0 + max(0, w // 4 - 1), x0 + w - 1 - max(0, w // 4 - 1) - 1):
            px[ex, ey] = clamp(eye) + (255,)
            px[ex + 1, ey] = clamp(pupil) + (255,)
    elif where == "side":
        for side in ("east", "west"):
            x0, y0, w, h = f[side]
            if w < 2:
                continue
            ex = x0 + (w - 2 if side == "east" else 0)
            ey = y0 + max(0, h // 3)
            px[ex, ey] = clamp(eye) + (255,)
            px[ex + 1, ey] = clamp(pupil) + (255,)
    elif where == "glow":
        x0, y0, w, h = f["north"]
        ey = y0 + max(0, min(h - 2, h // 3))
        for ex in (x0 + max(0, w // 4 - 1), x0 + w - 1 - max(0, w // 4 - 1) - 1):
            px[ex, ey] = clamp(eye) + (255,)
            px[ex + 1, ey] = clamp(eye) + (255,)


def paint_overlay(shape, alpha=115, face_clear=True):
    """The hybrid's fur/scale overlay in the player skin layout (drawn over the skin on an inflated shell)."""
    pal = shape.palette
    rnd = random.Random(shape.seed * 7 + 3)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    mat = shape.overlay_mat if hasattr(shape, "overlay_mat") else "fur"
    for y in range(64):
        for x in range(64):
            if face_clear and 8 <= x < 16 and 8 <= y < 16:
                continue
            if mat == "gold":
                t = 0.5 + 0.5 * math.sin((x + y * 0.7) * 0.5)
                c = mix((200, 150, 30), (255, 226, 110), t)
                px[x, y] = clamp(c) + (min(255, alpha + 60),)
                continue
            if mat == "bone":
                c = noisy(pal.get("bone", (232, 228, 214)), rnd, 8)
                a = alpha + 40 if (y % 4 != 3) else 40
                px[x, y] = clamp(c) + (min(255, a),)
                continue
            if mat == "flame":
                t = (y % 16) / 15.0
                c = mix(pal.get("flame_hi", (250, 236, 120)), pal.get("flame_lo", (60, 140, 255)), t)
                px[x, y] = clamp(c) + (alpha,)
                continue
            base = pal["base"]
            c = noisy(base, rnd, 10)
            if mat == "scale" and (x + (y // 2) % 2) % 2 == 0 and y % 2 == 0:
                c = shade(c, 0.8)
            if mat == "feather" and y % 3 == 2:
                c = shade(c, 0.8)
            px[x, y] = clamp(c) + (alpha,)
    pattern = shape.pattern
    if pattern != "none":
        tmp = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        tp = tmp.load()
        apply_pattern(tp, (0, 0, 64, 64), pattern, pal, rnd)
        for y in range(64):
            for x in range(64):
                if face_clear and 8 <= x < 16 and 8 <= y < 16:
                    continue
                r, g, b, a = tp[x, y]
                if a:
                    px[x, y] = (r, g, b, 210)
    return img


# ---------------------------------------------------------------------------------------------------------------
# Preview renderer
# ---------------------------------------------------------------------------------------------------------------

def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def apply(m, p):
    x, y, z = p
    return tuple(m[i][0] * x + m[i][1] * y + m[i][2] * z + m[i][3] for i in range(3))


IDENTITY = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]

PLAYER = {
    "HEAD": (0, 0, 0), "BODY": (0, 0, 0), "RIGHT_ARM": (-5, 2, 0), "LEFT_ARM": (5, 2, 0),
    "RIGHT_LEG": (-1.9, 12, 0), "LEFT_LEG": (1.9, 12, 0),
}


def player_polys():
    boxes = [((-4, -8, -4, 8, 8, 8), (0, 0, 0), (214, 170, 140)), ((-4, 0, -2, 8, 12, 4), (0, 0, 0), (70, 110, 170)),
             ((-3, -2, -2, 4, 12, 4), (-5, 2, 0), (214, 170, 140)), ((-1, -2, -2, 4, 12, 4), (5, 2, 0), (214, 170, 140)),
             ((-2, 0, -2, 4, 12, 4), (-1.9, 12, 0), (60, 60, 90)), ((-2, 0, -2, 4, 12, 4), (1.9, 12, 0), (60, 60, 90))]
    out = []
    for (x, y, z, w, h, d), piv, col in boxes:
        m = translate(*piv)
        out += box_polys(m, x, y, z, w, h, d, {k: col for k in ("top", "bottom", "east", "west", "north", "south")})
    return out


def box_polys(m, x, y, z, w, h, d, colors, inflate=0.0):
    x0, y0, z0 = x - inflate, y - inflate, z - inflate
    x1, y1, z1 = x + w + inflate, y + h + inflate, z + d + inflate
    v = {
        "000": (x0, y0, z0), "100": (x1, y0, z0), "010": (x0, y1, z0), "110": (x1, y1, z0),
        "001": (x0, y0, z1), "101": (x1, y0, z1), "011": (x0, y1, z1), "111": (x1, y1, z1),
    }
    quads = {
        "top": ("000", "100", "101", "001"), "bottom": ("010", "110", "111", "011"),
        "north": ("000", "100", "110", "010"), "south": ("001", "101", "111", "011"),
        "east": ("000", "001", "011", "010"), "west": ("100", "101", "111", "110"),
    }
    return [([apply(m, v[k]) for k in quad], colors[face]) for face, quad in quads.items()]


def face_colors(img, c):
    px = img.load()
    out = {}
    for face, (x0, y0, w, h) in faces(c).items():
        if w <= 0 or h <= 0:
            out[face] = (255, 0, 255)
            continue
        acc = [0, 0, 0]
        n = 0
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                r, g, b, a = px[x, y]
                if a > 0:
                    acc[0] += r
                    acc[1] += g
                    acc[2] += b
                    n += 1
        out[face] = tuple(int(a / max(1, n)) for a in acc) if n else (255, 0, 255)
    return out


def shape_polys(shape, img, root=IDENTITY, anchors=None):
    mats = {}
    polys = []
    for p in shape.parts:
        if p.parent:
            base = mats[p.parent]
        elif anchors is not None and p.anchor != "NONE":
            base = mat_mul(root, translate(*anchors[p.anchor]))
        else:
            base = root
        m = mat_mul(base, translate(*p.pivot))
        m = mat_mul(m, rot_z(p.rot[2]))
        m = mat_mul(m, rot_y(p.rot[1]))
        m = mat_mul(m, rot_x(p.rot[0]))
        mats[p.name] = m
        for c in p.cubes:
            polys += box_polys(m, c.x, c.y, c.z, c.w, c.h, c.d, face_colors(img, c), c.inflate)
    return polys


def render(polys, size=320, yaw=-35, pitch=20, background=(36, 40, 48), label=None):
    """Orthographic view; yaw/pitch in degrees. Model y is down, so the image is flipped to stand upright."""
    view = mat_mul(rot_x(math.radians(-pitch)), rot_y(math.radians(yaw)))
    projected = []
    for pts, col in polys:
        vp = [apply(view, p) for p in pts]
        depth = sum(p[2] for p in vp) / 4
        # simple lighting from the face normal
        ax, ay, az = (vp[1][i] - vp[0][i] for i in range(3))
        bx, by, bz = (vp[3][i] - vp[0][i] for i in range(3))
        nx, ny, nz = ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx
        ln = math.sqrt(nx * nx + ny * ny + nz * nz) or 1
        light = 0.55 + 0.45 * abs((nx * 0.3 - ny * 0.8 - nz * 0.5) / ln)
        projected.append((depth, [(p[0], p[1]) for p in vp], shade(col, light)))
    xs = [x for _, pts, _ in projected for x, _ in pts]
    ys = [y for _, pts, _ in projected for _, y in pts]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    span = max(maxx - minx, maxy - miny, 1)
    k = (size - 30) / span
    img = Image.new("RGB", (size, size), background)
    d = ImageDraw.Draw(img)
    for depth, pts, col in sorted(projected, key=lambda t: -t[0]):
        d.polygon([((x - minx) * k + 15, (y - miny) * k + 15) for x, y in pts], fill=clamp(col), outline=(20, 20, 24))
    if label:
        d.text((6, size - 14), label, fill=(240, 240, 240))
    return img
