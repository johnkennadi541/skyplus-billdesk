package com.skyplus.billdesk.model

/**
 * One line of a live order or a saved bill.
 *
 * [nameSnapshot] and [unitPricePaise] are copied at the time of billing so the
 * bill stays true even if the menu changes afterwards.
 */
data class OrderItem(
    val menuItemId: Long,
    val nameSnapshot: String,
    val unitPricePaise: Long,
    val quantity: Int,
    val note: String = ""
) {
    init {
        require(quantity > 0) { "Quantity must be at least 1" }
        require(unitPricePaise >= 0) { "Unit price must not be negative" }
    }

    val lineTotalPaise: Long get() = unitPricePaise * quantity

    /** Never goes below 1 - removing a line is a different operation. */
    fun withQuantity(newQuantity: Int): OrderItem {
        require(newQuantity > 0) { "Quantity must be at least 1; remove the line instead" }
        return copy(quantity = newQuantity)
    }
}
