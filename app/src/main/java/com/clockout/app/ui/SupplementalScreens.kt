package com.clockout.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.clockout.app.BuildConfig
import com.clockout.app.domain.*
import com.clockout.app.ui.theme.ClockOutVisuals
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

private val SGlass: Color @Composable get() = ClockOutVisuals.colors.glass
private val SBorder: Color @Composable get() = ClockOutVisuals.colors.border
private val SText: Color @Composable get() = ClockOutVisuals.colors.text
private val SMuted: Color @Composable get() = ClockOutVisuals.colors.muted
private val SWeak: Color @Composable get() = ClockOutVisuals.colors.weak
private val SAccent: Color @Composable get() = ClockOutVisuals.colors.accent
private val SAccentStrong: Color @Composable get() = ClockOutVisuals.colors.accentStrong
private val SGreen: Color @Composable get() = ClockOutVisuals.colors.positive
private val SRose: Color @Composable get() = ClockOutVisuals.colors.danger

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RecordsScreen(records: List<WorkDay>, vm: ClockOutViewModel, onOpen: (WorkDay) -> Unit) {
    val today = remember { LocalDate.now() }
    val weekStart = remember(today) { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val weekEnd = remember(weekStart) { weekStart.plusDays(6) }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WorkDay?>(null) }
    val weeklyRecords = remember(records, weekStart, weekEnd) {
        records.filter { day -> day.isVisibleHistoryRecord() && runCatching { LocalDate.parse(day.dateKey) }.getOrNull()?.let { !it.isBefore(weekStart) && !it.isAfter(weekEnd) } == true }
    }
    val filtered = remember(records, selectedDate, weeklyRecords) { if (selectedDate != null) records.filter { it.isVisibleHistoryRecord() && it.dateKey == selectedDate } else weeklyRecords }
    val weeklyMinutes = remember(weeklyRecords) { weeklyRecords.sumOf { WorkTimeCalculator.actualWorkMinutes(it) ?: 0L } }
    val currentMonth = remember(today) { YearMonth.from(today) }
    val monthlyMinutes = remember(records, currentMonth) {
        records.filter { it.isVisibleHistoryRecord() && runCatching { YearMonth.from(LocalDate.parse(it.dateKey)) }.getOrNull() == currentMonth }
            .sumOf { WorkTimeCalculator.actualWorkMinutes(it) ?: 0L }
    }
    ScreenColumn(scrollable = false) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("记录", color = SText, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.4).sp); Text(if (selectedDate == null) "本周 · 周一至周日" else "按日期查看", color = SMuted, fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.clip(RoundedCornerShape(99.dp)).clickable { showDatePicker = true }.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, "选择记录日期", tint = SMuted, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text("选择记录日期", color = SText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(selectedDate?.let { formatRecordDate(LocalDate.parse(it)) } ?: "${formatShortDate(weekStart)} — ${formatShortDate(weekEnd)}", color = SAccentStrong, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (selectedDate != null) TextButton(onClick = { selectedDate = null }) { Text("返回本周", fontSize = 12.sp) }
        }
        if (filtered.isEmpty()) RecordEmptyState(selectedDate, Modifier.weight(1f))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(filtered, key = { it.id }) { day -> SwipeRecordCard(day, vm, onOpen, onDelete = { pendingDelete = day }) }
        }
        var showMonthly by rememberSaveable { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().border(1.dp, SBorder, RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(SText.copy(alpha = .06f), SText.copy(alpha = .02f))), RoundedCornerShape(16.dp)).clickable { showMonthly = !showMonthly }.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (showMonthly) "本月累计工时" else "本周累计工时", color = SMuted, fontSize = 12.sp)
            ToggleDurationText(null, if (showMonthly) monthlyMinutes else weeklyMinutes, "period-total")
        }
    }
    if (showDatePicker) {
        val initialMillis = selectedDate?.let { LocalDate.parse(it) }?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis -> selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                showDatePicker = false
            }) { Text("查看") }
        }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }) { DatePicker(state = pickerState, colors = DatePickerDefaults.colors(containerColor = SGlass, titleContentColor = SText, headlineContentColor = SText, weekdayContentColor = SMuted, subheadContentColor = SMuted, dayContentColor = SText, dayInSelectionRangeContentColor = SText, selectedDayContentColor = ClockOutVisuals.colors.onAccent, selectedDayContainerColor = SAccent, todayContentColor = SAccent, todayDateBorderColor = SAccent), title = { Text("选择记录日期", color = SText, modifier = Modifier.padding(start = 24.dp, top = 18.dp)) }) }
    }
    pendingDelete?.let { day ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("删除 ${day.dateKey} 的记录？") }, text = { Text("左滑删除将移除这一天的全部打卡数据，此操作无法恢复。") }, confirmButton = { TextButton(onClick = { vm.deleteRecord(day); pendingDelete = null }) { Text("确认删除", color = SRose) } }, dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }, containerColor = SGlass, titleContentColor = SText, textContentColor = SMuted)
    }
}

