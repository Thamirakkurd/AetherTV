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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.GlassSearchPill
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

@Composable
private fun MediaPosterCard(
    item: MediaItem,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C2028))
            .clickable(onClick = onCardClick)
    ) {
        if (!item.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onFavoriteClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MatteGlassTokens.NotificationDot else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.group.isNotBlank()) {
                    Text(
                        text = item.group,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), CircleShape)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
