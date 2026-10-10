# Native Android design override for ClauDroide

This file is the tightened implementation source of truth for the accompanying Stitch export. It supersedes inconsistent font, card, button, and mock-device details in the original `DESIGN.md` and exported `code.html` files.

- **UI type:** one native sans-serif / existing Material 3 typography. No serif title, no runtime Google Fonts/CDN dependency.
- **Code type:** system monospace or an already bundled licensed monospace family. Never use monospaced type for general UI labels.
- **Body:** 16sp/24sp preferred; secondary 14sp/20sp; metadata 12sp/16sp sparingly. Respect font scaling.
- **Color:** neutral light/dark surfaces, restrained warm accent for one primary action or current selection; remove competing violet/green/orange decoration.
- **Containers:** no card-in-card stacks. Chat responses sit directly on canvas. Reserve panels for composer, code, attachments, artifacts, real tool runs, and modal sheets.
- **Buttons:** one primary action per section; secondary actions are quiet; icon-only actions have 44–48dp hit area; principal controls are at least 48dp high.
- **Navigation:** conversation-first. Chat, Projects, Coding as the main destinations; Tools/Integrations/Settings secondary. Media controls are contextual.
- **Android behavior:** use actual system bars, system insets, IME, native back navigation and lifecycle-aware state. Do not reproduce the status bar or phone shell in app content.
- **Implementation:** HTML/CSS is reference only. Rebuild with the application's actual UI toolkit and preserve business logic.
- **Motion:** subtle, native, state-driven; no fake typing, fake progress, shimmer wallpaper, or pixel-generation animation unless the real backend streams partial image data.

For detailed colors, spacing, screens and acceptance criteria see `ANDROID_HANDOFF/02_NATIVE_DESIGN_SPEC.md`. For motion see `ANDROID_HANDOFF/03_MOTION_SPEC.md`.