private fun WorkDay.isVisibleHistoryRecord(): Boolean = isRestDay || clockIn != null || lunchStart != null || lunchEnd != null || actualClockOut != null

@Composable
private fun ToggleDurationText(label: String?, minutes: Long, stateKey: String) {
    var showMinutes by rememberSaveable(stateKey) { mutableStateOf(false) }
    val value = if (showMinutes) "$minutes 分钟" else "${decimalHours(minutes)} 小时"
    Text(
        text = listOfNotNull(label, value).joinToString("  "),
        color = SText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showMinutes = !showMinutes }.padding(horizontal = 5.dp, vertical = 3.dp),
    )
}

private fun decimalHours(minutes: Long): String = String.format(Locale.US, "%.2f", minutes / 60.0).trimEnd('0').trimEnd('.')

@Composable private fun RecordEmptyState(selectedDate: String?, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = SGlass), shape = RoundedCornerShape(24.dp), modifier = modifier.fillMaxWidth().border(1.dp, SBorder, RoundedCornerShape(24.dp))) {
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CalendarMonth, null, tint = SMuted, modifier = Modifier.size(44.dp)); Spacer(Modifier.height(14.dp))
            Text(if (selectedDate == null) "本周还没有记录" else "这一天没有记录", color = SText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(if (selectedDate == null) "本周完成打卡后，记录会显示在这里。" else "可以选择其他日期，或返回本周查看。", color = SMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable private fun SwipeRecordCard(day: WorkDay, vm: ClockOutViewModel, onOpen: (WorkDay) -> Unit, onDelete: () -> Unit) {
    var offset by remember(day.id) { mutableStateOf(0f) }
    val maxOffset = with(LocalDensity.current) { 96.dp.toPx() }
    val triggerOffset = with(LocalDensity.current) { 72.dp.toPx() }
    val animated by androidx.compose.animation.core.animateFloatAsState(offset, androidx.compose.animation.core.tween(160), label = "record-swipe")
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).pointerInput(day.id) {
        detectHorizontalDragGestures(onHorizontalDrag = { change, amount -> change.consume(); offset = (offset + amount).coerceIn(-maxOffset, 0f) }, onDragEnd = { if (offset < -triggerOffset) onDelete(); offset = 0f }, onDragCancel = { offset = 0f })
    }) {
        if (offset < -4f) Row(Modifier.matchParentSize().background(Color(0xFF292225)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) { Text("左滑删除", color = SRose, fontSize = 13.sp); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Delete, null, tint = SRose) }
        Card(colors = CardDefaults.cardColors(containerColor = SGlass), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().offset { IntOffset(animated.roundToInt(), 0) }.border(1.dp, SBorder, RoundedCornerShape(20.dp)).clickable { onOpen(day) }) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatRecordDate(LocalDate.parse(day.dateKey)), color = SText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(statusLabel(day), color = if (day.isRestDay) SMuted else SGreen, fontSize = 12.sp)
                }
                val actualWork = WorkTimeCalculator.actualWorkMinutes(day)
                val difference = WorkTimeCalculator.summarize(day).differenceFromExpectedMinutes
                val metrics = listOf(
                    "上班" to shortTime(day.clockIn, day.zoneId, vm),
                    "午休" to lunchLabel(day, vm),
                    "预计下班" to vm.formattedExpected(day, day.updatedAt).replace("次日 ", "次 "),
                    "实际下班" to shortTime(day.actualClockOut, day.zoneId, vm),
                    "今日工时" to (actualWork?.let(::compactDuration) ?: "进行中"),
                    "超出预计" to differenceLabel(difference),
                )
                metrics.chunked(3).forEach { metricRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        metricRow.forEach { (label, value) -> RecordMetric(label, value, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private fun formatShortDate(date: LocalDate) = date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
private fun formatRecordDate(date: LocalDate) = date.format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA))
@Composable private fun RecordMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.heightIn(min = 56.dp).background(SText.copy(alpha = .04f), RoundedCornerShape(13.dp)).border(1.dp, SText.copy(alpha = .075f), RoundedCornerShape(13.dp)).padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SMuted, fontSize = 10.sp, maxLines = 1)
        Text(value, color = SText, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
private fun shortTime(instant: Instant?, zoneId: String, vm: ClockOutViewModel) = if (instant == null) "—" else vm.formatInstant(instant, zoneId).substringAfter(" ")
private fun compactDuration(minutes: Long): String { val h = minutes / 60; val m = minutes % 60; return when { h > 0 && m > 0 -> "${h}时${m}分"; h > 0 -> "${h}小时"; else -> "${m}分钟" } }
private fun differenceLabel(minutes: Long?) = when { minutes == null -> "—"; minutes > 0 -> "+${compactDuration(minutes)}"; minutes < 0 -> "-${compactDuration(-minutes)}"; else -> "准时" }
private fun lunchLabel(day: WorkDay, vm: ClockOutViewModel) = if (day.lunchMode == LunchMode.ACTUAL && day.lunchStart != null && day.lunchEnd != null) vm.formatDuration(Duration.between(day.lunchStart, day.lunchEnd).toMinutes()) else "${day.plannedLunchMinutes}分钟"
private fun statusLabel(day: WorkDay) = when (day.status()) { DayStatus.REST_DAY -> "休息日"; DayStatus.FINISHED -> "已完成"; DayStatus.ON_BREAK -> "午休中"; DayStatus.MORNING_WORK -> "上午工作"; DayStatus.AFTERNOON_WORK -> "下午工作"; DayStatus.NOT_STARTED -> "未上班" }

@Composable fun DetailScreen(day: WorkDay, vm: ClockOutViewModel, onBack: () -> Unit) {
    var deleteConfirm by remember { mutableStateOf(false) }
    ScreenColumn { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回", tint = SText) }; Column { Text(day.dateKey, color = SText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Text(statusLabel(day), color = SGreen, fontSize = 13.sp) } }
        Card(colors = CardDefaults.cardColors(containerColor = SGlass), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, SBorder, RoundedCornerShape(20.dp))) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { DetailRow("上班时间", vm.formatInstant(day.clockIn, day.zoneId)); DetailRow("午休开始", vm.formatInstant(day.lunchStart, day.zoneId)); DetailRow("午休结束", vm.formatInstant(day.lunchEnd, day.zoneId)); DetailRow("预计下班", vm.formattedExpected(day, day.updatedAt)); DetailRow("实际下班", vm.formatInstant(day.actualClockOut, day.zoneId)); DetailRow("实际工作时长", day.actualClockOut?.let { vm.formatDuration(WorkTimeCalculator.actualWorkMinutes(day) ?: 0) } ?: "—"); day.actualClockOut?.let { val diff = WorkTimeCalculator.summarize(day).differenceFromExpectedMinutes ?: 0; DetailRow("与预计下班差值", if (diff >= 0) "超出 ${vm.formatDuration(diff)}" else "提前 ${vm.formatDuration(-diff)}") } } }
        Button(onClick = { deleteConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292225), contentColor = SRose), modifier = Modifier.fillMaxWidth().border(1.dp, SRose.copy(alpha = .35f), RoundedCornerShape(18.dp))) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("删除这天记录") }
        Text("点击下方字段可进行补录或修改。", color = SMuted, fontSize = 13.sp)
        listOf("in" to "上班", "lunchStart" to "午休开始", "lunchEnd" to "午休结束", "out" to "下班").forEach { (field, label) -> EditableTimeRow(label, field, day, vm) }
        Text("当日工作时长", color = SText, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(420, 450, 480, 510).forEach { mins -> FilterChip(selected = day.workMinutes == mins, onClick = { vm.setDayWorkMinutes(day.dateKey, mins) }, label = { Text(if (mins % 60 == 0) "${mins / 60}小时" else "${mins / 60}.5小时") }) } }
        var lunchDraft by remember(day.id, day.plannedLunchMinutes) { mutableIntStateOf(day.plannedLunchMinutes.coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES)) }
        Text("固定午休时长 · $lunchDraft 分钟", color = SText, fontWeight = FontWeight.SemiBold)
        Slider(value = lunchDraft.toFloat(), onValueChange = { lunchDraft = ((it / LunchDurationLimits.STEP_MINUTES).toInt() * LunchDurationLimits.STEP_MINUTES) }, onValueChangeFinished = { vm.setDayLunchMinutes(day.dateKey, lunchDraft) }, valueRange = LunchDurationLimits.MIN_MINUTES.toFloat()..LunchDurationLimits.MAX_MINUTES.toFloat(), steps = 17)
        SwitchRow("标记为休息日", day.isRestDay) { vm.setRestDay(day.dateKey, it) }
    }
    if (deleteConfirm) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("删除这天记录？") }, text = { Text("此操作无法恢复。") }, confirmButton = { TextButton(onClick = { vm.deleteRecord(day); deleteConfirm = false; onBack() }) { Text("删除", color = SRose) } }, dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } }, containerColor = SGlass, titleContentColor = SText, textContentColor = SMuted)
}
@Composable private fun DetailRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = SMuted); Text(value, color = SText, fontWeight = FontWeight.Medium) } }
@Composable private fun EditableTimeRow(label: String, field: String, day: WorkDay, vm: ClockOutViewModel) { var value by remember(day.id, field) { mutableStateOf(vm.formatInstant(when (field) { "in" -> day.clockIn; "lunchStart" -> day.lunchStart; "lunchEnd" -> day.lunchEnd; else -> day.actualClockOut }, day.zoneId).substringAfter(" ").ifBlank { "09:00" }) }; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, color = SMuted, modifier = Modifier.weight(1f)); OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, modifier = Modifier.width(120.dp), label = { Text("H:mm") }); Spacer(Modifier.width(8.dp)); IconButton(onClick = { vm.saveRecord(day, field, value) }) { Icon(Icons.Default.Check, "保存") } } }

