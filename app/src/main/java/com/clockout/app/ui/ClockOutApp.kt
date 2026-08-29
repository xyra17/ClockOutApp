package com.clockout.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HolidayVillage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockout.app.domain.AppSettings
import com.clockout.app.domain.AppThemeStyle
import com.clockout.app.domain.ClockInWindow
import com.clockout.app.domain.DayStatus
import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.LunchDurationLimits
import com.clockout.app.domain.WorkDay
import com.clockout.app.domain.WorkTimeCalculator
import com.clockout.app.domain.status
import com.clockout.app.ui.theme.ClockOutTheme
import com.clockout.app.ui.theme.ClockOutVisuals
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PageBg: Color @Composable get() = ClockOutVisuals.colors.background
private val PageBgEnd: Color @Composable get() = ClockOutVisuals.colors.backgroundEnd
private val Glass: Color @Composable get() = ClockOutVisuals.colors.glass
private val GlassSoft: Color @Composable get() = ClockOutVisuals.colors.glassSoft
private val GlassBorder: Color @Composable get() = ClockOutVisuals.colors.border
private val TextPrimary: Color @Composable get() = ClockOutVisuals.colors.text
private val Muted: Color @Composable get() = ClockOutVisuals.colors.muted
private val Weak: Color @Composable get() = ClockOutVisuals.colors.weak
private val Accent: Color @Composable get() = ClockOutVisuals.colors.accent
private val AccentStrong: Color @Composable get() = ClockOutVisuals.colors.accentStrong
private val Green: Color @Composable get() = ClockOutVisuals.colors.positive
private val Rose: Color @Composable get() = ClockOutVisuals.colors.danger

private enum class Tab { TODAY, RECORDS, FORMULAS, SETTINGS }

@Composable
fun ClockOutApp(vm: ClockOutViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    ClockOutTheme(state.settings.themeStyle, state.settings.fontStyle) {
        ClockOutScaffold(vm, state)
    }
}

@Composable
private fun ClockOutScaffold(vm: ClockOutViewModel, state: ClockOutUiState) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message, state.error) {
        (state.message ?: state.error)?.let { snackbar.showSnackbar(it); vm.clearNotice() }
    }
    Scaffold(
        containerColor = PageBg,
        snackbarHost = { SnackbarHost(snackbar) { data -> Snackbar(snackbarData = data, containerColor = Glass, contentColor = TextPrimary, actionColor = AccentStrong, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(12.dp).border(1.dp, GlassBorder, RoundedCornerShape(16.dp))) } },
        bottomBar = {
            GlassNavigation(tab) { tab = it; detailId = null }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(PageBg, PageBgEnd)))) {
            if (state.settings.themeStyle == AppThemeStyle.MIDNIGHT) {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(ClockOutVisuals.colors.glow.copy(alpha = .22f), Color.Transparent), center = Offset(1180f, 180f), radius = 1180f)))
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Accent.copy(alpha = .13f), Color.Transparent), center = Offset(100f, 2450f), radius = 1080f)))
            } else {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(ClockOutVisuals.colors.glow.copy(alpha = .12f), Color.Transparent), radius = 980f)))
            }
            AnimatedContent(targetState = tab to detailId, transitionSpec = { (fadeIn(tween(200, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))) + slideInVertically(tween(210, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))) { it / 30 }) togetherWith fadeOut(tween(120)) }, label = "page") { target ->
                val selected = target.second?.let { id -> state.records.firstOrNull { it.id == id } }
                when {
                    selected != null -> DetailScreen(selected, vm, onBack = { detailId = null })
                    target.first == Tab.TODAY -> TodayScreen(state, vm)
                    target.first == Tab.RECORDS -> RecordsScreen(state.records, vm, onOpen = { detailId = it.id })
                    target.first == Tab.FORMULAS -> FormulaScreen()
                    else -> SettingsScreen(state.settings, vm, onDeleteAll = { showDeleteAll = true })
                }
            }
        }
    }
    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("删除全部数据？") },
            text = { Text("所有本地打卡记录、设置和已安排的提醒都会被永久删除，无法恢复。") },
            confirmButton = { TextButton(onClick = { vm.deleteAll(); showDeleteAll = false }) { Text("删除全部", color = Rose) } },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("取消") } },
            containerColor = Glass,
            titleContentColor = TextPrimary,
            textContentColor = Muted,
        )
    }
}

