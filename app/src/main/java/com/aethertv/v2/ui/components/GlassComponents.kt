package com.aethertv.v2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.theme.LiquidGlassTokens
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.liquidGlassSurface
import com.aethertv.v2.ui.theme.livelyPress
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Provides grid-wide connected spring physics to PosterMediaCard without breaking existing callers.
 */
val LocalGridSpringPhysics = compositionLocalOf<GridSpringPhysicsState?> { null }

data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector = selectedIcon
)

private val NavFieldShape = RoundedCornerShape(22.dp)
private val CardInnerShape = RoundedCornerShape(6.dp)
private val CardGlassTintBrush = Brush.verticalGradient(
    listOf(Color(0x331E2636), Color(0x550B0E15))
)
private val CardScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0x88090B10), Color(0xEB07090D))
)
private val CardIconBorder = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.36f))

@Composable
fun FloatingGlassNavBar(
    items: List<NavTabItem>,
    selectedIndex: Int,
    pagePosition: Float = selectedIndex.toFloat(),
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val density = LocalDensity.current
    val capsuleShape = RoundedCornerShape(34.dp)
    val activePillShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (MatteGlassTokens.isDark) 18.dp else 12.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.32f),
                    spotColor = Color.Black.copy(alpha = 0.28f)
                )
                .liquidGlassSurface(
                    hazeState = hazeState,
                    shape = capsuleShape,
                    tintColor = MatteGlassTokens.NavGlassBg,
                    fallbackColor = MatteGlassTokens.NavGlassFallback,
                    blurRadius = 34.dp,
                    borderWidth = 1.dp,
                    borderBrush = MatteGlassTokens.SpecularBorderBrush
                )
                .background(LiquidGlassTokens.InnerGlowBrush)
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            val count = items.size.coerceAtLeast(1)
            val tabWidth = maxWidth / count
            val tabWidthPx = with(density) { tabWidth.toPx() }
            val clampedPos = pagePosition.coerceIn(0f, (count - 1).toFloat())
            val fractional = abs(clampedPos - clampedPos.roundToInt())

            // GPU-driven Liquid Glass sliding pill with organic velocity stretch
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = clampedPos * tabWidthPx
                        scaleX = 1f + fractional * 0.16f
                        scaleY = 1f - fractional * 0.06f
                    }
                    .clip(activePillShape)
                    .background(MatteGlassTokens.ActivePillBg)
                    .background(LiquidGlassTokens.InnerGlowBrush)
                    .border(0.95.dp, MatteGlassTokens.SpecularBorderBrush, activePillShape)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        targetValue = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                        animationSpec = tween(160),
                        label = "navTint"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.10f else 0.96f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 440f),
                        label = "navScale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .livelyPress(pressedScale = 0.90f, depthShiftPx = 1.5f) { onSelect(index) }
                            .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier
                                .size(26.dp) // Scaled up app-wide icon
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (MatteGlassTokens.isDark) Color(0x52121824) else Color(0x99FFFFFF)
    val rim = if (MatteGlassTokens.isDark) Color.White.copy(alpha = 0.30f) else MatteGlassTokens.GlassNavBorder

    Box(
        modifier = modifier
            .size(48.dp) // Scaled up touch target
            .livelyPress(pressedScale = 0.91f, depthShiftPx = 2f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .background(LiquidGlassTokens.InnerGlowBrush)
            .border(BorderStroke(0.9.dp, rim), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(25.dp) // Scaled up app-wide icon
        )
    }
}

@Composable
fun SimpleScreenHeader(
    title: String,
    onAddClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                hazeState = hazeState,
                shape = RectangleShape,
                tintColor = MatteGlassTokens.NavGlassBg,
                fallbackColor = MatteGlassTokens.NavGlassFallback,
                blurRadius = 34.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
            .background(LiquidGlassTokens.InnerGlowBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MatteGlassTokens.CharcoalPrimary
            )
            if (onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .livelyPress(pressedScale = 0.90f, depthShiftPx = 2f, onClick = onAddClick)
                        .clip(CircleShape)
                        .background(MatteGlassTokens.FieldGlassBg)
                        .background(LiquidGlassTokens.InnerGlowBrush)
                        .border(0.9.dp, MatteGlassTokens.SpecularBorderBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Server",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(26.dp) // Scaled up app-wide icon
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.75.dp, color = MatteGlassTokens.HairlineDivider)
    }
}

