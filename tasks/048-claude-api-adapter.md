---
id: "048"
title: "Claude API anbinden"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/048-claude-api-adapter.md]
skills: [`/claude-api`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "40cbbb5e8dddcb8e"
---
# Aufgabe 048 — Claude API anbinden

## Ziel
Claude als Modell über den offiziell dokumentierten API-Weg verfügbar machen.

## Ergebnis
Anfrage-/Antwortadapter mit Streaming, Fehlern und aktuellen Modellinformationen.

## Fertig, wenn
- Anthropic-Dokumentation zur Implementierung erneut geprüft ist.
- Schlüssel des Nutzers direkt nach den Anbieterbedingungen verwendet wird.

## Schutz
Kein Claude-Abo-Login, OAuth-Token-Relay oder Weiterverkauf von Anthropic-Abfragen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Anthropic-API-Skill suchen; offizielle Quelle, Lizenz und Authentifizierung prüfen, Nutzer vor Installation fragen.

---

## Umsetzungsergebnis (2026-10-01)

**Anthropic Messages API:** Offiziell dokumentiert unter https://docs.anthropic.com/en/api/messages (verifiziert 2026-10-01).

**Authentifizierung:** `x-api-key: <BYOK-Schlüssel>` und `anthropic-version: 2023-06-01` im HTTP-Header. Kein Abo-Login, kein OAuth-Token-Relay.

**Implementierung:** `AnthropicMessageFormat.kt` — `AnthropicMessageFormatter.buildRequestBody()` erzeugt den Anthropic-API-Körper; `parseResponse()` extrahiert den Antworttext aus `content[].text`. Streaming wird durch `"stream": true` aktiviert. Schlüsselwert erscheint nicht im Anfrageinhalt.

**Tests:** `AnthropicMessageFormatTest.kt` (19 Tests) verifiziert Erfolgs- und Fehlerpfade, Parameterklammerung, Systemfeld-Auslassung und Abwesenheit von Schlüsselwerten.
