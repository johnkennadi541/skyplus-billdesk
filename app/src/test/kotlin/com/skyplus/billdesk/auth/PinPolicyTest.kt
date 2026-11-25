package com.skyplus.billdesk.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {

    @Test
    fun aCashierPinIsExactlyFourDigits() {
        assertTrue(PinPolicy.isValidCashierPin("1234"))
        assertFalse(PinPolicy.isValidCashierPin("123"))
        assertFalse(PinPolicy.isValidCashierPin("12345"))
        assertFalse(PinPolicy.isValidCashierPin("12a4"))
    }

    @Test
    fun anAdminPinIsFourToSixDigits() {
        assertTrue(PinPolicy.isValidAdminPin("1234"))
        assertTrue(PinPolicy.isValidAdminPin("123456"))
        assertFalse(PinPolicy.isValidAdminPin("123"))
        assertFalse(PinPolicy.isValidAdminPin("1234567"))
    }

    @Test
    fun obviousPinsAreFlagged() {
        assertTrue(PinPolicy.isObviousPin("0000"))
        assertTrue(PinPolicy.isObviousPin("1111"))
        assertTrue(PinPolicy.isObviousPin("1234"))
        assertTrue(PinPolicy.isObviousPin("4321"))
        assertFalse(PinPolicy.isObviousPin("1947"))
        assertFalse(PinPolicy.isObviousPin("8291"))
    }

    @Test
    fun fourWrongAttemptsDoNotLock() {
        var state = PinPolicy.LockState()
        repeat(4) { state = PinPolicy.onFailure(state, nowEpochMillis = 1000) }
        assertEquals(4, state.failedAttempts)
        assertFalse(PinPolicy.isLocked(state, 1000))
    }

    @Test
    fun theFifthWrongAttemptLocksForAMinute() {
        var state = PinPolicy.LockState()
        repeat(5) { state = PinPolicy.onFailure(state, nowEpochMillis = 1000) }
        assertEquals(5, state.failedAttempts)
        assertTrue(PinPolicy.isLocked(state, 1000))
        assertTrue(PinPolicy.isLocked(state, 1000 + 59_999))
        assertFalse(PinPolicy.isLocked(state, 1000 + 60_000))
    }

    @Test
    fun aSuccessResetsTheLadder() {
        var state = PinPolicy.LockState()
        repeat(4) { state = PinPolicy.onFailure(state, 1000) }
        state = PinPolicy.onSuccess()
        assertEquals(0, state.failedAttempts)
        assertEquals(null, state.lockedUntilEpochMillis)
    }

    @Test
    fun repeatedBlocksDoubleTheWait() {
        assertEquals(60_000L, PinPolicy.lockDuration(5))
        assertEquals(120_000L, PinPolicy.lockDuration(10))
        assertEquals(240_000L, PinPolicy.lockDuration(15))
        assertEquals(0L, PinPolicy.lockDuration(4))
    }

    @Test
    fun theWaitIsCappedAtHalfAnHour() {
        assertEquals(PinPolicy.MAX_LOCK_MILLIS, PinPolicy.lockDuration(1000))
    }

    @Test
    fun aLockExpires() {
        val state = PinPolicy.LockState(failedAttempts = 5, lockedUntilEpochMillis = 5000)
        assertTrue(PinPolicy.isLocked(state, 4999))
        assertFalse(PinPolicy.isLocked(state, 5000))
    }
}
