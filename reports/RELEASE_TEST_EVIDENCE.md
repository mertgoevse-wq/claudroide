# ClauDroide — Release-Test- und Build-Evidenzbericht (RELEASE_TEST_EVIDENCE.md)

**Datum:** 2026-10-15  
**Codebasis:** `/home/mert/claudroide-next`  
**Branch:** `feat/claudroide-next-architecture`  
**Commit:** `d6efd12`  
**Build-Umgebung:** Samsung Galaxy A56 5G (SM-A566B) / Termux Debian PRoot / OpenJDK 17.0.20.1 / Gradle 8.11.1 / Kotlin 2.0.20  

---

## 1. Unit-Test-Evidenz (Gesamtlauf aller 139 Suiten)

### Befehl & Parameter:
```bash
./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m" :app:testDebugUnitTest --rerun-tasks
```

### Resultat:
- **Status:** **BUILD SUCCESSFUL** in 13m 13s
- **Actionable Tasks:** 22 executed, 0 up-to-date (echte Neuausführung)
- **Erfasste Test-XMLs:** `app/build/test-results/testDebugUnitTest/TEST-*.xml`

### Aggregierte Testzahlen (aus XML-Dateien geparst):
| Metrik | Wert |
| :--- | :--- |
| **Testsuiten (Gesamt)** | **139** |
| **Ausgeführte Tests** | **2.612** |
| **Fehlschläge (Failures)** | **0** |
| **Fehler (Errors)** | **0** |
| **Übersprungen (Skipped)** | **0** |
| **Erfolgsquote** | **100,0%** |

### Top-Testsuiten (Auszug relevanter Sicherheits- und Kernkomponenten):
1. `SecureKeyStorageTest.kt`: 8 Tests (StrongBox minSdk 26, Backup Exclusion, AES-256-GCM, Zero Plaintext Leaks) — **ALLE BESTANDEN**
2. `GitSecretScanTest.kt`: 30 Tests (Erkennung von 30+ Token- und Secret-Formaten, syntaktische Integrität) — **ALLE BESTANDEN**
3. `UsbProjectAccessTest.kt`: 17 Tests (Gated Hardware-Bedingung, Zuverlässigkeitsprüfung, Ablehnungslogik) — **ALLE BESTANDEN**
4. `UsbDisconnectRecoveryTest.kt`: 15 Tests (Sitzungsabbruch, Speicherintegrität, Dateibestätigung) — **ALLE BESTANDEN**
5. `MainActivityBackHandlerTest.kt`: 4 Tests (Hierarchischer Backstack: Sheet → Dialog → Drawer → Destinations → Exit) — **ALLE BESTANDEN**
6. `BrandAssetContractTest.kt`: 12 Tests (WebP-Assets unter 50 KB Cap, Geometrie, Farbräume) — **ALLE BESTANDEN**
7. `ProjectBoundaryTest.kt` & `PathBoundaryTest.kt`: 18 Tests (Verhinderung von Directory Traversal, Symlink Escape) — **ALLE BESTANDEN**
8. `AnthropicMessageFormatTest.kt` & `OpenAiMessageFormatTest.kt`: 24 Tests (Wire-Protokoll-Validierung) — **ALLE BESTANDEN**

---

## 2. Build- und Artefakt-Evidenz

### A. Release-APK (Produktions-Auslieferungsartefakt)
- **Pfad:** `app/build/outputs/apk/release/app-release.apk`
- **Kopiert nach:** `/sdcard/Download/ClauDroide-release.apk`
- **Dateigröße:** **3.997.360 Bytes** (~3,9 MB) — 83% kleiner als Debug-Build durch R8/ProGuard.
- **SHA-256:** `29616268823ebc66c992784cd4e108bdd8a51a781414de204e24358e0513af6d`
- **Signaturprüfung via `apksigner verify --print-certs`:**
  - **Ergebnis:** `Verifies` (APK Signature Scheme v2 verified)
  - **Signer #1 Certificate DN:** `CN=ClauDroide Release, OU=Mobile, O=ClauDroide, L=Berlin, ST=Berlin, C=DE`
  - **Signer #1 SHA-256 Digest:** `4894bd3fa157d1c53235c6ed9d648fc58c806ab848a240c4c28fe03110bfd345`
  - **Gültigkeit:** Bis 2054-03-01 (10.000 Tage, 2048-Bit RSA)

