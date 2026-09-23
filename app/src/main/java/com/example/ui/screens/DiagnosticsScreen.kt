package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ActionHistoryEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val accessibleModels by viewModel.accessibleModels.collectAsStateWithLifecycle()
    val actionHistory by viewModel.actionHistory.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("diagnostics_screen"),
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
                    modifier = Modifier.testTag("btn_diagnostics_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "System Diagnostics",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        viewModel.refreshSystemStatuses()
                        viewModel.refreshGroqModels()
                    }
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
                // Section 1: Health & Connectivity
                item {
                    Text(
                        text = "System Health & Network",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    val cardShape = RoundedCornerShape(Dimens.CornerMedium)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, cardShape)
                    ) {
                        DiagnosticRow(
                            icon = Icons.Default.NetworkCheck,
                            title = "Internet Connectivity",
                            subtitle = if (isOnline) "Connected to Active Network" else "No Network Connection",
                            isHealthy = isOnline
                        )

                        SettingsDivider()

                        DiagnosticRow(
                            icon = Icons.Default.Security,
                            title = "Groq API Authentication",
                            subtitle = if (viewModel.prefsManager.hasGroqApiKey()) "API Key Present & Encrypted" else "No API Key Configured",
                            isHealthy = viewModel.prefsManager.hasGroqApiKey()
                        )

                        SettingsDivider()

                        DiagnosticRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Active Model",
                            subtitle = selectedModel,
                            isHealthy = true
                        )
                    }
                }

                // Section 2: Accessible Groq Models
                item {
                    Text(
                        text = "Accessible Groq Models (${accessibleModels.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    val cardShape = RoundedCornerShape(Dimens.CornerMedium)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(SurfaceCardWhite)
                            .border(Dimens.BorderThin, BorderLight, cardShape)
                            .padding(Dimens.Spacing16)
                    ) {
                        if (accessibleModels.isEmpty()) {
                            Text(
                                text = "Models will be loaded once a valid Groq API key is configured.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        } else {
                            accessibleModels.take(6).forEach { model ->
                                Text(
                                    text = "• $model",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (model == selectedModel) BrandAccent else TextSecondary,
                                        fontWeight = if (model == selectedModel) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Section 3: Action Execution History
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Action Audit Trail (${actionHistory.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (actionHistory.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearAllActionHistory() },
                                shape = RoundedCornerShape(Dimens.CornerSmall)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Log", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    if (actionHistory.isEmpty()) {
                        val cardShape = RoundedCornerShape(Dimens.CornerMedium)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(SurfaceCardWhite)
                                .border(Dimens.BorderThin, BorderLight, cardShape)
                                .padding(Dimens.Spacing24),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No autonomous actions executed yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8)) {
                            actionHistory.take(10).forEach { item ->
                                ActionHistoryRow(item)
                            }
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
fun DiagnosticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isHealthy: Boolean
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
                .background(if (isHealthy) StatusSuccessSubtle else StatusDestructiveSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHealthy) StatusSuccess else StatusDestructive,
                modifier = Modifier.size(Dimens.IconMedium)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.Spacing12))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.CornerSmall))
                .background(if (isHealthy) StatusSuccessSubtle else StatusDestructiveSubtle)
                .padding(horizontal = Dimens.Spacing8, vertical = 3.dp)
        ) {
            Text(
                text = if (isHealthy) "Healthy" else "Notice",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isHealthy) StatusSuccess else StatusDestructive,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun ActionHistoryRow(item: ActionHistoryEntity) {
    val shape = RoundedCornerShape(Dimens.CornerMedium)
    val dateFormat = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
    val isSuccess = item.status == "AUTONOMOUS" || item.status == "COMPLETED"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCardWhite)
            .border(Dimens.BorderThin, BorderLight, shape)
            .padding(Dimens.Spacing12)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.actionType.replace("_", " "),
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.CornerSmall))
                        .background(if (isSuccess) StatusSuccessSubtle else StatusWarningSubtle)
                        .padding(horizontal = Dimens.Spacing6, vertical = 2.dp)
                ) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSuccess) StatusSuccess else StatusWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Target: ${item.target}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1
            )

            if (item.executionMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.executionMessage,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateFormat.format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
            )
        }
    }
}
