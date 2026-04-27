package com.flowboard.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * FlowBoard shape scale following Material 3 geometry tokens.
 *
 * extra-small → small → medium → large → extra-large
 * Used via MaterialTheme.shapes.* throughout the UI.
 */
val FlowBoardShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, badges, tooltips
    small      = RoundedCornerShape(8.dp),   // text fields, buttons, snackbars
    medium     = RoundedCornerShape(12.dp),  // cards, dialogs
    large      = RoundedCornerShape(16.dp),  // bottom sheets, nav drawers
    extraLarge = RoundedCornerShape(24.dp)   // FAB, full-screen containers
)
