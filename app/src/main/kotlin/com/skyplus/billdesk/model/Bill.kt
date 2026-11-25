package com.skyplus.billdesk.model

enum class OrderType { DINE_IN, TAKEAWAY, DELIVERY }

enum class PaymentMode { CASH, CARD, UPI, OTHER }

/**
 * A saved bill. Pure data in Step 1 - the arithmetic lives in the billing
 * engine (Step 2) and is unit tested there.
 *
 * Audit fields are mandatory, not optional extras: every bill records which
 * profile created it, who applied a discount and who voided it.
 */
data class Bill(
    val id: Long = 0,
    val billNumber: String,
    val createdAtEpochMillis: Long,
    val orderType: OrderType,
    val tableNumber: String? = null,
    val items: List<OrderItem>,
    val subtotalPaise: Long,
    val taxPaise: Long,
    val serviceChargePaise: Long,
    val discountPaise: Long,
    val grandTotalPaise: Long,
    val paymentMode: PaymentMode? = null,
    val createdByRole: UserRole,
    val discountAppliedBy: UserRole? = null,
    val voidedBy: UserRole? = null,
    val voidedAtEpochMillis: Long? = null
) {
    init {
        require(billNumber.isNotBlank()) { "Bill number must not be blank" }
        require(items.isNotEmpty()) { "A bill needs at least one item" }
        require(discountPaise >= 0) { "Discount must not be negative" }
        require(grandTotalPaise >= 0) { "Grand total must not be negative" }
    }

    val isVoided: Boolean get() = voidedAtEpochMillis != null

    /** True when a discount was given, so the audit trail must name who gave it. */
    val hasDiscount: Boolean get() = discountPaise > 0
}
