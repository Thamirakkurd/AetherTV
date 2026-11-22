package com.aethertv.v2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.GlassMediaCard
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun HomeScreen(
    favoriteMedia: List<MediaItem>,
    recentlyWatched: List<MediaItem>,
    favoriteIds: Set<String>,
    hasPlaylist: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onAddPlaylistClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite),
        contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (!hasPlaylist) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MatteGlassTokens.HeaderPillBg,
                    border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Connect your IPTV playlist",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                        Text(
                            text = "Add your Xtream Codes login or M3U URL to load live channels, movies, and series.",
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                        Spacer(Modifier.height(2.dp))
                        Button(
                            onClick = onAddPlaylistClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MatteGlassTokens.CharcoalPrimary,
                                contentColor = MatteGlassTokens.CanvasWhite
                            )
                        ) {
                            Text("Add Playlist")
                        }
                    }
                }
            }
        }

        // Section 1: Movies & Series Favorites
        item {
            Column {
                Text(
                    text = "Movies & Series Favorites",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(10.dp))

                if (favoriteMedia.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MatteGlassTokens.HeaderPillBg,
                        border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Star any movie or series to pin it here.",
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(favoriteMedia, key = { _, item -> item.id }) { idx, item ->
                            val typeLabel = if (item.contentType == ContentType.SERIES) "Series" else "Movie"
                            Column(modifier = Modifier.width(230.dp)) {
                                Text(
                                    text = "$typeLabel • ${item.group}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MatteGlassTokens.CharcoalMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                GlassMediaCard(
                                    title = item.name,
                                    subtitle = item.group,
                                    badgeText = item.badge,
                                    showLiveDot = false,
                                    toneIndex = idx,
                                    imageUrl = item.posterUrl,
                                    isFavorite = favoriteIds.contains(item.id),
                                    onFavoriteClick = { onFavoriteClick(item) },
                                    onClick = { onItemClick(item) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Recently Watched >
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Watched",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (recentlyWatched.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MatteGlassTokens.HeaderPillBg,
                        border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Channels, movies, and episodes you play will appear here.",
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(recentlyWatched, key = { _, item -> item.id }) { idx, item ->
                            CompactRecentCard(
                                item = item,
                                toneIndex = idx,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactRecentCard(
    item: MediaItem,
    toneIndex: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val placeholderBrush = when (toneIndex % 4) {
        0 -> Brush.verticalGradient(listOf(Color(0xFFD4D4D8), Color(0xFF52525B)))
        1 -> Brush.verticalGradient(listOf(Color(0xFFC4C4CA), Color(0xFF3F3F46)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFE4E4E7), Color(0xFF71717A)))
        else -> Brush.verticalGradient(listOf(Color(0xFFD6D3D1), Color(0xFF57534E)))
    }

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(placeholderBrush)
                .border(1.dp, MatteGlassTokens.NeutralBorder, shape)
        ) {
            if (!item.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .border(1.dp, MatteGlassTokens.CardInnerEdge, RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MatteGlassTokens.CharcoalPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.group,
            fontSize = 11.sp,
            color = MatteGlassTokens.CharcoalMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
