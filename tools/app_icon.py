#!/usr/bin/env python3
"""Generate the Claudroide adaptive launcher icon from the merged mark.

The launcher icon was a flat colour rectangle (`@color/launcher_fg`), so the
app had no icon at all. This draws the same mark the README uses, at the
sizes Android asks for.

Adaptive icon geometry, which is easy to get wrong:
  - the canvas is 108dp, but only the middle 66dp is guaranteed visible; the
    rest is masked away by whatever launcher shape the user picked.
  - so the mark is scaled to `SAFE_FRACTION` of the canvas and centred, not
    drawn edge to edge. Drawn full-bleed it loses its burst rays to the mask.
  - the foreground layer must be transparent outside the mark.

Outputs, per density: `ic_launcher_foreground.png` (transparent, mark centred
in the safe zone), `ic_launcher_monochrome.png` (Android 13+ themed icons),
plus the legacy `ic_launcher.png` / `ic_launcher_round.png` that some
launchers still request.
"""
import pathlib
import subprocess
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import brand  # noqa: E402  -- sibling module; sys.path must be fixed first

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
BUILD = ROOT / "assets" / "brand" / ".build"

# 108dp canvas, 66dp safe zone.
SAFE_FRACTION = 66.0 / 108.0

# density bucket -> px per dp
DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}

S = 1024.0  # the mark's own design grid


def _split_mark():
    """brand.mark() returns a full <svg>; split it into reusable <defs> + body.

    The body paths reference gradient ids declared in its <defs>, so both
    halves must be carried over or the dome drops to black.
    """
    svg = brand.mark(1024)
    inner = svg.split(">", 1)[1].rsplit("</svg>", 1)[0]
    defs, body = inner.split("</defs>", 1)
    return defs + "</defs>", body


def _face_geometry():
    """Visor and eye geometry, read off the same numbers brand.mark() uses.

    Duplicated rather than imported because brand.mark() builds these inline;
    if that ever changes, the monochrome face drifts. `test_icon_geometry.py`
    asserts these against the rendered mark so the drift cannot pass silently.
    """
    cx, cy = S * 0.500, S * 0.545
    dome_r = S * 0.268
    vw, vh = dome_r * 1.46, dome_r * 0.455
    vx, vy = cx - vw / 2, cy - dome_r * 0.300
    return cx, vh, vw, vx, vy


def _monochrome_defs():
    """A mask that cuts the visor out of the silhouette and keeps the eyes.

    A themed icon is tinted by the launcher, so only the alpha channel
    survives: everything must be flattened to one opaque colour. Flattening
    naively would also swallow the near-black visor and the white eyes into
    the dome, and the face would become a featureless blob -- so the visor is
    cut to transparent and the eyes restored inside the cut.

    The mask is used rather than hand-drawn replacement paths: an earlier
    version rebuilt the dome's arc parameters from scratch and got them wrong,
    which rendered as a black blob, and rebuilding geometry here would mean a
    second place to keep in sync with brand.py.
    """
    _, vh, vw, vx, vy = _face_geometry()
    return (
        '<mask id="faceCut">'
        f'<rect x="0" y="0" width="{S:.0f}" height="{S:.0f}" fill="#ffffff"/>'
        f'<rect x="{vx:.2f}" y="{vy:.2f}" width="{vw:.2f}" height="{vh:.2f}" '
        f'rx="{vh / 2:.2f}" fill="#000000"/>'
        "</mask>"
    )


def _eyes_svg():
    cx, vh, vw, vx, vy = _face_geometry()
    er = vh * 0.300
    cy = vy + vh / 2
    return (
        f'<circle cx="{cx - vw * 0.235:.2f}" cy="{cy:.2f}" r="{er:.2f}" fill="#ffffff"/>'
        f'<circle cx="{cx + vw * 0.235:.2f}" cy="{cy:.2f}" r="{er:.2f}" fill="#ffffff"/>'
    )


def _flatten(body):
    """Recolour every gradient-filled shape to solid white."""
    for fill in ("url(#burst)", "url(#burstFront)", "url(#dome)"):
        body = body.replace(f'fill="{fill}"', 'fill="#ffffff"')
    return body


def layer_svg(size, monochrome=False):
    """The mark alone on a transparent canvas, centred in the safe zone."""
    defs, body = _split_mark()
    mark = size * SAFE_FRACTION
    offset = (size - mark) / 2.0
    transform = f"translate({offset:.3f},{offset:.3f}) scale({mark / S:.6f})"

    if monochrome:
        # The visor was already drawn near-black in `body`; flattening turns it
        # white, then the mask cuts it back out and the eyes go on top.
        inner = (
            f'<g mask="url(#faceCut)">{_flatten(body)}</g>{_eyes_svg()}'
        )
        return (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
            f'viewBox="0 0 {size} {size}">'
            f"<defs>{_monochrome_defs()}</defs>"
            f'<g transform="{transform}">{inner}</g>'
            f"</svg>"
        )

    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 {size} {size}">'
        f"<defs>{defs}</defs>"
        f'<g transform="{transform}">{body}</g>'
        f"</svg>"
    )


def render_layer(svg, out_path, size):
    BUILD.mkdir(parents=True, exist_ok=True)
    src = BUILD / (out_path.stem + "-" + out_path.parent.name + ".svg")
    src.write_text(svg, encoding="utf-8")
    subprocess.run(
        ["rsvg-convert", str(src), "-o", str(out_path), "-w", str(size), "-h", str(size)],
        check=True,
    )
    return out_path


def main():
    written = []
    for bucket, scale in DENSITIES.items():
        out_dir = RES / f"mipmap-{bucket}"
        if not out_dir.is_dir():
            continue
        size = int(round(108 * scale))
        written.append(render_layer(layer_svg(size), out_dir / "ic_launcher_foreground.png", size))
        written.append(
            render_layer(
                layer_svg(size, monochrome=True),
                out_dir / "ic_launcher_monochrome.png",
                size,
            )
        )
        # `ic_launcher` itself is the adaptive-icon XML, not a bitmap: a density
        # folder may hold either `ic_launcher.xml` or `ic_launcher.png`, not both,
        # and the build fails with "Duplicate resources" if it holds both. Only
        # the round legacy bitmap is emitted, under its own name.
        written.append(render_layer(layer_svg(size), out_dir / "ic_launcher_round.png", size))
    for p in written:
        print("wrote", p.relative_to(ROOT))


if __name__ == "__main__":
    main()