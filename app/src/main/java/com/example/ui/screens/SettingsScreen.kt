package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BackgroundWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandAccentHighlight
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isAutonomous by viewModel.isAutonomousMode.collectAsStateWithLifecycle()
    val accessibleModels by viewModel.accessibleModels.collectAsStateWithLifecycle()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }

    var isTtsEnabled by remember { mutableStateOf(viewModel.prefsManager.isTtsEnabled) }
    var isDirectSms by remember { mutableStateOf(viewModel.prefsManager.isDirectSmsEnabled) }
    var isDirectCall by remember { mutableStateOf(viewModel.prefsManager.isDirectCallEnabled) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = BackgroundWhite,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("btn_settings_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Dimens.MaxContentWidth),
                contentPadding = PaddingValues(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing24)
            ) {
                // Group 1: Groq AI Engine
                item {
                    SettingsGroupHeader("AI Engine & Model")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerMedium))
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, RoundedCornerShape(Dimens.CornerMedium))
                    ) {
                        SettingsClickableRow(
                            icon = Icons.Default.Key,
                            title = "Groq API Key",
                            subtitle = if (viewModel.prefsManager.hasGroqApiKey()) "Configured & Keystore Encrypted" else "Not Configured",
                            onClick = {
                                apiKeyInput = viewModel.prefsManager.getGroqApiKey() ?: ""
                                showApiKeyDialog = true
                            }
                        )

                        SettingsDivider()

                        Box {
                            SettingsClickableRow(
                                icon = Icons.Default.Psychology,
                                title = "Active Model",
                                subtitle = selectedModel,
                                onClick = { showModelMenu = true }
                            )

                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                val models = accessibleModels.ifEmpty {
                                    listOf(
                                        "llama-3.3-70b-versatile",
                                        "llama-3.1-8b-instant",
                                        "mixtral-8x7b-32768"
                                    )
                                }
                                models.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        trailingIcon = if (model == selectedModel) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = StatusSuccess) }
                                        } else null,
                                        onClick = {
                                            viewModel.selectModel(model)
                                            showModelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Group 2: Autonomous Actions
                item {
                    SettingsGroupHeader("Autonomous Execution")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerMedium))
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, RoundedCornerShape(Dimens.CornerMedium))
                    ) {
                        SettingsSwitchRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "Autonomous Mode",
                            subtitle = "Execute calendar events, alarms, and searches directly without prompt confirmation.",
                            checked = isAutonomous,
                            onCheckedChange = { viewModel.setAutonomousMode(it) }
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = Icons.Default.Message,
                            title = "Direct SMS Dispatch",
                            subtitle = "Send text messages directly using SmsManager when recipient is unambiguous.",
                            checked = isDirectSms,
                            onCheckedChange = {
                                isDirectSms = it
                                viewModel.setDirectSms(it)
                            }
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = Icons.Default.Call,
                            title = "Direct Phone Dialing",
                            subtitle = "Place phone calls directly when recipient phone number is resolved.",
                            checked = isDirectCall,
                            onCheckedChange = {
                                isDirectCall = it
                                viewModel.setDirectCall(it)
                            }
                        )
                    }
                }

                // Group 3: Voice & Audio
                item {
                    SettingsGroupHeader("Voice & Audio")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerMedium))
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, RoundedCornerShape(Dimens.CornerMedium))
                    ) {
                        SettingsSwitchRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Speech Output (TTS)",
                            subtitle = "Speak assistant replies aloud using Android Text-to-Speech.",
                            checked = isTtsEnabled,
                            onCheckedChange = {
                                isTtsEnabled = it
                                viewModel.setTts(it)
                            }
                        )
                    }
                }

                // Group 4: Permissions & System
                item {
                    SettingsGroupHeader("System & Privacy")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerMedium))
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, RoundedCornerShape(Dimens.CornerMedium))
                    ) {
                        SettingsNavigationRow(
                            icon = Icons.Default.Security,
                            title = "System Permissions & Roles",
                            subtitle = "Inspect and grant microphone, calendar, contacts, and default assistant role.",
                            onClick = onNavigateToPermissions
                        )

                        SettingsDivider()

                        SettingsNavigationRow(
                            icon = Icons.Default.Tune,
                            title = "System Diagnostics",
                            subtitle = "View network status, model access, and action execution audit history.",
                            onClick = onNavigateToDiagnostics
                        )

                        SettingsDivider()

                        SettingsNavigationRow(
                            icon = Icons.Default.Shield,
                            title = "Privacy & Security",
                            subtitle = "Review encryption guarantees and manage or clear local message storage.",
                            onClick = onNavigateToPrivacy
                        )

                        SettingsDivider()

                        SettingsNavigationRow(
                            icon = Icons.Default.Refresh,
                            title = "Setup Wizard",
                            subtitle = "Re-run the initial assistant onboarding checklist.",
                            onClick = onNavigateToSetup
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.Spacing32))
                }
            }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = BackgroundWhite,
            shape = RoundedCornerShape(Dimens.CornerMedium),
            title = {
                Text(
                    text = "Groq API Key",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your API key is stored securely using hardware-backed Android Keystore AES-256-GCM encryption.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing16))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gsk_...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            viewModel.saveGroqApiKey(apiKeyInput.trim())
                            Toast.makeText(context, "API Key saved securely", Toast.LENGTH_SHORT).show()
                        }
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(start = Dimens.Spacing4)
    )
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimens.Spacing16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceCanvas),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(Dimens.IconMedium))
        }

        Spacer(modifier = Modifier.width(Dimens.Spacing12))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SettingsClickableRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.Spacing16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceCanvas),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(Dimens.IconMedium))
        }

        Spacer(modifier = Modifier.width(Dimens.Spacing12))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        }

        Spacer(modifier = Modifier.width(Dimens.Spacing8))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandAccentHighlight,
                uncheckedTrackColor = SurfaceCanvas
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.BorderThin)
            .background(BorderSubtle)
    )
}
