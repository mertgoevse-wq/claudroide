# Aufgabe 079 — Wiederholungsregeln

## Ziel
Bei vorübergehendem Fehler sicher fortfahren, ohne Aktionen doppelt auszuführen.

## Ergebnis
Wiederholungsregeln je Anfrage- und Werkzeugtyp mit Nutzerinformation.

## Fertig, wenn
- Schreib-, Installations- und Git-Aktionen nicht automatisch wiederholt werden.
- Kostenpflichtige Anfragen vor Wiederholung sichtbar sind.

## Schutz
Wiederholungen von Nebenwirkungen benötigen Idempotenz oder Bestätigung.

## Skill
Global einen API-Retry-/Sicherheits-Skill suchen, Quelle und Lizenz prüfen; Nutzer vor Installation fragen.
