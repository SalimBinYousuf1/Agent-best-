package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BackgroundWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandAccentHighlight
import com.example.ui.theme.Dimens
import com.example.ui.theme.StatusDestructive
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextQuaternary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ChatComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    val canSend = inputText.isNotBlank() && !isLoading

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = BackgroundWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)
        ) {
            // Rounded white container with light gray border (ChatGPT / Claude style)
            val composerShape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(composerShape)
                    .background(BackgroundWhite)
                    .border(
                        width = Dimens.BorderThin,
                        color = if (isListening) BrandAccentHighlight else BorderLight,
                        shape = composerShape
                    )
                    .padding(horizontal = Dimens.Spacing12, vertical = Dimens.Spacing6),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multiline text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Dimens.Spacing8, end = Dimens.Spacing8, top = Dimens.Spacing6, bottom = Dimens.Spacing6),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = if (isListening) "Listening to your voice..." else "Message Salim...",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextTertiary)
                        )
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp, max = 120.dp)
                            .testTag("chat_input_field"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(BrandAccent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = if (inputText.contains("\n")) ImeAction.Default else ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend) {
                                    onSendMessage(inputText)
                                }
                            }
                        ),
                        maxLines = 5
                    )
                }

                // Microphone button
                Box(
                    modifier = Modifier
                        .size(Dimens.MinTouchTarget)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (isListening) "Stop listening" else "Start voice input",
                            onClick = onMicClick
                        )
                        .testTag("btn_mic"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .scale(micScale)
                                .clip(CircleShape)
                                .background(StatusDestructive.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop voice recording",
                                tint = StatusDestructive,
                                modifier = Modifier.size(Dimens.IconMedium)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start voice input",
                            tint = TextSecondary,
                            modifier = Modifier.size(Dimens.IconLarge)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Dimens.Spacing4))

                // Send button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (canSend) BrandAccent else BorderLight)
                        .clickable(
                            enabled = canSend,
                            role = Role.Button,
                            onClickLabel = "Send message",
                            onClick = {
                                if (canSend) {
                                    onSendMessage(inputText)
                                }
                            }
                        )
                        .testTag("btn_send"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else TextQuaternary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
