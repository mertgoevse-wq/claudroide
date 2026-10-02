#!/usr/bin/env python3
"""Draw the ClauDroide banner: two bots side by side, hands joined.

The second figure is a GENERIC terminal bot -- a rounded rectangle screen
showing a ">_" prompt. It is deliberately not a copy of Anthropic's Claude
mascot, and the reason is a correctness one, not a taste one:

    The settings screen shows the banner directly above an independence
    notice that says ClauDroide is not affiliated with Anthropic. A
    recognisable copy of Anthropic's character on that same screen would
    make that notice false. The notice is the more important claim, so the
    artwork yields to it.

What survives the substitution is the message: two different systems, one
shared task, meeting in the middle. The gesture carries that, not the face.

Geometry, not an image model, for the reason given in brand.py: the wordmark
has to spell "ClauDroide" exactly, and diffusion models misspell wordmarks.
The image-model route was tried and rejected -- the only provider reachable
from this environment (Pollinations) bakes a "pollinations.ai" watermark
into the output, which disqualifies it for a brand mark, and it also
returned the two robots facing each other rather than holding hands.

Output is PNG. The SVG is build input, not a shipped asset.
"""
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "brand"
BUILD = ROOT / "assets" / "brand" / ".build"

W, H = 1376, 768

GREEN = "#3DDC84"
GREEN_DEEP = "#24B566"
CORAL = "#E06D53"
CORAL_DEEP = "#C4553C"
VISOR = "#15201B"
SCREEN = "#1B2420"
EYE = "#F2F7F3"
INK = "#0C100E"
PAPER = "#F7FBF8"


def dome_bot(cx, cy, r, facing):
    """The Android dome: a half-disc head with a visor, and one arm.

    `facing` is -1 for the left figure (arm reaches right) and +1 for the
    right figure (arm reaches left). The arm is a thick round-capped stroke
    rather than a filled shape so the joint stays readable where the two
    arms meet.
    """
    parts = []

    parts.append(
        f'<path d="M{cx - r:.1f},{cy:.1f} a{r:.1f},{r:.1f} 0 0 1 {2 * r:.1f},0 Z" '
        f'fill="url(#dome)"/>'
    )

    vw, vh = r * 1.42, r * 0.44
    vx, vy = cx - vw / 2, cy - r * 0.86
    parts.append(
        f'<rect x="{vx:.1f}" y="{vy:.1f}" width="{vw:.1f}" height="{vh:.1f}" '
        f'rx="{vh / 2:.1f}" fill="{VISOR}"/>'
    )
    er = vh * 0.29
    for ex in (cx - vw * 0.23, cx + vw * 0.23):
        parts.append(f'<circle cx="{ex:.1f}" cy="{vy + vh / 2:.1f}" r="{er:.1f}" fill="{EYE}"/>')

    parts.append(
        f'<path d="M{cx:.1f},{cy - r:.1f} L{cx:.1f},{cy - r * 1.42:.1f}" '
        f'stroke="{GREEN}" stroke-width="{r * 0.10:.1f}" stroke-linecap="round"/>'
    )
    parts.append(f'<circle cx="{cx:.1f}" cy="{cy - r * 1.50:.1f}" r="{r * 0.15:.1f}" fill="{CORAL}"/>')

    bw, bh = r * 1.55, r * 0.72
    parts.append(
        f'<rect x="{cx - bw / 2:.1f}" y="{cy:.1f}" width="{bw:.1f}" height="{bh:.1f}" '
        f'rx="{r * 0.30:.1f}" fill="url(#body)"/>'
    )

    # The arm is a SOLID stroke, not a gradient one. Verified against
    # rsvg-convert: a gradient referenced from `stroke` renders as
    # background colour, which made both arms invisible while the markup
    # looked correct. The gradient version is only safe on `fill`.
    sx = cx + facing * r * 0.62
    ex = cx + facing * r * 1.62
    ay = cy + bh * 0.46
    parts.append(
        f'<path d="M{sx:.1f},{ay:.1f} L{ex:.1f},{ay:.1f}" stroke="{GREEN_DEEP}" '
        f'stroke-width="{r * 0.26:.1f}" stroke-linecap="round"/>'
    )
    return "".join(parts)


