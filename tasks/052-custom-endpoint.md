---
id: "052"
title: "Eigener Endpunkt"
wave: "W11"
depends_on: [044, 045]
files: [app/src/main/java/org/claudroide/app/feature/provider/CustomEndpointSecurity.kt, app/src/test/java/org/claudroide/app/CustomEndpointTest.kt, tasks/052-custom-endpoint.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "d39ce4aee3699da3"
---
# Aufgabe 052 — Eigener Endpunkt

## Ziel
Nutzer kann eigene Serveradresse, Format, Modell und erforderliche Kopfdaten einstellen.

## Ergebnis
Prüfbares Konfigurationsformular samt Verbindungstest und verständlichen Fehlermeldungen.

## Fertig, wenn
- Nutzer vor jeder Anfrage die Zieladresse sieht (`CustomEndpointConfig.targetUrl`).
- Inkompatible Formate oder Fähigkeiten sichtbar markiert werden (`protocolFormat`, `supportsStreaming`, `supportsTools`).
- SSRF-Schutz vor internen LAN-Adressen greift (`CustomEndpointGuard.verifyEndpointSecurity`).

## Schutz
Unsicheres HTTP nicht still akzeptieren; Geheimnisse niemals an falsche Hostnamen senden (`canSendKeyToHost` erzwingt striktes Host-Pinning und blockiert Cross-Host-Übertragungen).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/CustomEndpointSecurity.kt`:
  - `data class CustomEndpointConfig`: Konfiguration für benutzerdefinierte Server mit Protokoll und Kopfdaten.
  - `object CustomEndpointGuard`: TLS-Zwang für Remote-Server, Ausnahme für Localhost, SSRF-Filter gegen private IP-Bereiche und strikte Host-Bindung für API-Schlüssel.
- `app/src/test/java/org/claudroide/app/CustomEndpointTest.kt`:
  - Unit-Tests für HTTPS-Zwang, HTTP-Localhost-Ausnahme, SSRF-Blockade privater IP-Bereiche und Host-Pinning von Schlüsseln.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `android-permissions-security`: Schutz vor SSRF-Angriffen, TLS-Erzwingung und Credential-Isolation nach Hostnamen umgesetzt.
- `testing-setup`: Unit-Tests für Endpunktsicherheit und Host-Pinning implementiert.

