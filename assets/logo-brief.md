# Claudroide brand brief — the mark, the banner, the launcher icon

**Status: shipped as vector geometry (2026-10-02).**

## What ships

| File | Size | Use |
| :--- | :--- | :--- |
| `assets/brand/mark-1024.png` | 1024x1024 | master mark |
| `assets/brand/mark-256.png` | 256x256 | README corner mark |
| `assets/brand/mark-96.png` | 96x96 | small use |
| `assets/brand/banner.png` | 1376x768, 16:9 | README header |
| `app/src/main/res/mipmap-*/ic_launcher_foreground.png` | 108dp per density | adaptive icon foreground |
| `app/src/main/res/mipmap-*/ic_launcher_monochrome.png` | 108dp per density | themed icons, Android 13+ |
| `app/src/main/res/mipmap-*/ic_launcher_round.png` | 108dp per density | legacy round icon |

## The design

An Android dome in `#3DDC84`, wearing a Claude-grammar burst as its crown.

The two rays at **±26°** are the droid's antennae **and** two rays of the burst.
The same strokes serve both systems, so neither half can be deleted without
breaking the other. That is the brief's real requirement — "neither symbol may
read as subordinate" — expressed as geometry rather than as a caption. A caption
can claim a fusion the drawing does not have; shared strokes cannot.

The two lowest rays (at ±157°) are drawn **over** the dome rather than behind
it. If the burst sat entirely behind, the droid would be carrying a decoration
instead of being part of one.

## Why geometry and not an image model

The wordmark has to spell "Claudroide". Diffusion models misspell wordmarks — the
earlier renders produced a mascot holding a spark, and before that a spark with
a lightbulb disc in the middle. A misspelled product name on a 1376px banner is
worse than no banner.

So every glyph is real Inter, positioned by measured advance width, and the mark
is exact path geometry. `tools/brand.py` owns the mark; `tools/banner.py` and
`tools/app_icon.py` import it. One source, so the banner and the launcher icon
cannot drift apart.

## Palette

| Token | Value | Use |
| :--- | :--- | :--- |
| Brand green | `#3DDC84` | the dome; Android green, already the app's primary |
| Brand green deep | `#24B566` | the dome's shaded side |
| Brand coral | `#E06D53` | the burst; the app's existing secondary |
| Brand coral deep | `#C4553C` | the burst's lower half |
| Visor | `#15201B` | near-black, not pure black (AMOLED smear) |
| Ink | `#0C100E` | banner background |

## Two geometry bugs found by inspecting the output

Both were invisible in the colour render and obvious in the monochrome one,
which is why generating the themed icon was worth doing at all.

1. **The rays started outside the dome.** They began at radius `0.150·S` from
   the burst centre, but the dome's surface crosses that radius only at about
   `0.107·S`. The upward antennae therefore floated above the head with a visible
   gap. The colour render hid it — the dome is painted over the join — but a
   silhouette has no colour to hide it with. Fixed by starting the rays at
   `0.090·S`, comfortably inside for every angle.

2. **The monochrome layer was a black blob.** The first version rebuilt the
   dome's arc parameters by hand and got them wrong. It is now a mask over the
   *real* geometry: flatten every fill to white, cut the visor out, redraw the
   eyes. Nothing is re-derived by hand, so a change to `brand.py` is picked up
   automatically instead of silently diverging.

## Regenerating

```sh
python3 tools/brand.py     # mark-1024 / -256 / -96
python3 tools/banner.py    # banner.png
python3 tools/app_icon.py  # all five mipmap densities
```

Requires `rsvg-convert` and the Inter family installed for fontconfig.

## What the previous images got wrong

Worth recording, because these are the failures the current design exists to
avoid:

- The bot read as a generic green cartoon character rather than Android.
- The Claude spark was **consumed** — held like food and being eaten. It looked
  like the mascot was destroying the thing it is meant to represent.
- The fusion was accidental rather than designed, so the two halves did not read
  as one mark.
- Low resolution; it fell apart when GitHub scaled the banner up.

## Licence note

No image model produced any shipped asset. No AI-generated image is committed to
this repository, so no third-party image licence applies.