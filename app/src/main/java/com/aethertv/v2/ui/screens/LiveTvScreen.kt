package com.aethertv.v2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.UnifiedMediaTopBar
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.hazeSource

private val ChannelRowShape = RoundedCornerShape(12.dp)
private val ChannelLogoShape = RoundedCornerShape(8.dp)

@Composable
fun LiveTvScreen(
    items: List<MediaItem>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    emptyMessage: String,
    listState: LazyListState = rememberLazyListState(),
    topBarVisible: Boolean = true,
    extraBottomPadding: Dp = 0.dp,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onFavoriteClick: (MediaItem) -> Unit,
    onMoreClick: (MediaItem) -> Unit = {}
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = topInset + 64.dp,
                        bottom = navInset + 86.dp + extraBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.id }, contentType = { "live_channel" }) { channel ->
                        val isFav = favoriteIds.contains(channel.id)
                        val subtitleText = remember(channel.group, channel.badge) {
                            buildString {
                                if (channel.group.isNotBlank()) append(channel.group)
                                val extra = channel.badge.trim()
                                if (extra.isNotBlank() && !extra.equals("LIVE", ignoreCase = true)) {
                                    if (isNotEmpty()) append(" • ")
                                    append(extra)
                                }
                            }
                        }
                        val logoReq = remember(channel.posterUrl) {
                            channel.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .crossfade(true)
                                    .size(144, 144)
                                    .build()
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .livelyPress(pressedScale = 0.978f, depthShiftPx = 2.5f) {
                                    onPlayClick(channel)
                                }
                                .clip(ChannelRowShape)
                                .background(MatteGlassTokens.CardGlassBg)
                                .border(1.1.dp, MatteGlassTokens.SpecularBorderBrush, ChannelRowShape)
                                .padding(start = 10.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(ChannelLogoShape)
                                    .background(MatteGlassTokens.SubtleSurface)
                                    .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, ChannelLogoShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoReq != null) {
                                    AsyncImage(
                                        model = logoReq,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = MatteGlassTokens.CharcoalMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MatteGlassTokens.CharcoalPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (subtitleText.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = subtitleText,
                                        fontSize = 12.sp,
                                        color = MatteGlassTokens.CharcoalMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MatteGlassTokens.FieldGlassBg)
                                    .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(5.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MatteGlassTokens.CharcoalPrimary
                                )
                            }

                            IconButton(
                                onClick = { onFavoriteClick(channel) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite channel",
                                    tint = if (isFav) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoreClick(channel) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Channel options",
                                    tint = MatteGlassTokens.CharcoalMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                    searchPlaceholder = "Search live channels...",
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
