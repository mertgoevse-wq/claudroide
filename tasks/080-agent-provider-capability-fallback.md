---
id: "080"
title: "Anbieterfunktionen abgleichen"
wave: "W16"
depends_on: [061, 070, 071]
files: [tasks/080-agent-provider-capability-fallback.md]
skills: [`/claude-api`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "44cdb92256ac30fb"
---
# Aufgabe 080 — Anbieterfunktionen abgleichen

## Ziel
Agentenfunktionen an nachweisbare Fähigkeiten des aktiven Modells anpassen.

## Ergebnis
Fähigkeitsprüfung und klare Meldung für nicht unterstützte Werkzeuge oder Formate.

## Fertig, wenn
- Kein stiller Anbieterwechsel stattfindet.
- Ersatzweg neue Freigaben und Kostenhinweise berücksichtigt.

## Schutz
Fehlende Rechte werden nicht durch eine andere Anmeldung umgangen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Modelladapter-/Provider-Kompatibilitäts-Skill suchen; Quelle/Lizenz prüfen und Installation bestätigen lassen.
