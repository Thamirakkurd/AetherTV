package com.aethertv.v2.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aethertv.v2.R
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.EpisodeItem
import com.aethertv.v2.data.MediaDetailInfo
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.data.OnlineSubtitleResult
import com.aethertv.v2.data.OnlineSubtitleService
import com.aethertv.v2.ui.theme.LocalHazeState
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.liquidGlassSurface
import com.aethertv.v2.ui.theme.livelyPress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private enum class GestureHudType { BRIGHTNESS, VOLUME }

private val YouTubeRed = Color(0xFFFF0033)

private val LightPlayerAmbientBg = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF10131A),
        0.15f to Color(0xFF1E2430),
        0.26f to Color(0xFFC9D2DF),
        0.44f to Color(0xFFEEF2F7),
        1.00f to Color(0xFFF8F9FC)
    )
)

private val HeaderTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.58f),
    offset = Offset(0f, 1.5f),
    blurRadius = 5f
)

private val DarkPlayerAmbientBg = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF06080C),
        0.18f to Color(0xFF121826),
        0.44f to Color(0xFF0A0E16),
        1.00f to Color(0xFF070A0F)
    )
)

private val ChannelBannerBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF171C26), Color(0xFF283142), Color(0xFF12161E))
)

@Composable
private fun ImmersiveFullscreenEffect(enabled: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity

    DisposableEffect(enabled, activity, view) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enabled) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null && enabled) {
                WindowCompat.getInsetsController(w, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private fun readCurrentBrightness(context: Context, activity: Activity?): Float {
    val windowVal = activity?.window?.attributes?.screenBrightness ?: -1f
    if (windowVal in 0f..1f) return windowVal
    return try {
        val sys = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        (sys / 255f).coerceIn(0.05f, 1f)
    } catch (_: Exception) {
        0.5f
    }
}

private fun applyWindowBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    val lp = window.attributes
    lp.screenBrightness = value.coerceIn(0.02f, 1f)
    window.attributes = lp
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun createAetherExoPlayer(context: Context): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("AetherTV/3.3 (Android)")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)
    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
}

