package com.jarvis.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationParserTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `parses minutes`() {
        val result = DurationParser.parseRelative("in 5 minutes", now)
        assertEquals(now + 5 * 60_000L, result)
    }

    @Test
    fun `parses hours and minutes combined`() {
        val result = DurationParser.parseRelative("in 1 hour 30 minutes", now)
        assertEquals(now + 90 * 60_000L, result)
    }

    @Test
    fun `parses an hour`() {
        val result = DurationParser.parseRelative("in an hour", now)
        assertEquals(now + 3_600_000L, result)
    }

    @Test
    fun `returns null for unparseable duration`() {
        assertNull(DurationParser.parseRelative("in a while", now))
        assertNull(DurationParser.parseRelative("hello", now))
    }

    @Test
    fun `parses 24h clock time`() {
        val result = DurationParser.parseClockTime("at 19:30", now)
        assertNotNull(result)
        assertTrue(result!! > now)
    }

    @Test
    fun `parses 12h clock time`() {
        val result = DurationParser.parseClockTime("remind me at 7 pm", now)
        assertNotNull(result)
        assertTrue(result!! > now)
    }

    @Test
    fun `returns null for unparseable clock time`() {
        assertNull(DurationParser.parseClockTime("sometime later", now))
    }

    @Test
    fun `formats durations`() {
        assertEquals("5 minutes", DurationParser.formatDuration(5 * 60_000L))
        assertEquals("1 hour 30 minutes", DurationParser.formatDuration(90 * 60_000L))
        assertEquals("45 seconds", DurationParser.formatDuration(45_000L))
    }
}
