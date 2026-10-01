---
id: "032"
title: "Chat-Suche und Filter"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/ChatSearchManager.kt, app/src/test/java/org/claudroide/app/ChatSearchTest.kt, tasks/032-chat-search-and-filter.md]
skills: [`testing-setup`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "91213f1185d099d3"
---
# Aufgabe 032 — Chat-Suche und Filter

## Ziel
Nutzer können frühere Chats nach Text, Datum und Projekt finden.

## Ergebnis
Lokale Suche mit verständlichen Filtern und klarer Anzeige der Fundstelle.

## Fertig, wenn
- Suche nach gelöschten Chats keine Treffer mehr liefert (`isDeleted = false` Invariante).
- Große Verläufe die Bedienung nicht blockieren (`ChatSearchEngine.search` arbeitet asynchron/sequenziell mit Begrenzung auf `maxResults`).
- Trefferstelle mit Kontextausschnitt hervorgehoben wird (`SearchResultMatch.snippet`).

## Schutz
Suchtexte und Chatinhalte bleiben strikt lokal (`ChatSearchEngine.IS_STRICTLY_LOCAL = true`), keine Telemetrie- oder Netzwerkanfragen während der Suche.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ChatSearchManager.kt`:
  - `data class ChatSearchFilter`: Filter für Volltext, Projekt-ID, Datumsbereich und Ergebnisdeckel.
  - `data class SearchResultMatch`: Fundstellen mit Trefferart (Titel, Nachricht, Projektname) und Snippet.
  - `object ChatSearchEngine`: Strikt lokale Suchlogik mit garantierter Aussortierung gelöschter Unterhaltungen.
- `app/src/test/java/org/claudroide/app/ChatSearchTest.kt`:
  - Unit-Tests für Titelsuche, Inhaltssuche mit Snippet-Extraktion, Projektfilterung, Datumsbegrenzung und Ausschluss gelöschter Chats.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `testing-setup`: Unit-Tests für Suchfilter und Ausschluss gelöschter Unterhaltungen implementiert.
- `/code-review`: Lokale Datenisolation und Schutz vor Datenabfluss bei Suchanfragen gegengeprüft.

