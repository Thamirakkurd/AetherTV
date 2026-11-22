package com.aethertv.v2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object XtreamClient {
    const val USER_AGENT = "IPTVSmartersPro"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun normalizeHost(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }

    fun parseCategories(json: String): List<CategoryItem> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val arr = JSONArray(trimmed)
        val out = ArrayList<CategoryItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.safeStr("category_id")
            val name = obj.safeStr("category_name")
            if (id.isNotEmpty() && name.isNotEmpty()) {
                out.add(CategoryItem(id = id, name = name))
            }
        }
        return out
    }

    fun parseLiveStreams(
        json: String,
        host: String,
        user: String,
        pass: String,
        categoryName: String,
        categoryMap: Map<String, String> = emptyMap()
    ): List<MediaItem> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val arr = JSONArray(trimmed)
        val out = ArrayList<MediaItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val streamId = obj.safeStr("stream_id")
            val name = obj.safeStr("name")
            if (streamId.isEmpty() || name.isEmpty()) continue
            val icon = obj.safeStr("stream_icon").ifEmpty { null }
            val catId = obj.safeStr("category_id")
            val resolvedGroup = categoryMap[catId] ?: categoryName
            out.add(
                MediaItem(
                    id = "live_$streamId",
                    name = name,
                    group = resolvedGroup,
                    posterUrl = icon,
                    streamUrl = "$host/live/$user/$pass/$streamId.ts",
                    contentType = ContentType.LIVE,
                    badge = "LIVE"
                )
            )
        }
        return out
    }

    fun parseVodStreams(
        json: String,
        host: String,
        user: String,
        pass: String,
        categoryName: String,
        categoryMap: Map<String, String> = emptyMap()
    ): List<MediaItem> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val arr = JSONArray(trimmed)
        val out = ArrayList<MediaItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val streamId = obj.safeStr("stream_id")
            val name = obj.safeStr("name")
            if (streamId.isEmpty() || name.isEmpty()) continue
            val ext = obj.safeStr("container_extension").ifEmpty { "mp4" }
            val poster = obj.safeStr("stream_icon").ifEmpty { null }
            val rating = obj.safeStr("rating")
            val badge = if (rating.isNotEmpty() && rating != "0") "★ $rating" else "MOVIE"
            val catId = obj.safeStr("category_id")
            val resolvedGroup = categoryMap[catId] ?: categoryName
            out.add(
                MediaItem(
                    id = "vod_$streamId",
                    name = name,
                    group = resolvedGroup,
                    posterUrl = poster,
                    streamUrl = "$host/movie/$user/$pass/$streamId.$ext",
                    contentType = ContentType.MOVIE,
                    badge = badge
                )
            )
        }
        return out
    }

    fun parseSeries(
        json: String,
        categoryName: String,
        categoryMap: Map<String, String> = emptyMap()
    ): List<MediaItem> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val arr = JSONArray(trimmed)
        val out = ArrayList<MediaItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val seriesId = obj.safeStr("series_id")
            val name = obj.safeStr("name")
            if (seriesId.isEmpty() || name.isEmpty()) continue
            val cover = obj.safeStr("cover").ifEmpty { null }
            val rating = obj.safeStr("rating")
            val badge = if (rating.isNotEmpty() && rating != "0") "★ $rating" else "SERIES"
            val catId = obj.safeStr("category_id")
            val resolvedGroup = categoryMap[catId] ?: categoryName
            out.add(
                MediaItem(
                    id = "series_$seriesId",
                    name = name,
                    group = resolvedGroup,
                    posterUrl = cover,
                    streamUrl = "series://$seriesId",
                    contentType = ContentType.SERIES,
                    seriesId = seriesId,
                    badge = badge
                )
            )
        }
        return out
    }

    fun parseVodDetail(json: String): MediaDetailInfo {
        return try {
            val root = JSONObject(json)
            val info = root.optJSONObject("info") ?: return MediaDetailInfo()
            val backdrops = info.optJSONArray("backdrop_path")
            val backdrop = if (backdrops != null && backdrops.length() > 0) {
                backdrops.optString(0, "").trim().ifEmpty { null }
            } else {
                info.safeStr("movie_image").ifEmpty { null }
            }
            val releaseDate = info.safeStr("releasedate").ifEmpty { info.safeStr("release_date") }
            val year = if (releaseDate.length >= 4) releaseDate.take(4) else releaseDate
            MediaDetailInfo(
                plot = info.safeStr("plot").ifEmpty { info.safeStr("description") },
                genre = info.safeStr("genre"),
                releaseYear = year,
                rating = info.safeStr("rating"),
                duration = info.safeStr("duration"),
                backdropUrl = backdrop
            )
        } catch (_: Exception) {
            MediaDetailInfo()
        }
    }

    fun parseSeriesDetail(json: String): MediaDetailInfo {
        return try {
            val root = JSONObject(json)
            val info = root.optJSONObject("info") ?: return MediaDetailInfo()
            val backdrops = info.optJSONArray("backdrop_path")
            val backdrop = if (backdrops != null && backdrops.length() > 0) {
                backdrops.optString(0, "").trim().ifEmpty { null }
            } else {
                info.safeStr("cover").ifEmpty { null }
            }
            val releaseDate = info.safeStr("releaseDate").ifEmpty { info.safeStr("release_date") }
            val year = if (releaseDate.length >= 4) releaseDate.take(4) else releaseDate
            MediaDetailInfo(
                plot = info.safeStr("plot"),
                genre = info.safeStr("genre"),
                releaseYear = year,
                rating = info.safeStr("rating"),
                backdropUrl = backdrop
            )
        } catch (_: Exception) {
            MediaDetailInfo()
        }
    }

    fun parseSeriesEpisodes(
        json: String,
        host: String,
        user: String,
        pass: String
    ): List<EpisodeItem> {
        val root = JSONObject(json)
        val episodesObj = root.optJSONObject("episodes") ?: return emptyList()
        val out = ArrayList<EpisodeItem>()
        val seasonKeys = episodesObj.keys().asSequence().toList()
            .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
        for (seasonKey in seasonKeys) {
            val epArray = episodesObj.optJSONArray(seasonKey) ?: continue
            for (i in 0 until epArray.length()) {
                val ep = epArray.optJSONObject(i) ?: continue
                val epId = ep.safeStr("id")
                if (epId.isEmpty()) continue
                val epNum = ep.safeStr("episode_num")
                val rawTitle = ep.safeStr("title").ifEmpty { "Episode $epNum" }
                val ext = ep.safeStr("container_extension").ifEmpty { "mp4" }
                out.add(
                    EpisodeItem(
                        id = epId,
                        season = "Season $seasonKey",
                        title = rawTitle,
                        streamUrl = "$host/series/$user/$pass/$epId.$ext"
                    )
                )
            }
        }
        return out
    }

    fun parseM3u(body: String): List<MediaItem> {
        val lines = body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val out = ArrayList<MediaItem>()
        var pendingName = ""
        var pendingGroup = "General"
        var pendingLogo: String? = null
        var idx = 0

        for (line in lines) {
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                pendingName = line.substringAfterLast(",", "").trim()
                pendingGroup = extractAttr(line, "group-title").ifEmpty { "General" }
                pendingLogo = extractAttr(line, "tvg-logo").ifEmpty { null }
            } else if (!line.startsWith("#")) {
                val title = pendingName.ifEmpty { "Stream ${idx + 1}" }
                out.add(
                    MediaItem(
                        id = "m3u_${idx}_${line.hashCode()}",
                        name = title,
                        group = pendingGroup,
                        posterUrl = pendingLogo,
                        streamUrl = line,
                        contentType = ContentType.LIVE,
                        badge = "LIVE"
                    )
                )
                idx++
                pendingName = ""
                pendingGroup = "General"
                pendingLogo = null
            }
        }
        return out
    }

    fun encodeItems(items: List<MediaItem>): String {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("group", item.group)
                    put("posterUrl", item.posterUrl ?: "")
                    put("streamUrl", item.streamUrl)
                    put("contentType", item.contentType.name)
                    put("seriesId", item.seriesId ?: "")
                    put("badge", item.badge)
                }
            )
        }
        return arr.toString()
    }

    fun decodeItems(raw: String?): List<MediaItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<MediaItem>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val type = try {
                    ContentType.valueOf(obj.safeStr("contentType"))
                } catch (_: Exception) {
                    ContentType.LIVE
                }
                out.add(
                    MediaItem(
                        id = obj.safeStr("id"),
                        name = obj.safeStr("name"),
                        group = obj.safeStr("group"),
                        posterUrl = obj.safeStr("posterUrl").ifEmpty { null },
                        streamUrl = obj.safeStr("streamUrl"),
                        contentType = type,
                        seriesId = obj.safeStr("seriesId").ifEmpty { null },
                        badge = obj.safeStr("badge")
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractAttr(line: String, key: String): String {
        val marker = "$key=\""
        val start = line.indexOf(marker, ignoreCase = true)
        if (start == -1) return ""
        val valueStart = start + marker.length
        val end = line.indexOf('"', valueStart)
        return if (end > valueStart) line.substring(valueStart, end).trim() else ""
    }

    private fun JSONObject.safeStr(key: String): String {
        if (isNull(key)) return ""
        return optString(key, "").trim()
    }
}
