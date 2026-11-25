package com.skyplus.billdesk.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderModelTest {

    private val paneer = MenuItem(
        id = 1,
        code = "ST01",
        name = "Paneer Tikka",
        categoryId = 1,
        pricePaise = 24000
    )

    @Test
    fun lineTotalMultipliesPriceByQuantity() {
        val item = OrderItem(paneer.id, paneer.name, paneer.pricePaise, quantity = 3)
        assertEquals(72000L, item.lineTotalPaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun quantityZeroIsRejected() {
        OrderItem(1, "Paneer Tikka", 24000, quantity = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun withQuantityRejectsZero() {
        OrderItem(1, "Paneer Tikka", 24000, quantity = 1).withQuantity(0)
    }

    @Test
    fun searchMatchesNameOrCodeCaseInsensitively() {
        assertTrue(paneer.matches("paneer"))
        assertTrue(paneer.matches("ST01"))
        assertTrue(paneer.matches("st01"))
        assertTrue(paneer.matches("   "))
        assertFalse(paneer.matches("chicken"))
    }

    @Test
    fun billRecordsItsAuditFields() {
        val bill = Bill(
            billNumber = "B-0001",
            createdAtEpochMillis = 1_700_000_000_000,
            orderType = OrderType.DINE_IN,
            tableNumber = "4",
            items = listOf(OrderItem(1, "Paneer Tikka", 24000, quantity = 2)),
            subtotalPaise = 48000,
            taxPaise = 2400,
            serviceChargePaise = 0,
            discountPaise = 4800,
            grandTotalPaise = 45600,
            paymentMode = PaymentMode.UPI,
            createdByRole = UserRole.CASHIER,
            discountAppliedBy = UserRole.ADMIN
        )
        assertEquals(UserRole.CASHIER, bill.createdByRole)
        assertEquals(UserRole.ADMIN, bill.discountAppliedBy)
        assertTrue(bill.hasDiscount)
        assertFalse(bill.isVoided)
    }

    @Test(expected = IllegalArgumentException::class)
    fun billWithoutItemsIsRejected() {
        Bill(
            billNumber = "B-0002",
            createdAtEpochMillis = 0,
            orderType = OrderType.TAKEAWAY,
            items = emptyList(),
            subtotalPaise = 0,
            taxPaise = 0,
            serviceChargePaise = 0,
            discountPaise = 0,
            grandTotalPaise = 0,
            createdByRole = UserRole.CASHIER
        )
    }
}
