---
id: "050"
title: "OpenCode-Weg prüfen"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/050-opencode-provider-check.md]
skills: [`/swarm-planner`, `/claude-api`]
status: done
gate: false
done_since_last_edit: true
content-hash: "079392b53b00f766"
---
# Aufgabe 050 — OpenCode-Weg prüfen

## Ziel
OpenCode als Anbieter, Client oder Modellzugang sauber unterscheiden.

## Ergebnis
Dokumentierte Wege, unterstützte API-Formate, Authentifizierung und Lizenz festhalten.

## Fertig, wenn
- Nur tatsächlich angebotene Modell-API integriert wird.
- CLI-Anmeldung oder Abo-Zugang nicht als BYOK-API ausgegeben wird.

## Schutz
Keine fremden Tokens aus lokalen Programmen lesen oder wiederverwenden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen OpenCode-Integrations-Skill suchen; offizielle Dokumentation/Lizenz prüfen und Nutzer vor Installation fragen.

---

## Prüfungsergebnis (2026-10-01)

**Was ist OpenCode?**
OpenCode (github.com/opencode-ai/opencode) ist ein Open-Source-TUI-/CLI-Agent für Entwickler. Es ist kein Modellanbieter und stellt keine eigene Modell-API bereit. OpenCode ist ein Client — er verbindet sich mit Modellanbietern (Anthropic, OpenAI, Bedrock u. a.) über deren offizielle APIs.

**Unterstützte API-Formate:** OpenCode legt keine HTTP-Endpunkte offen, die ein Android-Client direkt aufrufen könnte. Es gibt keine dokumentierte Drittanbieter-API von OpenCode.

**Authentifizierung:** OpenCode verwendet die API-Schlüssel der konfigurierten Modellanbieter direkt. Es gibt kein eigenes Konto, kein Login und keinen Token-Relay.

**Lizenz:** MIT (github.com/opencode-ai/opencode/blob/main/LICENSE, Stand 2026-10-01).

**Ergebnis:** OpenCode ist **kein** Modellanbieter. Eine Integration als Anbieter in Claudroide ist **nicht möglich und nicht zulässig**. Nutzer, die lokale Modelle nutzen möchten, verwenden Ollama oder vLLM über den bestehenden `local_server`-Eintrag. Kein neuer Code, kein neuer Katalogeintrag erforderlich.
