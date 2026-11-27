package com.skyplus.billdesk.billing

import com.skyplus.billdesk.model.PaymentMode

data class Tender(val mode: PaymentMode, val amountPaise: Long) {
    init { require(amountPaise > 0) }
}

object TenderValidator {
    fun validate(grandTotalPaise: Long, tenders: List<Tender>) {
        require(grandTotalPaise >= 0)
        require(tenders.isNotEmpty()) { "At least one tender is required" }
        require(tenders.all { it.mode in setOf(PaymentMode.CASH, PaymentMode.UPI, PaymentMode.CARD) }) {
            "Only Cash, UPI and Card are supported for split tender"
        }
        require(tenders.sumOf { it.amountPaise } == grandTotalPaise) { "Tender total must equal bill total" }
    }
}
