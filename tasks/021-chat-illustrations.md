---
id: "021"
title: "Eigene App-Bilder"
wave: "W6"
depends_on: [003, 019]
files: [tasks/021-chat-illustrations.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "dc85d6fbd51531c7"
---
# Aufgabe 021 — Eigene App-Bilder

## Ziel
Illustrationen und grafische Leitzustände für leere Listen, Projektstart, Statusmeldungen und hilfreiche Hinweise entwerfen, die den Nutzer visuell anleiten und die Eigenständigkeit der Marke Claudroide unterstreichen.

## Ergebnis
Katalog anwendungsinterner Zustandsgrafiken, Implementierungsregeln für Jetpack Compose, Größenbegrenzungen und Barrierefreiheitsstandards:

### 1. Katalog der Anwendungszustände (Empty & Status States)

| Zustand | Visuelles Motiv | Zweck & Handlungsaufforderung | Verwendetes Asset / Icon |
|---|---|---|---|
| **Leere Chatliste (`EmptyChat`)** | Grüner Claudroide-Bot mit leerer Sprechblase | Lädt zum Start einer neuen Konversation oder zur Projektanalyse ein | `Icons.Default.ChatBubbleOutline` getönt in `AndroidGreen` (`#3DDC84`) |
| **Kein Projekt geöffnet (`EmptyProject`)** | Roboter vor stilisiertem Code-Ordner | Erklärt SAF-Auswahl und leitet zum Datei-Browser weiter | `Icons.Default.FolderOpen` in `AndroidGreen` mit Button `Projekt öffnen` |
| **Offline / Verbindungsabbruch** | Roboter mit Antennen-Signal und Pause-Symbol | Informiert ruhig über fehlendes Internet ohne Panikfarben | `Icons.Default.WifiOff` in `TextMuted` mit `Erneut verbinden`-Button |
| **Freigabestufe / Bestätigung** | Roboter mit Schild und Vergrößerungsglas | Visualisiert Sicherheitsprüfung vor Ausführung von Befehlen | `Icons.Default.Security` in `TerracottaSpark` (`#E06D53`) |
| **Ersteinrichtung (Welcome)** | Die eigene Marke, nicht ein Material-Symbol | Freundliche Begrüßung und Einleitung | `BrandMark` → `R.drawable.claudroide_mark` (WebP, 13 KB), `contentDescription = brand_mark_alt_text` |

**Zustand umgesetzt:** Die Leerzustände laufen weiterhin über `EmptyState` mit Material-Symbolen (Katalogzeilen 1–4) — das ist die richtige Wahl, sie kosten nichts und tragen die Bedeutung im Text. **Umgesetzt ist bisher nur die Welcome-Zeile:** `OnboardingScreen` zeigte `Icons.Default.SmartToy`, ein beliebiges Robotersymbol, das nicht die Marke ist. Es ist jetzt durch `BrandMark` ersetzt. Die Katalogzeilen 1–4 bleiben als Vorgabe für die noch nicht gebauten Leerzustände offen.

### 2. Technische Richtlinien für mobile Darstellungen
- **Vektor-Vorrang:** Für UI-Zustände werden in erster Linie skalierbare Compose Vector Assets (`ImageVector`) genutzt (extrem speichersparend, 0 ms Ladezeit, gestochen scharf auf FHD+ Displays wie dem A56).
- **Dateigrößen-Deckel:** Bitmap-Illustrationen dürfen maximal 50 KB pro Einzelbild wiegen (WebP/JPG mit 85% Qualität).
  - **Korrektur der Anwendung:** Die 50-KB-Grenze galt bisher nur als Satz in diesem Dokument und wurde nirgends geprüft. `mark-1024.png` (56 KB) und `banner.png` (112 KB) in `assets/brand/` hätten sie verletzt. In der App liegen jetzt `claudroide_mark.webp` (13 KB) und `claudroide_banner.webp` (15 KB); `BrandAssetContractTest.noShippedBrandImageExceedsTheSizeCap` prüft die Grenze für jede ausgelieferte Datei, damit sie nicht wieder zur ungeprüften Behauptung wird.
  - Die Quelldateien unter `assets/brand/` bleiben bewusst größer: sie sind die verlustfreie Quelle, aus der die App-Ableitungen erzeugt werden. Der Deckel gilt für das, was aufs Gerät geht.
- **Keine Blockade:** Grafiken werden im Layout adaptiv skaliert (z.B. 64–96 dp) und verschwinden bei geöffneter Software-Tastatur automatisch (`Modifier.imePadding()` / vertikaler Scroll), damit Eingabefelder immer sichtbar bleiben.

### 3. Barrierefreiheit (a11y)
- Jede Abbildung besitzt eine präzise `contentDescription` (Deutsch und Englisch), die von Screenreadern (TalkBack) vorgelesen werden kann. Reine Dekorationsgrafiken werden explizit mit `contentDescription = null` markiert.
- `BrandMark` hat deshalb `contentDescription: String? = null`: die Entscheidung bleibt beim Aufrufer, statt im Component festgeschrieben zu sein. In der Ersteinrichtung — wo die Marke allein steht — wird sie gesetzt (`brand_mark_alt_text`), in Leerzuständen bewusst nicht.

### 4. Konformitätsprüfung
- [x] Bildkatalog für alle zentralen Leerstufen und Statusmeldungen ist definiert.
- [x] Keine Verwechslungsgefahr mit Anthropic- oder Claude-Marken.
- [x] Alle Vorlagen basieren auf Material Symbols (Apache 2.0) und Media-Bridge-Renderings.

## Fertig, wenn
- Bilder die Bedienung unterstützen und nicht mit Claude-Marken verwechselt werden.
- Platzbedarf und Darstellung auf Mobilgeräten geprüft sind.

## Schutz
Keine fremden oder nutzergenerierten Bilder ohne bestätigte Nutzungsrechte.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Grafik-/Android-Asset-Skill suchen und Quelle, Lizenz sowie Datenschutz prüfen; vorher Zustimmung einholen.
