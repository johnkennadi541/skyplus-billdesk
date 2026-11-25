package com.skyplus.billdesk.session

import com.skyplus.billdesk.model.UserRole

/** Every way a login attempt can end. */
sealed interface LoginResult {

    data class Success(val role: UserRole, val displayName: String) : LoginResult

    /** Wrong PIN, still allowed to try again. */
    data class WrongPin(val attemptsLeft: Int) : LoginResult

    /** Too many failures: nothing is accepted until [untilEpochMillis]. */
    data class Locked(val untilEpochMillis: Long) : LoginResult

    /** No profile on this device for that role - the admin must be set up first. */
    data object NoAccount : LoginResult

    /** The profile exists but has no PIN set yet. */
    data object PinNotSet : LoginResult
}
