package com.aethertv.v2.data

import org.json.JSONArray
import org.json.JSONObject

data class PlaybackProgress(
    val mediaId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long
)

object PlaybackProgressStore {
    private const val MAX_ENTRIES = 150
    private const val MIN_RESUME_MS = 10_000L
    private const val COMPLETED_RATIO = 0.95f

    /**
     * Returns an updated map after recording playback progress.
     * Clears resume entry if watched >= 95% or < 10s.
     */
    fun upsert(
        current: Map<String, PlaybackProgress>,
        mediaId: String,
        positionMs: Long,
        durationMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Map<String, PlaybackProgress> {
        if (mediaId.isBlank() || durationMs <= 0L) return current
        val mutable = LinkedHashMap(current)
        val ratio = positionMs.toFloat() / durationMs.toFloat()
        if (positionMs < MIN_RESUME_MS || ratio >= COMPLETED_RATIO) {
            mutable.remove(mediaId)
        } else {
            mutable[mediaId] = PlaybackProgress(
                mediaId = mediaId,
                positionMs = positionMs.coerceIn(0L, durationMs),
                durationMs = durationMs,
                updatedAtMs = nowMs
            )
        }
        if (mutable.size <= MAX_ENTRIES) return mutable
        return mutable.values
            .sortedByDescending { it.updatedAtMs }
            .take(MAX_ENTRIES)
            .associateBy { it.mediaId }
    }

    fun getResumePositionMs(current: Map<String, PlaybackProgress>, mediaId: String): Long {
        val entry = current[mediaId] ?: return 0L
        if (entry.durationMs <= 0L) return 0L
        val ratio = entry.positionMs.toFloat() / entry.durationMs.toFloat()
        return if (entry.positionMs >= MIN_RESUME_MS && ratio < COMPLETED_RATIO) entry.positionMs else 0L
    }

    fun encode(entries: Map<String, PlaybackProgress>): String {
        val arr = JSONArray()
        for (item in entries.values) {
            arr.put(
                JSONObject().apply {
                    put("id", item.mediaId)
                    put("pos", item.positionMs)
                    put("dur", item.durationMs)
                    put("ts", item.updatedAtMs)
                }
            )
        }
        return arr.toString()
    }

    fun decode(raw: String?): Map<String, PlaybackProgress> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val out = LinkedHashMap<String, PlaybackProgress>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val pos = obj.optLong("pos", 0L)
                val dur = obj.optLong("dur", 0L)
                val ts = obj.optLong("ts", 0L)
                if (id.isNotEmpty() && pos >= MIN_RESUME_MS && dur > 0L) {
                    out[id] = PlaybackProgress(id, pos, dur, ts)
                }
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
