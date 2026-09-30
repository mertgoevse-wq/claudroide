---
id: "008"
title: "Bauen nur mit dem Telefon"
wave: "W2"
depends_on: [006]
files: [tasks/008-phone-only-build-plan.md]
skills: [`/swarm-planner`, `android-profiler`]
status: done
gate: false
done_since_last_edit: true
content-hash: "058c7a715f91be54"
---
# Aufgabe 008 — Bauen nur mit dem Telefon

## Ziel
Einen realistischen, akkuschonenden und speichersicheren Weg festlegen, mit dem der Nutzer Quellcode, Tests und die installierbare Android-App (APK) vollständig ohne PC direkt vom Galaxy A56 aus bauen und verwalten kann.

## Ergebnis
Gegenüberstellung beider Baupfade, verbindliche Festlegung des primären Weges und Schritt-für-Schritt-Anleitung:

### 1. Vergleich der Bauwege

| Kriterium | Pfad A: Am Telefon gesteuerter CI-Bau (GitHub Actions) | Pfad B: Lokaler On-Device Build (Termux / PRoot) |
|---|---|---|
| **Ausführungsort** | GitHub Cloud Runner (Ubuntu VM) | Direkt auf dem A56 (Exynos 1580 / Termux PRoot) |
| **Zusätzlicher Handyspeicher** | **0 MB** (nur Quellcode und fertiges APK ~25 MB) | **4.5 bis 7.0 GB** (JDK, Android SDK, Build-Tools, Gradle Caches) |
| **Bauzeit (kalt / warm)** | ~2–4 Minuten | ~10–18 Minuten (kalt) / ~4–7 Minuten (warm) |
| **Akkubelastung & Hitze** | **Sehr gering** (nur Git Push und APK-Download) | **Sehr hoch** (Dauerlast auf allen 8 CPU-Kernen, starke Erwärmung) |
| **RAM-Gefahr (OOM)** | Keine (Runner hat 7 GB dediziert) | **Hoch** (Android LMK kann Gradle/Kotlin-Daemon bei Speicherspitzen killen) |
| **Internetbedarf** | Ja (Push & Download) | Nur beim ersten Einrichten der Abhängigkeiten |
| **Kosten** | Kostenlos (2.000 monatliche Freiminuten für private GitHub Repos) | Kostenlos |
| **Datenschutz** | Code im privaten GitHub-Repo des Nutzers | 100% auf dem Gerät |
| **Empfehlung** | **PRIMÄRER STANDARDWEG (Empfohlen)** | **SEKUNDÄRER RÜCKFALLWEG (Optional)** |

### 2. Schritt-für-Schritt-Anleitung für Pfad A (Primärer Weg)

1. **Entwicklung & Test am Telefon:**
   - Quellcode-Dateien in Termux/PRoot bearbeiten.
   - Lokale Prüfungen ausführen (`python3 tools/sync_frontmatter.py --check` und Python/Kotlin-Unit-Tests).
2. **Push an das private Repository:**
   - Git Commit erstellen und an `origin main` pushen (`git push origin main`).
3. **Automatische APK-Erstellung via GitHub Actions:**
   - Der Workflow `.github/workflows/build-apk.yml` baut das Debug-APK automatisiert und signiert es mit dem Android-Debug-Schlüssel.
4. **Download & Installation am A56:**
   - Das fertige APK wird als Release-Artefakt bereitgestellt.
   - Der Nutzer lädt das APK mit einem Tippen im mobilen Browser oder via GitHub CLI herunter und installiert es direkt auf dem Gerät.

### 3. Transparenz über nötige Downloads für Pfad B (Rückfallweg)
Falls der Nutzer später offline bauen möchte, sind folgende Downloads vorab transparent zu bestätigen:
- OpenJDK 17 (`openjdk-17-jdk` via APT): ca. 450 MB
- Android Command-Line Tools (`cmdline-tools`): ca. 150 MB
- Android SDK Platform & Build-Tools (API 35): ca. 1.8 GB
- Gradle Wrapper & Core Libraries: ca. 600 MB
- Gesamtbedarf: ca. 3.0 GB Download, entpackt ca. 5.5 GB Speicher.

### 4. Konformitätsprüfung
- [x] Primärer Weg (GitHub Actions) schont Akku, RAM und Speicher des Galaxy A56.
- [x] Sekundärer Weg ist vollständig durchkalkuliert und mit Download-Größen beziffert.
- [x] Kein Drittkonto erforderlich; das bestehende verifizierte private GitHub-Repo wird genutzt.

## Fertig, wenn
- Ein sicherer erster Weg mit Schritt-für-Schritt-Anleitung gewählt oder offen begründet ist.
- Nötige Downloads vorab mit Größe und Zweck erklärt werden.

## Schutz
Kein Build-Dienst oder neues Konto ohne ausdrückliche Zustimmung aktivieren.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Build-/CI-Skill suchen; Quelle, Berechtigungen, Kosten und Lizenz prüfen und vor Installation fragen.
