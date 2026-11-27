package com.skyplus.billdesk.kitchen

import org.junit.Assert.*
import org.junit.Test

class EtaCalculatorTest {
    @Test fun dividesQueueByStaffAndAddsLongestItemPrep() {
        assertEquals(15, EtaCalculator.stationWaitMinutes(StationLoad(5, 6.0, 2)))
        val line = PersistedOrderLine("l", 7, "Dosa", 1, 1000, 5.0, "tandoor", "")
        assertEquals(19, EtaCalculator.forOrder(listOf(line), mapOf("tandoor" to StationLoad(5, 6.0, 2)), mapOf(7L to 4.0)).totalMinutes)
    }
    @Test fun zeroStaffUsesOneToAvoidDivisionByZero() { assertEquals(12, EtaCalculator.stationWaitMinutes(StationLoad(3, 4.0, 0))) }
}
