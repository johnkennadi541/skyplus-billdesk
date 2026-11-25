package com.skyplus.billdesk.session

import com.skyplus.billdesk.auth.LandingProfile
import com.skyplus.billdesk.auth.PinHasher
import com.skyplus.billdesk.auth.PinPolicy
import com.skyplus.billdesk.model.UserRole

/**
 * Opening the till: the one place that decides whether a PIN is right, counts
 * failures, locks the profile and applies the landing choice.
 *
 * The clock is injected so the lockout ladder can be tested in milliseconds
 * instead of by waiting a minute.
 */
class SessionManager(
    private val store: SessionStore,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /** Which profile the app should open in on this device. */
    fun landingRole(): UserRole = LandingProfile.roleFor(store.landingRole())

    fun setLandingRole(role: UserRole) {
        store.setLandingRole(role)
    }

    fun isLocked(role: UserRole): Boolean =
        PinPolicy.isLocked(store.lockState(role), now())

    /**
     * Checks a PIN. Every outcome is a value, not an exception: a wrong PIN is an
     * ordinary thing that happens forty times a day at a counter.
     */
    fun loginWithPin(role: UserRole, pin: String): LoginResult {
        val account = store.accountFor(role) ?: return LoginResult.NoAccount
        if (!account.isActive) return LoginResult.NoAccount

        val state = store.lockState(role)
        if (PinPolicy.isLocked(state, now())) {
            return LoginResult.Locked(state.lockedUntilEpochMillis ?: now())
        }

        val record = account.pinRecord ?: return LoginResult.PinNotSet
        if (PinHasher.verify(pin, record)) {
            store.saveLockState(role, PinPolicy.onSuccess())
            return LoginResult.Success(role, account.displayName)
        }

        val failed = PinPolicy.onFailure(state, now())
        store.saveLockState(role, failed)
        if (PinPolicy.isLocked(failed, now())) {
            return LoginResult.Locked(failed.lockedUntilEpochMillis ?: now())
        }
        return LoginResult.WrongPin(
            attemptsLeft = (PinPolicy.MAX_ATTEMPTS - failed.failedAttempts).coerceAtLeast(0)
        )
    }

    /**
     * Sets or replaces a PIN. The shape is enforced by [PinPolicy] before anything
     * is hashed, so a three-digit cashier PIN can never reach the store.
     */
    fun setPin(role: UserRole, pin: String) {
        val valid = when (role) {
            UserRole.CASHIER -> PinPolicy.isValidCashierPin(pin)
            UserRole.ADMIN -> PinPolicy.isValidAdminPin(pin)
        }
        require(valid) { "That PIN does not match the rules for $role" }

        val existing = store.accountFor(role) ?: defaultAccount(role)
        store.saveAccount(existing.copy(pinRecord = PinHasher.hash(pin)))
        store.saveLockState(role, PinPolicy.onSuccess())
    }

    /** True when this device has not been set up yet and needs an admin PIN. */
    fun needsSetup(): Boolean = store.accounts().isEmpty()

    private fun defaultAccount(role: UserRole): UserAccount = when (role) {
        UserRole.CASHIER -> UserAccount(
            id = "cashier-1",
            role = UserRole.CASHIER,
            displayName = "Cashier"
        )

        UserRole.ADMIN -> UserAccount(
            id = "admin-1",
            role = UserRole.ADMIN,
            displayName = "Admin",
            email = ""
        )
    }
}
