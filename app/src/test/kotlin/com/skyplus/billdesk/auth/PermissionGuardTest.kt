package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.Permission
import com.skyplus.billdesk.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGuardTest {

    @Test
    fun cashierCanBillAndTakeOrders() {
        assertTrue(PermissionGuard.can(UserRole.CASHIER, Permission.BILLING_CART))
        assertTrue(PermissionGuard.can(UserRole.CASHIER, Permission.ORDER_TYPE_AND_KOT))
        assertTrue(PermissionGuard.can(UserRole.CASHIER, Permission.PROFILE_SWITCH))
    }

    @Test
    fun cashierCannotEditMenuOrSettings() {
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.MENU_EDIT))
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.TAX_SETTINGS_EDIT))
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.VOID_REFUND_EDIT_BILL))
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.SALES_REPORT_FULL))
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.RESET_CASHIER_PIN))
    }

    @Test
    fun cashierCanSeeMenuAndSettingsReadOnly() {
        assertTrue(PermissionGuard.can(UserRole.CASHIER, Permission.MENU_VIEW))
        assertTrue(PermissionGuard.can(UserRole.CASHIER, Permission.TAX_SETTINGS_VIEW))
    }

    @Test
    fun cashierCanSeeStockButCannotChangeIt() {
        assertTrue(PermissionGuard.can(UserRole.CASHIER, Permission.INVENTORY_VIEW))
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.INVENTORY_EDIT))
        assertFalse(PermissionGuard.can(UserRole.CASHIER, Permission.STOCK_MOVEMENT))
    }

    @Test
    fun adminRunsTheInventory() {
        assertTrue(PermissionGuard.can(UserRole.ADMIN, Permission.INVENTORY_VIEW))
        assertTrue(PermissionGuard.can(UserRole.ADMIN, Permission.INVENTORY_EDIT))
        assertTrue(PermissionGuard.can(UserRole.ADMIN, Permission.STOCK_MOVEMENT))
    }

    @Test
    fun adminHasEveryPermission() {
        Permission.entries.forEach { permission ->
            assertTrue(
                "ADMIN must be granted ${permission.name}",
                PermissionGuard.can(UserRole.ADMIN, permission)
            )
        }
    }

    @Test
    fun everyPermissionIsGrantedToAtLeastOneRole() {
        Permission.entries.forEach { permission ->
            val granted = PermissionMatrix.grants[permission]
            assertFalse(
                "${permission.name} is granted to nobody",
                granted.isNullOrEmpty()
            )
        }
    }

    @Test
    fun everyPermissionHasAnEntryInTheMatrix() {
        assertEquals(Permission.entries.toSet(), PermissionMatrix.grants.keys)
    }

    @Test
    fun cashierPermissionsAreASubsetOfAdminPermissions() {
        val cashier = PermissionMatrix.permissionsOf(UserRole.CASHIER).toSet()
        val admin = PermissionMatrix.permissionsOf(UserRole.ADMIN).toSet()
        assertTrue(
            "Cashier has a permission admin does not: ${cashier - admin}",
            admin.containsAll(cashier)
        )
    }

    @Test
    fun discountAtTheLimitNeedsNoAdminPin() {
        assertFalse(PermissionGuard.requiresAdminPinForDiscount(UserRole.CASHIER, 10.0))
        assertFalse(PermissionGuard.requiresAdminPinForDiscount(UserRole.CASHIER, 0.0))
    }

    @Test
    fun discountAboveTheLimitNeedsAnAdminPinForCashier() {
        assertTrue(PermissionGuard.requiresAdminPinForDiscount(UserRole.CASHIER, 10.01))
        assertTrue(PermissionGuard.requiresAdminPinForDiscount(UserRole.CASHIER, 50.0))
    }

    @Test
    fun adminNeverNeedsAPinForADiscount() {
        assertFalse(PermissionGuard.requiresAdminPinForDiscount(UserRole.ADMIN, 25.0))
        assertFalse(PermissionGuard.requiresAdminPinForDiscount(UserRole.ADMIN, 100.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeDiscountIsRejected() {
        PermissionGuard.requiresAdminPinForDiscount(UserRole.CASHIER, -1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun discountOverOneHundredIsRejected() {
        PermissionGuard.requiresAdminPinForDiscount(UserRole.ADMIN, 101.0)
    }

    @Test(expected = PermissionDeniedException::class)
    fun requireThrowsWhenCashierTriesToVoidABill() {
        PermissionGuard.require(UserRole.CASHIER, Permission.VOID_REFUND_EDIT_BILL)
    }

    @Test(expected = PermissionDeniedException::class)
    fun requireThrowsWhenCashierTriesToEditStock() {
        PermissionGuard.require(UserRole.CASHIER, Permission.STOCK_MOVEMENT)
    }

    @Test
    fun requireDoesNotThrowForAGrantedPermission() {
        PermissionGuard.require(UserRole.ADMIN, Permission.VOID_REFUND_EDIT_BILL)
        PermissionGuard.require(UserRole.CASHIER, Permission.BILLING_CART)
    }
}
