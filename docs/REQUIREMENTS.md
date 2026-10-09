# REQUIREMENTS — MUSS / SOLL / SPÄTER mit Akzeptanzkriterien

**Stand:** 2026-10-09 · **Grundlage:** `claudroide-spec.md` §1, §3–§9, §14 · **Status:** Task 0.1 — aus Spec abgeleitet

---

## 1. MUSS — Stufe 1 (erste fertig nutzbare Version)

Diese Anforderungen sind **zwingend** für die Definition von „v1 fertig" (§1.3). Beide Beweis-Läufe (Coding-Agent + Device-Agent) müssen erfolgreich absolviert sein.

### 1.1 Projekt-Grundgerüst

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-1.1 | App baut aus `claudroide-next` (neue Kennung `org.claudroide.next`) | `./gradlew :app:assembleDebug` grün; APK installierbar neben alter App |
| R-1.2 | Banner + Symbol **byte-identisch** aus `~/claudroide` übernommen | `sha256sum` der Assets stimmt überein; Einbau optisch geprüft |
| R-1.3 | Alle Unit-Tests grün, Zahl **selbst gemessen** | Zahl + Datum in `progress/BUILD-STATE.md`; Abweichung zu 2530/2535 dokumentiert |
| R-1.4 | Neues öffentliches GitHub-Repo `mertgoevse-wq/claudroide-next` (Apache 2.0) | Repo existiert; `LICENSE` (Apache 2.0), `NOTICE` (nur Fremdteile), Issues an, Releases an |
| R-1.5 | Commit + Push pro Block direkt auf `main`, **vorher Secret-Scan** | Secret-Scan blockt Test-Schlüssel; keine Build-Reste, keine privaten Daten im Repo |

### 1.2 Chat + BYOK + Provider (Mindestmenge)

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-2.1 | Provider: **Anthropic, OpenAI, OpenRouter** (Wire-Format-Abstraktion) | Jeder Provider liefert echte Testantwort bei gültigem Key |
| R-2.2 | Provider-Erreichbarkeitsprüfung beim Einrichten | App testet automatisch; nicht erreichbare werden ausgeblendet |
| R-2.3 | **Ein-Klick-Gratis:** OpenRouter Free + Kilo-Code Free | Ohne Guthaben Antwort möglich; Anmeldung/Key nur einmal nötig |
| R-2.4 | Android Keystore für alle Schlüssel | Schlüssel **nie** im Klartext, nie in Git, nie im Protokoll (Redaction-Test) |
| R-2.5 | Eigene Custom-Endpoints (URL + Key + Modellliste) als Konfiguration | Kein Code für neuen kompatiblen Provider nötig |

### 1.3 Automatisches Routing + Failover + Kosten

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-3.1 | Rollen-Aliase: `role:fast`, `role:coder`, `role:vision`, `role:summary` | Jede Rolle löst plausibles Modell auf; Wahl im Protokoll sichtbar |
| R-3.2 | Auto-Failover: 2–3 Versuche, dann nächster Dienst, Text geht nie verloren | Absichtlich kaputter Dienst → Lauf läuft weiter, Wechsel protokolliert |
| R-3.3 | Kostenanzeige (Tages-/Monatsübersicht je Dienst), **kein Limit, kein Stopp** | Kosten sichtbar; nicht Erhebbares = „nicht verfügbar"; keine Warnung |

### 1.4 Coding-Agent (vertikal)

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-4.1 | Werkzeuge: Dateien lesen/schaffen/ändern/löschen, Shell (leicht + PRoot wählbar), Diffs | Ein echter kleiner Auftrag läuft durch; Änderung sichtbar |
| R-4.2 | Harte Grenzen: **exakt 1 Shell**, **exakt 1 Subagent** gleichzeitig | Test startet zwei Jobs → zweiter wird abgewiesen (nicht nur im Prompt!) |
| R-4.3 | Verifikation vor „fertig": Test-/Build-Erkennung, Ergebnisprüfung, ehrlicher Bericht | Absichtlich kaputter Test → Lauf meldet Fehler, nicht „fertig" |
| R-4.4 | Sicherungen vor jeder Änderung; unbegrenzt rückgängig; Speicher voll → pausieren + fragen | Löschen → Rückgängig stellt Datei wieder her; voller Speicher pausiert |
| R-4.5 | Blocker-Regel: nach 2 Fehlversuchen notieren, nächste Aufgabe, Lauf endet nie still | Blocker stehen im Checkpoint |

