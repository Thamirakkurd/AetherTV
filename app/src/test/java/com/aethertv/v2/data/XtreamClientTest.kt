package com.aethertv.v2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamClientTest {

    @Test
    fun buildApiUrl_encodesSpecialCharactersInCredentials() {
        val url = XtreamClient.buildApiUrl(
            host = "example.com:8080/",
            user = "user#1&a",
            pass = "p@ss+word/100%",
            action = "get_live_streams",
            extraParams = mapOf("category_id" to "42")
        )
        assertTrue(url.startsWith("http://example.com:8080/player_api.php?"))
        assertTrue(url.contains("username=user%231%26a"))
        assertTrue(url.contains("password=p%40ss%2Bword%2F100%25"))
        assertTrue(url.contains("action=get_live_streams"))
        assertTrue(url.contains("category_id=42"))
    }

    @Test
    fun buildStreamUrl_encodesPathSegmentsSafely() {
        val url = XtreamClient.buildStreamUrl(
            host = "https://tv.example.org",
            segmentType = "live",
            user = "u#1",
            pass = "p/2?x",
            streamId = "999",
            extension = ".ts"
        )
        assertEquals("https://tv.example.org/live/u%231/p%2F2%3Fx/999.ts", url)
    }

    @Test
    fun resolveStreamUrl_updatesCredentialsFromActiveServer() {
        val oldItem = MediaItem(
            id = "vod_777",
            name = "Sample Movie",
            group = "Action",
            posterUrl = null,
            streamUrl = "http://old.host/movie/olduser/oldpass/777.mkv",
            contentType = ContentType.MOVIE
        )
        val server = SavedServer(
            id = "srv1",
            name = "Updated",
            mode = "xtream",
            host = "https://new.host",
            user = "newuser",
            pass = "new#pass"
        )
        val resolved = XtreamClient.resolveStreamUrl(oldItem, server)
        assertEquals("https://new.host/movie/newuser/new%23pass/777.mkv", resolved)
    }

    @Test
    fun parseCategories_deduplicatesDuplicateIdsAndNames() {
        val json = """
            [
              {"category_id":"1","category_name":"Sports"},
              {"category_id":"1","category_name":"Sports Duplicate Id"},
              {"category_id":"2","category_name":"Sports"},
              {"category_id":"3","category_name":"Movies"}
            ]
        """.trimIndent()
        val cats = XtreamClient.parseCategories(json)
        assertEquals(2, cats.size)
        assertEquals("Sports", cats[0].name)
        assertEquals("Movies", cats[1].name)
    }

    @Test
    fun parseSeriesEpisodes_deduplicatesIdsAndSortsSeasonsNumerically() {
        val json = """
            {
              "episodes": {
                "10": [{"id":"101","episode_num":"1","title":"S10E1","container_extension":"mkv"}],
                "2": [
                  {"id":"201","episode_num":"1","title":"S2E1","container_extension":"mp4"},
                  {"id":"201","episode_num":"1","title":"S2E1 Dup","container_extension":"mp4"}
                ]
              }
            }
        """.trimIndent()
        val eps = XtreamClient.parseSeriesEpisodes(json, "http://host.test", "u", "p")
        assertEquals(2, eps.size)
        assertEquals("Season 2", eps[0].season)
        assertEquals("201", eps[0].id)
        assertEquals("Season 10", eps[1].season)
        assertEquals("http://host.test/series/u/p/101.mkv", eps[1].streamUrl)
    }

    @Test
    fun parseM3u_preservesCommasInsideTitlesAndSupportsSingleQuotes() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-name="News, Sports" group-title="UK, News" tvg-logo="http://img/bbc.png",BBC News, UK
            http://stream.test/bbc.ts
            #EXTINF:-1 group-title='Movies HD' x-group-title='Wrong',Action Movie
            http://stream.test/movie.mp4
        """.trimIndent()

        val items = XtreamClient.parseM3u(m3u)
        assertEquals(2, items.size)
        assertEquals("BBC News, UK", items[0].name)
        assertEquals("UK, News", items[0].group)
        assertEquals("http://img/bbc.png", items[0].posterUrl)
        assertEquals("Action Movie", items[1].name)
        assertEquals("Movies HD", items[1].group)
    }

    @Test
    fun isValidHttpUrl_validatesHostCorrectly() {
        assertTrue(XtreamClient.isValidHttpUrl("http://example.com:8080"))
        assertTrue(XtreamClient.isValidHttpUrl("example.com"))
        assertFalse(XtreamClient.isValidHttpUrl(""))
        assertFalse(XtreamClient.isValidHttpUrl("http://bad host with spaces"))
    }
}
