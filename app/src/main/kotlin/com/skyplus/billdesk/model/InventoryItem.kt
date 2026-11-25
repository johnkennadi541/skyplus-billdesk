package com.skyplus.billdesk.model

/**
 * How stock is measured.
 *
 * Quantities are stored in the base unit of each measure - grams for weight,
 * millilitres for volume, whole pieces for count - so no Double is ever needed.
 * A stock figure that drifts is a stock figure nobody trusts.
 */
enum class StockUnit(val displayLabel: String) {
    GRAM("kg"),
    MILLILITRE("L"),
    PIECE("pc");

    /** How many base units make one display unit: 1 kg = 1000 g, 1 L = 1000 ml. */
    val basePerDisplayUnit: Long
        get() = if (this == PIECE) 1L else 1000L

    fun displayToBase(value: Long): Long = value * basePerDisplayUnit
}

/**
 * One tracked stock item - rice, paneer, gas cylinder, whatever the kitchen counts.
 *
 * [costPerBasePaise] is the cost of ONE base unit in paise (per gram, per
 * millilitre, per piece). If an item costs less than one paise per base unit,
 * track it in a bigger base unit instead of rounding to zero.
 */
data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val unit: StockUnit,
    val quantityBase: Long,
    val reorderLevelBase: Long,
    val costPerBasePaise: Long,
    val isActive: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Stock item name must not be blank" }
        require(quantityBase >= 0) { "Quantity must not be negative" }
        require(reorderLevelBase >= 0) { "Reorder level must not be negative" }
        require(costPerBasePaise >= 0) { "Cost must not be negative" }
    }

    /** At or below the reorder level - the list the admin checks each morning. */
    val isLowStock: Boolean get() = quantityBase <= reorderLevelBase

    /** Value of what is on hand, in paise. */
    val stockValuePaise: Long get() = quantityBase * costPerBasePaise
}
