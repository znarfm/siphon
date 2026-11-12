package com.znarfm.siphon.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive Shape Tokens: high curvature contrast, asymmetrical pill emphasis
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

object ExpressiveShapeTokens {
    val HeaderSheetShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    val ActiveCardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 24.dp)
    val RestingCardShape = RoundedCornerShape(22.dp)
    val SelectedCardShape = RoundedCornerShape(26.dp)
    val PillShape = RoundedCornerShape(percent = 50)
}

