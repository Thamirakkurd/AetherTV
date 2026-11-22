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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.aethertv.v2.ui.components.MediaActionBottomSheet
import com.aethertv.v2.ui.components.NavTabItem
import com.aethertv.v2.ui.screens.HomeScreen
import com.aethertv.v2.ui.screens.LiveTvScreen
import com.aethertv.v2.ui.screens.MediaDetailScreen
import com.aethertv.v2.ui.screens.MediaGridScreen
import com.aethertv.v2.ui.screens.PlayerScreen
import com.aethertv.v2.ui.screens.SettingsScreen
import com.aethertv.v2.ui.screens.YouTubeLivePlayerOverlay
import com.aethertv.v2.ui.theme.AppThemeMode
import com.aethertv.v2.ui.theme.MatteGlassTheme
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            val prefs = remember { getSharedPreferences("aethertv_v2_prefs", Context.MODE_PRIVATE) }
            var themeMode by remember {
                val saved = prefs.getString("theme_mode", AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name
                mutableStateOf(
                    try { AppThemeMode.valueOf(saved) } catch (_: Exception) { AppThemeMode.LIGHT }
                )
            }
            val isDark = themeMode == AppThemeMode.DARK
            val appHazeState = remember { HazeState() }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            MatteGlassTheme(darkTheme = isDark, hazeState = appHazeState) {
                AetherV2App(
                    themeMode = themeMode,
                    onChangeThemeMode = { mode ->
                        themeMode = mode
                        prefs.edit().putString("theme_mode", mode.name).apply()
                    },
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
    themeMode: AppThemeMode,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("aethertv_v2_prefs", Context.MODE_PRIVATE) }

    val tabs = remember {
        listOf(
            NavTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem("Live", Icons.Filled.LiveTv, Icons.Outlined.LiveTv),
            NavTabItem("Movies", Icons.Filled.Movie, Icons.Outlined.Movie),
            NavTabItem("Series", Icons.Filled.Tv, Icons.Outlined.Tv),
            NavTabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    // HorizontalPager for smooth 1:1 tab swipe gestures
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage

    // Hoisted scroll states so tab scroll position is preserved across detail screens, players & tab switches
    val homeListState = rememberLazyListState()
    val liveListState = rememberLazyListState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var barsVisible by remember { mutableStateOf(true) }

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
    val playlistScopeKey = activeServer?.id ?: "none"

    // Download mode & offline files
    var downloadMode by remember {
        val savedName = prefs.getString("download_mode", DownloadMode.ASK.name) ?: DownloadMode.ASK.name
        mutableStateOf(
            try { DownloadMode.valueOf(savedName) } catch (_: Exception) { DownloadMode.ASK }
        )
    }
    var pendingDownloadTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var downloadedFiles by remember { mutableStateOf(MediaDownloadHelper.listDownloadedFiles()) }

    // Card ••• Action Sheet target
    var cardMenuTarget by remember { mutableStateOf<MediaItem?>(null) }

    // Confirmation prompt for "All" category
    var confirmAllTab by remember { mutableStateOf<Int?>(null) }

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

    // Instant in-memory detail & episode caches (backed by disk cache)
    val detailMemoryCache = remember { mutableStateMapOf<String, MediaDetailInfo>() }
    val episodesMemoryCache = remember { mutableStateMapOf<String, List<EpisodeItem>>() }

    // Persisted Favorites & Recents
    var favorites by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("favorites_json", null))) }
    var recents by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("recents_json", null))) }
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    // Media Detail (with lastDetailItem retained during slide-out transition), VOD & Live Players
    var selectedDetailItem by remember { mutableStateOf<MediaItem?>(null) }
    var lastRenderedDetailItem by remember { mutableStateOf<MediaItem?>(null) }
    var detailInfo by remember { mutableStateOf(MediaDetailInfo()) }
    var detailEpisodes by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var loadingDetail by remember { mutableStateOf(false) }

    var activeVodItem by remember { mutableStateOf<MediaItem?>(null) }
    var activeLiveItem by remember { mutableStateOf<MediaItem?>(null) }
    var isLiveMiniPlayer by remember { mutableStateOf(false) }

    val liveCatNames = remember(liveCategories) {
        listOf("All", "Favorites") + liveCategories.map { it.name }
    }
    val vodCatNames = remember(vodCategories) {
        listOf("All", "Favorites") + vodCategories.map { it.name }
    }
    val seriesCatNames = remember(seriesCategories) {
        listOf("All", "Favorites") + seriesCategories.map { it.name }
    }

    val shownLive by produceState(
        initialValue = emptyList<MediaItem>(),
        liveItems, selectedLiveCat, searchQuery, favorites
    ) {
        value = withContext(Dispatchers.Default) {
            val source = if (selectedLiveCat == "Favorites") {
                favorites.filter { it.contentType == ContentType.LIVE }
            } else {
                liveItems
            }
            if (searchQuery.isBlank()) source
            else source.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val shownVod by produceState(
        initialValue = emptyList<MediaItem>(),
        vodItems, selectedVodCat, searchQuery, favorites
    ) {
        value = withContext(Dispatchers.Default) {
            val source = if (selectedVodCat == "Favorites") {
                favorites.filter { it.contentType == ContentType.MOVIE }
            } else {
                vodItems
            }
            if (searchQuery.isBlank()) source
            else source.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val shownSeries by produceState(
        initialValue = emptyList<MediaItem>(),
        seriesItems, selectedSeriesCat, searchQuery, favorites
    ) {
        value = withContext(Dispatchers.Default) {
            val source = if (selectedSeriesCat == "Favorites") {
                favorites.filter { it.contentType == ContentType.SERIES }
            } else {
                seriesItems
            }
            if (searchQuery.isBlank()) source
            else source.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Back-navigation flow
    BackHandler(
        enabled = cardMenuTarget != null ||
            activeVodItem != null ||
            (activeLiveItem != null && !isLiveMiniPlayer) ||
            selectedDetailItem != null ||
            selectedTab != 0
    ) {
        when {
            cardMenuTarget != null -> cardMenuTarget = null
            activeVodItem != null -> activeVodItem = null
            activeLiveItem != null && !isLiveMiniPlayer -> isLiveMiniPlayer = true
            selectedDetailItem != null -> selectedDetailItem = null
            selectedTab != 0 -> {
                searchQuery = ""
                barsVisible = true
                scope.launch { pagerState.animateScrollToPage(0) }
            }
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
        if (item.contentType == ContentType.LIVE) {
            activeVodItem = null
            activeLiveItem = item
            isLiveMiniPlayer = false
        } else {
            activeLiveItem = null
            isLiveMiniPlayer = false
            activeVodItem = item
        }
    }

    fun openDetail(item: MediaItem) {
        selectedDetailItem = item
        lastRenderedDetailItem = item
        val cacheKey = "${playlistScopeKey}_${item.id}"

        // 1. Instant hit from in-memory cache (zero reload / zero flicker)
        val memInfo = detailMemoryCache[cacheKey]
        val memEps = episodesMemoryCache[cacheKey]
        if (memInfo != null && (item.contentType != ContentType.SERIES || memEps != null)) {
            detailInfo = memInfo
            detailEpisodes = memEps ?: emptyList()
            loadingDetail = false
            return
        }

        detailInfo = MediaDetailInfo()
        detailEpisodes = emptyList()
        if (playlistMode != "xtream" || xtreamHost.isBlank()) {
            loadingDetail = false
            return
        }

        when (item.contentType) {
            ContentType.MOVIE -> {
                val vodId = item.id.removePrefix("vod_")
                if (vodId.isBlank()) return
                scope.launch {
                    val diskInfo = PlaylistDiskCache.loadDetailInfo(context, cacheKey)
                    if (diskInfo != null) {
                        detailMemoryCache[cacheKey] = diskInfo
                        if (selectedDetailItem?.id == item.id) {
                            detailInfo = diskInfo
                            loadingDetail = false
                        }
                        return@launch
                    }
                    loadingDetail = true
                    try {
                        val json = XtreamClient.fetchText(
                            "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_info&vod_id=$vodId"
                        )
                        val parsed = withContext(Dispatchers.Default) { XtreamClient.parseVodDetail(json) }
                        detailMemoryCache[cacheKey] = parsed
                        PlaylistDiskCache.saveDetailInfo(context, cacheKey, parsed)
                        if (selectedDetailItem?.id == item.id) detailInfo = parsed
                    } catch (_: Exception) {
                    } finally {
                        if (selectedDetailItem?.id == item.id) loadingDetail = false
                    }
                }
            }
            ContentType.SERIES -> {
                val sid = item.seriesId ?: return
                scope.launch {
                    val diskInfo = PlaylistDiskCache.loadDetailInfo(context, cacheKey)
                    val diskEps = PlaylistDiskCache.loadEpisodes(context, cacheKey)
                    if (diskInfo != null && diskEps.isNotEmpty()) {
                        detailMemoryCache[cacheKey] = diskInfo
                        episodesMemoryCache[cacheKey] = diskEps
                        if (selectedDetailItem?.id == item.id) {
                            detailInfo = diskInfo
                            detailEpisodes = diskEps
                            loadingDetail = false
                        }
                        return@launch
                    }
                    loadingDetail = true
                    try {
                        val json = XtreamClient.fetchText(
                            "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series_info&series_id=$sid"
                        )
                        val parsedInfo = withContext(Dispatchers.Default) { XtreamClient.parseSeriesDetail(json) }
                        val parsedEps = withContext(Dispatchers.Default) {
                            XtreamClient.parseSeriesEpisodes(json, xtreamHost, xtreamUser, xtreamPass)
                        }
                        detailMemoryCache[cacheKey] = parsedInfo
                        episodesMemoryCache[cacheKey] = parsedEps
                        PlaylistDiskCache.saveDetailInfo(context, cacheKey, parsedInfo)
                        PlaylistDiskCache.saveEpisodes(context, cacheKey, parsedEps)
                        if (selectedDetailItem?.id == item.id) {
                            detailInfo = parsedInfo
                            detailEpisodes = parsedEps
                        }
                    } catch (_: Exception) {
                    } finally {
                        if (selectedDetailItem?.id == item.id) loadingDetail = false
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
        detailMemoryCache.clear()
        episodesMemoryCache.clear()
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

    // Trigger lazy category load when user swipes or taps to a tab
    LaunchedEffect(selectedTab, activeServerId) {
        searchQuery = ""
        barsVisible = true
        when (selectedTab) {
            1 -> ensureLiveCategories(forceNetwork = false)
            2 -> ensureVodCategories(forceNetwork = false)
            3 -> ensureSeriesCategories(forceNetwork = false)
            4 -> downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
        }
    }

    val hideBarsConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -18f && barsVisible) barsVisible = false
                else if (available.y > 18f && !barsVisible) barsVisible = true
                return Offset.Zero
            }
        }
    }

    val miniPlayerExtraPadding = if (activeLiveItem != null && isLiveMiniPlayer) 74.dp else 0.dp
    val detailOpen = selectedDetailItem != null
    val backgroundScale by animateFloatAsState(
        targetValue = if (detailOpen) 0.968f else 1f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "bgDepthScale"
    )
    val backgroundScrimAlpha by animateFloatAsState(
        targetValue = if (detailOpen) 0.24f else 0f,
        animationSpec = tween(260),
        label = "bgScrimAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
            .nestedScroll(hideBarsConnection)
    ) {
        // Main Swipeable Tab Pager (stays mounted underneath detail & player so scroll positions never reset)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = backgroundScale
                    scaleY = backgroundScale
                }
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !detailOpen && activeVodItem == null && (activeLiveItem == null || isLiveMiniPlayer),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        val movieAndSeriesFavs = remember(favorites) {
                            favorites.filter { it.contentType == ContentType.MOVIE || it.contentType == ContentType.SERIES }
                        }
                        HomeScreen(
                            favoriteMedia = movieAndSeriesFavs,
                            recentlyWatched = recents,
                            favoriteIds = favoriteIds,
                            hasPlaylist = playlistMode.isNotBlank(),
                            listState = homeListState,
                            extraBottomPadding = miniPlayerExtraPadding,
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { if (it.contentType == ContentType.SERIES) openDetail(it) else playItem(it) },
                            onMoreClick = { cardMenuTarget = it },
                            onAddPlaylistClick = { showAddDialog = true }
                        )
                    }
                    1 -> {
                        LiveTvScreen(
                            items = shownLive,
                            categories = liveCatNames,
                            selectedCategory = selectedLiveCat,
                            favoriteIds = favoriteIds,
                            isLoading = isLoading,
                            listState = liveListState,
                            topBarVisible = barsVisible,
                            extraBottomPadding = miniPlayerExtraPadding,
                            emptyMessage = if (playlistMode.isBlank()) "Add a server from Home or Settings to watch Live TV" else "No live channels in this category",
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSelectCategory = { cat ->
                                if (cat == "All") confirmAllTab = 1
                                else loadLiveCategory(cat, forceNetwork = false)
                            },
                            onPlayClick = { playItem(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onMoreClick = { cardMenuTarget = it }
                        )
                    }
                    2 -> {
                        MediaGridScreen(
                            items = shownVod,
                            categories = vodCatNames,
                            selectedCategory = selectedVodCat,
                            favoriteIds = favoriteIds,
                            isLoading = isLoading,
                            gridState = moviesGridState,
                            topBarVisible = barsVisible,
                            extraBottomPadding = miniPlayerExtraPadding,
                            emptyMessage = if (playlistMode != "xtream") "Add an Xtream Codes server for Movies" else "No movies in this category",
                            searchQuery = searchQuery,
                            searchPlaceholder = "Search movies...",
                            onSearchQueryChange = { searchQuery = it },
                            onSelectCategory = { cat ->
                                if (cat == "All") confirmAllTab = 2
                                else loadVodCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { playItem(it) },
                            onMoreClick = { cardMenuTarget = it }
                        )
                    }
                    3 -> {
                        MediaGridScreen(
                            items = shownSeries,
                            categories = seriesCatNames,
                            selectedCategory = selectedSeriesCat,
                            favoriteIds = favoriteIds,
                            isLoading = isLoading,
                            gridState = seriesGridState,
                            topBarVisible = barsVisible,
                            extraBottomPadding = miniPlayerExtraPadding,
                            emptyMessage = if (playlistMode != "xtream") "Add an Xtream Codes server for Series" else "No series in this category",
                            searchQuery = searchQuery,
                            searchPlaceholder = "Search series...",
                            onSearchQueryChange = { searchQuery = it },
                            onSelectCategory = { cat ->
                                if (cat == "All") confirmAllTab = 3
                                else loadSeriesCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { openDetail(it) },
                            onMoreClick = { cardMenuTarget = it }
                        )
                    }
                    4 -> {
                        SettingsScreen(
                            servers = servers,
                            activeServerId = activeServerId,
                            themeMode = themeMode,
                            downloadMode = downloadMode,
                            downloadedFiles = downloadedFiles,
                            favoritesCount = favorites.size,
                            recentCount = recents.size,
                            extraBottomPadding = miniPlayerExtraPadding,
                            onChangeThemeMode = onChangeThemeMode,
                            onSelectServer = { srv -> persistServers(servers, srv.id) },
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

            statusMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 74.dp + miniPlayerExtraPadding)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MatteGlassTokens.SheetBg)
                        .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (msg.contains("error", ignoreCase = true)) MatteGlassTokens.NotificationDot else MatteGlassTokens.CharcoalMuted
                    )
                }
            }

            AnimatedVisibility(
                visible = barsVisible && !detailOpen,
                enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingGlassNavBar(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { idx ->
                        selectedDetailItem = null
                        barsVisible = true
                        scope.launch { pagerState.animateScrollToPage(idx) }
                    }
                )
            }

            // Subtle dim scrim while detail screen slides over
            if (backgroundScrimAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = backgroundScrimAlpha))
                )
            }
        }

        // Slide-from-right animated Detail Screen overlay
        AnimatedVisibility(
            visible = detailOpen,
            enter = slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            val shownDetail = selectedDetailItem ?: lastRenderedDetailItem
            if (shownDetail != null) {
                MediaDetailScreen(
                    item = shownDetail,
                    detailInfo = detailInfo,
                    episodes = detailEpisodes,
                    isLoading = loadingDetail,
                    isFavorite = favoriteIds.contains(shownDetail.id),
                    onBack = { selectedDetailItem = null },
                    onToggleFavorite = { toggleFavorite(shownDetail) },
                    onWatchNow = { playItem(shownDetail) },
                    onDownloadItem = { triggerDownload(shownDetail.streamUrl, shownDetail.name) },
                    onEpisodeClick = { ep ->
                        val epMedia = MediaItem(
                            id = "ep_${ep.id}",
                            name = "${shownDetail.name} • ${ep.title}",
                            group = ep.season,
                            posterUrl = shownDetail.posterUrl,
                            streamUrl = ep.streamUrl,
                            contentType = ContentType.MOVIE,
                            badge = ep.season
                        )
                        playItem(epMedia)
                    },
                    onDownloadEpisode = { ep ->
                        triggerDownload(ep.streamUrl, "${shownDetail.name} - ${ep.title}")
                    }
                )
            }
        }

        // Persistent YouTube-style Live TV Player (Expanded, Immersive Fullscreen, or Docked Blurred Mini-Player)
        activeLiveItem?.let { liveChannel ->
            val livePool = remember(shownLive, liveItems, allLiveCache, favorites) {
                when {
                    shownLive.isNotEmpty() -> shownLive
                    liveItems.isNotEmpty() -> liveItems
                    allLiveCache.isNotEmpty() -> allLiveCache
                    else -> favorites.filter { it.contentType == ContentType.LIVE }
                }
            }
            YouTubeLivePlayerOverlay(
                channel = liveChannel,
                allChannels = livePool,
                categories = liveCatNames,
                selectedCategory = selectedLiveCat,
                isLoadingCategory = isLoading,
                favoriteIds = favoriteIds,
                isMiniPlayer = isLiveMiniPlayer,
                isInPipMode = isInPipMode,
                barsVisible = barsVisible && !detailOpen,
                onSelectCategory = { cat -> loadLiveCategory(cat, forceNetwork = false) },
                onSwitchChannel = { next ->
                    recordRecent(next)
                    activeLiveItem = next
                },
                onToggleMinimize = { mini -> isLiveMiniPlayer = mini },
                onClose = {
                    activeLiveItem = null
                    isLiveMiniPlayer = false
                },
                onToggleFavorite = { toggleFavorite(it) },
                onMoreClick = { cardMenuTarget = it },
                onEnterPip = onEnterPip,
                onOpenExternal = { launchExternalPlayer(context, it.streamUrl, it.name) }
            )
        }

        // Fullscreen Immersive VOD / Movie / Series Episode Player
        activeVodItem?.let { playingVod ->
            PlayerScreen(
                item = playingVod,
                isInPipMode = isInPipMode,
                onBack = { activeVodItem = null },
                onEnterPip = onEnterPip,
                onOpenExternal = { launchExternalPlayer(context, playingVod.streamUrl, playingVod.name) }
            )
        }
    }

    // Card ••• Action Sheet
    cardMenuTarget?.let { target ->
        val isDownloadable = target.contentType == ContentType.MOVIE
        MediaActionBottomSheet(
            item = target,
            isFavorite = favoriteIds.contains(target.id),
            onDismiss = { cardMenuTarget = null },
            onViewDetails = if (target.contentType != ContentType.LIVE) {
                { openDetail(target) }
            } else null,
            onPlay = {
                if (target.contentType == ContentType.SERIES) openDetail(target)
                else playItem(target)
            },
            onOpenExternal = { launchExternalPlayer(context, target.streamUrl, target.name) },
            onDownloadBuiltIn = if (isDownloadable) {
                {
                    MediaDownloadHelper.startBuiltInDownload(context, target.streamUrl, target.name)
                    downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
                }
            } else null,
            onDownloadExternal = if (isDownloadable) {
                { MediaDownloadHelper.startExternalDownload(context, target.streamUrl, target.name) }
            } else null,
            onToggleFavorite = { toggleFavorite(target) }
        )
    }

    // Confirmation Prompt when tapping "All"
    confirmAllTab?.let { targetTab ->
        Dialog(onDismissRequest = { confirmAllTab = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MatteGlassTokens.SheetBg)
                    .border(1.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(22.dp))
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
                    color = MatteGlassTokens.CharcoalMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .livelyPress(pressedScale = 0.94f) {
                                confirmAllTab = null
                                if (targetTab == 1) loadLiveCategory("All", forceNetwork = false)
                                if (targetTab == 2) loadVodCategory("All", forceNetwork = false)
                                if (targetTab == 3) loadSeriesCategory("All", forceNetwork = false)
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.SubtleSurface)
                            .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("No (Use Cache)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .livelyPress(pressedScale = 0.94f) {
                                confirmAllTab = null
                                if (targetTab == 1) loadLiveCategory("All", forceNetwork = true)
                                if (targetTab == 2) loadVodCategory("All", forceNetwork = true)
                                if (targetTab == 3) loadSeriesCategory("All", forceNetwork = true)
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CharcoalPrimary)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Yes (Update)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CanvasWhite)
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
                    .background(MatteGlassTokens.SheetBg)
                    .border(1.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(22.dp))
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
                    color = MatteGlassTokens.CharcoalMuted
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .livelyPress(pressedScale = 0.96f) {
                            pendingDownloadTarget = null
                            MediaDownloadHelper.startBuiltInDownload(context, dlUrl, dlTitle)
                            downloadedFiles = MediaDownloadHelper.listDownloadedFiles()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CharcoalPrimary)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Built-in Downloader", color = MatteGlassTokens.CanvasWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .livelyPress(pressedScale = 0.96f) {
                            pendingDownloadTarget = null
                            MediaDownloadHelper.startExternalDownload(context, dlUrl, dlTitle)
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.SubtleSurface)
                        .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
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
