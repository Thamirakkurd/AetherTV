package com.aethertv.v2.data

import android.Manifest
import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.GZIPInputStream

data class SavedServer(
    val id: String,
    val name: String,
    val mode: String, // "xtream" or "m3u"
    val host: String = "",
    val user: String = "",
    val pass: String = "",
    val m3uUrl: String = ""
)

data class OnlineSubtitleResult(
    val id: String,
    val fileName: String,
    val language: String,
    val downloadUrl: String
)

sealed interface SubtitleSearchOutcome {
    data class Success(val results: List<OnlineSubtitleResult>) : SubtitleSearchOutcome
    data class Error(val message: String) : SubtitleSearchOutcome
}

data class DownloadedMediaFile(
    val displayName: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val playUri: Uri,
    val file: File? = null,
    val mediaStoreUri: Uri? = null
)

enum class DownloadMode(val label: String) {
    ASK("Ask every time"),
    BUILTIN("Built-in Downloader"),
    EXTERNAL("External (1DM+ / App)")
}

object ServerStore {
    fun encode(servers: List<SavedServer>): String {
        val arr = JSONArray()
        for (s in servers) {
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("mode", s.mode)
                put("host", s.host)
                put("user", s.user)
                put("pass", s.pass)
                put("m3uUrl", s.m3uUrl)
            })
        }
        return arr.toString()
    }

    fun decode(raw: String?): List<SavedServer> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<SavedServer>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", UUID.randomUUID().toString())
                val name = o.optString("name", "Server ${i + 1}")
                val mode = o.optString("mode", "")
                if (mode.isNotBlank()) {
                    list.add(
                        SavedServer(
                            id = id,
                            name = name,
                            mode = mode,
                            host = o.optString("host", ""),
                            user = o.optString("user", ""),
                            pass = o.optString("pass", ""),
                            m3uUrl = o.optString("m3uUrl", "")
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object MediaDownloadHelper {
    private fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim().ifEmpty { "AetherTV_Video" }
    }

    fun startBuiltInDownload(context: Context, url: String, title: String) {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            Toast.makeText(context, "Only HTTP/HTTPS streams can be downloaded", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val cleanName = sanitizeFileName(title)
            val ext = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "")
                ?.takeIf { it.length in 2..5 } ?: "mp4"
            val fileName = if (cleanName.endsWith(".$ext", ignoreCase = true)) cleanName else "$cleanName.$ext"

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading via AetherTV")
                .addRequestHeader("User-Agent", XtreamClient.USER_AGENT)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val canWritePublic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

            val destLabel = if (canWritePublic) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AetherTV/$fileName")
                "Downloads/AetherTV/$fileName"
            } else {
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                "App Downloads/$fileName"
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Downloading to $destLabel", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun startExternalDownload(context: Context, url: String, title: String) {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            Toast.makeText(context, "Only HTTP/HTTPS streams can be downloaded", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanName = sanitizeFileName(title)
        val uri = Uri.parse(url)
        val idmPackages = listOf(
            "idm.internet.download.manager.plus",
            "idm.internet.download.manager",
            "com.dv.adm",
            "com.dv.adm.pay"
        )
        for (pkg in idmPackages) {
            try {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(pkg)
                        putExtra("title", cleanName)
                        putExtra("filename", cleanName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
            }
        }
        try {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    putExtra("title", cleanName)
                    putExtra("filename", cleanName)
                },
                "Download $title with"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "No external downloader found", Toast.LENGTH_SHORT).show()
        }
    }

    fun getShareableUri(context: Context, uriString: String): Pair<Uri, Boolean> {
        val parsed = Uri.parse(uriString)
        if (parsed.scheme == "file") {
            val path = parsed.path
            if (!path.isNullOrBlank()) {
                return try {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    )
                    contentUri to true
                } catch (_: Exception) {
                    parsed to false
                }
            }
        }
        return parsed to (parsed.scheme == "content")
    }

    suspend fun listDownloadedFilesAsync(context: Context): List<DownloadedMediaFile> =
        withContext(Dispatchers.IO) {
            val byName = LinkedHashMap<String, DownloadedMediaFile>()

            // 1. MediaStore.Downloads on API 29+ (Scoped Storage safe)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val projection = arrayOf(
                        MediaStore.Downloads._ID,
                        MediaStore.Downloads.DISPLAY_NAME,
                        MediaStore.Downloads.SIZE,
                        MediaStore.Downloads.DATE_MODIFIED,
                        MediaStore.Downloads.RELATIVE_PATH
                    )
                    val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
                    val selectionArgs = arrayOf("%AetherTV%")
                    context.contentResolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                    )?.use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                        val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                        val modIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idIdx)
                            val name = cursor.getString(nameIdx) ?: continue
                            val size = cursor.getLong(sizeIdx).coerceAtLeast(0L)
                            val modMs = cursor.getLong(modIdx) * 1000L
                            val contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                            byName[name] = DownloadedMediaFile(
                                displayName = name.substringBeforeLast('.').ifBlank { name },
                                sizeBytes = size,
                                lastModifiedMs = modMs,
                                playUri = contentUri,
                                file = null,
                                mediaStoreUri = contentUri
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }

            // 2. Public Downloads/AetherTV folder (API 26-28 + fallback)
            val dirs = listOfNotNull(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "AetherTV"
                ),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            )
            for (dir in dirs) {
                if (!dir.exists() || !dir.isDirectory) continue
                dir.listFiles()?.forEach { f ->
                    if (f.isFile && !byName.containsKey(f.name)) {
                        byName[f.name] = DownloadedMediaFile(
                            displayName = f.nameWithoutExtension.ifBlank { f.name },
                            sizeBytes = f.length().coerceAtLeast(0L),
                            lastModifiedMs = f.lastModified(),
                            playUri = Uri.fromFile(f),
                            file = f,
                            mediaStoreUri = null
                        )
                    }
                }
            }

            byName.values.sortedByDescending { it.lastModifiedMs }
        }

    suspend fun deleteDownloadedFile(context: Context, entry: DownloadedMediaFile): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (entry.mediaStoreUri != null) {
                    val deleted = context.contentResolver.delete(entry.mediaStoreUri, null, null) > 0
                    if (deleted) return@withContext true
                }
                entry.file?.delete() ?: false
            } catch (_: Exception) {
                false
            }
        }
}

