package com.aethertv.v2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.GlassSearchPill
import com.aethertv.v2.ui.components.MediaPosterCard
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun MediaGridScreen(
    items: List<MediaItem>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    emptyMessage: String,
    showSearchInRow: Boolean = false,
    searchQuery: String = "",
    searchPlaceholder: String = "Search...",
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onSelectCategory: (String) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (categories.isNotEmpty() || showSearchInRow) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSearchInRow) {
                    item(key = "unified_search_pill") {
                        GlassSearchPill(
                            query = searchQuery,
                            placeholder = searchPlaceholder,
                            onQueryChange = onSearchQueryChange,
                            onClose = onCloseSearch
                        )
                    }
                }
                items(categories, key = { it }) { cat ->
                    val selected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .shadow(
                                elevation = if (selected) 8.dp else 4.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.06f),
                                spotColor = Color.Black.copy(alpha = 0.06f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    Brush.verticalGradient(
                                        listOf(MatteGlassTokens.CharcoalPrimary, Color(0xFF2B313C))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.90f),
                                            Color(0xFFF2F4F8).copy(alpha = 0.84f)
                                        )
                                    )
                                }
                            )
                            .border(
                                BorderStroke(
                                    1.2.dp,
                                    if (selected) Color.White.copy(alpha = 0.45f) else Color.White
                                ),
                                CircleShape
                            )
                            .clickable { onSelectCategory(cat) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) Color.White else MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MatteGlassTokens.CharcoalPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (items.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    MediaPosterCard(
                        item = item,
                        isFavorite = favoriteIds.contains(item.id),
                        onCardClick = { onItemClick(item) },
                        onFavoriteClick = { onFavoriteClick(item) },
                        onPlayClick = { onPlayClick(item) }
                    )
                }
            }
        }
    }
}
