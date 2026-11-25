package com.skyplus.billdesk.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun parsesWholeRupees() {
        assertEquals(1200L, Money.rupeesToPaise("12"))
    }

    @Test
    fun parsesOneAndTwoDecimalPlaces() {
        assertEquals(1250L, Money.rupeesToPaise("12.5"))
        assertEquals(1255L, Money.rupeesToPaise("12.55"))
        assertEquals(1255L, Money.rupeesToPaise(" 12.55 "))
    }

    @Test
    fun parsesZero() {
        assertEquals(0L, Money.rupeesToPaise("0"))
        assertEquals(0L, Money.rupeesToPaise("0.00"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsThreeDecimals() {
        Money.rupeesToPaise("12.555")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeInput() {
        Money.rupeesToPaise("-12.50")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsGarbage() {
        Money.rupeesToPaise("twelve")
    }

    @Test
    fun formatsWithTwoDecimals() {
        assertEquals("12.50", Money.format(1250))
        assertEquals("0.05", Money.format(5))
        assertEquals("0.00", Money.format(0))
    }

    @Test
    fun formatsWithIndianGrouping() {
        assertEquals("1,234.50", Money.format(123450))
        assertEquals("12,345.00", Money.format(1234500))
        assertEquals("1,23,456.00", Money.format(12345600))
        assertEquals("12,34,56,789.00", Money.format(12345678900))
    }

    @Test
    fun formatsNegativeAmounts() {
        assertEquals("-12.50", Money.format(-1250))
    }

    @Test
    fun roundTrips() {
        val paise = Money.rupeesToPaise("2,499".replace(",", ""))
        assertEquals("2,499.00", Money.format(paise))
    }

    @Test
    fun prefixVersionIsPlainText() {
        assertEquals("Rs 1,234.50", Money.formatWithPrefix(123450))
    }
}
