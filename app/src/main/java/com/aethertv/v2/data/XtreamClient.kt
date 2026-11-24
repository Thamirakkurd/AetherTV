package com.aethertv.v2.data

import android.util.JsonReader
import android.util.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.util.concurrent.TimeUnit

object XtreamClient {
    const val USER_AGENT = "IPTVSmartersPro"

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val imageHttpClient: OkHttpClient = httpClient.newBuilder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(req)
        }
        .build()

    fun normalizeHost(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    fun isValidHttpUrl(raw: String): Boolean {
        val normalized = normalizeHost(raw)
        val parsed = normalized.toHttpUrlOrNull() ?: return false
        return parsed.host.isNotBlank()
    }

    /**
     * Safely builds an Xtream API URL using OkHttp's HttpUrl.Builder so credentials
     * containing #, &, +, %, ?, /, or spaces are properly RFC 3986 encoded.
     */
    fun buildApiUrl(
        host: String,
        user: String,
        pass: String,
        action: String,
        extraParams: Map<String, String> = emptyMap()
    ): String {
        val base = normalizeHost(host).toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid server URL: $host")
        val builder = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", user)
            .addQueryParameter("password", pass)
            .addQueryParameter("action", action)
        for ((k, v) in extraParams) {
            builder.addQueryParameter(k, v)
        }
        return builder.build().toString()
    }

    /**
     * Safely builds an Xtream media stream URL (live/movie/series) with encoded path segments.
     */
    fun buildStreamUrl(
        host: String,
        segmentType: String,
        user: String,
        pass: String,
        streamId: String,
        extension: String
    ): String {
        val base = normalizeHost(host).toHttpUrlOrNull()
        val cleanExt = extension.trim().trimStart('.').ifEmpty { "mp4" }
        return if (base != null) {
            base.newBuilder()
                .addPathSegment(segmentType)
                .addPathSegment(user)
                .addPathSegment(pass)
                .addPathSegment("$streamId.$cleanExt")
                .build()
                .toString()
        } else {
            "${normalizeHost(host)}/$segmentType/$user/$pass/$streamId.$cleanExt"
        }
    }

    /**
     * Rebuilds Xtream stream URLs against the active server's latest credentials so
     * saved Favorites and Recently Watched items keep working after password/host updates.
     */
    fun resolveStreamUrl(item: MediaItem, activeServer: SavedServer?): String {
        if (activeServer == null || activeServer.mode != "xtream" || activeServer.host.isBlank()) {
            return item.streamUrl
        }
        if (item.streamUrl.startsWith("file://") || item.streamUrl.startsWith("content://") || item.streamUrl.startsWith("series://")) {
            return item.streamUrl
        }
        val ext = item.streamUrl.substringAfterLast('/', "").substringAfterLast('.', "").substringBefore('?')
        return when {
            item.id.startsWith("live_") -> {
                val sid = item.id.removePrefix("live_")
                if (sid.isNotBlank()) buildStreamUrl(activeServer.host, "live", activeServer.user, activeServer.pass, sid, ext.ifEmpty { "ts" })
                else item.streamUrl
            }
            item.id.startsWith("vod_") -> {
                val sid = item.id.removePrefix("vod_")
                if (sid.isNotBlank()) buildStreamUrl(activeServer.host, "movie", activeServer.user, activeServer.pass, sid, ext.ifEmpty { "mp4" })
                else item.streamUrl
            }
            item.id.startsWith("ep_") -> {
                val eid = item.id.removePrefix("ep_")
                if (eid.isNotBlank()) buildStreamUrl(activeServer.host, "series", activeServer.user, activeServer.pass, eid, ext.ifEmpty { "mp4" })
                else item.streamUrl
            }
            else -> item.streamUrl
        }
    }

    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }

    suspend fun fetchLiveStreams(
        url: String,
        host: String,
        user: String,
        pass: String,
        categoryName: String,
        categoryMap: Map<String, String> = emptyMap()
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val stream = response.body?.byteStream() ?: return@withContext emptyList()
            val out = ArrayList<MediaItem>(256)
            val seen = HashSet<String>(256)
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.isLenient = true
                if (reader.peek() != JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                        reader.skipValue()
                        continue
                    }
                    var streamId = ""
                    var name = ""
                    var icon = ""
                    var catId = ""
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "stream_id" -> streamId = reader.nextSafeString()
                            "name" -> name = reader.nextSafeString()
                            "stream_icon" -> icon = reader.nextSafeString()
                            "category_id" -> catId = reader.nextSafeString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (streamId.isNotEmpty() && name.isNotEmpty()) {
                        val id = "live_$streamId"
                        if (seen.add(id)) {
                            out.add(
                                MediaItem(
                                    id = id,
                                    name = name,
                                    group = categoryMap[catId] ?: categoryName,
                                    posterUrl = icon.ifEmpty { null },
                                    streamUrl = buildStreamUrl(host, "live", user, pass, streamId, "ts"),
                                    contentType = ContentType.LIVE,
                                    badge = "LIVE"
                                )
                            )
                        }
                    }
                }
                reader.endArray()
            }
            out
        }
    }

    suspend fun fetchVodStreams(
        url: String,
        host: String,
        user: String,
        pass: String,
        categoryName: String,
        categoryMap: Map<String, String> = emptyMap()
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val stream = response.body?.byteStream() ?: return@withContext emptyList()
            val out = ArrayList<MediaItem>(256)
            val seen = HashSet<String>(256)
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.isLenient = true
                if (reader.peek() != JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                        reader.skipValue()
                        continue
                    }
                    var streamId = ""
                    var name = ""
                    var ext = ""
                    var poster = ""
                    var rating = ""
                    var catId = ""
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "stream_id" -> streamId = reader.nextSafeString()
                            "name" -> name = reader.nextSafeString()
                            "container_extension" -> ext = reader.nextSafeString()
                            "stream_icon" -> poster = reader.nextSafeString()
                            "rating" -> rating = reader.nextSafeString()
                            "category_id" -> catId = reader.nextSafeString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (streamId.isNotEmpty() && name.isNotEmpty()) {
                        val id = "vod_$streamId"
                        if (seen.add(id)) {
                            val badge = if (rating.isNotEmpty() && rating != "0") "★ $rating" else "MOVIE"
                            out.add(
                                MediaItem(
                                    id = id,
                                    name = name,
                                    group = categoryMap[catId] ?: categoryName,
                                    posterUrl = poster.ifEmpty { null },
                                    streamUrl = buildStreamUrl(host, "movie", user, pass, streamId, ext.ifEmpty { "mp4" }),
                                    contentType = ContentType.MOVIE,
                                    badge = badge
                                )
                            )
                        }
                    }
                }
                reader.endArray()
            }
            out
        }
    }

    suspend fun fetchSeries(
        url: String,
        categoryName: String,
        categoryMap: Map<String, String> = emptyMap()
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val stream = response.body?.byteStream() ?: return@withContext emptyList()
            val out = ArrayList<MediaItem>(256)
            val seen = HashSet<String>(256)
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.isLenient = true
                if (reader.peek() != JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                        reader.skipValue()
                        continue
                    }
                    var seriesId = ""
                    var name = ""
                    var cover = ""
                    var rating = ""
                    var catId = ""
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "series_id" -> seriesId = reader.nextSafeString()
                            "name" -> name = reader.nextSafeString()
                            "cover" -> cover = reader.nextSafeString()
                            "rating" -> rating = reader.nextSafeString()
                            "category_id" -> catId = reader.nextSafeString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (seriesId.isNotEmpty() && name.isNotEmpty()) {
                        val id = "series_$seriesId"
                        if (seen.add(id)) {
                            val badge = if (rating.isNotEmpty() && rating != "0") "★ $rating" else "SERIES"
                            out.add(
                                MediaItem(
                                    id = id,
                                    name = name,
                                    group = categoryMap[catId] ?: categoryName,
                                    posterUrl = cover.ifEmpty { null },
                                    streamUrl = "series://$seriesId",
                                    contentType = ContentType.SERIES,
                                    seriesId = seriesId,
                                    badge = badge
                                )
                            )
                        }
                    }
                }
                reader.endArray()
            }
            out
        }
    }

    /**
     * Streams M3U playlist directly from network response line-by-line to prevent
     * OutOfMemoryError on massive 50MB-150MB IPTV playlists.
     */
    suspend fun fetchAndParseM3u(url: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val stream = response.body?.byteStream() ?: return@withContext emptyList()
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                parseM3u(reader)
            }
        }
    }

    fun parseCategories(json: String): List<CategoryItem> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return try {
            val arr = JSONArray(trimmed)
            val out = ArrayList<CategoryItem>(arr.length())
            val seenIds = HashSet<String>(arr.length())
            val seenNames = HashSet<String>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.safeStr("category_id")
                val name = obj.safeStr("category_name")
                if (id.isNotEmpty() && name.isNotEmpty() && seenIds.add(id) && seenNames.add(name)) {
                    out.add(CategoryItem(id = id, name = name))
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
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
        return try {
            val root = JSONObject(json)
            val episodesObj = root.optJSONObject("episodes") ?: return emptyList()
            val out = ArrayList<EpisodeItem>()
            val seenIds = HashSet<String>()
            val seasonKeys = episodesObj.keys().asSequence().toList()
                .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
            for (seasonKey in seasonKeys) {
                val epArray = episodesObj.optJSONArray(seasonKey) ?: continue
                for (i in 0 until epArray.length()) {
                    val ep = epArray.optJSONObject(i) ?: continue
                    val epId = ep.safeStr("id")
                    if (epId.isEmpty() || !seenIds.add(epId)) continue
                    val epNum = ep.safeStr("episode_num")
                    val rawTitle = ep.safeStr("title").ifEmpty { "Episode $epNum" }
                    val ext = ep.safeStr("container_extension").ifEmpty { "mp4" }
                    out.add(
                        EpisodeItem(
                            id = epId,
                            season = "Season $seasonKey",
                            title = rawTitle,
                            streamUrl = buildStreamUrl(host, "series", user, pass, epId, ext)
                        )
                    )
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseM3u(body: String): List<MediaItem> {
        return BufferedReader(StringReader(body)).use { parseM3u(it) }
    }

    fun parseM3u(reader: BufferedReader): List<MediaItem> {
        val out = ArrayList<MediaItem>(256)
        var pendingName = ""
        var pendingGroup = "General"
        var pendingLogo: String? = null
        var idx = 0

        var rawLine = reader.readLine()
        while (rawLine != null) {
            val line = rawLine.trim()
            if (line.isNotEmpty()) {
                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    pendingName = extractExtInfTitle(line)
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
            rawLine = reader.readLine()
        }
        return out
    }

    /**
     * Finds the first comma outside quoted #EXTINF attributes so channel titles
     * containing commas (e.g. "BBC News, UK") are preserved intact.
     */
    internal fun extractExtInfTitle(extInfLine: String): String {
        var inDoubleQuotes = false
        var inSingleQuotes = false
        for (i in extInfLine.indices) {
            when (extInfLine[i]) {
                '"' -> if (!inSingleQuotes) inDoubleQuotes = !inDoubleQuotes
                '\'' -> if (!inDoubleQuotes) inSingleQuotes = !inSingleQuotes
                ',' -> if (!inDoubleQuotes && !inSingleQuotes) {
                    return extInfLine.substring(i + 1).trim()
                }
            }
        }
        return ""
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
            val seen = HashSet<String>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.safeStr("id")
                val name = obj.safeStr("name")
                val streamUrl = obj.safeStr("streamUrl")
                if (id.isEmpty() || name.isEmpty() || streamUrl.isEmpty() || !seen.add(id)) continue
                val type = try {
                    ContentType.valueOf(obj.safeStr("contentType"))
                } catch (_: Exception) {
                    ContentType.LIVE
                }
                out.add(
                    MediaItem(
                        id = id,
                        name = name,
                        group = obj.safeStr("group"),
                        posterUrl = obj.safeStr("posterUrl").ifEmpty { null },
                        streamUrl = streamUrl,
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

    private fun JsonReader.nextSafeString(): String {
        return when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                ""
            }
            JsonToken.STRING, JsonToken.NUMBER -> nextString().trim()
            JsonToken.BOOLEAN -> nextBoolean().toString()
            else -> {
                skipValue()
                ""
            }
        }
    }

    internal fun extractAttr(line: String, key: String): String {
        val pattern = Regex(
            """(?:^|[\s,])""" + Regex.escape(key) + """\s*=\s*(?:"([^"]*)"|'([^']*)')""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.find(line) ?: return ""
        return (match.groupValues[1].ifEmpty { match.groupValues[2] }).trim()
    }

    private fun JSONObject.safeStr(key: String): String {
        if (isNull(key)) return ""
        return optString(key, "").trim()
    }
}
