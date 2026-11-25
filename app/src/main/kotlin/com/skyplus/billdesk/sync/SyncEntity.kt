package com.skyplus.billdesk.sync

/**
 * Everything that can be synced between the device and the server.
 *
 * Kept as an enum rather than free strings so the conflict rules can be
 * exhaustive - a new table with no rule will not compile.
 */
enum class SyncEntity {
    BILL,
    BILL_ITEM,
    BILL_VOID,
    MENU_ITEM,
    CATEGORY,
    INVENTORY_ITEM,
    STOCK_MOVEMENT,
    SETTINGS,
    MEMBER
}
