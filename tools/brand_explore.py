#!/usr/bin/env python3
"""Four candidate constructions for the ClauDroide mark, rendered side by side.

The brief: an Android droid and the Claude spark fused so that NEITHER reads as
decoration on the other. The test applied to each candidate is deletion -- if
you can remove one half and still have a complete mark, the fusion failed.
"""
import math, pathlib, subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
BUILD = ROOT / "assets" / "brand" / ".build"
BUILD.mkdir(parents=True, exist_ok=True)

GREEN, GREEN_D = "#3DDC84", "#27B96C"
CORAL, CORAL_D = "#E0775B", "#C8553A"
VISOR, EYE = "#16211C", "#F4F8F5"


def spark(cx, cy, rx, ry, c=0.17):
    """Four-point spark with concave sides. c = waist; lower is sharper."""
    kx, ky = rx * c, ry * c
    return (f"M{cx:.1f},{cy-ry:.1f} Q{cx+kx:.1f},{cy-ky:.1f} {cx+rx:.1f},{cy:.1f} "
            f"Q{cx+kx:.1f},{cy+ky:.1f} {cx:.1f},{cy+ry:.1f} "
            f"Q{cx-kx:.1f},{cy+ky:.1f} {cx-rx:.1f},{cy:.1f} "
            f"Q{cx-kx:.1f},{cy-ky:.1f} {cx:.1f},{cy-ry:.1f} Z")


def ray(cx, cy, deg, r0, r1, hw):
    a = math.radians(deg - 90.0)
    ux, uy = math.cos(a), math.sin(a)
    px, py = -uy, ux
    P = lambda L, A: (cx + ux*L + px*A, cy + uy*L + py*A)
    bl, br_, tip = P(r0, -hw), P(r0, hw), P(r1, 0)
    cl, cr = P(r0 + (r1-r0)*0.5, -hw*0.22), P(r0 + (r1-r0)*0.5, hw*0.22)
    return (f"M{bl[0]:.1f},{bl[1]:.1f} Q{cl[0]:.1f},{cl[1]:.1f} {tip[0]:.1f},{tip[1]:.1f} "
            f"Q{cr[0]:.1f},{cr[1]:.1f} {br_[0]:.1f},{br_[1]:.1f} Z")


def antennae(cx, cy_head, hw, S):
    """Two angled antennae with round caps, the Android way."""
    out = ""
    for s in (-1, 1):
        x0 = cx + s * hw * 0.52
        y0 = cy_head - S*0.004
        x1 = cx + s * hw * 0.97
        y1 = cy_head - S*0.150
        out += (f'<line x1="{x0:.1f}" y1="{y0:.1f}" x2="{x1:.1f}" y2="{y1:.1f}" '
                f'stroke="{GREEN}" stroke-width="{S*0.026:.1f}" stroke-linecap="round"/>')
    return out


def head(cx, cy, hw, hh, S, fill="url(#g)"):
    """Android half-dome head with visor and two eyes."""
    d = (f'<path d="M{cx-hw:.1f},{cy:.1f} a{hw:.1f},{hh:.1f} 0 0 1 {hw*2:.1f},0 Z" fill="{fill}"/>')
    vw, vh = hw*1.26, hh*0.40
    vx, vy = cx-vw/2, cy-hh*0.66
    d += f'<rect x="{vx:.1f}" y="{vy:.1f}" width="{vw:.1f}" height="{vh:.1f}" rx="{vh/2:.1f}" fill="{VISOR}"/>'
    er = vh*0.29
    for s in (-1, 1):
        d += f'<circle cx="{cx+s*vw*0.25:.1f}" cy="{vy+vh/2:.1f}" r="{er:.1f}" fill="{EYE}"/>'
    return d


def limbs(cx, body_top, body_w, body_h, S):
    """Arm and leg pills, with the Android gap from the torso."""
    aw, ah = body_w*0.185, body_h*0.74
    gap = body_w*0.085
    out = ""
    for s in (-1, 1):
        x = cx + s*(body_w/2 + gap) - aw/2
        out += (f'<rect x="{x:.1f}" y="{body_top+body_h*0.06:.1f}" width="{aw:.1f}" '
                f'height="{ah:.1f}" rx="{aw/2:.1f}" fill="url(#g)"/>')
    lw, lh = body_w*0.245, body_h*0.46
    for s in (-1, 1):
        x = cx + s*body_w*0.235 - lw/2
        out += (f'<rect x="{x:.1f}" y="{body_top+body_h*0.80:.1f}" width="{lw:.1f}" '
                f'height="{lh:.1f}" rx="{lw/2:.1f}" fill="url(#g)"/>')
    return out


