package com.aethertv.v2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleShiftTest {

    @Test
    fun shiftSubtitleText_shiftsTimingLinesOnlyAndPreservesDialogue() {
        val srt = """
            1
            00:00:01,500 --> 00:00:04,000
            Meet me at 00:12:30,000 sharp!
        """.trimIndent()

        val shiftedPlus = SubtitleShiftHelper.shiftSubtitleText(srt, 1_500L)
        assertTrue(shiftedPlus.contains("00:00:03,000 --> 00:00:05,500"))
        // Dialogue text timestamp must remain completely untouched
        assertTrue(shiftedPlus.contains("Meet me at 00:12:30,000 sharp!"))

        val shiftedNeg = SubtitleShiftHelper.shiftSubtitleText(srt, -3_000L)
        assertTrue(shiftedNeg.contains("00:00:00,000 --> 00:00:01,000"))
    }

    @Test
    fun shiftSubtitleText_supportsVttWithoutHours() {
        val vtt = """
            WEBVTT

            00:05.250 --> 00:08.500
            Hello WebVTT
        """.trimIndent()

        val shifted = SubtitleShiftHelper.shiftSubtitleText(vtt, 500L)
        assertEquals(true, SubtitleShiftHelper.isWebVttContent(shifted))
        assertTrue(shifted.contains("00:05.750 --> 00:09.000"))
        assertFalse(SubtitleShiftHelper.isWebVttContent("1\n00:00:01,000 --> 00:00:02,000"))
    }
}