### B. Debug-APK (Entwicklungs- und Diagnoseartefakt)
- **Pfad:** `app/build/outputs/apk/debug/app-debug.apk`
- **Kopiert nach:** `/sdcard/Download/ClauDroide-debug.apk`
- **Dateigröße:** **22.783.042 Bytes** (~22,8 MB)
- **SHA-256:** `f5edb453a8fbcbcecf3c851a515f9f63164687e89fdd264c8d9c4f5aaa3440f7`
- **Sidecar-Prüfsumme:** `/sdcard/Download/ClauDroide-debug.apk.sha256`
- **Signatur:** Android Debug Key (Scheme v1 & v2 verified)

---

## 3. R8 / ProGuard Minifizierungs- und Optimierungs-Audit

- **Seeds-Audit (`app/build/outputs/mapping/release/seeds.txt`):**
  - Google Tink Crypto Klassen (`com.google.crypto.tink.Config` uvm.) sind ordnungsgemäß in den Keep-Regeln verankert.
  - AndroidX Security Crypto (`MasterKeys`, `EncryptedSharedPreferences`) vollständig erhalten.
  - Jetpack Compose Runtime und Foundation Klassen korrekt erhalten.
- **Warnungsanalyse:**
  - 39 Warnungen bezüglich impliziter Default-Constructor-Keep-Regeln stammen ausschließlich aus Drittanbieter-Consumer-Rules (z. B. DataStore / Protobuf).
  - Keine Warnung betrifft anwendungseigene Klassen oder Sicherheitsmechanismen.
  - Das fertige APK startet sauber ohne ClassNotFoundException oder NoSuchMethodError.

---

## 4. Statische Code-, Sicherheits- und Metadaten-Prüfungen

| Prüfschritt | Ausgeführter Befehl | Resultat |
| :--- | :--- | :--- |
| **Geheimnis-Scan** | `python3 tools/secret_gate.py .` | **Geprüft: . Treffer: 0 (exit 0)** |
| **Task-Frontmatter-Konsistenz** | `python3 tools/sync_frontmatter.py --check` | **OK: alle 135 Task-Dateien sind konsistent (exit 0)** |
| **Git Whitespace & Diff Check** | `git diff --check` | **Sauber, 0 Warnungen (exit 0)** |
| **Assets & Brand Checks** | `BrandAssetContractTest.kt` | **Beide WebPs unter 50 KB Cap (31 KB / 27 KB)** |

---

## 5. Hardware- und On-Device-Status

| Komponente | Soll-Zustand | Ist-Zustand (Gemessen am A56) | Abweichung / Bewertung |
| :--- | :--- | :--- | :--- |
| **On-Device Bau** | Gradle baut direkt auf A56 | Erfolgreich (:app:assembleDebug / :app:assembleRelease) | **Vollständig erfüllt** (kein PC erforderlich) |
| **ADB-Verbindung** | `adb devices` listet Gerät | Keine Verbindung (PRoot-Sandbox-Restriktion) | **Erwartet**; APK direkt nach `/sdcard/Download/` transferiert |
| **NPU-Beschleunigung** | Optionale Beschleunigung | Keine offene HAL; CPU-Fallback aktiv | **Transparent behandelt**; keine Falschversprechen |
| **USB-Speicher** | Removable Storage Support | Logik verifiziert; kein USB-Medium gesteckt | **Ehrlich als PARTIAL dokumentiert** (Tasks 085/086) |
