#!/usr/bin/env python3
"""Generates the boss definitions, their loot tables and their names.

Run from the repository root: python3 tools/gen_bosses.py

Writes
  src/main/resources/data/grandline/grandline/boss/<id>.json         the boss (see BossDefinition)
  src/main/resources/data/grandline/loot_table/entities/boss/<id>.json what they drop (their signature weapon always)
  boss.grandline.* entries in assets/grandline/lang/en_us.json
The moves named in each phase are code (BossMoves); unknown move ids fail here.
"""
import json
import os
import re

ROOT = "src/main/resources"
BOSSES_DIR = ROOT + "/data/grandline/grandline/boss"
LOOT_DIR = ROOT + "/data/grandline/loot_table/entities/boss"
MOVES_JAVA = "src/main/java/com/nerodev/grandline/boss/BossMoves.java"

BOSSES = {
    "morgan": {
        "name": "Captain Morgan",
        "title": "\"Axe-Hand\", Marine Captain of Shells Town",
        "skin": "morgan",
        "parts": "morgan",
        "weapon": "grandline:morgans_axe",
        "stats": {"health": 260, "attack": 9, "armor": 8, "speed": 0.27, "scale": 1.2, "dodge": 0.0, "guard": 0.25,
                  "knockback_resistance": 0.8},
        "bar_color": "white",
        "faction": "grandline:marine",
        "aggressive": False,
        "phases": [
            {"name": "Captain of the Base", "moves": {"cleave": 4, "jaw_bash": 3, "charge": 2, "slam": 1}, "pace": 1.0},
            {"threshold": 0.6, "name": "Axe-Hand", "pace": 1.15,
             "moves": {"cleave": 3, "slam": 3, "charge": 2, "jaw_bash": 2, "reinforcements": 2},
             "line": "Rank is everything! Kneel before your captain!"},
            {"threshold": 0.3, "name": "The Fallen Captain", "pace": 1.35,
             "moves": {"whirlwind": 3, "charge": 3, "slam": 2, "cleave": 2},
             "line": "I'll cut you down myself, and this whole town with you!"},
        ],
        "rewards": {"bounty": 8_000_000, "renown": 50, "reputation": {"grandline:civilians": 40, "grandline:marine": -25}},
        "lines": {"intro": "You dare raise a hand against Captain Morgan? The penalty is death!",
                  "defeat": "Impossible... I am... a Marine captain..."},
        "loot": [("grandline:morgans_axe", 1, 1), ("minecraft:emerald", 8, 16), ("minecraft:gold_ingot", 3, 6),
                 ("minecraft:iron_ingot", 4, 10)],
    },
    "arlong": {
        "name": "Arlong",
        "title": "\"the Saw\", Captain of the Arlong Pirates",
        "skin": "arlong",
        "parts": "arlong",
        "weapon": "grandline:kiribachi",
        "stats": {"health": 380, "attack": 11, "armor": 10, "speed": 0.3, "scale": 1.3, "dodge": 0.12, "guard": 0.1,
                  "knockback_resistance": 0.9},
        "bar_color": "blue",
        "faction": "grandline:pirate",
        "aggressive": True,
        "phases": [
            {"name": "Fish-Man Pride", "moves": {"bite": 4, "tooth_gun": 2, "shark_on_darts": 1}, "pace": 1.0},
            {"threshold": 0.6, "name": "Kiribachi", "pace": 1.15,
             "moves": {"saw_sweep": 4, "bite": 2, "shark_on_darts": 2, "tooth_gun": 2},
             "line": "An inferior species dares to stand against me? Then feel my Kiribachi!"},
            {"threshold": 0.3, "name": "Fury of the Saw", "pace": 1.4,
             "moves": {"kiribachi_spin": 3, "shark_on_darts": 3, "saw_sweep": 2, "bite": 2},
             "line": "I'll tear this island apart before I let a human take it!"},
        ],
        "rewards": {"bounty": 22_000_000, "renown": 120, "reputation": {"grandline:civilians": 60, "grandline:pirate": -40}},
        "lines": {"intro": "Shahahaha! A lowly human in Arlong Park? You'll learn your place!",
                  "defeat": "A human... beat me...?"},
        "loot": [("grandline:kiribachi", 1, 1), ("minecraft:emerald", 16, 32), ("minecraft:gold_ingot", 6, 12),
                 ("minecraft:diamond", 1, 3), ("minecraft:prismarine_shard", 4, 10)],
    },
    "buggy": {
        "name": "Buggy the Clown",
        "title": "Captain of the Buggy Pirates, of the Chop-Chop Fruit",
        "skin": "buggy",
        "parts": "buggy",
        "weapon": "grandline:buggys_knife",
        "stats": {"health": 300, "attack": 9, "armor": 5, "speed": 0.3, "scale": 1.05, "dodge": 0.15, "guard": 0.0,
                  "knockback_resistance": 0.5},
        "bar_color": "red",
        "faction": "grandline:pirate",
        "aggressive": True,
        "bladeproof": True,
        "phases": [
            {"name": "The Flashy Captain", "moves": {"knife_throw": 3, "bara_bara_cannon": 2, "claw_rake": 3}, "pace": 1.0},
            {"threshold": 0.6, "name": "Chop-Chop", "pace": 1.15,
             "moves": {"bara_bara_cannon": 3, "knife_throw": 2, "claw_rake": 2, "bara_bara_festival": 2},
             "line": "You're looking at my nose, aren't you?!"},
            {"threshold": 0.3, "name": "Buggy Ball", "pace": 1.3,
             "moves": {"buggy_ball": 2, "bara_bara_festival": 2, "bara_bara_cannon": 3, "knife_throw": 2, "claw_rake": 2},
             "line": "Nobody laughs at my nose! Buggy Ball, fire!"},
        ],
        "rewards": {"bounty": 15_000_000, "renown": 40, "reputation": {"grandline:civilians": 30, "grandline:pirate": -20}},
        "lines": {"intro": "Kyahahaha! You've wandered into the wrong town, fool!", "defeat": "Flashy... to the very end..."},
        "loot": [("grandline:buggys_knife", 1, 1), ("minecraft:emerald", 8, 16), ("minecraft:gold_ingot", 3, 6),
                 ("minecraft:golden_apple", 1, 1)],
    },
    "kuro": {
        "name": "Captain Kuro",
        "title": "\"of a Hundred Plans\", Captain of the Black Cat Pirates",
        "skin": "kuro",
        "parts": "kuro",
        "weapon": "grandline:cat_claws",
        "stats": {"health": 320, "attack": 10, "armor": 4, "speed": 0.34, "scale": 1.1, "dodge": 0.25, "guard": 0.0,
                  "knockback_resistance": 0.4},
        "bar_color": "purple",
        "faction": "grandline:pirate",
        "aggressive": False,
        "phases": [
            {"name": "Klahadore the Butler", "moves": {"claw_rake": 4, "silent_step": 2}, "pace": 1.0},
            {"threshold": 0.6, "name": "Captain Kuro", "pace": 1.2,
             "moves": {"claw_rake": 3, "silent_step": 3, "shakushi": 1},
             "line": "Three years of planning, and you would ruin it?"},
            {"threshold": 0.3, "name": "Out of Bounds", "pace": 1.4,
             "moves": {"shakushi": 3, "silent_step": 3, "claw_rake": 2},
             "line": "Then I will cut down everything in sight."},
        ],
        "rewards": {"bounty": 16_000_000, "renown": 45, "reputation": {"grandline:civilians": 30, "grandline:pirate": -20}},
        "lines": {"intro": "I am a butler, nothing more... and you have seen too much.", "defeat": "My plan... was flawless..."},
        "loot": [("grandline:cat_claws", 1, 1), ("minecraft:emerald", 10, 18), ("minecraft:gold_ingot", 3, 6),
                 ("minecraft:diamond", 1, 2)],
    },
    "lord_of_the_coast": {
        "name": "Lord of the Coast",
        "title": "Sea King of the East Blue",
        "body": "sea_king",
        "skin": "",
        "parts": "sea_king",
        "stats": {"health": 420, "attack": 13, "armor": 8, "speed": 1.0, "scale": 2.2, "dodge": 0.0, "guard": 0.0,
                  "knockback_resistance": 1.0},
        "bar_color": "purple",
        "aggressive": True,
        "phases": [
            {"name": "Rising from the Deep", "moves": {"sea_bite": 4, "tail_sweep": 2, "water_jet": 2}, "pace": 1.0},
            {"threshold": 0.6, "name": "Hunger", "pace": 1.15,
             "moves": {"sea_bite": 3, "water_jet": 2, "capsize": 2, "dive": 2, "tail_sweep": 2}},
            {"threshold": 0.3, "name": "Frenzy", "pace": 1.35,
             "moves": {"dive": 3, "water_jet": 3, "sea_bite": 3, "capsize": 2, "tail_sweep": 2}},
        ],
        "rewards": {"bounty": 0, "renown": 80, "reputation": {"grandline:civilians": 10}},
        "lines": {},
        "loot": [("minecraft:heart_of_the_sea", 1, 1), ("minecraft:nautilus_shell", 2, 4), ("minecraft:prismarine_shard", 8, 16),
                 ("minecraft:emerald", 10, 20), ("minecraft:cooked_cod", 12, 24)],
    },
}


