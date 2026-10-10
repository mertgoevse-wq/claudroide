---
name: ClauDroide Studio
colors:
  surface: '#f9f9f8'
  surface-dim: '#dadad9'
  surface-bright: '#f9f9f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f3'
  surface-container: '#eeeeed'
  surface-container-high: '#e8e8e7'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1a1c1c'
  on-surface-variant: '#57423b'
  inverse-surface: '#2f3130'
  inverse-on-surface: '#f1f1f0'
  outline: '#8a726a'
  outline-variant: '#dec0b7'
  surface-tint: '#a23e18'
  primary: '#9f3c16'
  on-primary: '#ffffff'
  primary-container: '#bf542c'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb59c'
  secondary: '#555f6f'
  on-secondary: '#ffffff'
  secondary-container: '#d6e0f3'
  on-secondary-container: '#596373'
  tertiary: '#4b41e1'
  on-tertiary: '#ffffff'
  tertiary-container: '#645efb'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcf'
  primary-fixed-dim: '#ffb59c'
  on-primary-fixed: '#390c00'
  on-primary-fixed-variant: '#822801'
  secondary-fixed: '#d9e3f6'
  secondary-fixed-dim: '#bdc7d9'
  on-secondary-fixed: '#121c2a'
  on-secondary-fixed-variant: '#3d4756'
  tertiary-fixed: '#e2dfff'
  tertiary-fixed-dim: '#c3c0ff'
  on-tertiary-fixed: '#0f0069'
  on-tertiary-fixed-variant: '#3323cc'
  background: '#f9f9f8'
  on-background: '#1a1c1c'
  surface-variant: '#e2e2e2'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 13.5px
    fontWeight: '450'
    lineHeight: 22px
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-mono:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
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
  gutter-tablet: 1.5rem
  gutter-desktop: 2rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

The design system embodies an ultra-focused, calm, and intellectually rigorous workspace tailored for senior Android engineers and technical leads. It balances technical precision with editorial warmth. The design style combines **Minimalism** with **Modern Tactile Utility**: expansive low-noise canvas surfaces, razor-sharp typographic hierarchy, and strict adherence to native Android ergonomics.

The visual language rejects loud gradients, sensory overload, and playful gimmicks. Instead, it projects the quiet confidence of an executive IDE—delivering a distraction-free environment where complex system architecture, Gradle builds, and Kotlin coroutines can be reasoned through alongside an advanced AI copilot.

## Colors

The palette establishes an organic, glare-free working surface using an ultra-pale warm alabaster (`#FBFBFA`) base rather than pure clinical white. Depth is achieved via subtle tone stepping:
- **Canvas Base:** `#FBFBFA` provides an eye-resting foundation for marathon coding sessions.
- **Surface Elevation:** Crisp white (`#FFFFFF`) for interactive cards and input hubs, with soft sub-surfaces (`#F1F3F5`) for code blocks, nested inspector panels, and side drawers.
- **Primary Accent:** Warm Terracotta (`#C85A32`) serves as the singular focal point—reserved strictly for primary run actions, active context chips, model response highlights, and terminal execution points.
- **Text & Structure:** Deep Charcoal (`#111827`) provides high-contrast legibility for code and headers, stepping down to `#4B5563` and `#6B7280` for secondary metadata and timestamps. Subtle low-contrast borders (`#E5E7EB`) structurally define boundaries without heavy visual weight.

## Typography

The type system prioritizes high-density technical reading:
- **Inter** handles all UI labels, conversation flows, documentation, and headlines. Its mechanical precision and open counters allow effortless parsing across handheld screen sizes.
- **JetBrains Mono** is embedded across the system for code blocks, terminal streaming traces, diff views, and file system crumbs. Its increased x-height and clear distinction between glyphs (0 vs O, 1 vs l) prevents code inspection fatigue.
- Paragraph text maintains an open `1.6` line-height ratio to preserve legibility when rendering long architectural explanations and stack traces.

## Layout & Spacing

The layout is governed by an 8pt base grid with a 4pt subgrid for fine technical alignments.

