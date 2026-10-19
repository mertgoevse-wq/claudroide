#!/usr/bin/env python3
"""Reproducibly generate WebP assets for ClauDroide from source PNGs.

Sources:
  - assets/brand/mark-1024.png -> app/src/main/res/drawable-nodpi/claudroide_mark.webp
  - assets/brand/banner.png    -> app/src/main/res/drawable-nodpi/claudroide_banner.webp

Both must stay under the 50 KB cap enforced by BrandAssetContractTest.
"""
import os
import pathlib
import subprocess
import sys
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
BRAND = ROOT / "assets" / "brand"
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
CAP_BYTES = 50 * 1024


def convert_to_webp(src_png: pathlib.Path, dst_webp: pathlib.Path, quality: int = 95):
    if not src_png.exists():
        raise FileNotFoundError(f"Source PNG not found: {src_png}")

    # Use cwebp if available for optimal compression, fallback to Pillow
    try:
        subprocess.run(
            ["cwebp", "-q", str(quality), str(src_png), "-o", str(dst_webp)],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    except (subprocess.SubprocessError, FileNotFoundError):
        img = Image.open(src_png)
        img.save(dst_webp, format="WEBP", quality=quality)

    size = dst_webp.stat().st_size
    if size > CAP_BYTES:
        raise ValueError(f"{dst_webp.name} is {size} bytes, exceeding cap of {CAP_BYTES} bytes")
    print(f"Generated {dst_webp.name}: {size} bytes ({size / 1024:.1f} KB) from {src_png.name}")
    return size


def main():
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    mark_src = BRAND / "mark-1024.png"
    mark_dst = DRAWABLE / "claudroide_mark.webp"
    banner_src = BRAND / "banner.png"
    banner_dst = DRAWABLE / "claudroide_banner.webp"

    s1 = convert_to_webp(mark_src, mark_dst, quality=95)
    s2 = convert_to_webp(banner_src, banner_dst, quality=95)
    print("All WebP assets generated successfully under 50 KB cap.")


if __name__ == "__main__":
    main()