@Composable fun SettingsScreen(settings: AppSettings, vm: ClockOutViewModel, onDeleteAll: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var customWork by remember(settings.workMinutes) { mutableStateOf(hoursInput(settings.workMinutes)) }
    var workError by remember { mutableStateOf<String?>(null) }
    var customLead by remember(settings.reminderLeadMinutes) { mutableStateOf(settings.reminderLeadMinutes.toString()) }
    fun saveWork() {
        val hours = customWork.replace(',', '.').toDoubleOrNull()
        if (hours == null || hours <= 0.0 || hours > 24.0) workError = "请输入 0～24 之间的小时数"
        else { workError = null; vm.updateSettings { it.copy(workMinutes = (hours * 60).roundToInt().coerceIn(1, 1440)) } }
    }
    ScreenColumn { Text("设置", color = SText, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.4).sp); Text("外观、工时与仅保存在本机的偏好", color = SMuted)
        SettingCard("外观主题", "五套低饱和玻璃主题，减少大块实色带来的压迫感") {
            AppThemeStyle.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { style -> ThemeChoice(style, settings.themeStyle == style, Modifier.weight(1f)) { vm.updateSettings { it.copy(themeStyle = style) } } }
                }
            }
        }
        SettingCard("字体", "选择阅读风格，立即应用到整个界面") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppFontStyle.SYSTEM to "系统", AppFontStyle.NOTEBOOK to "手帐", AppFontStyle.SERIF to "衬线").forEach { (font, label) ->
                    FilterChip(selected = settings.fontStyle == font, onClick = { vm.updateSettings { it.copy(fontStyle = font) } }, label = { Text(label, maxLines = 1) }, modifier = Modifier.weight(1f))
                }
            }
        }
        SettingCard("默认工作时长", "输入 8 即为 8 小时，输入 7.5 即为 7 小时 30 分钟") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = customWork, onValueChange = { raw -> customWork = normalizedDecimalInput(raw); workError = null }, placeholder = { Text("8") }, suffix = { Text("小时", color = SMuted) }, supportingText = workError?.let { message -> { Text(message, color = SRose) } }, isError = workError != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { saveWork() }), shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SAccentStrong.copy(alpha = .72f), unfocusedBorderColor = SBorder, focusedContainerColor = SText.copy(alpha = .035f), unfocusedContainerColor = SText.copy(alpha = .02f)), modifier = Modifier.weight(1f).height(58.dp)); Spacer(Modifier.width(8.dp)); Button(onClick = { saveWork() }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SAccent, contentColor = ClockOutVisuals.colors.onAccent), modifier = Modifier.height(48.dp)) { Text("保存") }
            }
        }
        SettingCard("首页午休调整", "控制首页是否显示午休模式、滑杆与快捷时长；不会删除已有午休记录") { SwitchRow("在首页显示午休调整", settings.showLunchControls) { value -> vm.updateSettings { it.copy(showLunchControls = value) } } }
        SettingCard("临近下班通知", "仅发送 ClockOut 本机 App 通知，不联网；触发精度可能受系统省电策略影响") { SwitchRow("开启本地通知", settings.reminderEnabled) { enabled -> vm.updateSettings { it.copy(reminderEnabled = enabled) }; if (enabled && Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }; if (settings.reminderEnabled) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5, 10, 30, 0).forEach { lead -> FilterChip(selected = settings.reminderLeadMinutes == lead, onClick = { vm.updateSettings { it.copy(reminderLeadMinutes = lead) } }, label = { Text(if (lead == 0) "到点通知" else "提前 $lead 分钟") }) } }; Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = customLead, onValueChange = { customLead = it.filter(Char::isDigit) }, label = { Text("自定义提前分钟") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); TextButton(onClick = { customLead.toIntOrNull()?.let { lead -> vm.updateSettings { it.copy(reminderLeadMinutes = lead.coerceIn(0, 1440)) } } }, colors = ButtonDefaults.textButtonColors(contentColor = SAccentStrong)) { Text("应用", fontWeight = FontWeight.SemiBold) } } } }
        SettingCard("体验", "保持界面安静，也可以关闭触感") { SwitchRow("震动反馈", settings.hapticsEnabled) { value -> vm.updateSettings { it.copy(hapticsEnabled = value) } }; SwitchRow("24 小时制显示", settings.use24Hour) { value -> vm.updateSettings { it.copy(use24Hour = value) } } }
        SettingCard("数据与隐私", "ClockOut 不联网、不登录、不使用广告或统计 SDK。数据只存储在应用私有目录。") { OutlinedButton(onClick = onDeleteAll, colors = ButtonDefaults.outlinedButtonColors(contentColor = SRose), modifier = Modifier.fillMaxWidth()) { Text("删除全部数据") } }
        Text("ClockOut ${BuildConfig.VERSION_NAME} · 个人使用版", color = SMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

private fun hoursInput(minutes: Int): String = decimalHours(minutes.toLong())
private fun normalizedDecimalInput(raw: String): String {
    val replaced = raw.replace(',', '.')
    val filtered = replaced.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) filtered else filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).replace(".", "")
}

