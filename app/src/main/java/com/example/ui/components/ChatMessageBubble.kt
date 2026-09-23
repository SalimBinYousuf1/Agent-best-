package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MessageEntity
import com.example.ui.theme.BorderLight
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BubbleUserShape
import com.example.ui.theme.CodeBlockBackground
import com.example.ui.theme.CodeBlockBorder
import com.example.ui.theme.CodeStyle
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UserBubbleBackground
import com.example.ui.theme.UserBubbleBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageBubble(
    message: MessageEntity,
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.sender.equals("USER", ignoreCase = true)
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing4)
            .testTag(if (isUser) "user_message_${message.id}" else "assistant_message_${message.id}"),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isUser) {
            UserMessageBubble(
                content = message.content,
                timestamp = formattedTime
            )
        } else {
            AssistantMessageItem(
                message = message,
                timestamp = formattedTime,
                onRetryClick = onRetryClick
            )
        }
    }
}

@Composable
private fun UserMessageBubble(
    content: String,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.widthIn(max = 320.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(BubbleUserShape)
                .background(UserBubbleBackground)
                .border(Dimens.BorderThin, UserBubbleBorder, BubbleUserShape)
                .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing12)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = timestamp,
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp),
            modifier = Modifier.padding(end = Dimens.Spacing4)
        )
    }
}

@Composable
private fun AssistantMessageItem(
    message: MessageEntity,
    timestamp: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFailed = message.actionStatus == "FAILED"

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = Dimens.Spacing6)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.Spacing8))

            Text(
                text = "Salim",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.width(Dimens.Spacing8))

            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
            )
        }

        // Action card if this message contains an executed autonomous action
        if (!message.actionType.isNullOrBlank() || !message.actionStatus.isNullOrBlank()) {
            InlineActionStatus(
                actionType = message.actionType,
                actionStatus = message.actionStatus,
                details = message.actionDetails,
                modifier = Modifier.padding(bottom = Dimens.Spacing8)
            )
        }

        // Clean readable text without oversized bubble (ChatGPT / Claude style)
        FormattedAssistantText(text = message.content)

        // If message failed, show clean inline retry
        if (isFailed) {
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.CornerSmall))
                    .clickable(onClick = onRetryClick)
                    .padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4)
                    .testTag("btn_retry_message")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = StatusDestructive,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.Spacing4))
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = StatusDestructive,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun FormattedAssistantText(text: String, modifier: Modifier = Modifier) {
    // Parse paragraphs and code-blocks cleanly
    val blocks = remember(text) { parseTextBlocks(text) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is TextBlock.Paragraph -> {
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            lineHeight = 23.sp
                        ),
                        modifier = Modifier.padding(bottom = Dimens.Spacing8)
                    )
                }
                is TextBlock.Bullet -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.Spacing4),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = BrandAccent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = Dimens.Spacing8)
                        )
                        Text(
                            text = block.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
                is TextBlock.Code -> {
                    val codeShape = RoundedCornerShape(Dimens.CornerSmall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.Spacing6)
                            .clip(codeShape)
                            .background(CodeBlockBackground)
                            .border(Dimens.BorderThin, CodeBlockBorder, codeShape)
                            .padding(Dimens.Spacing12)
                    ) {
                        Text(
                            text = block.content,
                            style = CodeStyle
                        )
                    }
                }
            }
        }
    }
}

sealed class TextBlock {
    data class Paragraph(val content: String) : TextBlock()
    data class Bullet(val content: String) : TextBlock()
    data class Code(val content: String) : TextBlock()
}

private fun parseTextBlocks(text: String): List<TextBlock> {
    val result = mutableListOf<TextBlock>()
    if (text.contains("```")) {
        val parts = text.split("```")
        for (i in parts.indices) {
            val part = parts[i].trim()
            if (part.isEmpty()) continue
            if (i % 2 == 1) {
                // Code block (strip optional language identifier on first line)
                val lines = part.lines()
                val code = if (lines.size > 1 && lines[0].matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                    lines.drop(1).joinToString("\n")
                } else {
                    part
                }
                result.add(TextBlock.Code(code))
            } else {
                parseParagraphsAndBullets(part, result)
            }
        }
    } else {
        parseParagraphsAndBullets(text, result)
    }
    return if (result.isEmpty()) listOf(TextBlock.Paragraph(text)) else result
}

private fun parseParagraphsAndBullets(text: String, list: MutableList<TextBlock>) {
    val lines = text.lines()
    val paragraphBuffer = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            if (paragraphBuffer.isNotEmpty()) {
                list.add(TextBlock.Paragraph(paragraphBuffer.toString().trim()))
                paragraphBuffer.clear()
            }
            list.add(TextBlock.Bullet(trimmed.substring(2)))
        } else if (trimmed.isEmpty()) {
            if (paragraphBuffer.isNotEmpty()) {
                list.add(TextBlock.Paragraph(paragraphBuffer.toString().trim()))
                paragraphBuffer.clear()
            }
        } else {
            if (paragraphBuffer.isNotEmpty()) {
                paragraphBuffer.append("\n")
            }
            paragraphBuffer.append(line)
        }
    }

    if (paragraphBuffer.isNotEmpty()) {
        list.add(TextBlock.Paragraph(paragraphBuffer.toString().trim()))
    }
}
