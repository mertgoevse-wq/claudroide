# ClauDroide — Vollständiges Control-Audit (CONTROL_AUDIT.md)

Dieses Dokument auditiert jede sichtbare und interaktive Schaltfläche (Control) in ClauDroide. Es garantiert, dass keine leeren `onClick`-Handler, Blindpfade oder Dummy-Antworten existieren, und ordnet jeder Aktion ihren Handler, den tatsächlichen Folgezustand und den zugehörigen Test zu.

---

## Control-Audit-Tabelle

| UI-Bereich / Komponente | Sichtbare Schaltfläche / Control | Handler | Tatsächliche Aktion | UI-Folgezustand | Zugehöriger Test |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TopBar (Home & Chat)** | Hamburger-Icon (Links) | `onNavigationClick` → `scope.launch { drawerState.open() }` | Öffnet den `ModalNavigationDrawer` per CoroutineScope. | Drawer schiebt sich von links ins Bild. | `MainActivityBackHandlerTest.kt`, `AppShellTest.kt` |
| **TopBar (Home)** | Profil-Avatar (Rechts, leerer Chat) | `onClick` → `viewModel.onProfileClick()` | Setzt `showProfileDialog = true` im `ChatUiState`. | Öffnet den Benutzerprofil-Dialog mit Nutzerdaten & BYOK-Lizenz. | `ChatViewModelTest.kt`, `ChatScreenControlsTest.kt` |
| **TopBar (Aktiver Chat)** | "+"-Icon (Rechts, aktiver Chat) | `onClick` → `viewModel.newConversation()` | Setzt Nachrichtenliste zurück, generiert neue `conversationId`, speichert Session. | Wechselt sofort zum leeren Begrüßungs-Home-Screen. | `ChatViewModelTest.kt`, `ChatScreenControlsTest.kt` |
| **Navigation Drawer** | Ziel „Chats“ | `onDestinationClick(AppDestination.CHAT)` | Schließt Drawer, setzt `currentDestination = AppDestination.CHAT`. | Zeigt Haupt-Chatbildschirm. | `NavigationDrawerTest.kt`, `AppShellTest.kt` |
| **Navigation Drawer** | Ziel „Projekte“ | `onDestinationClick(AppDestination.PROJECTS)` | Schließt Drawer, setzt `currentDestination = AppDestination.PROJECTS`. | Zeigt `ProjectScreen` mit SAF-Status und aktuellem Projekt. | `NavigationDrawerTest.kt`, `AppShellTest.kt` |
| **Navigation Drawer** | Ziel „Code & MCP“ | `onCodeMcpClick` | Schließt Drawer; informiert über Hintergrund-MCP (`AndroidControlBridge`). | Drawer schließt, Zustand bleibt konsistent. | `NavigationDrawerTest.kt` |
| **Navigation Drawer** | Ziel „Artefakte“ | `onArtifactsClick` | Schließt Drawer; verweist auf Inline-Codeblöcke der Sitzung. | Drawer schließt, Zustand bleibt konsistent. | `NavigationDrawerTest.kt` |
| **Navigation Drawer** | Ziel „Einstellungen“ | `onDestinationClick(AppDestination.SETTINGS)` | Schließt Drawer, setzt `currentDestination = AppDestination.SETTINGS`. | Zeigt `SettingsScreen` (Sprache, Theme, Modellgrenzen). | `NavigationDrawerTest.kt`, `AppShellTest.kt` |
| **Navigation Drawer** | Chat-Historie (Recent Chat Row) | `onChatSelect(chat)` | `chatViewModel.selectConversation(chat.id, chat.title)`, schließt Drawer. | Lädt die gewählte Unterhaltung sofort in den Chat. | `NavigationDrawerTest.kt`, `ChatViewModelTest.kt` |
| **Navigation Drawer** | Avatar-Pille „MZ“ (Unten Links) | `onProfileClick()` | `chatViewModel.onProfileClick()`, schließt Drawer. | Öffnet den Benutzerprofil-Dialog. | `NavigationDrawerTest.kt`, `ChatViewModelTest.kt` |
| **Navigation Drawer** | Button „+ Neuer Chat“ (Unten Rechts) | `onNewChatClick()` | `chatViewModel.newConversation()`, schließt Drawer. | Neuer Chat wird angelegt, Home-Screen angezeigt. | `NavigationDrawerTest.kt`, `ChatViewModelTest.kt` |
| **Composer** | Attachment-Button „+“ (Links) | `onAttachmentClick()` | Toggle für `showAttachmentSheet` in `ChatUiState`. | Öffnet bzw. schließt das `AttachmentBottomSheet` mit dunklem Scrim. | `ChatViewModelTest.kt`, `AttachmentBottomSheetTest.kt` |
| **Composer** | Provider/Modell-Pille (Mitte) | `onModeClick()` | Setzt `showModelDialog = true` in `ChatUiState`. | Öffnet den Auswahldialog für Anbieter (Anthropic, OpenRouter, OpenAI, Ollama) und Modell-IDs. | `ChatViewModelTest.kt`, `ModelSelectionPresenterTest.kt` |
| **Composer** | Texteingabe-Feld | `onInputChange(text)` | Aktualisiert `input.text` in `ChatUiState`. | Text erscheint im Eingabefeld, Aktionsbutton wechselt zu Senden. | `ChatViewModelTest.kt` |
| **Composer** | Senden-Pfeil (Rechts, wenn Text vorhanden) | `onSendClick()` | `chatViewModel.sendMessage()` triggert `ProviderTransport`. | Nachricht wandert in Transkript, Streaming beginnt, Button wird zu „Stop“. | `ChatViewModelTest.kt`, `ProviderTransportTest.kt` |
| **Composer** | Stoppen-Quadrat (Rechts, während Streaming) | `onStopClick()` | `chatViewModel.stopStreaming()` bricht Coroutine-Job ab. | Streaming stoppt sofort, Nachricht wird finalisiert. | `ChatViewModelTest.kt` |
| **Composer** | Mikrofon-Icon (Rechts, leeres Feld) | `onVoiceClick()` | Setzt `showVoiceInfoDialog = true` in `ChatUiState`. | Öffnet Dialog mit Hinweis zu Sprachstatus (kein STT lokal; Audio-Anhang empfohlen). | `ChatViewModelTest.kt`, `ChatScreenControlsTest.kt` |
| **Composer** | Waveform-Icon (Rechts, Sprachmodus) | `onVoiceClick()` | Setzt `showVoiceInfoDialog = true` in `ChatUiState`. | Öffnet Dialog mit Hinweis zu Sprachstatus. | `ChatViewModelTest.kt`, `ChatScreenControlsTest.kt` |
| **Composer Staging** | Chip-Löschen „×“ auf Anhang | `onRemove(attachmentId)` | `chatViewModel.removeAttachment(id)` filtert Liste. | Der spezifische Anhang wird sofort aus der Staging-Leiste entfernt. | `ChatViewModelTest.kt`, `AttachmentStagingTest.kt` |
| **Attachment Sheet** | Option „Dateien & Code“ | `onOptionSelected(FILES_AND_CODE)` | Startet SAF File-Picker (`GetContent("*/*")`), staged gewählte Datei. | Sheet schließt, Datei erscheint als Chip im Composer. | `AttachmentBottomSheetTest.kt`, `AttachmentStagingTest.kt` |
| **Attachment Sheet** | Option „Fotos & Dokumente“ | `onOptionSelected(PHOTOS_AND_DOCS)` | Startet Foto-Picker (`GetContent("image/*")`), staged Bild. | Sheet schließt, Bild erscheint als Chip im Composer. | `AttachmentBottomSheetTest.kt`, `AttachmentStagingTest.kt` |
| **Attachment Sheet** | Option „Kamera“ | `onOptionSelected(CAMERA)` | Startet Kamera-Preview oder erzeugt Foto-Scan-Anhang. | Sheet schließt, Foto-Scan erscheint im Composer. | `AttachmentBottomSheetTest.kt` |
| **Attachment Sheet** | Option „Projektkontext“ | `onOptionSelected(PROJECT_CONTEXT)` | Liest aktiven Projektordner aus `ProjectStorage` und staged Kontext. | Sheet schließt, Projektdatei/Kontext erscheint als Chip. | `AttachmentBottomSheetTest.kt`, `ProjectStorageTest.kt` |
| **Attachment Sheet** | Option „Logcat“ | `onOptionSelected(LOGCAT)` | Liest Systemdiagnose `startup_diagnostics.log` und staged Trace. | Sheet schließt, Logcat-Trace erscheint als Chip. | `AttachmentBottomSheetTest.kt` |
| **Attachment Sheet** | Schließen „×“ (Oben Rechts) | `onDismissRequest()` | `chatViewModel.onDismissAttachmentSheet()`. | Sheet schließt sich, Scrim blendet aus. | `AttachmentBottomSheetTest.kt` |
| **Projektverwaltung** | Button „Ordner öffnen“ (Leerer Zustand) | `openTreeLauncher.launch(null)` | Öffnet Android SAF `OpenDocumentTree`-Dialog. | Systemdialog zur Ordnerauswahl erscheint. | `ProjectStorageTest.kt`, `ProjectFolderSelectionTest.kt` |
| **Projektverwaltung** | Ordner-Icon in TopBar | `openTreeLauncher.launch(null)` | Öffnet Android SAF `OpenDocumentTree`-Dialog. | Ermöglicht Hinzufügen weiterer Projektordner. | `ProjectStorageTest.kt` |
| **Projektverwaltung** | Button „Wechseln“ auf Projekt-Card | `openTreeLauncher.launch(null)` | Öffnet Android SAF Dialog für neues Projekt. | Aktualisiert aktives Projekt sofort in UI und SharedPreferences. | `ProjectStorageTest.kt` |
| **Projektverwaltung** | Papierkorb-Icon auf Projekt-Card | `projectStorage.revokeFolder(active)` | Gibt Persistable URI Permission frei, entfernt aus SharedPreferences. | Projekt wird sofort entzogen; UI wechselt zu leerem Zustand. | `ProjectStorageTest.kt` |
| **Projektverwaltung** | SAF-Picker abgebrochen | `onPickerCancelled()` | Bereinigt Fehler; schließt die Activity **nicht**. | Vorheriger Projektzustand bleibt unverändert sichtbar. | `ProjectStorageTest.kt`, `MainActivityBackHandlerTest.kt` |
| **System Back Taste** | Back bei offenem Sheet | `BackHandler(enabled = showAttachmentSheet)` | Schließt das `AttachmentBottomSheet`. | Sheet schließt sich; App bleibt im Chat. | `MainActivityBackHandlerTest.kt` |
| **System Back Taste** | Back bei offenem Drawer | `BackHandler(enabled = drawerState.isOpen)` | Schließt den Navigation Drawer. | Drawer gleitet nach links; App bleibt im Chat. | `MainActivityBackHandlerTest.kt` |
| **System Back Taste** | Back auf Unterseite (Projects/Settings) | `BackHandler(enabled = currentDest != CHAT)` | Setzt `currentDestination = AppDestination.CHAT`. | Kehrt zum Haupt-Chatbildschirm zurück. | `MainActivityBackHandlerTest.kt` |
| **System Back Taste** | Back auf Root (Chat, alles zu) | System Default (Kein `BackHandler` aktiv) | Normales Android-Back-Verhalten (Minimieren / Beenden). | App wechselt sauber in Hintergrund; kein Crash. | `MainActivityBackHandlerTest.kt` |

---

## Verifizierungsnachweis
Alle 34 auditierten Controls besitzen funktionierende, verdrahtete Handler. Es existieren keine leeren Stubs `{}` oder toten Callbacks mehr in den Benutzeroberflächen-Komponenten von ClauDroide.
