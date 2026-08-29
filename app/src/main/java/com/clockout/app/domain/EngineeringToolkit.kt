package com.clockout.app.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class FormulaCategory(val label: String) {
    STRENGTH("强度"), TRANSMISSION("传动"), MACHINING("切削"), FLUID("液压"), GEOMETRY("几何")
}

data class FormulaInput(val symbol: String, val label: String, val unit: String)

data class EngineeringFormula(
    val id: String,
    val category: FormulaCategory,
    val title: String,
    val equation: String,
    val inputs: List<FormulaInput>,
    val resultUnit: String,
    val note: String,
    private val operation: (List<Double>) -> Double,
) {
    fun calculate(values: List<Double>): Double? = runCatching { operation(values) }
        .getOrNull()?.takeIf { it.isFinite() }
}

object EngineeringFormulas {
    val all = listOf(
        EngineeringFormula("stress", FormulaCategory.STRENGTH, "平均正应力", "σ = F / A", listOf(FormulaInput("F", "轴向载荷", "N"), FormulaInput("A", "截面积", "mm²")), "MPa", "N/mm² 与 MPa 数值相同。") { it[0] / it[1] },
        EngineeringFormula("strain", FormulaCategory.STRENGTH, "线应变", "ε = ΔL / L × 10⁶", listOf(FormulaInput("ΔL", "长度变化", "mm"), FormulaInput("L", "原始长度", "mm")), "με", "结果以微应变表示。") { it[0] / it[1] * 1_000_000.0 },
        EngineeringFormula("elongation", FormulaCategory.STRENGTH, "轴向伸长", "ΔL = FL / AE", listOf(FormulaInput("F", "轴向载荷", "N"), FormulaInput("L", "杆件长度", "mm"), FormulaInput("A", "截面积", "mm²"), FormulaInput("E", "弹性模量", "MPa")), "mm", "适用于线弹性、小变形轴向受力。") { it[0] * it[1] / (it[2] * it[3]) },
        EngineeringFormula("bending", FormulaCategory.STRENGTH, "弯曲正应力", "σ = M / W", listOf(FormulaInput("M", "弯矩", "N·mm"), FormulaInput("W", "截面模量", "mm³")), "MPa", "请使用危险截面的弯矩与对应截面模量。") { it[0] / it[1] },
        EngineeringFormula("torsion", FormulaCategory.STRENGTH, "实心轴扭转剪应力", "τ = 16T / πd³", listOf(FormulaInput("T", "扭矩", "N·mm"), FormulaInput("d", "轴径", "mm")), "MPa", "仅适用于实心圆轴。") { 16.0 * it[0] / (PI * it[1].pow(3)) },
        EngineeringFormula("bearing", FormulaCategory.STRENGTH, "平均挤压应力", "p = F / dl", listOf(FormulaInput("F", "载荷", "N"), FormulaInput("d", "销轴直径", "mm"), FormulaInput("l", "承压长度", "mm")), "MPa", "用于销轴、孔壁等平均承压估算。") { it[0] / (it[1] * it[2]) },
        EngineeringFormula("torque", FormulaCategory.TRANSMISSION, "功率换算扭矩", "T = 9550P / n", listOf(FormulaInput("P", "功率", "kW"), FormulaInput("n", "转速", "rpm")), "N·m", "9550 为常用工程近似系数。") { 9550.0 * it[0] / it[1] },
        EngineeringFormula("power", FormulaCategory.TRANSMISSION, "扭矩换算功率", "P = Tn / 9550", listOf(FormulaInput("T", "扭矩", "N·m"), FormulaInput("n", "转速", "rpm")), "kW", "忽略传动效率损失。") { it[0] * it[1] / 9550.0 },
        EngineeringFormula("gear_ratio", FormulaCategory.TRANSMISSION, "齿轮传动比", "i = z₂ / z₁", listOf(FormulaInput("z₁", "主动轮齿数", "齿"), FormulaInput("z₂", "从动轮齿数", "齿")), "", "结果大于 1 时为减速。") { it[1] / it[0] },
        EngineeringFormula("output_speed", FormulaCategory.TRANSMISSION, "输出转速", "n₂ = n₁ / i", listOf(FormulaInput("n₁", "输入转速", "rpm"), FormulaInput("i", "传动比", "")), "rpm", "未计入滑差。") { it[0] / it[1] },
        EngineeringFormula("belt_speed", FormulaCategory.TRANSMISSION, "带轮线速度", "v = πDn / 60000", listOf(FormulaInput("D", "带轮直径", "mm"), FormulaInput("n", "转速", "rpm")), "m/s", "用于带传动圆周速度估算。") { PI * it[0] * it[1] / 60_000.0 },
        EngineeringFormula("cut_speed", FormulaCategory.MACHINING, "切削速度", "Vc = πDn / 1000", listOf(FormulaInput("D", "工件/刀具直径", "mm"), FormulaInput("n", "主轴转速", "rpm")), "m/min", "车削取工件直径，铣削取刀具直径。") { PI * it[0] * it[1] / 1000.0 },
        EngineeringFormula("spindle", FormulaCategory.MACHINING, "主轴转速", "n = 1000Vc / πD", listOf(FormulaInput("Vc", "切削速度", "m/min"), FormulaInput("D", "工件/刀具直径", "mm")), "rpm", "结果可按机床实际档位取整。") { 1000.0 * it[0] / (PI * it[1]) },
        EngineeringFormula("mill_feed", FormulaCategory.MACHINING, "铣削进给速度", "Vf = fz·z·n", listOf(FormulaInput("fz", "每齿进给", "mm/z"), FormulaInput("z", "刀齿数", "齿"), FormulaInput("n", "主轴转速", "rpm")), "mm/min", "适用于多刃铣刀进给估算。") { it[0] * it[1] * it[2] },
        EngineeringFormula("drill_feed", FormulaCategory.MACHINING, "钻削进给速度", "Vf = f·n", listOf(FormulaInput("f", "每转进给", "mm/r"), FormulaInput("n", "主轴转速", "rpm")), "mm/min", "实际值需结合材料、刀具与机床刚性。") { it[0] * it[1] },
        EngineeringFormula("cylinder_force", FormulaCategory.FLUID, "液压缸推力", "F = pπD² / 4", listOf(FormulaInput("p", "系统压力", "MPa"), FormulaInput("D", "活塞直径", "mm")), "N", "理论推力，未扣除摩擦和背压。") { it[0] * PI * it[1].pow(2) / 4.0 },
        EngineeringFormula("annular_force", FormulaCategory.FLUID, "液压缸回程力", "F = pπ(D²-d²) / 4", listOf(FormulaInput("p", "系统压力", "MPa"), FormulaInput("D", "活塞直径", "mm"), FormulaInput("d", "活塞杆直径", "mm")), "N", "环形有效面积的理论回程力。") { it[0] * PI * (it[1].pow(2) - it[2].pow(2)) / 4.0 },
        EngineeringFormula("flow", FormulaCategory.FLUID, "液压流量", "Q = 0.00006Av", listOf(FormulaInput("A", "有效面积", "mm²"), FormulaInput("v", "运动速度", "mm/s")), "L/min", "面积乘速度换算为升每分钟。") { .00006 * it[0] * it[1] },
        EngineeringFormula("circle", FormulaCategory.GEOMETRY, "圆面积", "A = πd² / 4", listOf(FormulaInput("d", "直径", "mm")), "mm²", "按直径计算圆截面积。") { PI * it[0].pow(2) / 4.0 },
        EngineeringFormula("cylinder", FormulaCategory.GEOMETRY, "圆柱体积", "V = πd²L / 4", listOf(FormulaInput("d", "直径", "mm"), FormulaInput("L", "长度", "mm")), "mm³", "实心圆柱体积。") { PI * it[0].pow(2) * it[1] / 4.0 },
        EngineeringFormula("mass", FormulaCategory.GEOMETRY, "材料质量", "m = ρV", listOf(FormulaInput("ρ", "密度", "g/cm³"), FormulaInput("V", "体积", "cm³")), "g", "常用钢材密度约 7.85 g/cm³。") { it[0] * it[1] },
        EngineeringFormula("thermal", FormulaCategory.GEOMETRY, "线膨胀量", "ΔL = αLΔT", listOf(FormulaInput("α", "线膨胀系数", "10⁻⁶/K"), FormulaInput("L", "原始长度", "mm"), FormulaInput("ΔT", "温差", "K")), "mm", "系数输入如钢材 11.7。") { it[0] * 1e-6 * it[1] * it[2] },
    )
}

