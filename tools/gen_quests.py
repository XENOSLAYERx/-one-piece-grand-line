#!/usr/bin/env python3
"""Generates the quests: the East Blue story chain and the islanders' side quests, with their names.

Run from the repository root: python3 tools/gen_quests.py

Writes
  src/main/resources/data/grandline/grandline/quest/<id>.json   the quest (see QuestDefinition)
  quest.grandline.* entries in assets/grandline/lang/en_us.json
Givers, bosses, islands and items named here are checked against the mod's data.
"""
import json
import os

ROOT = "src/main/resources"
QUESTS = ROOT + "/data/grandline/grandline/quest"
NPCS = ROOT + "/data/grandline/grandline/npc"
BOSSES = ROOT + "/data/grandline/grandline/boss"
FACTIONS = ROOT + "/data/grandline/grandline/faction"
ITEM_MODELS = ROOT + "/assets/grandline/models/item"
ISLANDS = {"dawn_island", "shells_town", "conomi_islands"}


def obj(kind, target=None, count=1, consume=False):
    o = {"type": kind}
    if target:
        o["target"] = target
    if count != 1:
        o["count"] = count
    if consume:
        o["consume"] = True
    return o


def items(*pairs):
    return [{"id": i, "count": c} for i, c in pairs]


QUEST_LIST = {
    # ---- The East Blue story -------------------------------------------------------------------------------------
    "romance_dawn": {
        "name": "Romance Dawn",
        "desc": "Every great pirate began with one step off the shore. Get a ship of your own and set out to sea.",
        "category": "main", "order": 1,
        "objectives": [obj("sail")],
        "rewards": {"items": items(("grandline:log_pose", 1), ("minecraft:emerald", 5))},
        "lines": {"done": "The sea is yours now. Hold up your Log Pose and let it find the pull of the nearest island."},
    },
    "land_ho": {
        "name": "Land Ho!",
        "desc": "The Log Pose's needle trembles towards an island no chart of yours shows. Follow it and make landfall.",
        "category": "main", "order": 2, "requires": ["romance_dawn"],
        "objectives": [obj("discover", count=1)],
        "rewards": {"items": items(("minecraft:emerald", 8), ("minecraft:bread", 8))},
        "lines": {"done": "Each island you reach is recorded. Islanders talk; some of them will have work for you."},
    },
    "the_marine_base": {
        "name": "The Marine Base",
        "desc": "In Shells Town a Marine captain named Morgan rules by fear, and the townsfolk dare not even say his name. "
                "Find the town and end his rule.",
        "category": "main", "order": 3, "requires": ["land_ho"], "island": "shells_town",
        "objectives": [obj("discover", "grandline:shells_town"), obj("talk", "grandline:townsperson"),
                       obj("defeat_boss", "grandline:morgan")],
        "rewards": {"items": items(("minecraft:emerald", 12), ("minecraft:iron_ingot", 8)), "renown": 30,
                    "reputation": {"grandline:civilians": 10}},
        "lines": {"done": "Shells Town is free. Word of what you did will travel - to the Fish-Man pirates' ears, too."},
    },
    "arlong_park": {
        "name": "Arlong Park",
        "desc": "Fish-Man pirates hold the Conomi Islands, taxing every villager for the right to live. A tangerine farmer "
                "there knows the way into Arlong Park.",
        "category": "main", "order": 4, "requires": ["the_marine_base"], "island": "conomi_islands",
        "objectives": [obj("discover", "grandline:conomi_islands"), obj("talk", "grandline:tangerine_farmer"),
                       obj("defeat_npc", "grandline:fishman_pirate", 5), obj("defeat_boss", "grandline:arlong")],
        "rewards": {"items": items(("minecraft:emerald", 24), ("minecraft:diamond", 2), ("grandline:tangerine", 16)), "renown": 60,
                    "reputation": {"grandline:civilians": 20}},
        "lines": {"done": "Arlong Park has fallen. The Conomi Islands are free, and your bounty poster is on every wall."},
    },
    "toward_the_grand_line": {
        "name": "Toward the Grand Line",
        "desc": "Your name is known across the East Blue. Chart every island it holds, and earn a bounty worthy of the "
                "Grand Line.",
        "category": "main", "order": 5, "requires": ["arlong_park"],
        "objectives": [obj("discover", count=3), obj("bounty", count=30_000_000)],
        "rewards": {"items": items(("minecraft:gold_ingot", 16), ("minecraft:emerald", 32)), "renown": 100},
        "lines": {"done": "The East Blue is behind you. The Grand Line's charts are still being drawn - its islands await."},
    },
    # ---- Side quests from islanders -----------------------------------------------------------------------------------
    "partys_bar_supplies": {
        "name": "Partys Bar Supplies",
        "desc": "The bar is out of flour and the regulars are getting restless. Bring wheat for the kitchen.",
        "giver": "grandline:barkeeper", "island": "dawn_island", "order": 1,
        "objectives": [obj("collect", "minecraft:wheat", 12, consume=True)],
        "rewards": {"items": items(("minecraft:emerald", 4), ("minecraft:bread", 6)), "reputation": {"grandline:civilians": 5}},
        "lines": {"offer": "Could you spare some wheat? The whole village eats here, and the pantry's bare.",
                  "done": "Bless you! Come back any time - the first drink's on the house."},
    },
    "sea_bandits": {
        "name": "Sea Bandits",
        "desc": "Pirates have been raiding the villages along the coast. Drive some of them off.",
        "giver": "grandline:villager", "order": 2,
        "objectives": [obj("defeat_faction", "grandline:pirate", 3)],
        "rewards": {"items": items(("minecraft:emerald", 6)), "reputation": {"grandline:civilians": 8}},
        "lines": {"offer": "Pirates again, coming ashore at night... Would you teach them a lesson?",
                  "done": "They won't be back soon. Thank you, truly."},
    },
    "fish_for_the_town": {
        "name": "Fish for the Town",
        "desc": "With Marines watching the docks, no one in Shells Town dares go fishing. Bring the town a catch.",
        "giver": "grandline:townsperson", "island": "shells_town", "order": 3,
        "objectives": [obj("collect", "minecraft:cod", 10, consume=True)],
        "rewards": {"items": items(("minecraft:emerald", 6)), "reputation": {"grandline:civilians": 6}},
        "lines": {"offer": "We haven't had fresh fish in weeks. The captain's men take everything we land.",
                  "done": "Fish for dinner! You're a kind soul, stranger."},
    },
    "marine_drills": {
        "name": "Marine Drills",
        "desc": "The garrison's lieutenant wants pirates kept away from the base. Defeat pirates to prove yourself.",
        "giver": "grandline:marine_lieutenant", "island": "shells_town", "order": 4,
        "standing": {"grandline:marine": 0},
        "objectives": [obj("defeat_faction", "grandline:pirate", 6)],
        "rewards": {"items": items(("minecraft:emerald", 10), ("minecraft:iron_ingot", 8)), "reputation": {"grandline:marine": 25}},
        "lines": {"offer": "Civilian volunteers are rare. Bring down some pirates and the Marines will remember it.",
                  "done": "Fine work. Justice thanks you - consider joining up."},
    },
    "cargo_for_the_grand_line": {
        "name": "Cargo for the Grand Line",
        "desc": "A travelling merchant is outfitting a ship bound for the Grand Line and needs iron for her fittings.",
        "giver": "grandline:travelling_merchant", "order": 5,
        "objectives": [obj("collect", "minecraft:iron_ingot", 10, consume=True)],
        "rewards": {"items": items(("grandline:cannonball", 16), ("minecraft:emerald", 4))},
        "lines": {"offer": "Iron, friend - I need iron. Ten bars, and I'll pay you in shot for your cannons.",
                  "done": "Pleasure doing business. May the winds be kind to you."},
    },
    "tangerine_harvest": {
        "name": "Tangerine Harvest",
        "desc": "The grove's trees are heavy with fruit, and the tribute to Arlong is due. Help bring in the harvest.",
        "giver": "grandline:tangerine_farmer", "island": "conomi_islands", "order": 6,
        "objectives": [obj("collect", "grandline:tangerine", 16, consume=True)],
        "rewards": {"items": items(("minecraft:emerald", 8)), "reputation": {"grandline:civilians": 6}},
        "lines": {"offer": "These tangerines are all we have. Will you help me pick them before the Fish-Men come to collect?",
                  "done": "That's the whole harvest in. Here - you've earned more than a tangerine."},
    },
}


