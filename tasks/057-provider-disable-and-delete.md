---
id: "057"
title: "Anbieter pausieren und löschen"
wave: "W11"
depends_on: [044, 045]
files: [tasks/057-provider-disable-and-delete.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "19d07fc059ed7ddb"
---
# Aufgabe 057 — Anbieter pausieren und löschen

## Ziel
Anbieterzugang deaktivieren oder entfernen, ohne alte Unterhaltungen zu verlieren.

## Ergebnis
Deaktivieren, Schlüssel entfernen, Chatmodell als nicht mehr verfügbar markieren und wiederherstellen.

## Fertig, wenn
- Löschen des Schlüssels und Löschen des Chatverlaufs getrennte Aktionen sind.
- Nutzer klar sieht, ob Einstellungen endgültig entfernt werden.

## Schutz
Zugänge sofort aus aktivem Speicher entfernen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Datenlebenszyklus-Skill suchen; Quelle und Lizenz prüfen und Nutzer vor Installation fragen.
