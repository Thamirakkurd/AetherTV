package com.aethertv.v2.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

enum class AppThemeMode(val label: String) {
    LIGHT("Light Liquid Glass"),
    DARK("Dark Liquid Glass")
}

@Immutable
data class LiquidGlassPalette(
    val isDark: Boolean,
    val canvasBg: Color,
    val canvasAccentTop: Color,
    val canvasAccentBottom: Color,
    val subtleSurface: Color,
    val primaryText: Color,
    val actionInk: Color,
    val mutedText: Color,
    val navGlassTint: Color,
    val navGlassFallback: Color,
    val fieldGlassBg: Color,
    val activePillBg: Color,
    val headerPillBg: Color,
    val cardGlassBg: Color,
    val sheetBg: Color,
    val neutralBorder: Color,
    val glassBorder: Color,
    val specularTop: Color,
    val specularBottom: Color,
    val hairlineDivider: Color,
    val notificationDot: Color
) {
    val ambientCanvasBrush: Brush = Brush.verticalGradient(
        colors = listOf(canvasAccentTop, canvasBg, canvasAccentBottom)
    )

    val specularBorderBrush: Brush = Brush.verticalGradient(
        colors = listOf(specularTop, neutralBorder, specularBottom)
    )

}

val LightLiquidPalette = LiquidGlassPalette(
    isDark = false,
    canvasBg = Color(0xFFFFFFFF),
    canvasAccentTop = Color(0xFFFFFFFF),
    canvasAccentBottom = Color(0xFFFFFFFF),
    subtleSurface = Color(0xFFF3F5F8),
    primaryText = Color(0xFF0F1419),
    actionInk = Color(0xFF1A1F24),
    mutedText = Color(0xFF536471),
    navGlassTint = Color(0xCCFFFFFF),
    navGlassFallback = Color(0xF5FFFFFF),
    fieldGlassBg = Color(0xFFF1F3F6),
    activePillBg = Color(0x140F1419),
    headerPillBg = Color(0xF0FFFFFF),
    cardGlassBg = Color(0xFAFFFFFF),
    sheetBg = Color(0xF5FFFFFF),
    neutralBorder = Color(0x1A0F1419),
    glassBorder = Color(0x1F0F1419),
    specularTop = Color(0xFFFFFFFF),
    specularBottom = Color(0x140F1419),
    hairlineDivider = Color(0x1A0F1419),
    notificationDot = Color(0xFFDC2626)
)

val DarkLiquidPalette = LiquidGlassPalette(
    isDark = true,
    canvasBg = Color(0xFF06090F),
    canvasAccentTop = Color(0xFF101827),
    canvasAccentBottom = Color(0xFF070A10),
    subtleSurface = Color(0x52161E2E),
    primaryText = Color(0xFFF5F7FB),
    actionInk = Color(0xFFE2E8F0),
    mutedText = Color(0xFF9AA5B8),
    navGlassTint = Color(0x990A0F18),
    navGlassFallback = Color(0xEB0A0F18),
    fieldGlassBg = Color(0x5C1D273B),
    activePillBg = Color(0x33FFFFFF),
    headerPillBg = Color(0x99141C2B),
    cardGlassBg = Color(0x6E141C2B),
    sheetBg = Color(0xEE0F1522),
    neutralBorder = Color(0x26FFFFFF),
    glassBorder = Color(0x36FFFFFF),
    specularTop = Color(0x4AFFFFFF),
    specularBottom = Color(0x14FFFFFF),
    hairlineDivider = Color(0x22FFFFFF),
    notificationDot = Color(0xFFF87171)
)

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

object MatteGlassTokens {
    internal var palette by mutableStateOf(LightLiquidPalette)

    val isDark: Boolean get() = palette.isDark
    val CanvasWhite: Color get() = palette.canvasBg
    val SubtleSurface: Color get() = palette.subtleSurface
    val CharcoalPrimary: Color get() = palette.primaryText
    val CharcoalAction: Color get() = palette.actionInk
    val CharcoalMuted: Color get() = palette.mutedText

