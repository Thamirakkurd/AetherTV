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
)

val LightLiquidPalette = LiquidGlassPalette(
    isDark = false,
    canvasBg = Color(0xFFF6F8FB),
    canvasAccentTop = Color(0xFFEAF0FA),
    canvasAccentBottom = Color(0xFFF8F9FC),
    subtleSurface = Color(0xFFF0F2F6),
    primaryText = Color(0xFF14161A),
    actionInk = Color(0xFF23272F),
    mutedText = Color(0xFF555B66),
    navGlassTint = Color(0x9EF4F6FA),
    navGlassFallback = Color(0xECF4F6FA),
    fieldGlassBg = Color(0x8CE3E8F0),
    activePillBg = Color(0xCCFFFFFF),
    headerPillBg = Color(0xCCF1F4F8),
    cardGlassBg = Color(0xD9FFFFFF),
    sheetBg = Color(0xF0F4F6FA),
    neutralBorder = Color(0xFFDDE2EB),
    glassBorder = Color(0xFFCFD6E2),
    specularTop = Color(0xF2FFFFFF),
    specularBottom = Color(0x2814161A),
    hairlineDivider = Color(0x2214161A),
    notificationDot = Color(0xFFDC2626)
)

val DarkLiquidPalette = LiquidGlassPalette(
    isDark = true,
    canvasBg = Color(0xFF070A0F),
    canvasAccentTop = Color(0xFF131D30),
    canvasAccentBottom = Color(0xFF090C12),
    subtleSurface = Color(0xFF121722),
    primaryText = Color(0xFFF4F6FA),
    actionInk = Color(0xFFE2E8F0),
    mutedText = Color(0xFF98A2B3),
    navGlassTint = Color(0x940D121B),
    navGlassFallback = Color(0xEC0D121B),
    fieldGlassBg = Color(0x731C2434),
    activePillBg = Color(0x40FFFFFF),
    headerPillBg = Color(0xB3141A26),
    cardGlassBg = Color(0xA6141A26),
    sheetBg = Color(0xEE101520),
    neutralBorder = Color(0x2EFFFFFF),
    glassBorder = Color(0x3DFFFFFF),
    specularTop = Color(0x52FFFFFF),
    specularBottom = Color(0x14FFFFFF),
    hairlineDivider = Color(0x29FFFFFF),
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

    val AmbientCanvasBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                palette.canvasAccentTop,
                palette.canvasBg,
                palette.canvasAccentBottom
            )
        )

    val SpecularBorderBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                palette.specularTop,
                palette.neutralBorder,
                palette.specularBottom
            )
        )

    val CardRimBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (palette.isDark) 0.34f else 0.55f),
                Color.White.copy(alpha = if (palette.isDark) 0.10f else 0.14f),
                Color.Black.copy(alpha = if (palette.isDark) 0.35f else 0.16f)
            )
        )
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
): Modifier {
    val clipped = this.clip(shape)
    val blurred = if (hazeState != null) {
        clipped.hazeEffect(
            state = hazeState,
            style = liquidHazeStyle(tintColor = tintColor, blurRadius = blurRadius)
        )
    } else {
        clipped.background(fallbackColor)
    }
    return if (borderBrush != null && borderWidth > 0.dp) {
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
