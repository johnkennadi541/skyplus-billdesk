package com.skyplus.billdesk.billing

/**
 * The arithmetic of a bill - pure, deterministic and unit tested.
 *
 * ## The order of operations, fixed once
 *
 * 1. line total = unit price x quantity          (exact, no rounding)
 * 2. subtotal = sum of line totals               (exact)
 * 3. discount, on the subtotal                   (rounded half up, capped at the subtotal)
 * 4. the discount is allocated across the lines  (proportional, largest remainder,
 *                                                 so the parts always add up exactly)
 * 5. tax per line, on the discounted line amount (rounded half up, per line)
 * 6. service charge, on the discounted subtotal  (rounded half up)
 * 7. grand total = subtotal - discount + service charge + tax
 *
 * ## Why this order
 *
 * Tax is charged on what the customer actually pays for the food, which is after
 * the discount. Rounding happens once per line and once for the service charge,
 * never on the running total, so the printed bill can be re-added by hand and it
 * will match to the paisa.
 *
 * ## Why the discount is allocated
 *
 * With items taxed at different rates, a 10% discount cannot be applied to the
 * total and taxed at one rate - that would overcharge or undercharge. It is spread
 * over the lines in proportion, and the last paise go to the lines with the largest
 * fractional share, so the allocation always sums to the discount exactly.
 */
object BillingEngine {

    /**
     * @param serviceChargePercent applied to the subtotal after discount
     */
    fun compute(
        lines: List<BillingLine>,
        discount: Discount = Discount.None,
        serviceChargePercent: Double = 0.0
    ): BillTotals {
        require(lines.isNotEmpty()) { "A bill needs at least one line" }
        require(serviceChargePercent >= 0.0 && serviceChargePercent <= 100.0) {
            "serviceChargePercent must be between 0 and 100, was $serviceChargePercent"
        }

        val lineTotals = lines.map { it.lineTotalPaise }
        val subtotal = lineTotals.sum()

        val discountPaise = when (discount) {
            Discount.None -> 0L
            is Discount.Percent -> roundHalfUp(subtotal * basisPoints(discount.percent), BASIS)
            is Discount.Flat -> minOf(discount.amountPaise, subtotal)
        }

        val discountPerLine = allocate(discountPaise, lineTotals)
        val taxablePerLine = lineTotals.mapIndexed { index, total ->
            total - discountPerLine[index]
        }

        val taxPerLine = lines.mapIndexed { index, line ->
            roundHalfUp(taxablePerLine[index] * basisPoints(line.taxPercent), BASIS)
        }
        val taxPaise = taxPerLine.sum()

        val taxableBase = taxablePerLine.sum()
        val serviceChargePaise = roundHalfUp(taxableBase * basisPoints(serviceChargePercent), BASIS)

        val grandTotal = subtotal - discountPaise + serviceChargePaise + taxPaise

        return BillTotals(
            subtotalPaise = subtotal,
            discountPaise = discountPaise,
            taxableBasePaise = taxableBase,
            serviceChargePaise = serviceChargePaise,
            taxPaise = taxPaise,
            grandTotalPaise = grandTotal,
            discountPerLine = discountPerLine,
            taxPerLine = taxPerLine
        )
    }

    /** 1 basis point = 0.01%. Keeps percentages out of floating point arithmetic. */
    private const val BASIS = 10_000L

    private fun basisPoints(percent: Double): Long {
        require(percent >= 0.0) { "A percentage must not be negative" }
        return Math.round(percent * 100.0)
    }

    /** Round half up, integer only: 1.5 -> 2, 2.4 -> 2, 2.5 -> 3. */
    private fun roundHalfUp(numerator: Long, denominator: Long): Long {
        require(numerator >= 0) { "Only non-negative amounts are rounded here" }
        require(denominator > 0) { "Denominator must be positive" }
        return (2 * numerator + denominator) / (2 * denominator)
    }

    /**
     * Splits [total] across [weights] in proportion, with the remainder going to
     * the largest fractional shares. The result always sums to [total].
     */
    private fun allocate(total: Long, weights: List<Long>): List<Long> {
        if (total == 0L) return List(weights.size) { 0L }
        val weightSum = weights.sum()
        require(weightSum > 0) { "Cannot allocate a discount across zero-value lines" }

        val base = weights.map { weight -> total * weight / weightSum }
        val remainder = total - base.sum()
        if (remainder == 0L) return base

        val order = weights.indices.sortedByDescending { index ->
            total * weights[index] % weightSum
        }
        val result = base.toMutableList()
        for (i in 0 until remainder.toInt()) {
            result[order[i]] += 1
        }
        return result
    }
}
