package com.skyplus.billdesk.billing

/**
 * One line as the billing engine sees it: a price, a quantity and that item's
 * own tax rate. Kept separate from [com.skyplus.billdesk.model.OrderItem] so the
 * arithmetic can be tested without an order, a bill or a database.
 */
data class BillingLine(
    val unitPricePaise: Long,
    val quantity: Int,
    val taxPercent: Double = 0.0
) {
    init {
        require(quantity > 0) { "Quantity must be at least 1" }
        require(unitPricePaise >= 0) { "Unit price must not be negative" }
        require(taxPercent >= 0.0 && taxPercent <= 100.0) {
            "taxPercent must be between 0 and 100, was $taxPercent"
        }
    }

    /** Exact: no rounding happens here. */
    val lineTotalPaise: Long get() = unitPricePaise * quantity
}
