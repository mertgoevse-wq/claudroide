#!/usr/bin/env python3
"""Generate Android mipmap launcher icons and WebP brand mark from assets/brand/1791468217945.jpg.

Generates:
- mipmap-*/ic_launcher_round.png (circular launcher icons for all density buckets)
- mipmap-*/ic_launcher_foreground.png (adaptive icon foreground with circular crop / safe zone)
- mipmap-*/ic_launcher_monochrome.png (monochrome icon for Android 13+ themed icons)
- app/src/main/res/drawable-nodpi/claudroide_mark.webp (<= 50 KB WebP for BrandMark component)
"""
import pathlib
import sys
from PIL import Image, ImageDraw, ImageOps

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
SRC_JPG = ROOT / "assets" / "brand" / "1791468217945.jpg"
DRAWABLE_NODPI = RES / "drawable-nodpi"

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

def create_circular_icon(img: Image.Image, size: int) -> Image.Image:
    """Create a circular launcher icon with transparent background outside circle."""
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)

    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(resized, (0, 0), mask=mask)
    return out

def create_adaptive_foreground(img: Image.Image, canvas_size: int) -> Image.Image:
    """Create adaptive icon foreground (108dp canvas) with the artwork scaled into the safe zone."""
    # 66dp out of 108dp is guaranteed visible safe zone
    safe_zone_size = int(round(canvas_size * (66.0 / 108.0)))
    offset = (canvas_size - safe_zone_size) // 2

    # Scale artwork to fit inside safe zone
    resized = img.resize((safe_zone_size, safe_zone_size), Image.Resampling.LANCZOS)

    # Soft circular mask for safe zone with a subtle margin
    mask = Image.new("L", (safe_zone_size, safe_zone_size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, safe_zone_size - 1, safe_zone_size - 1), fill=255)

    out = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    out.paste(resized, (offset, offset), mask=mask)
    return out

def create_monochrome_foreground(fg_rgba: Image.Image) -> Image.Image:
    """Create themed monochrome icon from RGBA foreground."""
    alpha = fg_rgba.split()[3]
    mono = Image.new("RGBA", fg_rgba.size, (255, 255, 255, 0))
    mono.putalpha(alpha)
    return mono

def main():
    if not SRC_JPG.exists():
        print(f"Error: {SRC_JPG} does not exist", file=sys.stderr)
        sys.exit(1)

    src_img = Image.open(SRC_JPG).convert("RGB")
    print(f"Loaded source image {SRC_JPG.name}: {src_img.size}")

    # 1. Update mipmap icons for each density
    for bucket, scale in DENSITIES.items():
        out_dir = RES / f"mipmap-{bucket}"
        out_dir.mkdir(parents=True, exist_ok=True)

        legacy_size = int(round(48 * scale))
        adaptive_size = int(round(108 * scale))

        # Round icon (used by modern launchers as round icon)
        round_icon = create_circular_icon(src_img, legacy_size)
        round_icon.save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)

        # Adaptive foreground (108dp canvas)
        fg_icon = create_adaptive_foreground(src_img, adaptive_size)
        fg_icon.save(out_dir / "ic_launcher_foreground.png", "PNG", optimize=True)

        # Monochrome icon
        mono_icon = create_monochrome_foreground(fg_icon)
        mono_icon.save(out_dir / "ic_launcher_monochrome.png", "PNG", optimize=True)

        print(f"Generated icons for mipmap-{bucket}: round {legacy_size}px, fg {adaptive_size}px")

    # 2. Update claudroide_mark.webp
    mark_webp_path = DRAWABLE_NODPI / "claudroide_mark.webp"
    mark_img = create_circular_icon(src_img, 512)
    mark_img.save(mark_webp_path, "WEBP", quality=90)
    size_kb = mark_webp_path.stat().st_size / 1024.0
    print(f"Generated {mark_webp_path.name}: {size_kb:.1f} KB (must be <= 50 KB)")
    assert mark_webp_path.stat().st_size <= 50 * 1024, "claudroide_mark.webp exceeds 50 KB cap!"

    print("App icon and mark update complete successfully!")

if __name__ == "__main__":
    main()
