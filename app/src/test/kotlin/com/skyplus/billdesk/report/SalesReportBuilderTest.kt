package com.skyplus.billdesk.report

import com.skyplus.billdesk.model.OrderItem
import com.skyplus.billdesk.model.PaymentMode
import org.junit.Assert.*
import org.junit.Test

class SalesReportBuilderTest {
    @Test fun buildsDailyItemTenderAndCancellationTotals() {
        val sale = PaidSale(5, listOf(OrderItem(1, "Tea", 100, 2)), 200, mapOf(PaymentMode.CASH to 200))
        val report = SalesReportBuilder.build(0, 10, listOf(sale), listOf(CancelledOrderAudit("o1", 6, "duplicate")))
        assertEquals(1, report.billCount)
        assertEquals(200L, report.grossPaise)
        assertEquals(2, report.itemSales.single().quantity)
        assertEquals(1, report.cancellations.size)
    }
}
