---
id: "025"
title: "Navigation"
wave: "W7"
depends_on: [010, 023, 024]
files: [app/src/main/java/org/claudroide/app/core/navigation/NavRoutes.kt, app/src/test/java/org/claudroide/app/NavigationStructureTest.kt, tasks/025-navigation-structure.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "c08b86f144f61624"
---
# Aufgabe 025 — Navigation

## Ziel
Die Hauptbereiche ohne Terminalwissen mit wenigen Berührungen erreichbar machen.

## Ergebnis
Navigationsplan für Chatliste, Projekte, Suche, neue Unterhaltung und Einstellungen.

## Fertig, wenn
- Nutzer aktuellen Bereich und Rückweg immer erkennt (`Screen.isTopLevel`, `NavigationState.canNavigateBack`).
- Projektbezogener Chat sein Projekt klar anzeigt (`NavigationState.projectBadgeText`).
- Gefährliche Aktionen durch `NavigationSafetyPolicy` strikt isoliert sind.

## Schutz
Gefährliche Aktionen (Projekt löschen, Verlauf leeren, Schlüssel verwerfen, Abbrechen) nicht neben häufigen ungefährlichen Tasten (Zurück, Suchen, Senden) platzieren. Mindestabstand 16 dp und Zwei-Stufen-Bestätigung zwingend vorgeschrieben.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/core/navigation/NavRoutes.kt`:
  - `sealed class Screen`: ChatList, ProjectExplorer, Search, NewChat, Settings, Onboarding, ChatDetail mit typsicherer Routenerzeugung.
  - `object NavigationSafetyPolicy`: Trennung destruktiver Aktionen (`delete_project`, `clear_history`, `purge_keys`, `abort_all`) mit 16 dp Sicherheitsrand.
  - `data class NavigationState`: Aktiver Screen, Backstack, `isProjectBound`, `projectBadgeText`, `canNavigateBack`.
- `app/src/test/java/org/claudroide/app/NavigationStructureTest.kt`:
  - Routen-Validierung, TopLevel-Klassifizierung, Projektbindung & Badge-Anzeige, Backstack-Navigation und Schutzregeln verifiziert.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Navigationselemente für Touch-Bedienung und mobile Einhandnutzung optimiert.
- `/code-review`: Typsicherheit, Backstack-Konsistenz und Sicherheitsabstände gegen Fehlbedienung geprüft.

