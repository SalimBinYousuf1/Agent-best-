package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BackgroundWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandAccentHighlight
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSubtle
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSubtle
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AssistantViewModel

data class WizardStep(
    val number: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isComplete: Boolean,
    val actionLabel: String = "Configure",
    val onAction: () -> Unit
)

@Composable
fun SetupWizardScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    onFinishSetup: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAutonomous by viewModel.isAutonomousMode.collectAsStateWithLifecycle()
    val hasApiKey = viewModel.prefsManager.hasGroqApiKey()
    var apiKeyInput by remember { mutableStateOf(viewModel.prefsManager.getGroqApiKey() ?: "") }

    val micGranted = viewModel.permissionManager.isMicrophoneGranted()
    val contactsGranted = viewModel.permissionManager.isContactsGranted()
    val calendarGranted = viewModel.permissionManager.isCalendarGranted()
    val canExactAlarm = viewModel.permissionManager.canScheduleExactAlarms()
    val isAssistantRole = viewModel.permissionManager.isDefaultAssistantApp()

    // Activity result launcher for permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemStatuses()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatuses()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("setup_wizard_screen"),
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
                    modifier = Modifier.testTag("btn_wizard_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Assistant Setup",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundWhite)
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing12),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        viewModel.completeSetup()
                        onFinishSetup()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = Dimens.MaxContentWidth)
                        .height(Dimens.MinTouchTarget)
                        .testTag("btn_complete_setup"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                    shape = RoundedCornerShape(Dimens.CornerMedium)
                ) {
                    Text(
                        text = "Complete & Open Assistant",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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
                contentPadding = PaddingValues(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing12),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16)
            ) {
                // Header Intro
                item {
                    Column(modifier = Modifier.padding(vertical = Dimens.Spacing8)) {
                        Text(
                            text = "Get Started with Salim",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(Dimens.Spacing4))
                        Text(
                            text = "Configure your AI provider and grant permissions to unlock voice, alarms, calendar scheduling, and autonomous actions.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }

                // Step 1: Groq API Key Card
                item {
                    StepCard(
                        number = 1,
                        title = "Groq AI Key",
                        description = "Required for ultra-low latency inference and tool decision making.",
                        icon = Icons.Default.Key,
                        isComplete = hasApiKey
                    ) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("gsk_...", color = TextTertiary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_api_key_input"),
                            singleLine = true,
                            trailingIcon = {
                                if (apiKeyInput.isNotBlank()) {
                                    IconButton(onClick = {
                                        viewModel.saveGroqApiKey(apiKeyInput.trim())
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Save Key", tint = BrandAccentHighlight)
                                    }
                                }
                            }
                        )
                    }
                }

                // Step 2: Voice & Microphone Card
                item {
                    StepCard(
                        number = 2,
                        title = "Voice Input",
                        description = "Enables voice dictation and speech interaction directly through the mic button.",
                        icon = Icons.Default.Mic,
                        isComplete = micGranted,
                        actionLabel = if (micGranted) "Granted" else "Grant Microphone",
                        onAction = {
                            permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                        }
                    )
                }

                // Step 3: Calendar & Contacts Card
                item {
                    StepCard(
                        number = 3,
                        title = "Calendar & Contacts",
                        description = "Allows scheduling calendar events and resolving contact names for messaging or dialing.",
                        icon = Icons.Default.CalendarToday,
                        isComplete = calendarGranted && contactsGranted,
                        actionLabel = if (calendarGranted && contactsGranted) "Granted" else "Grant Access",
                        onAction = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CALENDAR,
                                    android.Manifest.permission.WRITE_CALENDAR,
                                    android.Manifest.permission.READ_CONTACTS
                                )
                            )
                        }
                    )
                }

                // Step 4: System Alarms Card
                item {
                    StepCard(
                        number = 4,
                        title = "Exact Alarms & Timers",
                        description = "Enables scheduling alarms and countdown timers through AlarmClock provider.",
                        icon = Icons.Default.Alarm,
                        isComplete = canExactAlarm,
                        actionLabel = if (canExactAlarm) "Ready" else "Configure Alarms",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                // Step 5: Default Assistant Role Card
                item {
                    StepCard(
                        number = 5,
                        title = "Default Assistant Role",
                        description = "Set Salim as your primary Android voice assistant for home long-press activation.",
                        icon = Icons.Default.Star,
                        isComplete = isAssistantRole,
                        actionLabel = if (isAssistantRole) "Active" else "Set as Default Assistant",
                        onAction = {
                            viewModel.permissionManager.openVoiceAssistantSettings(context)
                        }
                    )
                }

                // Step 6: Autonomous Mode Card
                item {
                    val cardShape = RoundedCornerShape(Dimens.CornerMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, cardShape)
                            .padding(Dimens.Spacing16)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCanvas),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(Dimens.IconMedium))
                            }

                            Spacer(modifier = Modifier.width(Dimens.Spacing12))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "6. Autonomous Execution",
                                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Execute system actions directly with inline status cards rather than extra dialog steps.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimens.Spacing8))

                            Switch(
                                checked = isAutonomous,
                                onCheckedChange = { viewModel.setAutonomousMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandAccentHighlight,
                                    uncheckedTrackColor = SurfaceCanvas
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.Spacing32))
                }
            }
        }
    }
}

@Composable
fun StepCard(
    number: Int,
    title: String,
    description: String,
    icon: ImageVector,
    isComplete: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(Dimens.CornerMedium)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(SurfaceCardWhite)
            .border(Dimens.BorderThin, BorderLight, cardShape)
            .padding(Dimens.Spacing16)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isComplete) StatusSuccessSubtle else SurfaceCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isComplete) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = if (isComplete) StatusSuccess else BrandAccent,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.Spacing12))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$number. $title",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                if (isComplete) {
                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.CornerSmall))
                            .background(StatusSuccessSubtle)
                            .padding(horizontal = Dimens.Spacing8, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Enabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(Dimens.Spacing12))
                extraContent()
            }

            if (!isComplete && onAction != null && actionLabel != null) {
                Spacer(modifier = Modifier.height(Dimens.Spacing12))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(Dimens.CornerSmall)
                ) {
                    Text(actionLabel, color = BrandAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
