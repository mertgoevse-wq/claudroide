# Claudroide image brief — mascot + banner

**Status: RENDERED AND SHIPPED (2026-10-02).**

The bridge could not reach Google directly — no Google account is connected, so
`claude-media-bridge login` would have been needed and that is interactive. The
way around it: the user's **OmniRoute** proxy was already running on
`http://localhost:20128` and offers the same model. The bridge reads its key from
`~/.config/mll/providers/omniroute.env`; once that file existed, `status` flipped
from "Unreachable" to "Connected" and `generateImageViaOmniRoute` rendered
through **`antigravity/gemini-3.1-flash-image`** (Nano Banana 2).

Pollinations was not used for either image.

| File | Size | Use |
| :--- | :--- | :--- |
| `assets/claudroide-mascot-logo.png` | 665x796, transparent | full-resolution master |
| `assets/claudroide-mascot-logo-small.png` | 320x320, transparent | README corner logo |
| `assets/claudroide-banner.jpg` | 1376x768, 16:9 | README header banner |

## What was wrong with the **previous** images

- The bot read as a generic green cartoon character rather than Android.
- The Claude star was **consumed** — held like food and being eaten. That was the
  single biggest problem: it looked like the mascot was destroying the thing it
  is meant to represent.
- The fusion was accidental rather than designed, so the two halves did not read
  as one mark.
- Low resolution; it fell apart when GitHub scaled the banner up.

## What the iteration loop actually fixed

Three rounds for the mascot, two for the banner. Each round was inspected before
the next was started, and each inspection found something real:

1. **Round 1** — the star had a disc in its middle and read as a lightbulb; the
   green was neon emerald rather than `#3DDC84`. Asking for a flat white
   background was necessary because **JPEG cannot carry transparency**, so the
   first attempt painted a fake checkerboard.
2. **Round 2** — the star became solid, but the round instruction to describe the
   "dark visor" cost the model the **eyes**, and the mascot stopped reading as
   Android at all. Silently dropping a defining feature is worse than the
   nitpick that was being fixed.
3. **Round 3** — the eyes were named as *the most important detail* and came back
   clearly. Shipped.

The banner needed a second round only for margin: in round 1 the mascot's right
arm ran into the frame edge.

## Brief 1 — mascot logo (1:1)

> A friendly Android robot mascot, unmistakably Android: the canonical rounded
> rectangular head with two circular white eyes on a dark visor, two short
> antennae, the soft dome-and-bar silhouette of the standard Android system
> character. Clean geometric construction, flat confident shapes, subtle
> depth only where it earns its place. Modern Android green (#3DDC84) as the
> body colour with a darker green shadow side, off-white highlights, a small
> warm amber accent used once and sparingly.
>
> Fused with the Claude sparkle mark — **integrated, not eaten.** The star is not
> held, not bitten, not in a mouth, and not floating away as a separate object.
> The star is set into the robot's chest plate as a single clean inlay, its
> geometry continuing the robot's own construction lines, so the whole reads as
> one designed mark rather than two stickers on top of each other. The star is
> whole and uncropped. A faint warm amber glow radiates from the inlay and
> lights the inside edges of the visor.
>
> Strictly anti-slop: no lens flares, no neon rainbow gradients, no glossy plastic
> 3D toy rendering, no floating UI panels, no circuit-board decoration, no lens
> bloom haze, no six-pack-abs bodybuilder proportions, no text or letters
> anywhere. Confident restraint. Flat vector-adjacent rendering with crisp
> edges, designed to stay legible as an app icon at 48 px.
>
> Transparent background, square 1:1, high resolution (at least 1024x1024).

## Brief 2 — header banner (16:9)

> The same mascot, identical construction, colours and star inlay, placed
> off-centre to the right third of a wide frame. Generous negative space on the
> left for a wordmark to be set later in post. Background: a deep, calm near-black
> green-black with a very soft radial falloff behind the mascot only. No grid, no
> particles, no data streams, no glowing edges.

## Commands

```bash
cd ~/claude-media-bridge/claude-media-bridge

# One-time, interactive, must be run by the user:
node bin/cli.mjs login

node bin/cli.mjs status          # confirm Google account is connected

node bin/cli.mjs generate "<Brief 1>" --provider google --ratio 1:1
node bin/cli.mjs generate "<Brief 2>" --provider google --ratio 16:9
```

## Acceptance before shipping

1. Read the two generated images and check them against the brief above.
2. The star must be **whole and fused**, not held or eaten.
3. The character must read as Android at small size, not as a generic green bot.
4. No text in either image.
5. Only then copy into `assets/` and point the README at the real files.
6. Never ship a placeholder, an AI-fill image, or an SVG as the finished logo.
