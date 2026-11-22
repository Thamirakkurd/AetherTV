package com.aethertv.v2.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.DownloadMode
import com.aethertv.v2.data.SavedServer
import com.aethertv.v2.ui.theme.MatteGlassTokens
import java.io.File

@Composable
fun SettingsScreen(
    servers: List<SavedServer>,
    activeServerId: String,
    downloadMode: DownloadMode,
    downloadedFiles: List<File>,
    favoritesCount: Int,
    recentCount: Int,
    onSelectServer: (SavedServer) -> Unit,
    onDeleteServer: (SavedServer) -> Unit,
    onAddServerClick: () -> Unit,
    onRefreshPlaylist: () -> Unit,
    onChangeDownloadMode: (DownloadMode) -> Unit,
    onPlayDownloadedFile: (File) -> Unit,
    onDeleteDownloadedFile: (File) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Multiple Named Servers Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servers & Playlists (${servers.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.CanvasWhite)
                                .clickable(onClick = onRefreshPlaylist)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp))
                                Text("Refresh", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.CharcoalPrimary)
                                .clickable(onClick = onAddServerClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }

                if (servers.isEmpty()) {
                    Text(
                        text = "No servers added yet. Tap Add to configure one.",
                        fontSize = 13.sp,
                        color = MatteGlassTokens.CharcoalMuted
                    )
                } else {
                    servers.forEach { srv ->
                        val isActive = srv.id == activeServerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CanvasWhite)
                                .clickable { onSelectServer(srv) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isActive) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = srv.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else MatteGlassTokens.CharcoalPrimary
                                    )
                                }
                                Text(
                                    text = if (srv.mode == "xtream") srv.host else srv.m3uUrl,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isActive) Color.White.copy(alpha = 0.78f) else MatteGlassTokens.CharcoalMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { onDeleteServer(srv) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Server",
                                    tint = if (isActive) Color.White.copy(alpha = 0.85f) else MatteGlassTokens.NotificationDot,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Downloader Option (Built-in vs External 1DM+)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Movie & Series Downloader",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
                DownloadMode.entries.forEach { mode ->
                    val selected = mode == downloadMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CanvasWhite)
                            .clickable { onChangeDownloadMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else MatteGlassTokens.CharcoalPrimary
                        )
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 3. Offline Downloads Library
        if (downloadedFiles.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Downloaded Videos (${downloadedFiles.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary
                    )
                    downloadedFiles.forEach { file ->
                        val sizeMb = String.format("%.1f MB", file.length() / (1024f * 1024f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MatteGlassTokens.CanvasWhite)
                                .clickable { onPlayDownloadedFile(file) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MatteGlassTokens.CharcoalPrimary
                                )
                                Text(sizeMb, fontSize = 11.sp, color = MatteGlassTokens.CharcoalMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MatteGlassTokens.NotificationDot,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onDeleteDownloadedFile(file) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Library Summary & Reset
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Library Summary",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
                Text(
                    text = "Favorites: $favoritesCount  •  Recently Watched: $recentCount",
                    fontSize = 13.sp,
                    color = MatteGlassTokens.CharcoalMuted
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.NotificationDot.copy(alpha = 0.12f))
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Clear All Data",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MatteGlassTokens.NotificationDot
                    )
                }
            }
        }

        // 5. About Section with Custom ThamerAK Logo + GitHub Link
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "About AetherTV",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ThamerAkLogo(modifier = Modifier.size(56.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ThamerAK",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                        Text(
                            text = "AetherTV v2 • Built with Kotlin & Compose",
                            fontSize = 12.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CharcoalPrimary)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thamirakkurd"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "github.com/Thamirakkurd",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open GitHub",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThamerAkLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF141820), Color(0xFF2B3445))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(11.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)

            // Stylized "T" top crossbar & stem
            drawLine(
                color = Color.White,
                start = Offset(w * 0.14f, h * 0.22f),
                end = Offset(w * 0.86f, h * 0.22f),
                strokeWidth = w * 0.09f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(w * 0.36f, h * 0.22f),
                end = Offset(w * 0.36f, h * 0.80f),
                strokeWidth = w * 0.09f,
                cap = StrokeCap.Round
            )

            // Stylized "A" peak & "K" diagonal flare
            val akPath = Path().apply {
                moveTo(w * 0.48f, h * 0.80f)
                lineTo(w * 0.66f, h * 0.36f)
                lineTo(w * 0.84f, h * 0.80f)
            }
            drawPath(path = akPath, color = Color(0xFF60A5FA), style = stroke)

            drawLine(
                color = Color(0xFF60A5FA),
                start = Offset(w * 0.54f, h * 0.63f),
                end = Offset(w * 0.78f, h * 0.63f),
                strokeWidth = w * 0.07f,
                cap = StrokeCap.Round
            )
        }
    }
}
