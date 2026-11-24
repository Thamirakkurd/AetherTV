package com.aethertv.v2

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Display
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
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
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.DownloadMode
import com.aethertv.v2.data.MediaDownloadHelper
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.ui.components.AddPlaylistDialog
import com.aethertv.v2.ui.components.FloatingGlassNavBar
import com.aethertv.v2.ui.components.MediaActionBottomSheet
import com.aethertv.v2.ui.components.NavTabItem
import com.aethertv.v2.ui.screens.HomeScreen
import com.aethertv.v2.ui.screens.LibraryCollectionScreen
import com.aethertv.v2.ui.screens.LiveTvScreen
import com.aethertv.v2.ui.screens.MediaDetailScreen
import com.aethertv.v2.ui.screens.MediaGridScreen
import com.aethertv.v2.ui.screens.RecentItemActionSheet
import com.aethertv.v2.ui.screens.SettingsScreen
import com.aethertv.v2.ui.screens.UnifiedMorphPlayerOverlay
import com.aethertv.v2.ui.theme.AppThemeMode
import com.aethertv.v2.ui.theme.MatteGlassTheme
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val viewModel: AetherViewModel by viewModels()
    private val isInPipModeState = mutableStateOf(false)

    private fun buildPipParams(): PictureInPictureParams {
        val rawW = viewModel.videoAspectWidth.coerceAtLeast(1)
        val rawH = viewModel.videoAspectHeight.coerceAtLeast(1)
        val ratio = rawW.toFloat() / rawH.toFloat()
        // Android requires PiP aspect ratio strictly within [0.41841, 2.39]
        val safeRational = when {
            ratio < 0.45f -> Rational(9, 20)
            ratio > 2.35f -> Rational(21, 9)
            else -> Rational(rawW, rawH)
        }
        val builder = PictureInPictureParams.Builder().setAspectRatio(safeRational)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(false)
        }
        return builder.build()
    }

    private fun tryEnterPip(showToastOnFail: Boolean) {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            if (showToastOnFail) {
                Toast.makeText(this, "PiP unavailable on this device", Toast.LENGTH_SHORT).show()
            }
            return
        }
        try {
            enterPictureInPictureMode(buildPipParams())
        } catch (_: Exception) {
            if (showToastOnFail) {
                Toast.makeText(this, "PiP unavailable on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock window to display's highest refresh rate WITHOUT downgrading native resolution
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
        val currentMode = display?.mode
        val maxMode = display?.supportedModes
            ?.filter { mode ->
                currentMode == null ||
                    (mode.physicalWidth == currentMode.physicalWidth &&
                        mode.physicalHeight == currentMode.physicalHeight)
            }
            ?.maxByOrNull { it.refreshRate }
        if (maxMode != null) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = maxMode.modeId
            }
        }

        // Sync palette before first composition so Dark mode launches with zero light-theme flash
        MatteGlassTokens.syncPalette(viewModel.themeMode == AppThemeMode.DARK)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        setContent {
            val themeMode = viewModel.themeMode
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
                    viewModel = viewModel,
                    appHazeState = appHazeState,
                    isInPipMode = isInPipModeState.value,
                    onEnterPip = { tryEnterPip(showToastOnFail = true) }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.activePlayerItem != null && !viewModel.isPlayerMini && viewModel.isPlayerActivelyPlaying) {
            tryEnterPip(showToastOnFail = false)
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
        val (shareUri, needsReadGrant) = MediaDownloadHelper.getShareableUri(context, url)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareUri, "video/*")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (needsReadGrant) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Play $title"))
    } catch (_: Exception) {
        Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AetherV2App(
    viewModel: AetherViewModel,
    appHazeState: HazeState,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val homeListState = rememberLazyListState()
    val liveListState = rememberLazyListState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()

    var showAddDialog by remember { mutableStateOf(false) }
    var barsVisible by remember { mutableStateOf(true) }
    var pendingDownloadTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var cardMenuTarget by remember { mutableStateOf<MediaItem?>(null) }
    var libraryRecentMenuTarget by remember { mutableStateOf<MediaItem?>(null) }

    // Dismiss floating modals automatically when entering PiP
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            cardMenuTarget = null
            libraryRecentMenuTarget = null
            pendingDownloadTarget = null
            showAddDialog = false
        }
    }

    val playerExpandAnim = remember { Animatable(0f) }
    val hasActivePlayer = viewModel.activePlayerItem != null

    LaunchedEffect(hasActivePlayer) {
        if (!hasActivePlayer) {
            playerExpandAnim.snapTo(0f)
        }
    }

    // Per-tab filtered lists (only recomputes for the specific tab whose query or source changed)
    val shownLive by produceState(
        initialValue = emptyList<MediaItem>(),
        viewModel.liveItems, viewModel.selectedLiveCat, viewModel.liveSearchQuery, viewModel.favorites
    ) {
        value = withContext(Dispatchers.Default) {
            val source = if (viewModel.selectedLiveCat == "Favorites") {
                viewModel.favorites.filter { it.contentType == ContentType.LIVE }
            } else {
                viewModel.liveItems
            }
            val q = viewModel.liveSearchQuery
            if (q.isBlank()) source else source.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    val shownVod by produceState(
        initialValue = emptyList<MediaItem>(),
        viewModel.vodItems, viewModel.selectedVodCat, viewModel.vodSearchQuery, viewModel.favorites
    ) {
        value = withContext(Dispatchers.Default) {
            val source = if (viewModel.selectedVodCat == "Favorites") {
                viewModel.favorites.filter { it.contentType == ContentType.MOVIE }
            } else {
                viewModel.vodItems
            }
            val q = viewModel.vodSearchQuery
            if (q.isBlank()) source else source.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    val shownSeries by produceState(
        initialValue = emptyList<MediaItem>(),
        viewModel.seriesItems, viewModel.selectedSeriesCat, viewModel.seriesSearchQuery, viewModel.favorites
    ) {
        value = withContext(Dispatchers.Default) {
            val source = if (viewModel.selectedSeriesCat == "Favorites") {
                viewModel.favorites.filter { it.contentType == ContentType.SERIES }
            } else {
                viewModel.seriesItems
            }
            val q = viewModel.seriesSearchQuery
            if (q.isBlank()) source else source.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    // Back-navigation flow
    BackHandler(
        enabled = cardMenuTarget != null ||
            (viewModel.activePlayerItem != null && !viewModel.isPlayerMini) ||
            viewModel.selectedDetailItem != null ||
            viewModel.activeLibraryPage != null ||
            selectedTab != 0
    ) {
        when {
            cardMenuTarget != null -> cardMenuTarget = null
            viewModel.activePlayerItem != null && !viewModel.isPlayerMini -> viewModel.isPlayerMini = true
            viewModel.selectedDetailItem != null -> viewModel.closeDetail()
            viewModel.activeLibraryPage != null -> viewModel.activeLibraryPage = null
            selectedTab != 0 -> {
                barsVisible = true
                scope.launch { pagerState.animateScrollToPage(0) }
            }
        }
    }

    fun triggerDownload(url: String, title: String) {
        when (viewModel.downloadMode) {
            DownloadMode.BUILTIN -> {
                MediaDownloadHelper.startBuiltInDownload(context, url, title)
            }
            DownloadMode.EXTERNAL -> {
                MediaDownloadHelper.startExternalDownload(context, url, title)
            }
            DownloadMode.ASK -> {
                pendingDownloadTarget = url to title
            }
        }
    }

    LaunchedEffect(selectedTab, viewModel.activeServerId) {
        barsVisible = true
        when (selectedTab) {
            1 -> viewModel.ensureVodCategories(forceNetwork = false)
            2 -> viewModel.ensureSeriesCategories(forceNetwork = false)
            3 -> viewModel.ensureLiveCategories(forceNetwork = false)
            4 -> viewModel.refreshDownloadedFiles()
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

    val miniPlayerExtraPadding = if (hasActivePlayer && viewModel.isPlayerMini) 74.dp else 0.dp
    val detailOpen = viewModel.selectedDetailItem != null

    val detailDepthState = animateFloatAsState(
        targetValue = if (detailOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 310f),
        label = "detailDepth"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
            .nestedScroll(hideBarsConnection)
    ) {
        // Main Swipeable Tab Pager — spatial depth & parallax deferred to graphicsLayer (zero root recomposition!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val dDepth = detailDepthState.value
                    val pDepth = if (hasActivePlayer) playerExpandAnim.value.coerceIn(0f, 1f) else 0f
                    val combined = maxOf(dDepth, pDepth)
                    val scale = 1f - 0.036f * combined
                    scaleX = scale
                    scaleY = scale
                    translationX = -dDepth * 52f
                }
        ) {
            // Single shared hazeSource on the pager container (eliminates multi-page duplicate blur capture!)
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !detailOpen && viewModel.activeLibraryPage == null && (!hasActivePlayer || viewModel.isPlayerMini),
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(appHazeState)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Read pager offset strictly in draw/layer phase (0 recompositions during swipe!)
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                            val s = 1f - absOffset * 0.045f
                            scaleX = s
                            scaleY = s
                            alpha = 1f - absOffset * 0.22f
                            translationX = pageOffset * -28f
                        }
                ) {
                    when (page) {
                        0 -> {
                            val movieAndSeriesFavs = remember(viewModel.favorites) {
                                viewModel.favorites.filter {
                                    it.contentType == ContentType.MOVIE || it.contentType == ContentType.SERIES
                                }
                            }
                            HomeScreen(
                                favoriteMedia = movieAndSeriesFavs,
                                recentlyWatched = viewModel.recents,
                                favoriteIds = viewModel.favoriteIds,
                                hasPlaylist = viewModel.playlistMode.isNotBlank(),
                                listState = homeListState,
                                extraBottomPadding = miniPlayerExtraPadding,
                                onItemClick = { viewModel.openDetail(it) },
                                onRecentClick = { viewModel.openFromRecents(it) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onPlayClick = {
                                    if (it.contentType == ContentType.SERIES) viewModel.openDetail(it)
                                    else viewModel.playItem(it)
                                },
                                onMoreClick = { cardMenuTarget = it },
                                onAddPlaylistClick = { showAddDialog = true },
                                onOpenFavoritesPage = {
                                    viewModel.lastLibraryPage = "favorites"
                                    viewModel.activeLibraryPage = "favorites"
                                },
                                onOpenRecentsPage = {
                                    viewModel.lastLibraryPage = "recents"
                                    viewModel.activeLibraryPage = "recents"
                                },
                                onRemoveRecent = { viewModel.removeRecent(it) },
                                onClearRecents = { viewModel.clearRecents() }
                            )
                        }
                        1 -> {
                            MediaGridScreen(
                                items = shownVod,
                                categories = viewModel.vodCatNames,
                                selectedCategory = viewModel.selectedVodCat,
                                favoriteIds = viewModel.favoriteIds,
                                isLoading = viewModel.isLoadingVod,
                                gridState = moviesGridState,
                                topBarVisible = barsVisible,
                                extraBottomPadding = miniPlayerExtraPadding,
                                emptyMessage = if (viewModel.playlistMode != "xtream") {
                                    "Add an Xtream Codes server for Movies"
                                } else {
                                    "No movies in this category"
                                },
                                searchQuery = viewModel.vodSearchQuery,
                                searchPlaceholder = "Search movies...",
                                onSearchQueryChange = { viewModel.vodSearchQuery = it },
                                onSelectCategory = { cat ->
                                    viewModel.loadVodCategory(cat, forceNetwork = false)
                                    scope.launch { moviesGridState.scrollToItem(0) }
                                },
                                onItemClick = { viewModel.openDetail(it) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onPlayClick = { viewModel.playItem(it) },
                                onMoreClick = { cardMenuTarget = it }
                            )
                        }
                        2 -> {
                            MediaGridScreen(
                                items = shownSeries,
                                categories = viewModel.seriesCatNames,
                                selectedCategory = viewModel.selectedSeriesCat,
                                favoriteIds = viewModel.favoriteIds,
                                isLoading = viewModel.isLoadingSeries,
                                gridState = seriesGridState,
                                topBarVisible = barsVisible,
                                extraBottomPadding = miniPlayerExtraPadding,
                                emptyMessage = if (viewModel.playlistMode != "xtream") {
                                    "Add an Xtream Codes server for Series"
                                } else {
                                    "No series in this category"
                                },
                                searchQuery = viewModel.seriesSearchQuery,
                                searchPlaceholder = "Search series...",
                                onSearchQueryChange = { viewModel.seriesSearchQuery = it },
                                onSelectCategory = { cat ->
                                    viewModel.loadSeriesCategory(cat, forceNetwork = false)
                                    scope.launch { seriesGridState.scrollToItem(0) }
                                },
                                onItemClick = { viewModel.openDetail(it) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onPlayClick = { viewModel.openDetail(it) },
                                onMoreClick = { cardMenuTarget = it }
                            )
                        }
                        3 -> {
                            LiveTvScreen(
                                items = shownLive,
                                categories = viewModel.liveCatNames,
                                selectedCategory = viewModel.selectedLiveCat,
                                favoriteIds = viewModel.favoriteIds,
                                isLoading = viewModel.isLoadingLive,
                                listState = liveListState,
                                topBarVisible = barsVisible,
                                extraBottomPadding = miniPlayerExtraPadding,
                                emptyMessage = if (viewModel.playlistMode.isBlank()) {
                                    "Add a server from Home or Settings to watch Live TV"
                                } else {
                                    "No live channels in this category"
                                },
                                searchQuery = viewModel.liveSearchQuery,
                                onSearchQueryChange = { viewModel.liveSearchQuery = it },
                                onSelectCategory = { cat ->
                                    viewModel.loadLiveCategory(cat, forceNetwork = false)
                                    scope.launch { liveListState.scrollToItem(0) }
                                },
                                onPlayClick = { viewModel.playItem(it) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onMoreClick = { cardMenuTarget = it }
                            )
                        }
                        4 -> {
                            SettingsScreen(
                                servers = viewModel.servers,
                                activeServerId = viewModel.activeServerId,
                                themeMode = viewModel.themeMode,
                                downloadMode = viewModel.downloadMode,
                                downloadedFiles = viewModel.downloadedFiles,
                                favoritesCount = viewModel.favorites.size,
                                recentCount = viewModel.recents.size,
                                extraBottomPadding = miniPlayerExtraPadding,
                                onChangeThemeMode = { viewModel.updateThemeMode(it) },
                                onSelectServer = { viewModel.selectServer(it) },
                                onDeleteServer = { viewModel.deleteServer(it) },
                                onAddServerClick = { showAddDialog = true },
                                onRefreshPlaylist = {
                                    if (viewModel.activeServer != null) {
                                        Toast.makeText(context, "Refreshing playlist cache...", Toast.LENGTH_SHORT).show()
                                        viewModel.refreshActivePlaylist()
                                    }
                                },
                                onChangeDownloadMode = { viewModel.updateDownloadMode(it) },
                                onPlayDownloadedFile = { entry ->
                                    val offlineItem = MediaItem(
                                        id = "offline_${entry.displayName}",
                                        name = entry.displayName,
                                        group = "Downloaded",
                                        posterUrl = null,
                                        streamUrl = entry.playUri.toString(),
                                        contentType = ContentType.MOVIE
                                    )
                                    viewModel.playItem(offlineItem)
                                },
                                onDeleteDownloadedFile = { viewModel.deleteDownloadedFile(it) },
                                onClearAll = { viewModel.clearAllData() }
                            )
                        }
                    }
                }
            }

            // Dismissible & auto-dismissing status pill
            viewModel.statusMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 74.dp + miniPlayerExtraPadding)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MatteGlassTokens.SheetBg)
                        .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                        .clickable { viewModel.dismissStatus() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (msg.contains("error", ignoreCase = true)) {
                            MatteGlassTokens.NotificationDot
                        } else {
                            MatteGlassTokens.CharcoalMuted
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = barsVisible && !detailOpen && viewModel.activeLibraryPage == null,
                enter = slideInVertically(spring(dampingRatio = 0.80f, stiffness = 360f)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(spring(dampingRatio = 0.86f, stiffness = 400f)) { it } + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingGlassNavBar(
                    items = tabs,
                    selectedIndex = selectedTab,
                    pagePositionProvider = {
                        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabs.size - 1).toFloat())
                    },
                    onSelect = { idx ->
                        viewModel.closeDetail()
                        viewModel.activeLibraryPage = null
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

            // Depth dim scrim drawn directly in draw phase (zero root recomposition!)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val pDepth = if (hasActivePlayer) playerExpandAnim.value.coerceIn(0f, 1f) else 0f
                        val combined = maxOf(detailDepthState.value, pDepth)
                        if (combined > 0.005f) {
                            drawRect(Color.Black.copy(alpha = 0.28f * combined))
                        }
                    }
            )
        }

        // Slide-from-right dedicated Favorites / Recently Watched page
        AnimatedVisibility(
            visible = viewModel.activeLibraryPage != null,
            enter = slideInHorizontally(spring(dampingRatio = 0.84f, stiffness = 310f)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(spring(dampingRatio = 0.86f, stiffness = 360f)) { it } + fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            val shownLibraryPage = viewModel.activeLibraryPage ?: viewModel.lastLibraryPage
            val isRecentsPage = shownLibraryPage == "recents"
            val pageItems = remember(shownLibraryPage, viewModel.favorites, viewModel.recents) {
                if (isRecentsPage) viewModel.recents
                else viewModel.favorites.filter {
                    it.contentType == ContentType.MOVIE || it.contentType == ContentType.SERIES
                }
            }
            LibraryCollectionScreen(
                title = if (isRecentsPage) "Recently Watched" else "Favorites",
                items = pageItems,
                favoriteIds = viewModel.favoriteIds,
                emptyMessage = if (isRecentsPage) {
                    "Items you watch will show up here for quick resume."
                } else {
                    "Tap ★ on any movie or series to add it to Favorites."
                },
                extraBottomPadding = miniPlayerExtraPadding,
                onBack = { viewModel.activeLibraryPage = null },
                onItemClick = { item ->
                    if (isRecentsPage) viewModel.openFromRecents(item)
                    else viewModel.openDetail(item)
                },
                onFavoriteClick = { viewModel.toggleFavorite(it) },
                onPlayClick = { item ->
                    if (item.contentType == ContentType.SERIES) viewModel.openDetail(item)
                    else viewModel.playItem(item)
                },
                onMoreClick = { cardMenuTarget = it },
                onItemLongClick = if (isRecentsPage) {
                    { item -> libraryRecentMenuTarget = item }
                } else null,
                onClearAllClick = if (isRecentsPage) {
                    { viewModel.clearRecents() }
                } else null
            )

            libraryRecentMenuTarget?.let { target ->
                RecentItemActionSheet(
                    item = target,
                    onDismiss = { libraryRecentMenuTarget = null },
                    onResume = {
                        libraryRecentMenuTarget = null
                        viewModel.openFromRecents(target)
                    },
                    onRemove = {
                        libraryRecentMenuTarget = null
                        viewModel.removeRecent(target)
                    },
                    onClearAll = {
                        libraryRecentMenuTarget = null
                        viewModel.clearRecents()
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
            val shownDetail = viewModel.selectedDetailItem ?: viewModel.lastRenderedDetailItem
            if (shownDetail != null) {
                MediaDetailScreen(
                    item = shownDetail,
                    detailInfo = viewModel.detailInfo,
                    episodes = viewModel.detailEpisodes,
                    isLoading = viewModel.loadingDetail,
                    isFavorite = viewModel.favoriteIds.contains(shownDetail.id),
                    onBack = { viewModel.closeDetail() },
                    onToggleFavorite = { viewModel.toggleFavorite(shownDetail) },
                    onWatchNow = {
                        if (shownDetail.contentType == ContentType.SERIES) {
                            val firstEp = viewModel.detailEpisodes.firstOrNull()
                            if (firstEp != null) {
                                val epMedia = MediaItem(
                                    id = "ep_${firstEp.id}",
                                    name = "${shownDetail.name} • ${firstEp.title}",
                                    group = firstEp.season,
                                    posterUrl = shownDetail.posterUrl,
                                    streamUrl = firstEp.streamUrl,
                                    contentType = ContentType.MOVIE,
                                    seriesId = shownDetail.seriesId,
                                    badge = firstEp.season
                                )
                                viewModel.playItem(epMedia, viewModel.detailInfo, viewModel.detailEpisodes, shownDetail)
                            } else {
                                Toast.makeText(context, "Loading episodes...", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.playItem(
                                item = shownDetail,
                                info = viewModel.detailInfo,
                                episodes = viewModel.detailEpisodes,
                                seriesParent = null
                            )
                        }
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
                            seriesId = shownDetail.seriesId,
                            badge = ep.season
                        )
                        viewModel.playItem(
                            item = epMedia,
                            info = viewModel.detailInfo,
                            episodes = viewModel.detailEpisodes,
                            seriesParent = shownDetail
                        )
                    },
                    onDownloadEpisode = { ep ->
                        triggerDownload(ep.streamUrl, "${shownDetail.name} - ${ep.title}")
                    }
                )
            }
        }

        // SINGLE PERSISTENT UNIFIED ZERO-FLASH MORPHING PLAYER
        viewModel.activePlayerItem?.let { playingItem ->
            val livePool = remember(shownLive, viewModel.liveItems, viewModel.allLiveCache, viewModel.favorites) {
                when {
                    shownLive.isNotEmpty() -> shownLive
                    viewModel.liveItems.isNotEmpty() -> viewModel.liveItems
                    viewModel.allLiveCache.isNotEmpty() -> viewModel.allLiveCache
                    else -> viewModel.favorites.filter { it.contentType == ContentType.LIVE }
                }
            }
            val relatedMovies = remember(
                viewModel.vodItems,
                playingItem.id,
                playingItem.contentType,
                viewModel.playerVodEpisodes
            ) {
                if (playingItem.contentType == ContentType.MOVIE && viewModel.playerVodEpisodes.isEmpty()) {
                    viewModel.vodItems.filter { it.id != playingItem.id }.take(24)
                } else emptyList()
            }
            val favCheckId = viewModel.playerSeriesParent?.id ?: playingItem.id
            val isCurrentFav = viewModel.favoriteIds.contains(favCheckId)
            val resumeMs = remember(playingItem.id) { viewModel.getResumePositionMs(playingItem.id) }

            UnifiedMorphPlayerOverlay(
                item = playingItem,
                expandAnim = playerExpandAnim,
                isMiniPlayer = viewModel.isPlayerMini,
                isInPipMode = isInPipMode,
                barsVisible = barsVisible && !detailOpen && viewModel.activeLibraryPage == null,
                isCurrentItemFavorite = isCurrentFav,
                initialPositionMs = resumeMs,
                liveChannels = livePool,
                liveCategories = viewModel.liveCatNames,
                selectedLiveCategory = viewModel.selectedLiveCat,
                isLoadingLiveCategory = viewModel.isLoadingLive,
                vodDetailInfo = viewModel.playerVodDetailInfo,
                vodEpisodes = viewModel.playerVodEpisodes,
                relatedVodItems = relatedMovies,
                onSelectLiveCategory = { cat -> viewModel.loadLiveCategory(cat, forceNetwork = false) },
                onSwitchItem = { next -> viewModel.switchPlayerItem(next) },
                onPlayEpisode = { ep ->
                    val parent = viewModel.playerSeriesParent
                    val epMedia = MediaItem(
                        id = "ep_${ep.id}",
                        name = if (parent != null) "${parent.name} • ${ep.title}" else ep.title,
                        group = ep.season,
                        posterUrl = parent?.posterUrl ?: playingItem.posterUrl,
                        streamUrl = ep.streamUrl,
                        contentType = ContentType.MOVIE,
                        seriesId = parent?.seriesId ?: playingItem.seriesId,
                        badge = ep.season
                    )
                    viewModel.playItem(
                        item = epMedia,
                        info = viewModel.playerVodDetailInfo,
                        episodes = viewModel.playerVodEpisodes,
                        seriesParent = parent
                    )
                },
                onDownloadItem = { dlUrl, dlTitle -> triggerDownload(dlUrl, dlTitle) },
                onToggleMinimize = { mini -> viewModel.isPlayerMini = mini },
                onClose = { viewModel.closePlayer() },
                onToggleFavorite = { target ->
                    val favTarget = viewModel.playerSeriesParent ?: target
                    viewModel.toggleFavorite(favTarget)
                },
                onMoreClick = { cardMenuTarget = it },
                onEnterPip = onEnterPip,
                onOpenExternal = { launchExternalPlayer(context, it.streamUrl, it.name) },
                onSaveProgress = { mediaId, posMs, durMs -> viewModel.savePlaybackProgress(mediaId, posMs, durMs) },
                onPlayingStateChanged = { playing -> viewModel.isPlayerActivelyPlaying = playing },
                onVideoAspectRatioChanged = { w, h ->
                    viewModel.videoAspectWidth = w
                    viewModel.videoAspectHeight = h
                }
            )
        }
    }

    // Card ••• Action Sheet
    cardMenuTarget?.let { target ->
        val isHttp = target.streamUrl.startsWith("http://", ignoreCase = true) ||
            target.streamUrl.startsWith("https://", ignoreCase = true)
        val isDownloadable = target.contentType == ContentType.MOVIE && isHttp
        MediaActionBottomSheet(
            item = target,
            isFavorite = viewModel.favoriteIds.contains(target.id),
            onDismiss = { cardMenuTarget = null },
            onViewDetails = if (target.contentType != ContentType.LIVE && !target.id.startsWith("ep_") && !target.id.startsWith("offline_")) {
                { viewModel.openDetail(target) }
            } else null,
            onPlay = {
                if (target.contentType == ContentType.SERIES) viewModel.openDetail(target)
                else viewModel.playItem(target)
            },
            onOpenExternal = { launchExternalPlayer(context, target.streamUrl, target.name) },
            onDownloadBuiltIn = if (isDownloadable) {
                { MediaDownloadHelper.startBuiltInDownload(context, target.streamUrl, target.name) }
            } else null,
            onDownloadExternal = if (isDownloadable) {
                { MediaDownloadHelper.startExternalDownload(context, target.streamUrl, target.name) }
            } else null,
            onToggleFavorite = { viewModel.toggleFavorite(target) }
        )
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
                val btnShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .livelyPress(
                            pressedScale = 0.96f,
                            shape = btnShape,
                            rippleColor = MatteGlassTokens.CanvasWhite
                        ) {
                            pendingDownloadTarget = null
                            MediaDownloadHelper.startBuiltInDownload(context, dlUrl, dlTitle)
                        }
                        .background(MatteGlassTokens.CharcoalPrimary)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Built-in Downloader", color = MatteGlassTokens.CanvasWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .livelyPress(pressedScale = 0.96f, shape = btnShape) {
                            pendingDownloadTarget = null
                            MediaDownloadHelper.startExternalDownload(context, dlUrl, dlTitle)
                        }
                        .background(MatteGlassTokens.SubtleSurface)
                        .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, btnShape)
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
                viewModel.addXtreamServer(name, h, u, p)
            },
            onLoadM3u = { name, url ->
                showAddDialog = false
                viewModel.addM3uServer(name, url)
            }
        )
    }
}
