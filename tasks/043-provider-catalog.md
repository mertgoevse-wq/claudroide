---
id: "043"
title: "Anbieter-Katalog"
wave: "W10"
depends_on: [005, 016, 017]
files: [app/src/main/java/org/claudroide/app/feature/provider/ProviderCatalog.kt, app/src/test/java/org/claudroide/app/ProviderCatalogTest.kt, tasks/043-provider-catalog.md]
skills: [`/swarm-planner`, `/claude-api`]
status: done
gate: false
done_since_last_edit: true
content-hash: "b20d73190fd09872"
---
# Aufgabe 043 — Anbieter-Katalog

## Ziel
Nutzern geprüfte Anbieter und frei konfigurierbare Anschlüsse verständlich anbieten.

## Ergebnis
Eintrag je Anbieter mit offizieller Quelle, erlaubter Authentifizierung, Format, Fähigkeiten und letztem Prüfdatum.

## Fertig, wenn
- Veraltete/unbestätigte Einträge deaktivierbar sind (`ProviderCatalogRegistry.setProviderEnabled`).
- Anbieter nicht als offiziell unterstützt erscheinen, wenn der Weg nicht belegt ist (`isOfficiallyDocumented`).
- Protokollformate und Fähigkeiten (Streaming, Werkzeuge, Bilder) exakt spezifiziert sind.

## Schutz
Keine Zugangsdaten oder nicht freigegebenen Tokenwege katalogisieren. Der Katalog enthält null hartcodierte Schlüssel oder unautorisierte Relays.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/ProviderCatalog.kt`:
  - `data class ProviderCatalogEntry`: Modell für geprüfte Anbieteranschlüsse (Anthropic Claude API, OpenRouter, Lokaler Server, Custom Endpoint).
  - `enum class ProviderAuthType`: x-api-key Header, Bearer-Token, keine Authentifizierung (lokal).
  - `enum class ApiProtocolFormat`: Anthropic Messages API vs. OpenAI-kompatibel.
  - `object ProviderCatalogRegistry`: Katalogverwaltung mit Deaktivierungsfunktion und Fähigkeitsprüfung.
- `app/src/test/java/org/claudroide/app/ProviderCatalogTest.kt`:
  - Unit-Tests für Standardanbieter, Authentifizierungsmethoden, Streaming-/Tool-Unterstützung, Deaktivierbarkeit und Abwesenheit von Geheimnissen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `/swarm-planner`: Katalogstrukturierung und Abhängigkeitsgrenzen für Anbieteranbindungen geplant.
- `/claude-api`: Offizielle Anthropic Messages API Spezifikation, Authentifizierung Header und Fähigkeiten verifiziert.

