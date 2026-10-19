package org.claudroide.app.feature.media

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.claudroide.app.core.design.theme_compat.CcTheme
import java.io.File

/**
 * High-fidelity Jetpack Compose implementation of Google Stitch's "Bild-Studio & Parameter" screen.
 * Connects directly to OmniRoute, Segmind, and Pollinations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaStudioScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = CcTheme.tokens.colors
    val scope = rememberCoroutineScope()
    val mediaBridge = remember { MediaBridge(context) }

    var promptText by remember { mutableStateOf("Cute green Android bot and orange Claude bot collaborating in space, starry galaxy, high fidelity") }
    var negativePrompt by remember { mutableStateOf("blurry, low quality, distorted, extra limbs") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var selectedProviderId by remember { mutableStateOf("omniroute") }
    var selectedModel by remember { mutableStateOf("segmind/flux-schnell") }
    var selectedModifiers by remember { mutableStateOf(setOf("Cinematic Kodachrome", "8k Photoreal")) }

    var isGenerating by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<MediaGenerationResult?>(null) }
    var history by remember { mutableStateOf(emptyList<MediaGenerationResult>()) }

    val availableModifiers = listOf(
        "8k Photoreal", "Cinematic Kodachrome", "Minimalist Vector", "Cyberpunk Neon", "App Icon Style", "Watercolor"
    )
    val aspectRatios = listOf("1:1", "16:9", "9:16", "4:3")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Media & Bild-Studio",
                            style = CcTheme.tokens.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "OmniRoute · Segmind · FLUX.1 Engine",
                            style = CcTheme.tokens.typography.bodySmall,
                            color = colors.accent,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh status */ }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Parameters",
                            tint = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Worker & Pipeline Status Card
            item {
                Surface(
                    color = colors.surfaceSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isGenerating) colors.warning else colors.success)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGenerating) "Pipeline Aktiv: Render läuft..." else "Worker Bereit: $selectedModel",
                                style = CcTheme.tokens.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Latenz ~2.8s · Device Safe Cache · 100% Autonom",
                                style = CcTheme.tokens.typography.labelSmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }

            // 2. Active Preview Card (if generated or generating)
            item {
                Surface(
                    color = colors.surfaceSubtle,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isGenerating) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = colors.accent)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Generiere Bild über $selectedProviderId...",
                                    style = CcTheme.tokens.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        } else if (currentResult?.localFilePath != null) {
                            val bitmap = remember(currentResult?.localFilePath) {
                                BitmapFactory.decodeFile(currentResult?.localFilePath)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = currentResult?.prompt,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "Fehler beim Laden des gerenderten Bildes",
                                    color = colors.danger,
                                    style = CcTheme.tokens.typography.bodySmall,
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = colors.textTertiary,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Bereit für Render-Auftrag",
                                    style = CcTheme.tokens.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // 3. Prompt Input Box
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bildbeschreibung & Prompt",
                        style = CcTheme.tokens.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceSubtle,
                            unfocusedContainerColor = colors.surfaceSubtle,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                        ),
                        minLines = 3,
                        maxLines = 5,
                    )
                }
            }

            // 4. Style Modifier Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stil-Modifikatoren",
                        style = CcTheme.tokens.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(availableModifiers) { mod ->
                            val isSelected = selectedModifiers.contains(mod)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedModifiers = if (isSelected) {
                                        selectedModifiers - mod
                                    } else {
                                        selectedModifiers + mod
                                    }
                                },
                                label = { Text(mod, style = CcTheme.tokens.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accent,
                                    selectedLabelColor = colors.onAccent,
                                    containerColor = colors.surfaceSubtle,
                                    labelColor = colors.textPrimary,
                                )
                            )
                        }
                    }
                }
            }

            // 5. Aspect Ratio Selectors
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Seitenverhältnis",
                        style = CcTheme.tokens.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        aspectRatios.forEach { ratio ->
                            val isSelected = selectedRatio == ratio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accent else colors.surfaceSubtle)
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .clickable { selectedRatio = ratio }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = ratio,
                                    style = CcTheme.tokens.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.onAccent else colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // 6. Action Button (Generate)
            item {
                Button(
                    onClick = {
                        if (!isGenerating && promptText.isNotBlank()) {
                            isGenerating = true
                            scope.launch {
                                val req = MediaGenerationRequest(
                                    prompt = promptText,
                                    negativePrompt = negativePrompt,
                                    providerId = selectedProviderId,
                                    modelId = selectedModel,
                                    aspectRatio = selectedRatio,
                                    modifiers = selectedModifiers.toList(),
                                )
                                val res = mediaBridge.generateImage(req)
                                currentResult = res
                                history = mediaBridge.getHistory()
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                    enabled = !isGenerating && promptText.isNotBlank(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Text(
                            text = if (isGenerating) "Generierung läuft..." else "Render Starten (FLUX / Segmind)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
