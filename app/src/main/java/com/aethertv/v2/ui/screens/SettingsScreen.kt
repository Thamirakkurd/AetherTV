package com.aethertv.v2.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.BuildConfig
import com.aethertv.v2.data.DownloadMode
import com.aethertv.v2.data.DownloadedMediaFile
import com.aethertv.v2.data.SavedServer
import com.aethertv.v2.ui.components.SimpleScreenHeader
import com.aethertv.v2.ui.theme.AppThemeMode
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import java.util.Locale

@Composable
fun SettingsScreen(
    servers: List<SavedServer>,
    activeServerId: String,
    themeMode: AppThemeMode,
    downloadMode: DownloadMode,
    downloadedFiles: List<DownloadedMediaFile>,
    favoritesCount: Int,
    recentCount: Int,
    extraBottomPadding: Dp = 0.dp,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    onSelectServer: (SavedServer) -> Unit,
    onDeleteServer: (SavedServer) -> Unit,
    onAddServerClick: () -> Unit,
    onRefreshPlaylist: () -> Unit,
    onChangeDownloadMode: (DownloadMode) -> Unit,
    onPlayDownloadedFile: (DownloadedMediaFile) -> Unit,
    onDeleteDownloadedFile: (DownloadedMediaFile) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var serverToDelete by remember { mutableStateOf<SavedServer?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MatteGlassTokens.AmbientCanvasBrush),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = topInset + 72.dp,
                bottom = navInset + 84.dp + extraBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Appearance
            item(key = "theme_section") {
                SettingsGlassGroup(title = "APPEARANCE") {
                    AppThemeMode.entries.forEachIndexed { index, mode ->
                        val selected = mode == themeMode
                        val icon = if (mode == AppThemeMode.DARK) Icons.Rounded.DarkMode else Icons.Rounded.LightMode
                        val label = if (mode == AppThemeMode.DARK) "Dark Glass" else "Light Glass"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .livelyPress(pressedScale = 0.985f, depthShiftPx = 1.5f) {
                                    onChangeThemeMode(mode)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = label,
                                fontSize = 15.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                        if (index < AppThemeMode.entries.lastIndex) {
                            SettingsRowDivider()
                        }
                    }
                }
            }

            // 2. IPTV Sources
            item(key = "servers_section") {
                SettingsGlassGroup(title = "IPTV SOURCES & PLAYLISTS") {
                    if (servers.isEmpty()) {
                        SettingsEmptyRow(
                            icon = Icons.Rounded.Dns,
                            message = "No IPTV servers connected yet. Add an Xtream Codes or M3U source below."
                        )
                    } else {
                        servers.forEachIndexed { index, srv ->
                            val isActive = srv.id == activeServerId
                            val endpoint = if (srv.mode == "xtream") srv.host else srv.m3uUrl
                            val serverTitle = srv.name.ifBlank { srv.host.ifBlank { "Playlist" } }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .livelyPress(pressedScale = 0.985f, depthShiftPx = 1.5f) {
                                        onSelectServer(srv)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Dns,
                                    contentDescription = null,
                                    tint = MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(25.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = serverTitle,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MatteGlassTokens.CharcoalPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = srv.mode.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MatteGlassTokens.CharcoalMuted
                                        )
                                    }
                                    if (endpoint.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = endpoint,
                                            fontSize = 12.5.sp,
                                            color = MatteGlassTokens.CharcoalMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Active",
                                        tint = MatteGlassTokens.CharcoalPrimary,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .size(23.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { serverToDelete = srv },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete server",
                                        tint = MatteGlassTokens.CharcoalMuted,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }
                            if (index < servers.lastIndex) {
                                SettingsRowDivider()
                            }
                        }
                    }

                    SettingsRowDivider(fullWidth = true)

                    SettingsActionRow(
                        icon = Icons.Rounded.AddCircleOutline,
                        title = "Add IPTV Server",
                        subtitle = "Connect Xtream Codes API or M3U URL",
                        onClick = onAddServerClick
                    )
                    SettingsRowDivider()
                    SettingsActionRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Refresh Playlist Cache",
                        subtitle = "Re-sync live channels, movies, and series",
                        onClick = onRefreshPlaylist
                    )
                }
            }

            // 3. Downloader Preferences
            item(key = "downloader_section") {
                SettingsGlassGroup(title = "DOWNLOAD ENGINE") {
                    DownloadMode.entries.forEachIndexed { index, mode ->
                        val selected = mode == downloadMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .livelyPress(pressedScale = 0.985f, depthShiftPx = 1.5f) {
                                    onChangeDownloadMode(mode)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = mode.label,
                                fontSize = 15.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                        if (index < DownloadMode.entries.lastIndex) {
                            SettingsRowDivider()
                        }
                    }
                }
            }

            // 4. Offline Downloads (uses precomputed sizeBytes off the Main Thread)
            item(key = "downloads_section") {
                SettingsGlassGroup(title = "OFFLINE DOWNLOADS (${downloadedFiles.size})") {
                    if (downloadedFiles.isEmpty()) {
                        SettingsEmptyRow(
                            icon = Icons.Rounded.FolderOpen,
                            message = "No offline files saved in Downloads/AetherTV."
                        )
                    } else {
                        downloadedFiles.forEachIndexed { index, entry ->
                            val sizeMb = entry.sizeBytes / (1024.0 * 1024.0)
                            val sizeLabel = String.format(Locale.US, "%.1f MB", sizeMb)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MatteGlassTokens.CharcoalPrimary,
                                    modifier = Modifier.size(25.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MatteGlassTokens.CharcoalPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = sizeLabel,
                                        fontSize = 12.sp,
                                        color = MatteGlassTokens.CharcoalMuted
                                    )
                                }
                                IconButton(
                                    onClick = { onPlayDownloadedFile(entry) },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play file",
                                        tint = MatteGlassTokens.CharcoalPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteDownloadedFile(entry) },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete file",
                                        tint = MatteGlassTokens.CharcoalMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (index < downloadedFiles.lastIndex) {
                                SettingsRowDivider()
                            }
                        }
                    }
                }
            }

            // 5. Library & Storage
            item(key = "library_section") {
                SettingsGlassGroup(title = "LIBRARY & STORAGE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(25.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Pinned Favorites",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = favoritesCount.toString(),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                    }
                    SettingsRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(25.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Recently Watched",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = recentCount.toString(),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                    }
                    SettingsRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .livelyPress(pressedScale = 0.985f, depthShiftPx = 1.5f) {
                                showClearAllConfirm = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear All Data",
                            tint = MatteGlassTokens.NotificationDot,
                            modifier = Modifier.size(25.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear All Saved Data",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.NotificationDot
                            )
                            Text(
                                text = "Removes playlists, cache, favorites, and history",
                                fontSize = 12.5.sp,
                                color = MatteGlassTokens.CharcoalMuted
                            )
                        }
                    }
                }
            }

            // 6. About
            item(key = "about_section") {
                SettingsGlassGroup(title = "ABOUT") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .livelyPress(pressedScale = 0.985f, depthShiftPx = 1.5f) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thamirakkurd"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No browser found to open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThamerAkLogo(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ThamerAK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatteGlassTokens.CharcoalPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "AetherTV v${BuildConfig.VERSION_NAME} • github.com/Thamirakkurd",
                                fontSize = 12.5.sp,
                                color = MatteGlassTokens.CharcoalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = "Open GitHub",
                            tint = MatteGlassTokens.CharcoalMuted,
                            modifier = Modifier.size(23.dp)
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

    serverToDelete?.let { srv ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            containerColor = MatteGlassTokens.SheetBg,
            title = {
                Text(
                    text = "Remove Server?",
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
            },
            text = {
                Text(
                    text = "Remove \"${srv.name.ifBlank { srv.host.ifBlank { "Playlist" } }}\" and its cached playlist data?",
                    color = MatteGlassTokens.CharcoalMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        serverToDelete = null
                        onDeleteServer(srv)
                    }
                ) {
                    Text("Remove", color = MatteGlassTokens.NotificationDot, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text("Cancel", color = MatteGlassTokens.CharcoalPrimary)
                }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = MatteGlassTokens.SheetBg,
            title = {
                Text(
                    text = "Clear All Saved Data?",
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all saved servers, cached playlists, favorites, watch history, and resume progress.",
                    color = MatteGlassTokens.CharcoalMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirm = false
                        onClearAll()
                    }
                ) {
                    Text("Clear Everything", color = MatteGlassTokens.NotificationDot, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = MatteGlassTokens.CharcoalPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsGlassGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.0.sp,
            color = MatteGlassTokens.CharcoalMuted,
            modifier = Modifier.padding(start = 10.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MatteGlassTokens.CardGlassBg)
                .border(0.6.dp, MatteGlassTokens.HairlineDivider, RoundedCornerShape(16.dp)),
            content = content
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .livelyPress(pressedScale = 0.985f, depthShiftPx = 1.5f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MatteGlassTokens.CharcoalPrimary,
            modifier = Modifier.size(25.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Normal,
                color = MatteGlassTokens.CharcoalPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = MatteGlassTokens.CharcoalMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MatteGlassTokens.CharcoalMuted,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SettingsEmptyRow(
    icon: ImageVector,
    message: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MatteGlassTokens.CharcoalMuted,
            modifier = Modifier.size(25.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = message,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = MatteGlassTokens.CharcoalMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsRowDivider(fullWidth: Boolean = false) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (fullWidth) 0.dp else 57.dp, end = if (fullWidth) 0.dp else 16.dp),
        thickness = 0.5.dp,
        color = MatteGlassTokens.HairlineDivider
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
