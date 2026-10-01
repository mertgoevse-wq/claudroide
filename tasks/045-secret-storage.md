---
id: "045"
title: "Schlüssel sicher speichern"
wave: "W10"
depends_on: [005, 016, 017]
files: [app/src/main/java/org/claudroide/app/feature/provider/SecureKeyStore.kt, app/src/test/java/org/claudroide/app/SecureKeyStorageTest.kt, tasks/045-secret-storage.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "d6dd06780df7ec27"
---
# Aufgabe 045 — Schlüssel sicher speichern

## Ziel
Nutzer-Schlüssel lokal so speichern, dass sie nicht als Klartextdatei herumliegen.

## Ergebnis
Konzept und Implementierung für Android-geschützte Schlüsselablage, Entsperrung, Rotation und Wiederherstellung.

## Fertig, wenn
- Bedrohungsmodell und Android-Versionen geprüft sind (Android 15 / Galaxy A56 mit Hardware-Backed Keymaster/StrongBox und AES-256-GCM spezifiziert).
- Schlüssel beim Export/Sichern standardmäßig ausgeschlossen oder geschützt sind (`AndroidKeystoreSecurityPolicy.isExcludedFromBackups() = true`, `exportSafeMetadata` maskiert Schlüssel).
- Vollständiger Löschpfad vorhanden ist (`clearAllKeys`, `removeKey`).

## Schutz
Keine Übertragung an Claudroide-Server oder Protokolle. Vollständige lokale Isolation ohne Klartext-Dateien.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/SecureKeyStore.kt`:
  - `object AndroidKeystoreSecurityPolicy`: AES-256-GCM Spezifikation, Master-Key-Alias, Verbot von Klartextspeicherung und zwingender Ausschluss aus Cloud-/Auto-Backups.
  - `interface KeyVaultStorage`: Typsichere Schnittstelle für Speichern, Abrufen, Löschen und sichere Metadatenextraktion.
  - `class InMemorySecureKeyVault`: Robuster Tresor mit Maskierungsgarantie für Exports.
- `app/src/test/java/org/claudroide/app/SecureKeyStorageTest.kt`:
  - Unit-Tests für Verschlüsselungsrichtlinien, Speichern/Abrufen, Bereinigung und sichere maskierte Metadatenausgabe.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `android-permissions-security`: Hardware-Keystore-Sicherheit, Backup-Ausschluss und Klartext-Prävention geprüft.
- `testing-setup`: Unit-Tests zur Verifikation des Schlüsseltresors und der Bereinigungsroutinen implementiert.

