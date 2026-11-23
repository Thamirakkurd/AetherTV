package com.aethertv.v2.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LiquidGlassTokens {
    // --- Global Icon Scale ---
    val AppWideIconSize = 27.dp
    val HeaderActionTouchTarget = 48.dp
    // STRICT GUARD: Never alter the 3 overlay icons on media cards
    val MediaCardOverlayIconSize = 14.dp

    // --- Edge-to-Edge Grid Spacing ---
    val GridHorizontalEdgePadding = 3.dp
    val GridCellSpacing = 4.dp

    // --- Triple-Line Ultra-Thin Portrait Frame ---
    val CardCornerRadius = 10.dp
    val FrameLineThickness = 0.5.dp
    val FrameLineGap = 1.25.dp

    // --- Top Header ---
    val TopHeaderHeight = 76.dp

    // --- Liquid Glass Palette ---
    val GlassFillDark = Color(0xFF0E131D).copy(alpha = 0.24f)
    val GlassFillElevated = Color(0xFF182233).copy(alpha = 0.34f)
    val GlassDivider = Color.White.copy(alpha = 0.08f)

    val SpecularRimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.16f)
        )
    )

    val InnerGlowBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.18f)
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