data class ConversionUnit(val name: String, val symbol: String, val factor: Double, val offset: Double = 0.0) {
    fun toBase(value: Double) = (value + offset) * factor
    fun fromBase(value: Double) = value / factor - offset
}

data class ConversionGroup(val id: String, val title: String, val units: List<ConversionUnit>)

object EngineeringConversions {
    val groups = listOf(
        ConversionGroup("length", "长度", listOf(ConversionUnit("毫米", "mm", .001), ConversionUnit("厘米", "cm", .01), ConversionUnit("米", "m", 1.0), ConversionUnit("英寸", "in", .0254), ConversionUnit("英尺", "ft", .3048))),
        ConversionGroup("area", "面积", listOf(ConversionUnit("平方毫米", "mm²", 1e-6), ConversionUnit("平方厘米", "cm²", 1e-4), ConversionUnit("平方米", "m²", 1.0), ConversionUnit("平方英寸", "in²", .00064516))),
        ConversionGroup("mass", "质量", listOf(ConversionUnit("克", "g", .001), ConversionUnit("千克", "kg", 1.0), ConversionUnit("吨", "t", 1000.0), ConversionUnit("磅", "lb", .45359237))),
        ConversionGroup("force", "力", listOf(ConversionUnit("牛", "N", 1.0), ConversionUnit("千牛", "kN", 1000.0), ConversionUnit("千克力", "kgf", 9.80665), ConversionUnit("磅力", "lbf", 4.4482216153))),
        ConversionGroup("pressure", "压力", listOf(ConversionUnit("帕", "Pa", 1.0), ConversionUnit("千帕", "kPa", 1e3), ConversionUnit("兆帕", "MPa", 1e6), ConversionUnit("巴", "bar", 1e5), ConversionUnit("磅力/平方英寸", "psi", 6894.757293))),
        ConversionGroup("torque", "扭矩", listOf(ConversionUnit("牛米", "N·m", 1.0), ConversionUnit("牛毫米", "N·mm", .001), ConversionUnit("千克力米", "kgf·m", 9.80665), ConversionUnit("磅力英尺", "lbf·ft", 1.3558179483))),
        ConversionGroup("power", "功率", listOf(ConversionUnit("瓦", "W", 1.0), ConversionUnit("千瓦", "kW", 1000.0), ConversionUnit("公制马力", "PS", 735.49875), ConversionUnit("英制马力", "hp", 745.699872))),
        ConversionGroup("speed", "速度", listOf(ConversionUnit("米/秒", "m/s", 1.0), ConversionUnit("千米/时", "km/h", 1.0 / 3.6), ConversionUnit("毫米/秒", "mm/s", .001), ConversionUnit("英尺/分钟", "ft/min", .00508))),
        ConversionGroup("temperature", "温度", listOf(ConversionUnit("摄氏度", "°C", 1.0, 273.15), ConversionUnit("华氏度", "°F", 5.0 / 9.0, 459.67), ConversionUnit("开尔文", "K", 1.0))),
    )

