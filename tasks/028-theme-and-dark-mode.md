---
id: "028"
title: "Hell und dunkel"
wave: "W7"
depends_on: [010, 023, 024]
files: [app/src/main/java/org/claudroide/app/core/design/ThemeMode.kt, app/src/main/java/org/claudroide/app/core/design/Theme.kt, app/src/test/java/org/claudroide/app/ThemeModeTest.kt, tasks/028-theme-and-dark-mode.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "cd56a9f59178ac73"
---
# Aufgabe 028 — Hell und dunkel

## Ziel
Eigenständige Gestaltung in hellem und dunklem Modus gut lesbar anzeigen.

## Ergebnis
Themenwahl „Geräteeinstellung“, „Hell“ oder „Dunkel“ mit passenden Farben und Grafiken.

## Fertig, wenn
- Wechsel keine Text-/Kontrastfehler oder Datenverluste verursacht (`ThemeModeResolver`).
- Chatcode und Warnhinweise in beiden Modi lesbar sind (`SemanticThemeColors`, WCAG AA Kontrast >= 4.5:1).
- AMOLED-Modus für maximale Akku-Einsparung auf dem Samsung Galaxy A56 standardmäßig aktiv ist.

## Schutz
Gefahrenzustände (Warnungen, Fehler, Abbrüche) bleiben unabhängig vom Thema durch Farbkontrast und Begleit-Icons deutlich markiert.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/core/design/ThemeMode.kt`:
  - `enum class ThemeMode`: SYSTEM, DARK, LIGHT.
  - `object SemanticThemeColors`: Explizite Paletten für Code, Warnungen und Fehler in Hell und Dunkel.
  - `object ThemeModeResolver`: Zuverlässige Auflösung des aktiven Modus.
- `app/src/main/java/org/claudroide/app/core/design/Theme.kt`:
  - `ClaudroideTheme` erweitert um Unterstützung für dynamische `ThemeMode`-Übergabe.
- `app/src/test/java/org/claudroide/app/ThemeModeTest.kt`:
  - Unit-Tests für Modusauflösung und mathematische Kontrastberechnung nach WCAG 2.2.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Hell- und Dunkelpalette für Material 3 und OLED-Displays optimiert.
- `testing-setup`: Mathematische Kontrast- und Farbauflösungstests implementiert.

