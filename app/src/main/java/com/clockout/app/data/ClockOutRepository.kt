package com.clockout.app.data

import com.clockout.app.domain.AppSettings
import com.clockout.app.domain.LunchDurationLimits
import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.WorkDay
import com.clockout.app.domain.WorkTimeCalculator
import com.clockout.app.notification.ReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClockOutRepository(
    private val dao: WorkDayDao,
    private val settingsStore: SettingsStore,
    private val reminders: ReminderScheduler,
) {
    private val writeMutex = Mutex()

    val settings: Flow<AppSettings> = settingsStore.settings
    val records: Flow<List<WorkDay>> = dao.observeAll().map { rows -> rows.map(WorkDayEntity::toDomain) }

    fun observeDate(dateKey: String): Flow<WorkDay?> = dao.observeByDate(dateKey).map { it?.toDomain() }

    fun observeToday(clock: () -> Instant = Instant::now): Flow<WorkDay?> {
        val key = WorkTimeCalculator.dateKey(clock(), ZoneId.systemDefault())
        return observeDate(key)
    }

    val openPreviousRecords: Flow<List<WorkDay>> = records.map { rows ->
        val today = LocalDate.now().toString()
        rows.filter { it.dateKey < today && it.clockIn != null && it.actualClockOut == null && !it.isRestDay }
    }

    suspend fun ensureToday(now: Instant = Instant.now()): WorkDay = writeMutex.withLock {
        val zone = ZoneId.systemDefault()
        val dateKey = WorkTimeCalculator.dateKey(now, zone)
        dao.getByDate(dateKey)?.toDomain() ?: run {
            val prefs = settings.first()
            val fresh = WorkDay(
                dateKey = dateKey,
                lunchMode = prefs.lunchMode,
                plannedLunchMinutes = suggestedLunchMinutes(dateKey, prefs.lunchMinutes),
                workMinutes = prefs.workMinutes,
                createdAt = now,
                updatedAt = now,
                zoneId = zone.id,
            )
            val id = dao.upsert(fresh.toEntity())
            fresh.copy(id = id)
        }
    }

    suspend fun updateDay(dateKey: String, transform: (WorkDay) -> WorkDay): Result<WorkDay> = writeMutex.withLock {
        runCatching {
            val existing = dao.getByDate(dateKey)?.toDomain() ?: createForDate(dateKey)
            val now = Instant.now()
            var next = transform(existing).copy(updatedAt = now)
            val error = WorkTimeCalculator.validate(next)
            require(error == null) { error ?: "记录无效" }
            next = next.copy(expectedClockOut = WorkTimeCalculator.expectedClockOut(next, now))
            val id = dao.upsert(next.toEntity())
            val saved = if (next.id == 0L) next.copy(id = id) else next
            rescheduleIfCurrent(saved)
            saved
        }
    }

    suspend fun updateById(id: Long, transform: (WorkDay) -> WorkDay): Result<WorkDay> {
        val day = dao.getById(id)?.toDomain() ?: return Result.failure(IllegalArgumentException("记录不存在"))
        return updateDay(day.dateKey, transform)
    }

    suspend fun delete(id: Long) = writeMutex.withLock {
        dao.getById(id)?.let { entity ->
            dao.delete(entity)
            if (entity.dateKey == LocalDate.now().toString()) reminders.cancel()
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val previous = settings.first()
        settingsStore.update(transform)
        val current = settings.first()
        val today = LocalDate.now().toString()
        val open = dao.getByDate(today)?.toDomain()
        if (open != null && open.actualClockOut == null && !open.isRestDay) {
            updateDay(today) { day ->
                day.copy(
                    workMinutes = if (previous.workMinutes != current.workMinutes) current.workMinutes else day.workMinutes,
                    lunchMode = if (previous.lunchMode != current.lunchMode && day.clockIn == null) current.lunchMode else day.lunchMode,
                    plannedLunchMinutes = if (previous.lunchMinutes != current.lunchMinutes) current.lunchMinutes else day.plannedLunchMinutes,
                )
            }
        } else {
            reminders.schedule(null, current)
        }
    }

    suspend fun deleteAllData() = writeMutex.withLock {
        reminders.cancel()
        dao.deleteAll()
        settingsStore.clear()
    }

    suspend fun refreshReminder(day: WorkDay?) = reminders.schedule(day, settings.first())

    suspend fun migrateV2Defaults() {
        val migratedLegacyLunch = settingsStore.migrateV2Defaults()
        if (!migratedLegacyLunch) return
        val today = LocalDate.now().toString()
        val current = dao.getByDate(today)?.toDomain() ?: return
        if (current.actualClockOut == null && current.lunchStart == null && current.plannedLunchMinutes == 60) {
            updateDay(today) { it.copy(plannedLunchMinutes = LunchDurationLimits.DEFAULT_MINUTES) }
        }
    }

    suspend fun suggestedLunchMinutes(dateKey: String, defaultMinutes: Int? = null): Int {
        val fallback = defaultMinutes ?: settings.first().lunchMinutes
        val previous = dao.getLatestWorkDayBefore(dateKey)?.toDomain()
        return WorkTimeCalculator.suggestedLunchMinutes(previous, fallback)
    }

    private suspend fun rescheduleIfCurrent(day: WorkDay) {
        if (day.dateKey == LocalDate.now().toString()) reminders.schedule(day, settings.first())
    }

    private suspend fun createForDate(dateKey: String): WorkDay {
        val now = Instant.now()
        val prefs = settings.first()
        return WorkDay(
            dateKey = dateKey,
            lunchMode = prefs.lunchMode,
            plannedLunchMinutes = suggestedLunchMinutes(dateKey, prefs.lunchMinutes),
            workMinutes = prefs.workMinutes,
            createdAt = now,
            updatedAt = now,
            zoneId = ZoneId.systemDefault().id,
        )
    }
}
