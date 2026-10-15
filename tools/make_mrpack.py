#!/usr/bin/env python3
"""Packs the built mod into a Modrinth modpack (.mrpack) for NeoForge.

Run from the repository root after `./gradlew build`:

    python3 tools/make_mrpack.py [out_dir]

The pack names Minecraft and the NeoForge version from gradle.properties, so launchers that import .mrpack files
(Modrinth App, Prism Launcher, ATLauncher...) install the right loader, and carries the mod jar itself in
overrides/mods.
"""
import glob
import json
import os
import sys
import zipfile


def properties(path="gradle.properties"):
    props = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                props[key.strip()] = value.strip()
    return props


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "build/mrpack"
    props = properties()
    mod_id = props["mod_id"]
    version = props["mod_version"]
    jars = [j for j in glob.glob(f"build/libs/{mod_id}-*.jar") if not j.endswith(("-sources.jar", "-javadoc.jar"))]
    if len(jars) != 1:
        sys.exit(f"Expected exactly one mod jar in build/libs, found {jars}")
    jar = jars[0]
    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": props["mod_name"],
        "summary": "Devil Fruits, Haki, fighting styles, ships, islands, bosses and the East Blue story for NeoForge "
        + props["minecraft_version"],
        "files": [],
        "dependencies": {
            "minecraft": props["minecraft_version"],
            "neoforge": props["neo_version"],
        },
    }
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, f"one-piece-grand-line-{version}.mrpack")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as pack:
        pack.writestr("modrinth.index.json", json.dumps(index, indent=2, ensure_ascii=False) + "\n")
        pack.write(jar, f"overrides/mods/{os.path.basename(jar)}")
    print(out)


if __name__ == "__main__":
    main()
