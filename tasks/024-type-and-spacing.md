---
id: "024"
title: "Schrift und Abstände"
wave: "W6"
depends_on: [003, 019]
files: [tasks/024-type-and-spacing.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "adee539ab04dc4dd"
---
# Aufgabe 024 — Schrift und Abstände

## Ziel
Text und Bedienelemente für längere Chat- und Programmierarbeit auf dem Galaxy A56 angenehm lesbar, augenschonend und ermüdungsfrei gestalten sowie strikte Touch- und Umbruchstandards verankern.

## Ergebnis
Implementierte Typografie- und Abstands-Tokenarchitektur (`TypeTokens.kt`) und Tests (`TypeAndSpacingTest.kt`):

### 1. Typografie- und Skalierungsregeln (Material 3)

| Rolle | Schriftgröße (`sp`) | Zeilenhöhe (`sp`) | Verwendung & Skalierungsgarantie |
|---|---|---|---|
| `HeadlineLarge` | 28 sp | 34 sp | Hauptüberschriften, App-Titel |
| `HeadlineMedium`| 24 sp | 30 sp | Dialog-Titel, Modul-Überschriften |
| `TitleMedium`   | 18 sp | 24 sp | Sektions-Überschriften in Chats und Projekten |
| `BodyLarge`     | 16 sp | 22 sp | Standard-Fließtext für Chatnachrichten (optimale mobile Lesbarkeit) |
| `BodyMedium`    | 14 sp | 20 sp | Begleittexte, Sicherheits- und Freigabedialoge (keine Kleinschrift!) |
| `CodeText`      | 13 sp | 18 sp | Monospace (`FontFamily.Monospace`) für Quellcode und Terminals |

- **Dynamische Skalierung:** Alle Schriftgrößen sind in `sp` (Scale-independent Pixels) definiert, sodass Systemschriftvergrößerungen (bis 150%) im Android-System voll unterstützt werden, ohne Text abzuschneiden.
- **Schutz vor Kleinschrift:** Gefahren-, Sicherheits- und Freigabehinweise dürfen niemals kleiner als 14 sp (`BodyMedium`) dargestellt werden.

### 2. Touch-Ziele & Abstands-Tokens
- **Mindest-Touch-Target (Material 3 Standard):** Alle interaktiven Elemente (Buttons, Icons, Tabs) besitzen eine Mindest-Trefferfläche von **48 x 48 dp** (`TypeTokens.MinTouchTarget`).
- **Abstandsraster:** Durchgängige Verwendung von 4/8-dp-Schritten:
  - `SpacingSmall = 8.dp`, `SpacingMedium = 16.dp`, `SpacingLarge = 24.dp`.

### 3. Horizontales Scrolling für Code-Blöcke
- Um zu verhindern, dass lange Codezeilen ungewollt umbrechen oder Syntax unleserlich machen, werden Code-Container mit horizontalem Scrollen ausgestattet:
  `Modifier.horizontalScroll(rememberScrollState())`
  Dies garantiert unverfälschtes Lesen und fehlerfreies Kopieren von Code.

### 4. Konformitätsprüfung
- [x] Typografie- und Spacing-Tokens in `TypeTokens.kt` implementiert.
- [x] Mindest-Touch-Target von 48 dp und Monospace-Schriftart in `TypeAndSpacingTest.kt` verifiziert.
- [x] Keine winzigen Gefahren- oder Freigabehinweise.

## Fertig, wenn
- Einstellungen zur Schriftgröße möglichst unterstützt werden.
- Lange Code- und Textzeilen ohne unbeabsichtigtes Abschneiden nutzbar sind.

## Schutz
Keine winzigen Freigabe- oder Gefahrenhinweise.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Mobile-Typografie-/Barrierefreiheits-Skill suchen; Quelle und Lizenz prüfen, Installation nur mit Nutzerzustimmung.
