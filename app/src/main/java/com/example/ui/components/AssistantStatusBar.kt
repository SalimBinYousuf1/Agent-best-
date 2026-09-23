package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundWhite
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AssistantStatusBar(
    modelName: String,
    isOnline: Boolean,
    onNewChatClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .statusBarsPadding()
            .padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Conversation History button
        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier
                .testTag("btn_conversation_history")
                .defaultMinSize(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = "Conversation History",
                tint = TextPrimary,
                modifier = Modifier.size(Dimens.IconLarge)
            )
        }

        // Center: Assistant name + connection status & model
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Dimens.CornerSmall))
                .clickable(onClick = onStatusClick)
                .padding(vertical = Dimens.Spacing4)
        ) {
            Text(
                text = "Salim Assistant",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online/Offline status dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) StatusSuccess else StatusDestructive)
                )

                Spacer(modifier = Modifier.width(Dimens.Spacing4))

                Text(
                    text = modelName.replace("-versatile", "").replace("llama-", "Llama "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Right side: New Chat button & Settings button (within phone margins)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNewChatClick,
                modifier = Modifier
                    .testTag("btn_new_chat")
                    .defaultMinSize(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "New Chat",
                    tint = TextPrimary,
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .testTag("btn_settings")
                    .defaultMinSize(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(Dimens.IconLarge)
                )
            }
        }
    }
}
