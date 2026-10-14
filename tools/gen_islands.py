#!/usr/bin/env python3
"""Generates the island data (structures, structure set, tags, loot tables), the tangerine assets and lang lines.

Run from the repository root: python3 tools/gen_islands.py
"""
import json
import os
import random

from PIL import Image

ROOT = "src/main/resources"
DATA = ROOT + "/data/grandline"
ASSETS = ROOT + "/assets/grandline"

ISLANDS = {
    "dawn_island": ("Dawn Island", "The windmill village of Foosha, Partys Bar and Mt. Colubo's forests, where a certain rubber boy grew up."),
    "shells_town": ("Shells Town", "A harbour town under the walls of the 153rd Marine branch, and the stake in its yard."),
    "conomi_islands": ("Conomi Islands", "Cocoyasi's villas and tangerine groves, in the shadow of Arlong Park."),
    "orange_town": ("Orange Town", "A port town emptied by the Buggy Pirates, whose captain holds court in the tavern under a circus tent."),
    "syrup_village": ("Syrup Village", "A quiet village of the Gecko Islands, a mansion on its hill and a slope down to the sea."),
}

OCEANS = ["minecraft:ocean", "minecraft:deep_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean",
          "minecraft:warm_ocean", "minecraft:cold_ocean", "minecraft:deep_cold_ocean"]


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def item(name, weight, lo=1, hi=1, functions=None):
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    fns = list(functions or [])
    if hi > 1:
        fns.insert(0, {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if fns:
        entry["functions"] = fns
    return entry


# A random Devil Fruit or legendary blade is drawn by a nested table: an expanded tag straight in a chest's pool would
# give every one of its items the entry's weight, so a pool meant to hold a fruit 1 time in 12 would almost always
# hold one.
LEGENDARY_BLADES = ["sandai_kitetsu", "wado_ichimonji", "shusui", "enma", "kikoku", "gryphon", "yoru", "murakumogiri"]


def pick_one(name, tag):
    write(f"{DATA}/loot_table/gameplay/{name}.json", {
        "type": "minecraft:generic",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:tag", "name": tag, "expand": True}]}],
        "random_sequence": f"grandline:gameplay/{name}",
    })


def fruit_pool(one_in):
    """A pool that holds a random Devil Fruit about one time in {one_in}, scaled by the server's fruitFindMultiplier."""
    return (1, 1, [{"type": "minecraft:loot_table", "value": "grandline:gameplay/devil_fruit",
                    "conditions": [{"condition": "grandline:fruit_chance", "chance": round(1 / one_in, 5)}]}])


def legendary_blade(weight):
    return {"type": "minecraft:loot_table", "value": "grandline:gameplay/legendary_blade", "weight": weight}


def ship(kind, weight):
    return item("grandline:ship_in_a_bottle", weight,
                functions=[{"function": "minecraft:set_components", "components": {"grandline:ship_type": "grandline:" + kind}}])


def table(name, pools):
    write(f"{DATA}/loot_table/chests/{name}.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries} for lo, hi, entries in pools],
        "random_sequence": f"grandline:chests/{name}",
    })


def loot_tables():
    pick_one("devil_fruit", "grandline:devil_fruits")
    pick_one("legendary_blade", "grandline:legendary_blades")
    table("village_house", [
        (3, 6, [
            item("minecraft:bread", 10, 1, 4),
            item("minecraft:apple", 8, 1, 3),
            item("grandline:tangerine", 6, 1, 4),
            item("minecraft:wheat", 6, 2, 6),
            item("minecraft:potato", 5, 1, 4),
            item("minecraft:carrot", 5, 1, 4),
            item("minecraft:coal", 4, 1, 4),
            item("minecraft:stick", 4, 2, 6),
            item("minecraft:string", 3, 1, 3),
            item("minecraft:iron_nugget", 3, 2, 6),
            item("minecraft:emerald", 2, 1, 2),
            item("minecraft:oak_sapling", 2, 1, 2),
            item("minecraft:feather", 2, 1, 3),
        ]),
    ])
    table("partys_bar", [
        (4, 8, [
            item("minecraft:cooked_beef", 10, 1, 4),
            item("minecraft:cooked_porkchop", 8, 1, 3),
            item("minecraft:bread", 8, 1, 4),
            item("minecraft:milk_bucket", 4),
            item("minecraft:honey_bottle", 5, 1, 2),
            item("minecraft:glass_bottle", 6, 1, 4),
            item("minecraft:sugar", 5, 1, 4),
            item("minecraft:sweet_berries", 6, 2, 6),
            item("minecraft:cake", 1),
            item("minecraft:emerald", 3, 1, 3),
        ]),
    ])
    table("marine_armory", [
        (3, 6, [
            item("grandline:iron_bullet", 10, 4, 16),
            item("grandline:cannonball", 8, 2, 8),
            item("minecraft:gunpowder", 8, 2, 6),
            item("minecraft:iron_ingot", 6, 1, 4),
            item("grandline:cutlass", 4),
            item("grandline:flintlock", 3),
            item("grandline:musket", 2),
            item("minecraft:iron_chestplate", 2),
            item("minecraft:shield", 3),
            item("grandline:seastone_bullet", 2, 2, 6),
            item("grandline:seastone_cuffs", 1),
            item("grandline:raw_seastone", 2, 1, 2),
        ]),
        (1, 1, [
            item("minecraft:bread", 4, 1, 3),
            item("minecraft:paper", 3, 1, 4),
            item("grandline:swordsman_scroll", 1),
            item("grandline:gunslinger_scroll", 1),
            item("grandline:soru_scroll", 1),
        ]),
    ])
    table("arlong_hoard", [
        (4, 8, [
            item("minecraft:gold_ingot", 10, 2, 6),
            item("minecraft:gold_nugget", 10, 4, 16),
            item("minecraft:emerald", 8, 2, 6),
            item("minecraft:diamond", 3, 1, 2),
            item("minecraft:map", 4),
            item("minecraft:prismarine_crystals", 5, 2, 6),
            item("minecraft:nautilus_shell", 2),
            item("grandline:tangerine", 6, 2, 6),
            item("grandline:kanabo", 1),
            item("grandline:fishman_karate_scroll", 2),
        ]),
        fruit_pool(20),
    ])
    table("buggy_stash", [
        (4, 8, [
            item("minecraft:gold_ingot", 10, 2, 6),
            item("minecraft:gold_nugget", 10, 4, 16),
            item("minecraft:emerald", 8, 2, 5),
            item("minecraft:gunpowder", 8, 2, 8),
            item("grandline:cannonball", 6, 2, 8),
            item("minecraft:map", 3),
            item("minecraft:red_dye", 4, 1, 3),
            item("minecraft:firework_rocket", 3, 1, 3),
            item("grandline:cutlass", 2),
        ]),
        fruit_pool(24),
    ])
    table("kaya_mansion", [
        (3, 6, [
            item("minecraft:book", 10, 1, 3),
            item("minecraft:paper", 8, 2, 6),
            item("minecraft:emerald", 6, 1, 4),
            item("minecraft:cake", 3),
            item("minecraft:glistering_melon_slice", 3, 1, 2),
            item("minecraft:golden_carrot", 4, 1, 3),
            item("minecraft:candle", 4, 1, 3),
            item("minecraft:gold_ingot", 3, 1, 3),
        ]),
    ])
    # The holds of ships met at sea (Encounter.cargo).
    table("ship_marine_patrol", [
        (3, 6, [
            item("grandline:cannonball", 10, 4, 12),
            item("grandline:iron_bullet", 8, 4, 12),
            item("minecraft:bread", 8, 2, 6),
            item("minecraft:iron_ingot", 6, 2, 5),
            item("grandline:raw_seastone", 4, 1, 3),
            item("grandline:seastone_bullet", 3, 2, 4),
            item("grandline:seastone_cuffs", 1),
            item("minecraft:emerald", 4, 2, 5),
        ]),
    ])
    table("ship_pirate_raider", [
        (4, 7, [
            item("minecraft:gold_ingot", 10, 2, 6),
            item("minecraft:gold_nugget", 8, 4, 16),
            item("minecraft:emerald", 8, 2, 6),
            item("grandline:cannonball", 8, 4, 10),
            item("minecraft:honey_bottle", 4, 1, 3),
            item("grandline:cutlass", 2),
            item("grandline:flintlock", 1),
            item("minecraft:map", 3),
        ]),
        fruit_pool(30),
    ])
    table("ship_merchant", [
        (5, 9, [
            item("minecraft:emerald", 10, 3, 8),
            item("minecraft:gold_ingot", 6, 2, 5),
            item("minecraft:diamond", 2, 1, 2),
            item("minecraft:leather", 6, 2, 6),
            item("minecraft:paper", 6, 3, 9),
            item("minecraft:sugar", 6, 3, 9),
            item("grandline:tangerine", 6, 3, 8),
            item("minecraft:cocoa_beans", 4, 2, 6),
            item("minecraft:white_wool", 5, 2, 6),
            item("minecraft:glass_bottle", 4, 2, 4),
        ]),
    ])
    table("buried_treasure", [
        (4, 7, [
            item("minecraft:gold_ingot", 10, 2, 6),
            item("minecraft:emerald", 8, 2, 6),
            item("minecraft:diamond", 4, 1, 3),
            item("minecraft:iron_ingot", 6, 2, 6),
            item("minecraft:heart_of_the_sea", 1),
            item("grandline:cannonball", 5, 4, 12),
            item("grandline:katana", 2),
            ship("sloop", 2),
            ship("caravel", 1),
        ]),
        fruit_pool(12),
        (1, 1, [
            {"type": "minecraft:empty", "weight": 39},
            legendary_blade(1),
        ]),
    ])


def leaves_loot():
    shears_or_silk = {"condition": "minecraft:any_of", "terms": [
        {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
        {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}},
    ]}
    write(f"{DATA}/loot_table/blocks/tangerine_leaves.json", {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": "grandline:tangerine_leaves", "conditions": [shears_or_silk]},
                {"type": "minecraft:item", "name": "grandline:tangerine", "conditions": [
                    {"condition": "minecraft:survives_explosion"},
                    {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                     "chances": [0.1, 0.125, 0.166, 0.2, 0.25]}]},
            ]}]},
            {"rolls": 1, "bonus_rolls": 0,
             "conditions": [{"condition": "minecraft:inverted", "term": shears_or_silk}],
             "entries": [{"type": "minecraft:item", "name": "minecraft:stick",
                          "conditions": [{"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                                          "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}],
                          "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                                        {"function": "minecraft:explosion_decay"}]}]},
        ],
        "random_sequence": "grandline:blocks/tangerine_leaves",
    })


