#!/usr/bin/env python3
"""Draw the Claudroide brand marks as exact vector geometry, then rasterise.

Why geometry and not an image model: the wordmark has to spell "Claudroide",
and diffusion models misspell wordmarks. Geometry also lets the two halves of
the mark be measured against each other, which is the whole point of the brief
-- neither the droid nor the burst may read as decoration on the other.

Output is PNG/WebP. The SVG is build input, not a shipped asset.
"""
import math
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "brand"
BUILD = ROOT / "assets" / "brand" / ".build"

# --- palette -----------------------------------------------------------------
GREEN = "#3DDC84"          # Android green, already the app's primary
GREEN_DEEP = "#24B566"     # shaded side of the dome
CORAL = "#E06D53"          # the app's existing secondary; the burst colour
CORAL_DEEP = "#C4553C"
VISOR = "#15201B"
EYE = "#F2F7F3"
INK = "#0C100E"            # near-black, not pure black (AMOLED smear)
INK_SOFT = "#131815"
PAPER = "#F7FBF8"


def ray(cx, cy, deg, r0, r1, half_w, waist=0.55):
    """One tapered burst ray: wide at r0, a point at r1, concave sides.

    `waist` pulls the curve controls toward the axis. Below ~0.7 the ray reads
    as a spark; at 1.0 it reads as a gear tooth, which is the wrong grammar.
    """
    a = math.radians(deg - 90.0)
    ux, uy = math.cos(a), math.sin(a)      # along the ray
    px, py = -uy, ux                       # across the ray

    def P(along, across):
        return (cx + ux * along + px * across, cy + uy * along + py * across)

    base_l = P(r0, -half_w)
    base_r = P(r0, half_w)
    tip = P(r1, 0.0)
    ctrl_l = P(r0 + (r1 - r0) * waist, -half_w * 0.30)
    ctrl_r = P(r0 + (r1 - r0) * waist, half_w * 0.30)
    return (
        f"M{base_l[0]:.2f},{base_l[1]:.2f} "
        f"Q{ctrl_l[0]:.2f},{ctrl_l[1]:.2f} {tip[0]:.2f},{tip[1]:.2f} "
        f"Q{ctrl_r[0]:.2f},{ctrl_r[1]:.2f} {base_r[0]:.2f},{base_r[1]:.2f} Z"
    )


def mark(size=1024, pad=0.0):
    """The symbol: an Android dome wearing a Claude-grammar burst as its crown.

    The two rays at +/-26 degrees are the droid's antennae AND two rays of the
    burst -- the same strokes serve both systems, so neither half can be
    deleted without breaking the other. That is the "no subordinate symbol"
    requirement expressed as geometry rather than as a caption.
    """
    S = size
    cx, cy = S * 0.500, S * 0.545
    R_IN = S * 0.150        # burst starts behind the dome
    R_OUT = S * 0.468       # burst reaches the canvas edge
    DOME_R = S * 0.268

    # Antennae are longer and thinner; flank rays are shorter and wider.
    rays = [
        (-26, R_OUT * 1.000, S * 0.0320),   # antenna L  (shared geometry)
        (+26, R_OUT * 1.000, S * 0.0320),   # antenna R  (shared geometry)
        (-72, R_OUT * 0.905, S * 0.0460),
        (+72, R_OUT * 0.905, S * 0.0460),
        (-118, R_OUT * 0.815, S * 0.0415),
        (+118, R_OUT * 0.815, S * 0.0415),
        (-157, R_OUT * 0.690, S * 0.0330),
        (+157, R_OUT * 0.690, S * 0.0330),
    ]

    behind = "".join(
        f'<path d="{ray(cx, cy, d, R_IN, r1, w)}" fill="url(#burst)"/>'
        for d, r1, w in rays
    )
    # The two lowest rays are redrawn over the dome so the burst is not simply
    # "behind the droid". They interlock, like two halves of one mark.
    front = "".join(
        f'<path d="{ray(cx, cy, d, R_IN, r1, w)}" fill="url(#burstFront)"/>'
        for d, r1, w in rays if abs(d) == 157
    )

    dome = (
        f'<path d="M{cx - DOME_R:.2f},{cy + DOME_R * 0.62:.2f} '
        f'a{DOME_R:.2f},{DOME_R:.2f} 0 0 1 {DOME_R * 2:.2f},0 Z" fill="url(#dome)"/>'
    )

    vw, vh = DOME_R * 1.46, DOME_R * 0.455
    vx, vy = cx - vw / 2, cy - DOME_R * 0.300
    visor = f'<rect x="{vx:.2f}" y="{vy:.2f}" width="{vw:.2f}" height="{vh:.2f}" rx="{vh / 2:.2f}" fill="{VISOR}"/>'
    er = vh * 0.300
    eyes = (
        f'<circle cx="{cx - vw * 0.235:.2f}" cy="{vy + vh / 2:.2f}" r="{er:.2f}" fill="{EYE}"/>'
        f'<circle cx="{cx + vw * 0.235:.2f}" cy="{vy + vh / 2:.2f}" r="{er:.2f}" fill="{EYE}"/>'
    )

    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{S}" height="{S}" viewBox="0 0 {S} {S}">
<defs>
<linearGradient id="burst" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="{CORAL}"/><stop offset="1" stop-color="{CORAL_DEEP}"/>
</linearGradient>
<linearGradient id="burstFront" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="{CORAL}"/><stop offset="1" stop-color="{CORAL_DEEP}"/>
</linearGradient>
<linearGradient id="dome" x1="0.15" y1="0" x2="0.85" y2="1">
  <stop offset="0" stop-color="{GREEN}"/><stop offset="1" stop-color="{GREEN_DEEP}"/>
</linearGradient>
</defs>
{behind}{dome}{front}{visor}{eyes}
</svg>'''


def render(svg, png, w=None):
    BUILD.mkdir(parents=True, exist_ok=True)
    src = BUILD / (pathlib.Path(png).stem + ".svg")
    src.write_text(svg, encoding="utf-8")
    cmd = ["rsvg-convert", str(src), "-o", str(png)]
    if w:
        cmd[2:2] = ["-w", str(w)]
    subprocess.run(cmd, check=True)
    return png


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    render(mark(1024), OUT / "mark-1024.png")
    render(mark(1024), OUT / "mark-256.png", w=256)
    render(mark(1024), OUT / "mark-96.png", w=96)
    print("ok")
