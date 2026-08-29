package com.clockout.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineeringToolkitTest {
    @Test fun `torque formula uses common engineering units`() {
        val formula = EngineeringFormulas.all.first { it.id == "torque" }
        assertEquals(95.5, formula.calculate(listOf(10.0, 1000.0))!!, 0.0001)
    }

    @Test fun `hydraulic flow converts cubic millimetres per second to litres per minute`() {
        val formula = EngineeringFormulas.all.first { it.id == "flow" }
        assertEquals(6.0, formula.calculate(listOf(1000.0, 100.0))!!, 0.0001)
    }

    @Test fun `pressure converts MPa to psi`() {
        val pressure = EngineeringConversions.groups.first { it.id == "pressure" }
        val mpa = pressure.units.first { it.symbol == "MPa" }
        val psi = pressure.units.first { it.symbol == "psi" }
        assertEquals(145.0377, EngineeringConversions.convert(1.0, mpa, psi), 0.001)
    }

    @Test fun `temperature conversion handles offsets`() {
        val temperature = EngineeringConversions.groups.first { it.id == "temperature" }
        val c = temperature.units.first { it.symbol == "°C" }
        val f = temperature.units.first { it.symbol == "°F" }
        assertEquals(32.0, EngineeringConversions.convert(0.0, c, f), 0.0001)
    }

    @Test fun `scientific parser supports precedence powers and degree trig`() {
        assertEquals(14.0, ScientificExpression.evaluate("2+3*4")!!, 0.0001)
        assertEquals(512.0, ScientificExpression.evaluate("2^3^2")!!, 0.0001)
        assertEquals(1.0, ScientificExpression.evaluate("sin(90)")!!, 0.0001)
        assertTrue(ScientificExpression.evaluate("sqrt(2)")!! > 1.414)
    }

    @Test fun `scientific parser rejects incomplete input`() {
        assertNull(ScientificExpression.evaluate("2+"))
    }
}
