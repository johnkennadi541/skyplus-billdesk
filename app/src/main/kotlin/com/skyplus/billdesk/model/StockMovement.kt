package com.skyplus.billdesk.model

/** Why stock moved. The type decides the direction; the quantity stays positive. */
enum class StockMovementType { PURCHASE, USAGE, WASTAGE, ADJUSTMENT }

/**
 * A single stock entry - the only way a quantity ever changes.
 *
 * Manual inventory (option B): nothing is deducted automatically when a bill is
 * made. The admin records purchases, usage and wastage here, so the stock figure
 * is only ever as accurate as these entries.
 */
data class StockMovement(
    val id: Long = 0,
    val inventoryItemId: Long,
    val type: StockMovementType,
    val quantityBase: Long,
    val atEpochMillis: Long,
    val byRole: UserRole,
    val note: String = ""
) {
    init {
        require(inventoryItemId > 0) { "A movement must belong to a stock item" }
        if (type == StockMovementType.ADJUSTMENT) {
            require(quantityBase != 0L) { "An adjustment needs a non-zero quantity" }
        } else {
            require(quantityBase > 0) {
                "Quantity must be positive; the movement type decides the direction"
            }
        }
        require(type != StockMovementType.WASTAGE || note.isNotBlank()) {
            "Wastage must record a reason"
        }
    }

    /** + for a purchase, - for usage and wastage, signed for an adjustment. */
    val signedDeltaBase: Long
        get() = when (type) {
            StockMovementType.PURCHASE -> quantityBase
            StockMovementType.USAGE, StockMovementType.WASTAGE -> -quantityBase
            StockMovementType.ADJUSTMENT -> quantityBase
        }
}
