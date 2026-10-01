---
id: "026"
title: "Smartphone-Ansichten"
wave: "W7"
depends_on: [010, 023, 024]
files: [app/src/main/java/org/claudroide/app/core/design/ViewportTokens.kt, app/src/test/java/org/claudroide/app/MobileLayoutTest.kt, tasks/026-small-screen-layout.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "1ebba4155de983b9"
---
# Aufgabe 026 — Smartphone-Ansichten

## Ziel
Chat, Dateivergleich und Freigaben auf dem Galaxy A56 ohne Computer-Layout nutzbar machen.

## Ergebnis
Ansichten für Hochformat, Tastatur offen, große Schrift und Systemleisten dokumentieren und architektonisch absichern.

## Fertig, wenn
- Wichtige Inhalte nicht hinter Tastatur oder Notch/Systemleiste geraten (`MobileViewportTokens.calculateAvailableContentHeight`).
- Bedienung mit einer Hand geprüft und dokumentiert ist (`OneHandedReachableMaxHeightRatio <= 0.65f`).
- Dateivergleiche auf Bildschirmen < 600 dp automatisch auf einspaltige Inline-Ansicht umschalten (`shouldUseSingleColumnLayout`).

## Schutz
Bestätigen/Ablehnen stets klar unterscheidbar, mindestens 48x48 dp groß und durch mindestens 16 dp Sicherheitsabstand isoliert (`validateApprovalButtonPolicy`).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/core/design/ViewportTokens.kt`:
  - Definition der Viewport-Tokens für Samsung Galaxy A56 (393 dp Breite, 852 dp Höhe im Hochformat).
  - Einhand-Ergonomie-Radius (untere 65% für primäre Aktionsbereiche).
  - IME-Tastaturabstand und Systemleisten-Schutz (`calculateAvailableContentHeight`).
  - Schutzprüfung für Freigabeknöpfe (`validateApprovalButtonPolicy`).
- `app/src/test/java/org/claudroide/app/MobileLayoutTest.kt`:
  - Unit-Tests für Einspalten-Umschaltung (< 600 dp), Knopftrennungsregeln, IME-Höhenberechnung und Einhand-Ergonomie.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Mobiloptimierte Viewport-Tokens für Touch und schmale Bildschirme entworfen.
- `testing-setup`: Validierungstests für Viewport-Berechnungen und Schaltflächenregeln aufgestellt.

