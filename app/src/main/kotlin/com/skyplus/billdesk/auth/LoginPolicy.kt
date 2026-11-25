package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.UserRole

/** How a profile proves who it is when the app opens. */
enum class LoginMethod {
    /** 4-digit PIN. Fast at the counter, works on every device. */
    PIN,

    /** Sign in with Google, admin only. Needs Google Play services on the device. */
    GOOGLE
}

/**
 * Which login a profile uses on which device.
 *
 * Decided with the owner: admin signs in with Google, cashier stays on a PIN.
 * A cashier's 4-digit PIN at 8pm on a busy Friday is the whole reason - a Google
 * account picker at the counter would cost seconds on every bill.
 *
 * A PIN fallback must always exist for admin too: many Android POS terminals ship
 * without Google Play services, and Google's own guidance is to check for Play
 * services before calling any Google API. A till that cannot be opened is worse
 * than a till with a weaker login.
 */
object LoginPolicy {

    /** Cashier is always PIN, whatever the device can do. */
    fun methodFor(role: UserRole, playServicesAvailable: Boolean): LoginMethod =
        when {
            role == UserRole.CASHIER -> LoginMethod.PIN
            playServicesAvailable -> LoginMethod.GOOGLE
            else -> LoginMethod.PIN
        }

    /**
     * The device has no Play services, so admin cannot use Google here and falls
     * back to a PIN. The UI must say so plainly, not fail silently.
     */
    fun adminNeedsPinFallback(playServicesAvailable: Boolean): Boolean = !playServicesAvailable

    /** Always true: no device may ever be locked out of its own till. */
    const val PIN_FALLBACK_ALWAYS_AVAILABLE: Boolean = true

    /** Google sign-in is for admin only; a cashier never sees the account picker. */
    fun googleAllowedFor(role: UserRole): Boolean = role == UserRole.ADMIN
}
