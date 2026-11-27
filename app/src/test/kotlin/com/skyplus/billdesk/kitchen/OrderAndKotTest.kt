package com.skyplus.billdesk.kitchen

import com.skyplus.billdesk.model.OrderType
import org.junit.Assert.*
import org.junit.Test

class OrderAndKotTest {
    @Test fun orderRequiresTableOnlyForDineInAndParcelUsesTakeawayType() {
        val dineIn = OrderDraft(type = OrderType.DINE_IN, tableLabel = "T4", openedAtEpochMillis = 1, lines = listOf(line()))
        val parcel = OrderDraft(type = OrderType.TAKEAWAY, openedAtEpochMillis = 2, lines = listOf(line()))
        assertEquals("T4", dineIn.tableLabel)
        assertNull(parcel.tableLabel)
        assertThrows(IllegalArgumentException::class.java) {
            OrderDraft(type = OrderType.DINE_IN, openedAtEpochMillis = 1, lines = listOf(line()))
        }
    }

    @Test fun kotGroupsByStationAndUsesOfflineTranslationOrOriginalFallback() {
        val orderLines = listOf(
            PersistedOrderLine("a", 1, "Masala Dosa", 2, 1000, 5.0, "tandoor", "  less spicy  "),
            PersistedOrderLine("b", 2, "Tea", 1, 200, 5.0, "drinks", "")
        )
        val stations = mapOf(
            "tandoor" to KitchenStation("tandoor", "Tandoor", KitchenLanguage.TAMIL),
            "drinks" to KitchenStation("drinks", "Beverages", KitchenLanguage.ENGLISH)
        )
        val tickets = KotBuilder { id, language -> if (id == 1L && language == KitchenLanguage.TAMIL) "மசாலா தோசை" else null }
            .build("order-1", 10, orderLines, stations)
        assertEquals(listOf("drinks", "tandoor"), tickets.map { it.stationId })
        assertEquals("Tea", tickets[0].lines.single().printedName)
        assertEquals("மசாலா தோசை", tickets[1].lines.single().printedName)
        assertEquals("less spicy", tickets[1].lines.single().note)
    }

    private fun line() = OrderLineDraft(1, "Dosa", 1, 1000, 5.0, "tandoor")
}
