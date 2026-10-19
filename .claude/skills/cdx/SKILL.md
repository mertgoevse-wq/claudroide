---
name: cdx
description: ClauDroide Developer eXecutive on Samsung Galaxy A56. Run builds, tests, status checks, and quality gates using the optimized local A56 memory profile (-Xmx1536m, in-process, single worker).
argument-hint: "[status|build debug|build release|test fast|test all|check|adb|install|clean]"
user-invocable: true
---

# ClauDroide Developer Workflow (cdx)

Dieser Skill steuert den reproduzierbaren Entwicklungs-, Build- und Test-Workflow für **ClauDroide** auf dem Samsung Galaxy A56 (Debian PRoot).

## Befehle und Verwendung

- `/cdx status` — Prüft Git-Zustand, Toolchain (Java 17, Android SDK, aapt2 aarch64), RAM/Disk-Ressourcen und APK-Stände.
- `/cdx build debug` — Baut die Debug-APK mit den gerätesicheren Parametern und kopiert sie nach `/sdcard/Download/ClauDroide-debug.apk` inkl. `.sha256`-Prüfsumme.
- `/cdx build release` — Baut die Release-APK mit Release-Keystore (`keystore.properties`) und kopiert sie nach `/sdcard/Download/ClauDroide-release.apk`.
- `/cdx test fast` — Führt schnelle Kern-Unit-Tests aus (~2-3 Min), um die Toolchain abzusichern, ohne 13 Minuten zu blockieren.
- `/cdx test all` — Führt alle 2.612 Unit-Tests in 139 Suiten aus und erstellt eine JUnit-Zusammenfassung (`tools/junit_summary.py`).
- `/cdx check` — Führt alle Sicherheits- und Qualitäts-Gates aus (`sync_frontmatter.py --check`, `secret_gate.py .`, `git diff --check`).
- `/cdx adb` — Zeigt den ADB-Verbindungsstatus und die Anleitung für Wireless Debugging.
- `/cdx install` — Installiert die Debug-APK via ADB auf dem Gerät (falls ADB autorisiert ist).
- `/cdx clean` — Beendet hängende Gradle-Daemons und bereinigt temporäre Reste.

## Ausführung
Rufe das Skript `tools/cdx <argument>` im Projektverzeichnis `/home/mert/claudroide-next` auf.