def worldgen():
    for name in ISLANDS:
        write(f"{DATA}/worldgen/structure/{name}.json", {
            "type": "grandline:island",
            "island": name,
            "biomes": "#grandline:has_structure/island",
            "step": "surface_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "none",
        })
    write(f"{DATA}/worldgen/structure_set/east_blue_islands.json", {
        "structures": [{"structure": "grandline:" + name, "weight": 1} for name in ISLANDS],
        "placement": {"type": "minecraft:random_spread", "spacing": 44, "separation": 20, "salt": 1640813377},
    })
    write(f"{DATA}/tags/worldgen/biome/has_structure/island.json", {"values": OCEANS})
    write(f"{DATA}/tags/worldgen/structure/islands.json", {"values": ["grandline:" + name for name in ISLANDS]})


def tags():
    models = sorted(f[:-5] for f in os.listdir(ASSETS + "/models/item") if f.endswith("_fruit.json"))
    write(f"{DATA}/tags/item/devil_fruits.json", {"values": ["grandline:" + m for m in models]})
    write(f"{DATA}/tags/item/legendary_blades.json", {"values": ["grandline:" + b for b in LEGENDARY_BLADES]})
    write(ROOT + "/data/minecraft/tags/block/leaves.json", {"values": ["grandline:tangerine_leaves"]})
    write(ROOT + "/data/minecraft/tags/item/leaves.json", {"values": ["grandline:tangerine_leaves"]})
    write(ROOT + "/data/minecraft/tags/block/mineable/hoe.json", {"values": ["grandline:tangerine_leaves"]})
    return len(models)


