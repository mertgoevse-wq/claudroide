# CLAUDROIDE — Aktueller Projektstand

> **Zweck dieser Datei:** Sie ist der **erste Ort**, den ein neuer Lauf liest (neue Claude-Code-Sitzung, ChatGPT, Freebuff). Aus ihr muss ohne Rückfragen klar sein, wo die Arbeit steht und was als Nächstes automatisch passiert.
> **Pflicht:** Nach **jedem** abgeschlossenen Aufgabenblock aktualisieren — zusammen mit `progress/BUILD-STATE.md`.
> **Sprache:** Englische Fachbegriffe erlaubt, aber jede Zeile so, dass der Nutzer sie versteht (einfaches Deutsch).
> **Spec-Lage (2026-10-08, zusammengeführt):** `claudroide-spec.md` ist die **einzige gültige Gesamtspezifikation** (Interviews 1–11, inkl. §29 Start/Wiederaufnahme). Alte Teil-Specs liegen unter `archive/`. Wiederaufnahme-Wort nach Absturz: **`WEITERARBEITEN`** (§29.8).

---

## 0. Kopfzeile (immer zuerst füllen)

**⚠️ Wiederherstellungs-Hinweis (2026-10-08, abends):** Die Spec wurde in dieser Sitzung durch einen fehlerhaften Zusammenbau-Befehl versehentlich auf 48 Zeilen gekürzt. Sie wurde **aus dem Backup (`archive/claudroide-spec-v2-backup.md`, Stand Interview 4) + den wörtlich vorliegenden Interview-5/6/7-Inhalten dieser Sitzung** vollständig rekonstruiert: 1094 Zeilen / 118.652 Bytes, alle Abschnitte §0–§25 + Anhang A/B/C, Format-Prüfung bestanden (Tabellen, fences, doppelte Überschriften). Anhang A/B enthalten die Interview-1-3-Protokolle aus dem Backup; die Interview-5/6/7-Entscheidungen stehen in §20/§21/§25. Wer die Geschichte prüfen will: `archive/claudroide-spec-v2-backup.md` ist die Vor-Rekonstruktions-Basis.

| Feld | Wert |
|---|---|
| **Stand** | `2026-10-09` (Planung abgeschlossen — Bau noch nicht begonnen; OPTIMIZE-FIRST ausgeführt) |
| **Aktuelle Phase** | Phase 0 (Bestandsaufnahme) |
| **Nächster Task** | **Task `0.1`** — Projektgerüst + Doku-Ebenen anlegen (`docs/REQUIREMENTS.md`, `ARCHITECTURE.md`, `ROADMAP.md`, `SECURITY.md` aus der Spec ableiten) — **nicht** `OPTIMIZE-FIRST.md` mehr (bereits ausgeführt) |
| **Letzter Schritt in diesem Lauf** | OPTIMIZE-FIRST ausgeführt: Widersprüche geklärt — `docs/SOURCE_REUSE_MATRIX.md` angelegt (Spec §10), `docs/TASKS.md` Task 0.1 um SOURCE_REUSE_MATRIX.md ergänzt, `docs/DECISIONS.md` um Anlage-Eintrag ergänzt, `CLAUDROIDE_CURRENT_STATE.md` und `progress/BUILD-STATE.md` aktualisiert; Task 0.7 (Skills-Index) bleibt erledigt |
| **Letzte abgeschlossene Tasks** | keine (nur Planung) |
| **Läuft gerade** | nichts — kein Build, kein Shell-Job, kein Subagent |
| **Not-Aus-Status** | nicht ausgelöst |
| **Nachweis-Datei** | `progress/BUILD-STATE.md` (technischer Zwilling dieser Datei) |

---

## 1. Was fertig ist — mit Beleg

| Task | Ergebnis | Beleg (Test/Screenshot/Messung) | Datum |
|---|---|---|---|
| — | noch nichts | — | — |

**Regel:** Eine Zeile darf nur hier stehen, wenn der Beleg existiert und im `BUILD-STATE.md` verlinkt ist. Keine Wunschliste.

---

## 2. Offene Probleme und Blocker

| Blocker | Seit | Versuche | Warum noch offen | Nächster Versuch |
|---|---|---|---|---|
| Kein Handy per Kabel (`adb devices` leer) | 2026-10-08 | 1 | Gerätebeweise nicht möglich | Nutzer schließt Gerät an oder aktiviert kabellose Fehlersuche |
| Testzahl widersprüchlich (2535 vs. 2530 in Repo-Dokumenten) | 2026-10-08 | 0 | Selbstmessung steht aus | Task 0.3 |
| NPU-Zugriff unbelegt | 2026-10-08 | 0 | Forschungsblock Phase 9 | Task 9.1 |
| Vor dem ersten Bau: noch nicht existierende Referenz-Dateien (CLAUDE.md, `progress/BUILD-STATE.md`, `docs/DECISIONS.md`) sind **jetzt angelegt**; `docs/REQUIREMENTS.md`/`ARCHITECTURE.md`/`ROADMAP.md`/`SECURITY.md` folgen in Task 0.1; `docs/SOURCE_REUSE_MATRIX.md` **angelegt** (2026-10-09, OPTIMIZE-FIRST Lauf); `docs/SKILL-INDEX.md` ist **erledigt** (Task 0.7) | 2026-10-09 | 1 | Gerüst-Aufgabe steht aus | Task 0.1 |

