---
id: "011"
title: "Bauweg direkt am A56"
wave: "W4"
depends_on: [008]
files: [tasks/011-phone-build-path.md]
skills: [`android-profiler`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "ecd8243230fac6d6"
---
# Aufgabe 011 — Bauweg direkt am A56

## Ziel
Machbarkeit einer lokalen Android-Bauumgebung ohne separate Termux-App untersuchen und technische Systemschranken sowie Ressourcenbedarfe fundiert analysieren.

## Ergebnis
Ausführlicher technischer Machbarkeitsbericht zu Werkzeugen, Sicherheitsgrenzen von Android 15, Speicherbedarf, Wärmeentwicklung und Berechtigungen:

### 1. Technische Hürden für Standalone-Builds in Android

1. **W^X-Sicherheitsrestriktion (Write XOR Execute):**
   - Seit Android 10/11 erzwingt das Betriebssystem `W^X` für App-Datenverzeichnisse. Eine normale Android-App darf keine ausführbaren Binaries (wie `aapt2`, `d8`, `kotlinc` oder `javac`) in beschreibbaren Verzeichnissen (`/data/data/org.claudroide.app/files/`) ausführen (`Permission denied / exec error`).
   - Binaries müssten als native Bibliotheken (`.so`) im `lib/arm64-v8a/`-Ordner der APK verpackt sein. Dies erfordert maßgeschneiderte JNI-Ports aller Gradle- und Kotlin-Compiler-Komponenten.
2. **Speicher- & RAM-Anforderungen:**
   - Gradle Daemon + Kotlin Compiler Daemon (K2) allokieren unter Last 2.5 bis 3.5 GB Heap.
   - Auf einem 8-GB-Gerät mit ca. 2.0 GiB freiem RAM riskiert ein On-Device-Kompiliervorgang ständige OOM-Kills durch den Android Low Memory Killer (LMK).
   - Speicherbelegung: Ein lokales SDK + Build-Tools + Abhängigkeiten benötigt ca. 5.5 GB interner Speicherplatz (auf dem A56 mit ~39 GB frei ein spürbarer Block).
3. **Akku und Thermik:**
   - Ein vollständiger Kalt-Build erzeugt über 10–15 Minuten 100% Last auf allen 8 Kernen des Exynos 1580, was zu starker Hitzeentwicklung und thermischer Drosselung führt.

### 2. Bewertung & Begründetes Urteil

- **Urteil:** Ein nativer APK-Bau vollständig *innerhalb* der normalen Android-App (ohne Termux/PRoot-Hilfsumgebung) ist mit moderner Jetpack-Compose-Toolchain **technisch unpraktikabel** und verstößt gegen Best Practices für Akku- und Speicherschutz.
- **Konsequenz für Claudroide:**
  1. **Standardweg:** Vom Smartphone aus gesteuerter CI/CD-Bau via GitHub Actions (Pfad A, siehe Task 008/012). Die App bleibt schlank, verbraucht minimalen Akku und baut in 2 Minuten saubere APKs.
  2. **Entwickler-Pfad:** Wer offline kompilieren möchte, nutzt die bewährte Termux/PRoot-Kommandozeile mit installiertem JDK/Android SDK. Die Claudroide-App fordert dafür keine unsicheren Sonderrechte an.

### 3. Konformitätsprüfung
- [x] Klare, technisch belegte Begründung für die Grenzen eines Standalone-In-App-Compilers.
- [x] Keine unsicheren Workarounds, kein Aushebeln von Android-Sicherheitsstandards.
- [x] Transparenter Verweis auf den funktionierenden, akkuschonenden Online-Bau.

## Fertig, wenn
- Ein reproduzierbarer Build oder ein klarer Grund für Unmöglichkeit dokumentiert ist.
- Kein Installationspaket ohne Prüfsumme/Herkunft bezogen wird.

## Schutz
Keine versteckten Systemrechte und keine unbestätigten großen Downloads.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-On-device-build-Skill suchen; Installationswege und Lizenz prüfen, Nutzerfreigabe vor Installation einholen.
