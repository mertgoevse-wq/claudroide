# Claudroide image brief — mascot + banner

**Status:** briefs ready, **not yet rendered.** The Claude Media Bridge is installed
at `~/claude-media-bridge` but has no Google account connected
(`claude-media-bridge status` → "Not logged in"). The only configured provider
right now is Pollinations, which is explicitly **not** wanted for these images.
So: no image has been generated and no placeholder has been installed in its place.

Provider wanted: **Google Antigravity / Nano Banana 2 (`gemini-3.1-flash-image`)**.

To unblock: run `claude-media-bridge login` once, interactively. It is an OAuth
flow against the user's own Google account and cannot be done from an agent
session.

Files to replace when the renders are approved:

| File | Use | Ratio |
| :--- | :--- | :--- |
| `assets/claudroide-mascot-logo.jpg` | README corner logo, 160 px | 1:1 |
| `assets/claudroide-banner.jpg` | README header banner, full width | 16:9 |

## What was wrong with the current images

- The bot reads as a generic green cartoon character rather than Android.
- The Claude star is **consumed** — held like food and being eaten. That is the
  single biggest problem: it looks like the mascot is destroying the thing it is
  meant to represent.
- The fusion is accidental rather than designed, so the two halves do not read as
  one mark.
- Low resolution; it falls apart when GitHub scales the banner up.

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
