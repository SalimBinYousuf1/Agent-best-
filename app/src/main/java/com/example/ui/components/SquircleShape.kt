package com.example.ui.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Superellipse / Squircle Shape
 * Provides continuous curvature (G2 approximation) for Apple-standard UI surfaces.
 */
class SquircleShape(val cornerRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2f)
        val w = size.width
        val h = size.height

        val path = Path().apply {
            reset()
            // Approximate continuous superellipse using cubic beziers
            // Top edge
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            // Top-right corner
            cubicTo(
                w - r * 0.448f, 0f,
                w, r * 0.448f,
                w, r
            )
            // Right edge
            lineTo(w, h - r)
            // Bottom-right corner
            cubicTo(
                w, h - r * 0.448f,
                w - r * 0.448f, h,
                w - r, h
            )
            // Bottom edge
            lineTo(r, h)
            // Bottom-left corner
            cubicTo(
                r * 0.448f, h,
                0f, h - r * 0.448f,
                0f, h - r
            )
            // Left edge
            lineTo(0f, r)
            // Top-left corner
            cubicTo(
                0f, r * 0.448f,
                r * 0.448f, 0f,
                r, 0f
            )
            close()
        }
        return Outline.Generic(path)
    }
}

val SquircleSmall = SquircleShape(12.dp)
val SquircleMedium = SquircleShape(18.dp)
val SquircleLarge = SquircleShape(26.dp)
val SquircleSheet = SquircleShape(32.dp)
val SquirclePill = SquircleShape(999.dp)
