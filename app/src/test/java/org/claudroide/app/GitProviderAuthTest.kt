package org.claudroide.app

import org.claudroide.app.feature.git.CredentialSink
import org.claudroide.app.feature.git.DocumentedFact
import org.claudroide.app.feature.git.GitAccessCatalog
import org.claudroide.app.feature.git.GitAccessDecision
import org.claudroide.app.feature.git.GitAccessGate
import org.claudroide.app.feature.git.GitAccessPath
import org.claudroide.app.feature.git.GitAccessPathKind
import org.claudroide.app.feature.git.GitAuthTopic
import org.claudroide.app.feature.git.GitCredential
import org.claudroide.app.feature.git.GitHubAppDocumentedFacts
import org.claudroide.app.feature.git.InMemoryGitCredentialVault
import org.claudroide.app.feature.git.OpenQuestion
import org.claudroide.app.feature.git.RepositoryScope
import org.claudroide.app.feature.git.UploadDisclosure
import org.claudroide.app.feature.provider.SecretMasker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aufgabe 095 — „Git-Zugang".
 *
 * Die Aufgabe nennt zwei Fertig-Bedingungen und einen Schutz:
 *
 *  1. *„Zugriff möglichst auf erforderliche Repositories beschränkt."*
 *  2. *„Nutzer versteht, dass Uploads externe Speicherung verursachen."*
 *  3. Schutz: *„Keine privaten Schlüssel in Projektdateien oder Chats."*
 *
 * Zu jeder davon wird hier **am Code** geprüft, nicht an einer Erwartung:
 *
 *  * Bedingung 1 fällt an, wenn [RepositoryScope.EverythingVisible] durch das Tor
 *    käme. Geprüft wird deshalb, dass das Tor es **ablehnt** — und dass es auch
 *    einen Weg ablehnt, dessen Umfang zwar benannt, aber zu breit ist.
 *  * Bedingung 2 fällt an, wenn [GitAccessGate.mayUse] ohne
 *    [UploadDisclosure.externalStorageAcknowledged] durchließe.
 *  * Der Schutz fällt an, wenn ein geheimer Wert in eine Projektdatei oder einen
 *    Chatverlauf gelegt werden könnte, oder wenn er durch [GitCredential.toString],
 *    [GitCredential.displayLine] oder [InMemoryGitCredentialVault.exportSafeMetadata]
 *    nach außen gelangte. Geprüft wird jeder dieser drei Wege einzeln.
 *
 * Zusätzlich wird die **Belegpflicht** geprüft: Eine Aussage über Rechte,
 * Lebensdauer oder Widerruf darf nur mit Quelle und Prüfdatum entstehen. Und die
 * Wege ohne Beleg — persönlicher Token, SSH-Schlüssel — müssen genau deshalb im
 * Tor abgelehnt werden.
 */
class GitProviderAuthTest {

    // ── Hilfen ─────────────────────────────────────────────────────────────

    private val repos = listOf("claudroide")

    private fun scope(vararg names: String): RepositoryScope = RepositoryScope.Selected(names.toList())

    private fun disclosure(
        acknowledged: Boolean = true,
        names: List<String> = repos
    ) = UploadDisclosure(
        repositoryNames = names,
        localOnlySummary = "Auf dem Gerät: nur der lokale Arbeitsstand.",
        externalStorageAcknowledged = acknowledged
    )

    /** Ein geheimer Wert im erlaubten Schlüsselspeicher. */
    private fun credentialInKeystore(
        id: String = "github-app-installation",
        secret: String = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA\n-----END RSA PRIVATE KEY-----"
    ): GitCredential = GitCredential.record(
        credentialId = id,
        pathKind = GitAccessPathKind.GITHUB_APP,
        secret = secret,
        sink = CredentialSink.KEYSTORE
    )

    // ── Belegpflicht ───────────────────────────────────────────────────────

