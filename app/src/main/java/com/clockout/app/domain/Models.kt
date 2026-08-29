package com.clockout.app.domain

import java.time.Instant

enum class LunchMode { FIXED, ACTUAL }

enum class AppThemeStyle { SILVER, MIST, MIDNIGHT, OCEAN, TEA }

enum class AppFontStyle { SYSTEM, NOTEBOOK, SERIF }

/** Bounds used by the quick clock-in editor on today's screen. */
object ClockInWindow {
    const val MINUTE_MIN = 8 * 60 + 30
    const val MINUTE_MAX = 9 * 60 + 10
    const val DEFAULT_MINUTE = 9 * 60

    fun contains(minuteOfDay: Int): Boolean = minuteOfDay in MINUTE_MIN..MINUTE_MAX
}

/** New and unfinished records use this fixed-lunch range; completed history is left untouched. */
object LunchDurationLimits {
    const val MIN_MINUTES = 0
    const val MAX_MINUTES = 90
    const val STEP_MINUTES = 5
    const val DEFAULT_MINUTES = 90
}

enum class DayStatus {
    NOT_STARTED, MORNING_WORK, ON_BREAK, AFTERNOON_WORK, FINISHED, REST_DAY
}

data class WorkDay(
    val id: Long = 0,
    val dateKey: String,
    val clockIn: Instant? = null,
    val lunchMode: LunchMode = LunchMode.FIXED,
    val plannedLunchMinutes: Int = LunchDurationLimits.DEFAULT_MINUTES,
    val lunchStart: Instant? = null,
    val lunchEnd: Instant? = null,
    val workMinutes: Int = 480,
    val expectedClockOut: Instant? = null,
    val actualClockOut: Instant? = null,
    val isRestDay: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val zoneId: String,
)

data class WorkSummary(
    val expectedClockOut: Instant?,
    val effectiveLunchMinutes: Long,
    val actualWorkMinutes: Long?,
    val differenceFromExpectedMinutes: Long?,
)

data class AppSettings(
    val workMinutes: Int = 480,
    val lunchMode: LunchMode = LunchMode.ACTUAL,
    val lunchMinutes: Int = LunchDurationLimits.DEFAULT_MINUTES,
    val reminderEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 10,
    val hapticsEnabled: Boolean = true,
    val use24Hour: Boolean = true,
    val showLunchControls: Boolean = true,
    val themeStyle: AppThemeStyle = AppThemeStyle.SILVER,
    val fontStyle: AppFontStyle = AppFontStyle.SYSTEM,
)

fun WorkDay.status(): DayStatus = when {
    isRestDay -> DayStatus.REST_DAY
    actualClockOut != null -> DayStatus.FINISHED
    clockIn == null -> DayStatus.NOT_STARTED
    lunchStart == null -> DayStatus.MORNING_WORK
    lunchEnd == null -> DayStatus.ON_BREAK
    else -> DayStatus.AFTERNOON_WORK
}
