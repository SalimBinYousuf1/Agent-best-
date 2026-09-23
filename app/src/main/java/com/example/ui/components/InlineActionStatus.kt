package com.example.ui.components

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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderLight
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.StatusDestructiveSubtle
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSubtle
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSubtle
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun InlineActionStatus(
    actionType: String?,
    actionStatus: String?,
    details: String?,
    modifier: Modifier = Modifier
) {
    if (actionType.isNullOrBlank() && actionStatus.isNullOrBlank()) return

    val icon: ImageVector = when (actionType?.uppercase()) {
        "ALARM" -> Icons.Default.Alarm
        "CALENDAR" -> Icons.Default.CalendarToday
        "SMS" -> Icons.Default.Message
        "CALL" -> Icons.Default.Call
        "APP_LAUNCH" -> Icons.Default.Launch
        "SEARCH_WEB", "OPEN_URL" -> Icons.Default.Search
        else -> Icons.Default.CheckCircle
    }

    val isSuccess = actionStatus == "AUTONOMOUS" || actionStatus == "COMPLETED" || actionStatus == "SUCCESS"
    val isFailed = actionStatus == "FAILED" || actionStatus == "ERROR"
    val isFallback = actionStatus == "SYSTEM_FALLBACK" || actionStatus == "DISPATCHED"

    val statusBgColor: Color = when {
        isSuccess -> StatusSuccessSubtle
        isFailed -> StatusDestructiveSubtle
        isFallback -> StatusWarningSubtle
        else -> SurfaceCanvas
    }

    val statusBorderColor: Color = when {
        isSuccess -> StatusSuccess.copy(alpha = 0.3f)
        isFailed -> StatusDestructive.copy(alpha = 0.3f)
        isFallback -> StatusWarning.copy(alpha = 0.3f)
        else -> BorderLight
    }

    val statusTextColor: Color = when {
        isSuccess -> StatusSuccess
        isFailed -> StatusDestructive
        isFallback -> StatusWarning
        else -> TextSecondary
    }

    val statusBadgeLabel = when {
        isSuccess -> "Executed"
        isFailed -> "Action Failed"
        isFallback -> "Dispatched"
        else -> actionStatus ?: "Status"
    }

    val shape = RoundedCornerShape(Dimens.CornerMedium)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_action_status_${actionType?.lowercase() ?: "general"}")
            .clip(shape)
            .background(SurfaceCanvas)
            .border(Dimens.BorderThin, statusBorderColor, shape)
            .padding(horizontal = Dimens.Spacing14, vertical = Dimens.Spacing10)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusTextColor,
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.Spacing12))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionType?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "System Action",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.CornerSmall))
                            .background(statusBgColor)
                            .padding(horizontal = Dimens.Spacing6, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusBadgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = statusTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                if (!details.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
