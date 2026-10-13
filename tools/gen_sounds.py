#!/usr/bin/env python3
"""Synthesises the mod's original sound effects and encodes them to mono Ogg Vorbis.

Usage: python3 tools/gen_sounds.py   (needs numpy and ffmpeg; writes into src/main/resources/assets/grandline/sounds)

Every sound is built from a few primitives (filtered noise sweeps, pitched thumps, inharmonic metal partials,
crackle) so each effect has its own character without any sampled audio.
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

RATE = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "grandline", "sounds")
RNG = np.random.default_rng(7)


# ---------------------------------------------------------------------------------------------------------------
# Primitives
# ---------------------------------------------------------------------------------------------------------------

def t_axis(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def envelope(n, attack, decay, sustain=0.0, curve=3.0):
    """Attack (s) then exponential-ish decay over the remaining length."""
    env = np.zeros(n)
    a = max(1, int(attack * RATE))
    env[:a] = np.linspace(0, 1, a)
    rest = n - a
    if rest > 0:
        x = np.linspace(0, 1, rest)
        env[a:] = sustain + (1 - sustain) * (1 - x) ** curve
    return env * np.clip(np.linspace(decay * 50, 0, n), 0, 1)


def lowpass_sweep(signal, start_hz, end_hz):
    """One-pole low-pass whose cutoff glides from start_hz to end_hz."""
    out = np.zeros_like(signal)
    cut = np.geomspace(start_hz, end_hz, len(signal))
    alpha = 1 - np.exp(-2 * np.pi * cut / RATE)
    y = 0.0
    for i, s in enumerate(signal):
        y += alpha[i] * (s - y)
        out[i] = y
    return out


def highpass(signal, hz):
    low = lowpass_sweep(signal, hz, hz)
    return signal - low


def noise(seconds):
    return RNG.uniform(-1, 1, int(RATE * seconds))


def thump(seconds, f0, f1, decay_curve=4.0):
    t = t_axis(seconds)
    freq = np.geomspace(f0, f1, len(t))
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    return np.sin(phase) * (1 - t / seconds) ** decay_curve


def metal(seconds, partials, decays, amps):
    t = t_axis(seconds)
    out = np.zeros_like(t)
    for f, d, a in zip(partials, decays, amps):
        out += a * np.sin(2 * np.pi * f * t + RNG.uniform(0, 6.28)) * np.exp(-t / d)
    return out


def crackle(seconds, density):
    n = int(RATE * seconds)
    out = np.zeros(n)
    count = int(density * seconds)
    for _ in range(count):
        i = RNG.integers(0, n - 200)
        length = RNG.integers(20, 200)
        out[i:i + length] += RNG.uniform(-1, 1, length) * np.exp(-np.arange(length) / (length / 4))
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def normalise(signal, peak=0.9):
    m = np.max(np.abs(signal)) or 1.0
    fade = min(len(signal), int(0.01 * RATE))
    signal = signal / m * peak
    signal[-fade:] *= np.linspace(1, 0, fade)
    return signal


FORCE = "--force" in sys.argv


def write(name, signal):
    path = os.path.join(ROOT, name + ".ogg")
    if os.path.exists(path) and not FORCE:
        return  # Ogg streams get random serial numbers; keep existing files stable unless forced.
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = (normalise(signal) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4",
                    "-ac", "1", path], check=True)
    os.unlink(wav_path)


# ---------------------------------------------------------------------------------------------------------------
# Sounds
# ---------------------------------------------------------------------------------------------------------------

def whoosh(seconds, start_hz, end_hz, attack=0.02):
    n = noise(seconds)
    return lowpass_sweep(n, start_hz, end_hz) * envelope(len(n), attack, 1.0, curve=2.2)


def tone(seconds, f0, f1, harmonics=(1.0,), vibrato=0.0, vib_rate=6.0):
    """Pitched tone gliding f0 -> f1 with optional harmonics and vibrato."""
    t = t_axis(seconds)
    freq = np.geomspace(f0, f1, len(t)) * (1 + vibrato * np.sin(2 * np.pi * vib_rate * t))
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    out = np.zeros_like(t)
    for i, amp in enumerate(harmonics):
        out += amp * np.sin(phase * (i + 1))
    return out


def delayed(signal, seconds):
    return np.concatenate([np.zeros(int(RATE * seconds)), signal])


def env(sig, attack, curve=2.0):
    return sig * envelope(len(sig), attack, 1.0, curve=curve)


def sounds():
    # Soru: a sharp snap then a fast air-tearing whoosh.
    snap = highpass(noise(0.03), 3000) * envelope(int(0.03 * RATE), 0.001, 1.0, curve=6)
    write("rokushiki/soru", mix(0.8 * snap, whoosh(0.32, 7000, 600, attack=0.008)))

    # Geppo: a dull air pop with a short puff.
    write("rokushiki/geppo", mix(thump(0.12, 160, 55, 3.0), 0.5 * whoosh(0.16, 2500, 400, attack=0.004)))

    # Tekkai on: body tensing into steel, a ringing metallic swell.
    ring = metal(0.9, [312, 781, 1290, 2033, 3110], [0.5, 0.35, 0.25, 0.15, 0.08], [1, 0.7, 0.5, 0.35, 0.2])
    write("rokushiki/tekkai_on", mix(ring * envelope(len(ring), 0.03, 1.0, curve=1.5),
                                     0.3 * whoosh(0.15, 1500, 300)))

    # Tekkai block: hard clang.
    clang = metal(0.6, [523, 1187, 1902, 2870, 4400], [0.3, 0.2, 0.12, 0.08, 0.05], [1, 0.8, 0.6, 0.45, 0.3])
    hit = highpass(noise(0.05), 1500) * envelope(int(0.05 * RATE), 0.001, 1.0, curve=5)
    write("rokushiki/tekkai_block", mix(clang, 0.9 * hit, 0.4 * thump(0.1, 120, 60)))

    # Evade: a soft swish.
    write("combat/evade", 0.6 * whoosh(0.2, 4000, 900, attack=0.03))

    # ---- Flame-Flame Fruit
    write("flame/charge", mix(env(lowpass_sweep(noise(1.0), 400, 2500), 0.6, 1.2), 0.5 * crackle(1.0, 60)))
    write("flame/fire_fist", mix(whoosh(0.8, 3500, 350, attack=0.03), 0.8 * thump(0.6, 90, 40, 2.0),
                                 0.4 * crackle(0.8, 50)))
    boom = mix(thump(1.2, 70, 28, 2.5), env(lowpass_sweep(noise(1.2), 2500, 150), 0.005, 2.5), 0.4 * crackle(1.2, 40))
    write("flame/explosion", boom)
    pops = [delayed(mix(env(highpass(noise(0.06), 800), 0.002, 4), 0.6 * thump(0.08, 200, 90)), i * 0.045)
            for i in range(5)]
    write("flame/fire_gun", mix(*pops))
    roar = env(lowpass_sweep(noise(1.3), 1800, 900), 0.15, 1.3) * (1 + 0.3 * np.sin(2 * np.pi * 9 * t_axis(1.3)))
    write("flame/wall", mix(roar, 0.5 * crackle(1.3, 70)))
    write("flame/pillar", mix(env(lowpass_sweep(noise(1.0), 300, 4500), 0.05, 1.8), 0.9 * thump(0.7, 80, 35, 2.0),
                              0.4 * crackle(1.0, 50)))
    rumble = tone(2.0, 40, 55, (1.0, 0.5, 0.25)) * envelope(int(2.0 * RATE), 1.2, 1.0, curve=0.8)
    write("flame/entei_charge", mix(rumble, env(lowpass_sweep(noise(2.0), 300, 1500), 1.4, 0.8), 0.3 * crackle(2.0, 50)))
    chimes = [delayed(env(tone(0.5, f, f, (1.0, 0.3)), 0.005, 3), d) for f, d in ((2093, 0), (2637, 0.07), (3136, 0.15), (4186, 0.22))]
    write("flame/firefly", 0.6 * mix(*chimes))

    # ---- Gum-Gum Fruit
    write("gum/stretch", mix(env(tone(0.35, 180, 620, (1.0, 0.4), vibrato=0.04, vib_rate=30), 0.02, 1.5),
                             0.3 * env(lowpass_sweep(noise(0.35), 1500, 600), 0.05, 1.5)))
    write("gum/snap", mix(env(tone(0.18, 900, 140, (1.0, 0.5)), 0.003, 2.0), 0.6 * env(highpass(noise(0.03), 2000), 0.001, 4)))
    write("gum/punch", mix(thump(0.3, 130, 45, 3.0), 0.7 * env(lowpass_sweep(noise(0.1), 2500, 400), 0.002, 4)))
    thuds = [delayed(mix(thump(0.12, 150, 60, 3.0), 0.4 * env(lowpass_sweep(noise(0.05), 2000, 500), 0.002, 4)), i * 0.04)
             for i in range(6)]
    write("gum/gatling", mix(*thuds))
    write("gum/steam", env(highpass(noise(1.2), 2500), 0.25, 1.5))
    write("gum/inflate", mix(env(tone(1.0, 80, 170, (1.0, 0.6, 0.3), vibrato=0.05, vib_rate=8), 0.2, 1.2),
                             0.5 * env(lowpass_sweep(noise(1.0), 400, 1600), 0.3, 1.2)))
    write("gum/bounce", env(tone(0.5, 140, 420, (1.0, 0.35), vibrato=0.12, vib_rate=14), 0.005, 1.8))
    write("gum/giant_impact", mix(thump(1.3, 60, 22, 2.0), env(lowpass_sweep(noise(1.0), 1800, 120), 0.003, 2.5)))

    # ---- Leopard
    growl = tone(0.9, 75, 65, (1.0, 0.7, 0.5, 0.35, 0.25)) * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * t_axis(0.9)))
    write("leopard/growl", mix(env(growl, 0.1, 1.2), 0.4 * env(lowpass_sweep(noise(0.9), 900, 500), 0.1, 1.2)))
    roar = tone(1.5, 150, 85, (1.0, 0.8, 0.6, 0.45, 0.3, 0.2)) * (0.7 + 0.3 * np.sin(2 * np.pi * 31 * t_axis(1.5)))
    write("leopard/roar", mix(env(roar, 0.08, 1.4), 0.8 * env(lowpass_sweep(noise(1.5), 2500, 700), 0.05, 1.4)))
    swipes = [delayed(env(highpass(lowpass_sweep(noise(0.14), 7000, 1500), 600), 0.01, 2.5), i * 0.07) for i in range(3)]
    write("leopard/claw", mix(*swipes))

    # ---- Haki
    # Hardening: a deep metallic swell, darker than Tekkai, with a low push.
    dark = metal(1.0, [156, 401, 655, 1012, 1530], [0.6, 0.45, 0.3, 0.2, 0.12], [1, 0.8, 0.55, 0.35, 0.2])
    write("haki/harden", mix(dark * envelope(len(dark), 0.06, 1.0, curve=1.4), 0.7 * thump(0.4, 90, 40, 2.5),
                             0.25 * env(lowpass_sweep(noise(0.5), 600, 2400), 0.15, 1.5)))
    # Armament hit: heavy thud, a hard crack and a short dark ring.
    write("haki/impact", mix(thump(0.45, 110, 38, 2.5), 0.9 * env(highpass(noise(0.05), 1800), 0.001, 5),
                             0.5 * metal(0.5, [233, 587, 941], [0.25, 0.15, 0.1], [1, 0.6, 0.4])))
    # Emission: a blow, then the shock arriving inside a beat later.
    write("haki/emission", mix(thump(0.3, 140, 50, 3.0), delayed(mix(thump(0.9, 70, 26, 2.0),
                               env(lowpass_sweep(noise(0.9), 3000, 200), 0.004, 2.2)), 0.12)))
    # Observation: a soft, ringing swell, like hearing everything at once.
    halo = mix(tone(1.2, 660, 660, (1.0, 0.3), vibrato=0.004), 0.6 * tone(1.2, 990, 990, (1.0,), vibrato=0.006))
    write("haki/sense", mix(env(halo, 0.35, 1.2), 0.3 * env(lowpass_sweep(noise(1.2), 500, 3000), 0.5, 1.0)))
    # Future Sight: a reversed rush ending in a bright chime.
    rush = env(lowpass_sweep(noise(0.6), 300, 6000), 0.55, 0.3)
    write("haki/future_sight", mix(rush, delayed(env(tone(0.7, 1760, 1760, (1.0, 0.4, 0.2)), 0.004, 2.5), 0.55)))
    # Conqueror's charge: rising rumble with electric crackle.
    rise = tone(1.5, 35, 70, (1.0, 0.6, 0.3)) * envelope(int(1.5 * RATE), 1.0, 1.0, curve=0.6)
    write("haki/conqueror_charge", mix(rise, 0.6 * crackle(1.5, 140), 0.3 * env(highpass(noise(1.5), 3000), 0.8, 0.8)))
    # Conqueror's release: a pressure wave that rolls out, full of static.
    wave_ = mix(thump(2.0, 55, 20, 1.6), env(lowpass_sweep(noise(2.0), 2200, 90), 0.01, 2.0))
    write("haki/conqueror_wave", mix(wave_, 0.7 * crackle(1.6, 220), 0.4 * env(highpass(noise(0.4), 4000), 0.002, 3)))
    # Clash: two wills colliding, a double boom and a storm of sparks.
    write("haki/clash", mix(thump(1.4, 65, 24, 1.8), delayed(thump(1.2, 80, 28, 1.8), 0.09), crackle(1.4, 400),
                            0.5 * env(lowpass_sweep(noise(1.4), 5000, 300), 0.005, 1.6)))
    # Infused strike: a sharp electric snap.
    write("haki/crackle", mix(crackle(0.35, 600) * envelope(int(0.35 * RATE), 0.002, 1.0, curve=2.5),
                              0.5 * thump(0.2, 180, 70, 3.0)))
    # Knock-out: a dull body drop.
    write("haki/knockout", mix(thump(0.5, 95, 40, 2.8), 0.4 * env(lowpass_sweep(noise(0.3), 900, 200), 0.004, 3)))


def sounds_json():
    """Every .ogg under sounds/ becomes an event named like the path with dots: flame/explosion -> flame.explosion."""
    import json
    events = {}
    for folder, _, files in os.walk(ROOT):
        for f in sorted(files):
            if f.endswith(".ogg"):
                rel = os.path.relpath(os.path.join(folder, f), ROOT)[:-4].replace(os.sep, "/")
                name = rel.replace("/", ".")
                events[name] = {"subtitle": "subtitles.grandline." + name, "sounds": ["grandline:" + rel]}
    with open(os.path.join(ROOT, "..", "sounds.json"), "w") as out:
        json.dump(dict(sorted(events.items())), out, indent=2)
        out.write("\n")


if __name__ == "__main__":
    sounds()
    sounds_json()
    print("sounds written to", os.path.normpath(ROOT), file=sys.stderr)
