---
id: "004"
title: "Claude-Zugang prüfen"
wave: "W1"
depends_on: [001, 002, 003]
files: [tasks/004-claude-access-feasibility.md]
skills: [`/claude-api`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "e14e8594d3412192"
---
# Aufgabe 004 — Claude-Zugang prüfen

## Ziel
Claude-Funktionen über einen ausdrücklich erlaubten Weg verfügbar machen und technische sowie rechtliche Grenzen auf Android verbindlich festlegen.

## Ergebnis
Eine datierte Entscheidungsvorlage zu Claude API-Schlüssel, SDK-Nutzung und technischen Grenzen auf Android:

### 1. Entscheidungsvorlage: Offizieller Zugangsweg (Stand: 2026-09-30)

| Kriterium | Festlegung für Claudroide | Begründung & Primärquelle |
|---|---|---|
| **Erlaubter Schnittstellenweg** | Offizielle Anthropic Messages API (`https://api.anthropic.com/v1/messages`) | Einzig offiziell dokumentierter, stabiler und lizenzkonformer Weg (`docs.anthropic.com`) |
| **Authentifizierungsmodell** | **BYOK („Bring Your Own Key“)** | Nutzer trägt seinen eigenen API-Key (`sk-ant-api...`) ein; Key verbleibt ausschließlich im geschützten Android Keystore |
| **Abrechnung & Kosten** | Direkt zwischen Nutzer und Anthropic | Claudroide verkauft keine Token weiter, betreibt keine Abrechnungs-Proxys und erhebt keine Aufschläge |
| **HTTP-Protokoll & Streaming** | HTTP POST mit Server-Sent Events (SSE) | Echtes Streaming (`stream: true`) über OkHttp/Ktor; Deltas werden live in Jetpack Compose gerendert |
| **Headers & Versionierung** | `x-api-key`, `anthropic-version: 2023-06-01`, `content-type: application/json` | Entspricht Anthropic REST-Spezifikation |

### 2. Ausschluss unzulässiger Zugangswege
- **Ausschluss von Claude Web-Abo-Login (Free/Pro/Max):**
  - Es erfolgt **kein** Abfangen von Session-Cookies, kein Web-Scraping und keine Emulation eines Browser-Logins.
  - *Grund:* Verstößt gegen die Nutzungsbedingungen von Anthropic, birgt hohes Risiko von Kontosperren des Nutzers und gefährdet Zugangsdaten.
- **Ausschluss inoffizieller Proxys & Reverse-Engineering-Binaries:**
  - Keine Integration von nicht-autorisierter Drittsoftware oder modifizierten Claude-Code-Binaries.
  - Claudroide nutzt eine native, saubere Kotlin-Client-Architektur.

### 3. Technische Grenzen & Android-Besonderheiten
- **Netzwerkwechsel & Standby:** Mobile Verbindungen können unterbrochen werden. Der Client muss HTTP-Streaming-Abbrüche abfangen und idempotent fortsetzen können (ohne Doppelübermittlung von Tool-Ergebnissen).
- **Kein Assistant-Prefill:** Anthropic Messages API weist Prefills bei aktuellen Modellen mit HTTP 400 ab. Antworten werden über System-Prompts und Structured Outputs gesteuert.
- **Token-Hygiene:** Prompt Caching (`cache_control: {type: "ephemeral"}`) wird unterstützt, um mobile Latenz und Kosten zu minimieren.

### 4. Konformitätsprüfung
- [x] Offizielle Bedingungen und API-Dokumentation sind vollständig verifiziert.
- [x] Nutzerabrechnung und Schlüsselbesitz (BYOK) sind transparent dokumentiert.
- [x] Unzulässige Umwege (Abo-Scraping, unautorisierte Proxys) sind verbindlich ausgeschlossen.

## Fertig, wenn
- Aktuelle offizielle Bedingungen und direkte technische Dokumentation geprüft sind.
- Gewählter API-Weg Nutzerabrechnung und Schlüsselbesitz erklärt.

## Schutz
Keine Session-Tokens, inoffiziellen Proxys oder veränderten Anthropic-Binaries verwenden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen API-/Lizenzrecherche-Skill finden, Quelle/Lizenz/Sicherheit prüfen und Installation mit dem Nutzer abstimmen.
