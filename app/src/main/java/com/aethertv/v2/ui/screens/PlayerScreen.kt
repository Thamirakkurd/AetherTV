package com.aethertv.v2.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.aethertv.v2.ui.theme.MatteGlassTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val YouTubeAmbientBg = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF14171D),
        0.14f to Color(0xFF282D37),
        0.28f to Color(0xFFDDE2EA),
        0.52f to Color(0xFFF1F3F6),
        1.00f to Color(0xFFF8F9FB)
    )
)

private val ChannelBannerBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF1A1F29), Color(0xFF2B3240), Color(0xFF161A22))
)

@OptIn(UnstableApi::class)
private fun createAetherExoPlayer(context: Context): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("AetherTV/3.0 (Android)")
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

@OptIn(UnstableApi::class)
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
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var isLandscape by remember { mutableStateOf(false) }

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
            .clickable { controlsVisible = !controlsVisible }
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

        if (controlsVisible && !isInPipMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
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
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
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
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size }
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

@OptIn(UnstableApi::class)
@Composable
fun YouTubeLivePlayerOverlay(
    channel: MediaItem,
    allChannels: List<MediaItem>,
    categories: List<String>,
    favoriteIds: Set<String>,
    isMiniPlayer: Boolean,
    isInPipMode: Boolean,
    barsVisible: Boolean,
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
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }

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

    // Filter chip state for YouTube "Next Channels" feed
    var feedFilter by remember(channel.id) { mutableStateOf("All") }

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

    val chipOptions = remember(channel.group, categories) {
        buildList {
            add("All")
            if (channel.group.isNotBlank()) add(channel.group)
            add("Favorites")
            categories.filter { it != "All" && it != "Favorites" && it != channel.group }.take(8).forEach { add(it) }
        }.distinct()
    }

    val feedChannels by produceState(
        initialValue = emptyList<MediaItem>(),
        allChannels,
        channel.id,
        feedFilter,
        favoriteIds
    ) {
        value = withContext(Dispatchers.Default) {
            val base = allChannels.filterNot { it.id == channel.id }
            when (feedFilter) {
                "All" -> base.take(80)
                "Favorites" -> base.filter { favoriteIds.contains(it.id) }.take(80)
                else -> base.filter { it.group.equals(feedFilter, ignoreCase = true) }.ifEmpty { base }.take(80)
            }
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

    if (isMiniPlayer && !isInPipMode) {
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomOffset: Dp = if (barsVisible) navInset + 57.dp else navInset + 8.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFAF4F6F9),
                border = BorderStroke(0.75.dp, Color(0x24181B20)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = bottomOffset)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onToggleMinimize(false) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
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
        }
        return
    }

    if (isFullscreen || isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { controlsVisible = !controlsVisible }
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

            if (controlsVisible && !isInPipMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
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
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
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
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size }
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
    } else {
        val isFav = favoriteIds.contains(channel.id)
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YouTubeAmbientBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Status bar dark backdrop for seamless YouTube top video look
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E1014))
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                )

                // Pinned 16:9 YouTube-style Live Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .clickable { controlsVisible = !controlsVisible }
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

                    if (controlsVisible) {
                        // Top bar inside 16:9 player
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

                        // Center Prev / Play-Pause / Next
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (prevChannel != null) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable { onSwitchChannel(prevChannel) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
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
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable { onSwitchChannel(nextChannel) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        // Bottom bar inside 16:9 player
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

                    // Thin YouTube red live line at bottom edge of video
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFFEF4444))
                    )
                }

                // Scrollable YouTube-style feed below the video
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = navInset + if (nextChannel != null) 78.dp else 20.dp)
                ) {
                    // Title, metadata & channel action pills
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
                                    color = Color(0xFF111317),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = statsLine.ifBlank { "Live TV Channel" },
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Channel avatar + Quick Action Pills (YouTube row)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Color(0x22000000), CircleShape),
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

                                Surface(
                                    shape = CircleShape,
                                    color = if (isFav) Color(0xFF111317) else Color(0xFF18181B),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onToggleFavorite(channel) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (isFav) "Favorited" else "Favorite",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        YouTubeActionPill(Icons.Default.OpenInNew, "External") { onOpenExternal(channel) }
                                    }
                                    item {
                                        YouTubeActionPill(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip)
                                    }
                                    item {
                                        YouTubeActionPill(Icons.Default.ClosedCaption, "CC") { showSubtitleSheet = true }
                                    }
                                    item {
                                        YouTubeActionPill(Icons.Default.Fullscreen, "Full") { isFullscreen = true }
                                    }
                                }
                            }
                        }
                    }

                    // Category Filter Chips (matching YouTube video)
                    item(key = "yt_chips") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chipOptions, key = { it }) { chip ->
                                val selected = chip == feedFilter
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) Color(0xFF111317) else Color(0x18111317),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { feedFilter = chip }
                                ) {
                                    Text(
                                        text = chip,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) Color.White else Color(0xFF18181B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // YouTube-style "Next Channels" full-width video cards
                    items(feedChannels, key = { "yt_ch_${it.id}" }, contentType = { "yt_channel_card" }) { nextItem ->
                        YouTubeChannelFeedCard(
                            channel = nextItem,
                            onClick = { onSwitchChannel(nextItem) },
                            onMoreClick = { onMoreClick(nextItem) }
                        )
                    }
                }
            }

            // Floating bottom "Next: ..." queue bar (matching YouTube video)
            if (nextChannel != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF5ECEFF4),
                    border = BorderStroke(0.75.dp, Color(0x22181B20)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSwitchChannel(nextChannel) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next: ${nextChannel.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatteGlassTokens.CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = nextChannel.group.ifBlank { "Live TV" },
                                fontSize = 11.sp,
                                color = MatteGlassTokens.CharcoalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Play Next",
                            tint = MatteGlassTokens.CharcoalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
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

@Composable
private fun YouTubeActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0x16111317),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, label, tint = Color(0xFF18181B), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
        }
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
            .clickable(onClick = onClick)
            .padding(bottom = 14.dp)
    ) {
        // 16:9 YouTube-style banner thumbnail
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

            // Bottom-right LIVE badge (like YouTube video duration badge)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC0B0D11))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Spacer(Modifier.width(5.dp))
                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Metadata row below thumbnail (Avatar + Title/Group + More)
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
                    .background(Color.White)
                    .border(1.dp, Color(0x1F000000), CircleShape),
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
                    color = Color(0xFF111317),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${channel.group.ifBlank { "Live TV" }} • Live stream",
                    fontSize = 12.sp,
                    color = Color(0xFF525866),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onMoreClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color(0xFF3F4552),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = Color(0xF7F5F7FA),
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
            // Header with Title + Save / Done button
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
                Surface(
                    shape = CircleShape,
                    color = MatteGlassTokens.CharcoalPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSaveAndDone(draftScale, draftDelayMs) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            // Subtitle Sync / Timing Delay (Forward & Backward)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE3E6EC))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
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
                                    color = Color(0xFF2563EB),
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEFF1F5))
                                    .border(0.75.dp, Color(0x22181B20), RoundedCornerShape(10.dp))
                                    .clickable { draftDelayMs = (draftDelayMs + delta).coerceIn(-30_000L, 30_000L) }
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
            }

            // Text Size selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE3E6EC))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) MatteGlassTokens.CharcoalPrimary else Color(0xFFEFF1F5))
                                    .clickable { draftScale = scale }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color.White else MatteGlassTokens.CharcoalPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Quick Track Controls (Off / Local / Embedded)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE3E6EC))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Track Source", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedLabel == "Off") MatteGlassTokens.CharcoalPrimary else Color(0xFFEFF1F5))
                                .clickable { onSelectOff() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Turn Off",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedLabel == "Off") Color.White else MatteGlassTokens.CharcoalPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF1F5))
                                .clickable { onPickLocal() }
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) Color(0xFFE9ECF1) else Color(0xFFF8F9FB))
                                    .border(0.75.dp, if (active) MatteGlassTokens.CharcoalPrimary else Color(0xFFE3E6EC), RoundedCornerShape(10.dp))
                                    .clickable { onSelectEmbeddedTrack(group, trackLabel) }
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
            }

            // Online Subtitles Search
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE3E6EC))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Search Online Subtitles", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("English" to "eng", "Arabic" to "ara", "Kurdish" to "kur").forEach { (name, code) ->
                            val selected = subLang == code
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) MatteGlassTokens.CharcoalPrimary else Color(0xFFEFF1F5))
                                    .clickable { subLang = code }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else MatteGlassTokens.CharcoalMuted
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(MatteGlassTokens.CharcoalPrimary)
                                .clickable {
                                    scope.launch {
                                        searching = true
                                        subResults = try {
                                            OnlineSubtitleService.searchSubtitles(searchQuery, subLang)
                                        } catch (_: Exception) {
                                            emptyList()
                                        }
                                        searching = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, "Search", tint = Color.White)
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF4F6F9))
                                        .border(0.75.dp, Color(0xFFE3E6EC), RoundedCornerShape(10.dp))
                                        .clickable { onDownloadAndApplyOnline(sub) }
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
            }

            // Full-width bottom Save & Done CTA
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MatteGlassTokens.CharcoalPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSaveAndDone(draftScale, draftDelayMs) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Save & Done", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
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
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
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
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Playback Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(err, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpenExternal)
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
