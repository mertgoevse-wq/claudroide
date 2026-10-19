---
id: "005"
title: "Anbieter-Regelmatrix"
wave: "W1"
depends_on: [001, 002, 003]
files: [tasks/005-provider-policy-matrix.md]
skills: [`/claude-api`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "be67b9787a2a95a6"
---
# Aufgabe 005 — Anbieter-Regelmatrix

## Ziel
Für Claude API, OpenRouter, OpenCode, Antigravity und eigene Endpunkte jeweils den realen, zulässigen Anschlussweg bestätigen und unzulässige Umwege verbindlich ausschließen.

## Ergebnis
Verbindliche Anbieter-Regelmatrix mit offizieller Quelle, Authentifizierungsform, Abrechnung, Datenschutz, App-Kompatibilität und Verifikationsstatus:

### 1. Verbindliche Anbieter-Matrix (Stand: 2026-09-30)

| Anbieter / Weg | Offizielle Primärquelle | Anmelde- & Schlüsseltyp | Kostenabrechnung | Datenschutz- & Übertragungshinweis | App-Kompatibilität | Status |
|---|---|---|---|---|---|---|
| **Anthropic Claude API** | `docs.anthropic.com` | Anthropic Console (`sk-ant-api...`) | Direkt mit Anthropic (Prepaid/Postpaid per MTok) | Commercial Terms (kein Training auf API-Kundendaten); Datenverarbeitung in USA | Vollständig (Messages API, Tool Calling, SSE Streaming, Prompt Caching) | **Verfügbar (Primär)** |
| **OpenRouter** | `openrouter.ai/docs` | OpenRouter Dashboard (`sk-or-v1-...`) | Direkt mit OpenRouter (Guthaben) | Datenübertragung an jeweiligen Modellbetreiber (z.B. Meta, Mistral, Anthropic) | Vollständig (OpenAI-kompatibles Format mit SSE Streaming) | **Verfügbar (Empfohlen)** |
| **Lokale Server / OpenCode** | OpenAI REST Spec / Ollama / vLLM | Lokaler LAN/Localhost-Endpunkt; optional Bearer Token | Keine (eigene Hardware) | 100% lokal im eigenen Netzwerk; kein Datenabfluss an Dritte | Vollständig über Custom Base-URL (`http://192.168.x.x:...`) | **Verfügbar** |
| **Google Gemini API** | `ai.google.dev` | Google AI Studio Key (`AIzaSy...`) | Google Cloud / AI Studio | Cloud-Verarbeitung gemäß Google Terms of Service | Kompatibel über offizielle REST-API oder OpenAI-Adapter | **Verfügbar (Offiziell)** |
| **Google Antigravity (Inoffiziell)** | Keine öffentliche Endnutzer-Doku | Inoffizielle OAuth-Routen / Cloud Code Proxies | Undokumentiert | Hohes Risiko von Token-Invalidierung und AGB-Verletzung | Unzulässig in regulärer App | **Nicht verfügbar (Ausgeschlossen)** |
| **Eigene Endpunkte (Custom)** | RFC 9110 / OpenAI API Standard | Frei konfigurierbar (Bearer Token, Custom Header) | Direkt beim jeweiligen Serverbetreiber | Hängt von Nutzerinfrastruktur ab | Vollständig konfigurierbar (URL, Header, Timeout) | **Verfügbar** |
| **Claude Abo / Web-Session** | Anthropic Terms of Service | Session-Cookies (`sessionKey`) | Unzulässige Abo-Zweckentfremdung | Hohes Sicherheitsrisiko (Cookie-Diebstahl, Kontosperre) | Technisch und rechtlich verboten | **Nicht verfügbar (Ausgeschlossen)** |

### 2. Sicherheits- und Trennungsregeln
1. **Kein CLI-Token-Import:** Claudroide liest keine Tokens aus Fremdsoftware (wie Termux-Konfigurationsdateien, fremden CLI-Tools oder Browser-Caches) aus.
2. **BYOK-Prinzip:** Der Nutzer trägt seine Schlüssel eigenverantwortlich in der App ein.
3. **Schlüsselisolation:** Schlüssel werden getrennt nach Anbieter im Android Keystore verschlüsselt abgelegt und niemals in Logs, Fehlermeldungen oder Backups im Klartext übertragen.

### 3. Konformitätsprüfung
- [x] Jede unterstützte Integration beruht auf offizieller Dokumentation mit Prüfdatum 2026-09-30.
- [x] Inoffizielle oder verbotene Zugangswege sind unmissverständlich als „Nicht verfügbar (Ausgeschlossen)“ markiert.
- [x] Abrechnungs- und Schlüsselbesitzverhältnisse sind eindeutig geregelt.

## Fertig, wenn
- Jede Integration eine Primärquelle und überprüfte Bedingungen hat.
- Abo-/CLI-Token-Import nicht mit normalem API-Zugang verwechselt wird.

## Schutz
Keine Provider-Token aus fremden Programmen auslesen oder weiterreichen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Skill zu API-Integration/Provider-Prüfung suchen; Quelle, Lizenz und Fähigkeiten prüfen, Nutzer vor Installation fragen.
