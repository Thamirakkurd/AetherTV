package com.aethertv.v2.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LiquidGlassTokens {
    // --- Global Icon Scale ---
    val AppWideIconSize = 26.dp
    val HeaderActionTouchTarget = 46.dp
    val MediaCardOverlayIconSize = 18.dp

    // --- Edge-to-Edge Grid Spacing ---
    val GridHorizontalEdgePadding = 1.dp
    val GridCellSpacing = 0.5.dp

    // --- Triple-Line Ultra-Thin Portrait Frame ---
    val CardCornerRadius = 3.dp
    val FrameOuterCornerRadius = 4.5.dp
    val FrameLineThickness = 0.35.dp
    val FrameLineGap = 0.35.dp
    val CardFrameInset = 2.2.dp

    // --- Top Header ---
    val TopHeaderHeight = 62.dp

    // --- Clean Glass Palette ---
    val GlassFillDark = Color(0xFF0E131D).copy(alpha = 0.24f)
    val GlassFillElevated = Color(0xFF182233).copy(alpha = 0.34f)
    val GlassDivider = Color.White.copy(alpha = 0.08f)

    val SpecularRimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.26f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.14f)
        )
    )

    // Clean top sheen with zero dark bottom shadow
    val InnerGlowBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

    // --- Spring Physics Presets ---
    fun <T> popSpring() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> meshSettleSpring() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessLow
    )
}