@Composable
fun UnifiedMediaTopBar(
    searchQuery: String,
    searchPlaceholder: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    var menuExpanded by remember { mutableStateOf(false) }
    var lastNonFavCategory by remember { mutableStateOf("") }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory.isNotBlank() && selectedCategory != "Favorites") {
            lastNonFavCategory = selectedCategory
        }
    }

    val isFavorites = selectedCategory == "Favorites"
    val displayCategory = when {
        selectedCategory.isBlank() -> "All"
        selectedCategory == "Favorites" -> "Favorites"
        else -> selectedCategory
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                hazeState = hazeState,
                shape = RectangleShape,
                tintColor = MatteGlassTokens.NavGlassBg,
                fallbackColor = MatteGlassTokens.NavGlassFallback,
                blurRadius = 34.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
            .background(LiquidGlassTokens.InnerGlowBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(68.dp) // Larger, stronger top header presence
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(NavFieldShape)
                    .background(MatteGlassTokens.FieldGlassBg)
                    .background(LiquidGlassTokens.InnerGlowBrush)
                    .border(0.9.dp, MatteGlassTokens.SpecularBorderBrush, NavFieldShape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MatteGlassTokens.CharcoalMuted,
                    modifier = Modifier.size(23.dp) // Scaled up app-wide icon
                )
                Spacer(Modifier.width(9.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 14.5.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.5.sp, color = MatteGlassTokens.CharcoalPrimary),
                        cursorBrush = SolidColor(MatteGlassTokens.CharcoalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Box {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(max = 146.dp)
                            .livelyPress(pressedScale = 0.94f, depthShiftPx = 2f) { menuExpanded = true }
                            .clip(NavFieldShape)
                            .background(MatteGlassTokens.FieldGlassBg)
                            .background(LiquidGlassTokens.InnerGlowBrush)
                            .border(0.9.dp, MatteGlassTokens.SpecularBorderBrush, NavFieldShape)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayCategory,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Categories",
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(22.dp) // Scaled up app-wide icon
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(MatteGlassTokens.SheetBg)
                            .widthIn(min = 200.dp, max = 290.dp)
                            .heightIn(max = 420.dp)
                    ) {
                        categories.forEach { cat ->
                            val active = cat == selectedCategory
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 14.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = MatteGlassTokens.CharcoalPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (active) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MatteGlassTokens.CharcoalPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSelectCategory(cat)
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .livelyPress(pressedScale = 0.90f, depthShiftPx = 2f) {
                        if (isFavorites) {
                            val fallback = lastNonFavCategory.ifBlank {
                                categories.firstOrNull { it != "All" && it != "Favorites" }
                                    ?: categories.firstOrNull() ?: "All"
                            }
                            onSelectCategory(fallback)
                        } else {
                            onSelectCategory("Favorites")
                        }
                    }
                    .clip(CircleShape)
                    .background(if (isFavorites) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                    .border(
                        0.9.dp,
                        if (isFavorites) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorites",
                    tint = if (isFavorites) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.size(24.dp) // Scaled up app-wide icon
                )
            }
        }
        HorizontalDivider(thickness = 0.75.dp, color = MatteGlassTokens.HairlineDivider)
    }
}

/**
 * Draws the custom 3-line ultra-thin, tightly spaced concentric portrait frame.
 * Pre-computes strokes and radii inside drawWithCache for zero per-frame allocation.
 */
private fun Modifier.tripleHairlinePortraitFrame(
    isDark: Boolean,
    isActive: Boolean
): Modifier = this.drawWithCache {
    val strokePx = LiquidGlassTokens.FrameLineThickness.toPx().coerceAtLeast(1f)
    val gapPx = LiquidGlassTokens.FrameLineGap.toPx()
    val outerRadiusPx = 9.dp.toPx()
    val stroke = Stroke(width = strokePx)

    val boost = if (isActive) 1.85f else 1f
    val baseColor = if (isDark) Color.White else Color(0xFF14161A)
    val ringColors = listOf(
        baseColor.copy(alpha = (0.09f * boost).coerceAtMost(0.38f)), // Outer hairline
        baseColor.copy(alpha = (0.14f * boost).coerceAtMost(0.52f)), // Middle hairline
        baseColor.copy(alpha = (0.21f * boost).coerceAtMost(0.68f))  // Inner hairline
    )

    onDrawWithContent {
        drawContent()
        for (ring in 0 until 3) {
            val inset = ring * (strokePx + gapPx) + (strokePx / 2f)
            val radius = (outerRadiusPx - inset * 0.65f).coerceAtLeast(2f)
            drawRoundRect(
                color = ringColors[ring],
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                cornerRadius = CornerRadius(radius, radius),
                style = stroke
            )
        }
    }
}

@Composable
fun PosterMediaCard(
    item: MediaItem,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onMoreClick: () -> Unit = onCardClick,
    isLarge: Boolean = false,
    modifier: Modifier = Modifier,
    gridIndex: Int = 0,
    gridColumns: Int = 3
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val active = pressed || focused
    val physicsState = LocalGridSpringPhysics.current
    val isDark = MatteGlassTokens.isDark

    // Tactile selection POP (expand & lift forward on GPU)
    val popScale by animateFloatAsState(
        targetValue = when {
            pressed -> 1.045f
            focused -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMediumLow),
        label = "cardPopScale"
    )
    val popLiftY by animateFloatAsState(
        targetValue = if (active) -7f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "cardPopLift"
    )
    val pressGlintAlpha by animateFloatAsState(
        targetValue = if (active) 0.14f else 0f,
        animationSpec = tween(110),
        label = "cardGlint"
    )

    val badgeText = remember(item.badge, item.contentType) {
        val raw = item.badge.trim()
        when {
            raw.isNotBlank() && raw != "0" && raw != "0.0" -> {
                if (raw.startsWith("★") || raw.equals("SERIES", true) || raw.equals("LIVE", true) || raw.equals("MOVIE", true)) raw
                else "★ $raw"
            }
            item.contentType == ContentType.SERIES -> "SERIES"
            else -> ""
        }
    }

    val imageRequest = remember(item.posterUrl, isLarge) {
        item.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .allowHardware(true) // Direct GPU texture upload
                .crossfade(120)
                .size(if (isLarge) 540 else 360, if (isLarge) 780 else 540)
                .build()
        }
    }

    val col = gridIndex % gridColumns.coerceAtLeast(1)
    val staggerFactor = 1f + (col * 0.14f)
    val frameInset = (LiquidGlassTokens.FrameLineThickness + LiquidGlassTokens.FrameLineGap) * 3

    Box(
        modifier = modifier
            .zIndex(if (active) 10f else 0f)
            .graphicsLayer {
                val vImpulse = physicsState?.verticalImpulse?.value ?: 0f
                val hImpulse = physicsState?.lateralImpulse?.value ?: 0f

                scaleX = popScale
                scaleY = popScale
                translationY = popLiftY + (vImpulse * 13f * staggerFactor)
                translationX = hImpulse * 9f * staggerFactor
                cameraDistance = 16f * density
                rotationX = (vImpulse * 3.0f).coerceIn(-4f, 4f)
                rotationY = (-hImpulse * 2.8f).coerceIn(-4f, 4f)
                shadowElevation = if (active) 22f else 0f
            }
            .tripleHairlinePortraitFrame(isDark = isDark, isActive = active)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardClick
            )
            .padding(frameInset)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardInnerShape)
                .background(CardGlassTintBrush)
                .background(LiquidGlassTokens.InnerGlowBrush)
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom readability scrim
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.56f)
                    .background(CardScrimBrush)
            )

            // Subtle inner glass glow & active glint
            if (pressGlintAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = pressGlintAlpha))
                )
            }

            // STRICT EXCEPTION: Media card overlay icons remain at original size
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardGlassCircleIcon(
                    icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    onClick = onFavoriteClick
                )
                CardGlassCircleIcon(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    onClick = onMoreClick
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 9.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = if (isLarge) 17.sp else 14.sp,
                    lineHeight = if (isLarge) 21.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.group.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.group,
                        fontSize = if (isLarge) 12.sp else 11.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        fontSize = if (isLarge) 12.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    CardGlassCircleIcon(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        onClick = onPlayClick
                    )
                }
            }
        }
    }
}

