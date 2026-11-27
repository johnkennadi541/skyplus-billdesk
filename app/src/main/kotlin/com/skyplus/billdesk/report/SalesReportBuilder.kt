package com.skyplus.billdesk.report

import com.skyplus.billdesk.model.OrderItem
import com.skyplus.billdesk.model.PaymentMode

data class PaidSale(val paidAtEpochMillis: Long, val items: List<OrderItem>, val totalPaise: Long, val tenders: Map<PaymentMode, Long>)
data class CancelledOrderAudit(val orderId: String, val cancelledAtEpochMillis: Long, val reason: String)
data class ItemSales(val name: String, val quantity: Int, val grossPaise: Long)
data class EndOfDaySummary(val billCount: Int, val grossPaise: Long, val tenderTotals: Map<PaymentMode, Long>, val itemSales: List<ItemSales>, val cancellations: List<CancelledOrderAudit>)

object SalesReportBuilder {
    fun build(dayStartInclusive: Long, nextDayExclusive: Long, sales: List<PaidSale>, cancellations: List<CancelledOrderAudit>): EndOfDaySummary {
        require(nextDayExclusive >= dayStartInclusive)
        val today = sales.filter { it.paidAtEpochMillis >= dayStartInclusive && it.paidAtEpochMillis < nextDayExclusive }
        val tenderTotals = PaymentMode.values().associateWith { mode -> today.sumOf { it.tenders[mode] ?: 0L } }
        val itemSales = today.flatMap { it.items }.groupBy { it.nameSnapshot }.map { (name, lines) ->
            ItemSales(name, lines.sumOf { it.quantity }, lines.sumOf { it.lineTotalPaise })
        }.sortedByDescending { it.grossPaise }
        return EndOfDaySummary(
            today.size, today.sumOf { it.totalPaise }, tenderTotals, itemSales,
            cancellations.filter { it.cancelledAtEpochMillis >= dayStartInclusive && it.cancelledAtEpochMillis < nextDayExclusive }
                .sortedByDescending { it.cancelledAtEpochMillis }
        )
    }
}
