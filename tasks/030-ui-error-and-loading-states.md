---
id: "030"
title: "Lade- und Fehlerzustände"
wave: "W7"
depends_on: [010, 023, 024]
files: [app/src/main/java/org/claudroide/app/core/design/ErrorStateTokens.kt, app/src/test/java/org/claudroide/app/ErrorStateTest.kt, tasks/030-ui-error-and-loading-states.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "5e02011436d7fb55"
---
# Aufgabe 030 — Lade- und Fehlerzustände

## Ziel
Nutzer bei Wartezeiten und Fehlern nicht mit unverständlichen Technikmeldungen alleinlassen.

## Ergebnis
Meldungen und nächste Schritte für Laden, leere Liste, Offline, Berechtigung verweigert und Anbieterfehler.

## Fertig, wenn
- Jede Meldung erklärt, ob Daten geändert oder gesendet wurden (`DataTransmissionStatus.NOT_SENT`, `PARTIALLY_SENT`, etc.).
- Wiederholen und Abbrechen getrennte, sichere Handlungen sind (`AppUiError.retryLabel`, `cancelLabel`).
- Fehlerzustände verständliche Handlungsanweisungen in Deutsch und Englisch bieten.

## Schutz
Fehlertext enthält keine Schlüssel, privaten Dateiinhalte oder vollständigen sensiblen Protokolle (`ErrorSanitizer.sanitizeErrorMessage` maskiert API-Keys, Bearer-Header und vertrauliche Parameter).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/core/design/ErrorStateTokens.kt`:
  - `enum class DataTransmissionStatus`: Transparenz über externen Datenabfluss und lokale Dateiänderungen.
  - `data class AppUiError`: Benutzerfreundliche Fehlermodelle mit klarer Handlungstrennung (Wiederholen vs. Abbrechen).
  - `object ErrorSanitizer`: Automatische Bereinigung sensibler Tokens (Claude/OpenAI Keys, Passwörter, Token-Parameter).
- `app/src/test/java/org/claudroide/app/ErrorStateTest.kt`:
  - Unit-Tests für Geheimnismaskierung, Status-Transparenz und Aktionsentkopplung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Fehlermeldungen, leere Zustände und Ladeindikatoren für Touchscreens gestaltet.
- `/code-review`: Sicherheitsfilter gegen Geheimnis-Lecks in Fehlermeldungen und UI-Aktionspfade überprüft.

