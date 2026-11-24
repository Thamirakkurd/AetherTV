package com.aethertv.v2

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethertv.v2.data.CategoryItem
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.DownloadMode
import com.aethertv.v2.data.DownloadedMediaFile
import com.aethertv.v2.data.EpisodeItem
import com.aethertv.v2.data.MediaDetailInfo
import com.aethertv.v2.data.MediaDownloadHelper
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.data.PlaybackProgress
import com.aethertv.v2.data.PlaybackProgressStore
import com.aethertv.v2.data.PlaylistDiskCache
import com.aethertv.v2.data.SavedServer
import com.aethertv.v2.data.SecurePrefsHelper
import com.aethertv.v2.data.ServerStore
import com.aethertv.v2.data.XtreamClient
import com.aethertv.v2.ui.theme.AppThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AetherViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext
    private val prefs = appContext.getSharedPreferences("aethertv_v2_prefs", Context.MODE_PRIVATE)

    // --- Theme & Preferences ---
    var themeMode by mutableStateOf(
        run {
            val saved = prefs.getString("theme_mode", AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name
            try { AppThemeMode.valueOf(saved) } catch (_: Exception) { AppThemeMode.LIGHT }
        }
    )
        private set

    var downloadMode by mutableStateOf(
        run {
            val saved = prefs.getString("download_mode", DownloadMode.ASK.name) ?: DownloadMode.ASK.name
            try { DownloadMode.valueOf(saved) } catch (_: Exception) { DownloadMode.ASK }
        }
    )
        private set

    // --- Servers ---
    var servers by mutableStateOf<List<SavedServer>>(emptyList())
        private set
    var activeServerId by mutableStateOf("")
        private set

    val activeServer: SavedServer? by derivedStateOf {
        servers.firstOrNull { it.id == activeServerId } ?: servers.firstOrNull()
    }
    val playlistMode: String get() = activeServer?.mode ?: ""
    val playlistScopeKey: String get() = activeServer?.id ?: "none"

    // --- Per-tab loading & auto-dismissing status ---
    var isLoadingLive by mutableStateOf(false)
        private set
    var isLoadingVod by mutableStateOf(false)
        private set
    var isLoadingSeries by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    // --- Per-tab search queries ---
    var liveSearchQuery by mutableStateOf("")
    var vodSearchQuery by mutableStateOf("")
    var seriesSearchQuery by mutableStateOf("")

    // --- Catalog state ---
    var liveCategories by mutableStateOf<List<CategoryItem>>(emptyList())
        private set
    var selectedLiveCat by mutableStateOf("")
        private set
    var liveItems by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var allLiveCache by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    private var m3uAllItems: List<MediaItem> = emptyList()

    var vodCategories by mutableStateOf<List<CategoryItem>>(emptyList())
        private set
    var selectedVodCat by mutableStateOf("")
        private set
    var vodItems by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    private var allVodCache: List<MediaItem> = emptyList()

    var seriesCategories by mutableStateOf<List<CategoryItem>>(emptyList())
        private set
    var selectedSeriesCat by mutableStateOf("")
        private set
    var seriesItems by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    private var allSeriesCache: List<MediaItem> = emptyList()

    // Deduplicated category names (prevents Compose duplicate-key crashes)
    val liveCatNames: List<String> by derivedStateOf {
        (listOf("All", "Favorites") + liveCategories.map { it.name }).distinct()
    }
    val vodCatNames: List<String> by derivedStateOf {
        (listOf("All", "Favorites") + vodCategories.map { it.name }).distinct()
    }
    val seriesCatNames: List<String> by derivedStateOf {
        (listOf("All", "Favorites") + seriesCategories.map { it.name }).distinct()
    }

    // --- Library & Resume Progress ---
    var favorites by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var recents by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    val favoriteIds: Set<String> by derivedStateOf { favorites.map { it.id }.toSet() }
    var playbackProgress by mutableStateOf<Map<String, PlaybackProgress>>(emptyMap())
        private set
    var downloadedFiles by mutableStateOf<List<DownloadedMediaFile>>(emptyList())
        private set

    // --- Memory caches for details & episodes ---
    private val detailMemoryCache = mutableStateMapOf<String, MediaDetailInfo>()
    private val episodesMemoryCache = mutableStateMapOf<String, List<EpisodeItem>>()

    // --- Overlay / Navigation state (survives config changes) ---
    var activeLibraryPage by mutableStateOf<String?>(null)
    var lastLibraryPage by mutableStateOf<String?>(null)

    var selectedDetailItem by mutableStateOf<MediaItem?>(null)
        private set
    var lastRenderedDetailItem by mutableStateOf<MediaItem?>(null)
        private set
    var detailInfo by mutableStateOf(MediaDetailInfo())
        private set
    var detailEpisodes by mutableStateOf<List<EpisodeItem>>(emptyList())
        private set
    var loadingDetail by mutableStateOf(false)
        private set

    var activePlayerItem by mutableStateOf<MediaItem?>(null)
        private set
    var isPlayerMini by mutableStateOf(false)
    var isPlayerActivelyPlaying by mutableStateOf(false)
    var videoAspectWidth by mutableIntStateOf(16)
    var videoAspectHeight by mutableIntStateOf(9)
    var playerVodDetailInfo by mutableStateOf(MediaDetailInfo())
        private set
    var playerVodEpisodes by mutableStateOf<List<EpisodeItem>>(emptyList())
        private set
    var playerSeriesParent by mutableStateOf<MediaItem?>(null)
        private set

    // --- Cancellable per-domain jobs ---
    private var liveJob: Job? = null
    private var vodJob: Job? = null
    private var seriesJob: Job? = null
    private var m3uJob: Job? = null
    private var detailJob: Job? = null
    private var playerDetailJob: Job? = null
    private var statusClearJob: Job? = null

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                refreshDownloadedFiles()
            }
        }
    }

    init {
        try {
            ContextCompat.registerReceiver(
                appContext,
                downloadCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {
        }

        // Load persisted preferences off the Main Thread
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val rawServers = SecurePrefsHelper.decrypt(prefs.getString("servers_json", null))
                val decodedServers = ServerStore.decode(rawServers).ifEmpty {
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
                val savedActiveId = prefs.getString("active_server_id", null)
                    ?.takeIf { id -> decodedServers.any { it.id == id } }
                    ?: decodedServers.firstOrNull()?.id
                    ?: ""
                val favs = XtreamClient.decodeItems(prefs.getString("favorites_json", null))
                val recs = XtreamClient.decodeItems(prefs.getString("recents_json", null))
                val prog = PlaybackProgressStore.decode(prefs.getString("playback_progress_json", null))

                // Silently upgrade legacy plaintext servers_json to encrypted Keystore storage
                val storedRaw = prefs.getString("servers_json", null)
                if (decodedServers.isNotEmpty() && (storedRaw == null || !storedRaw.startsWith("enc_v1:"))) {
                    prefs.edit()
                        .putString("servers_json", SecurePrefsHelper.encrypt(ServerStore.encode(decodedServers)))
                        .remove("host").remove("user").remove("pass").remove("m3u_url").remove("mode")
                        .apply()
                }
                InitialLoadBundle(decodedServers, savedActiveId, favs, recs, prog)
            }

            servers = loaded.servers
            activeServerId = loaded.activeId
            favorites = loaded.favorites
            recents = loaded.recents
            playbackProgress = loaded.progress

            if (loaded.activeId.isNotBlank()) {
                onServerActivated(loaded.activeId)
            }
            refreshDownloadedFiles()
        }
    }

    private data class InitialLoadBundle(
        val servers: List<SavedServer>,
        val activeId: String,
        val favorites: List<MediaItem>,
        val recents: List<MediaItem>,
        val progress: Map<String, PlaybackProgress>
    )

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(downloadCompleteReceiver)
        } catch (_: Exception) {
        }
    }

    fun showStatus(message: String?, autoDismissMs: Long = 3800L) {
        statusClearJob?.cancel()
        statusMessage = message
        if (message != null && autoDismissMs > 0L) {
            statusClearJob = viewModelScope.launch {
                delay(autoDismissMs)
                if (statusMessage == message) {
                    statusMessage = null
                }
            }
        }
    }

    fun dismissStatus() {
        statusClearJob?.cancel()
        statusMessage = null
    }

    fun setThemeMode(mode: AppThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setDownloadMode(mode: DownloadMode) {
        downloadMode = mode
        prefs.edit().putString("download_mode", mode.name).apply()
    }

    fun addXtreamServer(name: String, host: String, user: String, pass: String) {
        val newSrv = SavedServer(
            id = UUID.randomUUID().toString(),
            name = name,
            mode = "xtream",
            host = XtreamClient.normalizeHost(host),
            user = user.trim(),
            pass = pass.trim()
        )
        persistServers(servers + newSrv, newSrv.id)
    }

    fun addM3uServer(name: String, url: String) {
        val newSrv = SavedServer(
            id = UUID.randomUUID().toString(),
            name = name,
            mode = "m3u",
            m3uUrl = XtreamClient.normalizeHost(url)
        )
        persistServers(servers + newSrv, newSrv.id)
    }

    fun selectServer(srv: SavedServer) {
        if (srv.id == activeServerId) return
        persistServers(servers, srv.id)
    }

    fun deleteServer(srv: SavedServer) {
        val remaining = servers.filterNot { it.id == srv.id }
        val nextId = if (srv.id == activeServerId) remaining.firstOrNull()?.id ?: "" else activeServerId
        persistServers(remaining, nextId)
        viewModelScope.launch {
            PlaylistDiskCache.clearForServer(appContext, srv.id)
        }
    }

    private fun persistServers(updated: List<SavedServer>, newActiveId: String) {
        val changedActive = newActiveId != activeServerId
        servers = updated
        activeServerId = newActiveId
        viewModelScope.launch(Dispatchers.IO) {
            val encoded = SecurePrefsHelper.encrypt(ServerStore.encode(updated))
            prefs.edit()
                .putString("servers_json", encoded)
                .putString("active_server_id", newActiveId)
                .apply()
        }
        if (changedActive) {
            onServerActivated(newActiveId)
        }
    }

    private fun lastCatPrefKey(serverId: String, kind: String): String = "last_${kind}_cat_$serverId"

    private fun onServerActivated(serverId: String) {
        cancelAllCatalogJobs()
        resetActiveCatalogState()
        selectedLiveCat = prefs.getString(lastCatPrefKey(serverId, "live"), "") ?: ""
        selectedVodCat = prefs.getString(lastCatPrefKey(serverId, "vod"), "") ?: ""
        selectedSeriesCat = prefs.getString(lastCatPrefKey(serverId, "series"), "") ?: ""

        val srv = servers.firstOrNull { it.id == serverId } ?: return
        if (srv.mode == "xtream" && srv.host.isNotBlank()) {
            ensureLiveCategories(forceNetwork = false)
        } else if (srv.mode == "m3u" && srv.m3uUrl.isNotBlank()) {
            loadM3uForServer(srv, forceRefresh = false)
        }
    }

    private fun cancelAllCatalogJobs() {
        liveJob?.cancel()
        vodJob?.cancel()
        seriesJob?.cancel()
        m3uJob?.cancel()
        detailJob?.cancel()
        playerDetailJob?.cancel()
        isLoadingLive = false
        isLoadingVod = false
        isLoadingSeries = false
        loadingDetail = false
    }

    private fun resetActiveCatalogState() {
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

    fun refreshActivePlaylist() {
        val srv = activeServer ?: return
        cancelAllCatalogJobs()
        viewModelScope.launch {
            PlaylistDiskCache.clearForServer(appContext, srv.id)
            resetActiveCatalogState()
            if (srv.mode == "xtream") {
                ensureLiveCategories(forceNetwork = true)
            } else {
                loadM3uForServer(srv, forceRefresh = true)
            }
        }
    }

    fun refreshDownloadedFiles() {
        viewModelScope.launch {
            downloadedFiles = MediaDownloadHelper.listDownloadedFilesAsync(appContext)
        }
    }

    fun deleteDownloadedFile(entry: DownloadedMediaFile) {
        viewModelScope.launch {
            val ok = MediaDownloadHelper.deleteDownloadedFile(appContext, entry)
            if (!ok) {
                showStatus("Unable to delete ${entry.displayName}")
            }
            downloadedFiles = MediaDownloadHelper.listDownloadedFilesAsync(appContext)
        }
    }

    fun toggleFavorite(item: MediaItem) {
        val updated = if (favorites.any { it.id == item.id }) {
            favorites.filterNot { it.id == item.id }
        } else {
            listOf(item) + favorites
        }
        favorites = updated
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putString("favorites_json", XtreamClient.encodeItems(updated)).apply()
        }
    }

    fun recordRecent(item: MediaItem) {
        val updated = (listOf(item) + recents.filterNot { it.id == item.id }).take(20)
        recents = updated
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putString("recents_json", XtreamClient.encodeItems(updated)).apply()
        }
    }

    fun removeRecent(item: MediaItem) {
        val updated = recents.filterNot { it.id == item.id }
        recents = updated
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putString("recents_json", XtreamClient.encodeItems(updated)).apply()
        }
    }

    fun clearRecents() {
        recents = emptyList()
        prefs.edit().remove("recents_json").apply()
    }

    fun getResumePositionMs(mediaId: String): Long {
        return PlaybackProgressStore.getResumePositionMs(playbackProgress, mediaId)
    }

    fun savePlaybackProgress(mediaId: String, positionMs: Long, durationMs: Long) {
        val updated = PlaybackProgressStore.upsert(playbackProgress, mediaId, positionMs, durationMs)
        if (updated === playbackProgress) return
        playbackProgress = updated
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putString("playback_progress_json", PlaybackProgressStore.encode(updated)).apply()
        }
    }

    fun clearAllData() {
        cancelAllCatalogJobs()
        prefs.edit().clear().apply()
        servers = emptyList()
        activeServerId = ""
        selectedLiveCat = ""
        selectedVodCat = ""
        selectedSeriesCat = ""
        liveSearchQuery = ""
        vodSearchQuery = ""
        seriesSearchQuery = ""
        resetActiveCatalogState()
        favorites = emptyList()
        recents = emptyList()
        playbackProgress = emptyMap()
        selectedDetailItem = null
        lastRenderedDetailItem = null
        activeLibraryPage = null
        lastLibraryPage = null
        closePlayer()
        viewModelScope.launch { PlaylistDiskCache.clearAll(appContext) }
        showStatus("Cleared all saved data")
    }

    // --- Detail & Player actions ---
    fun closeDetail() {
        detailJob?.cancel()
        selectedDetailItem = null
        loadingDetail = false
    }

    fun closePlayer() {
        playerDetailJob?.cancel()
        activePlayerItem = null
        isPlayerMini = false
        isPlayerActivelyPlaying = false
        playerVodDetailInfo = MediaDetailInfo()
        playerVodEpisodes = emptyList()
        playerSeriesParent = null
        videoAspectWidth = 16
        videoAspectHeight = 9
    }

    fun playItem(
        item: MediaItem,
        info: MediaDetailInfo? = null,
        episodes: List<EpisodeItem> = emptyList(),
        seriesParent: MediaItem? = null
    ) {
        val resolvedUrl = XtreamClient.resolveStreamUrl(item, activeServer)
        val playable = if (resolvedUrl != item.streamUrl) item.copy(streamUrl = resolvedUrl) else item
        recordRecent(playable)
        activePlayerItem = playable
        isPlayerMini = false
        playerDetailJob?.cancel()

        if (playable.contentType == ContentType.LIVE) {
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
                fetchMovieDetailForPlayerIfNeeded(playable)
            }
        }
    }

    fun switchPlayerItem(next: MediaItem) {
        val resolvedUrl = XtreamClient.resolveStreamUrl(next, activeServer)
        val playable = if (resolvedUrl != next.streamUrl) next.copy(streamUrl = resolvedUrl) else next
        if (playable.contentType == ContentType.LIVE) {
            playerDetailJob?.cancel()
            recordRecent(playable)
            activePlayerItem = playable
            playerVodDetailInfo = MediaDetailInfo()
            playerVodEpisodes = emptyList()
            playerSeriesParent = null
        } else {
            playItem(playable)
        }
    }

    fun openFromRecents(item: MediaItem) {
        when {
            item.contentType == ContentType.LIVE -> playItem(item)
            item.id.startsWith("ep_") || item.id.startsWith("offline_") -> playItem(item)
            else -> openDetail(item)
        }
    }

    private fun fetchMovieDetailForPlayerIfNeeded(movie: MediaItem) {
        val srv = activeServer ?: return
        val scopeId = srv.id
        val cacheKey = "${scopeId}_${movie.id}"
        val mem = detailMemoryCache[cacheKey]
        if (mem != null) {
            playerVodDetailInfo = mem
            return
        }
        if (srv.mode != "xtream" || srv.host.isBlank()) return
        val vodId = movie.id.removePrefix("vod_")
        if (vodId.isBlank() || vodId == movie.id) return

        playerDetailJob?.cancel()
        playerDetailJob = viewModelScope.launch {
            val disk = PlaylistDiskCache.loadDetailInfo(appContext, cacheKey)
            if (disk != null) {
                detailMemoryCache[cacheKey] = disk
                if (activePlayerItem?.id == movie.id) playerVodDetailInfo = disk
                return@launch
            }
            try {
                val url = XtreamClient.buildApiUrl(
                    srv.host, srv.user, srv.pass, "get_vod_info", mapOf("vod_id" to vodId)
                )
                val json = XtreamClient.fetchText(url)
                val parsed = withContext(Dispatchers.Default) { XtreamClient.parseVodDetail(json) }
                detailMemoryCache[cacheKey] = parsed
                PlaylistDiskCache.saveDetailInfo(appContext, cacheKey, parsed)
                if (activePlayerItem?.id == movie.id) playerVodDetailInfo = parsed
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun openDetail(item: MediaItem) {
        detailJob?.cancel()
        if (item.id.startsWith("ep_") || item.id.startsWith("offline_")) {
            playItem(item)
            return
        }

        selectedDetailItem = item
        lastRenderedDetailItem = item
        val srv = activeServer
        val scopeId = srv?.id ?: "none"
        val cacheKey = "${scopeId}_${item.id}"

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
        if (srv == null || srv.mode != "xtream" || srv.host.isBlank()) {
            loadingDetail = false
            return
        }

        when (item.contentType) {
            ContentType.MOVIE -> {
                val vodId = item.id.removePrefix("vod_")
                if (vodId.isBlank() || vodId == item.id) {
                    loadingDetail = false
                    return
                }
                detailJob = viewModelScope.launch {
                    val diskInfo = PlaylistDiskCache.loadDetailInfo(appContext, cacheKey)
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
                        val url = XtreamClient.buildApiUrl(
                            srv.host, srv.user, srv.pass, "get_vod_info", mapOf("vod_id" to vodId)
                        )
                        val json = XtreamClient.fetchText(url)
                        val parsed = withContext(Dispatchers.Default) { XtreamClient.parseVodDetail(json) }
                        detailMemoryCache[cacheKey] = parsed
                        PlaylistDiskCache.saveDetailInfo(appContext, cacheKey, parsed)
                        if (selectedDetailItem?.id == item.id) detailInfo = parsed
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    } finally {
                        if (selectedDetailItem?.id == item.id) loadingDetail = false
                    }
                }
            }
            ContentType.SERIES -> {
                val sid = item.seriesId
                if (sid.isNullOrBlank()) {
                    loadingDetail = false
                    return
                }
                detailJob = viewModelScope.launch {
                    val diskInfo = PlaylistDiskCache.loadDetailInfo(appContext, cacheKey)
                    val diskEps = PlaylistDiskCache.loadEpisodes(appContext, cacheKey)
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
                        val url = XtreamClient.buildApiUrl(
                            srv.host, srv.user, srv.pass, "get_series_info", mapOf("series_id" to sid)
                        )
                        val json = XtreamClient.fetchText(url)
                        val parsedInfo = withContext(Dispatchers.Default) { XtreamClient.parseSeriesDetail(json) }
                        val parsedEps = withContext(Dispatchers.Default) {
                            XtreamClient.parseSeriesEpisodes(json, srv.host, srv.user, srv.pass)
                        }
                        detailMemoryCache[cacheKey] = parsedInfo
                        episodesMemoryCache[cacheKey] = parsedEps
                        PlaylistDiskCache.saveDetailInfo(appContext, cacheKey, parsedInfo)
                        PlaylistDiskCache.saveEpisodes(appContext, cacheKey, parsedEps)
                        if (selectedDetailItem?.id == item.id) {
                            detailInfo = parsedInfo
                            detailEpisodes = parsedEps
                        }
                    } catch (e: CancellationException) {
                        throw e
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

    // --- Catalog Loading (with strict Job cancellation & server/category freshness guards) ---
    fun loadLiveCategory(catName: String, forceNetwork: Boolean = false) {
        val srv = activeServer ?: return
        selectedLiveCat = catName
        prefs.edit().putString(lastCatPrefKey(srv.id, "live"), catName).apply()
        if (catName == "Favorites") return

        if (srv.mode == "m3u") {
            liveItems = if (catName == "All") m3uAllItems else m3uAllItems.filter { it.group == catName }
            return
        }

        val scopeId = srv.id
        val itemCacheKey = "${scopeId}_live_$catName"

        if (catName == "All") {
            if (!forceNetwork && allLiveCache.isNotEmpty()) {
                liveItems = allLiveCache
                return
            }
            liveJob?.cancel()
            liveJob = viewModelScope.launch {
                if (!forceNetwork) {
                    val diskItems = PlaylistDiskCache.loadItems(appContext, itemCacheKey)
                    if (diskItems.isNotEmpty() && activeServerId == scopeId && selectedLiveCat == catName) {
                        allLiveCache = diskItems
                        liveItems = diskItems
                        return@launch
                    }
                }
                isLoadingLive = true
                showStatus("Loading all live channels...", autoDismissMs = 0L)
                try {
                    val catMap = liveCategories.associate { it.id to it.name }
                    val url = XtreamClient.buildApiUrl(srv.host, srv.user, srv.pass, "get_live_streams")
                    val parsed = XtreamClient.fetchLiveStreams(url, srv.host, srv.user, srv.pass, "Live", catMap)
                    if (activeServerId == scopeId) {
                        allLiveCache = parsed
                        if (selectedLiveCat == catName) liveItems = parsed
                        PlaylistDiskCache.saveItems(appContext, itemCacheKey, parsed)
                        dismissStatus()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (activeServerId == scopeId) showStatus("Live error: ${e.message}")
                } finally {
                    if (activeServerId == scopeId) isLoadingLive = false
                }
            }
            return
        }

        val cat = liveCategories.firstOrNull { it.name == catName } ?: return
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            if (!forceNetwork) {
                val diskItems = PlaylistDiskCache.loadItems(appContext, itemCacheKey)
                if (diskItems.isNotEmpty() && activeServerId == scopeId && selectedLiveCat == catName) {
                    liveItems = diskItems
                    return@launch
                }
            }
            isLoadingLive = true
            showStatus("Loading $catName...", autoDismissMs = 0L)
            try {
                val url = XtreamClient.buildApiUrl(
                    srv.host, srv.user, srv.pass, "get_live_streams", mapOf("category_id" to cat.id)
                )
                val parsed = XtreamClient.fetchLiveStreams(url, srv.host, srv.user, srv.pass, cat.name)
                if (activeServerId == scopeId) {
                    if (selectedLiveCat == catName) liveItems = parsed
                    PlaylistDiskCache.saveItems(appContext, itemCacheKey, parsed)
                    dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) showStatus("Live error: ${e.message}")
            } finally {
                if (activeServerId == scopeId) isLoadingLive = false
            }
        }
    }

    fun loadVodCategory(catName: String, forceNetwork: Boolean = false) {
        val srv = activeServer ?: return
        selectedVodCat = catName
        prefs.edit().putString(lastCatPrefKey(srv.id, "vod"), catName).apply()
        if (catName == "Favorites" || srv.mode != "xtream") return

        val scopeId = srv.id
        val itemCacheKey = "${scopeId}_vod_$catName"

        if (catName == "All") {
            if (!forceNetwork && allVodCache.isNotEmpty()) {
                vodItems = allVodCache
                return
            }
            vodJob?.cancel()
            vodJob = viewModelScope.launch {
                if (!forceNetwork) {
                    val diskItems = PlaylistDiskCache.loadItems(appContext, itemCacheKey)
                    if (diskItems.isNotEmpty() && activeServerId == scopeId && selectedVodCat == catName) {
                        allVodCache = diskItems
                        vodItems = diskItems
                        return@launch
                    }
                }
                isLoadingVod = true
                showStatus("Loading all movies...", autoDismissMs = 0L)
                try {
                    val catMap = vodCategories.associate { it.id to it.name }
                    val url = XtreamClient.buildApiUrl(srv.host, srv.user, srv.pass, "get_vod_streams")
                    val parsed = XtreamClient.fetchVodStreams(url, srv.host, srv.user, srv.pass, "Movies", catMap)
                    if (activeServerId == scopeId) {
                        allVodCache = parsed
                        if (selectedVodCat == catName) vodItems = parsed
                        PlaylistDiskCache.saveItems(appContext, itemCacheKey, parsed)
                        dismissStatus()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (activeServerId == scopeId) showStatus("Movies error: ${e.message}")
                } finally {
                    if (activeServerId == scopeId) isLoadingVod = false
                }
            }
            return
        }

        val cat = vodCategories.firstOrNull { it.name == catName } ?: return
        vodJob?.cancel()
        vodJob = viewModelScope.launch {
            if (!forceNetwork) {
                val diskItems = PlaylistDiskCache.loadItems(appContext, itemCacheKey)
                if (diskItems.isNotEmpty() && activeServerId == scopeId && selectedVodCat == catName) {
                    vodItems = diskItems
                    return@launch
                }
            }
            isLoadingVod = true
            showStatus("Loading $catName...", autoDismissMs = 0L)
            try {
                val url = XtreamClient.buildApiUrl(
                    srv.host, srv.user, srv.pass, "get_vod_streams", mapOf("category_id" to cat.id)
                )
                val parsed = XtreamClient.fetchVodStreams(url, srv.host, srv.user, srv.pass, cat.name)
                if (activeServerId == scopeId) {
                    if (selectedVodCat == catName) vodItems = parsed
                    PlaylistDiskCache.saveItems(appContext, itemCacheKey, parsed)
                    dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) showStatus("Movies error: ${e.message}")
            } finally {
                if (activeServerId == scopeId) isLoadingVod = false
            }
        }
    }

    fun loadSeriesCategory(catName: String, forceNetwork: Boolean = false) {
        val srv = activeServer ?: return
        selectedSeriesCat = catName
        prefs.edit().putString(lastCatPrefKey(srv.id, "series"), catName).apply()
        if (catName == "Favorites" || srv.mode != "xtream") return

        val scopeId = srv.id
        val itemCacheKey = "${scopeId}_series_$catName"

        if (catName == "All") {
            if (!forceNetwork && allSeriesCache.isNotEmpty()) {
                seriesItems = allSeriesCache
                return
            }
            seriesJob?.cancel()
            seriesJob = viewModelScope.launch {
                if (!forceNetwork) {
                    val diskItems = PlaylistDiskCache.loadItems(appContext, itemCacheKey)
                    if (diskItems.isNotEmpty() && activeServerId == scopeId && selectedSeriesCat == catName) {
                        allSeriesCache = diskItems
                        seriesItems = diskItems
                        return@launch
                    }
                }
                isLoadingSeries = true
                showStatus("Loading all series...", autoDismissMs = 0L)
                try {
                    val catMap = seriesCategories.associate { it.id to it.name }
                    val url = XtreamClient.buildApiUrl(srv.host, srv.user, srv.pass, "get_series")
                    val parsed = XtreamClient.fetchSeries(url, "Series", catMap)
                    if (activeServerId == scopeId) {
                        allSeriesCache = parsed
                        if (selectedSeriesCat == catName) seriesItems = parsed
                        PlaylistDiskCache.saveItems(appContext, itemCacheKey, parsed)
                        dismissStatus()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (activeServerId == scopeId) showStatus("Series error: ${e.message}")
                } finally {
                    if (activeServerId == scopeId) isLoadingSeries = false
                }
            }
            return
        }

        val cat = seriesCategories.firstOrNull { it.name == catName } ?: return
        seriesJob?.cancel()
        seriesJob = viewModelScope.launch {
            if (!forceNetwork) {
                val diskItems = PlaylistDiskCache.loadItems(appContext, itemCacheKey)
                if (diskItems.isNotEmpty() && activeServerId == scopeId && selectedSeriesCat == catName) {
                    seriesItems = diskItems
                    return@launch
                }
            }
            isLoadingSeries = true
            showStatus("Loading $catName...", autoDismissMs = 0L)
            try {
                val url = XtreamClient.buildApiUrl(
                    srv.host, srv.user, srv.pass, "get_series", mapOf("category_id" to cat.id)
                )
                val parsed = XtreamClient.fetchSeries(url, cat.name)
                if (activeServerId == scopeId) {
                    if (selectedSeriesCat == catName) seriesItems = parsed
                    PlaylistDiskCache.saveItems(appContext, itemCacheKey, parsed)
                    dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) showStatus("Series error: ${e.message}")
            } finally {
                if (activeServerId == scopeId) isLoadingSeries = false
            }
        }
    }

    fun ensureLiveCategories(forceNetwork: Boolean = false) {
        val srv = activeServer ?: return
        if (srv.mode != "xtream" || (!forceNetwork && (liveCategories.isNotEmpty() || liveJob?.isActive == true))) return
        val scopeId = srv.id
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            if (!forceNetwork) {
                val cachedCats = PlaylistDiskCache.loadCategories(appContext, "${scopeId}_live")
                if (cachedCats.isNotEmpty() && activeServerId == scopeId) {
                    liveCategories = cachedCats
                    val targetCat = selectedLiveCat.takeIf {
                        it.isNotBlank() && (it == "All" || it == "Favorites" || cachedCats.any { c -> c.name == it })
                    } ?: cachedCats.first().name
                    loadLiveCategory(targetCat, forceNetwork = false)
                    return@launch
                }
            }
            isLoadingLive = true
            showStatus("Loading live categories...", autoDismissMs = 0L)
            try {
                val url = XtreamClient.buildApiUrl(srv.host, srv.user, srv.pass, "get_live_categories")
                val json = XtreamClient.fetchText(url)
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                if (activeServerId == scopeId) {
                    liveCategories = cats
                    PlaylistDiskCache.saveCategories(appContext, "${scopeId}_live", cats)
                    isLoadingLive = false
                    cats.firstOrNull()?.let { loadLiveCategory(it.name, forceNetwork = forceNetwork) }
                        ?: dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) {
                    isLoadingLive = false
                    showStatus("Live error: ${e.message}")
                }
            }
        }
    }

    fun ensureVodCategories(forceNetwork: Boolean = false) {
        val srv = activeServer ?: return
        if (srv.mode != "xtream" || (!forceNetwork && (vodCategories.isNotEmpty() || vodJob?.isActive == true))) return
        val scopeId = srv.id
        vodJob?.cancel()
        vodJob = viewModelScope.launch {
            if (!forceNetwork) {
                val cachedCats = PlaylistDiskCache.loadCategories(appContext, "${scopeId}_vod")
                if (cachedCats.isNotEmpty() && activeServerId == scopeId) {
                    vodCategories = cachedCats
                    val targetCat = selectedVodCat.takeIf {
                        it.isNotBlank() && (it == "All" || it == "Favorites" || cachedCats.any { c -> c.name == it })
                    } ?: cachedCats.first().name
                    loadVodCategory(targetCat, forceNetwork = false)
                    return@launch
                }
            }
            isLoadingVod = true
            showStatus("Loading movie categories...", autoDismissMs = 0L)
            try {
                val url = XtreamClient.buildApiUrl(srv.host, srv.user, srv.pass, "get_vod_categories")
                val json = XtreamClient.fetchText(url)
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                if (activeServerId == scopeId) {
                    vodCategories = cats
                    PlaylistDiskCache.saveCategories(appContext, "${scopeId}_vod", cats)
                    isLoadingVod = false
                    cats.firstOrNull()?.let { loadVodCategory(it.name, forceNetwork = forceNetwork) }
                        ?: dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) {
                    isLoadingVod = false
                    showStatus("Movies error: ${e.message}")
                }
            }
        }
    }

    fun ensureSeriesCategories(forceNetwork: Boolean = false) {
        val srv = activeServer ?: return
        if (srv.mode != "xtream" || (!forceNetwork && (seriesCategories.isNotEmpty() || seriesJob?.isActive == true))) return
        val scopeId = srv.id
        seriesJob?.cancel()
        seriesJob = viewModelScope.launch {
            if (!forceNetwork) {
                val cachedCats = PlaylistDiskCache.loadCategories(appContext, "${scopeId}_series")
                if (cachedCats.isNotEmpty() && activeServerId == scopeId) {
                    seriesCategories = cachedCats
                    val targetCat = selectedSeriesCat.takeIf {
                        it.isNotBlank() && (it == "All" || it == "Favorites" || cachedCats.any { c -> c.name == it })
                    } ?: cachedCats.first().name
                    loadSeriesCategory(targetCat, forceNetwork = false)
                    return@launch
                }
            }
            isLoadingSeries = true
            showStatus("Loading series categories...", autoDismissMs = 0L)
            try {
                val url = XtreamClient.buildApiUrl(srv.host, srv.user, srv.pass, "get_series_categories")
                val json = XtreamClient.fetchText(url)
                val cats = withContext(Dispatchers.Default) { XtreamClient.parseCategories(json) }
                if (activeServerId == scopeId) {
                    seriesCategories = cats
                    PlaylistDiskCache.saveCategories(appContext, "${scopeId}_series", cats)
                    isLoadingSeries = false
                    cats.firstOrNull()?.let { loadSeriesCategory(it.name, forceNetwork = forceNetwork) }
                        ?: dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) {
                    isLoadingSeries = false
                    showStatus("Series error: ${e.message}")
                }
            }
        }
    }

    fun loadM3uForServer(srv: SavedServer, forceRefresh: Boolean) {
        val scopeId = srv.id
        val m3uCacheKey = "${scopeId}_m3u_all"
        m3uJob?.cancel()
        m3uJob = viewModelScope.launch {
            if (!forceRefresh) {
                val cached = PlaylistDiskCache.loadItems(appContext, m3uCacheKey)
                if (cached.isNotEmpty() && activeServerId == scopeId) {
                    m3uAllItems = cached
                    val groups = cached.map { it.group }.distinct().sorted()
                    liveCategories = groups.map { CategoryItem(it, it) }
                    val targetCat = selectedLiveCat.takeIf {
                        it.isNotBlank() && (it == "All" || it == "Favorites" || groups.contains(it))
                    } ?: "All"
                    selectedLiveCat = targetCat
                    liveItems = if (targetCat == "All" || targetCat == "Favorites") cached else cached.filter { it.group == targetCat }
                    dismissStatus()
                    return@launch
                }
            }
            isLoadingLive = true
            showStatus("Loading ${srv.name}...", autoDismissMs = 0L)
            try {
                val parsed = XtreamClient.fetchAndParseM3u(srv.m3uUrl)
                if (activeServerId == scopeId) {
                    m3uAllItems = parsed
                    PlaylistDiskCache.saveItems(appContext, m3uCacheKey, parsed)
                    val groups = parsed.map { it.group }.distinct().sorted()
                    liveCategories = groups.map { CategoryItem(it, it) }
                    selectedLiveCat = "All"
                    liveItems = parsed
                    dismissStatus()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeServerId == scopeId) showStatus("M3U error: ${e.message}")
            } finally {
                if (activeServerId == scopeId) isLoadingLive = false
            }
        }
    }
}
