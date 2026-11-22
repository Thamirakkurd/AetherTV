package com.aethertv.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.PosterMediaCard
import com.aethertv.v2.ui.components.SimpleScreenHeader
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.hazeSource

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
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .background(MatteGlassTokens.AmbientCanvasBrush),
            contentPadding = PaddingValues(
                top = topInset + 68.dp,
                bottom = navInset + 86.dp + extraBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!hasPlaylist) {
                item(key = "welcome_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MatteGlassTokens.CardGlassBg)
                            .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Welcome to AetherTV",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                        Text(
                            text = "Add an Xtream Codes or M3U playlist to start streaming live TV, movies, and series.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .livelyPress(pressedScale = 0.94f, onClick = onAddPlaylistClick)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.CharcoalPrimary)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MatteGlassTokens.CanvasWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Add Server",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.CanvasWhite
                            )
                        }
                    }
                }
            }

            item(key = "favorites_shelf") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Movies & Series Favorites",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (favoriteMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MatteGlassTokens.CardGlassBg)
                                .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Tap ★ on any movie or series to pin it to your Home shelf.",
                                fontSize = 13.sp,
                                color = MatteGlassTokens.CharcoalMuted
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoriteMedia, key = { it.id }, contentType = { "fav_card" }) { item ->
                                val typeLabel = if (item.contentType == ContentType.SERIES) "Series" else "Movie"
                                val microHeader = if (item.group.isNotBlank()) "$typeLabel • ${item.group}" else typeLabel

                                Column(modifier = Modifier.width(252.dp)) {
                                    Text(
                                        text = microHeader,
                                        fontSize = 13.sp,
                                        color = MatteGlassTokens.CharcoalMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    PosterMediaCard(
                                        item = item,
                                        isFavorite = favoriteIds.contains(item.id),
                                        onCardClick = { onItemClick(item) },
                                        onFavoriteClick = { onFavoriteClick(item) },
                                        onPlayClick = { onPlayClick(item) },
                                        onMoreClick = { onMoreClick(item) },
                                        isLarge = true,
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Watched",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (recentlyWatched.isEmpty()) {
                        Text(
                            text = "Items you play will show up here for quick resume.",
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentlyWatched, key = { "recent_${it.id}" }, contentType = { "recent_card" }) { item ->
                                val thumbShape = RoundedCornerShape(10.dp)
                                val thumbReq = remember(item.posterUrl) {
                                    item.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                        ImageRequest.Builder(context)
                                            .data(url)
                                            .crossfade(true)
                                            .size(260, 360)
                                            .build()
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .width(126.dp)
                                        .livelyPress(pressedScale = 0.955f, depthShiftPx = 3f) {
                                            if (item.contentType == ContentType.LIVE) onPlayClick(item)
                                            else onItemClick(item)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(172.dp)
                                            .clip(thumbShape)
                                            .background(Color(0xFF1F2430))
                                            .border(0.9.dp, MatteGlassTokens.CardRimBrush, thumbShape)
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
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MatteGlassTokens.CharcoalPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
