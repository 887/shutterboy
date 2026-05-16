package com.eight87.shutterboy.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * m3-expressive C.2 — explicit Shapes so the M3E group-card shape lands
 * everywhere `MaterialTheme.shapes.extraLarge` is consulted (Cards,
 * BottomSheets, AlertDialogs). The rest of the ladder stays on M3
 * defaults; the 28-dp rounded extraLarge is the one corner the
 * Expressive look depends on.
 */
val ShutterboyShapes: Shapes = Shapes(
    extraLarge = RoundedCornerShape(28.dp),
)
