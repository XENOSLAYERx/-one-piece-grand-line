# One Piece: Grand Line

A One Piece RPG sandbox mod for **Minecraft Java 1.21.1** on **NeoForge 21.1**: Devil Fruits, Haki, fighting
styles, ships, islands, factions, bosses and crews. Built for both RPG players (earn everything) and creators
(unlock anything instantly).

## Status

| Milestone | State |
| --- | --- |
| 1. Core framework: registries, config, permission tiers, `/onepiece help` and `debug` | Done |
| 2. Player data: attachments, sync, persistence through death/relog/restart | Done |
| 3. Ability framework: keybinds, cooldowns, stamina, HUD, ability menu, body animations | Done |
| 4. Devil Fruits: all 139 canon fruits (12 Logia, 90 Paramecia, 17 Zoan, 9 Ancient Zoan, 11 Mythical Zoan) with mastery and awakening | Done |
| 5. Haki: Observation, Armament, Conqueror's | Done |
| 6. Combat: combos, heavy/air/dash hits, dodge, guard and parry; fighting styles and Rokushiki | Done |
| 7. Weapons (including the legendary swords) and Seastone | Done |
| 8. Races, with Mink Electro and Sulong | Done |
| 9. Creator Menu, unlock everything, loadouts, battle test | Done |
| 10. Ships: sail, fire broadsides, take damage, board, repair | Done |
| 11. Islands of the East Blue rising from the sea | Done |
| 12. Islanders and factions: patrols, reactions, trade | Done |
| 13. Bounties, bounty hunters and crews | Done |
| 14. Bosses fought in phases | Done |
| 15. Quests and the Log Pose | Done |
| 16. Content: more islands and bosses, a Sea King, world events, arenas | Done |
| 17. Polish and multiplayer pass | In progress |

## Playing

| Key | Action |
| --- | --- |
| `Z` `X` `C` `V` `G` | Ability slots 1-5 (press, hold or toggle depending on the ability) |
| `R` | Ability menu: pick what goes in each slot |
| `Left Alt` | Dodge in the direction you are moving |
| `B` (hold) | Guard; time it as a blow lands to parry |
| `K` | Quest journal and the islands your Log Pose has recorded |
| `U` | Guide: your profile, and every fruit, Haki, style, race, weapon, ship, faction, boss and island, with how to get it |
| `H` / `J` | Aboard a ship: fire a broadside / Coup de Burst |
| `N` | Creator Menu (players with the CREATOR permission) |

All keys can be rebound under Controls > One Piece: Grand Line.

The **Guide** (`U` or `/onepiece guide`) is the in-game encyclopedia: your profile (race, faction, bounty, crew,
fruit, Haki training, mastery, journey) beside your character, then every system's pages with what each thing does,
what it costs, what counters it and how to get it in survival play, read from the game's own recipes, merchants and
bosses. Creators also see buttons that grant each thing at once.

**Devil Fruits** are hidden in island treasure (buried treasure holds one about 1 time in 12) and eaten (one per
body; a second one kills). Using a fruit trains its mastery (0-100), which
unlocks stronger techniques; at 100 the fruit awakens. Any water drains fruit powers, and so does Seastone.

Every fruit has five techniques and two awakened ones, designed from what it does in canon. Logia users turn
intangible (Armament Haki and Seastone still hit them). Zoan users switch between their human, hybrid and full beast
bodies, each with its own model, movement and stats; Mythical Zoans carry their legend's power (the Phoenix's
regeneration, Yamata no Orochi's eight lives, the Nine-Tailed Fox's perfect disguises).

**Haki** awakens through fighting: land enough blows and Armament awakens, take enough and Observation does.
Conqueror's Haki is something you are born with (a small chance per player, set in the config). Using Haki raises
its level, which widens Observation, strengthens Armament and lets Conqueror's floor tougher foes.

| Haki | Techniques |
| --- | --- |
| Observation | Sense presences through walls (hostiles outlined red) and dodge attacks; **Future Sight** (lv 60) dodges every attack for a few seconds and shows where enemies are about to move |
| Armament | Coat your arms: harder blows that hit a Logia's real body; **Full-Body Hardening** (lv 35); **Emission: Internal Destruction** (lv 70) pierces armour |
| Conqueror's | Charge and release a wave that knocks out the weak and intimidates the rest, clashing with other kings; **Conqueror's Infusion** (lv 60) wraps blows in black lightning |

**Fighting styles** (Brawler, Black Leg, Fish-Man Karate, Swordsman with up to three swords, Polearm, Gunslinger)
and the six **Rokushiki** are learned from technique scrolls. **Weapons** run from cutlasses and muskets to the
legendary blades (Wado Ichimonji, Shusui, Enma, Kikoku, Yoru and more), each with its own special; Seastone cuffs,
nets, bullets and the jitte suppress Devil Fruit users; the legendary blades turn up very rarely in buried treasure,
with Sea Kings and with veteran bounty hunters. **Races**: Human, Fish-Man, Mink (Electro, and Sulong under the full
moon), Skypiean, Longarm, Longleg and Giant, each with its own traits and techniques. Everyone starts Human and may
choose their race once, in the Guide or with `/onepiece race choose`.

