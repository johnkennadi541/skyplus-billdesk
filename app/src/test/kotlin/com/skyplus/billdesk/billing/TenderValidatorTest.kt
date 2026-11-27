package com.skyplus.billdesk.billing

import com.skyplus.billdesk.model.PaymentMode
import org.junit.Assert.*
import org.junit.Test

class TenderValidatorTest {
    @Test fun acceptsExactSplitTender() {
        TenderValidator.validate(1000, listOf(Tender(PaymentMode.CASH, 400), Tender(PaymentMode.UPI, 600)))
    }
    @Test fun rejectsTenderMismatch() {
        assertThrows(IllegalArgumentException::class.java) { TenderValidator.validate(1000, listOf(Tender(PaymentMode.CASH, 999))) }
    }
}
