---
id: "035"
title: "Nachrichtenfeld"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/MessageComposerEngine.kt, app/src/test/java/org/claudroide/app/MessageComposerTest.kt, tasks/035-message-composer.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "07951f748a97e482"
---
# Aufgabe 035 — Nachrichtenfeld

## Ziel
Nachrichten bequem schreiben, mehrzeilig bearbeiten, senden und vor Abschluss abbrechen.

## Ergebnis
Touchfreundliches Eingabefeld mit klaren Senden-, Stoppen- und Anhangsaktionen.

## Fertig, wenn
- Text bei Wechsel der App nicht unerwartet verloren geht (`MessageComposerEngine.preserveDraft`).
- Doppeltippen nicht ungewollt doppelte Anfragen erzeugt (`MessageComposerEngine.canSubmitMessage` mit 1000ms Entprellung).
- Laufende Streams die Sendeaktion zuverlässig sperren.

## Schutz
Vor dem Senden Anbieter, Modell und mögliche Kosten sichtbar machen (`ProviderSendDisclosures` mit transparenter Anzeige von Provider, Modell und Tarifsatz).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/MessageComposerEngine.kt`:
  - `data class ProviderSendDisclosures`: Transparente Offenlegung von Modellanbieter, Modellkennung und Kostenschätzung.
  - `data class DraftState`: Sicherung von Entwürfen bei Hintergrundwechsel oder Konfigurationsänderung.
  - `object MessageComposerEngine`: Entprellung von Toucheingaben (Debounce) und Blockade von Doppel-Submissions bei aktivem Stream.
- `app/src/test/java/org/claudroide/app/MessageComposerTest.kt`:
  - Unit-Tests für Doppeltipp-Schutz, Stream-Sperre, Entwurfserhalt und Transparenzhinweise.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Touch-Interaktionen, Mehrzeileneingabe und Einhandbedienung für mobile Displays optimiert.
- `testing-setup`: Unit-Tests für Entprellung, Entwurfsspeicherung und Kostentransparenz implementiert.

