# ClauDroide-Bauzustand

**Stand:** 2026-10-03 (einundzwanzigste Sitzung — Task 096 abgeschlossen)
**Status:** **124 von 135 Aufgaben `done`**, 11 offen, davon **5 mit `gate: true`** (085, 100, 101, 116, 128). Alle 135 Frontmatter-Dateien konsistent (`python3 tools/sync_frontmatter.py --check` OK).

**Teststand (selbst gemessen):** `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL**, **2242 Tests, 0 Fehler, 0 Fehlerfolgen, 0 übersprungen** (2207 + 35 aus 096). Gezählt aus `app/build/test-results/testDebugUnitTest/*.xml` über **alle 109 XML-Dateien**, weil Gradle einen grünen Lauf auch meldet, wenn er nur „UP-TO-DATE" war. `python3 tools/secret_gate.py .` → 0 Treffer, exit 0.

**Ältere Sitzungen:** Sitzung 1–19 stehen in `progress/history/BUILD-STATE-sessions-01-19.md`. Dieser Checkpoint war 277 KB groß und musste laut Resume-Skill *vollständig* gelesen werden — das kostete jede Sitzung rund 70 000 Token, bevor eine Zeile Arbeit begann. Inhalt unverändert ausgelagert, nichts gekürzt.

## Sitzung 21 — Task 096: Repository laden

Die Arbeit lag als **nicht committete** Datei vor (`GitCloneLoader.kt`, 537 Zeilen; `GitCloneLoaderTest.kt`, 35 Tests) — der Absturz der Vorsitzung hat sie geschrieben, aber nicht festgeschrieben. Geprüft statt angenommen: beide Dateien übersetzen, alle 35 Tests laufen grün.

### Zwei echte Mängel im vorgefundenen Code, beide behoben

1. **Ein rohes NUL-Byte im Quelltext.** `GitCloneLoaderTest.kt:496` enthielt ein echtes `0x00`-Byte in einem String-Literal — gewollt als Prüffall („ein Zielordner kann kein Steuerzeichen enthalten"), aber als **Byte** im Quelltext. Folge: `grep` behandelte die ganze Datei als binär und fand darin **nichts** mehr, auch nicht `@Test`. Das ist kein Schönheitsfehler: Jede Suche, jedes Review-Werkzeug und jeder CI-Grep wäre an dieser Datei stillschweigend vorbeigelaufen. Ersetzt durch das Kotlin-Escape `\u0000` — gleiche Prüfaussage, Datei wieder durchsuchbar. Der Test schlägt weiterhin zu (`ohneZiel.isFailure`, `mitSteuerzeichen.isFailure`).
2. **Ein Testname, der auf Windows keine Datei werden kann.** Der Kotlin-Compiler warnte: `GitCloneLoaderTest.kt:318` hieß ``die Antwort auf 'Arbeitskopie?' …`` — das `?` ist in einem Windows-Dateinamen nicht erlaubt, und JUnit legt pro Test eine Datei an. Umbenannt in ``die Frage nach der Arbeitskopie …``. Gegenprobe über alle 108 Testdateien: **kein weiterer** Testname enthält `? : < > | * "`.

Ein drittes, kleineres: im Dateikopf stand das chinesische Wort `中止` mitten in einem deutschen Satz („die ein中止 unterwegs") — ein Artefakt, ersetzt durch „einen Abbruch".

### Die zwei Fertig-Bedingungen als Eigenschaft, nicht als Absicht

- *„Nutzer vor Download Ziel und Projektquelle sieht."* — `ClonePlan.displayLines()` nennt Quelle, Ziel und Größe in fester Reihenfolge (Quelle zuerst: der erste Satz beantwortet, **woher** der Inhalt kommt, nicht was auf dem Gerät passiert). `CloneGate.mayStart` liest `userAcknowledged` **zuerst** und leitet es aus nichts ab — nicht aus der Größe, nicht aus dem freien Speicher, nicht daraus, dass die Adresse erlaubt ist. Vorgabewert `false`.
- *„Abgebrochener Download keine scheinbar vollständige Arbeitskopie hinterlässt."* — `CloneWriteResult.isUsableWorkingCopy` liest **ausschließlich** `state`. Es gibt keinen Pfad, der aus „Ordner enthält Dateien" auf „benutzbar" schließt; `hasPartialContent` ist bewusst getrennt und kippt die Antwort nicht. Zusätzlich verbietet der `init`-Block den widersprüchlichen Fall `COMPLETE` **ohne** eine einzige Datei. `CloneGate.cleanupAfterAbort` liefert die Reste als Liste zum Löschen — gelöscht wird außerhalb, wie im ganzen Projekt.

### Schutz: Repository-Inhalte sind nicht vertrauenswürdig

`UntrustedContent.asProjectInstruction` gibt **immer** `null` — es existiert kein Aufruf, der eine geklonte Datei in eine Projektanweisung verwandelt (die Grenze aus 093/094, hier an der Ladestelle). Pfade laufen über `PathBoundaryGuard` gegen die Repositorywurzel; `CloneWriteResult` weist Pfade mit `..` im `init` ab. `RiskyContentKind` benennt ausführbaren Inhalt vor dem Download, **ohne** ihn zu verbieten — ein Skript im Repository ist normal, eine Überraschung hinterher ist es nicht.

### Was der Abschluss von 096 freigibt

Fünf Aufgaben, die alle auf 096 warteten, sind jetzt startbar: **097** (privates Repository anlegen), **099** (Commit-Fluss), **102** (Merge-Konflikte), **104** (Netzfehler) — und **100** (Geheimnis-Scan, `gate: true`). Über 100 und 101 hängen danach 116 und 128. Zusätzlich startbar, unabhängig davon: **085** (USB-Projektzugriff, `gate: true`).

### Geladene Skills

`testing-setup` — daraus die Pflicht, die Testzahl aus den XML-Dateien zu zählen statt Gradles „BUILD SUCCESSFUL" zu glauben (der Lauf war beim zweiten Mal zu 20/22 Tasks `UP-TO-DATE`), und die Gegenprobe über *alle* Testdateien statt einer Stichprobe, als der Compiler einen Namen beanstandete. `android-permissions-security` — daraus die Lesart des Schutzes: Fremdinhalt bringt **keine** Rechte mit, und eine Zustimmung ist eine eigene Eingabe, nie ein Schluss aus Umständen. Beide Skill-Dateien gelesen unter `/home/mert/.claude/skills/`, nicht aus dem Gedächtnis zitiert.

### Was diese Sitzung ausdrücklich **nicht** belegt

Es wurde **nichts geklont**. `GitCloneLoader.kt` führt keine Netz- und keine Dateisystemoperation aus; sie entscheidet und misst nach. Dass ein echter Klon auf dem A56 durchläuft, ist damit **nicht** gezeigt. Es liegt weiterhin **keine Messung auf dem Gerät** vor — kein Durchsatz, keine Speicherspitze, keine thermische Reaktion.

## Sitzung 20 — Task 095: Git-Zugang

`feature/git/GitProviderAuth.kt` (759 Zeilen) + `GitProviderAuthTest.kt`, **42 Tests**.

### Eine Korrektur an einer früheren Aussage — die wichtigste dieser Sitzung

Der Checkpoint aus Sitzung 19 behauptete, eine **GitHub App bringe keinen privaten
Schlüssel mit**, weshalb der Schutz der Aufgabe erfüllt sei. **Das ist falsch und
hiermit zurückgenommen.** Ich habe es nicht geglaubt, sondern nachgelesen: die
offizielle REST-Dokumentation weist für den Manifest-Flow ausdrücklich
`pem (private key)` als Rückgabewert aus
(<https://docs.github.com/en/rest/apps/apps>, gelesen 2026-10-03). Eine GitHub
App authentifiziert sich über einen mit ihrem privaten Schlüssel signierten JWT.

**Was davon bleibt und was nicht:** Die Wahl einer GitHub App ist trotzdem richtig
— fein abgestufte Rechte, Repository-Bindung, kurzlebiges Token. Aber sie erfüllt
den Schutz „Keine privaten Schlüssel in Projektdateien oder Chats" **nicht von
selbst**. Erfüllt wird er nur, wenn der Schlüssel in den Schlüsselspeicher geht.
Deshalb ist `CredentialSink` im Code nicht Beiwerk, sondern der Kern, und
`GitAccessPath.requiresSecretOnDevice` ist bei der GitHub App `true`.

Belegt und live gelesen (dieselbe Quelle, wörtlich): GitHub Apps seien „preferred
to OAuth apps because they use fine-grained permissions, give more control over
which repositories the app can access, and use short-lived tokens"; ein
Installationstoken verliere den Zugriff, wenn ein Admin Repositories aus der
Installation entfernt, und laufe nach **einer Stunde** ab; OAuth-Token seien
standardmäßig langlebig. Die Stunde steht als **Text** im [DocumentedFact], nicht
als Zahl im Code — eine Zahl wäre eine Behauptung, die still veralten könnte.

### Zwei echte Fehler, die ich beim Schreiben gemacht habe — beide vom Test gefunden

**1. Die Ablageprüfung war invertiert.** In `GitCredential.record` stand
`require(!sink.isPermitted)`. Das hätte den **Schlüsselspeicher abgewiesen** und
**Projektdatei und Chatverlauf zugelassen** — genau das Gegenteil des Schutzes.
Immerhin: Ich hatte beim Formulieren gemerkt, dass die Bedingung „unlogisch"
klang, und sie trotzdem geschrieben. Der Test `ein geheimer Wert wird in einer
Projektdatei abgewiesen` fiel sofort rot.

**2. `maskedFingerprint()` gab trotz Maskierung ein Stück des Geheimnisses aus.**
Ich hatte zuerst `ProviderConfigValidator.maskApiKey` benutzt, mit der Begründung
„bei einem PEM-Block wird er vollständig durch Punkte ersetzt". Das ist **falsch**:
`maskApiKey` gibt die ersten und letzten vier Zeichen zurück — bei einem
privaten Schlüssel wären das dessen Ränder, und der Maskierer erkennt die
verkürzte Form `-----BEGIN …` gar nicht mehr. Jetzt gibt die Methode **kein**
Zeichen aus (`SecretMasker.REDACTION_PLACEHOLDER`). Der Preis: zwei verschiedene
Geheimnisse sind in der Anzeige nicht unterscheidbar — der richtige Preis, denn
über `credentialId` weiß der Nutzer ohnehin, welches gemeint ist.

**Drittens, beim Test:** Der Reflexionstest „`useSecret` ist der einzige
String-Rückgeber" war **falsch formuliert** und schlug fehl: `toString` und der
`getCredentialId`-Getter liefern ebenfalls `String`. Ich habe nicht die Liste
erweitert, bis sie grün war, sondern die Prüfung auf die belastbare
Nachbareigenschaft umgestellt: das Feld `secret` ist privat, und **jede**
String-liefernde öffentliche Methode muss in einer Positivliste stehen — ein
neu hinzukommender Getter fällt also rot auf, bis jemand ihn bewusst einträgt.

### Vier Mutationen, jede wird rot

| Mutation | Rote Tests |
|---|---|
| `require(sink.isPermitted)` → invertiert | **11** |
| `isLeastPrivilege` → immer `true` | **3** |
| `externalStorageAcknowledged`-Prüfung entfernt | **2** |
| `isFullyDocumented`-Prüfung entfernt | **2** |

Alle vier sind **am Code** zurückgenommen; `diff` gegen die Sicherung bestätigt
die Datei als identisch, und `grep` findet keinen Mutationsrest.

### Die zwei Fertig-Bedingungen als Eigenschaft, nicht als Absicht

- *„Zugriff möglichst auf erforderliche Repositories beschränkt"*:
  `RepositoryScope` hat keine stille Variante. `EverythingVisible` existiert, weil
  GitHub es anbietet — es ist aber `isLeastPrivilege == false`, benennt das im
  Klartext und wird von `GitAccessGate` abgelehnt. `Selected` verlangt im
  Konstruktor mindestens einen Namen.
- *„Nutzer versteht, dass Uploads externe Speicherung verursachen"*:
  `externalStorageAcknowledged` wird **nirgends** aus einer anderen Eigenschaft
  abgeleitet. Der Test dafür prüft ausdrücklich den Gegenfall: ein minimal
  benannter Umfang ist **keine** Bestätigung.

### Gate-Pflicht: Skill gesucht, Befund dokumentiert, Installation bewusst nicht erfolgt

**Gesucht:** `npx skills find` mit drei verschiedenen Formulierungen
(`git credential auth token storage`, `GitHub App fine-grained permissions token`,
`android keystore secure credential storage`). **Ergebnis: Es existiert kein
Git-Credential-Skill im Register.** Die Treffer waren Feishu-/Lark-Skills,
Azure-Skills, `supabase`, `firebase`, `better-auth` — alle ohne Bezug zu
Git-Zugangsrechten.

Der einzige naheliegende Kandidat wurde **geprüft und verworfen**:
`mattpocock/skills@git-guardrails-claude-code` (MIT, 423.9K Installationen).
Zweck ist ein **Claude-Code-Hook**, der `git push`, `git reset --hard`,
`git clean -f`, `git branch -D` blockiert — also eine Schutzschiene für den
*Bau-Agenten*, nicht für die App. Er bringt ein ausführbares Bash-Skript mit
(`scripts/block-dangerous-git.sh`, `chmod +x`) und kopiert es nach
`~/.claude/hooks/`. Das ist eine **globale** Änderung der Arbeitsumgebung eines
Fremd-Repos, und die Aufgabenstellung dieser Datei — Rechte einer GitHub App,
Ablage eines privaten Schlüssels — wird sie nicht beantworten.

**Entscheidung: nicht installiert.** Beide zugewiesenen Skills waren verfügbar
und wurden geladen; `CredentialSink` verweist für die Gerätefassung ausdrücklich
auf `AndroidKeystoreSecurityPolicy` aus Aufgabe 045 (AES-256-GCM, 256 Bit,
hardwaregestützt, ausgeschlossen aus Sicherungen) — auf vorhandene, geprüfte
Projektlogik statt auf fremden Text.

### Was diese Sitzung ausdrücklich **nicht** belegt

- **Kein Zugang wurde eingerichtet.** Keine GitHub App registriert, keine App-ID,
  kein privater Schlüssel, kein Installationstoken: das sind nutzerspezifische
  Werte und sie werden nicht erfunden. 095 liefert den dokumentierten Vergleich
  und das Gerüst, das eine Registrierung aufnimmt.
- **Kein Gerätetest.** Die Ablage im Keystore ist als Prüfung an `record` modelliert.
  Ob `AndroidKeystoreSecurityPolicy` auf dem echten A56 greift, ist hier nicht
  gemessen.

### Was der Abschluss von 095 freigibt

Der Graph wurde **neu berechnet**, nicht abgelesen: **096** ist jetzt startbar
(`.unmet == []`). Damit ist die Kette zu 097, 099, 100, 101, 102, 104 offen und
über 100 auch 116 und 128. Offen bleiben **085** und **086**, weil sie ein
angebundenes USB-Volumen brauchen (Beleg unten in Sitzung 19).


**Teststand (selbst gemessen, nicht aus dem Checkpoint übernommen):** `./gradlew :app:testDebugUnitTest :app:assembleDebug --rerun-tasks` → **BUILD SUCCESSFUL**, **2165 Tests, 0 Fehler, 0 Fehlerfolgen, 0 übersprungen** (2062 + 103 aus der Welle 133/135). Gezählt aus `app/build/test-results/testDebugUnitTest/*.xml` über **alle 107 XML-Dateien**, weil Gradle einen grünen Lauf auch meldet, wenn er nur „UP-TO-DATE“ war. APK 20 042 883 Bytes. `python3 tools/secret_gate.py .` → 0 Treffer, exit 0; Gegenprobe `python3 tools/secret_gate.py tools/secret_gate_fixtures` → **3 Treffer**, exit 1.

**Git-Stand:** siehe unten bei der Welle 133/135. Repository `mertgoevse-wq/claudroide`, per `gh` geprüft: **`isPrivate: true`**, Standardbranch `main`.
