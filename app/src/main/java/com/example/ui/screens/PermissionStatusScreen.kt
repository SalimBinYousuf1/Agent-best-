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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.StatusDestructiveSubtle
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

enum class PermissionDisplayState(val label: String) {
    ENABLED("Enabled"),
    NEEDS_SETUP("Needs setup"),
    DENIED("Denied"),
    NOT_AVAILABLE("Not available")
}

@Composable
fun PermissionStatusScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionStatuses by viewModel.permissionStatuses.collectAsStateWithLifecycle()
    val roleStatuses by viewModel.roleStatuses.collectAsStateWithLifecycle()

    val canExactAlarm = viewModel.permissionManager.canScheduleExactAlarms()
    val isBatteryExempt = viewModel.permissionManager.isIgnoringBatteryOptimizations()

    // Activity result launcher for runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemStatuses()
    }

    // Role launcher
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatuses()
    }

    // Refresh on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("permission_status_screen"),
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
                    modifier = Modifier.testTag("btn_permissions_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "System Permissions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.refreshSystemStatuses() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = BrandAccent
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
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing20)
            ) {
                // Section 1: System Roles
                item {
                    Text(
                        text = "Android System Roles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing10)) {
                        roleStatuses.forEach { role ->
                            val state = when {
                                role.isHeld -> PermissionDisplayState.ENABLED
                                role.isAvailable -> PermissionDisplayState.NEEDS_SETUP
                                else -> PermissionDisplayState.NOT_AVAILABLE
                            }

                            val icon = when (role.roleName) {
                                "Default Assistant App" -> Icons.Default.Star
                                "Default SMS App" -> Icons.Default.Message
                                else -> Icons.Default.Call
                            }

                            ModernPermissionRow(
                                title = role.roleName,
                                description = role.rationale,
                                icon = icon,
                                state = state,
                                onActionClick = {
                                    if (role.roleName == "Default Assistant App") {
                                        viewModel.permissionManager.openVoiceAssistantSettings(context)
                                    } else {
                                        viewModel.permissionManager.requestRole(context as Activity, role.roleName, roleLauncher)
                                    }
                                }
                            )
                        }
                    }
                }

                // Section 2: Core Hardware & Contacts
                item {
                    Text(
                        text = "Voice & Data Permissions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing10)) {
                        permissionStatuses.forEach { perm ->
                            val state = if (perm.isGranted) PermissionDisplayState.ENABLED else PermissionDisplayState.NEEDS_SETUP
                            val icon = when (perm.name) {
                                "Microphone" -> Icons.Default.Mic
                                "Contacts" -> Icons.Default.Contacts
                                "Read Calendar", "Write Calendar" -> Icons.Default.CalendarToday
                                "Direct SMS" -> Icons.Default.Message
                                "Direct Phone Call" -> Icons.Default.Call
                                else -> Icons.Default.Notifications
                            }

                            ModernPermissionRow(
                                title = perm.name,
                                description = perm.rationale,
                                icon = icon,
                                state = state,
                                onActionClick = {
                                    permissionLauncher.launch(arrayOf(perm.permission))
                                }
                            )
                        }
                    }
                }

                // Section 3: Exact Alarms & Battery
                item {
                    Text(
                        text = "Power & Scheduling",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing10)) {
                        ModernPermissionRow(
                            title = "Exact Alarms",
                            description = "Required to schedule reliable on-the-minute alarms and timer wakeups.",
                            icon = Icons.Default.Alarm,
                            state = if (canExactAlarm) PermissionDisplayState.ENABLED else PermissionDisplayState.NEEDS_SETUP,
                            onActionClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )

                        ModernPermissionRow(
                            title = "Battery Optimization Exemption",
                            description = "Prevents Android from freezing assistant timers and background processes.",
                            icon = Icons.Default.BatteryChargingFull,
                            state = if (isBatteryExempt) PermissionDisplayState.ENABLED else PermissionDisplayState.NEEDS_SETUP,
                            onActionClick = {
                                viewModel.permissionManager.requestIgnoreBatteryOptimization(context)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.Spacing24))
                }
            }
        }
    }
}

@Composable
fun ModernPermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    state: PermissionDisplayState,
    onActionClick: () -> Unit
) {
    val shape = RoundedCornerShape(Dimens.CornerMedium)

    val (badgeBg, badgeText) = when (state) {
        PermissionDisplayState.ENABLED -> StatusSuccessSubtle to StatusSuccess
        PermissionDisplayState.NEEDS_SETUP -> StatusWarningSubtle to StatusWarning
        PermissionDisplayState.DENIED -> StatusDestructiveSubtle to StatusDestructive
        PermissionDisplayState.NOT_AVAILABLE -> SurfaceCanvas to TextTertiary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCardWhite)
            .border(Dimens.BorderThin, BorderLight, shape)
            .padding(Dimens.Spacing16)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.Spacing12))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
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

                Spacer(modifier = Modifier.width(Dimens.Spacing8))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.CornerSmall))
                        .background(badgeBg)
                        .padding(horizontal = Dimens.Spacing8, vertical = 3.dp)
                ) {
                    Text(
                        text = state.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (state != PermissionDisplayState.ENABLED && state != PermissionDisplayState.NOT_AVAILABLE) {
                Spacer(modifier = Modifier.height(Dimens.Spacing12))
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(Dimens.CornerSmall)
                ) {
                    Text("Enable / Configure", style = MaterialTheme.typography.labelMedium.copy(color = BrandAccent))
                }
            }
        }
    }
}
