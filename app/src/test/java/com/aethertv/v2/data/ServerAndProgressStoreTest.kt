package com.aethertv.v2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAndProgressStoreTest {

    @Test
    fun serverStore_roundTripPreservesAllFields() {
        val input = listOf(
            SavedServer("id1", "Primary", "xtream", "http://tv.test", "alice", "s3cr#t"),
            SavedServer("id2", "Backup", "m3u", m3uUrl = "https://tv.test/list.m3u")
        )
        val encoded = ServerStore.encode(input)
        val decoded = ServerStore.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun playbackProgress_ignoresShortWatchAndClearsCompleted() {
        var state = emptyMap<String, PlaybackProgress>()
        // < 10s should not save
        state = PlaybackProgressStore.upsert(state, "vod_1", 5_000L, 100_000L)
        assertEquals(0L, PlaybackProgressStore.getResumePositionMs(state, "vod_1"))

        // Mid-movie saves cleanly
        state = PlaybackProgressStore.upsert(state, "vod_1", 45_000L, 100_000L)
        assertEquals(45_000L, PlaybackProgressStore.getResumePositionMs(state, "vod_1"))

        // >= 95% clears resume entry
        state = PlaybackProgressStore.upsert(state, "vod_1", 96_000L, 100_000L)
        assertEquals(0L, PlaybackProgressStore.getResumePositionMs(state, "vod_1"))
    }

    @Test
    fun playbackProgress_encodeDecodeRoundTrip() {
        val map = PlaybackProgressStore.upsert(emptyMap(), "ep_42", 120_000L, 2_400_000L, nowMs = 123456L)
        val raw = PlaybackProgressStore.encode(map)
        val decoded = PlaybackProgressStore.decode(raw)
        assertEquals(120_000L, PlaybackProgressStore.getResumePositionMs(decoded, "ep_42"))
        assertTrue(decoded.containsKey("ep_42"))
    }
}
