---
id: "017"
title: "Android-Erlaubnisse"
wave: "W5"
depends_on: [010]
files: [tasks/017-device-permissions.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "b4afeb5c9dec78d0"
---
# Aufgabe 017 — Android-Erlaubnisse

## Ziel
Nur die Android-Zugriffe anfordern, die für eine gerade gewählte Funktion zwingend nötig sind, pauschalen Speicherzugriff strikt vermeiden und ein sicheres, modales Erlaubnis- und Widerrufsmanagement verankern.

## Ergebnis
Implementierte Berechtigungs- und SAF-Sicherheitsarchitektur (`PermissionManager.kt`) und Tests (`PermissionManagerTest.kt`):

### 1. Berechtigungsmatrix & Prinzip der minimalen Rechte (Least Privilege)

| Berechtigung / Zugriff | Erhebungszeitpunkt | Begründung (Rationale) | Folge bei Ablehnung | Schutz vor Überprivilegierung |
|---|---|---|---|---|
| **Projektdateien (SAF `OPEN_DOCUMENT_TREE`)** | Erst wenn der Nutzer aktiv „Projektordner öffnen“ wählt | Ermöglicht Lesen und Bearbeiten des gewählten Code-Ordners | App bleibt voll bedienbar (z.B. für reine Chat- oder API-Funktionen); keine Zwangsblockade | **Kein `MANAGE_EXTERNAL_STORAGE`!** Zugriff beschränkt sich ausschließlich auf den gewählten Baum |
| **Netzwerk (`INTERNET`, `ACCESS_NETWORK_STATE`)** | App-Installation (Normal Permission) | Kommunikation mit Claude API / OpenRouter | Keine Runtime-Aufforderung erforderlich | Ausgehende Verbindungen nur zu explizit konfigurierten Endpunkten |
| **Benachrichtigungen (`POST_NOTIFICATIONS`)** | Erst wenn der Nutzer eine langlaufende Hintergrundaufgabe startet | Informiert über Status von Kompilierungen und langen Tests im Hintergrund | Aufgabe läuft im Vordergrund; keine Systembenachrichtigung | Nutzer wird vorab über Zweck informiert; Ablehnung führt nicht zum Absturz |

### 2. Handhabung von Ablehnung und permanentem Widerruf
- **3-Stufen-Ablauf:**
  1. Prüfung, ob Berechtigung bereits vorliegt (`ContextCompat.checkSelfPermission`).
  2. Falls nicht: Anzeige einer leicht verständlichen Erklärung (Rationale) im App-Design.
  3. Systemdialog starten (`ActivityResultContracts.RequestPermission`).
- **Permanente Ablehnung („Nicht mehr fragen“):**
  - Wird eine optionale Berechtigung dauerhaft verweigert, zeigt Claudroide einen dezenten Hinweis mit direktem Link zu den App-Details (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`), statt in eine Anfrage-Schleife zu verfallen.

### 3. Persistenz und Widerruf von SAF-Zugriffsrechten
- Persistierung über `contentResolver.takePersistableUriPermission()` sichert Zugriff über Geräteneustarts hinweg.
- Bei Projektentkopplung oder Entfernen des Projekts in den Einstellungen ruft Claudroide sofort `contentResolver.releasePersistableUriPermission()` auf, um den Zugriff restlos zu entziehen.

### 4. Konformitätsprüfung
- [x] Dateizugriff erfolgt ausnahmslos über das System Storage Access Framework (SAF), kein pauschaler Gerätespeicherzugriff.
- [x] Ablehnung macht die App nicht unbrauchbar.
- [x] Keine Root-Rechte, keine invasiven Berechtigungen.

## Fertig, wenn
- Projektdateien über die System-Dateiauswahl und nicht über pauschalen Zugriff geöffnet werden.
- Ablehnen die App nicht unnötig unbrauchbar macht.

## Schutz
Keine Root-, versteckten oder unnötig weitreichenden Rechte verlangen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Berechtigungs-Skill suchen, Herkunft/Lizenz und Sicherheitswirkung prüfen; Nutzer vor Installation fragen.
