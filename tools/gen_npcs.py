#!/usr/bin/env python3
"""Generates the NPC skins (64x64, player layout with the second layer) and the NPC definitions.

Run from the repository root: python3 tools/gen_npcs.py [--preview out.png]
"""
import json
import os
import random
import sys

from PIL import Image

ROOT = "src/main/resources"
SKINS = ROOT + "/assets/grandline/textures/entity/npc"
NPCS = ROOT + "/data/grandline/grandline/npc"

# u, v, width, height, depth of every box in the 64x64 player skin; the second layer boxes end in an underscore.
BOXES = {
    "head": (0, 0, 8, 8, 8), "head_": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "body_": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rarm_": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "larm_": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rleg_": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lleg_": (0, 48, 4, 12, 4),
}
FACES = ("top", "bottom", "right", "front", "left", "back")


def face_rect(box, face):
    u, v, w, h, d = BOXES[box]
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }[face]


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    def px(self, x, y, color, jitter=5):
        if color is None:
            return
        if len(color) == 4 and color[3] == 0:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            return
        j = self.rng.randint(-jitter, jitter) if jitter else 0
        r, g, b = (max(0, min(255, c + j)) for c in color[:3])
        self.img.putpixel((x, y), (r, g, b, 255))

    def paint(self, box, fn, faces=FACES):
        """Calls fn(face, x, y, w, h) for every pixel of the given faces; it returns a colour or None."""
        for face in faces:
            fx, fy, w, h = face_rect(box, face)
            for y in range(h):
                for x in range(w):
                    self.px(fx + x, fy + y, fn(face, x, y, w, h))

    def fill(self, box, color, faces=FACES):
        self.paint(box, lambda *a: color, faces)

    def at(self, box, face, x, y, color, jitter=3):
        fx, fy, w, h = face_rect(box, face)
        if 0 <= x < w and 0 <= y < h:
            self.px(fx + x, fy + y, color, jitter)

    def save(self, name):
        os.makedirs(SKINS, exist_ok=True)
        self.img.save(f"{SKINS}/{name}.png")
        return self.img


# ---------------------------------------------------------------------------------------------------------------
# Building blocks
# ---------------------------------------------------------------------------------------------------------------

SKIN_TONES = {"light": hexc("e8b894"), "tan": hexc("c98e62"), "brown": hexc("8d5a3b"), "fish": hexc("5aa6c8")}


def darker(c, f=0.75):
    return tuple(int(v * f) for v in c[:3])


def head(s, skin, hair, style="short", eyes=hexc("2b2b2b"), mouth=None, beard=None, glasses=None):
    s.fill("head", skin)
    # Hair: top, the back, and the upper rows of the sides and the brow.
    s.fill("head", hair, faces=("top",))
    s.paint("head", lambda f, x, y, w, h: hair if y < (6 if style == "long" else 3) else None, faces=("back",))
    if style == "long":
        s.paint("head", lambda f, x, y, w, h: hair, faces=("back",))
    s.paint("head", lambda f, x, y, w, h: hair if y < 2 or (style == "long" and x < 2 and f == "left")
            or (style == "long" and x > 5 and f == "right") or (y < 3 and (x < 2 if f == "right" else x > 5)) else None,
            faces=("left", "right"))
    s.paint("head", lambda f, x, y, w, h: hair if y < 2 or (y == 2 and x in (0, 1, 6, 7)) else None, faces=("front",))
    # Face.
    for x in (2, 5):
        s.at("head", "front", x, 4, (245, 245, 245), 0)
        s.at("head", "front", x + (1 if x == 2 else -1), 4, eyes, 0)
    s.at("head", "front", 2, 3, darker(hair), 0)
    s.at("head", "front", 5, 3, darker(hair), 0)
    s.at("head", "front", 3, 5, darker(skin, 0.88), 0)
    s.at("head", "front", 4, 5, darker(skin, 0.88), 0)
    m = mouth or darker(skin, 0.6)
    s.at("head", "front", 3, 6, m, 0)
    s.at("head", "front", 4, 6, m, 0)
    if beard:
        for x in range(1, 7):
            s.at("head", "front", x, 7, beard)
        for x in (1, 2, 5, 6):
            s.at("head", "front", x, 6, beard)
    if glasses:
        for x in range(1, 7):
            s.at("head", "front", x, 4, glasses, 0)
        s.at("head", "front", 1, 3, glasses, 0)
        s.at("head", "front", 6, 3, glasses, 0)