@Composable private fun GlassNavigation(selected: Tab, onSelect: (Tab) -> Unit) {
    val items = listOf(Tab.TODAY to (Icons.Default.Home to "今日"), Tab.RECORDS to (Icons.Default.WorkHistory to "记录"), Tab.FORMULAS to (Icons.Default.Calculate to "公式"), Tab.SETTINGS to (Icons.Default.Settings to "设置"))
    val selectedIndex = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 12.dp, vertical = 8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(25.dp)).background(ClockOutVisuals.colors.glass.copy(alpha = .96f)).border(1.dp, GlassBorder, RoundedCornerShape(25.dp))) {
            val itemWidth = maxWidth / items.size
            val indicatorX by animateDpAsState(itemWidth * selectedIndex, tween(280, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)), label = "dock-indicator")
            Box(Modifier.offset(x = indicatorX).padding(vertical = 6.dp, horizontal = 6.dp).width(itemWidth - 12.dp).height(56.dp).clip(RoundedCornerShape(19.dp)).background(Brush.verticalGradient(listOf(TextPrimary.copy(alpha = .13f), TextPrimary.copy(alpha = .055f)))).border(1.dp, TextPrimary.copy(alpha = .11f), RoundedCornerShape(19.dp)))
            Row(Modifier.fillMaxSize()) {
                items.forEach { (tab, pair) ->
                    val active = selected == tab
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    Column(Modifier.weight(1f).fillMaxSize().scale(if (pressed) .94f else 1f).clickable(interactionSource = interaction, indication = null) { onSelect(tab) }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(pair.first, pair.second, tint = if (active) TextPrimary else Weak, modifier = Modifier.size(20.dp))
                        Text(pair.second, color = if (active) TextPrimary else Weak, fontSize = 10.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable fun ScreenColumn(scrollable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    // Scaffold already applies safe drawing insets through its content padding.
    // Applying them again here created a conspicuous double top gap on real devices.
    val base = Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 12.dp)
    Column(if (scrollable) base.verticalScroll(rememberScrollState()) else base, verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable private fun TodayScreen(state: ClockOutUiState, vm: ClockOutViewModel) {
    val day = state.today
    var editOpen by remember { mutableStateOf(false) }
    var preparedMinute by rememberSaveable { mutableIntStateOf(ClockInWindow.DEFAULT_MINUTE) }
    LaunchedEffect(day?.clockIn, day?.zoneId) {
        day?.clockIn?.atZone(ZoneId.of(day.zoneId))?.toLocalTime()?.let {
            preparedMinute = it.hour * 60 + it.minute
        }
    }
    val primaryAction: () -> Unit = {
        when (day?.status()) {
            DayStatus.NOT_STARTED, null -> vm.clockInAtMinute(preparedMinute)
            DayStatus.MORNING_WORK, DayStatus.AFTERNOON_WORK -> vm.clockOut()
            DayStatus.ON_BREAK -> vm.endLunch()
            DayStatus.REST_DAY -> vm.toggleRestDay()
            DayStatus.FINISHED -> editOpen = true
        }
    }
    ScreenColumn {
        TodayHeader(day, state.now)
        val finished = day?.status() == DayStatus.FINISHED
        val cardColor by animateColorAsState(Glass, tween(320), label = "card")
        if (day == null || day.clockIn == null) {
            ClockInCard(cardColor, preparedMinute, { preparedMinute = it }, primaryAction)
        } else {
            ExpectedCard(
                day = day,
                vm = vm,
                state = state,
                finished = finished,
                preparedMinute = preparedMinute,
                onPreparedMinuteChange = { preparedMinute = it },
                onPreparedMinuteCommit = vm::clockInAtMinute,
                onPrimaryAction = primaryAction,
            )
        }
        if (state.settings.showLunchControls) LunchControls(day, vm, state.now, state.settings.lunchMinutes, state.settings.hapticsEnabled)
        Timeline(day)
        day?.let { TextButton(onClick = { editOpen = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = AccentStrong)) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("编辑今日记录", fontWeight = FontWeight.SemiBold) } }
        if (state.previousOpen.isNotEmpty()) Card(colors = CardDefaults.cardColors(containerColor = Rose.copy(alpha = .10f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.border(1.dp, Rose.copy(alpha = .35f), RoundedCornerShape(16.dp))) { Text("发现 ${state.previousOpen.size} 条前一天未完成的记录，请到“记录”页补录下班时间。", color = Rose, modifier = Modifier.padding(14.dp), fontSize = 13.sp) }
    }
    if (editOpen && day != null) EditDaySheet(day, vm) { editOpen = false }
}

@Composable
private fun ExpectedCard(
    day: WorkDay,
    vm: ClockOutViewModel,
    state: ClockOutUiState,
    finished: Boolean,
    preparedMinute: Int,
    onPreparedMinuteChange: (Int) -> Unit,
    onPreparedMinuteCommit: (Int) -> Unit,
    onPrimaryAction: () -> Unit,
) {
    val cardColor by animateColorAsState(Glass, tween(320), label = "card")
    var editingClockIn by rememberSaveable(day.id) { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = .95f)), modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, RoundedCornerShape(24.dp)).shadow(9.dp, RoundedCornerShape(24.dp))) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            AnimatedContent(
                targetState = editingClockIn,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 8 }) togetherWith fadeOut(tween(130))
                },
                label = "hero-mode",
            ) { editing ->
                if (editing) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        ClockInTimeEditor(
                            label = "打卡时间",
                            minuteOfDay = preparedMinute,
                            onMinuteChange = onPreparedMinuteChange,
                            onCommit = onPreparedMinuteCommit,
                        )
                        TextButton(onClick = { editingClockIn = false }, colors = ButtonDefaults.textButtonColors(contentColor = AccentStrong), modifier = Modifier.align(Alignment.End)) {
                            Text("返回预计下班", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    ExpectedTimeHero(
                        time = vm.formattedExpected(day, state.now),
                        estimate = vm.lunchEstimateText(day, state.records, state.settings.lunchMinutes),
                        onEditClockIn = { editingClockIn = true },
                    )
                }
            }
            Text(vm.statusText(day, state.now), color = if (finished) Green else Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            BasisChips(day, vm)
            Spacer(Modifier.height(3.dp))
            PrimaryActionButton(day, onPrimaryAction, Modifier.height(50.dp), compact = true)
        }
    }
}

