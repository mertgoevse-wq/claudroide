#!/usr/bin/env python3
"""Task: turn the flat white background of the mascot render transparent.

The mascot has off-white highlights (#F7F9F6) that sit only a few steps away
from the pure white background. Keying *every* near-white pixel would punch
holes through the robot's own highlights, so this walks inwards from the border
instead: only pixels that are reachable from the edge through near-white
neighbours are dropped. A highlight enclosed by green is never reached.

Run with the Pillow venv:
    /tmp/imgvenv/bin/python tools/make_mascot_transparent.py \
        /tmp/logos/mascot-v3.jpg assets/ClauDroide-mascot-logo.png
"""
import sys
from collections import deque

from PIL import Image

# A pixel counts as background when every channel is this bright or brighter.
BG_LEVEL = 243
# How far a pixel may drift from white and still be treated as background.
TOLERANCE = 14


def is_background(r: int, g: int, b: int) -> bool:
    return (
        r >= BG_LEVEL - TOLERANCE
        and g >= BG_LEVEL - TOLERANCE
        and b >= BG_LEVEL - TOLERANCE
    )


def make_transparent(src: str, dst_png: str, dst_logo: str, logo_size: int = 320) -> None:
    image = Image.open(src).convert("RGB")
    width, height = image.size
    pixels = image.load()

    reached = bytearray(width * height)
    queue: deque[tuple[int, int]] = deque()

    def visit(x: int, y: int) -> None:
        index = y * width + x
        if reached[index]:
            return
        r, g, b = pixels[x, y]
        if not is_background(r, g, b):
            return
        reached[index] = 1
        queue.append((x, y))

    for x in range(width):
        visit(x, 0)
        visit(x, height - 1)
    for y in range(height):
        visit(0, y)
        visit(width - 1, y)

    while queue:
        x, y = queue.popleft()
        if x > 0:
            visit(x - 1, y)
        if x < width - 1:
            visit(x + 1, y)
        if y > 0:
            visit(x, y - 1)
        if y < height - 1:
            visit(x, y + 1)

    out = image.convert("RGBA")
    out_pixels = out.load()
    cleared = 0
    for y in range(height):
        for x in range(width):
            if reached[y * width + x]:
                out_pixels[x, y] = (out_pixels[x, y][0], out_pixels[x, y][1], out_pixels[x, y][2], 0)
                cleared += 1

    # Crop to the remaining content so the logo is not mostly margin.
    alpha = out.getchannel("A")
    box = alpha.getbbox()
    if box:
        out = out.crop(box)

    out.save(dst_png, "PNG", optimize=True)
    out.resize((logo_size, logo_size), Image.LANCZOS).save(dst_logo, "PNG", optimize=True)

    print(f"source        : {src} ({width}x{height})")
    print(f"cleared pixels: {cleared}")
    print(f"content box   : {box}")
    print(f"written       : {dst_png} {out.size}")
    print(f"written       : {dst_logo} {logo_size}x{logo_size}")


if __name__ == "__main__":
    make_transparent(sys.argv[1], sys.argv[2], sys.argv[3])