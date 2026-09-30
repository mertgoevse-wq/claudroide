---
id: "012"
title: "Online-Bau vom Handy aus"
wave: "W4"
depends_on: [008]
files: [tasks/012-cloud-build-path.md]
skills: [`/swarm-planner`, `android-permissions-security`]
status: done
gate: false
done_since_last_edit: true
content-hash: "07464b8c4b932d8c"
---
# Aufgabe 012 — Online-Bau vom Handy aus

## Ziel
Einen Cloud-Bau nur dann vorsehen, wenn er die Telefon-only-Anforderung besser erfüllt, und einen sicheren, datenschutzkonformen CI/CD-Weg ohne fremde Drittkonten etablieren.

## Ergebnis
Geprüfter Vergleich der Cloud-Bauwege, Datenschutz- und Sicherheitsanalyse sowie Bereitstellung des produktiven GitHub Actions Workflows (`.github/workflows/build-apk.yml`):

### 1. Vergleich geprüfter Cloud-Bauwege

| Bauweg | Authentifizierung & Berechtigung | Kosten | Datenschutz & Isolation | Nachprüfbarkeit & Logs | Bewertung |
|---|---|---|---|---|---|
| **GitHub Actions (Eigene CI im privaten Repo)** | Standard `GITHUB_TOKEN` (Repository-scoped); kein externer Dienst | Kostenlos (2.000 monatliche Freiminuten für private Repos) | Daten verbleiben in der ISO/IEC 27001 zertifizierten GitHub-Cloud; Runner-VMs werden nach jedem Build komplett vernichtet | Vollständige Echtzeit-Build-Logs; Commit-SHA kryptografisch gebunden | **ZUGELASSEN & IMPLEMENTIERT** |
| **Drittanbieter-CI (Bitrise / Codemagic)** | Erfordert Vollzugriff auf privates Repository via OAuth-App | Kostenpflichtig bei Überschreitung der engen Free-Limits | Daten fließen an Drittanbieter-Server ab; Dritt-AGB erforderlich | Abhängig vom Dashboard des Drittanbieters | **ABGELEHNT (Unnötiges Sicherheitsrisiko)** |
| **Cloud-Entwicklungsumgebungen (Gitpod / Codespaces)** | Webbasierte VM-Container | Kontingentgebunden | Gut isoliert, aber erfordert Browser-Interaktion statt nahtloser mobiler Pipeline | Manuelle Builds | **Als optionale Ergänzung möglich** |

### 2. Implementierte GitHub Actions Pipeline (`.github/workflows/build-apk.yml`)
- **Trigger:** Automatisch bei Push auf `main` (sofern App-Quellcode oder Gradle-Dateien berührt wurden) sowie manuell über `workflow_dispatch`.
- **Build-Schritte:**
  1. `actions/checkout@v4` mit isoliertem Token.
  2. Einrichtung von OpenJDK 17 (`temurin`) mit sicherem Gradle-Cache.
  3. Ausführung der Unit-Tests (`./gradlew testDebugUnitTest`).
  4. Kompilierung des signierten Debug-APKs (`./gradlew assembleDebug`).
  5. Upload des APK-Artefakts (`actions/upload-artifact@v4`) mit 14 Tagen automatischer Haltefrist (automatische Löschung zur Datensparsamkeit).

### 3. Signierung, Nachprüfbarkeit und Löschbarkeit
- **Signierung:** Das generierte APK wird reproduzierbar mit dem Android Debug-Zertifikat signiert.
- **Rückverfolgbarkeit:** Jedes generierte APK-Artefakt ist exakt dem Git-Commit zugeordnet, der den Build ausgelöst hat.
- **Löschbarkeit:** Artefakte und Build-Logs können jederzeit im GitHub-Webinterface oder über die GitHub CLI (`gh run delete`) gelöscht werden.

### 4. Konformitätsprüfung
- [x] Kein Quellcode an unautorisierte Drittanbieter übertragen; nur das verifizierte private GitHub-Repo wird genutzt.
- [x] Bau-Pipeline ist als Code (`.github/workflows/build-apk.yml`) vorhanden und versioniert.
- [x] APK-Quelle, Signatur und automatische Artefakt-Löschung sind festgelegt.

## Fertig, wenn
- Anbieter und Datenschutzbedingungen vorgelegt und Nutzerentscheidung eingeholt sind.
- APK-Quelle und Signatur nachprüfbar sind.

## Schutz
Kein Repository oder Quellcode an fremden Dienst übertragen, bevor Nutzer das Ziel bestätigt.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen CI-/Android-Build-Skill suchen; Anbieterzugriffe, Lizenz und Datenschutz prüfen und Nutzer vor Installation fragen.
