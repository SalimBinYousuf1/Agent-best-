package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassDarkBorder
import com.example.ui.theme.GlassSpecularHighlight
import com.example.ui.theme.OledSurfaceElevated

/**
 * Ambient Glass Transparency Level from Settings (0.2f clear to 0.95f opaque).
 */
data class GlassConfiguration(
    val baseOpacity: Float = 0.82f,
    val reducedTransparency: Boolean = false,
    val isScrolledOver: Boolean = false
)

val LocalGlassConfiguration = compositionLocalOf { GlassConfiguration() }

/**
 * Liquid Glass modifier giving physical thickness with top specular highlight,
 * darkened bottom edge, and reactive scroll-based opacity.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = SquircleMedium,
    glassConfig: GlassConfiguration = GlassConfiguration(),
    customTint: Color = Color(0xFF10141E)
): Modifier {
    val effectiveAlpha = when {
        glassConfig.reducedTransparency -> 1f
        glassConfig.isScrolledOver -> (glassConfig.baseOpacity + 0.15f).coerceAtMost(0.98f)
        else -> glassConfig.baseOpacity
    }

    val backgroundColor = if (glassConfig.reducedTransparency) {
        OledSurfaceElevated
    } else {
        customTint.copy(alpha = effectiveAlpha)
    }

    return this
        .clip(shape)
        .background(backgroundColor, shape)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (glassConfig.reducedTransparency) Color(0x33FFFFFF) else GlassSpecularHighlight,
                    if (glassConfig.reducedTransparency) Color(0x11000000) else GlassDarkBorder
                )
            ),
            shape = shape
        )
        .drawWithContent {
            drawContent()
            if (!glassConfig.reducedTransparency) {
                // Soft top specular highlight line
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.5.dp.toPx())
                )
            }
        }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = SquircleMedium,
    glassConfig: GlassConfiguration = LocalGlassConfiguration.current,
    tint: Color = Color(0xFF10141E),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = shape,
            glassConfig = glassConfig,
            customTint = tint
        ),
        content = content
    )
}
