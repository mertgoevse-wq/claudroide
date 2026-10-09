# ROADMAP — Phasen & Wellen (aus Spec §13 ausgearbeitet)

**Stand:** 2026-10-09 · **Grundlage:** `claudroide-spec.md` §13 · **Status:** Task 0.1 — aus Spec abgeleitet

---

## 1. Phasenübersicht

| Phase | Titel | Inhalt (Kurz) | Meilenstein |
|---|---|---|---|
| **0** | Bestandsaufnahme | Quellen sichten, Spec vermessen, Doku-Gerüst, Skills-Index, Loop-Skelett | Alle Planungsdateien stehen; Messwerte belegt |
| **1** | Grundgerüst | Code-Umzug, neue App-Kennung, Banner 1:1, Build+Tests grün, CI/Secret-Scan | `assembleDebug` grün; beide Apps parallel installierbar |
| **2** | Chat + Dienste + Schlüssel | Provider (Anthropic, OpenAI, OpenRouter), Keystore, Ein-Klick-Gratis, Routing, Failover, Kostenanzeige, Onboarding, DISCUSS/BUILD | Chat funktioniert ohne Guthaben; Umschalter sichtbar |
| **3** | Coding-Agent (vertikal) | Dateien/Shell/Diffs/Tests, Job-Grenzen (1/1), Verifikation, Sicherungen, Blocker-Regel, **Beweis: kleine Webseite** | Agent baut Webseite selbst; Vorschau + Screenshot + Protokoll |
| **4** | Kontext + Zustand | Kompression, Rotation, Disk-Persistenz, Wiederaufnahme (`WEITERARBEITEN`), Nutzer-Präferenzen | App-Neustart → Lauf macht am selben Punkt weiter |
| **5** | Projekte/Dateien/Git | Projektordner (app-eigen + frei wählbar), Syntax-Farben, Bearbeiten, Git-UI, GitHub-Sync (privat, AUS) | Commit im Projekt möglich; Secret-Scan blockt; Vorschau vor Push |
| **6** | Device Agent | AccessibilityService, Gesten, Screenshot+Liste, Umlaute, **dreifacher Not-Aus**, Sperrliste, ADB-Notweg, **Beweis: Browser + Musik-App** | Gerät: Browser-Seite aufrufen/prüfen → Musik-App Titel starten/prüfen |
| **7** | Fähigkeiten/Katalog | Skill-/Plugin-/MCP-/Prompt-Kataloge (Progressive Disclosure), Awesome-Prompts-Pipeline, Sicherheitsprüfung | Katalog sichtbar; Fähigkeit lädt erst bei passender Aufgabe |
| **8** | Kleine Modelle (Lokal) | Geräteprüfung, Katalog (GGUF ≤ 4 GB), Download mit Fortsetzen, **Grafik (Vulkan) → Prozessor → NPU**, Selbsttest + Begründung | Bestes Modell läuft offline; Auswahl begründet; Ladebalken voll |
| **9** | NPU-Forschung | LiteRT-Samsung-Backend, Samsung Neural SDK, Messliste (Wörter/s, Zeit, RAM, Wärme, Abstürze), Brücke über 20130 | Schriftliches Ergebnis je Weg; bei Misserfolg ehrliche Lücke |
| **10** | Absicherung + Veröffentlichung | 4 Absicherungen scharf, Anti-Slop-Prüfer, README/Releases, Bau im Internet, Code-Hygiene, Werkzeug-Räumung | Jeder Meilenstein: Release mit APK; README ehrlich; Build grün |
| **11** | Stufe 2 + Later | Spracheingabe, Vorlesen, Wakewort, Audio/Doku/Video-Auswertung, weitere Provider, DroidRoute, Musik-Steuerung, Modell-Studio, App-Self-Improvement | **Nach kurzer Nutzer-Rückfrage** (Beweis-Läufe 3.6 + 6.7 bestanden) |

---

## 2. Wellenplan (Parallellauf-Regeln beachten!)

| Welle | Tasks | Startbedingung | Hinweis |
|---|---|---|---|
| **W0** | 0.1 → 0.2 → 0.3 → 0.4 → 0.5 → 0.6 → 0.7 → 0.8 | Sofort | Streng der Reihe nach (gleiche Dateien) |
| **W1** | 1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6 | W0 fertig | 0.7–0.8 früh abschließen |
| **W2** | 2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9 | W1 fertig | |
| **W3** | 3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 3.6 | W2 fertig | |
| **W4** | 4.1 → 4.2 → 4.3 | W3 fertig | |
| **W5** | 5.1 → 5.2 → 5.3 → 5.4 | W4 fertig | |
| **W6** | 6.1 → 6.2 → 6.3 → 6.4 → 6.5 → 6.6 → 6.7 → 6.8 | W5 fertig | Gerätetest nötig |
| **W7** | 7.1 → 7.2 → 7.3 → 7.4 (7.5 jederzeit) | W6 fertig | |
| **W8** | 8.1 → 8.2 → 8.3 → 8.4 → 8.5 → 8.6 → 8.7 | W7 fertig | Grafik → Prozessor → NPU |
| **W9** | 9.1 → 9.2 → 9.3 → 9.4 | W8 fertig | Nur bei Gerät |
| **W10** | 10.1 → 10.2 → 10.3 → 10.4 → 10.5 → 10.6 | Ab W3 parallel möglich | 10.5/10.6 nach jedem Meilenstein |
| **W11** | 11.1 – 11.8 | Nach Stufe 1 + Nutzer-OK | |

