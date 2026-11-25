package com.skyplus.billdesk.billing

/**
 * The result of a billing calculation.
 *
 * Every amount is paise. The per-line figures are exposed on purpose: when a
 * customer asks why the tax is what it is, the answer has to be per line, not a
 * single number that nobody can reconstruct.
 */
data class BillTotals(
    val subtotalPaise: Long,
    val discountPaise: Long,
    val taxableBasePaise: Long,
    val serviceChargePaise: Long,
    val taxPaise: Long,
    val grandTotalPaise: Long,
    val discountPerLine: List<Long>,
    val taxPerLine: List<Long>
) {
    init {
        require(discountPerLine.sum() == discountPaise) {
            "Per-line discounts must add up to the total discount"
        }
        require(taxPerLine.sum() == taxPaise) {
            "Per-line tax must add up to the total tax"
        }
        require(grandTotalPaise >= 0) { "A grand total must not be negative" }
    }

    /** The amount the customer pays, in words for the bill footer. */
    fun payablePaise(): Long = grandTotalPaise
}
