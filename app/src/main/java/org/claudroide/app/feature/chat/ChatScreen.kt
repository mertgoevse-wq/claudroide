package org.claudroide.app.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.claudroide.app.R
import org.claudroide.app.core.design.components.BrandMark
import org.claudroide.app.core.design.components.CodeBlock
import org.claudroide.app.core.design.components.StreamingText
import org.claudroide.app.core.design.theme_compat.CcTheme
import org.claudroide.app.core.i18n.TimeBasedGreeting
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import org.claudroide.app.feature.provider.ModelRegistry
import org.claudroide.app.feature.provider.ProviderCatalogRegistry

/**
 * One entry in the transcript.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val code: String? = null,
    val isStreaming: Boolean = false,
)

/** Mode selector state for the composer. */
enum class ComposerMode {
    CHAT,
    CODE,
    PROJECT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage> = emptyList(),
    input: ChatInputState = ChatInputState(),
    onNewChatClick: () -> Unit = {},
    onInputChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    onModeClick: () -> Unit = {},
    onNavigationClick: () -> Unit = {},
    viewModel: ChatViewModel? = null,
) {
    val s = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: ChatUiState()
    val effectiveMessages = s.messages.ifEmpty { messages }
    val effectiveInput = s.input.copy(
        targetProviderName = ProviderCatalogRegistry.getProvider(s.currentProviderId)
            ?.displayName ?: input.targetProviderName
    )
    val effectiveError = s.error
    val colors = CcTheme.tokens.colors
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "datei_${System.currentTimeMillis()}"
            viewModel?.addAttachment(name, 1024L, "text/plain", true)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "bild_${System.currentTimeMillis()}.png"
            viewModel?.addAttachment(name, 20480L, "image/png", false)
        }
    }

    // Auto-scroll to latest message when new message arrives
    LaunchedEffect(effectiveMessages.size, s.isStreaming) {
        if (effectiveMessages.isNotEmpty()) {
            listState.animateScrollToItem(effectiveMessages.size - 1)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    if (effectiveMessages.isNotEmpty()) {
                        Column {
                            Text(
                                text = stringResource(R.string.chat_session_title),
                                style = CcTheme.tokens.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = effectiveInput.targetProviderName,
                                style = CcTheme.tokens.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigationClick,
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.nav_chat),
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    if (effectiveMessages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel?.newConversation() ?: onNewChatClick() },
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.chat_new_conversation),
                                tint = colors.textPrimary,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel?.onProfileClick() },
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = colors.textSecondary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (effectiveMessages.isEmpty()) {
                // ── Home / Empty State (Faithful to Screenshot_20261010_133740_Claude.jpg) ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        // Terracotta brand mark / sunburst
                        BrandMark(
                            size = 64.dp,
                            contentDescription = stringResource(R.string.brand_mark_alt_text),
                        )

                        Spacer(Modifier.height(18.dp))

                        // Large Serif Greeting: "Guten Tag, Mert" / "Good afternoon, Mert"
                        Text(
                            text = remember { TimeBasedGreeting.greetingNow("Mert") },
                            fontFamily = FontFamily.Serif,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.empty_chat_subtitle),
                            style = CcTheme.tokens.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                // ── Active Chat Transcript (Faithful to Screenshot_20261010_133802_Claude.jpg) ──
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(effectiveMessages, key = { it.id }) { message ->
                        if (message.fromUser) {
                            // User Message: Right-aligned subtle dark bubble
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = colors.surfaceSubtle,
                                    modifier = Modifier.widthIn(max = 310.dp),
                                ) {
                                    Text(
                                        text = message.text,
                                        style = CcTheme.tokens.typography.bodyLarge,
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        } else {
                            // Assistant Message: Left-aligned directly on canvas (no bubble)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StreamingText(
                                    text = message.text,
                                    isStreaming = message.isStreaming,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                message.code?.let { code ->
                                    CodeBlock(
                                        code = code,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }

                    if (s.isStreaming && effectiveMessages.lastOrNull()?.isStreaming != true) {
                        item(key = "streaming_indicator") {
                            StreamingIndicator()
                        }
                    }
                }
            }

            if (effectiveError != null) {
                Text(
                    text = effectiveError,
                    style = CcTheme.tokens.typography.bodySmall,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // ── Floating Bottom Composer Card ──
            MessageComposer(
                input = effectiveInput,
                onInputChange = { viewModel?.onInputChange(it) ?: onInputChange(it) },
                onSendClick = { viewModel?.sendMessage() ?: onSendClick() },
                onStopClick = { viewModel?.stopStreaming() ?: onStopClick() },
                onAttachmentClick = { viewModel?.onAttachmentClick() ?: onAttachmentClick() },
                onModeClick = { viewModel?.onModeClick() ?: onModeClick() },
                mode = ComposerMode.CHAT,
                viewModel = viewModel,
            )
        }
    }

    if (s.showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismissRequest = { viewModel?.onDismissAttachmentSheet() ?: onAttachmentClick() },
            onOptionSelected = { option ->
                viewModel?.onDismissAttachmentSheet()
                when (option) {
                    AttachmentOption.FILES_AND_CODE -> {
                        filePickerLauncher.launch("*/*")
                    }
                    AttachmentOption.PHOTOS_AND_DOCS -> {
                        photoPickerLauncher.launch("image/*")
                    }
                    AttachmentOption.CAMERA -> {
                        viewModel?.addAttachment("kamera_scan_${System.currentTimeMillis()}.jpg", 16384L, "image/jpeg", false)
                    }
                    AttachmentOption.PROJECT_CONTEXT -> {
                        val active = org.claudroide.app.feature.project.ProjectStorage.get(context).state.value.activeProject
                        val title = active?.displayName ?: "Projektkontext (claudroide-spec.md)"
                        viewModel?.addAttachment(title, 8192L, "text/markdown", true)
                    }
                    AttachmentOption.LOGCAT -> {
                        viewModel?.addAttachment("startup_diagnostics.log", 5120L, "text/plain", true)
                    }
                }
            },
            onManageContextClick = {
                viewModel?.onDismissAttachmentSheet()
            },
        )
    }

    // ── Model & Provider Selection Dialog ──
    if (s.showModelDialog) {
        val providers = remember { ProviderCatalogRegistry.getActiveProviders() }
        var selectedProviderId by remember { mutableStateOf(s.currentProviderId) }
        var selectedModelId by remember { mutableStateOf(s.currentModelId) }

        AlertDialog(
            onDismissRequest = { viewModel?.onDismissModelDialog() },
            title = {
                Text(
                    text = "Modell & Anbieter wählen",
                    style = CcTheme.tokens.typography.titleMedium,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Verfügbare Anbieter:",
                        style = CcTheme.tokens.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    providers.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedProviderId = provider.id
                                    selectedModelId = ModelRegistry
                                        .getModelsForProvider(provider.id)
                                        .firstOrNull()?.id ?: selectedModelId
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedProviderId == provider.id,
                                onClick = {
                                    selectedProviderId = provider.id
                                    selectedModelId = ModelRegistry
                                        .getModelsForProvider(provider.id)
                                        .firstOrNull()?.id ?: selectedModelId
                                }
                            )
                            Column {
                                Text(
                                    text = provider.displayName,
                                    style = CcTheme.tokens.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = provider.defaultEndpoint,
                                    style = CcTheme.tokens.typography.bodySmall,
                                    color = colors.textTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Modellauswahl:",
                        style = CcTheme.tokens.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    val models = remember(selectedProviderId) {
                        ModelRegistry.getModelsForProvider(selectedProviderId)
                    }
                    models.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedModelId = model.id }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedModelId == model.id,
                                onClick = { selectedModelId = model.id }
                            )
                            Text(
                                text = "${model.displayName} (${model.id})",
                                style = CcTheme.tokens.typography.bodySmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.onProviderSelected(selectedProviderId, selectedModelId)
                        viewModel?.onDismissModelDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    )
                ) {
                    Text("Auswählen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel?.onDismissModelDialog() }) {
                    Text("Abbrechen", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Voice Input & Conversation Mode Info Dialog ──
    if (s.showVoiceInfoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel?.onDismissVoiceInfoDialog() },
            title = {
                Text(
                    text = "Sprachmodus",
                    style = CcTheme.tokens.typography.titleMedium,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Auf diesem Gerät ist kein lokales Speech-to-Text-Modell (STT) oder Mikrofon-Streamer eingerichtet. Sprachnachrichten können stattdessen als Audioanhang über das Menü „Anhänge“ hinzugefügt werden.",
                    style = CcTheme.tokens.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel?.onDismissVoiceInfoDialog() }) {
                    Text("Verstanden", color = colors.accent)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── User Profile Dialog ──
    if (s.showProfileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel?.onDismissProfileDialog() },
            title = {
                Text(
                    text = "Benutzerprofil",
                    style = CcTheme.tokens.typography.titleMedium,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Angemeldet als: Mert",
                        style = CcTheme.tokens.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Konto: mert.govse.neu@gmail.com",
                        style = CcTheme.tokens.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Lizenz: ClauDroide Pro (BYOK)",
                        style = CcTheme.tokens.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel?.onDismissProfileDialog() }) {
                    Text("Schließen", color = colors.accent)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Floating bottom composer card matching Claude Android.
 */
@Composable
private fun MessageComposer(
    input: ChatInputState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onModeClick: () -> Unit,
    mode: ComposerMode,
    viewModel: ChatViewModel? = null,
) {
    val colors = CcTheme.tokens.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Staged attachments preview
                if (input.attachments.isNotEmpty()) {
                    AttachmentPreviewRow(
                        attachments = input.attachments,
                        onRemove = { attachmentId ->
                            viewModel?.removeAttachment(attachmentId)
                        },
                    )
                }

                // Clean multiline input text area
                TextField(
                    value = input.text,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.chat_composer_hint),
                            style = CcTheme.tokens.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent,
                    ),
                    textStyle = CcTheme.tokens.typography.bodyMedium,
                    maxLines = 6,
                )

                // Bottom row: + attachment, model pill, and action / voice button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Circular + button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSubtle)
                                .clickable(onClick = onAttachmentClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.chat_composer_attachment),
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Model / Provider Selector Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceSubtle,
                            modifier = Modifier.clickable(onClick = onModeClick),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = input.targetProviderName,
                                    style = CcTheme.tokens.typography.labelSmall,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Right controls: Voice / Mic button or Send / Stop button
                    val action = input.actionButtonState
                    if (input.text.isNotBlank() || action == InputActionButtonState.STOP) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    onClick = if (action == InputActionButtonState.STOP) onStopClick else onSendClick
                                ),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (action == InputActionButtonState.STOP) {
                                        Icons.Filled.Stop
                                    } else {
                                        Icons.Filled.ArrowUpward
                                    },
                                    contentDescription = stringResource(
                                        if (action == InputActionButtonState.STOP) R.string.chat_stop else R.string.chat_send
                                    ),
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceSubtle)
                                    .clickable(onClick = { viewModel?.onVoiceClick() }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice input",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable(onClick = { viewModel?.onVoiceClick() }),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Voice mode",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewRow(
    attachments: List<AttachmentItem>,
    onRemove: (String) -> Unit,
) {
    val colors = CcTheme.tokens.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceSubtle,
                modifier = Modifier.height(34.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = attachment.name,
                        style = CcTheme.tokens.typography.bodySmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onRemove(attachment.id) },
                    )
                }
            }
        }
    }
}

/**
 * Shown while a response is still arriving.
 */
@Composable
private fun StreamingIndicator() {
    val colors = CcTheme.tokens.colors

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceSubtle,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colors.accent, CircleShape),
            )
            Text(
                text = stringResource(R.string.chat_streaming),
                style = CcTheme.tokens.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}
