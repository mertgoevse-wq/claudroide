---
id: "031"
title: "Chatliste"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/ChatListModels.kt, app/src/test/java/org/claudroide/app/ChatListTest.kt, tasks/031-chat-list.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "5fe159b2f606b3af"
---
# Aufgabe 031 — Chatliste

## Ziel
Gespeicherte Unterhaltungen übersichtlich als Einstieg in die App darstellen.

## Ergebnis
Chatliste mit Titel, letzter Aktivität, optionalem Projektbezug und Knopf für neuen Chat.

## Fertig, wenn
- Leere und volle Liste verständlich funktionieren (`ChatListUiState.isEmpty`).
- Auswahl den richtigen Verlauf chronologisch absteigend liefert (`filteredChats`, sortiert nach `lastModifiedTimestampMs`).
- Filterung nach Projektzugehörigkeit nahtlos klappt (`selectedProjectIdFilter`).

## Schutz
Chatvorschauen enthalten keine Anbieterschlüssel oder ausgeschlossenen Geheimnisse (`ChatPreviewSanitizer.sanitizePreview` maskiert `sk-ant-*`, `sk-*`, Passwörter und Tokens).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ChatListModels.kt`:
  - `data class ChatSummaryItem`: ID, Titel, Vorschau, Zeitstempel, Projekt-Badge-Bindung und bereinigter Vorschautext.
  - `object ChatPreviewSanitizer`: Automatischer Schutz vor Schlüssel-Lecks in der Listenübersicht.
  - `data class ChatListUiState`: Reaktiver Listenzustand mit Projektfilterung und Lade-/Leerzustandsprüfung.
- `app/src/test/java/org/claudroide/app/ChatListTest.kt`:
  - Unit-Tests für Leerzustand, Zeitstempel-Sortierung, Projekt-Filterung und Geheimnisbereinigung in Vorschau-Snippets.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Listenlayouts und Statusbadges für Smartphone-Touchscreens gestaltet.
- `testing-setup`: Unit-Tests zur Verifikation von Filter-, Sortier- und Sicherheitsregeln implementiert.

