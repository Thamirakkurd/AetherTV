package com.aethertv.v2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlin.math.roundToInt
import kotlin.math.abs
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.liquidGlassSurface
import com.aethertv.v2.ui.theme.livelyPress

data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector = selectedIcon
)

private val NavFieldShape = RoundedCornerShape(18.dp)
private val CardShape = RoundedCornerShape(4.dp)
private val CardBgBrush = Brush.verticalGradient(listOf(Color(0xFF232833), Color(0xFF11141B)))
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
    val capsuleShape = RoundedCornerShape(30.dp)
    val activePillShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (MatteGlassTokens.isDark) 16.dp else 11.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.26f)
                )
                .liquidGlassSurface(
                    hazeState = hazeState,
                    shape = capsuleShape,
                    tintColor = MatteGlassTokens.NavGlassBg,
                    fallbackColor = MatteGlassTokens.NavGlassFallback,
                    blurRadius = 32.dp,
                    borderWidth = 1.dp,
                    borderBrush = MatteGlassTokens.SpecularBorderBrush
                )
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
                        targetValue = if (selected) 1.09f else 0.95f,
                        animationSpec = spring(dampingRatio = 0.52f, stiffness = 460f),
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
                                .size(21.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
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
    val bg = if (MatteGlassTokens.isDark) Color(0xB3121722) else Color(0xD9FFFFFF)
    val rim = if (MatteGlassTokens.isDark) Color.White.copy(alpha = 0.28f) else MatteGlassTokens.GlassNavBorder

    Box(
        modifier = modifier
            .size(42.dp)
            .livelyPress(pressedScale = 0.91f, depthShiftPx = 2f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(BorderStroke(0.9.dp, rim), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(20.dp)
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
                blurRadius = 32.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = MatteGlassTokens.CharcoalPrimary
            )
            if (onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .livelyPress(pressedScale = 0.90f, depthShiftPx = 2f, onClick = onAddClick)
                        .clip(CircleShape)
                        .background(MatteGlassTokens.FieldGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Server",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(19.dp)
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
                blurRadius = 32.dp,
                borderWidth = 0.dp,
                borderBrush = null
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(54.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(NavFieldShape)
                    .background(MatteGlassTokens.FieldGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, NavFieldShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MatteGlassTokens.CharcoalMuted,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = MatteGlassTokens.CharcoalPrimary),
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
                            .size(17.dp)
                            .clip(CircleShape)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Box {
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .widthIn(max = 136.dp)
                            .livelyPress(pressedScale = 0.94f, depthShiftPx = 2f) { menuExpanded = true }
                            .clip(NavFieldShape)
                            .background(MatteGlassTokens.FieldGlassBg)
                            .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, NavFieldShape)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayCategory,
                            fontSize = 12.sp,
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
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(MatteGlassTokens.SheetBg)
                            .widthIn(min = 190.dp, max = 280.dp)
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
                                            fontSize = 13.sp,
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
                                                modifier = Modifier.size(16.dp)
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
                    .size(38.dp)
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
                        0.85.dp,
                        if (isFavorites) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorites",
                    tint = if (isFavorites) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        HorizontalDivider(thickness = 0.75.dp, color = MatteGlassTokens.HairlineDivider)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.956f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "cardScale"
    )
    val depthY by animateFloatAsState(
        targetValue = if (pressed) 4f else 0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 420f),
        label = "cardDepth"
    )
    val pressGlintAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.16f else 0f,
        animationSpec = tween(130),
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
                .crossfade(true)
                .size(if (isLarge) 540 else 360, if (isLarge) 780 else 600)
                .build()
        }
    }

    val frameShape = RoundedCornerShape(6.dp)
    val frameBg = if (MatteGlassTokens.isDark) Color(0x6B1C2638) else Color(0xEBFFFFFF)
    val outerFrameBrush = if (MatteGlassTokens.isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.58f),
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.46f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0x8C14161A),
                Color(0x4D14161A),
                Color(0x7A14161A)
            )
        )
    }
    val innerPosterRim = if (MatteGlassTokens.isDark) Color.White.copy(alpha = 0.28f) else Color(0x3D14161A)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = depthY
                cameraDistance = 12f * density.density
            }
            .shadow(
                elevation = if (MatteGlassTokens.isDark) 4.dp else 2.5.dp,
                shape = frameShape,
                ambientColor = Color.Black.copy(alpha = 0.24f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(frameShape)
            .background(frameBg)
            .border(1.25.dp, outerFrameBrush, frameShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardClick
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShape)
                .background(CardBgBrush)
                .border(0.75.dp, innerPosterRim, CardShape)
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

            // Tactile glass glint on press
            if (pressGlintAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = pressGlintAlpha))
                )
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .background(MatteGlassTokens.CardGlassBg)
            .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MatteGlassTokens.CharcoalPrimary
        )
    }
}