def torso(s, shirt, pattern=None):
    s.paint("body", lambda f, x, y, w, h: pattern(f, x, y, w, h) if pattern else shirt)


def arms(s, sleeve, skin, length=12, cuff=None):
    def color(f, x, y, w, h):
        if f == "bottom":
            return skin
        if y < length:
            return cuff if cuff and y == length - 1 and length < 12 else sleeve
        return skin
    s.paint("rarm", color)
    s.paint("larm", color)


def legs(s, pants, shoes, shoe_height=2, shorts=None, skin=None):
    def color(f, x, y, w, h):
        if f == "bottom" or y >= h - shoe_height:
            return shoes
        if shorts and y >= shorts:
            return skin
        return pants
    s.paint("rleg", color)
    s.paint("lleg", color)


def belt(s, color, buckle=None):
    s.paint("body", lambda f, x, y, w, h: color if y == 10 else None, faces=("front", "back", "left", "right"))
    if buckle:
        s.at("body", "front", 3, 10, buckle, 0)
        s.at("body", "front", 4, 10, buckle, 0)


def coat(s, color, trim=None, open_front=True, length=6):
    """A long coat on the second layer: body, sleeves and the upper legs."""
    def body(f, x, y, w, h):
        if f == "front" and open_front and 3 <= x <= 4:
            return None
        if trim and f == "front" and open_front and x in (2, 5):
            return trim
        return color
    s.paint("body_", body)
    s.paint("rarm_", lambda f, x, y, w, h: (trim if trim and y == 11 else color) if f != "bottom" else None)
    s.paint("larm_", lambda f, x, y, w, h: (trim if trim and y == 11 else color) if f != "bottom" else None)

    def leg(side):
        def fn(f, x, y, w, h):
            if f in ("top", "bottom") or y >= length:
                return None
            if f == "front" and open_front and ((side == "r" and x == 3) or (side == "l" and x == 0)):
                return None
            return trim if trim and y == length - 1 else color
        return fn
    s.paint("rleg_", leg("r"))
    s.paint("lleg_", leg("l"))


def cap(s, top, band, brim=None, emblem=None):
    """A peaked cap on the hat layer."""
    s.fill("head_", top, faces=("top",))
    s.paint("head_", lambda f, x, y, w, h: (band if y >= 1 else top) if y < 3 else None, faces=("front", "back", "left", "right"))
    if brim:
        s.paint("head_", lambda f, x, y, w, h: brim if y == 3 and 1 <= x <= 6 else None, faces=("front",))
    if emblem:
        s.at("head_", "front", 3, 1, emblem, 0)
        s.at("head_", "front", 4, 1, emblem, 0)


def bandana(s, color, knot=True):
    s.fill("head_", color, faces=("top",))
    s.paint("head_", lambda f, x, y, w, h: color if y < 2 else None, faces=("front", "back", "left", "right"))
    if knot:
        s.at("head_", "back", 3, 2, color)
        s.at("head_", "back", 4, 2, color)
        s.at("head_", "back", 4, 3, color)


# ---------------------------------------------------------------------------------------------------------------
# The skins
# ---------------------------------------------------------------------------------------------------------------

WHITE = hexc("f2f2ee")
NAVY = hexc("27406e")
BLUE = hexc("2f63b8")
BLACK = hexc("1d1d1f")
BROWN = hexc("6b4426")
GOLD = hexc("e0b83a")
RED = hexc("b8262a")


def marine(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("5a3a22"))

    def shirt(f, x, y, w, h):
        if f == "back" and y < 4:
            return BLUE
        if f == "front" and y < 4 and abs(x - 3.5) < 4 - y:
            return BLUE if y < 3 else WHITE
        if f == "front" and 4 <= y <= 5 and x in (3, 4):
            return BLUE
        return WHITE
    torso(s, WHITE, shirt)
    arms(s, WHITE, skin, 11, cuff=BLUE)
    legs(s, hexc("3b5f97"), BLACK)
    belt(s, hexc("3a2a1a"), GOLD)
    cap(s, WHITE, BLUE, brim=WHITE, emblem=BLUE)


