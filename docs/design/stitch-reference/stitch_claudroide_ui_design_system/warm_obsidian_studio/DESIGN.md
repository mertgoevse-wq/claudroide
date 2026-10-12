---
name: Warm Obsidian Studio
colors:
  surface: '#141311'
  surface-dim: '#141311'
  surface-bright: '#3a3937'
  surface-container-lowest: '#0f0e0c'
  surface-container-low: '#1c1b19'
  surface-container: '#201f1d'
  surface-container-high: '#2b2a28'
  surface-container-highest: '#363532'
  on-surface: '#e6e2de'
  on-surface-variant: '#dbc1b9'
  inverse-surface: '#e6e2de'
  inverse-on-surface: '#31302e'
  outline: '#a38c85'
  outline-variant: '#55433d'
  surface-tint: '#ffb59e'
  primary: '#ffb59e'
  on-primary: '#5c1902'
  primary-container: '#d97757'
  on-primary-container: '#541400'
  inverse-primary: '#99462a'
  secondary: '#b9ccb0'
  on-secondary: '#253421'
  secondary-container: '#3d4d38'
  on-secondary-container: '#abbea2'
  tertiary: '#faba79'
  on-tertiary: '#492900'
  tertiary-container: '#bf864a'
  on-tertiary-container: '#422400'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdbd0'
  primary-fixed-dim: '#ffb59e'
  on-primary-fixed: '#390b00'
  on-primary-fixed-variant: '#7a2f15'
  secondary-fixed: '#d5e8cb'
  secondary-fixed-dim: '#b9ccb0'
  on-secondary-fixed: '#101f0d'
  on-secondary-fixed-variant: '#3b4b36'
  tertiary-fixed: '#ffdcbd'
  tertiary-fixed-dim: '#faba79'
  on-tertiary-fixed: '#2c1600'
  on-tertiary-fixed-variant: '#683d04'
  background: '#141311'
  on-background: '#e6e2de'
  surface-variant: '#363532'
typography:
  headline-lg:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 26px
    fontWeight: '400'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Newsreader
    fontSize: 22px
    fontWeight: '400'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: -0.015em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-code-lg:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  label-code-md:
    fontFamily: JetBrains Mono
    fontSize: 11.5px
    fontWeight: '400'
    lineHeight: 16px
  label-badge:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 12px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-compact: 0.5rem
  margin: 1rem
  margin-expanded: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an editorial, calm, and focused developer environment crafted specifically for hand-held coding and architectural inspection on mobile hardware (calibrated for modern high-density Samsung One UI devices). It bridges the warm, literary intellect of editorial desktop software with the ergonomics and precision of a native Jetpack Compose mobile client.

### Visual Character & Identity
- **Mindful Craftsmanship:** Moving deliberately away from the cold, neon-infused palettes typical of CLI-centric mobile tools, the interface feels like hot-pressed rag paper, oiled walnut, and dark volcanic stone. It invites quiet contemplation, extended reviewing, and low cognitive load.
- **Editorial Technology:** Code, AST representations, and terminal outputs are treated with the typographic reverence usually reserved for fine literature. Monospace blocks feel crisp, grounded, and legible rather than abrasive.
- **Thumb-Zone Utility:** Optimized for One UI ergonomics: the upper 40% of standard views serves context, status, and typography headers, while the bottom 60% holds interactive touchpoints, control sheets, and primary inputs with guaranteed minimum touch-targets of 48dp.

## Colors

The palette draws directly from warm terracotta clay, fibrous unbleached oat paper, and dense obsidian rock. It balances ambient warmth with surgical syntactic contrast.

### Palette Roles & Surface Architectures

#### Dark Mode (Primary Default)
- **Base Surface (`surface-dim`):** `#141312` — Deepest background for navigation rails and system bars.
- **Main Surface (`surface`):** `#181716` — Standard screen backdrop; soft, velvety obsidian with balanced warm-red undertones to prevent eye fatigue.
- **Card / Sheet Surface (`surface-container`):** `#21201E` — Card containers, elevated code snippets, and modal sheets.
- **Elevated Interactive (`surface-container-high`):** `#2C2A27` — Hover, pressed, and highlighted list items.
- **Hairline Borders (`outline-variant`):** `rgba(255, 245, 235, 0.08)` — Subtle definition lines with no harsh specular shine.
- **Text & Foreground:** Primary text sits at `#EDE8E1` (warm alabaster); secondary text at `#A8A196` (warm stone gray); muted annotations at `#706B63`.

