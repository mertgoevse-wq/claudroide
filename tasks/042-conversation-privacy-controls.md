---
id: "042"
title: "Gesprächsdatenschutz"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/ChatPrivacyPolicy.kt, app/src/test/java/org/claudroide/app/ChatPrivacyTest.kt, tasks/042-conversation-privacy-controls.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "154968eb3f5d179a"
---
# Aufgabe 042 — Gesprächsdatenschutz

## Ziel
Nutzer können Datenübertragung und lokale Speicherung pro Gespräch verstehen.

## Ergebnis
Anbieterkennzeichnung, Datenvorschau, Projekt-Ausschlüsse und Löschmöglichkeit.

## Fertig, wenn
- Vor erster externer Anfrage klar ist, dass Daten das Gerät verlassen (`PrivacyEnforcer.canDispatchExternalPrompt`).
- Ohne Verbindung Chatverlauf lokal sichtbar bleibt (`PrivacyEnforcer.isOfflineReadable`).
- Ausgeschlossene sensible Dateien (.env, Schlüssel, Zertifikate) automatisch vom Prompt-Kontext blockiert werden (`isPathExcluded`).

## Schutz
Keine stillen Analytik- oder Telemetrieinhalte (`PrivacyEnforcer.ZERO_TELEMETRY_INVARIANT = true`).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ChatPrivacyPolicy.kt`:
  - `data class ChatPrivacySettings`: Gesprächsbezogene Datenschutzeinstellungen, Ausschlussmuster und lokaler Modus.
  - `object PrivacyEnforcer`: Garantie null Telemetrie, Zustimmungsprüfung für externe Übertragungen, automatischer Dateiausschluss und Offline-Lesbarkeit.
- `app/src/test/java/org/claudroide/app/ChatPrivacyTest.kt`:
  - Unit-Tests für Telemetriefreiheit, externe Übertragungs-Freigabe, Filterung von Secret-Dateien (.env, id_rsa, .pem) und Offline-Verfügbarkeit.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `android-permissions-security`: Berechtigungsprüfungen, Geheimnisschutz und Ausschlussregeln für Gesprächsdaten durchgesetzt.
- `testing-setup`: Unit-Tests für Datenschutzrichtlinien und Dateiausschlussmuster implementiert.

