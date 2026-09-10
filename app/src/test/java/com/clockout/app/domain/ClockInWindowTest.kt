package com.clockout.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockInWindowTest {
    @Test
    fun `default clock-in range is eight thirty through nine`() {
        val settings = AppSettings()
        assertTrue(settings.clockInRangeEnabled)
        assertEquals(8 * 60 + 30, settings.clockInStartMinute)
        assertEquals(9 * 60, settings.clockInEndMinute)
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

    @Test
    fun `quick lunch input is the default style`() {
        assertEquals(LunchInputStyle.QUICK, AppSettings().lunchInputStyle)
    }
}
