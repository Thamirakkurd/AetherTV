package com.aethertv.v2.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.LocalGridSpringPhysics
import com.aethertv.v2.ui.components.PosterMediaCard
import com.aethertv.v2.ui.components.SimpleScreenHeader
import com.aethertv.v2.ui.components.rememberGridSpringPhysicsState
import com.aethertv.v2.ui.theme.LiquidGlassTokens
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.hazeSource

private fun Modifier.recentTripleHairlineFrame(
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

@Composable
fun HomeScreen(
    favoriteMedia: List<MediaItem>,
    recentlyWatched: List<MediaItem>,
    favoriteIds: Set<String>,
    hasPlaylist: Boolean,
    listState: LazyListState = rememberLazyListState(),
    extraBottomPadding: Dp = 0.dp,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onMoreClick: (MediaItem) -> Unit = onItemClick,
    onAddPlaylistClick: () -> Unit
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    val physicsState = rememberGridSpringPhysicsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isDark = MatteGlassTokens.isDark

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            physicsState.settle()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalGridSpringPhysics provides physicsState) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                    .background(MatteGlassTokens.AmbientCanvasBrush)
                    .nestedScroll(physicsState.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = topInset + 80.dp,
                    bottom = navInset + 96.dp + extraBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (!hasPlaylist) {
                    item(key = "welcome_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MatteGlassTokens.CardGlassBg)
                                .background(LiquidGlassTokens.InnerGlowBrush)
                                .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(18.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Welcome to AetherTV",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatteGlassTokens.CharcoalPrimary
                            )
                            Text(
                                text = "Add an Xtream Codes or M3U playlist to start streaming live TV, movies, and series.",
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                color = MatteGlassTokens.CharcoalMuted
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .livelyPress(pressedScale = 0.94f, onClick = onAddPlaylistClick)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MatteGlassTokens.CharcoalPrimary)
                                    .padding(horizontal = 18.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MatteGlassTokens.CanvasWhite,
                                    modifier = Modifier.size(23.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "Add Server",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MatteGlassTokens.CanvasWhite
                                )
                            }
                        }
                    }
                }

                item(key = "favorites_shelf") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Movies & Series Favorites",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        if (favoriteMedia.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MatteGlassTokens.CardGlassBg)
                                    .background(LiquidGlassTokens.InnerGlowBrush)
                                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Tap ★ on any movie or series to pin it to your Home shelf.",
                                    fontSize = 13.5.sp,
                                    color = MatteGlassTokens.CharcoalMuted
                                )
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.nestedScroll(physicsState.nestedScrollConnection),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                itemsIndexed(
                                    items = favoriteMedia,
                                    key = { _, item -> item.id },
                                    contentType = { _, _ -> "fav_card" }
                                ) { index, item ->
                                    val typeLabel = if (item.contentType == ContentType.SERIES) "Series" else "Movie"
                                    val microHeader = if (item.group.isNotBlank()) "$typeLabel • ${item.group}" else typeLabel

                                    Column(modifier = Modifier.width(252.dp)) {
                                        Text(
                                            text = microHeader,
                                            fontSize = 12.5.sp,
                                            color = MatteGlassTokens.CharcoalMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        Spacer(Modifier.height(5.dp))
                                        PosterMediaCard(
                                            item = item,
                                            isFavorite = favoriteIds.contains(item.id),
                                            onCardClick = { onItemClick(item) },
                                            onFavoriteClick = { onFavoriteClick(item) },
                                            onPlayClick = { onPlayClick(item) },
                                            onMoreClick = { onMoreClick(item) },
                                            isLarge = true,
                                            gridIndex = index,
                                            gridColumns = 3,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(360.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "recents_shelf") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Watched",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                color = MatteGlassTokens.CharcoalPrimary
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        if (recentlyWatched.isEmpty()) {
                            Text(
                                text = "Items you play will show up here for quick resume.",
                                fontSize = 13.5.sp,
                                color = MatteGlassTokens.CharcoalMuted,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        } else {
                            LazyRow(
                                modifier = Modifier.nestedScroll(physicsState.nestedScrollConnection),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                itemsIndexed(
                                    items = recentlyWatched,
                                    key = { _, item -> "recent_${item.id}" },
                                    contentType = { _, _ -> "recent_card" }
                                ) { index, item ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val pressed by interactionSource.collectIsPressedAsState()
                                    val popScale by animateFloatAsState(
                                        targetValue = if (pressed) 1.05f else 1f,
                                        animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMediumLow),
                                        label = "recentPop"
                                    )
                                    val stagger = 1f + ((index % 3) * 0.14f)
                                    val thumbShape = RoundedCornerShape(LiquidGlassTokens.CardCornerRadius)
                                    val thumbReq = remember(item.posterUrl) {
                                        item.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                            ImageRequest.Builder(context)
                                                .data(url)
                                                .allowHardware(true)
                                                .crossfade(120)
                                                .size(260, 360)
                                                .build()
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .width(126.dp)
                                            .zIndex(if (pressed) 8f else 0f)
                                            .graphicsLayer {
                                                scaleX = popScale
                                                scaleY = popScale
                                                translationY = (if (pressed) -5f else 0f) + (physicsState.verticalImpulse.value * 10f * stagger)
                                                translationX = physicsState.lateralImpulse.value * 8f * stagger
                                                shadowElevation = if (pressed) 16f else 0f
                                            }
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                if (item.contentType == ContentType.LIVE) onPlayClick(item)
                                                else onItemClick(item)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(172.dp)
                                                .recentTripleHairlineFrame(isDark = isDark, isActive = pressed)
                                                .padding(LiquidGlassTokens.CardFrameInset)
                                                .clip(thumbShape)
                                                .background(LiquidGlassTokens.GlassFillDark)
                                                .background(LiquidGlassTokens.InnerGlowBrush)
                                        ) {
                                            if (thumbReq != null) {
                                                AsyncImage(
                                                    model = thumbReq,
                                                    contentDescription = item.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            text = item.name,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MatteGlassTokens.CharcoalPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SimpleScreenHeader(
            title = "Home",
            onAddClick = onAddPlaylistClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
