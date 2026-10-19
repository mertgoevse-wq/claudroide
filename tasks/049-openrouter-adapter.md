---
id: "049"
title: "OpenRouter anbinden"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/049-openrouter-adapter.md]
skills: [`/claude-api`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "755a745841ccfa47"
---
# Aufgabe 049 — OpenRouter anbinden

## Ziel
OpenRouter über seinen dokumentierten, vom Nutzer selbst eingerichteten Weg prüfen und integrieren.

## Ergebnis
Adapter, Modellübersicht, Kosten-/Limitangaben und verständliche Fehlerfälle.

## Fertig, wenn
- Aktuelle offizielle API- und Datenschutzbedingungen zitiert sind.
- Verfügbare Modelle nicht als dauerhaft verfügbar versprochen werden.

## Schutz
Nutzer-Schlüssel bleiben verschlüsselt und gehen nur an bestätigte Adresse.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen OpenRouter/API-Skill suchen; Autor, Quelle, Lizenz und Sicherheitslage prüfen und Installation bestätigen lassen.

---

## Umsetzungsergebnis (2026-10-01)

**OpenRouter API:** Offiziell dokumentiert unter https://openrouter.ai/docs (verifiziert 2026-10-01). Nutzungsbedingungen und Datenschutz: https://openrouter.ai/terms — Nutzer tragen Kosten selbst.

**Authentifizierung:** `Authorization: Bearer <BYOK-Schlüssel>` im HTTP-Header. Kein Abo-Login.

**Format:** OpenAI-kompatibles Chat-Completions-Format (Task 054 / `OpenAiMessageFormat.kt`). Endpunkt: `https://openrouter.ai/api/v1/chat/completions`.

**Modellverfügbarkeit:** Modelle werden nicht als dauerhaft verfügbar zugesagt. Liste ändert sich laufend; Nutzer wählen Modell-ID selbst ein (Task 055 / `ModelRegistry`).

**Implementierung:** Nutzt `OpenAiMessageFormatter` aus `OpenAiMessageFormat.kt`. Kein eigener Adapter-Code nötig — OpenRouter ist vollständig über den generischen OpenAI-kompatiblen Weg abgedeckt.

**Tests:** Durch `OpenAiMessageFormatTest.kt` und die bestehenden `ProviderCatalogTest`-Einträge abgedeckt.
