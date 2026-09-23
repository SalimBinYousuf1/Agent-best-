package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MessageEntity
import com.example.ui.theme.LabelPrimary
import com.example.ui.theme.LabelSecondary
import com.example.ui.theme.LabelTertiary
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledSurfaceHighest
import com.example.ui.theme.SalimAccent
import com.example.ui.theme.SalimAccentSuccess
import com.example.ui.theme.SalimAccentWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: MessageEntity,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == "USER"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleShape = if (isUser) SquircleMedium else SquircleMedium

        Column(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .then(
                    if (isUser) {
                        Modifier
                            .clip(bubbleShape)
                            .background(SalimAccent.copy(alpha = 0.18f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    } else {
                        Modifier
                            .liquidGlassSurface(
                                shape = bubbleShape,
                                customTint = OledSurfaceElevated
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    }
                )
        ) {
            // Action / Autonomous Status Pill if applicable
            if (!isUser && message.actionStatus != "NONE" && message.actionStatus.isNotBlank()) {
                ActionStatusPill(status = message.actionStatus, actionType = message.actionType)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Message Body Text (Apple Body, generous line height)
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isUser) LabelPrimary else LabelPrimary,
                    lineHeight = 23.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tabular Figure Timestamp
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = LabelTertiary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ActionStatusPill(status: String, actionType: String) {
    val (bgColor, textColor, icon, label) = when (status) {
        "AUTONOMOUS" -> Quad(
            SalimAccentSuccess.copy(alpha = 0.16f),
            SalimAccentSuccess,
            Icons.Default.Bolt,
            "Autonomous Execution"
        )
        "FALLBACK_LAUNCHED" -> Quad(
            SalimAccent.copy(alpha = 0.16f),
            SalimAccent,
            Icons.Default.OpenInNew,
            "Pre-filled for Review"
        )
        "CLARIFICATION" -> Quad(
            SalimAccentWarning.copy(alpha = 0.16f),
            SalimAccentWarning,
            Icons.Default.HelpOutline,
            "Clarification Needed"
        )
        else -> Quad(
            OledSurfaceHighest,
            LabelSecondary,
            Icons.Default.CheckCircle,
            actionType.replace("_", " ")
        )
    }

    Row(
        modifier = Modifier
            .clip(SquirclePill)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontSize = 10.5.sp
            )
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
