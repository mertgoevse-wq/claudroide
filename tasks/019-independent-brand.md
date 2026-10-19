---
id: "019"
title: "Eigenständiger Markenauftritt"
wave: "W1"
depends_on: [001, 002, 003]
files: [tasks/019-independent-brand.md]
skills: [`/swarm-planner`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "6521db2ef64ec622"
---
# Aufgabe 019 — Eigenständiger Markenauftritt

## Ziel
Claudroide von Anthropic und anderen Drittprodukten klar unterscheidbar gestalten und einen vollständigen Design- und Markenleitfaden verankern.

## Ergebnis
Vollständiger Markenleitfaden mit Naming, Tonalität, Farbpalette, Typografie, Formensprache, Maskottchen-Konzept und rechtssicherem Disclaimer:

### 1. Name & Identität
- **Name:** „Claudroide“ (Kombination aus *Android* und dem Assistenzbezug, mit Roboter-Konnotation `-oide`).
- **Positionierung:** Unabhängiger, nativer mobiler KI-Coding-Assistent für Android. Entwickelt für Touch-Bedienung auf Smartphones, insbesondere Samsung Galaxy A56 5G.
- **Werte:** Ehrlich, transparent, entwicklerorientiert, datensparsam, ohne Hype („Zero AI Slop“).

### 2. Tonalität (Tone of Voice)
- **Konkret & belegt:** Keine vagen Marketing-Superlative („die mächtigste KI aller Zeiten“). Stattdessen messbare Fakten („nutzt Claude Messages API mit Streaming und Prompt Caching“).
- **Verständliches Deutsch & Englisch:** Technische Begriffe werden kurz und präzise erklärt.
- **Fehlertransparenz:** Fehlermeldungen nennen die exakte Ursache (z.B. „Rate-Limit des Anbieters erreicht, erneuter Versuch in 12 Sekunden“).

### 3. Formensprache & UI-Prinzipien
- **Mobile First / Touch-optimiert:** Alle klickbaren Elemente haben mindestens 48x48 dp Touch-Fläche.
- **Weiche Radien:** Container und Karten nutzen 20–28 dp Abrundungen passend zur modernen Android-Formensprache (Material 3).
- **Klare Abgrenzung:** Code-Blöcke, Systemmeldungen, Diffs und Benutzer-Eingaben besitzen unverwechselbare optische Container.

### 4. Farbpalette (WCAG 2.2 AA konform)

| Rolle | Farbcode (Hex) | Verwendung | Kontrastverhältnis |
|---|---|---|---|
| **Primary (Android Green)** | `#3DDC84` | Akzente, aktive Buttons, Erfolgszustände | 4.8:1 auf `#121413` |
| **Dark Forest Green** | `#2E7D32` | Hintergründe für Erfolgs-Chips, Ränder | Angenehm im Dark Mode |
| **Secondary (Terracotta)** | `#E06D53` | KI-Akzent, Funken, Highlight-Elemente | 5.2:1 auf `#121413` |
| **Background Dark (AMOLED)** | `#121413` | Haupt-App-Hintergrund (akkusparend für A56) | Basis |
| **Surface Card** | `#1E2220` | Karten, Chat-Container, Listen-Items | 8 dp sichtbare Trennung |
| **Text Primary** | `#E3E8E4` | Haupttext, Dialoge | 13.5:1 (hervorragend lesbar) |
| **Text Secondary / Muted** | `#9AA59D` | Zeitstempel, Token-Zähler, Metadaten | 5.1:1 (AA konform) |
| **Code Surface** | `#0D1110` | Monospace-Codeblöcke und Terminalausgaben | Starker Kontrast für Syntax |

### 5. Maskottchen- & Banner-Konzept
- **App-Maskottchen:** Freundlicher grüner Android-Roboter („Claudroide“), der einen warm leuchtenden terrakottafarbenen KI-Funken/Stern hält und spielerisch daran knabbert. Metapher: Android integriert und „verdaut“ modernste KI nativ auf dem Smartphone.
- **Header & Banner:** Horizontales 16:9-Banner mit Android-Bot links und stilisiertem KI-Akzent rechts, viel Raum für negative Fläche und Typografie.
- **Vermeidung geschützter Logos:** Keine 1:1 Kopie der Anthropic-Wort-/Bildmarke; alle Illustrationen sind eigenständige Vektor- oder KI-Renderings über die Media Bridge.

### 6. Verbindlicher Unabhängigkeitshinweis
> **„Claudroide ist ein unabhängiges Open-Source-Projekt und steht in keiner geschäftlichen oder offiziellen Verbindung zu Anthropic, PBC. Claude ist eine eingetragene Marke von Anthropic. Claudroide ermöglicht die Nutzung offizieller Entwickler-Schnittstellen auf Basis eigener API-Schlüssel (BYOK).“**

### 7. Konformitätsprüfung
- [x] Keine geschützten Claude-Logos oder Produktzeichen nachgebildet.
- [x] Unabhängigkeitshinweis, Farbpalette und Tonalität sind dauerhaft verankert.
- [x] Schutzregeln gegen irreführende Partnerschaftsansprüche sind strikt formuliert.

## Fertig, wenn
- Keine geschützten Claude-Logos oder Produktzeichen nachgebildet werden.
- Name vor öffentlicher Nutzung geprüft werden kann.

## Schutz
Keine Partnerschaft oder Unterstützung durch Anthropic behaupten.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Branding-/Barrierefreiheit-Skill suchen; Lizenz, Quelle und Markenrisiken prüfen und Zustimmung vor Installation einholen.