#### Light Mode (Paper Variant)
- **Base Surface (`surface-dim`):** `#F3EEE6` — System bar base and inset canvas.
- **Main Surface (`surface`):** `#FBF9F5` — Warm unbleached oat parchment.
- **Card / Sheet Surface (`surface-container`):** `#FFFFFF` — Pure crisp card planes.
- **Hairline Borders (`outline-variant`):** `rgba(33, 32, 30, 0.08)` — Subdued organic borders.
- **Text & Foreground:** Primary text at `#1A1918`; secondary text at `#666159`; muted at `#948F85`.

#### Accent & Semantic Tones
- **Primary Accent (`Terracotta Clay`):** `#D97757` (resting), `#C96442` (pressed/hover). Used for floating actions, active cursor states, primary commit CTAs, and semantic focal points.
- **Skill & Source Badges (Metadata Classification):**
  - *Anthropic / Core Intelligence:* Warm Amber (`#E0A364`, container: `rgba(224, 163, 100, 0.12)`)
  - *Local / System Execution:* Neutral Slate (`#8E9298`, container: `rgba(142, 146, 152, 0.12)`)
  - *Personal / Custom Prompts:* Sage Green (`#8B9D83`, container: `rgba(139, 157, 131, 0.12)`)
  - *Registry / Package Store:* Indigo Slate (`#7E8CC6`, container: `rgba(126, 140, 198, 0.12)`)
  - *VCS / Repository Integration:* Muted Plum (`#A57B96`, container: `rgba(165, 123, 150, 0.12)`)
- **Syntax & Code Highlighting (Muted Chroma):**
  - Keyword: `#E09F67` (Amber)
  - String: `#9DB48C` (Olive Sage)
  - Function: `#E8C587` (Sand)
  - Type / Struct: `#8BA3C7` (Dull Slate Blue)
  - Diff Add: `rgba(139, 157, 131, 0.15)` with `#9DB48C` gutter mark
  - Diff Remove: `rgba(201, 100, 66, 0.15)` with `#D97757` gutter mark

## Typography

The typography system pairs three distinct voices: an editorial humanist serif for contemplation and context, a utilitarian sans-serif for UI balance, and an ultra-precise monospaced engine for code evaluation.

### Typographic Hierarchy Rules
- **Editorial Anchors (`Newsreader`):** Utilized exclusively for screen titles, conceptual session headers, and long-form conversational markdown responses from intelligence models. Always set at regular weight (400) or italic; never bolded.
- **Product Architecture (`Inter`):** Drives all UI operational copy, modal alerts, list headers, button actions, and settings. High x-height ensures immediate legibility at glance speed under daylight reflections.
- **Code, Architecture & Metrics (`JetBrains Mono`):** Every path fragment, code token, git SHA, line count, and skill origin badge uses JetBrains Mono. Tabular figures must be enabled by default (`tnum`) for synchronized diff listings and numeric alignment.

## Layout & Spacing

The layout is grounded in a strict 8dp reference grid with a baseline 16dp (`1rem`) system gutter. It honors the ergonomic interaction physics of modern handheld screens (such as the 6.6-inch form factor of the Galaxy A56).

### Layout & Adaptation Mechanics
- **Vertical Ergonomics (One UI Principle):** The top app bar area allows for generous editorial breathing room (`headline-lg` titles drop down into the comfortable gaze zone), while all high-frequency controls—such as branch switchers, execution buttons, and input prompts—sit in the thumb zone within the lower 120dp to 240dp of the viewport.
- **Horizontal Framing:** Standard views maintain a fixed `16dp` horizontal edge margin. Code buffers, split diff views, and terminal shells switch to an edge-to-edge layout with internal `12dp` padding to maximize monospaced column character widths without unnecessary line wraps.
- **Touch Targets:** Interactive targets must maintain a minimum physical box of `48dp × 48dp`, even when the visual icon or pill glyph inside measures `28dp` or `32dp`.

## Elevation & Depth

Spatial layering relies on warm surface-container tonal shifts paired with ambient, light-absorbing shadows rather than intense synthetic drop shadows.

