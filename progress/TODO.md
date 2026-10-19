# Todo — Autonomer Bau

Dieser Vorrat liegt bewusst in `progress/` neben dem Sitzungsprotokoll: er ist die
einzige Datei, die den **offenen** Stand zeigt. `BUILD-STATE.md` protokolliert, *was
passiert ist*; dieses Datei sagt, *was noch fehlt*. Nach einem Absturz ist das hier der
erste Blick.

Legende: `[ ]` offen · `[~]` läuft · `[x]` erledigt und verifiziert · `[!]` blockiert, Grund dahinter

## A — Auftrag vom 2026-10-02

- [x] **A1** Umstellung auf „ClauDroide" (Anzeigename; Package bleibt `org.claudroide.app`) — verifiziert in APK `resources.arsc`
- [x] **A2** Neues Banner: Android-Bot und Terminal-Bot nebeneinander, Hand in Hand (`tools/brand_banner.py` erzeugt `assets/brand/banner.png` + `claudroide_banner.webp`)
- [x] **A3** Signet: bleibt die Einzelfigur (`mark-256.png`), da bei 48–96dp zwei Figuren unlesbar wären (per Test fixiert)
- [x] **A4** Bild-Alt-Texte auf die neue Bildsprache aktualisiert (in `values/` und `values-de/`)
- [x] **A5** Bildsprache gegen den Unabhängigkeitshinweis geprüft — generischer Terminal-Bot mit `>_`, kein Anthropic-Klon (beide Aussagen wahr)
- [ ] **A6** Banner-Feinschliff (später: Nutzer wünscht Nachbesserung am Aussehen, aktuell vorerst abgenommen)

## B — Verifizierbare Technik (ohne Gerät prüfbar)

- [ ] **B1** 4-KB/16-KB-Seitengröße: `.so`-Dateien im APK auf Alignment prüfen
- [ ] **B2** Ergebnis dokumentieren + Test, der es dauerhaft prüft
- [ ] **B3** `minSdk`/`targetSdk` prüfen, 16-KB-Anforderungen gegen das Setup abgleichen

## C — Abwärtskompatibilität

- [ ] **C1** Aktuelle Mindestanforderungen ermitteln
- [ ] **C2** Lite-Profil: was entfällt, ohne dass eine Kernfunktion kaputtgeht
- [ ] **C3** Buildvarianten (`productFlavors`) statt eines einzigen schweren APK

## D — NPU

- [ ] **D1** Bibliothek wählen: LiteRT/AIML-Interface, ExecuTorch oder MediaPipe
- [ ] **D2** Fähigkeitserkennung zur Laufzeit statt zur Buildzeit
- [ ] **D3** Fallback-Kette: NPU → GPU → CPU, ohne dass Aufrufe brechen
- [ ] **D4** **Ehrliche Grenze:** Ausführung auf echter Hardware unbelegt

## E — Gates abarbeiten (24 offen)

- [ ] **E1** 117 Freigabeübersicht (öffnet 132–135)
- [ ] **E2** 095 Git-Zugang (öffnet 096–104 und 128)
- [ ] **E3** 123 Skill-Quelle (öffnet 124, 130, 131)

## F — Sichtbarkeit

- [ ] **F1** Screenshots — **blockiert: kein Gerät und kein Emulator in dieser Umgebung**
- [ ] **F2** README pflegen; Badge-Zahlen aus dem Frontmatter statt von Hand

## Zwei Grenzen, die nicht ausgehandelt werden

**Markenzeichen.** Kein Anthropic-Wesen wird nachgezeichnet. Grund: Die App zeigt auf
demselben Bildschirm einen Unabhängigkeitshinweis („nicht mit Anthropic verbunden"). Ein
geklonter Claude-Bot würde diese Aussage zum selben Bildschirm widersprechen. Der
zweite Bot ist deshalb ein generischer Terminal-Bot — die Geste (Hand in Hand) und
nicht das Aussehen des Originals trägt die Aussage.

**Fremde Konten.** Ich melde mich nirgends als Claude Code an, lege keine Konten an
und gebe kein Geld aus. Dort baue ich den Code für alle Wege, die Zugangsdaten trägt
der Nutzer selbst ein. Betrifft aktuell nur Aufgabe 095 (Git-Zugang).