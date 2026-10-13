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
| 6+. Combat, weapons and Seastone, races, creator menu, ships, islands, NPCs, bounty, bosses, quests | Planned |

## Playing

| Key | Action |
| --- | --- |
| `Z` `X` `C` `V` `G` | Ability slots 1-5 (press, hold or toggle depending on the ability) |
| `R` | Ability menu: pick what goes in each slot |

All keys can be rebound under Controls > One Piece: Grand Line.

**Devil Fruits** are eaten (one per body; a second one kills). Using a fruit trains its mastery (0-100), which
unlocks stronger techniques; at 100 the fruit awakens. Any water drains fruit powers.

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

## Commands

Everything lives under `/onepiece` with tab-completion. `/onepiece help` lists only what your tier can run.
A few highlights for creators:

```text
/onepiece unlock all                     everything, max level
/onepiece fruit set <fruit>              gain a fruit's power now
/onepiece haki set <type> <0-100>        awaken a Haki at a level
/onepiece mastery max                    max fruit (awakens it) and Haki
/onepiece cooldown off | stamina infinite | haki infinite | invincible on
/onepiece reset <race|fruit|haki|faction|bounty|mastery|all> confirm
```

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
| `src/main/resources/data/grandline/grandline/<registry>/` | JSON content definitions (`race`, `faction`) |
| `src/main/resources/assets/grandline/grandline_animations/` | Keyframed body animations |
| `tools/` | Generators for the mod's original textures and sounds |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata (values come from `gradle.properties`) |
| `<world>/serverconfig/grandline-server.toml` | Per-world server config (permissions, balance, Haki training) |

## Permission tiers

`PLAYER < MODERATOR < ADMIN < CREATOR`. Defaults map to vanilla op levels 2 / 3 / 4. Players listed in
`creatorUuids` are always CREATOR. All checks run server-side.
