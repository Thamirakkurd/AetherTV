package com.aethertv.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.PosterMediaCard
import com.aethertv.v2.ui.components.UnifiedMediaTopBar
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun MediaGridScreen(
    items: List<MediaItem>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    emptyMessage: String,
    searchQuery: String = "",
    searchPlaceholder: String = "Search...",
    onSearchQueryChange: (String) -> Unit = {},
    onSelectCategory: (String) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
    ) {
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
                trackColor = MatteGlassTokens.HeaderPillBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    PosterMediaCard(
                        item = item,
                        isFavorite = favoriteIds.contains(item.id),
                        onCardClick = { onItemClick(item) },
                        onFavoriteClick = { onFavoriteClick(item) },
                        onPlayClick = { onPlayClick(item) },
                        isLarge = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.66f)
                    )
                }
            }
        }
    }
}
