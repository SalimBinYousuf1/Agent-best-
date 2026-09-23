package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SalimAccent
import com.example.ui.theme.SalimAccentSecondary

/**
 * Decorative Audio Wave Visualizer.
 * Interpolates smoothly per-frame.
 * Explicitly clears semantics so screen readers skip it (Constraint #10).
 */
@Composable
fun VoiceWaveVisualizer(
    soundLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_ambient")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clearAndSetSemantics { /* Skipped by TalkBack */ }
    ) {
        val barCount = 19
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount + 1)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val offsetFromCenter = kotlin.math.abs(i - (barCount / 2)) / (barCount / 2f)
            val bellFactor = 1f - (offsetFromCenter * 0.6f)

            val baseHeight = 6.dp.toPx()
            val animatedHeight = if (isListening) {
                val soundDynamic = soundLevel * size.height * 0.85f * bellFactor
                val waveDynamic = (kotlin.math.sin((i.toDouble() / 2.0) + (phase * kotlin.math.PI)).toFloat() * 10.dp.toPx())
                (baseHeight + soundDynamic + waveDynamic).coerceIn(6.dp.toPx(), size.height - 4.dp.toPx())
            } else {
                baseHeight
            }

            val x = spacing + i * (barWidth + spacing)
            val y = centerY - (animatedHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(SalimAccent, SalimAccentSecondary)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, animatedHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
