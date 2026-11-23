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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.aethertv.v2.ui.screens.SettingsScreen
import com.aethertv.v2.ui.screens.UnifiedMorphPlayerOverlay
import com.aethertv.v2.ui.theme.AppThemeMode
import com.aethertv.v2.ui.theme.MatteGlassTheme
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock window to display's highest refresh rate (120Hz)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val dm = getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            val maxMode = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = maxMode.modeId
                }
            }
        }

        // High-throughput Coil cache for instant zero-flicker poster scrolling
        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(applicationContext)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.35)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(java.io.File(applicationContext.cacheDir, "poster_cache"))
                        .maxSizeBytes(250L * 1024 * 1024)
                        .build()
                }
                .allowHardware(true)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .crossfade(false)
                .respectCacheHeaders(false)
                .build()
        )
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
            NavTabItem("Movies", Icons.Filled.Movie, Icons.Outlined.Movie),
            NavTabItem("Series", Icons.Filled.Tv, Icons.Outlined.Tv),
            NavTabItem("Live", Icons.Filled.LiveTv, Icons.Outlined.LiveTv),
            NavTabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage

    // Hoisted scroll states preserve exact scroll spots across tabs, details & players
    val homeListState = rememberLazyListState()
    val liveListState = rememberLazyListState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var barsVisible by remember { mutableStateOf(true) }

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

    var downloadMode by remember {
        val savedName = prefs.getString("download_mode", DownloadMode.ASK.name) ?: DownloadMode.ASK.name
        mutableStateOf(
            try { DownloadMode.valueOf(savedName) } catch (_: Exception) { DownloadMode.ASK }
        )
    }
    var pendingDownloadTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var downloadedFiles by remember { mutableStateOf(MediaDownloadHelper.listDownloadedFiles()) }

    var cardMenuTarget by remember { mutableStateOf<MediaItem?>(null) }
    var confirmAllTab by remember { mutableStateOf<Int?>(null) }

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

    // Instant memory caches for Movie/Series details & episodes (backed by disk cache)
    val detailMemoryCache = remember { mutableStateMapOf<String, MediaDetailInfo>() }
    val episodesMemoryCache = remember { mutableStateMapOf<String, List<EpisodeItem>>() }

    var favorites by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("favorites_json", null))) }
    var recents by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("recents_json", null))) }
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    // Dedicated Home collection pages ("favorites" or "recents")
    var activeLibraryPage by remember { mutableStateOf<String?>(null) }
    var lastLibraryPage by remember { mutableStateOf<String?>(null) }
    var libraryRecentMenuTarget by remember { mutableStateOf<MediaItem?>(null) }

    // Detail screen state
    var selectedDetailItem by remember { mutableStateOf<MediaItem?>(null) }
    var lastRenderedDetailItem by remember { mutableStateOf<MediaItem?>(null) }
    var detailInfo by remember { mutableStateOf(MediaDetailInfo()) }
    var detailEpisodes by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var loadingDetail by remember { mutableStateOf(false) }

    // Unified Zero-Flash Morphing Player state (Live TV + Movies + Series Episodes)
    var activePlayerItem by remember { mutableStateOf<MediaItem?>(null) }
    var isPlayerMini by remember { mutableStateOf(false) }
    var playerVodDetailInfo by remember { mutableStateOf(MediaDetailInfo()) }
    var playerVodEpisodes by remember { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var playerSeriesParent by remember { mutableStateOf<MediaItem?>(null) }
    val playerExpandAnim = remember { Animatable(0f) }

    LaunchedEffect(activePlayerItem == null) {
        if (activePlayerItem == null) {
            playerExpandAnim.snapTo(0f)
        }
    }

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
            (activePlayerItem != null && !isPlayerMini) ||
            selectedDetailItem != null ||
            selectedTab != 0
    ) {
        when {
            cardMenuTarget != null -> cardMenuTarget = null
            activePlayerItem != null && !isPlayerMini -> isPlayerMini = true
            selectedDetailItem != null -> selectedDetailItem = null
            activeLibraryPage != null -> activeLibraryPage = null
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

    fun removeRecent(item: MediaItem) {
        val updated = recents.filterNot { it.id == item.id }
        recents = updated
        prefs.edit().putString("recents_json", XtreamClient.encodeItems(updated)).apply()
    }

    fun clearRecents() {
        recents = emptyList()
        prefs.edit().remove("recents_json").apply()
    }

    fun fetchMovieDetailForPlayerIfNeeded(movie: MediaItem) {
        val cacheKey = "${playlistScopeKey}_${movie.id}"
        val mem = detailMemoryCache[cacheKey]
        if (mem != null) {
            playerVodDetailInfo = mem
            return
        }
        if (playlistMode != "xtream" || xtreamHost.isBlank()) return
        val vodId = movie.id.removePrefix("vod_")
        if (vodId.isBlank() || vodId == movie.id) return
        scope.launch {
            val disk = PlaylistDiskCache.loadDetailInfo(context, cacheKey)
            if (disk != null) {
                detailMemoryCache[cacheKey] = disk
                if (activePlayerItem?.id == movie.id) playerVodDetailInfo = disk
                return@launch
            }
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_info&vod_id=$vodId"
                )
                val parsed = withContext(Dispatchers.Default) { XtreamClient.parseVodDetail(json) }
                detailMemoryCache[cacheKey] = parsed
                PlaylistDiskCache.saveDetailInfo(context, cacheKey, parsed)
                if (activePlayerItem?.id == movie.id) playerVodDetailInfo = parsed
            } catch (_: Exception) {
            }
        }
    }

    fun playItem(
        item: MediaItem,
        info: MediaDetailInfo? = null,
        episodes: List<EpisodeItem> = emptyList(),
        seriesParent: MediaItem? = null
    ) {
        recordRecent(item)
        activePlayerItem = item
        isPlayerMini = false
        if (item.contentType == ContentType.LIVE) {
            playerVodDetailInfo = MediaDetailInfo()
            playerVodEpisodes = emptyList()
            playerSeriesParent = null
        } else {
            playerVodEpisodes = episodes
            playerSeriesParent = seriesParent
            if (info != null) {
                playerVodDetailInfo = info
            } else {
                playerVodDetailInfo = MediaDetailInfo()
                fetchMovieDetailForPlayerIfNeeded(item)
            }
        }
    }

    fun openDetail(item: MediaItem) {
        selectedDetailItem = item
        lastRenderedDetailItem = item
        val cacheKey = "${playlistScopeKey}_${item.id}"

        // Instant hit from in-memory cache
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

    LaunchedEffect(selectedTab, activeServerId) {
        searchQuery = ""
        barsVisible = true
        when (selectedTab) {
            1 -> ensureVodCategories(forceNetwork = false)
            2 -> ensureSeriesCategories(forceNetwork = false)
            3 -> ensureLiveCategories(forceNetwork = false)
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

    val miniPlayerExtraPadding = if (activePlayerItem != null && isPlayerMini) 74.dp else 0.dp
    val detailOpen = selectedDetailItem != null

    val detailDepth by animateFloatAsState(
        targetValue = if (detailOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 310f),
        label = "detailDepth"
    )
    val playerDepth = if (activePlayerItem != null) playerExpandAnim.value.coerceIn(0f, 1f) else 0f
    val combinedDepth = maxOf(detailDepth, playerDepth)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
            .nestedScroll(hideBarsConnection)
    ) {
        // Main Swipeable Tab Pager with subtle spatial depth & horizontal parallax behind overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1f - 0.036f * combinedDepth
                    scaleX = scale
                    scaleY = scale
                    translationX = -detailDepth * 52f
                }
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !detailOpen && (activePlayerItem == null || isPlayerMini),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val s = 1f - absOffset * 0.045f
                            scaleX = s
                            scaleY = s
                            alpha = 1f - absOffset * 0.22f
                            translationX = pageOffset * -28f
                        }
                ) {
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
                            onAddPlaylistClick = { showAddDialog = true },
                            onOpenFavoritesPage = {
                                lastLibraryPage = "favorites"
                                activeLibraryPage = "favorites"
                            },
                            onOpenRecentsPage = {
                                lastLibraryPage = "recents"
                                activeLibraryPage = "recents"
                            },
                            onRemoveRecent = { removeRecent(it) },
                            onClearRecents = { clearRecents() }
                        )
                    }
                    1 -> {
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
                                if (cat == "All") confirmAllTab = 1
                                else loadVodCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { playItem(it) },
                            onMoreClick = { cardMenuTarget = it }
                        )
                    }
                    2 -> {
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
                                if (cat == "All") confirmAllTab = 2
                                else loadSeriesCategory(cat, forceNetwork = false)
                            },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { openDetail(it) },
                            onMoreClick = { cardMenuTarget = it }
                        )
                    }
                    3 -> {
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
                                if (cat == "All") confirmAllTab = 3
                                else loadLiveCategory(cat, forceNetwork = false)
                            },
                            onPlayClick = { playItem(it) },
                            onFavoriteClick = { toggleFavorite(it) },
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
                enter = slideInVertically(spring(dampingRatio = 0.80f, stiffness = 360f)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(spring(dampingRatio = 0.86f, stiffness = 400f)) { it } + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val livePagePos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, (tabs.size - 1).toFloat())
                FloatingGlassNavBar(
                    items = tabs,
                    selectedIndex = selectedTab,
                    pagePosition = livePagePos,
                    onSelect = { idx ->
                        selectedDetailItem = null
                        activeLibraryPage = null
                        barsVisible = true
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = idx,
                                animationSpec = spring(dampingRatio = 0.84f, stiffness = 340f)
                            )
                        }
                    }
                )
            }

            if (combinedDepth > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f * combinedDepth))
                )
            }
        }

        // Slide-from-right dedicated Favorites / Recently Watched page
        AnimatedVisibility(
            visible = activeLibraryPage != null,
            enter = slideInHorizontally(spring(dampingRatio = 0.84f, stiffness = 310f)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(spring(dampingRatio = 0.86f, stiffness = 360f)) { it } + fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            val shownLibraryPage = activeLibraryPage ?: lastLibraryPage
            val isRecentsPage = shownLibraryPage == "recents"
            val pageItems = remember(shownLibraryPage, favorites, recents) {
                if (isRecentsPage) recents
                else favorites.filter { it.contentType == ContentType.MOVIE || it.contentType == ContentType.SERIES }
            }
            com.aethertv.v2.ui.screens.LibraryCollectionScreen(
                title = if (isRecentsPage) "Recently Watched" else "Favorites",
                items = pageItems,
                favoriteIds = favoriteIds,
                emptyMessage = if (isRecentsPage) {
                    "Items you watch will show up here for quick resume."
                } else {
                    "Tap ★ on any movie or series to add it to Favorites."
                },
                extraBottomPadding = miniPlayerExtraPadding,
                onBack = { activeLibraryPage = null },
                onItemClick = { item ->
                    if (isRecentsPage && item.contentType == ContentType.LIVE) playItem(item)
                    else openDetail(item)
                },
                onFavoriteClick = { toggleFavorite(it) },
                onPlayClick = { item ->
                    if (item.contentType == ContentType.SERIES) openDetail(item)
                    else playItem(item)
                },
                onMoreClick = { cardMenuTarget = it },
                onItemLongClick = if (isRecentsPage) {
                    { item -> libraryRecentMenuTarget = item }
                } else null,
                onClearAllClick = if (isRecentsPage) {
                    { clearRecents() }
                } else null
            )

            libraryRecentMenuTarget?.let { target ->
                com.aethertv.v2.ui.screens.RecentItemActionSheet(
                    item = target,
                    onDismiss = { libraryRecentMenuTarget = null },
                    onResume = {
                        libraryRecentMenuTarget = null
                        if (target.contentType == ContentType.LIVE) playItem(target)
                        else openDetail(target)
                    },
                    onRemove = {
                        libraryRecentMenuTarget = null
                        removeRecent(target)
                    },
                    onClearAll = {
                        libraryRecentMenuTarget = null
                        clearRecents()
                    }
                )
            }
        }

        // Slide-from-right spring animated Detail Screen overlay
        AnimatedVisibility(
            visible = detailOpen,
            enter = slideInHorizontally(spring(dampingRatio = 0.84f, stiffness = 310f)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(spring(dampingRatio = 0.86f, stiffness = 360f)) { it } + fadeOut(tween(160)),
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
                    onWatchNow = {
                        playItem(
                            item = shownDetail,
                            info = detailInfo,
                            episodes = detailEpisodes,
                            seriesParent = if (shownDetail.contentType == ContentType.SERIES) shownDetail else null
                        )
                    },
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
                        playItem(
                            item = epMedia,
                            info = detailInfo,
                            episodes = detailEpisodes,
                            seriesParent = shownDetail
                        )
                    },
                    onDownloadEpisode = { ep ->
                        triggerDownload(ep.streamUrl, "${shownDetail.name} - ${ep.title}")
                    }
                )
            }
        }

        // SINGLE PERSISTENT UNIFIED ZERO-FLASH MORPHING PLAYER (Live TV + Movies + Series Episodes)
        activePlayerItem?.let { playingItem ->
            val livePool = remember(shownLive, liveItems, allLiveCache, favorites) {
                when {
                    shownLive.isNotEmpty() -> shownLive
                    liveItems.isNotEmpty() -> liveItems
                    allLiveCache.isNotEmpty() -> allLiveCache
                    else -> favorites.filter { it.contentType == ContentType.LIVE }
                }
            }
            val relatedMovies = remember(vodItems, playingItem.id, playingItem.contentType) {
                if (playingItem.contentType == ContentType.MOVIE && playerVodEpisodes.isEmpty()) {
                    vodItems.filter { it.id != playingItem.id }.take(24)
                } else emptyList()
            }

            UnifiedMorphPlayerOverlay(
                item = playingItem,
                expandAnim = playerExpandAnim,
                isMiniPlayer = isPlayerMini,
                isInPipMode = isInPipMode,
                barsVisible = barsVisible && !detailOpen,
                favoriteIds = favoriteIds,
                liveChannels = livePool,
                liveCategories = liveCatNames,
                selectedLiveCategory = selectedLiveCat,
                isLoadingLiveCategory = isLoading,
                vodDetailInfo = playerVodDetailInfo,
                vodEpisodes = playerVodEpisodes,
                relatedVodItems = relatedMovies,
                onSelectLiveCategory = { cat -> loadLiveCategory(cat, forceNetwork = false) },
                onSwitchItem = { next ->
                    if (next.contentType == ContentType.LIVE) {
                        recordRecent(next)
                        activePlayerItem = next
                    } else {
                        playItem(next)
                    }
                },
                onPlayEpisode = { ep ->
                    val parent = playerSeriesParent
                    val epMedia = MediaItem(
                        id = "ep_${ep.id}",
                        name = if (parent != null) "${parent.name} • ${ep.title}" else ep.title,
                        group = ep.season,
                        posterUrl = parent?.posterUrl ?: playingItem.posterUrl,
                        streamUrl = ep.streamUrl,
                        contentType = ContentType.MOVIE,
                        badge = ep.season
                    )
                    playItem(
                        item = epMedia,
                        info = playerVodDetailInfo,
                        episodes = playerVodEpisodes,
                        seriesParent = parent
                    )
                },
                onDownloadItem = { dlUrl, dlTitle -> triggerDownload(dlUrl, dlTitle) },
                onToggleMinimize = { mini -> isPlayerMini = mini },
                onClose = {
                    activePlayerItem = null
                    isPlayerMini = false
                    playerVodEpisodes = emptyList()
                    playerSeriesParent = null
                },
                onToggleFavorite = { target ->
                    val favTarget = playerSeriesParent ?: target
                    toggleFavorite(favTarget)
                },
                onMoreClick = { cardMenuTarget = it },
                onEnterPip = onEnterPip,
                onOpenExternal = { launchExternalPlayer(context, it.streamUrl, it.name) }
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
                                if (targetTab == 1) loadVodCategory("All", forceNetwork = false)
                                if (targetTab == 2) loadSeriesCategory("All", forceNetwork = false)
                                if (targetTab == 3) loadLiveCategory("All", forceNetwork = false)
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
                                if (targetTab == 1) loadVodCategory("All", forceNetwork = true)
                                if (targetTab == 2) loadSeriesCategory("All", forceNetwork = true)
                                if (targetTab == 3) loadLiveCategory("All", forceNetwork = true)
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