    @Test
    fun `eine belegte Angabe braucht Quelle und Datum`() {
        val belegt = DocumentedFact(
            statement = "Token sind kurzlebig.",
            sourceUrl = "https://docs.github.com/en/example",
            verifiedOn = "2026-10-03"
        )

        assertEquals("https://docs.github.com/en/example", belegt.sourceUrl)
        assertEquals("2026-10-03", belegt.verifiedOn)
    }

    @Test
    fun `eine Angabe ohne Quelle laesst sich nicht als bekannt deklarieren`() {
        val ohneQuelle = runCatching {
            DocumentedFact(statement = "Token sind kurzlebig.", sourceUrl = "", verifiedOn = "2026-10-03")
        }
        val ohneDatum = runCatching {
            DocumentedFact(statement = "Token sind kurzlebig.", sourceUrl = "https://docs.github.com/en/example", verifiedOn = "  ")
        }

        assertTrue("Eine Angabe ohne Quelle muss abgewiesen werden.", ohneQuelle.isFailure)
        assertTrue("Eine Angabe ohne Prüfdatum muss abgewiesen werden.", ohneDatum.isFailure)
    }

    @Test
    fun `der Katalog nennt fuer jeden belegten Weg die Quelle und das Datum`() {
        val zeilen = GitAccessCatalog.all.flatMap { it.displayLines() }.joinToString(" ")

        assertTrue(
            "Jede belegte Zeile muss ihre Quelle nennen.",
            zeilen.contains(GitHubAppDocumentedFacts.SOURCE_URL)
        )
        assertTrue(
            "Jede belegte Zeile muss ihr Pruefdatum nennen.",
            zeilen.contains(GitHubAppDocumentedFacts.VERIFIED_ON)
        )
    }

    @Test
    fun `der gewaehlte Weg traegt die drei Belege aus der gelesenen Quelle`() {
        val belege = GitAccessCatalog.GITHUB_APP.facts

        assertEquals(3, belege.size)
        assertTrue(belege.all { it.sourceUrl == GitHubAppDocumentedFacts.SOURCE_URL })
        assertTrue(belege.all { it.verifiedOn == GitHubAppDocumentedFacts.VERIFIED_ON })
        assertTrue(
            "Mindestens ein Beleg muss die Rechteabgrenzung nennen.",
            belege.any { it.statement.contains("fein abgestufte") }
        )
        assertTrue(
            "Mindestens ein Beleg muss den Widerruf nennen.",
            belege.any { it.statement.contains("Widerruf") || it.statement.contains("verliert") }
        )
    }

    @Test
    fun `der Alternativweg traegt seinen eigenen Nachteil als Beleg`() {
        val oauth = GitAccessCatalog.OAUTH_APP

        // Ein Vergleich, der nur die gute Seite zeigte, waere keine Entscheidungsgrundlage.
        assertTrue(
            "Der Nachteil des Alternativwegs muss belegt sein.",
            oauth.facts.any { it.statement.contains("langlebig") }
        )
        assertEquals("OAuth-App", oauth.displayName)
    }

    @Test
    fun `ein Weg ohne Beleg sagt offen, dass nichts belegt ist`() {
        listOf(GitAccessCatalog.PERSONAL_ACCESS_TOKEN, GitAccessCatalog.SSH_KEY).forEach { weg ->
            assertTrue("${weg.displayName} darf keine unbelegte Tatsache behaupten.", weg.facts.isEmpty())
            assertTrue("${weg.displayName} muss etwas offen benennen.", weg.hasOpenQuestions)
            assertFalse("${weg.displayName} darf nicht als vollstaendig belegt gelten.", weg.isFullyDocumented)
        }
    }

    @Test
    fun `eine offene Frage erscheint als offen und nicht als leeres Feld`() {
        val zeilen = GitAccessCatalog.PERSONAL_ACCESS_TOKEN.displayLines().joinToString(" ")

        assertTrue(zeilen.contains("offen"))
        assertTrue(
            "Der Nutzer muss sehen, dass die App es nicht weiss.",
            zeilen.contains("keine belegte Angabe")
        )
    }

    // ── Bedingung 1: Umfang auf erforderliche Repositories beschränkt ───────

