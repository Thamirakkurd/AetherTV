package com.aethertv.v2.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.DownloadMode
import com.aethertv.v2.data.SavedServer
import com.aethertv.v2.ui.components.SimpleScreenHeader
import com.aethertv.v2.ui.theme.AppThemeMode
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.hazeSource
import java.io.File

@Composable
fun SettingsScreen(
    servers: List<SavedServer>,
    activeServerId: String,
    themeMode: AppThemeMode,
    downloadMode: DownloadMode,
    downloadedFiles: List<File>,
    favoritesCount: Int,
    recentCount: Int,
    extraBottomPadding: Dp = 0.dp,
    onChangeThemeMode: (AppThemeMode) -> Unit,
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
    val hazeState = LocalHazeState.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .background(MatteGlassTokens.AmbientCanvasBrush),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = topInset + 68.dp,
                bottom = navInset + 86.dp + extraBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "theme_section") {
                SettingsSectionCard(title = "Appearance") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = mode == themeMode
                            val icon = if (mode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .livelyPress(pressedScale = 0.96f, depthShiftPx = 2f) {
                                        onChangeThemeMode(mode)
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.SubtleSurface)
                                    .border(
                                        0.85.dp,
                                        if (selected) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (mode == AppThemeMode.DARK) "Dark Glass" else "Light Glass",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item(key = "servers_section") {
                SettingsSectionCard(title = "IPTV Servers") {
                    if (servers.isEmpty()) {
                        Text(
                            text = "No servers saved yet. Tap Add Server to connect Xtream Codes or M3U.",
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                    } else {
                        servers.forEach { srv ->
                            val isActive = srv.id == activeServerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .livelyPress(pressedScale = 0.975f) { onSelectServer(srv) }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) MatteGlassTokens.FieldGlassBg else MatteGlassTokens.SubtleSurface)
                                    .border(
                                        0.9.dp,
                                        if (isActive) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = srv.name.ifBlank { srv.host.ifBlank { "Playlist" } },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MatteGlassTokens.CharcoalPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = srv.mode.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MatteGlassTokens.CharcoalMuted
                                        )
                                    }
                                    val endpoint = if (srv.mode == "xtream") srv.host else srv.m3uUrl
                                    if (endpoint.isNotBlank()) {
                                        Text(
                                            text = endpoint,
                                            fontSize = 12.sp,
                                            color = MatteGlassTokens.CharcoalMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MatteGlassTokens.CharcoalPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp).size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteServer(srv) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete server",
                                        tint = MatteGlassTokens.CharcoalMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .weight(1f).height(42.dp)
                                .livelyPress(pressedScale = 0.95f, onClick = onAddServerClick)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.CharcoalPrimary),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = MatteGlassTokens.CanvasWhite, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Server", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CanvasWhite)
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f).height(42.dp)
                                .livelyPress(pressedScale = 0.95f, onClick = onRefreshPlaylist)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.SubtleSurface)
                                .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh Cache", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                        }
                    }
                }
            }

            item(key = "downloader_section") {
                SettingsSectionCard(title = "Downloader") {
                    DownloadMode.entries.forEach { mode ->
                        val selected = mode == downloadMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .livelyPress(pressedScale = 0.975f) { onChangeDownloadMode(mode) }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.SubtleSurface)
                                .border(
                                    0.85.dp,
                                    if (selected) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                            )
                            if (selected) {
                                Icon(Icons.Default.Check, null, tint = MatteGlassTokens.CanvasWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item(key = "downloads_section") {
                SettingsSectionCard(title = "Downloaded Files (${downloadedFiles.size})") {
                    if (downloadedFiles.isEmpty()) {
                        Text(
                            text = "No offline files found in Downloads/AetherTV.",
                            fontSize = 13.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                    } else {
                        downloadedFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MatteGlassTokens.SubtleSurface)
                                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = file.nameWithoutExtension,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MatteGlassTokens.CharcoalPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onPlayDownloadedFile(file) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play file",
                                        tint = MatteGlassTokens.CharcoalPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteDownloadedFile(file) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete file",
                                        tint = MatteGlassTokens.CharcoalMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "library_section") {
                SettingsSectionCard(title = "Library") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(label = "Favorites", value = favoritesCount.toString(), modifier = Modifier.weight(1f))
                        StatChip(label = "Recent", value = recentCount.toString(), modifier = Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .livelyPress(pressedScale = 0.97f, onClick = onClearAll)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MatteGlassTokens.SubtleSurface)
                            .border(1.dp, MatteGlassTokens.NotificationDot.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear All Saved Data",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.NotificationDot
                        )
                    }
                }
            }

            item(key = "about_section") {
                SettingsSectionCard(title = "About") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .livelyPress(pressedScale = 0.975f) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thamirakkurd"))
                                context.startActivity(intent)
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThamerAkLogo(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ThamerAK",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatteGlassTokens.CharcoalPrimary
                            )
                            Text(
                                text = "AetherTV v3.3.3 • github.com/Thamirakkurd",
                                fontSize = 12.sp,
                                color = MatteGlassTokens.CharcoalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open GitHub",
                            tint = MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        SimpleScreenHeader(
            title = "Settings",
            onAddClick = onAddServerClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MatteGlassTokens.CardGlassBg)
            .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MatteGlassTokens.CharcoalPrimary
        )
        content()
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MatteGlassTokens.SubtleSurface)
            .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MatteGlassTokens.CharcoalMuted)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
    }
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