def assets():
    write(f"{ASSETS}/blockstates/tangerine_leaves.json", {"variants": {"": {"model": "grandline:block/tangerine_leaves"}}})
    write(f"{ASSETS}/models/block/tangerine_leaves.json", {
        "parent": "minecraft:block/cube_all",
        "render_type": "minecraft:cutout_mipped",
        "textures": {"all": "grandline:block/tangerine_leaves"},
    })
    write(f"{ASSETS}/models/item/tangerine_leaves.json", {"parent": "grandline:block/tangerine_leaves"})
    write(f"{ASSETS}/models/item/tangerine.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "grandline:item/tangerine"}})

    rng = random.Random(7)
    leaves = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    greens = [(46, 104, 38), (58, 124, 44), (72, 140, 52), (38, 88, 32), (88, 156, 60)]
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.82:
                leaves.putpixel((x, y), greens[rng.randrange(len(greens))] + (255,))
    for cx, cy in [(3, 4), (11, 2), (8, 9), (2, 12), (13, 12), (6, 14)]:
        for dx, dy, shade in [(0, 0, (250, 150, 30)), (1, 0, (236, 128, 22)), (0, 1, (230, 120, 20)), (1, 1, (204, 98, 16))]:
            leaves.putpixel(((cx + dx) % 16, (cy + dy) % 16), shade + (255,))
        leaves.putpixel((cx % 16, (cy - 1) % 16), (255, 196, 90, 255))
    os.makedirs(ASSETS + "/textures/block", exist_ok=True)
    leaves.save(ASSETS + "/textures/block/tangerine_leaves.png")

    fruit = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx = x - 7.5
            dy = y - 8.5
            r = (dx * dx + dy * dy * 1.15) ** 0.5
            if r <= 6.2:
                light = max(0.0, min(1.0, 1.0 - (dx + dy + 6) / 16))
                base = (226, 112, 18)
                hi = (255, 186, 70)
                c = tuple(int(base[i] + (hi[i] - base[i]) * light) for i in range(3))
                if r > 5.4:
                    c = (170, 72, 10)
                if (x * 7 + y * 3) % 11 == 0 and r < 5:
                    c = tuple(min(255, v + 18) for v in c)
                fruit.putpixel((x, y), c + (255,))
    for x, y, c in [(7, 2, (70, 50, 24)), (8, 1, (70, 50, 24)), (9, 2, (70, 140, 40)), (10, 1, (86, 160, 50)), (11, 1, (70, 140, 40)),
                    (10, 2, (58, 120, 36))]:
        fruit.putpixel((x, y), c + (255,))
    os.makedirs(ASSETS + "/textures/item", exist_ok=True)
    fruit.save(ASSETS + "/textures/item/tangerine.png")


def lang():
    path = ASSETS + "/lang/en_us.json"
    with open(path) as f:
        data = json.load(f)
    for name, (title, desc) in ISLANDS.items():
        data["island.grandline." + name] = title
        data["island.grandline." + name + ".desc"] = desc
    data["block.grandline.tangerine_leaves"] = "Tangerine Leaves"
    data["item.grandline.tangerine"] = "Tangerine"
    with open(path, "w") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


if __name__ == "__main__":
    loot_tables()
    leaves_loot()
    worldgen()
    fruits = tags()
    assets()
    lang()
    print(f"islands: {len(ISLANDS)}, devil fruit tag: {fruits} fruits")
