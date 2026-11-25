package com.skyplus.billdesk.inventory

import com.skyplus.billdesk.model.InventoryItem
import com.skyplus.billdesk.model.StockMovement
import com.skyplus.billdesk.model.StockUnit

/**
 * Pure stock arithmetic - no Android, no database - so it can be unit tested.
 *
 * Manual inventory (option B): a bill does not touch stock. Stock only changes
 * through a recorded [StockMovement].
 */
object StockCalculator {

    /** Quantity after applying [movements] in order. Refuses to go below zero. */
    fun quantityAfter(startBase: Long, movements: List<StockMovement>): Long {
        require(startBase >= 0) { "Starting quantity must not be negative" }
        var quantity = startBase
        movements.forEach { quantity += it.signedDeltaBase }
        require(quantity >= 0) {
            "Stock would go negative ($quantity); record a correction first"
        }
        return quantity
    }

    /** The same item with its quantity updated by [movements]. */
    fun apply(item: InventoryItem, movements: List<StockMovement>): InventoryItem =
        item.copy(quantityBase = quantityAfter(item.quantityBase, movements))

    /**
     * Active items at or below their reorder level, most urgent first.
     *
     * Urgency is the ratio to the reorder level, not the raw shortfall: 2 kg short
     * of rice and 2 pieces short of gas cylinders are not the same thing, and
     * subtracting base units of grams from pieces would rank them nonsense.
     */
    fun lowStockItems(items: List<InventoryItem>): List<InventoryItem> =
        items.filter { it.isActive && it.isLowStock }
            .sortedBy { urgency(it) }

    private fun urgency(item: InventoryItem): Double =
        if (item.reorderLevelBase <= 0L) 0.0
        else item.quantityBase.toDouble() / item.reorderLevelBase

    /** Value of all active stock, in paise. */
    fun totalStockValuePaise(items: List<InventoryItem>): Long =
        items.filter { it.isActive }.sumOf { it.stockValuePaise }

    /** 1500 g -> "1.5 kg", 250 g -> "0.25 kg", 2 pc -> "2 pc". */
    fun formatQuantity(quantityBase: Long, unit: StockUnit): String {
        require(quantityBase >= 0) { "Quantity must not be negative" }
        if (unit == StockUnit.PIECE) return "$quantityBase pc"
        val whole = quantityBase / 1000
        val remainder = quantityBase % 1000
        if (remainder == 0L) return "$whole ${unit.displayLabel}"
        val fraction = remainder.toString().padStart(3, '0').trimEnd('0')
        return "$whole.$fraction ${unit.displayLabel}"
    }
}