def marine_officer(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("2e2a26"), beard=None)
    torso(s, hexc("9fc0e6"), lambda f, x, y, w, h: RED if f == "front" and x in (3, 4) and 1 <= y <= 7 else (
        NAVY if f != "front" or x < 2 or x > 5 else hexc("9fc0e6")))
    arms(s, NAVY, skin, 12)
    legs(s, NAVY, BLACK)
    coat(s, WHITE, trim=hexc("d8d8d0"), length=7)
    # "JUSTICE" across the back of the coat, as a dark band of letters.
    for x in range(8):
        if x not in (2, 5):
            s.at("body_", "back", x, 3, BLACK, 0)
        if x in (0, 3, 4, 7):
            s.at("body_", "back", x, 4, BLACK, 0)
    cap(s, WHITE, NAVY, brim=BLACK, emblem=GOLD)


def pirate(s):
    skin = SKIN_TONES["tan"]
    head(s, skin, hexc("1f1a17"), beard=hexc("2a221c"))
    torso(s, WHITE, lambda f, x, y, w, h: RED if y % 3 == 0 else WHITE)
    arms(s, WHITE, skin, 4)
    s.paint("rarm", lambda f, x, y, w, h: RED if y < 4 and y % 3 == 0 and f != "bottom" else None)
    s.paint("larm", lambda f, x, y, w, h: RED if y < 4 and y % 3 == 0 and f != "bottom" else None)
    legs(s, hexc("6b5232"), hexc("3b2a1c"), shoe_height=4)
    belt(s, BLACK, GOLD)
    bandana(s, RED)


def pirate_captain(s):
    skin = SKIN_TONES["tan"]
    head(s, skin, hexc("2a1e14"), beard=hexc("2a1e14"))
    torso(s, WHITE, lambda f, x, y, w, h: GOLD if f == "front" and x in (3, 4) and y in (2, 5, 8) else WHITE)
    arms(s, WHITE, skin, 12)
    legs(s, BLACK, hexc("3b2a1c"), shoe_height=5)
    coat(s, hexc("9e1d22"), trim=GOLD, length=8)
    # A black tricorn: crown on top, brim folded up all round.
    s.fill("head_", BLACK, faces=("top",))
    s.paint("head_", lambda f, x, y, w, h: (GOLD if y == 2 and f == "front" and 2 <= x <= 5 else BLACK) if y < 3 else None,
            faces=("front", "back", "left", "right"))
    s.at("head_", "front", 3, 1, WHITE, 0)
    s.at("head_", "front", 4, 1, WHITE, 0)


def fishman(s):
    skin = SKIN_TONES["fish"]
    head(s, skin, hexc("1d2f58"), mouth=WHITE)
    for f in ("left", "right"):
        for y in (5, 6):
            s.at("head", f, 1 if f == "left" else 6, y, darker(skin, 0.6), 0)
            s.at("head", f, 2 if f == "left" else 5, y, darker(skin, 0.6), 0)
    flower = [hexc("f4a62a"), hexc("e8e2c8"), hexc("d94f3a")]

    def shirt(f, x, y, w, h):
        if f == "front" and 3 <= x <= 4:
            return skin
        return flower[(x * 7 + y * 3) % 5 % 3] if (x + y) % 3 == 0 else hexc("e8862a")
    torso(s, hexc("e8862a"), shirt)
    arms(s, hexc("e8862a"), skin, 3)
    legs(s, hexc("f0ece0"), hexc("8a6a3a"), shoe_height=1, shorts=7, skin=skin)
    belt(s, hexc("3a2a1a"))


def villager_a(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("d8b65a"))
    torso(s, hexc("4f8a3c"), lambda f, x, y, w, h: hexc("3e6e30") if f == "front" and x in (3, 4) and y < 3 else hexc("4f8a3c"))
    arms(s, hexc("4f8a3c"), skin, 6)
    legs(s, hexc("7a5a38"), hexc("4a3220"))
    belt(s, hexc("4a3220"))


def villager_b(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("7a4a2a"), style="long")
    dress = hexc("4a7ac8")
    torso(s, dress, lambda f, x, y, w, h: WHITE if f == "front" and y < 2 and 2 <= x <= 5 else dress)
    s.paint("body", lambda f, x, y, w, h: hexc("7a4a2a") if f == "back" and y < 5 else None)
    arms(s, dress, skin, 3)
    legs(s, dress, hexc("5a3a24"), shoe_height=1, shorts=8, skin=skin)
    s.paint("rleg_", lambda f, x, y, w, h: dress if y < 6 and f not in ("top", "bottom") else None)
    s.paint("lleg_", lambda f, x, y, w, h: dress if y < 6 and f not in ("top", "bottom") else None)


