package com.aethertv.v2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
fun GlassSearchStrip(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = MatteGlassTokens.CharcoalMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MatteGlassTokens.CharcoalAction) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close search", tint = MatteGlassTokens.CharcoalMuted)
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MatteGlassTokens.HeaderPillBg,
            unfocusedContainerColor = MatteGlassTokens.HeaderPillBg,
            focusedBorderColor = MatteGlassTokens.CharcoalPrimary,
            unfocusedBorderColor = MatteGlassTokens.NeutralBorder,
            focusedTextColor = MatteGlassTokens.CharcoalPrimary,
            unfocusedTextColor = MatteGlassTokens.CharcoalPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun CategoryChipRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    if (categories.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(categories, key = { it }) { cat ->
            val isSelected = cat == selected
            Surface(
                shape = CircleShape,
                color = if (isSelected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.HeaderPillBg,
                border = if (isSelected) null else BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelect(cat) }
            ) {
                Text(
                    text = cat,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
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
    imageUrl: String? = null,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
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
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // Subtle 1dp inner white glass frame
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(1.dp, MatteGlassTokens.CardInnerEdge, RoundedCornerShape(3.dp))
        )

        // Top-right translucent circular action icons + quick menu
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CardTopCircleIcon(
                icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite",
                onClick = onFavoriteClick
            )
            Box {
                CardTopCircleIcon(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = "More",
                    onClick = { showMenu = true }
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MatteGlassTokens.CanvasWhite
                ) {
                    DropdownMenuItem(
                        text = { Text("Details", color = MatteGlassTokens.CharcoalPrimary) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Now", color = MatteGlassTokens.CharcoalPrimary) },
                        onClick = {
                            showMenu = false
                            onPlayClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isFavorite) "Remove Favorite" else "Add to Favorites",
                                color = MatteGlassTokens.CharcoalPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onFavoriteClick()
                        }
                    )
                }
            }
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
                            Color(0xA618181B),
                            Color(0xEB18181B)
                        )
                    )
                )
                .padding(start = 10.dp, end = 10.dp, top = 32.dp, bottom = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
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
                        text = badgeText.ifEmpty { "HD" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Now",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTopCircleIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0x6618181B))
            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            .clickable(onClick = onClick),
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

@Composable
fun AddPlaylistDialog(
    initialHost: String,
    initialUser: String,
    initialPass: String,
    initialM3u: String,
    onDismiss: () -> Unit,
    onLoadXtream: (String, String, String) -> Unit,
    onLoadM3u: (String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    var host by remember { mutableStateOf(initialHost) }
    var user by remember { mutableStateOf(initialUser) }
    var pass by remember { mutableStateOf(initialPass) }
    var m3uUrl by remember { mutableStateOf(initialM3u) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MatteGlassTokens.CanvasWhite,
        title = {
            Text(
                text = "Add Playlist",
                fontWeight = FontWeight.Bold,
                color = MatteGlassTokens.CharcoalPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        label = { Text("Xtream Codes") }
                    )
                    FilterChip(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        label = { Text("M3U URL") }
                    )
                }
                if (mode == 0) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Server URL (http://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("M3U / M3U8 URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == 0 && host.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                        onLoadXtream(host, user, pass)
                    } else if (mode == 1 && m3uUrl.isNotBlank()) {
                        onLoadM3u(m3uUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatteGlassTokens.CharcoalPrimary,
                    contentColor = MatteGlassTokens.CanvasWhite
                )
            ) {
                Text("Load")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MatteGlassTokens.CharcoalMuted)
            }
        }
    )
}