**Ships** come in bottles. Rafts, sloops, caravels and galleons are crafted (sloops and caravels are sometimes dug
up with buried treasure); the Marine warship, Revolutionary ship and Thousand Sunny are, for now, given by creators.
Steer from the helm, load the hold with cannonballs and fire broadsides; hulls take damage, burn, are repaired with
planks, and sink.

**The East Blue** rises out of open ocean as islands, each with its own town and people: Dawn Island's windmill
village, Shells Town under its Marine base, Orange Town under Buggy's circus tent, Syrup Village below Kaya's
mansion, and the Conomi Islands in the shadow of Arlong Park. Islanders patrol, trade, flee and fight; how they treat
you depends on your faction (Pirates, Marines, Revolutionaries, Bounty Hunters) and on your standing, which your
deeds raise and lower. Once a faction trusts you (standing 30) you may join it with `/onepiece faction join`; the
Marines take no one with a bounty, and walking out costs you their trust.

**Bounties** rise with crimes against the Marines and the townsfolk, and the bigger yours grows the stronger the
bounty hunters and Marine squads that come for you. Found a **crew**, fly your own Jolly Roger, claim a ship and
recruit islanders.

**Bosses** fight in three phases with telegraphed moves you can read and punish, and drop their signature weapons:
Captain "Axe-Hand" Morgan, Buggy the Clown (blades pass through his Chop-Chop body; Haki doesn't), Captain Kuro,
Arlong the Saw, and the Lord of the Coast, a Sea King that surfaces beside ships at sea. A boss grows tougher when
fought by a party, everyone who fought it shares the credit, and island bosses return to their posts after a while.

**Quests** tell the East Blue story chapter by chapter, from Romance Dawn to Toward the Grand Line, alongside side
quests the islanders offer. Chapters grant **titles**, worn before your name in chat and in the player list
(`/onepiece title`). The **Log Pose** records every island you reach and its needle points the way to the
next one, whose name stays a mystery until you arrive.

**World events** befall players now and then: pirate, Marine and Fish-Man raids, bands of bounty hunters, and Sea
Kings at sea. Creators can mark out **arenas** where players fight round after round of ever harder enemies.

## Commands

Everything lives under `/onepiece` with tab-completion. `/onepiece help` lists only what your tier can run.
A few highlights:

```text
/onepiece guide                           the Guide (anyone)
/onepiece race choose <race>              choose your race, once (anyone)
/onepiece faction join <faction> | leave  join a faction that trusts you (anyone)
/onepiece title [wear <title> | clear]    wear a title you have earned (anyone)
/onepiece menu                            the Creator Menu (every system, with unlock/equip/test buttons)
/onepiece unlock all                      everything, max level
/onepiece fruit set <fruit>               gain a fruit's power now
/onepiece haki set <type> <0-100>         awaken a Haki at a level
/onepiece mastery max                     max fruit (awakens it) and Haki
/onepiece cooldown off | stamina infinite | haki infinite | invincible on
/onepiece battle random <easy|...|legendary>   a wave of enemies around you
/onepiece boss spawn <boss|random>        /onepiece boss move <move> | phase <n> | respawn
/onepiece teleport <island>               /onepiece island place <island>
/onepiece quest [list|accept|complete|reset]   /onepiece logpose [next|set|record]
/onepiece event start <event>             /onepiece arena create|join|next
/onepiece crew create <name>              /onepiece bounty set <amount>
/onepiece reset <race|fruit|haki|faction|bounty|mastery|all> confirm
```

## Configuration

`<world>/serverconfig/grandline-server.toml` holds the server's settings: permission tiers and creator UUIDs,
combat and ability balance, Haki training, bounty gain and bounty hunters, crew size and friendly fire, how often
world events happen, and how long island bosses take to return.

## Building

```sh
./gradlew build              # jar ends up in build/libs/
./gradlew runClient          # dev client
./gradlew runGameTestServer  # headless server running the automated GameTests
```

Every push runs `build`, the GameTests and a scripted client smoke test (`ci/autotest.txt`) on GitHub Actions.
The jar is attached to each run as the `grandline-jar` artifact; the smoke test's screenshots are published to
the `ci-screenshots` branch.

## Layout

| Path | What lives there |
| --- | --- |
| `src/main/java/com/nerodev/grandline/` | Mod code (`GrandLine` is the entry point, `client/` is client-only) |
| `src/main/resources/data/grandline/grandline/<registry>/` | JSON content definitions: `race`, `faction`, `ship`, `npc`, `boss`, `quest` |
| `src/main/resources/assets/grandline/grandline_animations/` | Keyframed body animations |
| `tools/` | Generators for the mod's original textures, models, sounds and data (`gen_*.py`) |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata (values come from `gradle.properties`) |
| `<world>/serverconfig/grandline-server.toml` | Per-world server config (permissions, balance, Haki training) |

## Permission tiers

`PLAYER < MODERATOR < ADMIN < CREATOR`. Defaults map to vanilla op levels 2 / 3 / 4. Players listed in
`creatorUuids` are always CREATOR. All checks run server-side.
