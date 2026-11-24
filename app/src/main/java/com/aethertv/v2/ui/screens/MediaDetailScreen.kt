package com.aethertv.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.EpisodeItem
import com.aethertv.v2.data.MediaDetailInfo
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.GlassCircleActionButton
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress

private val DetailCtaShape = RoundedCornerShape(12.dp)

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
    val context = LocalContext.current
    val seasons = remember(episodes) { episodes.map { it.season }.distinct() }
    var selectedSeason by remember(seasons) { mutableStateOf(seasons.firstOrNull() ?: "") }
    val visibleEpisodes = remember(episodes, selectedSeason) {
        if (selectedSeason.isBlank()) episodes else episodes.filter { it.season == selectedSeason }
    }

    val heroImage = item.posterUrl?.takeIf { it.isNotBlank() } ?: detailInfo.backdropUrl
    val heroRequest = remember(heroImage) {
        heroImage?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(900, 1200)
                .build()
        }
    }
    val isHttpStream = item.streamUrl.startsWith("http://", ignoreCase = true) ||
        item.streamUrl.startsWith("https://", ignoreCase = true)
    val showMovieDownload = item.contentType != ContentType.SERIES && item.contentType != ContentType.LIVE && isHttpStream
    val topScrimColor = if (MatteGlassTokens.isDark) Color.Black.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.45f)

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
                                listOf(
                                    Color(0xFF2B3240),
                                    Color(0xFF1A1F29),
                                    MatteGlassTokens.CanvasWhite
                                )
                            )
                        )
                )

                if (heroRequest != null) {
                    AsyncImage(
                        model = heroRequest,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(92.dp)
                        .background(Brush.verticalGradient(colors = listOf(topScrimColor, Color.Transparent)))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MatteGlassTokens.CanvasWhite.copy(alpha = 0.80f),
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
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .livelyPress(
                                pressedScale = 0.96f,
                                depthShiftPx = 3f,
                                shape = DetailCtaShape,
                                rippleColor = MatteGlassTokens.CanvasWhite
                            ) {
                                if (item.contentType == ContentType.SERIES && episodes.isNotEmpty()) {
                                    onEpisodeClick(episodes.first())
                                } else {
                                    onWatchNow()
                                }
                            }
                            .background(MatteGlassTokens.CharcoalPrimary),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = MatteGlassTokens.CanvasWhite, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(ctaLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CanvasWhite)
                    }

                    if (showMovieDownload) {
                        Row(
                            modifier = Modifier
                                .height(50.dp)
                                .livelyPress(
                                    pressedScale = 0.95f,
                                    depthShiftPx = 3f,
                                    shape = DetailCtaShape,
                                    onClick = onDownloadItem
                                )
                                .background(MatteGlassTokens.CardGlassBg)
                                .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, DetailCtaShape)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, "Download", tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                        }
                    }
                }

                if (isLoading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        color = MatteGlassTokens.CharcoalPrimary,
                        trackColor = MatteGlassTokens.FieldGlassBg,
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
                            Box(
                                modifier = Modifier
                                    .livelyPress(
                                        pressedScale = 0.94f,
                                        shape = CircleShape,
                                        rippleColor = if (selected) MatteGlassTokens.CanvasWhite else Color.Unspecified
                                    ) { selectedSeason = season }
                                    .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                    .border(
                                        0.8.dp,
                                        if (selected) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                        CircleShape
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = season,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            itemsIndexed(visibleEpisodes, key = { idx, ep -> "${idx}_${ep.id}" }) { _, ep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .livelyPress(
                            pressedScale = 0.975f,
                            depthShiftPx = 2f,
                            shape = DetailCtaShape
                        ) { onEpisodeClick(ep) }
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, DetailCtaShape)
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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

@Composable
private fun DetailPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MatteGlassTokens.CardGlassBg)
            .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MatteGlassTokens.CharcoalPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
