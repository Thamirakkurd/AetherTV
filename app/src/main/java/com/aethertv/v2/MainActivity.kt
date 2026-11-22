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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethertv.v2.data.CategoryItem
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.EpisodeItem
import com.aethertv.v2.data.MediaDetailInfo
import com.aethertv.v2.data.MediaItem
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

    var playlistMode by remember { mutableStateOf(prefs.getString("mode", "") ?: "") }
    var xtreamHost by remember { mutableStateOf(prefs.getString("host", "") ?: "") }
    var xtreamUser by remember { mutableStateOf(prefs.getString("user", "") ?: "") }
    var xtreamPass by remember { mutableStateOf(prefs.getString("pass", "") ?: "") }
    var m3uUrl by remember { mutableStateOf(prefs.getString("m3u_url", "") ?: "") }

    // Catalog state + in-memory cache for "All"
    var liveCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedLiveCat by remember { mutableStateOf("") }
    var liveItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allLiveCache by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var m3uAllItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var vodCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedVodCat by remember { mutableStateOf("") }
    var vodItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allVodCache by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var seriesCategories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedSeriesCat by remember { mutableStateOf("") }
    var seriesItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allSeriesCache by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    // Persisted Favorites & Recents
    var favorites by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("favorites_json", null))) }
    var recents by remember { mutableStateOf(XtreamClient.decodeItems(prefs.getString("recents_json", null))) }
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    // Media Detail (Info Page) & Built-in Player state
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

    fun loadLiveCategory(catName: String) {
        selectedLiveCat = catName
        if (catName == "Favorites") return
        if (playlistMode == "m3u") {
            liveItems = if (catName == "All") m3uAllItems else m3uAllItems.filter { it.group == catName }
            return
        }
        if (catName == "All") {
            if (allLiveCache.isNotEmpty()) {
                liveItems = allLiveCache
                return
            }
            scope.launch {
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
            isLoading = true
            statusMessage = "Loading $catName..."
            try {
                liveItems = XtreamClient.fetchLiveStreams(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_live_streams&category_id=${cat.id}",
                    xtreamHost, xtreamUser, xtreamPass, cat.name
                )
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "Live error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadVodCategory(catName: String) {
        selectedVodCat = catName
        if (catName == "Favorites") return
        if (catName == "All") {
            if (allVodCache.isNotEmpty()) {
                vodItems = allVodCache
                return
            }
            scope.launch {
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
            isLoading = true
            statusMessage = "Loading $catName..."
            try {
                vodItems = XtreamClient.fetchVodStreams(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_streams&category_id=${cat.id}",
                    xtreamHost, xtreamUser, xtreamPass, cat.name
                )
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "Movies error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadSeriesCategory(catName: String) {
        selectedSeriesCat = catName
        if (catName == "Favorites") return
        if (catName == "All") {
            if (allSeriesCache.isNotEmpty()) {
                seriesItems = allSeriesCache
                return
            }
            scope.launch {
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
            isLoading = true
            statusMessage = "Loading $catName..."
            try {
                seriesItems = XtreamClient.fetchSeries(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series&category_id=${cat.id}",
                    cat.name
                )
                statusMessage = null
            } catch (e: Exception) {
                statusMessage = "Series error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun ensureLiveCategories() {
        if (playlistMode != "xtream" || liveCategories.isNotEmpty()) return
        scope.launch {
            isLoading = true
            statusMessage = "Loading live categories..."
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_live_categories"
                )
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                liveCategories = cats
                cats.firstOrNull()?.let { loadLiveCategory(it.name) }
            } catch (e: Exception) {
                statusMessage = "Live error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun ensureVodCategories() {
        if (playlistMode != "xtream" || vodCategories.isNotEmpty()) return
        scope.launch {
            isLoading = true
            statusMessage = "Loading movie categories..."
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_vod_categories"
                )
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                vodCategories = cats
                cats.firstOrNull()?.let { loadVodCategory(it.name) }
            } catch (e: Exception) {
                statusMessage = "Movies error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun ensureSeriesCategories() {
        if (playlistMode != "xtream" || seriesCategories.isNotEmpty()) return
        scope.launch {
            isLoading = true
            statusMessage = "Loading series categories..."
            try {
                val json = XtreamClient.fetchText(
                    "$xtreamHost/player_api.php?username=$xtreamUser&password=$xtreamPass&action=get_series_categories"
                )
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                seriesCategories = cats
                cats.firstOrNull()?.let { loadSeriesCategory(it.name) }
            } catch (e: Exception) {
                statusMessage = "Series error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun connectXtream(rawHost: String, rawUser: String, rawPass: String) {
        val base = XtreamClient.normalizeHost(rawHost)
        val u = rawUser.trim()
        val p = rawPass.trim()
        playlistMode = "xtream"
        xtreamHost = base
        xtreamUser = u
        xtreamPass = p
        liveCategories = emptyList()
        vodCategories = emptyList()
        seriesCategories = emptyList()
        liveItems = emptyList()
        vodItems = emptyList()
        seriesItems = emptyList()
        allLiveCache = emptyList()
        allVodCache = emptyList()
        allSeriesCache = emptyList()
        prefs.edit()
            .putString("mode", "xtream")
            .putString("host", base)
            .putString("user", u)
            .putString("pass", p)
            .apply()
        ensureLiveCategories()
    }

    fun connectM3u(url: String) {
        val cleanUrl = url.trim()
        playlistMode = "m3u"
        m3uUrl = cleanUrl
        prefs.edit().putString("mode", "m3u").putString("m3u_url", cleanUrl).apply()
        scope.launch {
            isLoading = true
            statusMessage = "Loading M3U playlist..."
            try {
                val body = XtreamClient.fetchText(cleanUrl)
                val parsed = withContext(Dispatchers.Default) { XtreamClient.parseM3u(body) }
                m3uAllItems = parsed
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

    LaunchedEffect(Unit) {
        if (playlistMode == "xtream" && xtreamHost.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()) {
            ensureLiveCategories()
        } else if (playlistMode == "m3u" && m3uUrl.isNotBlank()) {
            connectM3u(m3uUrl)
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
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                GlassTopHeader(
                    title = tabs[selectedTab].label,
                    onSearchClick = { showSearch = !showSearch },
                    onFilterClick = {
                        if (selectedTab == 1) ensureLiveCategories()
                        if (selectedTab == 2) ensureVodCategories()
                        if (selectedTab == 3) ensureSeriesCategories()
                    },
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
                            emptyMessage = if (playlistMode.isBlank()) "Tap + to add your playlist" else "No live channels in this category",
                            onSelectCategory = { loadLiveCategory(it) },
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
                            emptyMessage = if (playlistMode != "xtream") "Add an Xtream Codes playlist for Movies" else "No movies in this category",
                            onSelectCategory = { loadVodCategory(it) },
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
                            emptyMessage = if (playlistMode != "xtream") "Add an Xtream Codes playlist for Series" else "No series in this category",
                            onSelectCategory = { loadSeriesCategory(it) },
                            onItemClick = { openDetail(it) },
                            onFavoriteClick = { toggleFavorite(it) },
                            onPlayClick = { openDetail(it) }
                        )
                    }
                    4 -> {
                        val label = when (playlistMode) {
                            "xtream" -> xtreamHost
                            "m3u" -> m3uUrl
                            else -> "No playlist configured"
                        }
                        SettingsScreen(
                            playlistLabel = label,
                            favoritesCount = favorites.size,
                            recentCount = recents.size,
                            onRefreshPlaylist = {
                                if (playlistMode == "xtream" && xtreamHost.isNotBlank()) {
                                    connectXtream(xtreamHost, xtreamUser, xtreamPass)
                                } else if (playlistMode == "m3u" && m3uUrl.isNotBlank()) {
                                    connectM3u(m3uUrl)
                                }
                            },
                            onAddOrChangePlaylist = { showAddDialog = true },
                            onClearAll = {
                                prefs.edit().clear().apply()
                                playlistMode = ""
                                xtreamHost = ""
                                xtreamUser = ""
                                xtreamPass = ""
                                m3uUrl = ""
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
                                favorites = emptyList()
                                recents = emptyList()
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
                if (idx == 1) ensureLiveCategories()
                if (idx == 2) ensureVodCategories()
                if (idx == 3) ensureSeriesCategories()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            initialHost = xtreamHost,
            initialUser = xtreamUser,
            initialPass = xtreamPass,
            initialM3u = m3uUrl,
            onDismiss = { showAddDialog = false },
            onLoadXtream = { h, u, p ->
                showAddDialog = false
                connectXtream(h, u, p)
            },
            onLoadM3u = { url ->
                showAddDialog = false
                connectM3u(url)
            }
        )
    }
}