// UNCHANGED: Keeps the 3 media card overlay icons at their exact finalized size (32.dp / 17.dp)
@Composable
private fun CardGlassCircleIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .livelyPress(pressedScale = 0.86f, depthShiftPx = 1.5f, onClick = onClick)
            .clip(CircleShape)
            .background(Color(0x88141720))
            .border(CardIconBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaActionBottomSheet(
    item: MediaItem,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)?,
    onPlay: () -> Unit,
    onOpenExternal: () -> Unit,
    onDownloadBuiltIn: (() -> Unit)?,
    onDownloadExternal: (() -> Unit)?,
    onToggleFavorite: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MatteGlassTokens.SheetBg,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E222A))
                        .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(8.dp))
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.group.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.group,
                            fontSize = 12.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.75.dp, color = MatteGlassTokens.HairlineDivider)
            Spacer(Modifier.height(2.dp))

            if (onViewDetails != null) {
                ActionSheetRow(
                    icon = Icons.Default.Info,
                    label = "View Details",
                    onClick = {
                        onDismiss()
                        onViewDetails()
                    }
                )
            }

            ActionSheetRow(
                icon = Icons.Default.PlayArrow,
                label = if (item.contentType == ContentType.SERIES) "Browse & Play" else "Play Now",
                onClick = {
                    onDismiss()
                    onPlay()
                }
            )

            if (item.contentType != ContentType.SERIES) {
                ActionSheetRow(
                    icon = Icons.Default.OpenInNew,
                    label = "Open Externally",
                    onClick = {
                        onDismiss()
                        onOpenExternal()
                    }
                )
            }

            if (onDownloadBuiltIn != null) {
                ActionSheetRow(
                    icon = Icons.Default.Download,
                    label = "Download (Built-in)",
                    onClick = {
                        onDismiss()
                        onDownloadBuiltIn()
                    }
                )
            }

            if (onDownloadExternal != null) {
                ActionSheetRow(
                    icon = Icons.Default.FileDownload,
                    label = "Download Externally (1DM+ / ADM)",
                    onClick = {
                        onDismiss()
                        onDownloadExternal()
                    }
                )
            }

            ActionSheetRow(
                icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .livelyPress(pressedScale = 0.975f, depthShiftPx = 2f, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(MatteGlassTokens.CardGlassBg)
            .background(LiquidGlassTokens.InnerGlowBrush)
            .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(25.dp) // Scaled up app-wide icon
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MatteGlassTokens.CharcoalPrimary
        )
    }
}
