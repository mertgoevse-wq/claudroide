#!/usr/bin/env python3
"""Draw the Claudroide README banner: the merged mark, the wordmark, a tagline.

Why geometry and not an image model: a diffusion model cannot spell
"Claudroide" reliably, and a banner whose product name is misspelled is worse
than no banner. Every glyph here is real Inter, so the wordmark is correct by
construction.

The mark is imported from tools/brand.py rather than redrawn, so the banner and
the app icon cannot drift apart: same geometry, same palette, one source.

Output: assets/brand/banner.png (1376x768, 16:9).
The SVG under .build/ is build input, not a shipped asset.
"""
import pathlib
import re
import subprocess
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import brand  # noqa: E402  -- sibling module; sys.path must be fixed first

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "brand"
BUILD = OUT / ".build"

W, H = 1376, 768

INK = brand.INK
PAPER = brand.PAPER
GREEN = brand.GREEN
CORAL = brand.CORAL
MUTED = "#8E9A93"

WORD = "Claudroide"
TAGLINE = "Mobile AI coding assistant · on-device, your keys"


def text_width(s, size, weight=700):
    """Inked width of `s` in px, measured from the real font.

    The alternative -- spacing glyphs by eye -- is what gives a hand-rolled
    wordmark away. This renders the run with rsvg (which uses Pango for layout)
    and measures the pixels it actually inked, so the rule under the wordmark
    matches its true width rather than a guess.

    pango-view would report the same number directly, but it shells out to
    GraphicsMagick to rasterise, which is not installed here.
    """
    scratch = BUILD / ".measure"
    scratch.mkdir(parents=True, exist_ok=True)
    probe = scratch / "probe.png"
    src = scratch / "probe.svg"
    pad = size  # room for ascenders/descenders on both axes
    src.write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{int(size * 16)}" '
        f'height="{int(size * 3)}">'
        f'<rect width="100%" height="100%" fill="#000"/>'
        f'<text x="{size}" y="{int(size * 2)}" font-family="Inter" '
        f'font-size="{size}" font-weight="{weight}" fill="#fff">{s}</text>'
        f'</svg>',
        encoding="utf-8",
    )
    subprocess.run(["rsvg-convert", str(src), "-o", str(probe)], check=True)
    return _inked_width(probe)


def _inked_width(png_path):
    """Width in px of the non-black region of a black-background render."""
    import struct
    import zlib

    data = png_path.read_bytes()
    pos, idat, width, height = 8, bytearray(), None, None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, ctype = struct.unpack(">IIBB", chunk[:10])
            if depth != 8:
                raise SystemExit("measure probe must be 8-bit")
            channels = {2: 3, 6: 4}[ctype]
        elif kind == b"IDAT":
            idat += chunk
        elif kind == b"IEND":
            break
        pos += 12 + length

    raw = zlib.decompress(bytes(idat))
    stride = width * channels
    prev = bytearray(stride)
    out = bytearray()
    k = 0
    for _ in range(height):
        filt = raw[k]
        k += 1
        line = bytearray(raw[k:k + stride])
        k += stride
        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = prev[i]
            c = prev[i - channels] if i >= channels else 0
            if filt == 1:
                line[i] = (line[i] + a) & 255
            elif filt == 2:
                line[i] = (line[i] + b) & 255
            elif filt == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif filt == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pred) & 255
        out += line
        prev = line

    # x offset of the run is `size` inside the probe, so subtract it back out.
    min_x, max_x = width, -1
    for y in range(height):
        row = y * stride
        for x in range(width):
            o = row + x * channels
            if out[o] > 40 or out[o + 1] > 40 or out[o + 2] > 40:
                if x < min_x:
                    min_x = x
                if x > max_x:
                    max_x = x
    if max_x < 0:
        raise SystemExit("measure probe inked nothing -- wrong font name?")
    return float(max_x - min_x)


def _split_mark():
    """brand.mark() returns a full <svg>; split it into reusable <defs> + body.

    The body paths reference ids declared in its <defs>, so both halves are
    carried over or the gradients silently drop to black.
    """
    svg = brand.mark(1024)
    inner = svg.split(">", 1)[1].rsplit("</svg>", 1)[0]
    defs, body = inner.split("</defs>", 1)
    return defs + "</defs>", body


def banner():
    defs, body = _split_mark()

    mark_size = 296
    mark_x = 96
    mark_y = (H - mark_size) // 2
    mark_g = (
        f'<g transform="translate({mark_x},{mark_y}) scale({mark_size / 1024:.6f})">'
        f"{body}</g>"
    )

    text_x = mark_x + mark_size + 76
    centre = H // 2

    word_size = 94
    word_w = text_width(WORD, word_size)
    tag_size = 29
    tag_w = text_width(TAGLINE, tag_size, 400)

    def text_el(s, x, y, size, weight, fill, spacing=None):
        ls = f' letter-spacing="{spacing}"' if spacing is not None else ""
        return (
            f'<text x="{x}" y="{y}" font-family="Inter" font-size="{size}" '
            f'font-weight="{weight}" fill="{fill}"{ls}>{s}</text>'
        )

    # Baseline of the wordmark sits slightly above centre so the block reads as
    # optically centred rather than mathematically centred -- the descender of
    # nothing here is deep, but the tagline below pulls the eye down.
    word_baseline = centre - 14
    rule_y = centre + 20
    tag_baseline = centre + 56

    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
{defs}
<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
  <stop offset="0" stop-color="{INK}"/>
  <stop offset="1" stop-color="#161D19"/>
</linearGradient>
<radialGradient id="glow" cx="0.5" cy="0.5" r="0.5">
  <stop offset="0" stop-color="{GREEN}" stop-opacity="0.22"/>
  <stop offset="1" stop-color="{GREEN}" stop-opacity="0"/>
</radialGradient>
</defs>
<rect width="{W}" height="{H}" fill="url(#bg)"/>
<circle cx="{mark_x + mark_size / 2:.0f}" cy="{centre}" r="300" fill="url(#glow)"/>
{mark_g}
{text_el(WORD, text_x, word_baseline, word_size, 700, PAPER, spacing="-2")}
<rect x="{text_x}" y="{rule_y}" width="{max(word_w, tag_w):.1f}" height="2" fill="{CORAL}" opacity="0.6"/>
{text_el(TAGLINE, text_x, tag_baseline, tag_size, 400, MUTED)}
</svg>'''


def render():
    BUILD.mkdir(parents=True, exist_ok=True)
    src = BUILD / "banner.svg"
    src.write_text(banner(), encoding="utf-8")
    png = OUT / "banner.png"
    subprocess.run(["rsvg-convert", str(src), "-o", str(png)], check=True)
    return png


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    print("wrote", render())