def barkeeper(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("3a2418"), style="long")
    apron = hexc("e8d27a")
    torso(s, WHITE, lambda f, x, y, w, h: apron if f == "front" and y >= 4 and 1 <= x <= 6 else WHITE)
    arms(s, WHITE, skin, 5)
    legs(s, BLACK, hexc("3a2418"))
    s.paint("rleg", lambda f, x, y, w, h: apron if f == "front" and y < 6 else None)
    s.paint("lleg", lambda f, x, y, w, h: apron if f == "front" and y < 6 else None)
    bandana(s, hexc("3d8f4a"))


def merchant(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("5a5a5a"), beard=hexc("5a5a5a"))
    coat_c = hexc("6a2c4a")
    torso(s, hexc("d8c8a0"), lambda f, x, y, w, h: hexc("6b4426") if f == "back" and 2 <= y <= 9 and 1 <= x <= 6 else hexc("d8c8a0"))
    arms(s, coat_c, skin, 12)
    legs(s, hexc("c8b890"), hexc("4a3220"), shoe_height=3)
    coat(s, coat_c, trim=GOLD, length=5)
    s.fill("head_", hexc("6b4a2a"), faces=("top",))
    s.paint("head_", lambda f, x, y, w, h: hexc("6b4a2a") if y < 2 else (hexc("3a2418") if y == 2 else None),
            faces=("front", "back", "left", "right"))


def revolutionary(s):
    skin = SKIN_TONES["tan"]
    head(s, skin, hexc("2b2b2b"))
    cloak = hexc("2f4a33")
    torso(s, hexc("3a3a3a"))
    arms(s, hexc("3a3a3a"), skin, 12)
    legs(s, hexc("2a2a2a"), BLACK, shoe_height=4)
    coat(s, cloak, open_front=False, length=9)
    # Hood: covers the head layer except the face.
    s.fill("head_", cloak, faces=("top", "back", "left", "right"))
    s.paint("head_", lambda f, x, y, w, h: cloak if y < 2 or x in (0, 7) else None, faces=("front",))


def bounty_hunter(s):
    skin = SKIN_TONES["light"]
    head(s, skin, hexc("e8e8e8"), glasses=BLACK)
    torso(s, hexc("4a4a52"))
    arms(s, BLACK, skin, 12)
    legs(s, hexc("5a5a62"), BLACK, shoe_height=4)
    belt(s, hexc("6b4426"), hexc("c0c0c0"))
    coat(s, BLACK, trim=hexc("3a3a3a"), length=9)


def farmer(s):
    skin = SKIN_TONES["brown"]
    head(s, skin, hexc("2f5aa8"))
    torso(s, WHITE, lambda f, x, y, w, h: skin if y < 1 and 2 <= x <= 5 and f == "front" else WHITE)
    arms(s, WHITE, skin, 0)
    legs(s, hexc("2f4f8a"), hexc("5a3a24"))
    belt(s, hexc("5a3a24"))
    s.paint("head_", lambda f, x, y, w, h: RED if y == 2 else None, faces=("front", "back", "left", "right"))


SKIN_PAINTERS = {
    "marine": marine, "marine_officer": marine_officer, "pirate": pirate, "pirate_captain": pirate_captain,
    "fishman": fishman, "villager_a": villager_a, "villager_b": villager_b, "barkeeper": barkeeper,
    "merchant": merchant, "revolutionary": revolutionary, "bounty_hunter": bounty_hunter, "farmer": farmer,
}

# ---------------------------------------------------------------------------------------------------------------
# Definitions
# ---------------------------------------------------------------------------------------------------------------


def trade(buy, buy_count, sell, sell_count=1, uses=12, members_only=False, buy_b=None, buy_b_count=1):
    t = {"buy": buy, "buy_count": buy_count, "sell": sell, "sell_count": sell_count, "max_uses": uses}
    if members_only:
        t["members_only"] = True
    if buy_b:
        t["buy_b"] = buy_b
        t["buy_b_count"] = buy_b_count
    return t


