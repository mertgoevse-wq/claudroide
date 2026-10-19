package org.claudroide.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.claudroide.app.R
import org.claudroide.app.core.design.TypeTokens

/**
 * Options presented in the attachment and context bottom sheet.
 * Aligned with the Stitch design specification (claudroide_anhang_men).
 */
enum class AttachmentOption(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val badgeText: String? = null,
) {
    FILES_AND_CODE(
        titleRes = R.string.chat_attachment_files_code,
        descriptionRes = R.string.chat_attachment_files_code_desc,
        icon = Icons.Default.Folder,
        badgeText = ".kt, .json",
    ),
    PHOTOS_AND_DOCS(
        titleRes = R.string.chat_attachment_photos_docs,
        descriptionRes = R.string.chat_attachment_photos_docs_desc,
        icon = Icons.Default.PhotoLibrary,
    ),
    CAMERA(
        titleRes = R.string.chat_attachment_camera,
        descriptionRes = R.string.chat_attachment_camera_desc,
        icon = Icons.Default.PhotoCamera,
    ),
    PROJECT_CONTEXT(
        titleRes = R.string.chat_attachment_project_context,
        descriptionRes = R.string.chat_attachment_project_context_desc,
        icon = Icons.Default.AccountTree,
        badgeText = "Lokal",
    ),
    LOGCAT(
        titleRes = R.string.chat_attachment_logcat,
        descriptionRes = R.string.chat_attachment_logcat_desc,
        icon = Icons.Default.BugReport,
        badgeText = "ADB",
    ),
}

/**
 * Modal bottom sheet for attaching files, media, project context, and logcat traces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismissRequest: () -> Unit,
    onOptionSelected: (AttachmentOption) -> Unit,
    modifier: Modifier = Modifier,
    onManageContextClick: () -> Unit = {},
    tokenFreeFormatted: String = "42k",
    tokenTotalFormatted: String = "200k",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
        dragHandle = null,
        modifier = modifier,
    ) {
        AttachmentBottomSheetContent(
            onClose = onDismissRequest,
            onOptionClick = onOptionSelected,
            onManageContextClick = onManageContextClick,
            tokenFreeFormatted = tokenFreeFormatted,
            tokenTotalFormatted = tokenTotalFormatted,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/**
 * Pure layout content of the attachment bottom sheet, facilitating standalone testing and previewing.
 */
@Composable
fun AttachmentBottomSheetContent(
    onClose: () -> Unit,
    onOptionClick: (AttachmentOption) -> Unit,
    modifier: Modifier = Modifier,
    onManageContextClick: () -> Unit = {},
    tokenFreeFormatted: String = "42k",
    tokenTotalFormatted: String = "200k",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        // Top grip handle & Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TypeTokens.SpacingMedium, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TypeTokens.SpacingSmall),
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.chat_attachment_title),
                        style = TypeTokens.LabelMediumStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.chat_attachment_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Attachment options list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TypeTokens.SpacingSmall, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AttachmentOption.entries.forEach { option ->
                AttachmentOptionRow(
                    option = option,
                    onClick = { onOptionClick(option) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Context Preload Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TypeTokens.SpacingMedium, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.chat_attachment_context_memory,
                            tokenFreeFormatted,
                            tokenTotalFormatted,
                        ),
                        style = TypeTokens.CodeSmallStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.chat_attachment_manage),
                    style = TypeTokens.CodeSmallStyle.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onManageContextClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun AttachmentOptionRow(
    option: AttachmentOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(option.titleRes),
                        style = TypeTokens.BodyMediumStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    option.badgeText?.let { badge ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = badge,
                                style = TypeTokens.CodeSmallStyle.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(option.descriptionRes),
                    style = TypeTokens.BodySmallStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