def terminal_bot(cx, cy, r, facing, arm_y):
    """A generic terminal assistant: a screen showing a prompt, on a body.

    Nothing here derives from any existing mascot's silhouette. The screen
    with ">_" is the entire visual vocabulary: it says "command line" and
    stops there.

    `arm_y` is passed in rather than derived from this figure's own geometry.
    The two figures have different body heights, so each computing its own arm
    line put them 34px apart -- which read as two arms hanging beside each
    other instead of hands meeting. One shared line makes the clasp real.
    """
    parts = []

    sw, sh = r * 1.86, r * 1.30
    sx, sy = cx - sw / 2, cy - sh * 0.92
    parts.append(
        f'<rect x="{sx:.1f}" y="{sy:.1f}" width="{sw:.1f}" height="{sh:.1f}" '
        f'rx="{r * 0.28:.1f}" fill="url(#termBody)"/>'
    )

    iw, ih = sw * 0.80, sh * 0.66
    ix, iy = cx - iw / 2, sy + (sh - ih) / 2
    parts.append(
        f'<rect x="{ix:.1f}" y="{iy:.1f}" width="{iw:.1f}" height="{ih:.1f}" '
        f'rx="{r * 0.14:.1f}" fill="{SCREEN}"/>'
    )

    px, py = ix + iw * 0.22, iy + ih * 0.52
    ps = ih * 0.20
    parts.append(
        f'<path d="M{px:.1f},{py - ps:.1f} L{px + ps * 0.9:.1f},{py:.1f} L{px:.1f},{py + ps:.1f}" '
        f'fill="none" stroke="{GREEN}" stroke-width="{ps * 0.32:.1f}" '
        f'stroke-linecap="round" stroke-linejoin="round"/>'
    )
    parts.append(
        f'<path d="M{px + ps * 1.5:.1f},{py + ps * 1.5:.1f} L{px + ps * 2.5:.1f},{py + ps * 1.5:.1f}" '
        f'stroke="{GREEN}" stroke-width="{ps * 0.32:.1f}" stroke-linecap="round"/>'
    )

    bw, bh = r * 1.52, r * 0.66
    body_y = cy + r * 0.14
    parts.append(
        f'<rect x="{cx - r * 0.16:.1f}" y="{sy + sh:.1f}" width="{r * 0.32:.1f}" '
        f'height="{r * 0.20:.1f}" fill="{CORAL}"/>'
    )
    parts.append(
        f'<rect x="{cx - bw / 2:.1f}" y="{body_y:.1f}" width="{bw:.1f}" '
        f'height="{bh:.1f}" rx="{r * 0.28:.1f}" fill="url(#termBody)"/>'
    )

    sx = cx + facing * r * 0.60
    ex = cx + facing * r * 1.58
    # Solid stroke for the same renderer reason as the droid's arm.
    parts.append(
        f'<path d="M{sx:.1f},{arm_y:.1f} L{ex:.1f},{arm_y:.1f}" stroke="{CORAL_DEEP}" '
        f'stroke-width="{r * 0.25:.1f}" stroke-linecap="round"/>'
    )
    return "".join(parts)


def joined_hands(x, y, r):
    """The clasp where the two arms meet.

    Radius is deliberately smaller than the arm thickness would suggest, and
    the circle sits exactly on the shared arm line. An earlier version used
    r*0.30 and hung 10px below the line; it covered both wrists instead of
    joining them, so the gesture read as a ball being passed between two
    strangers rather than two hands clasped.
    """
    hr = r * 0.19
    return (
        f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{hr:.1f}" fill="{PAPER}"/>'
        f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{hr:.1f}" fill="none" '
        f'stroke="{INK}" stroke-width="{r * 0.035:.1f}" opacity="0.22"/>'
    )


def banner():
    """The banner. Figures left of centre, wordmark right of centre.

    The asymmetry is deliberate and is the reason the component crops rather
    than fits: on a 19.5:9 phone the right-hand empty band is what gets cut,
    so the wordmark stays whole. A centred safe zone would have cut the
    product name in half.
    """
    fig_r = 118.0
    # Vertically centred: the figures span roughly cy-1.5r to cy+0.9r, so the
    # midpoint of the drawn area is a little below H/2. The first version put
    # them at y=300, which left a wide empty band along the bottom and made the
    # composition look like it had slid up out of frame.
    base_y = 372.0

    left_x = 372.0
    right_x = 610.0

    # One shared arm line for both figures, so the hands actually meet.
    arm_y = base_y + fig_r * 0.72 * 0.46

    figures = (
        dome_bot(left_x, base_y, fig_r, facing=+1)
        + terminal_bot(right_x, base_y, fig_r, facing=-1, arm_y=arm_y)
        + joined_hands((left_x + right_x) / 2, arm_y, fig_r)
    )

    text_x = 838.0
    text_y = 424.0
    wordmark = (
        f'<text x="{text_x:.0f}" y="{text_y}" font-family="Inter, Helvetica, Arial, sans-serif" '
        f'font-size="82" font-weight="700" fill="{PAPER}" letter-spacing="-2">'
        f'Clau<tspan fill="{GREEN}">D</tspan>roide</text>'
        f'<path d="M{text_x:.0f},{text_y + 32} L{text_x + 300:.0f},{text_y + 32}" stroke="{CORAL}" '
        f'stroke-width="3" stroke-linecap="round"/>'
        f'<text x="{text_x:.0f}" y="{text_y + 72}" font-family="Inter, Helvetica, Arial, sans-serif" '
        f'font-size="25" font-weight="400" fill="#9FB3A8" letter-spacing="0.3">'
        f'Android native &#183; your own keys</text>'
    )

    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#101813"/>
    <stop offset="1" stop-color="#080B09"/>
  </linearGradient>
  <linearGradient id="dome" x1="0.15" y1="0" x2="0.85" y2="1">
    <stop offset="0" stop-color="{GREEN}"/><stop offset="1" stop-color="{GREEN_DEEP}"/>
  </linearGradient>
  <linearGradient id="body" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="{GREEN_DEEP}"/><stop offset="1" stop-color="#1B8A4C"/>
  </linearGradient>
  <linearGradient id="termBody" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="{CORAL}"/><stop offset="1" stop-color="{CORAL_DEEP}"/>
  </linearGradient>
</defs>
<rect width="{W}" height="{H}" fill="url(#bg)"/>
{figures}
{wordmark}
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
    render(banner(), OUT / "banner.png")
    render(banner(), OUT / "banner-688.png", w=688)
    print("ok")