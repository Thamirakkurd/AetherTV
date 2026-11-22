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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MatteGlassTokens.CharcoalPrimary,
            letterSpacing = (-0.4).sp
        )

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MatteGlassTokens.GlassSurface)
                .border(BorderStroke(1.dp, MatteGlassTokens.GlassBorder), CircleShape)
                .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                    color = if (isSelected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalSecondary,
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
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun GlassSearchStrip(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = MatteGlassTokens.CharcoalMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MatteGlassTokens.CharcoalPrimary,
                unfocusedBorderColor = MatteGlassTokens.GlassBorder
            ),
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
