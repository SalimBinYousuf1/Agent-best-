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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.ui.theme.BackgroundWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.StatusDestructiveSubtle
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSubtle
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun PrivacyScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteKeyDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_screen"),
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
                    modifier = Modifier.testTag("btn_privacy_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Privacy & Security",
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
                contentPadding = PaddingValues(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing12),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing20)
            ) {
                // Section 1: Data Guarantees
                item {
                    Text(
                        text = "Data Architecture & Protection",
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
                        PrivacyInfoRow(
                            icon = Icons.Default.Lock,
                            title = "Local Encrypted Storage",
                            description = "Your Groq API key is encrypted using AES-256-GCM backed by the hardware Android Keystore. It is never stored in plaintext."
                        )

                        SettingsDivider()

                        PrivacyInfoRow(
                            icon = Icons.Default.Shield,
                            title = "Zero Cloud Tracking",
                            description = "Messages, conversation logs, and executed actions reside exclusively on your local device in Room SQLite database. No third-party telemetry."
                        )
                    }
                }

                // Section 2: Data Management Actions
                item {
                    Text(
                        text = "Data Management",
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
                        PrivacyActionRow(
                            icon = Icons.Default.DeleteOutline,
                            title = "Clear Current Chat Messages",
                            subtitle = "Permanently removes all messages from the currently active conversation.",
                            isDestructive = false,
                            onClick = { showClearChatDialog = true }
                        )

                        SettingsDivider()

                        PrivacyActionRow(
                            icon = Icons.Default.History,
                            title = "Clear Action Audit History",
                            subtitle = "Deletes recorded logs of autonomous system dispatches and executions.",
                            isDestructive = false,
                            onClick = { showClearHistoryDialog = true }
                        )

                        SettingsDivider()

                        PrivacyActionRow(
                            icon = Icons.Default.KeyOff,
                            title = "Erase Groq API Key",
                            subtitle = "Removes the encrypted credential from the Android Keystore.",
                            isDestructive = true,
                            onClick = { showDeleteKeyDialog = true }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimens.Spacing32))
                }
            }
        }
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            containerColor = BackgroundWhite,
            shape = RoundedCornerShape(Dimens.CornerMedium),
            title = { Text("Clear Current Chat", color = TextPrimary) },
            text = { Text("Are you sure you want to clear all messages in this conversation?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCurrentConversation()
                        showClearChatDialog = false
                        Toast.makeText(context, "Current chat cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = BackgroundWhite,
            shape = RoundedCornerShape(Dimens.CornerMedium),
            title = { Text("Clear Action History", color = TextPrimary) },
            text = { Text("Permanently delete the audit trail of all previous autonomous actions?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllActionHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Audit log cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showDeleteKeyDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteKeyDialog = false },
            containerColor = BackgroundWhite,
            shape = RoundedCornerShape(Dimens.CornerMedium),
            title = { Text("Erase API Key", color = StatusDestructive) },
            text = { Text("This will delete your encrypted Groq API key from Android Keystore. Assistant will stop processing prompts until a new key is added.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroqApiKey()
                        showDeleteKeyDialog = false
                        Toast.makeText(context, "API Key removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDestructive)
                ) {
                    Text("Erase Key", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteKeyDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun PrivacyInfoRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.Spacing16),
        verticalAlignment = Alignment.Top
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
            Text(description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 20.sp))
        }
    }
}

@Composable
fun PrivacyActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean,
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
                .background(if (isDestructive) StatusDestructiveSubtle else SurfaceCanvas),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) StatusDestructive else BrandAccent,
                modifier = Modifier.size(Dimens.IconMedium)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.Spacing12))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = if (isDestructive) StatusDestructive else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        }
    }
}
