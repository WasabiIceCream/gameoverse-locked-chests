#!/usr/bin/env python3
"""Draws the keys and the Locked Chest from the pixel maps below (original art, MIT like the mod)."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/gameoverse_locked_chests/textures'

# Per tier: light, mid, dark.
TIERS = {
    'common': ((228, 228, 232), (170, 170, 178), (104, 104, 112)),
    'rare': ((170, 255, 150), (85, 200, 70), (40, 120, 34)),
    'epic': ((255, 190, 255), (220, 100, 230), (130, 40, 150)),
    'legendary': ((255, 240, 140), (240, 196, 60), (176, 120, 20)),
    'divine': ((200, 255, 250), (92, 219, 213), (32, 140, 150)),
}
OUTLINE = (34, 26, 20)

KEY = [
    "................",
    "..........kkkk..",
    ".........kLLMMk.",
    "........kLMkkMDk",
    "........kMk..kDk",
    "........kMkk.kDk",
    ".........kMMMDk.",
    "........kMDkkk..",
    ".......kMDk.....",
    "......kMDk......",
    ".....kMDk.......",
    "....kMDkMk......",
    "...kMDkkDk......",
    "..kMDk..kk......",
    "..kDk...........",
    "...k............",
]

# Chest faces: w/W/v planks (light/mid/dark), f frame, i/I iron band, L/M/D lock plate, h keyhole.
WOOD = {'w': (156, 112, 66), 'W': (134, 94, 54), 'v': (108, 74, 40), 'f': (66, 44, 24),
        'i': (168, 170, 180), 'I': (104, 106, 118), 'h': (20, 16, 14)}
SIDE = [
    "ffffffffffffffff",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "iiiiiiiiiiiiiiii",
    "IIIIIIIIIIIIIIII",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "ffffffffffffffff",
]
TOP = [
    "ffffffffffffffff",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "iiiiiiiiiiiiiiii",
    "IIIIIIIIIIIIIIII",
]
FRONT = [
    "ffffffffffffffff",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "fwwwwwkkkkwwwwwf",
    "fWWWWkLLMMkWWWWf",
    "fvvvvkLMMDkvvvvf",
    "iiiiikMhhDkiiiii",
    "IIIIIkMhhDkIIIII",
    "fwwwwkMMhDkwwwwf",
    "fWWWWkDDDDkWWWWf",
    "fvvvvvkkkkvvvvvf",
    "fwwwwwwwwwwwwwwf",
    "fWWWWWWWWWWWWWWf",
    "fvvvvvvvvvvvvvvf",
    "ffffffffffffffff",
]


def draw(rows, palette, path):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (path, y)
        for x, ch in enumerate(row):
            color = palette.get(ch)
            if color:
                img.putpixel((x, y), color + (255,))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


for tier, (light, mid, dark) in TIERS.items():
    lock = {'k': OUTLINE, 'L': light, 'M': mid, 'D': dark}
    draw(KEY, lock, ROOT / 'item' / f'{tier}_key.png')
    draw(FRONT, {**WOOD, **lock}, ROOT / 'block' / f'locked_chest_front_{tier}.png')
draw(SIDE, WOOD, ROOT / 'block' / 'locked_chest_side.png')
draw(TOP, WOOD, ROOT / 'block' / 'locked_chest_top.png')
print('drawn')
