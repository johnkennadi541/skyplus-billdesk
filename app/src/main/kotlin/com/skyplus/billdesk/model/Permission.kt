package com.skyplus.billdesk.model

/**
 * Every action in the app that is gated by role.
 *
 * Rule: add the permission here first, then grant it in
 * [com.skyplus.billdesk.auth.PermissionMatrix], then use it at the call site
 * through [com.skyplus.billdesk.auth.PermissionGuard]. Never test the role
 * directly in UI code.
 */
enum class Permission {
    BILLING_CART,
    ORDER_TYPE_AND_KOT,
    DISCOUNT_WITHIN_LIMIT,
    DISCOUNT_ABOVE_LIMIT,
    VOID_REFUND_EDIT_BILL,
    MENU_VIEW,
    MENU_EDIT,
    TAX_SETTINGS_VIEW,
    TAX_SETTINGS_EDIT,
    SALES_REPORT_OWN_SHIFT,
    SALES_REPORT_FULL,
    PROFILE_SWITCH,
    RESET_CASHIER_PIN,
    INVENTORY_VIEW,
    INVENTORY_EDIT,
    STOCK_MOVEMENT
}