---

## 3. Wichtige Entscheidungen (Kurzform, Langform in `docs/DECISIONS.md`)

| Datum | Entscheidung | Grund | Quelle |
|---|---|---|---|
| 2026-10-08 | Eigene Maschine **und** echte Claude-Code-Maschine; echte automatisch, wenn Anthropic-Schlüssel vorhanden | Nutzerwunsch | Spec §21 |
| 2026-10-08 | `claudroide-spec.md` = einzige gültige Gesamtspezifikation; alte Teil-Specs in `archive/`; §29 regelt Start (§29.7) und Wiederaufnahme mit `WEITERARBEITEN` (§29.8) | Nutzerentscheidung (Zusammenführung) | Spec §29, `docs/DECISIONS.md` |
| 2026-10-08 | Wöchentliche lesbare Sicherung an **zwei Orten**; Commit+Push nach jedem Block mit Secret-Scan | Nutzerbestätigung (Klärungsrunde) | Spec §5.4/§19.3, `docs/DECISIONS.md` |
| 2026-10-08 | Anschlüsse: **20128 nur OmniRoute**, **20130 Claudroide**, 8787 DroidRoute, 5037 der eine adb-Server | Nutzerentscheidung | Spec §3.1 |
| 2026-10-08 | Kleine Modelle: **GGUF** Standard, **LiteRT** für NPU; Reihenfolge **Grafik → Prozessor → NPU** | Nutzerentscheidung | Spec §7.4b/§23 |
| 2026-10-08 | Eigenes Code-Material **ohne** Herkunftsangabe; Fremdteile mit kurzer Zeile; fremde Gesamt-Repos bleiben privat | Nutzerwunsch + Lizenzpflicht | Spec §2.4 |
| 2026-10-08 | Nur **1 Subagent**, **1 Shell**, **1 Build** gleichzeitig (A56 stürzt sonst ab) | gemessene Gerätegrenze | Spec §4.2 |

---

## 4. Genutzte Quellen in diesem Lauf

| Quelle | Pfad/Link | Wofür benutzt | Ergebnis |
|---|---|---|---|
| — | — | noch kein Lauf | — |

**Regel:** Hier steht nur, was **tatsächlich** in diesem Lauf gelesen oder geklont wurde — nicht die ganze Liste (die steht in `docs/SOURCE_INDEX.md`).

---

## 5. Tests und Nachweise

| Prüfung | Befehl | Ergebnis | Datum |
|---|---|---|---|
| — | — | noch nicht gelaufen | — |

**Regel:** Immer der **echte** Befehl, die **echte** Zahl, das **echte** Datum. Bei Fehlschlag: Fehlschlag eintragen, nicht weglassen.

---

## 6. Bekannte Einschränkungen (ehrlich)

- Bau läuft auf einem Handy in einer PRoot-Umgebung: **~2,2 GB freier Arbeitsspeicher**, **22 GB freier Speicher**, 8 Kerne → langsame Builds, ein Build gleichzeitig.
- **Kein Kabel** → Gerätebeweise (Bildschirmsteuerung, APK-Installation) noch offen.
- **NPU** für Apps normalerweise gesperrt; Wege sind recherchiert, aber **nicht belegt** (Spec §23).
- **Kein Anthropic-Schlüssel** vorhanden → die echte Claude-Maschine ist vorbereitet, aber ungetestet.

---

## 7. Was als Nächstes automatisch passiert

1. Task `0.1` abarbeiten (Projektgerüst + Dokumente aus der Spec).
2. Danach `0.2` (Quellen erheben), `0.3` (Ist-Stand selbst messen), `0.4` (ungeordnete Arbeit im alten Ordner bewerten), `0.5` (Umgebung prüfen), `0.6` (Container-Ansatz entscheiden).
3. Nach jedem Task: Ergebnis hier + in `progress/BUILD-STATE.md` eintragen, dann weiter.
4. Anhalten nur bei: fehlendem Nutzerentscheid, fehlendem Schlüssel/Gerät (dann weiterbauen und Lücke offen markieren, Spec §4.4), oder zwei gescheiterten Versuchen.

---

## 8. Übergabe-Hinweis für ein anderes Programm

Wenn ChatGPT oder eine neue Sitzung übernimmt: **Zuerst diese Datei lesen, dann `docs/TASKS.md` (Phase und nächster Task), dann `progress/BUILD-STATE.md` (Belege).** Widersprüche zwischen Dateien werden zugunsten der **neuesten Messung** entschieden und sofort hier eingetragen.
