---
id: "047"
title: "Verbindung testen"
wave: "W11"
depends_on: [044, 045]
files: [app/src/main/java/org/claudroide/app/feature/provider/ProviderPingTester.kt, app/src/test/java/org/claudroide/app/ProviderPingTest.kt, tasks/047-provider-connection-test.md]
skills: [`/claude-api`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "e864caaf106b4990"
---
# Aufgabe 047 — Verbindung testen

## Ziel
Fehler bei Anbieteradresse, Schlüssel oder Modell vor einer längeren Anfrage sichtbar machen.

## Ergebnis
Kleine, klar abgegrenzte Testanfrage mit Ergebnis, Anbieter und Modell.

## Fertig, wenn
- Testdaten keine Projektdateien oder Geheimnisse enthalten (`PingRequestPayload.containsZeroProjectData = true`).
- Kosten/Abrechnung für den Test transparent ausgewiesen sind (`costDisclosure: ~2-5 Tokens (< $0.0001)`).
- Unsichere Weiterleitungen zu fremden Hosts oder HTTP-Downgrades strikt blockiert werden (`isRedirectSafe`).

## Schutz
Keine Weiterleitung an unbekannte Adressen ohne ausdrückliche Auswahl. Schutz vor Man-in-the-Middle durch TLS-Prüfung.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/ProviderPingTester.kt`:
  - `data class PingRequestPayload`: Minimaler Test-Ping mit 1 Token und statischem "ping"-Prompt ohne Projektbezug.
  - `enum class PingResultStatus`: Differenzierte Statuscodes für Authentifizierungsfehler (401), ungültige Endpunkte (404), Ratenbegrenzung (429) und blockierte Redirects.
  - `object ProviderPingTester`: Protokoll-Serialisierung für Anthropic und OpenAI sowie Host-Schutzprüfung bei Weiterleitungen.
- `app/src/test/java/org/claudroide/app/ProviderPingTest.kt`:
  - Unit-Tests für minimale Prompt-Erzeugung, Protokoll-Serialisierung, transparente Kostenanzeige und Blockade von Cross-Host-Redirects.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `/claude-api`: Offizielles Anthropic-Nachrichtenformat für minimale Ping-Aufrufe verifiziert.
- `testing-setup`: Unit-Tests für Verbindungstests, Fehlerstatus und Redirect-Sicherheitsgrenzen implementiert.

