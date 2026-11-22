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
    val SubtleSurface = Color(0xFFF6F6F7)
    val CharcoalPrimary = Color(0xFF18181B)
    val CharcoalAction = Color(0xFF27272A)
    val CharcoalMuted = Color(0xFF52525B)

    val NavGlassBg = Color(0xF2FFFFFF)
    val ActivePillBg = Color(0xFFF1F1F4)
    val HeaderPillBg = Color(0xFFF4F4F5)
    val NeutralBorder = Color(0xFFE2E2E6)
    val GlassNavBorder = Color(0xFFD4D4D8)
    val InnerTopHighlight = Color(0xF2FFFFFF)
    val CardInnerEdge = Color(0x26000000)

    val CardTitleOverlay = Color(0x7318181B)
    val BadgeBg = Color(0xD9FFFFFF)
    val NotificationDot = Color(0xFFDC2626)
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
        fontSize = 20.sp,
        letterSpacing = (-0.25).sp,
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
