package com.clockout.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clockout.app.ClockOutApplication
import com.clockout.app.data.ClockOutRepository
import com.clockout.app.domain.AppSettings
import com.clockout.app.domain.DayStatus
import com.clockout.app.domain.ClockInWindow
import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.LunchDurationLimits
import com.clockout.app.domain.WorkDay
import com.clockout.app.domain.WorkTimeCalculator
import com.clockout.app.domain.status
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClockOutUiState(
    val now: Instant = Instant.now(),
    val today: WorkDay? = null,
    val settings: AppSettings = AppSettings(),
    val records: List<WorkDay> = emptyList(),
    val previousOpen: List<WorkDay> = emptyList(),
    val error: String? = null,
    val message: String? = null,
)

class ClockOutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ClockOutRepository = (application as ClockOutApplication).repository
    private val now = MutableStateFlow(Instant.now())
    private val today = MutableStateFlow<WorkDay?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ClockOutUiState> = combine(
        now, today, repository.settings, repository.records, repository.openPreviousRecords, error, message,
    ) { values -> ClockOutUiState(values[0] as Instant, values[1] as WorkDay?, values[2] as AppSettings, values[3] as List<WorkDay>, values[4] as List<WorkDay>, values[5] as String?, values[6] as String?) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClockOutUiState())

    init {
        viewModelScope.launch { repository.migrateV2Defaults() }
        viewModelScope.launch {
            while (true) { now.value = Instant.now(); delay(30_000) }
        }
        viewModelScope.launch {
            repository.observeToday().collect { day ->
                if (day != null && day.actualClockOut == null && day.plannedLunchMinutes > LunchDurationLimits.MAX_MINUTES) {
                    repository.updateDay(day.dateKey) {
                        it.copy(plannedLunchMinutes = LunchDurationLimits.MAX_MINUTES)
                    }
                } else {
                    today.value = day
                    repository.refreshReminder(day)
                }
            }
        }
    }

    fun clearNotice() { error.value = null; message.value = null }

    fun clockIn() = mutateToday { it.copy(clockIn = Instant.now()) }
    fun clockInAtMinute(minuteOfDay: Int) {
        if (!ClockInWindow.contains(minuteOfDay)) {
            error.value = "上班时间需在 08:30～09:10 之间"
            return
        }
        val date = LocalDate.now()
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        val zone = ZoneId.systemDefault()
        val requested = WorkTimeCalculator.resolveTime(date, time, zone)
        if (requested.isAfter(Instant.now())) {
            error.value = "不能输入未来的打卡时间"
            return
        }
        viewModelScope.launch {
            val suggestedLunch = repository.suggestedLunchMinutes(date.toString())
            val result = repository.updateDay(date.toString()) { day ->
                day.copy(
                    clockIn = requested,
                    plannedLunchMinutes = if (day.clockIn == null && day.lunchStart == null) suggestedLunch else day.plannedLunchMinutes,
                )
            }
            handle(result, "上班时间已保存")
        }
    }
    fun startLunch() = mutateToday { it.copy(lunchStart = Instant.now()) }
    fun endLunch() = mutateToday { it.copy(lunchEnd = Instant.now()) }
    fun clockOut() = mutateToday { it.copy(actualClockOut = Instant.now()) }
    fun toggleRestDay() = mutateToday { it.copy(isRestDay = !it.isRestDay, clockIn = if (!it.isRestDay) null else it.clockIn) }

    fun setLunchMode(mode: LunchMode) {
        if (today.value?.clockIn == null) updateSettings { it.copy(lunchMode = mode) }
        else mutateToday { it.copy(lunchMode = mode) }
    }
    fun setLunchMinutes(minutes: Int) {
        val bounded = minutes.coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES)
        if (today.value?.clockIn == null) updateSettings { it.copy(lunchMinutes = bounded) }
        else mutateToday { it.copy(plannedLunchMinutes = bounded) }
    }

    fun setDayWorkMinutes(dateKey: String, minutes: Int) {
        viewModelScope.launch { handle(repository.updateDay(dateKey) { it.copy(workMinutes = minutes.coerceIn(1, 1_440)) }, "当日工作时长已保存") }
    }

    fun setDayLunchMinutes(dateKey: String, minutes: Int) {
        viewModelScope.launch {
            handle(
                repository.updateDay(dateKey) {
                    it.copy(plannedLunchMinutes = minutes.coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES))
                },
                "当日午休时长已保存",
            )
        }
    }

    fun setRestDay(dateKey: String, rest: Boolean) {
        viewModelScope.launch { handle(repository.updateDay(dateKey) { it.copy(isRestDay = rest) }, if (rest) "已标记休息日" else "已取消休息日") }
    }

    fun saveManualTime(dateKey: String, field: String, text: String) {
        val date = runCatching { LocalDate.parse(dateKey) }.getOrElse { error.value = "日期格式无效"; return }
        val time = runCatching { LocalTime.parse(text.trim(), DateTimeFormatter.ofPattern("H:mm")) }
            .getOrElse { error.value = "请输入类似 09:03 的时间"; return }
        viewModelScope.launch {
            val result = repository.updateDay(dateKey) { day ->
                val zone = ZoneId.of(day.zoneId)
                when (field) {
                    "in" -> {
                        val clockIn = WorkTimeCalculator.resolveTime(date, time, zone)
                        if (date == LocalDate.now(zone) && clockIn.isAfter(Instant.now())) {
                            throw IllegalArgumentException("不能输入未来的打卡时间")
                        }
                        day.copy(clockIn = clockIn)
                    }
                    "lunchStart" -> day.copy(lunchStart = WorkTimeCalculator.resolveTime(date, time, zone, day.clockIn, rollToNextDay = false))
                    "lunchEnd" -> day.copy(lunchEnd = WorkTimeCalculator.resolveTime(date, time, zone, day.lunchStart, rollToNextDay = false))
                    "out" -> day.copy(actualClockOut = WorkTimeCalculator.resolveTime(date, time, zone, day.clockIn))
                    else -> day
                }
            }
            handle(result, "时间已保存")
        }
    }

    fun saveRecord(day: WorkDay, field: String, text: String) = saveManualTime(day.dateKey, field, text)

    fun saveManualLunch(startText: String, endText: String) {
        val startMinute = startText.takeIf(String::isNotBlank)?.let(::parseMinuteOfDay)
        val endMinute = endText.takeIf(String::isNotBlank)?.let(::parseMinuteOfDay)
        if ((startText.isNotBlank() && startMinute == null) || (endText.isNotBlank() && endMinute == null)) {
            error.value = "午休时间可输入 1210 或 12:10"
            return
        }
        viewModelScope.launch {
            val date = LocalDate.now()
            val result = repository.updateDay(date.toString()) { day ->
                val zone = ZoneId.of(day.zoneId)
                val resolvedStart = startMinute?.let { minute ->
                    WorkTimeCalculator.resolveTime(date, LocalTime.of(minute / 60, minute % 60), zone, day.clockIn, rollToNextDay = false)
                } ?: day.lunchStart
                val resolvedEnd = endMinute?.let { minute ->
                    WorkTimeCalculator.resolveTime(date, LocalTime.of(minute / 60, minute % 60), zone, resolvedStart, rollToNextDay = false)
                } ?: day.lunchEnd
                day.copy(lunchMode = LunchMode.ACTUAL, lunchStart = resolvedStart, lunchEnd = resolvedEnd)
            }
            handle(result, "午休时间已保存")
        }
    }

    fun deleteRecord(day: WorkDay) {
        viewModelScope.launch { repository.delete(day.id); if (day.dateKey == LocalDate.now().toString()) today.value = null; message.value = "已删除 ${day.dateKey} 的记录" }
    }

    fun deleteAll() { viewModelScope.launch { repository.deleteAllData(); today.value = null; message.value = "全部本地数据已删除" } }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.updateSettings(transform); message.value = "设置已保存" }
    }

    fun statusText(day: WorkDay?, current: Instant = now.value): String {
        if (day == null) return "未打卡：记录上班时间后即可计算"
        if (day.isRestDay) return "休息日"
        return when (day.status()) {
            DayStatus.NOT_STARTED -> "未打卡：记录上班时间后即可计算"
            DayStatus.MORNING_WORK, DayStatus.AFTERNOON_WORK -> countdownText(day, current)
            DayStatus.ON_BREAK -> "午休中：午休已进行 ${formatDuration(Duration.between(day.lunchStart, current).toMinutes())}"
            DayStatus.FINISHED -> "今日已完成打卡"
            DayStatus.REST_DAY -> "休息日"
        }
    }

    fun countdownText(day: WorkDay, current: Instant = now.value): String {
        val expected = WorkTimeCalculator.expectedClockOut(day, current) ?: return "记录上班时间后即可计算"
        val mins = Duration.between(current, expected).toMinutes()
        return when {
            mins > 0 -> if (mins <= 10) "即将下班 · ${mins}分钟" else "距下班还有 ${formatDuration(mins)}"
            mins == 0L -> "到达时间：今天可以下班啦"
            else -> "已超时 · ${formatDuration(-mins)}"
        }
    }

    fun formattedExpected(day: WorkDay?, current: Instant = now.value): String {
        val expected = day?.let { WorkTimeCalculator.expectedClockOut(it, current) } ?: return "--:--"
        // Render the historical rule in the zone captured with the record so a later
        // device time-zone change cannot silently shift old expected times.
        val zoned = expected.atZone(ZoneId.of(day.zoneId))
        val pattern = if (uiState.value.settings.use24Hour) "HH:mm" else "h:mm a"
        val time = zoned.toLocalTime().format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
        val baseDate = runCatching { LocalDate.parse(day.dateKey) }.getOrDefault(LocalDate.now())
        return if (zoned.toLocalDate() == baseDate) time else "次日 $time"
    }

    fun formattedPreparedExpected(minuteOfDay: Int, current: Instant = now.value): String {
        val settings = uiState.value.settings
        val zone = ZoneId.systemDefault()
        val baseDate = current.atZone(zone).toLocalDate()
        val start = baseDate.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(zone)
        val expected = start.plusMinutes((settings.workMinutes + settings.lunchMinutes).toLong())
        val pattern = if (settings.use24Hour) "HH:mm" else "h:mm a"
        val time = expected.toLocalTime().format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
        return if (expected.toLocalDate() == baseDate) time else "次日 $time"
    }

    fun lunchEstimateText(day: WorkDay, records: List<WorkDay>, defaultMinutes: Int): String {
        if (day.lunchMode == LunchMode.FIXED) return "固定午休 ${day.plannedLunchMinutes} 分钟"
        if (day.lunchStart != null && day.lunchEnd != null) {
            val actual = Duration.between(day.lunchStart, day.lunchEnd).toMinutes().coerceAtLeast(0)
            return "已按实际午休 ${actual} 分钟更新"
        }
        if (day.lunchStart != null) return "动态估算 · 午休进行中"
        val previous = records.firstOrNull { it.dateKey < day.dateKey && !it.isRestDay && it.clockIn != null }
        val suggested = WorkTimeCalculator.suggestedLunchMinutes(previous, defaultMinutes)
        return if (previous != null) "估算 · 参考上个工作日 $suggested 分钟" else "估算 · 使用默认午休 $suggested 分钟"
    }

    fun formatInstant(instant: Instant?, zoneId: String = ZoneId.systemDefault().id): String {
        val pattern = if (uiState.value.settings.use24Hour) "MM-dd HH:mm" else "MM-dd h:mm a"
        return instant?.atZone(ZoneId.of(zoneId))?.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) ?: "—"
    }
    fun formatDuration(minutes: Long): String {
        val h = minutes / 60; val m = minutes % 60
        return if (h > 0) "${h}小时${m}分钟" else "${m}分钟"
    }

    private fun mutateToday(transform: (WorkDay) -> WorkDay) {
        viewModelScope.launch { handle(repository.updateDay(LocalDate.now().toString(), transform), "已保存") }
    }

    private fun handle(result: Result<WorkDay>, success: String) {
        result.onSuccess { today.value = it; message.value = success; error.value = null }
            .onFailure { error.value = it.message ?: "保存失败" }
    }
}
