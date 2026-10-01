---
id: "044"
title: "Anbieter hinzufügen"
wave: "W10"
depends_on: [005, 016, 017]
files: [app/src/main/java/org/claudroide/app/feature/provider/ProviderConfigManager.kt, app/src/test/java/org/claudroide/app/ProviderConfigTest.kt, tasks/044-provider-add-flow.md]
skills: [`adaptive`, `android-permissions-security`]
status: done
gate: false
done_since_last_edit: true
content-hash: "b089d643da86e06a"
---
# Aufgabe 044 — Anbieter hinzufügen

## Ziel
Eigene Anbieterzugänge mit klaren, sicheren Feldern anlegen.

## Ergebnis
Name, API-Format, Serveradresse, Schlüssel, Modell und Verbindungstest erfassen.

## Fertig, wenn
- Eingabefehler konkret erklärt werden (`ProviderConfigValidator.validateDraft` mit spezifischen Feldfehlern für URL, Name, Modell und Key).
- Nutzer vor dem Speichern den Anbieter und das Datenziel prüfen kann (`createReviewSummary`).
- Externe Server zwingend HTTPS für TLS-Verschlüsselung verlangen (HTTP nur für Localhost/Ollama erlaubt).

## Schutz
Schlüssel beim Tippen und in Übersichten maskieren (`maskApiKey`) und niemals in unmaskierte Diagnoseausgaben übernehmen.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/ProviderConfigManager.kt`:
  - `data class CustomProviderDraft`: Eingabemodell für Name, URL, Protokoll, Schlüssel und Modell.
  - `object ProviderConfigValidator`: URL-Protokollprüfung (HTTPS-Zwang für remote), Validierung der Modellkennung, Schlüsselmaskierung und Prüfung vor dem Speichern.
  - `data class ProviderReviewSummary`: Sichere Zusammenfassung mit maskiertem Schlüssel vor Speicherung.
- `app/src/test/java/org/claudroide/app/ProviderConfigTest.kt`:
  - Unit-Tests für HTTPS-Zwang, HTTP-Localhost-Ausnahme, Mindestlängen, Schlüsselmaskierung und Vorschauzusammenfassungen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Mobiloptimierte Eingabefelder und Dialoge für Konfigurationsprüfungen gestaltet.
- `android-permissions-security`: TLS-Verschlüsselungspflicht und Schutz vor Klartext-Lecks in Diagnoseausgaben durchgesetzt.