    fun convert(value: Double, from: ConversionUnit, to: ConversionUnit): Double = to.fromBase(from.toBase(value))
}

object ScientificExpression {
    fun evaluate(source: String): Double? = runCatching { Parser(source).parse() }.getOrNull()?.takeIf { it.isFinite() }

    private class Parser(private val source: String) {
        private var index = 0
        fun parse(): Double { val result = expression(); skip(); require(index == source.length); return result }
        private fun expression(): Double { var value = term(); while (true) value = when { eat('+') -> value + term(); eat('-') -> value - term(); else -> return value } }
        private fun term(): Double { var value = power(); while (true) value = when { eat('*') -> value * power(); eat('/') -> value / power(); else -> return value } }
        private fun power(): Double { var value = unary(); if (eat('^')) value = value.pow(power()); return value }
        private fun unary(): Double { if (eat('+')) return unary(); if (eat('-')) return -unary(); return primary() }
        private fun primary(): Double {
            skip()
            if (eat('(')) return expression().also { require(eat(')')) }
            if (peekLetter()) {
                val name = readName()
                if (name == "pi") return PI
                require(eat('(')); val value = expression(); require(eat(')'))
                return when (name) { "sin" -> sin(Math.toRadians(value)); "cos" -> cos(Math.toRadians(value)); "tan" -> tan(Math.toRadians(value)); "sqrt" -> sqrt(value); "log" -> log10(value); "ln" -> ln(value); "abs" -> abs(value); else -> error("unknown function") }
            }
            val start = index
            while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
            require(index > start)
            return source.substring(start, index).toDouble()
        }
        private fun peekLetter(): Boolean { skip(); return index < source.length && source[index].isLetter() }
        private fun readName(): String { val start = index; while (index < source.length && source[index].isLetter()) index++; return source.substring(start, index).lowercase() }
        private fun skip() { while (index < source.length && source[index].isWhitespace()) index++ }
        private fun eat(char: Char): Boolean { skip(); return if (index < source.length && source[index] == char) { index++; true } else false }
    }
}
