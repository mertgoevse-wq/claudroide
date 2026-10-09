# DECISIONS — Entscheidungslog

**Regel (Spec §0):** Jede Entscheidungsänderung und jede Widerspruchs-Klärung wird hier mit Begründung festgehalten. Neueste Entscheidungen stehen unten.

| Datum | Entscheidung | Begründung | Quelle |
|---|---|---|---|
| 2026-10-08 | `claudroide-spec.md` ist die **einzige gültige Gesamtspezifikation**; die frühere `claudroide-development-spec.md` ist als §29 eingearbeitet; `claudroide-spec-v2-backup.md` ist historisch | Ein einziges maßgebliches Dokument verhindert widersprüchliche Agent-Läufe; beide alte Dateien liegen unter `archive/` | Nutzerentscheidung (Interview 11, Zusammenführung) |
| 2026-10-08 | Widerspruchs-Rangfolge erweitert: **§29 vor §28** | §29 enthält den verbindlichen Startauftrag und das Wiederaufnahme-Verfahren | §0 dieser Spec |
| 2026-10-08 | Automatik-Sicherung: **wöchentlich, lesbar, an zwei Orten** (App-Bereich + normaler Handy-Ordner); Chats bleiben zusätzlich im App-Bereich | Nutzerbestätigung in der Klärungsrunde; löst den Widerspruch zwischen §5.4 (2 Orte) und Interview-7-Runde 17 (nur App-Bereich) zugunsten von §5.4 | Nutzerentscheidung (Interview 11) |
| 2026-10-08 | Commit + Push **nach jedem Block** direkt auf `main`, immer mit Secret-Scan vorher | Nutzerbestätigung; entspricht Spec §19 Punkt 3 und §25 Punkt 6 | Nutzerentscheidung (Interview 11) |
| 2026-10-08 | Wiederaufnahme-Wort nach Absturz: **`WEITERARBEITEN`** | Nutzerentscheidung; Verfahren in §29.8 und `CLAUDE.md` §2 festgehalten | Nutzerentscheidung (Interview 11) |
| 2026-10-08 | Alte App wird **nicht automatisch importiert**; Sicherungs-Wiederherstellung führt zusammen statt zu überschreiben | Aus der Entwicklungs-Spezifikation (Runde 1) übernommen | §29.3 |
| 2026-10-08 | Anschlüsse: 20128 nur OmniRoute · 20130 Claudroides eigene Tür · 8787 DroidRoute · 5037 der eine adb-Server | Nutzerentscheidung aus Interview 6, unverändert übernommen | §3.8 |
| 2026-10-08 | Kleine Modelle: GGUF Standard, LiteRT für NPU; Reihenfolge Grafik → Prozessor → NPU | Nutzerentscheidung aus Interview 6, unverändert übernommen | §7.4b/§23 |
| 2026-10-08 | Eigenes Code-Material ohne Herkunftsangabe; Fremdteile mit kurzer Herkunftszeile; fremde Gesamt-Repos bleiben privat | Nutzerentscheidung Interview 6, unverändert übernommen | §2.4/§17 |

| 2026-10-09 | `docs/SOURCE_REUSE_MATRIX.md` angelegt (Spec §10) | Spec §10 nennt die Datei als Teil der Dokumentationsstruktur, sie fehlte aber im Projekt. Aus `docs/SOURCE_INDEX.md` §2 (Übernahme-Matrix) abgeleitet und mit Status **UNGEPRÜFT** für alle Einträge angelegt — Entscheidungen erfolgen erst nach tatsächlicher Datei-Sichtung in Task 0.2/0.6. | OPTIMIZE-FIRST Lauf, Widerspruchs-Klärung |

## Offene Punkte (nicht eigenmächtig festlegen)

1. Separate **private Online-Sicherung** für persönliche Notizen — ob/wie, steht offen (§29.9).
2. Manueller Ablauf zum **Teilen eines Fehlerberichts** — offen (§29.9).
3. Eigener **Starthelfer** (Skript/Startknopf) — offen; aktuell nur `claude` + Startauftrag §29.7 bzw. `WEITERARBEITEN` (§29.9).
4. **Testzahl** 2535 vs. 2530 — wird erst durch Task 0.3 (Selbstmessung) entschieden.
5. **DroidRoute-Stand** — vor Integrationsarbeit frisch erheben (§15).
