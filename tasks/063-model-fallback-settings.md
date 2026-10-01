---
id: "063"
title: "Ersatzmodell einstellen"
wave: "W13b"
depends_on: [062, 064, 065]
files: [tasks/063-model-fallback-settings.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "6c3cd61b585123d6"
---
# Aufgabe 063 — Ersatzmodell einstellen

## Ziel
Nutzer entscheidet, ob die App bei Fehlern nachfragt oder einen freigegebenen Ersatz verwendet.

## Ergebnis
Einstellung pro Projekt mit Standard „vorher fragen“ und sichtbarem Ersatzanbieter.

## Fertig, wenn
- Automatischer Wechsel nur nach ausdrücklicher Einrichtung erfolgt.
- Anbieterkosten und Datenregeln erneut berücksichtigt werden.

## Schutz
Keine stille Übertragung an anderen Anbieter.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen KI-Routing-/Datenschutz-Skill suchen, Quelle/Lizenz prüfen und Zustimmung vor Installation einholen.
