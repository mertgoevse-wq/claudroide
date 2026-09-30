---
id: "018"
title: "Hintergrundaufgaben"
wave: "W5"
depends_on: [010]
files: [tasks/018-background-task-lifecycle.md]
skills: [`android-permissions-security`, `android-profiler`]
status: done
gate: true
done_since_last_edit: true
content-hash: "6d71899085e5f7ac"
---
# Aufgabe 018 — Hintergrundaufgaben

## Ziel
Vom Nutzer gestartete längere Arbeit nachvollziehbar fortsetzen, den Fortschritt transparent visualisieren und jederzeit durch den Nutzer abbrechen können, ohne unbegrenzte oder heimliche Hintergrundprozesse zu erzeugen.

## Ergebnis
Implementiertes Zustands- und Lebenszyklusmodell (`BackgroundTaskManager.kt`, `TaskLifecycleState`) und Tests (`BackgroundTaskManagerTest.kt`):

### 1. Zustandsmodell für Hintergrundaufgaben

```
[IDLE]
  │
  ├──► startTask() ──► [RUNNING] (Sichtbare Notification mit Fortschritt und Stop-Action)
  │                         │
  │                         ├──► pauseTask() ──► [PAUSED]
  │                         │                       │
  │                         │                       └──► resumeTask() ──► [RUNNING]
  │                         │
  │                         ├──► cancelTask() ──► [CANCELLED] (Ressourcenfreigabe, Job storniert)
  │                         │
  │                         ├──► failTask() ──► [FAILED] (Fehlermeldung, Logeintrag)
  │                         │
  │                         └──► completeTask() ──► [COMPLETED] (Erfolgsmeldung, Fortschritt 100%)
```

### 2. Android 14 / 15 Foreground Service Vorgaben
- **Keine heimliche Dauerlast:** Hintergrundprozesse dürfen nur für explizit vom Nutzer angestoßene Aktionen (z.B. Testläufe, Git-Klone, Agenten-Aufgaben) laufen.
- **Sichtbare Benachrichtigung (Ongoing Notification):**
  - Zeigt Task-Titel, Echtzeit-Fortschrittsbalken und Laufzeit.
  - Enthält zwingend eine sichtbare **„Abbrechen“-Aktion**, mit der der Nutzer den Job sofort terminieren kann.
- **Service-Typisierung:** Konform mit Android 14+ (`foregroundServiceType="specialUse"` mit Begründung für Entwicklerwerkzeuge oder `shortService`).
- **Bereinigung bei Abbruch:** Bei Abbruch werden alle assoziierten Coroutinen-Scopes abgebrochen, Kindprozesse beendet und temporäre Dateien entfernt.

### 3. Konformitätsprüfung
- [x] Vollständiges Zustandsmodell (`TaskLifecycleState`) in `BackgroundTaskManager.kt` umgesetzt.
- [x] Laufende Aufgaben sind reaktiv über `StateFlow` beobachtbar und sofort abbrechbar.
- [x] Unit-Test `BackgroundTaskManagerTest.kt` verifiziert Start, Abbruch, Abschluss und Fortschritt.

## Fertig, wenn
- Android-Anforderungen der Zielversion geprüft sind.
- Nutzer über laufende Arbeit informiert ist und sie stoppen kann.

## Schutz
Keine heimliche oder unbegrenzt laufende Hintergrundarbeit.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Hintergrundarbeit-Skill suchen; aktuelle Android-Regeln und Lizenz prüfen, Installation nur nach Zustimmung.
