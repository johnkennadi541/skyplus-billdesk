package com.skyplus.billdesk.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictPolicyTest {

    @Test
    fun everyEntityHasARule() {
        SyncEntity.entries.forEach { entity ->
            ConflictPolicy.ruleFor(entity)
        }
        assertEquals(SyncEntity.entries.size, SyncEntity.entries.map { ConflictPolicy.ruleFor(it) }.size)
    }

    @Test
    fun moneyIsNeverOverwritten() {
        assertTrue(ConflictPolicy.isAppendOnly(SyncEntity.BILL))
        assertTrue(ConflictPolicy.isAppendOnly(SyncEntity.BILL_ITEM))
        assertTrue(ConflictPolicy.isAppendOnly(SyncEntity.STOCK_MOVEMENT))
        assertFalse(ConflictPolicy.newerWins(SyncEntity.BILL, 200, 100))
    }

    @Test
    fun aBillCanOnlyBeVoidedOnce() {
        assertFalse(ConflictPolicy.newerWins(SyncEntity.BILL_VOID, 200, 100))
        assertEquals(ConflictRule.MONOTONIC_VOID, ConflictPolicy.ruleFor(SyncEntity.BILL_VOID))
    }

    @Test
    fun menuAndSettingsTakeTheNewestEdit() {
        assertTrue(ConflictPolicy.newerWins(SyncEntity.MENU_ITEM, 200, 100))
        assertTrue(ConflictPolicy.newerWins(SyncEntity.SETTINGS, 200, 100))
    }

    @Test
    fun anEqualTimestampKeepsTheExistingRow() {
        assertFalse(ConflictPolicy.newerWins(SyncEntity.MENU_ITEM, 100, 100))
    }

    @Test
    fun anOlderEditNeverWins() {
        assertFalse(ConflictPolicy.newerWins(SyncEntity.MENU_ITEM, 100, 200))
    }

    @Test
    fun stockQuantityIsDerivedOnTheServer() {
        assertTrue(ConflictPolicy.isServerDerived(SyncEntity.INVENTORY_ITEM))
        assertFalse(ConflictPolicy.newerWins(SyncEntity.INVENTORY_ITEM, 200, 100))
        assertFalse(ConflictPolicy.isAppendOnly(SyncEntity.INVENTORY_ITEM))
    }

    @Test
    fun membershipIsServerDerived() {
        assertTrue(ConflictPolicy.isServerDerived(SyncEntity.MEMBER))
    }
}