### 1.5 Device Agent (Bildschirmsteuerung)

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-5.1 | Eigener `AccessibilityService`: Element-Liste lesen, Gesten (tap/swipe/zurück/home/recents) | Am Gerät: Element-Liste kommt an; Geste wirkt |
| R-5.2 | **Immer** Screenshot **und** Liste; Bild als Augen wenn Liste leer | Grafik-App (Musik-Programm) wird über das Bild bedient |
| R-5.3 | Text mit Umlauten (Zwischenablage-Weg, Rückfall mit Hinweis) | „ä ö ü ß" kommt richtig an |
| R-5.4 | **Not-Aus dreifach:** Knopf oben, schwebender Knopf, Leiser-Taste zweimal | Alle drei stoppen sofort; nichts startet danach von selbst |
| R-5.5 | Sperrliste: Standard Geld/Banking/Bezahlung/Passwörter; erweiterbar; gesperrt = **kein** Tippen, **kein** Bild, **kein** Senden | Test-App in Sperrliste wird abgewiesen |
| R-5.6 | ADB-Notweg: **ein** adb-Server (5037), kabellose Fehlersuche, scrcpy + Rückfall | Aus Termux: Bild holen, tippen, App starten |

### 1.6 Context Manager

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-6.1 | Automatische Kompression + Rotation mit Gedächtnis-Datei | Lauf über Grenze → Arbeit geht **nicht** verloren; Fortsetzung korrekt |
| R-6.2 | Zustand auf Platte nach jedem Schritt: Aufgabe, Plan, Fortschritt, Entscheidungen, Blocker, letzte Dateien | App-Neustart → Lauf macht am selben Punkt weiter |

### 1.7 Onboarding + Geräteprüfung + Lokale Modelle (Stufe 1)

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-7.1 | Begrüßung mit Banner; Frage nach alten Gesprächen (Import) | Alle 4 Schritte laufen einmal durch; Ablehnen eines Rechts lässt Rest nutzbar |
| R-7.2 | Geräteprüfung (Speicher, RAM, Kerne, Vulkan, Android-Version, Wärme, Akku) **im Klartext** | Werte stimmen mit Wirklichkeit überein (nachmessen) |
| R-7.3 | Automatische Auswahl bestes kleines Modell (Grafik → Prozessor → NPU), **einmalige Nachfrage + Download** bis 4 GB mit vollem Ladebalken | Begründung verständlich; Ergebnis nach Neustart noch da; Selbsttest nach Laden |
| R-7.4 | Eigene Tür `127.0.0.1:20130` für lokale Modelle (Prüfschlüssel, Protokoll) | Aufruf aus Termux liefert Antwort; Protokoll zeigt ihn |

### 1.8 Fertig-Beweis (Definition of Done für v1)

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-8.1 | **Coding-Agent-Beweis:** kleine Webseite baut sich selbst, Vorschau in App, Screenshot + Prüfprotokoll | Screenshot der Vorschau + Prüfprotokoll + Eintrag in `BUILD-STATE.md` |
| R-8.2 | **Device-Agent-Beweis:** Browser (Seite aufrufen + prüfen), dann Musik-App (Titel suchen + starten + prüfen) | Screenshots vor/nach + Protokoll + Eintrag in `BUILD-STATE.md` |
| R-8.3 | Nach **beiden** Beweisen: **kurz anhalten und fragen** bevor Stufe 2 | Nutzer-OK eingeholt |

---

