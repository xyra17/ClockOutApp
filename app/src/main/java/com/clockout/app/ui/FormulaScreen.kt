package com.clockout.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockout.app.domain.*
import com.clockout.app.ui.theme.ClockOutVisuals
import java.util.Locale

private val FGlass: Color @Composable get() = ClockOutVisuals.colors.glass
private val FBorder: Color @Composable get() = ClockOutVisuals.colors.border
private val FText: Color @Composable get() = ClockOutVisuals.colors.text
private val FMuted: Color @Composable get() = ClockOutVisuals.colors.muted
private val FWeak: Color @Composable get() = ClockOutVisuals.colors.weak
private val FAccent: Color @Composable get() = ClockOutVisuals.colors.accent
private val FAccentStrong: Color @Composable get() = ClockOutVisuals.colors.accentStrong

private enum class FormulaTab(val title: String) { MECHANICS("机械公式"), CONVERT("单位换算"), CALCULATOR("计算器") }

@Composable
fun FormulaScreen() {
    var tab by remember { mutableStateOf(FormulaTab.MECHANICS) }
    ScreenColumn {
        Text("工程工具", color = FText, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
        Text("常用机械公式、工程单位与科学计算", color = FMuted, fontSize = 13.sp)
        FormulaTabs(tab) { tab = it }
        AnimatedContent(targetState = tab, transitionSpec = { (fadeIn(tween(260, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))) + slideInVertically(tween(280)) { it / 12 }) togetherWith fadeOut(tween(120)) }, label = "formula-panel") { selected ->
            when (selected) { FormulaTab.MECHANICS -> MechanicalPanel(); FormulaTab.CONVERT -> ConversionPanel(); FormulaTab.CALCULATOR -> CalculatorPanel() }
        }
    }
}

@Composable
private fun FormulaTabs(selected: FormulaTab, onSelect: (FormulaTab) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(FText.copy(alpha = .045f)).border(1.dp, FBorder, RoundedCornerShape(18.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FormulaTab.entries.forEach { tab ->
            val active = selected == tab
            Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (active) Brush.verticalGradient(listOf(FAccent, FAccent.copy(alpha = FAccent.alpha * .55f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))).border(1.dp, if (active) FAccentStrong.copy(alpha = .20f) else Color.Transparent, RoundedCornerShape(14.dp)).clickable { onSelect(tab) }, contentAlignment = Alignment.Center) {
                Text(tab.title, color = if (active) FText else FMuted, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MechanicalPanel() {
    var category by remember { mutableStateOf(FormulaCategory.STRENGTH) }
    var formula by remember { mutableStateOf(EngineeringFormulas.all.first()) }
    val values = remember { mutableStateListOf<String>().apply { repeat(formula.inputs.size) { add("") } } }
    fun select(next: EngineeringFormula) { formula = next; values.clear(); repeat(next.inputs.size) { values.add("") } }
    val numeric = values.map { it.toDoubleOrNull() }
    val result = if (numeric.all { it != null }) formula.calculate(numeric.filterNotNull()) else null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FormulaCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item; select(EngineeringFormulas.all.first { it.category == item }) }, label = { Text(item.label, fontSize = 12.sp) }) }
        }
        Text("选择计算", color = FMuted, fontSize = 11.sp, letterSpacing = .8.sp)
        EngineeringFormulas.all.filter { it.category == category }.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item -> FormulaChoice(item, formula.id == item.id, Modifier.weight(1f)) { select(item) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text(formula.title, color = FText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(formula.equation, color = FAccentStrong, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp)) }
                Box(Modifier.size(34.dp).clip(CircleShape).background(FText.copy(alpha = .065f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Calculate, null, tint = FMuted, modifier = Modifier.size(18.dp)) }
            }
            formula.inputs.forEachIndexed { index, input -> FormulaField("${input.label}  ${input.symbol}", input.unit, values[index]) { values[index] = it } }
            ResultPanel(result?.let(::formatNumber) ?: "—", formula.resultUnit, result == null)
            Text(formula.note, color = FMuted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Text("快速计算用于现场估算；涉及安全系数、疲劳、屈曲与标准选型时，请结合规范复核。", color = FWeak, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 3.dp))
    }
}

