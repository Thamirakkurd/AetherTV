package com.aethertv.v2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.theme.MatteGlassTokens

data class NavTabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun FloatingGlassNavBar(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MatteGlassTokens.NavGlassBg,
        border = BorderStroke(1.dp, MatteGlassTokens.GlassNavBorder),
        shadowElevation = 6.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (selected) MatteGlassTokens.ActivePillBg else Color.Transparent
                        )
                        .then(
                            if (selected) Modifier.border(
                                BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                                CircleShape
                            ) else Modifier
                        )
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun GlassCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color(0xEBFFFFFF),
        border = BorderStroke(1.dp, MatteGlassTokens.GlassNavBorder),
        shadowElevation = 0.dp,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MatteGlassTokens.CharcoalPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SimpleScreenHeader(
    title: String,
    onAddClick: (() -> Unit)? = null,
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
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            color = MatteGlassTokens.CharcoalPrimary
        )
        if (onAddClick != null) {
            Surface(
                shape = CircleShape,
                color = MatteGlassTokens.HeaderPillBg,
                border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onAddClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Server",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedMediaTopBar(
    searchQuery: String,
    searchPlaceholder: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var lastNonFavCategory by remember { mutableStateOf("") }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory.isNotBlank() && selectedCategory != "Favorites") {
            lastNonFavCategory = selectedCategory
        }
    }

    val isFavorites = selectedCategory == "Favorites"
    val displayCategory = when {
        selectedCategory.isBlank() -> "Category"
        selectedCategory == "Favorites" -> "Favorites"
        else -> selectedCategory
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MatteGlassTokens.HeaderPillBg,
            border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MatteGlassTokens.CharcoalMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 14.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = MatteGlassTokens.CharcoalPrimary),
                        cursorBrush = SolidColor(MatteGlassTokens.CharcoalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .width(1.dp).height(22.dp)
                            .background(MatteGlassTokens.GlassNavBorder)
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { menuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .widthIn(max = 128.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayCategory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Categories",
                                tint = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .background(MatteGlassTokens.CanvasWhite)
                                .widthIn(min = 190.dp, max = 280.dp)
                                .heightIn(max = 420.dp)
                        ) {
                            categories.forEach { cat ->
                                val active = cat == selectedCategory
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 13.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                color = MatteGlassTokens.CharcoalPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (active) {
                                                Spacer(Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MatteGlassTokens.CharcoalPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onSelectCategory(cat)
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp).height(22.dp)
                        .background(MatteGlassTokens.GlassNavBorder)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isFavorites) MatteGlassTokens.CharcoalPrimary else Color.Transparent)
                        .clickable {
                            if (isFavorites) {
                                val fallback = lastNonFavCategory.ifBlank {
                                    categories.firstOrNull { it != "All" && it != "Favorites" }
                                        ?: categories.firstOrNull() ?: "All"
                                }
                                onSelectCategory(fallback)
                            } else {
                                onSelectCategory("Favorites")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorites",
                        tint = if (isFavorites) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PosterMediaCard(
    item: MediaItem,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlayClick: () -> Unit,
    isLarge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(6.dp)
    val badgeText = remember(item.badge, item.contentType) {
        val raw = item.badge.trim()
        when {
            raw.isNotBlank() && raw != "0" && raw != "0.0" -> {
                if (raw.startsWith("★") || raw.equals("SERIES", true) || raw.equals("LIVE", true)) raw
                else "★ $raw"
            }
            item.contentType == ContentType.SERIES -> "SERIES"
            else -> ""
        }
    }

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF27272A), Color(0xFF18181B))))
            .border(BorderStroke(1.dp, Color(0x1F000000)), cardShape)
            .clickable { onCardClick() }
    ) {
        if (!item.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.56f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x9909090B), Color(0xEA09090B))
                    )
                )
        )

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardGlassCircleIcon(
                icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite",
                onClick = onFavoriteClick
            )
            CardGlassCircleIcon(
                icon = Icons.Default.MoreHoriz,
                contentDescription = "Details",
                onClick = onCardClick
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = item.name,
                fontSize = if (isLarge) 17.sp else 14.sp,
                lineHeight = if (isLarge) 21.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.group.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.group,
                    fontSize = if (isLarge) 12.sp else 11.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badgeText,
                    fontSize = if (isLarge) 12.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                CardGlassCircleIcon(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    onClick = onPlayClick
                )
            }
        }
    }
}

@Composable
private fun CardGlassCircleIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x8818181B))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}
