---
id: "029"
title: "Zugänglichkeit"
wave: "W7"
depends_on: [010, 023, 024]
files: [app/src/main/java/org/claudroide/app/core/design/AccessibilityTokens.kt, app/src/test/java/org/claudroide/app/AccessibilityTest.kt, tasks/029-accessibility-basics.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "205c15676c799adf"
---
# Aufgabe 029 — Zugänglichkeit

## Ziel
Wichtige Abläufe auch mit Bildschirmleser, großer Schrift und eingeschränkter Motorik nutzbar machen.

## Ergebnis
Beschriftungen, Fokusreihenfolge, Tastaturbedienung, Kontrast und Vergrößerung festlegen.

## Fertig, wenn
- Freigaben und Dateiänderungen verständlich vorgelesen werden (`AccessibilityPolicy.buildApprovalAnnouncement`, `buildDiffAnnouncement`).
- Kein wichtiger Status nur als Farbe oder Symbol ohne Erklärung erscheint (`validateStatusSemantics`).
- Dynamische Systemschrift-Skalierung bis 200% (`isScaleSupported`) und Mindest-Touch-Target von 48 dp garantiert sind.

## Schutz
Sicherheits- und Datenschutzhinweise bleiben bei Hilfstechnologien wahrnehmbar (Priorisierung in der Fokus-Reihenfolge durch `FocusTraversalRole.CRITICAL_SECURITY_NOTICE`).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/core/design/AccessibilityTokens.kt`:
  - `object AccessibilityPolicy`: Generierung barrierefreier TalkBack-Ansagen für Freigaben und Datei-Diffs.
  - Durchsetzung der Nicht-Allein-Farbe-Regel (`validateStatusSemantics`: Farbe + Icon + Textbeschreibung zwingend).
  - Skalierungsgarantie bis 200% Systemschrift (`isScaleSupported`).
  - `enum class FocusTraversalRole`: Klare Hierarchie für barrierefreie Fokus-Navigation.
- `app/src/test/java/org/claudroide/app/AccessibilityTest.kt`:
  - Unit-Tests für Vorlese-Texte, Invarianten der Statusanzeige und Schriftenskalierung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Barrierefreie Komponenten, Touch-Targets und Skalierbarkeitsregeln entworfen.
- `testing-setup`: Accessibility-Unit-Tests zur Absicherung der TalkBack-Semantik implementiert.

