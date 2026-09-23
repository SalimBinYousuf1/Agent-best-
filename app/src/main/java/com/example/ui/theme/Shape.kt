package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BubbleUserShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 4.dp
)

val BubbleAssistantShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 4.dp,
    bottomEnd = 20.dp
)

val CardModernShape = RoundedCornerShape(18.dp)
val CardLargeShape = RoundedCornerShape(22.dp)
val ComposerShape = RoundedCornerShape(24.dp)
val ChipShape = RoundedCornerShape(20.dp)
val ButtonShape = RoundedCornerShape(14.dp)
val PillShape = RoundedCornerShape(999.dp)

val SalimShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
