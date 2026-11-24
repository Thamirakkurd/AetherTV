package com.aethertv.v2.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.MessageDigest

object PlaylistDiskCache {
    private const val CACHE_DIR_NAME = "playlist_cache"
    private const val MAX_CACHE_FILES = 200
    private const val MAX_CACHE_BYTES = 50L * 1024L * 1024L

    private fun cacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    internal fun sha256Hex(rawKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sanitizePrefix(rawPrefix: String): String {
        return rawPrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(36).ifEmpty { "srv" }
    }

    private fun fileFor(context: Context, scopePrefix: String, key: String): File {
        val safeScope = sanitizePrefix(scopePrefix)
        val hash = sha256Hex(key)
        return File(cacheDir(context), "${safeScope}_${hash}.jsonl")
    }

    private fun atomicWrite(target: File, writerBlock: (BufferedWriter) -> Unit) {
        val tmp = File(target.parentFile, "${target.name}.${System.nanoTime()}.tmp")
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(tmp), Charsets.UTF_8)).use(writerBlock)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }

    private fun pruneIfNeeded(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jsonl") } ?: return
        var totalBytes = files.sumOf { it.length() }
        if (files.size <= MAX_CACHE_FILES && totalBytes <= MAX_CACHE_BYTES) return

        val oldestFirst = files.sortedBy { it.lastModified() }
        var remainingCount = files.size
        for (file in oldestFirst) {
            if (remainingCount <= MAX_CACHE_FILES && totalBytes <= MAX_CACHE_BYTES) break
            val len = file.length()
            if (file.delete()) {
                totalBytes -= len
                remainingCount--
            }
        }
    }

    private fun extractScope(key: String): String = key.substringBefore('_', "srv")

    suspend fun saveCategories(context: Context, key: String, categories: List<CategoryItem>) =
        withContext(Dispatchers.IO) {
            val dir = cacheDir(context)
            val target = fileFor(context, extractScope(key), "cats_$key")
            atomicWrite(target) { writer ->
                for (cat in categories) {
                    val obj = JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                    }
                    writer.write(obj.toString())
                    writer.newLine()
                }
            }
            pruneIfNeeded(dir)
        }

    suspend fun loadCategories(context: Context, key: String): List<CategoryItem> =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, extractScope(key), "cats_$key")
            if (!target.exists() || !target.isFile) return@withContext emptyList()
            val out = ArrayList<CategoryItem>(64)
            val seenIds = HashSet<String>(64)
            val seenNames = HashSet<String>(64)
            BufferedReader(InputStreamReader(FileInputStream(target), Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        try {
                            val obj = JSONObject(line)
                            val id = obj.optString("id", "").trim()
                            val name = obj.optString("name", "").trim()
                            if (id.isNotEmpty() && name.isNotEmpty() && seenIds.add(id) && seenNames.add(name)) {
                                out.add(CategoryItem(id = id, name = name))
                            }
                        } catch (_: Exception) {
                        }
                    }
                    line = reader.readLine()
                }
            }
            out
        }

    suspend fun saveItems(context: Context, key: String, items: List<MediaItem>) =
        withContext(Dispatchers.IO) {
            val dir = cacheDir(context)
            val target = fileFor(context, extractScope(key), "items_$key")
            atomicWrite(target) { writer ->
                for (item in items) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("group", item.group)
                        put("posterUrl", item.posterUrl ?: "")
                        put("streamUrl", item.streamUrl)
                        put("contentType", item.contentType.name)
                        put("seriesId", item.seriesId ?: "")
                        put("badge", item.badge)
                    }
                    writer.write(obj.toString())
                    writer.newLine()
                }
            }
            pruneIfNeeded(dir)
        }

    suspend fun loadItems(context: Context, key: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, extractScope(key), "items_$key")
            if (!target.exists() || !target.isFile) return@withContext emptyList()
            val out = ArrayList<MediaItem>(256)
            val seen = HashSet<String>(256)
            BufferedReader(InputStreamReader(FileInputStream(target), Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        try {
                            val obj = JSONObject(line)
                            val id = obj.optString("id", "").trim()
                            val name = obj.optString("name", "").trim()
                            val streamUrl = obj.optString("streamUrl", "").trim()
                            if (id.isNotEmpty() && name.isNotEmpty() && streamUrl.isNotEmpty() && seen.add(id)) {
                                val type = try {
                                    ContentType.valueOf(obj.optString("contentType", "LIVE"))
                                } catch (_: Exception) {
                                    ContentType.LIVE
                                }
                                out.add(
                                    MediaItem(
                                        id = id,
                                        name = name,
                                        group = obj.optString("group", "").trim(),
                                        posterUrl = obj.optString("posterUrl", "").trim().ifEmpty { null },
                                        streamUrl = streamUrl,
                                        contentType = type,
                                        seriesId = obj.optString("seriesId", "").trim().ifEmpty { null },
                                        badge = obj.optString("badge", "").trim()
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }
                    line = reader.readLine()
                }
            }
            out
        }

    suspend fun saveDetailInfo(context: Context, key: String, info: MediaDetailInfo) =
        withContext(Dispatchers.IO) {
            val dir = cacheDir(context)
            val target = fileFor(context, extractScope(key), "detail_$key")
            val obj = JSONObject().apply {
                put("plot", info.plot)
                put("genre", info.genre)
                put("releaseYear", info.releaseYear)
                put("rating", info.rating)
                put("duration", info.duration)
                put("backdropUrl", info.backdropUrl ?: "")
            }
            atomicWrite(target) { writer ->
                writer.write(obj.toString())
            }
            pruneIfNeeded(dir)
        }

    suspend fun loadDetailInfo(context: Context, key: String): MediaDetailInfo? =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, extractScope(key), "detail_$key")
            if (!target.exists() || !target.isFile) return@withContext null
            try {
                val obj = JSONObject(target.readText(Charsets.UTF_8))
                MediaDetailInfo(
                    plot = obj.optString("plot", ""),
                    genre = obj.optString("genre", ""),
                    releaseYear = obj.optString("releaseYear", ""),
                    rating = obj.optString("rating", ""),
                    duration = obj.optString("duration", ""),
                    backdropUrl = obj.optString("backdropUrl", "").ifBlank { null }
                )
            } catch (_: Exception) {
                null
            }
        }

    suspend fun saveEpisodes(context: Context, key: String, episodes: List<EpisodeItem>) =
        withContext(Dispatchers.IO) {
            val dir = cacheDir(context)
            val target = fileFor(context, extractScope(key), "eps_$key")
            atomicWrite(target) { writer ->
                for (ep in episodes) {
                    val obj = JSONObject().apply {
                        put("id", ep.id)
                        put("season", ep.season)
                        put("title", ep.title)
                        put("streamUrl", ep.streamUrl)
                    }
                    writer.write(obj.toString())
                    writer.newLine()
                }
            }
            pruneIfNeeded(dir)
        }

    suspend fun loadEpisodes(context: Context, key: String): List<EpisodeItem> =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, extractScope(key), "eps_$key")
            if (!target.exists() || !target.isFile) return@withContext emptyList()
            val out = ArrayList<EpisodeItem>(64)
            val seen = HashSet<String>(64)
            BufferedReader(InputStreamReader(FileInputStream(target), Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        try {
                            val obj = JSONObject(line)
                            val id = obj.optString("id", "").trim()
                            val streamUrl = obj.optString("streamUrl", "").trim()
                            if (id.isNotEmpty() && streamUrl.isNotEmpty() && seen.add(id)) {
                                out.add(
                                    EpisodeItem(
                                        id = id,
                                        season = obj.optString("season", "Season 1"),
                                        title = obj.optString("title", "Episode"),
                                        streamUrl = streamUrl
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }
                    line = reader.readLine()
                }
            }
            out
        }

    suspend fun clearForServer(context: Context, serverId: String) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) return@withContext
        val prefix = "${sanitizePrefix(serverId)}_"
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(prefix)) {
                file.delete()
            }
        }
    }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        }
    }
}