@Composable
private fun ThemeChoice(style: AppThemeStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val preview = when (style) {
        AppThemeStyle.SILVER -> Triple(Color(0xFF0A0B0D), Color(0xFF34373D), Color(0xFFD9DBDF))
        AppThemeStyle.MIST -> Triple(Color(0xFFF2F6F8), Color(0xFFDDE8EE), Color(0xFF597487))
        AppThemeStyle.OCEAN -> Triple(Color(0xFF15120F), Color(0xFF46372B), Color(0xFFEED6B8))
        AppThemeStyle.MIDNIGHT -> Triple(Color(0xFF07111B), Color(0xFF1D3A50), Color(0xFF8FB6CE))
        AppThemeStyle.TEA -> Triple(Color(0xFFF8F8F6), Color(0xFFE4E4E0), Color(0xFF222222))
    }
    val title = when (style) { AppThemeStyle.SILVER -> "银灰"; AppThemeStyle.MIST -> "晨雾"; AppThemeStyle.OCEAN -> "暖砂"; AppThemeStyle.MIDNIGHT -> "深海"; AppThemeStyle.TEA -> "黑白" }
    val lightPreview = style == AppThemeStyle.TEA || style == AppThemeStyle.MIST
    val titleColor = if (lightPreview) Color(0xFF161616) else Color.White
    val neutralBorder = if (lightPreview) Color.Black.copy(alpha = .16f) else Color.White.copy(alpha = .12f)
    Column(modifier.height(88.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(preview.first, preview.second))).border(if (selected) 1.5.dp else 1.dp, if (selected) preview.third else neutralBorder, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); if (selected) Icon(Icons.Default.Check, "已选择", tint = preview.third, modifier = Modifier.size(17.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(preview.third, if (lightPreview) Color.Black.copy(alpha = .24f) else Color.White.copy(alpha = .72f), preview.second).forEach { color -> Box(Modifier.size(9.dp).clip(RoundedCornerShape(99.dp)).background(color)) } }
    }
}
@Composable private fun SettingCard(title: String, subtitle: String, content: @Composable () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = SGlass), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, SBorder, RoundedCornerShape(20.dp))) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, color = SText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = SMuted, fontSize = 12.sp); content() } } }
@Composable private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = SText); Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SAccent, checkedBorderColor = SAccent, uncheckedThumbColor = SMuted, uncheckedTrackColor = SGlass, uncheckedBorderColor = SBorder)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditDaySheet(day: WorkDay, vm: ClockOutViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SGlass, contentColor = SText) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("编辑今日记录", color = SText, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            val zone = remember(day.zoneId) { runCatching { java.time.ZoneId.of(day.zoneId) }.getOrDefault(java.time.ZoneId.systemDefault()) }
            val savedMinute = day.clockIn?.atZone(zone)?.toLocalTime()?.let { it.hour * 60 + it.minute } ?: ClockInWindow.DEFAULT_MINUTE
            var clockInMinute by remember(day.id, day.clockIn) { mutableIntStateOf(savedMinute) }
            ClockInTimeEditor(label = "打卡时间", minuteOfDay = clockInMinute, onMinuteChange = { clockInMinute = it }, onCommit = vm::clockInAtMinute)
            listOf("lunchStart" to "午休开始", "lunchEnd" to "午休结束", "out" to "下班").forEach { (field, label) -> EditableTimeRow(label, field, day, vm) }
            Spacer(Modifier.height(20.dp))
        }
    }
}
