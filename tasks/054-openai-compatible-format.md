---
id: "054"
title: "OpenAI-artiges Nachrichtenformat"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/054-openai-compatible-format.md]
skills: [`/claude-api`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "0c7649ac6ee55726"
---
# Aufgabe 054 — OpenAI-artiges Nachrichtenformat

## Ziel
Eigene Endpunkte mit verbreitetem Nachrichtenformat anschließen.

## Ergebnis
Adapter und Prüfungen für Nachrichten, Streaming, Werkzeugantworten und Fehler.

## Fertig, wenn
- Nicht unterstützte Felder klar ausgewiesen sind.
- Keine Anbieterfunktion als universell angenommen wird.

## Schutz
Kopfdaten und Schlüssel nur an bestätigten Endpunkt senden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen API-Format-/Adapter-Skill suchen; Quelle, Lizenz und Sicherheitswirkung prüfen, Nutzer vor Installation fragen.

---

## Umsetzungsergebnis (2026-10-01)

**Implementierung:** `OpenAiMessageFormat.kt` — `OpenAiMessageFormatter` bildet das OpenAI Chat Completions-Format ab (Nachrichten mit System-Rolle, `max_tokens`, `stream`, `temperature`). Nicht universell unterstützte Felder (n, logprobs, functions) werden dokumentiert aber nicht hinzugefügt. Schlüssel und Kopfdaten erscheinen nicht im Anfragekörper.

**Finish-Reason-Transparenz:** `finish_reason: "length"` → `truncated: true` im Ergebnis — kein stilles Abschneiden.

**Verwendung:** Für alle Anbieter mit `ApiProtocolFormat.OPENAI_COMPATIBLE` (OpenRouter, lokaler Server, Custom Endpoint, Gemini-API-kompatibler Weg).

**Tests:** `OpenAiMessageFormatTest.kt` (19 Tests) deckt Erfolgs- und Fehlerpfade, Systemblock, Parameter-Klammerung, Abschneideerkennung und Abwesenheit von Schlüsselwerten ab.
