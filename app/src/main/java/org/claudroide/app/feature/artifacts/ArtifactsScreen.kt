package org.claudroide.app.feature.artifacts

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.claudroide.app.core.design.theme_compat.CcTheme
import org.claudroide.app.feature.media.MediaBridge
import org.claudroide.app.feature.media.MediaGenerationResult
import java.io.File

/**
 * Interactive Artifacts & Media Gallery screen.
 * Displays generated media assets, diagrams, and codebase artifacts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToMediaStudio: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = CcTheme.tokens.colors
    val mediaBridge = remember { MediaBridge(context) }
    var selectedFilter by remember { mutableStateOf(0) } // 0: Bilder & Medien, 1: Dokumente & Code

    val mediaFiles = remember {
        val dir = File(context.filesDir, "generated_media")
        dir.listFiles()?.filter { it.extension in listOf("png", "jpg", "webp") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Artefakte & Medien-Galerie",
                            style = CcTheme.tokens.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "${mediaFiles.size} Mediendateien generiert",
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
                    IconButton(onClick = onNavigateToMediaStudio) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Neues Bild generieren",
                            tint = colors.accent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = selectedFilter,
                containerColor = colors.surfaceSubtle,
                contentColor = colors.accent,
                divider = { HorizontalDivider(color = colors.border) },
            ) {
                Tab(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    text = { Text("Bilder & Render-Assets", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    text = { Text("Reports & Dokumente", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedFilter == 0) {
                if (mediaFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = colors.textTertiary,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Noch keine Bilder generiert",
                                style = CcTheme.tokens.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nutze das Media-Studio, um Bilder via OmniRoute oder Segmind zu erstellen.",
                                style = CcTheme.tokens.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToMediaStudio,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent,
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Media Studio öffnen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(mediaFiles) { file ->
                            val bitmap = remember(file.absolutePath) {
                                BitmapFactory.decodeFile(file.absolutePath)
                            }
                            Surface(
                                color = colors.surfaceSubtle,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = file.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(colors.background.copy(alpha = 0.85f))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = file.name,
                                            style = CcTheme.tokens.typography.labelSmall,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Reports & Documents Tab
                val docList = listOf(
                    Triple("ClauDroide Master Specification", "claudroide-spec.md", "135/135 Tasks vollendet"),
                    Triple("Release Readiness Audit", "reports/RELEASE_READINESS_AUDIT.md", "Votum: CONDITIONAL GO"),
                    Triple("Feature Traceability Matrix", "reports/FEATURE_TRACEABILITY_MATRIX.csv", "135 verifizierte Tasks"),
                    Triple("Crash & Regression Audit", "reports/CRASH_AND_REGRESSION_AUDIT.md", "5 behobene Crash-Ursachen")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(docList) { (title, path, badge) ->
                        Surface(
                            color = colors.surfaceSubtle,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Article,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = CcTheme.tokens.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = path,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = colors.textTertiary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.success.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        style = CcTheme.tokens.typography.labelSmall,
                                        color = colors.success,
                                        fontWeight = FontWeight.Bold,
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
