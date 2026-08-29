package com.clockout.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockInWindowTest {
    @Test
    fun `clock-in window includes both endpoints`() {
        assertTrue(ClockInWindow.contains(8 * 60 + 30))
        assertTrue(ClockInWindow.contains(9 * 60 + 10))
    }

    @Test
    fun `clock-in window rejects minutes immediately outside the range`() {
        assertFalse(ClockInWindow.contains(8 * 60 + 29))
        assertFalse(ClockInWindow.contains(9 * 60 + 11))
    }

    @Test
    fun `fixed lunch bounds are zero through ninety minutes`() {
        assertTrue(0 in LunchDurationLimits.MIN_MINUTES..LunchDurationLimits.MAX_MINUTES)
        assertTrue(90 in LunchDurationLimits.MIN_MINUTES..LunchDurationLimits.MAX_MINUTES)
        assertFalse(91 in LunchDurationLimits.MIN_MINUTES..LunchDurationLimits.MAX_MINUTES)
    }

    @Test
    fun `default lunch duration is ninety minutes`() {
        assertEquals(90, LunchDurationLimits.DEFAULT_MINUTES)
        assertEquals(LunchDurationLimits.DEFAULT_MINUTES, AppSettings().lunchMinutes)
        assertEquals(
            LunchDurationLimits.DEFAULT_MINUTES,
            WorkDay(
                dateKey = "2026-08-30",
                createdAt = java.time.Instant.EPOCH,
                updatedAt = java.time.Instant.EPOCH,
                zoneId = "Asia/Shanghai",
            ).plannedLunchMinutes,
        )
    }
}
