package com.aethertv.v2.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
    // Cached GPU brushes — allocated once per palette instead of per frame/recomposition
    val ambientCanvasBrush: Brush = Brush.verticalGradient(
        colors = listOf(canvasAccentTop, canvasBg, canvasAccentBottom)
    )

    val specularBorderBrush: Brush = Brush.verticalGradient(
        colors = listOf(specularTop, neutralBorder, specularBottom)
    )

    val cardRimBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.34f else 0.58f),
            Color.White.copy(alpha = if (isDark) 0.10f else 0.14f),
            Color.Black.copy(alpha = if (isDark) 0.32f else 0.14f)
        )
    )

    val innerGlowBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.11f else 0.42f),
            Color.Transparent,
            Color.Black.copy(alpha = if (isDark) 0.16f else 0.04f)
        )
    )
}

val LightLiquidPalette = LiquidGlassPalette(
    isDark = false,
    canvasBg = Color(0xFFF3F6FC),
    canvasAccentTop = Color(0xFFE4ECFA),
    canvasAccentBottom = Color(0xFFF7F9FD),
    subtleSurface = Color(0x99EEF2F8),
    primaryText = Color(0xFF12151C),
    actionInk = Color(0xFF202530),
    mutedText = Color(0xFF565E6B),
    navGlassTint = Color(0x88F4F7FC),
    navGlassFallback = Color(0xE6F4F7FC),
    fieldGlassBg = Color(0x7AE1E7F2),
    activePillBg = Color(0xCCFFFFFF),
    headerPillBg = Color(0xBFF0F4FA),
    cardGlassBg = Color(0xB8FFFFFF),
    sheetBg = Color(0xECF3F6FB),
    neutralBorder = Color(0x8CD5DCE8),
    glassBorder = Color(0xB8CBD3E0),
    specularTop = Color(0xF5FFFFFF),
    specularBottom = Color(0x2614161A),
    hairlineDivider = Color(0x1E14161A),
    notificationDot = Color(0xFFDC2626)
)

val DarkLiquidPalette = LiquidGlassPalette(
    isDark = true,
    canvasBg = Color(0xFF06090F),
    canvasAccentTop = Color(0xFF142138),
    canvasAccentBottom = Color(0xFF080B12),
    subtleSurface = Color(0x52161E2E),
    primaryText = Color(0xFFF5F7FB),
    actionInk = Color(0xFFE2E8F0),
    mutedText = Color(0xFF9AA5B8),
    navGlassTint = Color(0x7A0C121D),
    navGlassFallback = Color(0xE60C121D),
    fieldGlassBg = Color(0x5C1D273B),
    activePillBg = Color(0x38FFFFFF),
    headerPillBg = Color(0x94141C2B),
    cardGlassBg = Color(0x6E141C2B),
    sheetBg = Color(0xE80F1522),
    neutralBorder = Color(0x2BFFFFFF),
    glassBorder = Color(0x3DFFFFFF),
    specularTop = Color(0x58FFFFFF),
    specularBottom = Color(0x14FFFFFF),
    hairlineDivider = Color(0x24FFFFFF),
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
    val ActivePillBg: Color get() = palette.activePillBg
    val HeaderPillBg: Color get() = palette.headerPillBg
    val CardGlassBg: Color get() = palette.cardGlassBg
    val SheetBg: Color get() = palette.sheetBg
    val NeutralBorder: Color get() = palette.neutralBorder
    val GlassNavBorder: Color get() = palette.glassBorder
    val InnerTopHighlight: Color get() = palette.specularTop
    val HairlineDivider: Color get() = palette.hairlineDivider
    val NotificationDot: Color get() = palette.notificationDot

    val AmbientCanvasBrush: Brush get() = palette.ambientCanvasBrush
    val SpecularBorderBrush: Brush get() = palette.specularBorderBrush
    val CardRimBrush: Brush get() = palette.cardRimBrush
    val InnerGlowBrush: Brush get() = palette.innerGlowBrush
}

fun matteGlassBorder(): BorderStroke = BorderStroke(
    width = 0.85.dp,
    brush = MatteGlassTokens.SpecularBorderBrush
)

fun liquidHazeStyle(
    tintColor: Color = MatteGlassTokens.NavGlassBg,
    blurRadius: Dp = 32.dp,
    noiseFactor: Float = 0.045f
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
    blurRadius: Dp = 32.dp,
    borderWidth: Dp = 0.85.dp,
    borderBrush: Brush? = MatteGlassTokens.SpecularBorderBrush
): Modifier = composed {
    val style = remember(tintColor, blurRadius, MatteGlassTokens.isDark) {
        liquidHazeStyle(tintColor = tintColor, blurRadius = blurRadius)
    }
    val clipped = this.clip(shape)
    val blurred = if (hazeState != null) {
        clipped
            .hazeEffect(state = hazeState, style = style)
            .background(MatteGlassTokens.InnerGlowBrush)
    } else {
        clipped
            .background(fallbackColor)
            .background(MatteGlassTokens.InnerGlowBrush)
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
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = translateY
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
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

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
