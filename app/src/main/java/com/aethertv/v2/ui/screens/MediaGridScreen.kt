package com.aethertv.v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.CategoryChipRow
import com.aethertv.v2.ui.components.GlassMediaCard
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun MediaGridScreen(
    items: List<MediaItem>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    emptyMessage: String,
    onSelectCategory: (String) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryChipRow(
            categories = categories,
            selected = selectedCategory,
            onSelect = onSelectCategory
        )

        if (isLoading) {
            LinearProgressIndicator(
                color = MatteGlassTokens.CharcoalPrimary,
                trackColor = MatteGlassTokens.HeaderPillBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }

        if (items.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
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
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 104.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { idx, item ->
                    GlassMediaCard(
                        title = item.name,
                        subtitle = item.group,
                        badgeText = item.badge,
                        showLiveDot = item.contentType == ContentType.LIVE,
                        toneIndex = idx,
                        imageUrl = item.posterUrl,
                        isFavorite = favoriteIds.contains(item.id),
                        onFavoriteClick = { onFavoriteClick(item) },
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}
