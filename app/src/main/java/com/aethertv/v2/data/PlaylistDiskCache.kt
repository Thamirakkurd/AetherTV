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

object PlaylistDiskCache {
    private const val CACHE_DIR_NAME = "playlist_cache"

    private fun cacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun sanitizeKey(rawKey: String): String {
        val hex = rawKey.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
        return if (hex.length <= 180) hex else "${hex.take(160)}_${rawKey.hashCode()}"
    }

    private fun fileFor(context: Context, key: String): File {
        return File(cacheDir(context), "${sanitizeKey(key)}.jsonl")
    }

    suspend fun saveCategories(context: Context, key: String, categories: List<CategoryItem>) =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, "cats_$key")
            BufferedWriter(OutputStreamWriter(FileOutputStream(target), Charsets.UTF_8)).use { writer ->
                for (cat in categories) {
                    val obj = JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                    }
                    writer.write(obj.toString())
                    writer.newLine()
                }
            }
        }

    suspend fun loadCategories(context: Context, key: String): List<CategoryItem> =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, "cats_$key")
            if (!target.exists() || !target.isFile) return@withContext emptyList()
            val out = ArrayList<CategoryItem>(64)
            BufferedReader(InputStreamReader(FileInputStream(target), Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        try {
                            val obj = JSONObject(line)
                            val id = obj.optString("id", "").trim()
                            val name = obj.optString("name", "").trim()
                            if (id.isNotEmpty() && name.isNotEmpty()) {
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
            val target = fileFor(context, "items_$key")
            BufferedWriter(OutputStreamWriter(FileOutputStream(target), Charsets.UTF_8)).use { writer ->
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
        }

    suspend fun loadItems(context: Context, key: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, "items_$key")
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
            val target = fileFor(context, "detail_$key")
            val obj = JSONObject().apply {
                put("plot", info.plot)
                put("genre", info.genre)
                put("releaseYear", info.releaseYear)
                put("rating", info.rating)
                put("duration", info.duration)
                put("backdropUrl", info.backdropUrl ?: "")
            }
            target.writeText(obj.toString(), Charsets.UTF_8)
        }

    suspend fun loadDetailInfo(context: Context, key: String): MediaDetailInfo? =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, "detail_$key")
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
            val target = fileFor(context, "eps_$key")
            BufferedWriter(OutputStreamWriter(FileOutputStream(target), Charsets.UTF_8)).use { writer ->
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
        }

    suspend fun loadEpisodes(context: Context, key: String): List<EpisodeItem> =
        withContext(Dispatchers.IO) {
            val target = fileFor(context, "eps_$key")
            if (!target.exists() || !target.isFile) return@withContext emptyList()
            val out = ArrayList<EpisodeItem>(64)
            BufferedReader(InputStreamReader(FileInputStream(target), Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        try {
                            val obj = JSONObject(line)
                            val id = obj.optString("id", "").trim()
                            val streamUrl = obj.optString("streamUrl", "").trim()
                            if (id.isNotEmpty() && streamUrl.isNotEmpty()) {
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

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        }
    }
}