    val NavGlassBg: Color get() = palette.navGlassTint
    val NavGlassFallback: Color get() = palette.navGlassFallback
    val FieldGlassBg: Color get() = palette.fieldGlassBg
    val CardGlassBg: Color get() = palette.cardGlassBg
    val SheetBg: Color get() = palette.sheetBg
    val NeutralBorder: Color get() = palette.neutralBorder
    val HairlineDivider: Color get() = palette.hairlineDivider
    val NotificationDot: Color get() = palette.notificationDot

    val AmbientCanvasBrush: Brush get() = palette.ambientCanvasBrush
    val SpecularBorderBrush: Brush get() = palette.specularBorderBrush
}

fun liquidHazeStyle(
    tintColor: Color = MatteGlassTokens.NavGlassBg,
    blurRadius: Dp = 16.dp,
    noiseFactor: Float = 0f
): HazeStyle = HazeStyle(
    backgroundColor = MatteGlassTokens.CanvasWhite,
    tint = HazeTint(color = tintColor),
    blurRadius = blurRadius,
    noiseFactor = noiseFactor
)

fun Modifier.liquidGlassSurface(
    hazeState: HazeState?,
    shape: Shape = RectangleShape,
    tintColor: Color = MatteGlassTokens.NavGlassBg,
    fallbackColor: Color = MatteGlassTokens.NavGlassFallback,
    blurRadius: Dp = 16.dp,
    borderWidth: Dp = 0.75.dp,
    borderBrush: Brush? = MatteGlassTokens.SpecularBorderBrush
): Modifier = composed {
    val style = remember(tintColor, blurRadius, MatteGlassTokens.isDark) {
        liquidHazeStyle(tintColor = tintColor, blurRadius = blurRadius)
    }
    val clipped = this.clip(shape)
    val blurred = if (hazeState != null) {
        clipped.hazeEffect(state = hazeState, style = style)
    } else {
        clipped.background(fallbackColor)
    }
    if (borderBrush != null && borderWidth > 0.dp) {
        blurred.border(borderWidth, borderBrush, shape)
    } else {
        blurred
    }
}

fun Modifier.livelyPress(
    enabled: Boolean = true,
    pressedScale: Float = 0.968f,
    depthShiftPx: Float = 3f,
    bounded: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val rippleColor = MatteGlassTokens.CharcoalPrimary
    val rippleIndication = remember(bounded, rippleColor) {
        ripple(bounded = bounded, color = rippleColor)
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 420f),
        label = "livelyScale"
    )
    val translateY by animateFloatAsState(
        targetValue = if (pressed && enabled) depthShiftPx else 0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 420f),
        label = "livelyDepth"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.84f else 1f,
        animationSpec = tween(durationMillis = 75),
        label = "livelyAlpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = translateY
            alpha = pressAlpha
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = rippleIndication,
            onClick = onClick
        )
}

@Composable
fun MatteGlassTheme(
    darkTheme: Boolean = false,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val targetPalette = if (darkTheme) DarkLiquidPalette else LightLiquidPalette
    MatteGlassTokens.palette = targetPalette

    val colorScheme = remember(darkTheme) {
        if (darkTheme) {
            darkColorScheme(
                primary = targetPalette.primaryText,
                onPrimary = targetPalette.canvasBg,
                background = targetPalette.canvasBg,
                onBackground = targetPalette.primaryText,
                surface = targetPalette.headerPillBg,
                onSurface = targetPalette.primaryText,
                onSurfaceVariant = targetPalette.mutedText,
                outline = targetPalette.neutralBorder
            )
        } else {
            lightColorScheme(
                primary = targetPalette.primaryText,
                onPrimary = targetPalette.canvasBg,
                background = targetPalette.canvasBg,
                onBackground = targetPalette.primaryText,
                surface = targetPalette.headerPillBg,
                onSurface = targetPalette.primaryText,
                onSurfaceVariant = targetPalette.mutedText,
                outline = targetPalette.neutralBorder
            )
        }
    }

    val typography = remember(darkTheme) {
        Typography(
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.25).sp,
                color = targetPalette.primaryText
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = targetPalette.primaryText
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = targetPalette.mutedText
            )
        )
    }

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalContentColor provides targetPalette.primaryText
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
