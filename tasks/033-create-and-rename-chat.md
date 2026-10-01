---
id: "033"
title: "Chats erstellen und umbenennen"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/ConversationManager.kt, app/src/test/java/org/claudroide/app/ConversationManagerTest.kt, tasks/033-create-and-rename-chat.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "ecd4120f2986d483"
---
# Aufgabe 033 — Chats erstellen und umbenennen

## Ziel
Chats erstellen, mit verständlichem Titel benennen und aus der Hauptliste archivieren.

## Ergebnis
Aktionen mit Bestätigung bei folgenreichen Änderungen und wiederherstellbarem Archiv.

## Fertig, wenn
- Automatische Titel keine Geheimnisse unnötig hervorheben (`ConversationManager.autoGenerateTitle` maskiert Schlüssel und Tokens).
- Archivieren nicht mit Löschen verwechselt wird (`isArchived = true` ist über `unarchiveConversation` jederzeit verlustfrei umkehrbar).
- Erstellen und Umbenennen keine leeren Titel erzeugt (`DEFAULT_CHAT_TITLE`-Fallback).

## Schutz
Keine Unterhaltung wird ohne Nutzeraktion an Anbieter übertragen. Das Erstellen, Umbenennen und Archivieren erfolgt zu 100% lokal ohne Netzaufrufe.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ConversationManager.kt`:
  - `data class ManagedConversation`: Lokales Datenmodell für Unterhaltungen mit Archivierungs- und Projektstatus.
  - `object ConversationManager`: Chat-Erstellung, automatische Titelerzeugung mit Geheimnisbereinigung, Umbenennung und verlustfreies Archivieren/Wiederherstellen.
- `app/src/test/java/org/claudroide/app/ConversationManagerTest.kt`:
  - Unit-Tests für Standard-Titel, Projekt-Bindung, Maskierung sensibler Schlüssel im Auto-Titel, Titelbegrenzung und reversible Archivierung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Schnelle Aktionen für Chat-Erstellung und Archivierungsgesten für Touchscreens entworfen.
- `testing-setup`: Unit-Tests für Lebenszyklus und Sicherheitsmaskierung implementiert.