@Composable
private fun FormulaChoice(formula: EngineeringFormula, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(72.dp).clip(RoundedCornerShape(15.dp)).background(if (selected) FAccent else FText.copy(alpha = .032f)).border(if (selected) 1.2.dp else 1.dp, if (selected) FAccentStrong.copy(alpha = .34f) else FBorder, RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(formula.title, color = if (selected) FText else FMuted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        Text(formula.equation, color = if (selected) FAccentStrong else FWeak, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
private fun ConversionPanel() {
    var group by remember { mutableStateOf(EngineeringConversions.groups.first()) }
    var from by remember { mutableStateOf(group.units.first()) }
    var to by remember { mutableStateOf(group.units[1]) }
    var input by remember { mutableStateOf("") }
    val result = input.toDoubleOrNull()?.let { EngineeringConversions.convert(it, from, to) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            EngineeringConversions.groups.forEach { item -> FilterChip(selected = group.id == item.id, onClick = { group = item; from = item.units.first(); to = item.units.getOrElse(1) { item.units.first() }; input = "" }, label = { Text(item.title, fontSize = 12.sp) }) }
        }
        GlassCard {
            Text("${group.title}换算", color = FText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                UnitSelector(group, from, Modifier.weight(1f)) { from = it }
                OutlinedButton(onClick = { val old = from; from = to; to = old }, modifier = Modifier.size(50.dp), shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(0.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FBorder)) { Icon(Icons.Default.SwapHoriz, "交换单位", tint = FText) }
                UnitSelector(group, to, Modifier.weight(1f)) { to = it }
            }
            OutlinedTextField(value = input, onValueChange = { input = decimalInput(it) }, label = { Text("输入 ${from.symbol}") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            ResultPanel(result?.let(::formatNumber) ?: "—", to.symbol, result == null)
            if (result != null) Text("${formatNumber(input.toDouble())} ${from.symbol} = ${formatNumber(result)} ${to.symbol}", color = FMuted, fontSize = 11.sp)
        }
        Text("已覆盖长度、面积、质量、力、压力、扭矩、功率、速度和温度。", color = FWeak, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSelector(group: ConversionGroup, selected: ConversionUnit, modifier: Modifier, onSelect: (ConversionUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = selected.symbol, onValueChange = {}, readOnly = true, label = { Text(selected.name, maxLines = 1) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { group.units.forEach { unit -> DropdownMenuItem(text = { Text("${unit.name} · ${unit.symbol}") }, onClick = { onSelect(unit); expanded = false }) } }
    }
}

@Composable
private fun CalculatorPanel() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<String>() }
    val clipboard = LocalClipboardManager.current
    val rows = listOf(
        listOf("sin" to "sin(", "cos" to "cos(", "tan" to "tan(", "√" to "sqrt(", "xʸ" to "^"),
        listOf("7" to "7", "8" to "8", "9" to "9", "(" to "(", ")" to ")"),
        listOf("4" to "4", "5" to "5", "6" to "6", "×" to "*", "÷" to "/"),
        listOf("1" to "1", "2" to "2", "3" to "3", "+" to "+", "−" to "-"),
        listOf("0" to "0", "." to ".", "π" to "pi", "%" to "/100", "⌫" to "back"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("科学计算器", color = FMuted, fontSize = 11.sp, letterSpacing = .7.sp)
                if (result != "0") Icon(Icons.Default.ContentCopy, "复制结果", tint = FMuted, modifier = Modifier.size(19.dp).clickable { clipboard.setText(AnnotatedString(result)) })
            }
            Text(result, color = if (error == null) FText else ClockOutVisuals.colors.danger, fontSize = 34.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), maxLines = 1)
            Text(error ?: expression.ifBlank { "输入表达式 · 三角函数使用角度制" }, color = if (error == null) FMuted else ClockOutVisuals.colors.danger, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(4.dp))
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (label, token) ->
                        OutlinedButton(onClick = { error = null; if (token == "back") expression = expression.dropLast(1) else expression += token; result = expression.ifBlank { "0" } }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (label in listOf("×", "÷", "+", "−", "xʸ")) FAccent else FText), border = androidx.compose.foundation.BorderStroke(1.dp, FBorder)) { Text(label, fontSize = if (label.length > 2) 11.sp else 15.sp, maxLines = 1) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { expression = ""; result = "0"; error = null }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FBorder)) { Text("清除", color = FMuted) }
                Button(onClick = { val evaluated = ScientificExpression.evaluate(expression); if (evaluated == null) error = "表达式无效，请检查括号或运算符" else { val formatted = formatNumber(evaluated); history.add(0, "$expression = $formatted"); if (history.size > 3) history.removeAt(history.lastIndex); result = formatted; expression = formatted; error = null } }, modifier = Modifier.weight(2f).height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = FAccent, contentColor = ClockOutVisuals.colors.onAccent)) { Text("= 计算", fontWeight = FontWeight.SemiBold) }
            }
        }
        if (history.isNotEmpty()) GlassCard { Text("最近计算", color = FText, fontSize = 14.sp, fontWeight = FontWeight.Medium); history.forEach { item -> Text(item, color = FMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expression = item.substringAfterLast("= "); result = expression }.padding(vertical = 5.dp)) } }
    }
}

@Composable private fun GlassCard(content: @Composable () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = FGlass), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().border(1.dp, FBorder, RoundedCornerShape(22.dp))) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) { content() } } }
@Composable private fun FormulaField(label: String, unit: String, value: String, onValueChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = { onValueChange(decimalInput(it)) }, label = { Text(label) }, suffix = { if (unit.isNotBlank()) Text(unit) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
@Composable private fun ResultPanel(value: String, unit: String, empty: Boolean) { Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(FText.copy(alpha = .075f), FText.copy(alpha = .025f))), RoundedCornerShape(16.dp)).border(1.dp, FBorder, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 13.dp)) { Text("计算结果", color = FMuted, fontSize = 10.sp, letterSpacing = .7.sp); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Text(if (empty) "等待输入" else value, color = if (empty) FWeak else FText, fontSize = if (empty) 16.sp else 27.sp, fontWeight = if (empty) FontWeight.Normal else FontWeight.SemiBold, maxLines = 1); if (unit.isNotBlank()) Text(unit, color = FAccentStrong, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp)) } } }

private fun decimalInput(raw: String): String { val filtered = raw.replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' }; val sign = if (filtered.startsWith('-')) "-" else ""; val unsigned = filtered.replace("-", ""); val dot = unsigned.indexOf('.'); return sign + if (dot < 0) unsigned else unsigned.take(dot + 1) + unsigned.drop(dot + 1).replace(".", "") }
private fun formatNumber(value: Double): String { if (!value.isFinite()) return "—"; val absolute = kotlin.math.abs(value); return if (absolute != 0.0 && (absolute >= 1e9 || absolute < 1e-5)) String.format(Locale.US, "%.6e", value) else String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.') }
