package com.aethertv.v2.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object SubtitleShiftHelper {
    private val timeRegex = Regex("""(\d{1,2}:)?(\d{2}):(\d{2})([,.])(\d{3})""")

    /**
     * Shifts timestamps ONLY on cue timing lines (lines containing "-->"),
     * leaving dialogue text untouched even if it contains time-like strings.
     */
    fun shiftSubtitleText(rawText: String, delayMs: Long): String {
        if (delayMs == 0L) return rawText
        return rawText.lines().joinToString("\n") { line ->
            if (!line.contains("-->")) {
                line
            } else {
                timeRegex.replace(line) { m ->
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
            }
        }
    }

    fun isWebVttContent(rawText: String): Boolean {
        return rawText.trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith("WEBVTT")
    }

    suspend fun detectIsWebVtt(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (uri.toString().endsWith(".vtt", ignoreCase = true)) return@withContext true
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0) ?: ""
                        if (name.endsWith(".vtt", ignoreCase = true)) return@withContext true
                    }
                }
            }
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val head = reader.readLine() ?: ""
                isWebVttContent(head)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun shiftSubtitleFile(
        context: Context,
        sourceUri: Uri,
        delayMs: Long
    ): Pair<Uri, Boolean>? = withContext(Dispatchers.IO) {
        try {
            val rawText = context.contentResolver.openInputStream(sourceUri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return@withContext null
            val isVtt = isWebVttContent(rawText) || detectIsWebVtt(context, sourceUri)
            val shifted = shiftSubtitleText(rawText, delayMs)
            val dir = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }
            val ext = if (isVtt) "vtt" else "srt"
            // Overwrite fixed slot to prevent unbounded subtitle cache accumulation
            val outFile = File(dir, "shifted_current.$ext")
            val tmpFile = File(dir, "shifted_current.$ext.tmp")
            tmpFile.writeText(shifted, Charsets.UTF_8)
            if (!tmpFile.renameTo(outFile)) {
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }
            Uri.fromFile(outFile) to isVtt
        } catch (_: Exception) {
            null
        }
    }
}
