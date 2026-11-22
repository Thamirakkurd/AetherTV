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
import androidx.compose.material.icons.filled.Search
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Screen title inside flat matte frosted pill (zero shadow)
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

        // Circular matte frosted action buttons (zero shadow)
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
    badgeText: String,
    showLiveDot: Boolean,
    toneIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    // Neutral monochrome placeholder backdrops for Milestone 1 preview
    val placeholderBrush = when (toneIndex % 4) {
        0 -> Brush.verticalGradient(listOf(Color(0xFFE4E4E7), Color(0xFFA1A1AA)))
        1 -> Brush.verticalGradient(listOf(Color(0xFFD4D4D8), Color(0xFF71717A)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFECECEE), Color(0xFF9F9FA9)))
        else -> Brush.verticalGradient(listOf(Color(0xFFDFDFE2), Color(0xFF8A8A93)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.74f)
            .clip(shape)
            .background(placeholderBrush)
            .border(1.dp, MatteGlassTokens.NeutralBorder, shape)
            .clickable(onClick = onClick)
    ) {
        // Faint 1dp inner glass edge highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(1.dp, MatteGlassTokens.CardInnerEdge, RoundedCornerShape(17.dp))
        )

        // Top-left matte frosted badge + optional red dot
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MatteGlassTokens.BadgeBg,
                border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
                        color = MatteGlassTokens.CharcoalPrimary
                    )
                }
            }
        }

        // Bottom-aligned dark translucent frosted title strip
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MatteGlassTokens.CardTitleOverlay)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
