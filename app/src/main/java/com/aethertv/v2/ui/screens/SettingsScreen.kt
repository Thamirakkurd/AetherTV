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
import androidx.compose.ui.draw.shadow
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
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Servers & Playlists
        item {
            GlassSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servers (${servers.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MatteGlassTokens.CanvasWhite)
                                .border(BorderStroke(1.dp, Color.White), CircleShape)
                                .clickable(onClick = onRefreshPlaylist)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Refresh",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MatteGlassTokens.CharcoalPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MatteGlassTokens.CharcoalPrimary)
                                .clickable(onClick = onAddServerClick)
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Add",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (servers.isEmpty()) {
                    Text(
                        text = "No servers added yet. Tap Add to configure one.",
                        fontSize = 12.sp,
                        color = MatteGlassTokens.CharcoalMuted
                    )
                } else {
                    servers.forEach { srv ->
                        val isActive = srv.id == activeServerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CanvasWhite)
                                .clickable { onSelectServer(srv) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isActive) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = srv.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else MatteGlassTokens.CharcoalPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onDeleteServer(srv) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Server",
                                    tint = if (isActive) Color.White.copy(alpha = 0.85f) else MatteGlassTokens.NotificationDot,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Movie & Series Downloader
        item {
            GlassSettingsCard {
                Text(
                    text = "Movie & Series Downloader",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DownloadMode.entries.forEach { mode ->
                        val selected = mode == downloadMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CanvasWhite)
                                .clickable { onChangeDownloadMode(mode) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Offline Downloads Library
        if (downloadedFiles.isNotEmpty()) {
            item {
                GlassSettingsCard {
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
                                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                    modifier = Modifier.size(19.dp)
                                )
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MatteGlassTokens.NotificationDot,
                                    modifier = Modifier
                                        .size(19.dp)
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
            GlassSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Library Summary",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                        Text(
                            text = "Favorites: $favoritesCount  •  Recent: $recentCount",
                            fontSize = 12.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MatteGlassTokens.NotificationDot.copy(alpha = 0.12f))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.NotificationDot,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 5. About AetherTV with ThamerAK Logo + GitHub Link
        item {
            GlassSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThamerAkLogo(modifier = Modifier.size(50.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ThamerAK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                        Text(
                            text = "AetherTV v2 • Kotlin & Compose",
                            fontSize = 12.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MatteGlassTokens.CharcoalPrimary)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thamirakkurd"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "GitHub",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open GitHub",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassSettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color(0xFFF4F6FA).copy(alpha = 0.86f)
                    )
                )
            )
            .border(BorderStroke(1.2.dp, Color.White), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun ThamerAkLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF141820), Color(0xFF2B3445))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)

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