@Composable
private fun ClockInCard(
    cardColor: Color,
    preparedMinute: Int,
    onPreparedMinuteChange: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = .95f)), modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, RoundedCornerShape(24.dp)).shadow(9.dp, RoundedCornerShape(24.dp))) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(AccentStrong.copy(alpha = .78f)))
                Spacer(Modifier.width(9.dp))
                Text("先记录今天的开始", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            ClockInTimeEditor(label = "打卡时间", minuteOfDay = preparedMinute, onMinuteChange = onPreparedMinuteChange, onCommit = {})
            Text("确认后自动参考上个工作日午休；没有历史时使用默认午休。", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(2.dp))
            PrimaryActionButton(null, onConfirm, Modifier.height(50.dp), compact = true)
        }
    }
}

@Composable
private fun ExpectedTimeHero(time: String, estimate: String, onEditClockIn: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(
            Brush.linearGradient(
                listOf(Accent.copy(alpha = .14f), ClockOutVisuals.colors.glow.copy(alpha = .07f), Glass.copy(alpha = .30f)),
            ),
        ).border(1.dp, AccentStrong.copy(alpha = .13f), RoundedCornerShape(22.dp)).padding(start = 17.dp, top = 10.dp, end = 17.dp, bottom = 13.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AccentStrong.copy(alpha = .78f)))
                    Spacer(Modifier.width(8.dp))
                    Text("预计下班", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = .5.sp)
                }
                TextButton(
                    onClick = onEditClockIn,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("修改打卡时间", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            AnimatedContent(targetState = time, transitionSpec = { (fadeIn(tween(180)) + slideInVertically(tween(190)) { it / 5 }) togetherWith fadeOut(tween(110)) }, label = "expected-time") { value ->
                Text(
                    value,
                    color = AccentStrong,
                    fontSize = 40.sp,
                    lineHeight = 45.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.4).sp,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                )
            }
            Text(estimate, color = Weak.copy(alpha = .82f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun ClockInTimeEditor(
    label: String,
    minuteOfDay: Int,
    onMinuteChange: (Int) -> Unit,
    onCommit: (Int) -> Unit,
) {
    var input by remember(minuteOfDay) {
        val formatted = formatMinuteOfDay(minuteOfDay)
        mutableStateOf(TextFieldValue(formatted, TextRange(formatted.length)))
    }
    var inputError by remember { mutableStateOf<String?>(null) }
    val boundedMinute = minuteOfDay.coerceIn(ClockInWindow.MINUTE_MIN, ClockInWindow.MINUTE_MAX)
    val commitInput = {
        val parsed = parseMinuteOfDay(input.text)
        if (parsed == null || !ClockInWindow.contains(parsed)) {
            inputError = "输入 856 或 08:56，范围 08:30～09:10"
        } else {
            inputError = null
            val formatted = formatMinuteOfDay(parsed)
            input = TextFieldValue(formatted, TextRange(formatted.length))
            onMinuteChange(parsed)
            onCommit(parsed)
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(TextPrimary.copy(alpha = .065f), TextPrimary.copy(alpha = .025f)))).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Accent.copy(alpha = .85f), AccentStrong.copy(alpha = .16f)))).border(1.dp, AccentStrong.copy(alpha = .22f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AccessTime, null, tint = AccentStrong, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            BasicTextField(
                value = input,
                onValueChange = { next ->
                    val filtered = normalizeClockInput(input.text, next.text)
                    input = TextFieldValue(filtered, TextRange(filtered.length))
                    inputError = null
                    val digits = filtered.filter(Char::isDigit)
                    if (!filtered.contains(':') && digits.length in 3..4) {
                        parseMinuteOfDay(digits)?.takeIf(ClockInWindow::contains)?.let { parsed ->
                            val formatted = formatMinuteOfDay(parsed)
                            input = TextFieldValue(formatted, TextRange(formatted.length))
                            onMinuteChange(parsed)
                            onCommit(parsed)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitInput() }),
                cursorBrush = SolidColor(AccentStrong),
                modifier = Modifier.width(92.dp).height(42.dp).clip(RoundedCornerShape(13.dp)).background(Glass.copy(alpha = .72f)).border(1.dp, if (inputError == null) AccentStrong.copy(alpha = .22f) else Rose, RoundedCornerShape(13.dp)).padding(horizontal = 10.dp, vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AccentStrong, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (input.text.isEmpty()) Text("如 856", color = Muted, fontSize = 12.sp)
                        innerTextField()
                    }
                },
            )
        }
        Slider(
            value = boundedMinute.toFloat(),
            onValueChange = { raw ->
                val minute = raw.roundToInt().coerceIn(ClockInWindow.MINUTE_MIN, ClockInWindow.MINUTE_MAX)
                inputError = null
                val formatted = formatMinuteOfDay(minute)
                input = TextFieldValue(formatted, TextRange(formatted.length))
                onMinuteChange(minute)
            },
            onValueChangeFinished = { onCommit(boundedMinute) },
            valueRange = ClockInWindow.MINUTE_MIN.toFloat()..ClockInWindow.MINUTE_MAX.toFloat(),
            steps = ClockInWindow.MINUTE_MAX - ClockInWindow.MINUTE_MIN - 1,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = AccentStrong,
                activeTrackColor = AccentStrong.copy(alpha = .70f),
                inactiveTrackColor = TextPrimary.copy(alpha = .14f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("08:30", color = Muted, fontSize = 10.sp)
            Text("09:10", color = Muted, fontSize = 10.sp)
        }
        inputError?.let { Text(it, color = Rose, fontSize = 10.sp, lineHeight = 14.sp) }
        if (!ClockInWindow.contains(minuteOfDay)) {
            Text("原记录 ${formatMinuteOfDay(minuteOfDay)} 超出新范围；拖动或输入后将按新范围保存。", color = Rose, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

internal fun formatMinuteOfDay(minuteOfDay: Int): String = "%02d:%02d".format(
    (minuteOfDay.coerceAtLeast(0) / 60) % 24,
    minuteOfDay.coerceAtLeast(0) % 60,
)

internal fun normalizeClockInput(previous: String, next: String): String {
    val appendingToFormattedValue = previous.contains(':') && next.startsWith(previous) && next.length > previous.length
    val entered = if (appendingToFormattedValue) next.removePrefix(previous) else next
    return entered.filter { it.isDigit() || it == ':' }.take(5)
}

internal fun parseMinuteOfDay(text: String): Int? {
    val trimmed = text.trim()
    val (hour, minute) = if (':' in trimmed) {
        val parts = trimmed.split(':')
        if (parts.size != 2) return null
        (parts[0].toIntOrNull() ?: return null) to (parts[1].toIntOrNull() ?: return null)
    } else {
        if (!trimmed.all(Char::isDigit)) return null
        val digits = trimmed
        when (digits.length) {
            3 -> (digits.take(1).toIntOrNull() ?: return null) to (digits.takeLast(2).toIntOrNull() ?: return null)
            4 -> (digits.take(2).toIntOrNull() ?: return null) to (digits.takeLast(2).toIntOrNull() ?: return null)
            else -> return null
        }
    }
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private data class HeaderCopy(val title: String, val subtitle: String)

private fun headerCopy(day: WorkDay?): HeaderCopy {
    return when (day?.status()) {
        DayStatus.MORNING_WORK -> HeaderCopy("今天已经开始", "第一段工作时间正在安静累积")
        DayStatus.ON_BREAK -> HeaderCopy("先把时间留给午休", "回来时结束午休，预计下班会随之更新")
        DayStatus.AFTERNOON_WORK -> HeaderCopy("下午这一程，继续", "时间线已经接上，按自己的节奏收尾")
        DayStatus.FINISHED -> HeaderCopy("今天收工", "上下班与午休已经完整归档")
        DayStatus.REST_DAY -> HeaderCopy("今天留白", "不计工时，也是一种明确的安排")
        else -> HeaderCopy("今天，从几点开始？", "记下第一刻，剩下的交给 ClockOut")
    }
}

@Composable
private fun TodayHeader(day: WorkDay?, now: Instant) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(40); visible = true }
    val date = now.atZone(ZoneId.systemDefault()).toLocalDate()
    val copy = headerCopy(day)
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))) + slideInVertically(tween(320, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))) { it / 5 }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(RoundedCornerShape(99.dp)).background(TextPrimary.copy(alpha = .055f)).border(1.dp, GlassBorder, RoundedCornerShape(99.dp)).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AccentStrong.copy(alpha = .68f))); Spacer(Modifier.width(8.dp))
                    Text(date.format(DateTimeFormatter.ofPattern("MM / dd", Locale.CHINA)), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .6.sp)
                    Spacer(Modifier.width(8.dp)); Text(date.format(DateTimeFormatter.ofPattern("EEE", Locale.CHINA)), color = Muted, fontSize = 11.sp)
                }
                Text("CLOCKOUT · LOCAL", color = Weak, fontSize = 9.sp, letterSpacing = 1.1.sp)
            }
            AnimatedContent(targetState = copy, transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 4 }) togetherWith fadeOut(tween(130)) }, label = "header-copy") { item ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.title, color = TextPrimary, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
                    Text(item.subtitle, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}
private fun primaryAction(day: WorkDay?) = when (day?.status()) { DayStatus.NOT_STARTED, null -> "确认打卡时间"; DayStatus.MORNING_WORK -> "下班打卡"; DayStatus.ON_BREAK -> "结束午休"; DayStatus.AFTERNOON_WORK -> "下班打卡"; DayStatus.FINISHED -> "查看今日总结"; DayStatus.REST_DAY -> "取消休息日" }

@Composable
private fun PrimaryActionButton(day: WorkDay?, onClick: () -> Unit, modifier: Modifier = Modifier.height(56.dp), compact: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, tween(150), label = "primary-press")
    val radius = if (compact) 15.dp else 18.dp
    Button(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().scale(scale).shadow(if (pressed) 2.dp else 6.dp, RoundedCornerShape(radius), ambientColor = ClockOutVisuals.colors.glow.copy(alpha = .18f), spotColor = ClockOutVisuals.colors.glow.copy(alpha = .18f)).clip(RoundedCornerShape(radius)).background(Brush.verticalGradient(listOf(Accent, Accent.copy(alpha = Accent.alpha * .58f)))).border(1.dp, AccentStrong.copy(alpha = .24f), RoundedCornerShape(radius)).semantics { contentDescription = "今日主要操作" },
        shape = RoundedCornerShape(radius),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ClockOutVisuals.colors.onAccent),
    ) { Text(primaryAction(day), fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable private fun BasisChips(day: WorkDay, vm: ClockOutViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "上班 ${vm.formatInstant(day.clockIn, day.zoneId).substringAfter(" ")}",
            "工作 ${day.workMinutes / 60}小时${if (day.workMinutes % 60 > 0) "${day.workMinutes % 60}分" else ""}",
            "午休 ${day.plannedLunchMinutes / 60}小时${if (day.plannedLunchMinutes % 60 > 0) "${day.plannedLunchMinutes % 60}分" else ""}",
        ).forEach { label -> Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(TextPrimary.copy(alpha = .065f)).padding(horizontal = 3.dp, vertical = 6.dp), contentAlignment = Alignment.Center) { Text(label, color = Muted, fontSize = if (label.length > 7) 9.sp else 10.sp, maxLines = 1, softWrap = false) } }
    }
}

