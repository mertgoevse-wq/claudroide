package org.claudroide.app.feature.control.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Execution phases for live human visibility into autonomous agent work.
 */
enum class AgentExecutionStatus(val label: String, val color: Color) {
    IDLE("Ready", Color(0xFF6B7280)),
    OBSERVING("Observing screen", Color(0xFF3B82F6)),
    PLANNING("Planning next step", Color(0xFF8B5CF6)),
    ACTING("Executing action", Color(0xFF10B981)),
    VERIFYING("Verifying outcome", Color(0xFFF59E0B)),
    PAUSED("Paused by user", Color(0xFFEF4444)),
    STOPPED("Halted", Color(0xFF9CA3AF)),
    ERROR("Action error", Color(0xFFDC2626))
}

/**
 * Observable UI state for the live agent inspection layer.
 */
data class AgentStatusOverlayState(
    val isVisible: Boolean = false,
    val status: AgentExecutionStatus = AgentExecutionStatus.IDLE,
    val currentTask: String = "",
    val currentAction: String = "",
    val observation: String = "",
    val nextAction: String = "",
    val isPaused: Boolean = false,
    val lastError: String? = null
)

/**
 * Live agent execution card showing the user exactly what the AI agent is doing,
 * what it observes, what it plans next, and provides an immediate Emergency Stop button.
 */
@Composable
fun AgentWorkVisibilityCard(
    state: AgentStatusOverlayState,
    onPauseToggle: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Status indicator dot + phase title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(state.status.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agent: ${state.status.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = state.status.color
                        )
                    }

                    // Emergency Stop & Pause controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onPauseToggle,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (state.isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (state.isPaused) "Resume" else "Pause", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = onEmergencyStop,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Emergency Stop",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Task
                if (state.currentTask.isNotBlank()) {
                    StatusRow(label = "Current task:", value = state.currentTask, isBold = true)
                }

                // Current Action
                if (state.currentAction.isNotBlank()) {
                    StatusRow(label = "Current action:", value = state.currentAction)
                }

                // Observation
                if (state.observation.isNotBlank()) {
                    StatusRow(label = "Observation:", value = state.observation)
                }

                // Next Step
                if (state.nextAction.isNotBlank()) {
                    StatusRow(label = "Next:", value = state.nextAction)
                }

                // Error alert
                if (state.lastError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notice: ${state.lastError}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
