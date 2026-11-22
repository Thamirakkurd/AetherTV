package com.aethertv.v2.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.ui.components.GlassMediaCard
import com.aethertv.v2.ui.theme.MatteGlassTokens

data class HomePreviewItem(
    val kicker: String,
    val title: String,
    val subtitle: String,
    val badge: String
)

@Composable
fun HomeScreen(
    onItemClick: (HomePreviewItem) -> Unit = {},
    onRecentlyWatchedClick: () -> Unit = {}
) {
    val favoriteMedia = listOf(
        HomePreviewItem("Movie • Sci-Fi", "Interstellar", "2014 • 4K HDR", "MOVIE"),
        HomePreviewItem("Series • Drama", "Shogun", "Season 1 • 10 Episodes", "SERIES"),
        HomePreviewItem("Movie • Action", "Dune: Part Two", "2024 • 4K UHD", "MOVIE"),
        HomePreviewItem("Series • Thriller", "Severance", "Season 2 • 10 Episodes", "SERIES")
    )

    val recentlyWatched = listOf(
        HomePreviewItem("Live TV", "beIN Sports 1 HD", "Sports • Live", "LIVE"),
        HomePreviewItem("Movie", "The Batman", "2022 • Action", "4K"),
        HomePreviewItem("Series", "Arcane", "S2 E04", "HD"),
        HomePreviewItem("Live TV", "Sky Sports Main", "Sports • Live", "LIVE"),
        HomePreviewItem("Movie", "Blade Runner 2049", "2017 • Sci-Fi", "4K")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite),
        contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section 1: Movies & Series Favorites (Large horizontal hero cards)
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

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(favoriteMedia) { idx, item ->
                        Column(modifier = Modifier.width(230.dp)) {
                            Text(
                                text = item.kicker,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MatteGlassTokens.CharcoalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            GlassMediaCard(
                                title = item.title,
                                subtitle = item.subtitle,
                                badgeText = item.badge,
                                showLiveDot = false,
                                toneIndex = idx,
                                onClick = { onItemClick(item) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Recently Watched > (Compact horizontal row)
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRecentlyWatchedClick)
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
                        contentDescription = "See all recently watched",
                        tint = MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(recentlyWatched) { idx, item ->
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

@Composable
private fun CompactRecentCard(
    item: HomePreviewItem,
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .border(1.dp, MatteGlassTokens.CardInnerEdge, RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MatteGlassTokens.CharcoalPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.subtitle,
            fontSize = 11.sp,
            color = MatteGlassTokens.CharcoalMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
