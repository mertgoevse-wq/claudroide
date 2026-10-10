package org.claudroide.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.claudroide.app.R
import org.claudroide.app.core.design.TypeTokens
import org.claudroide.app.core.design.components.CodeBlock
import org.claudroide.app.core.design.components.EmptyState
import org.claudroide.app.core.design.components.MessageBubble
import org.claudroide.app.feature.provider.ProviderCatalogRegistry

/**
 * One entry in the transcript.
 *
 * `code` is separate from `text` rather than embedded in it: a fenced block
 * inside a paragraph cannot be given the monospaced surface, the darker
 * background and the horizontal padding it needs, and a code sample squeezed
 * into proportional text is the single most common way an assistant UI stops
 * being readable.
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
    viewModel: ChatViewModel? = null,
) {
    val s = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: ChatUiState()
    val effectiveMessages = s.messages.ifEmpty { messages }
    val effectiveInput = s.input.copy(
        targetProviderName = ProviderCatalogRegistry.getProvider(s.currentProviderId)
            ?.displayName ?: input.targetProviderName
    )
    val effectiveError = s.error

    val listState = rememberLazyListState()
    var showAttachmentSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.chat_session_title),
                            style = TypeTokens.TitleLargeStyle, // 16sp / 24sp, medium per spec
                        )
                        Text(
                            text = effectiveInput.targetProviderName,
                            style = TypeTokens.BodySmallStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { /* Open navigation drawer - TODO */ },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.nav_chat),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel?.newConversation() ?: onNewChatClick() },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.chat_new_conversation),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (effectiveMessages.isEmpty()) {
                FilledIconButton(
                    onClick = { viewModel?.newConversation() ?: onNewChatClick() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.chat_new_conversation),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (effectiveMessages.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = stringResource(R.string.empty_chat_title),
                        description = stringResource(R.string.empty_chat_desc),
                        action = {
                            FilledIconButton(
                                onClick = { viewModel?.newConversation() ?: onNewChatClick() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.chat_new_conversation),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.chat_new_conversation),
                                    style = TypeTokens.LabelLargeStyle,
                                )
                            }
                        },
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = TypeTokens.SpacingMedium,
                        vertical = TypeTokens.SpacingMedium,
                    ),
                    verticalArrangement = Arrangement.spacedBy(TypeTokens.SpacingMedium),
                ) {
                    items(effectiveMessages, key = { it.id }) { message ->
                        MessageBubble(
                            text = message.text,
                            mine = message.fromUser,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 320.dp),
                            footer = message.code?.let { code ->
                                { CodeBlock(code = code, modifier = Modifier.fillMaxWidth()) }
                            },
                        )
                    }
                    if (s.isStreaming && effectiveMessages.lastOrNull()?.isStreaming == true) {
                        item(key = "streaming") { StreamingIndicator() }
                    }
                }
            }

            if (effectiveError != null) {
                Text(
                    text = effectiveError,
                    style = TypeTokens.BodySmallStyle,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = TypeTokens.SpacingMedium)
                )
            }

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
}

/**
 * Shown while a response is still arriving.
 *
 * It states that work is happening. A spinner alone says "the app is busy";
 * this says the answer is arriving and offers the way out of it.
 */
@Composable
private fun StreamingIndicator() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TypeTokens.SpacingSmall),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
                text = stringResource(R.string.chat_streaming),
                style = TypeTokens.BodyMediumStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = TypeTokens.SpacingMedium, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(TypeTokens.SpacingSmall),
        ) {
            // Attachment preview row
            if (input.attachments.isNotEmpty()) {
                AttachmentPreviewRow(
                    attachments = input.attachments,
                    onRemove = { attachmentId ->
                        viewModel?.removeAttachment(attachmentId)
                    }
                )
            }

            TextField(
                value = input.text,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 0.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_composer_hint),
                        style = TypeTokens.BodyMediumStyle,
                    )
                },
                leadingIcon = {
                    // Attachment button
                    IconButton(onClick = onAttachmentClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.chat_composer_attachment),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Mode selector button
                        IconButton(
                            onClick = onModeClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = stringResource(R.string.chat_composer_mode),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // Send/Stop action button
                        val action = input.actionButtonState
                        FilledIconButton(
                            onClick = when (action) {
                                InputActionButtonState.STOP -> onStopClick
                                InputActionButtonState.SEND -> onSendClick
                                InputActionButtonState.DISABLED -> ({ })
                            },
                            enabled = action != InputActionButtonState.DISABLED,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = if (action == InputActionButtonState.STOP) {
                                    Icons.Filled.Stop
                                } else {
                                    Icons.Filled.ArrowUpward
                                },
                                contentDescription = stringResource(
                                    when (action) {
                                        InputActionButtonState.STOP -> R.string.chat_stop
                                        else -> R.string.chat_send
                                    }
                                ),
                            )
                        }
                    }
                },
                maxLines = 6,
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
            Text(
                text = stringResource(R.string.chat_composer_helper),
                style = TypeTokens.BodySmallStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = TypeTokens.SpacingXSmall),
            )
        }
    }
}

@Composable
private fun AttachmentPreviewRow(
    attachments: List<AttachmentItem>,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TypeTokens.SpacingXSmall, vertical = TypeTokens.SpacingXSmall),
        horizontalArrangement = Arrangement.spacedBy(TypeTokens.SpacingSmall),
    ) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(TypeTokens.CornerRadiusSmall),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .height(32.dp)
                    .padding(end = TypeTokens.SpacingXSmall),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = TypeTokens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TypeTokens.SpacingXSmall),
                ) {
                    Text(
                        text = attachment.name,
                        style = TypeTokens.BodySmallStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = { onRemove(attachment.id) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}