package com.aethertv.v2.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LiquidGlassTokens {
    // --- Edge-to-Edge Grid Spacing ---
    val GridHorizontalEdgePadding = 1.dp
    val GridCellSpacing = 0.5.dp

    // --- Triple-Line Ultra-Thin Portrait Frame ---
    val CardCornerRadius = 3.dp
    val FrameOuterCornerRadius = 4.5.dp
    val FrameLineThickness = 0.35.dp
    val FrameLineGap = 0.35.dp
    val CardFrameInset = 2.2.dp

    // --- Card Backing ---
    val GlassFillDark = Color(0xFF0E131D).copy(alpha = 0.24f)

    // --- Spring Physics Preset ---
    fun <T> meshSettleSpring() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessLow
    )
}