private suspend fun shiftSubtitleFile(
    context: Context,
    sourceUri: Uri,
    delayMs: Long
): Uri? = withContext(Dispatchers.IO) {
    try {
        val rawText = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext null
        val isVtt = sourceUri.toString().endsWith(".vtt", ignoreCase = true) || rawText.trimStart().startsWith("WEBVTT")
        val timeRegex = Regex("""(\d{1,2}:)?(\d{2}):(\d{2})([,.])(\d{3})""")
        val shifted = timeRegex.replace(rawText) { m ->
            val hasHours = m.groupValues[1].isNotEmpty()
            val h = m.groupValues[1].removeSuffix(":").toLongOrNull() ?: 0L
            val min = m.groupValues[2].toLong()
            val sec = m.groupValues[3].toLong()
            val sep = m.groupValues[4]
            val ms = m.groupValues[5].toLong()
            val total = ((h * 3600L + min * 60L + sec) * 1000L + ms + delayMs).coerceAtLeast(0L)
            val nh = total / 3_600_000L
            val nmin = (total % 3_600_000L) / 60_000L
            val nsec = (total % 60_000L) / 1000L
            val nms = total % 1000L
            if (hasHours || nh > 0L) {
                String.format(Locale.US, "%02d:%02d:%02d%s%03d", nh, nmin, nsec, sep, nms)
            } else {
                String.format(Locale.US, "%02d:%02d%s%03d", nmin, nsec, sep, nms)
            }
        }
        val dir = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }
        val ext = if (isVtt) "vtt" else "srt"
        val outFile = File(dir, "shifted_${System.currentTimeMillis()}.$ext")
        outFile.writeText(shifted)
        Uri.fromFile(outFile)
    } catch (_: Exception) {
        null
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun UnifiedMorphPlayerOverlay(
    item: MediaItem,
    expandAnim: Animatable<Float, AnimationVector1D>,
    isMiniPlayer: Boolean,
    isInPipMode: Boolean,
    barsVisible: Boolean,
    favoriteIds: Set<String>,
    // Live TV context
    liveChannels: List<MediaItem>,
    liveCategories: List<String>,
    selectedLiveCategory: String,
    isLoadingLiveCategory: Boolean,
    // Movie / Series context
    vodDetailInfo: MediaDetailInfo,
    vodEpisodes: List<EpisodeItem>,
    relatedVodItems: List<MediaItem>,
    // Callbacks
    onSelectLiveCategory: (String) -> Unit,
    onSwitchItem: (MediaItem) -> Unit,
    onPlayEpisode: (EpisodeItem) -> Unit,
    onDownloadItem: (String, String) -> Unit,
    onToggleMinimize: (Boolean) -> Unit,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoreClick: (MediaItem) -> Unit,
    onEnterPip: () -> Unit,
    onOpenExternal: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val mainHazeState = LocalHazeState.current
    val feedHazeState = remember { HazeState() }

    val isLive = item.contentType == ContentType.LIVE

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscapeConfig = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(isLandscapeConfig && !isMiniPlayer) }
    var manualOrientationLock by remember { mutableStateOf<Int?>(null) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }

    val effectiveFullscreen = !isMiniPlayer && (isFullscreen || isLandscapeConfig)

    fun enterFullscreen() {
        isFullscreen = true
        manualOrientationLock = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    fun exitFullscreen() {
        isFullscreen = false
        val autoRotateOn = try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        } catch (_: Exception) { false }
        if (isLandscapeConfig && autoRotateOn) {
            manualOrientationLock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            manualOrientationLock = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(isLandscapeConfig, isMiniPlayer) {
        if (!isMiniPlayer) {
            isFullscreen = isLandscapeConfig
        } else {
            isFullscreen = false
            manualOrientationLock = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(context, activity) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(angle: Int) {
                if (angle == ORIENTATION_UNKNOWN) return
                val lock = manualOrientationLock ?: return
                val autoRotateOn = try {
                    Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                } catch (_: Exception) { false }
                if (!autoRotateOn) return

                val physicallyPortrait = angle <= 35 || angle >= 325
                val physicallyLandscape = (angle in 55..125) || (angle in 235..305)
                if (lock == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && physicallyPortrait) {
                    manualOrientationLock = null
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else if (lock == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE && physicallyLandscape) {
                    manualOrientationLock = null
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    var videoResolution by remember { mutableStateOf("") }
    var videoCodec by remember { mutableStateOf("") }
    var audioInfo by remember { mutableStateOf("") }
    var bitrateInfo by remember { mutableStateOf("") }

    // Sheets
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showPlayerSettingsSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }

    // Tracks & Subtitles
    var embeddedTextGroups by remember { mutableStateOf<List<Tracks.Group>>(emptyList()) }
    var embeddedAudioGroups by remember { mutableStateOf<List<Tracks.Group>>(emptyList()) }
    var selectedSubtitleLabel by remember { mutableStateOf("Auto") }
    var selectedAudioLabel by remember { mutableStateOf("Default") }
    var subtitleFontScale by remember { mutableFloatStateOf(1.0f) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var externalSubtitleUri by remember { mutableStateOf<Uri?>(null) }
    var delayedEmbeddedText by remember { mutableStateOf("") }

    // Brightness / Volume & Double-Tap Seek HUDs
    var activeHud by remember { mutableStateOf<GestureHudType?>(null) }
    var hudValue by remember { mutableFloatStateOf(0.5f) }
    var hudStamp by remember { mutableLongStateOf(0L) }
    var seekBurstText by remember { mutableStateOf<String?>(null) }
    var seekBurstStamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hudStamp) {
        if (activeHud != null) {
            delay(1050)
            activeHud = null
        }
    }
    LaunchedEffect(seekBurstStamp) {
        if (seekBurstText != null) {
            delay(750)
            seekBurstText = null
        }
    }

    ImmersiveFullscreenEffect(enabled = effectiveFullscreen && !isInPipMode)

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
            AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom"
        )
    }

    // Single persistent ExoPlayer instance
    val exoPlayer = remember { createAetherExoPlayer(context) }

    // Smoothly sync expandAnim with isMiniPlayer state changes (taps, back button, new item)
    var pendingFlingVelocity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isMiniPlayer) {
        val target = if (isMiniPlayer) 0f else 1f
        if (isMiniPlayer && effectiveFullscreen) exitFullscreen()
        val flingVel = pendingFlingVelocity
        pendingFlingVelocity = 0f
        expandAnim.animateTo(
            targetValue = target,
            initialVelocity = flingVel,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f)
        )
    }

    // Load new stream seamlessly on the same TextureView surface
    LaunchedEffect(item.streamUrl) {
        errorText = null
        isBuffering = true
        currentPosMs = 0L
        durationMs = 0L
        videoResolution = ""
        videoCodec = ""
        audioInfo = ""
        bitrateInfo = ""
        externalSubtitleUri = null
        delayedEmbeddedText = ""
        subtitleDelayMs = 0L
        selectedSubtitleLabel = "Auto"
        selectedAudioLabel = "Default"
        playbackSpeed = 1.0f
        exoPlayer.setPlaybackSpeed(1.0f)
        exoPlayer.setMediaItem(ExoMediaItem.fromUri(item.streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun applyExternalSubtitle(subUri: Uri, label: String, delayMs: Long) {
        scope.launch {
            val effectiveUri = if (delayMs != 0L) {
                shiftSubtitleFile(context, subUri, delayMs) ?: subUri
            } else {
                subUri
            }
            val resumePos = exoPlayer.currentPosition
            val mimeType = if (effectiveUri.toString().endsWith(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            val subConfig = ExoMediaItem.SubtitleConfiguration.Builder(effectiveUri)
                .setMimeType(mimeType)
                .setLanguage("en")
                .setLabel(label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            val newMediaItem = ExoMediaItem.Builder()
                .setUri(item.streamUrl)
                .setSubtitleConfigurations(listOf(subConfig))
                .build()

            externalSubtitleUri = subUri
            selectedSubtitleLabel = label
            delayedEmbeddedText = ""
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()
            exoPlayer.setMediaItem(newMediaItem, resumePos)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    val localSubtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            applyExternalSubtitle(uri, "Local File", subtitleDelayMs)
            Toast.makeText(context, "Loaded local subtitle", Toast.LENGTH_SHORT).show()
        }
    }

    val useEmbeddedDelayOverlay = !isLive && externalSubtitleUri == null && subtitleDelayMs > 0L

    DisposableEffect(exoPlayer) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    errorText = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoResolution = "${videoSize.width}×${videoSize.height}"
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                if (!isLive && externalSubtitleUri == null && subtitleDelayMs > 0L) {
                    val cueText = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
                    val waitMs = subtitleDelayMs
                    scope.launch {
                        delay(waitMs)
                        delayedEmbeddedText = cueText
                    }
                } else {
                    delayedEmbeddedText = ""
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val textGroups = ArrayList<Tracks.Group>()
                val audioGroups = ArrayList<Tracks.Group>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) textGroups.add(group)
                    if (group.type == C.TRACK_TYPE_AUDIO) audioGroups.add(group)
                    if (group.isSelected && group.length > 0) {
                        val format = group.getTrackFormat(0)
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            val fps = if (format.frameRate > 0f) " ${format.frameRate.toInt()}fps" else ""
                            val codecName = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                            videoCodec = "$codecName$fps".trim()
                            if (format.bitrate > 0) {
                                bitrateInfo = String.format(Locale.US, "%.1f Mbps", format.bitrate / 1_000_000f)
                            }
                        } else if (group.type == C.TRACK_TYPE_AUDIO) {
                            val aCodec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: "AUDIO"
                            val ch = if (format.channelCount > 0) "${format.channelCount}ch" else ""
                            audioInfo = "$aCodec $ch".trim()
                            selectedAudioLabel = format.label ?: format.language ?: audioInfo
                        }
                    }
                }
                embeddedTextGroups = textGroups
                embeddedAudioGroups = audioGroups
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorText = error.localizedMessage ?: "Playback failed"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }



    LaunchedEffect(exoPlayer, isLive) {
        while (true) {
            if (!isLive) {
                currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            }
            delay(450)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isMiniPlayer) {
        if (controlsVisible && isPlaying && !isMiniPlayer) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Queue computation for Prev / Next
    val currentLiveIdx = remember(liveChannels, item.id) {
        if (isLive) liveChannels.indexOfFirst { it.id == item.id } else -1
    }
    val nextLiveChannel = remember(liveChannels, currentLiveIdx, isLive) {
        if (isLive && liveChannels.size > 1 && currentLiveIdx >= 0) {
            liveChannels[(currentLiveIdx + 1) % liveChannels.size]
        } else if (isLive) liveChannels.firstOrNull { it.id != item.id } else null
    }
    val prevLiveChannel = remember(liveChannels, currentLiveIdx, isLive) {
        if (isLive && liveChannels.size > 1 && currentLiveIdx >= 0) {
            liveChannels[(currentLiveIdx - 1 + liveChannels.size) % liveChannels.size]
        } else null
    }

    val currentEpIdx = remember(vodEpisodes, item.id) {
        if (!isLive && vodEpisodes.isNotEmpty()) {
            val rawId = item.id.removePrefix("ep_")
            vodEpisodes.indexOfFirst { it.id == rawId }
        } else -1
    }
    val prevEpisode = remember(vodEpisodes, currentEpIdx) {
        if (currentEpIdx > 0) vodEpisodes[currentEpIdx - 1] else null
    }
    val nextEpisode = remember(vodEpisodes, currentEpIdx) {
        if (currentEpIdx in 0 until vodEpisodes.lastIndex) vodEpisodes[currentEpIdx + 1] else null
    }

    val quickLiveChips = remember(liveCategories, selectedLiveCategory) {
        buildList {
            if (selectedLiveCategory.isNotBlank()) add(selectedLiveCategory)
            add("Favorites")
            liveCategories.filter { it != selectedLiveCategory && it != "Favorites" }.take(12).forEach { add(it) }
        }.distinct()
    }

    val feedChannels by produceState(
        initialValue = emptyList<MediaItem>(),
        liveChannels,
        item.id,
        isLive
    ) {
        value = if (isLive) {
            withContext(Dispatchers.Default) {
                liveChannels.filterNot { it.id == item.id }.take(100)
            }
        } else emptyList()
    }

    val statsLine = remember(videoResolution, videoCodec, audioInfo, bitrateInfo, item.group) {
        listOfNotNull(
            item.group.takeIf { it.isNotBlank() },
            videoResolution.takeIf { it.isNotBlank() },
            videoCodec.takeIf { it.isNotBlank() },
            bitrateInfo.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
    }

    BackHandler(enabled = effectiveFullscreen) {
        exitFullscreen()
    }

    // Updated refs so active pointer gestures never cancel mid-drag
    val isLiveState by rememberUpdatedState(isLive)
    val isFullscreenState by rememberUpdatedState(effectiveFullscreen)
    val onToggleMinimizeState by rememberUpdatedState(onToggleMinimize)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val animatedMiniBottom by animateDpAsState(
            targetValue = if (barsVisible) navBottom + 78.dp else navBottom + 10.dp,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
            label = "miniDockBottom"
        )

        val p = if (effectiveFullscreen || isInPipMode) 1f else expandAnim.value.coerceIn(0f, 1f)
        val expandedVideoH = screenW * (9f / 16f)
        val miniH = 64.dp
        val miniW = 114.dp
        val miniMarginX = 10.dp
        val miniTopY = (screenH - animatedMiniBottom - miniH).coerceAtLeast(statusBarTop)
        val dragSpanPx = with(density) { (miniTopY - statusBarTop).toPx().coerceAtLeast(260f) }

        val videoX: Dp = if (effectiveFullscreen || isInPipMode) 0.dp else lerp(miniMarginX, 0.dp, p)
        val videoY: Dp = if (effectiveFullscreen || isInPipMode) 0.dp else lerp(miniTopY, statusBarTop, p)
        val videoW: Dp = if (effectiveFullscreen || isInPipMode) screenW else lerp(miniW, screenW, p)
        val videoH: Dp = if (effectiveFullscreen || isInPipMode) screenH else lerp(miniH, expandedVideoH, p)
        val videoCorner: Dp = if (effectiveFullscreen || isInPipMode) 0.dp else lerp(12.dp, 0.dp, p)

        val feedAlpha = ((p - 0.12f) / 0.88f).coerceIn(0f, 1f)
        val miniChromeAlpha = ((1f - p) / 0.50f).coerceIn(0f, 1f)
        val statusCapAlpha = ((p - 0.72f) / 0.28f).coerceIn(0f, 1f)
        val ambientBg = if (MatteGlassTokens.isDark) DarkPlayerAmbientBg else LightPlayerAmbientBg

        // LAYER 1: Portrait Ambient Backdrop + Status Bar Cap + Below-Video Content Sheet
        if (!effectiveFullscreen && !isInPipMode && p > 0.02f) {
            // Status bar dark backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarTop + 2.dp)
                    .graphicsLayer { alpha = statusCapAlpha }
                    .background(Color(0xFF0A0C10))
            )

            // Below-video scrollable sheet (fixed height + GPU translation -> butter-smooth 120fps drag)
            val expandedFeedH = (screenH - statusBarTop - expandedVideoH).coerceAtLeast(0.dp)
            val videoBottomPx = with(density) { (videoY + videoH).toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(expandedFeedH)
                    .graphicsLayer {
                        translationY = videoBottomPx
                        alpha = feedAlpha
                        val s = 0.965f + 0.035f * feedAlpha
                        scaleX = s
                        scaleY = s
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .background(ambientBg)
            ) {
                if (isLive) {
                    LiveFeedBelowPlayer(
                        channel = item,
                        statsLine = statsLine,
                        isFavorite = favoriteIds.contains(item.id),
                        quickChips = quickLiveChips,
                        selectedCategory = selectedLiveCategory,
                        isLoadingCategory = isLoadingLiveCategory,
                        feedChannels = feedChannels,
                        totalChannelCount = liveChannels.size,
                        feedHazeState = feedHazeState,
                        navBottom = navBottom,
                        onSelectCategory = onSelectLiveCategory,
                        onOpenCategorySheet = { showCategorySheet = true },
                        onOpenDescription = { showDescriptionSheet = true },
                        onSwitchChannel = onSwitchItem,
                        onToggleFavorite = { onToggleFavorite(item) },
                        onOpenExternal = { onOpenExternal(item) },
                        onEnterPip = onEnterPip,
                        onOpenSubtitles = { showSubtitleSheet = true },
                        onEnterFullscreen = { enterFullscreen() },
                        onMoreClick = onMoreClick
                    )
                } else {
                    VodContentBelowPlayer(
                        item = item,
                        statsLine = statsLine,
                        detailInfo = vodDetailInfo,
                        episodes = vodEpisodes,
                        relatedItems = relatedVodItems,
                        isFavorite = favoriteIds.contains(item.id),
                        navBottom = navBottom,
                        onToggleFavorite = { onToggleFavorite(item) },
                        onOpenDescription = { showDescriptionSheet = true },
                        onDownload = { onDownloadItem(item.streamUrl, item.name) },
                        onOpenExternal = { onOpenExternal(item) },
                        onEnterPip = onEnterPip,
                        onOpenSubtitles = { showSubtitleSheet = true },
                        onEnterFullscreen = { enterFullscreen() },
                        onPlayEpisode = onPlayEpisode,
                        onDownloadEpisode = { ep -> onDownloadItem(ep.streamUrl, "${item.name} - ${ep.title}") },
                        onSelectRelated = onSwitchItem
                    )
                }
            }
        }

        // LAYER 2: Docked Mini-Player Liquid Glass Bar + Right Controls
        if (!effectiveFullscreen && !isInPipMode && p < 0.88f) {
            val dockW = lerp(screenW - miniMarginX * 2, screenW, p)
            val dockX = lerp(miniMarginX, 0.dp, p)
            val dockShape = RoundedCornerShape(lerp(12.dp, 0.dp, p))

            Box(
                modifier = Modifier
                    .offset(x = dockX, y = videoY)
                    .size(width = dockW, height = videoH)
                    .graphicsLayer { alpha = miniChromeAlpha }
                    .liquidGlassSurface(
                        hazeState = mainHazeState,
                        shape = dockShape,
                        tintColor = MatteGlassTokens.NavGlassBg,
                        fallbackColor = MatteGlassTokens.NavGlassFallback,
                        blurRadius = 32.dp,
                        borderWidth = 0.9.dp,
                        borderBrush = MatteGlassTokens.SpecularBorderBrush
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = miniW + 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(YouTubeRed)
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = if (isLive) {
                                    item.group.ifBlank { "LIVE" }
                                } else if (durationMs > 0L) {
                                    "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}"
                                } else {
                                    item.group.ifBlank { "Video" }
                                },
                                fontSize = 11.sp,
                                color = MatteGlassTokens.CharcoalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Space reserved for interactive Play/Pause & Close buttons placed above gesture layer
                    Spacer(Modifier.width(88.dp))
                }

                // Thin YouTube red progress bar along bottom edge of mini-player
                if (!isLive && durationMs > 0L) {
                    val frac = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MatteGlassTokens.HairlineDivider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac)
                                .background(YouTubeRed)
                        )
                    }
                }
            }
        }

        // LAYER 3: SINGLE PERSISTENT TEXTUREVIEW VIDEO CONTAINER (NEVER UNMOUNTS -> ZERO BLACK FLASH!)
        val videoShape = RoundedCornerShape(videoCorner)
        Box(
            modifier = Modifier
                .offset(x = videoX, y = videoY)
                .size(width = videoW, height = videoH)
                .clip(videoShape)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.aether_texture_player_view, null) as PlayerView).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) view.player = exoPlayer
                    view.resizeMode = if (p < 0.25f && !effectiveFullscreen && !isInPipMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        resizeModes[resizeModeIndex].first
                    }
                    view.subtitleView?.alpha = if (useEmbeddedDelayOverlay) 0f else 1f
                    val baseSp = if (effectiveFullscreen) 18f else 14f
                    view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp * subtitleFontScale)
                },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize()
            )

            // Delayed embedded subtitle fallback overlay
            if (useEmbeddedDelayOverlay && delayedEmbeddedText.isNotBlank() && p > 0.7f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = if (controlsVisible) 58.dp else 18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = delayedEmbeddedText,
                        color = Color.White,
                        fontSize = ((if (effectiveFullscreen) 17f else 13f) * subtitleFontScale).sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isBuffering && errorText == null && p > 0.35f) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.6.dp,
                    modifier = Modifier.size(if (effectiveFullscreen) 40.dp else 32.dp).align(Alignment.Center)
                )
            }

            if (p > 0.7f) {
                errorText?.let { err ->
                    PlayerErrorCard(
                        err = err,
                        onRetry = {
                            errorText = null
                            isBuffering = true
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        },
                        onOpenExternal = { onOpenExternal(item) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Double-tap ±10s burst pill
            androidx.compose.animation.AnimatedVisibility(
                visible = seekBurstText != null && p > 0.7f && !isInPipMode,
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                seekBurstText?.let { txt ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(txt, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Brightness / Volume Gesture HUD Pill
            androidx.compose.animation.AnimatedVisibility(
                visible = activeHud != null && effectiveFullscreen && !isInPipMode,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp)
            ) {
                activeHud?.let { hudType -> GestureFeedbackPill(type = hudType, progress = hudValue) }
            }

            // Thin red progress/live bar when controls are hidden in portrait
            if (!controlsVisible && !effectiveFullscreen && !isInPipMode && p > 0.85f) {
                val frac = if (isLive) 1f else if (durationMs > 0L) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac)
                            .background(YouTubeRed)
                    )
                }
            }
        }

        // LAYER 4: FIXED-POSITION GESTURE CATCHER (NEVER MOVES MID-DRAG -> BUTTER-SMOOTH 1:1 TRACKING!)
        if (!isInPipMode) {
            if (effectiveFullscreen) {
                // Fullscreen 3-Zone Gesture Layer: Left=Brightness, Middle-Down=Exit Fullscreen, Right=Volume
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = { pos ->
                                    if (!isLiveState) {
                                        if (pos.x < size.width * 0.4f) {
                                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                            seekBurstText = "-10s"
                                            seekBurstStamp++
                                        } else if (pos.x > size.width * 0.6f) {
                                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                                            seekBurstText = "+10s"
                                            seekBurstStamp++
                                        } else {
                                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            var zone = 1
                            var level = 0.5f
                            var lastAppliedVol = -1
                            var middleDragDown = 0f
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

                            detectVerticalDragGestures(
                                onDragStart = { startOffset ->
                                    val third = size.width / 3f
                                    zone = when {
                                        startOffset.x < third -> 0
                                        startOffset.x > third * 2f -> 2
                                        else -> 1
                                    }
                                    middleDragDown = 0f
                                    when (zone) {
                                        0 -> {
                                            level = readCurrentBrightness(context, activity)
                                            activeHud = GestureHudType.BRIGHTNESS
                                            hudValue = level
                                            hudStamp++
                                        }
                                        2 -> {
                                            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                            lastAppliedVol = cur
                                            level = cur.toFloat() / maxVol.toFloat()
                                            activeHud = GestureHudType.VOLUME
                                            hudValue = level
                                            hudStamp++
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (zone == 1 && middleDragDown > 92f) {
                                        exitFullscreen()
                                    }
                                    middleDragDown = 0f
                                },
                                onDragCancel = { middleDragDown = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    val span = (size.height * 0.65f).coerceAtLeast(220f)
                                    when (zone) {
                                        0 -> {
                                            level = (level - dragAmount / span).coerceIn(0f, 1f)
                                            hudValue = level
                                            hudStamp++
                                            applyWindowBrightness(activity, level)
                                        }
                                        1 -> middleDragDown += dragAmount
                                        2 -> {
                                            level = (level - dragAmount / span).coerceIn(0f, 1f)
                                            hudValue = level
                                            hudStamp++
                                            val targetVol = (level * maxVol).roundToInt().coerceIn(0, maxVol)
                                            if (targetVol != lastAppliedVol) {
                                                lastAppliedVol = targetVol
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )
            } else if (!isMiniPlayer) {
                // Expanded Portrait fixed top 16:9 gesture catcher (1:1 smooth minimize drag + swipe-up fullscreen)
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = statusBarTop)
                        .size(width = screenW, height = expandedVideoH)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = { pos ->
                                    if (!isLiveState) {
                                        if (pos.x < size.width * 0.4f) {
                                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                            seekBurstText = "-10s"
                                            seekBurstStamp++
                                        } else if (pos.x > size.width * 0.6f) {
                                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                                            seekBurstText = "+10s"
                                            seekBurstStamp++
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(dragSpanPx) {
                            val tracker = VelocityTracker()
                            var cumulativeY = 0f

                            detectVerticalDragGestures(
                                onDragStart = {
                                    tracker.resetTracking()
                                    cumulativeY = 0f
                                    controlsVisible = false
                                },
                                onDragEnd = {
                                    val vy = tracker.calculateVelocity().y
                                    val curP = expandAnim.value
                                    val normVel = (-vy / dragSpanPx).coerceIn(-6f, 6f)
                                    when {
                                        cumulativeY < -88f && curP > 0.95f -> {
                                            enterFullscreen()
                                        }
                                        curP < 0.68f || vy > 560f -> {
                                            pendingFlingVelocity = normVel
                                            onToggleMinimizeState(true)
                                        }
                                        else -> {
                                            scope.launch {
                                                expandAnim.animateTo(
                                                    targetValue = 1f,
                                                    initialVelocity = normVel,
                                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f)
                                                )
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        expandAnim.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 380f))
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeY += dragAmount
                                    tracker.addPosition(change.uptimeMillis, Offset(0f, cumulativeY))
                                    val nextP = (1f - (cumulativeY / dragSpanPx)).coerceIn(0f, 1f)
                                    scope.launch { expandAnim.snapTo(nextP) }
                                }
                            )
                        }
                )
            } else {
                // Docked Mini-Player fixed gesture catcher (1:1 smooth swipe-up to maximize, tap to expand)
                Box(
                    modifier = Modifier
                        .offset(x = miniMarginX, y = miniTopY)
                        .size(width = screenW - miniMarginX * 2, height = miniH)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onToggleMinimizeState(false) })
                        }
                        .pointerInput(dragSpanPx) {
                            val tracker = VelocityTracker()
                            var cumulativeY = 0f

                            detectVerticalDragGestures(
                                onDragStart = {
                                    tracker.resetTracking()
                                    cumulativeY = 0f
                                },
                                onDragEnd = {
                                    val vy = tracker.calculateVelocity().y
                                    val curP = expandAnim.value
                                    val normVel = (-vy / dragSpanPx).coerceIn(-6f, 6f)
                                    if (curP > 0.22f || vy < -560f) {
                                        pendingFlingVelocity = normVel
                                        onToggleMinimizeState(false)
                                    } else {
                                        scope.launch {
                                            expandAnim.animateTo(
                                                targetValue = 0f,
                                                initialVelocity = normVel,
                                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        expandAnim.animateTo(0f, spring(dampingRatio = 0.86f, stiffness = 380f))
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeY += dragAmount
                                    tracker.addPosition(change.uptimeMillis, Offset(0f, cumulativeY))
                                    val nextP = (-cumulativeY / dragSpanPx).coerceIn(0f, 1f)
                                    scope.launch { expandAnim.snapTo(nextP) }
                                }
                            )
                        }
                ) {
                    // Mini-player Play/Pause & Close buttons (sit cleanly above drag layer on right end)
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .graphicsLayer { alpha = miniChromeAlpha },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MatteGlassTokens.CharcoalPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close mini player",
                                tint = MatteGlassTokens.CharcoalMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // LAYER 5: YOUTUBE PLAYER OVERLAY CONTROLS (Exact Icons from Screenshot!)
        val showControls = controlsVisible && !isInPipMode && (effectiveFullscreen || (!isMiniPlayer && p > 0.92f))
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .offset(x = videoX, y = videoY)
                .size(width = videoW, height = videoH)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
            ) {
                // Top Row: Clean Down Chevron (left) + Cast / CC / Settings Gear (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)))
                        .then(if (effectiveFullscreen) Modifier.displayCutoutPadding() else Modifier)
                        .padding(horizontal = if (effectiveFullscreen) 16.dp else 8.dp, vertical = if (effectiveFullscreen) 10.dp else 4.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        YouTubeCleanIconBtn(
                            icon = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (effectiveFullscreen) "Exit Fullscreen" else "Minimize",
                            iconSize = 28.dp,
                            onClick = {
                                if (effectiveFullscreen) exitFullscreen()
                                else onToggleMinimize(true)
                            }
                        )
                        if (effectiveFullscreen) {
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (statsLine.isNotBlank()) {
                                    Text(
                                        text = statsLine,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        YouTubeCleanIconBtn(
                            icon = Icons.Outlined.Cast,
                            contentDescription = "External Player / Cast",
                            iconSize = 22.dp,
                            onClick = { onOpenExternal(item) }
                        )
                        YouTubeCleanIconBtn(
                            icon = Icons.Outlined.ClosedCaption,
                            contentDescription = "Subtitles",
                            iconSize = 23.dp,
                            onClick = { showSubtitleSheet = true }
                        )
                        YouTubeCleanIconBtn(
                            icon = Icons.Outlined.Settings,
                            contentDescription = "Player Settings",
                            iconSize = 22.dp,
                            onClick = { showPlayerSettingsSheet = true }
                        )
                    }
                }

                // Center Row: Prev / Large Clean Play-Pause / Next (matching YouTube screenshot)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(if (effectiveFullscreen) 48.dp else 36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val leftEnabled = when {
                        isLive -> prevLiveChannel != null
                        vodEpisodes.isNotEmpty() -> prevEpisode != null
                        else -> true
                    }
                    val leftIcon = if (!isLive && vodEpisodes.isEmpty()) Icons.Rounded.Replay10 else Icons.Rounded.SkipPrevious
                    YouTubeCleanIconBtn(
                        icon = leftIcon,
                        contentDescription = "Previous",
                        iconSize = 32.dp,
                        tint = if (leftEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                        onClick = {
                            when {
                                isLive && prevLiveChannel != null -> onSwitchItem(prevLiveChannel)
                                prevEpisode != null -> onPlayEpisode(prevEpisode)
                                !isLive -> exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .livelyPress(pressedScale = 0.88f) {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    val rightEnabled = when {
                        isLive -> nextLiveChannel != null
                        vodEpisodes.isNotEmpty() -> nextEpisode != null
                        else -> true
                    }
                    val rightIcon = if (!isLive && vodEpisodes.isEmpty()) Icons.Rounded.Forward10 else Icons.Rounded.SkipNext
                    YouTubeCleanIconBtn(
                        icon = rightIcon,
                        contentDescription = "Next",
                        iconSize = 32.dp,
                        tint = if (rightEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                        onClick = {
                            when {
                                isLive && nextLiveChannel != null -> onSwitchItem(nextLiveChannel)
                                nextEpisode != null -> onPlayEpisode(nextEpisode)
                                !isLive -> exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                            }
                        }
                    )
                }

                // Bottom Row: Timestamp / LIVE + Diagonal Fullscreen Arrows + YouTube Red Scrub Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))))
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (effectiveFullscreen) 18.dp else 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(YouTubeRed))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (videoResolution.isNotBlank()) "LIVE • $videoResolution" else "LIVE",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        YouTubeCleanIconBtn(
                            icon = if (effectiveFullscreen) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull,
                            contentDescription = if (effectiveFullscreen) "Exit Fullscreen" else "Fullscreen",
                            iconSize = 20.dp,
                            onClick = { if (effectiveFullscreen) exitFullscreen() else enterFullscreen() }
                        )
                    }

                    // Sleek YouTube Red Edge Scrub Bar
                    if (!isLive && durationMs > 0L) {
                        YouTubeScrubBar(
                            currentMs = currentPosMs,
                            durationMs = durationMs,
                            onSeekTo = { targetMs ->
                                currentPosMs = targetMs
                                exoPlayer.seekTo(targetMs)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (effectiveFullscreen) 16.dp else 0.dp,
                                    vertical = if (effectiveFullscreen) 10.dp else 0.dp
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(YouTubeRed)
                        )
                    }
                }
            }
        }
    }

    // YouTube-style Description Bottom Sheet (opened by tapping title / ...more)
    if (showDescriptionSheet) {
        MediaDescriptionSheet(
            item = item,
            isLive = isLive,
            detailInfo = vodDetailInfo,
            episodeCount = vodEpisodes.size,
            videoResolution = videoResolution,
            videoCodec = videoCodec,
            audioInfo = audioInfo,
            bitrateInfo = bitrateInfo,
            onDismiss = { showDescriptionSheet = false }
        )
    }

    // Category Switcher Bottom Sheet (Live TV)
    if (showCategorySheet) {
        LiveCategoryPickerSheet(
            categories = liveCategories,
            selectedCategory = selectedLiveCategory,
            onDismiss = { showCategorySheet = false },
            onSelectCategory = { cat ->
                showCategorySheet = false
                onSelectLiveCategory(cat)
            }
        )
    }

    // YouTube Gear Player Settings Sheet
    if (showPlayerSettingsSheet) {
        PlayerSettingsSheet(
            isLive = isLive,
            statsLine = statsLine,
            resizeModes = resizeModes,
            resizeModeIndex = resizeModeIndex,
            playbackSpeed = playbackSpeed,
            audioGroups = embeddedAudioGroups,
            selectedAudioLabel = selectedAudioLabel,
            onDismiss = { showPlayerSettingsSheet = false },
            onSelectResizeMode = { idx -> resizeModeIndex = idx },
            onSelectSpeed = { spd ->
                playbackSpeed = spd
                exoPlayer.setPlaybackSpeed(spd)
            },
            onSelectAudioTrack = { group, label ->
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                    .build()
                selectedAudioLabel = label
            },
            onOpenSubtitles = {
                showPlayerSettingsSheet = false
                showSubtitleSheet = true
            },
            onEnterPip = {
                showPlayerSettingsSheet = false
                onEnterPip()
            },
            onOpenExternal = {
                showPlayerSettingsSheet = false
                onOpenExternal(item)
            }
        )
    }

    // Subtitle Settings Sheet
    if (showSubtitleSheet) {
        SubtitleSettingsSheet(
            mediaTitle = item.name,
            embeddedTextGroups = embeddedTextGroups,
            selectedLabel = selectedSubtitleLabel,
            fontScale = subtitleFontScale,
            delayMs = subtitleDelayMs,
            hasExternalSub = externalSubtitleUri != null,
            onDismiss = { showSubtitleSheet = false },
            onPickLocal = {
                if (isLive) {
                    Toast.makeText(context, "Local subtitles are for Movies & Series", Toast.LENGTH_SHORT).show()
                } else {
                    localSubtitlePicker.launch(arrayOf("*/*"))
                }
            },
            onSelectOff = {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                selectedSubtitleLabel = "Off"
                externalSubtitleUri = null
                delayedEmbeddedText = ""
            },
            onSelectEmbeddedTrack = { group, label ->
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                    .build()
                externalSubtitleUri = null
                selectedSubtitleLabel = label
            },
            onDownloadAndApplyOnline = { sub ->
                if (!isLive) {
                    scope.launch {
                        val cachedUri = OnlineSubtitleService.downloadSubtitleToCache(context, sub)
                        if (cachedUri != null) {
                            applyExternalSubtitle(cachedUri, sub.language, subtitleDelayMs)
                            Toast.makeText(context, "Loaded: ${sub.fileName}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to download subtitle", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onSaveAndDone = { newScale, newDelayMs ->
                val delayChanged = newDelayMs != subtitleDelayMs
                subtitleFontScale = newScale
                subtitleDelayMs = newDelayMs
                if (delayChanged && !isLive) {
                    val ext = externalSubtitleUri
                    if (ext != null) {
                        applyExternalSubtitle(ext, selectedSubtitleLabel, newDelayMs)
                    } else if (newDelayMs < 0L) {
                        Toast.makeText(
                            context,
                            "Negative sync shift applies to loaded/online .srt/.vtt subtitles",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                showSubtitleSheet = false
            }
        )
    }
}

@Composable
private fun YouTubeCleanIconBtn(
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .livelyPress(pressedScale = 0.86f, depthShiftPx = 0f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun YouTubeScrubBar(
    currentMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var draggingFrac by remember { mutableStateOf<Float?>(null) }
    val displayFrac = draggingFrac ?: (currentMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .height(18.dp)
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekTo((frac * safeDuration).toLong())
                }
            }
            .pointerInput(safeDuration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        draggingFrac = frac
                    },
                    onDragEnd = {
                        draggingFrac?.let { f -> onSeekTo((f * safeDuration).toLong()) }
                        draggingFrac = null
                    },
                    onDragCancel = { draggingFrac = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        draggingFrac = frac
                    }
                )
            },
        contentAlignment = Alignment.BottomStart
    ) {
        val trackW = maxWidth
        val thumbSize = if (draggingFrac != null) 14.dp else 11.dp
        val thumbOffsetX = (trackW * displayFrac - thumbSize / 2).coerceIn(0.dp, (trackW - thumbSize).coerceAtLeast(0.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.CenterStart)
                .background(Color.White.copy(alpha = 0.30f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(displayFrac)
                    .background(YouTubeRed)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffsetX)
                .size(thumbSize)
                .clip(CircleShape)
                .background(YouTubeRed)
        )
    }
}

@Composable
private fun LiveFeedBelowPlayer(
    channel: MediaItem,
    statsLine: String,
    isFavorite: Boolean,
    quickChips: List<String>,
    selectedCategory: String,
    isLoadingCategory: Boolean,
    feedChannels: List<MediaItem>,
    totalChannelCount: Int,
    feedHazeState: HazeState,
    navBottom: Dp,
    onSelectCategory: (String) -> Unit,
    onOpenCategorySheet: () -> Unit,
    onOpenDescription: () -> Unit,
    onSwitchChannel: (MediaItem) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenExternal: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onMoreClick: (MediaItem) -> Unit
) {
    val activeCategoryLabel = selectedCategory.ifBlank { channel.group.ifBlank { "All Channels" } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(feedHazeState),
            contentPadding = PaddingValues(bottom = navBottom + 86.dp)
        ) {
            item(key = "yt_live_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenDescription)
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = channel.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = TextStyle(shadow = HeaderTextShadow),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = statsLine.ifBlank { "Live TV Channel" },
                                fontSize = 12.sp,
                                color = Color(0xE6E2E8F0),
                                style = TextStyle(shadow = HeaderTextShadow),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "...more",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                style = TextStyle(shadow = HeaderTextShadow)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel.posterUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = channel.posterUrl,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                            } else {
                                Icon(Icons.Default.LiveTv, null, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .livelyPress(pressedScale = 0.93f, onClick = onToggleFavorite)
                                .clip(CircleShape)
                                .background(MatteGlassTokens.CharcoalPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = MatteGlassTokens.CanvasWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isFavorite) "Favorited" else "Favorite",
                                    color = MatteGlassTokens.CanvasWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { YouTubeActionPill(Icons.Default.OpenInNew, "External", onOpenExternal) }
                            item { YouTubeActionPill(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip) }
                            item { YouTubeActionPill(Icons.Outlined.ClosedCaption, "CC", onOpenSubtitles) }
                            item { YouTubeActionPill(Icons.Rounded.OpenInFull, "Full", onEnterFullscreen) }
                        }
                    }
                }
            }

            item(key = "yt_live_chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickChips, key = { it }) { chip ->
                        val selected = chip == selectedCategory
                        Box(
                            modifier = Modifier
                                .livelyPress(pressedScale = 0.94f) { onSelectCategory(chip) }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                .border(
                                    0.75.dp,
                                    if (selected) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = chip,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (isLoadingCategory) {
                    LinearProgressIndicator(
                        color = MatteGlassTokens.CharcoalPrimary,
                        trackColor = MatteGlassTokens.FieldGlassBg,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            items(feedChannels, key = { "yt_ch_${it.id}" }, contentType = { "yt_channel_card" }) { nextItem ->
                YouTubeChannelFeedCard(
                    channel = nextItem,
                    onClick = { onSwitchChannel(nextItem) },
                    onMoreClick = { onMoreClick(nextItem) }
                )
            }
        }

        // Floating blurred Liquid Glass Category Switcher Bar
        val barShape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 10.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
                .livelyPress(pressedScale = 0.975f, depthShiftPx = 2f, onClick = onOpenCategorySheet)
                .liquidGlassSurface(
                    hazeState = feedHazeState,
                    shape = barShape,
                    tintColor = MatteGlassTokens.NavGlassBg,
                    fallbackColor = MatteGlassTokens.NavGlassFallback,
                    blurRadius = 32.dp,
                    borderWidth = 0.9.dp,
                    borderBrush = MatteGlassTokens.SpecularBorderBrush
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Category, "Categories", tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeCategoryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$totalChannelCount channels • Tap to switch category",
                    fontSize = 11.sp,
                    color = MatteGlassTokens.CharcoalMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.KeyboardArrowUp, "Change Category", tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun VodContentBelowPlayer(
    item: MediaItem,
    statsLine: String,
    detailInfo: MediaDetailInfo,
    episodes: List<EpisodeItem>,
    relatedItems: List<MediaItem>,
    isFavorite: Boolean,
    navBottom: Dp,
    onToggleFavorite: () -> Unit,
    onOpenDescription: () -> Unit,
    onDownload: () -> Unit,
    onOpenExternal: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onPlayEpisode: (EpisodeItem) -> Unit,
    onDownloadEpisode: (EpisodeItem) -> Unit,
    onSelectRelated: (MediaItem) -> Unit
) {
    val seasons = remember(episodes) { episodes.map { it.season }.distinct() }
    var selectedSeason by remember(seasons, item.group) {
        mutableStateOf(seasons.firstOrNull { it == item.group } ?: seasons.firstOrNull() ?: "")
    }
    val visibleEpisodes = remember(episodes, selectedSeason) {
        if (selectedSeason.isBlank()) episodes else episodes.filter { it.season == selectedSeason }
    }
    val currentEpId = item.id.removePrefix("ep_")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = navBottom + 28.dp)
    ) {
        item(key = "vod_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenDescription)
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = TextStyle(shadow = HeaderTextShadow),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOfNotNull(
                        detailInfo.releaseYear.takeIf { it.isNotBlank() },
                        detailInfo.rating.takeIf { it.isNotBlank() && it != "0" }?.let { "★ $it" },
                        detailInfo.duration.takeIf { it.isNotBlank() },
                        statsLine.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                fontSize = 12.sp,
                                color = Color(0xE6E2E8F0),
                                style = TextStyle(shadow = HeaderTextShadow),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Text(
                            text = "...more",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            style = TextStyle(shadow = HeaderTextShadow)
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Box(
                            modifier = Modifier
                                .livelyPress(pressedScale = 0.93f, onClick = onToggleFavorite)
                                .clip(CircleShape)
                                .background(MatteGlassTokens.CharcoalPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = MatteGlassTokens.CanvasWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isFavorite) "Favorited" else "Favorite",
                                    color = MatteGlassTokens.CanvasWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item { YouTubeActionPill(Icons.Default.Download, "Download", onDownload) }
                    item { YouTubeActionPill(Icons.Outlined.ClosedCaption, "Subtitles", onOpenSubtitles) }
                    item { YouTubeActionPill(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip) }
                    item { YouTubeActionPill(Icons.Default.OpenInNew, "External", onOpenExternal) }
                    item { YouTubeActionPill(Icons.Rounded.OpenInFull, "Full", onEnterFullscreen) }
                }

                if (detailInfo.plot.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .livelyPress(pressedScale = 0.98f, onClick = onOpenDescription)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CardGlassBg)
                            .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detailInfo.genre.ifBlank { "Description" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Read more",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.CharcoalMuted
                            )
                        }
                        Text(
                            text = detailInfo.plot,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (episodes.isNotEmpty()) {
            item(key = "vod_episodes_header") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                    Text(
                        text = "Episodes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    if (seasons.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seasons) { season ->
                                val active = season == selectedSeason
                                Box(
                                    modifier = Modifier
                                        .livelyPress(pressedScale = 0.94f) { selectedSeason = season }
                                        .clip(CircleShape)
                                        .background(if (active) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                        .border(
                                            0.75.dp,
                                            if (active) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                            CircleShape
                                        )
                                        .padding(horizontal = 13.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = season,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(visibleEpisodes, key = { "vod_ep_${it.id}" }) { ep ->
                val playingThis = ep.id == currentEpId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .livelyPress(pressedScale = 0.975f) { onPlayEpisode(ep) }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (playingThis) MatteGlassTokens.FieldGlassBg else MatteGlassTokens.CardGlassBg)
                        .border(
                            0.9.dp,
                            if (playingThis) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ep.title,
                            fontSize = 13.sp,
                            fontWeight = if (playingThis) FontWeight.Bold else FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (playingThis) "${ep.season} • Playing now" else ep.season,
                            fontSize = 11.sp,
                            color = MatteGlassTokens.CharcoalMuted
                        )
                    }
                    IconButton(onClick = { onDownloadEpisode(ep) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Download, "Download", tint = MatteGlassTokens.CharcoalMuted, modifier = Modifier.size(18.dp))
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.padding(end = 6.dp).size(20.dp)
                    )
                }
            }
        } else if (relatedItems.isNotEmpty()) {
            item(key = "vod_related_header") {
                Text(
                    text = "More in ${item.group.ifBlank { "Movies" }}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            items(relatedItems.take(24), key = { "rel_${it.id}" }) { rel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .livelyPress(pressedScale = 0.975f) { onSelectRelated(rel) }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 66.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1D222C))
                    ) {
                        if (!rel.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = rel.posterUrl,
                                contentDescription = rel.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rel.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (rel.badge.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(rel.badge, fontSize = 11.sp, color = MatteGlassTokens.CharcoalMuted)
                        }
                    }
                    Icon(Icons.Rounded.PlayArrow, null, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.padding(end = 6.dp).size(22.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    isLive: Boolean,
    statsLine: String,
    resizeModes: List<Pair<Int, String>>,
    resizeModeIndex: Int,
    playbackSpeed: Float,
    audioGroups: List<Tracks.Group>,
    selectedAudioLabel: String,
    onDismiss: () -> Unit,
    onSelectResizeMode: (Int) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectAudioTrack: (Tracks.Group, String) -> Unit,
    onOpenSubtitles: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MatteGlassTokens.SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("Player Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                if (statsLine.isNotBlank()) {
                    Text(statsLine, fontSize = 12.sp, color = MatteGlassTokens.CharcoalMuted)
                }
            }

            // Aspect Ratio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.CardGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Aspect Ratio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    resizeModes.forEachIndexed { idx, (_, label) ->
                        val active = idx == resizeModeIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .livelyPress(pressedScale = 0.94f) { onSelectResizeMode(idx) }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                            )
                        }
                    }
                }
            }

            // Playback Speed (VOD)
            if (!isLive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Playback Speed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            val active = playbackSpeed == spd
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .livelyPress(pressedScale = 0.94f) { onSelectSpeed(spd) }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${spd}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Audio Tracks
            if (audioGroups.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Audio Track", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                    audioGroups.forEachIndexed { idx, group ->
                        val fmt = group.getTrackFormat(0)
                        val label = fmt.label ?: fmt.language ?: "Audio ${idx + 1}"
                        val active = label == selectedAudioLabel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .livelyPress(pressedScale = 0.97f) { onSelectAudioTrack(group, label) }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MatteGlassTokens.FieldGlassBg else MatteGlassTokens.SubtleSurface)
                                .border(0.8.dp, if (active) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = MatteGlassTokens.CharcoalPrimary)
                            if (active) Icon(Icons.Default.Check, null, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Quick Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .livelyPress(pressedScale = 0.95f, onClick = onOpenSubtitles)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Subtitles", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .livelyPress(pressedScale = 0.95f, onClick = onEnterPip)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PiP Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .livelyPress(pressedScale = 0.95f, onClick = onOpenExternal)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("External", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveCategoryPickerSheet(
    categories: List<String>,
    selectedCategory: String,
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filter by remember { mutableStateOf("") }
    val filtered = remember(categories, filter) {
        if (filter.isBlank()) categories
        else categories.filter { it.contains(filter, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MatteGlassTokens.SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Switch Live TV Category",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MatteGlassTokens.CharcoalPrimary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.FieldGlassBg)
                    .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = MatteGlassTokens.CharcoalMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (filter.isEmpty()) {
                        Text("Filter categories...", fontSize = 13.sp, color = MatteGlassTokens.CharcoalMuted)
                    }
                    BasicTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = MatteGlassTokens.CharcoalPrimary),
                        cursorBrush = SolidColor(MatteGlassTokens.CharcoalPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (filter.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MatteGlassTokens.CharcoalMuted,
                        modifier = Modifier.size(17.dp).clickable { filter = "" }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it }) { cat ->
                    val active = cat == selectedCategory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .livelyPress(pressedScale = 0.975f) { onSelectCategory(cat) }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CardGlassBg)
                            .border(
                                0.8.dp,
                                if (active) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat,
                            fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (active) {
                            Icon(Icons.Default.Check, null, tint = MatteGlassTokens.CanvasWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureFeedbackPill(
    type: GestureHudType,
    progress: Float
) {
    val pct = (progress * 100f).roundToInt().coerceIn(0, 100)
    val icon = when {
        type == GestureHudType.BRIGHTNESS -> Icons.Default.Brightness6
        pct == 0 -> Icons.AutoMirrored.Filled.VolumeOff
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xCC10141D))
            .border(0.9.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Text(
            text = "$pct%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun YouTubeActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .livelyPress(pressedScale = 0.93f, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MatteGlassTokens.CharcoalPrimary)
    }
}

@Composable
private fun YouTubeChannelFeedCard(
    channel: MediaItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    val logoReq = remember(channel.posterUrl) {
        channel.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .size(280, 180)
                .build()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .livelyPress(pressedScale = 0.98f, depthShiftPx = 3f, onClick = onClick)
            .padding(bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(ChannelBannerBrush),
            contentAlignment = Alignment.Center
        ) {
            if (logoReq != null) {
                AsyncImage(
                    model = logoReq,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.42f).fillMaxHeight(0.56f)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(44.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xCC0B0D11))
                    .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(YouTubeRed))
                Spacer(Modifier.width(5.dp))
                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (logoReq != null) {
                    AsyncImage(
                        model = logoReq,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(5.dp)
                    )
                } else {
                    Icon(Icons.Default.LiveTv, null, tint = MatteGlassTokens.CharcoalMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MatteGlassTokens.CharcoalPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${channel.group.ifBlank { "Live TV" }} • Live stream",
                    fontSize = 12.sp,
                    color = MatteGlassTokens.CharcoalMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onMoreClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MatteGlassTokens.CharcoalMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleSettingsSheet(
    mediaTitle: String,
    embeddedTextGroups: List<Tracks.Group>,
    selectedLabel: String,
    fontScale: Float,
    delayMs: Long,
    hasExternalSub: Boolean,
    onDismiss: () -> Unit,
    onPickLocal: () -> Unit,
    onSelectOff: () -> Unit,
    onSelectEmbeddedTrack: (Tracks.Group, String) -> Unit,
    onDownloadAndApplyOnline: (OnlineSubtitleResult) -> Unit,
    onSaveAndDone: (Float, Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draftScale by remember(fontScale) { mutableFloatStateOf(fontScale) }
    var draftDelayMs by remember(delayMs) { mutableLongStateOf(delayMs) }
    var searchQuery by remember(mediaTitle) { mutableStateOf(mediaTitle) }
    var subLang by remember { mutableStateOf("eng") }
    var subResults by remember { mutableStateOf<List<OnlineSubtitleResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MatteGlassTokens.SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Subtitles", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                    Text("Active: $selectedLabel", fontSize = 12.sp, color = MatteGlassTokens.CharcoalMuted)
                }
                Box(
                    modifier = Modifier
                        .livelyPress(pressedScale = 0.93f) { onSaveAndDone(draftScale, draftDelayMs) }
                        .clip(CircleShape)
                        .background(MatteGlassTokens.CharcoalPrimary)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = MatteGlassTokens.CanvasWhite, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CanvasWhite)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.CardGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Timing Delay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = formatDelay(draftDelayMs),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (draftDelayMs == 0L) MatteGlassTokens.CharcoalMuted else MatteGlassTokens.CharcoalPrimary
                        )
                        if (draftDelayMs != 0L) {
                            Text(
                                text = "Reset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { draftDelayMs = 0L }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("-0.5s" to -500L, "-0.1s" to -100L, "+0.1s" to 100L, "+0.5s" to 500L).forEach { (label, delta) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .livelyPress(pressedScale = 0.93f) {
                                    draftDelayMs = (draftDelayMs + delta).coerceIn(-30_000L, 30_000L)
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.FieldGlassBg)
                                .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                        }
                    }
                }

                Text(
                    text = if (hasExternalSub) {
                        "Negative (-) shows subtitles earlier • Positive (+) shows them later"
                    } else {
                        "Tap Done to apply • Full +/- sync works best with local or online .srt/.vtt"
                    },
                    fontSize = 11.sp,
                    color = MatteGlassTokens.CharcoalMuted
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.CardGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Text Size", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Small" to 0.8f, "Medium" to 1.0f, "Large" to 1.25f, "XL" to 1.5f).forEach { (label, scale) ->
                        val active = draftScale == scale
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .livelyPress(pressedScale = 0.94f) { draftScale = scale }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.CardGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Track Source", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .livelyPress(pressedScale = 0.94f, onClick = onSelectOff)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedLabel == "Off") MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Turn Off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedLabel == "Off") MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .livelyPress(pressedScale = 0.94f, onClick = onPickLocal)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MatteGlassTokens.FieldGlassBg)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.FolderOpen, null, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(15.dp))
                            Text("Local .srt/.vtt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                        }
                    }
                }

                if (embeddedTextGroups.isNotEmpty()) {
                    embeddedTextGroups.forEachIndexed { idx, group ->
                        val fmt = group.getTrackFormat(0)
                        val trackLabel = fmt.label ?: fmt.language ?: "Track ${idx + 1}"
                        val active = trackLabel == selectedLabel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .livelyPress(pressedScale = 0.97f) { onSelectEmbeddedTrack(group, trackLabel) }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MatteGlassTokens.FieldGlassBg else MatteGlassTokens.SubtleSurface)
                                .border(0.75.dp, if (active) SolidColor(MatteGlassTokens.CharcoalPrimary) else MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(trackLabel, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = MatteGlassTokens.CharcoalPrimary)
                            if (active) Icon(Icons.Default.Check, null, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.CardGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Search Online Subtitles", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("English" to "eng", "Arabic" to "ara", "Kurdish" to "kur").forEach { (name, code) ->
                        val selected = subLang == code
                        Box(
                            modifier = Modifier
                                .livelyPress(pressedScale = 0.93f) { subLang = code }
                                .clip(CircleShape)
                                .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.FieldGlassBg)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalMuted
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Movie or show name", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .livelyPress(pressedScale = 0.92f) {
                                scope.launch {
                                    searching = true
                                    subResults = try {
                                        OnlineSubtitleService.searchSubtitles(searchQuery, subLang)
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                    searching = false
                                }
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CharcoalPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, "Search", tint = MatteGlassTokens.CanvasWhite)
                    }
                }

                if (searching) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MatteGlassTokens.CharcoalPrimary)
                    }
                } else if (subResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subResults.take(10).forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .livelyPress(pressedScale = 0.97f) { onDownloadAndApplyOnline(sub) }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MatteGlassTokens.FieldGlassBg)
                                    .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sub.language} • ${sub.fileName}",
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MatteGlassTokens.CharcoalPrimary
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .livelyPress(pressedScale = 0.965f) { onSaveAndDone(draftScale, draftDelayMs) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(MatteGlassTokens.CharcoalPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("Save & Done", color = MatteGlassTokens.CanvasWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PlayerErrorCard(
    err: String,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.86f))
            .border(0.85.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Playback Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(err, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .livelyPress(pressedScale = 0.92f, onClick = onRetry)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .livelyPress(pressedScale = 0.92f, onClick = onOpenExternal)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("External Player", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    else String.format(Locale.US, "%02d:%02d", mins, secs)
}

private fun formatDelay(delayMs: Long): String {
    val sec = delayMs / 1000f
    return if (delayMs > 0L) String.format(Locale.US, "+%.1fs", sec)
    else String.format(Locale.US, "%.1fs", sec)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaDescriptionSheet(
    item: MediaItem,
    isLive: Boolean,
    detailInfo: MediaDetailInfo,
    episodeCount: Int,
    videoResolution: String,
    videoCodec: String,
    audioInfo: String,
    bitrateInfo: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MatteGlassTokens.SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.75.dp, color = MatteGlassTokens.HairlineDivider)

            Text(
                text = item.name,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MatteGlassTokens.CharcoalPrimary
            )

            val statItems = if (isLive) {
                listOf(
                    "Status" to "LIVE",
                    "Quality" to videoResolution.ifBlank { "Stream" },
                    "Category" to item.group.ifBlank { "Live TV" }
                )
            } else {
                listOfNotNull(
                    detailInfo.rating.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }?.let { "Rating" to "★ $it" }
                        ?: item.badge.takeIf { it.isNotBlank() }?.let { "Info" to it },
                    detailInfo.releaseYear.takeIf { it.isNotBlank() }?.let { "Year" to it },
                    detailInfo.duration.takeIf { it.isNotBlank() }?.let { "Duration" to it }
                        ?: episodeCount.takeIf { it > 0 }?.let { "Episodes" to "$it" }
                ).ifEmpty {
                    listOf("Category" to item.group.ifBlank { "Video" })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statItems.take(3).forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CardGlassBg)
                            .border(0.8.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatteGlassTokens.CharcoalPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!isLive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (detailInfo.genre.isNotBlank()) {
                        Text(
                            text = detailInfo.genre,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatteGlassTokens.CharcoalPrimary
                        )
                    }
                    Text(
                        text = detailInfo.plot.ifBlank { "No description available for this title." },
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = if (detailInfo.plot.isNotBlank()) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CharcoalMuted
                    )
                }
            }

            val techRows = listOfNotNull(
                item.group.takeIf { it.isNotBlank() }?.let { "Group" to it },
                videoResolution.takeIf { it.isNotBlank() }?.let { "Resolution" to it },
                videoCodec.takeIf { it.isNotBlank() }?.let { "Video Codec" to it },
                audioInfo.takeIf { it.isNotBlank() }?.let { "Audio" to it },
                bitrateInfo.takeIf { it.isNotBlank() }?.let { "Bitrate" to it }
            )
            if (techRows.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MatteGlassTokens.CardGlassBg)
                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Stream Info",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary
                    )
                    techRows.forEach { (k, v) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(k, fontSize = 12.sp, color = MatteGlassTokens.CharcoalMuted)
                            Text(
                                text = v,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