DEFINITIONS = {
    "marine_recruit": dict(
        display_name="Marine Recruit", faction="grandline:marine", role="soldier", skin="marine",
        health=24, attack=3, speed=0.3, armor=2, equipment={"mainhand": "grandline:cutlass"}, patrol_radius=14,
        lines={"friendly": ["Good to see you on our side.", "The seas are calmer with you around.", "Justice will prevail!"],
               "neutral": ["Move along, citizen.", "Keep out of trouble.", "We're keeping an eye on pirates around here."],
               "hostile": ["Halt! You're under arrest!", "There's a price on your head!", "Surrender to the Marines!"]}),
    "marine_rifleman": dict(
        display_name="Marine Rifleman", faction="grandline:marine", role="soldier", skin="marine",
        health=22, attack=2, speed=0.28, armor=2, equipment={"mainhand": "grandline:musket"}, patrol_radius=10,
        lines={"friendly": ["Covering you from back here.", "Steady aim, steady heart."],
               "neutral": ["Don't make me use this.", "Stay where I can see you."],
               "hostile": ["Fire!", "Target sighted!", "Open fire!"]}),
    "marine_lieutenant": dict(
        display_name="Marine Lieutenant", faction="grandline:marine", role="soldier", skin="marine_officer",
        health=60, attack=7, speed=0.31, armor=6, equipment={"mainhand": "grandline:katana"}, patrol_radius=6,
        lines={"friendly": ["Report, sailor. Anything to note?", "Fine work. Keep it up."],
               "neutral": ["State your business on this base.", "This is Marine ground."],
               "hostile": ["You dare raise your hand against Justice?", "I'll bring you in myself!"]}),
    "marine_quartermaster": dict(
        display_name="Marine Quartermaster", faction="grandline:marine", role="merchant", skin="marine",
        health=26, attack=2, speed=0.25, armor=2, wander_radius=4,
        lines={"friendly": ["Need supplies? You've earned them.", "Fresh stock from headquarters."],
               "neutral": ["Supplies for the Marines and their friends.", "Make it quick."],
               "hostile": ["I don't arm criminals."]},
        trades=[trade("minecraft:emerald", 1, "grandline:iron_bullet", 8, 24),
                trade("minecraft:emerald", 2, "grandline:cannonball", 4, 16),
                trade("minecraft:emerald", 6, "grandline:cutlass"),
                trade("minecraft:emerald", 10, "grandline:flintlock", 1, 4),
                trade("minecraft:emerald", 4, "grandline:seastone_bullet", 4, 12, members_only=True),
                trade("minecraft:emerald", 16, "grandline:seastone_cuffs", 1, 2, members_only=True),
                trade("minecraft:emerald", 14, "grandline:musket", 1, 3, members_only=True)]),
    "pirate_grunt": dict(
        display_name="Pirate", faction="grandline:pirate", role="guard", skin="pirate",
        health=24, attack=4, speed=0.3, armor=1, equipment={"mainhand": "grandline:cutlass"}, patrol_radius=8,
        lines={"friendly": ["Ahoy, mate!", "Seen any treasure lately?", "Yo ho!"],
               "neutral": ["What're you looking at?", "Keep walking, landlubber."],
               "hostile": ["Get 'em, lads!", "Your treasure or your life!", "Haha! Easy pickings!"]}),
    "pirate_gunner": dict(
        display_name="Pirate Gunner", faction="grandline:pirate", role="guard", skin="pirate",
        health=22, attack=2, speed=0.29, armor=1, equipment={"mainhand": "grandline:flintlock"}, patrol_radius=6,
        lines={"friendly": ["Need a shot fired, mate?"], "neutral": ["One wrong move..."], "hostile": ["Bang! Hahaha!"]}),
    "pirate_captain": dict(
        display_name="Pirate Captain", faction="grandline:pirate", role="guard", skin="pirate_captain",
        health=70, attack=8, speed=0.31, armor=5, equipment={"mainhand": "grandline:katana"}, patrol_radius=4,
        lines={"friendly": ["Welcome aboard, friend of the crew.", "The sea's ours for the taking!"],
               "neutral": ["You're in pirate waters now.", "Mind your tongue around my crew."],
               "hostile": ["I'll feed you to the Sea Kings!", "All hands, attack!"]}),
    "fishman_pirate": dict(
        display_name="Arlong Pirate", faction="grandline:pirate", role="guard", skin="fishman", swimmer=True,
        health=40, attack=7, speed=0.32, armor=3, equipment={}, patrol_radius=8,
        lines={"friendly": ["Shahaha! You've got guts, human.", "Arlong Park welcomes you... for now."],
               "neutral": ["Lowly human. Don't stand in my way.", "This island belongs to Arlong."],
               "hostile": ["Fish-Men are the superior race!", "Humans are no match for us!"]}),
    "revolutionary_agent": dict(
        display_name="Revolutionary Agent", faction="grandline:revolutionary", role="guard", skin="revolutionary",
        health=34, attack=5, speed=0.33, armor=3, equipment={"mainhand": "grandline:flintlock"}, patrol_radius=8,
        lines={"friendly": ["The dawn is coming, comrade.", "Dragon sends his regards."],
               "neutral": ["You didn't see me.", "Keep your voice down."],
               "hostile": ["For the revolution!", "You serve the World Government!"]}),
    "bounty_hunter": dict(
        display_name="Bounty Hunter", faction="grandline:bounty_hunter", role="guard", skin="bounty_hunter",
        health=36, attack=6, speed=0.33, armor=3, equipment={"mainhand": "grandline:katana"}, patrol_radius=10,
        lines={"friendly": ["Business is good. Know any wanted faces?"],
               "neutral": ["You're not worth anything to me. Yet."],
               "hostile": ["Your bounty is mine!", "Dead or alive, you're coming with me!"]}),
    "bounty_hunter_veteran": dict(
        display_name="Veteran Bounty Hunter", faction="grandline:bounty_hunter", role="guard", skin="bounty_hunter",
        health=70, attack=9, speed=0.34, armor=6, equipment={"mainhand": "grandline:sandai_kitetsu", "head": "minecraft:chainmail_helmet"},
        patrol_radius=10,
        lines={"friendly": ["A fellow professional. Good hunting."],
               "neutral": ["Not worth my time."],
               "hostile": ["I've taken bigger heads than yours.", "Your poster's coming down today!"]}),
    "marine_captain": dict(
        display_name="Marine Captain", faction="grandline:marine", role="soldier", skin="marine_officer",
        health=110, attack=11, speed=0.32, armor=9, equipment={"mainhand": "grandline:katana", "chest": "minecraft:iron_chestplate"},
        patrol_radius=6,
        lines={"friendly": ["At ease. The sea is safer with you on it."],
               "neutral": ["Mind yourself. I answer to Headquarters."],
               "hostile": ["In the name of Absolute Justice!", "You won't escape this time, pirate!"]}),
    "villager": dict(
        display_name="Villager", role="civilian", skin="villager_a", health=20, attack=1, speed=0.28, armor=0, wander_radius=10,
        lines={"friendly": ["Thank you for keeping us safe!", "Lovely day, isn't it?", "Come back anytime!"],
               "neutral": ["Hello there, traveller.", "Not many visitors these days.", "The sea's been rough lately."],
               "hostile": ["Stay away from me!", "Help! Somebody call the Marines!"]}),
    "townsperson": dict(
        display_name="Townsperson", role="civilian", skin="villager_b", health=20, attack=1, speed=0.28, armor=0, wander_radius=10,
        lines={"friendly": ["You're always welcome here!", "Have you tried the bar? Best meat in town."],
               "neutral": ["Good day.", "Watch out for pirates on the coast."],
               "hostile": ["Don't hurt me!", "Marines! Help!"]}),
    "barkeeper": dict(
        display_name="Barkeeper", role="merchant", skin="barkeeper", health=20, attack=1, speed=0.26, armor=0, wander_radius=3,
        lines={"friendly": ["The usual? Coming right up!", "You've got a tab here, don't worry."],
               "neutral": ["Welcome! Have a seat.", "Hungry? We've got meat and milk."],
               "hostile": ["Please, leave my bar..."]},
        trades=[trade("minecraft:emerald", 1, "minecraft:cooked_beef", 4, 32),
                trade("minecraft:emerald", 1, "minecraft:milk_bucket", 1, 8, buy_b="minecraft:bucket"),
                trade("minecraft:emerald", 1, "minecraft:bread", 6, 32),
                trade("minecraft:emerald", 2, "minecraft:cake", 1, 6),
                trade("minecraft:wheat", 18, "minecraft:emerald", 1, 16)]),
    "travelling_merchant": dict(
        display_name="Travelling Merchant", role="merchant", skin="merchant", health=24, attack=1, speed=0.3, armor=0,
        wander_radius=40,
        lines={"friendly": ["Ah, my favourite customer!", "Rare goods from the Grand Line!"],
               "neutral": ["Wares from every sea! Take a look.", "Everything has a price, friend."],
               "hostile": ["Business is closed. For you."]},
        trades=[trade("minecraft:emerald", 3, "minecraft:map", 1, 6),
                trade("minecraft:emerald", 2, "minecraft:compass", 1, 6),
                trade("minecraft:emerald", 8, "grandline:ship_in_a_bottle", 1, 2),
                trade("minecraft:emerald", 5, "grandline:raw_seastone", 1, 6),
                trade("minecraft:gold_ingot", 4, "minecraft:emerald", 3, 12),
                trade("minecraft:emerald", 24, "grandline:swordsman_scroll", 1, 1),
                trade("minecraft:emerald", 24, "grandline:gunslinger_scroll", 1, 1)]),
    "tangerine_farmer": dict(
        display_name="Tangerine Farmer", role="merchant", skin="farmer", health=22, attack=2, speed=0.28, armor=0, wander_radius=8,
        lines={"friendly": ["Take a tangerine for the road!", "Bell-mere's trees never fail us."],
               "neutral": ["Best tangerines in the East Blue.", "Careful near Arlong Park."],
               "hostile": ["Get off my grove!"]},
        trades=[trade("minecraft:emerald", 1, "grandline:tangerine", 8, 32),
                trade("minecraft:emerald", 3, "grandline:tangerine_leaves", 4, 12),
                trade("grandline:tangerine", 24, "minecraft:emerald", 1, 16)]),
}

