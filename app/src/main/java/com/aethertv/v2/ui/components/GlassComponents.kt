package com.aethertv.v2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.matteGlassBorder
import com.aethertv.v2.ui.theme.matteInnerHighlightBrush

data class NavTabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun GlassTopHeader(
    title: String,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MatteGlassTokens.HeaderPillBg,
            border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
            shadowElevation = 0.dp
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MatteGlassTokens.CharcoalPrimary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleActionButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = onSearchClick)
            GlassCircleActionButton(icon = Icons.Default.Tune, contentDescription = "Filter", onClick = onFilterClick)
            GlassCircleActionButton(icon = Icons.Default.Add, contentDescription = "Add", onClick = onAddClick)
        }
    }
}

@Composable
fun GlassCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MatteGlassTokens.HeaderPillBg,
        border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MatteGlassTokens.CharcoalAction,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FloatingGlassNavBar(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MatteGlassTokens.NavGlassBg,
            border = matteGlassBorder(),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(matteInnerHighlightBrush())
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val pillModifier = if (selected) {
                        Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(MatteGlassTokens.ActivePillBg)
                            .border(1.dp, MatteGlassTokens.NeutralBorder, RoundedCornerShape(26.dp))
                    } else {
                        Modifier.clip(RoundedCornerShape(26.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .then(pillModifier)
                            .clickable { onSelect(index) }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassMediaCard(
    title: String,
    subtitle: String,
    badgeText: String,
    showLiveDot: Boolean,
    toneIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    val placeholderBrush = when (toneIndex % 4) {
        0 -> Brush.verticalGradient(listOf(Color(0xFFD4D4D8), Color(0xFF52525B), Color(0xFF18181B)))
        1 -> Brush.verticalGradient(listOf(Color(0xFFC4C4CA), Color(0xFF3F3F46), Color(0xFF18181B)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFE4E4E7), Color(0xFF71717A), Color(0xFF27272A)))
        else -> Brush.verticalGradient(listOf(Color(0xFFD6D3D1), Color(0xFF57534E), Color(0xFF1C1917)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.58f)
            .clip(shape)
            .background(placeholderBrush)
            .border(1.dp, MatteGlassTokens.NeutralBorder, shape)
            .clickable(onClick = onClick)
    ) {
        // Subtle 1dp inner white glass frame
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(1.dp, MatteGlassTokens.CardInnerEdge, RoundedCornerShape(3.dp))
        )

        // Top-right translucent circular action icons (reference style)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CardTopCircleIcon(icon = Icons.Default.StarBorder, contentDescription = "Favorite")
            CardTopCircleIcon(icon = Icons.Default.MoreHoriz, contentDescription = "More")
        }

        // Bottom smooth dark scrim + title, subtitle, and metadata row
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x9918181B),
                            Color(0xE618181B)
                        )
                    )
                )
                .padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (showLiveDot) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MatteGlassTokens.NotificationDot)
                        )
                    }
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTopCircleIcon(
    icon: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0x5218181B))
            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
    }
}
