package com.aethertv.v2.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aethertv.v2.data.ContentType
import com.aethertv.v2.data.MediaItem
import com.aethertv.v2.data.OnlineSubtitleResult
import com.aethertv.v2.data.OnlineSubtitleService
import com.aethertv.v2.ui.theme.MatteGlassTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var embeddedTextGroups by remember { mutableStateOf<List<Tracks.Group>>(emptyList()) }
    var selectedSubtitleLabel by remember { mutableStateOf("Auto") }
    var subtitleFontScale by remember { mutableFloatStateOf(1.0f) }
    var externalSubtitleUri by remember { mutableStateOf<Uri?>(null) }
    var subSearchQuery by remember { mutableStateOf(item.name) }
    var subLang by remember { mutableStateOf("eng") }
    var subResults by remember { mutableStateOf<List<OnlineSubtitleResult>>(emptyList()) }
    var searchingSubs by remember { mutableStateOf(false) }

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
            AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom"
        )
    }

    val exoPlayer = remember(item.streamUrl) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AetherTV/2.0 (Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
            .apply {
                setMediaItem(ExoMediaItem.fromUri(item.streamUrl))
                prepare()
                playWhenReady = true
            }
    }

    fun applyExternalSubtitle(subUri: Uri, label: String) {
        val resumePos = exoPlayer.currentPosition
        val mimeType = if (subUri.toString().endsWith(".vtt", ignoreCase = true)) {
            MimeTypes.TEXT_VTT
        } else {
            MimeTypes.APPLICATION_SUBRIP
        }
        val subConfig = ExoMediaItem.SubtitleConfiguration.Builder(subUri)
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
        exoPlayer.setMediaItem(newMediaItem, resumePos)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    val localSubtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            applyExternalSubtitle(uri, "Local File")
            showSubtitleDialog = false
            Toast.makeText(context, "Loaded local subtitle", Toast.LENGTH_SHORT).show()
        }
    }

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

            override fun onTracksChanged(tracks: Tracks) {
                val textGroups = ArrayList<Tracks.Group>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        textGroups.add(group)
                    }
                    if (group.isSelected && group.length > 0) {
                        val format = group.getTrackFormat(0)
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            val fps = if (format.frameRate > 0f) " ${format.frameRate.toInt()}fps" else ""
                            val codecName = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                            videoCodec = "$codecName$fps".trim()
                            if (format.bitrate > 0) {
                                bitrateInfo = String.format("%.1f Mbps", format.bitrate / 1_000_000f)
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
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
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
                view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * subtitleFontScale)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && errorText == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.Center)
            )
        }

        errorText?.let { err ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Playback Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(err, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable {
                                errorText = null
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable(onClick = onOpenExternal)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("External Player", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (controlsVisible && !isInPipMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }

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
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable { showSubtitleDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ClosedCaption, contentDescription = "Subtitles", tint = Color.White, modifier = Modifier.size(19.dp))
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            isLandscape = !isLandscape
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClick = onEnterPip),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClick = onOpenExternal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "External Player", tint = Color.White, modifier = Modifier.size(18.dp))
                }
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
                        Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
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
                        Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
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
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.contentType == ContentType.LIVE) {
                            "● LIVE  •  CC: $selectedSubtitleLabel"
                        } else {
                            "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect", tint = Color.White, modifier = Modifier.size(15.dp))
                        Text(resizeModes[resizeModeIndex].second, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showSubtitleDialog) {
        Dialog(onDismissRequest = { showSubtitleDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Subtitles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteGlassTokens.CharcoalPrimary
                )

                // Font size selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Text Size", fontSize = 13.sp, color = MatteGlassTokens.CharcoalSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("S" to 0.8f, "M" to 1.0f, "L" to 1.3f).forEach { (label, scale) ->
                            val active = subtitleFontScale == scale
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CanvasWhite)
                                    .clickable { subtitleFontScale = scale }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MatteGlassTokens.CharcoalPrimary
                                )
                            }
                        }
                    }
                }

                // Embedded & Off tracks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MatteGlassTokens.CanvasWhite)
                            .clickable {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .build()
                                selectedSubtitleLabel = "Off"
                                showSubtitleDialog = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Turn Off", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MatteGlassTokens.CanvasWhite)
                            .clickable {
                                localSubtitlePicker.launch(arrayOf("*/*"))
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text("Local .srt/.vtt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MatteGlassTokens.CharcoalPrimary)
                        }
                    }
                }

                if (embeddedTextGroups.isNotEmpty()) {
                    Text("Stream Tracks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalSecondary)
                    embeddedTextGroups.forEachIndexed { idx, group ->
                        val fmt = group.getTrackFormat(0)
                        val trackLabel = fmt.label ?: fmt.language ?: "Track ${idx + 1}"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MatteGlassTokens.CanvasWhite)
                                .clickable {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                        .build()
                                    selectedSubtitleLabel = trackLabel
                                    showSubtitleDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(trackLabel, fontSize = 13.sp, color = MatteGlassTokens.CharcoalPrimary)
                        }
                    }
                }

                // Online subtitle search
                Text("Search Online Subtitles", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MatteGlassTokens.CharcoalSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("English" to "eng", "Arabic" to "ara", "Kurdish" to "kur").forEach { (name, code) ->
                        val selected = subLang == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MatteGlassTokens.CharcoalPrimary else MatteGlassTokens.CanvasWhite)
                                .clickable { subLang = code }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else MatteGlassTokens.CharcoalSecondary
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
                        value = subSearchQuery,
                        onValueChange = { subSearchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Movie or show name", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MatteGlassTokens.CharcoalPrimary)
                            .clickable {
                                scope.launch {
                                    searchingSubs = true
                                    subResults = try {
                                        OnlineSubtitleService.searchSubtitles(subSearchQuery, subLang)
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                    searchingSubs = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }

                if (searchingSubs) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                } else if (subResults.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 170.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subResults, key = { it.id }) { sub ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MatteGlassTokens.CanvasWhite)
                                    .clickable {
                                        scope.launch {
                                            val cachedUri = OnlineSubtitleService.downloadSubtitleToCache(context, sub)
                                            if (cachedUri != null) {
                                                applyExternalSubtitle(cachedUri, sub.language)
                                                showSubtitleDialog = false
                                                Toast.makeText(context, "Loaded: ${sub.fileName}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to download subtitle", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(10.dp)
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
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) String.format("%d:%02d:%02d", hrs, mins, secs) else String.format("%02d:%02d", mins, secs)
}
