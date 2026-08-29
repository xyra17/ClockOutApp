package com.clockout.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WorkTimeCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun day(
        start: String = "2026-08-29T09:00:00+08:00",
        mode: LunchMode = LunchMode.FIXED,
        lunchMinutes: Int = LunchDurationLimits.DEFAULT_MINUTES,
        lunchStart: String? = null,
        lunchEnd: String? = null,
        out: String? = null,
        workMinutes: Int = 480,
    ) = WorkDay(
        dateKey = "2026-08-29", clockIn = start.toInstant(), lunchMode = mode,
        plannedLunchMinutes = lunchMinutes, lunchStart = lunchStart?.let { it.toInstant() },
        lunchEnd = lunchEnd?.let { it.toInstant() }, workMinutes = workMinutes,
        actualClockOut = out?.let { it.toInstant() }, createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH, zoneId = zone.id,
    ).let { it.copy(expectedClockOut = WorkTimeCalculator.expectedClockOut(it)) }

    @Test fun `0900 plus eight hours and 90 minute lunch is 1830`() {
        assertEquals(Instant.parse("2026-08-29T10:30:00Z"), WorkTimeCalculator.expectedClockOut(day()))
    }

    @Test fun `zero minute lunch`() {
        assertEquals(Instant.parse("2026-08-29T09:00:00Z"), WorkTimeCalculator.expectedClockOut(day(lunchMinutes = 0)))
    }

    @Test fun `changing fixed lunch recalculates`() {
        val changed = day().copy(plannedLunchMinutes = 60)
        assertEquals(Instant.parse("2026-08-29T10:00:00Z"), WorkTimeCalculator.expectedClockOut(changed))
    }

    @Test fun `completed actual lunch shorter than default uses actual`() {
        val d = day(mode = LunchMode.ACTUAL, lunchStart = "2026-08-29T12:00:00+08:00", lunchEnd = "2026-08-29T13:00:00+08:00")
        assertEquals(60, WorkTimeCalculator.effectiveLunchMinutes(d))
        assertEquals(Instant.parse("2026-08-29T10:00:00Z"), WorkTimeCalculator.expectedClockOut(d))
    }

    @Test fun `completed actual lunch longer than default uses actual`() {
        val d = day(mode = LunchMode.ACTUAL, lunchStart = "2026-08-29T12:00:00+08:00", lunchEnd = "2026-08-29T14:00:00+08:00")
        assertEquals(120, WorkTimeCalculator.effectiveLunchMinutes(d))
    }

    @Test fun `running lunch uses max of default and elapsed`() {
        val d = day(mode = LunchMode.ACTUAL, lunchStart = "2026-08-29T12:00:00+08:00")
        assertEquals(90, WorkTimeCalculator.effectiveLunchMinutes(d, "2026-08-29T12:42:00+08:00".toInstant()))
        assertEquals(105, WorkTimeCalculator.effectiveLunchMinutes(d, "2026-08-29T13:45:00+08:00".toInstant()))
        assertNull(WorkTimeCalculator.actualWorkMinutes(d.copy(actualClockOut = "2026-08-29T18:30:00+08:00".toInstant())))
    }

    @Test fun `shift can cross midnight`() {
        val d = day(start = "2026-08-29T17:00:00+08:00", lunchMinutes = 30)
        assertEquals("2026-08-30T01:30+08:00", WorkTimeCalculator.expectedClockOut(d)!!.atZone(zone).toOffsetDateTime().toString())
    }

    @Test fun `manual edits recalculate all derived values`() {
        val edited = day().copy(clockIn = "2026-08-29T09:15:00+08:00".toInstant(), workMinutes = 450, plannedLunchMinutes = 60)
        assertEquals(Instant.parse("2026-08-29T09:45:00Z"), WorkTimeCalculator.expectedClockOut(edited))
    }

    @Test fun `early and late clock out differences are signed`() {
        val early = day(out = "2026-08-29T18:00:00+08:00")
        val late = day(out = "2026-08-29T19:00:00+08:00")
        assertEquals(-30L, WorkTimeCalculator.summarize(early).differenceFromExpectedMinutes)
        assertEquals(30L, WorkTimeCalculator.summarize(late).differenceFromExpectedMinutes)
        assertEquals(450L, WorkTimeCalculator.actualWorkMinutes(early))
    }

    @Test fun `actual work never becomes negative when shift is shorter than fixed lunch`() {
        val short = day(out = "2026-08-29T09:30:00+08:00", lunchMinutes = 90)
        assertEquals(0L, WorkTimeCalculator.actualWorkMinutes(short))
    }

    @Test fun `suggested lunch prefers previous actual duration`() {
        val previous = day(
            mode = LunchMode.ACTUAL,
            lunchStart = "2026-08-29T12:05:00+08:00",
            lunchEnd = "2026-08-29T13:10:00+08:00",
        )
        assertEquals(65, WorkTimeCalculator.suggestedLunchMinutes(previous, 90))
    }

    @Test fun `suggested lunch falls back to configured default and respects new upper bound`() {
        assertEquals(75, WorkTimeCalculator.suggestedLunchMinutes(null, 75))
        assertEquals(90, WorkTimeCalculator.suggestedLunchMinutes(null, 120))
    }

    @Test fun `zone change preserves instant and date boundary`() {
        val instant = Instant.parse("2026-08-29T16:30:00Z")
        assertEquals("2026-08-30", WorkTimeCalculator.dateKey(instant, ZoneId.of("Asia/Shanghai")))
        assertEquals("2026-08-29", WorkTimeCalculator.dateKey(instant, ZoneId.of("America/Los_Angeles")))
    }

    @Test fun `resolve end clock to following day when needed`() {
        val start = WorkTimeCalculator.resolveTime(LocalDate.parse("2026-08-29"), LocalTime.of(22, 0), zone)
        val end = WorkTimeCalculator.resolveTime(LocalDate.parse("2026-08-29"), LocalTime.of(1, 0), zone, start)
        assertTrue(end.isAfter(start))
        assertEquals("2026-08-30T01:00+08:00", end.atZone(zone).toOffsetDateTime().toString())
    }

    @Test fun `strict ordered time does not silently roll lunch to next day`() {
        val start = WorkTimeCalculator.resolveTime(LocalDate.parse("2026-08-29"), LocalTime.of(12, 0), zone)
        val earlier = WorkTimeCalculator.resolveTime(LocalDate.parse("2026-08-29"), LocalTime.of(11, 0), zone, start, rollToNextDay = false)
        assertTrue(earlier.isBefore(start))
        assertEquals("午休结束不能早于午休开始", WorkTimeCalculator.validate(day(lunchStart = "2026-08-29T12:00:00+08:00", lunchEnd = "2026-08-29T11:00:00+08:00")))
    }

    @Test fun `invalid ordering is rejected`() {
        val d = day(lunchStart = "2026-08-29T13:00:00+08:00", lunchEnd = "2026-08-29T12:00:00+08:00")
        assertEquals("午休结束不能早于午休开始", WorkTimeCalculator.validate(d))
    }

    private fun String.toInstant(): Instant = java.time.OffsetDateTime.parse(this).toInstant()
}
