package org.claudroide.app.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.claudroide.app.R

enum class OnboardingStep {
    WELCOME,
    LANGUAGE_THEME,
    PROVIDER_EXPLANATION,
    FIRST_PROJECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ersteinrichtung") },
                navigationIcon = {
                    if (currentStep != OnboardingStep.WELCOME) {
                        IconButton(onClick = {
                            currentStep = when (currentStep) {
                                OnboardingStep.LANGUAGE_THEME -> OnboardingStep.WELCOME
                                OnboardingStep.PROVIDER_EXPLANATION -> OnboardingStep.LANGUAGE_THEME
                                OnboardingStep.FIRST_PROJECT -> OnboardingStep.PROVIDER_EXPLANATION
                                else -> OnboardingStep.WELCOME
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onComplete) {
                        Text("Überspringen")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schritt ${currentStep.ordinal + 1} von 4",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            when (currentStep) {
                                OnboardingStep.WELCOME -> currentStep = OnboardingStep.LANGUAGE_THEME
                                OnboardingStep.LANGUAGE_THEME -> currentStep = OnboardingStep.PROVIDER_EXPLANATION
                                OnboardingStep.PROVIDER_EXPLANATION -> currentStep = OnboardingStep.FIRST_PROJECT
                                OnboardingStep.FIRST_PROJECT -> onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (currentStep == OnboardingStep.FIRST_PROJECT) {
                            Text("Loslegen")
                        } else {
                            Text("Weiter")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentStep) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.LANGUAGE_THEME -> LanguageThemeStep()
                OnboardingStep.PROVIDER_EXPLANATION -> ProviderExplanationStep()
                OnboardingStep.FIRST_PROJECT -> FirstProjectStep(onComplete = onComplete)
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Willkommen bei Claudroide",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Dein unabhängiger Coding-Assistent für Android. Entwickelt für die direkte Touch-Bedienung auf deinem Galaxy A56.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = stringResource(R.string.disclaimer_text),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageThemeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sprache & Darstellung",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Claudroide passt sich automatisch an deine Handysprache an. Das dunkle AMOLED-Theme schont den Akku deines A56.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Deutsch (System)") }
            )
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("English") }
            )
        }
    }
}

@Composable
private fun ProviderExplanationStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.VpnKey,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Eigene Schlüssel (BYOK)",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Claudroide ist unabhängig und verkauft keine KI-Nutzung weiter. Du nutzt deine eigenen Schlüssel für Claude API, OpenRouter oder lokale Server.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• Keine versteckten Abos oder Relays\n• Schlüssel bleiben verschlüsselt auf dem A56\n• Du kannst die App auch vorerst ohne Schlüssel erkunden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FirstProjectStep(
    onComplete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.FolderSpecial,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Projektarbeitsbereich",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Öffne ein beliebiges Code-Projekt auf deinem Gerät oder starte direkt mit dem Chat.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Erst erkunden (ohne Projekt)")
        }
    }
}
