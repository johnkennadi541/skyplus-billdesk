package com.skyplus.billdesk.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingEngineTest {

    @Test
    fun aSingleLineWithoutTaxIsJustTheLineTotal() {
        val totals = BillingEngine.compute(listOf(BillingLine(24000, 2)))
        assertEquals(48000L, totals.subtotalPaise)
        assertEquals(0L, totals.discountPaise)
        assertEquals(0L, totals.taxPaise)
        assertEquals(0L, totals.serviceChargePaise)
        assertEquals(48000L, totals.grandTotalPaise)
    }

    @Test
    fun linesAreSummedExactly() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(24000, 2), BillingLine(3000, 3), BillingLine(12550, 1))
        )
        assertEquals(24000L * 2 + 3000L * 3 + 12550L, totals.subtotalPaise)
        assertEquals(totals.subtotalPaise, totals.grandTotalPaise)
    }

    @Test
    fun taxIsChargedPerLine() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(10000, 1, taxPercent = 5.0), BillingLine(20000, 1, taxPercent = 12.0))
        )
        assertEquals(listOf(500L, 2400L), totals.taxPerLine)
        assertEquals(2900L, totals.taxPaise)
        assertEquals(32900L, totals.grandTotalPaise)
    }

    @Test
    fun aPercentageDiscountIsTakenBeforeTax() {
        // 100.00 with 10% discount = 90.00 taxable, 5% tax = 4.50
        val totals = BillingEngine.compute(
            listOf(BillingLine(10000, 1, taxPercent = 5.0)),
            discount = Discount.Percent(10.0)
        )
        assertEquals(10000L, totals.subtotalPaise)
        assertEquals(1000L, totals.discountPaise)
        assertEquals(9000L, totals.taxableBasePaise)
        assertEquals(450L, totals.taxPaise)
        assertEquals(9450L, totals.grandTotalPaise)
    }

    @Test
    fun aFlatDiscountLargerThanTheSubtotalIsCapped() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(5000, 1)),
            discount = Discount.Flat(9000)
        )
        assertEquals(5000L, totals.discountPaise)
        assertEquals(0L, totals.taxableBasePaise)
        assertEquals(0L, totals.grandTotalPaise)
    }

    @Test
    fun aFullPercentageDiscountLeavesNothingToPay() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(12345, 1, taxPercent = 18.0)),
            discount = Discount.Percent(100.0)
        )
        assertEquals(12345L, totals.discountPaise)
        assertEquals(0L, totals.taxPaise)
        assertEquals(0L, totals.grandTotalPaise)
    }

    @Test
    fun theDiscountIsAllocatedAcrossLinesWithoutLosingAPaisa() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(3333, 1), BillingLine(3333, 1), BillingLine(3334, 1)),
            discount = Discount.Percent(10.0)
        )
        assertEquals(1000L, totals.discountPaise)
        assertEquals(1000L, totals.discountPerLine.sum())
        assertEquals(3, totals.discountPerLine.size)
        assertTrue(totals.discountPerLine.all { it >= 0 })
    }

    @Test
    fun allocationNeverExceedsTheLineItBelongsTo() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(100, 1), BillingLine(9900, 1)),
            discount = Discount.Flat(5000)
        )
        assertEquals(5000L, totals.discountPerLine.sum())
        assertTrue(totals.discountPerLine[0] <= 100)
        assertTrue(totals.discountPerLine[1] <= 9900)
    }

    @Test
    fun differentTaxRatesSurviveADiscount() {
        // 60.00 at 5% and 40.00 at 18%, 10% discount -> 54.00 and 36.00
        val totals = BillingEngine.compute(
            listOf(
                BillingLine(6000, 1, taxPercent = 5.0),
                BillingLine(4000, 1, taxPercent = 18.0)
            ),
            discount = Discount.Percent(10.0)
        )
        assertEquals(600L, totals.discountPerLine[0])
        assertEquals(400L, totals.discountPerLine[1])
        assertEquals(270L, totals.taxPerLine[0])
        assertEquals(648L, totals.taxPerLine[1])
        assertEquals(918L, totals.taxPaise)
        assertEquals(9918L, totals.grandTotalPaise)
    }

    @Test
    fun serviceChargeIsChargedOnTheDiscountedSubtotal() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(10000, 1)),
            discount = Discount.Percent(10.0),
            serviceChargePercent = 5.0
        )
        // 100.00 - 10.00 discount = 90.00 taxable; 5% service charge = 4.50
        assertEquals(450L, totals.serviceChargePaise)
        assertEquals(9450L, totals.grandTotalPaise)
    }

    @Test
    fun everythingTogetherAddsUp() {
        val totals = BillingEngine.compute(
            listOf(
                BillingLine(24000, 2, taxPercent = 5.0),
                BillingLine(18000, 1, taxPercent = 5.0),
                BillingLine(4500, 4, taxPercent = 12.0)
            ),
            discount = Discount.Percent(10.0),
            serviceChargePercent = 10.0
        )
        val expectedSubtotal = 48000L + 18000L + 18000L
        assertEquals(expectedSubtotal, totals.subtotalPaise)
        assertEquals(totals.subtotalPaise - totals.discountPaise, totals.taxableBasePaise)
        assertEquals(
            totals.subtotalPaise - totals.discountPaise + totals.serviceChargePaise + totals.taxPaise,
            totals.grandTotalPaise
        )
        assertEquals(totals.discountPaise, totals.discountPerLine.sum())
        assertEquals(totals.taxPaise, totals.taxPerLine.sum())
    }

    @Test
    fun roundingIsHalfUp() {
        // 2.5% of 100 paise = 2.5 paise -> 3
        val totals = BillingEngine.compute(listOf(BillingLine(100, 1, taxPercent = 2.5)))
        assertEquals(3L, totals.taxPaise)
    }

    @Test
    fun roundingDownHappensBelowAHalfPaisa() {
        // 2.4% of 100 paise = 2.4 paise -> 2
        val totals = BillingEngine.compute(listOf(BillingLine(100, 1, taxPercent = 2.4)))
        assertEquals(2L, totals.taxPaise)
    }

    @Test
    fun aFractionOfAPaisaBecomesZero() {
        // 1.25% of 1 paise = 0.0125 -> 0
        val totals = BillingEngine.compute(listOf(BillingLine(1, 1, taxPercent = 1.25)))
        assertEquals(0L, totals.taxPaise)
    }

    @Test
    fun aLargeBillStaysExact() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(99999, 7, taxPercent = 5.0), BillingLine(1, 1, taxPercent = 5.0)),
            discount = Discount.Percent(7.5)
        )
        assertEquals(99999L * 7 + 1, totals.subtotalPaise)
        assertEquals(totals.discountPaise, totals.discountPerLine.sum())
        assertTrue(totals.grandTotalPaise > 0)
    }

    @Test
    fun thePerLineFiguresRebuildTheGrandTotal() {
        val totals = BillingEngine.compute(
            listOf(BillingLine(33333, 3, taxPercent = 5.0), BillingLine(7777, 2, taxPercent = 18.0)),
            discount = Discount.Percent(12.5),
            serviceChargePercent = 6.0
        )
        val rebuilt = totals.subtotalPaise - totals.discountPaise +
            totals.serviceChargePaise + totals.taxPaise
        assertEquals(totals.grandTotalPaise, rebuilt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun anEmptyBillIsRejected() {
        BillingEngine.compute(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun aZeroQuantityLineIsRejected() {
        BillingLine(1000, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aNegativePriceIsRejected() {
        BillingLine(-1, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aTaxRateAboveOneHundredIsRejected() {
        BillingLine(1000, 1, taxPercent = 100.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aDiscountAboveOneHundredPercentIsRejected() {
        Discount.Percent(101.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aNegativeFlatDiscountIsRejected() {
        Discount.Flat(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aServiceChargeAboveOneHundredIsRejected() {
        BillingEngine.compute(listOf(BillingLine(1000, 1)), serviceChargePercent = 101.0)
    }

    @Test
    fun aFlatDiscountReportsItsEffectivePercentage() {
        assertEquals(10.0, Discount.Flat(1000).percentOf(10000), 0.0001)
        assertEquals(0.0, Discount.Flat(1000).percentOf(0), 0.0001)
        assertEquals(0.0, Discount.None.percentOf(10000), 0.0001)
        assertEquals(12.5, Discount.Percent(12.5).percentOf(10000), 0.0001)
    }
}
