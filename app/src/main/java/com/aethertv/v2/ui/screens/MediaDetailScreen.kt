package com.aethertv.v2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.EpisodeItem
import com.aethertv.v2.data.MediaDetailInfo
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.GlassCircleActionButton
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun MediaDetailScreen(
    item: MediaItem,
    detailInfo: MediaDetailInfo,
    episodes: List<EpisodeItem>,
    isLoading: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onWatchNow: () -> Unit,
    onDownloadItem: () -> Unit,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onDownloadEpisode: (EpisodeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons = remember(episodes) { episodes.map { it.season }.distinct() }
    var selectedSeason by remember(seasons) { mutableStateOf(seasons.firstOrNull() ?: "") }
    val visibleEpisodes = remember(episodes, selectedSeason) {
        if (selectedSeason.isBlank()) episodes else episodes.filter { it.season == selectedSeason }
    }

    val heroImage = item.posterUrl?.takeIf { it.isNotBlank() } ?: detailInfo.backdropUrl
    val showMovieDownload = item.contentType != ContentType.SERIES && item.contentType != ContentType.LIVE

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(440.dp)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFD4D4D8), Color(0xFF71717A), Color(0xFFFFFFFF))
                            )
                        )
                )

                if (!heroImage.isNullOrBlank()) {
                    AsyncImage(
                        model = heroImage,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MatteGlassTokens.CanvasWhite.copy(alpha = 0.78f),
                                    MatteGlassTokens.CanvasWhite
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleActionButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    GlassCircleActionButton(
                        icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        onClick = onToggleFavorite
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))

                val pills = buildList {
                    add(item.group.ifBlank { item.contentType.name })
                    if (detailInfo.releaseYear.isNotBlank()) add(detailInfo.releaseYear)
                    val ratingText = detailInfo.rating.ifBlank { item.badge }
                    if (ratingText.isNotBlank() && ratingText != "0" && ratingText != "0.0") {
                        add(if (ratingText.startsWith("★") || ratingText == "LIVE") ratingText else "★ $ratingText")
                    }
                    if (detailInfo.duration.isNotBlank()) add(detailInfo.duration)
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(pills) { pillText -> DetailPill(text = pillText) }
                }

                val metaLine = buildList {
                    if (detailInfo.genre.isNotBlank()) add(detailInfo.genre)
                    else if (item.group.isNotBlank()) add(item.group)
                }.joinToString(" • ")

                if (metaLine.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = metaLine,
                        fontSize = 13.sp,
                        color = MatteGlassTokens.CharcoalMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(18.dp))

                val ctaLabel = if (item.contentType == ContentType.SERIES && episodes.isNotEmpty()) {
                    "Play First Episode"
                } else {
                    "Watch Now"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MatteGlassTokens.CharcoalPrimary,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (item.contentType == ContentType.SERIES && episodes.isNotEmpty()) {
                                    onEpisodeClick(episodes.first())
                                } else {
                                    onWatchNow()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = MatteGlassTokens.CanvasWhite, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ctaLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CanvasWhite)
                        }
                    }

                    if (showMovieDownload) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MatteGlassTokens.HeaderPillBg,
                            border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onDownloadItem() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, "Download", tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                            }
                        }
                    }
                }

                if (isLoading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        color = MatteGlassTokens.CharcoalPrimary,
                        trackColor = MatteGlassTokens.HeaderPillBg,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (detailInfo.plot.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = detailInfo.plot,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (item.contentType == ContentType.SERIES && episodes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Episodes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                if (seasons.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(seasons) { season ->
                            val selected = season == selectedSeason
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.HeaderPillBg,
                                border = if (selected) null else BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                                shadowElevation = 0.dp,
                                modifier = Modifier.clip(CircleShape).clickable { selectedSeason = season }
                            ) {
                                Text(
                                    text = season,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            items(visibleEpisodes, key = { it.id }) { ep ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MatteGlassTokens.HeaderPillBg,
                    border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEpisodeClick(ep) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ep.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ep.season, fontSize = 12.sp, color = MatteGlassTokens.CharcoalMuted)
                        }
                        IconButton(onClick = { onDownloadEpisode(ep) }) {
                            Icon(Icons.Default.Download, "Download episode", tint = MatteGlassTokens.CharcoalAction, modifier = Modifier.size(20.dp))
                        }
                        Icon(Icons.Default.PlayArrow, "Play episode", tint = MatteGlassTokens.CharcoalAction, modifier = Modifier.padding(end = 6.dp).size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPill(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MatteGlassTokens.HeaderPillBg,
        border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MatteGlassTokens.CharcoalPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
