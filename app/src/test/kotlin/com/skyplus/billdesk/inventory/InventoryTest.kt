package com.skyplus.billdesk.inventory

import com.skyplus.billdesk.model.InventoryItem
import com.skyplus.billdesk.model.StockMovement
import com.skyplus.billdesk.model.StockMovementType
import com.skyplus.billdesk.model.StockUnit
import com.skyplus.billdesk.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryTest {

    private fun item(
        name: String = "Paneer",
        unit: StockUnit = StockUnit.GRAM,
        quantityBase: Long = 5000,
        reorderLevelBase: Long = 1000,
        costPerBasePaise: Long = 40,
        isActive: Boolean = true
    ) = InventoryItem(
        id = 1,
        name = name,
        unit = unit,
        quantityBase = quantityBase,
        reorderLevelBase = reorderLevelBase,
        costPerBasePaise = costPerBasePaise,
        isActive = isActive
    )

    private fun movement(
        type: StockMovementType,
        quantityBase: Long,
        note: String = ""
    ) = StockMovement(
        inventoryItemId = 1,
        type = type,
        quantityBase = quantityBase,
        atEpochMillis = 1_700_000_000_000,
        byRole = UserRole.ADMIN,
        note = note
    )

    @Test
    fun lowStockIsTrueAtOrBelowTheReorderLevel() {
        assertTrue(item(quantityBase = 1000).isLowStock)
        assertTrue(item(quantityBase = 500).isLowStock)
        assertFalse(item(quantityBase = 1001).isLowStock)
    }

    @Test
    fun stockValueIsQuantityTimesCost() {
        assertEquals(200000L, item(quantityBase = 5000, costPerBasePaise = 40).stockValuePaise)
    }

    @Test
    fun purchaseAddsAndUsageSubtracts() {
        val after = StockCalculator.quantityAfter(
            5000,
            listOf(movement(StockMovementType.PURCHASE, 2000), movement(StockMovementType.USAGE, 1500))
        )
        assertEquals(5500L, after)
    }

    @Test
    fun wastageSubtractsAndNeedsAReason() {
        assertEquals(
            4500L,
            StockCalculator.quantityAfter(
                5000,
                listOf(movement(StockMovementType.WASTAGE, 500, note = "spoiled"))
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun wastageWithoutAReasonIsRejected() {
        movement(StockMovementType.WASTAGE, 500)
    }

    @Test
    fun adjustmentCanBeNegative() {
        assertEquals(
            4800L,
            StockCalculator.quantityAfter(
                5000,
                listOf(movement(StockMovementType.ADJUSTMENT, -200))
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun usageQuantityMustBePositive() {
        movement(StockMovementType.USAGE, -500)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stockCannotGoNegative() {
        StockCalculator.quantityAfter(1000, listOf(movement(StockMovementType.USAGE, 1200)))
    }

    @Test
    fun applyReturnsTheUpdatedItem() {
        val updated = StockCalculator.apply(item(quantityBase = 5000), listOf(movement(StockMovementType.USAGE, 4500)))
        assertEquals(500L, updated.quantityBase)
        assertTrue(updated.isLowStock)
    }

    @Test
    fun lowStockListSkipsInactiveItemsAndSortsByUrgency() {
        val rice = item(name = "Rice", quantityBase = 900, reorderLevelBase = 1000)
        val gas = item(name = "Gas", unit = StockUnit.PIECE, quantityBase = 0, reorderLevelBase = 2)
        val old = item(name = "Old", quantityBase = 10, reorderLevelBase = 100, isActive = false)
        val plenty = item(name = "Plenty", quantityBase = 9000, reorderLevelBase = 1000)
        val result = StockCalculator.lowStockItems(listOf(rice, gas, old, plenty))
        assertEquals(listOf("Gas", "Rice"), result.map { it.name })
    }

    @Test
    fun totalValueIgnoresInactiveItems() {
        val total = StockCalculator.totalStockValuePaise(
            listOf(item(quantityBase = 1000, costPerBasePaise = 10), item(name = "Old", isActive = false))
        )
        assertEquals(10000L, total)
    }

    @Test
    fun formatsWeightInKilos() {
        assertEquals("1.5 kg", StockCalculator.formatQuantity(1500, StockUnit.GRAM))
        assertEquals("2 kg", StockCalculator.formatQuantity(2000, StockUnit.GRAM))
        assertEquals("0.25 kg", StockCalculator.formatQuantity(250, StockUnit.GRAM))
        assertEquals("0.045 kg", StockCalculator.formatQuantity(45, StockUnit.GRAM))
    }

    @Test
    fun formatsVolumeInLitres() {
        assertEquals("3.25 L", StockCalculator.formatQuantity(3250, StockUnit.MILLILITRE))
    }

    @Test
    fun formatsPieces() {
        assertEquals("12 pc", StockCalculator.formatQuantity(12, StockUnit.PIECE))
    }

    @Test
    fun displayUnitsConvertToBaseUnits() {
        assertEquals(2500L, StockUnit.GRAM.displayToBase(2) + 500)
        assertEquals(3000L, StockUnit.MILLILITRE.displayToBase(3))
        assertEquals(7L, StockUnit.PIECE.displayToBase(7))
    }
}