**Harte Parallel-Regel (A56):** Maximal **1 Subagent**, **1 Shell-Befehl**, **1 Build** gleichzeitig. Nur bei getrennten Dateien darf parallel gearbeitet werden.

---

## 3. Grobe Zeitschätzung (Schätzung, keine Zusage)

| Phase | Spanne | Flaschenhälse |
|---|---|---|
| Phase 0 | 1–2 Tage | Sichtung, Messungen, wenig Gerätelast |
| Phase 1 | 2–4 Tage | Umzug, erster grüner Build (langsam: 2,2 GB freier RAM) |
| Phase 2 | 4–7 Tage | Kern der App (Provider, Routing, Chat) |
| Phase 3 | 4–7 Tage | + Beweis-Lauf Webseite |
| Phase 4 | 2–3 Tage | |
| Phase 5 | 3–5 Tage | |
| Phase 6 | 5–9 Tage | **Gerätetest nötig**; ohne Kabel nur teilweise prüfbar |
| Phase 7 | 4–6 Tage | |
| Phase 8 | 5–8 Tage | Grafik zuerst, dann Prozessor |
| Phase 9 | **2–3 Wochen** | Eigenes Experiment; Erfolg nicht garantiert |
| Phase 10 | Laufend | Bei jedem Meilenstein |

**Begrenzende Faktoren:** Speicher (2,2 GB freier RAM), **ein Build gleichzeitig**, fehlendes Kabel. Nachtarbeit am Ladekabel erlaubt (§19.11).

---

## 4. Abhängigkeiten & Kritischer Pfad

```
0.1 → 0.2 → 0.3 → 0.4 → 0.5 → 0.6 → 0.7 → 0.8
    ↓
1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6
    ↓
2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9
    ↓
3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 3.6  (Coding-Agent-Beweis)
    ↓
4.1 → 4.2 → 4.3
    ↓
5.1 → 5.2 → 5.3 → 5.4
    ↓
6.1 → 6.2 → 6.3 → 6.4 → 6.5 → 6.6 → 6.7 → 6.8  (Device-Agent-Beweis)
    ↓
7.1 → 7.2 → 7.3 → 7.4
    ↓
8.1 → 8.2 → 8.3 → 8.4 → 8.5 → 8.6 → 8.7
    ↓
9.1 → 9.2 → 9.3 → 9.4
    ↓
10.1 → 10.2 → 10.3 → 10.4 → 10.5 → 10.6
    ↓
[NUTZER-RÜCKFRAGE]
    ↓
11.x (Stufe 2)
```

**Entscheidungspunkte mit Nutzer-Rückfrage:**
- Nach Phase 3.6 + 6.7 (beide Beweis-Läufe) → Start Stufe 2 (§1.3, §26.16)
- Vor öffentlicher Repo-Anlage (§19.3) — erfolgt in Phase 1.1 automatisiert

---

## 5. Risiken & Gegenmaßnahmen

| Risiko | Gegenmaßnahme |
|---|---|
| A56 stürzt bei Parallelität ab | **Immer**: 1 Subagent, 1 Shell, 1 Build; Checkpoint vor jedem Schritt |
| Kein Gerät per adb | Ohne Kabel: so weit wie möglich bauen; Gerätetests offen markieren, nicht behaupten |
| NPU nicht erreichbar | Forschungsblock isoliert; Rückfall (Grafik/Prozessor) bleibt funktionsfähig |
| Build bricht ab (Speicher) | Vor jedem Build: Platz prüfen; nur 1 Build; Artefakte nicht doppelt speichern |
| Geheimnisse im Repo | Secret-Gate **vor jedem Push**; Redaction-Test in CI |
| Kontext-Verlust bei Rotation | Context Manager (§4) + `WEITERARBEITEN`-Dateien (§29.8) |
| Alte Repo-Daten versehentlich ins öffentliche Repo | Quellen read-only; Private Repos niemals pushen; Secret-Scan prüft Pfade |

---

## 6. Querbezüge

- **Tasks:** `docs/TASKS.md` (detaillierter Arbeitsplan mit Akzeptanzkriterien)
- **Requirements:** `docs/REQUIREMENTS.md` (MUSS/SOLL/SPÄTER je Baustein)
- **Architecture:** `docs/ARCHITECTURE.md` (Bausteine, Datenflüsse, Modul-Grenzen)
- **Security:** `docs/SECURITY.md` (Sicherheitsarchitektur)
- **Decisions:** `docs/DECISIONS.md` (Architektur-Entscheidungen)
- **Source Index:** `docs/SOURCE_INDEX.md` (Herkunft je Baustein)
- **Reuse Matrix:** `docs/SOURCE_REUSE_MATRIX.md` (KEEP/ADAPT/REWRITE/REFERENCE/IGNORE)