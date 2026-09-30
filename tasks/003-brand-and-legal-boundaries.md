---
id: "003"
title: "Eigenständige Marke und Grenzen"
wave: "W0"
depends_on: []
files: [tasks/003-brand-and-legal-boundaries.md]
skills: [`/swarm-planner`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "4dad53f0c05dfba2"
---
# Aufgabe 003 — Eigenständige Marke und Grenzen

## Ziel
Sicherstellen, dass Claudroide als unabhängiges Produkt erkennbar bleibt und alle rechtlichen sowie markenbezogenen Grenzen strikt eingehalten werden.

## Ergebnis
Eine verbindliche Prüfliste und Richtlinie für Name, Icon, Farbpalette, Screenshots, Store-Präsenz, Dokumentation und Abgrenzung zu Drittmarken.

### 1. Verbindlicher Unabhängigkeitshinweis (Disclaimer)
Dieser Hinweis ist an allen zentralen Stellen gut sichtbar und unmissverständlich zu platzieren (README, App-Infodialog, App-Store-Beschreibung):
> **„Claudroide ist ein unabhängiges Open-Source-Projekt und steht in keiner geschäftlichen oder offiziellen Verbindung zu Anthropic, PBC. Claude ist eine eingetragene Marke von Anthropic. Claudroide ermöglicht die Nutzung offizieller Entwickler-Schnittstellen auf Basis eigener API-Schlüssel (BYOK).“**

### 2. Abgrenzungsmatrix: Erlaubt vs. Unzulässig

| Bereich | Zulässig (Sachlicher Bestimmungshinweis) | Unzulässig (Markenverletzung / Täuschung) |
|---|---|---|
| **Produktname** | „Claudroide — Mobiler KI-Coding-Assistent für Android“ | „Claude Code Mobile“, „Claude für Android“, „Anthropic App“ |
| **App-Icon** | Eigenständiger grüner Android-Roboter („Claudroide“) mit eigenem Design | Offizieller Anthropic Spark / Claude-Logo als Icon oder dominantes Element |
| **Maskottchen & Banner** | Verspielter grüner Android-Bot, der einen warmen KI-Funken/Stern greift | 1:1 Kopie der Anthropic-Wort-/Bildmarke; Imitation offizieller Corporate Identity |
| **Funktionsbeschreibung** | „Unterstützt offizielle Claude Messages API (BYOK) und CLAUDE.md-Dateien“ | „Offizielle Claude Code App“, Versprechen von Claude Pro/Max Abo-Zugängen |
| **Store-Präsenz** | Transparente Auflistung der Kompatibilität im Fließtext | Verwendung von „Claude“ im App-Titel auf Google Play / F-Droid |
| **Farbwelt & Theme** | Android-Grün (`#3DDC84` / Waldgrün) mit warmen Terrakotta-Akzenten | Exakte 1:1 Kopie des geschützten Anthropic Web-Designs |

### 3. Prüfliste für UI, Dokumentation und Veröffentlichung
- [x] **Kein Identitätsdiebstahl:** Keine Behauptung von Zugehörigkeit zu Anthropic.
- [x] **Klare Kennzeichnung von Drittmarken:** Fremde Markennamen werden nur deskriptiv zur Kennzeichnung von Kompatibilität verwendet (§ 23 MarkenG / Nominative Fair Use).
- [x] **Eigenständige Grafik-Assets:** Keine unlizenzierten SVG-Logos oder Markenressourcen von Dritten in `assets/`.
- [x] **Screenshots & Beispieldaten:** Screenshots für Store und README nutzen ausschließlich eigens erstellte Demos und mockierte Projekte ohne fremde Firmengeheimnisse.
- [x] **Fehlermeldungen:** Klare Zuordnung von Fehlern (z.B. „Fehler vom Anbieter Claude API: Guthaben aufgebraucht“) ohne Verschleierung der Quelle.

### 4. Konformitätsprüfung
- [x] Unabhängigkeitshinweis und rechtliche Schranken sind dauerhaft dokumentiert.
- [x] Klare Richtlinien für Maskottchen, Banner und Launcher-Icon stehen fest.
- [x] Alle Kriterien für eigenständiges Branding sind erfüllt.

## Fertig, wenn
- Kein eigener Produktname oder Logo „Claude Code“/Anthropic imitiert.
- Unabhängigkeitshinweis und Quellen für verwendete Assets feststehen.

## Schutz
Keine fremden Logos, geleakten Quellen oder Screenshots ohne Nutzungsrecht übernehmen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Marken-/Lizenzprüfungs-Skill suchen und vor Installation Quelle, Lizenz und Risiken prüfen; Nutzerfreigabe einholen.
