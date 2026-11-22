package com.aethertv.v2.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.aethertv.v2.data.ContentType
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

private val LightYouTubeAmbientBg = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF14171D),
        0.15f to Color(0xFF282D37),
        0.30f to Color(0xFFDDE2EA),
        0.54f to Color(0xFFF1F3F6),
        1.00f to Color(0xFFF8F9FB)
    )
)

private val DarkYouTubeAmbientBg = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF06080C),
        0.18f to Color(0xFF121826),
        0.42f to Color(0xFF0A0E16),
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
        .setUserAgent("AetherTV/3.2 (Android)")
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
fun PlayerScreen(
    item: MediaItem,
    isInPipMode: Boolean,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val scope = rememberCoroutineScope()

    ImmersiveFullscreenEffect(enabled = !isInPipMode)

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var isLandscape by remember { mutableStateOf(false) }

    // Brightness / Volume gesture HUD
    var activeHud by remember { mutableStateOf<GestureHudType?>(null) }
    var hudValue by remember { mutableFloatStateOf(0.5f) }
    var hudStamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hudStamp) {
        if (activeHud != null) {
            delay(1100)
            activeHud = null
        }
    }

    var videoResolution by remember { mutableStateOf("") }
    var videoCodec by remember { mutableStateOf("") }
    var audioInfo by remember { mutableStateOf("") }
    var bitrateInfo by remember { mutableStateOf("") }

    // Subtitle state
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var embeddedTextGroups by remember { mutableStateOf<List<Tracks.Group>>(emptyList()) }
    var selectedSubtitleLabel by remember { mutableStateOf("Auto") }
    var subtitleFontScale by remember { mutableFloatStateOf(1.0f) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var externalSubtitleUri by remember { mutableStateOf<Uri?>(null) }
    var delayedEmbeddedText by remember { mutableStateOf("") }

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
            AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom"
        )
    }

    val exoPlayer = remember(item.streamUrl) {
        createAetherExoPlayer(context).apply {
            setMediaItem(ExoMediaItem.fromUri(item.streamUrl))
            prepare()
            playWhenReady = true
        }
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

    val useEmbeddedDelayOverlay = externalSubtitleUri == null && subtitleDelayMs > 0L

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
                if (externalSubtitleUri == null && subtitleDelayMs > 0L) {
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
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) textGroups.add(group)
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
                        }
                    }
                }
                embeddedTextGroups = textGroups
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorText = error.localizedMessage ?: "Stream playback failed"
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

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4500)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible }
                )
            }
            .pointerInput(isInPipMode) {
                if (isInPipMode) return@pointerInput
                var isLeftSide = true
                var level = 0.5f
                var lastAppliedVol = -1
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

                detectVerticalDragGestures(
                    onDragStart = { startOffset ->
                        isLeftSide = startOffset.x < size.width / 2f
                        if (isLeftSide) {
                            level = readCurrentBrightness(context, activity)
                            activeHud = GestureHudType.BRIGHTNESS
                        } else {
                            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            lastAppliedVol = cur
                            level = cur.toFloat() / maxVol.toFloat()
                            activeHud = GestureHudType.VOLUME
                        }
                        hudValue = level
                        hudStamp++
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val span = (size.height * 0.65f).coerceAtLeast(240f)
                        level = (level - dragAmount / span).coerceIn(0f, 1f)
                        hudValue = level
                        hudStamp++
                        if (isLeftSide) {
                            applyWindowBrightness(activity, level)
                        } else {
                            val targetVol = (level * maxVol).roundToInt().coerceIn(0, maxVol)
                            if (targetVol != lastAppliedVol) {
                                lastAppliedVol = targetVol
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = resizeModes[resizeModeIndex].first
                view.subtitleView?.alpha = if (useEmbeddedDelayOverlay) 0f else 1f
                view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * subtitleFontScale)
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize()
        )

        if (useEmbeddedDelayOverlay && delayedEmbeddedText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = if (controlsVisible) 96.dp else 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = delayedEmbeddedText,
                    color = Color.White,
                    fontSize = (17f * subtitleFontScale).sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (isBuffering && errorText == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(42.dp).align(Alignment.Center)
            )
        }

        errorText?.let { err ->
            PlayerErrorCard(
                err = err,
                onRetry = {
                    errorText = null
                    isBuffering = true
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                },
                onOpenExternal = onOpenExternal,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Brightness / Volume Gesture HUD Pill
        AnimatedVisibility(
            visible = activeHud != null && !isInPipMode,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            activeHud?.let { hudType ->
                GestureFeedbackPill(type = hudType, progress = hudValue)
            }
        }

        // Smooth fade playback controls
        AnimatedVisibility(
            visible = controlsVisible && !isInPipMode,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.74f), Color.Transparent)))
                        .displayCutoutPadding()
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerOverlayIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val stats = listOfNotNull(
                            videoResolution.takeIf { it.isNotBlank() },
                            videoCodec.takeIf { it.isNotBlank() },
                            audioInfo.takeIf { it.isNotBlank() },
                            bitrateInfo.takeIf { it.isNotBlank() }
                        )
                        Text(
                            text = if (stats.isNotEmpty()) stats.joinToString(" • ") else item.group,
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    PlayerOverlayIconButton(Icons.Default.ClosedCaption, "Subtitles") { showSubtitleSheet = true }
                    PlayerOverlayIconButton(Icons.Default.ScreenRotation, "Rotate") {
                        isLandscape = !isLandscape
                        activity?.requestedOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                    PlayerOverlayIconButton(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip)
                    PlayerOverlayIconButton(Icons.Default.OpenInNew, "External Player", onOpenExternal)
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.contentType != ContentType.LIVE) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .livelyPress(pressedScale = 0.88f) {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                }
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .livelyPress(pressedScale = 0.88f) {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    if (item.contentType != ContentType.LIVE) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .livelyPress(pressedScale = 0.88f) {
                                    exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                                }
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    if (item.contentType != ContentType.LIVE && durationMs > 0L) {
                        Slider(
                            value = currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                            onValueChange = { exoPlayer.seekTo(it.toLong()) },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFEF4444),
                                inactiveTrackColor = Color.White.copy(alpha = 0.30f)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val delayTag = if (subtitleDelayMs != 0L) " (${formatDelay(subtitleDelayMs)})" else ""
                        Text(
                            text = "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}  •  CC: $selectedSubtitleLabel$delayTag",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        Row(
                            modifier = Modifier
                                .livelyPress(pressedScale = 0.92f) {
                                    resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(0.75.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, "Aspect", tint = Color.White, modifier = Modifier.size(15.dp))
                            Text(resizeModes[resizeModeIndex].second, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSubtitleSheet) {
        SubtitleSettingsSheet(
            mediaTitle = item.name,
            embeddedTextGroups = embeddedTextGroups,
            selectedLabel = selectedSubtitleLabel,
            fontScale = subtitleFontScale,
            delayMs = subtitleDelayMs,
            hasExternalSub = externalSubtitleUri != null,
            onDismiss = { showSubtitleSheet = false },
            onPickLocal = { localSubtitlePicker.launch(arrayOf("*/*")) },
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
                scope.launch {
                    val cachedUri = OnlineSubtitleService.downloadSubtitleToCache(context, sub)
                    if (cachedUri != null) {
                        applyExternalSubtitle(cachedUri, sub.language, subtitleDelayMs)
                        Toast.makeText(context, "Loaded: ${sub.fileName}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to download subtitle", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSaveAndDone = { newScale, newDelayMs ->
                val delayChanged = newDelayMs != subtitleDelayMs
                subtitleFontScale = newScale
                subtitleDelayMs = newDelayMs
                if (delayChanged) {
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun YouTubeLivePlayerOverlay(
    channel: MediaItem,
    allChannels: List<MediaItem>,
    categories: List<String>,
    selectedCategory: String,
    isLoadingCategory: Boolean,
    favoriteIds: Set<String>,
    isMiniPlayer: Boolean,
    isInPipMode: Boolean,
    barsVisible: Boolean,
    onSelectCategory: (String) -> Unit,
    onSwitchChannel: (MediaItem) -> Unit,
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
    val mainHazeState = LocalHazeState.current
    val feedHazeState = remember { HazeState() }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // Drag offsets for tactile minimize/maximize gesture tracking
    var portraitDragY by remember { mutableFloatStateOf(0f) }
    var miniDragY by remember { mutableFloatStateOf(0f) }
    val animatedPortraitDragY by animateFloatAsState(
        targetValue = portraitDragY,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
        label = "portraitDrag"
    )
    val animatedMiniDragY by animateFloatAsState(
        targetValue = miniDragY,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
        label = "miniDrag"
    )

    // Brightness / Volume gesture HUD
    var activeHud by remember { mutableStateOf<GestureHudType?>(null) }
    var hudValue by remember { mutableFloatStateOf(0.5f) }
    var hudStamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hudStamp) {
        if (activeHud != null) {
            delay(1100)
            activeHud = null
        }
    }

    // Immersive fullscreen system bar hiding ONLY when fullscreen & not mini/PiP
    ImmersiveFullscreenEffect(enabled = isFullscreen && !isMiniPlayer && !isInPipMode)

    var videoResolution by remember { mutableStateOf("") }
    var videoCodec by remember { mutableStateOf("") }
    var audioInfo by remember { mutableStateOf("") }
    var bitrateInfo by remember { mutableStateOf("") }

    // Subtitle state for live player
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var embeddedTextGroups by remember { mutableStateOf<List<Tracks.Group>>(emptyList()) }
    var selectedSubtitleLabel by remember { mutableStateOf("Auto") }
    var subtitleFontScale by remember { mutableFloatStateOf(1.0f) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
            AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom"
        )
    }

    val exoPlayer = remember { createAetherExoPlayer(context) }

    LaunchedEffect(channel.streamUrl) {
        errorText = null
        isBuffering = true
        videoResolution = ""
        videoCodec = ""
        audioInfo = ""
        bitrateInfo = ""
        exoPlayer.setMediaItem(ExoMediaItem.fromUri(channel.streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) errorText = null
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoResolution = "${videoSize.width}×${videoSize.height}"
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val textGroups = ArrayList<Tracks.Group>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) textGroups.add(group)
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
                        }
                    }
                }
                embeddedTextGroups = textGroups
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorText = error.localizedMessage ?: "Live stream failed"
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

    LaunchedEffect(isFullscreen, isMiniPlayer) {
        activity?.requestedOrientation = if (isFullscreen && !isMiniPlayer) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isMiniPlayer) {
        if (controlsVisible && isPlaying && !isMiniPlayer) {
            delay(4000)
            controlsVisible = false
        }
    }

    val currentIndex = remember(allChannels, channel.id) {
        allChannels.indexOfFirst { it.id == channel.id }
    }
    val nextChannel = remember(allChannels, currentIndex) {
        if (allChannels.size > 1 && currentIndex >= 0) allChannels[(currentIndex + 1) % allChannels.size]
        else allChannels.firstOrNull { it.id != channel.id }
    }
    val prevChannel = remember(allChannels, currentIndex) {
        if (allChannels.size > 1 && currentIndex >= 0) {
            allChannels[(currentIndex - 1 + allChannels.size) % allChannels.size]
        } else null
    }

    val quickChips = remember(categories, selectedCategory) {
        buildList {
            if (selectedCategory.isNotBlank()) add(selectedCategory)
            add("Favorites")
            categories.filter { it != selectedCategory && it != "Favorites" }.take(12).forEach { add(it) }
        }.distinct()
    }

    val feedChannels by produceState(
        initialValue = emptyList<MediaItem>(),
        allChannels,
        channel.id
    ) {
        value = withContext(Dispatchers.Default) {
            allChannels.filterNot { it.id == channel.id }.take(100)
        }
    }

    val statsLine = remember(videoResolution, videoCodec, audioInfo, bitrateInfo, channel.group) {
        val parts = listOfNotNull(
            channel.group.takeIf { it.isNotBlank() },
            videoResolution.takeIf { it.isNotBlank() },
            videoCodec.takeIf { it.isNotBlank() },
            bitrateInfo.takeIf { it.isNotBlank() }
        )
        parts.joinToString(" • ")
    }

    BackHandler(enabled = !isMiniPlayer && isFullscreen) {
        isFullscreen = false
    }

    // 1. DOCKED MINI-PLAYER (with swipe-up-to-maximize + real-time blur)
    if (isMiniPlayer && !isInPipMode) {
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomOffset: Dp = if (barsVisible) navInset + 64.dp else navInset + 10.dp
        val miniShape = RoundedCornerShape(14.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = bottomOffset)
                    .graphicsLayer {
                        translationY = animatedMiniDragY
                    }
                    .fillMaxWidth()
                    .height(64.dp)
                    .liquidGlassSurface(
                        hazeState = mainHazeState,
                        shape = miniShape,
                        tintColor = MatteGlassTokens.NavGlassBg,
                        fallbackColor = MatteGlassTokens.NavGlassFallback,
                        blurRadius = 26.dp,
                        borderWidth = 0.9.dp,
                        borderBrush = MatteGlassTokens.SpecularBorderBrush
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onToggleMinimize(false) })
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (miniDragY < -72f) {
                                    miniDragY = 0f
                                    onToggleMinimize(false)
                                } else {
                                    miniDragY = 0f
                                }
                            },
                            onDragCancel = { miniDragY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                miniDragY = (miniDragY + dragAmount).coerceIn(-160f, 36f)
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(114.dp)
                        .fillMaxHeight()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { it.player = exoPlayer },
                        onRelease = { it.player = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MatteGlassTokens.CharcoalPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = channel.group.ifBlank { "LIVE" },
                            fontSize = 11.sp,
                            color = MatteGlassTokens.CharcoalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(24.dp)
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
        return
    }

    // 2. FULLSCREEN IMMERSIVE LIVE PLAYER (3-zone gestures: Left=Brightness, Mid-Down=Minimize, Right=Volume)
    if (isFullscreen || isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                }
                .pointerInput(isInPipMode) {
                    if (isInPipMode) return@pointerInput
                    var zone = 1 // 0=left, 1=middle, 2=right
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
                            if (zone == 1 && middleDragDown > 96f) {
                                isFullscreen = false
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
                                1 -> {
                                    middleDragDown += dragAmount
                                }
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
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = resizeModes[resizeModeIndex].first
                    view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * subtitleFontScale)
                },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && errorText == null) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp).align(Alignment.Center))
            }

            errorText?.let { err ->
                PlayerErrorCard(
                    err = err,
                    onRetry = {
                        errorText = null
                        isBuffering = true
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    },
                    onOpenExternal = { onOpenExternal(channel) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            AnimatedVisibility(
                visible = activeHud != null && !isInPipMode,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp)
            ) {
                activeHud?.let { hudType -> GestureFeedbackPill(type = hudType, progress = hudValue) }
            }

            AnimatedVisibility(
                visible = controlsVisible && !isInPipMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.74f), Color.Transparent)))
                            .displayCutoutPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayerOverlayIconButton(Icons.Default.KeyboardArrowDown, "Exit fullscreen") { isFullscreen = false }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (statsLine.isNotBlank()) {
                                Text(statsLine, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        PlayerOverlayIconButton(Icons.Default.ClosedCaption, "Subtitles") { showSubtitleSheet = true }
                        PlayerOverlayIconButton(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip)
                        PlayerOverlayIconButton(Icons.Default.OpenInNew, "External") { onOpenExternal(channel) }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (prevChannel != null) {
                            PlayerOverlayIconButton(Icons.Default.SkipPrevious, "Previous channel") { onSwitchChannel(prevChannel) }
                        }
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .livelyPress(pressedScale = 0.88f) {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.Black, modifier = Modifier.size(34.dp))
                        }
                        if (nextChannel != null) {
                            PlayerOverlayIconButton(Icons.Default.SkipNext, "Next channel") { onSwitchChannel(nextChannel) }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f))))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Spacer(Modifier.width(6.dp))
                            Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier
                                    .livelyPress(pressedScale = 0.92f) {
                                        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AspectRatio, "Aspect", tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(resizeModes[resizeModeIndex].second, color = Color.White, fontSize = 12.sp)
                            }
                            PlayerOverlayIconButton(Icons.Default.FullscreenExit, "Exit fullscreen") { isFullscreen = false }
                        }
                    }
                }
            }
        }
    } else {
        // 3. PORTRAIT YOUTUBE-STYLE LIVE PLAYER + SCROLLABLE FEED + FLOATING BLUR CATEGORY SWITCHER
        val isFav = favoriteIds.contains(channel.id)
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val ambientBg = if (MatteGlassTokens.isDark) DarkYouTubeAmbientBg else LightYouTubeAmbientBg
        val primaryText = MatteGlassTokens.CharcoalPrimary
        val mutedText = MatteGlassTokens.CharcoalMuted
        val activeCategoryLabel = selectedCategory.ifBlank { channel.group.ifBlank { "All Channels" } }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedPortraitDragY.coerceAtLeast(0f)
                    val shrink = (animatedPortraitDragY / 2400f).coerceIn(0f, 0.08f)
                    scaleX = 1f - shrink
                    scaleY = 1f - shrink
                }
                .background(ambientBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0C10))
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                )

                // Pinned 16:9 Video Box (swipe down -> minimize, swipe up -> fullscreen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    when {
                                        portraitDragY > 110f -> {
                                            portraitDragY = 0f
                                            onToggleMinimize(true)
                                        }
                                        portraitDragY < -90f -> {
                                            portraitDragY = 0f
                                            isFullscreen = true
                                        }
                                        else -> portraitDragY = 0f
                                    }
                                },
                                onDragCancel = { portraitDragY = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    portraitDragY = (portraitDragY + dragAmount).coerceIn(-160f, 340f)
                                }
                            )
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                            view.resizeMode = resizeModes[resizeModeIndex].first
                            view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 15f * subtitleFontScale)
                        },
                        onRelease = { it.player = null },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isBuffering && errorText == null) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(34.dp).align(Alignment.Center)
                        )
                    }

                    errorText?.let { err ->
                        PlayerErrorCard(
                            err = err,
                            onRetry = {
                                errorText = null
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            },
                            onOpenExternal = { onOpenExternal(channel) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(tween(160)),
                        exit = fadeOut(tween(180)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onToggleMinimize(true) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onEnterPip, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.PictureInPictureAlt, "PiP", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { showSubtitleSheet = true }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.ClosedCaption, "Subtitles", tint = Color.White, modifier = Modifier.size(21.dp))
                                    }
                                    IconButton(onClick = { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.AspectRatio, "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (prevChannel != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .livelyPress(pressedScale = 0.88f) { onSwitchChannel(prevChannel) }
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.48f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .livelyPress(pressedScale = 0.88f) {
                                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        }
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.56f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                if (nextChannel != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .livelyPress(pressedScale = 0.88f) { onSwitchChannel(nextChannel) }
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.48f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (videoResolution.isNotBlank()) "LIVE • $videoResolution" else "LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(onClick = { isFullscreen = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Fullscreen, "Fullscreen", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFFEF4444))
                    )
                }

                // Scrollable channel feed (hazeSource for the real-time blurred bottom Category Switcher bar)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .hazeSource(feedHazeState),
                    contentPadding = PaddingValues(bottom = navInset + 84.dp)
                ) {
                    item(key = "yt_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = channel.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = statsLine.ifBlank { "Live TV Channel" },
                                    fontSize = 12.sp,
                                    color = mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MatteGlassTokens.CardGlassBg)
                                        .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, CircleShape),
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
                                        Icon(Icons.Default.LiveTv, null, tint = primaryText, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .livelyPress(pressedScale = 0.93f) { onToggleFavorite(channel) }
                                        .clip(CircleShape)
                                        .background(MatteGlassTokens.CharcoalPrimary)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = MatteGlassTokens.CanvasWhite,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (isFav) "Favorited" else "Favorite",
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
                                    item { YouTubeActionPill(Icons.Default.OpenInNew, "External") { onOpenExternal(channel) } }
                                    item { YouTubeActionPill(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip) }
                                    item { YouTubeActionPill(Icons.Default.ClosedCaption, "CC") { showSubtitleSheet = true } }
                                    item { YouTubeActionPill(Icons.Default.Fullscreen, "Full") { isFullscreen = true } }
                                }
                            }
                        }
                    }

                    // Quick category chips synced with active Live TV category
                    item(key = "yt_chips") {
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
            }

            // Floating bottom real-time blurred Liquid Glass CATEGORY SWITCHER bar (replaces "Next:")
            val barShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .livelyPress(pressedScale = 0.975f, depthShiftPx = 2f) { showCategorySheet = true }
                    .liquidGlassSurface(
                        hazeState = feedHazeState,
                        shape = barShape,
                        tintColor = MatteGlassTokens.NavGlassBg,
                        fallbackColor = MatteGlassTokens.NavGlassFallback,
                        blurRadius = 26.dp,
                        borderWidth = 0.9.dp,
                        borderBrush = MatteGlassTokens.SpecularBorderBrush
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MatteGlassTokens.FieldGlassBg)
                        .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Categories",
                        tint = MatteGlassTokens.CharcoalPrimary,
                        modifier = Modifier.size(18.dp)
                    )
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
                        text = "${allChannels.size} channels • Tap to switch category",
                        fontSize = 11.sp,
                        color = MatteGlassTokens.CharcoalMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Change Category",
                    tint = MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // Category Switcher Bottom Sheet
    if (showCategorySheet) {
        LiveCategoryPickerSheet(
            categories = categories,
            selectedCategory = selectedCategory,
            onDismiss = { showCategorySheet = false },
            onSelectCategory = { cat ->
                showCategorySheet = false
                onSelectCategory(cat)
            }
        )
    }

    if (showSubtitleSheet) {
        SubtitleSettingsSheet(
            mediaTitle = channel.name,
            embeddedTextGroups = embeddedTextGroups,
            selectedLabel = selectedSubtitleLabel,
            fontScale = subtitleFontScale,
            delayMs = subtitleDelayMs,
            hasExternalSub = false,
            onDismiss = { showSubtitleSheet = false },
            onPickLocal = {
                Toast.makeText(context, "Local subtitles are for Movies & Series", Toast.LENGTH_SHORT).show()
            },
            onSelectOff = {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                selectedSubtitleLabel = "Off"
            },
            onSelectEmbeddedTrack = { group, label ->
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                    .build()
                selectedSubtitleLabel = label
            },
            onDownloadAndApplyOnline = {},
            onSaveAndDone = { newScale, newDelay ->
                subtitleFontScale = newScale
                subtitleDelayMs = newDelay
                showSubtitleSheet = false
            }
        )
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
            .clip(CircleShape)
            .background(MatteGlassTokens.FieldGlassBg)
            .border(0.75.dp, MatteGlassTokens.SpecularBorderBrush, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = MatteGlassTokens.CharcoalPrimary, modifier = Modifier.size(15.dp))
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
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
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
                    .clip(CircleShape)
                    .background(MatteGlassTokens.CardGlassBg)
                    .border(0.85.dp, MatteGlassTokens.SpecularBorderBrush, CircleShape),
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
                    Text(
                        text = "Subtitles",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteGlassTokens.CharcoalPrimary
                    )
                    Text(
                        text = "Active: $selectedLabel",
                        fontSize = 12.sp,
                        color = MatteGlassTokens.CharcoalMuted
                    )
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

            // Subtitle Sync / Timing Delay
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

            // Text Size selector
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

            // Track Source
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

            // Online Subtitles Search
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
private fun PlayerOverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .livelyPress(pressedScale = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(0.75.dp, Color.White.copy(alpha = 0.24f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(19.dp))
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
