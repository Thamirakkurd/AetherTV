package com.aethertv.v2.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

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
        try {
            val cleanName = sanitizeFileName(title)
            val ext = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "")
                ?.takeIf { it.length in 2..5 } ?: "mp4"
            val fileName = if (cleanName.endsWith(".$ext", ignoreCase = true)) cleanName else "$cleanName.$ext"

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading via AetherTV")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AetherTV/$fileName")

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Downloading to Downloads/AetherTV/$fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun startExternalDownload(context: Context, url: String, title: String) {
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

    fun listDownloadedFiles(): List<File> {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "AetherTV"
        )
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

object OnlineSubtitleService {
    suspend fun searchSubtitles(query: String, lang: String = "eng"): List<OnlineSubtitleResult> =
        withContext(Dispatchers.IO) {
            val cleanQuery = query
                .replace(Regex("\\(\\d{4}\\)"), "")
                .replace(Regex("S\\d+E\\d+.*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (cleanQuery.isBlank()) return@withContext emptyList()

            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val endpoint = "https://rest.opensubtitles.org/search/query-$encoded/sublanguageid-$lang"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 12000
                setRequestProperty("User-Agent", "TemporaryUserAgent")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val results = ArrayList<OnlineSubtitleResult>(arr.length().coerceAtMost(25))
            for (i in 0 until arr.length().coerceAtMost(25)) {
                val obj = arr.optJSONObject(i) ?: continue
                val subUrl = obj.optString("SubDownloadLink", "")
                    .replace(".gz", "")
                    .trim()
                val fileName = obj.optString("SubFileName", "Subtitle ${i + 1}").trim()
                val language = obj.optString("LanguageName", lang.uppercase()).trim()
                val id = obj.optString("IDSubtitleFile", "$i")
                if (subUrl.isNotBlank()) {
                    results.add(
                        OnlineSubtitleResult(
                            id = id,
                            fileName = fileName,
                            language = language,
                            downloadUrl = subUrl
                        )
                    )
                }
            }
            results
        }

    suspend fun downloadSubtitleToCache(context: Context, sub: OnlineSubtitleResult): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }
                val ext = if (sub.fileName.endsWith(".vtt", ignoreCase = true)) "vtt" else "srt"
                val outFile = File(dir, "sub_${sub.id}.$ext")
                val conn = (URL(sub.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 12000
                    setRequestProperty("User-Agent", "TemporaryUserAgent")
                }
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                Uri.fromFile(outFile)
            } catch (_: Exception) {
                null
            }
        }
}
