package com.aethertv.v2

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aethertv.v2.data.CategoryItem
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.DownloadMode
import com.aethertv.v2.data.EpisodeItem
import com.aethertv.v2.data.MediaDetailInfo
import com.aethertv.v2.data.MediaDownloadHelper
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.data.PlaylistDiskCache
import com.aethertv.v2.data.SavedServer
import com.aethertv.v2.data.ServerStore
import com.aethertv.v2.data.XtreamClient
import com.aethertv.v2.ui.components.AddPlaylistDialog
import com.aethertv.v2.ui.components.FloatingGlassNavBar
import com.aethertv.v2.ui.components.GlassSearchStrip
import com.aethertv.v2.ui.components.GlassTopHeader
import com.aethertv.v2.ui.components.NavTabItem
import com.aethertv.v2.ui.screens.HomeScreen
import com.aethertv.v2.ui.screens.MediaDetailScreen
import com.aethertv.v2.ui.screens.MediaGridScreen
import com.aethertv.v2.ui.screens.PlayerScreen
import com.aethertv.v2.ui.screens.SettingsScreen
import com.aethertv.v2.ui.theme.MatteGlassTheme
import com.aethertv.v2.ui.theme.MatteGlassTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            MatteGlassTheme {
                AetherV2App(
                    isInPipMode = isInPipModeState.value,
                    onEnterPip = {
                        try {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            enterPictureInPictureMode(params)
                        } catch (_: Exception) {
                            Toast.makeText(this, "PiP unavailable on this device", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }
}

private fun launchExternalPlayer(context: Context, url: String, title: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Play $title"))
    } catch (_: Exception) {
        Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AetherV2App(
    isInPipMode: Boolean,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("aethertv_v2_prefs", Context.MODE_PRIVATE) }

    val tabs = remember {
        listOf(
            NavTabItem("Home", Icons.Default.Home),
            NavTabItem("Live", Icons.Default.LiveTv),
            NavTabItem("Movies", Icons.Default.Movie),
            NavTabItem("Series", Icons.Default.Tv),
            NavTabItem("Settings", Icons.Default.Settings)
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Migrate legacy single server into SavedServer list if needed
    val initialServers = remember {
        val saved = ServerStore.decode(prefs.getString("servers_json", null))
        if (saved.isNotEmpty()) {
            saved
        } else {
            val legacyMode = prefs.getString("mode", "") ?: ""
            if (legacyMode == "xtream") {
                val h = prefs.getString("host", "") ?: ""
                val u = prefs.getString("user", "") ?: ""
                val p = prefs.getString("pass", "") ?: ""
                if (h.isNotBlank()) listOf(SavedServer("default_1", "Default Server", "xtream", h, u, p)) else emptyList()
            } else if (legacyMode == "m3u") {
                val m = prefs.getString("m3u_url", "") ?: ""
                if (m.isNotBlank()) listOf(SavedServer("default_1", "Default M3U", "m3u", m3uUrl = m)) else emptyList()
            } else {
                emptyList()
            }
        }
    }

    var servers by remember { mutableStateOf(initialServers) }
    var activeServerId by remember {
        mutableStateOf(
            prefs.getString("active_server_id", null)
                ?: initialServers.firstOrNull()?.id
                ?: ""
        )
    }

    val activeServer = remember(servers, activeServerId) {
        servers.firstOrNull { it.id == activeServerId } ?: servers.firstOrNull()
    }

    val playlistMode = activeServer?.mode ?: ""
    val xtreamHost = activeServer?.host ?: ""
    val xtreamUser = activeServer?.user ?: ""
    val xtreamPass = activeServer?.pass ?: ""
    val m3uUrl = activeServer?.m3uUrl ?: ""
    val playlistScopeKey = activeServer?.id ?: "none"

    // Download mode & offline files
    var downloadMode by remember {
        val savedName = prefs.getString("download_mode", DownloadMode.ASK.name) ?: DownloadMode.ASK.name
        mutableStateOf(
            try { DownloadMode.valueOf(savedName) } catch (_: Exception) { DownloadMode.ASK }
        )
    }
    var pendingDownloadTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // url to title
    var downloadedFiles by remember { mutableStateOf(MediaDownloadHelper.listDownloadedFiles()) }

    // Confirmation prompt for "All" category
    var confirmAllTab by remember { mutableStateOf<Int?>(null) } // 1=Live, 2=Movies, 3=Series

    // Catalog state + in-memory cache for "All"
    var liveCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedLiveCat by remember { mutableStateOf(prefs.getString("last_live_cat", "") ?: "") }
    var liveItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allLiveCache by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var m3uAllItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var vodCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedVodCat by remember { mutableStateOf(prefs.getString("last_vod_cat", "") ?: "") }
    var vodItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allVodCache by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var seriesCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedSeriesCat by remember { mutableStateOf(prefs.getString("last_series_cat", "") ?: "") }
    var seriesItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allSeriesCache by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    // Persisted Favorites & Recents
    var favorites by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("favorites_json", null))) }
    var recents by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("recents_json", null))) }
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    // Media Detail & Built-in Player state
    var selectedDetailItem by remember { mutableStateOf<MediaItem?>(null) }
    var detailInfo by remember { mutableStateOf(MediaDetailInfo()) }
    var detailEpisodes by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var loadingDetail by remember { mutableStateOf(false) }
    var activePlayerItem by remember { mutableStateOf<MediaItem?>(null) }

    BackHandler(enabled = activePlayerItem != null || selectedDetailItem != null) {
        if (activePlayerItem != null) {
            activePlayerItem = null
        } else {
            selectedDetailItem = null
        }
    }

    fun persistServers(updated: List<SavedServer>, newActiveId: String) {
        servers = updated
        activeServerId = newActiveId
        prefs.edit()
            .putString("servers_json", ServerStore.encode(updated))
            .putString("active_server_id", newActiveId)
            .apply()
    }

    fun triggerDownload(url: String, title: String) {
        when (downloadMode) {
            DownloadMode.BUILTIN -> {
                MediaDownloadHelper.startBuiltInDownload(context, url, title)
                downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
            }
            DownloadMode.EXTERNAL -> {
                MediaDownloadHelper.startExternalDownload(context, url, title)
            }
            DownloadMode.ASK -> {
                pendingDownloadTarget = url to title
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        val updated = if (favorites.any { it.id == item.id }) {
            favorites.filterNot { it.id == item.id }
        } else {
            listOf(item) + favorites
        }
        favorites = updated
        prefs.edit().putString("favorites_json", XtreamClient.encodeItems(updated)).apply()
    }

    fun recordRecent(item: MediaItem) {
        val updated = (listOf(item) + recents.filterNot { it.id == item.id }).take(20)
        recents = updated
        prefs.edit().putString("recents_json", XtreamClient.encodeItems(updated)).apply()
    }

    fun playItem(item: MediaItem) {
        recordRecent(item)
        activePlayerItem = item
    }

    fun openDetail(item: MediaItem) {
        selectedDetailItem = item
        detailInfo = MediaDetailInfo()
        detailEpisodes = emptyList()
        if (playlistMode != "xtream" || xtreamHost.isBlank()) return

        when (item.contentType) {
            ContentType.MOVIE -> {
                val vodId = item.id.removePrefix("vod_")
                if (vodId.isBlank()) return
                loadingDetail = true
                scope.launch {
                    try {
                        val json = XtreamClient.fetchText(
                            "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_info&vod_id=$vodId"
                        )
                        detailInfo = withContext(Dispatchers.Default) { XtreamClient.parseVodDetail(json) }
                    } catch (_: Exception) {
                    } finally {
                        loadingDetail = false
                    }
                }
            }
            ContentType.SERIES -> {
                val sid = item.seriesId ?: return
                loadingDetail = true
                scope.launch {
                    try {
                        val json = XtreamClient.fetchText(
                            "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series_info&series_id=$sid"
                        )
                        val parsedInfo = withContext(Dispatchers.Default) { XtreamClient.parseSeriesDetail(json) }
                        val parsedEps = withContext(Dispatchers.Default) {
                            XtreamClient.parseSeriesEpisodes(json, xtreamHost, xtreamUser, xtreamPass)
                        }
                        detailInfo = parsedInfo
                        detailEpisodes = parsedEps
                    } catch (_: Exception) {
                    } finally {
                        loadingDetail = false
                    }
                }
            }
            ContentType.LIVE -> {
                loadingDetail = false
            }
        }
    }

    fun loadLiveCategory(catName: String, forceNetwork: Boolean = false) {
        selectedLiveCat = catName
        prefs.edit().putString("last_live_cat", catName).apply()
        if (catName == "Favorites") return

        if (playlistMode == "m3u") {
            liveItems = if (catName == "All") m3uAllItems else m3uAllItems.filter { it.group == catName }
            return
        }

        val itemCacheKey = "${playlistScopeKey}_live_$catName"

        if (catName == "All") {
            if (!forceNetwork && allLiveCache.isNotEmpty()) {
                liveItems = allLiveCache
                return
            }
            scope.launch {
                if (!forceNetwork) {
                    val diskItems = PlaylistDiskCache.loadItems(context, itemCacheKey)
                    if (diskItems.isNotEmpty()) {
                        allLiveCache = diskItems
                        liveItems = diskItems
                        statusMessage = null
                        return@launch
                    }
                }
                isLoading = true
                statusMessage = "Loading all live channels..."
                try {
                    val catMap = liveCategories.associate { it.id to it.name }
                    val parsed = XtreamClient.fetchLiveStreams(
                        "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_live_streams",
                        xtreamHost, xtreamUser, xtreamPass, "Live", catMap
                    )
                    allLiveCache = parsed
                    liveItems = parsed
                    PlaylistDiskCache.saveItems(context, itemCacheKey, parsed)
                    statusMessage = null
                } catch (e: Exception) {
                    statusMessage = "Live error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
            return
        }

        val cat = liveCategories.firstOrNull { it.name == catName } ?: return
        scope.launch {
            if (!forceNetwork) {
                val diskItems = PlaylistDiskCache.loadItems(context, itemCacheKey)
                if (diskItems.isNotEmpty()) {
                    liveItems = diskItems
                    statusMessage = null
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading $catName..."
            try {
                val parsed = XtreamClient.fetchLiveStreams(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_live_streams&category_id=${cat.id}",
                    xtreamHost, xtreamUser, xtreamPass, cat.name
                )
                liveItems = parsed
                PlaylistDiskCache.saveItems(context, itemCacheKey, parsed)
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "Live error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadVodCategory(catName: String, forceNetwork: Boolean = false) {
        selectedVodCat = catName
        prefs.edit().putString("last_vod_cat", catName).apply()
        if (catName == "Favorites") return

        val itemCacheKey = "${playlistScopeKey}_vod_$catName"

        if (catName == "All") {
            if (!forceNetwork && allVodCache.isNotEmpty()) {
                vodItems = allVodCache
                return
            }
            scope.launch {
                if (!forceNetwork) {
                    val diskItems = PlaylistDiskCache.loadItems(context, itemCacheKey)
                    if (diskItems.isNotEmpty()) {
                        allVodCache = diskItems
                        vodItems = diskItems
                        statusMessage = null
                        return@launch
                    }
                }
                isLoading = true
                statusMessage = "Loading all movies..."
                try {
                    val catMap = vodCategories.associate { it.id to it.name }
                    val parsed = XtreamClient.fetchVodStreams(
                        "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_streams",
                        xtreamHost, xtreamUser, xtreamPass, "Movies", catMap
                    )
                    allVodCache = parsed
                    vodItems = parsed
                    PlaylistDiskCache.saveItems(context, itemCacheKey, parsed)
                    statusMessage = null
                } catch (e: Exception) {
                    statusMessage = "Movies error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
            return
        }

        val cat = vodCategories.firstOrNull { it.name == catName } ?: return
        scope.launch {
            if (!forceNetwork) {
                val diskItems = PlaylistDiskCache.loadItems(context, itemCacheKey)
                if (diskItems.isNotEmpty()) {
                    vodItems = diskItems
                    statusMessage = null
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading $catName..."
            try {
                val parsed = XtreamClient.fetchVodStreams(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_streams&category_id=${cat.id}",
                    xtreamHost, xtreamUser, xtreamPass, cat.name
                )
                vodItems = parsed
                PlaylistDiskCache.saveItems(context, itemCacheKey, parsed)
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "Movies error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadSeriesCategory(catName: String, forceNetwork: Boolean = false) {
        selectedSeriesCat = catName
        prefs.edit().putString("last_series_cat", catName).apply()
        if (catName == "Favorites") return

        val itemCacheKey = "${playlistScopeKey}_series_$catName"

        if (catName == "All") {
            if (!forceNetwork && allSeriesCache.isNotEmpty()) {
                seriesItems = allSeriesCache
                return
            }
            scope.launch {
                if (!forceNetwork) {
                    val diskItems = PlaylistDiskCache.loadItems(context, itemCacheKey)
                    if (diskItems.isNotEmpty()) {
                        allSeriesCache = diskItems
                        seriesItems = diskItems
                        statusMessage = null
                        return@launch
                    }
                }
                isLoading = true
                statusMessage = "Loading all series..."
                try {
                    val catMap = seriesCategories.associate { it.id to it.name }
                    val parsed = XtreamClient.fetchSeries(
                        "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series",
                        "Series", catMap
                    )
                    allSeriesCache = parsed
                    seriesItems = parsed
                    PlaylistDiskCache.saveItems(context, itemCacheKey, parsed)
                    statusMessage = null
                } catch (e: Exception) {
                    statusMessage = "Series error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
            return
        }

        val cat = seriesCategories.firstOrNull { it.name == catName } ?: return
        scope.launch {
            if (!forceNetwork) {
                val diskItems = PlaylistDiskCache.loadItems(context, itemCacheKey)
                if (diskItems.isNotEmpty()) {
                    seriesItems = diskItems
                    statusMessage = null
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading $catName..."
            try {
                val parsed = XtreamClient.fetchSeries(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series&category_id=${cat.id}",
                    cat.name
                )
                seriesItems = parsed
                PlaylistDiskCache.saveItems(context, itemCacheKey, parsed)
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "Series error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun ensureLiveCategories(forceNetwork: Boolean = false) {
        if (playlistMode != "xtream" || (!forceNetwork && liveCategories.isNotEmpty())) return
        scope.launch {
            if (!forceNetwork) {
                val cachedCats = PlaylistDiskCache.loadCategories(context, "${playlistScopeKey}_live")
                if (cachedCats.isNotEmpty()) {
                    liveCategories = cachedCats
                    val targetCat = selectedLiveCat.takeIf { it.isNotBlank() && (it == "All" || it == "Favorites" || cachedCats.any { c -> c.name == it }) }
                        ?: cachedCats.first().name
                    loadLiveCategory(targetCat, forceNetwork = false)
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading live categories..."
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_live_categories"
                )
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                liveCategories = cats
                PlaylistDiskCache.saveCategories(context, "${playlistScopeKey}_live", cats)
                cats.firstOrNull()?.let { loadLiveCategory(it.name, forceNetwork = forceNetwork) }
            } catch (e: Exception) {
                statusMessage = "Live error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun ensureVodCategories(forceNetwork: Boolean = false) {
        if (playlistMode != "xtream" || (!forceNetwork && vodCategories.isNotEmpty())) return
        scope.launch {
            if (!forceNetwork) {
                val cachedCats = PlaylistDiskCache.loadCategories(context, "${playlistScopeKey}_vod")
                if (cachedCats.isNotEmpty()) {
                    vodCategories = cachedCats
                    val targetCat = selectedVodCat.takeIf { it.isNotBlank() && (it == "All" || it == "Favorites" || cachedCats.any { c -> c.name == it }) }
                        ?: cachedCats.first().name
                    loadVodCategory(targetCat, forceNetwork = false)
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading movie categories..."
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_categories"
                )
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                vodCategories = cats
                PlaylistDiskCache.saveCategories(context, "${playlistScopeKey}_vod", cats)
                cats.firstOrNull()?.let { loadVodCategory(it.name, forceNetwork = forceNetwork) }
            } catch (e: Exception) {
                statusMessage = "Movies error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun ensureSeriesCategories(forceNetwork: Boolean = false) {
        if (playlistMode != "xtream" || (!forceNetwork && seriesCategories.isNotEmpty())) return
        scope.launch {
            if (!forceNetwork) {
                val cachedCats = PlaylistDiskCache.loadCategories(context, "${playlistScopeKey}_series")
                if (cachedCats.isNotEmpty()) {
                    seriesCategories = cachedCats
                    val targetCat = selectedSeriesCat.takeIf { it.isNotBlank() && (it == "All" || it == "Favorites" || cachedCats.any { c -> c.name == it }) }
                        ?: cachedCats.first().name
                    loadSeriesCategory(targetCat, forceNetwork = false)
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading series categories..."
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series_categories"
                )
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                seriesCategories = cats
                PlaylistDiskCache.saveCategories(context, "${playlistScopeKey}_series", cats)
                cats.firstOrNull()?.let { loadSeriesCategory(it.name, forceNetwork = forceNetwork) }
            } catch (e: Exception) {
                statusMessage = "Series error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun resetActiveState() {
        liveCategories = emptyList()
        vodCategories = emptyList()
        seriesCategories = emptyList()
        liveItems = emptyList()
        vodItems = emptyList()
        seriesItems = emptyList()
        allLiveCache = emptyList()
        allVodCache = emptyList()
        allSeriesCache = emptyList()
        m3uAllItems = emptyList()
    }

    fun loadM3uForServer(srv: SavedServer, forceRefresh: Boolean) {
        val m3uCacheKey = "${srv.id}_m3u_all"
        scope.launch {
            if (!forceRefresh) {
                val cached = PlaylistDiskCache.loadItems(context, m3uCacheKey)
                if (cached.isNotEmpty()) {
                    m3uAllItems = cached
                    val groups = cached.map { it.group }.distinct().sorted()
                    liveCategories = groups.map { CategoryItem(it, it) }
                    val targetCat = selectedLiveCat.takeIf { it.isNotBlank() && (it == "All" || it == "Favorites" || groups.contains(it)) } ?: "All"
                    selectedLiveCat = targetCat
                    liveItems = if (targetCat == "All") cached else cached.filter { it.group == targetCat }
                    statusMessage = null
                    return@launch
                }
            }
            isLoading = true
            statusMessage = "Loading ${srv.name}..."
            try {
                val body = XtreamClient.fetchText(srv.m3uUrl)
                val parsed = withContext(Dispatchers.Default) { XtreamClient.parseM3u(body) }
                m3uAllItems = parsed
                PlaylistDiskCache.saveItems(context, m3uCacheKey, parsed)
                val groups = parsed.map { it.group }.distinct().sorted()
                liveCategories = groups.map { CategoryItem(it, it) }
                selectedLiveCat = "All"
                liveItems = parsed
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "M3U error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeServerId) {
        resetActiveState()
        val srv = activeServer ?: return@LaunchedEffect
        if (srv.mode == "xtream" && srv.host.isNotBlank()) {
            ensureLiveCategories(forceNetwork = false)
        } else if (srv.mode == "m3u" && srv.m3uUrl.isNotBlank()) {
            loadM3uForServer(srv, forceRefresh = false)
        }
    }

    val playingNow = activePlayerItem
    if (playingNow != null) {
        PlayerScreen(
            item = playingNow,
            isInPipMode = isInPipMode,
            onBack = { activePlayerItem = null },
            onEnterPip = onEnterPip,
            onOpenExternal = {
                launchExternalPlayer(context, playingNow.streamUrl, playingNow.name)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
    ) {
        val activeDetail = selectedDetailItem
        if (activeDetail != null) {
            MediaDetailScreen(
                item = activeDetail,
                detailInfo = detailInfo,
                episodes = detailEpisodes,
                isLoading = loadingDetail,
                isFavorite = favoriteIds.contains(activeDetail.id),
                onBack = { selectedDetailItem = null },
                onToggleFavorite = { toggleFavorite(activeDetail) },
                onWatchNow = { playItem(activeDetail) },
                onDownloadItem = { triggerDownload(activeDetail.streamUrl, activeDetail.name) },
                onEpisodeClick = { ep ->
                    val epMedia = MediaItem(
                        id = "ep_${ep.id}",
                        name = "${activeDetail.name} • ${ep.title}",
                        group = ep.season,
                        posterUrl = activeDetail.posterUrl,
                        streamUrl = ep.streamUrl,
                        contentType = ContentType.MOVIE,
                        badge = ep.season
                    )
                    playItem(epMedia)
                },
                onDownloadEpisode = { ep ->
                    triggerDownload(ep.streamUrl, "${activeDetail.name} - ${ep.title}")
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                val activeTabCategories = remember(selectedTab, liveCategories, vodCategories, seriesCategories) {
                    when (selectedTab) {
                        1 -> listOf("All", "Favorites") + liveCategories.map { it.name }
                        2 -> listOf("All", "Favorites") + vodCategories.map { it.name }
                        3 -> listOf("All", "Favorites") + seriesCategories.map { it.name }
                        else -> emptyList()
                    }
                }
                val activeTabSelectedCat = when (selectedTab) {
                    1 -> selectedLiveCat
                    2 -> selectedVodCat
                    3 -> selectedSeriesCat
                    else -> ""
                }

                GlassTopHeader(
                    title = tabs[selectedTab].label,
                    categories = activeTabCategories,
                    selectedCategory = activeTabSelectedCat,
                    onSelectCategory = { cat ->
                        if (cat == "All") {
                            confirmAllTab = selectedTab
                        } else {
                            if (selectedTab == 1) loadLiveCategory(cat, forceNetwork = false)
                            if (selectedTab == 2) loadVodCategory(cat, forceNetwork = false)
                            if (selectedTab == 3) loadSeriesCategory(cat, forceNetwork = false)
                        }
                    },
                    onSearchClick = { showSearch = !showSearch },
                    onAddClick = { showAddDialog = true }
                )

                if (showSearch && selectedTab in 1..3) {
                    GlassSearchStrip(
                        query = searchQuery,
                        placeholder = "Search ${tabs[selectedTab].label.lowercase()}...",
                        onQueryChange = { searchQuery = it },
                        onClose = {
                            searchQuery = ""
                            showSearch = false
                        }
                    )
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (msg.contains("error", ignoreCase = true)) MatteGlassTokens.NotificationDot else MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }

                when (selectedTab) {
                    0 -> {
                        val movieAndSeriesFavs = remember(favorites) {
                            favorites.filter { it.contentType == ContentType.MOVIE || it.contentType == ContentType.SERIES }
                        }
                        HomeScreen(
                            favoriteMedia = movieAndSeriesFavs,
                            recentlyWatched = recents,
                            favoriteIds = favoriteIds,
                            hasPlaylist = playlistMode.isNotBlank(),
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { if (it.contentType == ContentType.SERIES) openDetail(it) else playItem(it) },
                            onAddPlaylistClick = { showAddDialog = true }
                        )
                    }
                    1 -> {
                        val catNames = remember(liveCategories) {
                            listOf("All", "Favorites") + liveCategories.map { it.name }
                        }
                        val shown = remember(liveItems, selectedLiveCat, searchQuery, favorites) {
                            val source = if (selectedLiveCat == "Favorites") {
                                favorites.filter { it.contentType == ContentType.LIVE }
                            } else {
                                liveItems
                            }
                            if (searchQuery.isBlank()) source
                            else source.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        MediaGridScreen(
                            items = shown,
                            categories = catNames,
                            selectedCategory = selectedLiveCat,
                            favoriteIds = favoriteIds,
                            isLoading = isLoading,
                            emptyMessage = if (playlistMode.isBlank()) "Tap + to add your server" else "No live channels in this category",
                            onSelectCategory = { cat ->
                                if (cat == "All") confirmAllTab = 1
                                else loadLiveCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { playItem(it) }
                        )
                    }
                    2 -> {
                        val catNames = remember(vodCategories) {
                            listOf("All", "Favorites") + vodCategories.map { it.name }
                        }
                        val shown = remember(vodItems, selectedVodCat, searchQuery, favorites) {
                            val source = if (selectedVodCat == "Favorites") {
                                favorites.filter { it.contentType == ContentType.MOVIE }
                            } else {
                                vodItems
                            }
                            if (searchQuery.isBlank()) source
                            else source.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        MediaGridScreen(
                            items = shown,
                            categories = catNames,
                            selectedCategory = selectedVodCat,
                            favoriteIds = favoriteIds,
                            isLoading = isLoading,
                            emptyMessage = if (playlistMode != "xtream") "Add an Xtream Codes server for Movies" else "No movies in this category",
                            onSelectCategory = { cat ->
                                if (cat == "All") confirmAllTab = 2
                                else loadVodCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { playItem(it) }
                        )
                    }
                    3 -> {
                        val catNames = remember(seriesCategories) {
                            listOf("All", "Favorites") + seriesCategories.map { it.name }
                        }
                        val shown = remember(seriesItems, selectedSeriesCat, searchQuery, favorites) {
                            val source = if (selectedSeriesCat == "Favorites") {
                                favorites.filter { it.contentType == ContentType.SERIES }
                            } else {
                                seriesItems
                            }
                            if (searchQuery.isBlank()) source
                            else source.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        MediaGridScreen(
                            items = shown,
                            categories = catNames,
                            selectedCategory = selectedSeriesCat,
                            favoriteIds = favoriteIds,
                            isLoading = isLoading,
                            emptyMessage = if (playlistMode != "xtream") "Add an Xtream Codes server for Series" else "No series in this category",
                            onSelectCategory = { cat ->
                                if (cat == "All") confirmAllTab = 3
                                else loadSeriesCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { openDetail(it) }
                        )
                    }
                    4 -> {
                        SettingsScreen(
                            servers = servers,
                            activeServerId = activeServerId,
                            downloadMode = downloadMode,
                            downloadedFiles = downloadedFiles,
                            favoritesCount = favorites.size,
                            recentCount = recents.size,
                            onSelectServer = { srv ->
                                persistServers(servers, srv.id)
                            },
                            onDeleteServer = { srv ->
                                val remaining = servers.filterNot { it.id == srv.id }
                                val nextId = if (srv.id == activeServerId) remaining.firstOrNull()?.id ?: "" else activeServerId
                                persistServers(remaining, nextId)
                            },
                            onAddServerClick = { showAddDialog = true },
                            onRefreshPlaylist = {
                                val srv = activeServer ?: return@SettingsScreen
                                scope.launch {
                                    PlaylistDiskCache.clearAll(context)
                                    resetActiveState()
                                    if (srv.mode == "xtream") ensureLiveCategories(forceNetwork = true)
                                    else loadM3uForServer(srv, forceRefresh = true)
                                }
                            },
                            onChangeDownloadMode = { mode ->
                                downloadMode = mode
                                prefs.edit().putString("download_mode", mode.name).apply()
                            },
                            onPlayDownloadedFile = { file ->
                                val offlineItem = MediaItem(
                                    id = "offline_${file.name}",
                                    name = file.nameWithoutExtension,
                                    group = "Downloaded",
                                    posterUrl = null,
                                    streamUrl = Uri.fromFile(file).toString(),
                                    contentType = ContentType.MOVIE
                                )
                                playItem(offlineItem)
                            },
                            onDeleteDownloadedFile = { file ->
                                file.delete()
                                downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
                            },
                            onClearAll = {
                                prefs.edit().clear().apply()
                                servers = emptyList()
                                activeServerId = ""
                                resetActiveState()
                                favorites = emptyList()
                                recents = emptyList()
                                scope.launch { PlaylistDiskCache.clearAll(context) }
                                statusMessage = "Cleared all saved data"
                            }
                        )
                    }
                }
            }
        }

        FloatingGlassNavBar(
            items = tabs,
            selectedIndex = selectedTab,
            onSelect = { idx ->
                selectedDetailItem = null
                selectedTab = idx
                searchQuery = ""
                if (idx == 1) ensureLiveCategories(forceNetwork = false)
                if (idx == 2) ensureVodCategories(forceNetwork = false)
                if (idx == 3) ensureSeriesCategories(forceNetwork = false)
                if (idx == 4) downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Confirmation Prompt when tapping "All"
    confirmAllTab?.let { targetTab ->
        Dialog(onDismissRequest = { confirmAllTab = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Update All List?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
                Text(
                    text = "Do you want to update and reload everything from the server, or use your saved cache?",
                    fontSize = 13.sp,
                    color = MatteGlassTokens.CharcoalSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CanvasWhite)
                            .clickable {
                                confirmAllTab = null
                                if (targetTab == 1) loadLiveCategory("All", forceNetwork = false)
                                if (targetTab == 2) loadVodCategory("All", forceNetwork = false)
                                if (targetTab == 3) loadSeriesCategory("All", forceNetwork = false)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("No (Use Cache)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CharcoalPrimary)
                            .clickable {
                                confirmAllTab = null
                                if (targetTab == 1) loadLiveCategory("All", forceNetwork = true)
                                if (targetTab == 2) loadVodCategory("All", forceNetwork = true)
                                if (targetTab == 3) loadSeriesCategory("All", forceNetwork = true)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Yes (Update)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }

    // Downloader Choice Prompt when mode is "Ask every time"
    pendingDownloadTarget?.let { (dlUrl, dlTitle) ->
        Dialog(onDismissRequest = { pendingDownloadTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Download Video",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
                Text(
                    text = dlTitle,
                    fontSize = 13.sp,
                    color = MatteGlassTokens.CharcoalSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CharcoalPrimary)
                        .clickable {
                            pendingDownloadTarget = null
                            MediaDownloadHelper.startBuiltInDownload(context, dlUrl, dlTitle)
                            downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Built-in Downloader", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CanvasWhite)
                        .clickable {
                            pendingDownloadTarget = null
                            MediaDownloadHelper.startExternalDownload(context, dlUrl, dlTitle)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("External App (1DM+ / ADM)", color = MatteGlassTokens.CharcoalPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onLoadXtream = { name, h, u, p ->
                showAddDialog = false
                val newSrv = SavedServer(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    mode = "xtream",
                    host = XtreamClient.normalizeHost(h),
                    user = u.trim(),
                    pass = p.trim()
                )
                persistServers(servers + newSrv, newSrv.id)
            },
            onLoadM3u = { name, url ->
                showAddDialog = false
                val newSrv = SavedServer(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    mode = "m3u",
                    m3uUrl = url.trim()
                )
                persistServers(servers + newSrv, newSrv.id)
            }
        )
    }
}
