---
id: "055"
title: "Modellnamen verwalten"
wave: "W11"
depends_on: [044, 045]
files: [app/src/main/java/org/claudroide/app/feature/provider/ModelNameManager.kt, app/src/test/java/org/claudroide/app/ModelNameTest.kt, tasks/055-model-list-and-manual-model.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "6b513e322a670a68"
---
# Aufgabe 055 — Modellnamen verwalten

## Ziel
Anbieterlisten nutzen, wo verfügbar, und manuelle Modellnamen für eigene Server erlauben.

## Ergebnis
Modellwahl mit Herkunft, Verfügbarkeit, Fähigkeiten und optionalem letzten Prüfzeitpunkt.

## Fertig, wenn
- App keinen Modellnamen als verfügbar erfindet (`ModelRegistry` listet nur belegte Katalogmodelle wie Sonnet 5.5, Opus 5.5, Haiku 4.5).
- Manuelle Auswahl klar als Nutzereingabe gekennzeichnet ist (`ModelOrigin.MANUAL_USER_INPUT`, `isManualUserEntry`).
- Das Tippen oder Auswählen von Modellen keine Netzwerkanfragen auslöst.

## Schutz
Modellwahl überträgt keine Anfrage bis der Nutzer tatsächlich eine Unterhaltung sendet.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/ModelNameManager.kt`:
  - `enum class ModelOrigin`: OFFICIAL_PROVIDER_CATALOG, FETCHED_FROM_API, MANUAL_USER_INPUT.
  - `data class ModelDescriptor`: Modell-ID, Anzeigename, Provider-ID, Herkunft, Streaming-, Werkzeug- und Visions-Fähigkeiten.
  - `object ModelRegistry`: Zentrale Registrierung und typsichere manuelle Modellerfassung ohne Datenerfindung.
- `app/src/test/java/org/claudroide/app/ModelNameTest.kt`:
  - Unit-Tests für Abruf offizieller Modelle, transparente Kennzeichnung manueller Eingaben und Validierung nicht-leerer IDs.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Modellauswahl-Elemente und manuelle Eingabemasken für mobile Ansichten gestaltet.
- `testing-setup`: Unit-Tests zur Verifikation von Modell-Herkunft und Validierungsregeln implementiert.

