#!/usr/bin/env python3
"""Fills in missing ability names and descriptions in en_us.json.

Scans the fruit sources for ``ability("id", Move::new)`` registrations and takes:
  - the display name from an ``@name Display Name`` line in the move's javadoc, else NAMES, else the id title-cased
    (for Paramecia and Zoan moves, whose ids start with "<fruit id>_", that prefix is left out of the name);
  - the description from the rest of the javadoc above ``class Move``.
Existing keys are never touched, so hand-written text always wins.
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..")
SOURCES = os.path.join(ROOT, "src", "main", "java", "com", "nerodev", "grandline", "fruit")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "grandline", "lang", "en_us.json")

SMALL = {"no", "of", "the", "de", "a", "el", "la", "del"}
NAMES = {
    "el_thor": "El Thor",
    "ama_no_murakumo": "Ama no Murakumo",
    "yasakani_no_magatama": "Yasakani no Magatama",
    "white_out": "White Out",
    "ice_time_capsule": "Ice Time Capsule",
}

REGISTRATION = re.compile(r'ability\("([a-z0-9_]+)",\s*(\w+)::new\)')
CLASS_DOC = r'/\*\*((?:(?!\*/).)*?)\*/\s*public static (?:final )?class {}\b'


def display_name(ability_id, fruit_id=None, doc=None):
    if doc:
        named = re.search(r"@name\s+(.+)", doc)
        if named:
            return named.group(1).strip()
    if ability_id in NAMES:
        return NAMES[ability_id]
    if fruit_id and ability_id.startswith(fruit_id + "_"):
        ability_id = ability_id[len(fruit_id) + 1:]
    words = ability_id.split("_")
    out = []
    for i, word in enumerate(words):
        out.append(word if i > 0 and word in SMALL else word.capitalize())
    return " ".join(out)


def clean_doc(doc):
    lines = [line.strip().lstrip("*").strip() for line in doc.strip().splitlines()]
    text = " ".join(line for line in lines if line and not line.startswith("@"))
    text = re.sub(r"\{@(?:code|link|literal) ([^}]*)\}", r"\1", text)
    text = re.sub(r"<[^>]+>", "", text)
    return re.sub(r"\s+", " ", text).strip()


def main():
    with open(LANG, encoding="utf-8") as f:
        lang = json.load(f)
    added = 0
    missing_docs = []
    for folder, _, files in os.walk(SOURCES):
        for name in sorted(files):
            if not name.endswith(".java"):
                continue
            source = open(os.path.join(folder, name), encoding="utf-8").read()
            prefixed = os.sep + "paramecia" in folder or os.sep + "zoan" in folder
            fruit = re.search(r'"fruit/grandline:([a-z0-9_]+)"', source)
            fruit_id = fruit.group(1) if fruit and prefixed else None
            for ability_id, cls in REGISTRATION.findall(source):
                key = "ability.grandline." + ability_id
                match = re.search(CLASS_DOC.format(cls), source, re.S)
                if key not in lang:
                    lang[key] = display_name(ability_id, fruit_id, match.group(1) if match else None)
                    added += 1
                if key + ".desc" not in lang:
                    if match:
                        lang[key + ".desc"] = clean_doc(match.group(1))
                        added += 1
                    else:
                        missing_docs.append(f"{name}:{cls}")
    with open(LANG, "w", encoding="utf-8") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("ability lang entries added:", added, file=sys.stderr)
    if missing_docs:
        print("no javadoc for:", ", ".join(missing_docs), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