    @Test
    fun `ein benannter Umfang gilt als beschränkt`() {
        val umfang = scope("claudroide")

        assertTrue(umfang.isLeastPrivilege)
        assertTrue(umfang.covers("claudroide"))
        assertFalse(umfang.covers("fremdes-projekt"))
    }

    @Test
    fun `alles Sichtbare gilt nicht als beschränkt und benennt das offen`() {
        val umfang = RepositoryScope.EverythingVisible

        assertFalse("Ein unbeschraenkter Umfang darf nicht als beschränkt gelten.", umfang.isLeastPrivilege)
        assertTrue(umfang.covers("beliebiges-fremdes-repo"))
        assertTrue(
            "Die Anzeige muss den weiten Zugriff benennen, nicht verschweigen.",
            umfang.displayLine().contains("nicht auf erforderliche beschränkt")
        )
    }

    @Test
    fun `das Tor laesst einen benannten Umfang zu`() {
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.GITHUB_APP,
            scope = scope("claudroide"),
            disclosure = disclosure()
        )

        assertTrue("Ein belegter Weg mit benanntem Umfang muss erlaubt sein.", ergebnis is GitAccessDecision.Allowed)
    }

    @Test
    fun `das Tor weist einen unbeschraenkten Umfang zurueck`() {
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.GITHUB_APP,
            scope = RepositoryScope.EverythingVisible,
            disclosure = disclosure()
        )

        assertTrue(
            "Ein Zugriff auf alles Sichtbare darf nicht durch das Tor.",
            ergebnis is GitAccessDecision.ScopeTooBroad
        )
        val grund = (ergebnis as GitAccessDecision.ScopeTooBroad).reason
        assertTrue(grund.contains("nicht auf erforderliche"))
    }

    @Test
    fun `der benannte Umfang erscheint in der Erlaubnis, damit der Nutzer das Ziel sieht`() {
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.GITHUB_APP,
            scope = scope("claudroide"),
            disclosure = disclosure()
        ) as GitAccessDecision.Allowed

        assertTrue(
            "Die Erlaubnis muss sagen, wohin es geht.",
            ergebnis.scopeLines.any { it.contains("claudroide") }
        )
    }

    @Test
    fun `eine leere Repository-Liste ist kein gueltiger Umfang`() {
        val leer = runCatching { RepositoryScope.Selected(emptyList()) }
        val mitLeeremNamen = runCatching { RepositoryScope.Selected(listOf("claudroide", " ")) }
        val doppelt = runCatching { RepositoryScope.Selected(listOf("claudroide", "claudroide")) }

        assertTrue("Eine leere Liste kann keinen Umfang benennen.", leer.isFailure)
        assertTrue("Ein leerer Repository-Name benennt nichts.", mitLeeremNamen.isFailure)
        assertTrue("Dieselbe Liste zu wiederholen aendert nichts.", doppelt.isFailure)
    }

    // ── Bedingung 2: Der Nutzer versteht die externe Speicherung ────────────

    @Test
    fun `ohne Bestaetigung der externen Speicherung gibt es keinen Zugang`() {
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.GITHUB_APP,
            scope = scope("claudroide"),
            disclosure = disclosure(acknowledged = false)
        )

        assertTrue(
            "Ohne Bestaetigung darf der Zugang nicht erlaubt sein.",
            ergebnis is GitAccessDecision.NotAcknowledged
        )
        val gruende = (ergebnis as GitAccessDecision.NotAcknowledged).reasons.joinToString(" ")
        assertTrue(gruende.contains("externe Speicherung"))
    }

    @Test
    fun `die Bestaetigung wird nicht aus dem Umfang abgeleitet`() {
        // Ein minimaler, eng benannter Umfang ist KEINE Bestaetigung der
        // externen Speicherung. Wuerde mayUse das ableiten, waere die zweite
        // Fertig-Bedingung der Aufgabe erfuellt, ohne dass der Nutzer etwas
        // bestaetigt haette.
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.GITHUB_APP,
            scope = scope("claudroide"),
            disclosure = disclosure(acknowledged = false)
        )

        assertFalse(
            "Ein benannter Umfang darf die Bestaetigung nicht ersetzen.",
            ergebnis is GitAccessDecision.Allowed
        )
    }

    @Test
    fun `die Aufklaerung nennt das Ziel und die externe Speicherung`() {
        val zeilen = disclosure(names = listOf("claudroide", "notizen")).displayLines().joinToString(" ")

        assertTrue(zeilen.contains("claudroide"))
        assertTrue(zeilen.contains("notizen"))
        assertTrue(
            "Der Nutzer muss hoeren, dass der Inhalt danach ausserhalb des Geraets liegt.",
            zeilen.contains("außerhalb des Geräts")
        )
    }

    @Test
    fun `ein Upload ohne benanntes Ziel ist nicht moeglich`() {
        val ohneZiel = runCatching {
            UploadDisclosure(
                repositoryNames = emptyList(),
                localOnlySummary = "nichts",
                externalStorageAcknowledged = true
            )
        }

        assertTrue("Vor einem Upload muss das Ziel benannt sein.", ohneZiel.isFailure)
    }

    // ── Der Schutz: keine privaten Schluessel in Projektdateien oder Chats ───

    @Test
    fun `ein geheimer Wert wird nur im Schluesselspeicher angenommen`() {
        val erlaubt = runCatching {
            GitCredential.record("kennung", GitAccessPathKind.GITHUB_APP, "geheim", CredentialSink.KEYSTORE)
        }

        assertTrue("Der Schluesselspeicher muss erlaubt sein.", erlaubt.isSuccess)
    }

    @Test
    fun `ein geheimer Wert wird in einer Projektdatei abgewiesen`() {
        val abgewiesen = runCatching {
            GitCredential.record(
                "kennung",
                GitAccessPathKind.GITHUB_APP,
                "geheim",
                CredentialSink.PROJECT_FILE
            )
        }

        assertTrue(
            "Ein geheimer Wert darf nicht in eine Projektdatei.",
            abgewiesen.isFailure
        )
        val grund = abgewiesen.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "Die Ablehnung muss den unzulaessigen Ort nennen.",
            grund.contains("Projektdatei")
        )
    }

    @Test
    fun `ein geheimer Wert wird in einem Chatverlauf abgewiesen`() {
        val abgewiesen = runCatching {
            GitCredential.record(
                "kennung",
                GitAccessPathKind.GITHUB_APP,
                "geheim",
                CredentialSink.CHAT_TRANSCRIPT
            )
        }

        assertTrue(
            "Ein geheimer Wert darf nicht in einen Chatverlauf.",
            abgewiesen.isFailure
        )
        val grund = abgewiesen.exceptionOrNull()?.message.orEmpty()
        assertTrue(grund.contains("Chatverlauf"))
    }

    @Test
    fun `nur der Schluesselspeicher ist als Ablage zugelassen`() {
        val erlaubteOrte = CredentialSink.entries.filter { it.isPermitted }

        assertEquals(listOf(CredentialSink.KEYSTORE), erlaubteOrte)
    }

    @Test
    fun `jede abgewiesene Ablage nennt ihren Ablehnungsgrund`() {
        CredentialSink.entries.filter { !it.isPermitted }.forEach { ort ->
            val zeile = ort.refusalLine()
            assertTrue(
                "Der Ablehnungstext fuer $ort muss die Regel nennen.",
                zeile.contains("nicht zulässig")
            )
            assertTrue(zeile.contains(ort.germanLabel))
        }
    }

    @Test
    fun `eine Kennung darf keinen Schluessel enthalten`() {
        // Die Kennung wird in Logs und Anzeigen benutzt. Ein Schluessel darin waere
        // derselbe Fehler an anderer Stelle.
        val mitSchluessel = runCatching {
            GitCredential.record(
                "sk-ant-api03-abcdefghijklmnopqrstuvwxyz0123456789",
                GitAccessPathKind.GITHUB_APP,
                "geheim",
                CredentialSink.KEYSTORE
            )
        }

        assertTrue(
            "Die Kennung wird angezeigt; sie darf keinen Schluessel tragen.",
            mitSchluessel.isFailure
        )
    }

    @Test
    fun `ein leerer geheimer Wert wird abgewiesen`() {
        val leer = runCatching {
            GitCredential.record("kennung", GitAccessPathKind.GITHUB_APP, "   ", CredentialSink.KEYSTORE)
        }
        val ohneKennung = runCatching {
            GitCredential.record("", GitAccessPathKind.GITHUB_APP, "geheim", CredentialSink.KEYSTORE)
        }

        assertTrue("Ein leerer geheimer Wert traegt nichts.", leer.isFailure)
        assertTrue("Ohne Kennung ist der Wert nicht auffindbar.", ohneKennung.isFailure)
    }

    @Test
    fun `toString gibt das Geheimnis nicht aus`() {
        val geheim = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA-privater-teil\n-----END RSA PRIVATE KEY-----"
        val zugangsdaten = credentialInKeystore(secret = geheim)

        val text = zugangsdaten.toString()

        assertFalse(
            "toString darf das Geheimnis nicht nennen.",
            text.contains("privater-teil")
        )
        assertFalse(text.contains("BEGIN RSA PRIVATE KEY"))
        assertTrue(text.contains("nicht ausgegeben"))
    }

    @Test
    fun `die Anzeige gibt weder Anfang noch Ende des Geheimnisses aus`() {
        val geheim = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA-privater-teil\n-----END RSA PRIVATE KEY-----"
        val zugangsdaten = credentialInKeystore(secret = geheim)

        val zeile = zugangsdaten.displayLine()

        // Weder ein Anfang noch ein Ende: bei einem PEM-Block waeren die Randzeichen
        // ein Anteil des privaten Schluessels.
        assertFalse(zeile.contains("BEGIN"))
        assertFalse(zeile.contains("MIIEowIBAAKCAQEA"))
        assertFalse(zeile.contains("privater-teil"))
        assertFalse(zeile.contains("END RSA"))
    }

    @Test
    fun `zwei verschiedene Geheimnisse sind in der Anzeige nicht unterscheidbar`() {
        val erstes = credentialInKeystore(id = "a", secret = "erstes-geheim-wert")
        val zweites = credentialInKeystore(id = "b", secret = "voellig-anderes-geheim")

        assertEquals(erstes.maskedFingerprint(), zweites.maskedFingerprint())
        assertNotEquals(
            "Im Gebrauch muessen sie verschiedene Werte sein.",
            erstes.useSecret(),
            zweites.useSecret()
        )
    }

    @Test
    fun `der Gebrauchswert kommt unveraendert zurueck`() {
        val geheim = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA\n-----END RSA PRIVATE KEY-----"

        assertEquals(geheim, credentialInKeystore(secret = geheim).useSecret())
    }

    // ── Der Tresor ─────────────────────────────────────────────────────────

    @Test
    fun `der Tresor gibt gespeicherte Werte zurueck und entfernt sie wieder`() {
        val tresor = InMemoryGitCredentialVault()
        val zugangsdaten = credentialInKeystore()

        assertEquals(0, tresor.size())
        tresor.store(zugangsdaten)
        assertEquals(1, tresor.size())
        assertEquals(zugangsdaten, tresor.retrieve("github-app-installation"))
        assertNull(tresor.retrieve("gibt-es-nicht"))

        assertTrue(tresor.remove("github-app-installation"))
        assertEquals(0, tresor.size())
        assertFalse("Ein zweites Entfernen findet nichts mehr.", tresor.remove("github-app-installation"))
    }

    @Test
    fun `die Tresor-Ausgabe enthaelt kein Geheimnis - auch kein Anfang`() {
        val tresor = InMemoryGitCredentialVault()
        tresor.store(
            credentialInKeystore(
                secret = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA-geheimanteil\n-----END RSA PRIVATE KEY-----"
            )
        )

        val ausgabe = tresor.exportSafeMetadata().values.joinToString(" ")

        assertFalse("Die Ausgabe darf kein Geheimnis nennen.", ausgabe.contains("geheimanteil"))
        assertFalse(ausgabe.contains("MIIEowIBAAKCAQEA"))
        assertFalse(ausgabe.contains("BEGIN RSA PRIVATE KEY"))
        assertFalse(ausgabe.contains("PRIVATE KEY"))
    }

    @Test
    fun `die Tresor-Ausgabe nennt den Ablageort, damit der Ort des Geheimnisses sichtbar bleibt`() {
        val tresor = InMemoryGitCredentialVault()
        tresor.store(credentialInKeystore())

        val ausgabe = tresor.exportSafeMetadata().getValue("github-app-installation")

        assertTrue(
            "Der Nutzer muss sehen, wo sein Geheimnis liegt.",
            ausgabe.contains(CredentialSink.KEYSTORE.germanLabel)
        )
        assertTrue(
            "Der Wert selbst muss als zensiert gekennzeichnet sein.",
            ausgabe.contains(SecretMasker.REDACTION_PLACEHOLDER)
        )
    }

    @Test
    fun `zwei Zugangsdaten mit gleichem Inhalt sind gleich`() {
        val a = credentialInKeystore()
        val b = credentialInKeystore()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val anders = credentialInKeystore(secret = "anderes-geheim")
        assertNotEquals(a, anders)
    }

    // ── Das Tor: Prioritaet und Gassen ──────────────────────────────────────

    @Test
    fun `ein Weg mit offenen Fragen wird abgewiesen, auch bei perfektem Umfang`() {
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.PERSONAL_ACCESS_TOKEN,
            scope = scope("claudroide"),
            disclosure = disclosure()
        )

        assertTrue(
            "Ein unbelegter Weg darf nicht durch das Tor, egal wie eng der Umfang ist.",
            ergebnis is GitAccessDecision.NotAcknowledged
        )
        val gruende = (ergebnis as GitAccessDecision.NotAcknowledged).reasons.joinToString(" ")
        assertTrue(gruende.contains("offen"))
    }

    @Test
    fun `die offene Frage wird vor dem Umfang gemeldet`() {
        // Feste Reihenfolge: Der Nutzer soll die ERSTE offene Sache sehen.
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.SSH_KEY,
            scope = RepositoryScope.EverythingVisible,
            disclosure = disclosure(acknowledged = false)
        )

        val gruende = (ergebnis as GitAccessDecision.NotAcknowledged).reasons.joinToString(" ")

        assertTrue("Die offene Frage des Weges muss zuerst kommen.", gruende.contains("offen"))
        assertFalse(
            "Der zu weite Umfang darf die offene Frage nicht verdecken.",
            gruende.contains("nicht auf erforderliche beschränkt")
        )
    }

    @Test
    fun `der Umfang wird vor der fehlenden Bestaetigung gemeldet`() {
        val ergebnis = GitAccessGate.mayUse(
            path = GitAccessCatalog.GITHUB_APP,
            scope = RepositoryScope.EverythingVisible,
            disclosure = disclosure(acknowledged = false)
        )

        assertTrue(
            "Ein zu weiter Umfang kommt vor der fehlenden Bestaetigung.",
            ergebnis is GitAccessDecision.ScopeTooBroad
        )
    }

    @Test
    fun `nur der gewaehlte Weg ist vollstaendig belegt`() {
        val vollstaendigBelegt = GitAccessCatalog.all.filter { it.isFullyDocumented }.map { it.kind }

        assertEquals(
            listOf(GitAccessPathKind.GITHUB_APP, GitAccessPathKind.OAUTH_APP),
            vollstaendigBelegt
        )
    }

    @Test
    fun `ein Weg mit geheimer Ablage auf dem Geraet sagt das offen`() {
        val mitGeheimnis = GitAccessCatalog.all.filter { it.requiresSecretOnDevice }.map { it.kind }

        assertTrue(
            "Ein GitHub App gehoert zu den Wegen mit geheimem Anteil auf dem Geraet.",
            GitAccessPathKind.GITHUB_APP in mitGeheimnis
        )
        assertFalse(
            "Eine OAuth-App bringt keinen geheimen Schluessel auf das Geraet.",
            GitAccessPathKind.OAUTH_APP in mitGeheimnis
        )
    }

    // ── Strukturelle Zusicherungen ──────────────────────────────────────────

    @Test
    fun `die Ablagepruefung sitzt in record und nicht in einer aufrufbaren Alternative`() {
        // record() ist der einzige Weg zu einer Zugangsdatenauskunft. Waere der
        // Konstruktor aufrufbar, koennte man die Ablehnung umgehen.
        val konstruktoren = GitCredential::class.java.declaredConstructors
            .filter { !it.isSynthetic }
            .map { java.lang.reflect.Modifier.isPrivate(it.modifiers) }

        assertEquals(
            "Jeder Konstruktor dieser Klasse muss privat sein.",
            listOf(true),
            konstruktoren
        )
    }

    @Test
    fun `das Geheimnisfeld ist privat und jede String-Methode ist bekannt`() {
        // Die Absicht war und bleibt: useSecret ist der einzige Weg zum
        // Geheimnis. Ein reiner Methodenvergleich kann das nicht beweisen,
        // weil auch toString und der Kennungs-Getter einen String liefern —
        // ohne etwas preiszugeben. Geprueft wird deshalb die belastbare
        // Nachbareigenschaft, die dabei herausfaellt: das Feld ist privat, und
        // JEDE String-liefernde oeffentliche Methode steht in dieser Liste.
        // Ein neu hinzugekommener String-Getter faellt hier also rot auf, bis
        // jemand ihn bewusst eintragen muss.
        val bekannte = setOf(
            "toString",              // gibt den Wert gerade NICHT aus
            "useSecret",             // der einzige Abrufpfad
            "getCredentialId",       // eine Kennung, kein Geheimnis
            "getSink",               // ein Ablageort, kein Geheimnis
            "getPathKind",           // ein Weg, kein Geheimnis
            "maskedFingerprint",     // gibt kein Zeichen aus
            "displayLine"            // gibt kein Zeichen aus
        )

        val gefunde = GitCredential::class.java.methods
            .filter { it.returnType == String::class.java }
            .map { it.name }
            .filterNot { it in bekannte }

        assertEquals(
            "Eine neue String-liefernde Methode muss hier bewusst eingetragen werden.",
            emptyList<String>(),
            gefunde
        )

        val geheimnisFeld = GitCredential::class.java.declaredFields
            .first { it.name == "secret" }

        assertTrue(
            "Das Geheimnisfeld muss privat sein.",
            java.lang.reflect.Modifier.isPrivate(geheimnisFeld.modifiers)
        )
    }

    @Test
    fun `kein Topic gilt als bekannt, solange es nicht belegt ist`() {
        val offeneThemen = GitAccessCatalog.PERSONAL_ACCESS_TOKEN.openQuestions.map { it.topic }

        assertTrue(GitAuthTopic.TOKEN_LIFETIME in offeneThemen)
        assertTrue(GitAuthTopic.REVOCATION in offeneThemen)
        assertFalse(
            "PERMISSION_SCOPE steht beim Token offen, nicht beim Schluessel.",
            GitAuthTopic.PERMISSION_SCOPE in GitAccessCatalog.SSH_KEY.openQuestions.map { it.topic }
        )
    }

    @Test
    fun `eine offene Frage braucht eine Beschreibung`() {
        val ohneText = runCatching { OpenQuestion(GitAuthTopic.TOKEN_LIFETIME, "  ") }

        assertTrue("Eine offene Frage ohne Text sagt nichts.", ohneText.isFailure)
    }

    @Test
    fun `ein Zugangsweg braucht einen Namen`() {
        val ohneNamen = runCatching {
            GitAccessPath(
                kind = GitAccessPathKind.GITHUB_APP,
                displayName = " ",
                facts = emptyList(),
                openQuestions = listOf(OpenQuestion(GitAuthTopic.REVOCATION, "offen"))
            )
        }

        assertTrue(ohneNamen.isFailure)
    }
}
