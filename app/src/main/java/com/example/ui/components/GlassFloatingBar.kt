package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LabelPrimary
import com.example.ui.theme.LabelSecondary
import com.example.ui.theme.LabelTertiary
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledSurfaceHighest
import com.example.ui.theme.SalimAccent
import com.example.ui.theme.SalimAccentDestructive

@Composable
fun GlassFloatingBar(
    textQuery: String,
    onQueryChange: (String) -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onCancelClick: () -> Unit,
    glassConfig: GlassConfiguration,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleLarge,
            glassConfig = glassConfig,
            tint = OledSurfaceElevated
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button when active loading or listening
                AnimatedVisibility(visible = isLoading || isListening) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCancelClick()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("cancel_button")
                            .applePressFeedback()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel current request",
                            tint = SalimAccentDestructive
                        )
                    }
                }

                // Text Input Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textQuery.isEmpty() && !isListening) {
                        Text(
                            text = if (isLoading) "Processing..." else "Ask Salim Assistant...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = LabelTertiary,
                                fontSize = 16.sp
                            )
                        )
                    } else if (isListening) {
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = SalimAccent,
                                fontSize = 16.sp
                            )
                        )
                    }

                    BasicTextField(
                        value = textQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_field"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = LabelPrimary,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(SalimAccent),
                        maxLines = 4,
                        enabled = !isLoading
                    )
                }

                // Right action: Send button if text entered, otherwise Mic button
                if (textQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(SquirclePill)
                            .background(SalimAccent)
                            .applePressFeedback(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendClick()
                            })
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 1.dp) // Optical alignment
                        )
                    }
                } else {
                    val micBgColor = if (isListening) SalimAccent else OledSurfaceHighest
                    val micIconColor = if (isListening) Color.Black else LabelPrimary

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(SquirclePill)
                            .background(micBgColor)
                            .applePressFeedback(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMicClick()
                            })
                            .testTag("microphone_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop listening" else "Start speech recognition",
                            tint = micIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
