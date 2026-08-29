package com.clockout.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockInTimeInputTest {
    @Test
    fun `manual time input accepts one or two digit hours`() {
        assertEquals(8 * 60 + 30, parseMinuteOfDay("8:30"))
        assertEquals(9 * 60 + 10, parseMinuteOfDay("09:10"))
    }

    @Test
    fun `compact numeric input is recognized without a colon`() {
        assertEquals(8 * 60 + 56, parseMinuteOfDay("856"))
        assertEquals(9 * 60 + 1, parseMinuteOfDay("901"))
        assertEquals(8 * 60 + 56, parseMinuteOfDay("0856"))
    }

    @Test
    fun `manual time input rejects invalid clock values`() {
        assertNull(parseMinuteOfDay("9:60"))
        assertNull(parseMinuteOfDay("09.10"))
        assertNull(parseMinuteOfDay("text"))
    }

    @Test
    fun `minute formatting is always padded`() {
        assertEquals("08:30", formatMinuteOfDay(8 * 60 + 30))
        assertEquals("09:10", formatMinuteOfDay(9 * 60 + 10))
    }

    @Test
    fun `typing after a formatted value starts a fast replacement`() {
        assertEquals("8", normalizeClockInput("09:00", "09:008"))
        assertEquals("856", normalizeClockInput("09:00", "09:00856"))
        assertEquals("85", normalizeClockInput("8", "85"))
    }
}
