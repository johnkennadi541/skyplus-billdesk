package com.skyplus.billdesk.session

import com.skyplus.billdesk.auth.PinPolicy
import com.skyplus.billdesk.model.UserRole

/**
 * Where the profiles, the landing choice and the lockout state live.
 *
 * An interface so the login logic can be tested against an in-memory store, and so
 * the Room implementation in the next step is a swap rather than a rewrite.
 */
interface SessionStore {
    fun accounts(): List<UserAccount>
    fun accountFor(role: UserRole): UserAccount?
    fun saveAccount(account: UserAccount)
    fun lockState(role: UserRole): PinPolicy.LockState
    fun saveLockState(role: UserRole, state: PinPolicy.LockState)

    /** Null means this device has never chosen, so the default applies. */
    fun landingRole(): UserRole?
    fun setLandingRole(role: UserRole)
}

/** Until Room lands. Deliberately simple, and honest about being temporary. */
class InMemorySessionStore(
    private val accounts: MutableMap<UserRole, UserAccount> = mutableMapOf(),
    private val lockStates: MutableMap<UserRole, PinPolicy.LockState> = mutableMapOf(),
    private var landing: UserRole? = null
) : SessionStore {

    override fun accounts(): List<UserAccount> = accounts.values.toList()

    override fun accountFor(role: UserRole): UserAccount? = accounts[role]

    override fun saveAccount(account: UserAccount) {
        accounts[account.role] = account
    }

    override fun lockState(role: UserRole): PinPolicy.LockState =
        lockStates[role] ?: PinPolicy.LockState()

    override fun saveLockState(role: UserRole, state: PinPolicy.LockState) {
        lockStates[role] = state
    }

    override fun landingRole(): UserRole? = landing

    override fun setLandingRole(role: UserRole) {
        landing = role
    }
}
