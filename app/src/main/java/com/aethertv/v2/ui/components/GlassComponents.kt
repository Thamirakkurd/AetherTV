package com.aethertv.v2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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

val LocalGridSpringPhysics = compositionLocalOf<GridSpringPhysicsState?> { null }

data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector = selectedIcon
)

private val SearchFieldShape = RoundedCornerShape(20.dp)
private val CardInnerShape = RoundedCornerShape(LiquidGlassTokens.CardCornerRadius)
private val CardGlassTintBrush = Brush.verticalGradient(
    listOf(Color(0x331E2636), Color(0x550B0E15))
)
private val CardTopScrimBrush = Brush.verticalGradient(
    colors = listOf(Color(0x88000000), Color.Transparent)
)
private val CardScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0x88090B10), Color(0xEB07090D))
)

/**
 * Clean, bright full-width frosted bottom bar matching the minimal Apple/iOS style:
 * pure icons + subtle labels, hairline top border, zero circles or heavy beveled pills.
 */
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                hazeState = hazeState,
                shape = RectangleShape,
                tintColor = MatteGlassTokens.NavGlassBg,
                fallbackColor = MatteGlassTokens.NavGlassFallback,
                blurRadius = 16.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp)
        ) {
            val count = items.size.coerceAtLeast(1)
            val tabWidth = maxWidth / count
            val tabWidthPx = with(density) { tabWidth.toPx() }
            val clampedPos = pagePosition.coerceIn(0f, (count - 1).toFloat())
            val fractional = abs(clampedPos - clampedPos.roundToInt())

            // Crisp minimal top sliding indicator driven on GPU
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationX = clampedPos * tabWidthPx
                        scaleX = 1f + fractional * 0.25f
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(MatteGlassTokens.CharcoalPrimary)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        targetValue = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                        animationSpec = tween(150),
                        label = "navTint"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 0.96f,
                        animationSpec = spring(dampingRatio = 0.56f, stiffness = 460f),
                        label = "navScale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .livelyPress(pressedScale = 0.88f, depthShiftPx = 1.5f) { onSelect(index) }
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier
                                .size(25.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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

/**
 * Pure icon action button — circle background & border completely removed.
 */
@Composable
fun GlassCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .livelyPress(pressedScale = 0.88f, depthShiftPx = 1.5f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(26.dp)
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
                blurRadius = 16.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MatteGlassTokens.CharcoalPrimary
            )
            if (onAddClick != null) {
                // Pure icon — no circle around +
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .livelyPress(pressedScale = 0.86f, depthShiftPx = 1.5f, onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Server",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
    }
}

/**
 * Clean, airy frosted top bar: pure icons + text, no circles, no dark beveled boxes.
 */
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
                blurRadius = 16.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Flat, bright search input (no dark gradient bevel)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(SearchFieldShape)
                    .background(MatteGlassTokens.FieldGlassBg)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MatteGlassTokens.CharcoalMuted,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(8.dp))
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
                            .size(20.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }

            // Category selector: clean text + chevron icon (no box/pill border)
            if (categories.isNotEmpty()) {
                Box {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(max = 128.dp)
                            .livelyPress(pressedScale = 0.94f, depthShiftPx = 1.5f) { menuExpanded = true }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayCategory,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Categories",
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(22.dp)
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

            // Favorites toggle: pure icon, no circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .livelyPress(pressedScale = 0.86f, depthShiftPx = 1.5f) {
                        if (isFavorites) {
                            val fallback = lastNonFavCategory.ifBlank {
                                categories.firstOrNull { it != "All" && it != "Favorites" }
                                    ?: categories.firstOrNull() ?: "All"
                            }
                            onSelectCategory(fallback)
                        } else {
                            onSelectCategory("Favorites")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorites",
                    tint = MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
    }
}

/**
 * Tight, razor-sharp 3-line hairline portrait frame (4.5dp -> 4.0dp -> 3.5dp concentric with 3.0dp poster).
 */
fun Modifier.tripleHairlinePortraitFrame(
    isDark: Boolean,
    isActive: Boolean
): Modifier = this.drawWithCache {
    val strokePx = LiquidGlassTokens.FrameLineThickness.toPx().coerceAtLeast(1f)
    val gapPx = LiquidGlassTokens.FrameLineGap.toPx()
    val outerRadiusPx = LiquidGlassTokens.FrameOuterCornerRadius.toPx()
    val radiusStepPx = 0.5.dp.toPx()
    val stroke = Stroke(width = strokePx)

    val boost = if (isActive) 1.75f else 1f
    val baseColor = if (isDark) Color.White else Color(0xFF14161A)
    val ringColors = listOf(
        baseColor.copy(alpha = (0.12f * boost).coerceAtMost(0.42f)),
        baseColor.copy(alpha = (0.19f * boost).coerceAtMost(0.56f)),
        baseColor.copy(alpha = (0.28f * boost).coerceAtMost(0.72f))
    )

    onDrawWithContent {
        drawContent()
        for (ring in 0 until 3) {
            val inset = ring * (strokePx + gapPx) + (strokePx / 2f)
            val radius = (outerRadiusPx - (ring * radiusStepPx)).coerceAtLeast(1.5f)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterMediaCard(
    item: MediaItem,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onMoreClick: () -> Unit = onCardClick,
    onLongClick: (() -> Unit)? = null,
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
                .allowHardware(true)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .precision(coil.size.Precision.INEXACT)
                .crossfade(false)
                .size(if (isLarge) 480 else 320, if (isLarge) 700 else 480)
                .build()
        }
    }

    val col = gridIndex % gridColumns.coerceAtLeast(1)
    val staggerFactor = 1f + (col * 0.14f)

    Box(
        modifier = modifier
            .zIndex(if (active) 10f else 0f)
            .graphicsLayer {
                val vImpulse = physicsState?.verticalImpulse ?: 0f
                val hImpulse = physicsState?.lateralImpulse ?: 0f

                scaleX = popScale
                scaleY = popScale
                translationY = popLiftY + (vImpulse * 11f * staggerFactor)
                translationX = hImpulse * 8f * staggerFactor
                shadowElevation = if (active) 20f else 0f
            }
            .tripleHairlinePortraitFrame(isDark = isDark, isActive = active)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardClick,
                onLongClick = onLongClick
            )
            .padding(LiquidGlassTokens.CardFrameInset)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardInnerShape)
                .background(CardGlassTintBrush)
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle top scrim so borderless white icons stay sharp over bright posters
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.24f)
                    .background(CardTopScrimBrush)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.56f)
                    .background(CardScrimBrush)
            )

            if (pressGlintAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = pressGlintAlpha))
                )
            }

            // Clean icons only — no circles around Star or More
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                    .padding(horizontal = 9.dp, vertical = 8.dp)
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
                Spacer(Modifier.height(5.dp))
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
                    // Pure Play icon — no circle
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

/**
 * Borderless pure white media card icon (circle background & border removed).
 */
@Composable
private fun CardGlassCircleIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .livelyPress(pressedScale = 0.84f, depthShiftPx = 1.5f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E222A))
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.group.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.group,
                            fontSize = 12.5.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)

            if (onViewDetails != null) {
                ActionSheetRow(
                    icon = Icons.Default.Info,
                    label = "View Details",
                    onClick = {
                        onDismiss()
                        onViewDetails()
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
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
                HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
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
                HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
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
                HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
                ActionSheetRow(
                    icon = Icons.Default.FileDownload,
                    label = "Download Externally (1DM+ / ADM)",
                    onClick = {
                        onDismiss()
                        onDownloadExternal()
                    }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
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

/**
 * Clean flat row — just crisp icon + text, no beveled box or border.
 */
@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .livelyPress(pressedScale = 0.98f, depthShiftPx = 1.5f, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(25.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            color = MatteGlassTokens.CharcoalPrimary
        )
    }
}