@Composable private fun LunchControls(day: WorkDay?, vm: ClockOutViewModel, now: Instant, defaultLunchMinutes: Int, hapticsEnabled: Boolean) {
    var mode by remember(day?.id, day?.lunchMode) { mutableStateOf(day?.lunchMode ?: LunchMode.FIXED) }
    Card(colors = CardDefaults.cardColors(containerColor = GlassSoft), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, RoundedCornerShape(20.dp))) {
      Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(TextPrimary.copy(alpha = .045f)).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(LunchMode.ACTUAL to "午休打卡", LunchMode.FIXED to "固定午休").forEach { (m, label) ->
                val active = mode == m
                Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (active) Accent else Color.Transparent).border(1.dp, if (active) AccentStrong.copy(alpha = .25f) else GlassBorder, RoundedCornerShape(16.dp)).clickable { mode = m; vm.setLunchMode(m) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) { Text(label, color = if (active) ClockOutVisuals.colors.onAccent else Muted, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
        if (mode == LunchMode.FIXED) {
            val minutes = (day?.plannedLunchMinutes ?: defaultLunchMinutes)
                .coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES)
            val haptics = LocalHapticFeedback.current
            var draftMinutes by remember(day?.id, minutes) { mutableIntStateOf(minutes) }
            var lastTick by remember(day?.id, minutes) { mutableIntStateOf(minutes) }
            Text("固定午休 · $draftMinutes 分钟", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Slider(value = draftMinutes.toFloat(), onValueChange = { raw -> val tick = (raw / LunchDurationLimits.STEP_MINUTES).toInt() * LunchDurationLimits.STEP_MINUTES; if (tick != lastTick) { if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lastTick = tick }; draftMinutes = tick }, onValueChangeFinished = { vm.setLunchMinutes(draftMinutes) }, valueRange = LunchDurationLimits.MIN_MINUTES.toFloat()..LunchDurationLimits.MAX_MINUTES.toFloat(), steps = 17, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = AccentStrong, activeTrackColor = AccentStrong.copy(alpha = .65f), inactiveTrackColor = TextPrimary.copy(alpha = .14f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(0, 30, 60, LunchDurationLimits.MAX_MINUTES).forEach { value -> FilterChip(selected = draftMinutes == value, onClick = { draftMinutes = value; vm.setLunchMinutes(value) }, modifier = Modifier.weight(1f), label = { Text("$value", fontSize = 11.sp, maxLines = 1) }, colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = ClockOutVisuals.colors.onAccent, labelColor = Muted), border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(enabled = true, selected = draftMinutes == value, borderColor = GlassBorder, selectedBorderColor = Accent)) } }
        } else {
            when {
                day?.clockIn == null -> Text("上班后可记录实际午休", color = Muted, fontSize = 13.sp)
                day.lunchStart == null -> Button(onClick = vm::startLunch, modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Accent, Accent.copy(alpha = Accent.alpha * .58f)))).border(1.dp, AccentStrong.copy(alpha = .22f), RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ClockOutVisuals.colors.onAccent)) { Icon(Icons.Default.Restaurant, "开始午休", modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("开始午休") }
                day.lunchEnd == null -> Text("午休已进行 ${vm.formatDuration(Duration.between(day.lunchStart, now).toMinutes())} · 请在预计下班卡片结束午休", color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                else -> Text("实际午休 · ${vm.formatDuration(Duration.between(day.lunchStart, day.lunchEnd).toMinutes())}", color = Muted, fontSize = 13.sp)
            }
            if (day?.clockIn != null) ManualLunchEditor(day, vm)
        }
      }
    }
}

