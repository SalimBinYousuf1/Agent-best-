package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.Dimens
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val dot1Scale = remember { Animatable(0.4f) }
    val dot2Scale = remember { Animatable(0.4f) }
    val dot3Scale = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        val animationSpec = infiniteRepeatable<Float>(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )

        launch {
            dot1Scale.animateTo(targetValue = 1.0f, animationSpec = animationSpec)
        }
        delay(200)
        launch {
            dot2Scale.animateTo(targetValue = 1.0f, animationSpec = animationSpec)
        }
        delay(200)
        launch {
            dot3Scale.animateTo(targetValue = 1.0f, animationSpec = animationSpec)
        }
    }

    Box(
        modifier = modifier
            .testTag("typing_indicator")
            .clip(RoundedCornerShape(Dimens.CornerMedium))
            .background(SurfaceCanvas)
            .border(Dimens.BorderThin, BorderSubtle, RoundedCornerShape(Dimens.CornerMedium))
            .padding(horizontal = Dimens.Spacing14, vertical = Dimens.Spacing10),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing6),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.Spacing6)
                    .scale(dot1Scale.value)
                    .clip(CircleShape)
                    .background(TextTertiary)
            )
            Box(
                modifier = Modifier
                    .size(Dimens.Spacing6)
                    .scale(dot2Scale.value)
                    .clip(CircleShape)
                    .background(TextTertiary)
            )
            Box(
                modifier = Modifier
                    .size(Dimens.Spacing6)
                    .scale(dot3Scale.value)
                    .clip(CircleShape)
                    .background(TextTertiary)
            )
        }
    }
}
