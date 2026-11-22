package com.aethertv.v2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.ui.theme.MatteGlassTokens

@Composable
fun SettingsScreen(
    playlistLabel: String,
    favoritesCount: Int,
    recentCount: Int,
    onRefreshPlaylist: () -> Unit,
    onAddOrChangePlaylist: () -> Unit,
    onClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCard(title = "Active Playlist") {
                Text(
                    text = playlistLabel,
                    fontSize = 13.sp,
                    color = MatteGlassTokens.CharcoalMuted
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRefreshPlaylist,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatteGlassTokens.CharcoalPrimary,
                            contentColor = MatteGlassTokens.CanvasWhite
                        )
                    ) {
                        Text("Refresh")
                    }
                    OutlinedButton(
                        onClick = onAddOrChangePlaylist,
                        border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder)
                    ) {
                        Text("Change", color = MatteGlassTokens.CharcoalPrimary)
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Library") {
                Text(
                    text = "Favorites: $favoritesCount • Recently Watched: $recentCount",
                    fontSize = 13.sp,
                    color = MatteGlassTokens.CharcoalMuted
                )
            }
        }

        item {
            SettingsCard(title = "Storage") {
                Text(
                    text = "Clear saved playlist credentials, favorites, and watch history.",
                    fontSize = 13.sp,
                    color = MatteGlassTokens.CharcoalMuted
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClearAll,
                    border = BorderStroke(1.dp, MatteGlassTokens.NotificationDot)
                ) {
                    Text("Clear All Data", color = MatteGlassTokens.NotificationDot)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MatteGlassTokens.HeaderPillBg,
        border = BorderStroke(1.dp, MatteGlassTokens.NeutralBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MatteGlassTokens.CharcoalPrimary
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
