package com.skyplus.billdesk.auth

/**
 * The rules around a PIN: what counts as valid, and what happens after repeated
 * failures. Pure logic, so the lockout can be tested without a device.
 */
object PinPolicy {

    const val CASHIER_PIN_LENGTH = 4
    const val ADMIN_PIN_MIN_LENGTH = 4
    const val ADMIN_PIN_MAX_LENGTH = 6

    const val MAX_ATTEMPTS = 5
    const val FIRST_LOCK_MILLIS = 60_000L
    const val MAX_LOCK_MILLIS = 30 * 60_000L

    fun isValidCashierPin(pin: String): Boolean = pin.length == CASHIER_PIN_LENGTH && pin.all { it.isDigit() }

    fun isValidAdminPin(pin: String): Boolean =
        pin.length in ADMIN_PIN_MIN_LENGTH..ADMIN_PIN_MAX_LENGTH && pin.all { it.isDigit() }

    /**
     * Rejects the PINs people actually choose when a device is set up in a hurry.
     * The check is a warning at setup, not a hard block, except for the worst ones.
     */
    fun isObviousPin(pin: String): Boolean {
        if (pin.isEmpty()) return true
        if (pin.toSet().size == 1) return true
        val sequences = listOf("0123456789", "9876543210")
        return sequences.any { it.contains(pin) }
    }

    /** Where a profile stands on the lockout ladder. */
    data class LockState(
        val failedAttempts: Int = 0,
        val lockedUntilEpochMillis: Long? = null
    )

    fun isLocked(state: LockState, nowEpochMillis: Long): Boolean {
        val until = state.lockedUntilEpochMillis ?: return false
        return nowEpochMillis < until
    }

    /** Applies a wrong PIN: counts up and locks on the fifth failure. */
    fun onFailure(state: LockState, nowEpochMillis: Long): LockState {
        val attempts = state.failedAttempts + 1
        if (attempts < MAX_ATTEMPTS) return LockState(attempts, null)
        val lockFor = lockDuration(attempts)
        return LockState(attempts, nowEpochMillis + lockFor)
    }

    /** Applies a correct PIN: the ladder resets completely. */
    fun onSuccess(): LockState = LockState()

    /** Each block of five failures doubles the wait, up to half an hour. */
    fun lockDuration(attempts: Int): Long {
        if (attempts < MAX_ATTEMPTS) return 0L
        val block = attempts / MAX_ATTEMPTS
        var duration = FIRST_LOCK_MILLIS
        repeat((block - 1).coerceAtLeast(0)) {
            duration *= 2
        }
        return minOf(duration, MAX_LOCK_MILLIS)
    }
}
