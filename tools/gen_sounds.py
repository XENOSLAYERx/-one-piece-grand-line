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


def write(name, signal):
    path = os.path.join(ROOT, name + ".ogg")
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


if __name__ == "__main__":
    sounds()
    print("sounds written to", os.path.normpath(ROOT), file=sys.stderr)