### Tonal Hierarchy
- **Level 0 (Canvas Base):** `#141312` (Dark) / `#F3EEE6` (Light). Used for window frames, status margins, and editor backsplashes.
- **Level 1 (Card & Content Blocks):** `#181716` (Dark) / `#FBF9F5` (Light). Primary surface resting on the canvas. Delineated by an ultra-thin border (`1dp` solid with `rgba(255, 245, 235, 0.06)`).
- **Level 2 (Active Panels & Code Inspection Tiles):** `#21201E` (Dark) / `#FFFFFF` (Light). Provides visual elevation through color luminosity rather than heavy drop shadows.
- **Level 3 (Bottom Sheets & Quick Actions):** `#2C2A27` (Dark) / `#FFFFFF` (Light). Combined with a warm ambient shadow: `box-shadow: 0px 8px 32px rgba(10, 9, 8, 0.45)`.
- **Level 4 (Floating Utility & Popovers):** `#34322E` (Dark). Rimmed with an accent-tinted border (`rgba(217, 119, 87, 0.25)`) to guarantee spatial isolation above text buffers.

## Shapes

The geometry uses smooth squircle radii that feel organic and tactile while preserving the clean rectangular discipline required for dense developer tooling.

### Shape Geometry Principles
- **Base Components (Cards, Panels, Dialogs):** Rounded with a `16dp` (`rounded-lg`) radius, mirroring the natural corner curvature of Android device displays.
- **Chips, Badges, and Small Tags:** Rounded with `6dp` to `8dp` (`space-sm` to `space-md`), creating compact capsules without turning into generic pill shapes.
- **Action Buttons & Floating Prompts:** `12dp` smooth squircle radius. Floating prompts avoid full pills to keep an engineered, structured feel.
- **Code Viewports & Terminal Frames:** Inner containers reduce to `10dp` to frame monospaced blocks cleanly within cards.

## Components

### Buttons & Interactive Controls
- **Primary Action (Execute / Apply Diff):** Built with Terracotta Clay (`#D97757`), contrasting with deep obsidian text (`#141312`) in dark mode or pure white in light mode. Border radius `12dp`. Min-height `48dp`. Text in `Inter 14px SemiBold`.
- **Secondary Action (Stash / Discard):** Surface-container container (`#2C2A27`), subtle border (`outline-variant`), and text in `#EDE8E1`.
- **Ghost Action (Copy / Inline Toggle):** Transparent background, active ripple bounded to `48dp`, icon size `20dp`.

### Skill & Origin Badges
- **Visual Composition:** Compact layout with `4dp` top/bottom and `8dp` lateral padding. Monospaced `10px` uppercase text with a leading `6dp` circular dot indicator.
- **Styling by Origin:**
  - *Anthropic Core:* Border and text `#E0A364`, background `rgba(224, 163, 100, 0.08)`.
  - *Local Shell:* Border and text `#8E9298`, background `rgba(142, 146, 152, 0.08)`.
  - *Personal Script:* Border and text `#8B9D83`, background `rgba(139, 157, 131, 0.08)`.
  - *Marketplace Extension:* Border and text `#7E8CC6`, background `rgba(126, 140, 198, 0.08)`.
  - *GitHub Source:* Border and text `#A57B96`, background `rgba(165, 123, 150, 0.08)`.

### Code Buffers & Diff Cards
- **Structure:** Encapsulated inside `surface-container` (`#21201E`) cards with a top status strip (displaying file name, language indicator, and total changed lines).
- **Line Numbers:** Right-aligned `JetBrains Mono 11.5px`, tinted stone gray (`#706B63`), non-selectable, with a `12dp` right margin separating them from code text.
- **Diff Presentation:** Full-width color blocks across insertion/deletion lines. Insertions highlighted with subtle green tint (`rgba(139, 157, 131, 0.12)`); deletions highlighted with warm terracotta tint (`rgba(217, 119, 87, 0.12)`).

### Input Fields & Command Bar
- **Prompt Bar:** Sits anchored to the bottom thumb zone with an elevated container (`#262422`), featuring a subtle top border. Houses an auto-expanding multiline input, attachment pill, skill indicator, and send button.
- **Standard Text Fields:** Background `#1D1C1A`, hairline outline (`outline-variant`), focusing to an active Terracotta Clay border highlight (`#D97757`) without sudden layout shifts.

### Lists & Selection Controls
- **List Rows:** Standard height `56dp` to comply with One UI tactile rhythm. Divided by horizontal hairlines indented by `16dp` from the leading edge.
- **Radio Buttons & Checkboxes:** Custom rounded squares (`4dp` for check, circular for radio) utilizing Terracotta Clay fill with dark Obsidian icons when selected. Touch target expands to `48dp` transparent bounds.