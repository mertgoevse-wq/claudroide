---
id: "038"
title: "Stoppen und Wiederholen"
wave: "W9"
depends_on: [031, 035]
files: [app/src/main/java/org/claudroide/app/feature/chat/ExecutionController.kt, app/src/test/java/org/claudroide/app/ExecutionControllerTest.kt, tasks/038-stop-retry-and-continue.md]
skills: [`testing-setup`, `android-permissions-security`]
status: done
gate: false
done_since_last_edit: true
content-hash: "acebc2f7700f6e55"
---
# Aufgabe 038 — Stoppen und Wiederholen

## Ziel
Nutzer steuern laufende oder unvollständige Antworten sicher.

## Ergebnis
Stop-, Retry- und Continue-Aktionen mit klarer Information über neue Anfrage und mögliche Kosten.

## Fertig, wenn
- Wiederholen eine neue Anfrage nur nach erkennbarem Nutzerbefehl sendet (`ExecutionControlEngine.evaluateRetry` verweigert unaufgeforderte Auto-Retries).
- Stoppen sowohl den SSE-Stream als auch laufende Agent-Werkzeuge sauber abbricht (`requestStop` setzt Abbruchsignal und terminiert Werkzeugprozess).
- Abbruch ehrlich über bereits getätigte Änderungen berichtet.

## Schutz
Bereits ausgeführte Datei- oder Netzwerkaktionen werden nicht automatisch als ungeschehen behauptet (`SideEffectCommitRecord` hält getätigte Dateimodifikationen transparent fest).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/ExecutionController.kt`:
  - `enum class ExecutionStatus`: IDLE, STREAMING_TEXT, EXECUTING_AGENT_TOOL, STOPPED_BY_USER, COMPLETED, FAILED.
  - `data class SideEffectCommitRecord`: Ehrliche Protokollierung nicht rückgängig gemachter Datei- und Netzwerkmutationen.
  - `data class AbortResult`: Transparente Rückmeldung über Abbruch von Socket- und Werkzeugprozessen.
  - `class ExecutionControlEngine`: Steuerung von Abbruch, Wiederholung mit Kostenhinweis und Fortsetzung.
- `app/src/test/java/org/claudroide/app/ExecutionControllerTest.kt`:
  - Unit-Tests für Stream-Abbruch, Werkzeugabbruch, ehrliche Nebenwirkungsberichte und Blockade automatischer Wiederholungen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `testing-setup`: Unit-Tests zur Verifikation von Abbruch- und Wiederholungsentscheidungen implementiert.
- `android-permissions-security`: Schutz vor ungewollten wiederholten Kosten und Transparenz irreversibler Dateizugriffe abgesichert.

