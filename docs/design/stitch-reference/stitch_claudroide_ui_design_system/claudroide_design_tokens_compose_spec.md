# Claudroide Design System & Android Jetpack Compose Reference
**Target Device:** Samsung Galaxy A56 5G (Display: 6.7" Super AMOLED, 1080 x 2340 px, ~390dp x 844dp viewport, One UI 6 / Android 14+)

---

## 1. Design-Philosophie & Tonalität
- **Ruhig & Hochwertig:** Inspiriert von der typografischen Eleganz und visuellen Ruhe von Claude Desktop/Mobile, kombiniert mit nativer Android/One UI Ergonomie (Einhand-Erreichbarkeit, untere Daumenzone).
- **Kein Cyberpunk, keine Neon-Exzesse:** Ruhige Oberflächen in mattem Obsidian-Graphit (`#181716`), warme Papyrus/Haferflocken-Nuancen (`#FBF9F5` / `#F3EFEA`), Akzentfarben in warmem Terrakotta/Claude-Clay (`#D97757`).
- **Präzise Entwickler-Typografie:** Lesbare Monospace (`JetBrains Mono`), humanistische Serife für Headlines (`Newsreader` / Editorial), hochlesbare Sans (`Inter` / System Sans) für Interface und Tool-Call-Zustände.

---

## 2. Design Tokens (Compose Theme Mappings)

### Farbpalette (Color Palette)

| Token Name | Hex Light | Hex Dark | Beschreibung / Rolle |
|---|---|---|---|
| `claudroide_surface` | `#FAF8F5` | `#161514` | Primärer App-Hintergrund |
| `claudroide_surface_container` | `#F2EDE4` | `#201F1D` | Composer, Bubble Bot, Tool Card Container |
| `claudroide_surface_high` | `#E8E2D8` | `#2A2825` | Modals, Dropdowns, Bottom Sheets |
| `claudroide_surface_user_bubble` | `#EBE4D8` | `#282623` | Benutzer-Nachrichtenblasen (subtil abgesetzt) |
| `claudroide_primary_accent` | `#C86241` | `#D97757` | Terrakotta/Warm Clay (Buttons, Send, Active States) |
| `claudroide_primary_on_accent` | `#FFFFFF` | `#161514` | Text auf primärem Akzent |
| `claudroide_border_subtle` | `#E2DDD4` | `#2D2B28` | Haarlinien (1dp), Code-Block Rahmen |
| `claudroide_text_primary` | `#1F1E1C` | `#EDEAE4` | Fließtext, Code |
| `claudroide_text_secondary` | `#63605A` | `#A39F97` | Metadaten, Tool-Status, Timestamps |
| `claudroide_text_tertiary` | `#8F8B83` | `#736F67` | Placeholders, Keyboard hints |

#### Diff & Terminal Tokens
- `diff_added_bg`: `#1B3322` (Dark) / `#E8F5E9` (Light)
- `diff_added_text`: `#7CE39B` (Dark) / `#1B5E20` (Light)
- `diff_removed_bg`: `#3B1D1D` (Dark) / `#FFEBEE` (Light)
- `diff_removed_text`: `#F87171` (Dark) / `#B71C1C` (Light)
- `terminal_bg`: `#11100F`
- `terminal_text`: `#E4E0D7`
- `terminal_cyan`: `#79C0FF`
- `terminal_amber`: `#E3B341`

#### Skill-Herkunft Provenance Badges
- **Anthropic Claude:** Background `#3D271D`, Border `#6A3E2C`, Text `#F09A75` (Warm Amber/Clay)
- **Local Skill:** Background `#23272E`, Border `#353B45`, Text `#90A4AE` (Slate Steel)
- **Personal Skill:** Background `#1D2F27`, Border `#2E4E40`, Text `#81C784` (Sage Green)
- **Marketplace:** Background `#222438`, Border `#393D64`, Text `#9FA8DA` (Muted Indigo)
- **GitHub:** Background `#2C2234`, Border `#4D3A5C`, Text `#CE93D8` (Muted Plum)

---

## 3. Typografie (Jetpack Compose Typography Scale)
- **Display / Model Title:** `Newsreader`, Medium, 20sp / 26sp Line-height
- **Body Large (Chat Stream):** `Inter` / System Sans, Regular, 15sp / 23sp Line-height (für entspanntes längeres Lesen auf dem A56)
- **Body Medium (UI Controls):** 13.5sp / 18sp
- **Code & Tool Inspector:** `JetBrains Mono`, Regular, 12.5sp / 19sp Line-height
- **Label Small (Badges, Chips):** 11sp / 14sp, SemiBold, Tracking +0.3sp

---

## 4. Spacing, Touch Targets & Ergonomie (Galaxy A56)
- **Base Grid:** 4dp / 8dp / 16dp / 24dp
- **Minimum Touch Target:** 48dp x 48dp (One UI Accessibility Standard)
- **Bottom Navigation / Thumb Zone:** Composer und Bottom Sheets innerhalb der unteren 40% des Bildschirms platziert.
- **Corner Radius Tokens:**
  - `Small`: 6dp (Badges, Buttons inline)
  - `Medium`: 12dp (Codeblöcke, Tool-Cards)
  - `Large`: 20dp (Composer Container, Chat Bubbles)
  - `Full`: 999dp (Floating Buttons, Action Pills)
  - `Bottom Sheet Top`: 24dp