## 2. SOLL — Erweiterungen (nach Stufe 1, vor/nach Release)

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-9.1 | Weitere Provider: Groq, Mistral, NVIDIA NIM, Bynara, Ollama Cloud, Orcarouter, Fastrouter | Jeder Provider getestet; Wire-Format-basiert, kein neuer Code |
| R-9.2 | DroidRoute als optionaler Custom-Provider (nach frischer Standserhebung) | Integration nur wenn Nutzer will; Provider-Abstraktion erlaubt es |
| R-9.3 | Skills/Plugins/MCP/Prompt-Kataloge (Progressive Disclosure, §7) | Katalog sichtbar; Fähigkeit wird erst bei passender Aufgabe ganz geladen |
| R-9.4 | Projekte/Dateien/Git vertikal: Browser, Git-UI, GitHub-Sync (privat, AUS per Default) | Commit im Projekt möglich; Secret-Scan blockt Testschlüssel; Vorschau vor Push |
| R-9.5 | Media Runtime (Bilder, Audio, MIDI, Video, Dokumente) + claude-media-bridge | Erzeugung + Verständnis; MCP-Anbindung |
| R-9.6 | Anti-Slop-Prüfung im Lauf (Textprüfer gegen Werbesprache, erfundene Zahlen, Emoji-Wolken) | Prüfer findet eingebauten Testfall |

---

## 3. SPÄTER — Stufe 2 + Forschung

| ID | Anforderung | Akzeptanzkriterium |
|---|---|---|
| R-10.1 | **Stufe 2:** Spracheingabe, Vorlesen, Wakewort (sichtbarer Zustand, jederzeit abschaltbar) | Nach bestandenen Beweis-Läufen + kurzer Nutzer-Rückfrage |
| R-10.2 | Audio-/Dokumenten-/Videoauswertung (kurze Videos zuerst) | Inhalte werden verstanden und verarbeitet |
| R-10.3 | NPU-Forschungsblock: LiteRT-Samsung-Backend, Samsung Neural SDK (NNAPI **nicht**) | Schriftliches Ergebnis je Weg mit Quelle/Datum; Partnerfreigabe ehrlich als offen |
| R-10.4 | Feintuning-Experiment am Handy (Platzprüfung, Abbruch bei Hitze) | Entweder belegtes Ergebnis oder dokumentiertes Scheitern |
| R-10.5 | Modell-Studio (eigene Modelle hereinholen, umwandeln, messen, bewerten) | Nach Phase 9 Messliste |
| R-10.6 | App arbeitet im abgetrennten Bereich am eigenen Programm | Nach Absicherungen §24 scharf |

---

## 4. Nicht-Ziele (explizit AUSGESCHLOSSEN)

| Thema | Begründung |
|---|---|
| Kein „einfacher Chat-Prototyp"/AI-Slop | Hohe Qualität, Ruhe, tägliche Nutzbarkeit (§1.2) |
| Keine Nachahmung von Claude-/Anthropic-Marken | Eigenständige Marke, Clean-Room (§1.2) |
| Kein Root, keine Umgehung Android-Sicherheit | §2.4, §8, §29.3 |
| Kein monatliches Ausgabenlimit/Warnsystem | Nutzerentscheidung: nur Anzeige, kein Stopp (§3.7) |
| Kein Verlust von Agentenarbeit durch Kontextrotation | „neuer Kontext ≠ vergessen" (§1.2) |
| NNAPI verfolgen | Abgekündigt (§23) |
| Zweiter adb-Server | Ausdrücklich verboten (§6.3) |
| Fremde Gesamt-Repos in öffentliches Repo | READ-ONLY (§2.4) |
| Banner-Motiv ersetzen | Nie durch anderes KI-Bild (§2.2) |

---

## 5. Querbezüge

- **Spec:** §1 (Produktvision), §3 (Provider), §4 (Coding-Agent), §5 (Context), §6 (Device Agent), §7 (Skills), §8 (Sicherheit), §9 (Onboarding), §13 (Roadmap), §14 (Anforderungen), §17 (GitHub), §18 (README/Slop)
- **Tasks:** Phase 0–10 in `docs/TASKS.md`
- **Entscheidungen:** `docs/DECISIONS.md`
- **Architektur:** `docs/ARCHITECTURE.md`
- **Roadmap:** `docs/ROADMAP.md`
- **Sicherheit:** `docs/SECURITY.md`