def check(qid, q):
    def exists(folder, rl):
        ns, path = rl.split(":")
        return os.path.exists(f"{folder}/{path}.json") if ns == "grandline" else True
    for req in q.get("requires", []):
        assert req in QUEST_LIST, f"{qid}: requires unknown quest {req}"
    if "giver" in q:
        assert exists(NPCS, q["giver"]), f"{qid}: unknown giver {q['giver']}"
    for o in q["objectives"]:
        t = o.get("target")
        if o["type"] in ("defeat_npc", "talk"):
            assert exists(NPCS, t), f"{qid}: unknown islander {t}"
        if o["type"] == "defeat_boss":
            assert exists(BOSSES, t), f"{qid}: unknown boss {t}"
        if o["type"] == "defeat_faction":
            assert exists(FACTIONS, t), f"{qid}: unknown faction {t}"
        if o["type"] == "discover" and t:
            assert t.split(":")[1] in ISLANDS, f"{qid}: unknown island {t}"
        if o["type"] == "collect":
            assert exists(ITEM_MODELS, t), f"{qid}: unknown item {t}"
    for it in q.get("rewards", {}).get("items", []):
        assert exists(ITEM_MODELS, it["id"]), f"{qid}: unknown reward {it['id']}"


def main():
    os.makedirs(QUESTS, exist_ok=True)
    lang_path = ROOT + "/assets/grandline/lang/en_us.json"
    with open(lang_path) as f:
        lang = json.load(f)
    for qid, q in QUEST_LIST.items():
        check(qid, q)
        key = f"quest.grandline.{qid}"
        data = {"display_name": {"translate": key}, "description": {"translate": key + ".desc"},
                "category": q.get("category", "side"), "order": q.get("order", 0)}
        for field in ("giver", "island", "standing"):
            if field in q:
                data[field] = q[field]
        if "requires" in q:
            data["requires"] = ["grandline:" + req for req in q["requires"]]
        data["objectives"] = q["objectives"]
        if "rewards" in q:
            data["rewards"] = q["rewards"]
        if "lines" in q:
            data["lines"] = q["lines"]
        with open(f"{QUESTS}/{qid}.json", "w") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        lang[key] = q["name"]
        lang[key + ".desc"] = q["desc"]
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{len(QUEST_LIST)} quests")


if __name__ == "__main__":
    main()
