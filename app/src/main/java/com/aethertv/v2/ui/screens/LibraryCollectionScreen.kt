package com.aethertv.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.LocalGridSpringPhysics
import com.aethertv.v2.ui.components.PosterMediaCard
import com.aethertv.v2.ui.components.rememberGridSpringPhysicsState
import com.aethertv.v2.ui.theme.LiquidGlassTokens
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.liquidGlassSurface
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.hazeSource

@Composable
fun LibraryCollectionScreen(
    title: String,
    items: List<MediaItem>,
    favoriteIds: Set<String>,
    emptyMessage: String,
    extraBottomPadding: Dp = 0.dp,
    onBack: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onMoreClick: (MediaItem) -> Unit = onItemClick,
    onItemLongClick: ((MediaItem) -> Unit)? = null,
    onClearAllClick: (() -> Unit)? = null
) {
    val hazeState = LocalHazeState.current
    val haptics = LocalHapticFeedback.current
    val physicsState = rememberGridSpringPhysicsState()
    val gridState = rememberLazyGridState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val columns = 2

    LaunchedEffect(gridState.isScrollInProgress) {
        if (!gridState.isScrollInProgress) {
            physicsState.settle()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .background(MatteGlassTokens.AmbientCanvasBrush)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        fontSize = 14.sp,
                        color = MatteGlassTokens.CharcoalMuted
                    )
                }
            } else {
                CompositionLocalProvider(LocalGridSpringPhysics provides physicsState) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(physicsState.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = LiquidGlassTokens.GridHorizontalEdgePadding,
                            end = LiquidGlassTokens.GridHorizontalEdgePadding,
                            top = topInset + 66.dp,
                            bottom = navInset + 24.dp + extraBottomPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.GridCellSpacing),
                        verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.GridCellSpacing)
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> "${title}_${item.id}" },
                            contentType = { _, _ -> "poster_card" }
                        ) { index, item ->
                            PosterMediaCard(
                                item = item,
                                isFavorite = favoriteIds.contains(item.id),
                                onCardClick = { onItemClick(item) },
                                onFavoriteClick = { onFavoriteClick(item) },
                                onPlayClick = { onPlayClick(item) },
                                onMoreClick = { onMoreClick(item) },
                                onLongClick = if (onItemLongClick != null) {
                                    {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onItemLongClick(item)
                                    }
                                } else null,
                                isLarge = false,
                                gridIndex = index,
                                gridColumns = columns,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.56f)
                            )
                        }
                    }
                }
            }
        }

        // Bright frosted header with borderless Back icon
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
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
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .livelyPress(pressedScale = 0.88f, depthShiftPx = 1.5f, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = MatteGlassTokens.CharcoalPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onClearAllClick != null && items.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .livelyPress(pressedScale = 0.88f, depthShiftPx = 1.5f, onClick = onClearAllClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = MatteGlassTokens.NotificationDot,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MatteGlassTokens.HairlineDivider)
        }
    }
}
