# One Piece: Grand Line

A One Piece RPG sandbox mod for **Minecraft Java 1.21.1** on **NeoForge 21.1**: Devil Fruits, Haki, fighting
styles, ships, islands, factions, bosses and crews. Built for both RPG players (earn everything) and creators
(unlock anything instantly).

## Status

Milestone 1 — core framework: mod skeleton, data-driven registries, server config, permission tiers,
`/onepiece help` and `/onepiece debug`.

## Building

```sh
./gradlew build              # jar ends up in build/libs/
./gradlew runClient          # dev client
./gradlew runGameTestServer  # headless server running the automated GameTests
```

Every push runs both `build` and `runGameTestServer` on GitHub Actions. The jar is attached to each run as the
`grandline-jar` artifact.

## Layout

| Path | What lives there |
| --- | --- |
| `src/main/java/com/nerodev/grandline/` | Mod code (`GrandLine` is the entry point) |
| `src/main/resources/data/grandline/grandline/<registry>/` | JSON content definitions (`devil_fruit`, `race`, `faction`) |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata (values come from `gradle.properties`) |
| `<world>/serverconfig/grandline-server.toml` | Per-world server config (permission tiers) |

## Permission tiers

`PLAYER < MODERATOR < ADMIN < CREATOR`. Defaults map to vanilla op levels 2 / 3 / 4. Players listed in
`creatorUuids` are always CREATOR. All checks run server-side.
