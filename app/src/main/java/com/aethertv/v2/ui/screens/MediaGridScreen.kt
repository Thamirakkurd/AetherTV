package com.aethertv.v2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.LocalGridSpringPhysics
import com.aethertv.v2.ui.components.PosterMediaCard
import com.aethertv.v2.ui.components.UnifiedMediaTopBar
import com.aethertv.v2.ui.components.rememberGridSpringPhysicsState
import com.aethertv.v2.ui.theme.LiquidGlassTokens
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import dev.chrisbanes.haze.hazeSource

@Composable
fun MediaGridScreen(
    items: List<MediaItem>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    emptyMessage: String,
    gridState: LazyGridState = rememberLazyGridState(),
    topBarVisible: Boolean = true,
    extraBottomPadding: Dp = 0.dp,
    searchQuery: String = "",
    searchPlaceholder: String = "Search...",
    onSearchQueryChange: (String) -> Unit = {},
    onSelectCategory: (String) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onMoreClick: (MediaItem) -> Unit = onItemClick
) {
    val hazeState = LocalHazeState.current
    val physicsState = rememberGridSpringPhysicsState()
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
            if (items.isEmpty() && !isLoading) {
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
                            top = topInset + 72.dp,
                            bottom = navInset + 96.dp + extraBottomPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.GridCellSpacing),
                        verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.GridCellSpacing)
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> "poster_card" }
                        ) { index, item ->
                            PosterMediaCard(
                                item = item,
                                isFavorite = favoriteIds.contains(item.id),
                                onCardClick = { onItemClick(item) },
                                onFavoriteClick = { onFavoriteClick(item) },
                                onPlayClick = { onPlayClick(item) },
                                onMoreClick = { onMoreClick(item) },
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

        AnimatedVisibility(
            visible = topBarVisible,
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = 360f)) { -it } + fadeIn(tween(180)),
            exit = slideOutVertically(spring(dampingRatio = 0.86f, stiffness = 400f)) { -it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                UnifiedMediaTopBar(
                    searchQuery = searchQuery,
                    searchPlaceholder = searchPlaceholder,
                    onSearchQueryChange = onSearchQueryChange,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = onSelectCategory
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        color = MatteGlassTokens.CharcoalPrimary,
                        trackColor = MatteGlassTokens.FieldGlassBg,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
