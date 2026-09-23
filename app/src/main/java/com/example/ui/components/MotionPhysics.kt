package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Apple Design & Engineering Standard — Spring Physics
 */
object AppleSprings {
    // Responsive snappy spring for controls, buttons, toggles
    val Snappy: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 380f
    )

    // Gentle spring for sheets, cards, modal reveals
    val Gentle: SpringSpec<Float> = spring(
        dampingRatio = 0.86f,
        stiffness = 220f
    )

    // Quick spring for micro-interactions and typing bubbles
    val Quick: SpringSpec<Float> = spring(
        dampingRatio = 0.80f,
        stiffness = 550f
    )
}

/**
 * Press-down feedback that begins the instant of contact (scale to 0.96f with natural spring settle).
 */
fun Modifier.applePressFeedback(
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = AppleSprings.Snappy,
        label = "press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}
