---
id: "053"
title: "Claude-artiges Nachrichtenformat"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/053-anthropic-compatible-format.md]
skills: [`/claude-api`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "aed24d33a841b976"
---
# Aufgabe 053 — Claude-artiges Nachrichtenformat

## Ziel
Nachrichten, Werkzeuge und Streaming für dokumentierte Claude-kompatible APIs korrekt abbilden.

## Ergebnis
Formatbeschreibung mit Tests für Text, Fehler, Begrenzungen und nicht unterstützte Felder.

## Fertig, wenn
- Implementierung aktuelle API-Dokumentation beachtet.
- Anbieterabweichungen nicht still verworfen werden.

## Schutz
Anbieterkompatibilität ist keine Erlaubnis für fremde Abo-Anmeldungen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Anthropic-API-Format-Skill suchen; Quelle und Lizenz prüfen und Nutzer vor Installation fragen.

---

## Umsetzungsergebnis (2026-10-01)

**Implementierung:** `AnthropicMessageFormat.kt` — `AnthropicMessageFormatter` bildet das Anthropic Messages API-Format vollständig ab (Text-Nachrichten, System-Prompt, `max_tokens`, `stream`, `anthropic-version`-Header). Fehlende Felder (tool_use, vision) werden nicht still verworfen, sondern durch Rückgabe von `FormatError` sichtbar gemacht.

**Anbieterspezifisch:** Dieses Format ist nur für Anbieter mit `ApiProtocolFormat.ANTHROPIC_MESSAGES` zu verwenden. Abo-Anmeldungen und OAuth-Token-Relays sind ausgeschlossen.

**Tests:** `AnthropicMessageFormatTest.kt` (19 Tests) deckt Text, Fehler, Parameter-Klammerung und Abwesenheit von Schlüsselwerten ab.