def known_moves():
    with open(MOVES_JAVA) as f:
        return set(re.findall(r'new BossMove\("([a-z_]+)"', f.read()))


def definitions():
    moves = known_moves()
    os.makedirs(BOSSES_DIR, exist_ok=True)
    lang_path = ROOT + "/assets/grandline/lang/en_us.json"
    with open(lang_path) as f:
        lang = json.load(f)
    for bid, b in BOSSES.items():
        key = f"boss.grandline.{bid}"
        phases = []
        for i, p in enumerate(b["phases"]):
            unknown = set(p["moves"]) - moves
            assert not unknown, f"{bid}: unknown moves {unknown}"
            phase = {"name": {"translate": f"{key}.phase{i + 1}"}, "moves": p["moves"], "pace": p["pace"]}
            if "threshold" in p:
                phase["threshold"] = p["threshold"]
            if p.get("line"):
                phase["line"] = p["line"]
            phases.append(phase)
            lang[f"{key}.phase{i + 1}"] = p["name"]
        data = {"display_name": {"translate": key}, "title": {"translate": key + ".title"}}
        if b.get("body", "humanoid") != "humanoid":
            data["body"] = b["body"]
        data.update({"skin": b["skin"], "parts": b["parts"]})
        if "weapon" in b:
            data["weapon"] = b["weapon"]
        data.update({"stats": b["stats"], "bar_color": b["bar_color"]})
        if "faction" in b:
            data["faction"] = b["faction"]
        data["aggressive"] = b["aggressive"]
        if b.get("bladeproof"):
            data["bladeproof"] = True
        data.update({"phases": phases, "rewards": b["rewards"], "lines": b["lines"]})
        with open(f"{BOSSES_DIR}/{bid}.json", "w") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        lang[key] = b["name"]
        lang[key + ".title"] = b["title"]
    lang["entity.grandline.boss"] = "Boss"
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


def loot():
    os.makedirs(LOOT_DIR, exist_ok=True)
    for bid, b in BOSSES.items():
        pools = []
        for item, lo, hi in b["loot"]:
            entry = {"type": "minecraft:item", "name": item}
            if hi > 1:
                entry["functions"] = [{"function": "minecraft:set_count",
                                       "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
            pools.append({"rolls": 1, "entries": [entry]})
        table = {"type": "minecraft:entity", "pools": pools, "random_sequence": f"grandline:entities/boss/{bid}"}
        with open(f"{LOOT_DIR}/{bid}.json", "w") as f:
            json.dump(table, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    definitions()
    loot()
    print(f"{len(BOSSES)} bosses")
