package com.aethertv.v2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun GlassTopHeader(
    title: String,
    categories: List<String> = emptyList(),
    selectedCategory: String = "",
    onSelectCategory: (String) -> Unit = {},
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MatteGlassTokens.CharcoalPrimary,
            letterSpacing = (-0.4).sp
        )

        Row(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color(0xFFF2F4F8).copy(alpha = 0.84f)
                        )
                    )
                )
                .border(BorderStroke(1.2.dp, Color.White), CircleShape)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderPillIcon(Icons.Default.Search, "Search", onSearchClick)

            Box {
                HeaderPillIcon(Icons.Default.FilterList, "Categories") {
                    if (categories.isNotEmpty()) {
                        categoryMenuExpanded = true
                    }
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .background(Color.White)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = cat,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                categoryMenuExpanded = false
                                onSelectCategory(cat)
                            }
                        )
                    }
                }
            }

            HeaderPillIcon(Icons.Default.Add, "Add Server", onAddClick)
        }
    }
}

@Composable
private fun HeaderPillIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun GlassSearchPill(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.07f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color(0xFFF2F4F8).copy(alpha = 0.85f)
                    )
                )
            )
            .border(BorderStroke(1.2.dp, Color.White), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MatteGlassTokens.CharcoalMuted,
            modifier = Modifier.size(15.dp)
        )
        Box(
            modifier = Modifier.widthIn(min = 95.dp, max = 145.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = MatteGlassTokens.CharcoalMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MatteGlassTokens.CharcoalPrimary
                ),
                cursorBrush = SolidColor(MatteGlassTokens.CharcoalPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Search",
                tint = MatteGlassTokens.CharcoalPrimary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
