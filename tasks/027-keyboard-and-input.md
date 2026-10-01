---
id: "027"
title: "Eingabe und Tastatur"
wave: "W7"
depends_on: [010, 023, 024]
files: [app/src/main/java/org/claudroide/app/feature/chat/ChatInputState.kt, app/src/test/java/org/claudroide/app/ChatInputTest.kt, tasks/027-keyboard-and-input.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "a62fd0b2fa173b2d"
---
# Aufgabe 027 — Eingabe und Tastatur

## Ziel
Text, mehrzeilige Eingaben, Code und optionale Anhänge bequem eingeben.

## Ergebnis
Eingabefeld mit Tastaturverhalten, Senden, Abbrechen, Diktat-Übergabe und sicherem Anhangswähler.

## Fertig, wenn
- Tastatur verdeckt weder Text noch nötige Aktionsknöpfe (`maxCollapsedLines`, Inset-Schutz).
- Senden während einer laufenden Anfrage eindeutig behandelt wird (`InputActionButtonState.STOP` verhindert Doppelsendungen).
- Anhangstransparenz technisch garantiert ist.

## Schutz
Anhänge werden nicht ohne Anbieter- und Datenhinweis übertragen (`AttachmentPolicy.canTransmitAttachment` verlangt expliziten Provider-Consent und max. 10 MB).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ChatInputState.kt`:
  - `enum class InputActionButtonState`: DISABLED, SEND, STOP.
  - `data class AttachmentItem`: Typ, Name, Größe und Status des Anbieter-Einverständnisses.
  - `object AttachmentPolicy`: Begrenzung auf 10 MB, zwingende Provider-Einverständnisprüfung.
  - `data class ChatInputState`: Reaktiver Eingabezustand mit automatischer Schaltflächenumwandlung (Senden -> Stopp bei Stream).
- `app/src/test/java/org/claudroide/app/ChatInputTest.kt`:
  - Unit-Tests für Senden/Deaktiviert/Stopp-Zustandswechsel, Anhangs-Consent und Größenbegrenzungen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Mehrzeiliges Eingabefeld und Einhand-Aktionsknopf für mobile Touchscreens gestaltet.
- `testing-setup`: Zustands- und Richtlinientests für Eingabe und Anhangs-Compliance aufgesetzt.

