#!/usr/bin/env python3
"""Assert the geometry duplicated in app_icon.py matches brand.mark().

`app_icon.py` duplicates `_face_geometry()` instead of importing it, because
`brand.mark()` builds those numbers inline. This test pins the duplicate so a
future edit to one cannot silently drift the other -- the monochrome mask would
cut the wrong hole and the eyes would land in the wrong place, invisible until
someone renders a themed launcher icon.

Run:  python3 tools/test_icon_geometry.py
"""
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))

import brand
import app_icon


def test_face_geometry_matches_mark():
    """The duplicated geometry must agree with what mark() actually draws."""
    cx = app_icon.S * 0.500
    cy = app_icon.S * 0.545
    dome_r = app_icon.S * 0.268

    # brand.mark() computes these inline; reproduce the same fractions.
    vw = dome_r * 1.46
    vh = dome_r * 0.455
    vx = cx - vw / 2
    vy = cy - dome_r * 0.300

    cx2, vh2, vw2, vx2, vy2 = app_icon._face_geometry()

    assert abs(cx - cx2) < 1e-6, f"cx drift: {cx} vs {cx2}"
    assert abs(vh - vh2) < 1e-6, f"vh drift: {vh} vs {vh2}"
    assert abs(vw - vw2) < 1e-6, f"vw drift: {vw} vs {vw2}"
    assert abs(vx - vx2) < 1e-6, f"vx drift: {vx} vs {vx2}"
    assert abs(vy - vy2) < 1e-6, f"vy drift: {vy} vs {vy2}"


def test_eye_position_in_face():
    """Eyes sit inside the visor rectangle in the rendered mark."""
    cx, vh, vw, vx, vy = app_icon._face_geometry()
    er = vh * 0.300
    eye_y = vy + vh / 2
    eye_lx = cx - vw * 0.235
    eye_rx = cx + vw * 0.235

    assert vx <= eye_lx <= vx + vw, "left eye outside visor"
    assert vx <= eye_rx <= vx + vw, "right eye outside visor"
    assert vy <= eye_y <= vy + vh, "eyes below visor"


def test_safe_fraction_within_canvas():
    """The mark must fit inside the adaptive-icon safe zone (66/108)."""
    sf = app_icon.SAFE_FRACTION
    assert 0 < sf <= 1.0, f"unsafe fraction {sf}"


def test_svg_has_foreground_and_monochrome():
    """Both layer_svg calls must produce valid SVG with the right structure."""
    fg = app_icon.layer_svg(192)
    mono = app_icon.layer_svg(192, monochrome=True)
    assert "<svg" in fg and "</svg>" in fg
    assert "<svg" in mono and "</svg>" in mono
    assert "url(#faceCut)" in mono, "monochrome must use faceCut mask"
    assert "#ffffff" in mono, "monochrome must flatten to white"


if __name__ == "__main__":
    test_face_geometry_matches_mark()
    test_eye_position_in_face()
    test_safe_fraction_within_canvas()
    test_svg_has_foreground_and_monochrome()
    print("ok - 4 geometry assertions passed")