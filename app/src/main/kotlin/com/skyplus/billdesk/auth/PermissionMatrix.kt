package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.Permission
import com.skyplus.billdesk.model.UserRole

/**
 * Single source of truth for Cashier vs Admin access.
 *
 * docs/roles-and-permissions.md is generated from this same table. Change both
 * together, or the documentation lies.
 */
object PermissionMatrix {

    val grants: Map<Permission, Set<UserRole>> = mapOf(
        Permission.BILLING_CART to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.ORDER_TYPE_AND_KOT to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.DISCOUNT_WITHIN_LIMIT to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.DISCOUNT_ABOVE_LIMIT to setOf(UserRole.ADMIN),
        Permission.VOID_REFUND_EDIT_BILL to setOf(UserRole.ADMIN),
        Permission.MENU_VIEW to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.MENU_EDIT to setOf(UserRole.ADMIN),
        Permission.TAX_SETTINGS_VIEW to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.TAX_SETTINGS_EDIT to setOf(UserRole.ADMIN),
        Permission.SALES_REPORT_OWN_SHIFT to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.SALES_REPORT_FULL to setOf(UserRole.ADMIN),
        Permission.PROFILE_SWITCH to setOf(UserRole.CASHIER, UserRole.ADMIN),
        Permission.RESET_CASHIER_PIN to setOf(UserRole.ADMIN)
    )

    /** Permissions granted to [role], in declaration order. */
    fun permissionsOf(role: UserRole): List<Permission> =
        Permission.entries.filter { isGranted(role, it) }

    fun isGranted(role: UserRole, permission: Permission): Boolean =
        grants[permission]?.contains(role) == true
}
