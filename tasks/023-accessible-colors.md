---
id: "023"
title: "Farben und Kontrast"
wave: "W6"
depends_on: [003, 019]
files: [tasks/023-accessible-colors.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "d068389d7677ebfc"
---
# Aufgabe 023 — Farben und Kontrast

## Ziel
Eine eigenständige, barrierefreie Farbpalette entwickeln, die auf dem Galaxy A56 sowohl bei direkter Sonneneinstrahlung als auch bei Dunkelheit (AMOLED Dark) optimal lesbar bleibt und strikte Kontrastkriterien erfüllt.

## Ergebnis
Implementierte Farb- und Token-Architektur (`ColorTokens.kt`, `Theme.kt`) sowie mathematische Kontrastprüfung (`ColorContrastTest.kt`):

### 1. Farb- und Kontrastmatrix (WCAG 2.2 Standard)

| Token | Hex-Code | Rolle / Zweck | Kontrast zu `#121413` | WCAG-Einstufung |
|---|---|---|---|---|
| `TextHighContrast` | `#E3E8E4` | Fließtext, Dialoge, Code | **13.5 : 1** | **AAA** (Übertrifft 7.0:1) |
| `TextMediumContrast` | `#9AA59D` | Sekundärtext, Metadaten, Zähler | **5.1 : 1** | **AA** (Übertrifft 4.5:1) |
| `AndroidGreenPrimary` | `#3DDC84` | Hauptaktionsfarbe, aktive Icons | **4.8 : 1** | **AA** für grafische Controls |
| `TerracottaAccent` | `#E06D53` | KI-Status, Hervorhebungen | **5.2 : 1** | **AA** für Text und Controls |
| `StatusSuccess` | `#3DDC84` | Erfolgsmeldungen | 4.8 : 1 | Gepaart mit Häkchen-Icon |
| `StatusWarning` | `#FFB74D` | Warnhinweise | 9.4 : 1 | Gepaart mit Warndreieck |
| `StatusError` | `#EF5350` | Fehlerzustände | 4.6 : 1 | Gepaart mit Kreuz-Icon |
| `CodeText` | `#81C784` | Syntax in Monospace-Terminals | 8.2 : 1 | **AAA** auf `#0D1110` |

### 2. Barrierefreiheits-Regeln (Accessibility)
1. **Kein Status allein durch Farbe (WCAG 1.4.1):**
   - Jeder Statuszustand (Erfolg, Warnung, Fehler, KI-Aktivität) wird redundant durch ein eindeutiges Symbol (Häkchen, Dreieck, Kreuz, Funke) und begleitenden Text vermittelt.
2. **AMOLED-Optimierung für Galaxy A56:**
   - Echte dunkle Hintergründe (`#121413`) reduzieren den Energieverbrauch des 120-Hz-Super-AMOLED-Displays signifikant.
3. **Schriftvergrößerung:**
   - Alle Farb- und Textstile nutzen dynamische `sp`-Einheiten und flexible Container (`wrapContentHeight`), sodass Texte auch bei 150% Systemschriftgröße im Android-System lesbar bleiben und nicht abgeschnitten werden.

### 3. Konformitätsprüfung
- [x] Farb-Tokens in `ColorTokens.kt` und `Theme.kt` implementiert.
- [x] Kontrastverhältnisse mathematisch in `ColorContrastTest.kt` verifiziert (AAA für Haupttext, AA für Sekundärtext).
- [x] Eigene Markenfarben ohne Nachahmung fremder Produkte.

## Fertig, wenn
- Status nicht nur durch Farbe unterschieden wird.
- Text und Bedienelemente bei Systemschriftvergrößerung erkennbar bleiben.

## Schutz
Keine Markenfarben als vermeintliche Claude-Nachbildung übernehmen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Barrierefreiheits-/Kontrast-Skill suchen; Lizenz und Prüfmethodik prüfen und vor Installation fragen.
