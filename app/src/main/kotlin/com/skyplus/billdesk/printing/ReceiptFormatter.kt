package com.skyplus.billdesk.printing

import com.skyplus.billdesk.kitchen.OrderEta
import com.skyplus.billdesk.model.Money

object ReceiptFormatter {
    fun formatEta(eta: OrderEta): String = "Estimated prep/wait: ${eta.totalMinutes} min"
    fun formatTotal(totalPaise: Long): String = "TOTAL ${Money.formatWithPrefix(totalPaise)}"
}
