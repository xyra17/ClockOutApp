package com.clockout.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.WorkDay
import java.time.Instant

@Entity(tableName = "work_days", indices = [Index(value = ["dateKey"], unique = true)])
data class WorkDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val clockInEpochMillis: Long?,
    val lunchMode: String,
    val plannedLunchMinutes: Int,
    val lunchStartEpochMillis: Long?,
    val lunchEndEpochMillis: Long?,
    val workMinutes: Int,
    val expectedClockOutEpochMillis: Long?,
    val actualClockOutEpochMillis: Long?,
    val isRestDay: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val zoneId: String,
)

fun WorkDayEntity.toDomain() = WorkDay(
    id = id,
    dateKey = dateKey,
    clockIn = clockInEpochMillis?.let(Instant::ofEpochMilli),
    lunchMode = runCatching { LunchMode.valueOf(lunchMode) }.getOrDefault(LunchMode.FIXED),
    plannedLunchMinutes = plannedLunchMinutes,
    lunchStart = lunchStartEpochMillis?.let(Instant::ofEpochMilli),
    lunchEnd = lunchEndEpochMillis?.let(Instant::ofEpochMilli),
    workMinutes = workMinutes,
    expectedClockOut = expectedClockOutEpochMillis?.let(Instant::ofEpochMilli),
    actualClockOut = actualClockOutEpochMillis?.let(Instant::ofEpochMilli),
    isRestDay = isRestDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    zoneId = zoneId,
)

fun WorkDay.toEntity() = WorkDayEntity(
    id, dateKey, clockIn?.toEpochMilli(), lunchMode.name, plannedLunchMinutes,
    lunchStart?.toEpochMilli(), lunchEnd?.toEpochMilli(), workMinutes,
    expectedClockOut?.toEpochMilli(), actualClockOut?.toEpochMilli(), isRestDay,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(), zoneId,
)
