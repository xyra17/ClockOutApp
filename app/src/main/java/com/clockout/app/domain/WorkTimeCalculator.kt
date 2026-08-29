package com.clockout.app.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max

object WorkTimeCalculator {
    fun suggestedLunchMinutes(previous: WorkDay?, defaultMinutes: Int): Int {
        val previousMinutes = previous?.let { actualLunchMinutes(it) ?: it.plannedLunchMinutes.toLong() }
        return (previousMinutes?.toInt() ?: defaultMinutes)
            .coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES)
    }

    fun effectiveLunchMinutes(day: WorkDay, now: Instant = Instant.now()): Long = when (day.lunchMode) {
        LunchMode.FIXED -> day.plannedLunchMinutes.toLong()
        LunchMode.ACTUAL -> when {
            day.lunchStart == null -> day.plannedLunchMinutes.toLong()
            day.lunchEnd != null -> durationMinutes(day.lunchStart, day.lunchEnd)
            else -> max(day.plannedLunchMinutes.toLong(), durationMinutes(day.lunchStart, now))
        }
    }

    fun expectedClockOut(day: WorkDay, now: Instant = Instant.now()): Instant? {
        val start = day.clockIn ?: return null
        return start.plus(Duration.ofMinutes(day.workMinutes + effectiveLunchMinutes(day, now)))
    }

    fun actualLunchMinutes(day: WorkDay): Long? {
        return when (day.lunchMode) {
            LunchMode.FIXED -> day.plannedLunchMinutes.toLong()
            LunchMode.ACTUAL -> {
                val start = day.lunchStart ?: return 0
                val end = day.lunchEnd ?: return null
                durationMinutes(start, end)
            }
        }
    }

    fun actualWorkMinutes(day: WorkDay): Long? {
        val start = day.clockIn ?: return null
        val end = day.actualClockOut ?: return null
        val lunch = actualLunchMinutes(day) ?: return null
        return (durationMinutes(start, end) - lunch).coerceAtLeast(0)
    }

    fun summarize(day: WorkDay, now: Instant = Instant.now()): WorkSummary {
        val expected = if (day.actualClockOut != null) {
            day.expectedClockOut ?: expectedClockOut(day, now)
        } else expectedClockOut(day, now)
        return WorkSummary(
            expectedClockOut = expected,
            effectiveLunchMinutes = effectiveLunchMinutes(day, now),
            actualWorkMinutes = actualWorkMinutes(day),
            differenceFromExpectedMinutes = if (expected != null && day.actualClockOut != null) {
                Duration.between(expected, day.actualClockOut).toMinutes()
            } else null,
        )
    }

    fun validate(day: WorkDay): String? {
        if (day.workMinutes !in 1..1_440) return "工作时长需在 1 分钟到 24 小时之间"
        if (day.plannedLunchMinutes !in 0..180) return "午休时长需在 0～180 分钟之间"
        if (day.lunchEnd != null && day.lunchStart == null) return "请先记录午休开始时间"
        if (day.lunchStart != null && day.lunchEnd != null && day.lunchEnd.isBefore(day.lunchStart)) {
            return "午休结束不能早于午休开始"
        }
        if (day.clockIn != null && day.lunchStart != null && day.lunchStart.isBefore(day.clockIn)) {
            return "午休开始不能早于上班时间"
        }
        if (day.actualClockOut != null && day.clockIn == null) return "请先记录上班时间"
        if (day.clockIn != null && day.actualClockOut != null && day.actualClockOut.isBefore(day.clockIn)) {
            return "下班不能早于上班"
        }
        if (day.lunchMode == LunchMode.ACTUAL && day.actualClockOut != null && day.lunchStart != null && day.lunchEnd == null) {
            return "请先结束午休，再记录下班"
        }
        if (day.lunchEnd != null && day.actualClockOut != null && day.actualClockOut.isBefore(day.lunchEnd)) {
            return "下班不能早于午休结束"
        }
        return null
    }

    fun resolveTime(
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId,
        notBefore: Instant? = null,
        rollToNextDay: Boolean = true,
    ): Instant {
        var resolved = ZonedDateTime.of(date, time, zone).toInstant()
        while (rollToNextDay && notBefore != null && resolved.isBefore(notBefore)) {
            resolved = resolved.plus(Duration.ofDays(1))
        }
        return resolved
    }

    fun dateKey(now: Instant, zone: ZoneId): String = now.atZone(zone).toLocalDate().toString()

    private fun durationMinutes(start: Instant, end: Instant): Long =
        max(0, Duration.between(start, end).toMinutes())
}