DEFS = f'''<defs>
<linearGradient id="g" x1="0.1" y1="0" x2="0.9" y2="1">
 <stop offset="0" stop-color="{GREEN}"/><stop offset="1" stop-color="{GREEN_D}"/></linearGradient>
<linearGradient id="c" x1="0.1" y1="0" x2="0.9" y2="1">
 <stop offset="0" stop-color="{CORAL}"/><stop offset="1" stop-color="{CORAL_D}"/></linearGradient>
</defs>'''


def variant_A(S):
    """Spark IS the torso. Delete it and the droid has no body."""
    cx = S*0.5
    hw, hh = S*0.255, S*0.215
    head_base = S*0.405
    bt, bw, bh = S*0.445, S*0.51, S*0.345
    g = antennae(cx, head_base-hh, hw, S) + head(cx, head_base, hw, hh, S)
    g += limbs(cx, bt, bw, bh, S)
    g += f'<path d="{spark(cx, bt+bh*0.46, bw*0.56, bh*0.72, 0.16)}" fill="url(#c)"/>'
    return g


def variant_B(S):
    """Uniform 12-ray burst; two rays are green and act as the antennae."""
    cx, cy = S*0.5, S*0.52
    r0, r1, hw = S*0.145, S*0.455, S*0.0235
    g = ""
    for i in range(12):
        deg = i*30 + 15
        col = "url(#g)" if deg in (345, 15) else "url(#c)"
        g += f'<path d="{ray(cx, cy, deg, r0, r1, hw)}" fill="{col}"/>'
    g += head(cx, cy+S*0.105, S*0.205, S*0.175, S)
    return g


def variant_C(S):
    """Spark as torso AND coral antennae, so coral owns top and centre."""
    cx = S*0.5
    hw, hh = S*0.255, S*0.215
    head_base = S*0.405
    bt, bw, bh = S*0.445, S*0.51, S*0.345
    g = ""
    for s in (-1, 1):
        g += (f'<line x1="{cx+s*hw*0.52:.1f}" y1="{head_base-hh:.1f}" '
              f'x2="{cx+s*hw*0.97:.1f}" y2="{head_base-hh-S*0.150:.1f}" '
              f'stroke="{CORAL}" stroke-width="{S*0.027:.1f}" stroke-linecap="round"/>')
    g += head(cx, head_base, hw, hh, S) + limbs(cx, bt, bw, bh, S)
    g += f'<path d="{spark(cx, bt+bh*0.46, bw*0.56, bh*0.72, 0.16)}" fill="url(#c)"/>'
    return g


def variant_D(S):
    """Large coral spark; the droid head nests in its centre, same hub."""
    cx, cy = S*0.5, S*0.5
    g = f'<path d="{spark(cx, cy, S*0.455, S*0.470, 0.22)}" fill="url(#c)"/>'
    g += antennae(cx, cy+S*0.012, S*0.175, S)
    g += head(cx, cy+S*0.135, S*0.178, S*0.150, S)
    return g


def sheet():
    S, G = 420, 28
    W = G + 4*(S+G)
    H = S + 2*G + 56
    body = f'<rect width="{W}" height="{H}" fill="#101513"/>'
    for i, (name, fn) in enumerate([("A torso-spark", variant_A), ("B shared burst", variant_B),
                                    ("C coral crown", variant_C), ("D nested hub", variant_D)]):
        x = G + i*(S+G)
        body += f'<g transform="translate({x},{G})">{fn(S)}</g>'
        body += (f'<text x="{x+S/2}" y="{G+S+36}" fill="#8A958D" font-family="Inter" '
                 f'font-size="20" text-anchor="middle">{name}</text>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">{DEFS}{body}</svg>'


if __name__ == "__main__":
    p = BUILD / "explore.svg"
    p.write_text(sheet(), encoding="utf-8")
    subprocess.run(["rsvg-convert", str(p), "-o", str(BUILD / "explore.png")], check=True)
    print("ok")
