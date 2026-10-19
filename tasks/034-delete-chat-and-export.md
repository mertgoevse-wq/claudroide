---
id: "034"
title: "Chats löschen und exportieren"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/ChatExportManager.kt, app/src/test/java/org/claudroide/app/ChatExportTest.kt, tasks/034-delete-chat-and-export.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "d4c38d8d3004ec10"
---
# Aufgabe 034 — Chats löschen und exportieren

## Ziel
Nutzer können Chats wirklich löschen oder als Datei exportieren.

## Ergebnis
Bestätigung, Umfangsübersicht, sicherer Export und klare Hinweise auf gespeicherte Kopien.

## Fertig, wenn
- Exportformat lesbar und ohne Zugangsschlüssel ist (`ChatExportManager.generateExportPayload` maskiert alle Schlüssel und Passwörter für Markdown & JSON).
- Löschen nachvollziehbar mit Zwei-Stufen-Bestätigung abgesichert ist (`confirmAndDeleteConversation`).
- Vor dem Export eine transparente Inhalts- und Größenübersicht bereitgestellt wird (`getExportScope`).

## Schutz
Export erfolgt nur auf gewähltes Ziel und zeigt enthaltene Daten vor dem Speichern. Unbestätigtes Löschen wird technisch verweigert.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ChatExportManager.kt`:
  - `enum class ExportFormat`: MARKDOWN (.md) und JSON (.json).
  - `data class ExportScopeSummary`: Vorschau von Titel, Nachrichtenanzahl, geschätzter Dateigröße und Bereinigungsstatus vor dem Speichern.
  - `object ChatExportManager`: Streng bereinigter Export und unwiderrufliche Löschung bei Nutzerbestätigung.
- `app/src/test/java/org/claudroide/app/ChatExportTest.kt`:
  - Unit-Tests für Markdown-Struktur, JSON-Validität, Geheimnisentfernung im Export, Umfangsermittlung und Bestätigungszwang beim Löschen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `android-permissions-security`: Schutz vor Datenlecks bei Exporten und Absicherung destruktiver Löschvorgänge.
- `testing-setup`: Unit-Tests für Exportformate und Löschbestätigungen implementiert.