- **Mobile (<600dp):** Fluid single-column stream. Margins are fixed at `16px` to maximize conversational viewport width. The bottom prompt bar sits permanently anchored above Android system navigation bars with safe-area insets.
- **Tablet & Foldables (600dp–1024dp):** Dual-pane master-detail arrangement. Left rail displays recent threads, open Kotlin modules, and ADB targets (280dp fixed); right canvas hosts the AI workspace and live file previews.
- **Desktop / DeX (>1024dp):** Three-column IDE canvas with a fluid center chat terminal, collapsible left project hierarchy, and right-hand live inspector/code preview pane.
- Outer container margins scale proportionally from `16px` (mobile) to `40px` (desktop canvas).

## Elevation & Depth

Visual hierarchy is maintained via low-contrast structural borders and quiet ambient shadows rather than dramatic artificial drops.

- **Level 0 (Base Canvas):** Background `#FBFBFA` with no elevation.
- **Level 1 (Cards, Code Blocks, Chat Bubbles):** Pure `#FFFFFF` or `#F1F3F5` bound by a crisp 1px hairline border (`#E5E7EB`). Subtle ambient drop: `0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02)`.
- **Level 2 (Floating Action Bars, Dropdowns, Hovered Toolbars):** `#FFFFFF` surface with an expanded soft blur: `0 4px 12px -2px rgba(17, 24, 39, 0.06), 0 2px 6px -1px rgba(17, 24, 39, 0.04)`, hairline border in `#EAECF0`.
- **Level 3 (Modal Sheets, Log Consoles):** Grounded by a delicate backdrop blur (`backdrop-filter: blur(8px)`) with a `0 20px 25px -5px rgba(0, 0, 0, 0.08)` presence.

## Shapes

The geometric vocabulary balances Android's pill-shaped tactile controls with developer-tool structure:
- **Interactive Pill Elements (Chips, Quick Actions, Buttons):** Fully rounded pill geometry (`9999px` or `24px` radius) to signify touch friendliness and instant affordance.
- **Content Surfaces (Message Blocks, Code Panels, Modals):** Medium roundedness (`12px` to `16px`) providing enough curvature to feel modern while maximizing internal screen area for monospaced code.
- **Micro-Controls (Checkboxes, Toggles, Badges):** `6px` to `8px` corner radius for crisp density.

## Components

### Buttons & Quick Actions
- **Primary Button:** Warm Terracotta (`#C85A32`) filled pill container, white `#FFFFFF` label, minimum height of `48px` for Android touch compliance. Pressed state darkens to `#B34E2A`.
- **Secondary / Ghost Button:** Transparent background with hairline border (`#E5E7EB`), text in `#1F2937`. On press, fills with `#F1F3F5`.
- **Icon Actions:** `44x44px` minimum hit area containing centered 20px outline stroke iconography (`1.5px` stroke width).

### Input Hub (Prompt Field)
- Anchored floating dock with an active `#FFFFFF` fill, 1px `#E5E7EB` border, shifting to a 1.5px `#C85A32` glow on focus.
- Embedded attachment and context selector pills on the leading edge (`+ File`, `@Context`), with a terracotta circle send button on the trailing edge.

### Chips & Tags
- `32px` height pills using `#F1F3F5` background, `#374151` text in `label-mono`, and an optional leading status dot (e.g., green `#10B981` for connected ADB devices).

### Code & Diff Containers
- Embedded containers using `#F8F9FA` fill with an inset `1px` border (`#E5E7EB`).
- Top utility header featuring file path in `label-mono`, language badge, and one-tap copy/apply buttons.
- Syntax highlighting uses a muted slate and terracotta palette to prevent visual harshness against the light surface.

### Cards & Chat Streams
- **User Prompts:** Subtly recessed `#F1F3F5` cards aligned right or full-width with a clean left border accent.
- **AI Responses:** Flush `#FFFFFF` surface cards with full markdown, live preview tabs, and inline terminal outputs.
- **Divider Rules:** Subtle horizontal rules (`1px` solid `#EAECF0`) separating conversation iterations.