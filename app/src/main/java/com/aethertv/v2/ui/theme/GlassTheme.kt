package com.aethertv.v2.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MatteGlassTokens {
    val CanvasWhite = Color(0xFFFFFFFF)
    val CharcoalPrimary = Color(0xFF18181B)
    val CharcoalAction = Color(0xFF27272A)
    val CharcoalMuted = Color(0xFF52525B)

    // Neutral matte frosted white surfaces (zero blue/color tint)
    val NavGlassBg = Color(0xBFFFFFFF)          // rgba(255, 255, 255, 0.75)
    val ActivePillBg = Color(0xE6FFFFFF)        // rgba(255, 255, 255, 0.90)
    val HeaderPillBg = Color(0xD9F4F4F5)        // rgba(244, 244, 245, 0.85)
    val NeutralBorder = Color(0xD9E2E2E2)       // rgba(226, 226, 226, 0.85)
    val InnerTopHighlight = Color(0xF2FFFFFF)   // rgba(255, 255, 255, 0.95)
    val CardInnerEdge = Color(0x99FFFFFF)       // faint 1dp inner glass edge

    // Media card overlays & badges
    val CardTitleOverlay = Color(0x7318181B)    // rgba(24, 24, 27, 0.45)
    val BadgeBg = Color(0xD9FFFFFF)             // rgba(255, 255, 255, 0.85)
    val NotificationDot = Color(0xFFDC2626)     // #DC2626
}

fun matteGlassBorder(): BorderStroke = BorderStroke(
    width = 1.dp,
    brush = Brush.verticalGradient(
        colors = listOf(
            MatteGlassTokens.InnerTopHighlight,
            MatteGlassTokens.NeutralBorder,
            MatteGlassTokens.NeutralBorder
        )
    )
)

fun matteInnerHighlightBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        MatteGlassTokens.InnerTopHighlight.copy(alpha = 0.55f),
        Color.Transparent
    )
)

private val MatteWhiteColorScheme = lightColorScheme(
    primary = MatteGlassTokens.CharcoalPrimary,
    onPrimary = MatteGlassTokens.CanvasWhite,
    background = MatteGlassTokens.CanvasWhite,
    onBackground = MatteGlassTokens.CharcoalPrimary,
    surface = MatteGlassTokens.HeaderPillBg,
    onSurface = MatteGlassTokens.CharcoalPrimary,
    onSurfaceVariant = MatteGlassTokens.CharcoalMuted,
    outline = MatteGlassTokens.NeutralBorder
)

private val MatteGlassTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.2).sp,
        color = MatteGlassTokens.CharcoalPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MatteGlassTokens.CharcoalPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = MatteGlassTokens.CharcoalMuted
    )
)

@Composable
fun MatteGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MatteWhiteColorScheme,
        typography = MatteGlassTypography,
        content = content
    )
}