@Composable
private fun ManualLunchEditor(day: WorkDay, vm: ClockOutViewModel) {
    fun initialTime(instant: Instant?): String = instant?.atZone(ZoneId.of(day.zoneId))?.toLocalTime()
        ?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
    var start by remember(day.id, day.lunchStart) { mutableStateOf(initialTime(day.lunchStart)) }
    var end by remember(day.id, day.lunchEnd) { mutableStateOf(initialTime(day.lunchEnd)) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(TextPrimary.copy(alpha = .05f), TextPrimary.copy(alpha = .018f)))).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, null, tint = AccentStrong, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("手动补录午休", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("可输入 1210", color = Weak, fontSize = 9.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            ManualTimeField("开始", start, { start = it }, Modifier.weight(1f))
            ManualTimeField("结束", end, { end = it }, Modifier.weight(1f))
            Button(
                onClick = { vm.saveManualLunch(start, end) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = ClockOutVisuals.colors.onAccent),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 12.dp),
            ) { Text("保存", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ManualTimeField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == ':' }.take(5)) },
        placeholder = { Text(if (label == "开始") "12:00" else "13:30", color = Weak, fontSize = 11.sp) },
        prefix = { Text(label, color = Muted, fontSize = 9.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentStrong.copy(alpha = .58f),
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = Glass.copy(alpha = .60f),
            unfocusedContainerColor = Glass.copy(alpha = .40f),
        ),
        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
        modifier = modifier,
    )
}

@Composable private fun Timeline(day: WorkDay?) {
    Card(colors = CardDefaults.cardColors(containerColor = GlassSoft), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("今日时间轴", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            listOf("上班" to day?.clockIn, "午休开始" to day?.lunchStart, "午休结束" to day?.lunchEnd, "预计下班" to day?.expectedClockOut, "实际下班" to day?.actualClockOut).forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(9.dp).clip(CircleShape).background(if (item.second != null) AccentStrong.copy(alpha = .75f) else Weak.copy(alpha = .55f))); Spacer(Modifier.width(11.dp)); Text(item.first, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(timelineTime(item.second, day), color = if (item.second != null) TextPrimary else Weak, fontSize = 13.sp) }
                if (index < 4) Divider(color = GlassBorder, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private fun timelineTime(instant: Instant?, day: WorkDay?): String {
    if (instant == null) return "—"
    val zone = runCatching { ZoneId.of(day?.zoneId ?: ZoneId.systemDefault().id) }.getOrDefault(ZoneId.systemDefault())
    val zdt = instant.atZone(zone)
    val base = day?.dateKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val time = zdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    return if (base != null && zdt.toLocalDate().isAfter(base)) "次日 $time" else time
}