object OnlineSubtitleService {
    suspend fun searchSubtitles(query: String, lang: String = "eng"): SubtitleSearchOutcome =
        withContext(Dispatchers.IO) {
            val cleanQuery = query
                .replace(Regex("\\(\\d{4}\\)"), "")
                .replace(Regex("S\\d+E\\d+.*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (cleanQuery.isBlank()) {
                return@withContext SubtitleSearchOutcome.Error("Enter a movie or series title to search.")
            }

            try {
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8").replace("+", "%20")
                val endpoint = "https://rest.opensubtitles.org/search/query-$encoded/sublanguageid-$lang"
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "TemporaryUserAgent")
                    .header("Accept", "application/json")
                    .build()

                XtreamClient.httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext SubtitleSearchOutcome.Error(
                            "Subtitle service unavailable (HTTP ${response.code}). Try loading a local .srt/.vtt file."
                        )
                    }
                    val body = response.body?.string()?.trim().orEmpty()
                    if (!body.startsWith("[")) {
                        return@withContext SubtitleSearchOutcome.Error(
                            "Subtitle provider returned an unexpected response. Try again or load a local subtitle."
                        )
                    }
                    val arr = JSONArray(body)
                    val results = ArrayList<OnlineSubtitleResult>(arr.length().coerceAtMost(25))
                    for (i in 0 until arr.length().coerceAtMost(25)) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val subUrl = obj.optString("SubDownloadLink", "").trim()
                        val fileName = obj.optString("SubFileName", "Subtitle ${i + 1}").trim()
                        val language = obj.optString("LanguageName", lang.uppercase()).trim()
                        val rawId = obj.optString("IDSubtitleFile", "$i")
                        val safeId = rawId.replace(Regex("[^a-zA-Z0-9_-]"), "").ifEmpty { "sub_$i" }
                        if (subUrl.startsWith("http://", ignoreCase = true) || subUrl.startsWith("https://", ignoreCase = true)) {
                            results.add(
                                OnlineSubtitleResult(
                                    id = safeId,
                                    fileName = fileName,
                                    language = language,
                                    downloadUrl = subUrl
                                )
                            )
                        }
                    }
                    SubtitleSearchOutcome.Success(results)
                }
            } catch (e: Exception) {
                SubtitleSearchOutcome.Error("Search failed: ${e.message ?: "network error"}")
            }
        }

    suspend fun downloadSubtitleToCache(context: Context, sub: OnlineSubtitleResult): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }
                val safeId = sub.id.replace(Regex("[^a-zA-Z0-9_-]"), "").ifEmpty { "online" }
                val ext = if (sub.fileName.endsWith(".vtt", ignoreCase = true)) "vtt" else "srt"
                val outFile = File(dir, "sub_$safeId.$ext")
                val tmpFile = File(dir, "sub_$safeId.$ext.tmp")

                val request = Request.Builder()
                    .url(sub.downloadUrl)
                    .header("User-Agent", "TemporaryUserAgent")
                    .build()

                XtreamClient.httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val rawStream = response.body?.byteStream() ?: return@withContext null
                    val contentType = response.header("Content-Type").orEmpty()
                    val isGzipped = sub.downloadUrl.endsWith(".gz", ignoreCase = true) ||
                        contentType.contains("gzip", ignoreCase = true)
                    val input = if (isGzipped) GZIPInputStream(rawStream) else rawStream
                    input.use { src ->
                        tmpFile.outputStream().use { dst -> src.copyTo(dst) }
                    }
                }
                if (tmpFile.length() == 0L) {
                    tmpFile.delete()
                    return@withContext null
                }
                if (!tmpFile.renameTo(outFile)) {
                    tmpFile.copyTo(outFile, overwrite = true)
                    tmpFile.delete()
                }
                Uri.fromFile(outFile)
            } catch (_: Exception) {
                null
            }
        }
}
