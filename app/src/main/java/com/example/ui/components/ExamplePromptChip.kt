package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderLight
import com.example.ui.theme.Dimens
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary

@Composable
fun ExamplePromptChip(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimens.CornerMedium)

    Row(
        modifier = modifier
            .testTag("prompt_chip_${text.take(10).replace(" ", "_").lowercase()}")
            .clip(shape)
            .background(SurfaceCardWhite)
            .border(Dimens.BorderThin, BorderLight, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
            .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(Dimens.IconMedium)
            )
            Spacer(modifier = Modifier.width(Dimens.Spacing10))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.width(Dimens.Spacing8))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}