ITEM_IDS = None


def check_items(data):
    models = {f[:-5] for f in os.listdir(ROOT + "/assets/grandline/models/item")}
    for key, d in data.items():
        items = list(d.get("equipment", {}).values())
        for t in d.get("trades", []):
            items += [t["buy"], t["sell"]] + ([t["buy_b"]] if "buy_b" in t else [])
        for item in items:
            ns, path = item.split(":")
            if ns == "grandline":
                assert path in models, f"{key}: unknown item {item}"


def definitions():
    check_items(DEFINITIONS)
    os.makedirs(NPCS, exist_ok=True)
    for name, d in DEFINITIONS.items():
        data = dict(d)
        data["display_name"] = {"translate": f"npc.grandline.{name}"}
        with open(f"{NPCS}/{name}.json", "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    path = ROOT + "/assets/grandline/lang/en_us.json"
    with open(path) as f:
        lang = json.load(f)
    for name, d in DEFINITIONS.items():
        lang[f"npc.grandline.{name}"] = d["display_name"]
    lang["entity.grandline.npc"] = "Islander"
    lang["entity.grandline.npc_shot"] = "Musket Ball"
    with open(path, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


def loot():
    path = ROOT + "/data/grandline/loot_table/entities/npc.json"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    table = {
        "type": "minecraft:entity",
        "pools": [{
            "rolls": 1,
            "conditions": [{"condition": "minecraft:killed_by_player"}],
            "entries": [{"type": "minecraft:item", "name": "minecraft:emerald", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}},
                {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                 "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}],
        }],
        "random_sequence": "grandline:entities/npc",
    }
    with open(path, "w") as f:
        json.dump(table, f, indent=2)
        f.write("\n")


def skins(preview=None):
    images = []
    for i, (name, painter) in enumerate(SKIN_PAINTERS.items()):
        s = Skin(1000 + i)
        painter(s)
        images.append((name, s.save(name)))
    if preview:
        sheet = Image.new("RGBA", (64 * 4 * 4, 64 * 4 * 3), (40, 40, 48, 255))
        for i, (name, img) in enumerate(images):
            sheet.paste(img.resize((256, 256), Image.NEAREST), ((i % 4) * 256, (i // 4) * 256), img.resize((256, 256), Image.NEAREST))
        sheet.save(preview)


if __name__ == "__main__":
    preview = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    skins(preview)
    definitions()
    loot()
    print(f"{len(SKIN_PAINTERS)} skins, {len(DEFINITIONS)} NPC definitions")
