---
name: design-library
description: On-demand access to a 316-skill design library (UI/UX, typography, color, motion, 3D, game UI, data-viz, accessibility, design systems, Figma, anti-AI-slop). Use when the user asks for a design skill by name, wants to search or browse design skills, or when UI/UX/design work should pull in the right expert skills. Triggers on "design skill", "design library", "ui skill", "ux skill", "/design", or any request to design, critique, review, polish or refine an interface.
---

# Design Skill Library — router

A curated library of **316 design skills** loaded **on demand**. Their descriptions
live in `index.json` and are **not** auto-loaded into context. Load only what a task
actually needs.

## Layout

```
~/.claude/design-skill-library/
├── index.json            metadata for all 316 skills (slug, category, description, path)
├── INDEX.md              the same data as a table
├── select-skills.py      selection engine — scores skills against a task
├── CURATION.tsv          wave 1 manifest   (design, UX, motion, A11y, brand)
├── CURATION2.tsv         wave 2 manifest   (anti-slop, grids, motion, Figma, research)
├── CURATION3.tsv         wave 3 manifest   (motion depth, 3D/WebGL, game UI, dataviz, platforms)
├── ATTRIBUTION.md        sources, licenses, commit hashes
├── slop-scan.sh          anti-slop gate — exit 1 on violation
├── build-index.py        regenerate INDEX.md + index.json
├── install.sh            install skills from the manifests
└── skills/<slug>/        the skills themselves (SKILL.md + references)
```

## Selecting skills

Prefer the engine over picking by hand:

```bash
python3 ~/.claude/design-skill-library/select-skills.py "build a pricing page" --n=6
python3 ~/.claude/design-skill-library/select-skills.py "3d game hero" --domain=3d,game
python3 ~/.claude/design-skill-library/select-skills.py "saas dashboard" --style=dense-data
```

It detects the task domains, scores every skill, keeps category diversity, and prints
the load path for each pick. Treat the result as a strong starting point, not gospel.

## Loading a skill

`Read` the `path` from `index.json`. **If a SKILL.md references `references/`,
`assets/` or `templates/`, read those too** — they hold the actual rule catalogue, and
skipping them is the most common way these skills get misapplied.

## Category map

| Category | Count | Covers |
|---|---|---|
| `motion` | 61 | GSAP, ScrollTrigger, principles, timing, per-element, per-industry |
| `ux` | 25 | research, personas, journeys, forms, onboarding, HCI |
| `review` | 18 | critique, audits, refactoring, design debt |
| `marketing` | 18 | landing, pricing, CRO, copy, brand, onboarding |
| `3d` | 16 | Three.js, shaders, R3F, Babylon, WebGL, postprocessing |
| `mobile` | 15 | iOS, Android, watchOS, tvOS, visionOS, React Native, responsive |
| `visual` | 13 | brand kits, diagrams, logos, image direction |
| `core` | 11 | anti-slop, interface design, frontend craft |
| `design-system` | 11 | tokens, components, theming, dark mode, governance |
| `a11y` | 7 | WCAG, ARIA, screen readers, accessible motion |
| `layout` | 7 | grid, spacing, editorial, image-first |
| `process` / `hci` | 12 | design ops, laws of UX, Fitts, Miller, Hick, Doherty |
| `figma` | 6 | design-to-code, library generation, SwiftUI bridge |
| `foundations` | 5 | hierarchy, spacing scales, icons, shadows |
| `quality` | 5 | Web Vitals, performance, QA |
| `design-doc` | 5 | DESIGN.md, design systems in Figma |
| `typography` / `color` | 7 | type scales, pairing, OKLCH, palettes |
| `dataviz` / `game` / `2d` | 5 | charts, HUDs, PixiJS |
| `copy` / `research` / `iterate` | 8 | UX writing, empathy maps, variants, stress tests |

## Anti-slop rules — binding

This library is deliberately free of Liquid Glass, Glassmorphism, Neumorphism,
Brutalism and Skeuomorphism. When doing design work:

**Forbidden:** Liquid Glass · Glassmorphism · Neumorphism · Brutalism · Skeuomorphism ·
frosted-glass shells · generic AI gradients · undifferentiated uniform spacing ·
stock 3D buttons · interchangeable buzzword copy.

**Expected:** intentional type hierarchy · a real grid · a systematic colour system
(prefer OKLCH) with checked contrast · flat surfaces with functional depth · motion
that earns its place · one detail that could not appear in any other project.

If a loaded skill promotes a banned aesthetic, ignore that section and say so.

Verify with:

```bash
cd ~/.claude/design-skill-library && ./slop-scan.sh   # exit 0 = clean
```

## Symlinked skills

Some skills in `~/.claude/skills/` are **symlinks** into this library. They still appear
in the skill tool but cost nothing extra:

```bash
ls -la ~/.claude/skills/ | grep '^l'
```

To stop one being auto-loaded, delete the symlink — the content stays in the library and
is still reachable through `/design`.
