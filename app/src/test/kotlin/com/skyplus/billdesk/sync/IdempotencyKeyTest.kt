package com.skyplus.billdesk.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdempotencyKeyTest {

    @Test
    fun generatesValidKeys() {
        repeat(200) {
            assertTrue(IdempotencyKey.isValid(IdempotencyKey.newKey()))
        }
    }

    @Test
    fun twoKeysAreNeverTheSame() {
        assertNotEquals(IdempotencyKey.newKey(), IdempotencyKey.newKey())
    }

    @Test
    fun acceptsACanonicalUuidV4() {
        assertTrue(IdempotencyKey.isValid("3f2504e0-4f89-41d3-9a0c-0305e82c3301"))
    }

    @Test
    fun rejectsUppercase() {
        assertFalse(IdempotencyKey.isValid("3F2504E0-4F89-41D3-9A0C-0305E82C3301"))
    }

    @Test
    fun rejectsTheWrongVersionNibble() {
        assertFalse(IdempotencyKey.isValid("3f2504e0-4f89-11d3-9a0c-0305e82c3301"))
    }

    @Test
    fun rejectsGarbage() {
        assertFalse(IdempotencyKey.isValid(""))
        assertFalse(IdempotencyKey.isValid("bill-1"))
        assertFalse(IdempotencyKey.isValid("3f2504e0-4f89-41d3-9a0c"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun requireValidThrowsOnGarbage() {
        IdempotencyKey.requireValid("bill-1")
    }

    @Test
    fun requireValidReturnsTheKey() {
        val key = IdempotencyKey.newKey()
        assertEquals(key, IdempotencyKey.requireValid(key))
    }
}
